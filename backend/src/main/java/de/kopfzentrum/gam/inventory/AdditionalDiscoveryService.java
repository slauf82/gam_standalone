package de.kopfzentrum.gam.inventory;

import org.springframework.stereotype.Service;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.*;

@Service
public class AdditionalDiscoveryService {
  private static final Pattern IPV4=Pattern.compile("(?<![0-9])((?:[0-9]{1,3}\\.){3}[0-9]{1,3})(?![0-9])");
  private static final Pattern MAC=Pattern.compile("(?i)([0-9a-f]{2}(?:[:-][0-9a-f]{2}){5})");

  public void scan(Map<String,Boolean> settings,String now,Consumer<DiscoveredDevice> out,Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent> diag,String session,int count){
    run("WS_DISCOVERY",settings,diag,session,count,()->wsd(now,out));
    run("DHCP_LEASES",settings,diag,session,count,()->dhcp(now,out));
    run("USB_LOCAL",settings,diag,session,count,()->usb(now,out));
    run("BLUETOOTH_LOCAL",settings,diag,session,count,()->bluetooth(now,out));
    run("DOCKER_LOCAL",settings,diag,session,count,()->docker(now,out));
  }
  private void run(String key,Map<String,Boolean>s,Consumer<DeviceDiscoveryService.DiscoveryDiagnosticEvent>d,String session,int count,Runnable r){
    if(!s.getOrDefault(key,false)){d.accept(new DeviceDiscoveryService.DiscoveryDiagnosticEvent(session,OffsetDateTime.now().toString(),key,"SKIPPED","Quelle ist deaktiviert",count));return;}
    d.accept(new DeviceDiscoveryService.DiscoveryDiagnosticEvent(session,OffsetDateTime.now().toString(),key,"RUNNING","Quelle wird ausgewertet",count));
    try{r.run();d.accept(new DeviceDiscoveryService.DiscoveryDiagnosticEvent(session,OffsetDateTime.now().toString(),key,"COMPLETED","Quelle wurde ausgewertet",count));}
    catch(Exception ex){d.accept(new DeviceDiscoveryService.DiscoveryDiagnosticEvent(session,OffsetDateTime.now().toString(),key,"FAILED",ex.getMessage(),count));}
  }
  private void wsd(String now,Consumer<DiscoveredDevice> out){
    String id=UUID.randomUUID().toString();
    String xml="<?xml version=\"1.0\"?><e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:w=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\"><e:Header><w:MessageID>uuid:"+id+"</w:MessageID><w:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To><w:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action></e:Header><e:Body><d:Probe/></e:Body></e:Envelope>";
    byte[] req=xml.getBytes(StandardCharsets.UTF_8);
    try(DatagramSocket socket=new DatagramSocket()){socket.setSoTimeout(600);socket.send(new DatagramPacket(req,req.length,InetAddress.getByName("239.255.255.250"),3702));long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(System.nanoTime()<until){try{byte[] b=new byte[16384];DatagramPacket p=new DatagramPacket(b,b.length);socket.receive(p);String body=new String(p.getData(),0,p.getLength(),StandardCharsets.UTF_8);String ip=p.getAddress().getHostAddress();String low=body.toLowerCase(Locale.ROOT);String type=low.contains("networkvideotransmitter")||low.contains("onvif")?"IP-Kamera / ONVIF-Gerät":"WS-Discovery-Gerät";String name=scope(body,"name");if(name==null)name=scope(body,"hardware");if(name==null)name=type+" "+ip;out.accept(new DiscoveredDevice("wsd:"+ip,name,type,ip,null,"WS-Discovery / ONVIF","ONLINE",null,null,now,false));}catch(SocketTimeoutException ignored){}}}catch(Exception ignored){}
  }
  private String scope(String body,String key){Matcher m=Pattern.compile("(?i)"+key+"/([^\\s<]+)").matcher(body);if(!m.find())return null;try{return URLDecoder.decode(m.group(1),StandardCharsets.UTF_8);}catch(Exception e){return m.group(1);}}
  private void dhcp(String now,Consumer<DiscoveredDevice> out){if(isWindows())return;for(String f:List.of("/var/lib/misc/dnsmasq.leases","/var/lib/dhcp/dhcpd.leases","/tmp/dhcp.leases")){for(String line:cmd(List.of("sh","-c","cat "+f+" 2>/dev/null"))){Matcher im=IPV4.matcher(line);if(!im.find())continue;Matcher mm=MAC.matcher(line);String ip=im.group(1),mac=mm.find()?mm.group(1).replace('-',':').toUpperCase(Locale.ROOT):null;String[] p=line.trim().split("\\s+");String name=p.length>3&&!p[3].equals("*")?p[3]:"DHCP-Gerät "+ip;out.accept(new DiscoveredDevice("dhcp:"+ip,name,"Netzwerkgerät",ip,mac,"DHCP-Lease","ERKANNT",null,null,now,false));}}}
  /**
   * 40k37a4: Die generische USB-Quelle bleibt plattformneutral, aber Apple-Mobile-
   * PnP-Knoten werden hier bewusst NICHT mehr emittiert. Sie werden zentral durch
   * AppleMobileDiscoveryService gruppiert, damit ein physisches iPad nicht als
   * Basisgeraet + PTP + USB-Multiplexor mehrfach registriert wird.
   */
  private void usb(String now,Consumer<DiscoveredDevice> out){
    if(!isWindows()){
      int i=0; for(String row:cmd(List.of("sh","-c","lsusb 2>/dev/null"))){
        if(row.isBlank())continue;
        // 40k37e55e: lsusb-Ausgabe strukturiert darstellen. Bus/Device/VID:PID sind
        // technische Details und gehoeren nicht in den sichtbaren Gerätenamen.
        Matcher lm=Pattern.compile("^Bus\\s+(\\d+)\\s+Device\\s+(\\d+):\\s+ID\\s+([0-9a-fA-F]{4}):([0-9a-fA-F]{4})\\s*(.*)$").matcher(row.trim());
        String name=row.trim(), address=null;
        if(lm.matches()) {
          String vendorId=lm.group(3).toLowerCase(Locale.ROOT);
          // 40k37e55g-fix2: Apple-Mobile-Hardware (USB Vendor 05ac) ist unter Linux
          // ausschließlich ein physischer Hinweis fuer AppleMobileDiscoveryService.
          // Sobald die dedizierte Apple-Schicht usbmuxd/Lockdown nutzt, darf dieselbe
          // Hardware nicht zusaetzlich als generisches "Lokales USB-Gerät" erscheinen.
          // Das ist keine Zusammenfuehrung und kein Enrichment zwischen Quellen.
          if("05ac".equals(vendorId)) continue;
          String description=lm.group(5).trim();
          name=description.isBlank()?"USB-Gerät":description;
          address="Bus "+lm.group(1)+" · Device "+lm.group(2)+" · USB-ID "+vendorId+":"+lm.group(4).toLowerCase(Locale.ROOT);
        }
        out.accept(new DiscoveredDevice("usb:"+(i++),name,"Lokales USB-Gerät",address,null,"USB / Plug and Play","ONLINE",null,null,now,false));
      }
      return;
    }
    String ps="$ErrorActionPreference='SilentlyContinue'; "
      + "$all=Get-PnpDevice -PresentOnly -ErrorAction SilentlyContinue; "
      + "$d=$all | Where-Object { $_.Class -eq 'USB' -or $_.InstanceId -match 'VID_05AC' -or $_.FriendlyName -match 'Apple|iPhone|iPad|Mobile Device' }; "
      + "foreach($x in $d){ $bus=(Get-PnpDeviceProperty -InstanceId $x.InstanceId -KeyName 'DEVPKEY_Device_BusReportedDeviceDesc' -ErrorAction SilentlyContinue).Data; "
      + "$f=@($x.FriendlyName,$bus,$x.InstanceId,$x.Manufacturer,$x.Class,$x.Status) | ForEach-Object { [string]$_ -replace '[\t\r\n]',' ' }; "
      + "[Console]::WriteLine(($f -join [char]9)) }";
    List<String> rows=cmd(List.of("powershell.exe","-NoProfile","-NonInteractive","-Command",ps),12000);
    int i=0;
    for(String row:rows){
      if(row==null||row.isBlank())continue;
      String[] p=row.split("\t",-1);
      String friendly=p.length>0?p[0].trim():"";
      String bus=p.length>1?p[1].trim():"";
      String instance=p.length>2?p[2].trim():"";
      String manufacturer=p.length>3?p[3].trim():"";
      String pnpClass=p.length>4?p[4].trim():"";
      String status=p.length>5?p[5].trim():"";
      String evidence=(friendly+" "+bus+" "+instance+" "+manufacturer).toLowerCase(Locale.ROOT);
      boolean apple=evidence.contains("vid_05ac")||evidence.contains("apple")||evidence.contains("iphone")||evidence.contains("ipad");
      if(apple){
        // 40k37a4: Apple-Mobile wird ausschliesslich im dedizierten Service emittiert.
        // Sonst erscheinen MI_00/PTP, MI_01/Multiplexor und Basisgeraet als getrennte Geräte.
        continue;
      } else {
        String name=!friendly.isBlank()?friendly:!bus.isBlank()?bus:"Lokales USB-Gerät";
        out.accept(new DiscoveredDevice("usb:"+(i++),name,"Lokales USB-Gerät",instance.isBlank()?null:instance,null,"USB / Plug and Play","ONLINE",manufacturer.isBlank()?null:manufacturer,instance.isBlank()?null:instance,now,false));
      }
    }
  }
  private void bluetooth(String now,Consumer<DiscoveredDevice> out){List<String> rows=isWindows()?cmd(List.of("powershell.exe","-NoProfile","-Command","Get-PnpDevice -PresentOnly -Class Bluetooth -ErrorAction SilentlyContinue | % { $_.FriendlyName + '|' + $_.InstanceId }")):cmd(List.of("sh","-c","bluetoothctl paired-devices 2>/dev/null"));int i=0;for(String row:rows){if(row.isBlank())continue;Matcher m=MAC.matcher(row);String mac=m.find()?m.group(1):null;String name=row.replaceFirst("(?i)^Device\\s+[0-9A-F:.-]+\\s*","").split("\\|",2)[0];out.accept(new DiscoveredDevice("bluetooth:"+(mac!=null?mac:i++),name,"Bluetooth-Gerät",mac,mac,"Bluetooth","ERKANNT",null,null,now,false));}}
  private void docker(String now,Consumer<DiscoveredDevice> out){String exe=isWindows()?"docker.exe":"docker";for(String row:cmd(List.of(exe,"ps","--format","{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}"))){String[]p=row.split("\\|",4);if(p.length<2)continue;out.accept(new DiscoveredDevice("docker:"+p[0],p[1],"Docker-Container",p[0],null,"Docker Engine","ONLINE",p.length>2?p[2]:null,p[0],now,false));}}
  private List<String> cmd(List<String> c){return cmd(c,2500);}
  private List<String> cmd(List<String> c,long timeoutMs){try{Process p=new ProcessBuilder(c).redirectErrorStream(true).start();if(!p.waitFor(timeoutMs,TimeUnit.MILLISECONDS)){p.destroyForcibly();return List.of();}return Arrays.stream(new String(p.getInputStream().readAllBytes(),Charset.defaultCharset()).split("\\R")).limit(500).toList();}catch(Exception e){return List.of();}}
  private boolean isWindows(){return System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win");}
}
