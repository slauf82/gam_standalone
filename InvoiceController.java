package de.kopfzentrum.gam.auth;

public record UserApplicationAccess(
  Integer id,
  String username,
  String application,
  Integer companyId,
  String role
) {
  public String normalizedRole() {
    return role == null || role.isBlank() ? "user" : role.trim().toLowerCase();
  }

  public boolean isUser() { return "user".equals(normalizedRole()); }
  public boolean isMainUser() { return "mainuser".equals(normalizedRole()); }
  public boolean isAdmin() { return "admin".equals(normalizedRole()); }
}
