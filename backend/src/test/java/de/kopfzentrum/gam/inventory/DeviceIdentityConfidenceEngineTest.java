package de.kopfzentrum.gam.inventory;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeviceIdentityConfidenceEngineTest {
  private final DeviceIdentityConfidenceEngine engine = new DeviceIdentityConfidenceEngine();

  @Test void sameMacMergesEvenWithChangedIp() {
    var a = d("pc", "192.168.2.10", "AA:BB:CC:DD:EE:FF", null, "FritzBox");
    var b = d("pc", "192.168.2.77", "AA-BB-CC-DD-EE-FF", null, "SNMP");
    assertThat(engine.assess(a,b).decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.AUTO_MERGE);
  }

  @Test void sameIpAloneNeverMerges() {
    var a = d("Gerät A", "192.168.2.10", null, null, "ARP");
    var b = d("Gerät B", "192.168.2.10", null, null, "Ping");
    assertThat(engine.assess(a,b).decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.DISTINCT);
  }

  @Test void appleWlanAndFritzSameLanIpWithDifferentMacOffersDuplicate() {
    var apple = new DiscoveredDevice("apple-mobile-wlan:ipad","iPad","Tablet / iPadOS","192.168.2.163","44:A8:FC:07:E5:AE",
      "Apple Mobile / CoreDevice / USERSPACE_RSD / WLAN (40k37e34)","ERKANNT","Apple","00008020-TEST","2026-09-21",false);
    var fritz = new DiscoveredDevice("network:192.168.2.163","Eisenbahn-iPad","Server & virtuelle Systeme","192.168.2.163","9E:AA:50:3C:F5:3E",
      "Windows ARP · FRITZ!Box TR-064","ONLINE","",null,"2026-09-21",false);
    var result = engine.assess(apple,fritz);
    assertThat(result.decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.POSSIBLE_DUPLICATE);
    assertThat(result.matchedSignals()).contains("Apple-WLAN ↔ FRITZ!Box");
  }

  @Test void appleWlanAndFritzIpadNameOffersDuplicateWithoutCopyingNetworkIdentityIntoApple() {
    var apple = new DiscoveredDevice("apple-mobile-wlan:ipad","iPad","Tablet / iPadOS",null,"44:A8:FC:07:E5:AE",
      "Apple Mobile / CoreDevice / USERSPACE_RSD / WLAN (40k37e34)","ERKANNT","Apple","00008020-TEST","2026-09-21",false);
    var network = new DiscoveredDevice("network:192.168.2.163","Eisenbahn-iPad","Server & virtuelle Systeme","192.168.2.163","9E:AA:50:3C:F5:3E",
      "Windows ARP · FRITZ!Box TR-064","ONLINE","",null,"2026-09-21",false);
    var result = engine.assess(apple,network);
    assertThat(apple.address()).isNull();
    assertThat(network.address()).isEqualTo("192.168.2.163");
    assertThat(result.decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.POSSIBLE_DUPLICATE);
    assertThat(result.matchedSignals()).contains("Apple-WLAN ↔ FRITZ!Box");
  }

  @Test void matchingHardwareSerialMerges() {
    var a = d("PC alt", "192.168.2.10", null, "SN-ABC-12345", "Windows WMI");
    var b = d("PC neu", "192.168.2.11", null, "SN-ABC-12345", "WinRM");
    assertThat(engine.assess(a,b).decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.AUTO_MERGE);
  }

  @Test void distinctiveHostnameAcrossIndependentSourcesMergesAutomatically() {
    var a = d("OFFICE-PC", null, null, null, "FritzBox");
    var b = d("office-pc", null, null, null, "Home Assistant");
    var result = engine.assess(a,b);
    assertThat(result.decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.AUTO_MERGE);
  }

  @Test void androidAndIpadNeverGenerateMergeCandidateEvenWithSameMacAndSerial() {
    var ipad = new DiscoveredDevice("apple-mobile:ipad", "Eisenbahn-iPad", "Tablet / iPadOS", "192.168.2.10", "AA:BB:CC:DD:EE:FF", "Apple Mobile / CoreDevice", "ONLINE", "Apple", "SERIAL123", null, true);
    var android = new DiscoveredDevice("adb:tablet", "Eisenbahn-iPad", "Android Tablet", "192.168.2.10", "AA:BB:CC:DD:EE:FF", "ADB / Android", "ONLINE", "Apple", "SERIAL123", null, true);
    assertThat(engine.assess(ipad,android).decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.DISTINCT);
  }

  @Test void staleIpadCategoryWithAndroidAdbRemainsAndroid() {
    var ipad = new DiscoveredDevice("apple-mobile:ipad", "Eisenbahn-iPad", "Tablet / iPadOS", "192.168.2.163", null,
      "Apple Mobile / CoreDevice", "ONLINE", "Apple", null, null, false);
    var android = new DiscoveredDevice("adb:galaxy", "Galaxy-A56-5G", "Tablet / iPadOS", "192.168.2.182", null,
      "ADB / Android", "ONLINE", "Samsung", null, null, false);
    assertThat(DeviceIdentityConfidenceEngine.incompatibleMobilePlatforms(ipad,android)).isTrue();
    assertThat(engine.assess(ipad,android).decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.DISTINCT);
  }

  @Test void unknownNetworkSourceIsNotFalselyClassifiedAsApple() {
    var network = new DiscoveredDevice("network:163", "unbekannt", "Tablet / iPadOS", "192.168.2.163", null,
      "ARP / FRITZ!Box", "ONLINE", "", null, null, false);
    assertThat(DeviceIdentityConfidenceEngine.mobileFamily(network)).isEmpty();
  }

  @Test void verifiedWindowsAndLinuxCannotMerge() {
    var windows = d("host", "192.168.2.1", "AA:BB:CC:DD:EE:FF", "XYZ", "WinRM");
    var linux = d("host", "192.168.2.1", "AA:BB:CC:DD:EE:FF", "XYZ", "Linux SSH");
    assertThat(engine.assess(windows,linux).decision()).isEqualTo(DeviceIdentityConfidenceEngine.Decision.DISTINCT);
  }

  private static DiscoveredDevice d(String name,String ip,String mac,String serial,String protocol) {
    return new DiscoveredDevice(name,name,"Windows-PC",ip,mac,protocol,"ONLINE","Dell",serial,"2026-07-20",false);
  }
}
