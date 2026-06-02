package de.kopfzentrum.gam.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final LoginService loginService;
  private final JwtService jwtService;
  private final RoleCatalog roleCatalog;
  private final AccountRepository accounts;
  private final LegacyPasswordVerifier passwordVerifier;
  private final TotpVerifier totpVerifier;
  private final PasskeyRepository passkeys;
  private static final Map<String, String> PASSKEY_CHALLENGES = new ConcurrentHashMap<>();

  public AuthController(LoginService loginService, JwtService jwtService, RoleCatalog roleCatalog, AccountRepository accounts, LegacyPasswordVerifier passwordVerifier, TotpVerifier totpVerifier, PasskeyRepository passkeys) {
    this.loginService = loginService;
    this.jwtService = jwtService;
    this.roleCatalog = roleCatalog;
    this.accounts = accounts;
    this.passwordVerifier = passwordVerifier;
    this.totpVerifier = totpVerifier;
    this.passkeys = passkeys;
  }

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest request) {
    LoginDecision decision = loginService.login(request);
    return new LoginResponse(
      jwtService.create(decision.account(), decision.mode()),
      AccountDto.from(decision.account()),
      decision.mode().name(),
      jwtService.jwtMinutes()
    );
  }

  @PostMapping("/totp/setup")
  public TotpSetupResponse setupTotp(@RequestBody TotpSetupRequest request) {
    Account account = accounts.findByUsername(request.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden"));
    String secret = generateBase32Secret();
    return new TotpSetupResponse(account.username(), secret, otpauthUri(account.username(), secret), account.hasTwoFactorSecret());
  }

  @PostMapping("/totp/confirm")
  public AccountDto confirmTotp(@RequestBody TotpConfirmRequest request) {
    Account account = accounts.findByUsername(request.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden"));
    if (request.secret() == null || request.secret().isBlank() || !totpVerifier.verify(request.secret(), request.totpCode())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "2FA-Code konnte nicht bestaetigt werden");
    }
    return AccountDto.from(accounts.updateSecretKeyByUsername(
      request.username().trim(),
      request.secret().trim().toUpperCase(Locale.ROOT)
    ));
  }

  @GetMapping("/passkey/status")
  public java.util.Map<String,Object> passkeyStatus() {
    return java.util.Map.of(
      "prepared", true,
      "table", "account_passkeys",
      "mode", "local-webauthn-test",
      "note", "Passkey/WebAuthn ist als lokaler Testworkflow aktiv. Fuer Produktivbetrieb muss die Signaturpruefung vollstaendig gehärtet werden."
    );
  }

  @PostMapping("/passkey/register/options")
  public PasskeyOptionsResponse passkeyRegisterOptions(@RequestBody PasskeyUsernameRequest request) {
    Account account = accounts.findByUsername(request.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden"));
    String challenge = randomBase64Url(32);
    PASSKEY_CHALLENGES.put("register:" + account.username(), challenge);
    return new PasskeyOptionsResponse(account.username(), Integer.toString(account.id()), challenge, "localhost", "GAM 2.0", passkeys.findCredentialIdsByUsername(account.username()));
  }

  @PostMapping("/passkey/register/finish")
  public AccountDto passkeyRegisterFinish(@RequestBody PasskeyRegisterFinishRequest request) {
    Account account = accounts.findByUsername(request.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden"));
    String expected = PASSKEY_CHALLENGES.remove("register:" + account.username());
    if (expected == null || !expected.equals(request.challenge())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey-Challenge ungueltig oder abgelaufen");
    }
    if (request.credentialId() == null || request.credentialId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credential-ID fehlt");
    }
    passkeys.save(account.id(), request.credentialId(), request.publicKey() == null ? "" : request.publicKey(), request.deviceName());
    return AccountDto.from(account);
  }

  @PostMapping("/passkey/login/options")
  public PasskeyOptionsResponse passkeyLoginOptions(@RequestBody PasskeyUsernameRequest request) {
    Account account = accounts.findByUsername(request.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden"));
    String challenge = randomBase64Url(32);
    PASSKEY_CHALLENGES.put("login:" + account.username(), challenge);
    return new PasskeyOptionsResponse(account.username(), Integer.toString(account.id()), challenge, "localhost", "GAM 2.0", passkeys.findCredentialIdsByUsername(account.username()));
  }

  @PostMapping("/passkey/login/finish")
  public LoginResponse passkeyLoginFinish(@RequestBody PasskeyLoginFinishRequest request) {
    Account account = accounts.findByUsername(request.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden"));
    String expected = PASSKEY_CHALLENGES.remove("login:" + account.username());
    if (expected == null || !expected.equals(request.challenge())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey-Challenge ungueltig oder abgelaufen");
    }
    if (!passkeys.existsActiveForUsername(account.username(), request.credentialId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey nicht bekannt");
    }
    return new LoginResponse(
      jwtService.create(account, LoginMode.PASSKEY),
      AccountDto.from(account),
      LoginMode.PASSKEY.name(),
      jwtService.jwtMinutes()
    );
  }

  private String randomBase64Url(int bytes) {
    byte[] data = new byte[bytes];
    new SecureRandom().nextBytes(data);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
  }

  private Account passwordVerifiedAccount(String username, String password) {
    Account account = accounts.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden"));
    if (!passwordVerifier.matchesSha256(password, account.password())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passwort falsch");
    }
    return account;
  }

  private String otpauthUri(String username, String secret) {
    String label = URLEncoder.encode("GAM:" + username, StandardCharsets.UTF_8);
    String issuer = URLEncoder.encode("GAM 2.0", StandardCharsets.UTF_8);
    return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30";
  }

  private String generateBase32Secret() {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    byte[] bytes = new byte[20];
    new SecureRandom().nextBytes(bytes);
    StringBuilder sb = new StringBuilder(32);
    int buffer = 0;
    int bitsLeft = 0;
    for (byte b : bytes) {
      buffer = (buffer << 8) | (b & 0xff);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        sb.append(alphabet.charAt((buffer >> (bitsLeft - 5)) & 31));
        bitsLeft -= 5;
      }
    }
    if (bitsLeft > 0) {
      sb.append(alphabet.charAt((buffer << (5 - bitsLeft)) & 31));
    }
    return sb.toString();
  }

  @GetMapping("/me")
  public AccountDto me(@AuthenticationPrincipal AuthenticatedUser user) {
    return user == null ? null : user.dto();
  }

  @GetMapping("/menu")
  public RoleDto menu(@AuthenticationPrincipal AuthenticatedUser user) {
    return user == null ? roleCatalog.describe("viewer") : roleCatalog.describe(user.account());
  }

  public record PasskeyUsernameRequest(String username) {}
  public record PasskeyOptionsResponse(String username, String userId, String challenge, String rpId, String rpName, java.util.List<String> allowCredentialIds) {}
  public record PasskeyRegisterFinishRequest(String username, String challenge, String credentialId, String publicKey, String deviceName) {}
  public record PasskeyLoginFinishRequest(String username, String challenge, String credentialId) {}
}
