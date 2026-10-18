package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.net.*;
import java.util.*;

/** 40k36q: halblokale Browser-Clients als echte, aber ausdrücklich vorläufige Discovery-Quelle. */
@Repository
public class BrowserClientDiscoveryRepository {
  private final JdbcTemplate jdbc;
  public BrowserClientDiscoveryRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

  public record Client(String clientId,String address,String platform,String platformVersion,String browser,String browserVersion,
                       String architecture,String model,String language,String timezone,String screen,String deviceClass,
                       Integer hardwareConcurrency,Double deviceMemory,boolean touch,String userAgent,String lastSeen) {}

  @PostConstruct void init(){
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS gam_browser_discovery_clients (
        client_id VARCHAR(120) NOT NULL,
        address VARCHAR(80) NOT NULL,
        platform VARCHAR(80) NULL,
        platform_version VARCHAR(80) NULL,
        browser VARCHAR(80) NULL,
        browser_version VARCHAR(80) NULL,
        architecture VARCHAR(80) NULL,
        model VARCHAR(255) NULL,
        language VARCHAR(80) NULL,
        timezone VARCHAR(120) NULL,
        screen VARCHAR(80) NULL,
        device_class VARCHAR(40) NULL,
        hardware_concurrency INT NULL,
        device_memory DOUBLE NULL,
        touch BOOLEAN NOT NULL DEFAULT FALSE,
        user_agent TEXT NULL,
        last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(client_id)
      )
      """);
  }

  public Client touch(String remoteAddress, Map<String,Object> body){
    String clientId=clean(body.get("clientId"));
    if(clientId.isBlank()) throw new IllegalArgumentException("Browser-Client-ID fehlt.");
    String address=normalizeRemoteAddress(remoteAddress);
    String platform=clean(body.get("platform"));
    String platformVersion=clean(body.get("platformVersion"));
    String browser=clean(body.get("browser"));
    String browserVersion=clean(body.get("browserVersion"));
    String architecture=clean(body.get("architecture"));
    String model=clean(body.get("model"));
    String language=clean(body.get("language"));
    String timezone=clean(body.get("timezone"));
    String screen=clean(body.get("screen"));
    String deviceClass=clean(body.get("deviceClass"));
    Integer cores=integer(body.get("hardwareConcurrency"));
    Double memory=decimal(body.get("deviceMemory"));
    boolean touch=Boolean.TRUE.equals(body.get("touch"));
    String userAgent=clean(body.get("userAgent"));
    jdbc.update("""
      INSERT INTO gam_browser_discovery_clients(client_id,address,platform,platform_version,browser,browser_version,architecture,model,language,timezone,screen,device_class,hardware_concurrency,device_memory,touch,user_agent,last_seen)
      VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
      ON DUPLICATE KEY UPDATE address=VALUES(address),platform=VALUES(platform),platform_version=VALUES(platform_version),browser=VALUES(browser),browser_version=VALUES(browser_version),architecture=VALUES(architecture),model=VALUES(model),language=VALUES(language),timezone=VALUES(timezone),screen=VALUES(screen),device_class=VALUES(device_class),hardware_concurrency=VALUES(hardware_concurrency),device_memory=VALUES(device_memory),touch=VALUES(touch),user_agent=VALUES(user_agent),last_seen=CURRENT_TIMESTAMP
      """,clientId,address,platform,platformVersion,browser,browserVersion,architecture,model,language,timezone,screen,deviceClass,cores,memory,touch,userAgent);
    return find(clientId);
  }

  public List<Client> recent(){
    return jdbc.query("""
      SELECT client_id,address,COALESCE(platform,''),COALESCE(platform_version,''),COALESCE(browser,''),COALESCE(browser_version,''),COALESCE(architecture,''),COALESCE(model,''),COALESCE(language,''),COALESCE(timezone,''),COALESCE(screen,''),COALESCE(device_class,''),hardware_concurrency,device_memory,touch,COALESCE(user_agent,''),CAST(last_seen AS CHAR)
      FROM gam_browser_discovery_clients WHERE last_seen >= (CURRENT_TIMESTAMP - INTERVAL 24 HOUR) ORDER BY last_seen DESC
      """,(rs,n)->new Client(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10),rs.getString(11),rs.getString(12),(Integer)rs.getObject(13),(Double)rs.getObject(14),rs.getBoolean(15),rs.getString(16),rs.getString(17)));
  }

  private Client find(String id){
    return jdbc.queryForObject("""
      SELECT client_id,address,COALESCE(platform,''),COALESCE(platform_version,''),COALESCE(browser,''),COALESCE(browser_version,''),COALESCE(architecture,''),COALESCE(model,''),COALESCE(language,''),COALESCE(timezone,''),COALESCE(screen,''),COALESCE(device_class,''),hardware_concurrency,device_memory,touch,COALESCE(user_agent,''),CAST(last_seen AS CHAR)
      FROM gam_browser_discovery_clients WHERE client_id=?
      """,(rs,n)->new Client(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10),rs.getString(11),rs.getString(12),(Integer)rs.getObject(13),(Double)rs.getObject(14),rs.getBoolean(15),rs.getString(16),rs.getString(17)),id);
  }

  private static String normalizeRemoteAddress(String value){
    String v=value==null?"":value.trim();
    if(v.startsWith("::ffff:")) v=v.substring(7);
    // 40k36r2: Ein Browser, der das Backend ueber localhost/::1 aufruft, ist kein
    // eigenes physisches Geraet. Fuer die Discovery wird deshalb eine echte lokale
    // Backend-Adresse verwendet. So entstehen keine macOS-Browser-Dubletten mit
    // 127.0.0.1 bzw. 0:0:0:0:0:0:0:1.
    if(isLoopback(v)){
      String local=firstRealLocalAddress();
      if(local!=null&&!local.isBlank()) return local;
    }
    return v;
  }
  private static boolean isLoopback(String value){
    try{return value!=null&&!value.isBlank()&&InetAddress.getByName(value).isLoopbackAddress();}
    catch(Exception e){return false;}
  }
  private static String firstRealLocalAddress(){
    try{
      Enumeration<NetworkInterface> all=NetworkInterface.getNetworkInterfaces();
      if(all==null)return null;
      List<String> ipv4=new ArrayList<>(), ipv6=new ArrayList<>();
      while(all.hasMoreElements()){
        NetworkInterface ni=all.nextElement();
        if(!ni.isUp()||ni.isLoopback()||ni.isVirtual())continue;
        Enumeration<InetAddress> addresses=ni.getInetAddresses();
        while(addresses.hasMoreElements()){
          InetAddress a=addresses.nextElement();
          if(a.isLoopbackAddress()||a.isLinkLocalAddress())continue;
          String host=a.getHostAddress();
          int zone=host.indexOf('%'); if(zone>=0)host=host.substring(0,zone);
          if(a instanceof Inet4Address)ipv4.add(host); else ipv6.add(host);
        }
      }
      if(!ipv4.isEmpty())return ipv4.get(0);
      return ipv6.isEmpty()?null:ipv6.get(0);
    }catch(Exception e){return null;}
  }
  private static String clean(Object v){return v==null?"":String.valueOf(v).trim();}
  private static Integer integer(Object v){try{return v==null?null:Integer.valueOf(String.valueOf(v));}catch(Exception e){return null;}}
  private static Double decimal(Object v){try{return v==null?null:Double.valueOf(String.valueOf(v));}catch(Exception e){return null;}}
}
