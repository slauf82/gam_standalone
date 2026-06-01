package de.kopfzentrum.gam.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
  private final SecretKey key;
  private final long jwtMinutes;

  public JwtService(
      @Value("${app.security.jwt-secret}") String secret,
      @Value("${app.security.jwt-minutes}") long jwtMinutes
  ) {
    String material = secret == null ? "" : secret;
    String padded = (material + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx").substring(0, 64);
    this.key = Keys.hmacShaKeyFor(padded.getBytes(StandardCharsets.UTF_8));
    this.jwtMinutes = jwtMinutes;
  }

  public String create(Account account, LoginMode mode) {
    Instant now = Instant.now();
    return Jwts.builder()
      .subject(account.username())
      .claims(Map.of(
        "accountId", account.id(),
        "role", account.normalizedRole(),
        "name", account.fullname() == null ? account.username() : account.fullname(),
        "mode", mode.name()
      ))
      .issuedAt(Date.from(now))
      .expiration(Date.from(now.plusSeconds(jwtMinutes * 60)))
      .signWith(key)
      .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public String subject(String token) { return parse(token).getSubject(); }
  public long jwtMinutes() { return jwtMinutes; }
}
