package de.kopfzentrum.gam.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 40k34b: ADB als optionale Tiefeninventarisierungsquelle für Android -
 * vergleichbar mit SSH bei Linux (LinuxNetworkDiscoveryService). Reine
 * Wiederverwendung desselben Musters: konfigurierbarer Pfad/Einstellungen,
 * ProcessBuilder mit Timeout, strukturierte, fehlertolerante Auswertung.
 *
 * Sicherheitsprinzipien (siehe Auftrag):
 * - Ausschließlich feste, vorab definierte Befehle - keine freie Shell, keine
 *   Verkettung aus Benutzereingaben. Adressen/Ports werden validiert, bevor
 *   sie überhaupt in einen ProcessBuilder-Aufruf gelangen.
 * - ProcessBuilder erhält IMMER eine Argumentliste (nie eine zusammengesetzte
 *   Shell-Zeile) - "adb -s SERIAL shell getprop" wird als vier getrennte
 *   Argumente übergeben, nicht als String verkettet.
 * - Kein "adb root", kein "su", keine schreibenden Befehle.
 * - Pairing-Code wird niemals protokolliert, niemals gespeichert.
 */
@Service
public class AndroidAdbService {
  private static final Logger log = LoggerFactory.getLogger(AndroidAdbService.class);
  private final Environment env;

  public AndroidAdbService(Environment env) { this.env = env; }

  public record AdbAvailability(boolean available, String path, String version) {}
  public record AdbDevice(String serial, String state, String transportId, String model,
                          String product, String device, boolean network) {}
  public record AdbStatus(boolean available, String path, String version, boolean serverActive,
                          List<AdbDevice> devices, int authorized, int unauthorized, int offline,
                          int otherState, String checkedAt) {}
  public record ActionResult(boolean success, String message) {}
  public record InventoryResult(boolean success, Map<String,String> fields, String status, long durationMs) {}
  public record PackageDumpResult(String output, boolean timedOut, boolean truncated, String warning) {}
  private record CommandResult(String output, boolean timedOut, boolean truncated, int exitCode, String error) {}
  private record StreamRead(String text, boolean truncated) {}

  /**
   * 40k34b: Prüft zunächst einen konfigurierten Pfad
   * (gam.discovery.android.adb-path / GAM_ANDROID_ADB_PATH), danach ein
   * bereits im PATH installiertes "adb". Kein automatischer Download - das
   * Projekt besitzt keine allgemeine, abgesicherte Downloadarchitektur für
   * externe Werkzeuge, daher wird keine neue eingeführt.
   */
  public AdbAvailability findAdb() {
    String configured = setting("gam.discovery.android.adb-path", "GAM_ANDROID_ADB_PATH", "");
    List<String> candidates = new ArrayList<>();
    if (!configured.isBlank()) candidates.add(configured);
    candidates.add("adb");
    for (String candidate : candidates) {
      String version = runVersion(candidate);
      if (version != null) return new AdbAvailability(true, candidate, version);
    }
    return new AdbAvailability(false, null, null);
  }

  private String runVersion(String adbPath) {
    try {
      Process p = new ProcessBuilder(adbPath, "version").redirectErrorStream(true).start();
      if (!p.waitFor(5, TimeUnit.SECONDS)) { p.destroyForcibly(); return null; }
      String out = readStream(p.getInputStream());
      if (p.exitValue() != 0 && out.isBlank()) return null;
      Matcher m = Pattern.compile("Android Debug Bridge version ([\\d.]+)").matcher(out);
      return m.find() ? m.group(1) : (out.isBlank() ? null : "unbekannt");
    } catch (Exception e) { return null; }
  }

  /** 40k34b: Liest "adb devices -l" aus und unterscheidet device/unauthorized/offline/sonstige. */
  public AdbStatus status() {
    var avail = findAdb();
    String now = OffsetDateTime.now().toString();
    if (!avail.available()) {
      log.info("[ADB] Nicht verfügbar (weder konfigurierter Pfad noch PATH) - Android-Erkennung ohne ADB bleibt unverändert verfügbar");
      return new AdbStatus(false, null, null, false, List.of(), 0, 0, 0, 0, now);
    }
    List<AdbDevice> devices = new ArrayList<>();
    boolean serverActive = false;
    try {
      Process p = new ProcessBuilder(avail.path(), "devices", "-l").redirectErrorStream(true).start();
      if (p.waitFor(8, TimeUnit.SECONDS)) {
        serverActive = true;
        String out = readStream(p.getInputStream());
        for (String rawLine : out.split("\n")) {
          String line = rawLine.trim();
          if (line.isEmpty() || line.toLowerCase(Locale.ROOT).startsWith("list of devices") || line.startsWith("*")) continue;
          String[] parts = line.split("\\s+");
          if (parts.length < 2) continue;
          String serial = parts[0];
          String state = parts[1];
          String model = null, product = null, device = null, transportId = null;
          for (int i = 2; i < parts.length; i++) {
            String tok = parts[i];
            if (tok.startsWith("model:")) model = tok.substring("model:".length());
            else if (tok.startsWith("product:")) product = tok.substring("product:".length());
            else if (tok.startsWith("device:")) device = tok.substring("device:".length());
            else if (tok.startsWith("transport_id:")) transportId = tok.substring("transport_id:".length());
          }
          boolean network = serial.matches(".*:\\d+$");
          devices.add(new AdbDevice(serial, state, transportId, model, product, device, network));
        }
      } else p.destroyForcibly();
    } catch (Exception e) {
      log.debug("[ADB] 'adb devices -l' fehlgeschlagen: {}", e.toString());
    }
    int authorized = (int) devices.stream().filter(d -> "device".equals(d.state())).count();
    int unauthorized = (int) devices.stream().filter(d -> "unauthorized".equals(d.state())).count();
    int offline = (int) devices.stream().filter(d -> "offline".equals(d.state())).count();
    int other = devices.size() - authorized - unauthorized - offline;
    log.info("[ADB] Status geprüft: Pfad={}, Version={}, Geräte={} (autorisiert={}, nicht autorisiert={}, offline={}, sonstige={})",
      avail.path(), avail.version(), devices.size(), authorized, unauthorized, offline, other);
    return new AdbStatus(true, avail.path(), avail.version(), serverActive, devices, authorized, unauthorized, offline, other, now);
  }

  /**
   * 40k34b: Modernes Wireless Debugging. Der Pairing-Code wird NIEMALS
   * protokolliert oder gespeichert - er verlässt diese Methode nicht in
   * irgendeiner Log-/Fehlerausgabe.
   */
  public ActionResult pair(String host, int pairingPort, String pairingCode) {
    if (!validHost(host)) return new ActionResult(false, "Ungültige IP-Adresse oder ungültiger Hostname.");
    if (!validPort(pairingPort)) return new ActionResult(false, "Ungültiger Pairing-Port.");
    if (pairingCode == null || !pairingCode.matches("\\d{6}")) return new ActionResult(false, "Der Pairing-Code muss aus 6 Ziffern bestehen.");
    var avail = findAdb();
    if (!avail.available()) return new ActionResult(false, "ADB ist nicht verfügbar.");
    try {
      Process p = new ProcessBuilder(avail.path(), "pair", host + ":" + pairingPort, pairingCode).start();
      boolean finished = p.waitFor(15, TimeUnit.SECONDS);
      if (!finished) {
        p.destroyForcibly();
        log.info("[ADB] Pairing zu {}:{} lief in eine Zeitüberschreitung", host, pairingPort);
        return new ActionResult(false, "Zeitüberschreitung beim Koppeln - Gerät nicht erreichbar oder Pairing-Code abgelaufen.");
      }
      String out = readStream(p.getInputStream());
      boolean ok = p.exitValue() == 0 && out.toLowerCase(Locale.ROOT).contains("successfully paired");
      log.info("[ADB] Pairing zu {}:{} {}", host, pairingPort, ok ? "erfolgreich" : "fehlgeschlagen");
      return new ActionResult(ok, ok ? "Pairing erfolgreich." : "Pairing fehlgeschlagen - Pairing-Code ungültig oder abgelaufen, oder das Gerät hat die Kopplung abgelehnt.");
    } catch (Exception e) {
      log.warn("[ADB] Pairing-Prozess konnte nicht gestartet werden: {}", e.toString());
      return new ActionResult(false, "ADB-Prozess konnte nicht gestartet werden.");
    }
  }

  public ActionResult connect(String host, int port) {
    if (!validHost(host)) return new ActionResult(false, "Ungültige IP-Adresse oder ungültiger Hostname.");
    if (!validPort(port)) return new ActionResult(false, "Ungültiger Port.");
    var avail = findAdb();
    if (!avail.available()) return new ActionResult(false, "ADB ist nicht verfügbar.");
    try {
      Process p = new ProcessBuilder(avail.path(), "connect", host + ":" + port).start();
      if (!p.waitFor(10, TimeUnit.SECONDS)) { p.destroyForcibly(); log.info("[ADB] Verbindung zu {}:{} in Zeitüberschreitung gelaufen", host, port); return new ActionResult(false, "Zeitüberschreitung - Gerät nicht erreichbar."); }
      String out = readStream(p.getInputStream());
      boolean ok = out.toLowerCase(Locale.ROOT).contains("connected to");
      log.info("[ADB] Verbindungsversuch zu {}:{}: {}", host, port, ok ? "erfolgreich" : "fehlgeschlagen");
      return new ActionResult(ok, out.isBlank() ? "Keine Rückmeldung von ADB." : out.trim());
    } catch (Exception e) {
      log.warn("[ADB] Verbindungs-Prozess konnte nicht gestartet werden: {}", e.toString());
      return new ActionResult(false, "ADB-Prozess konnte nicht gestartet werden.");
    }
  }

  /** 40k34b: Trennt gezielt NUR die angegebene Verbindung - niemals alle ADB-Geräte auf einmal. */
  public ActionResult disconnect(String host, int port) {
    if (!validHost(host)) return new ActionResult(false, "Ungültige IP-Adresse oder ungültiger Hostname.");
    if (!validPort(port)) return new ActionResult(false, "Ungültiger Port.");
    var avail = findAdb();
    if (!avail.available()) return new ActionResult(false, "ADB ist nicht verfügbar.");
    try {
      Process p = new ProcessBuilder(avail.path(), "disconnect", host + ":" + port).start();
      if (!p.waitFor(10, TimeUnit.SECONDS)) { p.destroyForcibly(); return new ActionResult(false, "Zeitüberschreitung beim Trennen."); }
      String out = readStream(p.getInputStream());
      log.info("[ADB] Verbindung zu {}:{} getrennt", host, port);
      return new ActionResult(true, out.isBlank() ? "Verbindung getrennt." : out.trim());
    } catch (Exception e) {
      log.warn("[ADB] Trenn-Prozess konnte nicht gestartet werden: {}", e.toString());
      return new ActionResult(false, "ADB-Prozess konnte nicht gestartet werden.");
    }
  }

  // 40k34b: feste, vorab definierte, ausschließlich lesende Befehle - keine freie Shell,
  // keine Root-Rechte, keine großen dumpsys-Gesamtausgaben.
  private static final List<List<String>> SHELL_COMMANDS = List.of(
    List.of("GETPROP", "getprop"),
    List.of("UNAME", "uname", "-a"),
    List.of("MEMINFO", "cat", "/proc/meminfo"),
    List.of("DISKS", "df"),
    List.of("DISPLAYSIZE", "wm", "size"),
    List.of("DISPLAYDENSITY", "wm", "density"),
    List.of("BATTERY", "dumpsys", "battery"),
    List.of("UPTIME", "uptime"),
    List.of("SELINUX", "getenforce"),
    List.of("NETWORK", "ip", "-4", "-o", "addr", "show", "scope", "global"),
    // 40k34d: Laufzeitzustand - additive Erweiterung derselben Befehlsliste/Pipeline,
    // keine zweite Inventarisierung, kein neuer Befehlsausführungsmechanismus.
    List.of("PROCESSES", "ps", "-A"),
    List.of("SERVICES", "dumpsys", "activity", "services"),
    List.of("USERS", "pm", "list", "users"),
    List.of("POWER", "dumpsys", "power"),
    List.of("CPU_ONLINE", "cat", "/sys/devices/system/cpu/online"),
    List.of("LOADAVG", "cat", "/proc/loadavg"),
    List.of("AIRPLANE_MODE", "settings", "get", "global", "airplane_mode_on"),
    List.of("WIFI_ON", "settings", "get", "global", "wifi_on"),
    List.of("BLUETOOTH_ON", "settings", "get", "global", "bluetooth_on"),
    List.of("DEFAULT_IME", "settings", "get", "secure", "default_input_method"),
    List.of("ACCESSIBILITY_SERVICES", "settings", "get", "secure", "enabled_accessibility_services"),
    List.of("NOTIFICATION_LISTENERS", "settings", "get", "secure", "enabled_notification_listeners"),
    List.of("AUTOFILL_SERVICE", "settings", "get", "secure", "autofill_service"),
    List.of("ROLE_DIALER", "cmd", "role", "holders", "android.app.role.DIALER"),
    List.of("ROLE_SMS", "cmd", "role", "holders", "android.app.role.SMS"),
    List.of("ROLE_ASSISTANT", "cmd", "role", "holders", "android.app.role.ASSISTANT"),
    List.of("DEVICE_POLICY", "dumpsys", "device_policy"),
    List.of("CONNECTIVITY", "dumpsys", "connectivity")
  );

  // 40k34d: nur die wenigen potenziell großen dumpsys-Sammelausgaben erhalten einen
  // größeren (weiterhin begrenzten) Lesepuffer als die übrigen, kleinen Befehle -
  // dieselbe runShellCommand()-Architektur aus 40k34b/c, kein neuer Mechanismus.
  private static final Map<String,Integer> LARGE_OUTPUT_LABELS = Map.of(
    "SERVICES", 180000, "POWER", 80000, "DEVICE_POLICY", 100000, "CONNECTIVITY", 140000, "PROCESSES", 100000
  );

  // 40k35a: Kleine Befehle müssen nicht pauschal 15 Sekunden blockieren.
  // Nur die erfahrungsgemäß größeren Android-Quellen erhalten etwas mehr Zeit.
  private static final Map<String,Integer> COMMAND_TIMEOUT_SECONDS = Map.of(
    "GETPROP", 8, "PROCESSES", 8, "SERVICES", 12, "POWER", 10,
    "DEVICE_POLICY", 10, "CONNECTIVITY", 10
  );

  /**
   * 40k34b: Führt die erste echte Android-Inventarisierung über ADB Shell durch.
   * Jeder Befehl läuft einzeln mit eigenem Timeout; ein fehlgeschlagener Befehl
   * bricht die übrigen nicht ab (siehe Fehlerbehandlung im Auftrag). Adressiert
   * IMMER über "-s SERIAL", damit bei mehreren verbundenen Geräten keine Daten
   * vermischt werden.
   */
  public InventoryResult inventorize(String serial) {
    var avail = findAdb();
    if (!avail.available()) return new InventoryResult(false, Map.of(), "ADB nicht verfügbar", 0);
    if (serial == null || serial.isBlank()) return new InventoryResult(false, Map.of(), "Keine Seriennummer/Transport-ID angegeben", 0);
    long started = System.currentTimeMillis();
    Map<String,String> raw = new LinkedHashMap<>();
    List<String> partialErrors = new ArrayList<>();
    log.info("[ADB] Inventarisierung gestartet für Gerät {}", shortSerial(serial));
    for (List<String> cmd : SHELL_COMMANDS) {
      String label = cmd.get(0);
      List<String> args = new ArrayList<>(List.of(avail.path(), "-s", serial, "shell"));
      args.addAll(cmd.subList(1, cmd.size()));
      int cap = LARGE_OUTPUT_LABELS.getOrDefault(label, 20000);
      long commandStarted = System.currentTimeMillis();
      try {
        int timeoutSeconds = COMMAND_TIMEOUT_SECONDS.getOrDefault(label, 5);
        CommandResult commandResult = runShellCommandDetailed(args, timeoutSeconds, cap);
        String output = commandResult.output();
        if (output != null && !output.isBlank()) {
          raw.put(label, output);
          if (commandResult.timedOut()) partialErrors.add(label + " (Zeitüberschreitung; Teildaten übernommen)");
          else if (commandResult.truncated()) partialErrors.add(label + " (Ausgabe begrenzt; relevante Teildaten übernommen)");
        } else {
          long commandDuration = System.currentTimeMillis() - commandStarted;
          partialErrors.add(label + " (keine Daten, " + commandDuration + " ms)");
          log.debug("[ADB] Befehl '{}' für {} lieferte keine Daten nach {} ms - wird übersprungen, Inventarisierung läuft weiter",
            String.join(" ", cmd.subList(1, cmd.size())), shortSerial(serial), commandDuration);
        }
      } catch (Exception e) {
        long commandDuration = System.currentTimeMillis() - commandStarted;
        partialErrors.add(label + " (" + safeError(e) + ", " + commandDuration + " ms)");
        log.warn("[ADB] Teilabfrage '{}' für {} fehlgeschlagen - Inventarisierung läuft weiter: {}",
          label, shortSerial(serial), safeError(e));
      }
    }
    long duration = System.currentTimeMillis() - started;
    Map<String,String> parsed = AndroidInventoryParser.parse(raw);
    String status;
    if (parsed.isEmpty()) status = partialErrors.isEmpty() ? "Keine Daten erhalten" : "Keine Daten erhalten. Teilfehler: " + String.join("; ", partialErrors);
    else if (partialErrors.isEmpty()) status = "erfolgreich";
    else status = "teilweise erfolgreich (" + parsed.size() + " Merkmalsgruppen). Nicht verfügbare Teilabfragen: " + String.join("; ", partialErrors);
    log.info("[ADB] Inventarisierung für {} abgeschlossen: {} Merkmalsgruppen in {} ms erfasst{}",
      shortSerial(serial), parsed.size(), duration, partialErrors.isEmpty() ? "" : ", " + partialErrors.size() + " Teilfehler");
    return new InventoryResult(!parsed.isEmpty(), parsed, status, duration);
  }

  public record AndroidUser(int id, String name) {}
  public record UserPackageList(int userId, String userName, List<String> packages) {}
  public record AppPackageLists(List<UserPackageList> userPackageLists, List<String> systemPackages,
                                List<String> disabledPackages, List<String> enabledPackages) {
    public List<String> userPackages() {
      return userPackageLists.stream().flatMap(x -> x.packages().stream()).distinct().toList();
    }
  }

  /**
   * 40k34c: Liefert die Paketlisten über wenige, günstige Sammelabfragen
   * (`pm list packages -3/-s/-d/-e`) statt eines Prozesses je App - siehe
   * Performance-Anforderung im Auftrag.
   */
  public AppPackageLists listInstalledPackages(String serial) {
    var avail = findAdb();
    if (!avail.available() || serial == null || serial.isBlank()) return new AppPackageLists(List.of(), List.of(), List.of(), List.of());
    List<AndroidUser> users = listAndroidUsers(avail.path(), serial);
    if (users.isEmpty()) users = List.of(new AndroidUser(0, "Hauptbenutzer"));
    List<UserPackageList> userLists = new ArrayList<>();
    for (AndroidUser user : users) {
      List<String> packages = packageNames(avail.path(), serial, "-3", user.id());
      userLists.add(new UserPackageList(user.id(), user.name(), packages));
    }
    return new AppPackageLists(
      userLists,
      packageNames(avail.path(), serial, "-s", null),
      packageNames(avail.path(), serial, "-d", null),
      packageNames(avail.path(), serial, "-e", null));
  }

  private List<AndroidUser> listAndroidUsers(String adbPath, String serial) {
    String out = runShellCommand(List.of(adbPath, "-s", serial, "shell", "cmd", "user", "list"), 8, 20000);
    if (out == null || out.isBlank()) out = runShellCommand(List.of(adbPath, "-s", serial, "shell", "pm", "list", "users"), 8, 20000);
    if (out == null) return List.of();
    List<AndroidUser> result = new ArrayList<>();
    Matcher matcher = Pattern.compile("UserInfo\\{(\\d+):([^:}]+)").matcher(out);
    while (matcher.find()) {
      try { result.add(new AndroidUser(Integer.parseInt(matcher.group(1)), matcher.group(2).trim())); }
      catch (NumberFormatException ignored) {}
    }
    return result;
  }

  private List<String> packageNames(String adbPath, String serial, String flag, Integer userId) {
    List<String> args = new ArrayList<>(List.of(adbPath, "-s", serial, "shell", "pm", "list", "packages"));
    if (userId != null) args.addAll(List.of("--user", String.valueOf(userId)));
    args.add(flag);
    String out = runShellCommand(args, 10, 200000);
    if (out == null) return List.of();
    List<String> result = new ArrayList<>();
    for (String line : out.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("package:")) {
        String name = trimmed.substring("package:".length()).trim();
        if (isValidPackageName(name)) result.add(name);
      }
    }
    return result;
  }

  /**
   * 40k34c: EIN Sammelaufruf von "dumpsys package" statt eines Aufrufs je App -
   * die Ausgabe wird anschließend von AndroidAppInventoryParser gezielt geparst,
   * nicht vollständig/ungefiltert dauerhaft gespeichert.
   */
  public String packageDump(String serial) {
    return packageDumpDetailed(serial).output();
  }

  /**
   * 40k35a: Liest nur den Paketbereich von dumpsys. Der Reader speichert eine
   * begrenzte Nutzmenge, leert den Prozess-Puffer aber bis EOF weiter. Dadurch
   * kann ADB sauber enden, auch wenn die Gesamtausgabe größer als der Speicher-
   * ausschnitt ist.
   */
  public PackageDumpResult packageDumpDetailed(String serial) {
    var avail = findAdb();
    if (!avail.available() || serial == null || serial.isBlank())
      return new PackageDumpResult(null, false, false, "ADB oder Gerätekennung nicht verfügbar");
    List<String> args = List.of(avail.path(), "-s", serial, "shell", "dumpsys", "package", "packages");
    CommandResult result = runShellCommandDetailed(args, 30, 8_000_000);
    if ((result.output() == null || result.output().isBlank()) && !result.timedOut()) {
      // Kompatibilitätsfallback für Android-Versionen ohne den Teilbefehl "packages".
      args = List.of(avail.path(), "-s", serial, "shell", "dumpsys", "package");
      result = runShellCommandDetailed(args, 30, 8_000_000);
    }
    String warning = result.timedOut() ? "Paketdetails liefen in eine Zeitüberschreitung; gelesene Daten wurden übernommen."
      : result.truncated() ? "Paketdetails waren größer als 8 MB; der relevante Ausschnitt wurde ausgewertet."
      : result.error();
    return new PackageDumpResult(result.output(), result.timedOut(), result.truncated(), warning);
  }

  /** 40k34c: EIN günstiger Aufruf zur Ermittlung des aktuellen Standard-Launchers. */
  public String resolveDefaultLauncher(String serial) {
    var avail = findAdb();
    if (!avail.available() || serial == null || serial.isBlank()) return null;
    List<String> args = List.of(avail.path(), "-s", serial, "shell", "cmd", "package", "resolve-activity",
      "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME", "--brief");
    String out = runShellCommand(args, 8, 4000);
    return extractPackageFromResolve(out);
  }

  /** 40k34c: EIN günstiger Aufruf zur Ermittlung des aktuellen Standardbrowsers. */
  public String resolveDefaultBrowser(String serial) {
    var avail = findAdb();
    if (!avail.available() || serial == null || serial.isBlank()) return null;
    List<String> args = List.of(avail.path(), "-s", serial, "shell", "cmd", "package", "resolve-activity",
      "-a", "android.intent.action.VIEW", "-d", "http://example.org", "--brief");
    String out = runShellCommand(args, 8, 4000);
    return extractPackageFromResolve(out);
  }

  private static String extractPackageFromResolve(String out) {
    if (out == null || out.isBlank()) return null;
    for (String line : out.split("\n")) {
      String trimmed = line.trim();
      Matcher m = Pattern.compile("([a-zA-Z][a-zA-Z0-9_.]*)/[\\w.$]+").matcher(trimmed);
      if (m.find() && isValidPackageName(m.group(1))) return m.group(1);
    }
    return null;
  }

  /**
   * 40k34c: Validiert einen Paketnamen, BEVOR er in irgendeinen ADB-Aufruf
   * gelangt (z.B. für eine spätere gezielte "pm path"-Abfrage) - keine
   * ungeprüfte Übernahme von Fremdtext in einen Prozessaufruf.
   */
  public static boolean isValidPackageName(String name) {
    return name != null && name.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+");
  }

  private String runShellCommand(List<String> args, int timeoutSeconds) {
    return runShellCommand(args, timeoutSeconds, 20000);
  }

  private String runShellCommand(List<String> args, int timeoutSeconds, int maxChars) {
    return runShellCommandDetailed(args, timeoutSeconds, maxChars).output();
  }

  private CommandResult runShellCommandDetailed(List<String> args, int timeoutSeconds, int maxChars) {
    Process p = null;
    Thread stdoutReader = null;
    Thread stderrReader = null;
    try {
      ProcessBuilder pb = new ProcessBuilder(args);
      p = pb.start();
      AtomicReference<StreamRead> stdout = new AtomicReference<>(new StreamRead("", false));
      AtomicReference<StreamRead> stderr = new AtomicReference<>(new StreamRead("", false));
      Process process = p;
      stdoutReader = new Thread(() -> stdout.set(readStreamDrainingQuietly(process.getInputStream(), maxChars)), "gam-adb-stdout");
      stderrReader = new Thread(() -> stderr.set(readStreamDrainingQuietly(process.getErrorStream(), 12000)), "gam-adb-stderr");
      stdoutReader.setDaemon(true);
      stderrReader.setDaemon(true);
      stdoutReader.start();
      stderrReader.start();

      boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
      if (!finished) {
        p.destroy();
        if (!p.waitFor(500, TimeUnit.MILLISECONDS)) p.destroyForcibly();
      }
      joinQuietly(stdoutReader, 2500);
      joinQuietly(stderrReader, 2500);

      StreamRead outRead = stdout.get();
      StreamRead errRead = stderr.get();
      String out = outRead.text();
      int exitCode = finished ? p.exitValue() : -1;
      String error = errRead.text().replaceAll("\\s+", " ").trim();
      if (!finished) {
        log.debug("[ADB] Befehl lief nach {} Sekunden in den Timeout; bereits gelesene Teildaten: {} Zeichen", timeoutSeconds, out.length());
      } else if (exitCode != 0 && out.isBlank() && !error.isBlank()) {
        log.debug("[ADB] Befehl fehlgeschlagen: {}", error);
      }
      return new CommandResult(out.isBlank() ? null : out, !finished, outRead.truncated(), exitCode, error.isBlank() ? null : error);
    } catch (Exception e) {
      log.debug("[ADB] Prozessaufruf fehlgeschlagen: {}", safeError(e));
      return new CommandResult(null, false, false, -1, safeError(e));
    } finally {
      if (p != null && p.isAlive()) p.destroyForcibly();
    }
  }

  /**
   * Speichert höchstens maxChars, liest danach aber bis EOF weiter. Das ist der
   * zentrale 40k35a-Fix gegen den Deadlock großer ADB-Ausgaben.
   */
  private static StreamRead readStreamDrainingQuietly(InputStream in, int maxChars) {
    try { return readStreamDraining(in, maxChars); }
    catch (Exception e) { return new StreamRead("", false); }
  }

  private static StreamRead readStreamDraining(InputStream in, int maxChars) throws Exception {
    StringBuilder sb = new StringBuilder(Math.min(maxChars, 65536));
    byte[] buf = new byte[16384];
    int n;
    boolean truncated = false;
    while ((n = in.read(buf)) != -1) {
      if (sb.length() < maxChars) {
        String chunk = new String(buf, 0, n, StandardCharsets.UTF_8);
        int remaining = maxChars - sb.length();
        if (chunk.length() <= remaining) sb.append(chunk);
        else {
          sb.append(chunk, 0, remaining);
          truncated = true;
        }
      } else {
        truncated = true;
      }
    }
    return new StreamRead(sb.toString(), truncated);
  }

  private static void joinQuietly(Thread thread, long millis) {
    if (thread == null) return;
    try { thread.join(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }

  private static String safeError(Throwable error) {
    if (error == null) return "unbekannter Fehler";
    String message = error.getMessage();
    return message == null || message.isBlank() ? error.getClass().getSimpleName() : message.replaceAll("\\s+", " ").trim();
  }

  private static String shortSerial(String serial) {
    if (serial == null) return "unbekannt";
    return serial.length() > 8 ? serial.substring(0, 4) + "…" + serial.substring(serial.length() - 4) : serial;
  }

  private static String readStream(InputStream in) throws Exception {
    return readStream(in, 40000);
  }

  private static String readStream(InputStream in, int maxChars) throws Exception {
    StringBuilder sb = new StringBuilder();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) != -1) {
      sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
      if (sb.length() > maxChars) break; // Schutz vor sehr großen, ungefilterten Ausgaben
    }
    return sb.toString();
  }

  private static boolean validHost(String host) {
    if (host == null || host.isBlank()) return false;
    return host.matches("(?:\\d{1,3}\\.){3}\\d{1,3}") || host.matches("[a-zA-Z0-9.-]+");
  }

  private static boolean validPort(int port) { return port > 0 && port <= 65535; }

  private String setting(String property, String variable, String fallback) {
    String v = env.getProperty(property);
    if (v == null || v.isBlank()) v = System.getenv(variable);
    return v == null || v.isBlank() ? fallback : v.trim();
  }
}
