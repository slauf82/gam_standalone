package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 40k36q8: unbeaufsichtigte Inventarisierungs-/Discovery-Läufe für lokale Scheduler. */
@Service
public class InventoryAutomationService {
  private static final Logger log = LoggerFactory.getLogger(InventoryAutomationService.class);
  private final DeviceDiscoveryService discovery;
  private final DeviceIdentityService identities;
  private final JdbcTemplate jdbc;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public InventoryAutomationService(DeviceDiscoveryService discovery, DeviceIdentityService identities, JdbcTemplate jdbc) {
    this.discovery = discovery;
    this.identities = identities;
    this.jdbc = jdbc;
  }

  public enum Mode { DISCOVERY, INVENTORY, FULL }
  public record RunResult(long runId, String mode, boolean success, String startedAt, String finishedAt,
                          long durationMs, int discoveredDevices, int registeredDevices,
                          int inventoryAttempts, int inventorySuccesses, int inventoryFailures,
                          List<String> messages) {}

  @PostConstruct
  void init() {
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_inventory_automation_run (
        id BIGINT NOT NULL AUTO_INCREMENT,
        mode VARCHAR(32) NOT NULL,
        status VARCHAR(32) NOT NULL,
        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        finished_at TIMESTAMP NULL,
        duration_ms BIGINT NULL,
        discovered_devices INT NOT NULL DEFAULT 0,
        registered_devices INT NOT NULL DEFAULT 0,
        inventory_attempts INT NOT NULL DEFAULT 0,
        inventory_successes INT NOT NULL DEFAULT 0,
        inventory_failures INT NOT NULL DEFAULT 0,
        message TEXT NULL,
        PRIMARY KEY(id)
      )
      """);
  }

  public boolean isRunning() { return running.get(); }

  public RunResult run(String requestedMode) {
    Mode mode;
    try { mode = Mode.valueOf((requestedMode == null || requestedMode.isBlank() ? "FULL" : requestedMode).trim().toUpperCase(Locale.ROOT)); }
    catch (Exception e) { throw new IllegalArgumentException("Unbekannter Automationsmodus. Erlaubt: DISCOVERY, INVENTORY, FULL."); }
    if (!running.compareAndSet(false, true)) throw new IllegalStateException("Es läuft bereits ein automatischer Gerätescan.");

    long startedMs = System.currentTimeMillis();
    String startedAt = OffsetDateTime.now().toString();
    jdbc.update("INSERT INTO gam_inventory_automation_run(mode,status,started_at) VALUES(?, 'RUNNING', CURRENT_TIMESTAMP)", mode.name());
    Long runId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    List<String> messages = new ArrayList<>();
    int discovered = 0, registered = 0, attempts = 0, successes = 0, failures = 0;
    boolean success = false;
    try {
      // FULL inventarisiert bekannte Geräte zuerst vollständig. Der anschließende Discovery-Lauf
      // sucht/aktualisiert Geräte, startet aber bewusst KEINE zweite nachgelagerte Plattforminventarisierung.
      if (mode == Mode.INVENTORY || mode == Mode.FULL) {
        List<DeviceIdentityService.IdentityRow> rows = identities.overview();
        registered = rows.size();
        for (DeviceIdentityService.IdentityRow row : rows) {
          List<DeviceIdentityService.PlatformInventoryResult> results;
          try {
            results = identities.runAllKnownPlatforms(row.identityKey(), "automatisch (Scheduler 40k36q8)");
          } catch (Exception e) {
            failures++; attempts++;
            messages.add((row.name() == null ? row.identityKey() : row.name()) + ": " + Objects.toString(e.getMessage(), "Inventarisierung fehlgeschlagen"));
            continue;
          }
          if (results.isEmpty()) {
            messages.add((row.name() == null ? row.identityKey() : row.name()) + ": keine unterstützte Plattform ermittelt; übersprungen");
            continue;
          }
          for (var result : results) {
            attempts++;
            if (result.success()) successes++; else failures++;
            if (!result.success()) messages.add((row.name() == null ? row.identityKey() : row.name()) + " / " + result.platform() + ": " + result.message());
          }
        }
      }

      if (mode == Mode.DISCOVERY || mode == Mode.FULL) {
        List<DiscoveredDevice> found = discovery.scan();
        discovered = found.size();
        registered = discovery.registeredCount();
      }

      success = failures == 0;
      if (messages.isEmpty()) messages.add(success ? "Automatischer Lauf erfolgreich abgeschlossen." : "Automatischer Lauf mit Fehlern abgeschlossen.");
      return finish(runId == null ? -1 : runId, mode, success, startedAt, startedMs, discovered, registered, attempts, successes, failures, messages);
    } catch (RuntimeException e) {
      failures++;
      messages.add(Objects.toString(e.getMessage(), e.getClass().getSimpleName()));
      finish(runId == null ? -1 : runId, mode, false, startedAt, startedMs, discovered, registered, attempts, successes, failures, messages);
      throw e;
    } finally {
      running.set(false);
    }
  }

  private RunResult finish(long runId, Mode mode, boolean success, String startedAt, long startedMs,
                           int discovered, int registered, int attempts, int successes, int failures, List<String> messages) {
    long duration = System.currentTimeMillis() - startedMs;
    String finishedAt = OffsetDateTime.now().toString();
    String msg = String.join(" | ", messages);
    jdbc.update("""
      UPDATE gam_inventory_automation_run SET status=?,finished_at=CURRENT_TIMESTAMP,duration_ms=?,discovered_devices=?,
      registered_devices=?,inventory_attempts=?,inventory_successes=?,inventory_failures=?,message=? WHERE id=?
      """, success ? "SUCCESS" : "COMPLETED_WITH_ERRORS", duration, discovered, registered, attempts, successes, failures, msg, runId);
    log.info("[AUTOMATION-SCAN] runId={} mode={} success={} discovered={} registered={} inventory={}/{} failures={} durationMs={}",
      runId, mode, success, discovered, registered, successes, attempts, failures, duration);
    return new RunResult(runId, mode.name(), success, startedAt, finishedAt, duration, discovered, registered, attempts, successes, failures, List.copyOf(messages));
  }

  public List<Map<String,Object>> history(int limit) {
    int safe = Math.max(1, Math.min(limit, 100));
    return jdbc.queryForList("SELECT id,mode,status,started_at AS startedAt,finished_at AS finishedAt,duration_ms AS durationMs,discovered_devices AS discoveredDevices,registered_devices AS registeredDevices,inventory_attempts AS inventoryAttempts,inventory_successes AS inventorySuccesses,inventory_failures AS inventoryFailures,message FROM gam_inventory_automation_run ORDER BY id DESC LIMIT " + safe);
  }
}
