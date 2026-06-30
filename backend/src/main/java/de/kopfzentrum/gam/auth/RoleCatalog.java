package de.kopfzentrum.gam.auth;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Zentrale Rollenbeschreibung fuer GAM 2.0.
 * Die Werte bleiben bewusst textbasiert und kompatibel zur bestehenden Spalte accounts.role.
 */
@Component
public class RoleCatalog {
  private final UserApplicationRepository userApplications;

  public RoleCatalog(UserApplicationRepository userApplications) {
    this.userApplications = userApplications;
  }
  public List<RoleDto> roles() {
    return List.of(
      new RoleDto("user", "Benutzer", false, List.of("dashboard")),
      new RoleDto("mainuser", "Hauptbenutzer", false, List.of("dashboard", "reports")),
      new RoleDto("admin", "Admin", true, List.of("dashboard", "invoices", "inventory", "warehouse", "patients", "appointments", "users", "tasks", "approvals", "orders", "communication", "personnel", "cashbook", "workplace", "priceList", "compliance", "reports", "admin")),
      new RoleDto("superadmin", "Superadmin", true, List.of("dashboard", "invoices", "inventory", "warehouse", "patients", "appointments", "users", "tasks", "approvals", "orders", "communication", "personnel", "cashbook", "workplace", "priceList", "compliance", "reports", "admin")),
      new RoleDto("viewer", "Nur Lesen", false, List.of("dashboard"))
    );
  }

  public boolean isAdmin(Account account) {
    if (account == null) return false;
    String r = account.normalizedRole();
    return r.equals("admin") || r.equals("superadmin");
  }

  public RoleDto describe(Account account) {
    if (account == null) return describe("viewer");
    if (account.isSuperAdmin()) return describe("superadmin");
    var modules = new java.util.ArrayList<>(userApplications.frontendModulesFor(account.username()));
    if (modules.isEmpty()) modules.add("dashboard");
    String label = account.fullname() == null || account.fullname().isBlank() ? account.username() : account.fullname();
    return new RoleDto(account.normalizedRole(), label, false, modules);
  }

  public RoleDto describe(String role) {
    String normalized = role == null || role.isBlank() ? "user" : role.trim().toLowerCase();
    return roles().stream().filter(r -> r.key().equals(normalized)).findFirst()
      .orElse(new RoleDto(normalized, normalized, false, List.of("dashboard")));
  }
}
