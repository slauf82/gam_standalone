package de.kopfzentrum.gam.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Kleine, abhaengigkeitsfreie SNMP-v1-Discovery fuer die Standard-System-OIDs.
 * Sie liest nur Identitaetsdaten und nimmt keinerlei Aenderungen an Zielgeraeten vor.
 */
@Service
public class SnmpDiscoveryService {
  private static final Logger log = LoggerFactory.getLogger(SnmpDiscoveryService.class);
  private static final String SYS_DESCR = "1.3.6.1.2.1.1.1.0";
  private static final String SYS_OBJECT_ID = "1.3.6.1.2.1.1.2.0";
  private static final String SYS_NAME = "1.3.6.1.2.1.1.5.0";
  private static final AtomicInteger REQUEST_ID = new AtomicInteger(1000);

  public Result scan(Collection<String> addresses, Consumer<SnmpDevice> consumer) {
    LinkedHashSet<String> targets = new LinkedHashSet<>();
    for (String address : addresses) if (isIpv4(address)) targets.add(address);
    if (targets.isEmpty()) return new Result(0, 0, "Keine IPv4-Ziele fuer SNMP vorhanden");

    String community = setting("gam.discovery.snmp.community", "GAM_SNMP_COMMUNITY", "public");
    int port = intSetting("gam.discovery.snmp.port", "GAM_SNMP_PORT", 161, 1, 65535);
    int timeout = intSetting("gam.discovery.snmp.timeout-ms", "GAM_SNMP_TIMEOUT_MS", 350, 100, 5000);
    int workers = Math.min(24, Math.max(2, targets.size()));
    ExecutorService pool = Executors.newFixedThreadPool(workers, r -> {
      Thread t = new Thread(r, "gam-snmp-discovery"); t.setDaemon(true); return t;
    });
    AtomicInteger responses = new AtomicInteger();
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (String target : targets) futures.add(pool.submit(() -> {
        try {
          SnmpDevice device = query(target, port, community, timeout);
          if (device != null) { responses.incrementAndGet(); consumer.accept(device); }
        } catch (Exception ex) {
          log.debug("[SNMP] {} antwortet nicht oder ist nicht lesbar: {}", target, ex.toString());
        }
      }));
      long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos((long) timeout * 2 + 1500L);
      for (Future<?> future : futures) {
        long left = deadline - System.nanoTime();
        if (left <= 0) { future.cancel(true); continue; }
        try { future.get(left, TimeUnit.NANOSECONDS); }
        catch (Exception ex) { future.cancel(true); }
      }
    } finally { pool.shutdownNow(); }
    return new Result(targets.size(), responses.get(), responses.get() + " SNMP-Geräte bei " + targets.size() + " IPv4-Zielen erkannt");
  }

  private SnmpDevice query(String address, int port, String community, int timeout) throws Exception {
    byte[] request = request(community, REQUEST_ID.incrementAndGet(), List.of(SYS_DESCR, SYS_OBJECT_ID, SYS_NAME));
    byte[] buffer = new byte[8192];
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(timeout);
      socket.send(new DatagramPacket(request, request.length, InetAddress.getByName(address), port));
      DatagramPacket response = new DatagramPacket(buffer, buffer.length);
      socket.receive(response);
      Map<String, String> values = parseResponse(Arrays.copyOf(response.getData(), response.getLength()));
      String descr = clean(values.get(SYS_DESCR));
      String objectId = clean(values.get(SYS_OBJECT_ID));
      String name = clean(values.get(SYS_NAME));
      if (descr == null && name == null && objectId == null) return null;
      String displayName = name != null ? name : bestName(descr, address);
      String manufacturer = manufacturer(descr, objectId);
      String type = type(descr, objectId);
      return new SnmpDevice(address, displayName, type, manufacturer, descr, objectId);
    }
  }

  private static byte[] request(String community, int requestId, List<String> oids) {
    List<byte[]> varbinds = new ArrayList<>();
    for (String oid : oids) varbinds.add(tlv(0x30, concat(tlv(0x06, encodeOid(oid)), tlv(0x05, new byte[0]))));
    byte[] pdu = tlv(0xA0, concat(tlv(0x02, encodeInt(requestId)), tlv(0x02, new byte[]{0}), tlv(0x02, new byte[]{0}), tlv(0x30, concat(varbinds.toArray(byte[][]::new)))));
    return tlv(0x30, concat(tlv(0x02, new byte[]{0}), tlv(0x04, community.getBytes(StandardCharsets.ISO_8859_1)), pdu));
  }

  private static Map<String, String> parseResponse(byte[] data) {
    LinkedHashMap<String, String> result = new LinkedHashMap<>();
    parseNode(data, 0, data.length, result, new String[1]);
    return result;
  }

  private static void parseNode(byte[] data, int start, int end, Map<String, String> out, String[] pendingOid) {
    int pos = start;
    while (pos < end) {
      int tag = data[pos++] & 0xff;
      int[] lengthAndBytes = readLength(data, pos, end);
      int length = lengthAndBytes[0]; pos += lengthAndBytes[1];
      int valueEnd = Math.min(end, pos + length);
      if (tag == 0x06 && pendingOid[0] == null) pendingOid[0] = decodeOid(data, pos, valueEnd);
      else if ((tag == 0x30 || (tag & 0x20) != 0) && valueEnd > pos) parseNode(data, pos, valueEnd, out, pendingOid);
      else if (pendingOid[0] != null && isValueTag(tag)) {
        out.put(pendingOid[0], decodeValue(tag, data, pos, valueEnd));
        pendingOid[0] = null;
      }
      pos = valueEnd;
    }
  }

  private static boolean isValueTag(int tag) { return tag == 0x04 || tag == 0x02 || tag == 0x06 || tag == 0x40 || tag == 0x41 || tag == 0x42 || tag == 0x43 || tag == 0x46; }
  private static String decodeValue(int tag, byte[] data, int start, int end) {
    if (tag == 0x04) return new String(data, start, end - start, StandardCharsets.ISO_8859_1).trim();
    if (tag == 0x06) return decodeOid(data, start, end);
    if (tag == 0x40 && end - start == 4) return (data[start]&255)+"."+(data[start+1]&255)+"."+(data[start+2]&255)+"."+(data[start+3]&255);
    long value = 0; for (int i=start;i<end;i++) value=(value<<8)|(data[i]&255L); return Long.toString(value);
  }

  private static int[] readLength(byte[] data, int pos, int end) {
    if (pos >= end) return new int[]{0,0};
    int first=data[pos]&255;
    if ((first&0x80)==0) return new int[]{first,1};
    int count=first&0x7f, length=0;
    for(int i=0;i<count&&pos+1+i<end;i++) length=(length<<8)|(data[pos+1+i]&255);
    return new int[]{length,1+count};
  }

  private static byte[] encodeOid(String oid) {
    String[] parts=oid.split("\\."); ByteArrayOutputStream out=new ByteArrayOutputStream();
    int first=Integer.parseInt(parts[0]), second=Integer.parseInt(parts[1]); out.write(first*40+second);
    for(int i=2;i<parts.length;i++) writeBase128(out,Long.parseLong(parts[i]));
    return out.toByteArray();
  }
  private static String decodeOid(byte[] data,int start,int end){
    if(start>=end)return ""; List<Long> parts=new ArrayList<>(); int first=data[start++]&255; parts.add((long)Math.min(2,first/40)); parts.add((long)(first-Math.min(2,first/40)*40));
    long value=0; for(int i=start;i<end;i++){int b=data[i]&255;value=(value<<7)|(b&0x7f);if((b&0x80)==0){parts.add(value);value=0;}}
    return String.join(".",parts.stream().map(String::valueOf).toList());
  }
  private static void writeBase128(ByteArrayOutputStream out,long value){int count=1;long v=value;while((v>>=7)>0)count++;for(int i=count-1;i>=0;i--){int b=(int)((value>>(i*7))&0x7f);if(i>0)b|=0x80;out.write(b);}}
  private static byte[] encodeInt(int value){return new byte[]{(byte)(value>>>24),(byte)(value>>>16),(byte)(value>>>8),(byte)value};}
  private static byte[] tlv(int tag,byte[] value){return concat(new byte[]{(byte)tag},encodeLength(value.length),value);}
  private static byte[] encodeLength(int length){if(length<128)return new byte[]{(byte)length};if(length<256)return new byte[]{(byte)0x81,(byte)length};return new byte[]{(byte)0x82,(byte)(length>>>8),(byte)length};}
  private static byte[] concat(byte[]...arrays){int n=0;for(byte[]a:arrays)n+=a.length;byte[]r=new byte[n];int p=0;for(byte[]a:arrays){System.arraycopy(a,0,r,p,a.length);p+=a.length;}return r;}

  private static String bestName(String descr,String address){if(descr==null)return "SNMP-Gerät "+address;String d=descr.replaceAll("[\\r\\n]+"," ").trim();return d.length()>70?d.substring(0,70):d;}
  private static String manufacturer(String descr,String oid){String h=((descr==null?"":descr)+" "+(oid==null?"":oid)).toLowerCase(Locale.ROOT);if(h.contains("synology"))return "Synology";if(h.contains("qnap"))return "QNAP";if(h.contains("cisco"))return "Cisco";if(h.contains("mikrotik"))return "MikroTik";if(h.contains("ubiquiti")||h.contains("unifi"))return "Ubiquiti";if(h.contains("aruba"))return "HPE Aruba";if(h.contains("hewlett")||h.contains(" hp ")||h.contains("laserjet"))return "HP";if(h.contains("brother"))return "Brother";if(h.contains("epson"))return "Epson";if(h.contains("canon"))return "Canon";if(h.contains("xerox"))return "Xerox";if(h.contains("apc"))return "APC";if(h.contains("fortinet"))return "Fortinet";if(h.contains("sophos"))return "Sophos";if(h.contains("avm")||h.contains("fritz"))return "AVM";return null;}
  private static String type(String descr,String oid){String h=((descr==null?"":descr)+" "+(oid==null?"":oid)).toLowerCase(Locale.ROOT);if(h.contains("printer")||h.contains("laserjet")||h.contains("brother")||h.contains("epson")||h.contains("xerox"))return "Drucker / Scanner";if(h.contains("synology")||h.contains("qnap")||h.contains("truenas")||h.contains("nas"))return "NAS-System";if(h.contains("ups")||h.contains("apc"))return "USV";if(h.contains("switch"))return "Netzwerk-Switch";if(h.contains("router")||h.contains("fritz"))return "Router";if(h.contains("access point")||h.contains("wireless")||h.contains("unifi ap"))return "Access Point";if(h.contains("linux")||h.contains("windows")||h.contains("server"))return "Server / Netzwerkgerät";return "SNMP-Netzwerkgerät";}
  private static String clean(String value){if(value==null)return null;String v=value.replace("\u0000","").trim();return v.isBlank()?null:v;}
  private static boolean isIpv4(String value){return value!=null&&value.matches("(?:\\d{1,3}\\.){3}\\d{1,3}");}
  private static String setting(String property,String env,String fallback){String v=System.getProperty(property);if(v==null||v.isBlank())v=System.getenv(env);return v==null||v.isBlank()?fallback:v.trim();}
  private static int intSetting(String property,String env,int fallback,int min,int max){try{int v=Integer.parseInt(setting(property,env,Integer.toString(fallback)));return Math.max(min,Math.min(max,v));}catch(Exception ex){return fallback;}}

  public record SnmpDevice(String address,String name,String type,String manufacturer,String description,String objectId){}
  public record Result(int targets,int responses,String message){}
}
