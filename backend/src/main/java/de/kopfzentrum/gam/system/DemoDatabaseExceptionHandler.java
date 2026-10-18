package de.kopfzentrum.gam.system;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 40k36q28: Aussagekraeftige Fehlerantworten fuer den Demo-Datenbankimport. */
@RestControllerAdvice(assignableTypes = DemoDatabaseController.class)
public class DemoDatabaseExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String,Object>> handle(Exception ex) {
    Map<String,Object> body=new LinkedHashMap<>();
    body.put("error","DEMO_DATABASE_IMPORT_FAILED");
    body.put("message", ex.getMessage()==null ? "Beispieldatenimport fehlgeschlagen." : ex.getMessage());
    body.put("exception", ex.getClass().getSimpleName());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
