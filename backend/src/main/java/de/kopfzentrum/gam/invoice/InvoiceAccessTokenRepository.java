package de.kopfzentrum.gam.invoice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Repository
public class InvoiceAccessTokenRepository {
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;
  private final int defaultValidityDays;

  public InvoiceAccessTokenRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named,
      @Value("${app.invoice.portal.default-validity-days:730}") int defaultValidityDays) {
    this.jdbc = jdbc;
    this.named = named;
    this.defaultValidityDays = defaultValidityDays;
    ensureTable();
  }

  private void ensureTable() {
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS invoice_access_tokens (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        invoice_number VARCHAR(80) NOT NULL,
        company_id INT NULL,
        address_id INT NULL,
        token VARCHAR(160) NOT NULL UNIQUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        expires_at TIMESTAMP NULL,
        active BOOLEAN DEFAULT TRUE,
        access_count INT DEFAULT 0,
        last_access TIMESTAMP NULL,
        INDEX idx_invoice_access_invoice (invoice_number, company_id),
        INDEX idx_invoice_access_address (address_id),
        INDEX idx_invoice_access_token (token)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8
      """);
  }

  public InvoiceAccessToken getOrCreate(String invoiceNumber, Integer companyId) {
    Integer addressId = findAddressId(invoiceNumber, companyId);
    try {
      return named.queryForObject("""
        SELECT * FROM invoice_access_tokens
        WHERE invoice_number = :number
          AND (:companyId IS NULL OR company_id = :companyId)
          AND active = TRUE
        ORDER BY id DESC LIMIT 1
        """, new MapSqlParameterSource().addValue("number", invoiceNumber).addValue("companyId", companyId), (rs, row) -> map(rs));
    } catch (EmptyResultDataAccessException ignored) { }

    String token = newToken();
    LocalDateTime expires = defaultValidityDays <= 0 ? null : LocalDateTime.now().plusDays(defaultValidityDays);
    named.update("""
      INSERT INTO invoice_access_tokens (invoice_number, company_id, address_id, token, expires_at, active)
      VALUES (:number, :companyId, :addressId, :token, :expiresAt, TRUE)
      """, new MapSqlParameterSource()
        .addValue("number", invoiceNumber).addValue("companyId", companyId)
        .addValue("addressId", addressId).addValue("token", token).addValue("expiresAt", expires));
    return findByToken(token);
  }

  public InvoiceAccessToken findByToken(String token) {
    InvoiceAccessToken access = named.queryForObject("SELECT * FROM invoice_access_tokens WHERE token = :token",
      new MapSqlParameterSource().addValue("token", token), (rs, row) -> map(rs));
    if (access == null || !access.active()) throw new IllegalArgumentException("Abruflink ist deaktiviert");
    if (access.expiresAt() != null && access.expiresAt().isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Abruflink ist abgelaufen");
    named.update("UPDATE invoice_access_tokens SET access_count = COALESCE(access_count,0)+1, last_access = CURRENT_TIMESTAMP WHERE token = :token",
      new MapSqlParameterSource().addValue("token", token));
    return access;
  }

  public List<InvoicePortalInvoice> listPatientInvoices(InvoiceAccessToken access) {
    if (access.addressId() == null) {
      return listSingleInvoice(access.invoiceNumber(), access.companyId());
    }
    return named.query("""
      SELECT d.RNUMMER, d.RDATUM, d.ENDPREIS, d.RGESELLSCHAFTS_ID, g.gesellschaftsname,
             d.GUTSCHRIFT, d.STORNO, d.ZAHLUNGSAVIS
      FROM rechnungsdetails d
      LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
      WHERE d.ADRESSID = :addressId
        AND COALESCE(d.STORNO,0) = 0
        AND d.RNUMMER IS NOT NULL
      GROUP BY d.RNUMMER, d.RDATUM, d.ENDPREIS, d.RGESELLSCHAFTS_ID, g.gesellschaftsname, d.GUTSCHRIFT, d.STORNO, d.ZAHLUNGSAVIS
      ORDER BY d.RDATUM DESC, d.ID DESC
      LIMIT 100
      """, new MapSqlParameterSource().addValue("addressId", access.addressId()), (rs, row) -> new InvoicePortalInvoice(
        rs.getString("RNUMMER"), rs.getString("RDATUM"), getDouble(rs, "ENDPREIS"), getInt(rs, "RGESELLSCHAFTS_ID"), rs.getString("gesellschaftsname"),
        getBool(rs, "GUTSCHRIFT"), getBool(rs, "STORNO"), getBool(rs, "ZAHLUNGSAVIS")));
  }

  private List<InvoicePortalInvoice> listSingleInvoice(String number, Integer companyId) {
    return named.query("""
      SELECT d.RNUMMER, d.RDATUM, d.ENDPREIS, d.RGESELLSCHAFTS_ID, g.gesellschaftsname,
             d.GUTSCHRIFT, d.STORNO, d.ZAHLUNGSAVIS
      FROM rechnungsdetails d
      LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
      WHERE d.RNUMMER = :number AND (:companyId IS NULL OR d.RGESELLSCHAFTS_ID = :companyId)
      LIMIT 1
      """, new MapSqlParameterSource().addValue("number", number).addValue("companyId", companyId), (rs, row) -> new InvoicePortalInvoice(
        rs.getString("RNUMMER"), rs.getString("RDATUM"), getDouble(rs, "ENDPREIS"), getInt(rs, "RGESELLSCHAFTS_ID"), rs.getString("gesellschaftsname"),
        getBool(rs, "GUTSCHRIFT"), getBool(rs, "STORNO"), getBool(rs, "ZAHLUNGSAVIS")));
  }

  private Integer findAddressId(String number, Integer companyId) {
    try {
      return named.queryForObject("""
        SELECT ADRESSID FROM rechnungsdetails
        WHERE RNUMMER = :number AND (:companyId IS NULL OR RGESELLSCHAFTS_ID = :companyId)
        LIMIT 1
        """, new MapSqlParameterSource().addValue("number", number).addValue("companyId", companyId), Integer.class);
    } catch (Exception e) { return null; }
  }

  private String newToken() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private InvoiceAccessToken map(java.sql.ResultSet rs) throws java.sql.SQLException {
    java.sql.Timestamp exp = rs.getTimestamp("expires_at");
    java.sql.Timestamp created = rs.getTimestamp("created_at");
    java.sql.Timestamp last = rs.getTimestamp("last_access");
    return new InvoiceAccessToken(rs.getLong("id"), rs.getString("invoice_number"), getInt(rs, "company_id"), getInt(rs, "address_id"), rs.getString("token"),
      created == null ? null : created.toLocalDateTime(), exp == null ? null : exp.toLocalDateTime(), rs.getBoolean("active"), getInt(rs, "access_count"), last == null ? null : last.toLocalDateTime());
  }
  private static Integer getInt(java.sql.ResultSet rs, String col) throws java.sql.SQLException { int v=rs.getInt(col); return rs.wasNull()?null:v; }
  private static Double getDouble(java.sql.ResultSet rs, String col) throws java.sql.SQLException { double v=rs.getDouble(col); return rs.wasNull()?null:v; }
  private static Boolean getBool(java.sql.ResultSet rs, String col) throws java.sql.SQLException { boolean v=rs.getBoolean(col); return rs.wasNull()?null:v; }
}
