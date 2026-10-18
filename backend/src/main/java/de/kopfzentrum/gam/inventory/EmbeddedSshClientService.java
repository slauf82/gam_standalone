package de.kopfzentrum.gam.inventory;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;

/**
 * 40k36q: Plattformunabhängiger SSH-Client für Linux/macOS.
 *
 * Vorher wurde das externe Programm ssh (und bei Passwortauthentifizierung zusätzlich
 * sshpass) vorausgesetzt. Das ist besonders auf einem Windows-GAM-Server unzuverlässig.
 * Apache MINA SSHD stellt die Verbindung vollständig innerhalb der JVM her.
 */
@Service
public class EmbeddedSshClientService {
  private static final Logger log = LoggerFactory.getLogger(EmbeddedSshClientService.class);

  public record Result(boolean success, String stdout, String stderr, String message, long durationMs) {}
  public record TestResult(boolean handshakeOk, boolean authOk, boolean commandOk, String message, long durationMs) {}

  /** 40k36q1: gestufter SSH-Test mit getrennten Zeitfenstern für Handshake und Authentifizierung. */
  public TestResult testDetailed(String host, int port, String username, String authMode, String password, String keyPath) {
    long started = System.currentTimeMillis();
    if (host == null || host.isBlank()) return new TestResult(false, false, false, "SSH-Ziel fehlt.", 0);
    if (username == null || username.isBlank()) return new TestResult(false, false, false, "SSH-Benutzername fehlt.", 0);
    int effectivePort = port > 0 && port <= 65535 ? port : 22;
    Duration handshakeTimeout = Duration.ofSeconds(10);
    Duration authTimeout = Duration.ofSeconds(15);
    Duration commandTimeout = Duration.ofSeconds(10);

    try (SshClient client = SshClient.setUpDefaultClient()) {
      client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
      client.start();
      ClientSession session;
      try {
        session = client.connect(username.trim(), host.trim(), effectivePort).verify(handshakeTimeout).getSession();
      } catch (Exception e) {
        String msg = cleanMessage(e);
        log.debug("SSH-Handshake zu {}:{} fehlgeschlagen: {}", host, effectivePort, e.toString());
        return new TestResult(false, false, false, "TCP-Port erreichbar, aber SSH-Handshake fehlgeschlagen oder hat länger als 10 Sekunden gedauert: " + msg, System.currentTimeMillis() - started);
      }
      try (session) {
        if ("PASSWORD".equalsIgnoreCase(authMode)) {
          if (password == null || password.isBlank()) return new TestResult(true, false, false, "SSH-Handshake erfolgreich, aber kein Passwort ist hinterlegt.", System.currentTimeMillis() - started);
          session.addPasswordIdentity(password);
        } else if (keyPath != null && !keyPath.isBlank()) {
          Path key = Path.of(keyPath.trim()).toAbsolutePath().normalize();
          if (!Files.isRegularFile(key)) return new TestResult(true, false, false, "SSH-Handshake erfolgreich, aber die SSH-Schlüsseldatei wurde nicht gefunden: " + key, System.currentTimeMillis() - started);
          FileKeyPairProvider provider = new FileKeyPairProvider(key);
          for (var pair : provider.loadKeys(session)) session.addPublicKeyIdentity(pair);
        }

        try {
          session.auth().verify(authTimeout);
        } catch (Exception e) {
          String msg = cleanMessage(e);
          log.debug("SSH-Authentifizierung zu {}:{} fehlgeschlagen: {}", host, effectivePort, e.toString());
          return new TestResult(true, false, false, "SSH-Handshake erfolgreich, aber Authentifizierung fehlgeschlagen oder hat länger als 15 Sekunden gedauert: " + msg, System.currentTimeMillis() - started);
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try (ClientChannel channel = session.createExecChannel("printf GAM_SSH_OK")) {
          channel.setOut(stdout);
          channel.setErr(stderr);
          channel.open().verify(commandTimeout);
          channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), commandTimeout.toMillis());
        } catch (Exception e) {
          String msg = cleanMessage(e);
          return new TestResult(true, true, false, "SSH-Authentifizierung erfolgreich, aber der Testbefehl konnte nicht ausgeführt werden: " + msg, System.currentTimeMillis() - started);
        }
        boolean ok = stdout.toString(StandardCharsets.UTF_8).contains("GAM_SSH_OK");
        return new TestResult(true, true, ok, ok ? "SSH-Handshake, Authentifizierung und Testbefehl erfolgreich." : "SSH-Authentifizierung erfolgreich, aber der Testbefehl lieferte keine Bestätigung.", System.currentTimeMillis() - started);
      } finally {
        client.stop();
      }
    } catch (Exception e) {
      String msg = cleanMessage(e);
      log.debug("Eingebetteter SSH-Test zu {}:{} fehlgeschlagen: {}", host, effectivePort, e.toString());
      return new TestResult(false, false, false, "SSH-Test fehlgeschlagen: " + msg, System.currentTimeMillis() - started);
    }
  }

  public Result test(String host, int port, String username, String authMode, String password, String keyPath) {
    TestResult r = testDetailed(host, port, username, authMode, password, keyPath);
    return new Result(r.commandOk(), r.commandOk() ? "GAM_SSH_OK" : "", "", r.message(), r.durationMs());
  }

  public Result execute(String host, int port, String username, String authMode, String password,
                        String keyPath, String command, String stdin, int timeoutSeconds) {
    long started = System.currentTimeMillis();
    if (host == null || host.isBlank()) return fail("SSH-Ziel fehlt.", started);
    if (username == null || username.isBlank()) return fail("SSH-Benutzername fehlt.", started);
    int effectivePort = port > 0 && port <= 65535 ? port : 22;
    Duration timeout = Duration.ofSeconds(Math.max(3, timeoutSeconds));

    try (SshClient client = SshClient.setUpDefaultClient()) {
      // Verhalten entspricht dem bisherigen OpenSSH-Aufruf mit StrictHostKeyChecking=accept-new.
      // Die spätere Known-Hosts-Verwaltung kann darauf aufsetzen; für 40k36q bleibt der
      // bestehende vertrauensbasierte Discovery-Workflow erhalten.
      client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
      client.start();
      try (ClientSession session = client.connect(username.trim(), host.trim(), effectivePort)
          .verify(timeout).getSession()) {
        if ("PASSWORD".equalsIgnoreCase(authMode)) {
          if (password == null || password.isBlank()) return fail("SSH-Passwort ist nicht hinterlegt.", started);
          session.addPasswordIdentity(password);
        } else if (keyPath != null && !keyPath.isBlank()) {
          Path key = Path.of(keyPath.trim()).toAbsolutePath().normalize();
          if (!Files.isRegularFile(key)) return fail("SSH-Schlüsseldatei wurde nicht gefunden: " + key, started);
          FileKeyPairProvider provider = new FileKeyPairProvider(key);
          for (var pair : provider.loadKeys(session)) session.addPublicKeyIdentity(pair);
        }

        session.auth().verify(timeout);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try (ClientChannel channel = session.createExecChannel(command)) {
          channel.setOut(stdout);
          channel.setErr(stderr);
          if (stdin != null) channel.setIn(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));
          channel.open().verify(timeout);
          channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), timeout.toMillis());
        }
        String out = stdout.toString(StandardCharsets.UTF_8);
        String err = stderr.toString(StandardCharsets.UTF_8);
        return new Result(true, out, err, "SSH-Verbindung und Authentifizierung erfolgreich.", System.currentTimeMillis() - started);
      } finally {
        client.stop();
      }
    } catch (Exception e) {
      log.debug("Eingebettete SSH-Verbindung zu {}:{} fehlgeschlagen: {}", host, effectivePort, e.toString());
      String msg = e.getMessage();
      if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
      return new Result(false, "", "", "SSH-Verbindung fehlgeschlagen: " + msg, System.currentTimeMillis() - started);
    }
  }

  private static Result fail(String message, long started) {
    return new Result(false, "", "", message, System.currentTimeMillis() - started);
  }

  private static String cleanMessage(Exception e) {
    String msg = e.getMessage();
    if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
    return msg.replaceAll("[\r\n]+", " ").trim();
  }
}
