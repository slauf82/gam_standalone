package de.kopfzentrum.gam.modules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Schritt 5: Rahmen fuer die restlichen GAM-Module.
 *
 * Absicht: Erst lesen/anzeigen, noch keine riskanten Schreiboperationen.
 * Dadurch kann die alte Fachlogik kontrolliert rekonstruiert werden, ohne bestehende Daten zu veraendern.
 */
@RestController
@RequestMapping("/api/gam")
public class GamModuleController {
  private final JdbcTemplate jdbc;

  public GamModuleController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/modules")
  public List<ModuleOverview> modules() {
    return List.of(
      overview("settings", "Einstellungen", "gam_settings", "Zentrale GAM- und Workflow-Einstellungen"),
      overview("tasks", "Aufgaben", "aufgaben", "Aufgabenverwaltung aus dem alten GAM"),
      overview("approvals", "Freigaben", "freigabe", "Freigabe-/Entscheidungsmodul"),
      overview("personnel", "Personal", "personal", "Mitarbeiter, Konten und interne Zuordnungen"),
      overview("cashbook", "Kassenbuch", "kassenbuch", "Kassenbuch und Mandantenbezug"),
      overview("workplace", "Arbeitsplatzausstattung", "arbeitsplatz", "Arbeitsplätze, Einrichtung und Arbeitsplatzstatus"),
      overview("priceList", "Preisliste", "preisliste", "Preislisten- und Lieferantenpreise"),
      overviewMulti("compliance", "Prüfungen", List.of("kontrolle", "inbetriebnahme", "einweisung"), "Prüfungen, Inbetriebnahmen und Einweisungen als GDS-Modul"),
      overview("folders", "Ordnerfreigaben", "ordnerfreigabe", "Arbeitsplatz- und Ordnerfreigaben"),
      overview("news", "News", "news", "Startseiten-/Informationsmodul"),
      overview("reports", "Reports/Exporte", "rechnung", "Berichte aus Rechnungen, Inventar, Lager und Aufgaben")
    );
  }

  @GetMapping("/tasks")
  public List<ModuleRecord> tasks(@RequestParam(defaultValue = "100") int limit) {
    return query("tasks", """
      SELECT ID, USERNAME, TAGESDATUM, `KÜRZEL` AS KUERZEL, FILIALE_ID, FACHBEREICH,
             AUFGABE, VERANTWORTLICHER, `PRIORITÄT` AS PRIORITAET, STATUS, FRIST, ERLEDIGT, BEMERKUNG
      FROM aufgaben ORDER BY COALESCE(FRIST, TAGESDATUM) DESC, ID DESC LIMIT ?
      """, limit);
  }

  @GetMapping("/approvals")
  public List<ModuleRecord> approvals(@RequestParam(defaultValue = "100") int limit) {
    return query("approvals", """
      SELECT ID, DATUM, EINTRAGENDER, BESCHREIBUNG, GESELLSCHAFT_ID, FILIALE_ID, STATUS, BEMERKUNG
      FROM freigabe ORDER BY DATUM DESC, ID DESC LIMIT ?
      """, limit);
  }

  @GetMapping("/personnel")
  public List<ModuleRecord> personnel(@RequestParam(defaultValue = "150") int limit) {
    return query("personnel", """
      SELECT ID, NAME, VORNAME, STATUS, POSITION, FILIALE_ID, EMAIL, TELEFON, TELEFON2,
             DIENSTHANDY, VPN_TOKEN, DIENSTLAPTOP, BEMERKUNG
      FROM personal ORDER BY NAME, VORNAME LIMIT ?
      """, limit);
  }

  @GetMapping("/cashbook")
  public List<ModuleRecord> cashbook(@RequestParam(defaultValue = "100") int limit) {
    return query("cashbook", """
      SELECT k.ID, k.DATUM, k.`GESCHÄFTSVORGANG` AS GESCHAEFTSVORGANG, k.STEUER,
             k.EINNAHMEN, k.AUSGABEN, k.BESTAND, k.GEGENKONTO, k.MANDANTENNUMMER, o.FIRMA
      FROM kassenbuch k LEFT JOIN kassenbuchoben o ON o.MANDANTENNUMMER = k.MANDANTENNUMMER
      ORDER BY k.DATUM DESC, k.ID DESC LIMIT ?
      """, limit);
  }

  @GetMapping("/compliance")
  public Map<String, List<ModuleRecord>> compliance(@RequestParam(defaultValue = "100") int limit) {
    return Map.of(
      "controls", query("controls", """
        SELECT KONTROLL_ID, `AUTORISIERTERPRÜFER` AS AUTORISIERTERPRUEFER, `GERÄTE` AS GERAETE,
               `DATUMLETZTEPRÜFUNG_STK` AS DATUMLETZTEPRUEFUNG_STK, INTERVALL_STK,
               `DATUMLETZTEPRÜFUNG_MTK` AS DATUMLETZTEPRUEFUNG_MTK, INTERVALL_MTK,
               `DATUMLETZTEPRÜFUNG_BGV_A3` AS DATUMLETZTEPRUEFUNG_BGV_A3, INTERVALL_BGV_A3,
               AKTUELLE_PRÜFPLAKETTE AS AKTUELLE_PRUEFPLAKETTE, WARTUNGSVERTRAG
        FROM kontrolle ORDER BY KONTROLL_ID DESC LIMIT ?
        """, limit),
      "commissioning", query("commissioning", """
        SELECT INBETRIEBNAHME_ID, DATUMINBETRIEBNAHME, DATUMFUNKTIONSKONTROLLE, `GERÄTE` AS GERAETE
        FROM inbetriebnahme ORDER BY INBETRIEBNAHME_ID DESC LIMIT ?
        """, limit),
      "instructions", query("instructions", """
        SELECT EINWEISUNGS_ID, DATUMERSTEINWEISUNG, NAMEERSTEINGEWIESENER, INBETRIEBNAHME_ID,
               DATUMFOLGEEINWEISUNG, NAMEFOLGEEINGEWIESENER
        FROM einweisung ORDER BY EINWEISUNGS_ID DESC LIMIT ?
        """, limit)
    );
  }

  @GetMapping("/folders")
  public List<ModuleRecord> folders(@RequestParam(defaultValue = "100") int limit) {
    return query("folders", """
      SELECT ofg.ID, ofg.ORDNERFREIGABE, ofg.ARBEITSPLATZ_ID, a.ARBEITSPLATZ, a.RECHNERNAME, a.IP_ADRESSE
      FROM ordnerfreigabe ofg LEFT JOIN arbeitsplatz a ON a.ID = ofg.ARBEITSPLATZ_ID
      ORDER BY ofg.ID DESC LIMIT ?
      """, limit);
  }

  @GetMapping("/news")
  public List<ModuleRecord> news(@RequestParam(defaultValue = "50") int limit) {
    return query("news", "SELECT ID, NEWS FROM news ORDER BY ID DESC LIMIT ?", limit);
  }

  @GetMapping("/reports/summary")
  public Map<String, Object> reportSummary() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("invoices", count("rechnung"));
    result.put("invoiceLines", count("rechnungsdetails"));
    result.put("inventoryLegacy", count("geräte"));
    result.put("inventoryNew", count("geräte_neu"));
    result.put("warehouse", count("lager"));
    result.put("consumables", count("verbrauchsmaterial"));
    result.put("tasks", count("aufgaben"));
    result.put("approvals", count("freigabe"));
    result.put("personnel", count("personal"));
    result.put("note", "Schritt 5 liefert vorerst eine sichere Lese-/Uebersichtsgrundlage. Detailberichte und Exporte folgen modulweise.");
    return result;
  }

  private ModuleOverview overview(String key, String label, String tableName, String note) {
    long c = count(tableName);
    return new ModuleOverview(key, label, tableName, c, c >= 0 ? "angebunden" : "nicht verfuegbar", note);
  }

  private ModuleOverview overviewMulti(String key, String label, List<String> tableNames, String note) {
    long total = 0;
    boolean anyAvailable = false;
    for (String tableName : tableNames) {
      long c = count(tableName);
      if (c >= 0) {
        total += c;
        anyAvailable = true;
      }
    }
    return new ModuleOverview(
      key,
      label,
      String.join(", ", tableNames),
      anyAvailable ? total : -1,
      anyAvailable ? "angebunden" : "nicht verfuegbar",
      note
    );
  }

  private long count(String table) {
    if (table == null || !table.matches("[A-Za-z0-9_ÄÖÜäöüß]+")) {
      return -1;
    }
    try { return jdbc.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class); }
    catch (Exception ex) { return -1; }
  }

  private List<ModuleRecord> query(String module, String sql, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 500));
    try {
      return jdbc.queryForList(sql, safeLimit).stream().map(row -> new ModuleRecord(module, row)).toList();
    } catch (Exception ex) {
      List<ModuleRecord> fallback = new ArrayList<>();
      fallback.add(new ModuleRecord(module, Map.of("error", ex.getMessage())));
      return fallback;
    }
  }
}
