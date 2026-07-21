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

  private static DiscoveredDevice d(String name,String ip,String mac,String serial,String protocol) {
    return new DiscoveredDevice(name,name,"Windows-PC",ip,mac,protocol,"ONLINE","Dell",serial,"2026-07-20",false);
  }
}
