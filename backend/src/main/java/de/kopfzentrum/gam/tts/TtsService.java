package de.kopfzentrum.gam.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Service
public class TtsService {
  private final boolean enabled;
  private final String engine;
  private final boolean browserFallback;
  private final boolean maryEnabled;
  private final String maryEndpoint;
  private final MaryTtsRuntimeManager maryRuntime;
  private final RestTemplate restTemplate = new RestTemplate();
  private final Map<String, List<String>> fallback;

  public TtsService(
    @Value("${app.tts.enabled:true}") boolean enabled,
    @Value("${app.tts.engine:marytts}") String engine,
    @Value("${app.tts.browser-fallback:true}") boolean browserFallback,
    @Value("${app.tts.marytts.enabled:true}") boolean maryEnabled,
    @Value("${app.tts.marytts.endpoint:http://localhost:59125/process}") String maryEndpoint,
    MaryTtsRuntimeManager maryRuntime,
    @Value("${app.tts.fallback.de:de-DE,de,en-US,en}") String de,
    @Value("${app.tts.fallback.en:en-US,en-GB,en,de-DE,de}") String en,
    @Value("${app.tts.fallback.fr:fr-FR,fr,en-US,en,de-DE,de}") String fr,
    @Value("${app.tts.fallback.uk:uk-UA,uk,ru-RU,ru,en-US,en,de-DE,de}") String uk,
    @Value("${app.tts.fallback.it:it-IT,it,en-US,en,de-DE,de}") String it,
    @Value("${app.tts.fallback.sv:sv-SE,sv,en-US,en,de-DE,de}") String sv,
    @Value("${app.tts.fallback.tr:tr-TR,tr,en-US,en,de-DE,de}") String tr,
    @Value("${app.tts.fallback.ru:ru-RU,ru,en-US,en,de-DE,de}") String ru
  ) {
    this.enabled = enabled;
    this.engine = (engine == null || engine.isBlank()) ? "marytts" : engine;
    this.browserFallback = browserFallback;
    this.maryEnabled = maryEnabled;
    this.maryEndpoint = maryEndpoint;
    this.maryRuntime = maryRuntime;
    this.fallback = Map.of(
      "de", split(de),
      "en", split(en),
      "fr", split(fr),
      "uk", split(uk),
      "it", split(it),
      "sv", split(sv),
      "tr", split(tr),
      "ru", split(ru)
    );
  }

  public TtsStatus status() {
    return new TtsStatus(
      enabled,
      engine,
      "bundled",
      maryEnabled,
      maryRuntime.bundled(),
      maryRuntime.bundledStarted(),
      maryRuntime.bundledAvailable(),
      maryRuntime.bundledError(),
      maryRuntime.home(),
      maryEndpoint,
      maryReachable(),
      browserFallback,
      fallback
    );
  }

  public ResponseEntity<byte[]> audio(TtsRequest request) {
    if (!enabled || !maryEnabled) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new byte[0]);
    String text = request.text() == null ? "" : request.text();
    if (text.isBlank()) return ResponseEntity.badRequest().body(new byte[0]);

    LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("INPUT_TYPE", "TEXT");
    form.add("OUTPUT_TYPE", "AUDIO");
    form.add("AUDIO", "WAVE_FILE");
    form.add("LOCALE", maryLocale(request.language()));
    form.add("INPUT_TEXT", text);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    try {
      ResponseEntity<byte[]> response = restTemplate.postForEntity(URI.create(maryEndpoint), new HttpEntity<>(form, headers), byte[].class);
      return ResponseEntity.status(response.getStatusCode())
        .contentType(MediaType.valueOf("audio/wav"))
        .body(response.getBody() == null ? new byte[0] : response.getBody());
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("X-GAM-TTS-Error", "marytts-unavailable")
        .body(new byte[0]);
    }
  }

  private boolean maryReachable() {
    try {
      String base = maryEndpoint.replace("/process", "/version");
      return restTemplate.getForEntity(base, String.class).getStatusCode().is2xxSuccessful();
    } catch (Exception ex) {
      return false;
    }
  }

  private String maryLocale(String lang) {
    String l = lang == null ? "de" : lang.toLowerCase(Locale.ROOT);
    if (l.startsWith("en")) return "en_US";
    if (l.startsWith("fr")) return "fr";
    if (l.startsWith("uk") || l.startsWith("ru")) return "ru";
    if (l.startsWith("it")) return "it";
    if (l.startsWith("sv")) return "sv";
    if (l.startsWith("tr")) return "tr";
    return "de";
  }

  private static List<String> split(String csv) {
    if (csv == null || csv.isBlank()) return List.of();
    return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
  }
}
