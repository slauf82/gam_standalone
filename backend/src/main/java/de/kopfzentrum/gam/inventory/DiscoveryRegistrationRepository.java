package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
        discovery_protocol VARCHAR(500) NULL,
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
    addColumn("discovery_protocol", "VARCHAR(500) NULL");
    addColumn("device_status", "VARCHAR(100) NULL");
    addColumn("first_seen_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
    addColumn("last_seen_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
    addColumn("detection_count", "BIGINT NOT NULL DEFAULT 0");
    addColumn("last_scan_hits", "INT NOT NULL DEFAULT 0");
    addColumn("manual_device_type", "BOOLEAN NOT NULL DEFAULT FALSE");
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

  public List<Map<String,Object>> findAll(){
    return jdbc.queryForList("""
      SELECT identity_key AS identityKey, name, device_type AS deviceType, address,
             hardware_address AS hardwareAddress, serial_number AS serialNumber,
             manufacturer, discovery_protocol AS protocol, device_status AS status,
             first_seen_at AS firstSeenAt, last_seen_at AS lastSeenAt, registered_at AS registeredAt,
             detection_count AS detectionCount, last_scan_hits AS lastScanHits,
             manual_device_type AS manualDeviceType
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
             manual_device_type AS manualDeviceType
      FROM gam_discovery_registered_devices WHERE identity_key=?
      """, identityKey);
    if(rows.isEmpty()) throw new IllegalArgumentException("Registriertes Gerät wurde nicht gefunden.");
    return rows.get(0);
  }


  public Map<String,Object> updateDeviceType(String identityKey, String newType, String changedBy){
    String type=clean(newType);
    if(type==null) throw new IllegalArgumentException("Gerätetyp fehlt.");
    Map<String,Object> current=find(identityKey);
    Object oldValue=current.get("deviceType");
    String oldType=oldValue==null?null:clean(String.valueOf(oldValue));
    jdbc.update("UPDATE gam_discovery_registered_devices SET device_type=?, manual_device_type=TRUE WHERE identity_key=?", type, identityKey);
    jdbc.update("INSERT INTO gam_discovery_device_type_history(identity_key,old_device_type,new_device_type,changed_by) VALUES (?,?,?,?)", identityKey, oldType, type, clean(changedBy));
    return find(identityKey);
  }

  public Map<String,Object> updateDeviceName(String identityKey, String newName){
    String name=clean(newName);
    if(name==null) throw new IllegalArgumentException("Gerätename fehlt.");
    find(identityKey);
    jdbc.update("UPDATE gam_discovery_registered_devices SET name=? WHERE identity_key=?", name, identityKey);
    return find(identityKey);
  }

  public boolean isRegistered(String ip,String mac,String serial,String name){
    for(String key: keys(ip,mac,serial,name)){
      Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM gam_discovery_registered_devices WHERE identity_key=?",Integer.class,key);
      if(count!=null&&count>0)return true;
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
        name=COALESCE(VALUES(name),name), device_type=CASE WHEN manual_device_type THEN device_type ELSE COALESCE(VALUES(device_type),device_type) END,
        address=COALESCE(VALUES(address),address), hardware_address=COALESCE(VALUES(hardware_address),hardware_address),
        serial_number=COALESCE(VALUES(serial_number),serial_number), manufacturer=COALESCE(VALUES(manufacturer),manufacturer),
        discovery_protocol=COALESCE(VALUES(discovery_protocol),discovery_protocol), device_status=COALESCE(VALUES(device_status),device_status),
        last_seen_at=CURRENT_TIMESTAMP
      """,key,clean(name),clean(type),clean(ip),clean(mac),clean(serial),clean(manufacturer),clean(protocol),clean(status));
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
          name=COALESCE(?,name), device_type=CASE WHEN manual_device_type THEN device_type ELSE COALESCE(?,device_type) END, address=COALESCE(?,address),
          hardware_address=COALESCE(?,hardware_address), serial_number=COALESCE(?,serial_number),
          manufacturer=COALESCE(?,manufacturer),
          discovery_protocol=CASE WHEN ? IS NULL THEN discovery_protocol WHEN discovery_protocol IS NULL OR discovery_protocol='' THEN ? WHEN LOCATE(?,discovery_protocol)>0 THEN discovery_protocol ELSE CONCAT(discovery_protocol,' · ',?) END,
          device_status=COALESCE(?,device_status), last_seen_at=CURRENT_TIMESTAMP,
          detection_count=detection_count+1, last_scan_hits=last_scan_hits+1
        WHERE identity_key=?
        """,clean(name),clean(type),clean(ip),clean(mac),clean(serial),clean(manufacturer),clean(protocol),clean(protocol),clean(protocol),clean(protocol),clean(status),existingKey);
      return;
    }
    String key=primaryKey(ip,mac,serial,name);
    jdbc.update("""
      INSERT INTO gam_discovery_registered_devices
        (identity_key,name,device_type,address,hardware_address,serial_number,manufacturer,discovery_protocol,device_status,first_seen_at,last_seen_at,detection_count,last_scan_hits)
      VALUES (?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1)
      """,key,clean(name),clean(type),clean(ip),clean(mac),clean(serial),clean(manufacturer),clean(protocol),clean(status));
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
    }
    return null;
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
      boolean manualType=Boolean.TRUE.equals(keep.get("manualDeviceType")) || number(keep.get("manualDeviceType"))==1;
      String mac=(String)keep.get("hardwareAddress"), serial=(String)keep.get("serialNumber"), manufacturer=(String)keep.get("manufacturer");
      String status=(String)keep.get("status");
      LinkedHashSet<String> protocols=new LinkedHashSet<>();
      long detections=0; int scanHits=0;
      for(Map<String,Object> row:rows){
        name=prefer(name,(String)row.get("name"));
        boolean rowManual=Boolean.TRUE.equals(row.get("manualDeviceType")) || number(row.get("manualDeviceType"))==1;
        if(rowManual){ type=(String)row.get("deviceType"); manualType=true; }
        else if(!manualType) type=preferSpecificType(type,(String)row.get("deviceType"));
        address=prefer(address,(String)row.get("address")); mac=prefer(mac,(String)row.get("hardwareAddress"));
        serial=prefer(serial,(String)row.get("serialNumber")); manufacturer=prefer(manufacturer,(String)row.get("manufacturer"));
        status="ONLINE".equalsIgnoreCase((String)row.get("status"))?"ONLINE":prefer(status,(String)row.get("status"));
        addProtocols(protocols,(String)row.get("protocol"));
        detections+=number(row.get("detectionCount")); scanHits+=(int)number(row.get("lastScanHits"));
      }
      jdbc.update("""
        UPDATE gam_discovery_registered_devices SET name=?,device_type=?,manual_device_type=?,address=?,hardware_address=?,serial_number=?,manufacturer=?,discovery_protocol=?,device_status=?,detection_count=?,last_scan_hits=?,last_seen_at=CURRENT_TIMESTAMP WHERE identity_key=?
        """,clean(name),clean(type),manualType,clean(address),clean(mac),clean(serial),clean(manufacturer),clean(String.join(" · ",protocols)),clean(status),detections,scanHits,keepKey);
      for(int i=1;i<rows.size();i++) jdbc.update("DELETE FROM gam_discovery_registered_devices WHERE identity_key=?",String.valueOf(rows.get(i).get("identityKey")));
    }
  }

  private static void addProtocols(LinkedHashSet<String> target,String value){if(value==null)return;for(String part:value.split("[,·]")){String x=part.trim();if(!x.isBlank())target.add(x);}}
  private static long number(Object value){return value instanceof Number n?n.longValue():0L;}
  private static String prefer(String a,String b){return clean(a)!=null?a:b;}
  private static String preferSpecificType(String a,String b){if(clean(a)==null)return b;if(clean(b)==null)return a;return a.toLowerCase().contains("netzwerk")&&!b.toLowerCase().contains("netzwerk")?b:a;}
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

  private static List<String> keys(String ip,String mac,String serial,String name){
    LinkedHashSet<String> keys=new LinkedHashSet<>();
    if(clean(mac)!=null)keys.add("mac:"+norm(mac));
    if(clean(serial)!=null)keys.add("serial:"+norm(serial));
    if(clean(ip)!=null)keys.add("ip:"+clean(ip).toLowerCase());
    if(clean(name)!=null)keys.add("name:"+norm(name));
    return new ArrayList<>(keys);
  }
  private static String primaryKey(String ip,String mac,String serial,String name){var keys=keys(ip,mac,serial,name);return keys.isEmpty()?"generated:"+java.util.UUID.randomUUID():keys.get(0);}
  private static String norm(String v){return semanticName(v);}
  private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
}
