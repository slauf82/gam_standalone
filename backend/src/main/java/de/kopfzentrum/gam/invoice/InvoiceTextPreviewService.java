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
    return preview(companyId, language, treatmentDate, recipient, "");
  }

  public InvoiceTextPreview preview(Integer companyId, String language, String treatmentDate, LbdRecipient recipient, String paymentMethod) {
    InvoiceCompany company = companyId == null ? null : repo.findCompany(companyId);
    String lang = normalize(language);
    Map<String, String> labels = labels(lang);
    Map<String, String> assigned = "de".equals(lang) ? assignedTexts(companyId) : Map.of();
    String invoiceText = "de".equals(lang)
        ? resolveText(assigned, "invoiceText", "rechnungstext", hardcodedFallback("invoiceText"))
        : translations.invoice("invoiceInvoiceTextLabel0", lang);
    String legalNote = "de".equals(lang)
        ? resolveText(assigned, "legalNote", "rechnungsrechtlicherhinweis", hardcodedFallback("legalNote"))
        : translations.invoice("invoiceLawHintLabel0", lang);
    String greeting = "de".equals(lang)
        ? resolveText(assigned, "greeting", "rechnungsgrussformel", hardcodedFallback("greeting"))
        : translations.invoice("invoiceGreetingsLabel0", lang);
    return new InvoiceTextPreview(
      lang,
      companyId,
      translations.invoice("invoice", lang),
      personalSalutation(recipient, lang),
      replace(invoiceText, lang, company, recipient, treatmentDate),
      replace(legalNote, lang, company, recipient, treatmentDate),
      replace(greeting, lang, company, recipient, treatmentDate),
      labels,
      recipient,
      treatmentDate,
      paymentMethod
    );
  }

  private String personalSalutation(LbdRecipient recipient, String language) {
    if (recipient == null || !recipient.found()) return translations.invoice("invoiceNoRecipient", language);
    String name = java.util.stream.Stream.of(recipient.title(), recipient.firstName(), recipient.nameSuffix(), recipient.lastName())
        .filter(v -> v != null && !v.isBlank()).map(String::trim).collect(java.util.stream.Collectors.joining(" "));
    String salutation = value(recipient.salutation()).toLowerCase(Locale.ROOT);
    Integer index = recipient.salutationIndex();
    boolean male = Integer.valueOf(1).equals(index) || salutation.startsWith("herr") || salutation.startsWith("mr") || salutation.startsWith("monsieur");
    boolean female = Integer.valueOf(2).equals(index) || salutation.startsWith("frau") || salutation.startsWith("mrs") || salutation.startsWith("ms") || salutation.startsWith("madame");
    return switch (normalize(language)) {
      case "en" -> male ? "Dear Mr " + name + "," : female ? "Dear Ms " + name + "," : "Hello " + name + ",";
      case "fr" -> male ? "Monsieur " + name + "," : female ? "Madame " + name + "," : "Bonjour " + name + ",";
      case "uk" -> male ? "Шановний пане " + name + "," : female ? "Шановна пані " + name + "," : "Добрий день, " + name + ",";
      case "it" -> male ? "Gentile Signor " + name + "," : female ? "Gentile Signora " + name + "," : "Gentile " + name + ",";
      case "sv" -> male ? "Bäste Herr " + name + "," : female ? "Bästa Fru " + name + "," : "Hej " + name + ",";
      case "tr" -> male ? "Sayın Bay " + name + "," : female ? "Sayın Bayan " + name + "," : "Sayın " + name + ",";
      case "ru" -> male ? "Уважаемый господин " + name + "," : female ? "Уважаемая госпожа " + name + "," : "Здравствуйте, " + name + ",";
      case "es" -> male ? "Estimado Señor " + name + "," : female ? "Estimada Señora " + name + "," : "Estimado/a " + name + ",";
      case "pt" -> male ? "Prezado Senhor " + name + "," : female ? "Prezada Senhora " + name + "," : "Prezado(a) " + name + ",";
      case "nl" -> male ? "Geachte heer " + name + "," : female ? "Geachte mevrouw " + name + "," : "Geachte " + name + ",";
      case "pl" -> male ? "Szanowny Panie " + name + "," : female ? "Szanowna Pani " + name + "," : "Dzień dobry " + name + ",";
      case "cs" -> male ? "Vážený pane " + name + "," : female ? "Vážená paní " + name + "," : "Dobrý den " + name + ",";
      default -> male ? "Sehr geehrter Herr " + name + "," : female ? "Sehr geehrte Frau " + name + "," : "Guten Tag " + name + ",";
    };
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
    result = result.replace("<Anrede>", localizedSalutation(recipient == null ? null : recipient.salutation(), language));
    result = result.replace("<Titel>", value(recipient == null ? null : recipient.title()));
    result = result.replace("<Vorname>", value(recipient == null ? null : recipient.firstName()));
    result = result.replace("<Namenszusatz>", value(recipient == null ? null : recipient.nameSuffix()));
    result = result.replace("<Nachname>", value(recipient == null ? null : recipient.lastName()));
    result = result.replace("<Behandlungsdatum>", formatDate(treatmentDate, language));
    result = result.replace("<Gesellschaftsname>", value(company == null ? null : company.name()));
    result = result.replace("<SieIhrKind>", translations.invoice("invoiceYou", language));
    return result.replaceAll("[ \\t]+", " ").replace(" ,", ",").trim();
  }

  private static String localizedSalutation(String salutation, String language) {
    String raw = value(salutation);
    if (raw.isBlank()) return "";
    String normalized = raw.toLowerCase(Locale.ROOT).replace(".", "").trim();
    boolean female = normalized.equals("frau") || normalized.equals("mrs") || normalized.equals("ms") || normalized.equals("madame")
        || normalized.equals("sigra") || normalized.equals("señora") || normalized.equals("senhora") || normalized.equals("mevrouw")
        || normalized.equals("pani") || normalized.equals("paní") || normalized.equals("fru") || normalized.equals("bayan")
        || normalized.equals("пані") || normalized.equals("госпожа");
    boolean male = normalized.equals("herr") || normalized.equals("herrn") || normalized.equals("mr") || normalized.equals("monsieur")
        || normalized.equals("sig") || normalized.equals("signor") || normalized.equals("señor") || normalized.equals("senhor")
        || normalized.equals("de heer") || normalized.equals("pan") || normalized.equals("bay") || normalized.equals("пан")
        || normalized.equals("господин");
    return switch (normalize(language)) {
      case "en" -> female ? "Ms" : male ? "Mr" : raw;
      case "fr" -> female ? "Madame" : male ? "Monsieur" : raw;
      case "uk" -> female ? "Пані" : male ? "Пан" : raw;
      case "it" -> female ? "Signora" : male ? "Signor" : raw;
      case "sv" -> female ? "Fru" : male ? "Herr" : raw;
      case "tr" -> female ? "Bayan" : male ? "Bay" : raw;
      case "ru" -> female ? "Госпожа" : male ? "Господин" : raw;
      case "es" -> female ? "Señora" : male ? "Señor" : raw;
      case "pt" -> female ? "Senhora" : male ? "Senhor" : raw;
      case "nl" -> female ? "Mevrouw" : male ? "De heer" : raw;
      case "pl" -> female ? "Pani" : male ? "Pan" : raw;
      case "cs" -> female ? "Paní" : male ? "Pan" : raw;
      default -> female ? "Frau" : male ? "Herr" : raw;
    };
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
    if (l.startsWith("es")) return "es";
    if (l.startsWith("pt")) return "pt";
    if (l.startsWith("nl")) return "nl";
    if (l.startsWith("pl")) return "pl";
    if (l.startsWith("cs") || l.startsWith("cz")) return "cs";
    return "de";
  }
}
