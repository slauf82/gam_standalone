package de.kopfzentrum.gam.inventory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 40k34b: Reine Textauswertung der über ADB Shell abgerufenen, bereits
 * vorhandenen Rohausgaben (getprop/uname/meminfo/df/wm/dumpsys battery/
 * uptime/getenforce/ip addr). Kein Netzwerkzugriff, keine Prozessausführung -
 * das übernimmt ausschließlich AndroidAdbService.
 */
final class AndroidInventoryParser {
  private AndroidInventoryParser() {}

  static Map<String,String> parse(Map<String,String> raw) {
    Map<String,String> out = new LinkedHashMap<>();
    Map<String,String> props = parseGetprop(raw.get("GETPROP"));

    putIfPresent(out, "Hersteller", props.get("ro.product.manufacturer"));
    putIfPresent(out, "Marke", props.get("ro.product.brand"));
    putIfPresent(out, "Modell", props.get("ro.product.model"));
    putIfPresent(out, "Produktname", props.get("ro.product.name"));
    String deviceName = firstNonBlank(props.get("net.hostname"), props.get("ro.product.model"));
    putIfPresent(out, "Gerätename", deviceName);
    putIfPresent(out, "Gerätecodename", props.get("ro.product.device"));
    putIfPresent(out, "Hardwarebezeichnung", props.get("ro.hardware"));
    putIfPresent(out, "Seriennummer", props.get("ro.serialno"));

    putIfPresent(out, "Android-Version", props.get("ro.build.version.release"));
    putIfPresent(out, "API-Level", props.get("ro.build.version.sdk"));
    putIfPresent(out, "Buildnummer", props.get("ro.build.display.id"));
    putIfPresent(out, "Build-ID", props.get("ro.build.id"));
    putIfPresent(out, "Build-Fingerprint", props.get("ro.build.fingerprint"));
    putIfPresent(out, "Sicherheits-Patch-Level", props.get("ro.build.version.security_patch"));
    putIfPresent(out, "Build-Typ", props.get("ro.build.type"));
    putIfPresent(out, "Build-Tags", props.get("ro.build.tags"));

    putIfPresent(out, "CPU-Architektur", props.get("ro.product.cpu.abi"));
    putIfPresent(out, "Unterstützte ABIs", props.get("ro.product.cpu.abilist"));
    putIfPresent(out, "Hardwareplattform", props.get("ro.board.platform"));
    putIfPresent(out, "SoC-Bezeichnung", firstNonBlank(props.get("ro.soc.model"), props.get("ro.hardware.chipname")));

    String uname = raw.get("UNAME");
    if (uname != null && !uname.isBlank()) {
      String[] tokens = uname.trim().split("\\s+");
      if (tokens.length >= 3) putIfPresent(out, "Kernel-Version", tokens[2]);
      if (tokens.length >= 1) putIfPresent(out, "Kernel-Architektur", tokens[tokens.length - 1]);
    }

    parseMeminfo(raw.get("MEMINFO"), out);
    parseDisks(raw.get("DISKS"), out);

    String size = raw.get("DISPLAYSIZE");
    if (size != null) {
      Matcher m = Pattern.compile("(\\d+x\\d+)").matcher(size);
      if (m.find()) putIfPresent(out, "Display-Auflösung", m.group(1));
    }
    String density = raw.get("DISPLAYDENSITY");
    if (density != null) {
      Matcher m = Pattern.compile("(\\d+)").matcher(density);
      if (m.find()) putIfPresent(out, "Display-Dichte", m.group(1) + " dpi");
    }

    parseBattery(raw.get("BATTERY"), out);
    putIfPresent(out, "Uptime", clean(raw.get("UPTIME")));
    putIfPresent(out, "SELinux-Status", clean(raw.get("SELINUX")));
    putIfPresent(out, "Zeitzone", props.get("persist.sys.timezone"));
    putIfPresent(out, "Sprache/Locale", firstNonBlank(props.get("persist.sys.locale"),
      joinNonBlank(props.get("persist.sys.language"), props.get("persist.sys.country"))));
    putIfPresent(out, "Geräteverschlüsselungsstatus", props.get("ro.crypto.state"));
    String debuggable = props.get("ro.debuggable");
    if (debuggable != null) putIfPresent(out, "Debug-/Entwicklerstatus", "1".equals(debuggable.trim()) ? "aktiviert" : "deaktiviert");

    String network = raw.get("NETWORK");
    if (network != null) {
      Matcher m = Pattern.compile("inet\\s+(\\d{1,3}(?:\\.\\d{1,3}){3})").matcher(network);
      if (m.find()) putIfPresent(out, "IP-Adresse", m.group(1));
    }
    putIfPresent(out, "DNS-Server", firstNonBlank(props.get("net.dns1"), props.get("net.rmnet0.dns1")));
    putIfPresent(out, "Mobilfunk vorhanden", props.get("gsm.version.baseband") != null ? "ja" : null);

    // 40k34d: Laufzeitzustand - additive Erweiterung derselben Auswertungsfunktion,
    // dieselbe "· Label: Value"-Konvention, keine zweite Inventarisierung.
    parseProcesses(raw.get("PROCESSES"), out);
    parseServices(raw.get("SERVICES"), out);
    parseUsers(raw.get("USERS"), out);
    parsePower(raw.get("POWER"), out);
    parseCpu(raw.get("CPU_ONLINE"), raw.get("LOADAVG"), out);
    putIfPresent(out, "Flugmodus", boolSetting(raw.get("AIRPLANE_MODE")));
    putIfPresent(out, "WLAN aktiv", boolSetting(raw.get("WIFI_ON")));
    putIfPresent(out, "Bluetooth aktiv", boolSetting(raw.get("BLUETOOTH_ON")));
    parseRoles(raw, out);
    parseDevicePolicy(raw.get("DEVICE_POLICY"), out);
    parseConnectivityVpn(raw.get("CONNECTIVITY"), out);

    return out;
  }

  /** 40k34d: Anzahl laufender Prozesse aus "ps -A" - keine Rohausgabe wird gespeichert. */
  private static void parseProcesses(String raw, Map<String,String> out) {
    if (raw == null || raw.isBlank()) return;
    String[] lines = raw.trim().split("\n");
    int count = Math.max(0, lines.length - 1); // erste Zeile ist die Kopfzeile
    if (count > 0) putIfPresent(out, "Laufende Prozesse", String.valueOf(count));
  }

  /**
   * 40k34d: Anzahl aktiver Dienste aus "dumpsys activity services" - es werden nur
   * die Anzahl und bis zu 10 Beispielpaketnamen übernommen, nicht der vollständige Dump.
   */
  private static void parseServices(String raw, Map<String,String> out) {
    if (raw == null || raw.isBlank()) return;
    Matcher m = Pattern.compile("(?m)^\\s*\\*\\s*ServiceRecord\\{[^}]*\\s([\\w.]+)/").matcher(raw);
    java.util.LinkedHashSet<String> packages = new java.util.LinkedHashSet<>();
    while (m.find() && packages.size() < 200) packages.add(m.group(1));
    if (!packages.isEmpty()) {
      putIfPresent(out, "Laufende Dienste (Anzahl)", String.valueOf(packages.size()));
      putIfPresent(out, "Laufende Dienste (Beispiele)", String.join(", ", packages.stream().limit(10).toList()));
    }
  }

  /** 40k34d: Benutzer aus "pm list users" - z.B. "UserInfo{0:Owner:...}". */
  private static void parseUsers(String raw, Map<String,String> out) {
    if (raw == null || raw.isBlank()) return;
    Matcher m = Pattern.compile("UserInfo\\{(\\d+):([^:]*):").matcher(raw);
    java.util.List<String> users = new java.util.ArrayList<>();
    while (m.find()) users.add(m.group(1) + ":" + m.group(2));
    if (!users.isEmpty()) {
      putIfPresent(out, "Benutzeranzahl", String.valueOf(users.size()));
      putIfPresent(out, "Benutzer", String.join(", ", users));
    }
  }

  /** 40k34d: Bildschirm/Doze/Battery-Saver aus "dumpsys power". */
  private static void parsePower(String raw, Map<String,String> out) {
    if (raw == null || raw.isBlank()) return;
    Boolean screenOn = boolFlag(raw, "mWakefulness=Awake", "mWakefulness=Asleep");
    if (screenOn != null) putIfPresent(out, "Bildschirm an", screenOn ? "ja" : "nein");
    Matcher doze = Pattern.compile("mDeviceIdleMode=(true|false)").matcher(raw);
    if (doze.find()) putIfPresent(out, "Doze Mode", "true".equals(doze.group(1)) ? "aktiv" : "inaktiv");
    Matcher saver = Pattern.compile("mSettingBatterySaverEnabled=(true|false)").matcher(raw);
    if (saver.find()) putIfPresent(out, "Battery Saver", "true".equals(saver.group(1)) ? "aktiv" : "inaktiv");
  }

  private static Boolean boolFlag(String raw, String truePattern, String falsePattern) {
    if (raw.contains(truePattern)) return true;
    if (raw.contains(falsePattern)) return false;
    return null;
  }

  /** 40k34d: CPU-Kerne/Last aus /sys/.../cpu/online und /proc/loadavg - keine Dauerabfragen. */
  private static void parseCpu(String cpuOnline, String loadavg, Map<String,String> out) {
    if (cpuOnline != null && !cpuOnline.isBlank()) {
      String range = cpuOnline.trim();
      Matcher m = Pattern.compile("(\\d+)-(\\d+)").matcher(range);
      if (m.matches()) putIfPresent(out, "CPU-Kerne (online)", String.valueOf(Integer.parseInt(m.group(2)) - Integer.parseInt(m.group(1)) + 1));
      else putIfPresent(out, "CPU-Kerne (online)", String.valueOf(range.split(",").length));
    }
    if (loadavg != null && !loadavg.isBlank()) {
      String[] parts = loadavg.trim().split("\\s+");
      if (parts.length >= 3) putIfPresent(out, "CPU-Last (1/5/15 min)", parts[0] + " / " + parts[1] + " / " + parts[2]);
    }
  }

  private static String boolSetting(String raw) {
    if (raw == null) return null;
    String v = raw.trim();
    if (v.equals("1")) return "ja";
    if (v.equals("0")) return "nein";
    return null; // "null"/leer/nicht unterstützt - keine Vermutung
  }

  /**
   * 40k34d: Rollen ohne Root, ausschließlich aus eindeutigen Systemantworten - keine
   * Vermutungen. "cmd role holders" liefert bei Nichtunterstützung/-zuweisung leere
   * Ausgabe, die dann korrekt nicht übernommen wird.
   */
  private static void parseRoles(Map<String,String> raw, Map<String,String> out) {
    putIfPresent(out, "Standard-IME", clean(raw.get("DEFAULT_IME")));
    String accessibility = clean(raw.get("ACCESSIBILITY_SERVICES"));
    if (accessibility != null && !"null".equalsIgnoreCase(accessibility)) putIfPresent(out, "Accessibility Services", accessibility);
    String notificationListeners = clean(raw.get("NOTIFICATION_LISTENERS"));
    if (notificationListeners != null && !"null".equalsIgnoreCase(notificationListeners)) putIfPresent(out, "Notification Listener", notificationListeners);
    String autofill = clean(raw.get("AUTOFILL_SERVICE"));
    if (autofill != null && !"null".equalsIgnoreCase(autofill)) putIfPresent(out, "Autofill Service", autofill);
    putIfPresent(out, "Standard-Telefon-App", clean(raw.get("ROLE_DIALER")));
    putIfPresent(out, "Standard-SMS-App", clean(raw.get("ROLE_SMS")));
    putIfPresent(out, "Standard-Assistent", clean(raw.get("ROLE_ASSISTANT")));
  }

  /** 40k34d: Device/Profile Owner aus "dumpsys device_policy" - nur falls eindeutig genannt. */
  private static void parseDevicePolicy(String raw, Map<String,String> out) {
    if (raw == null || raw.isBlank()) return;
    Matcher owner = Pattern.compile("Device Owner:.*?admin=ComponentInfo\\{([\\w.]+)/").matcher(raw);
    if (owner.find()) putIfPresent(out, "Device Owner", owner.group(1));
    Matcher profileOwner = Pattern.compile("Profile Owner \\(User \\d+\\):.*?admin=ComponentInfo\\{([\\w.]+)/").matcher(raw);
    if (profileOwner.find()) putIfPresent(out, "Profile Owner", profileOwner.group(1));
  }

  /**
   * 40k34d: VPN-Hinweis aus "dumpsys connectivity" - bewusst als Best-Effort-Hinweis
   * dokumentiert (siehe Dokumentation), da ohne Root nicht auf allen Android-Versionen
   * zuverlässig auswertbar.
   */
  private static void parseConnectivityVpn(String raw, Map<String,String> out) {
    if (raw == null || raw.isBlank()) return;
    if (Pattern.compile("(?i)VPN\\b.*\\bconnected", Pattern.DOTALL).matcher(raw).find()
      || Pattern.compile("Type: VPN").matcher(raw).find()) {
      putIfPresent(out, "VPN aktiv", "ja (Hinweis, ohne Garantie auf Vollständigkeit)");
    }
  }

  private static Map<String,String> parseGetprop(String raw) {
    Map<String,String> props = new LinkedHashMap<>();
    if (raw == null) return props;
    // Zeilenformat: [key]: [value]
    Pattern line = Pattern.compile("^\\[(.+?)\\]:\\s*\\[(.*)\\]$");
    for (String l : raw.split("\n")) {
      Matcher m = line.matcher(l.trim());
      if (m.matches()) props.put(m.group(1), m.group(2));
    }
    return props;
  }

  private static void parseMeminfo(String raw, Map<String,String> out) {
    if (raw == null) return;
    Long total = extractKb(raw, "MemTotal");
    Long available = extractKb(raw, "MemAvailable");
    if (total != null) putIfPresent(out, "Arbeitsspeicher gesamt", formatKb(total));
    if (available != null) putIfPresent(out, "Arbeitsspeicher verfügbar", formatKb(available));
  }

  private static Long extractKb(String raw, String key) {
    Matcher m = Pattern.compile(key + ":\\s*(\\d+)\\s*kB").matcher(raw);
    return m.find() ? Long.parseLong(m.group(1)) : null;
  }

  private static String formatKb(long kb) {
    double gb = kb / 1024.0 / 1024.0;
    return gb >= 0.9 ? String.format(Locale.ROOT, "%.1f GiB", gb) : String.format(Locale.ROOT, "%d MiB", kb / 1024);
  }

  private static void parseDisks(String raw, Map<String,String> out) {
    if (raw == null) return;
    for (String l : raw.split("\n")) {
      String[] cols = l.trim().split("\\s+");
      // Android df: Filesystem  1K-blocks  Used  Available  Use%  Mounted on
      if (cols.length >= 6 && "/data".equals(cols[cols.length - 1])) {
        try {
          long usedKb = Long.parseLong(cols[cols.length - 4]);
          long availKb = Long.parseLong(cols[cols.length - 3]);
          putIfPresent(out, "Speicher belegt (/data)", formatKb(usedKb));
          putIfPresent(out, "Speicher verfügbar (/data)", formatKb(availKb));
          putIfPresent(out, "Speicher gesamt (/data)", formatKb(usedKb + availKb));
        } catch (NumberFormatException ignored) { /* herstellerspezifisches df-Format - wird übersprungen */ }
      }
    }
  }

  private static void parseBattery(String raw, Map<String,String> out) {
    if (raw == null) return;
    Integer level = extractInt(raw, "level");
    if (level != null) putIfPresent(out, "Akku-Ladezustand", level + " %");
    Integer status = extractInt(raw, "status");
    if (status != null) putIfPresent(out, "Akkustatus", batteryStatus(status));
    Integer plugged = extractInt(raw, "plugged");
    if (plugged != null) putIfPresent(out, "Ladeart", chargingType(plugged));
    Integer health = extractInt(raw, "health");
    if (health != null) putIfPresent(out, "Gesundheitsstatus", batteryHealth(health));
    Integer temperature = extractInt(raw, "temperature");
    if (temperature != null) putIfPresent(out, "Akkutemperatur", String.format(Locale.ROOT, "%.1f °C", temperature / 10.0));
    Integer voltage = extractInt(raw, "voltage");
    if (voltage != null) putIfPresent(out, "Akkuspannung", voltage + " mV");
  }

  private static Integer extractInt(String raw, String key) {
    Matcher m = Pattern.compile("(?m)^\\s*" + key + ":\\s*(-?\\d+)").matcher(raw);
    return m.find() ? Integer.parseInt(m.group(1)) : null;
  }

  private static String batteryStatus(int status) {
    return switch (status) { case 1 -> "unbekannt"; case 2 -> "wird geladen"; case 3 -> "entlädt sich"; case 4 -> "nicht geladen"; case 5 -> "voll"; default -> "Code " + status; };
  }
  private static String batteryHealth(int health) {
    return switch (health) { case 1 -> "unbekannt"; case 2 -> "gut"; case 3 -> "überhitzt"; case 4 -> "defekt"; case 5 -> "Überspannung"; case 6 -> "unbekannter Fehler"; case 7 -> "kühl"; default -> "Code " + health; };
  }
  private static String chargingType(int plugged) {
    return switch (plugged) { case 0 -> "nicht angeschlossen"; case 1 -> "Netzteil"; case 2 -> "USB"; case 4 -> "kabellos"; default -> "Code " + plugged; };
  }

  private static void putIfPresent(Map<String,String> out, String label, String value) {
    String v = clean(value);
    if (v != null && !v.isBlank() && !v.equals("unknown")) out.put(label, v);
  }
  private static String clean(String v) { return v == null ? null : v.replaceAll("[\\r\\n]+", " ").trim(); }
  private static String firstNonBlank(String... values) { for (String v : values) if (v != null && !v.isBlank()) return v.trim(); return null; }
  private static String joinNonBlank(String a, String b) {
    if ((a == null || a.isBlank()) && (b == null || b.isBlank())) return null;
    return (a == null ? "" : a) + (b == null || b.isBlank() ? "" : "_" + b);
  }
}
