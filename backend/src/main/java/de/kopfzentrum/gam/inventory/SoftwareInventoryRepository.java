package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 40k36q9: Plattformneutrales, strukturiertes Softwareinventar.
 * macOS ist der erste Nutzer; Windows/Linux können später dieselbe Tabelle verwenden.
 */
@Repository
public class SoftwareInventoryRepository {
  private final JdbcTemplate jdbc;
  public SoftwareInventoryRepository(JdbcTemplate jdbc){ this.jdbc=jdbc; }

  @PostConstruct void init(){
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_discovery_software_inventory (
        identity_key VARCHAR(255) NOT NULL,
        platform VARCHAR(32) NOT NULL,
        scope_type VARCHAR(32) NOT NULL,
        scope_name VARCHAR(191) NOT NULL DEFAULT '',
        software_key CHAR(64) NOT NULL,
        display_name VARCHAR(512) NOT NULL,
        version VARCHAR(255) NULL,
        bundle_or_package VARCHAR(512) NULL,
        install_path TEXT NULL,
        source VARCHAR(128) NULL,
        first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        removed_at TIMESTAMP NULL,
        PRIMARY KEY(identity_key, platform, scope_type, scope_name, software_key),
        INDEX idx_software_identity(identity_key, platform, removed_at),
        INDEX idx_software_scope(identity_key, scope_type, scope_name)
      )
      """);
  }

  public List<Map<String,Object>> current(String identityKey){
    return jdbc.queryForList("""
      SELECT platform, scope_type AS scopeType, scope_name AS scopeName,
             software_key AS softwareKey, display_name AS displayName, version,
             bundle_or_package AS bundleOrPackage, install_path AS installPath, source,
             first_seen_at AS firstSeenAt, last_seen_at AS lastSeenAt
      FROM gam_discovery_software_inventory
      WHERE identity_key=? AND removed_at IS NULL
      ORDER BY CASE WHEN scope_type='SYSTEM' THEN 0 ELSE 1 END, scope_name, display_name, version
      """, identityKey);
  }

  public int replaceAppleMobile(String identityKey, String platform, java.util.List<AppleMobileDiscoveryService.SoftwareItem> items){
    List<Row> rows=new ArrayList<>();
    if(items!=null) for(var item:items){
      if(item==null||clean(item.name())==null) continue;
      String scope="SYSTEM".equalsIgnoreCase(item.scopeType())?"SYSTEM":"USER";
      rows.add(new Row(scope,"",key(item.name(),item.bundleId(),item.installPath(),item.source()),item.name(),cleanDash(item.version()),cleanDash(item.bundleId()),cleanDash(item.installPath()),cleanDash(item.source())));
    }
    replace(identityKey,"iPadOS".equals(platform)?"iPadOS":"iOS",rows);
    return rows.size();
  }

  public void replaceMacOs(String identityKey, java.util.List<MacOsInventoryService.SoftwareItem> items){
    List<Row> rows=new ArrayList<>();
    if(items!=null) for(var item:items){
      if(item==null||clean(item.name())==null) continue;
      String scope="USER".equalsIgnoreCase(item.scopeType())?"USER":"SYSTEM";
      String scopeName="USER".equals(scope)?Objects.toString(clean(item.scopeName()),""):"";
      rows.add(new Row(scope,scopeName,key(item.name(),item.bundleOrPackage(),item.installPath(),item.source()),item.name(),cleanDash(item.version()),cleanDash(item.bundleOrPackage()),cleanDash(item.installPath()),cleanDash(item.source())));
    }
    replace(identityKey,"macOS",rows);
  }

  public void replaceMacOsFromProtocol(String identityKey,String protocol){
    List<Row> rows=new ArrayList<>();
    String system=detail(protocol,"Systemsoftware-Details");
    for(String item:splitRows(system)){
      String[] c=item.split("\\|",-1);
      if(c.length<1||clean(c[0])==null)continue;
      String name=clean(c[0]), version=at(c,1), bundle=at(c,2), path=at(c,3), source=at(c,4);
      rows.add(new Row("SYSTEM","",key(name,bundle,path,source),name,version,bundle,path,source));
    }
    String users=detail(protocol,"Benutzersoftware-Details");
    for(String item:splitRows(users)){
      String[] c=item.split("\\|",-1);
      if(c.length<2||clean(c[0])==null||clean(c[1])==null)continue;
      String user=clean(c[0]), name=clean(c[1]), version=at(c,2), bundle=at(c,3), path=at(c,4), source=at(c,5);
      rows.add(new Row("USER",user,key(name,bundle,path,source),name,version,bundle,path,source));
    }
    replace(identityKey,"macOS",rows);
  }

  /** 40k36t: Legacy-Import aus älteren Protokollen. */
  public void replaceLinuxFromProtocol(String identityKey,String protocol){
    replaceLinuxFromDetails(identityKey,detail(protocol,"Software-Details"));
  }

  /** 40k36t3: Direkter Import ohne Umweg über das Discovery-Protokoll. */
  public int replaceLinuxFromDetails(String identityKey,String value){
    List<Row> rows=new ArrayList<>();
    if(value!=null){
      for(String item:value.split("\\u001e")){
        String line=item.trim(); if(line.isBlank())continue;
        String[] c=line.split("\\|",-1); if(c.length<2)continue;
        String source=clean(c[0]),name=clean(c[1]),version=c.length>2?cleanDash(c[2]):null,pkg=c.length>3?cleanDash(c[3]):null,path=c.length>4?cleanDash(c[4]):null;
        if(name==null)continue;
        rows.add(new Row("SYSTEM","",key(name,pkg,path,source),name,version,pkg,path,source));
      }
    }
    replace(identityKey,"Linux",rows);
    return rows.size();
  }

  /** 40k36t45: Direkter Import der per WinRM ermittelten Windows-Software. */
  public int replaceWindowsFromDetails(String identityKey,String value){
    List<Row> rows=new ArrayList<>();
    if(value!=null){
      for(String item:value.split("\\u001e")){
        String line=item.trim(); if(line.isBlank()) continue;
        String[] c=line.split("\\|",-1); if(c.length<3) continue;
        String scope="USER".equalsIgnoreCase(clean(c[0]))?"USER":"SYSTEM";
        String scopeName="USER".equals(scope)?Objects.toString(clean(c[1]),""):"";
        String name=clean(c[2]); if(name==null) continue;
        String version=c.length>3?cleanDash(c[3]):null;
        String pkg=c.length>4?cleanDash(c[4]):null;
        String path=c.length>5?cleanDash(c[5]):null;
        String source=c.length>6?cleanDash(c[6]):null;
        rows.add(new Row(scope,scopeName,key(name,pkg,path,source),name,version,pkg,path,source));
      }
    }
    replace(identityKey,"Windows",rows);
    return rows.size();
  }

  private static final int BULK_ROWS=250;

  /**
   * 40k36t6: Echter SQL-Multirow-Upsert statt treiberabhängigem JDBC-batchUpdate.
   * Ohne rewriteBatchedStatements hat der MySQL/MariaDB-Treiber die 3742 Linux-
   * Pakete faktisch als tausende Einzelstatements übertragen (~374 s im Realtest).
   * Hier werden pro Chunk bis zu 250 Datensätze in EINEM INSERT/UPSERT gesendet.
   */
  private void replace(String identityKey,String platform,List<Row> rows){
    jdbc.update("UPDATE gam_discovery_software_inventory SET removed_at=CURRENT_TIMESTAMP WHERE identity_key=? AND platform=? AND removed_at IS NULL",identityKey,platform);
    if(rows.isEmpty()) return;

    // Doppelte Schlüssel innerhalb eines Inventarlaufs zusammenfassen. Die letzte
    // Sicht gewinnt; Reihenfolge bleibt für reproduzierbare SQL-Chunks stabil.
    LinkedHashMap<String,Row> unique=new LinkedHashMap<>();
    for(Row row:rows) unique.put(row.scopeType()+"\u0000"+row.scopeName()+"\u0000"+row.key(),row);
    List<Row> compact=new ArrayList<>(unique.values());

    for(int start=0;start<compact.size();start+=BULK_ROWS){
      int end=Math.min(compact.size(),start+BULK_ROWS);
      List<Row> chunk=compact.subList(start,end);
      StringBuilder sql=new StringBuilder("""
        INSERT INTO gam_discovery_software_inventory
          (identity_key,platform,scope_type,scope_name,software_key,display_name,version,bundle_or_package,install_path,source,first_seen_at,last_seen_at,removed_at)
        VALUES
        """);
      List<Object> args=new ArrayList<>(chunk.size()*10);
      for(int i=0;i<chunk.size();i++){
        if(i>0) sql.append(',');
        sql.append("(?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL)");
        Row row=chunk.get(i);
        args.add(identityKey); args.add(platform); args.add(row.scopeType()); args.add(row.scopeName());
        args.add(row.key()); args.add(row.name()); args.add(row.version()); args.add(row.bundle());
        args.add(row.path()); args.add(row.source());
      }
      sql.append("""
        ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),version=VALUES(version),
          bundle_or_package=VALUES(bundle_or_package),install_path=VALUES(install_path),source=VALUES(source),
          last_seen_at=CURRENT_TIMESTAMP,removed_at=NULL
        """);
      jdbc.update(sql.toString(),args.toArray());
    }
  }

  private record Row(String scopeType,String scopeName,String key,String name,String version,String bundle,String path,String source){}
  private static List<String> splitRows(String value){
    String v=clean(value); if(v==null)return List.of();
    return Arrays.stream(v.split("\\s+;\\s+")).map(String::trim).filter(x->!x.isBlank()).toList();
  }
  private static String detail(String protocol,String label){
    if(protocol==null)return null;
    String prefix=label+": ";
    for(String token:protocol.split("\\s+·\\s+")){
      if(token.startsWith(prefix))return token.substring(prefix.length()).trim();
    }
    return null;
  }
  private static String key(String name,String bundle,String path,String source){
    String raw=String.join("|",Objects.toString(name,""),Objects.toString(bundle,""),Objects.toString(path,""),Objects.toString(source,"")).toLowerCase(Locale.ROOT);
    try{
      byte[] digest=MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
      StringBuilder b=new StringBuilder(64); for(byte x:digest)b.append(String.format("%02x",x)); return b.toString();
    }catch(Exception e){return Integer.toHexString(raw.hashCode());}
  }
  private static String at(String[] c,int i){return i<c.length?cleanDash(c[i]):null;}
  private static String cleanDash(String v){String x=clean(v);return x==null||"-".equals(x)?null:x;}
  private static String clean(String v){if(v==null)return null;String x=v.trim();return x.isEmpty()?null:x;}
}
