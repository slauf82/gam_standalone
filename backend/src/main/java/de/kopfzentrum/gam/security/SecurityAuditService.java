package de.kopfzentrum.gam.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditService {
  private static final Logger log = LoggerFactory.getLogger("GAM_SECURITY_AUDIT");

  public void loginSuccess(String username, String mode) {
    log.info("LOGIN_SUCCESS user={} mode={}", safe(username), safe(mode));
  }

  public void loginFailure(String username) {
    log.warn("LOGIN_FAILURE user={}", safe(username));
  }

  public void tokenRejected(String reason) {
    log.warn("TOKEN_REJECTED reason={}", safe(reason));
  }

  private String safe(String value) {
    if (value == null) return "";
    return value.replaceAll("[\r\n\t]", "_");
  }
}
