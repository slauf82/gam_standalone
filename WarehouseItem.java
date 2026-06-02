package de.kopfzentrum.gam.admin;

import de.kopfzentrum.gam.auth.Account;

public record AccountAdminDto(
  Integer id,
  String username,
  String fullname,
  String role,
  String email,
  boolean twoFactorConfigured,
  boolean passwordPresent
) {
  public static AccountAdminDto from(Account a) {
    return new AccountAdminDto(
      a.id(), a.username(), a.fullname(), a.normalizedRole(), a.email(), a.hasTwoFactorSecret(),
      a.password() != null && !a.password().isBlank()
    );
  }
}
