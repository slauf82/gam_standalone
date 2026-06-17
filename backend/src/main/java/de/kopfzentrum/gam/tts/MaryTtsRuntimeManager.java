package de.kopfzentrum.gam.tts;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class MaryTtsRuntimeManager implements DisposableBean {
  private final boolean autostart;
  private final Path configuredHome;
  private final int port;
  private final boolean diagnostics;
  private Process process;
  private volatile boolean bundledStarted = false;
  private volatile String bundledError = null;
  private volatile Path activeHome;

  public MaryTtsRuntimeManager(
    @Value("${app.tts.marytts.autostart:true}") boolean autostart,
    @Value("${app.tts.marytts.home:./tts/marytts}") String home,
    @Value("${app.tts.marytts.port:59125}") int port
  ) {
    this.autostart = autostart;
    this.configuredHome = Paths.get(home).toAbsolutePath().normalize();
    this.activeHome = this.configuredHome;
    this.port = port;
    this.diagnostics = Boolean.parseBoolean(System.getenv().getOrDefault("GAM_MARYTTS_DIAGNOSTICS", "false"))
      || Boolean.getBoolean("gam.marytts.diagnostics");
  }

  @EventListener(ApplicationReadyEvent.class)
  public void startIfBundled() {
    if (!autostart) {
      bundledStarted = false;
      bundledError = null;
      logDiag("[GAM TTS] Bundled MaryTTS autostart disabled; browser TTS fallback remains available.");
      return;
    }
    if (isReachable()) {
      bundledStarted = false;
      bundledError = null;
      logDiag("[GAM TTS] MaryTTS already reachable on port " + port + ".");
      return;
    }

    Path launcher = findLauncher();
    if (launcher == null) {
      bundledStarted = false;
      bundledError = null;
      logDiag("[GAM TTS] Bundled MaryTTS runtime not present; browser TTS fallback remains available.");
      return;
    }

    try {
      activeHome = maryHomeForLauncher(launcher);
      ProcessBuilder pb = new ProcessBuilder(commandFor(launcher));
      pb.directory(activeHome.toFile());
      pb.redirectErrorStream(true);
      pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile().toFile()));
      process = pb.start();

      if (waitUntilReachable(20_000)) {
        bundledStarted = true;
        bundledError = null;
        logDiag("[GAM TTS] Bundled MaryTTS started from " + launcher + ".");
        return;
      }

      boolean exited = !process.isAlive();
      bundledStarted = false;
      bundledError = exited
        ? "MaryTTS launcher exited before the server became reachable"
        : "MaryTTS server did not become reachable on port " + port;
      logDiag("[GAM TTS] Bundled MaryTTS not reachable; browser TTS fallback remains available.");
    } catch (Exception ex) {
      bundledStarted = false;
      bundledError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
      logDiag("[GAM TTS] Bundled MaryTTS autostart failed; browser TTS fallback remains available.");
    }
  }

  public boolean bundled() {
    return findLauncher() != null;
  }

  public boolean bundledStarted() {
    return bundledStarted;
  }

  public boolean bundledAvailable() {
    return bundled() && (isReachable() || bundledStarted);
  }

  public String bundledError() {
    return bundledError;
  }

  public boolean isReachable() {
    try {
      var url = URI.create("http://localhost:" + port + "/version").toURL();
      var con = (java.net.HttpURLConnection) url.openConnection();
      con.setConnectTimeout(700);
      con.setReadTimeout(700);
      return con.getResponseCode() >= 200 && con.getResponseCode() < 300;
    } catch (Exception ex) {
      return false;
    }
  }

  public String home() {
    return activeHome == null ? configuredHome.toString() : activeHome.toString();
  }

  private boolean waitUntilReachable(long timeoutMs) {
    long end = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < end) {
      if (isReachable()) return true;
      if (process != null && !process.isAlive()) return false;
      try {
        Thread.sleep(500);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  private Path findLauncher() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String[] candidates = os.contains("win")
      ? new String[] {"bin/marytts-server.bat", "bin/marytts.bat", "marytts-server.bat", "start-marytts.bat"}
      : new String[] {"bin/marytts-server", "bin/marytts", "marytts-server", "start-marytts.sh"};

    for (Path h : possibleHomes()) {
      for (String c : candidates) {
        Path p = h.resolve(c).toAbsolutePath().normalize();
        if (Files.isRegularFile(p) && isRealLauncher(p)) {
          activeHome = h;
          return p;
        }
      }
    }
    return null;
  }

  private List<Path> possibleHomes() {
    Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Set<Path> homes = new LinkedHashSet<>();
    homes.add(configuredHome);
    homes.add(userDir.resolve("tts/marytts").normalize());
    homes.add(userDir.resolve("backend/tts/marytts").normalize());
    Path parent = userDir.getParent();
    if (parent != null) {
      homes.add(parent.resolve("tts/marytts").normalize());
      homes.add(parent.resolve("backend/tts/marytts").normalize());
    }
    return new ArrayList<>(homes);
  }

  private boolean isRealLauncher(Path launcher) {
    String normalized = launcher.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
    if (normalized.contains("/bin/")) return true;

    // Root start-marytts.* is allowed only if it is not the bundled placeholder.
    try {
      String text = Files.readString(launcher, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
      if (text.contains("vollstaendiges marytts-paket") || text.contains("vollständiges marytts-paket")) return false;
      if (text.contains("placeholder") || text.contains("exit /b 1")) return false;
    } catch (Exception ignored) {
      // Binary or non-UTF launcher: accept it.
    }
    return true;
  }

  private Path maryHomeForLauncher(Path launcher) {
    Path p = launcher.toAbsolutePath().normalize();
    Path parent = p.getParent();
    if (parent != null && parent.getFileName() != null && "bin".equalsIgnoreCase(parent.getFileName().toString())) {
      Path grandParent = parent.getParent();
      if (grandParent != null) return grandParent;
    }
    return parent == null ? configuredHome : parent;
  }

  private List<String> commandFor(Path launcher) {
    String name = launcher.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".bat") || name.endsWith(".cmd")) {
      return List.of("cmd.exe", "/c", "call", launcher.toString());
    }
    return List.of(launcher.toString());
  }

  private Path logFile() throws Exception {
    Path logs = Paths.get("logs").toAbsolutePath().normalize();
    Files.createDirectories(logs);
    return logs.resolve("marytts.log");
  }

  private void logDiag(String message) {
    if (diagnostics) {
      System.out.println(message);
    }
  }

  public boolean diagnosticsEnabled() {
    return diagnostics;
  }

  @Override
  public void destroy() {
    if (process != null && process.isAlive()) {
      process.destroy();
    }
  }
}
