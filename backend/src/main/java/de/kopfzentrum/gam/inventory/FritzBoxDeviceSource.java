package de.kopfzentrum.gam.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Service
public class FritzBoxDeviceSource {
  private static final Logger log=LoggerFactory.getLogger(FritzBoxDeviceSource.class);
  private final FritzBoxSettingsRepository settingsRepository;
  public FritzBoxDeviceSource(FritzBoxSettingsRepository settingsRepository){this.settingsRepository=settingsRepository;}

  public FritzResult load(){
    var sources=settingsRepository.loadEnabledInternal();
    if(sources.isEmpty())return new FritzResult(List.of(),"SKIPPED","Keine aktive FRITZ!Box-Quelle konfiguriert");
    List<FritzDevice> all=new ArrayList<>();int completed=0;List<String> failures=new ArrayList<>();
    for(var s:sources){
      if(s.password().isBlank()){failures.add(s.name()+": Kennwort fehlt");continue;}
      try{FritzResult one=load(createClient(s),s);all.addAll(one.devices());completed++;}
      catch(Exception ex){log.warn("FRITZ!Box-Anreicherung {} fehlgeschlagen: {}",s.name(),ex.toString());failures.add(s.name()+": "+Objects.toString(ex.getMessage(),ex.getClass().getSimpleName()));}
    }
    Map<String,FritzDevice> unique=new LinkedHashMap<>();for(FritzDevice d:all){String key=!d.mac().isBlank()?"M:"+d.mac():"S:"+d.sourceId()+":I:"+d.ip();unique.putIfAbsent(key,d);}
    String status=completed==0?"FAILED":failures.isEmpty()?"COMPLETED":"PARTIAL";
    String message=completed+" von "+sources.size()+" FRITZ!Box-Quellen geladen, "+unique.size()+" Geräte"+(failures.isEmpty()?"":"; "+String.join(" | ",failures));
    return new FritzResult(new ArrayList<>(unique.values()),status,message);
  }

  public FritzTestResult test(long sourceId){
    var settings=settingsRepository.loadOne(sourceId);
    if(!settings.enabled())return new FritzTestResult(settings.id(),settings.name(),false,"SKIPPED","Die FRITZ!Box-Gerätequelle ist deaktiviert.",settings.host(),"","",0,0,List.of());
    if(settings.password().isBlank())return new FritzTestResult(settings.id(),settings.name(),false,"CONFIGURATION","Das FRITZ!Box-Kennwort fehlt.",settings.host(),"","",0,0,List.of());
    try{
      HttpClient client=createClient(settings);
      FritzInfo info=loadInfo(client,settings);
      FritzResult result=load(client,settings);
      boolean success="COMPLETED".equals(result.status());
      int active=(int)result.devices().stream().filter(FritzDevice::active).count();
      List<String> preview=result.devices().stream().map(FritzDevice::name).filter(Objects::nonNull).map(String::trim).filter(v->!v.isBlank()).distinct().limit(10).toList();
      return new FritzTestResult(settings.id(),settings.name(),success,success?"COMPLETED":"FAILED",result.message(),settings.host(),info.modelName(),info.softwareVersion(),result.devices().size(),active,preview);
    }catch(Exception ex){
      log.warn("FRITZ!Box-Verbindungstest fehlgeschlagen: {}",ex.toString());
      return new FritzTestResult(settings.id(),settings.name(),false,classifyFailure(ex),friendlyFailure(ex),settings.host(),"","",0,0,List.of());
    }
  }

  private static HttpClient createClient(FritzBoxSettingsRepository.FritzBoxSettings s){
    return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).authenticator(new Authenticator(){@Override protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(Objects.toString(s.username(),""),s.password().toCharArray());}}).build();
  }

  private FritzResult load(HttpClient client,FritzBoxSettingsRepository.FritzBoxSettings s) throws Exception{
    String base="http://"+s.host()+":"+s.port()+"/upnp/control/hosts";
    int count=Integer.parseInt(text(soap(client,base,"urn:dslforum-org:service:Hosts:1","GetHostNumberOfEntries",""),"NewHostNumberOfEntries","0"));
    List<FritzDevice> devices=new ArrayList<>();
    for(int i=0;i<count;i++){
      try{
        Document d=soap(client,base,"urn:dslforum-org:service:Hosts:1","GetGenericHostEntry","<NewIndex>"+i+"</NewIndex>");
        String ip=text(d,"NewIPAddress",""); String mac=normalizeMac(text(d,"NewMACAddress","")); String name=text(d,"NewHostName","");
        boolean active="1".equals(text(d,"NewActive","0")); String iface=text(d,"NewInterfaceType","");
        if(!ip.isBlank()||!mac.isBlank()||!name.isBlank())devices.add(new FritzDevice(ip,mac,name,active,iface,s.id(),s.name(),s.location()));
      }catch(Exception entryFailure){log.debug("FRITZ!Box Host-Eintrag {} konnte nicht gelesen werden: {}",i,entryFailure.toString());}
    }
    return new FritzResult(devices,"COMPLETED",devices.size()+" FRITZ!Box-Geräte geladen");
  }

  private static FritzInfo loadInfo(HttpClient client,FritzBoxSettingsRepository.FritzBoxSettings s){
    try{
      String base="http://"+s.host()+":"+s.port()+"/upnp/control/deviceinfo";
      Document d=soap(client,base,"urn:dslforum-org:service:DeviceInfo:1","GetInfo","");
      return new FritzInfo(text(d,"NewModelName","FRITZ!Box"),text(d,"NewSoftwareVersion",""));
    }catch(Exception ignored){return new FritzInfo("FRITZ!Box","");}
  }

  private static String classifyFailure(Exception ex){
    String m=Objects.toString(ex.getMessage(),"").toLowerCase(Locale.ROOT);
    if(m.contains("http 401")||m.contains("http 403"))return "AUTHENTICATION";
    if(m.contains("connect")||m.contains("unresolved")||m.contains("timed out")||m.contains("timeout"))return "UNREACHABLE";
    return "FAILED";
  }
  private static String friendlyFailure(Exception ex){
    String code=classifyFailure(ex);
    if("AUTHENTICATION".equals(code))return "Anmeldung fehlgeschlagen. Benutzername oder Kennwort sind nicht korrekt oder der Benutzer besitzt nicht die erforderlichen Rechte.";
    if("UNREACHABLE".equals(code))return "Die FRITZ!Box ist unter der eingetragenen Adresse und dem TR-064-Port nicht erreichbar.";
    return "TR-064-Verbindung fehlgeschlagen: "+Objects.toString(ex.getMessage(),ex.getClass().getSimpleName());
  }

  private static Document soap(HttpClient client,String url,String service,String action,String body) throws Exception{
    String xml="<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:"+action+" xmlns:u=\""+service+"\">"+body+"</u:"+action+"></s:Body></s:Envelope>";
    HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6)).header("Content-Type","text/xml; charset=\"utf-8\"").header("SoapAction","\""+service+"#"+action+"\"").POST(HttpRequest.BodyPublishers.ofString(xml,StandardCharsets.UTF_8)).build();
    HttpResponse<byte[]> res=client.send(req,HttpResponse.BodyHandlers.ofByteArray());
    if(res.statusCode()<200||res.statusCode()>=300)throw new IllegalStateException("HTTP "+res.statusCode());
    DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
    return f.newDocumentBuilder().parse(new ByteArrayInputStream(res.body()));
  }
  private static String text(Document d,String local,String fallback){NodeList n=d.getElementsByTagNameNS("*",local);return n.getLength()==0?fallback:Objects.toString(n.item(0).getTextContent(),fallback).trim();}
  private static String normalizeMac(String value){return value==null?"":value.replace('-',':').toUpperCase(Locale.ROOT);}
  public record FritzDevice(String ip,String mac,String name,boolean active,String interfaceType,long sourceId,String sourceName,String location){}
  public record FritzResult(List<FritzDevice> devices,String status,String message){}
  public record FritzInfo(String modelName,String softwareVersion){}
  public record FritzTestResult(long sourceId,String sourceName,boolean success,String status,String message,String host,String modelName,String softwareVersion,int deviceCount,int activeDeviceCount,List<String> devicePreview){}
}
