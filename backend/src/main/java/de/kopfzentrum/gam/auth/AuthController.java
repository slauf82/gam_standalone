package de.kopfzentrum.gam.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final LoginService loginService;
  private final JwtService jwtService;
  private final RoleCatalog roleCatalog;

  public AuthController(LoginService loginService, JwtService jwtService, RoleCatalog roleCatalog) {
    this.loginService = loginService;
    this.jwtService = jwtService;
    this.roleCatalog = roleCatalog;
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

  @GetMapping("/me")
  public AccountDto me(@AuthenticationPrincipal AuthenticatedUser user) {
    return user == null ? null : user.dto();
  }

  @GetMapping("/menu")
  public RoleDto menu(@AuthenticationPrincipal AuthenticatedUser user) {
    return user == null ? roleCatalog.describe("viewer") : roleCatalog.describe(user.account().normalizedRole());
  }
}
