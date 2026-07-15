package de.kopfzentrum.gam.settings;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ModuleSettingsController {
  private final ModuleSettingsRepository settings;
  public ModuleSettingsController(ModuleSettingsRepository settings) { this.settings = settings; }

  @GetMapping("/api/public/module-settings")
  public Map<String, Boolean> publicSettings() { return settings.load(); }

  @GetMapping("/api/settings/modules")
  public Map<String, Boolean> get(@AuthenticationPrincipal AuthenticatedUser user) {
    requireUser(user); return settings.load();
  }

  @PutMapping("/api/settings/modules")
  public Map<String, Boolean> save(@RequestBody Map<String, Boolean> request,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
    requireUser(user); return settings.save(request, user.getUsername());
  }

  private void requireUser(AuthenticatedUser user) {
    if (user == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }
}
