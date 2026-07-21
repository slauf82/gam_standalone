package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class FritzBoxSettingsRepository {
  private final JdbcTemplate jdbc;
  public FritzBoxSettingsRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

  @PostConstruct void init(){
    jdbc.execute("CREATE TABLE IF NOT EXISTS gam_fritzbox_sources (id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(120) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, host VARCHAR(255) NOT NULL, port INT NOT NULL DEFAULT 49000, username VARCHAR(255) NULL, password TEXT NULL, location VARCHAR(255) NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(id))");
    migrateLegacySettings();
  }

  private void migrateLegacySettings(){
    try{
      Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM gam_fritzbox_sources",Integer.class);
      if(count!=null&&count>0)return;
      Integer legacy=jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='gam_settings'",Integer.class);
      if(legacy==null||legacy==0)return;
      String host=getLegacy("device.discovery.fritz.host","fritz.box");
      String port=getLegacy("device.discovery.fritz.port","49000");
      String user=getLegacy("device.discovery.fritz.username","");
      String password=getLegacy("device.discovery.fritz.password","");
      String enabled=getLegacy("device.discovery.fritz.enabled","false");
      jdbc.update("INSERT INTO gam_fritzbox_sources(name,enabled,host,port,username,password,location,updated_by) VALUES('FRITZ!Box 1',?,?,?,?,?,'','migration')",Boolean.parseBoolean(enabled),host,parseInt(port,49000),user,password);
    }catch(Exception ignored){}
  }

  public List<FritzBoxSettingsView> loadViews(){return jdbc.query("SELECT id,name,enabled,host,port,COALESCE(username,''),COALESCE(password,''),COALESCE(location,'') FROM gam_fritzbox_sources ORDER BY name,id",(rs,n)->new FritzBoxSettingsView(rs.getLong(1),rs.getString(2),rs.getBoolean(3),rs.getString(4),rs.getInt(5),rs.getString(6),!rs.getString(7).isBlank(),rs.getString(8)));}
  public List<FritzBoxSettings> loadEnabledInternal(){return loadInternal(null).stream().filter(FritzBoxSettings::enabled).toList();}
  public FritzBoxSettings loadOne(long id){return loadInternal(id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("FRITZ!Box-Quelle nicht gefunden: "+id));}

  private List<FritzBoxSettings> loadInternal(Long id){
    String sql="SELECT id,name,enabled,host,port,COALESCE(username,''),COALESCE(password,''),COALESCE(location,'') FROM gam_fritzbox_sources"+(id==null?" ORDER BY name,id":" WHERE id=?");
    return id==null?jdbc.query(sql,(rs,n)->map(rs)):jdbc.query(sql,(rs,n)->map(rs),id);
  }
  private FritzBoxSettings map(java.sql.ResultSet rs) throws java.sql.SQLException{return new FritzBoxSettings(rs.getLong(1),rs.getString(2),rs.getBoolean(3),rs.getString(4),rs.getInt(5),rs.getString(6),rs.getString(7),rs.getString(8));}

  public FritzBoxSettingsView create(FritzBoxSettingsUpdate update,String actor){
    java.sql.ResultSet[] keyHolder=new java.sql.ResultSet[1];
    org.springframework.jdbc.support.GeneratedKeyHolder kh=new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO gam_fritzbox_sources(name,enabled,host,port,username,password,location,updated_by) VALUES(?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setString(1,clean(update.name(),"Neue FRITZ!Box"));ps.setBoolean(2,update.enabled());ps.setString(3,clean(update.host(),"fritz.box"));ps.setInt(4,update.port()>0?update.port():49000);ps.setString(5,clean(update.username(),""));ps.setString(6,update.password()==null?"":update.password());ps.setString(7,clean(update.location(),""));ps.setString(8,actor);return ps;},kh);
    long id=kh.getKey().longValue();return view(loadOne(id));
  }
  public FritzBoxSettingsView save(long id,FritzBoxSettingsUpdate update,String actor){
    FritzBoxSettings before=loadOne(id);String password=before.password();if(update.password()!=null&&!update.password().isBlank())password=update.password();else if(update.clearPassword())password="";
    jdbc.update("UPDATE gam_fritzbox_sources SET name=?,enabled=?,host=?,port=?,username=?,password=?,location=?,updated_by=? WHERE id=?",clean(update.name(),before.name()),update.enabled(),clean(update.host(),"fritz.box"),update.port()>0?update.port():49000,clean(update.username(),""),password,clean(update.location(),""),actor,id);return view(loadOne(id));
  }
  public void delete(long id){jdbc.update("DELETE FROM gam_fritzbox_sources WHERE id=?",id);}
  private FritzBoxSettingsView view(FritzBoxSettings s){return new FritzBoxSettingsView(s.id(),s.name(),s.enabled(),s.host(),s.port(),s.username(),!s.password().isBlank(),s.location());}
  private String getLegacy(String key,String fallback){List<String> r=jdbc.query("SELECT setting_value FROM gam_settings WHERE setting_key=?",(rs,n)->rs.getString(1),key);return r.isEmpty()||r.get(0)==null?fallback:r.get(0);}
  private static int parseInt(String value,int fallback){try{return Integer.parseInt(value);}catch(Exception ignored){return fallback;}}
  private static String clean(String value,String fallback){return value==null||value.isBlank()?fallback:value.trim();}

  public record FritzBoxSettings(long id,String name,boolean enabled,String host,int port,String username,String password,String location){}
  public record FritzBoxSettingsView(long id,String name,boolean enabled,String host,int port,String username,boolean passwordConfigured,String location){}
  public record FritzBoxSettingsUpdate(String name,boolean enabled,String host,int port,String username,String password,boolean clearPassword,String location){}
}
