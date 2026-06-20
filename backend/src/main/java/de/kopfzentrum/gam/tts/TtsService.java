package de.kopfzentrum.gam.tts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TtsService {
  private final boolean enabled;
  private final boolean browserFallback;
  private final boolean autoDownload;
  private final String piperExecutable;
  private final String voicesDir;
  private final Map<String, List<String>> fallback;
  private final HttpClient httpClient;
  private final Set<String> downloadsInProgress = ConcurrentHashMap.newKeySet();
  private final Map<String, String> downloadErrors = new ConcurrentHashMap<>();

  private static final String HF = "https://huggingface.co/rhasspy/piper-voices/resolve/main/";

  private record PiperVoice(
    String language,
    String label,
    String voiceId,
    String modelUrl,
    String configUrl,
    boolean standardPackage
  ) {}

  private static final Map<String, PiperVoice> PIPER_VOICES = Map.ofEntries(
    voice("de", "Deutsch", "de_DE-thorsten-medium", "de/de_DE/thorsten/medium/de_DE-thorsten-medium", true),
    voice("en", "English", "en_US-lessac-medium", "en/en_US/lessac/medium/en_US-lessac-medium", true),
    voice("fr", "Français", "fr_FR-siwis-medium", "fr/fr_FR/siwis/medium/fr_FR-siwis-medium", true),
    voice("it", "Italiano", "it_IT-paola-medium", "it/it_IT/paola/medium/it_IT-paola-medium", false),
    voice("es", "Español", "es_ES-sharvard-medium", "es/es_ES/sharvard/medium/es_ES-sharvard-medium", false),
    voice("pt", "Português", "pt_PT-tugao-medium", "pt/pt_PT/tugão/medium/pt_PT-tug%C3%A3o-medium", false),
    voice("nl", "Nederlands", "nl_NL-ronnie-medium", "nl/nl_NL/ronnie/medium/nl_NL-ronnie-medium", false),
    voice("pl", "Polski", "pl_PL-gosia-medium", "pl/pl_PL/gosia/medium/pl_PL-gosia-medium", false),
    voice("cs", "Čeština", "cs_CZ-jirka-low", "cs/cs_CZ/jirka/low/cs_CZ-jirka-low", false),
    voice("sv", "Svenska", "sv_SE-nst-medium", "sv/sv_SE/nst/medium/sv_SE-nst-medium", false),
    voice("tr", "Türkçe", "tr_TR-dfki-medium", "tr/tr_TR/dfki/medium/tr_TR-dfki-medium", false),
    voice("ru", "Русский", "ru_RU-ruslan-medium", "ru/ru_RU/ruslan/medium/ru_RU-ruslan-medium", false),
    voice("uk", "Українська", "uk_UA-ukrainian_tts-medium", "uk/uk_UA/ukrainian_tts/medium/uk_UA-ukrainian_tts-medium", false)
  );

  private static Map.Entry<String, PiperVoice> voice(String lang, String label, String localVoiceId, String remoteBase, boolean standardPackage) {
    return Map.entry(lang, new PiperVoice(lang, label, localVoiceId, HF + remoteBase + ".onnx", HF + remoteBase + ".onnx.json", standardPackage));
  }

  public TtsService(
    @Value("${app.tts.enabled:true}") boolean enabled,
    @Value("${app.tts.browser-fallback:true}") boolean browserFallback,
    @Value("${app.tts.piper.auto-download:true}") boolean autoDownload,
    @Value("${app.tts.piper.executable:./tts/piper/piper.exe}") String piperExecutable,
    @Value("${app.tts.piper.voices-dir:./tts/piper/voices}") String voicesDir,
    @Value("${app.tts.fallback.de:de-DE,de,en-US,en}") String de,
    @Value("${app.tts.fallback.en:en-US,en-GB,en,de-DE,de}") String en,
    @Value("${app.tts.fallback.fr:fr-FR,fr,en-US,en,de-DE,de}") String fr,
    @Value("${app.tts.fallback.uk:uk-UA,uk,ru-RU,ru,en-US,en,de-DE,de}") String uk,
    @Value("${app.tts.fallback.it:it-IT,it,en-US,en,de-DE,de}") String it,
    @Value("${app.tts.fallback.sv:sv-SE,sv,en-US,en-GB,en,de-DE,de}") String sv,
    @Value("${app.tts.fallback.tr:tr-TR,tr,en-US,en-GB,en,de-DE,de}") String tr,
    @Value("${app.tts.fallback.ru:ru-RU,ru,en-US,en-GB,en,de-DE,de}") String ru,
    @Value("${app.tts.fallback.es:es-ES,es,en-US,en-GB,en,de-DE,de}") String es,
    @Value("${app.tts.fallback.pt:pt-PT,pt-BR,pt,en-US,en-GB,en,de-DE,de}") String pt,
    @Value("${app.tts.fallback.nl:nl-NL,nl,en-US,en-GB,en,de-DE,de}") String nl,
    @Value("${app.tts.fallback.pl:pl-PL,pl,en-US,en-GB,en,de-DE,de}") String pl,
    @Value("${app.tts.fallback.cs:cs-CZ,cs,en-US,en-GB,en,de-DE,de}") String cs
  ) {
    this.enabled = enabled;
    this.browserFallback = browserFallback;
    this.autoDownload = autoDownload;
    this.piperExecutable = piperExecutable;
    this.voicesDir = voicesDir;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
    this.fallback = Map.ofEntries(
      Map.entry("de", split(de)), Map.entry("en", split(en)), Map.entry("fr", split(fr)),
      Map.entry("uk", split(uk)), Map.entry("it", split(it)), Map.entry("sv", split(sv)),
      Map.entry("tr", split(tr)), Map.entry("ru", split(ru)), Map.entry("es", split(es)),
      Map.entry("pt", split(pt)), Map.entry("nl", split(nl)), Map.entry("pl", split(pl)), Map.entry("cs", split(cs))
    );
  }

  public TtsStatus status() {
    return new TtsStatus(
      enabled,
      "piper",
      "piper",
      piperAvailable(),
      piperAvailabilityMessage(),
      piperDiagnostics(),
      piperExecutable,
      voicesDir,
      voiceIdMap(),
      languageLabelMap(),
      installedLanguages(),
      supportedLanguages(),
      standardLanguages(),
      new TreeSet<>(downloadsInProgress).stream().toList(),
      new TreeMap<>(downloadErrors),
      autoDownload,
      browserFallback,
      fallback
    );
  }

  public ResponseEntity<byte[]> audio(TtsRequest request) {
    if (!enabled) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new byte[0]);
    String text = request.text() == null ? "" : request.text();
    if (text.isBlank()) return ResponseEntity.badRequest().body(new byte[0]);

    String lang = normalizeLanguage(request.language());
    PiperVoice voice = PIPER_VOICES.get(lang);
    if (voice == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .header("X-GAM-TTS-Error", "unsupported-language")
        .body(new byte[0]);
    }
    if (!piperAvailable()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("X-GAM-TTS-Error", "piper-executable-unavailable")
        .body(new byte[0]);
    }
    if (!voiceAvailable(voice)) {
      if (autoDownload) startVoiceDownload(lang);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("X-GAM-TTS-Error", autoDownload ? "piper-voice-download-started" : "piper-voice-missing")
        .header("X-GAM-TTS-Language", lang)
        .body(new byte[0]);
    }

    Path output = null;
    try {
      Path model = resolveVoiceFile(voice, ".onnx");
      Path exe = resolvePiperExecutable().orElse(Path.of(piperExecutable).normalize());
      output = Files.createTempFile("gam-piper-", ".wav");
      ProcessBuilder pb = new ProcessBuilder(
        exe.toString(),
        "--model", model.toString(),
        "--output_file", output.toString()
      );
      pb.redirectErrorStream(true);
      Process process = pb.start();
      try (OutputStream stdin = process.getOutputStream()) {
        stdin.write(text.getBytes(StandardCharsets.UTF_8));
      }
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .header("X-GAM-TTS-Error", "piper-exit-" + exitCode)
          .body(new byte[0]);
      }
      byte[] wav = Files.readAllBytes(output);
      return ResponseEntity.ok()
        .contentType(MediaType.valueOf("audio/wav"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=gam-tts.wav")
        .body(wav);
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("X-GAM-TTS-Error", "piper-failed")
        .body(new byte[0]);
    } finally {
      if (output != null) {
        try { Files.deleteIfExists(output); } catch (Exception ignored) {}
      }
    }
  }

  public Map<String, Object> installLanguage(String language) {
    String lang = normalizeLanguage(language);
    PiperVoice voice = PIPER_VOICES.get(lang);
    if (voice == null) return Map.of("language", lang, "supported", false, "installed", false, "downloading", false);
    if (voiceAvailable(voice)) return Map.of("language", lang, "supported", true, "installed", true, "downloading", false);
    startVoiceDownload(lang);
    return Map.of("language", lang, "supported", true, "installed", false, "downloading", downloadsInProgress.contains(lang));
  }

  private void startVoiceDownload(String lang) {
    PiperVoice voice = PIPER_VOICES.get(lang);
    if (voice == null || voiceAvailable(voice) || !downloadsInProgress.add(lang)) return;
    downloadErrors.remove(lang);
    Thread worker = new Thread(() -> {
      try {
        Path dir = resolveVoicesDirForWrite();
        Files.createDirectories(dir);
        downloadFile(voice.modelUrl(), dir.resolve(voice.voiceId() + ".onnx"));
        downloadFile(voice.configUrl(), dir.resolve(voice.voiceId() + ".onnx.json"));
      } catch (Exception ex) {
        downloadErrors.put(lang, ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()));
      } finally {
        downloadsInProgress.remove(lang);
      }
    }, "gam-piper-download-" + lang);
    worker.setDaemon(true);
    worker.start();
  }

  private void downloadFile(String url, Path target) throws Exception {
    Path temp = target.resolveSibling(target.getFileName().toString() + ".download");
    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10)).GET().build();
    HttpResponse<Path> res = httpClient.send(req, HttpResponse.BodyHandlers.ofFile(temp));
    if (res.statusCode() < 200 || res.statusCode() >= 300) {
      Files.deleteIfExists(temp);
      throw new IllegalStateException("HTTP " + res.statusCode() + " for " + url);
    }
    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private boolean piperAvailable() {
    return resolvePiperExecutable().isPresent();
  }

  private String piperAvailabilityMessage() {
    Optional<Path> resolved = resolvePiperExecutable();
    if (resolved.isEmpty()) {
      return "piper.exe nicht gefunden. Geprüfte Pfade siehe piperDiagnostics.checkedExecutablePaths.";
    }
    Path exe = resolved.get();
    Path dir = exe.getParent();
    if (dir == null) dir = Path.of(".");
    boolean hasOnnxRuntime = Files.isRegularFile(dir.resolve("onnxruntime.dll"));
    boolean hasEspeakData = Files.isDirectory(dir.resolve("espeak-ng-data"));
    boolean hasAnyDll = false;
    try (var files = Files.list(dir)) {
      hasAnyDll = files.anyMatch(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dll"));
    } catch (Exception ignored) {}
    if (!hasOnnxRuntime && !hasEspeakData && !hasAnyDll) {
      return "piper.exe gefunden: " + exe + ". Hinweis: keine DLL/espeak-ng-data im selben Ordner erkannt; falls Audio fehlschlägt, INSTALL_PIPER_TTS.bat erneut ausführen.";
    }
    return "Piper Engine gefunden: " + exe;
  }

  private Map<String, Object> piperDiagnostics() {
    Optional<Path> resolved = resolvePiperExecutable();
    Path exe = resolved.orElse(Path.of(piperExecutable).normalize());
    Path dir = exe.getParent();
    if (dir == null) dir = Path.of(".");
    Map<String, Object> result = new TreeMap<>();
    result.put("workingDirectory", Path.of("").toAbsolutePath().normalize().toString());
    result.put("configuredPiperExecutable", piperExecutable);
    result.put("resolvedPiperExecutable", resolved.map(Path::toString).orElse(null));
    result.put("piperExecutableExists", resolved.isPresent());
    result.put("checkedExecutablePaths", executableCandidates().stream().map(Path::toString).toList());
    result.put("piperDirectory", dir.toString());
    result.put("onnxruntimeDllExists", Files.isRegularFile(dir.resolve("onnxruntime.dll")));
    result.put("espeakNgDataExists", Files.isDirectory(dir.resolve("espeak-ng-data")));
    try (var files = Files.list(dir)) {
      result.put("piperDirectoryEntries", files
        .map(f -> f.getFileName().toString())
        .sorted()
        .limit(120)
        .toList());
    } catch (Exception ex) {
      result.put("piperDirectoryListError", ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()));
    }
    result.put("configuredVoicesDir", voicesDir);
    result.put("resolvedVoicesDir", resolveVoicesDirForRead().map(Path::toString).orElse(null));
    result.put("checkedVoiceDirs", voiceDirCandidates().stream().map(Path::toString).toList());
    return result;
  }

  private boolean voiceAvailable(PiperVoice voice) {
    return resolveVoiceFile(voice, ".onnx") != null && resolveVoiceFile(voice, ".onnx.json") != null;
  }

  private List<String> installedLanguages() {
    return PIPER_VOICES.values().stream()
      .filter(this::voiceAvailable)
      .map(PiperVoice::language)
      .sorted()
      .toList();
  }

  private List<String> supportedLanguages() {
    return PIPER_VOICES.keySet().stream().sorted().toList();
  }

  private List<String> standardLanguages() {
    return PIPER_VOICES.values().stream().filter(PiperVoice::standardPackage).map(PiperVoice::language).sorted().toList();
  }

  private Map<String, String> voiceIdMap() {
    Map<String, String> result = new TreeMap<>();
    PIPER_VOICES.forEach((lang, voice) -> result.put(lang, voice.voiceId()));
    return result;
  }

  private Map<String, String> languageLabelMap() {
    Map<String, String> result = new TreeMap<>();
    PIPER_VOICES.forEach((lang, voice) -> result.put(lang, voice.label()));
    return result;
  }

  private Optional<Path> resolvePiperExecutable() {
    for (Path candidate : executableCandidates()) {
      if (Files.isRegularFile(candidate)) return Optional.of(candidate);
    }
    return Optional.empty();
  }

  private List<Path> executableCandidates() {
    LinkedHashSet<Path> paths = new LinkedHashSet<>();
    addCandidate(paths, Path.of(piperExecutable));
    addCandidate(paths, Path.of("tts", "piper", "piper.exe"));
    addCandidate(paths, Path.of("..", "tts", "piper", "piper.exe"));
    addCandidate(paths, Path.of(".", "tts", "piper", "piper.exe"));
    addCandidate(paths, Path.of("..", "..", "tts", "piper", "piper.exe"));
    try {
      Path cwd = Path.of("").toAbsolutePath().normalize();
      addCandidate(paths, cwd.resolve("tts").resolve("piper").resolve("piper.exe"));
      addCandidate(paths, cwd.getParent() == null ? null : cwd.getParent().resolve("tts").resolve("piper").resolve("piper.exe"));
    } catch (Exception ignored) {}
    return paths.stream().toList();
  }

  private Optional<Path> resolveVoicesDirForRead() {
    for (Path candidate : voiceDirCandidates()) {
      if (Files.isDirectory(candidate)) return Optional.of(candidate);
    }
    return Optional.empty();
  }

  private Path resolveVoicesDirForWrite() {
    return resolveVoicesDirForRead().orElse(Path.of(voicesDir).normalize());
  }

  private List<Path> voiceDirCandidates() {
    LinkedHashSet<Path> paths = new LinkedHashSet<>();
    addCandidate(paths, Path.of(voicesDir));
    addCandidate(paths, Path.of("tts", "piper", "voices"));
    addCandidate(paths, Path.of("..", "tts", "piper", "voices"));
    addCandidate(paths, Path.of(".", "tts", "piper", "voices"));
    addCandidate(paths, Path.of("..", "..", "tts", "piper", "voices"));
    resolvePiperExecutable().ifPresent(exe -> {
      Path dir = exe.getParent();
      if (dir != null) addCandidate(paths, dir.resolve("voices"));
    });
    try {
      Path cwd = Path.of("").toAbsolutePath().normalize();
      addCandidate(paths, cwd.resolve("tts").resolve("piper").resolve("voices"));
      addCandidate(paths, cwd.getParent() == null ? null : cwd.getParent().resolve("tts").resolve("piper").resolve("voices"));
    } catch (Exception ignored) {}
    return paths.stream().toList();
  }

  private Path resolveVoiceFile(PiperVoice voice, String suffix) {
    String fileName = voice.voiceId() + suffix;
    for (Path dir : voiceDirCandidates()) {
      Path candidate = dir.resolve(fileName).normalize();
      if (Files.isRegularFile(candidate)) return candidate;
    }
    return null;
  }

  private static void addCandidate(LinkedHashSet<Path> paths, Path path) {
    if (path == null) return;
    paths.add(path.toAbsolutePath().normalize());
  }

  private static String normalizeLanguage(String value) {
    String l = value == null ? "de" : value.toLowerCase(Locale.ROOT).replace('-', '_');
    if (l.startsWith("en")) return "en";
    if (l.startsWith("fr")) return "fr";
    if (l.startsWith("uk") || l.startsWith("ua")) return "uk";
    if (l.startsWith("it")) return "it";
    if (l.startsWith("sv") || l.startsWith("se")) return "sv";
    if (l.startsWith("tr")) return "tr";
    if (l.startsWith("ru")) return "ru";
    if (l.startsWith("es")) return "es";
    if (l.startsWith("pt")) return "pt";
    if (l.startsWith("nl")) return "nl";
    if (l.startsWith("pl")) return "pl";
    if (l.startsWith("cs") || l.startsWith("cz")) return "cs";
    return "de";
  }

  private static List<String> split(String csv) {
    if (csv == null || csv.isBlank()) return List.of();
    return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
  }
}
