package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;

/** 40k36t46: explizite, versionsunabhaengig persistente Vertrauensfreigaben fuer WinRM-Ziele.
 *  Keine Wildcards: GAM speichert ausschliesslich konkrete Hosts/IPs. */
@Repository
public class WinRmTrustedTargetRepository {
  private static final Logger log=LoggerFactory.getLogger(WinRmTrustedTargetRepository.class);
  private final JdbcTemplate jdbc;
  public WinRmTrustedTargetRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

  public record TrustedTarget(String host, String createdAt, String createdBy) {}

  private final Path mirrorFile=resolvePersistentMirrorFile();

  private static Path resolvePersistentMirrorFile(){
    String configured=System.getenv("GAM_DATA_DIR");
    if(configured!=null && !configured.isBlank())
      return Path.of(configured).toAbsolutePath().normalize().resolve("winrm-trusted-targets.txt");
    String home=System.getProperty("user.home","");
    if(home!=null && !home.isBlank())
      return Path.of(home).toAbsolutePath().normalize().resolve(".gam").resolve("winrm-trusted-targets.txt");
    return Path.of("data").toAbsolutePath().normalize().resolve("winrm-trusted-targets.txt");
  }

  @PostConstruct void init(){
    jdbc.execute("CREATE TABLE IF NOT EXISTS gam_winrm_trusted_targets (host VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, created_by VARCHAR(255) NULL, PRIMARY KEY(host))");
    migrateLegacyMirror();
    importMirror();
    exportMirror();
    log.info("[WINRM-TRUST] Phase=STARTUP TrustedTargets={} Mirror={}", list().size(), mirrorFile);
  }

  private void migrateLegacyMirror(){
    try{
      Path legacy=Path.of("data","winrm-trusted-targets.txt").toAbsolutePath().normalize();
      if(legacy.equals(mirrorFile) || !Files.isRegularFile(legacy) || Files.isRegularFile(mirrorFile)) return;
      Files.createDirectories(mirrorFile.getParent());
      Files.copy(legacy,mirrorFile);
    }catch(Exception ignored){}
  }

  /**
   * 40k36t46: Trust bleibt primär in MariaDB gespeichert. Zusätzlich wird nur die
   * Liste der freigegebenen Hosts/IPs (keine Kennwörter) in data/ gespiegelt.
   * Dadurch überlebt die explizite Zielfreigabe auch normale Versionskopien und
   * kann bei einer versehentlich neu erzeugten Runtime-Tabelle wiederhergestellt
   * werden. Ein bewusster Datenbank-/Datenverzeichnis-Reset entfernt sie weiterhin.
   */
  private synchronized void importMirror(){
    if(!Files.isRegularFile(mirrorFile)) return;
    try{
      int imported=0;
      for(String raw:Files.readAllLines(mirrorFile,StandardCharsets.UTF_8)){
        String h=clean(raw);
        if(h.isBlank() || h.startsWith("#") || "*".equals(h) || h.contains(",") || h.contains(";")) continue;
        jdbc.update("INSERT INTO gam_winrm_trusted_targets(host,created_by) VALUES(?,?) ON DUPLICATE KEY UPDATE host=VALUES(host)",h,"persistent-mirror");
        imported++;
      }
      log.info("[WINRM-TRUST] Persistente Vertrauensliste geladen: {} Ziele aus {}",imported,mirrorFile);
    }catch(Exception e){
      log.warn("[WINRM-TRUST] Persistente Vertrauensliste konnte nicht gelesen werden: {} ({})",mirrorFile,e.getMessage());
    }
  }

  private synchronized void exportMirror(){
    try{
      Files.createDirectories(mirrorFile.getParent());
      Set<String> hosts=new LinkedHashSet<>();
      for(TrustedTarget target:list()) if(target.host()!=null && !target.host().isBlank()) hosts.add(target.host().trim());
      Path tmp=mirrorFile.resolveSibling(mirrorFile.getFileName()+".tmp");
      Files.write(tmp,hosts,StandardCharsets.UTF_8);
      try{ Files.move(tmp,mirrorFile,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
      catch(Exception atomic){ Files.move(tmp,mirrorFile,StandardCopyOption.REPLACE_EXISTING); }
      log.info("[WINRM-TRUST] Vertrauensliste dauerhaft gespeichert: {} Ziele in {}",hosts.size(),mirrorFile);
    }catch(Exception e){
      log.warn("[WINRM-TRUST] Vertrauensliste konnte NICHT dauerhaft gespeichert werden: {} ({})",mirrorFile,e.getMessage());
    }
  }

  public boolean isTrusted(String host){
    String h=clean(host); if(h.isBlank()) return false;
    Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM gam_winrm_trusted_targets WHERE LOWER(host)=LOWER(?)",Integer.class,h);
    if(n!=null&&n>0) return true;
    // 40k36t46: Falls ein Versionswechsel/DB-Neuaufbau die Laufzeittabelle leert,
    // vor der Entscheidung einmal aus dem versionsunabhaengigen Mirror selbst heilen.
    importMirror();
    n=jdbc.queryForObject("SELECT COUNT(*) FROM gam_winrm_trusted_targets WHERE LOWER(host)=LOWER(?)",Integer.class,h);
    return n!=null&&n>0;
  }

  public TrustedTarget trust(String host,String actor){
    String h=clean(host); if(h.isBlank()) throw new IllegalArgumentException("Host/IP fehlt.");
    if("*".equals(h)||h.contains(",")||h.contains(";")) throw new IllegalArgumentException("Nur ein konkreter Host oder eine konkrete IP-Adresse darf freigegeben werden; Wildcards/Listen sind nicht erlaubt.");
    jdbc.update("INSERT INTO gam_winrm_trusted_targets(host,created_by) VALUES(?,?) ON DUPLICATE KEY UPDATE created_by=VALUES(created_by)",h,actor);
    exportMirror();
    return new TrustedTarget(h, OffsetDateTime.now().toString(), actor);
  }

  public void revoke(String host){
    String h=clean(host);
    if(!h.isBlank()) jdbc.update("DELETE FROM gam_winrm_trusted_targets WHERE LOWER(host)=LOWER(?)",h);
    exportMirror();
  }
  public List<TrustedTarget> list(){return jdbc.query("SELECT host,CAST(created_at AS CHAR),COALESCE(created_by,'') FROM gam_winrm_trusted_targets ORDER BY host",(rs,n)->new TrustedTarget(rs.getString(1),rs.getString(2),rs.getString(3)));}
  private static String clean(String v){return v==null?"":v.trim();}
}
