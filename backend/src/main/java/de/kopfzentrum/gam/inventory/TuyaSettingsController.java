package de.kopfzentrum.gam.inventory;
import de.kopfzentrum.gam.auth.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/discovery/tuya")
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
public class TuyaSettingsController {
  private final TuyaSettingsRepository settings;
  private final TuyaDeviceSource source;
  public TuyaSettingsController(TuyaSettingsRepository s,TuyaDeviceSource d){settings=s;source=d;}
  @GetMapping("/sources") public List<TuyaSettingsRepository.View> list(){return settings.loadViews();}
  @PostMapping("/sources") public TuyaSettingsRepository.View create(@RequestBody TuyaSettingsRepository.Update u,@AuthenticationPrincipal AuthenticatedUser a){return settings.create(u,a==null?"system":a.getUsername());}
  @PutMapping("/sources/{id}") public TuyaSettingsRepository.View save(@PathVariable long id,@RequestBody TuyaSettingsRepository.Update u,@AuthenticationPrincipal AuthenticatedUser a){return settings.save(id,u,a==null?"system":a.getUsername());}
  @DeleteMapping("/sources/{id}") public TuyaSettingsRepository.DeleteResult delete(@PathVariable long id){return settings.delete(id);}
  @DeleteMapping("/sources") public TuyaSettingsRepository.DeleteAllResult deleteAll(){return settings.deleteAll();}
  @PostMapping("/sources/{id}/test") public TuyaDeviceSource.TestResult test(@PathVariable long id){return source.test(id);}
}
