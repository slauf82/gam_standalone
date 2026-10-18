package de.kopfzentrum.gam.inventory;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.*;

/** 40k36q8: localhost-only Einstieg für Windows Aufgabenplanung, cron/systemd und launchd. */
@RestController
@RequestMapping("/api/inventory/automation")
public class InventoryAutomationController {
  private final InventoryAutomationService service;
  public InventoryAutomationController(InventoryAutomationService service){ this.service=service; }

  public record RunRequest(String mode) {}

  @PostMapping("/run")
  public ResponseEntity<?> run(@RequestBody(required=false) RunRequest body, HttpServletRequest request) {
    if (!isLoopback(request.getRemoteAddr())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
      "error","AUTOMATION_LOCAL_ONLY","message","Automatische Scans dürfen nur lokal auf dem GAM-Backend-Host gestartet werden."));
    try {
      return ResponseEntity.ok(service.run(body == null ? "FULL" : body.mode()));
    } catch (IllegalStateException busy) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","AUTOMATION_ALREADY_RUNNING","message",busy.getMessage()));
    }
  }

  @GetMapping("/status")
  public ResponseEntity<?> status(HttpServletRequest request) {
    if (!isLoopback(request.getRemoteAddr())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","AUTOMATION_LOCAL_ONLY"));
    return ResponseEntity.ok(Map.of("running",service.isRunning(),"modes",List.of("DISCOVERY","INVENTORY","FULL"),"defaultMode","FULL","inventoryDepth","VOLLSTAENDIG"));
  }

  @GetMapping("/history")
  public ResponseEntity<?> history(@RequestParam(name="limit",defaultValue="20") int limit, HttpServletRequest request) {
    if (!isLoopback(request.getRemoteAddr())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","AUTOMATION_LOCAL_ONLY"));
    return ResponseEntity.ok(service.history(limit));
  }

  private static boolean isLoopback(String remote) {
    try { return remote != null && InetAddress.getByName(remote).isLoopbackAddress(); }
    catch (Exception e) { return false; }
  }
}
