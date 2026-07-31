package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WinRmSettingsRepository {
  private final JdbcTemplate jdbc;
  private final Environment env;

  public WinRmSettingsRepository(JdbcTemplate jdbc, Environment env) {
    this.jdbc = jdbc;
    this.env = env;
  }

  public record View(boolean enabled, String username, boolean passwordConfigured, int port, boolean https) {}
  public record Settings(boolean enabled, String username, String password, int port, boolean https) {}
  public record Update(boolean enabled, String username, String password, Boolean clearPassword, int port, boolean https) {}

  @PostConstruct void init() {
    jdbc.execute("CREATE TABLE IF NOT EXISTS gam_winrm_settings (id INT NOT NULL, enabled BOOLEAN NOT NULL DEFAULT FALSE, username VARCHAR(255) NOT NULL DEFAULT '', password TEXT NULL, port INT NOT NULL DEFAULT 5985, use_https BOOLEAN NOT NULL DEFAULT FALSE, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(id))");
    jdbc.update("INSERT IGNORE INTO gam_winrm_settings(id,enabled,username,password,port,use_https,updated_by) VALUES(1,FALSE,'','',5985,FALSE,'system')");
  }

  public View view() {
    Settings s = load();
    return new View(s.enabled(), s.username(), !s.password().isBlank(), s.port(), s.https());
  }

  public Settings load() {
    List<Settings> rows = jdbc.query("SELECT enabled,COALESCE(username,''),COALESCE(password,''),port,use_https FROM gam_winrm_settings WHERE id=1",
      (rs,n) -> new Settings(rs.getBoolean(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getBoolean(5)));
    Settings db = rows.isEmpty() ? new Settings(false,"","",5985,false) : rows.get(0);
    String envUser = setting("gam.discovery.windows.winrm-user", "GAM_WINDOWS_WINRM_USER", "");
    if (!db.enabled() && db.username().isBlank() && !envUser.isBlank()) {
      return new Settings(true, envUser,
        setting("gam.discovery.windows.winrm-password", "GAM_WINDOWS_WINRM_PASSWORD", ""),
        intSetting("gam.discovery.windows.winrm-port", "GAM_WINDOWS_WINRM_PORT", 5985),
        Boolean.parseBoolean(setting("gam.discovery.windows.winrm-https", "GAM_WINDOWS_WINRM_HTTPS", "false")));
    }
    return db;
  }

  public View save(Update update, String actor) {
    Settings old = load();
    String password = Boolean.TRUE.equals(update.clearPassword()) ? "" :
      (update.password() == null || update.password().isBlank() ? old.password() : update.password());
    int port = update.port() > 0 && update.port() <= 65535 ? update.port() : (update.https() ? 5986 : 5985);
    jdbc.update("UPDATE gam_winrm_settings SET enabled=?,username=?,password=?,port=?,use_https=?,updated_by=? WHERE id=1",
      update.enabled(), clean(update.username()), password, port, update.https(), actor);
    return view();
  }

  private String setting(String property, String variable, String fallback) {
    String v = env.getProperty(property);
    if (v == null || v.isBlank()) v = System.getenv(variable);
    return v == null || v.isBlank() ? fallback : v.trim();
  }
  private int intSetting(String property,String variable,int fallback){try{return Integer.parseInt(setting(property,variable,Integer.toString(fallback)));}catch(Exception e){return fallback;}}
  private static String clean(String value){return value==null?"":value.trim();}
}
