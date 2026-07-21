package de.kopfzentrum.gam.inventory;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/discovery/fritzbox")
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
public class FritzBoxSettingsController {
  private final FritzBoxSettingsRepository settings; private final FritzBoxDeviceSource source;
  public FritzBoxSettingsController(FritzBoxSettingsRepository settings,FritzBoxDeviceSource source){this.settings=settings;this.source=source;}
  @GetMapping("/sources") public List<FritzBoxSettingsRepository.FritzBoxSettingsView> list(){return settings.loadViews();}
  @PostMapping("/sources") public FritzBoxSettingsRepository.FritzBoxSettingsView create(@RequestBody FritzBoxSettingsRepository.FritzBoxSettingsUpdate update,@AuthenticationPrincipal AuthenticatedUser user){return settings.create(update,user==null?"system":user.getUsername());}
  @PutMapping("/sources/{id}") public FritzBoxSettingsRepository.FritzBoxSettingsView put(@PathVariable long id,@RequestBody FritzBoxSettingsRepository.FritzBoxSettingsUpdate update,@AuthenticationPrincipal AuthenticatedUser user){return settings.save(id,update,user==null?"system":user.getUsername());}
  @DeleteMapping("/sources/{id}") public void delete(@PathVariable long id){settings.delete(id);}
  @PostMapping("/sources/{id}/test") public FritzBoxDeviceSource.FritzTestResult test(@PathVariable long id){return source.test(id);}
}
