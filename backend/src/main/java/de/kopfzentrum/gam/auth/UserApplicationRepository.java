package de.kopfzentrum.gam.auth;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserApplicationRepository {
  private final JdbcTemplate jdbc;
  public UserApplicationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<UserApplicationAccess> findByUsername(String username) {
    if (username == null || username.isBlank()) return List.of();
    return jdbc.query("""
      SELECT ID, USERNAME, APPLICATION, RGESELLSCHAFTS_ID, ROLE
      FROM userapplication
      WHERE USERNAME = ?
      ORDER BY APPLICATION, RGESELLSCHAFTS_ID, ROLE
      """, (rs, row) -> new UserApplicationAccess(
        rs.getInt("ID"), rs.getString("USERNAME"), rs.getString("APPLICATION"),
        (Integer) rs.getObject("RGESELLSCHAFTS_ID"), rs.getString("ROLE")
      ), username.trim());
  }

  public List<UserApplicationAccess> find(String username, String application, Integer companyId) {
    if (username == null || username.isBlank() || application == null || application.isBlank()) return List.of();
    if (companyId == null) {
      return jdbc.query("""
        SELECT ID, USERNAME, APPLICATION, RGESELLSCHAFTS_ID, ROLE
        FROM userapplication
        WHERE USERNAME = ? AND APPLICATION = ?
        ORDER BY RGESELLSCHAFTS_ID IS NULL DESC, RGESELLSCHAFTS_ID
        """, (rs, row) -> new UserApplicationAccess(
          rs.getInt("ID"), rs.getString("USERNAME"), rs.getString("APPLICATION"),
          (Integer) rs.getObject("RGESELLSCHAFTS_ID"), rs.getString("ROLE")
        ), username.trim(), application.trim());
    }
    return jdbc.query("""
      SELECT ID, USERNAME, APPLICATION, RGESELLSCHAFTS_ID, ROLE
      FROM userapplication
      WHERE USERNAME = ? AND APPLICATION = ? AND (RGESELLSCHAFTS_ID = ? OR RGESELLSCHAFTS_ID IS NULL)
      ORDER BY RGESELLSCHAFTS_ID IS NULL ASC
      """, (rs, row) -> new UserApplicationAccess(
        rs.getInt("ID"), rs.getString("USERNAME"), rs.getString("APPLICATION"),
        (Integer) rs.getObject("RGESELLSCHAFTS_ID"), rs.getString("ROLE")
      ), username.trim(), application.trim(), companyId);
  }

  public boolean hasApplication(String username, String application, Integer companyId) {
    return !find(username, application, companyId).isEmpty();
  }

  public boolean canReport(String username, String application, Integer companyId) {
    return find(username, application, companyId).stream().anyMatch(a -> a.isMainUser() || a.isAdmin());
  }

  public boolean canWrite(String username, String application, Integer companyId) {
    return find(username, application, companyId).stream().anyMatch(UserApplicationAccess::isAdmin);
  }

  public Set<String> frontendModulesFor(String username) {
    Set<String> modules = new LinkedHashSet<>();
    modules.add("dashboard");
    for (UserApplicationAccess a : findByUsername(username)) {
      switch (a.application()) {
        case "Rechnungsprogramm" -> {
          modules.add("invoices");
          if (a.isMainUser() || a.isAdmin()) modules.add("reports");
        }
        case "Preisliste" -> modules.add("priceList");
        case "Geraeteverzeichnis" -> modules.add("inventory");
        case "Lagerverwaltung" -> modules.add("warehouse");
        case "Patientenverwaltung", "Patienten", "Adressen", "Adressverwaltung" -> modules.add("patients");
        case "Terminverwaltung", "Termine", "Kalender" -> modules.add("appointments");
        case "Aufgabenverwaltung" -> modules.add("tasks");
        case "Freigabemanagement" -> modules.add("approvals");
        case "Bestelltool" -> modules.add("orders");
        case "Kommunikation" -> modules.add("communication");
        case "Rechteverwaltung", "Benutzer/Rechte" -> modules.add("users");
        case "Personaldaten" -> modules.add("personnel");
        case "Kassenbuch" -> modules.add("cashbook");
        case "MOH Auswertung" -> modules.add("reports");
        case "Arbeitsplatzausstattung", "Arbeitsplatz", "Arbeitsplätze" -> modules.add("workplace");
        default -> { }
      }
    }
    return modules;
  }
}
