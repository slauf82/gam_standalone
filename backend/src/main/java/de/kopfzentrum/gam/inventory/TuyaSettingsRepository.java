package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class TuyaSettingsRepository {
  private final JdbcTemplate jdbc;
  public TuyaSettingsRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

  @PostConstruct void init(){
    jdbc.execute("CREATE TABLE IF NOT EXISTS gam_tuya_sources (id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(120) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, connection_mode VARCHAR(30) NOT NULL DEFAULT 'HOME_ASSISTANT', homeassistant_source_id BIGINT NULL, app_type VARCHAR(30) NOT NULL DEFAULT 'SMART_LIFE', region VARCHAR(30) NOT NULL DEFAULT 'EUROPE', client_id VARCHAR(255) NOT NULL, client_secret TEXT NULL, user_uid VARCHAR(255) NULL, account_username VARCHAR(255) NULL, account_password TEXT NULL, country_code VARCHAR(12) NULL, app_schema VARCHAR(80) NULL, location VARCHAR(255) NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(id))");
    add("user_uid","ALTER TABLE gam_tuya_sources ADD COLUMN user_uid VARCHAR(255) NULL AFTER client_secret");
    add("connection_mode","ALTER TABLE gam_tuya_sources ADD COLUMN connection_mode VARCHAR(30) NOT NULL DEFAULT 'DIRECT_CLOUD' AFTER enabled");
    add("homeassistant_source_id","ALTER TABLE gam_tuya_sources ADD COLUMN homeassistant_source_id BIGINT NULL AFTER connection_mode");
    add("account_username","ALTER TABLE gam_tuya_sources ADD COLUMN account_username VARCHAR(255) NULL AFTER user_uid");
    add("account_password","ALTER TABLE gam_tuya_sources ADD COLUMN account_password TEXT NULL AFTER account_username");
    add("country_code","ALTER TABLE gam_tuya_sources ADD COLUMN country_code VARCHAR(12) NULL AFTER account_password");
    add("app_schema","ALTER TABLE gam_tuya_sources ADD COLUMN app_schema VARCHAR(80) NULL AFTER country_code");
  }
  private void add(String column,String ddl){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gam_tuya_sources' AND COLUMN_NAME=?",Integer.class,column);if(n!=null&&n==0)jdbc.execute(ddl);}

  public List<View> loadViews(){return jdbc.query("SELECT id,name,enabled,COALESCE(connection_mode,'DIRECT_CLOUD'),homeassistant_source_id,app_type,region,client_id,COALESCE(client_secret,''),COALESCE(user_uid,''),COALESCE(account_username,''),COALESCE(account_password,''),COALESCE(country_code,'49'),COALESCE(app_schema,CASE WHEN app_type='TUYA_SMART' THEN 'tuyaSmart' ELSE 'smartlife' END),COALESCE(location,'') FROM gam_tuya_sources ORDER BY name,id",(rs,n)->new View(rs.getLong(1),rs.getString(2),rs.getBoolean(3),rs.getString(4),(Long)rs.getObject(5),rs.getString(6),rs.getString(7),rs.getString(8),!rs.getString(9).isBlank(),rs.getString(10),rs.getString(11),!rs.getString(12).isBlank(),rs.getString(13),rs.getString(14),rs.getString(15)));}
  public List<Settings> loadEnabledInternal(){return loadInternal(null).stream().filter(Settings::enabled).toList();}
  public Settings loadOne(long id){return loadInternal(id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Smart-Life-/Tuya-Quelle nicht gefunden: "+id));}
  private List<Settings> loadInternal(Long id){String sql="SELECT id,name,enabled,COALESCE(connection_mode,'DIRECT_CLOUD'),homeassistant_source_id,app_type,region,client_id,COALESCE(client_secret,''),COALESCE(user_uid,''),COALESCE(account_username,''),COALESCE(account_password,''),COALESCE(country_code,'49'),COALESCE(app_schema,CASE WHEN app_type='TUYA_SMART' THEN 'tuyaSmart' ELSE 'smartlife' END),COALESCE(location,'') FROM gam_tuya_sources"+(id==null?" ORDER BY name,id":" WHERE id=?");return id==null?jdbc.query(sql,(rs,n)->map(rs)):jdbc.query(sql,(rs,n)->map(rs),id);}
  private Settings map(java.sql.ResultSet rs)throws java.sql.SQLException{return new Settings(rs.getLong(1),rs.getString(2),rs.getBoolean(3),rs.getString(4),(Long)rs.getObject(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10),rs.getString(11),rs.getString(12),rs.getString(13),rs.getString(14),rs.getString(15));}

  public View create(Update u,String actor){var kh=new org.springframework.jdbc.support.GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO gam_tuya_sources(name,enabled,connection_mode,homeassistant_source_id,app_type,region,client_id,client_secret,user_uid,account_username,account_password,country_code,app_schema,location,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setString(1,clean(u.name(),"Smart Life"));ps.setBoolean(2,u.enabled());ps.setString(3,mode(u.connectionMode()));if(u.homeAssistantSourceId()==null)ps.setNull(4,java.sql.Types.BIGINT);else ps.setLong(4,u.homeAssistantSourceId());ps.setString(5,app(u.appType()));ps.setString(6,region(u.region()));ps.setString(7,clean(u.clientId(),""));ps.setString(8,u.clientSecret()==null?"":u.clientSecret().trim());ps.setString(9,clean(u.userUid(),""));ps.setString(10,clean(u.accountUsername(),""));ps.setString(11,u.accountPassword()==null?"":u.accountPassword());ps.setString(12,clean(u.countryCode(),"49"));ps.setString(13,clean(u.appSchema(),schema(app(u.appType()))));ps.setString(14,clean(u.location(),""));ps.setString(15,actor);return ps;},kh);return view(loadOne(kh.getKey().longValue()));}
  public View save(long id,Update u,String actor){Settings b=loadOne(id);String secret=b.clientSecret();if(u.clientSecret()!=null&&!u.clientSecret().isBlank())secret=u.clientSecret().trim();else if(u.clearClientSecret())secret="";String password=b.accountPassword();if(u.accountPassword()!=null&&!u.accountPassword().isBlank())password=u.accountPassword();else if(u.clearAccountPassword())password="";jdbc.update("UPDATE gam_tuya_sources SET name=?,enabled=?,connection_mode=?,homeassistant_source_id=?,app_type=?,region=?,client_id=?,client_secret=?,user_uid=?,account_username=?,account_password=?,country_code=?,app_schema=?,location=?,updated_by=? WHERE id=?",clean(u.name(),b.name()),u.enabled(),mode(u.connectionMode()),u.homeAssistantSourceId(),app(u.appType()),region(u.region()),clean(u.clientId(),b.clientId()),secret,clean(u.userUid(),b.userUid()),clean(u.accountUsername(),b.accountUsername()),password,clean(u.countryCode(),b.countryCode()),clean(u.appSchema(),schema(app(u.appType()))),clean(u.location(),""),actor,id);return view(loadOne(id));}
  public DeleteResult delete(long id){
    Settings existing=loadInternal(id).stream().findFirst().orElse(null);
    if(existing==null)return new DeleteResult(id,false,"Die Smart-Life-/Tuya-Konfiguration war bereits entfernt.");
    int deleted=jdbc.update("DELETE FROM gam_tuya_sources WHERE id=?",id);
    return new DeleteResult(id,deleted>0,deleted>0?"Smart-Life-/Tuya-Konfiguration wurde vollständig entfernt.":"Konfiguration konnte nicht entfernt werden.");
  }
  public DeleteAllResult deleteAll(){
    int count=jdbc.update("DELETE FROM gam_tuya_sources");
    return new DeleteAllResult(count, count==1?"Eine Smart-Life-/Tuya-Konfiguration wurde entfernt.":count+" Smart-Life-/Tuya-Konfigurationen wurden entfernt.");
  }
  private View view(Settings s){return new View(s.id(),s.name(),s.enabled(),s.connectionMode(),s.homeAssistantSourceId(),s.appType(),s.region(),s.clientId(),!s.clientSecret().isBlank(),s.userUid(),s.accountUsername(),!s.accountPassword().isBlank(),s.countryCode(),s.appSchema(),s.location());}
  private static String mode(String v){String x=clean(v,"ACCOUNT_LOGIN").toUpperCase();return switch(x){case "DIRECT_CLOUD","HOME_ASSISTANT","ACCOUNT_LOGIN"->x;default->"ACCOUNT_LOGIN";};}
  private static String app(String v){return "TUYA_SMART".equalsIgnoreCase(v)?"TUYA_SMART":"SMART_LIFE";}
  private static String schema(String app){return "TUYA_SMART".equals(app)?"tuyaSmart":"smartlife";}
  private static String region(String v){String x=clean(v,"EUROPE").toUpperCase();return switch(x){case "AMERICA","CHINA","INDIA","WESTERN_AMERICA","EASTERN_AMERICA","EUROPE"->x;default->"EUROPE";};}
  private static String clean(String v,String f){return v==null||v.isBlank()?f:v.trim();}
  public record Settings(long id,String name,boolean enabled,String connectionMode,Long homeAssistantSourceId,String appType,String region,String clientId,String clientSecret,String userUid,String accountUsername,String accountPassword,String countryCode,String appSchema,String location){}
  public record View(long id,String name,boolean enabled,String connectionMode,Long homeAssistantSourceId,String appType,String region,String clientId,boolean clientSecretConfigured,String userUid,String accountUsername,boolean accountPasswordConfigured,String countryCode,String appSchema,String location){}
  public record Update(String name,boolean enabled,String connectionMode,Long homeAssistantSourceId,String appType,String region,String clientId,String clientSecret,boolean clearClientSecret,String userUid,String accountUsername,String accountPassword,boolean clearAccountPassword,String countryCode,String appSchema,String location){}
  public record DeleteResult(long id,boolean deleted,String message){}
  public record DeleteAllResult(int deletedCount,String message){}
}
