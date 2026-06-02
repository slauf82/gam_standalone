package de.kopfzentrum.gam.security;

public class LoginRateLimitException extends RuntimeException {
  private final long retryAfterSeconds;

  public LoginRateLimitException(String message, long retryAfterSeconds) {
    super(message);
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
