package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public class DeviceIdentityMergeSettingsRepository {
  private final JdbcTemplate jdbc;
  public DeviceIdentityMergeSettingsRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

  @PostConstruct void init(){
    jdbc.execute("CREATE TABLE IF NOT EXISTS gam_discovery_merge_settings (id INT NOT NULL PRIMARY KEY, auto_merge_threshold INT NOT NULL DEFAULT 95, duplicate_threshold INT NOT NULL DEFAULT 60, mac_weight INT NOT NULL DEFAULT 85, hardware_serial_weight INT NOT NULL DEFAULT 95, snmp_serial_weight INT NOT NULL DEFAULT 90, device_id_weight INT NOT NULL DEFAULT 80, hostname_weight INT NOT NULL DEFAULT 50, ip_weight INT NOT NULL DEFAULT 5, manufacturer_weight INT NOT NULL DEFAULT 10, type_weight INT NOT NULL DEFAULT 15, two_source_bonus INT NOT NULL DEFAULT 5, three_source_bonus INT NOT NULL DEFAULT 10, automatic_merge_enabled BOOLEAN NOT NULL DEFAULT TRUE, hard_conflicts_block_merge BOOLEAN NOT NULL DEFAULT TRUE, ip_never_merges_alone BOOLEAN NOT NULL DEFAULT TRUE, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL)");
    jdbc.update("INSERT IGNORE INTO gam_discovery_merge_settings(id,updated_by) VALUES(1,'system')");
  }

  public DeviceIdentityConfidenceEngine.Settings load(){
    return jdbc.queryForObject("SELECT auto_merge_threshold,duplicate_threshold,mac_weight,hardware_serial_weight,snmp_serial_weight,device_id_weight,hostname_weight,ip_weight,manufacturer_weight,type_weight,two_source_bonus,three_source_bonus,automatic_merge_enabled,hard_conflicts_block_merge,ip_never_merges_alone FROM gam_discovery_merge_settings WHERE id=1", (rs,n)->new DeviceIdentityConfidenceEngine.Settings(rs.getInt(1),rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getInt(5),rs.getInt(6),rs.getInt(7),rs.getInt(8),rs.getInt(9),rs.getInt(10),rs.getInt(11),rs.getInt(12),rs.getBoolean(13),rs.getBoolean(14),rs.getBoolean(15)));
  }

  public DeviceIdentityConfidenceEngine.Settings save(Map<String,Object> v,String actor){
    int auto=i(v,"autoMergeThreshold",95,1,1000), dup=i(v,"possibleDuplicateThreshold",60,0,999);
    if(dup>=auto) throw new IllegalArgumentException("Der Dublettenschwellwert muss kleiner als der Auto-Merge-Schwellwert sein.");
    jdbc.update("UPDATE gam_discovery_merge_settings SET auto_merge_threshold=?,duplicate_threshold=?,mac_weight=?,hardware_serial_weight=?,snmp_serial_weight=?,device_id_weight=?,hostname_weight=?,ip_weight=?,manufacturer_weight=?,type_weight=?,two_source_bonus=?,three_source_bonus=?,automatic_merge_enabled=?,hard_conflicts_block_merge=?,ip_never_merges_alone=?,updated_by=? WHERE id=1", auto,dup,i(v,"macWeight",85,0,1000),i(v,"hardwareSerialWeight",95,0,1000),i(v,"snmpSerialWeight",90,0,1000),i(v,"deviceIdWeight",80,0,1000),i(v,"hostnameWeight",50,0,1000),i(v,"ipWeight",5,0,1000),i(v,"manufacturerWeight",10,0,1000),i(v,"typeWeight",15,0,1000),i(v,"twoSourceBonus",5,0,1000),i(v,"threeSourceBonus",10,0,1000),b(v,"automaticMergeEnabled",true),b(v,"hardConflictsBlockMerge",true),b(v,"ipNeverMergesAlone",true),actor);
    return load();
  }
  public DeviceIdentityConfidenceEngine.Settings reset(String actor){return save(Map.of(),actor);}
  private static int i(Map<String,Object> v,String k,int d,int min,int max){Object x=v==null?null:v.get(k);int n=x instanceof Number?((Number)x).intValue():d;return Math.max(min,Math.min(max,n));}
  private static boolean b(Map<String,Object> v,String k,boolean d){Object x=v==null?null:v.get(k);return x instanceof Boolean?(Boolean)x:d;}
}
