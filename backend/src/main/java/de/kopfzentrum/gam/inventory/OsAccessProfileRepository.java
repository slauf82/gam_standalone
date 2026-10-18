package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** 40k36q3: Mehrfachkonfiguration fuer Betriebssystemzugaenge (WinRM, SSH, ADB). */
@Repository
public class OsAccessProfileRepository {
  private static final Logger log = LoggerFactory.getLogger(OsAccessProfileRepository.class);
  private final JdbcTemplate jdbc;
  public OsAccessProfileRepository(JdbcTemplate jdbc){ this.jdbc=jdbc; }

  public record Profile(Long id,String type,String name,String host,boolean enabled,String username,String authMode,
                        boolean passwordConfigured,String keyPath,int port,boolean https,Integer pairingPort,
                        boolean pairingCodeConfigured,String identityKey,String updatedAt) {}
  public record SecretProfile(Long id,String type,String name,String host,boolean enabled,String username,String authMode,
                              String password,String keyPath,int port,boolean https,Integer pairingPort,
                              String pairingCode,String identityKey) {}
  public record Update(Long id,String type,String name,String host,Boolean enabled,String username,String authMode,
                       String password,Boolean clearPassword,String keyPath,Integer port,Boolean https,
                       Integer pairingPort,String pairingCode,Boolean clearPairingCode,String identityKey) {}

  @PostConstruct void init(){
    jdbc.execute("CREATE TABLE IF NOT EXISTS gam_os_access_profile (id BIGINT NOT NULL AUTO_INCREMENT, type VARCHAR(16) NOT NULL, name VARCHAR(255) NOT NULL DEFAULT '', host VARCHAR(255) NOT NULL DEFAULT '', enabled BOOLEAN NOT NULL DEFAULT TRUE, username VARCHAR(255) NOT NULL DEFAULT '', auth_mode VARCHAR(32) NOT NULL DEFAULT '', password TEXT NULL, key_path TEXT NULL, port INT NOT NULL DEFAULT 0, use_https BOOLEAN NOT NULL DEFAULT FALSE, pairing_port INT NULL, pairing_code TEXT NULL, identity_key VARCHAR(512) NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(id), INDEX idx_os_access_type(type), INDEX idx_os_access_host(type,host))");
    // 40k36t16: additive Vorabmigration für Datenbanken aus abgebrochenen Erststarts.
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS type VARCHAR(16) NOT NULL DEFAULT ''");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS name VARCHAR(255) NOT NULL DEFAULT ''");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS host VARCHAR(255) NOT NULL DEFAULT ''");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS username VARCHAR(255) NOT NULL DEFAULT ''");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS auth_mode VARCHAR(32) NOT NULL DEFAULT ''");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS password TEXT NULL");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS key_path TEXT NULL");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS port INT NOT NULL DEFAULT 0");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS use_https BOOLEAN NOT NULL DEFAULT FALSE");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS pairing_port INT NULL");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS pairing_code TEXT NULL");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS identity_key VARCHAR(512) NULL");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
    jdbc.execute("ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NULL");
    logPersistedProfileCounts("STARTUP");
  }

  /** 40k36t117: Sichtbare Persistenzdiagnose. WinRM, SSH und ADB liegen gemeinsam
   * in gam_os_access_profile; diese Zählung beweist nach einem Backend-Neustart,
   * welche Datensätze tatsächlich aus MariaDB wieder geladen werden können. */
  private void logPersistedProfileCounts(String phase){
    try {
      Integer total=jdbc.queryForObject("SELECT COUNT(*) FROM gam_os_access_profile",Integer.class);
      Integer winrm=jdbc.queryForObject("SELECT COUNT(*) FROM gam_os_access_profile WHERE type='WINRM'",Integer.class);
      Integer ssh=jdbc.queryForObject("SELECT COUNT(*) FROM gam_os_access_profile WHERE type='SSH'",Integer.class);
      Integer adb=jdbc.queryForObject("SELECT COUNT(*) FROM gam_os_access_profile WHERE type='ADB'",Integer.class);
      log.info("[OS-ACCESS-PERSIST] Phase={} DB=gam_os_access_profile Total={} WINRM={} SSH={} ADB={}",phase,total,winrm,ssh,adb);
    } catch (Exception ex) {
      log.warn("[OS-ACCESS-PERSIST] Phase={} Persistenzstatus konnte nicht gelesen werden: {}",phase,ex.getMessage());
    }
  }

  public List<Profile> list(){
    return jdbc.query("SELECT id,type,name,host,enabled,username,auth_mode,password,key_path,port,use_https,pairing_port,pairing_code,identity_key,updated_at FROM gam_os_access_profile ORDER BY type,name,host,id",
      (rs,n)->new Profile(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5),rs.getString(6),rs.getString(7),
        rs.getString(8)!=null&&!rs.getString(8).isBlank(),rs.getString(9),rs.getInt(10),rs.getBoolean(11),(Integer)rs.getObject(12),
        rs.getString(13)!=null&&!rs.getString(13).isBlank(),rs.getString(14),String.valueOf(rs.getTimestamp(15))));
  }

  public SecretProfile find(long id){
    var rows=jdbc.query("SELECT id,type,name,host,enabled,username,auth_mode,COALESCE(password,''),COALESCE(key_path,''),port,use_https,pairing_port,COALESCE(pairing_code,''),COALESCE(identity_key,'') FROM gam_os_access_profile WHERE id=?",
      (rs,n)->new SecretProfile(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getInt(10),rs.getBoolean(11),(Integer)rs.getObject(12),rs.getString(13),rs.getString(14)),id);
    return rows.isEmpty()?null:rows.get(0);
  }

  public SecretProfile resolve(String type,String host){
    String t=clean(type).toUpperCase(); String h=clean(host);
    // 40k36t38: Bei mehreren aktiven Zugängen für denselben Host muss der zuletzt
    // gespeicherte/getestete Zugang verwendet werden. Der Verbindungstest speichert
    // den sichtbaren Formularstand unmittelbar vor dem Test; ORDER BY id hatte
    // bislang dagegen das älteste Profil gewählt (z.B. altes "admin"-Profil).
    var rows=jdbc.query("SELECT id,type,name,host,enabled,username,auth_mode,COALESCE(password,''),COALESCE(key_path,''),port,use_https,pairing_port,COALESCE(pairing_code,''),COALESCE(identity_key,'') FROM gam_os_access_profile WHERE type=? AND enabled=TRUE AND LOWER(host)=LOWER(?) ORDER BY updated_at DESC, id DESC LIMIT 1",
      (rs,n)->new SecretProfile(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getInt(10),rs.getBoolean(11),(Integer)rs.getObject(12),rs.getString(13),rs.getString(14)),t,h);
    return rows.isEmpty()?null:rows.get(0);
  }

  /** 40k36t88: Zielbewusste Auflösung für echte Geräteinventarisierung.
   * Priorität: explizite Identity-Bindung -> IP/Host -> Hostname-Hinweis.
   * Damit verwendet die Inventarisierung denselben Zugang wie die Konfiguration,
   * auch wenn dort ein Hostname und im Geräteverzeichnis eine IP steht. */
  public SecretProfile resolveTarget(String type,String host,String hostname,String identityKey){
    String t=clean(type).toUpperCase();
    String h=clean(host), hn=clean(hostname), ik=clean(identityKey);
    if(!ik.isBlank()){
      var byIdentity=jdbc.query("SELECT id,type,name,host,enabled,username,auth_mode,COALESCE(password,''),COALESCE(key_path,''),port,use_https,pairing_port,COALESCE(pairing_code,''),COALESCE(identity_key,'') FROM gam_os_access_profile WHERE type=? AND enabled=TRUE AND LOWER(COALESCE(identity_key,''))=LOWER(?) ORDER BY updated_at DESC,id DESC LIMIT 1",
        (rs,n)->new SecretProfile(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getInt(10),rs.getBoolean(11),(Integer)rs.getObject(12),rs.getString(13),rs.getString(14)),t,ik);
      if(!byIdentity.isEmpty()) return byIdentity.get(0);
    }
    if(!h.isBlank()){
      SecretProfile p=resolve(t,h);
      if(p!=null) return p;
    }
    if(!hn.isBlank()){
      SecretProfile p=resolve(t,hn);
      if(p!=null) return p;
    }
    return null;
  }

  public Profile save(Update u,String actor){
    String type=clean(u.type()).toUpperCase();
    if(!List.of("SSH","WINRM","ADB").contains(type)) throw new IllegalArgumentException("Unbekannter Betriebssystemzugang: "+type);
    Long id=u.id(); SecretProfile old=id==null?null:find(id);
    String password=Boolean.TRUE.equals(u.clearPassword())?"":(u.password()==null||u.password().isBlank()?(old==null?"":old.password()):u.password());
    String pairing=Boolean.TRUE.equals(u.clearPairingCode())?"":(u.pairingCode()==null||u.pairingCode().isBlank()?(old==null?"":old.pairingCode()):u.pairingCode());
    int def="WINRM".equals(type)?(Boolean.TRUE.equals(u.https())?5986:5985):"SSH".equals(type)?22:5555;
    int port=u.port()!=null&&u.port()>0&&u.port()<=65535?u.port():def;
    if(id==null){
      jdbc.update("INSERT INTO gam_os_access_profile(type,name,host,enabled,username,auth_mode,password,key_path,port,use_https,pairing_port,pairing_code,identity_key,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        type,clean(u.name()),clean(u.host()),u.enabled()==null||u.enabled(),clean(u.username()),clean(u.authMode()),password,clean(u.keyPath()),port,Boolean.TRUE.equals(u.https()),u.pairingPort(),pairing,clean(u.identityKey()),actor);
      id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
    } else {
      jdbc.update("UPDATE gam_os_access_profile SET type=?,name=?,host=?,enabled=?,username=?,auth_mode=?,password=?,key_path=?,port=?,use_https=?,pairing_port=?,pairing_code=?,identity_key=?,updated_by=? WHERE id=?",
        type,clean(u.name()),clean(u.host()),u.enabled()==null||u.enabled(),clean(u.username()),clean(u.authMode()),password,clean(u.keyPath()),port,Boolean.TRUE.equals(u.https()),u.pairingPort(),pairing,clean(u.identityKey()),actor,id);
    }
    long finalId=id;
    Profile saved=list().stream().filter(p->p.id()==finalId).findFirst().orElseThrow();
    log.info("[OS-ACCESS-PERSIST] Phase=SAVE Id={} Type={} Host={} Enabled={} -- dauerhaft in gam_os_access_profile gespeichert",saved.id(),saved.type(),saved.host(),saved.enabled());
    return saved;
  }

  public void delete(long id){ jdbc.update("DELETE FROM gam_os_access_profile WHERE id=?",id); log.info("[OS-ACCESS-PERSIST] Phase=DELETE Id={} -- aus gam_os_access_profile entfernt",id); }
  private static String clean(String v){ return v==null?"":v.trim(); }
}
