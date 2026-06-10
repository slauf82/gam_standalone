package de.kopfzentrum.gam.reports;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InvoiceReportService {
  private static final DateTimeFormatter DE_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
  private final NamedParameterJdbcTemplate jdbc;

  public InvoiceReportService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

  public List<InvoiceReportRow> rows(LocalDate from, LocalDate to, Integer companyId) {
    return rows("overview", from, to, companyId);
  }

  public List<InvoiceReportRow> rows(String reportType, LocalDate from, LocalDate to, Integer companyId) {
    String type = normalizeReportType(reportType);
    MapSqlParameterSource p = params(from, to, companyId);
    String sql = switch (type) {
      case "allData" -> allDataSql();
      case "datev" -> datevSql();
      case "debitoren" -> debitorenSql();
      case "umsatz" -> umsatzSql();
      case "umsatzArzt" -> umsatzArztSql();
      case "umsatzFiliale" -> umsatzFilialeSql();
      case "tagesliste" -> tageslisteSql();
      case "produktranking" -> produktrankingSql();
      default -> overviewSql();
    };
    return jdbc.query(sql, p, (rs, rowNum) -> new InvoiceReportRow(
      rs.getString("datum"),
      getInt(rs, "gesellschaft_id"),
      rs.getString("gesellschaftsname"),
      rs.getString("typ"),
      rs.getString("nummer"),
      rs.getString("username"),
      rs.getString("grund"),
      getDouble(rs, "brutto"),
      getInt(rs, "storno") != null && getInt(rs, "storno") != 0,
      getInt(rs, "gutschrift") != null && getInt(rs, "gutschrift") != 0,
      getInt(rs, "zahlungsavis") != null && getInt(rs, "zahlungsavis") != 0
    ));
  }

  public InvoiceReportSummary summary(LocalDate from, LocalDate to, Integer companyId) {
    return summary("overview", from, to, companyId);
  }

  public InvoiceReportSummary summary(String reportType, LocalDate from, LocalDate to, Integer companyId) {
    List<InvoiceReportRow> rows = rows(reportType, from, to, companyId);
    double gross = rows.stream().mapToDouble(r -> r.gross() == null ? 0.0 : r.gross()).sum();
    String type = normalizeReportType(reportType);
    return new InvoiceReportSummary(
      rows.size(),
      gross,
      rows.stream().filter(r -> "Rechnung".equals(r.documentType())).count(),
      rows.stream().filter(r -> Boolean.TRUE.equals(r.cancelled())).count(),
      rows.stream().filter(r -> Boolean.TRUE.equals(r.creditNote())).count(),
      rows.stream().filter(r -> Boolean.TRUE.equals(r.paymentAdvice())).count(),
      rows.stream().filter(r -> "Proforma-Rechnung".equals(r.documentType())).count(),
      note(type)
    );
  }

  private MapSqlParameterSource params(LocalDate from, LocalDate to, Integer companyId) {
    return new MapSqlParameterSource()
      .addValue("fromDate", from)
      .addValue("toDate", to)
      .addValue("companyId", companyId);
  }

  private String dateFilter(String alias) {
    return "(:fromDate IS NULL OR " + alias + ".report_date >= :fromDate) AND (:toDate IS NULL OR " + alias + ".report_date <= :toDate)";
  }

  private String overviewSql() {
    return """
      SELECT * FROM (
        SELECT d.ID,
               d.RNUMMER AS nummer,
               d.RDATUM AS datum,
               COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.ENDPREIS AS brutto,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id,
               g.gesellschaftsname,
               d.USERNAME,
               d.GRUND,
               CASE WHEN COALESCE(d.ZAHLUNGSAVIS,0) <> 0 OR d.RNUMMER LIKE '%Z' THEN 'Zahlungsavis'
                    WHEN COALESCE(d.STORNO,0) <> 0 OR d.RNUMMER LIKE '%S' THEN 'Stornorechnung'
                    WHEN COALESCE(d.GUTSCHRIFT,0) <> 0 OR d.RNUMMER LIKE '%G' THEN 'Gutschrift'
                    ELSE 'Rechnung' END AS typ,
               COALESCE(d.STORNO,0) AS storno,
               COALESCE(d.GUTSCHRIFT,0) AS gutschrift,
               COALESCE(d.ZAHLUNGSAVIS,0) AS zahlungsavis
        FROM rechnungsdetails d
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        WHERE d.RNUMMER IS NOT NULL

        UNION ALL

        SELECT pd.ID,
               CONCAT('PS_', pd.ID) AS nummer,
               pd.RDATUM AS datum,
               COALESCE(STR_TO_DATE(pd.RDATUM, '%d.%m.%Y'), STR_TO_DATE(pd.RDATUM, '%Y-%m-%d')) AS report_date,
               pd.ENDPREIS AS brutto,
               pd.RGESELLSCHAFTS_ID AS gesellschaft_id,
               g.gesellschaftsname,
               pd.USERNAME,
               pd.GRUND,
               'Proforma-Rechnung' AS typ,
               0 AS storno,
               0 AS gutschrift,
               0 AS zahlungsavis
        FROM p_rechnungsdetails pd
        LEFT JOIN rechnungsgesellschaft g ON g.id = pd.RGESELLSCHAFTS_ID
      ) x
      WHERE (:companyId IS NULL OR x.gesellschaft_id = :companyId)
        AND (:fromDate IS NULL OR x.report_date >= :fromDate)
        AND (:toDate IS NULL OR x.report_date <= :toDate)
      ORDER BY x.report_date ASC, x.ID ASC
      """;
  }

  private String allDataSql() {
    return positionUnionSql("Alle Daten", false) + " ORDER BY report_date ASC, nummer ASC, ID ASC";
  }

  private String datevSql() {
    return positionUnionSql("DATEV", true) + " ORDER BY report_date ASC, nummer ASC, ID ASC";
  }

  private String positionUnionSql(String label, boolean datevOnly) {
    String proformaDatev = datevOnly ? " AND COALESCE(pd.DATEV,0) = 1 " : "";
    return """
      SELECT * FROM (
        SELECT re.ID,
               d.RDATUM AS datum,
               COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id,
               g.gesellschaftsname,
               'Rechnung' AS typ,
               re.NUMMER AS nummer,
               re.AUFTRAGGEBER AS username,
               CONCAT(COALESCE(rd.CODE,''), ' ', COALESCE(rd.BESCHREIBUNG,''), ' | Menge ', COALESCE(re.MENGE,0), ' | MwSt ', COALESCE(re.MWST,0), '%') AS grund,
               (COALESCE(re.MENGE,0) * COALESCE(re.PREIS2,0)) AS brutto,
               0 AS storno,
               0 AS gutschrift,
               0 AS zahlungsavis
        FROM rechnung re
        JOIN rechnungsdetails d ON d.RNUMMER = re.NUMMER AND d.RGESELLSCHAFTS_ID = re.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = re.PRODUKT_ID
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID

        UNION ALL

        SELECT st.ID,
               d.RDATUM AS datum,
               COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id,
               g.gesellschaftsname,
               'Stornorechnung' AS typ,
               st.NUMMER AS nummer,
               st.AUFTRAGGEBER AS username,
               CONCAT(COALESCE(rd.CODE,''), ' ', COALESCE(rd.BESCHREIBUNG,''), ' | Menge ', COALESCE(st.MENGE,0), ' | MwSt ', COALESCE(st.MWST,0), '%') AS grund,
               (COALESCE(st.MENGE,0) * COALESCE(st.PREIS2,0)) AS brutto,
               1 AS storno,
               0 AS gutschrift,
               0 AS zahlungsavis
        FROM storno st
        JOIN rechnungsdetails d ON d.RNUMMER = st.NUMMER AND d.RGESELLSCHAFTS_ID = st.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = st.PRODUKT_ID
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID

        UNION ALL

        SELECT gu.ID,
               d.RDATUM AS datum,
               COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id,
               g.gesellschaftsname,
               'Gutschrift' AS typ,
               gu.NUMMER AS nummer,
               gu.AUFTRAGGEBER AS username,
               CONCAT(COALESCE(rd.CODE,''), ' ', COALESCE(rd.BESCHREIBUNG,''), ' | Menge ', COALESCE(gu.MENGE,0), ' | MwSt ', COALESCE(gu.MWST,0), '%') AS grund,
               (COALESCE(gu.MENGE,0) * COALESCE(gu.PREIS2,0)) AS brutto,
               0 AS storno,
               1 AS gutschrift,
               0 AS zahlungsavis
        FROM gutschrift gu
        JOIN rechnungsdetails d ON d.RNUMMER = gu.NUMMER AND d.RGESELLSCHAFTS_ID = gu.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = gu.PRODUKT_ID
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID

        UNION ALL

        SELECT za.ID,
               d.RDATUM AS datum,
               COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id,
               g.gesellschaftsname,
               'Zahlungsavis' AS typ,
               za.NUMMER AS nummer,
               za.AUFTRAGGEBER AS username,
               CONCAT(COALESCE(rd.CODE,''), ' ', COALESCE(rd.BESCHREIBUNG,''), ' | Menge ', COALESCE(za.MENGE,0), ' | MwSt ', COALESCE(za.MWST,0), '%') AS grund,
               (COALESCE(za.MENGE,0) * COALESCE(za.PREIS2,0)) AS brutto,
               0 AS storno,
               0 AS gutschrift,
               1 AS zahlungsavis
        FROM zahlungsavis za
        JOIN rechnungsdetails d ON d.RNUMMER = za.NUMMER AND d.RGESELLSCHAFTS_ID = za.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = za.PRODUKT_ID
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID

        UNION ALL

        SELECT pr.ID,
               pd.RDATUM AS datum,
               COALESCE(STR_TO_DATE(pd.RDATUM, '%d.%m.%Y'), STR_TO_DATE(pd.RDATUM, '%Y-%m-%d')) AS report_date,
               pd.RGESELLSCHAFTS_ID AS gesellschaft_id,
               g.gesellschaftsname,
               'Proforma-Rechnung' AS typ,
               CONCAT('PS_', pd.ID) AS nummer,
               pr.AUFTRAGGEBER AS username,
               CONCAT(COALESCE(rd.CODE,''), ' ', COALESCE(rd.BESCHREIBUNG,''), ' | Menge ', COALESCE(pr.MENGE,0), ' | MwSt ', COALESCE(pr.MWST,0), '%') AS grund,
               (COALESCE(pr.MENGE,0) * COALESCE(pr.PREIS2,0)) AS brutto,
               0 AS storno,
               0 AS gutschrift,
               0 AS zahlungsavis
        FROM p_rechnung pr
        JOIN p_rechnungsdetails pd ON pd.ID = pr.PSRDID AND pd.RGESELLSCHAFTS_ID = pr.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = pr.PRODUKT_ID
        LEFT JOIN rechnungsgesellschaft g ON g.id = pd.RGESELLSCHAFTS_ID
        WHERE 1=1 %s
      ) x
      WHERE (:companyId IS NULL OR x.gesellschaft_id = :companyId)
        AND (:fromDate IS NULL OR x.report_date >= :fromDate)
        AND (:toDate IS NULL OR x.report_date <= :toDate)
      """.formatted(proformaDatev);
  }

  private String debitorenSql() {
    return """
      SELECT MIN(x.ID) AS ID,
             MIN(x.datum) AS datum,
             MIN(x.report_date) AS report_date,
             x.gesellschaft_id,
             MIN(x.gesellschaftsname) AS gesellschaftsname,
             'Debitoren' AS typ,
             MIN(x.nummer) AS nummer,
             '' AS username,
             CONCAT(COALESCE(x.vorname,''), ' ', COALESCE(x.nachname,''), ' | ', COALESCE(x.strasse,''), ' | ', COALESCE(x.plz,''), ' ', COALESCE(x.ort,'')) AS grund,
             0 AS brutto,
             0 AS storno,
             0 AS gutschrift,
             0 AS zahlungsavis
      FROM (
        SELECT d.ID, d.RNUMMER AS nummer, d.RDATUM AS datum, COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id, g.gesellschaftsname, ad.VORNAME AS vorname, ad.NACHNAME AS nachname, ad.STRASSE AS strasse, ad.PLZ AS plz, ad.ORT AS ort
        FROM rechnungsdetails d
        JOIN adressen ad ON ad.ID = d.ADRESSID
        LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        UNION ALL
        SELECT pd.ID, CONCAT('PS_', pd.ID) AS nummer, pd.RDATUM AS datum, COALESCE(STR_TO_DATE(pd.RDATUM, '%d.%m.%Y'), STR_TO_DATE(pd.RDATUM, '%Y-%m-%d')) AS report_date,
               pd.RGESELLSCHAFTS_ID AS gesellschaft_id, g.gesellschaftsname, ad.VORNAME AS vorname, ad.NACHNAME AS nachname, ad.STRASSE AS strasse, ad.PLZ AS plz, ad.ORT AS ort
        FROM p_rechnungsdetails pd
        JOIN adressen ad ON ad.ID = pd.ADRESSID
        LEFT JOIN rechnungsgesellschaft g ON g.id = pd.RGESELLSCHAFTS_ID
      ) x
      WHERE (:companyId IS NULL OR x.gesellschaft_id = :companyId)
        AND (:fromDate IS NULL OR x.report_date >= :fromDate)
        AND (:toDate IS NULL OR x.report_date <= :toDate)
      GROUP BY x.gesellschaft_id, x.vorname, x.nachname, x.strasse, x.plz, x.ort
      ORDER BY x.nachname, x.vorname
      """;
  }

  private String umsatzSql() {
    return """
      SELECT MIN(x.ID) AS ID,
             MIN(x.datum) AS datum,
             MIN(x.report_date) AS report_date,
             x.gesellschaft_id,
             MIN(x.gesellschaftsname) AS gesellschaftsname,
             'Umsatz' AS typ,
             MIN(x.nummer) AS nummer,
             MIN(x.auftraggeber) AS username,
             CONCAT(x.beschreibung, ' | Menge ', FORMAT(SUM(x.menge),2), ' | Einzelpreis ', FORMAT(x.preis,2)) AS grund,
             SUM(x.menge * x.preis) AS brutto,
             0 AS storno,
             0 AS gutschrift,
             0 AS zahlungsavis
      FROM ( %s ) x
      WHERE (:companyId IS NULL OR x.gesellschaft_id = :companyId)
        AND (:fromDate IS NULL OR x.report_date >= :fromDate)
        AND (:toDate IS NULL OR x.report_date <= :toDate)
      GROUP BY x.gesellschaft_id, x.beschreibung, x.preis
      ORDER BY x.beschreibung, x.preis
      """.formatted(productBaseUnion());
  }

  private String umsatzArztSql() {
    return """
      SELECT MIN(x.ID) AS ID,
             MIN(x.datum) AS datum,
             MIN(x.report_date) AS report_date,
             x.gesellschaft_id,
             MIN(x.gesellschaftsname) AS gesellschaftsname,
             'Umsatz je Arzt' AS typ,
             x.auftraggeber AS nummer,
             x.auftraggeber AS username,
             CONCAT(COALESCE(x.beschreibung,''), ' | Menge ', FORMAT(SUM(x.menge),2), ' | Einzelpreis ', FORMAT(x.preis,2)) AS grund,
             SUM(x.menge * x.preis) AS brutto,
             0 AS storno,
             0 AS gutschrift,
             0 AS zahlungsavis
      FROM ( %s ) x
      WHERE (:companyId IS NULL OR x.gesellschaft_id = :companyId)
        AND (:fromDate IS NULL OR x.report_date >= :fromDate)
        AND (:toDate IS NULL OR x.report_date <= :toDate)
      GROUP BY x.gesellschaft_id, x.auftraggeber, x.beschreibung, x.preis
      ORDER BY x.auftraggeber, x.beschreibung
      """.formatted(productBaseUnion());
  }

  private String umsatzFilialeSql() {
    return """
      SELECT MIN(x.ID) AS ID,
             MIN(x.datum) AS datum,
             MIN(x.report_date) AS report_date,
             x.gesellschaft_id,
             MIN(x.gesellschaftsname) AS gesellschaftsname,
             'Umsatz je Filiale' AS typ,
             CAST(x.filiale_id AS CHAR) AS nummer,
             '' AS username,
             CONCAT('Filiale ', COALESCE(CAST(x.filiale_id AS CHAR),'—'), ' | Menge ', FORMAT(SUM(x.menge),2), ' | Einzelpreis ', FORMAT(x.preis,2)) AS grund,
             SUM(x.menge * x.preis) AS brutto,
             0 AS storno,
             0 AS gutschrift,
             0 AS zahlungsavis
      FROM ( %s ) x
      WHERE (:companyId IS NULL OR x.gesellschaft_id = :companyId)
        AND (:fromDate IS NULL OR x.report_date >= :fromDate)
        AND (:toDate IS NULL OR x.report_date <= :toDate)
      GROUP BY x.gesellschaft_id, x.filiale_id, x.preis
      ORDER BY x.filiale_id, x.preis
      """.formatted(productBaseUnion());
  }

  private String produktrankingSql() {
    return """
      SELECT MIN(x.ID) AS ID,
             MIN(x.datum) AS datum,
             MIN(x.report_date) AS report_date,
             x.gesellschaft_id,
             MIN(x.gesellschaftsname) AS gesellschaftsname,
             'Produktranking' AS typ,
             MIN(x.nummer) AS nummer,
             '' AS username,
             CONCAT(x.beschreibung, ' | Menge ', FORMAT(SUM(x.menge),2), ' | Einzelpreis ', FORMAT(x.preis,2)) AS grund,
             SUM(x.menge * x.preis) AS brutto,
             0 AS storno,
             0 AS gutschrift,
             0 AS zahlungsavis
      FROM ( %s ) x
      WHERE (:companyId IS NULL OR x.gesellschaft_id = :companyId)
        AND (:fromDate IS NULL OR x.report_date >= :fromDate)
        AND (:toDate IS NULL OR x.report_date <= :toDate)
      GROUP BY x.gesellschaft_id, x.beschreibung, x.preis
      ORDER BY SUM(x.menge) DESC, x.beschreibung
      """.formatted(productBaseUnion());
  }

  private String tageslisteSql() {
    return """
      SELECT MIN(x.ID) AS ID,
             x.datum,
             x.report_date,
             x.gesellschaft_id,
             MIN(x.gesellschaftsname) AS gesellschaftsname,
             'Tagesliste' AS typ,
             x.nummer,
             MIN(x.auftraggeber) AS username,
             CONCAT(COALESCE(MIN(x.vorname),''), ' ', COALESCE(MIN(x.nachname),''), ' | Zahlungsart ', COALESCE(MIN(x.zahlungsart),'')) AS grund,
             SUM(x.menge * x.preis) AS brutto,
             MAX(x.storno) AS storno,
             0 AS gutschrift,
             0 AS zahlungsavis
      FROM ( %s ) x
      WHERE (:companyId IS NULL OR x.gesellschaft_id = :companyId)
        AND (:fromDate IS NULL OR x.report_date >= :fromDate)
        AND (:toDate IS NULL OR x.report_date <= :toDate)
      GROUP BY x.gesellschaft_id, x.datum, x.report_date, x.nummer
      ORDER BY x.report_date, x.nummer
      """.formatted(productBaseUnionWithPatient());
  }

  private String productBaseUnion() {
    return """
        SELECT re.ID, re.NUMMER AS nummer, d.RDATUM AS datum, COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id, g.gesellschaftsname, COALESCE(rd.BESCHREIBUNG,'') AS beschreibung, COALESCE(re.MENGE,0) AS menge, COALESCE(re.PREIS2,0) AS preis,
               re.AUFTRAGGEBER AS auftraggeber, re.FILIALE_ID AS filiale_id, d.ZAHLUNGSART AS zahlungsart, 0 AS storno
        FROM rechnung re JOIN rechnungsdetails d ON d.RNUMMER = re.NUMMER AND d.RGESELLSCHAFTS_ID = re.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = re.PRODUKT_ID LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        UNION ALL
        SELECT st.ID, st.NUMMER AS nummer, d.RDATUM AS datum, COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id, g.gesellschaftsname, COALESCE(rd.BESCHREIBUNG,'') AS beschreibung, COALESCE(st.MENGE,0) AS menge, COALESCE(st.PREIS2,0) AS preis,
               st.AUFTRAGGEBER AS auftraggeber, st.FILIALE_ID AS filiale_id, d.ZAHLUNGSART AS zahlungsart, 1 AS storno
        FROM storno st JOIN rechnungsdetails d ON d.RNUMMER = st.NUMMER AND d.RGESELLSCHAFTS_ID = st.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = st.PRODUKT_ID LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        UNION ALL
        SELECT gu.ID, gu.NUMMER AS nummer, d.RDATUM AS datum, COALESCE(STR_TO_DATE(d.RDATUM, '%d.%m.%Y'), STR_TO_DATE(d.RDATUM, '%Y-%m-%d')) AS report_date,
               d.RGESELLSCHAFTS_ID AS gesellschaft_id, g.gesellschaftsname, COALESCE(rd.BESCHREIBUNG,'') AS beschreibung, COALESCE(gu.MENGE,0) AS menge, COALESCE(gu.PREIS2,0) AS preis,
               gu.AUFTRAGGEBER AS auftraggeber, gu.FILIALE_ID AS filiale_id, d.ZAHLUNGSART AS zahlungsart, 0 AS storno
        FROM gutschrift gu JOIN rechnungsdetails d ON d.RNUMMER = gu.NUMMER AND d.RGESELLSCHAFTS_ID = gu.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = gu.PRODUKT_ID LEFT JOIN rechnungsgesellschaft g ON g.id = d.RGESELLSCHAFTS_ID
        UNION ALL
        SELECT pr.ID, CONCAT('PS_', pd.ID) AS nummer, pd.RDATUM AS datum, COALESCE(STR_TO_DATE(pd.RDATUM, '%d.%m.%Y'), STR_TO_DATE(pd.RDATUM, '%Y-%m-%d')) AS report_date,
               pd.RGESELLSCHAFTS_ID AS gesellschaft_id, g.gesellschaftsname, COALESCE(rd.BESCHREIBUNG,'') AS beschreibung, COALESCE(pr.MENGE,0) AS menge, COALESCE(pr.PREIS2,0) AS preis,
               pr.AUFTRAGGEBER AS auftraggeber, pr.FILIALE_ID AS filiale_id, NULL AS zahlungsart, 0 AS storno
        FROM p_rechnung pr JOIN p_rechnungsdetails pd ON pd.ID = pr.PSRDID AND pd.RGESELLSCHAFTS_ID = pr.RGESELLSCHAFTS_ID
        LEFT JOIN rechnungsdaten rd ON rd.rdaten_id = pr.PRODUKT_ID LEFT JOIN rechnungsgesellschaft g ON g.id = pd.RGESELLSCHAFTS_ID
      """;
  }

  private String productBaseUnionWithPatient() {
    return """
      SELECT x.*, ad.VORNAME AS vorname, ad.NACHNAME AS nachname FROM (
      """ + productBaseUnion() + """
      ) x
      LEFT JOIN rechnungsdetails d2 ON d2.RNUMMER = x.nummer AND d2.RGESELLSCHAFTS_ID = x.gesellschaft_id
      LEFT JOIN p_rechnungsdetails pd2 ON CONCAT('PS_', pd2.ID) = x.nummer AND pd2.RGESELLSCHAFTS_ID = x.gesellschaft_id
      LEFT JOIN adressen ad ON ad.ID = COALESCE(d2.ADRESSID, pd2.ADRESSID)
      """;
  }

  public byte[] csv(List<InvoiceReportRow> rows) {
    StringBuilder sb = new StringBuilder('\ufeff');
    sb.append("Datum;Gesellschaft;Report/Belegart;Nummer/Gruppe;Benutzer/Arzt;Beschreibung/Grund;Betrag;Storno;Gutschrift;Zahlungsavis\n");
    for (InvoiceReportRow r : rows) {
      sb.append(csv(r.invoiceDate())).append(';')
        .append(csv(r.companyName())).append(';')
        .append(csv(r.documentType())).append(';')
        .append(csv(r.number())).append(';')
        .append(csv(r.username())).append(';')
        .append(csv(r.reason())).append(';')
        .append(csv(amount(r.gross()))).append(';')
        .append(Boolean.TRUE.equals(r.cancelled()) ? "1" : "0").append(';')
        .append(Boolean.TRUE.equals(r.creditNote()) ? "1" : "0").append(';')
        .append(Boolean.TRUE.equals(r.paymentAdvice()) ? "1" : "0").append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  public byte[] datevCsv(List<InvoiceReportRow> rows) {
    StringBuilder sb = new StringBuilder('\ufeff');
    sb.append("Umsatz;Soll/Haben;WKZ Umsatz;Belegdatum;Belegfeld 1;Buchungstext;KOST1;Gegenkonto;Konto\n");
    for (InvoiceReportRow r : rows) {
      double value = r.gross() == null ? 0.0 : Math.abs(r.gross());
      boolean negativeDocument = Boolean.TRUE.equals(r.cancelled()) || Boolean.TRUE.equals(r.creditNote()) || (r.gross() != null && r.gross() < 0);
      sb.append(csv(amount(value))).append(';')
        .append(negativeDocument ? "H" : "S").append(';')
        .append("EUR").append(';')
        .append(csv(normalizeDate(r.invoiceDate()))).append(';')
        .append(csv(r.number())).append(';')
        .append(csv(r.documentType() + " " + nullToEmpty(r.companyName()) + " " + nullToEmpty(r.reason()))).append(';')
        .append(csv(r.companyId() == null ? "" : String.valueOf(r.companyId()))).append(';')
        .append("8400").append(';')
        .append("10000").append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  public byte[] xlsHtml(List<InvoiceReportRow> rows) {
    StringBuilder sb = new StringBuilder();
    sb.append("<html><head><meta charset=\"UTF-8\"></head><body><table border=\"1\">");
    sb.append("<tr><th>Datum</th><th>Gesellschaft</th><th>Report/Belegart</th><th>Nummer/Gruppe</th><th>Benutzer/Arzt</th><th>Beschreibung/Grund</th><th>Betrag</th></tr>");
    for (InvoiceReportRow r : rows) {
      sb.append("<tr><td>").append(xml(r.invoiceDate())).append("</td><td>").append(xml(r.companyName())).append("</td><td>").append(xml(r.documentType())).append("</td><td>").append(xml(r.number())).append("</td><td>").append(xml(r.username())).append("</td><td>").append(xml(r.reason())).append("</td><td>").append(amount(r.gross())).append("</td></tr>");
    }
    sb.append("</table></body></html>");
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  public byte[] xlsx(List<InvoiceReportRow> rows) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
        entry(zip, "[Content_Types].xml", """
          <?xml version="1.0" encoding="UTF-8"?>
          <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          </Types>
          """);
        entry(zip, "_rels/.rels", """
          <?xml version="1.0" encoding="UTF-8"?>
          <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
          </Relationships>
          """);
        entry(zip, "xl/workbook.xml", """
          <?xml version="1.0" encoding="UTF-8"?>
          <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets><sheet name="GAM Report" sheetId="1" r:id="rId1"/></sheets>
          </workbook>
          """);
        entry(zip, "xl/_rels/workbook.xml.rels", """
          <?xml version="1.0" encoding="UTF-8"?>
          <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          </Relationships>
          """);
        entry(zip, "xl/worksheets/sheet1.xml", sheetXml(rows));
      }
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("XLSX konnte nicht erzeugt werden", ex);
    }
  }

  private String sheetXml(List<InvoiceReportRow> rows) {
    StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
    String[] headers = {"Datum","Gesellschaft","Report/Belegart","Nummer/Gruppe","Benutzer/Arzt","Beschreibung/Grund","Betrag"};
    sb.append(row(1, headers));
    int i = 2;
    for (InvoiceReportRow r : rows) {
      sb.append(row(i++, new String[]{r.invoiceDate(), r.companyName(), r.documentType(), r.number(), r.username(), r.reason(), amount(r.gross())}));
    }
    sb.append("</sheetData></worksheet>");
    return sb.toString();
  }

  private String row(int rowIndex, String[] values) {
    StringBuilder sb = new StringBuilder("<row r=\"").append(rowIndex).append("\">");
    for (String v : values) sb.append("<c t=\"inlineStr\"><is><t>").append(xml(v)).append("</t></is></c>");
    return sb.append("</row>").toString();
  }

  private void entry(ZipOutputStream zip, String name, String content) throws java.io.IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(content.stripIndent().trim().getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private String normalizeReportType(String reportType) {
    if (reportType == null || reportType.isBlank()) return "overview";
    return switch (reportType) {
      case "allData", "datev", "debitoren", "umsatz", "umsatzArzt", "umsatzFiliale", "tagesliste", "produktranking" -> reportType;
      default -> "overview";
    };
  }

  private String note(String type) {
    return switch (type) {
      case "allData" -> "Alt-GAM Report: Export aller Daten / Positionsdaten. Entspricht fachlich ExportAllData.";
      case "datev" -> "Alt-GAM Report: DATEV-Export-Vorbereitung. Enthält Rechnung, Rechnungsdummy, Storno, Gutschrift und Proforma.";
      case "debitoren" -> "Alt-GAM Report: Debitorenliste im Zeitraum.";
      case "umsatz" -> "Alt-GAM Report: Umsatz nach Produkt/Preis.";
      case "umsatzArzt" -> "Alt-GAM Report: Umsatz je Arzt/Auftraggeber.";
      case "umsatzFiliale" -> "Alt-GAM Report: Umsatz je Filiale.";
      case "tagesliste" -> "Alt-GAM Report: Tagesliste im Zeitraum.";
      case "produktranking" -> "Alt-GAM Report: Produktranking.";
      default -> "Übersichtsreport für Rechnungen, Stornos, Gutschriften, Proforma und Zahlungsavis.";
    };
  }

  private static String csv(Object v) { return '"' + nullToEmpty(v).replace("\"", "\"\"") + '"'; }
  private static String xml(Object v) { return nullToEmpty(v).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
  private static String nullToEmpty(Object v) { return v == null ? "" : String.valueOf(v); }
  private static String amount(Double d) { return d == null ? "0,00" : String.format(Locale.GERMANY, "%.2f", d); }
  private static String normalizeDate(String s) { return s == null ? "" : s.replace(".", ""); }

  private static Double getDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    double d = rs.getDouble(column); return rs.wasNull() ? null : d;
  }
  private static Integer getInt(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    int i = rs.getInt(column); return rs.wasNull() ? null : i;
  }
}
