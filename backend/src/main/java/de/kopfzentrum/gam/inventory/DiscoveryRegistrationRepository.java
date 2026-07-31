package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Repository
public class DiscoveryRegistrationRepository {
  private final JdbcTemplate jdbc;
  public DiscoveryRegistrationRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

  @PostConstruct void init(){
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_registered_devices (
        identity_key VARCHAR(255) NOT NULL,
        name VARCHAR(255) NULL,
        device_type VARCHAR(255) NULL,
        address VARCHAR(255) NULL,
        hardware_address VARCHAR(100) NULL,
        serial_number VARCHAR(255) NULL,
        manufacturer VARCHAR(255) NULL,
        discovery_protocol LONGTEXT NULL,
        device_status VARCHAR(100) NULL,
        first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        detection_count BIGINT NOT NULL DEFAULT 0,
        last_scan_hits INT NOT NULL DEFAULT 0,
        manual_device_type BOOLEAN NOT NULL DEFAULT FALSE,
        PRIMARY KEY(identity_key)
      )
      """);
    // Upgrade bestehender 40k30e-Installationen ohne separate Migration.
    addColumn("device_type", "VARCHAR(255) NULL");
    addColumn("manufacturer", "VARCHAR(255) NULL");
    addColumn("discovery_protocol", "LONGTEXT NULL");
    upgradeProtocolColumn();
    addColumn("device_status", "VARCHAR(100) NULL");
    addColumn("first_seen_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
    addColumn("last_seen_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
    addColumn("detection_count", "BIGINT NOT NULL DEFAULT 0");
    addColumn("last_scan_hits", "INT NOT NULL DEFAULT 0");
    addColumn("manual_device_type", "BOOLEAN NOT NULL DEFAULT FALSE");
    // 40k33b4: manueller Namensschutz, analog zur bereits bestehenden manual_device_type-Sperre.
    addColumn("manual_name", "BOOLEAN NOT NULL DEFAULT FALSE");
    // 40k34b: ADB-Wiederverbindungsdaten - rein additiv, keine Pairing-Codes, keine
    // temporären Tokens. Nur Verbindungsadresse/-port und letzter Erfolgszeitpunkt.
    addColumn("adb_host", "VARCHAR(255) NULL");
    addColumn("adb_port", "INT NULL");
    addColumn("adb_last_connected_at", "TIMESTAMP NULL");
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_device_type_history (
        id BIGINT NOT NULL AUTO_INCREMENT,
        identity_key VARCHAR(255) NOT NULL,
        old_device_type VARCHAR(255) NULL,
        new_device_type VARCHAR(255) NOT NULL,
        changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        changed_by VARCHAR(255) NULL,
        PRIMARY KEY(id),
        INDEX idx_gam_discovery_type_history_identity(identity_key)
      )
      """);
    // 40k33b4: kleine, migrationssichere Alias-Tabelle. Namen zusammengeführter oder
    // umbenannter Geräte gehen nicht verloren, sondern bleiben als Alias erhalten, damit
    // ein künftiger Treffer mit diesem Namen wieder derselben Identität zugeordnet wird.
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_device_aliases (
        identity_key VARCHAR(255) NOT NULL,
        alias_name VARCHAR(255) NOT NULL,
        alias_semantic VARCHAR(255) NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(identity_key, alias_name),
        INDEX idx_gam_discovery_device_aliases_semantic(alias_semantic)
      )
      """);
  }

  private void addColumn(String name, String definition){
    Integer count = jdbc.queryForObject("""
      SELECT COUNT(*)
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'gam_discovery_registered_devices'
        AND column_name = ?
      """, Integer.class, name);
    if (count != null && count == 0) {
      jdbc.execute("ALTER TABLE gam_discovery_registered_devices ADD COLUMN `" + name + "` " + definition);
    }
  }

  private void upgradeProtocolColumn(){
    String dataType = jdbc.queryForObject("""
      SELECT DATA_TYPE
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'gam_discovery_registered_devices'
        AND column_name = 'discovery_protocol'
      """, String.class);
    if (dataType != null && !"longtext".equalsIgnoreCase(dataType)) {
      jdbc.execute("ALTER TABLE gam_discovery_registered_devices MODIFY COLUMN discovery_protocol LONGTEXT NULL");
    }
  }

  public List<Map<String,Object>> findAll(){
    return jdbc.queryForList("""
      SELECT identity_key AS identityKey, name, device_type AS deviceType, address,
             hardware_address AS hardwareAddress, serial_number AS serialNumber,
             manufacturer, discovery_protocol AS protocol, device_status AS status,
             first_seen_at AS firstSeenAt, last_seen_at AS lastSeenAt, registered_at AS registeredAt,
             detection_count AS detectionCount, last_scan_hits AS lastScanHits,
             manual_device_type AS manualDeviceType, manual_name AS manualName,
             adb_host AS adbHost, adb_port AS adbPort, adb_last_connected_at AS adbLastConnectedAt
      FROM gam_discovery_registered_devices
      ORDER BY registered_at DESC, name ASC
      """);
  }

  public Map<String,Object> find(String identityKey){
    List<Map<String,Object>> rows=jdbc.queryForList("""
      SELECT identity_key AS identityKey, name, device_type AS deviceType, address,
             hardware_address AS hardwareAddress, serial_number AS serialNumber,
             manufacturer, discovery_protocol AS protocol, device_status AS status,
             first_seen_at AS firstSeenAt, last_seen_at AS lastSeenAt, registered_at AS registeredAt,
             detection_count AS detectionCount, last_scan_hits AS lastScanHits,
             manual_device_type AS manualDeviceType, manual_name AS manualName,
             adb_host AS adbHost, adb_port AS adbPort, adb_last_connected_at AS adbLastConnectedAt
      FROM gam_discovery_registered_devices WHERE identity_key=?
      """, identityKey);
    if(rows.isEmpty()) throw new IllegalArgumentException("Registriertes Gerät wurde nicht gefunden.");
    return rows.get(0);
  }


  public Map<String,Object> updateDeviceType(String identityKey, String newType, String changedBy){
    String type=canonicalDeviceType(newType);
    if(type==null) throw new IllegalArgumentException("Gerätetyp fehlt.");
    Map<String,Object> current=find(identityKey);
    Object oldValue=current.get("deviceType");
    String oldType=oldValue==null?null:clean(String.valueOf(oldValue));
    jdbc.update("UPDATE gam_discovery_registered_devices SET device_type=?, manual_device_type=TRUE WHERE identity_key=?", type, identityKey);
    jdbc.update("INSERT INTO gam_discovery_device_type_history(identity_key,old_device_type,new_device_type,changed_by) VALUES (?,?,?,?)", identityKey, oldType, type, clean(changedBy));
    return find(identityKey);
  }

  /**
   * 40k34h: Automatische, evidenzbasierte Nachklassifizierung. Im Unterschied zu
   * updateDeviceType() (manuelle Benutzeraktion) wird HIER bewusst KEIN
   * manual_device_type gesetzt - eine spätere, noch spezifischere automatische
   * Einstufung (oder eine echte manuelle Korrektur durch den Benutzer) bleibt
   * weiterhin möglich. Eine bereits bestehende manuelle Zuordnung wird niemals
   * überschrieben (WHERE manual_device_type=FALSE). Nutzt dieselbe
   * Änderungshistorie wie die manuelle Umkategorisierung - keine zweite Tabelle.
   */
  public boolean applyEvidenceReclassification(String identityKey, String newType, String reasonSummary){
    String type=canonicalDeviceType(newType);
    if(type==null) return false;
    Map<String,Object> current=find(identityKey);
    Object oldValue=current.get("deviceType");
    String oldType=oldValue==null?null:clean(String.valueOf(oldValue));
    if(type.equals(oldType)) return false;
    // 40k34i: generische Korrektur (gilt für alle Plattformen, nicht nur Android) - eine
    // automatische Nachklassifizierung darf eine bereits spezifischere Kategorie niemals
    // durch eine weniger spezifische ersetzen. Wiederverwendet dieselbe Spezifitäts-
    // Rangfolge, die auch bei der Discovery-Konsolidierung bereits verwendet wird.
    if(categorySpecificity(type) < categorySpecificity(oldType)) return false;
    int updated=jdbc.update(
      "UPDATE gam_discovery_registered_devices SET device_type=? WHERE identity_key=? AND manual_device_type=FALSE",
      type, identityKey);
    if(updated==0) return false; // manuell gesperrt oder Identität nicht (mehr) vorhanden - keine Überschreibung
    jdbc.update("INSERT INTO gam_discovery_device_type_history(identity_key,old_device_type,new_device_type,changed_by) VALUES (?,?,?,?)",
      identityKey, oldType, type, "Automatische Nachklassifizierung (Evidence): " + clean(reasonSummary));
    return true;
  }

  public Map<String,Object> updateDeviceName(String identityKey, String newName){
    String name=clean(newName);
    if(name==null) throw new IllegalArgumentException("Gerätename fehlt.");
    Map<String,Object> current=find(identityKey);
    String oldName=clean((String)current.get("name"));
    if(oldName!=null && !oldName.equalsIgnoreCase(name)) addAlias(identityKey, oldName);
    jdbc.update("UPDATE gam_discovery_registered_devices SET name=?, manual_name=TRUE WHERE identity_key=?", name, identityKey);
    return find(identityKey);
  }

  public boolean isRegistered(String ip,String mac,String serial,String name){
    for(String key: keys(ip,mac,serial,name)){
      Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM gam_discovery_registered_devices WHERE identity_key=?",Integer.class,key);
      if(count!=null&&count>0)return true;
    }
    return false;
  }

  /**
   * 40k33b3: Liefert true, wenn für diese Identität bereits eine manuell gesetzte
   * Gerätekategorie existiert. Ändert nichts an der Datenbank; dient ausschließlich
   * dazu, in nachgelagerten Prüfungen (z.B. der Linux-Nachprüfung) nachvollziehbar
   * protokollieren zu können, warum eine automatische Kategorie unangetastet bleibt.
   * Die eigentliche Schreibsperre für manuelle Kategorien bleibt unverändert in
   * register()/recordDiscoveryHit() über die vorhandene SQL-CASE-Bedingung bestehen.
   */
  public boolean hasManualDeviceType(String ip,String mac,String serial,String name){
    for(String key: keys(ip,mac,serial,name)){
      List<Boolean> rows=jdbc.query(
        "SELECT manual_device_type FROM gam_discovery_registered_devices WHERE identity_key=?",
        (rs,rowNum)->rs.getBoolean(1), key);
      if(!rows.isEmpty()) return Boolean.TRUE.equals(rows.get(0));
    }
    return false;
  }

  public int register(String ip,String mac,String serial,String name){
    return register(ip,mac,serial,name,null,null,null,null);
  }

  public int register(String ip,String mac,String serial,String name,String type,String manufacturer,String protocol,String status){
    String key=primaryKey(ip,mac,serial,name);
    return jdbc.update("""
      INSERT INTO gam_discovery_registered_devices
        (identity_key,name,device_type,address,hardware_address,serial_number,manufacturer,discovery_protocol,device_status,first_seen_at,last_seen_at)
      VALUES (?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
      ON DUPLICATE KEY UPDATE
        name=CASE WHEN manual_name THEN name ELSE COALESCE(VALUES(name),name) END,
        device_type=CASE WHEN manual_device_type THEN device_type ELSE COALESCE(VALUES(device_type),device_type) END,
        address=COALESCE(VALUES(address),address), hardware_address=COALESCE(VALUES(hardware_address),hardware_address),
        serial_number=COALESCE(VALUES(serial_number),serial_number), manufacturer=COALESCE(VALUES(manufacturer),manufacturer),
        discovery_protocol=COALESCE(VALUES(discovery_protocol),discovery_protocol), device_status=COALESCE(VALUES(device_status),device_status),
        last_seen_at=CURRENT_TIMESTAMP
      """,key,clean(name),canonicalDeviceType(type),clean(ip),clean(mac),clean(serial),clean(manufacturer),clean(protocol),clean(status));
  }


  /** Resets only the per-run counter. Registrations and cumulative counters remain untouched. */
  public void beginDiscoveryRun(){
    consolidateSemanticDuplicates();
    jdbc.update("UPDATE gam_discovery_registered_devices SET last_scan_hits=0");
  }

  /**
   * Persists one raw discovery hit. Multiple protocols may report the same physical device;
   * therefore every hit increments the counters while the identity remains unique.
   */
  public void recordDiscoveryHit(String ip,String mac,String serial,String name,String type,String manufacturer,String protocol,String status){
    String existingKey=findExistingKey(ip,mac,serial,name);
    if(existingKey!=null){
      jdbc.update("""
        UPDATE gam_discovery_registered_devices SET
          name=CASE WHEN manual_name THEN name ELSE COALESCE(?,name) END,
          device_type=CASE WHEN manual_device_type THEN device_type ELSE COALESCE(?,device_type) END, address=COALESCE(?,address),
          hardware_address=COALESCE(?,hardware_address), serial_number=COALESCE(?,serial_number),
          manufacturer=COALESCE(?,manufacturer),
          discovery_protocol=?,
          device_status=COALESCE(?,device_status), last_seen_at=CURRENT_TIMESTAMP,
          detection_count=detection_count+1, last_scan_hits=last_scan_hits+1
        WHERE identity_key=?
        """,clean(name),canonicalDeviceType(type),clean(ip),clean(mac),clean(serial),clean(manufacturer),
          mergeDiscoveryProtocol(protocolFor(existingKey), protocol),clean(status),existingKey);
      return;
    }
    String key=primaryKey(ip,mac,serial,name);
    jdbc.update("""
      INSERT INTO gam_discovery_registered_devices
        (identity_key,name,device_type,address,hardware_address,serial_number,manufacturer,discovery_protocol,device_status,first_seen_at,last_seen_at,detection_count,last_scan_hits)
      VALUES (?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1)
      """,key,clean(name),canonicalDeviceType(type),clean(ip),clean(mac),clean(serial),clean(manufacturer),clean(protocol),clean(status));
  }

  private String protocolFor(String identityKey){
    List<String> rows = jdbc.query("SELECT discovery_protocol FROM gam_discovery_registered_devices WHERE identity_key=?",
      (rs,row)->rs.getString(1), identityKey);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /**
   * Discovery-Quellen und technische Detailwerte werden strukturiert zusammengeführt.
   * Ein neuer Windows-Scan ersetzt gleichnamige, ältere Werte, statt immer weitere
   * (und früher durch VARCHAR(500) abgeschnittene) Detailketten anzuhängen.
   */
  private static String mergeDiscoveryProtocol(String existing, String incoming){
    LinkedHashSet<String> sources = new LinkedHashSet<>();
    java.util.LinkedHashMap<String,String> details = new java.util.LinkedHashMap<>();
    collectProtocol(existing, sources, details);
    collectProtocol(incoming, sources, details);
    List<String> merged = new ArrayList<>(sources);
    details.forEach((label,value)->merged.add(label + ": " + value));
    return merged.isEmpty() ? null : String.join(" · ", merged);
  }

  private static void collectProtocol(String value, LinkedHashSet<String> sources, java.util.LinkedHashMap<String,String> details){
    String cleaned = clean(value);
    if(cleaned==null)return;
    for(String raw : cleaned.split("\\s+·\\s+")){
      String token=clean(raw);
      if(token==null)continue;
      int colon=token.indexOf(':');
      if(colon>0){
        String label=clean(token.substring(0,colon));
        String detailValue=clean(token.substring(colon+1));
        if(label!=null&&detailValue!=null)details.put(label,detailValue);
      }else{
        sources.add(token);
      }
    }
  }

  private String findExistingKey(String ip,String mac,String serial,String name){
    for(String key:keys(ip,mac,serial,name)){
      List<String> found=jdbc.query("SELECT identity_key FROM gam_discovery_registered_devices WHERE identity_key=?",
        (rs,row)->rs.getString(1),key);
      if(!found.isEmpty())return found.get(0);
    }
    String semantic=semanticName(name);
    if(isDistinctiveSemanticName(semantic)){
      for(Map<String,Object> row:findAll()){
        if(semantic.equals(semanticName((String)row.get("name")))) return String.valueOf(row.get("identityKey"));
      }
      String aliasMatch=findIdentityKeyByAliasSemantic(semantic);
      if(aliasMatch!=null) return aliasMatch;
    }
    return null;
  }

  /**
   * 40k33b4: Speichert einen Namen als Alias einer Identität, z.B. wenn ein Gerät
   * umbenannt oder mit einem anderen Datensatz zusammengeführt wird. Bereits
   * vorhandene Aliase werden nicht dupliziert.
   */
  /**
   * 40k34b: Speichert ausschließlich Verbindungsadresse/-port und Zeitpunkt für eine
   * spätere automatische Wiederverbindung - niemals einen Pairing-Code oder ein
   * temporäres Token.
   */
  public void recordAdbConnection(String identityKey, String host, int port){
    jdbc.update("UPDATE gam_discovery_registered_devices SET adb_host=?, adb_port=?, adb_last_connected_at=CURRENT_TIMESTAMP WHERE identity_key=?",
      clean(host), port, identityKey);
  }

  /** 40k34b: Bereits bekannte ADB-Netzwerkziele für die begrenzte automatische Wiederverbindung. */
  public List<Map<String,Object>> knownAdbTargets(){
    return jdbc.queryForList("""
      SELECT identity_key AS identityKey, adb_host AS adbHost, adb_port AS adbPort
      FROM gam_discovery_registered_devices WHERE adb_host IS NOT NULL AND adb_port IS NOT NULL
      ORDER BY adb_last_connected_at DESC
      """);
  }

  /**
   * 40k34e: Ermittelt andere Identitäten, die AKTUELL an dieselbe ADB-Verbindung
   * (Host+Port) gebunden sind wie die übergebene - ein technisch unplausibler
   * 1:n-Zustand (siehe Merge-Ausschlüsse, Abschnitt 6). Reine Datenbankabfrage,
   * kein ADB-Aufruf.
   */
  public List<String> otherIdentitiesWithSameAdbConnection(String identityKey, String adbHost, int adbPort){
    if (adbHost == null) return List.of();
    return jdbc.query(
      "SELECT identity_key FROM gam_discovery_registered_devices WHERE adb_host=? AND adb_port=? AND identity_key<>?",
      (rs, n) -> rs.getString(1), adbHost, adbPort, identityKey);
  }

  public void addAlias(String identityKey, String aliasName){
    String alias=clean(aliasName);
    if(alias==null) return;
    String semantic=semanticName(alias);
    if(!isDistinctiveSemanticName(semantic)) return;
    jdbc.update("INSERT IGNORE INTO gam_discovery_device_aliases(identity_key,alias_name,alias_semantic) VALUES (?,?,?)", identityKey, alias, semantic);
  }

  public List<String> aliasesFor(String identityKey){
    return jdbc.query("SELECT alias_name FROM gam_discovery_device_aliases WHERE identity_key=? ORDER BY created_at ASC",
      (rs,row)->rs.getString(1), identityKey);
  }

  /** 40k33b5: Aliase mit Zeitstempel, Grundlage für Aliasverwaltung und Identitätsverlauf. */
  public List<Map<String,Object>> aliasesWithTimestamps(String identityKey){
    return jdbc.queryForList("""
      SELECT alias_name AS aliasName, created_at AS createdAt
      FROM gam_discovery_device_aliases WHERE identity_key=? ORDER BY created_at ASC
      """, identityKey);
  }

  /** 40k33b5: Entfernt einen einzelnen Alias, ohne die Identität selbst zu verändern. */
  public void removeAlias(String identityKey, String aliasName){
    String alias = clean(aliasName);
    if (alias == null) return;
    jdbc.update("DELETE FROM gam_discovery_device_aliases WHERE identity_key=? AND alias_name=?", identityKey, alias);
  }

  /**
   * 40k33b5: Benennt einen Alias um (z.B. Tippfehler korrigieren), ohne die
   * Identität oder den Hauptnamen zu verändern.
   */
  public void renameAlias(String identityKey, String oldAlias, String newAlias){
    String from = clean(oldAlias), to = clean(newAlias);
    if (from == null || to == null) throw new IllegalArgumentException("Alter und neuer Aliasname werden benötigt.");
    removeAlias(identityKey, from);
    addAlias(identityKey, to);
  }

  /** 40k33b5: Kategorie-Änderungshistorie einer Identität (bereits seit 40k33b1 protokolliert). */
  public List<Map<String,Object>> typeHistoryFor(String identityKey){
    return jdbc.queryForList("""
      SELECT old_device_type AS oldDeviceType, new_device_type AS newDeviceType,
             changed_at AS changedAt, changed_by AS changedBy
      FROM gam_discovery_device_type_history WHERE identity_key=? ORDER BY changed_at DESC
      """, identityKey);
  }

  /**
   * 40k33b5: Liefert die reinen Quellennamen (ohne technische Detailwerte) aus dem
   * bereits vorhandenen discovery_protocol-Text - Wiederverwendung der in
   * mergeDiscoveryProtocol()/collectProtocol() bereits vorhandenen Parsing-Logik
   * (keine zweite, parallele Quellen-Erkennung).
   */
  public static List<String> distinctSourceLabels(String protocol){
    LinkedHashSet<String> sources = new LinkedHashSet<>();
    java.util.LinkedHashMap<String,String> details = new java.util.LinkedHashMap<>();
    collectProtocol(protocol, sources, details);
    return new ArrayList<>(sources);
  }

  /**
   * 40k33b7: Liefert die bereits im discovery_protocol-Text vorhandenen
   * "Label: Wert"-Detailpaare (z.B. "Betriebssystem", "Erkannte Rollen") -
   * Wiederverwendung derselben Parsing-Logik wie distinctSourceLabels()/
   * mergeDiscoveryProtocol(), keine neue Datenstruktur.
   */
  public static Map<String,String> detailValues(String protocol){
    LinkedHashSet<String> sources = new LinkedHashSet<>();
    java.util.LinkedHashMap<String,String> details = new java.util.LinkedHashMap<>();
    collectProtocol(protocol, sources, details);
    return details;
  }

  /**
   * 40k33b10: EIN gemeinsamer Feld-Vergleich für zwei Identitäten - wird sowohl von der
   * Merge-Kandidatenermittlung (DeviceMergeService) als auch von der Integritätsprüfung
   * (DeviceIdentityService, seit 40k33b7) verwendet. Keine zweite Konflikterkennung.
   */
  public record FieldComparisonRow(String label, String valueA, String valueB, boolean differs) {}

  public static List<FieldComparisonRow> compareIdentityFields(Map<String,Object> a, Map<String,Object> b){
    Map<String,String> detailsA = detailValues(str(a.get("protocol")));
    Map<String,String> detailsB = detailValues(str(b.get("protocol")));
    List<FieldComparisonRow> rows = new ArrayList<>();
    rows.add(comparisonRow("IP-Adresse", str(a.get("address")), str(b.get("address"))));
    rows.add(comparisonRow("Erreichbar", statusLabel(str(a.get("status"))), statusLabel(str(b.get("status")))));
    rows.add(comparisonRow("MAC-Adresse", str(a.get("hardwareAddress")), str(b.get("hardwareAddress"))));
    rows.add(comparisonRow("Seriennummer", str(a.get("serialNumber")), str(b.get("serialNumber"))));
    rows.add(comparisonRow("Hostname", str(a.get("name")), str(b.get("name"))));
    rows.add(comparisonRow("SSH-Hostkey", detailsA.get("SSH-Hostkey"), detailsB.get("SSH-Hostkey")));
    rows.add(comparisonRow("Build-Fingerprint (ADB)", detailsA.get("Build-Fingerprint"), detailsB.get("Build-Fingerprint")));
    rows.add(comparisonRow("Android-Version", detailsA.get("Android-Version"), detailsB.get("Android-Version")));
    rows.add(comparisonRow("ADB-Verbindung", adbConnectionLabel(a), adbConnectionLabel(b)));
    rows.add(comparisonRow("Letzter ADB-Kontakt", str(a.get("adbLastConnectedAt")), str(b.get("adbLastConnectedAt"))));
    rows.add(comparisonRow("Betriebssystem/Kategorie", str(a.get("deviceType")), str(b.get("deviceType"))));
    rows.add(comparisonRow("Hersteller/Hardware", str(a.get("manufacturer")), str(b.get("manufacturer"))));
    rows.add(comparisonRow("Discovery-Quellen", String.join(", ", distinctSourceLabels(str(a.get("protocol")))),
      String.join(", ", distinctSourceLabels(str(b.get("protocol"))))));
    rows.add(comparisonRow("Rollen", detailsA.get("Erkannte Rollen"), detailsB.get("Erkannte Rollen")));
    rows.add(comparisonRow("Zeitpunkt (zuletzt erkannt)", str(a.get("lastSeenAt")), str(b.get("lastSeenAt"))));
    return rows;
  }

  private static String adbConnectionLabel(Map<String,Object> row){
    String host = str(row.get("adbHost"));
    Object port = row.get("adbPort");
    return host != null && port != null ? host + ":" + port : null;
  }

  private static FieldComparisonRow comparisonRow(String label, String valueA, String valueB){
    boolean differs = valueA != null && valueB != null && !valueA.equalsIgnoreCase(valueB);
    return new FieldComparisonRow(label, valueA, valueB, differs);
  }

  private static String statusLabel(String status){
    if (status == null) return null;
    return "ONLINE".equalsIgnoreCase(status) || "ERKANNT".equalsIgnoreCase(status) ? "ja" : status;
  }

  /**
   * 40k33b10: Ordnet einen bereits vorhandenen Konflikttext (aus
   * DeviceIdentityConfidenceEngine.assess()) einer der drei geforderten Schutzstufen zu -
   * reine Darstellungs-Einordnung, keine neue fachliche Konfliktlogik.
   */
  public static String conflictSeverity(String conflictText){
    String t = nullToEmpty(conflictText).toLowerCase(Locale.ROOT);
    if (t.contains("ip-adresse") && t.contains("gleichzeitig")) return "KRITISCH";
    if (t.contains("seriennummer")) return "KRITISCH";
    if (t.contains("ssh-hostkey")) return "KRITISCH";
    if (t.contains("zwei unterschiedliche datensätze") && t.contains("adb")) return "KRITISCH";
    if (t.contains("mac-adresse")) return "HOCH";
    if (t.contains("build-fingerprint")) return "HOCH";
    if (t.contains("adb") && t.contains("plattform")) return "HOCH";
    if (t.contains("app-inventarlauf") || t.contains("app-inventar")) return "HOCH";
    return "PRUEFHINWEIS";
  }

  /** 40k33b10: Höchste Schutzstufe unter mehreren bereits vorhandenen Konflikttexten, oder null. */
  public static String highestConflictSeverity(List<String> conflicts){
    if (conflicts == null || conflicts.isEmpty()) return null;
    boolean hasHoch = false;
    for (String c : conflicts) {
      String severity = conflictSeverity(c);
      if ("KRITISCH".equals(severity)) return "KRITISCH";
      if ("HOCH".equals(severity)) hasHoch = true;
    }
    return hasHoch ? "HOCH" : "PRUEFHINWEIS";
  }

  /**
   * 40k34g: EINE zentrale Stelle, die entscheidet, ob eine bewusste Bestätigung vor
   * einer Zusammenführung erforderlich ist. Bisher prüfte das Frontend an mehreren
   * Stellen direkt "criticalityLevel==='KRITISCH'" und erfasste dabei HOCH-Konflikte
   * (z.B. abweichender Build-Fingerprint, siehe 40k34e) nicht - obwohl der Auftrag
   * für HOCH ausdrücklich ebenfalls eine explizite Bestätigung vor einem manuellen
   * Merge verlangt. Frontend und Backend nutzen ab jetzt ausschließlich dieses
   * eine Flag statt eigener Severity-Abfragen.
   */
  public static boolean requiresManualConfirmation(String criticalityLevel){
    return "KRITISCH".equals(criticalityLevel) || "HOCH".equals(criticalityLevel);
  }

  /** 40k33b10: Verständlicher, konflikttyp-spezifischer Warnhinweis für die kritische Schutzregel. */
  public static String criticalConflictMessage(List<String> conflicts){
    if (conflicts == null) return null;
    for (String c : conflicts) {
      String lower = c.toLowerCase(Locale.ROOT);
      if (lower.contains("ip-adresse") && lower.contains("gleichzeitig"))
        return "Zwei unterschiedliche IP-Adressen wurden gleichzeitig aktiv erkannt. Dies ist ein starkes Indiz dafür, dass es sich um zwei verschiedene Geräte handeln könnte. Zusammenführung nur nach sorgfältiger manueller Prüfung.";
      if (lower.contains("seriennummer"))
        return "Die beiden Geräte melden unterschiedliche Seriennummern. Das spricht stark dafür, dass es sich um zwei verschiedene physische Geräte handelt.";
      if (lower.contains("ssh-hostkey"))
        return "Der SSH-Hostkey unterscheidet sich. Ein unterschiedlicher Hostkey ist ein starkes Indiz für ein anderes physisches Gerät an derselben Adresse.";
      if (lower.contains("zwei unterschiedliche datensätze") && lower.contains("adb"))
        return "Zwei unterschiedliche Geräteidentitäten sind derzeit mit derselben ADB-Verbindung (Host/Port) verknüpft. Das ist technisch nicht plausibel und deutet auf einen Zuordnungsfehler hin.";
      // 40k34g: auch für HOCH eingestufte Konflikte eine verständliche Erklärung liefern,
      // da diese seit dieser Nachbesserung ebenfalls eine bewusste Bestätigung verlangen.
      if (lower.contains("build-fingerprint"))
        return "Der Build-Fingerprint unterscheidet sich, obwohl ein anderes starkes Merkmal (z.B. Seriennummer) bereits übereinstimmt. Das kann auf ein anderes physisches Gerät oder ein zurückgesetztes/neu bespieltes Gerät hindeuten.";
      if (lower.contains("mac-adresse"))
        return "Die MAC-Adressen der beiden Geräte weichen voneinander ab. Bitte prüfen, ob es sich tatsächlich um dasselbe Gerät handelt.";
      if ((lower.contains("adb") && lower.contains("plattform")))
        return "Für dieses Gerät ist eine ADB-Verbindung hinterlegt, obwohl die erkannte Plattform nicht Android ist. Bitte die Zuordnung prüfen.";
      if (lower.contains("app-inventarlauf") || lower.contains("app-inventar"))
        return "Der App-Inventarlauf zeigt eine unplausible Auffälligkeit (siehe Begründung). Bitte vor einer Zusammenführung prüfen.";
    }
    return null;
  }

  private String findIdentityKeyByAliasSemantic(String semantic){
    List<String> found=jdbc.query("SELECT identity_key FROM gam_discovery_device_aliases WHERE alias_semantic=? ORDER BY created_at ASC LIMIT 1",
      (rs,row)->rs.getString(1), semantic);
    return found.isEmpty()?null:found.get(0);
  }

  /**
   * 40k31d: Bereits gespeicherte Mehrfachtreffer mit eindeutig gleichem,
   * normalisiertem Gerätenamen werden vor einer neuen Suche konsolidiert.
   */
  private void consolidateSemanticDuplicates(){
    Map<String,List<Map<String,Object>>> groups=new java.util.LinkedHashMap<>();
    for(Map<String,Object> row:findAll()){
      String semantic=semanticName((String)row.get("name"));
      if(isDistinctiveSemanticName(semantic)) groups.computeIfAbsent(semantic,k->new ArrayList<>()).add(row);
    }
    for(List<Map<String,Object>> rows:groups.values()){
      if(rows.size()<2) continue;
      Map<String,Object> keep=rows.get(0);
      String keepKey=String.valueOf(keep.get("identityKey"));
      String name=(String)keep.get("name"), type=(String)keep.get("deviceType"), address=(String)keep.get("address");
      boolean manualType=truthy(keep.get("manualDeviceType"));
      boolean manualName=truthy(keep.get("manualName"));
      String mac=(String)keep.get("hardwareAddress"), serial=(String)keep.get("serialNumber"), manufacturer=(String)keep.get("manufacturer");
      String status=(String)keep.get("status");
      String mergedProtocol=null;
      long detections=0; int scanHits=0;
      for(Map<String,Object> row:rows){
        boolean rowManualName=truthy(row.get("manualName"));
        if(rowManualName){ name=(String)row.get("name"); manualName=true; }
        else if(!manualName) name=prefer(name,(String)row.get("name"));
        boolean rowManual=truthy(row.get("manualDeviceType"));
        if(rowManual){ type=(String)row.get("deviceType"); manualType=true; }
        else if(!manualType) type=preferSpecificType(type,(String)row.get("deviceType"));
        address=prefer(address,(String)row.get("address")); mac=prefer(mac,(String)row.get("hardwareAddress"));
        serial=prefer(serial,(String)row.get("serialNumber")); manufacturer=prefer(manufacturer,(String)row.get("manufacturer"));
        status="ONLINE".equalsIgnoreCase((String)row.get("status"))?"ONLINE":prefer(status,(String)row.get("status"));
        mergedProtocol=mergeDiscoveryProtocol(mergedProtocol,(String)row.get("protocol"));
        detections+=number(row.get("detectionCount")); scanHits+=(int)number(row.get("lastScanHits"));
      }
      jdbc.update("""
        UPDATE gam_discovery_registered_devices SET name=?,device_type=?,manual_device_type=?,manual_name=?,address=?,hardware_address=?,serial_number=?,manufacturer=?,discovery_protocol=?,device_status=?,detection_count=?,last_scan_hits=?,last_seen_at=CURRENT_TIMESTAMP WHERE identity_key=?
        """,clean(name),canonicalDeviceType(type),manualType,manualName,clean(address),clean(mac),clean(serial),clean(manufacturer),clean(mergedProtocol),clean(status),detections,scanHits,keepKey);
      // 40k33b4: Namen der zusammengelegten Datensätze als Alias erhalten (Section 10),
      // statt sie beim Löschen ersatzlos zu verlieren.
      for(int i=1;i<rows.size();i++){
        addAlias(keepKey,(String)rows.get(i).get("name"));
        jdbc.update("DELETE FROM gam_discovery_registered_devices WHERE identity_key=?",String.valueOf(rows.get(i).get("identityKey")));
      }
    }
  }

  private static long number(Object value){return value instanceof Number n?n.longValue():0L;}
  private static boolean truthy(Object value){return Boolean.TRUE.equals(value)||number(value)==1;}
  private static String prefer(String a,String b){return clean(a)!=null?a:b;}

  /**
   * 40k33b4: Gemeinsame, spezifitätsbasierte Kategorie-Priorität. Ersetzt die frühere,
   * nur auf das Wort "netzwerk" prüfende Variante, die eine bereits vorhandene
   * spezifische Kategorie (z.B. "Energie / Wechselrichter") fälschlich beibehalten
   * konnte, wenn eine generische Linux-Plattformkategorie zuerst gespeichert wurde.
   * Wird sowohl hier als auch von DeviceDiscoveryService für die Live-Zusammenführung
   * während eines laufenden Suchlaufs verwendet - keine parallele Implementierung.
   */
  static String preferSpecificType(String a, String b){
    int ra=categorySpecificity(a), rb=categorySpecificity(b);
    if(ra>rb) return prefer(a,b);
    if(rb>ra) return prefer(b,a);
    return prefer(a,b);
  }

  /**
   * 0 = generischer Discovery-Platzhalter (z.B. "Netzwerkgerät"), 1 = erkannte
   * Plattform/Betriebssystem ohne eigene Geräteklasse (z.B. "Server / Linux-System"),
   * 2 = konkrete Geräteklasse (z.B. "Energie / Wechselrichter", "Smart-Home-Zentrale / Linux").
   */
  static int categorySpecificity(String type){
    String v=normalizedCategory(type);
    if(v.isEmpty()) return 0;
    if(v.equals("netzwerkgeraet")||v.equals("homeassistantgeraet")||v.equals("snmpnetzwerkgeraet")||v.startsWith("mdnsgeraet")||v.equals("unbekanntesgeraet")) return 0;
    if(v.equals("serverlinuxsystem")||v.equals("computerlinux")||v.equals("dateiserverlinux")||v.equals("einplatinencomputerlinux")||v.equals("androidgeraet")) return 1;
    return 2;
  }
  private static boolean isDistinctiveSemanticName(String value){return value!=null&&value.length()>=8&&!java.util.Set.of("smartdevice","networkdevice","unknown device","unbekanntesgeraet","geraet","device").contains(value);}
  private static String semanticName(String value){
    if(value==null)return "";
    String v=value.trim().toLowerCase().replace("ä","ae").replace("ö","oe").replace("ü","ue").replace("ß","ss");
    int dot=v.indexOf('.'); if(dot>0&&dot<v.length()-1&&v.substring(0,dot).matches("[a-z_]+"))v=v.substring(dot+1);
    return v.replaceAll("[^a-z0-9]","");
  }

  public int deregisterByKey(String identityKey){
    return jdbc.update("DELETE FROM gam_discovery_registered_devices WHERE identity_key=?", identityKey);
  }

  public int deregisterAll(){
    return jdbc.update("DELETE FROM gam_discovery_registered_devices");
  }

  public int count(){
    Integer value=jdbc.queryForObject("SELECT COUNT(*) FROM gam_discovery_registered_devices", Integer.class);
    return value==null?0:value;
  }

  public int deregister(String ip,String mac,String serial,String name){
    int deleted=0; for(String key:keys(ip,mac,serial,name)) deleted+=deregisterByKey(key); return deleted;
  }

  /**
   * 40k33b4: Führt eine manuell bestätigte Zusammenführung mehrerer registrierter
   * Discovery-Datensätze zu EINEM Zieldatensatz durch. Wird ausschließlich nach
   * einer vom Benutzer bestätigten Vorschau aufgerufen (siehe DeviceMergeService).
   *
   * Schutzregeln:
   * - Zwei unterschiedliche BEKANNTE MAC-Adressen verhindern die Zusammenführung,
   *   außer confirmMacConflict=true wird ausdrücklich übergeben.
   * - Ein manueller Name/eine manuelle Kategorie an Ziel ODER Quelle bleibt erhalten
   *   und wird niemals durch einen automatischen Wert überschrieben.
   * - Trefferzähler werden aufsummiert (jede Quelle zählte eigene Rohtreffer,
   *   siehe recordDiscoveryHit) statt überschrieben, first_seen_at/last_seen_at
   *   werden über alle beteiligten Datensätze hinweg als MIN/MAX übernommen.
   * - Alle bisherigen Namen der zusammengeführten Datensätze bleiben als Alias
   *   erhalten, damit ein künftiger Treffer wieder korrekt zugeordnet wird.
   *
   * Die gesamte Operation ist transaktional: schlägt ein Schritt fehl, wird
   * nichts gespeichert (siehe @Transactional auf DeviceMergeService).
   */
  /** Pure, non-persisting result of a planned merge - reused by both the preview and the actual merge write. */
  public record MergePlan(String targetKey, List<String> sourceKeys, Map<String,Object> targetRow,
                          List<Map<String,Object>> sourceRows, String name, boolean manualName,
                          String deviceType, boolean manualDeviceType, String address, String mac,
                          String serialNumber, String manufacturer, String protocol, String status,
                          long detectionCount, int lastScanHits, Object firstSeenAt, Object lastSeenAt,
                          List<String> knownMacs) {}

  @Transactional
  public Map<String,Object> mergeDevices(String targetKey, List<String> sourceKeys, Map<String,Object> overrides, String actor){
    MergePlan plan = planMerge(targetKey, sourceKeys, overrides);
    jdbc.update("""
      UPDATE gam_discovery_registered_devices SET
        name=?, device_type=?, manual_name=?, manual_device_type=?, address=?, hardware_address=?,
        serial_number=?, manufacturer=?, discovery_protocol=?, device_status=?,
        detection_count=?, last_scan_hits=?, first_seen_at=?, last_seen_at=?
      WHERE identity_key=?
      """, clean(plan.name()), clean(plan.deviceType()), plan.manualName(), plan.manualDeviceType(),
      clean(plan.address()), clean(plan.mac()), clean(plan.serialNumber()), clean(plan.manufacturer()),
      clean(plan.protocol()), clean(plan.status()), plan.detectionCount(), plan.lastScanHits(),
      plan.firstSeenAt(), plan.lastSeenAt(), plan.targetKey());

    // Aliase: bisherige Namen von Ziel und Quellen sowie deren bereits vorhandene
    // Aliase gehen nicht verloren (Section 9/10).
    addAlias(plan.targetKey(), (String)plan.targetRow().get("name"));
    for(Map<String,Object> row: plan.sourceRows()){
      addAlias(plan.targetKey(), (String)row.get("name"));
      String sourceKey=String.valueOf(row.get("identityKey"));
      for(String existingAlias: aliasesFor(sourceKey)) addAlias(plan.targetKey(), existingAlias);
    }
    for(String key: plan.sourceKeys()){
      jdbc.update("DELETE FROM gam_discovery_device_aliases WHERE identity_key=?", key);
      jdbc.update("DELETE FROM gam_discovery_registered_devices WHERE identity_key=?", key);
    }
    return find(plan.targetKey());
  }

  /**
   * Berechnet ausschließlich das Ergebnis einer geplanten Zusammenführung (keine
   * Datenbankänderung), damit DeviceMergeService daraus eine Vorschau erzeugen
   * kann, die exakt der später tatsächlich ausgeführten Logik entspricht.
   */
  public MergePlan planMerge(String targetKey, List<String> sourceKeys, Map<String,Object> overrides){
    String target=clean(targetKey);
    if(target==null) throw new IllegalArgumentException("Zielgerät fehlt.");
    List<String> sources=(sourceKeys==null?List.<String>of():sourceKeys).stream()
      .map(DiscoveryRegistrationRepository::clean).filter(k->k!=null&&!k.equals(target)).distinct().toList();
    if(sources.isEmpty()) throw new IllegalArgumentException("Es wird mindestens ein weiteres Gerät für die Zusammenführung benötigt.");

    Map<String,Object> targetRow=find(target);
    List<Map<String,Object>> sourceRows=new ArrayList<>();
    for(String key:sources) sourceRows.add(find(key));

    LinkedHashSet<String> knownMacs=new LinkedHashSet<>();
    String macDisplay=null;
    String targetMacRaw=(String)targetRow.get("hardwareAddress");
    addIfPresent(knownMacs,targetMacRaw);
    if(clean(targetMacRaw)!=null) macDisplay=clean(targetMacRaw);
    for(Map<String,Object> row:sourceRows){
      String rawMac=(String)row.get("hardwareAddress");
      addIfPresent(knownMacs,rawMac);
      if(macDisplay==null && clean(rawMac)!=null) macDisplay=clean(rawMac);
    }
    boolean confirmMacConflict=overrides!=null && Boolean.TRUE.equals(overrides.get("confirmMacConflict"));
    if(knownMacs.size()>1 && !confirmMacConflict){
      throw new IllegalStateException("Widersprüchliche MAC-Adressen erkannt ("+String.join(", ",knownMacs)+"). Zusammenführung wurde nicht durchgeführt.");
    }

    boolean manualName=truthy(targetRow.get("manualName"));
    String name=(String)targetRow.get("name");
    String manualNameFrom=manualName?(String)targetRow.get("name"):null;
    if(!manualName){
      for(Map<String,Object> row:sourceRows){
        if(truthy(row.get("manualName"))){ manualNameFrom=(String)row.get("name"); manualName=true; break; }
      }
    }
    if(manualNameFrom!=null) name=manualNameFrom;
    else {
      // Kein manueller Name vorhanden: bevorzugten (nicht-generischen) Namen anhand der
      // vorhandenen Werte wählen, Zielname zuerst.
      for(Map<String,Object> row:sourceRows) name=prefer(name,(String)row.get("name"));
    }
    Object nameOverride=overrides==null?null:overrides.get("name");
    if(nameOverride instanceof String s && clean(s)!=null){ name=clean(s); manualName=true; }

    boolean manualType=truthy(targetRow.get("manualDeviceType"));
    String type=(String)targetRow.get("deviceType");
    if(manualType){
      // bleibt wie am Ziel
    } else {
      String manualTypeFrom=null;
      for(Map<String,Object> row:sourceRows){
        if(truthy(row.get("manualDeviceType"))){ manualTypeFrom=(String)row.get("deviceType"); manualType=true; break; }
      }
      if(manualTypeFrom!=null) type=manualTypeFrom;
      else for(Map<String,Object> row:sourceRows) type=preferSpecificType(type,(String)row.get("deviceType"));
    }
    Object typeOverride=overrides==null?null:overrides.get("deviceType");
    if(typeOverride instanceof String s && clean(s)!=null){ type=canonicalDeviceType(clean(s)); manualType=true; }

    String address=(String)targetRow.get("address");
    String serial=(String)targetRow.get("serialNumber");
    String manufacturer=(String)targetRow.get("manufacturer");
    String status=(String)targetRow.get("status");
    String protocol=(String)targetRow.get("protocol");
    long detections=number(targetRow.get("detectionCount"));
    int scanHits=(int)number(targetRow.get("lastScanHits"));
    Object firstSeen=targetRow.get("firstSeenAt");
    Object lastSeen=targetRow.get("lastSeenAt");
    for(Map<String,Object> row:sourceRows){
      address=prefer(address,(String)row.get("address"));
      serial=prefer(serial,(String)row.get("serialNumber"));
      manufacturer=prefer(manufacturer,(String)row.get("manufacturer"));
      status="ONLINE".equalsIgnoreCase((String)row.get("status"))?"ONLINE":prefer(status,(String)row.get("status"));
      protocol=mergeDiscoveryProtocol(protocol,(String)row.get("protocol"));
      detections+=number(row.get("detectionCount"));
      scanHits+=(int)number(row.get("lastScanHits"));
      firstSeen=earlier(firstSeen,row.get("firstSeenAt"));
      lastSeen=later(lastSeen,row.get("lastSeenAt"));
    }

    return new MergePlan(target, sources, targetRow, sourceRows, name, manualName, canonicalDeviceType(type),
      manualType, address, macDisplay, serial, manufacturer, protocol, status, detections, scanHits,
      firstSeen, lastSeen, new ArrayList<>(knownMacs));
  }

  private static void addIfPresent(Set<String> target, String mac){
    String v=clean(mac);
    if(v==null) return;
    String normalized=v.replaceAll("[^0-9A-Fa-f]","").toUpperCase(Locale.ROOT);
    if(!normalized.isBlank()) target.add(normalized);
  }
  private static Object earlier(Object a, Object b){
    if(a==null) return b; if(b==null) return a;
    return (a instanceof Comparable c) && c.compareTo(b) <= 0 ? a : b;
  }
  private static Object later(Object a, Object b){
    if(a==null) return b; if(b==null) return a;
    return (a instanceof Comparable c) && c.compareTo(b) >= 0 ? a : b;
  }

  /**
   * 40k33b1: Synchronisiert automatisch gelieferte Gerätetypen mit den bereits
   * vorhandenen Kategorien. Manuell verwendete Kategorien haben Vorrang, danach
   * Kategorien des Gerätebestands und danach die zuerst registrierte automatische
   * Kategorie. Nur wenn keine semantisch passende Kategorie existiert, wird der
   * neue Typ verwendet.
   */
  private String canonicalDeviceType(String requestedType) {
    String requested = clean(requestedType);
    if (requested == null) return null;
    String family = categoryFamily(requested);
    if (family == null) return requested;

    for (String existing : existingDeviceTypes()) {
      if (family.equals(categoryFamily(existing))) return existing;
    }
    return requested;
  }

  private List<String> existingDeviceTypes() {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    // Explizite manuelle Zuordnungen zuerst.
    addTypes(result, "SELECT device_type FROM gam_discovery_registered_devices WHERE manual_device_type=TRUE AND device_type IS NOT NULL ORDER BY registered_at ASC");
    // Danach bestehender GAM-2- und GAM-1-Gerätebestand.
    addTypes(result, "SELECT `Typ` FROM `geräte_neu` WHERE `Typ` IS NOT NULL ORDER BY `ID` ASC");
    addTypes(result, "SELECT `GeräteTyp` FROM `geräte` WHERE `GeräteTyp` IS NOT NULL ORDER BY `GERÄTE_ID` ASC");
    // Bereits automatisch angelegte Kategorien behalten ebenfalls Vorrang vor neuen Synonymen.
    addTypes(result, "SELECT device_type FROM gam_discovery_registered_devices WHERE manual_device_type=FALSE AND device_type IS NOT NULL ORDER BY registered_at ASC");
    return new ArrayList<>(result);
  }

  private void addTypes(LinkedHashSet<String> target, String sql) {
    try {
      for (String value : jdbc.query(sql, (rs, rowNum) -> clean(rs.getString(1)))) {
        if (value != null) target.add(value);
      }
    } catch (Exception ignored) {
      // Legacy-Tabellen können in einzelnen Installationen noch fehlen.
    }
  }

  private static String categoryFamily(String value) {
    String v = normalizedCategory(value);
    if (v.isEmpty()) return null;
    if (containsAny(v, "computer", "pc", "desktop", "notebook", "laptop", "workstation", "thinclient", "linuxpc", "windowspc", "macos", "imac", "macbook")) return "computer";
    if (containsAny(v, "server", "linuxsystem", "virtualisierungshost", "esxi", "proxmox", "hyperv", "kvm", "vmware")) return "server";
    if (containsAny(v, "netzwerk", "router", "switch", "accesspoint", "gateway", "firewall", "wlan")) return "network";
    if (containsAny(v, "drucker", "scanner", "multifunktion", "printserver")) return "printer";
    if (containsAny(v, "nas", "speicher", "storage", "synology", "qnap", "truenas")) return "storage";
    if (containsAny(v, "kamera", "camera", "videoüberwachung", "videoueberwachung", "onvif", "nvr")) return "camera";
    if (containsAny(v, "smartphone", "mobilgerät", "mobilgeraet", "tablet", "android", "iphone", "ipad")) return "mobile";
    if (containsAny(v, "robotik", "roboter", "saugroboter", "mähroboter", "maehroboter")) return "robotics";
    if (containsAny(v, "haushaltsgerät", "haushaltsgeraet", "geschirrspüler", "geschirrspueler", "waschmaschine", "trockner")) return "appliance";
    if (containsAny(v, "energie", "wechselrichter", "inverter", "batterie", "solar", "photovoltaik")) return "energy";
    if (containsAny(v, "usv", "ups")) return "ups";
    return null;
  }

  private static boolean containsAny(String value, String... needles) {
    for (String needle : needles) if (value.contains(normalizedCategory(needle))) return true;
    return false;
  }

  private static String normalizedCategory(String value) {
    if (value == null) return "";
    return value.toLowerCase(Locale.ROOT)
      .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
      .replaceAll("[^a-z0-9]", "");
  }

  private static List<String> keys(String ip,String mac,String serial,String name){
    LinkedHashSet<String> keys=new LinkedHashSet<>();
    if(clean(mac)!=null)keys.add("mac:"+norm(mac));
    if(clean(serial)!=null)keys.add("serial:"+norm(serial));
    if(clean(ip)!=null)keys.add("ip:"+clean(ip).toLowerCase());
    if(clean(name)!=null)keys.add("name:"+norm(name));
    return new ArrayList<>(keys);
  }
  private static String primaryKey(String ip,String mac,String serial,String name){var keys=keys(ip,mac,serial,name);return keys.isEmpty()?"generated:"+java.util.UUID.randomUUID():keys.get(0);}

  /**
   * 40k33b7: Öffentlicher Zugriff auf dieselbe Schlüsselberechnung wie register() -
   * damit die neue Wiederauftrennung nach dem Anlegen eines abgespaltenen Geräts
   * dessen Identitätsschlüssel zuverlässig auflösen kann, ohne eine zweite
   * Schlüssellogik einzuführen. Setzt voraus, dass mindestens ein Merkmal
   * (ip/mac/serial/name) angegeben ist - sonst ist das Ergebnis nicht deterministisch.
   */
  public static String resolveKey(String ip,String mac,String serial,String name){ return primaryKey(ip,mac,serial,name); }
  private static String norm(String v){return semanticName(v);}
  private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
  private static String str(Object value){return value==null?null:clean(String.valueOf(value));}
  private static String nullToEmpty(String value){return value==null?"":value;}
}
