package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class HomeAssistantSettingsRepository {
  private final JdbcTemplate jdbc;
  public HomeAssistantSettingsRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
  @PostConstruct void init(){jdbc.execute("CREATE TABLE IF NOT EXISTS gam_homeassistant_sources (id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(120) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, base_url VARCHAR(500) NOT NULL, access_token TEXT NULL, location VARCHAR(255) NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(id))");}
  public List<View> loadViews(){return jdbc.query("SELECT id,name,enabled,base_url,COALESCE(access_token,''),COALESCE(location,'') FROM gam_homeassistant_sources ORDER BY name,id",(rs,n)->new View(rs.getLong(1),rs.getString(2),rs.getBoolean(3),rs.getString(4),!rs.getString(5).isBlank(),rs.getString(6)));}
  public List<Settings> loadEnabledInternal(){return loadInternal(null).stream().filter(Settings::enabled).toList();}
  public Settings loadOne(long id){return loadInternal(id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Home-Assistant-Quelle nicht gefunden: "+id));}
  private List<Settings> loadInternal(Long id){String sql="SELECT id,name,enabled,base_url,COALESCE(access_token,''),COALESCE(location,'') FROM gam_homeassistant_sources"+(id==null?" ORDER BY name,id":" WHERE id=?");return id==null?jdbc.query(sql,(rs,n)->map(rs)):jdbc.query(sql,(rs,n)->map(rs),id);}
  private Settings map(java.sql.ResultSet rs)throws java.sql.SQLException{return new Settings(rs.getLong(1),rs.getString(2),rs.getBoolean(3),rs.getString(4),rs.getString(5),rs.getString(6));}
  public View create(Update u,String actor){var kh=new org.springframework.jdbc.support.GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO gam_homeassistant_sources(name,enabled,base_url,access_token,location,updated_by) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setString(1,clean(u.name(),"Home Assistant"));ps.setBoolean(2,u.enabled());ps.setString(3,normalizeUrl(u.baseUrl()));ps.setString(4,u.accessToken()==null?"":u.accessToken().trim());ps.setString(5,clean(u.location(),""));ps.setString(6,actor);return ps;},kh);return view(loadOne(kh.getKey().longValue()));}
  public View save(long id,Update u,String actor){Settings before=loadOne(id);String token=before.accessToken();if(u.accessToken()!=null&&!u.accessToken().isBlank())token=u.accessToken().trim();else if(u.clearAccessToken())token="";jdbc.update("UPDATE gam_homeassistant_sources SET name=?,enabled=?,base_url=?,access_token=?,location=?,updated_by=? WHERE id=?",clean(u.name(),before.name()),u.enabled(),normalizeUrl(u.baseUrl()),token,clean(u.location(),""),actor,id);return view(loadOne(id));}
  public void delete(long id){jdbc.update("DELETE FROM gam_homeassistant_sources WHERE id=?",id);}
  private View view(Settings s){return new View(s.id(),s.name(),s.enabled(),s.baseUrl(),!s.accessToken().isBlank(),s.location());}
  private static String normalizeUrl(String v){String x=clean(v,"http://homeassistant.local:8123");while(x.endsWith("/"))x=x.substring(0,x.length()-1);return x;}
  private static String clean(String v,String f){return v==null||v.isBlank()?f:v.trim();}
  public record Settings(long id,String name,boolean enabled,String baseUrl,String accessToken,String location){}
  public record View(long id,String name,boolean enabled,String baseUrl,boolean accessTokenConfigured,String location){}
  public record Update(String name,boolean enabled,String baseUrl,String accessToken,boolean clearAccessToken,String location){}
}
