package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 40k35i: zentrale, datenbankgestützte SSH-Konfiguration für Linux und künftig macOS. */
@Repository
public class LinuxSshSettingsRepository {
  private final JdbcTemplate jdbc;
  private final Environment env;
  public LinuxSshSettingsRepository(JdbcTemplate jdbc, Environment env){this.jdbc=jdbc;this.env=env;}

  public record View(boolean enabled,String username,String authMode,boolean passwordConfigured,String keyPath,int port){}
  public record Settings(boolean enabled,String username,String authMode,String password,String keyPath,int port){}
  public record Update(boolean enabled,String username,String authMode,String password,Boolean clearPassword,String keyPath,int port){}

  @PostConstruct void init(){
    jdbc.execute("CREATE TABLE IF NOT EXISTS gam_linux_ssh_settings (id INT NOT NULL, enabled BOOLEAN NOT NULL DEFAULT FALSE, username VARCHAR(255) NOT NULL DEFAULT '', auth_mode VARCHAR(32) NOT NULL DEFAULT 'KEY', password TEXT NULL, key_path TEXT NULL, port INT NOT NULL DEFAULT 22, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(id))");
    jdbc.update("INSERT IGNORE INTO gam_linux_ssh_settings(id,enabled,username,auth_mode,password,key_path,port,updated_by) VALUES(1,FALSE,'','KEY','','',22,'system')");
  }
  public View view(){Settings s=load();return new View(s.enabled(),s.username(),s.authMode(),!s.password().isBlank(),s.keyPath(),s.port());}
  public Settings load(){
    List<Settings> rows=jdbc.query("SELECT enabled,COALESCE(username,''),COALESCE(auth_mode,'KEY'),COALESCE(password,''),COALESCE(key_path,''),port FROM gam_linux_ssh_settings WHERE id=1",(rs,n)->new Settings(rs.getBoolean(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6)));
    Settings db=rows.isEmpty()?new Settings(false,"","KEY","","",22):rows.get(0);
    String envUser=setting("gam.discovery.linux.ssh-user","GAM_LINUX_SSH_USER","");
    if(!db.enabled()&&db.username().isBlank()&&!envUser.isBlank()) return new Settings(true,envUser,"KEY","",setting("gam.discovery.linux.ssh-key","GAM_LINUX_SSH_KEY",""),intSetting("gam.discovery.linux.ssh-port","GAM_LINUX_SSH_PORT",22));
    return db;
  }
  public View save(Update u,String actor){
    Settings old=load(); String mode=clean(u.authMode()).equalsIgnoreCase("PASSWORD")?"PASSWORD":"KEY";
    String password=Boolean.TRUE.equals(u.clearPassword())?"":(u.password()==null||u.password().isBlank()?old.password():u.password());
    int port=u.port()>0&&u.port()<=65535?u.port():22;
    jdbc.update("UPDATE gam_linux_ssh_settings SET enabled=?,username=?,auth_mode=?,password=?,key_path=?,port=?,updated_by=? WHERE id=1",u.enabled(),clean(u.username()),mode,password,clean(u.keyPath()),port,actor);
    return view();
  }
  private String setting(String property,String variable,String fallback){String v=env.getProperty(property);if(v==null||v.isBlank())v=System.getenv(variable);return v==null||v.isBlank()?fallback:v.trim();}
  private int intSetting(String property,String variable,int fallback){try{return Integer.parseInt(setting(property,variable,Integer.toString(fallback)));}catch(Exception e){return fallback;}}
  private static String clean(String v){return v==null?"":v.trim();}
}
