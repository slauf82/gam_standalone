package de.kopfzentrum.gam.security;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message, String path, Long retryAfterSeconds) {
  public static ApiError of(int status, String error, String message, String path) {
    return new ApiError(Instant.now(), status, error, message, path, null);
  }

  public static ApiError rateLimited(String message, String path, long retryAfterSeconds) {
    return new ApiError(Instant.now(), 429, "TOO_MANY_REQUESTS", message, path, retryAfterSeconds);
  }
}
