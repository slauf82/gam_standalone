package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class InvoiceTextPreviewService {
  private final InvoiceRepository repo;
  private final TranslationService translations;

  public InvoiceTextPreviewService(InvoiceRepository repo, TranslationService translations) {
    this.repo = repo;
    this.translations = translations;
  }

  public InvoiceTextPreview preview(Integer companyId, String language, String treatmentDate, LbdRecipient recipient) {
    InvoiceCompany company = companyId == null ? null : repo.findCompany(companyId);
    String lang = normalize(language);
    Map<String, String> labels = labels(lang);
    return new InvoiceTextPreview(
      lang,
      companyId,
      translations.invoice("invoice", lang),
      replace(translations.invoice("invoiceSalutationLabel0", lang), lang, company, recipient, treatmentDate),
      replace(translations.invoice("invoiceInvoiceTextLabel0", lang), lang, company, recipient, treatmentDate),
      replace(translations.invoice("invoiceLawHintLabel0", lang), lang, company, recipient, treatmentDate),
      replace(translations.invoice("invoiceGreetingsLabel0", lang), lang, company, recipient, treatmentDate),
      labels
    );
  }

  private Map<String, String> labels(String language) {
    Map<String, String> map = new LinkedHashMap<>();
    String[] keys = {
      "invoice", "proformaInvoice", "credit", "cancellation", "paymentAdvice",
      "invoiceRecipient", "invoiceNoRecipient", "invoiceDate", "invoiceCustomerFile", "invoiceUser", "invoicePaymentMethod",
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
    result = result.replace("<Behandlungsdatum>", value(treatmentDate));
    result = result.replace("<Gesellschaftsname>", value(company == null ? null : company.name()));
    result = result.replace("<SieIhrKind>", translations.invoice("invoiceYou", language));
    return result.replaceAll("[ \\t]+", " ").replace(" ,", ",").trim();
  }

  private static String value(String value) {
    return value == null ? "" : value.trim();
  }

  private static String normalize(String language) {
    if (language == null || language.isBlank()) return "de";
    String l = language.trim().toLowerCase();
    if (l.startsWith("en")) return "en";
    if (l.startsWith("fr")) return "fr";
    if (l.startsWith("uk") || l.startsWith("ua")) return "uk";
    return "de";
  }
}
