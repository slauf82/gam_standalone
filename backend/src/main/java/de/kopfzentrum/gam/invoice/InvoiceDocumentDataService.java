package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Einheitliche Quelle fuer Empfaenger- und Zahlungsdaten aller Rechnungsdokumente.
 * Rechnung, PDF, ZUGFeRD, Patientenportal und Zahlungsworkflow sollen keine
 * voneinander abweichenden Adress- oder Zahlungsart-Fallbacks mehr implementieren.
 */
@Service
public class InvoiceDocumentDataService {
  private final NamedParameterJdbcTemplate named;

  public InvoiceDocumentDataService(NamedParameterJdbcTemplate named) {
    this.named = named;
  }

  public InvoiceDocumentData load(String invoiceNumber, Integer companyId) {
    if (invoiceNumber == null || invoiceNumber.isBlank()) return empty(invoiceNumber, companyId);
    boolean proforma = invoiceNumber.toUpperCase(Locale.ROOT).endsWith("P");
    String table = proforma ? "p_rechnungsdetails" : "rechnungsdetails";
    String numberColumn = proforma ? "ID" : "RNUMMER";
    Object numberValue = proforma ? parseProformaId(invoiceNumber) : invoiceNumber;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("number", numberValue)
        .addValue("companyId", companyId);
    try {
      String storedColumns = proforma ? "NULL AS FADRESSE, NULL AS FEMAIL, NULL AS GPREIS, NULL AS GBEMERKUNG, NULL AS RPROZENT, NULL AS RBEMERKUNG" : "FADRESSE, FEMAIL, GPREIS, GBEMERKUNG, RPROZENT, RBEMERKUNG";
      Header header = named.queryForObject("""
          SELECT ADRESSID, KINDADRESSID, FIRMAADRESSID, RDATUM, BDATUM, ZAHLUNGSART, %s, RGESELLSCHAFTS_ID
          FROM %s
          WHERE %s = :number
            AND (:companyId IS NULL OR RGESELLSCHAFTS_ID = :companyId)
          ORDER BY ID DESC
          LIMIT 1
          """.formatted(storedColumns, table, numberColumn), params, (rs, row) -> new Header(
              getInteger(rs, "ADRESSID"),
              getInteger(rs, "KINDADRESSID"),
              getInteger(rs, "FIRMAADRESSID"),
              clean(rs.getString("RDATUM")),
              clean(rs.getString("BDATUM")),
              clean(rs.getString("ZAHLUNGSART")),
              getDouble(rs, "GPREIS"),
              clean(rs.getString("GBEMERKUNG")),
              getInteger(rs, "RPROZENT"),
              clean(rs.getString("RBEMERKUNG")),
              rs.getString("FADRESSE"),
              clean(rs.getString("FEMAIL")),
              getInteger(rs, "RGESELLSCHAFTS_ID")));

      if (header == null) return empty(invoiceNumber, companyId);
      LbdRecipient primaryPerson = findAddress(header.addressId());
      boolean masterData = primaryPerson != null && primaryPerson.found();
      if (!masterData) primaryPerson = recipientFromStoredAddress(invoiceNumber, header.storedAddress(), header.email());
      LbdRecipient treatedChild = findAddress(header.childAddressId());
      LbdRecipient company = findAddress(header.companyAddressId());
      LbdRecipient invoiceRecipient = company != null && company.found() ? company : primaryPerson;
      LbdRecipient treatedPerson = treatedChild != null && treatedChild.found() ? treatedChild : primaryPerson;
      String role = header.companyAddressId() != null ? "COMPANY" : (header.childAddressId() != null ? "PARENT_GUARDIAN" : "SELF");
      return new InvoiceDocumentData(invoiceNumber,
          header.companyId() == null ? companyId : header.companyId(),
          header.addressId(), header.childAddressId(), header.companyAddressId(),
          header.invoiceDate(), header.treatmentDate(), header.couponAmount(), header.couponRemark(),
          header.discountPercent(), header.discountRemark(), header.paymentMethod(), role,
          primaryPerson, treatedChild, company, invoiceRecipient, treatedPerson,
          firstNonBlank(header.email(), rawEmail(invoiceRecipient)), masterData);
    } catch (EmptyResultDataAccessException ex) {
      return empty(invoiceNumber, companyId);
    }
  }


  /**
   * Fachliche Schutzlogik aus GAM 1.0: Minderjaehrige duerfen nicht selbst
   * Rechnungsempfaenger sein. Bei Kinderfaellen muss ein erwachsener
   * Rechnungsempfaenger sowie KINDADRESSID gesetzt sein. Firmen werden ueber
   * FIRMAADRESSID als Kostenuebernehmer getrennt gefuehrt.
   */
  public void validateRoleAssignment(Integer addressId, Integer childAddressId, Integer companyAddressId) {
    if (addressId == null || addressId <= 0) {
      throw new IllegalArgumentException("Ein Rechnungsempfaenger (ADRESSID) ist erforderlich.");
    }
    LbdRecipient primary = findAddress(addressId);
    if (primary == null || !primary.found()) {
      throw new IllegalArgumentException("Der Rechnungsempfaenger aus ADRESSID wurde in ADRESSEN nicht gefunden.");
    }
    if (childAddressId != null && childAddressId > 0) {
      if (childAddressId.equals(addressId)) {
        throw new IllegalArgumentException("Das behandelte Kind darf nicht zugleich Rechnungsempfaenger sein.");
      }
      LbdRecipient child = findAddress(childAddressId);
      if (child == null || !child.found()) {
        throw new IllegalArgumentException("Das behandelte Kind aus KINDADRESSID wurde in ADRESSEN nicht gefunden.");
      }
      if (isMinor(primary)) {
        throw new IllegalArgumentException("Als Rechnungsempfaenger muss bei einem Kinderfall ein volljaehriger Elternteil oder gesetzlicher Vertreter eingelesen werden.");
      }
    } else if (isMinor(primary)) {
      throw new IllegalArgumentException("Minderjaehrige duerfen keine Rechnung erhalten. Bitte zuerst die LBD von Mutter, Vater oder gesetzlichem Vertreter einlesen und das Kind ueber KINDADRESSID zuordnen.");
    }
    if (companyAddressId != null && companyAddressId > 0) {
      if (companyAddressId.equals(childAddressId)) {
        throw new IllegalArgumentException("Firma und behandeltes Kind duerfen nicht dieselbe Adresse verwenden.");
      }
      LbdRecipient company = findAddress(companyAddressId);
      if (company == null || !company.found()) {
        throw new IllegalArgumentException("Der Kostenuebernehmer aus FIRMAADRESSID wurde in ADRESSEN nicht gefunden.");
      }
    }
  }

  private static boolean isMinor(LbdRecipient recipient) {
    LocalDate birth = parseDate(recipient == null ? null : recipient.birthDate());
    return birth != null && Period.between(birth, LocalDate.now()).getYears() < 18;
  }

  private static LocalDate parseDate(String value) {
    String v = clean(value);
    if (v.isBlank()) return null;
    for (DateTimeFormatter formatter : new DateTimeFormatter[]{
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd.MM.uuuu"),
        DateTimeFormatter.ofPattern("dd/MM/uuuu")}) {
      try { return LocalDate.parse(v, formatter); } catch (DateTimeParseException ignored) { }
    }
    return null;
  }

  private LbdRecipient findAddress(Integer addressId) {
    if (addressId == null || addressId <= 0) return emptyRecipient();
    try {
      return named.queryForObject("""
          SELECT ID, PATIENTENNUMMER, ANREDE, TITEL, VORNAME, NAMENSZUSATZ, NACHNAME,
                 STRASSE, PLZ, ORT, LAND, GEBDATUM, VERSICHERTENNUMMER, VERSICHERTENART
          FROM adressen
          WHERE ID = :addressId
          LIMIT 1
          """, new MapSqlParameterSource("addressId", addressId), this::mapAddress);
    } catch (EmptyResultDataAccessException ex) {
      return emptyRecipient();
    }
  }

  private LbdRecipient mapAddress(ResultSet rs, int row) throws SQLException {
    String salutation = cleanByteLiteral(rs.getString("ANREDE"));
    return new LbdRecipient(true,
        "adressen:" + rs.getInt("ID"),
        clean(rs.getString("PATIENTENNUMMER")),
        clean(rs.getString("NAMENSZUSATZ")),
        clean(rs.getString("NACHNAME")),
        clean(rs.getString("VORNAME")),
        clean(rs.getString("GEBDATUM")),
        clean(rs.getString("TITEL")),
        clean(rs.getString("VERSICHERTENNUMMER")),
        clean(rs.getString("PLZ")),
        clean(rs.getString("ORT")),
        clean(rs.getString("LAND")),
        cleanHtmlBreaks(rs.getString("STRASSE")),
        clean(rs.getString("VERSICHERTENART")),
        salutationIndex(salutation),
        normalizedSalutation(salutation),
        Map.of("addressId", Integer.toString(rs.getInt("ID"))));
  }

  private static LbdRecipient recipientFromStoredAddress(String file, String postal, String email) {
    if (postal == null || postal.isBlank()) return emptyRecipient();
    String normalized = postal.replace("<br/>", "\n").replace("<br>", "\n")
        .replace("<br />", "\n").replace("\r", "");
    String[] lines = normalized.split("\n");
    String name = lines.length > 0 ? clean(lines[0]) : "";
    String street = lines.length > 1 ? cleanHtmlBreaks(lines[1]) : "";
    String cityLine = lines.length > 2 ? clean(lines[2]) : "";
    String country = lines.length > 3 ? clean(lines[3]) : "";
    String postalCode = "";
    String city = cityLine;
    Matcher cityMatcher = Pattern.compile("^(\\d{4,6})\\s+(.+)$").matcher(cityLine);
    if (cityMatcher.matches()) { postalCode = cityMatcher.group(1); city = cityMatcher.group(2); }

    ParsedName parsed = parseName(name);
    return new LbdRecipient(true, file, "", "", parsed.lastName(), parsed.firstName(), "",
        parsed.title(), "", postalCode, city, country, street, "", parsed.salutationIndex(),
        parsed.salutation(), Map.of("email", email == null ? "" : email, "source", "FADRESSE-fallback"));
  }

  private static ParsedName parseName(String value) {
    String remaining = cleanByteLiteral(value);
    Integer index = salutationIndex(remaining);
    String salutation = normalizedSalutation(remaining);
    remaining = remaining.replaceFirst("(?i)^(?:Herrn?|Frau|Divers|Mx\\.?|Mr\\.?|Mrs\\.?|Ms\\.?|[123]|b['\"][123]['\"])(?:\\s+|$)", "").trim();
    String title = "";
    Matcher titleMatcher = Pattern.compile("(?i)^((?:Prof\\. Dr\\.|PD Dr\\.|Prof\\.|Dr\\.)\\s+)(.*)$").matcher(remaining);
    if (titleMatcher.matches()) { title = titleMatcher.group(1).trim(); remaining = titleMatcher.group(2).trim(); }
    String[] parts = remaining.isBlank() ? new String[0] : remaining.split("\\s+");
    String first = parts.length > 1 ? parts[0] : "";
    String last = parts.length > 1 ? remaining.substring(first.length()).trim() : remaining;
    return new ParsedName(index, salutation, title, first, last);
  }

  private static Integer salutationIndex(String value) {
    String v = cleanByteLiteral(value).toLowerCase(Locale.ROOT);
    if (v.equals("1") || v.startsWith("herr") || v.equals("mr") || v.startsWith("mr.")) return 1;
    if (v.equals("2") || v.startsWith("frau") || v.startsWith("mrs") || v.startsWith("ms")) return 2;
    if (v.equals("3") || v.startsWith("divers") || v.startsWith("mx")) return 3;
    return null;
  }

  private static String normalizedSalutation(String value) {
    Integer index = salutationIndex(value);
    if (Integer.valueOf(1).equals(index)) return "Herr";
    if (Integer.valueOf(2).equals(index)) return "Frau";
    if (Integer.valueOf(3).equals(index)) return "Divers";
    return cleanByteLiteral(value);
  }

  private static String cleanByteLiteral(String value) {
    String v = clean(value);
    if (v.matches("(?i)^b['\"].*['\"]$")) return v.substring(2, v.length() - 1).trim();
    return v;
  }

  private static String cleanHtmlBreaks(String value) {
    return clean(value).replaceAll("(?i)<br\\s*/?>", " ").replaceAll("\\s+", " ").trim();
  }

  private static String clean(String value) { return value == null ? "" : value.trim(); }
  private static String firstNonBlank(String a, String b) { return a != null && !a.isBlank() ? a : (b == null ? "" : b); }
  private static String rawEmail(LbdRecipient r) { return r == null || r.rawFields() == null ? "" : r.rawFields().getOrDefault("email", ""); }
  private static Integer getInteger(ResultSet rs, String column) throws SQLException { int v = rs.getInt(column); return rs.wasNull() ? null : v; }
  private static Double getDouble(ResultSet rs, String column) throws SQLException { double v = rs.getDouble(column); return rs.wasNull() ? null : v; }
  private static int parseProformaId(String n) { return Integer.parseInt(n.substring(0, n.length() - 1)); }
  private static InvoiceDocumentData empty(String number, Integer companyId) { return new InvoiceDocumentData(number, companyId, null, null, null, "", "", null, "", null, "", null, "UNKNOWN", emptyRecipient(), emptyRecipient(), emptyRecipient(), emptyRecipient(), emptyRecipient(), "", false); }
  private static LbdRecipient emptyRecipient() { return new LbdRecipient(false, "", "", "", "", "", "", "", "", "", "", "", "", "", null, "", Map.of()); }

  private record Header(Integer addressId, Integer childAddressId, Integer companyAddressId, String invoiceDate, String treatmentDate, String paymentMethod, Double couponAmount, String couponRemark, Integer discountPercent, String discountRemark, String storedAddress, String email, Integer companyId) {}
  private record ParsedName(Integer salutationIndex, String salutation, String title, String firstName, String lastName) {}
}
