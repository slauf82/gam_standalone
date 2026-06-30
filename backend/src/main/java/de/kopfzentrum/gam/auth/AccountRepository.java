package de.kopfzentrum.gam.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;

@Repository
public class AccountRepository {
  private final JdbcTemplate jdbc;
  public AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Optional<Account> findByUsername(String username) {
    if (username == null || username.isBlank()) return Optional.empty();
    List<Account> list = jdbc.query("""
      SELECT id, username, password, fullname, role, email, secretkey
      FROM accounts
      WHERE username = ?
      LIMIT 1
      """, (rs, row) -> new Account(
        rs.getInt("id"),
        rs.getString("username"),
        rs.getString("password"),
        rs.getString("fullname"),
        rs.getString("role"),
        rs.getString("email"),
        rs.getString("secretkey")
      ), username.trim());
    return list.stream().findFirst();
  }


  public List<Account> findAll(String q, int limit) {
    String like = "%" + (q == null ? "" : q.trim()) + "%";
    int safeLimit = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 500));
    return jdbc.query("""
      SELECT id, username, password, fullname, role, email, secretkey
      FROM accounts
      WHERE (? = '%%' OR username LIKE ? OR fullname LIKE ? OR email LIKE ? OR role LIKE ?)
      ORDER BY username
      LIMIT ?
      """, (rs, row) -> new Account(
        rs.getInt("id"), rs.getString("username"), rs.getString("password"), rs.getString("fullname"),
        rs.getString("role"), rs.getString("email"), rs.getString("secretkey")
      ), like, like, like, like, like, safeLimit);
  }

  public Optional<Account> findById(int id) {
    try {
      return Optional.ofNullable(jdbc.queryForObject("""
        SELECT id, username, password, fullname, role, email, secretkey
        FROM accounts
        WHERE id = ?
        """, (rs, row) -> new Account(
          rs.getInt("id"), rs.getString("username"), rs.getString("password"), rs.getString("fullname"),
          rs.getString("role"), rs.getString("email"), rs.getString("secretkey")
        ), id));
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  public Account updateMetadata(int id, String fullname, String role, String email, String secretkey) {
    jdbc.update("""
      UPDATE accounts
      SET fullname = COALESCE(?, fullname),
          role = COALESCE(?, role),
          email = COALESCE(?, email),
          secretkey = COALESCE(?, secretkey)
      WHERE id = ?
      """, blankToNull(fullname), blankToNull(role), blankToNull(email), secretkey, id);
    return findById(id).orElseThrow();
  }

  public Account updateSecretKey(int id, String secretkey) {
    jdbc.update("""
      UPDATE accounts
      SET secretkey = ?
      WHERE id = ?
      """, blankToNull(secretkey), id);
    return findById(id).orElseThrow();
  }
  public Account create(String username, String rawPassword, String fullname, String role, String email) {
    String u = blankToNull(username);
    if (u == null) throw new IllegalArgumentException("Benutzername fehlt");
    String hash = sha256(blankToNull(rawPassword) == null ? "changeme" : rawPassword);
    jdbc.update("""
      INSERT INTO accounts (username, password, fullname, role, email, secretkey)
      VALUES (?, ?, ?, ?, ?, NULL)
      """, u, hash, blankToNull(fullname), blankToNull(role) == null ? "user" : blankToNull(role), blankToNull(email));
    return findByUsername(u).orElseThrow();
  }

  public Account updatePassword(int id, String rawPassword) {
    String pw = blankToNull(rawPassword);
    if (pw == null) throw new IllegalArgumentException("Passwort fehlt");
    jdbc.update("""
      UPDATE accounts
      SET password = ?
      WHERE id = ?
      """, sha256(pw), id);
    return findById(id).orElseThrow();
  }

  public void deleteById(int id) {
    jdbc.update("DELETE FROM accounts WHERE id = ?", id);
  }

  private String sha256(String value) {
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      return java.util.HexFormat.of().formatHex(md.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Passwort konnte nicht verschluesselt werden", e);
    }
  }


  public Account updateSecretKeyByUsername(String username, String secretkey) {
    String normalizedUsername = username == null ? "" : username.trim();
    if (normalizedUsername.isBlank()) {
      throw new IllegalArgumentException("Benutzername fehlt");
    }

    int changed = jdbc.update("""
      UPDATE accounts
      SET secretkey = ?
      WHERE username = ?
      """, blankToNull(secretkey), normalizedUsername);

    if (changed != 1) {
      throw new IllegalArgumentException("Secretkey konnte nicht gespeichert werden fuer Benutzer: " + normalizedUsername);
    }

    return findByUsername(normalizedUsername).orElseThrow();
  }

  private String blankToNull(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  public long countAccounts() {
    Long count = jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Long.class);
    return count == null ? 0 : count;
  }
}
