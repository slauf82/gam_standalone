package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StartupInventorySettingsRepository {
  private final JdbcTemplate jdbc;
  public StartupInventorySettingsRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

  @PostConstruct void init(){
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_inventory_startup_settings (
        id INT NOT NULL PRIMARY KEY,
        enabled BOOLEAN NOT NULL DEFAULT TRUE,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        updated_by VARCHAR(255) NULL
      )
      """);
    jdbc.update("INSERT IGNORE INTO gam_inventory_startup_settings(id,enabled,updated_by) VALUES(1,TRUE,'system')");
  }

  public record Settings(boolean enabled){}
  public Settings load(){
    Boolean v=jdbc.queryForObject("SELECT enabled FROM gam_inventory_startup_settings WHERE id=1",Boolean.class);
    return new Settings(v==null||v);
  }
  public Settings save(boolean enabled,String actor){
    jdbc.update("UPDATE gam_inventory_startup_settings SET enabled=?,updated_by=? WHERE id=1",enabled,actor);
    return load();
  }
}
