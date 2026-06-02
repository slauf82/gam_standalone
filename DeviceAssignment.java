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
               d.USERNAME, d.GUTSCHRIFT, d.STORNO, d.ZAHLUNGSAVIS, d.GRUND
        FROM rechnungsdetails d
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        WHERE d.RNUMMER IS NOT NULL

        UNION ALL

        SELECT pd.ID, CONCAT(pd.ID, 'P') AS RNUMMER, pd.RDATUM, pd.ENDPREIS, pd.RGESELLSCHAFTS_ID, g.gesellschaftsname,
               pd.USERNAME, pd.GUTSCHRIFT, pd.STORNO, pd.ZAHLUNGSAVIS, pd.GRUND
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
    if (isProformaNumber(number)) return findProformaSummary(number);
    try {
      return jdbc.queryForObject("""
        SELECT d.ID, d.RNUMMER, d.RDATUM, d.ENDPREIS, d.RGESELLSCHAFTS_ID, g.gesellschaftsname,
               d.USERNAME, d.GUTSCHRIFT, d.STORNO, d.ZAHLUNGSAVIS
        FROM rechnungsdetails d
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        WHERE d.RNUMMER = ? LIMIT 1
        """, (rs, row) -> mapSummary(rs), number);
    } catch (EmptyResultDataAccessException ex) {
      throw new IllegalArgumentException("Rechnung nicht gefunden: " + number, ex);
    }
  }

  private InvoiceSummary findProformaSummary(String number) {
    int id = parseProformaId(number);
    try {
      return jdbc.queryForObject("""
        SELECT pd.ID, CONCAT(pd.ID, 'P') AS RNUMMER, pd.RDATUM, pd.ENDPREIS, pd.RGESELLSCHAFTS_ID, g.gesellschaftsname,
               pd.USERNAME, pd.GUTSCHRIFT, pd.STORNO, pd.ZAHLUNGSAVIS
        FROM p_rechnungsdetails pd
        LEFT JOIN rechnungsgesellschaft g ON g.id = pd.RGESELLSCHAFTS_ID
        WHERE pd.ID = ? LIMIT 1
        """, (rs, row) -> mapSummary(rs), id);
    } catch (EmptyResultDataAccessException ex) {
      throw new IllegalArgumentException("Proforma-Rechnung nicht gefunden: " + number, ex);
    }
  }

  public List<InvoiceLine> findLines(String number) {
    if (isProformaNumber(number)) return findProformaLines(number);
    if (isStornoNumber(number) && rowsExist("storno", number)) return findLinesFromTable("storno", number);
    if (isCreditNumber(number) && rowsExist("gutschrift", number)) return findLinesFromTable("gutschrift", number);
    return findLinesFromTable("rechnung", number);
  }

  private List<InvoiceLine> findLinesFromTable(String table, String number) {
    String sql = """
      SELECT r.ID, r.NUMMER, r.MENGE, r.PRODUKT_ID, rd.code, rd.beschreibung, r.MWST, r.PREIS2,
             r.FILIALE_ID, r.AUFTRAGGEBER, r.`DURCHFÜHRENDER`
      FROM %s r
      LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = r.PRODUKT_ID
      WHERE r.NUMMER = ?
      ORDER BY r.ID ASC
      """.formatted(table);
    return jdbc.query(sql, (rs, row) -> new InvoiceLine(
      rs.getInt("ID"), rs.getString("NUMMER"), getDouble(rs, "MENGE"), getInt(rs, "PRODUKT_ID"), rs.getString("code"),
      rs.getString("beschreibung"), getInt(rs, "MWST"), getDouble(rs, "PREIS2"), getInt(rs, "FILIALE_ID"),
      rs.getString("AUFTRAGGEBER"), rs.getString("DURCHFÜHRENDER")), number);
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
    InvoiceSummary summary = findSummary(number);
    List<InvoiceLine> lines = findLines(number);
    return new InvoiceDetail(summary, lines, calculateFromExistingLines(lines));
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
    String max = jdbc.queryForObject("SELECT COALESCE(MAX(CAST(RNUMMER AS UNSIGNED)),0) FROM rechnungsdetails WHERE RNUMMER REGEXP '^[0-9]+$'", String.class);
    long current = Long.parseLong(max == null || max.isBlank() ? "0" : max);
    return new InvoiceNumberPreview(Long.toString(current + 1), Long.toString(current), true,
      "Normaler Nummernkreis aus rechnungsdetails. Storno = Original+S. Gutschrift aus gutschrift. Proforma aus p_rechnungsdetails.");
  }

  public String nextInvoiceNumber() { return nextInvoiceNumberPreview().nextNumber(); }

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
    if (isProformaNumber(number)) {
      Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM p_rechnungsdetails WHERE ID = ?", Integer.class, parseProformaId(number));
      return count != null && count > 0;
    }
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM rechnungsdetails WHERE RNUMMER = ?", Integer.class, number);
    return count != null && count > 0;
  }

  @Transactional
  public InvoiceCreateResponse createInvoice(InvoiceCreateRequest req, String username) {
    if (req.lines() == null || req.lines().isEmpty()) throw new IllegalArgumentException("Mindestens eine Rechnungsposition ist erforderlich.");
    String number = req.number() == null || req.number().isBlank() ? nextInvoiceNumber() : req.number().trim();
    if (invoiceNumberExists(number)) throw new IllegalArgumentException("Rechnungsnummer existiert bereits: " + number);
    Integer companyId = Objects.requireNonNullElse(req.companyId(), 2);
    InvoiceTotals totals = calculator.calculate(req.lines());
    String invoiceDate = normalizeDate(req.invoiceDate());
    String treatmentDate = normalizeDate(req.treatmentDate() == null || req.treatmentDate().isBlank() ? req.invoiceDate() : req.treatmentDate());

    KeyHolder keyHolder = new GeneratedKeyHolder();
    named.update("""
      INSERT INTO rechnungsdetails
      (RNUMMER, ADRESSID, KINDADRESSID, FIRMAADRESSID, RDATUM, BDATUM, GBEMERKUNG, RGESELLSCHAFTS_ID,
       GUTSCHRIFT, STORNO, ZAHLUNGSAVIS, ZAHLUNGSART, ENDPREIS, USERNAME, GRUND, RFILIALE_ID, FADRESSE, FEMAIL)
      VALUES
      (:number, :addressId, :childAddressId, :firmAddressId, :invoiceDate, :treatmentDate, :remark, :companyId,
       :creditNote, :cancelled, :paymentAdvice, :paymentMethod, :gross, :username, :reason, :branchId, :postal, :email)
      """, new MapSqlParameterSource()
        .addValue("number", number).addValue("addressId", req.addressId()).addValue("childAddressId", req.childAddressId())
        .addValue("firmAddressId", req.firmAddressId()).addValue("invoiceDate", invoiceDate).addValue("treatmentDate", treatmentDate)
        .addValue("remark", req.remark()).addValue("companyId", companyId)
        .addValue("creditNote", Boolean.TRUE.equals(req.creditNote())).addValue("cancelled", Boolean.TRUE.equals(req.cancelled()))
        .addValue("paymentAdvice", Boolean.TRUE.equals(req.paymentAdvice())).addValue("paymentMethod", blankToDefault(req.paymentMethod(), "unbekannt"))
        .addValue("gross", totals.gross()).addValue("username", username).addValue("reason", req.reason()).addValue("branchId", req.branchId())
        .addValue("postal", true).addValue("email", false), keyHolder, new String[]{"ID"});

    insertLines("rechnung", number, req.lines(), companyId, req.branchId());
    InvoiceDetail detail = findDetail(number);
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
    InvoiceTotals totals = calculator.calculate(req.lines());
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
        .addValue("gross", totals.gross()).addValue("username", username).addValue("reason", req.reason()).addValue("branchId", req.branchId()));

    jdbc.update("DELETE FROM rechnung WHERE NUMMER = ?", number);
    insertLines("rechnung", number, req.lines(), companyId, req.branchId());
    return new InvoiceCreateResponse(number, findSummary(number).id(), totals, findDetail(number));
  }

  private InvoiceCreateResponse updateProformaInvoice(String number, InvoiceUpdateRequest req, String username) {
    int id = parseProformaId(number);
    if (!invoiceNumberExists(number)) throw new IllegalArgumentException("Proforma-Rechnung nicht gefunden: " + number);
    if (req.lines() == null || req.lines().isEmpty()) throw new IllegalArgumentException("Mindestens eine Position ist erforderlich.");
    Integer companyId = Objects.requireNonNullElse(req.companyId(), findSummary(number).companyId());
    InvoiceTotals totals = calculator.calculate(req.lines());
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
    if (Boolean.TRUE.equals(req.cancelled())) return createCancellationInvoice(number, username).invoice();
    if (Boolean.TRUE.equals(req.creditNote())) return createCreditNote(number, username).invoice();
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
    String original = normalizeBaseInvoiceNumber(originalNumber);
    return createSpecialInvoiceFromOriginal(original, original + "S", username, true, false, "Stornorechnung zu " + original, "storno");
  }

  @Transactional
  public InvoiceCreateResponse createCreditNote(String originalNumber, String username) {
    String original = normalizeBaseInvoiceNumber(originalNumber);
    String targetNumber = nextCreditNumber();
    return createSpecialInvoiceFromOriginal(original, targetNumber, username, false, true, "Gutschrift zu " + original, "gutschrift");
  }

  @Transactional
  public InvoiceCreateResponse createProformaInvoice(InvoiceCreateRequest req, String username) {
    if (req.lines() == null || req.lines().isEmpty()) throw new IllegalArgumentException("Mindestens eine Proforma-Position ist erforderlich.");
    Integer companyId = Objects.requireNonNullElse(req.companyId(), 2);
    InvoiceTotals totals = calculator.calculate(req.lines());
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

  private InvoiceCreateResponse createSpecialInvoiceFromOriginal(String originalNumber, String targetNumber, String username,
      boolean cancelled, boolean creditNote, String reason, String lineTable) {
    if (invoiceNumberExists(targetNumber)) return new InvoiceCreateResponse(targetNumber, findSummary(targetNumber).id(), findDetail(targetNumber).totals(), findDetail(targetNumber));
    InvoiceSummary original = findSummary(originalNumber);
    List<InvoiceLine> sourceLines = findLines(originalNumber);
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
      LIMIT 1
      """, new MapSqlParameterSource().addValue("targetNumber", targetNumber).addValue("originalNumber", originalNumber).addValue("today", today)
        .addValue("creditNote", creditNote).addValue("cancelled", cancelled).addValue("gross", totals.gross()).addValue("username", username).addValue("reason", reason));

    insertLines(lineTable, targetNumber, lines, original.companyId(), null);

    if (cancelled || creditNote) {
      named.update("""
        UPDATE rechnungsdetails
        SET STORNO = CASE WHEN :cancelled THEN 1 ELSE STORNO END,
            GUTSCHRIFT = CASE WHEN :creditNote THEN 1 ELSE GUTSCHRIFT END,
            GRUND = COALESCE(GRUND, :reason),
            USERNAME = :username
        WHERE RNUMMER = :originalNumber
        """, new MapSqlParameterSource().addValue("originalNumber", originalNumber).addValue("cancelled", cancelled)
          .addValue("creditNote", creditNote).addValue("reason", reason).addValue("username", username));
    }

    return new InvoiceCreateResponse(targetNumber, findSummary(targetNumber).id(), totals, findDetail(targetNumber));
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
    String sql = "SELECT COUNT(*) FROM " + table + " WHERE NUMMER = ?";
    Integer count = jdbc.queryForObject(sql, Integer.class, number);
    return count != null && count > 0;
  }

  private static boolean isStornoNumber(String number) { return number != null && number.trim().endsWith("S"); }
  private static boolean isCreditNumber(String number) { return number != null && number.trim().endsWith("G"); }
  private static boolean isProformaNumber(String number) { return number != null && number.trim().endsWith("P"); }
  private static int parseProformaId(String number) {
    String n = number == null ? "" : number.trim();
    if (n.endsWith("P")) n = n.substring(0, n.length() - 1);
    return Integer.parseInt(n);
  }

  private InvoiceSummary mapSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new InvoiceSummary(
      rs.getInt("ID"), rs.getString("RNUMMER"), rs.getString("RDATUM"), getDouble(rs, "ENDPREIS"),
      getInt(rs, "RGESELLSCHAFTS_ID"), rs.getString("gesellschaftsname"), rs.getString("USERNAME"),
      getBool(rs, "GUTSCHRIFT"), getBool(rs, "STORNO"), getBool(rs, "ZAHLUNGSAVIS"));
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
