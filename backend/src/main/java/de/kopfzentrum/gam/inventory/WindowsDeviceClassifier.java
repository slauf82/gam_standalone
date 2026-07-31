package de.kopfzentrum.gam.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 40k34i: Evidenzbasierte Windows-Nachklassifizierung - zweiter Anwendungsfall
 * der generischen {@link DeviceEvidenceEngine} (nach Android, 40k34h).
 *
 * Es gab bisher KEINE eigenständige Windows-Klassifizierungsklasse - die
 * Erkennung geschieht implizit innerhalb von {@link WindowsInventoryDiscoveryService}
 * (lokale WMI-/PowerShell-Abfragen, registriert direkt als "Windows-PC"). Diese
 * neue, schlanke Klasse wertet ausschließlich bereits vorhandene, bereits
 * gespeicherte Informationen aus (siehe {@code detail(...)}-Felder in
 * WindowsInventoryDiscoveryService) - keine neue WMI-/WinRM-/Netzwerkabfrage.
 *
 * "Windows-PC" ist aktuell die einzige im Projekt tatsächlich verwendete
 * Windows-Gerätekategorie (keine separate "Windows-Server"/"Laptop"/
 * "Workstation"-Kategorie vorhanden) - es wurde bewusst KEINE neue
 * Kategorienlandschaft erfunden, siehe Dokumentation.
 */
public final class WindowsDeviceClassifier {
  private WindowsDeviceClassifier() {}

  private static final String CATEGORY = "Windows-PC";

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

    // Sehr hohe Priorität: bereits erfolgreiche WMI-/PowerShell-Inventarisierung
    // (siehe WindowsInventoryDiscoveryService - der Protokolltext beginnt in diesem
    // Fall bereits mit "Windows-Inventarisierung"), oder ein bereits erfasster
    // konkreter Windows-Produktname/-Version ("Edition"-Detailfeld).
    if (protocolLower.contains("windows-inventarisierung")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.SEHR_HOCH,
        "Bereits erfolgreiche Windows-Inventarisierung (WMI/PowerShell) vorhanden"));
    }
    if (protocolLower.contains("windows 10") || protocolLower.contains("windows 11") || protocolLower.contains("windows server")
      || (protocolLower.contains("edition:") && protocolLower.contains("windows"))) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.SEHR_HOCH,
        "Windows-Produktname/-Version bereits erfasst"));
    }

    // Hohe Priorität: SMB/NetBIOS, Domäne/Arbeitsgruppe, Windows-Rollen/Features -
    // allesamt bereits vorhandene Detailfelder, kein neuer Aufruf.
    if (protocolLower.contains("smb") || protocolLower.contains("netbios")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.HOCH, "SMB-/NetBIOS-Hinweis bereits vorhanden"));
    }
    if (protocolLower.contains("domäne/arbeitsgruppe:") || protocolLower.contains("workgroup")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.HOCH, "Windows-Domäne/Arbeitsgruppe bereits erfasst"));
    }
    if (protocolLower.contains("windows-rollen/features:")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.HOCH, "Windows-Rollen/Features bereits erfasst"));
    }

    // Mittlere Priorität: typische Windows-Hostnamen-Muster.
    if (hostLower.matches(".*\\b(desktop-|win-|pc-).*") || hostLower.endsWith("-pc")) {
      evidence.add(new DeviceEvidenceEngine.Evidence(DeviceEvidenceEngine.Tier.MITTEL, "Windows-typisches Hostnamen-Muster"));
    }

    if (evidence.isEmpty()) return Optional.empty();
    return DeviceEvidenceEngine.classify(evidence, CATEGORY, "Windows", null, null);
  }

  private static String nullToEmpty(String value) { return value == null ? "" : value; }
}
