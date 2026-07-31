package de.kopfzentrum.gam.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 40k34i: Evidenzbasierte Linux-Nachklassifizierung - dritter Anwendungsfall
 * der generischen {@link DeviceEvidenceEngine} (nach Android/Windows).
 *
 * Es gab bisher KEINE eigenständige Linux-Klassifizierungsklasse - die
 * eigentliche Unterkategorie-Entscheidung (Server/Client/Container-Host/…)
 * geschieht bereits sehr differenziert innerhalb von
 * {@link LinuxNetworkDiscoveryService#classify}, ABER erst NACHDEM Linux
 * durch einen der bestehenden Trigger (Hostname, SSH+Cockpit, SNMP-Hinweis
 * usw.) bereits bestätigt wurde. Diese neue, schlanke Klasse deckt den davor
 * liegenden Fall ab: ein Gerät, das (noch) generisch klassifiziert ist,
 * obwohl bereits ausreichend Linux-Evidence aus anderen, bereits vorhandenen
 * Quellen vorliegt (z.B. eine SNMP-Systembeschreibung, die "Linux" nennt).
 * Reine Auswertung bereits vorhandener Textfelder - kein neuer SSH-/SNMP-/
 * HTTP-Aufruf.
 *
 * Wichtig (siehe Auftrag): SSH allein bestätigt Linux NICHT, da auch macOS,
 * NAS-Systeme, Netzwerkgeräte oder andere Unix-Systeme SSH anbieten können -
 * SSH-Erreichbarkeit fließt hier nur als NIEDRIGE Priorität ein und reicht
 * für sich genommen nie für eine Nachklassifizierung aus.
 */
public final class LinuxDeviceClassifier {
  private LinuxDeviceClassifier() {}

  /** Bereits vorhandene, konservative Standardkategorie aus LinuxNetworkDiscoveryService - keine neue Kategorie. */
  private static final String GENERIC_CATEGORY = "Computer / Linux";

  private static final List<String> KNOWN_PACKAGE_MANAGERS = List.of("apt", "dnf", "yum", "zypper", "pacman", "apk");

  /**
   * @param currentCategory bereits bekannte, aktuelle Gerätekategorie (nur zur Information/Logging)
   * @param protocol bereits gespeicherter discovery_protocol-Text der Identität, falls vorhanden
   * @param hostname bereits bekannter Anzeigename/Hostname
   */
  public static Optional<DeviceEvidenceEngine.Classification> classifyWithEvidence(
      String currentCategory, String protocol, String hostname) {
    String protocolLower = nullToEmpty(protocol).toLowerCase(Locale.ROOT);
    String hostLower = nullToEmpty(hostname).toLowerCase(Locale.ROOT);
    List<DeviceEvidenceEngine.Evidence> evidence = new ArrayList<>();

    // Sehr hohe Priorität: bereits erfolgreich ausgewertete Linux-Inventardaten
    // (siehe LinuxNetworkDiscoveryService/LinuxInventoryDiscoveryService - "Linux
    // bestätigt: ja" bzw. konkrete Distributions-/Kernel-Felder sind bereits
    // vorhanden), oder eine SNMP-Systembeschreibung, die bereits wörtlich "Linux"
    // meldet (siehe 40k33b9 - dort wurde dieser Fund bereits gespeichert, statt
    // ihn wie zuvor zu verwerfen).
    if (protocolLower.contains("linux bestätigt: ja")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.SEHR_HOCH,
        "Linux bereits durch die bestehende Linux-Nachprüfung bestätigt"));
    }
    if (protocolLower.contains("distributions-id:") || protocolLower.contains("kernel:")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.SEHR_HOCH,
        "Bereits erfasste Linux-Distributions-/Kernel-Angaben vorhanden"));
    }
    if (protocolLower.contains("snmp-systembeschreibung:") && protocolLower.matches(".*snmp-systembeschreibung:\\s*linux.*")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.SEHR_HOCH,
        "SNMP-Systembeschreibung meldet bereits wörtlich \"Linux\""));
    }

    // Hohe Priorität: SSH-Erreichbarkeit NUR zusammen mit einem weiteren
    // Linux-spezifischen Hinweis (Paketmanager, systemd/init, "linux"-Nennung im
    // Protokolltext) - SSH allein zählt bewusst nicht als hohe Priorität.
    boolean sshReachable = protocolLower.contains("ssh-port 22 erreichbar") || protocolLower.contains("ssh (wlan)")
      || protocolLower.contains("erkennungsmerkmale") && protocolLower.contains("ssh");
    boolean packageManagerHint = KNOWN_PACKAGE_MANAGERS.stream().anyMatch(pm -> protocolLower.contains("paketmanager: " + pm) || protocolLower.contains("paketmanager:" + pm));
    boolean systemdHint = protocolLower.contains("systemd") || protocolLower.contains("init-system:");
    if (sshReachable && (packageManagerHint || systemdHint || protocolLower.contains("linux"))) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.HOCH,
        "SSH erreichbar zusammen mit einem weiteren Linux-spezifischen Hinweis"));
    }
    if (packageManagerHint) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.HOCH, "Bereits erkannter Linux-Paketmanager vorhanden"));
    }

    // Mittlere Priorität: Linux-typische Hostnamen/Dienstkombinationen bzw. bereits
    // vorhandene mDNS-/HTTP-/UPnP-Texte mit Linux-Hinweis.
    if (hostLower.startsWith("linux-") || hostLower.contains("ubuntu") || hostLower.contains("debian") || hostLower.contains("raspberrypi")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.MITTEL, "Linux-typisches Hostnamen-Muster"));
    }

    // Niedrige Priorität: SSH allein, ohne jeden weiteren Hinweis.
    if (sshReachable && evidence.isEmpty()) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.NIEDRIG, "Nur SSH erreichbar, kein weiterer Linux-Hinweis"));
    }

    if (evidence.isEmpty()) return Optional.empty();
    return DeviceEvidenceEngine.classify(evidence, GENERIC_CATEGORY, "Linux", null, null);
  }

  private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
