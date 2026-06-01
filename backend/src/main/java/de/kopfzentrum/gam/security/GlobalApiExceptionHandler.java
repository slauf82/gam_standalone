package de.kopfzentrum.gam.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> accessDenied(AccessDeniedException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
      .body(ApiError.of(403, "FORBIDDEN", "Keine Berechtigung fuer dieses GAM-Modul.", request.getRequestURI()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiError> responseStatus(ResponseStatusException ex, HttpServletRequest request) {
    int code = ex.getStatusCode().value();
    String message = ex.getReason() == null ? ex.getMessage() : ex.getReason();
    return ResponseEntity.status(ex.getStatusCode())
      .body(ApiError.of(code, ex.getStatusCode().toString(), message, request.getRequestURI()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiError> badRequest(IllegalArgumentException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(ApiError.of(400, "BAD_REQUEST", ex.getMessage(), request.getRequestURI()));
  }
}
