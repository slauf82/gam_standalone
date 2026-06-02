package de.kopfzentrum.gam.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PasskeyRepository {
  private final JdbcTemplate jdbc;

  public PasskeyRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void save(int accountId, String credentialId, String publicKey, String deviceName) {
    jdbc.update("""
      INSERT INTO account_passkeys (account_id, credential_id, public_key, device_name, active)
      VALUES (?, ?, ?, ?, TRUE)
      ON DUPLICATE KEY UPDATE
        public_key = VALUES(public_key),
        device_name = VALUES(device_name),
        active = TRUE
      """, accountId, credentialId, publicKey, deviceName);
  }

  public boolean existsActiveForUsername(String username, String credentialId) {
    Integer count = jdbc.queryForObject("""
      SELECT COUNT(*)
      FROM account_passkeys p
      JOIN accounts a ON a.id = p.account_id
      WHERE a.username = ? AND p.credential_id = ? AND p.active = TRUE
      """, Integer.class, username, credentialId);
    return count != null && count > 0;
  }

  public List<String> findCredentialIdsByUsername(String username) {
    return jdbc.query("""
      SELECT p.credential_id
      FROM account_passkeys p
      JOIN accounts a ON a.id = p.account_id
      WHERE a.username = ? AND p.active = TRUE
      ORDER BY p.created_at DESC
      """, (rs, row) -> rs.getString("credential_id"), username);
  }

  public int countActiveByUsername(String username) {
    Integer count = jdbc.queryForObject("""
      SELECT COUNT(*)
      FROM account_passkeys p
      JOIN accounts a ON a.id = p.account_id
      WHERE a.username = ? AND p.active = TRUE
      """, Integer.class, username);
    return count == null ? 0 : count;
  }
}
