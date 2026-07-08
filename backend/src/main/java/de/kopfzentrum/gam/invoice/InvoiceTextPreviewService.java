package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class InvoiceTextPreviewService {
  private final InvoiceRepository repo;
  private final TranslationService translations;
  private final JdbcTemplate jdbc;

  public InvoiceTextPreviewService(InvoiceRepository repo, TranslationService translations, JdbcTemplate jdbc) {
    this.repo = repo;
    this.translations = translations;
    this.jdbc = jdbc;
  }

  public InvoiceTextPreview preview(Integer companyId, String language, String treatmentDate, LbdRecipient recipient) {
    InvoiceCompany company = companyId == null ? null : repo.findCompany(companyId);
    String lang = normalize(language);
    Map<String, String> labels = labels(lang);
    Map<String, String> assigned = assignedTexts(companyId);
    return new InvoiceTextPreview(
      lang,
      companyId,
      translations.invoice("invoice", lang),
      replace(resolveText(assigned, "salutation", "rechnungsanrede", hardcodedFallback("salutation")), lang, company, recipient, treatmentDate),
      replace(resolveText(assigned, "invoiceText", "rechnungstext", hardcodedFallback("invoiceText")), lang, company, recipient, treatmentDate),
      replace(resolveText(assigned, "legalNote", "rechnungsrechtlicherhinweis", hardcodedFallback("legalNote")), lang, company, recipient, treatmentDate),
      replace(resolveText(assigned, "greeting", "rechnungsgrussformel", hardcodedFallback("greeting")), lang, company, recipient, treatmentDate),
      labels
    );
  }

  private Map<String, String> assignedTexts(Integer companyId) {
    Map<String, String> result = new LinkedHashMap<>();
    if (companyId == null) return result;
    try {
      ensureInvoiceTextFallbackRows();
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungstext_gesellschaft_zuordnung` (" +
        "`ID` int NOT NULL AUTO_INCREMENT," +
        "`RGESELLSCHAFTS_ID` int(50) NOT NULL," +
        "`ANREDE_ID` int(50) DEFAULT NULL," +
        "`RECHNUNGSTEXT_ID` int(50) DEFAULT NULL," +
        "`RECHTLICHER_HINWEIS_ID` int(50) DEFAULT NULL," +
        "`GRUSSFORMEL_ID` int(50) DEFAULT NULL," +
        "PRIMARY KEY (`ID`)," +
        "UNIQUE KEY `uk_rechnungstext_gesellschaft` (`RGESELLSCHAFTS_ID`)" +
        ")");
      Map<String, Object> row = jdbc.queryForObject("""
        SELECT a.TEXT AS salutation, t.TEXT AS invoiceText, h.TEXT AS legalNote, g.TEXT AS greeting
          FROM rechnungstext_gesellschaft_zuordnung z
          LEFT JOIN rechnungsanrede a ON a.ID=z.ANREDE_ID
          LEFT JOIN rechnungstext t ON t.ID=z.RECHNUNGSTEXT_ID
          LEFT JOIN rechnungsrechtlicherhinweis h ON h.ID=z.RECHTLICHER_HINWEIS_ID
          LEFT JOIN rechnungsgrussformel g ON g.ID=z.GRUSSFORMEL_ID
         WHERE z.RGESELLSCHAFTS_ID=?
         LIMIT 1
        """, new ColumnMapRowMapper(), companyId);
      for (Map.Entry<String, Object> e : row.entrySet()) {
        if (e.getValue() != null && !String.valueOf(e.getValue()).isBlank()) result.put(e.getKey(), String.valueOf(e.getValue()));
      }
    } catch (Exception ignored) { }
    return result;
  }

  private String resolveText(Map<String, String> assigned, String key, String table, String emergency) {
    String specific = assigned.get(key);
    if (specific != null && !specific.isBlank()) return specific;
    String system = systemFallbackText(table);
    return system == null || system.isBlank() ? emergency : system;
  }

  private String systemFallbackText(String table) {
    try {
      ensureInvoiceTextFallbackRows();
      return jdbc.queryForObject("SELECT `TEXT` FROM `" + table + "` WHERE `ID`=0 LIMIT 1", String.class);
    } catch (Exception ignored) { return null; }
  }

  private void ensureInvoiceTextFallbackRows() {
    try {
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsanrede` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungstext` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsrechtlicherhinweis` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("CREATE TABLE IF NOT EXISTS `rechnungsgrussformel` (`ID` int NOT NULL AUTO_INCREMENT, `TEXT` text, PRIMARY KEY (`ID`))");
      jdbc.execute("SET SESSION sql_mode = CONCAT_WS(',', @@sql_mode, 'NO_AUTO_VALUE_ON_ZERO')");
      jdbc.update("INSERT IGNORE INTO `rechnungsanrede` (`ID`, `TEXT`) VALUES (0, ?)", "Sehr geehrte Damen und Herren,");
      jdbc.update("INSERT IGNORE INTO `rechnungstext` (`ID`, `TEXT`) VALUES (0, ?)", "Wir erlauben uns folgende Leistungen in Rechnung zu stellen.");
      jdbc.update("INSERT IGNORE INTO `rechnungsrechtlicherhinweis` (`ID`, `TEXT`) VALUES (0, ?)", "Bitte begleichen Sie den Rechnungsbetrag innerhalb der angegebenen Frist.");
      jdbc.update("INSERT IGNORE INTO `rechnungsgrussformel` (`ID`, `TEXT`) VALUES (0, ?)", "Mit freundlichen Grüßen");
    } catch (Exception ignored) { }
  }

  private String hardcodedFallback(String key) {
    return switch (key) {
      case "salutation" -> "Sehr geehrte Damen und Herren,";
      case "invoiceText" -> "Wir erlauben uns folgende Leistungen in Rechnung zu stellen.";
      case "legalNote" -> "Bitte begleichen Sie den Rechnungsbetrag innerhalb der angegebenen Frist.";
      case "greeting" -> "Mit freundlichen Grüßen";
      default -> "";
    };
  }

  private Map<String, String> labels(String language) {
    Map<String, String> map = new LinkedHashMap<>();
    String[] keys = {
      "invoice", "proformaInvoice", "credit", "cancellation", "paymentAdvice",
      "invoiceRecipient", "invoiceNoRecipient", "invoiceDate", "treatmentDate", "serviceDate", "dueDate", "invoiceCustomerFile", "invoiceUser", "invoicePaymentMethod",
      "invoiceAmount", "invoiceProductCode", "invoiceDescription", "invoiceTaxRate", "invoiceSinglePrice", "invoiceTotalPrice",
      "invoiceZugferdNote", "invoiceFallbackPdfNote", "net", "vat", "gross",
      "invoiceReducement", "invoiceReducedTotal", "invoiceCoupon", "invoiceInstallments", "invoiceInstallmentApprox",
      "bank", "invoiceTaxNumberVatId", "invoicePreviewTitle", "invoicePreviewHelp", "invoiceNoLines",
      "invoicePortalTitle", "invoicePortalHelp", "invoicePortalQrHint", "invoiceReadAloud"
    };
    for (String key : keys) map.put(key, translations.invoice(key, language));
    return map;
  }

  private String replace(String text, String language, InvoiceCompany company, LbdRecipient recipient, String treatmentDate) {
    if (text == null) return "";
    String result = text;
    result = result.replace("-br-", "\n");
    result = result.replace("<Anrede>", value(recipient == null ? null : recipient.salutation()));
    result = result.replace("<Titel>", value(recipient == null ? null : recipient.title()));
    result = result.replace("<Vorname>", value(recipient == null ? null : recipient.firstName()));
    result = result.replace("<Namenszusatz>", value(recipient == null ? null : recipient.nameSuffix()));
    result = result.replace("<Nachname>", value(recipient == null ? null : recipient.lastName()));
    result = result.replace("<Behandlungsdatum>", formatDate(treatmentDate, language));
    result = result.replace("<Gesellschaftsname>", value(company == null ? null : company.name()));
    result = result.replace("<SieIhrKind>", translations.invoice("invoiceYou", language));
    return result.replaceAll("[ \\t]+", " ").replace(" ,", ",").trim();
  }

  private static String value(String value) {
    return value == null ? "" : value.trim();
  }

  private static String formatDate(String value, String language) {
    if (value == null || value.isBlank()) return "";
    try {
      String s = value.trim();
      LocalDate d;
      if (s.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) d = LocalDate.parse(s, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
      else d = LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
      String lang = language == null ? "de" : language.toLowerCase(Locale.ROOT);
      return switch (lang) {
        case "en", "english" -> d.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        case "fr", "french", "it", "italian" -> d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        case "sv", "swedish" -> d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        default -> d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
      };
    } catch (Exception ignored) { return value; }
  }

  private static String normalize(String language) {
    if (language == null || language.isBlank()) return "de";
    String l = language.trim().toLowerCase();
    if (l.startsWith("en")) return "en";
    if (l.startsWith("fr")) return "fr";
    if (l.startsWith("uk") || l.startsWith("ua")) return "uk";
    if (l.startsWith("it")) return "it";
    if (l.startsWith("sv") || l.startsWith("se")) return "sv";
    if (l.startsWith("tr")) return "tr";
    if (l.startsWith("ru")) return "ru";
    return "de";
  }
}
