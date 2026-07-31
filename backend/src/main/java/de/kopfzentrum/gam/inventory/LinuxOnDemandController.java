package de.kopfzentrum.gam.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 40k33b6b: Liest die erweiterte Linux-Analyse für einen einzelnen,
 * einklappbaren Bereich - ausschließlich auf Anforderung des Benutzers
 * (Lazy Loading), niemals automatisch während eines Suchlaufs. Die
 * Identitätsauflösung (Schlüssel -> IP-Adresse) nutzt ausschließlich die
 * bereits vorhandene DiscoveryRegistrationRepository - keine zweite
 * Geräteverwaltung, keine neue Datenstruktur.
 */
@RestController
@RequestMapping("/api/inventory/discovery/linux")
public class LinuxOnDemandController {
  private final LinuxNetworkDiscoveryService linux;
  private final DiscoveryRegistrationRepository registrations;

  public LinuxOnDemandController(LinuxNetworkDiscoveryService linux, DiscoveryRegistrationRepository registrations) {
    this.linux = linux;
    this.registrations = registrations;
  }

  @GetMapping("/section")
  public LinuxNetworkDiscoveryService.OnDemandSection section(@RequestParam String identityKey, @RequestParam String section) {
    Map<String,Object> device = registrations.find(identityKey);
    Object address = device.get("address");
    if (address == null || String.valueOf(address).isBlank()) {
      throw new IllegalArgumentException("Für dieses Gerät ist keine IP-Adresse bekannt - eine erweiterte Linux-Analyse ist nicht möglich.");
    }
    return linux.fetchSection(String.valueOf(address), section);
  }
}
