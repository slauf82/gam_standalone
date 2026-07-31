package de.kopfzentrum.gam.inventory;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/discovery/merge")
public class DeviceMergeController {
  private final DeviceMergeService service;
  public DeviceMergeController(DeviceMergeService service) { this.service = service; }

  @GetMapping("/candidates")
  public List<DeviceMergeService.Candidate> candidates() {
    return service.findCandidates();
  }

  @PostMapping("/preview")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public DeviceMergeService.MergePreviewResult preview(@RequestBody Map<String,Object> request) {
    return service.preview(targetKey(request), sourceKeys(request), overrides(request));
  }

  @PostMapping("/confirm")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> confirm(@RequestBody Map<String,Object> request, Principal principal) {
    return service.confirm(targetKey(request), sourceKeys(request), overrides(request),
      principal == null ? null : principal.getName());
  }

  @PostMapping("/ignore")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public Map<String,Object> ignore(@RequestBody Map<String,Object> request, Principal principal) {
    Object a = request.get("keyA"), b = request.get("keyB");
    if (a == null || b == null) throw new IllegalArgumentException("Beide Geräte-Schlüssel werden benötigt.");
    service.ignore(String.valueOf(a), String.valueOf(b), principal == null ? null : principal.getName());
    return Map.of("ignored", true);
  }

  @GetMapping("/log")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public List<Map<String,Object>> log(@RequestParam(defaultValue = "50") int limit) {
    return service.auditLog(limit);
  }

  private static String targetKey(Map<String,Object> request) {
    Object value = request.get("targetKey");
    if (value == null) throw new IllegalArgumentException("Zielgerät fehlt.");
    return String.valueOf(value);
  }

  @SuppressWarnings("unchecked")
  private static List<String> sourceKeys(Map<String,Object> request) {
    Object value = request.get("sourceKeys");
    if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Mindestens ein weiteres Gerät wird benötigt.");
    return list.stream().map(String::valueOf).toList();
  }

  @SuppressWarnings("unchecked")
  private static Map<String,Object> overrides(Map<String,Object> request) {
    Object value = request.get("overrides");
    return value instanceof Map<?,?> map ? (Map<String,Object>) map : Map.of();
  }
}
