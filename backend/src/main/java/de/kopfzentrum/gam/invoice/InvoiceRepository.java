package de.kopfzentrum.gam.invoice;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Repository
public class InvoiceRepository {
  private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;
  private final InvoiceCalculator calculator;

  public InvoiceRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, InvoiceCalculator calculator) {
    this.jdbc = jdbc;
    this.named = named;
    this.calculator = calculator;
  }

  public List<InvoiceSummary> findRecent(int limit) {
    return search(new InvoiceSearchCriteria(null, null, null, null, null, null, null, limit, 0));
  }

  public List<InvoiceSummary> search(InvoiceSearchCriteria criteria) {
    int limit = Math.min(Math.max(criteria.limit() <= 0 ? 100 : criteria.limit(), 1), 500);
    int offset = Math.max(criteria.offset(), 0);
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset);
    String q = criteria.q() == null ? "" : criteria.q().trim();
    p.addValue("q", "%" + q + "%");

    StringBuilder sql = new StringBuilder("""
      SELECT * FROM (
        SELECT d.ID, d.RNUMMER, d.RDATUM, d.ENDPREIS, d.RGESELLSCHAFTS_ID, g.gesellschaftsname,
               d.USERNAME, d.GUTSCHRIFT, d.STORNO, d.ZAHLUNGSAVIS, d.GRUND, d.GPREIS, d.RPROZENT, d.RBEMERKUNG, d.RATENANZAHL
        FROM rechnungsdetails d
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        WHERE d.RNUMMER IS NOT NULL

        UNION ALL

        SELECT pd.ID, CONCAT(pd.ID, 'P') AS RNUMMER, pd.RDATUM, pd.ENDPREIS, pd.RGESELLSCHAFTS_ID, g.gesellschaftsname,
               pd.USERNAME, pd.GUTSCHRIFT, pd.STORNO, pd.ZAHLUNGSAVIS, pd.GRUND, NULL AS GPREIS, NULL AS RPROZENT, NULL AS RBEMERKUNG, NULL AS RATENANZAHL
        FROM p_rechnungsdetails pd
        LEFT JOIN rechnungsgesellschaft g ON g.id = pd.RGESELLSCHAFTS_ID
        WHERE pd.ID IS NOT NULL
      ) x
      WHERE 1=1
      """);
    if (!q.isBlank()) sql.append(" AND (x.RNUMMER LIKE :q OR x.gesellschaftsname LIKE :q OR x.USERNAME LIKE :q OR x.GRUND LIKE :q) ");
    if (criteria.companyId() != null) { sql.append(" AND x.RGESELLSCHAFTS_ID = :companyId "); p.addValue("companyId", criteria.companyId()); }
    if (criteria.creditNote() != null) { sql.append(" AND COALESCE(x.GUTSCHRIFT,0) = :creditNote "); p.addValue("creditNote", criteria.creditNote()); }
    if (criteria.cancelled() != null) { sql.append(" AND COALESCE(x.STORNO,0) = :cancelled "); p.addValue("cancelled", criteria.cancelled()); }
    if (criteria.paymentAdvice() != null) { sql.append(" AND COALESCE(x.ZAHLUNGSAVIS,0) = :paymentAdvice "); p.addValue("paymentAdvice", criteria.paymentAdvice()); }
    sql.append(" ORDER BY x.ID DESC LIMIT :limit OFFSET :offset ");
    return named.query(sql.toString(), p, (rs, row) -> mapSummary(rs));
  }

  public InvoiceSummary findSummary(String number) {
    return findSummary(number, null);
  }

  public InvoiceSummary findSummary(String number, Integer companyId) {
    if (isProformaNumber(number)) return findProformaSummary(number, companyId);
    try {
      MapSqlParameterSource p = new MapSqlParameterSource()
        .addValue("number", number)
        .addValue("companyId", companyId);
      return named.queryForObject("""
        SELECT d.ID, d.RNUMMER, d.RDATUM, d.ENDPREIS, d.RGESELLSCHAFTS_ID, g.gesellschaftsname,
               d.USERNAME, d.GUTSCHRIFT, d.STORNO, d.ZAHLUNGSAVIS, d.GPREIS, d.RPROZENT, d.RBEMERKUNG, d.RATENANZAHL
        FROM rechnungsdetails d
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        WHERE d.RNUMMER = :number
          AND (:companyId IS NULL OR d.RGESELLSCHAFTS_ID = :companyId)
        LIMIT 1
        """, p, (rs, row) -> mapSummary(rs));
    } catch (EmptyResultDataAccessException ex) {
      throw new IllegalArgumentException("Rechnung nicht gefunden: " + number + (companyId == null ? "" : " fuer Gesellschaft " + companyId), ex);
    }
  }

  private InvoiceSummary findProformaSummary(String number) {
    return findProformaSummary(number, null);
  }

  private InvoiceSummary findProformaSummary(String number, Integer companyId) {
    int id = parseProformaId(number);
    try {
      MapSqlParameterSource p = new MapSqlParameterSource().addValue("id", id).addValue("companyId", companyId);
      return named.queryForObject("""
        SELECT pd.ID, CONCAT(pd.ID, 'P') AS RNUMMER, pd.RDATUM, pd.ENDPREIS, pd.RGESELLSCHAFTS_ID, g.gesellschaftsname,
               pd.USERNAME, pd.GUTSCHRIFT, pd.STORNO, pd.ZAHLUNGSAVIS, NULL AS GPREIS, NULL AS RPROZENT, NULL AS RBEMERKUNG, NULL AS RATENANZAHL
        FROM p_rechnungsdetails pd
        LEFT JOIN rechnungsgesellschaft g ON g.id = pd.RGESELLSCHAFTS_ID
        WHERE pd.ID = :id
          AND (:companyId IS NULL OR pd.RGESELLSCHAFTS_ID = :companyId)
        LIMIT 1
        """, p, (rs, row) -> mapSummary(rs));
    } catch (EmptyResultDataAccessException ex) {
      throw new IllegalArgumentException("Proforma-Rechnung nicht gefunden: " + number, ex);
    }
  }

  public List<InvoiceLine> findLines(String number) {
    return findLines(number, null);
  }

  public List<InvoiceLine> findLines(String number, Integer companyId) {
    if (isProformaNumber(number)) return findProformaLines(number);
    if (isStornoNumber(number) && rowsExist("storno", number, companyId)) return findLinesFromTable("storno", number, companyId);
    if (isCreditNumber(number) && rowsExist("gutschrift", number, companyId)) return findLinesFromTable("gutschrift", number, companyId);
    if (isPaymentAdviceNumber(number) && rowsExist("zahlungsavis", number, companyId)) return findLinesFromTable("zahlungsavis", number, companyId);
    return findLinesFromTable("rechnung", number, companyId);
  }

  private List<InvoiceLine> findLinesFromTable(String table, String number) {
    return findLinesFromTable(table, number, null);
  }

  private List<InvoiceLine> findLinesFromTable(String table, String number, Integer companyId) {
    String sql = """
      SELECT r.ID, r.NUMMER, r.MENGE, r.PRODUKT_ID, rd.code, rd.beschreibung, r.MWST, r.PREIS2,
             r.FILIALE_ID, r.AUFTRAGGEBER, r.`DURCHFÜHRENDER`
      FROM %s r
      LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = r.PRODUKT_ID
      WHERE r.NUMMER = :number
        AND (:companyId IS NULL OR r.RGESELLSCHAFTS_ID = :companyId)
      ORDER BY r.ID ASC
      """.formatted(table);
    return named.query(sql, new MapSqlParameterSource().addValue("number", number).addValue("companyId", companyId), (rs, row) -> new InvoiceLine(
      rs.getInt("ID"), rs.getString("NUMMER"), getDouble(rs, "MENGE"), getInt(rs, "PRODUKT_ID"), rs.getString("code"),
      rs.getString("beschreibung"), getInt(rs, "MWST"), getDouble(rs, "PREIS2"), getInt(rs, "FILIALE_ID"),
      rs.getString("AUFTRAGGEBER"), rs.getString("DURCHFÜHRENDER")));
  }

  private List<InvoiceLine> findProformaLines(String number) {
    int id = parseProformaId(number);
    return jdbc.query("""
      SELECT r.ID, CONCAT(r.PSRDID, 'P') AS NUMMER, r.MENGE, r.PRODUKT_ID, rd.code, rd.beschreibung, r.MWST, r.PREIS2,
             r.FILIALE_ID, r.AUFTRAGGEBER, r.`DURCHFÜHRENDER`
      FROM p_rechnung r
      LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = r.PRODUKT_ID
      WHERE r.PSRDID = ?
      ORDER BY r.ID ASC
      """, (rs, row) -> new InvoiceLine(
      rs.getInt("ID"), rs.getString("NUMMER"), getDouble(rs, "MENGE"), getInt(rs, "PRODUKT_ID"), rs.getString("code"),
      rs.getString("beschreibung"), getInt(rs, "MWST"), getDouble(rs, "PREIS2"), getInt(rs, "FILIALE_ID"),
      rs.getString("AUFTRAGGEBER"), rs.getString("DURCHFÜHRENDER")), id);
  }

  public InvoiceDetail findDetail(String number) {
    return findDetail(number, null);
  }

  public InvoiceDetail findDetail(String number, Integer companyId) {
    InvoiceSummary summary = findSummary(number, companyId);
    List<InvoiceLine> lines = findLines(number, summary.companyId());
    InvoiceTotals lineTotals = calculateFromExistingLines(lines);
    InvoiceTotals displayTotals = totalsWithStoredGross(lineTotals, summary.totalGross());
    return new InvoiceDetail(summary, lines, displayTotals);
  }

  public List<ProductDto> findProducts(String q, int limit) {
    String like = "%" + (q == null ? "" : q) + "%";
    return jdbc.query("""
      SELECT rdaten_id, code, beschreibung, kategorie, preis1, mwst, rgesellschafts_id
      FROM rechnungsdaten
      WHERE (? = '' OR code LIKE ? OR beschreibung LIKE ? OR kategorie LIKE ?)
      ORDER BY code ASC
      LIMIT ?
      """, (rs, row) -> new ProductDto(getInt(rs, "rdaten_id"), rs.getString("code"), rs.getString("beschreibung"), rs.getString("kategorie"), getDouble(rs, "preis1"), getInt(rs, "mwst"), getInt(rs, "rgesellschafts_id")), q == null ? "" : q, like, like, like, limit);
  }

  public List<InvoiceCompany> findCompanies() {
    return jdbc.query("""
      SELECT id, `gesellschaftskürzel`, gesellschaftsname, gesellschaftsadresse, `post_straße_nummer`, post_plz_ort,
             ustid, register, steuernummer, gerichtsstand, `gesellschaftsführer`, ansprechpartner,
             telefon, fax, email, kontoinhaber, iban, bic
      FROM rechnungsgesellschaft
      ORDER BY id ASC
      """, (rs, row) -> new InvoiceCompany(
        getInt(rs,"id"), rs.getString("gesellschaftskürzel"), rs.getString("gesellschaftsname"), rs.getString("gesellschaftsadresse"),
        rs.getString("post_straße_nummer"), rs.getString("post_plz_ort"), rs.getString("ustid"), rs.getString("register"),
        rs.getString("steuernummer"), rs.getString("gerichtsstand"), rs.getString("gesellschaftsführer"), rs.getString("ansprechpartner"),
        rs.getString("telefon"), rs.getString("fax"), rs.getString("email"), rs.getString("kontoinhaber"), rs.getString("iban"), rs.getString("bic")));
  }

  public InvoiceCompany findCompany(Integer id) {
    if (id == null) return null;
    return findCompanies().stream().filter(c -> id.equals(c.id())).findFirst().orElse(null);
  }

  public InvoiceNumberPreview nextInvoiceNumberPreview() {
    return nextInvoiceNumberPreview(null);
  }

  public InvoiceNumberPreview nextInvoiceNumberPreview(Integer companyId) {
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("companyId", companyId);
    String max = named.queryForObject("""
      SELECT COALESCE(MAX(CAST(NUMMER AS UNSIGNED)),0)
      FROM rechnung
      WHERE NUMMER REGEXP '^[0-9]+$'
        AND (:companyId IS NULL OR RGESELLSCHAFTS_ID = :companyId)
      """, p, String.class);
    long current = Long.parseLong(max == null || max.isBlank() ? "0" : max);
    return new InvoiceNumberPreview(Long.toString(current + 1), Long.toString(current), true,
      companyId == null
        ? "Normaler Nummernkreis aus rechnung.NUMMER. Storno = Original+S. Gutschrift aus gutschrift. Proforma aus p_rechnungsdetails."
        : "Normaler Nummernkreis aus rechnung.NUMMER fuer Gesellschaft " + companyId + ". Positionsbezug ueber rechnungsdetails.RNUMMER.");
  }

  public String nextInvoiceNumber() { return nextInvoiceNumberPreview(null).nextNumber(); }

  public String nextInvoiceNumber(Integer companyId) { return nextInvoiceNumberPreview(companyId).nextNumber(); }

  public String nextProformaNumber() {
    Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(ID),0) FROM p_rechnungsdetails", Integer.class);
    return ((max == null ? 0 : max) + 1) + "P";
  }

  public String nextCreditNumber() {
    String max = jdbc.queryForObject("SELECT COALESCE(MAX(CAST(REPLACE(NUMMER,'G','') AS UNSIGNED)),0) FROM gutschrift WHERE NUMMER REGEXP '^[0-9]+G$'", String.class);
    long current = Long.parseLong(max == null || max.isBlank() ? "0" : max);
    return (current + 1) + "G";
  }

  public boolean invoiceNumberExists(String number) {
    return invoiceNumberExists(number, null);
  }

  public boolean invoiceNumberExists(String number, Integer companyId) {
    if (isProformaNumber(number)) {
      MapSqlParameterSource p = new MapSqlParameterSource().addValue("id", parseProformaId(number)).addValue("companyId", companyId);
      Integer count = named.queryForObject("""
        SELECT COUNT(*) FROM p_rechnungsdetails
        WHERE ID = :id AND (:companyId IS NULL OR RGESELLSCHAFTS_ID = :companyId)
        """, p, Integer.class);
      return count != null && count > 0;
    }
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("number", number).addValue("companyId", companyId);
    Integer count = named.queryForObject("""
      SELECT COUNT(*) FROM rechnungsdetails
      WHERE RNUMMER = :number AND (:companyId IS NULL OR RGESELLSCHAFTS_ID = :companyId)
      """, p, Integer.class);
    return count != null && count > 0;
  }

  @Transactional
  public InvoiceCreateResponse createInvoice(InvoiceCreateRequest req, String username) {
    if (req.lines() == null || req.lines().isEmpty()) throw new IllegalArgumentException("Mindestens eine Rechnungsposition ist erforderlich.");
    Integer companyId = Objects.requireNonNullElse(req.companyId(), 2);
    String number = req.number() == null || req.number().isBlank() ? nextInvoiceNumber(companyId) : req.number().trim();
    if (invoiceNumberExists(number, companyId)) throw new IllegalArgumentException("Rechnungsnummer existiert bereits: " + number + " fuer Gesellschaft " + companyId);
    InvoiceTotals totals = calculateWithAdjustments(req.lines(), req);
    String invoiceDate = normalizeDate(req.invoiceDate());
    String treatmentDate = normalizeDate(req.treatmentDate() == null || req.treatmentDate().isBlank() ? req.invoiceDate() : req.treatmentDate());

    KeyHolder keyHolder = new GeneratedKeyHolder();
    named.update("""
      INSERT INTO rechnungsdetails
      (RNUMMER, ADRESSID, KINDADRESSID, FIRMAADRESSID, RDATUM, BDATUM, GBEMERKUNG, RGESELLSCHAFTS_ID,
       GUTSCHRIFT, STORNO, ZAHLUNGSAVIS, ZAHLUNGSART, GPREIS, RPROZENT, RBEMERKUNG, RATENANZAHL, ENDPREIS, USERNAME, GRUND, RFILIALE_ID, FADRESSE, FEMAIL)
      VALUES
      (:number, :addressId, :childAddressId, :firmAddressId, :invoiceDate, :treatmentDate, :remark, :companyId,
       :creditNote, :cancelled, :paymentAdvice, :paymentMethod, :couponAmount, :discountPercent, :discountRemark, :installments, :gross, :username, :reason, :branchId, :postal, :email)
      """, new MapSqlParameterSource()
        .addValue("number", number).addValue("addressId", req.addressId()).addValue("childAddressId", req.childAddressId())
        .addValue("firmAddressId", req.firmAddressId()).addValue("invoiceDate", invoiceDate).addValue("treatmentDate", treatmentDate)
        .addValue("remark", req.remark()).addValue("companyId", companyId)
        .addValue("creditNote", Boolean.TRUE.equals(req.creditNote())).addValue("cancelled", Boolean.TRUE.equals(req.cancelled()))
        .addValue("paymentAdvice", Boolean.TRUE.equals(req.paymentAdvice())).addValue("paymentMethod", blankToDefault(req.paymentMethod(), "unbekannt"))
        .addValue("couponAmount", positiveOrNull(req.couponAmount())).addValue("discountPercent", discountPercent(req.discountType(), req.discountValue()))
        .addValue("discountRemark", discountRemark(req)).addValue("installments", normalizeInstallments(req.installments()))
        .addValue("gross", totals.gross()).addValue("username", username).addValue("reason", req.reason()).addValue("branchId", req.branchId())
        .addValue("postal", true).addValue("email", false), keyHolder, new String[]{"ID"});

    insertLines("rechnung", number, req.lines(), companyId, req.branchId());
    createPaymentAdviceInstallments(number, req, username, companyId, totals);
    InvoiceDetail detail = findDetail(number, companyId);
    Number key = keyHolder.getKey();
    return new InvoiceCreateResponse(number, key == null ? null : key.intValue(), totals, detail);
  }

  public InvoiceTotals calculate(List<InvoiceCreateLineRequest> lines) { return calculator.calculate(lines); }

  public InvoiceTotals calculateFromExistingLines(List<InvoiceLine> lines) {
    List<InvoiceCreateLineRequest> req = lines.stream()
      .map(l -> new InvoiceCreateLineRequest(l.productId(), l.quantity(), l.price(), l.vat(), l.branchId(), l.client(), l.performer()))
      .toList();
    return calculator.calculate(req);
  }

  @Transactional
  public InvoiceCreateResponse updateInvoice(String number, InvoiceUpdateRequest req, String username) {
    if (number == null || number.isBlank()) throw new IllegalArgumentException("Rechnungsnummer fehlt.");
    if (isProformaNumber(number)) return updateProformaInvoice(number, req, username);
    if (!invoiceNumberExists(number)) throw new IllegalArgumentException("Rechnung nicht gefunden: " + number);
    if (req.lines() == null || req.lines().isEmpty()) throw new IllegalArgumentException("Mindestens eine Rechnungsposition ist erforderlich.");

    Integer companyId = Objects.requireNonNullElse(req.companyId(), findSummary(number).companyId());
    InvoiceTotals totals = calculateWithAdjustments(req.lines(), req);
    String invoiceDate = normalizeDate(req.invoiceDate());
    String treatmentDate = normalizeDate(req.treatmentDate() == null || req.treatmentDate().isBlank() ? req.invoiceDate() : req.treatmentDate());

    named.update("""
      UPDATE rechnungsdetails
      SET ADRESSID = :addressId,
          KINDADRESSID = :childAddressId,
          FIRMAADRESSID = :firmAddressId,
          RDATUM = :invoiceDate,
          BDATUM = :treatmentDate,
          GBEMERKUNG = :remark,
          RGESELLSCHAFTS_ID = :companyId,
          GUTSCHRIFT = :creditNote,
          STORNO = :cancelled,
          ZAHLUNGSAVIS = :paymentAdvice,
          ZAHLUNGSART = :paymentMethod,
          GPREIS = :couponAmount,
          RPROZENT = :discountPercent,
          RBEMERKUNG = :discountRemark,
          RATENANZAHL = :installments,
          ENDPREIS = :gross,
          USERNAME = :username,
          GRUND = :reason,
          RFILIALE_ID = :branchId
      WHERE RNUMMER = :number
      """, new MapSqlParameterSource()
        .addValue("number", number).addValue("addressId", req.addressId()).addValue("childAddressId", req.childAddressId())
        .addValue("firmAddressId", req.firmAddressId()).addValue("invoiceDate", invoiceDate).addValue("treatmentDate", treatmentDate)
        .addValue("remark", req.remark()).addValue("companyId", companyId)
        .addValue("creditNote", Boolean.TRUE.equals(req.creditNote())).addValue("cancelled", Boolean.TRUE.equals(req.cancelled()))
        .addValue("paymentAdvice", Boolean.TRUE.equals(req.paymentAdvice())).addValue("paymentMethod", blankToDefault(req.paymentMethod(), "unbekannt"))
        .addValue("couponAmount", positiveOrNull(req.couponAmount())).addValue("discountPercent", discountPercent(req.discountType(), req.discountValue()))
        .addValue("discountRemark", discountRemark(req)).addValue("installments", normalizeInstallments(req.installments()))
        .addValue("gross", totals.gross()).addValue("username", username).addValue("reason", req.reason()).addValue("branchId", req.branchId()));

    jdbc.update("DELETE FROM rechnung WHERE NUMMER = ?", number);
    jdbc.update("DELETE FROM zahlungsavis WHERE NUMMER LIKE ?", number + "Z%");
    insertLines("rechnung", number, req.lines(), companyId, req.branchId());
    createPaymentAdviceInstallments(number, req, username, companyId, totals);
    return new InvoiceCreateResponse(number, findSummary(number).id(), totals, findDetail(number));
  }

  private InvoiceCreateResponse updateProformaInvoice(String number, InvoiceUpdateRequest req, String username) {
    int id = parseProformaId(number);
    if (!invoiceNumberExists(number)) throw new IllegalArgumentException("Proforma-Rechnung nicht gefunden: " + number);
    if (req.lines() == null || req.lines().isEmpty()) throw new IllegalArgumentException("Mindestens eine Position ist erforderlich.");
    Integer companyId = Objects.requireNonNullElse(req.companyId(), findSummary(number).companyId());
    InvoiceTotals totals = calculateWithAdjustments(req.lines(), req);
    String invoiceDate = normalizeDate(req.invoiceDate());
    String treatmentDate = normalizeDate(req.treatmentDate() == null || req.treatmentDate().isBlank() ? req.invoiceDate() : req.treatmentDate());
    named.update("""
      UPDATE p_rechnungsdetails
      SET ADRESSID=:addressId, KINDADRESSID=:childAddressId, FIRMAADRESSID=:firmAddressId,
          RDATUM=:invoiceDate, BDATUM=:treatmentDate, GBEMERKUNG=:remark,
          RGESELLSCHAFTS_ID=:companyId, ZAHLUNGSART=:paymentMethod, ENDPREIS=:gross,
          USERNAME=:username, GRUND=:reason, RFILIALE_ID=:branchId
      WHERE ID=:id
      """, new MapSqlParameterSource().addValue("id", id).addValue("addressId", req.addressId()).addValue("childAddressId", req.childAddressId())
        .addValue("firmAddressId", req.firmAddressId()).addValue("invoiceDate", invoiceDate).addValue("treatmentDate", treatmentDate)
        .addValue("remark", req.remark()).addValue("companyId", companyId).addValue("paymentMethod", blankToDefault(req.paymentMethod(), "unbekannt"))
        .addValue("gross", totals.gross()).addValue("username", username).addValue("reason", req.reason()).addValue("branchId", req.branchId()));
    jdbc.update("DELETE FROM p_rechnung WHERE PSRDID = ?", id);
    insertProformaLines(id, req.lines(), companyId, req.branchId());
    return new InvoiceCreateResponse(number, id, totals, findDetail(number));
  }

  @Transactional
  public InvoiceDetail updateStatus(String number, InvoiceStatusUpdateRequest req, String username) {
    if (!invoiceNumberExists(number)) throw new IllegalArgumentException("Rechnung nicht gefunden: " + number);
    if (Boolean.TRUE.equals(req.cancelled())) return createCancellationInvoice(number, null, username).invoice();
    if (Boolean.TRUE.equals(req.creditNote())) return createCreditNote(number, null, username).invoice();
    named.update("""
      UPDATE rechnungsdetails
      SET STORNO = COALESCE(:cancelled, STORNO),
          GUTSCHRIFT = COALESCE(:creditNote, GUTSCHRIFT),
          ZAHLUNGSAVIS = COALESCE(:paymentAdvice, ZAHLUNGSAVIS),
          GRUND = COALESCE(:reason, GRUND),
          USERNAME = :username
      WHERE RNUMMER = :number
      """, new MapSqlParameterSource().addValue("number", number).addValue("cancelled", req.cancelled()).addValue("creditNote", req.creditNote())
        .addValue("paymentAdvice", req.paymentAdvice()).addValue("reason", req.reason()).addValue("username", username));
    return findDetail(number);
  }

  @Transactional
  public InvoiceCreateResponse createCancellationInvoice(String originalNumber, String username) {
    return createCancellationInvoice(originalNumber, null, username);
  }

  @Transactional
  public InvoiceCreateResponse createCancellationInvoice(String originalNumber, Integer companyId, String username) {
    String original = normalizeBaseInvoiceNumber(originalNumber);
    return createSpecialInvoiceFromOriginal(original, companyId, original + "S", username, true, false, "Stornorechnung zu " + original, "storno");
  }

  @Transactional
  public InvoiceCreateResponse createCreditNote(String originalNumber, String username) {
    return createCreditNote(originalNumber, null, username);
  }

  @Transactional
  public InvoiceCreateResponse createCreditNote(String originalNumber, Integer companyId, String username) {
    String original = normalizeBaseInvoiceNumber(originalNumber);
    String targetNumber = nextCreditNumber();
    return createSpecialInvoiceFromOriginal(original, companyId, targetNumber, username, false, true, "Gutschrift zu " + original, "gutschrift");
  }

  @Transactional
  public InvoiceCreateResponse createProformaInvoice(InvoiceCreateRequest req, String username) {
    if (req.lines() == null || req.lines().isEmpty()) throw new IllegalArgumentException("Mindestens eine Proforma-Position ist erforderlich.");
    Integer companyId = Objects.requireNonNullElse(req.companyId(), 2);
    InvoiceTotals totals = calculateWithAdjustments(req.lines(), req);
    String invoiceDate = normalizeDate(req.invoiceDate());
    String treatmentDate = normalizeDate(req.treatmentDate() == null || req.treatmentDate().isBlank() ? req.invoiceDate() : req.treatmentDate());
    KeyHolder keyHolder = new GeneratedKeyHolder();
    named.update("""
      INSERT INTO p_rechnungsdetails
      (ADRESSID, KINDADRESSID, FIRMAADRESSID, RDATUM, BDATUM, GBEMERKUNG, RGESELLSCHAFTS_ID,
       GUTSCHRIFT, STORNO, ZAHLUNGSAVIS, ZAHLUNGSART, ENDPREIS, USERNAME, GRUND, RFILIALE_ID)
      VALUES
      (:addressId, :childAddressId, :firmAddressId, :invoiceDate, :treatmentDate, :remark, :companyId,
       0, 0, 1, :paymentMethod, :gross, :username, :reason, :branchId)
      """, new MapSqlParameterSource().addValue("addressId", req.addressId()).addValue("childAddressId", req.childAddressId())
        .addValue("firmAddressId", req.firmAddressId()).addValue("invoiceDate", invoiceDate).addValue("treatmentDate", treatmentDate)
        .addValue("remark", req.remark()).addValue("companyId", companyId).addValue("paymentMethod", blankToDefault(req.paymentMethod(), "unbekannt"))
        .addValue("gross", totals.gross()).addValue("username", username).addValue("reason", req.reason()).addValue("branchId", req.branchId()),
      keyHolder, new String[]{"ID"});
    Number key = keyHolder.getKey();
    int id = key == null ? jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class) : key.intValue();
    jdbc.update("UPDATE p_rechnungsdetails SET PSRDID = ? WHERE ID = ?", id, id);
    insertProformaLines(id, req.lines(), companyId, req.branchId());
    String number = id + "P";
    return new InvoiceCreateResponse(number, id, totals, findDetail(number));
  }

  private InvoiceCreateResponse createSpecialInvoiceFromOriginal(String originalNumber, Integer requestedCompanyId, String targetNumber, String username,
      boolean cancelled, boolean creditNote, String reason, String lineTable) {
    InvoiceSummary original = findSummary(originalNumber, requestedCompanyId);
    Integer companyId = original.companyId();
    if (invoiceNumberExists(targetNumber, companyId)) return new InvoiceCreateResponse(targetNumber, findSummary(targetNumber, companyId).id(), findDetail(targetNumber, companyId).totals(), findDetail(targetNumber, companyId));
    List<InvoiceLine> sourceLines = findLines(originalNumber, companyId);
    if (sourceLines.isEmpty()) throw new IllegalArgumentException("Originalrechnung hat keine Positionen: " + originalNumber);
    List<InvoiceCreateLineRequest> lines = sourceLines.stream()
      .map(l -> creditNote
        ? new InvoiceCreateLineRequest(l.productId(), positiveQuantity(l.quantity()), negativePrice(l.price()), l.vat(), l.branchId(), l.client(), l.performer())
        : new InvoiceCreateLineRequest(l.productId(), negativeQuantity(l.quantity()), positive(l.price()), l.vat(), l.branchId(), l.client(), l.performer()))
      .toList();
    InvoiceTotals totals = calculator.calculate(lines);
    String today = LocalDate.now().format(GERMAN_DATE);

    named.update("""
      INSERT INTO rechnungsdetails
      (RNUMMER, RDATUM, BDATUM, GBEMERKUNG, RGESELLSCHAFTS_ID,
       GUTSCHRIFT, STORNO, ZAHLUNGSAVIS, ZAHLUNGSART, ENDPREIS, USERNAME, GRUND, FADRESSE, FEMAIL)
      SELECT :targetNumber, :today, COALESCE(BDATUM, RDATUM, :today), GBEMERKUNG, RGESELLSCHAFTS_ID,
             :creditNote, :cancelled, 0, ZAHLUNGSART, :gross, :username, :reason, FADRESSE, FEMAIL
      FROM rechnungsdetails
      WHERE RNUMMER = :originalNumber
        AND RGESELLSCHAFTS_ID = :companyId
      LIMIT 1
      """, new MapSqlParameterSource().addValue("targetNumber", targetNumber).addValue("originalNumber", originalNumber).addValue("companyId", companyId).addValue("today", today)
        .addValue("creditNote", creditNote).addValue("cancelled", cancelled).addValue("gross", totals.gross()).addValue("username", username).addValue("reason", reason));

    insertLines(lineTable, targetNumber, lines, companyId, null);

    if (cancelled || creditNote) {
      named.update("""
        UPDATE rechnungsdetails
        SET STORNO = CASE WHEN :cancelled THEN 1 ELSE STORNO END,
            GUTSCHRIFT = CASE WHEN :creditNote THEN 1 ELSE GUTSCHRIFT END,
            GRUND = COALESCE(GRUND, :reason),
            USERNAME = :username
        WHERE RNUMMER = :originalNumber
          AND RGESELLSCHAFTS_ID = :companyId
        """, new MapSqlParameterSource().addValue("originalNumber", originalNumber).addValue("companyId", companyId).addValue("cancelled", cancelled)
          .addValue("creditNote", creditNote).addValue("reason", reason).addValue("username", username));
    }

    return new InvoiceCreateResponse(targetNumber, findSummary(targetNumber, companyId).id(), totals, findDetail(targetNumber, companyId));
  }

  private void insertLines(String table, String number, List<InvoiceCreateLineRequest> lines, Integer companyId, Integer fallbackBranchId) {
    for (InvoiceCreateLineRequest line : lines) {
      if (line.productId() == null) throw new IllegalArgumentException("Jede Rechnungsposition benötigt eine Produkt-ID.");
      String sql = """
        INSERT INTO %s (NUMMER, MENGE, PRODUKT_ID, MWST, PREIS2, RGESELLSCHAFTS_ID, AUFTRAGGEBER, `DURCHFÜHRENDER`, FILIALE_ID)
        VALUES (:number, :quantity, :productId, :vat, :price, :companyId, :client, :performer, :branchId)
        """.formatted(table);
      named.update(sql, new MapSqlParameterSource().addValue("number", number).addValue("quantity", line.quantity() == null ? 1.0 : line.quantity())
        .addValue("productId", line.productId()).addValue("vat", line.vat() == null ? 0 : line.vat()).addValue("price", line.price() == null ? 0.0 : line.price())
        .addValue("companyId", companyId).addValue("client", line.client()).addValue("performer", line.performer())
        .addValue("branchId", line.branchId() == null ? fallbackBranchId : line.branchId()));
    }
  }

  private void insertProformaLines(int psrdid, List<InvoiceCreateLineRequest> lines, Integer companyId, Integer fallbackBranchId) {
    for (InvoiceCreateLineRequest line : lines) {
      if (line.productId() == null) throw new IllegalArgumentException("Jede Proforma-Position benötigt eine Produkt-ID.");
      named.update("""
        INSERT INTO p_rechnung (PSRDID, MENGE, PRODUKT_ID, MWST, PREIS2, RGESELLSCHAFTS_ID, AUFTRAGGEBER, `DURCHFÜHRENDER`, FILIALE_ID)
        VALUES (:psrdid, :quantity, :productId, :vat, :price, :companyId, :client, :performer, :branchId)
        """, new MapSqlParameterSource().addValue("psrdid", psrdid).addValue("quantity", line.quantity() == null ? 1.0 : line.quantity())
        .addValue("productId", line.productId()).addValue("vat", line.vat() == null ? 0 : line.vat()).addValue("price", line.price() == null ? 0.0 : line.price())
        .addValue("companyId", companyId).addValue("client", line.client()).addValue("performer", line.performer())
        .addValue("branchId", line.branchId() == null ? fallbackBranchId : line.branchId()));
    }
  }

  private static Double negativeQuantity(Double value) {
    if (value == null) return -1.0;
    return -Math.abs(value);
  }
  private static Double positiveQuantity(Double value) {
    if (value == null) return 1.0;
    return Math.abs(value);
  }
  private static Double positive(Double value) { return value == null ? null : Math.abs(value); }
  private static Double negativePrice(Double value) { return value == null ? null : -Math.abs(value); }

  private static String normalizeBaseInvoiceNumber(String number) {
    if (number == null || number.isBlank()) throw new IllegalArgumentException("Rechnungsnummer fehlt.");
    String n = number.trim();
    if (n.endsWith("S") || n.endsWith("G")) return n.substring(0, n.length() - 1);
    return n;
  }


  private InvoiceTotals calculateWithAdjustments(List<InvoiceCreateLineRequest> lines, InvoiceCreateRequest req) {
    InvoiceTotals base = calculator.calculate(lines);
    return applyCommercialAdjustments(base, req.couponAmount(), req.discountType(), req.discountValue());
  }

  private InvoiceTotals calculateWithAdjustments(List<InvoiceCreateLineRequest> lines, InvoiceUpdateRequest req) {
    InvoiceTotals base = calculator.calculate(lines);
    return applyCommercialAdjustments(base, req.couponAmount(), req.discountType(), req.discountValue());
  }

  private InvoiceTotals applyCommercialAdjustments(InvoiceTotals base, Double couponAmount, String discountType, Double discountValue) {
    double gross = base.gross();
    double adjustment = 0.0;
    double coupon = positiveOrZero(couponAmount);
    if (coupon > 0) adjustment += coupon;
    if (discountValue != null && discountValue > 0) {
      if ("percent".equalsIgnoreCase(discountType) || "prozent".equalsIgnoreCase(discountType)) adjustment += gross * (discountValue / 100.0);
      else adjustment += discountValue;
    }
    double newGross = round(Math.max(0.0, gross - adjustment));
    double vatRatio = gross == 0.0 ? 0.0 : (base.vat() / gross);
    double newVat = round(newGross * vatRatio);
    double newNet = round(newGross - newVat);
    return new InvoiceTotals(newNet, newVat, newGross, base.vatByRate());
  }

  private void createPaymentAdviceInstallments(String number, InvoiceCreateRequest req, String username, Integer companyId, InvoiceTotals totals) {
    int installments = normalizeInstallments(req.installments());
    if (installments <= 1) return;
    double gross = totals.gross();
    double base = Math.floor((gross / installments) * 100.0) / 100.0;
    double assigned = 0.0;
    for (int i = 1; i <= installments; i++) {
      double amount = i == installments ? round(gross - assigned) : base;
      assigned = round(assigned + amount);
      String zNumber = number + "Z" + i;
      named.update("""
        INSERT INTO rechnungsdetails
        (RNUMMER, RDATUM, BDATUM, RGESELLSCHAFTS_ID, ZAHLUNGSAVIS, ZAHLUNGSART, ENDPREIS, USERNAME, GRUND, RFILIALE_ID)
        VALUES (:number, :date, :date, :companyId, 1, :paymentMethod, :amount, :username, :reason, :branchId)
        """, new MapSqlParameterSource().addValue("number", zNumber).addValue("date", normalizeDate(req.invoiceDate()))
          .addValue("companyId", companyId).addValue("paymentMethod", blankToDefault(req.paymentMethod(), "Ratenzahlung"))
          .addValue("amount", amount).addValue("username", username).addValue("reason", "Rate " + i + " von " + installments + " zu " + number)
          .addValue("branchId", req.branchId()));
      named.update("""
        INSERT INTO zahlungsavis (NUMMER, MENGE, PRODUKT_ID, MWST, PREIS2, AUFTRAGGEBER, `DURCHFÜHRENDER`, FILIALE_ID, RGESELLSCHAFTS_ID)
        VALUES (:number, 1, :productId, 0, :amount, '', '', :branchId, :companyId)
        """, new MapSqlParameterSource().addValue("number", zNumber).addValue("productId", req.lines().get(0).productId())
          .addValue("amount", amount).addValue("branchId", req.branchId() == null ? 0 : req.branchId()).addValue("companyId", companyId));
    }
  }


  private void createPaymentAdviceInstallments(String number, InvoiceUpdateRequest req, String username, Integer companyId, InvoiceTotals totals) {
    int installments = normalizeInstallments(req.installments());
    if (installments <= 1) return;
    double gross = totals.gross();
    double base = Math.floor((gross / installments) * 100.0) / 100.0;
    double assigned = 0.0;
    for (int i = 1; i <= installments; i++) {
      double amount = i == installments ? round(gross - assigned) : base;
      assigned = round(assigned + amount);
      String zNumber = number + "Z" + i;
      named.update("""
        INSERT INTO rechnungsdetails
        (RNUMMER, RDATUM, BDATUM, RGESELLSCHAFTS_ID, ZAHLUNGSAVIS, ZAHLUNGSART, ENDPREIS, USERNAME, GRUND, RFILIALE_ID)
        VALUES (:number, :date, :date, :companyId, 1, :paymentMethod, :amount, :username, :reason, :branchId)
        """, new MapSqlParameterSource().addValue("number", zNumber).addValue("date", normalizeDate(req.invoiceDate()))
          .addValue("companyId", companyId).addValue("paymentMethod", blankToDefault(req.paymentMethod(), "Ratenzahlung"))
          .addValue("amount", amount).addValue("username", username).addValue("reason", "Rate " + i + " von " + installments + " zu " + number)
          .addValue("branchId", req.branchId()));
      named.update("""
        INSERT INTO zahlungsavis (NUMMER, MENGE, PRODUKT_ID, MWST, PREIS2, AUFTRAGGEBER, `DURCHFÜHRENDER`, FILIALE_ID, RGESELLSCHAFTS_ID)
        VALUES (:number, 1, :productId, 0, :amount, '', '', :branchId, :companyId)
        """, new MapSqlParameterSource().addValue("number", zNumber).addValue("productId", req.lines().get(0).productId())
          .addValue("amount", amount).addValue("branchId", req.branchId() == null ? 0 : req.branchId()).addValue("companyId", companyId));
    }
  }

  private static int normalizeInstallments(Integer installments) {
    if (installments == null) return 1;
    return Math.min(5, Math.max(1, installments));
  }
  private static Double positiveOrNull(Double value) { return value == null || value <= 0 ? null : value; }
  private static double positiveOrZero(Double value) { return value == null || value <= 0 ? 0.0 : value; }
  private static Integer discountPercent(String type, Double value) {
    if (value == null || value <= 0) return null;
    return ("percent".equalsIgnoreCase(type) || "prozent".equalsIgnoreCase(type)) ? value.intValue() : null;
  }
  private static String discountRemark(InvoiceCreateRequest req) { return discountRemark(req.couponText(), req.discountType(), req.discountValue()); }
  private static String discountRemark(InvoiceUpdateRequest req) { return discountRemark(req.couponText(), req.discountType(), req.discountValue()); }
  private static String discountRemark(String couponText, String discountType, Double discountValue) {
    StringBuilder sb = new StringBuilder();
    if (couponText != null && !couponText.isBlank()) sb.append("Gutschein: ").append(couponText.trim());
    if (discountValue != null && discountValue > 0) {
      if (sb.length() > 0) sb.append("; ");
      sb.append("Rabatt: ").append(discountValue).append(("percent".equalsIgnoreCase(discountType) || "prozent".equalsIgnoreCase(discountType)) ? "%" : " EUR");
    }
    return sb.length() == 0 ? null : sb.toString();
  }
  private static double round(double v) { return Math.round(v * 100.0) / 100.0; }

  @Transactional
  public void deleteDraftCompletely(String number) {
    if (!invoiceNumberExists(number)) return;
    if (isProformaNumber(number)) {
      int id = parseProformaId(number);
      jdbc.update("DELETE FROM p_rechnung WHERE PSRDID = ?", id);
      jdbc.update("DELETE FROM p_rechnungsdetails WHERE ID = ?", id);
      return;
    }
    jdbc.update("DELETE FROM rechnung WHERE NUMMER = ?", number);
    jdbc.update("DELETE FROM storno WHERE NUMMER = ?", number);
    jdbc.update("DELETE FROM gutschrift WHERE NUMMER = ?", number);
    jdbc.update("DELETE FROM rechnungsdetails WHERE RNUMMER = ?", number);
  }

  private boolean rowsExist(String table, String number) {
    return rowsExist(table, number, null);
  }

  private boolean rowsExist(String table, String number, Integer companyId) {
    String sql = "SELECT COUNT(*) FROM " + table + " WHERE NUMMER = :number AND (:companyId IS NULL OR RGESELLSCHAFTS_ID = :companyId)";
    Integer count = named.queryForObject(sql, new MapSqlParameterSource().addValue("number", number).addValue("companyId", companyId), Integer.class);
    return count != null && count > 0;
  }

  private static boolean isStornoNumber(String number) { return number != null && number.trim().endsWith("S"); }
  private static boolean isCreditNumber(String number) { return number != null && number.trim().endsWith("G"); }
  private static boolean isProformaNumber(String number) { return number != null && number.trim().endsWith("P"); }
  private static boolean isPaymentAdviceNumber(String number) { return number != null && number.trim().matches(".*Z\\d*$"); }
  private static int parseProformaId(String number) {
    String n = number == null ? "" : number.trim();
    if (n.endsWith("P")) n = n.substring(0, n.length() - 1);
    return Integer.parseInt(n);
  }

  private InvoiceSummary mapSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new InvoiceSummary(
      rs.getInt("ID"), rs.getString("RNUMMER"), rs.getString("RDATUM"), getDouble(rs, "ENDPREIS"),
      getInt(rs, "RGESELLSCHAFTS_ID"), rs.getString("gesellschaftsname"), rs.getString("USERNAME"),
      getBool(rs, "GUTSCHRIFT"), getBool(rs, "STORNO"), getBool(rs, "ZAHLUNGSAVIS"),
      getDouble(rs, "GPREIS"), getInt(rs, "RPROZENT"), rs.getString("RBEMERKUNG"), getInt(rs, "RATENANZAHL"));
  }

  private static InvoiceTotals totalsWithStoredGross(InvoiceTotals base, Double storedGross) {
    if (storedGross == null) return base;
    double gross = round(storedGross);
    double ratio = base.gross() == 0.0 ? 0.0 : base.vat() / base.gross();
    double vat = round(gross * ratio);
    double net = round(gross - vat);
    return new InvoiceTotals(net, vat, gross, base.vatByRate());
  }

  private static String normalizeDate(String input) {
    if (input == null || input.isBlank()) return LocalDate.now().format(GERMAN_DATE);
    String s = input.trim();
    if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(s).format(GERMAN_DATE);
    return s;
  }
  private static String blankToDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
  private static Double getDouble(java.sql.ResultSet rs, String col) throws java.sql.SQLException { double v = rs.getDouble(col); return rs.wasNull() ? null : v; }
  private static Integer getInt(java.sql.ResultSet rs, String col) throws java.sql.SQLException { int v = rs.getInt(col); return rs.wasNull() ? null : v; }
  private static Boolean getBool(java.sql.ResultSet rs, String col) throws java.sql.SQLException { boolean v = rs.getBoolean(col); return rs.wasNull() ? null : v; }
}
