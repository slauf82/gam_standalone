package de.kopfzentrum.gam.inventory;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Kleine, dependency-freie mDNS-/Bonjour-Erkennung.
 *
 * Sie arbeitet direkt mit UDP-Multicast auf 224.0.0.251:5353 und benötigt
 * weder Bonjour/dns-sd unter Windows/macOS noch avahi-browse unter Linux.
 */
final class NativeMdnsDiscovery {
  private static final InetAddress MDNS_GROUP;
  private static final int MDNS_PORT = 5353;

  static {
    try {
      MDNS_GROUP = InetAddress.getByName("224.0.0.251");
    } catch (Exception ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private static final List<String> COMMON_SERVICE_TYPES = List.of(
    "_http._tcp.local",
    "_https._tcp.local",
    "_workstation._tcp.local",
    "_device-info._tcp.local",
    "_printer._tcp.local",
    "_ipp._tcp.local",
    "_ipps._tcp.local",
    "_pdl-datastream._tcp.local",
    "_scanner._tcp.local",
    "_smb._tcp.local",
    "_afpovertcp._tcp.local",
    "_ssh._tcp.local",
    "_sftp-ssh._tcp.local",
    "_rfb._tcp.local",
    "_airplay._tcp.local",
    "_raop._tcp.local",
    "_googlecast._tcp.local",
    "_hap._tcp.local",
    "_home-assistant._tcp.local",
    "_mqtt._tcp.local",
    "_onvif._tcp.local",
    "_axis-video._tcp.local"
  );

  record Result(List<Host> hosts, int packets, int records, Set<String> serviceTypes) { }
  record Host(String hostName, String displayName, String address, String serviceType, String txt) { }

  Result discover(long timeoutMillis) throws Exception {
    long deadline = System.currentTimeMillis() + Math.max(1200, timeoutMillis);
    Map<String, String> ptr = new LinkedHashMap<>();
    Map<String, String> srv = new LinkedHashMap<>();
    Map<String, String> ipv4 = new LinkedHashMap<>();
    Map<String, String> txt = new LinkedHashMap<>();
    Set<String> serviceTypes = new LinkedHashSet<>();
    int packetCount = 0;
    int recordCount = 0;

    try (MulticastSocket socket = new MulticastSocket()) {
      socket.setReuseAddress(true);
      socket.setTimeToLive(1);
      socket.setSoTimeout(220);

      sendPtrQuery(socket, "_services._dns-sd._udp.local");
      for (String serviceType : COMMON_SERVICE_TYPES) sendPtrQuery(socket, serviceType);

      boolean sentDynamicQueries = false;
      while (System.currentTimeMillis() < deadline) {
        byte[] buffer = new byte[9000];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
          socket.receive(packet);
        } catch (java.net.SocketTimeoutException ignored) {
          if (!sentDynamicQueries && !serviceTypes.isEmpty()) {
            for (String serviceType : serviceTypes) sendPtrQuery(socket, serviceType);
            sentDynamicQueries = true;
          }
          continue;
        }

        packetCount++;
        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
        recordCount += parsePacket(data, ptr, srv, ipv4, txt, serviceTypes);

        if (!sentDynamicQueries && !serviceTypes.isEmpty()) {
          for (String serviceType : serviceTypes) sendPtrQuery(socket, serviceType);
          sentDynamicQueries = true;
        }
      }
    }

    List<Host> hosts = buildHosts(ptr, srv, ipv4, txt);
    return new Result(hosts, packetCount, recordCount, serviceTypes);
  }

  private static void sendPtrQuery(MulticastSocket socket, String name) throws Exception {
    byte[] query = buildQuery(name, 12); // PTR
    socket.send(new DatagramPacket(query, query.length, MDNS_GROUP, MDNS_PORT));
  }

  private static byte[] buildQuery(String name, int type) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writeU16(out, 0);       // transaction id
    writeU16(out, 0);       // flags
    writeU16(out, 1);       // questions
    writeU16(out, 0);       // answers
    writeU16(out, 0);       // authority
    writeU16(out, 0);       // additional
    writeName(out, name);
    writeU16(out, type);
    writeU16(out, 0x8001);  // IN + Unicast-Response-Bit: Antworten gehen an unseren Quellport
    return out.toByteArray();
  }

  private static int parsePacket(byte[] data,
                                 Map<String, String> ptr,
                                 Map<String, String> srv,
                                 Map<String, String> ipv4,
                                 Map<String, String> txt,
                                 Set<String> serviceTypes) {
    if (data.length < 12) return 0;
    int qd = u16(data, 4);
    int an = u16(data, 6);
    int ns = u16(data, 8);
    int ar = u16(data, 10);
    int[] offset = {12};

    for (int i = 0; i < qd && offset[0] < data.length; i++) {
      readName(data, offset);
      offset[0] = Math.min(data.length, offset[0] + 4);
    }

    int parsed = 0;
    int total = an + ns + ar;
    for (int i = 0; i < total && offset[0] < data.length; i++) {
      String owner = normalizeDnsName(readName(data, offset));
      if (offset[0] + 10 > data.length) break;
      int type = u16(data, offset[0]);
      int rdLength = u16(data, offset[0] + 8);
      offset[0] += 10;
      int rdataStart = offset[0];
      int rdataEnd = Math.min(data.length, rdataStart + rdLength);
      if (rdataEnd < rdataStart || rdataStart >= data.length) break;

      try {
        if (type == 12) { // PTR
          int[] ptrOffset = {rdataStart};
          String target = normalizeDnsName(readName(data, ptrOffset));
          if (!owner.isBlank() && !target.isBlank()) {
            ptr.put(owner + "\u0000" + target, target);
            if (owner.equals("_services._dns-sd._udp.local")) serviceTypes.add(target);
          }
        } else if (type == 33 && rdLength >= 6) { // SRV
          int[] srvOffset = {rdataStart + 6};
          String targetHost = normalizeDnsName(readName(data, srvOffset));
          if (!owner.isBlank() && !targetHost.isBlank()) srv.put(owner, targetHost);
        } else if (type == 1 && rdLength == 4) { // A
          String ip = (data[rdataStart] & 0xff) + "." + (data[rdataStart + 1] & 0xff) + "." +
            (data[rdataStart + 2] & 0xff) + "." + (data[rdataStart + 3] & 0xff);
          if (!owner.isBlank()) ipv4.put(owner, ip);
        } else if (type == 16) { // TXT
          String value = readTxt(data, rdataStart, rdataEnd);
          if (!owner.isBlank() && !value.isBlank()) txt.put(owner, value);
        }
        parsed++;
      } catch (RuntimeException ignored) {
        // Einzelne beschädigte oder ungewöhnliche Records dürfen die gesamte Discovery nicht stoppen.
      }
      offset[0] = rdataEnd;
    }
    return parsed;
  }

  private static List<Host> buildHosts(Map<String, String> ptr,
                                       Map<String, String> srv,
                                       Map<String, String> ipv4,
                                       Map<String, String> txt) {
    LinkedHashMap<String, Host> result = new LinkedHashMap<>();

    for (Map.Entry<String, String> entry : ptr.entrySet()) {
      String[] keyParts = entry.getKey().split("\u0000", 2);
      String serviceType = keyParts.length > 0 ? keyParts[0] : "";
      if (serviceType.equals("_services._dns-sd._udp.local")) continue;
      String instance = entry.getValue();
      String host = srv.get(instance);
      String ip = host == null ? null : ipv4.get(host);
      if (ip == null || ip.isBlank()) continue;
      String displayName = serviceDisplayName(instance, serviceType, host);
      result.put(ip + "|" + instance, new Host(host, displayName, ip, serviceType, txt.get(instance)));
    }

    // Einige Geräte antworten nur mit einem A-Record, ohne PTR/SRV-Kette.
    for (Map.Entry<String, String> entry : ipv4.entrySet()) {
      String host = entry.getKey();
      String ip = entry.getValue();
      boolean alreadyPresent = result.values().stream().anyMatch(value -> value.address().equals(ip));
      if (!alreadyPresent) result.put(ip + "|" + host, new Host(host, stripLocal(host), ip, "mDNS", null));
    }

    return new ArrayList<>(result.values());
  }

  private static String serviceDisplayName(String instance, String serviceType, String host) {
    String suffix = "." + serviceType;
    if (instance.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
      String value = instance.substring(0, instance.length() - suffix.length());
      if (!value.isBlank()) return value;
    }
    return stripLocal(host);
  }

  private static String stripLocal(String value) {
    if (value == null) return null;
    return value.toLowerCase(Locale.ROOT).endsWith(".local") ? value.substring(0, value.length() - 6) : value;
  }

  private static String readTxt(byte[] data, int start, int end) {
    List<String> values = new ArrayList<>();
    int offset = start;
    while (offset < end) {
      int length = data[offset++] & 0xff;
      if (length == 0 || offset + length > end) break;
      values.add(new String(data, offset, length, StandardCharsets.UTF_8));
      offset += length;
    }
    return String.join("; ", values);
  }

  private static String readName(byte[] data, int[] offsetRef) {
    StringBuilder result = new StringBuilder();
    int offset = offsetRef[0];
    int resume = -1;
    int hops = 0;

    while (offset >= 0 && offset < data.length && hops++ < 64) {
      int length = data[offset] & 0xff;
      if (length == 0) {
        offset++;
        if (resume < 0) offsetRef[0] = offset;
        else offsetRef[0] = resume;
        break;
      }
      if ((length & 0xc0) == 0xc0) {
        if (offset + 1 >= data.length) break;
        int pointer = ((length & 0x3f) << 8) | (data[offset + 1] & 0xff);
        if (resume < 0) resume = offset + 2;
        offset = pointer;
        continue;
      }
      offset++;
      if (offset + length > data.length) break;
      if (!result.isEmpty()) result.append('.');
      result.append(new String(data, offset, length, StandardCharsets.UTF_8));
      offset += length;
      if (resume < 0) offsetRef[0] = offset;
    }
    if (resume >= 0) offsetRef[0] = resume;
    return result.toString();
  }

  private static String normalizeDnsName(String value) {
    if (value == null) return "";
    String result = value.trim();
    while (result.endsWith(".")) result = result.substring(0, result.length() - 1);
    return result;
  }

  private static void writeName(ByteArrayOutputStream out, String name) {
    for (String label : name.split("\\.")) {
      byte[] bytes = label.getBytes(StandardCharsets.UTF_8);
      out.write(Math.min(63, bytes.length));
      out.write(bytes, 0, Math.min(63, bytes.length));
    }
    out.write(0);
  }

  private static void writeU16(ByteArrayOutputStream out, int value) {
    out.write((value >>> 8) & 0xff);
    out.write(value & 0xff);
  }

  private static int u16(byte[] data, int offset) {
    if (offset + 1 >= data.length) return 0;
    return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
  }
}
