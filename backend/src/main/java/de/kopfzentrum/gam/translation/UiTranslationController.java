package de.kopfzentrum.gam.translation;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ui-translations")
public class UiTranslationController {
  private final UiTranslationService service;

  public UiTranslationController(UiTranslationService service) {
    this.service = service;
  }

  @PostMapping
  public Map<String, String> translate(@RequestBody UiTranslationRequest request) {
    return service.translateUi(request.language(), request.entries(), request.knownTranslations());
  }

  @PostMapping("/live")
  public Map<String, String> translateLive(@RequestBody UiTranslationRequest request) {
    return service.translateLive(request.language(), request.entries());
  }

  public record UiTranslationRequest(String language, Map<String, String> entries, Map<String, String> knownTranslations) {}
}
