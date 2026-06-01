package de.kopfzentrum.gam.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Leichte In-Memory-Bremse fuer Loginversuche.
 * Ziel: alte DB-Loginlogik nicht veraendern, aber Brute-Force erschweren.
 * Fuer Mehrinstanzbetrieb spaeter durch DB/Redis ersetzen.
 */
@Component
public class LoginRateLimiter {
  private final int maxFailures;
  private final Duration lockDuration;
  private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

  public LoginRateLimiter(
      @Value("${app.security.login.max-failures:8}") int maxFailures,
      @Value("${app.security.login.lock-minutes:10}") long lockMinutes
  ) {
    this.maxFailures = maxFailures;
    this.lockDuration = Duration.ofMinutes(lockMinutes);
  }

  public void assertAllowed(String username) {
    String key = key(username);
    Attempt a = attempts.get(key);
    if (a == null || a.lockedUntil == null) return;
    if (Instant.now().isAfter(a.lockedUntil)) {
      attempts.remove(key);
      return;
    }
    throw new IllegalArgumentException("Zu viele fehlgeschlagene Loginversuche. Bitte spaeter erneut versuchen.");
  }

  public void success(String username) { attempts.remove(key(username)); }

  public void failure(String username) {
    String key = key(username);
    attempts.compute(key, (k, old) -> {
      int failures = old == null ? 1 : old.failures + 1;
      Instant locked = failures >= maxFailures ? Instant.now().plus(lockDuration) : null;
      return new Attempt(failures, locked);
    });
  }

  private String key(String username) { return username == null ? "" : username.trim().toLowerCase(); }
  private record Attempt(int failures, Instant lockedUntil) {}
}
