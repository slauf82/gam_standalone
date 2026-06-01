package de.kopfzentrum.gam.auth;

public record AccountDto(
    Integer id,
    String username,
    String fullname,
    String role,
    String email,
    boolean twoFactorConfigured
) {
  public static AccountDto from(Account account) {
    return new AccountDto(
        account.id(),
        account.username(),
        account.fullname(),
        account.normalizedRole(),
        account.email(),
        account.hasTwoFactorSecret()
    );
  }
}
