package de.kopfzentrum.gam.auth;

import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;

@Component
public class TotpVerifier {
  public boolean verify(String base32Secret, String code) {
    if (base32Secret == null || base32Secret.isBlank() || code == null || !code.matches("\\d{6}")) return false;
    long timeStep = Instant.now().getEpochSecond() / 30L;
    for (long drift = -1; drift <= 1; drift++) {
      if (constantEquals(generate(base32Secret, timeStep + drift), code)) return true;
    }
    return false;
  }

  private String generate(String secret, long counter) {
    try {
      byte[] key = base32Decode(secret);
      byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] h = mac.doFinal(msg);
      int offset = h[h.length - 1] & 0x0f;
      int binary = ((h[offset] & 0x7f) << 24) | ((h[offset + 1] & 0xff) << 16) | ((h[offset + 2] & 0xff) << 8) | (h[offset + 3] & 0xff);
      return String.format("%06d", binary % 1_000_000);
    } catch (Exception e) { return "------"; }
  }

  private boolean constantEquals(String a, String b) { return MessageDigest.isEqual(a.getBytes(), b.getBytes()); }

  private byte[] base32Decode(String s) {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    String clean = s.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
    int buffer = 0, bitsLeft = 0, count = 0;
    byte[] out = new byte[clean.length() * 5 / 8 + 8];
    for (char c : clean.toCharArray()) {
      int val = alphabet.indexOf(c);
      if (val < 0) continue;
      buffer = (buffer << 5) | val;
      bitsLeft += 5;
      if (bitsLeft >= 8) out[count++] = (byte) ((buffer >> (bitsLeft -= 8)) & 0xff);
    }
    return java.util.Arrays.copyOf(out, count);
  }
}
