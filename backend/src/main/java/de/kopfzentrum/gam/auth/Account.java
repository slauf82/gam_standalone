package de.kopfzentrum.gam.auth;

import java.util.Locale;

/**
 * Kompatibles Mapping der bestehenden Tabelle `accounts` aus dem alten GAM.
 * Die Spaltennamen bleiben bewusst unverändert, damit die vorhandene Datenbank
 * ohne Migration weiterverwendet werden kann.
 */
public record Account(
    Integer id,
    String username,
    String password,
    String fullname,
    String role,
    String email,
    String secretkey
) {
  public String normalizedRole() {
    if (role == null || role.isBlank()) return "user";
    return role.trim().toLowerCase(Locale.ROOT);
  }

  public boolean hasTwoFactorSecret() {
    return secretkey != null && !secretkey.isBlank();
  }
}
