package de.kopfzentrum.gam.security;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import de.kopfzentrum.gam.auth.AuthenticatedUser;

@RestController
@RequestMapping("/api/security")
public class SecurityStatusController {
  private final boolean allowSha256Password;
  private final boolean allowTotpOnly;
  private final boolean requireTotpWhenSecretExists;

  public SecurityStatusController(
      @Value("${app.legacy-login.allow-sha256-password:true}") boolean allowSha256Password,
      @Value("${app.legacy-login.allow-totp-only:true}") boolean allowTotpOnly,
      @Value("${app.legacy-login.require-totp-when-secret-exists:false}") boolean requireTotpWhenSecretExists
  ) {
    this.allowSha256Password = allowSha256Password;
    this.allowTotpOnly = allowTotpOnly;
    this.requireTotpWhenSecretExists = requireTotpWhenSecretExists;
  }

  @GetMapping("/status")
  public Map<String, Object> status(@AuthenticationPrincipal AuthenticatedUser user) {
    return Map.of(
      "authenticated", user != null,
      "username", user == null ? "" : user.getUsername(),
      "role", user == null ? "" : user.account().normalizedRole(),
      "legacySha256PasswordAllowed", allowSha256Password,
      "totpOnlyAllowed", allowTotpOnly,
      "totpRequiredWhenSecretExists", requireTotpWhenSecretExists
    );
  }
}
