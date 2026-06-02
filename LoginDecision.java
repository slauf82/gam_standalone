package de.kopfzentrum.gam.auth;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class LegacyPasswordVerifier {
  public boolean matchesSha256(String rawPassword, String storedHash) {
    if (rawPassword == null || storedHash == null || storedHash.isBlank()) return false;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      String hash = HexFormat.of().formatHex(md.digest(rawPassword.getBytes(StandardCharsets.UTF_8)));
      return MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8), storedHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) { return false; }
  }
}
