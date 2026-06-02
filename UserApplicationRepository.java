package de.kopfzentrum.gam.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import de.kopfzentrum.gam.security.LoginRateLimiter;
import de.kopfzentrum.gam.security.SecurityAuditService;

@Service
public class LoginService {
  private final AccountRepository accounts;
  private final LegacyPasswordVerifier passwordVerifier;
  private final TotpVerifier totpVerifier;
  private final boolean allowSha256Password;
  private final boolean allowTotpOnly;
  private final boolean requireTotpWhenSecretExists;
  private final LoginRateLimiter rateLimiter;
  private final SecurityAuditService audit;

  public LoginService(
      AccountRepository accounts,
      LegacyPasswordVerifier passwordVerifier,
      TotpVerifier totpVerifier,
      @Value("${app.legacy-login.allow-sha256-password}") boolean allowSha256Password,
      @Value("${app.legacy-login.allow-totp-only}") boolean allowTotpOnly,
      @Value("${app.legacy-login.require-totp-when-secret-exists}") boolean requireTotpWhenSecretExists,
      LoginRateLimiter rateLimiter,
      SecurityAuditService audit
  ) {
    this.accounts = accounts;
    this.passwordVerifier = passwordVerifier;
    this.totpVerifier = totpVerifier;
    this.allowSha256Password = allowSha256Password;
    this.allowTotpOnly = allowTotpOnly;
    this.requireTotpWhenSecretExists = requireTotpWhenSecretExists;
    this.rateLimiter = rateLimiter;
    this.audit = audit;
  }

  public LoginDecision login(LoginRequest request) {
    if (request == null || request.username() == null || request.username().isBlank()) {
      throw unauthorized();
    }

    rateLimiter.assertAllowed(request.username());
    Account account = accounts.findByUsername(request.username()).orElseThrow(() -> {
      rateLimiter.failure(request.username());
      audit.loginFailure(request.username());
      return unauthorized();
    });
    boolean passwordOk = allowSha256Password && passwordVerifier.matchesSha256(request.password(), account.password());
    boolean totpOk = account.hasTwoFactorSecret() && totpVerifier.verify(account.secretkey(), request.totpCode());

    if (passwordOk && totpOk) {
      rateLimiter.success(account.username());
      audit.loginSuccess(account.username(), LoginMode.PASSWORD_PLUS_2FA.name());
      return new LoginDecision(account, LoginMode.PASSWORD_PLUS_2FA);
    }

    if (passwordOk && (!account.hasTwoFactorSecret() || !requireTotpWhenSecretExists)) {
      rateLimiter.success(account.username());
      audit.loginSuccess(account.username(), LoginMode.PASSWORD_LEGACY.name());
      return new LoginDecision(account, LoginMode.PASSWORD_LEGACY);
    }

    if (allowTotpOnly && totpOk) {
      rateLimiter.success(account.username());
      audit.loginSuccess(account.username(), LoginMode.TWO_FACTOR_ONLY.name());
      return new LoginDecision(account, LoginMode.TWO_FACTOR_ONLY);
    }

    rateLimiter.failure(account.username());
    audit.loginFailure(account.username());
    throw unauthorized();
  }

  private ResponseStatusException unauthorized() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login fehlgeschlagen");
  }
}
