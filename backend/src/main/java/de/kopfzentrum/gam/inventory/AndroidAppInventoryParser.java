package de.kopfzentrum.gam.inventory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 40k34c: Reine Textauswertung der EINEN gesammelten "dumpsys package"-Ausgabe
 * (siehe AndroidAdbService.packageDump - ein Sammelaufruf für alle Pakete,
 * kein Prozess je App). Es wird gezielt geparst, die vollständige Rohausgabe
 * wird nirgends dauerhaft gespeichert.
 */
final class AndroidAppInventoryParser {
  private AndroidAppInventoryParser() {}

  record AppRecord(
    String packageName, String versionName, Long versionCode, Long firstInstallTime, Long lastUpdateTime,
    String installerPackage, boolean system, boolean updatedSystem, boolean enabled, boolean debuggable,
    boolean testOnly, Integer minSdk, Integer targetSdk, Long uid) {}

  private static final Pattern PACKAGE_BLOCK = Pattern.compile(
    "(?m)^\\s{2}Package \\[([\\w.]+)]");

  /**
   * Zerlegt die Sammel-Ausgabe in einzelne Paketblöcke (jeweils beginnend mit
   * "Package [name]") und wertet aus jedem Block nur die benötigten,
   * bekannten Zeilen aus.
   */
  static Map<String,AppRecord> parse(String dump) {
    Map<String,AppRecord> result = new LinkedHashMap<>();
    if (dump == null || dump.isBlank()) return result;
    Matcher starts = PACKAGE_BLOCK.matcher(dump);
    List<int[]> blocks = new ArrayList<>();
    List<String> names = new ArrayList<>();
    while (starts.find()) { blocks.add(new int[]{starts.start()}); names.add(starts.group(1)); }
    for (int i = 0; i < blocks.size(); i++) {
      int from = blocks.get(i)[0];
      int to = (i + 1 < blocks.size()) ? blocks.get(i + 1)[0] : dump.length();
      String block = dump.substring(from, Math.min(to, from + 8000)); // je Paketblock begrenzt
      String name = names.get(i);
      if (!AndroidAdbService.isValidPackageName(name)) continue;
      result.put(name, parseBlock(name, block));
    }
    return result;
  }

  private static AppRecord parseBlock(String packageName, String block) {
    String versionName = find(block, "versionName=([^\\s]+)");
    Long versionCode = findLong(block, "versionCode=(\\d+)");
    Long longVersionCode = findLong(block, "longVersionCode=(\\d+)");
    Long firstInstall = findLong(block, "firstInstallTime=([-\\d]+)");
    Long lastUpdate = findLong(block, "lastUpdateTime=([-\\d]+)");
    String installer = find(block, "installerPackageName=([\\w.]+)");
    boolean enabled = !block.contains("enabled=false");
    String flags = find(block, "flags=\\[([^]]*)]") ;
    String pkgFlags = find(block, "pkgFlags=\\[([^]]*)]");
    String allFlags = (flags == null ? "" : flags) + " " + (pkgFlags == null ? "" : pkgFlags);
    boolean system = allFlags.contains("SYSTEM");
    boolean updatedSystem = allFlags.contains("UPDATED_SYSTEM_APP");
    boolean debuggable = allFlags.contains("DEBUGGABLE");
    boolean testOnly = allFlags.contains("TEST_ONLY");
    Long uid = findLong(block, "userId=(\\d+)");
    if (uid == null) uid = findLong(block, "appId=(\\d+)");
    Integer minSdk = findInt(block, "minSdk=(\\d+)");
    Integer targetSdk = findInt(block, "targetSdk=(\\d+)");
    return new AppRecord(packageName, versionName, longVersionCode != null ? longVersionCode : versionCode,
      plausibleTime(firstInstall), plausibleTime(lastUpdate), installer, system, updatedSystem, enabled,
      debuggable, testOnly, minSdk, targetSdk, uid);
  }

  /** 40k34c: ungültige/leere Zeitwerte (z.B. 0) werden NICHT als 01.01.1970 ausgegeben. */
  private static Long plausibleTime(Long epochMs) {
    return (epochMs == null || epochMs <= 0) ? null : epochMs;
  }

  private static String find(String text, String regex) {
    Matcher m = Pattern.compile(regex).matcher(text);
    return m.find() ? m.group(1) : null;
  }
  private static Long findLong(String text, String regex) {
    String v = find(text, regex);
    try { return v == null ? null : Long.parseLong(v); } catch (NumberFormatException e) { return null; }
  }
  private static Integer findInt(String text, String regex) {
    String v = find(text, regex);
    try { return v == null ? null : Integer.parseInt(v); } catch (NumberFormatException e) { return null; }
  }
}
