package de.kopfzentrum.gam.tts;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.*;
import java.util.Locale;

@Component
public class MaryTtsRuntimeManager implements DisposableBean {
  private final boolean embedded;
  private final boolean autostart;
  private final Path home;
  private final int port;
  private Process process;

  public MaryTtsRuntimeManager(
    @Value("${app.tts.marytts.embedded:true}") boolean embedded,
    @Value("${app.tts.marytts.autostart:true}") boolean autostart,
    @Value("${app.tts.marytts.home:./tts/marytts}") String home,
    @Value("${app.tts.marytts.port:59125}") int port
  ) {
    this.embedded = embedded;
    this.autostart = autostart;
    this.home = Paths.get(home).toAbsolutePath().normalize();
    this.port = port;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void startIfBundled() {
    if (!embedded || !autostart) return;
    if (isReachable()) return;

    Path launcher = findLauncher();
    if (launcher == null) {
      System.out.println("[GAM TTS] MaryTTS bundle not found at " + home + " - browser TTS fallback remains active.");
      return;
    }

    try {
      ProcessBuilder pb = new ProcessBuilder(commandFor(launcher));
      pb.directory(home.toFile());
      pb.redirectErrorStream(true);
      pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile().toFile()));
      process = pb.start();
      System.out.println("[GAM TTS] MaryTTS autostart requested from " + launcher);
    } catch (Exception ex) {
      System.out.println("[GAM TTS] MaryTTS autostart failed: " + ex.getMessage());
    }
  }

  public boolean bundled() {
    return Files.isDirectory(home) && findLauncher() != null;
  }

  public boolean isReachable() {
    try {
      var url = new java.net.URL("http://localhost:" + port + "/version");
      var con = (java.net.HttpURLConnection) url.openConnection();
      con.setConnectTimeout(700);
      con.setReadTimeout(700);
      return con.getResponseCode() >= 200 && con.getResponseCode() < 300;
    } catch (Exception ex) {
      return false;
    }
  }

  public String home() {
    return home.toString();
  }

  private Path findLauncher() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String[] candidates = os.contains("win")
      ? new String[] {"bin/marytts-server.bat", "bin/marytts.bat", "marytts-server.bat", "start-marytts.bat"}
      : new String[] {"bin/marytts-server", "bin/marytts", "marytts-server", "start-marytts.sh"};

    for (String c : candidates) {
      Path p = home.resolve(c);
      if (Files.exists(p)) return p;
    }
    return null;
  }

  private java.util.List<String> commandFor(Path launcher) {
    String name = launcher.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".bat") || name.endsWith(".cmd")) {
      return java.util.List.of("cmd.exe", "/c", launcher.toString());
    }
    return java.util.List.of(launcher.toString());
  }

  private Path logFile() throws Exception {
    Path logs = Paths.get("logs").toAbsolutePath().normalize();
    Files.createDirectories(logs);
    return logs.resolve("marytts.log");
  }

  @Override
  public void destroy() {
    if (process != null && process.isAlive()) {
      process.destroy();
    }
  }
}
