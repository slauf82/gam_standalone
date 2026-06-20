package de.kopfzentrum.gam.invoice;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/invoice-portal")
public class InvoicePortalController {
  private final InvoiceAccessTokenRepository tokens;
  private final InvoicePdfService pdfService;
  private final TranslationService translations;
  private final QrCodeService qrCodeService;

  public InvoicePortalController(InvoiceAccessTokenRepository tokens, InvoicePdfService pdfService, TranslationService translations, QrCodeService qrCodeService) {
    this.tokens = tokens; this.pdfService = pdfService; this.translations = translations; this.qrCodeService = qrCodeService;
  }

  @GetMapping(value="/{token}", produces=MediaType.TEXT_HTML_VALUE)
  public String portal(@PathVariable String token,
                       @RequestParam(defaultValue="de") String lang,
                       @RequestParam(required=false) String pdfLang) {
    String uiLang = normalizeLang(lang);
    String invoiceLang = normalizeLang(pdfLang == null || pdfLang.isBlank() ? uiLang : pdfLang);
    InvoiceAccessToken access = tokens.findByToken(token);
    List<InvoicePortalInvoice> invoices = tokens.listPatientInvoices(access);
    StringBuilder rows = new StringBuilder();
    for (InvoicePortalInvoice inv : invoices) {
      String title = title(inv.number(), invoiceLang);
      String rowText = title + " " + (inv.number()==null?"":inv.number()) + ", " + (inv.invoiceDate()==null?"":inv.invoiceDate()) + ", " + (inv.companyName()==null?"":inv.companyName()) + ", " + (inv.totalGross()==null?"":String.format(java.util.Locale.GERMANY,"%.2f €", inv.totalGross()));
      String pdfUrl = "/api/invoice-portal/" + escape(token) + "/pdf/" + url(inv.number()) + "?lang=" + escape(invoiceLang);
      rows.append("<tr data-read='").append(escape(rowText)).append("'><td>").append(escape(title)).append(" ").append(escape(inv.number())).append("</td><td>").append(escape(inv.invoiceDate())).append("</td><td>").append(escape(inv.companyName())).append("</td><td>").append(inv.totalGross()==null?"—":String.format(java.util.Locale.GERMANY,"%.2f €", inv.totalGross())).append("</td><td><a href='").append(pdfUrl).append("' target='_blank'>PDF</a></td><td><button type='button' class='read-invoice'>").append(escape(portalLabel("readInvoice", uiLang))).append("</button></td></tr>");
    }
    String langLinks = langLinks(uiLang, invoiceLang);
    String pdfOptions = languageOptions(invoiceLang);
    return """
      <!doctype html><html lang='%s'><head><meta charset='utf-8'/><meta name='viewport' content='width=device-width,initial-scale=1'/>
      <title>%s</title><style>body{font-family:Arial,sans-serif;margin:2rem;line-height:1.4;color:#111;background:#f6f7f9}table{border-collapse:collapse;width:100%%;background:white}td,th{border-bottom:1px solid #ddd;padding:.6rem;text-align:left}.card{max-width:1000px;margin:auto;background:white;border-radius:14px;padding:1.5rem;box-shadow:0 2px 12px #0001}.lang a{margin-right:.7rem}.toolbar{display:flex;gap:.75rem;align-items:end;flex-wrap:wrap;margin:1rem 0}button,.button{border:1px solid #aaa;border-radius:8px;background:#f3f4f6;padding:.45rem .7rem;cursor:pointer}button.active{background:#fee2e2;border-color:#b91c1c}.muted{color:#444}select{padding:.45rem;border-radius:8px;border:1px solid #aaa}</style></head>
      <body><main class='card'><h1>%s</h1><p>%s</p><p class='lang'>%s</p>
      <form class='toolbar' method='get'><input type='hidden' name='lang' value='%s'/><label>%s<br/><select name='pdfLang' onchange='this.form.submit()'>%s</select></label></form>
      <table id='invoiceTable'><thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th><th>PDF</th><th>%s</th></tr></thead><tbody>%s</tbody></table>
      <p><button id='readPortal' type='button' onclick='togglePortalReading()'>%s</button></p>
      <script>
      const speechLang = '%s';
      const readLabel = %s;
      const stopLabel = %s;
      let speaking=false;
      function speakText(text, button){
        speechSynthesis.cancel();
        if(speaking){ speaking=false; resetButtons(); return; }
        speaking=true; resetButtons(); if(button){button.classList.add('active');button.textContent=stopLabel;}
        const u=new SpeechSynthesisUtterance(text); u.lang=speechLang; u.onend=()=>{speaking=false;resetButtons();}; speechSynthesis.speak(u);
      }
      function resetButtons(){document.querySelectorAll('button').forEach(b=>{if(b.id==='readPortal')b.textContent=readLabel; if(b.classList.contains('read-invoice'))b.textContent=%s; b.classList.remove('active');});}
      function togglePortalReading(){speakText(document.querySelector('main').innerText, document.getElementById('readPortal'));}
      document.querySelectorAll('.read-invoice').forEach(btn=>btn.addEventListener('click',()=>speakText(btn.closest('tr').dataset.read, btn)));
      </script>
      </main></body></html>
      """.formatted(languageTag(uiLang), escape(translations.invoice("invoicePortalTitle", uiLang)), escape(translations.invoice("invoicePortalTitle", uiLang)),
      escape(translations.invoice("invoicePortalHelp", uiLang)), langLinks, escape(uiLang), escape(portalLabel("pdfLanguage", uiLang)), pdfOptions,
      escape(translations.invoice("invoice", uiLang)), escape(translations.invoice("invoiceDate", uiLang)), escape(translations.invoice("company", uiLang)), escape(translations.invoice("gross", uiLang)), escape(portalLabel("action", uiLang)), rows,
      escape(translations.invoice("invoiceReadAloud", uiLang)), languageTag(uiLang), jsString(translations.invoice("invoiceReadAloud", uiLang)), jsString(portalLabel("stopReading", uiLang)), jsString(portalLabel("readInvoice", uiLang)));
  }



  @GetMapping(value="/{token}/qr", produces=MediaType.IMAGE_PNG_VALUE)
  public byte[] qr(@PathVariable String token, HttpServletRequest request) {
    tokens.findByToken(token);
    String url = request.getRequestURL().toString().replaceAll("/qr$", "");
    return qrCodeService.png(url, 240);
  }

  @GetMapping("/{token}/data")
  public InvoicePortalResponse data(@PathVariable String token) {
    InvoiceAccessToken access = tokens.findByToken(token);
    List<InvoicePortalInvoice> invoices = tokens.listPatientInvoices(access);
    String companyName = invoices.isEmpty() ? null : invoices.get(0).companyName();
    return new InvoicePortalResponse(token, access.invoiceNumber(), access.companyId(), companyName, access.addressId(), invoices);
  }

  @GetMapping("/{token}/pdf/{number}")
  public ResponseEntity<byte[]> pdf(@PathVariable String token, @PathVariable String number, @RequestParam(defaultValue="de") String lang) {
    InvoiceAccessToken access = tokens.findByToken(token);
    InvoicePortalInvoice selected = tokens.listPatientInvoices(access).stream()
      .filter(i -> number.equals(i.number()))
      .filter(i -> access.companyId() == null || java.util.Objects.equals(access.companyId(), i.companyId()))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Rechnung gehoert nicht zu diesem Portalzugriff"));
    byte[] bytes = pdfService.render(number, selected.companyId(), lang);
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=rechnung-"+number+"-"+lang+".pdf").contentType(MediaType.APPLICATION_PDF).body(bytes);
  }

  private String title(String number, String lang) {
    if (number == null) return translations.invoice("invoice", lang);
    if (number.endsWith("S")) return translations.invoice("cancellation", lang);
    if (number.endsWith("G")) return translations.invoice("credit", lang);
    if (number.endsWith("P")) return translations.invoice("proformaInvoice", lang);
    if (number.matches(".*Z\\d*$")) return translations.invoice("paymentAdvice", lang);
    return translations.invoice("invoice", lang);
  }

  private static String normalizeLang(String language) {
    String l = (language == null || language.isBlank() ? "de" : language).toLowerCase(java.util.Locale.ROOT);
    return switch (l) { case "en", "english" -> "en"; case "fr", "french" -> "fr"; case "uk", "ua", "ukrainian" -> "uk"; case "it", "italian" -> "it"; case "sv", "swedish" -> "sv"; case "tr", "turkish" -> "tr"; case "ru", "russian" -> "ru"; default -> "de"; };
  }
  private static String langLinks(String uiLang, String pdfLang) {
    String[][] langs = {{"de","Deutsch"},{"en","English"},{"fr","Français"},{"uk","Українська"},{"it","Italiano"},{"sv","Svenska"},{"tr","Türkçe"},{"ru","Русский"}};
    StringBuilder out = new StringBuilder();
    for (String[] l : langs) out.append("<a href='?lang=").append(l[0]).append("&pdfLang=").append(pdfLang).append("'>").append(l[1]).append("</a>");
    return out.toString();
  }
  private static String languageOptions(String selected) {
    String[][] langs = {{"de","Deutsch"},{"en","English"},{"fr","Français"},{"uk","Українська"},{"it","Italiano"},{"sv","Svenska"},{"tr","Türkçe"},{"ru","Русский"}};
    StringBuilder out = new StringBuilder();
    for (String[] l : langs) out.append("<option value='").append(l[0]).append("'").append(l[0].equals(selected)?" selected":"").append(">").append(l[1]).append("</option>");
    return out.toString();
  }
  private static String portalLabel(String key, String lang) {
    String l = normalizeLang(lang);
    return switch (key + ":" + l) {
      case "pdfLanguage:en" -> "Invoice/PDF language"; case "pdfLanguage:fr" -> "Langue de la facture/PDF"; case "pdfLanguage:uk" -> "Мова рахунку/PDF"; case "pdfLanguage:it" -> "Lingua fattura/PDF"; case "pdfLanguage:sv" -> "Faktura-/PDF-språk"; case "pdfLanguage:tr" -> "Fatura/PDF dili"; case "pdfLanguage:ru" -> "Язык счета/PDF";
      case "stopReading:en" -> "Stop reading"; case "stopReading:fr" -> "Arrêter la lecture"; case "stopReading:uk" -> "Зупинити читання"; case "stopReading:it" -> "Ferma lettura"; case "stopReading:sv" -> "Stoppa uppläsning"; case "stopReading:tr" -> "Okumayı durdur"; case "stopReading:ru" -> "Остановить чтение";
      case "readInvoice:en" -> "Read invoice"; case "readInvoice:fr" -> "Lire cette facture"; case "readInvoice:uk" -> "Зачитати цей рахунок"; case "readInvoice:it" -> "Leggi questa fattura"; case "readInvoice:sv" -> "Läs denna faktura"; case "readInvoice:tr" -> "Bu faturayı oku"; case "readInvoice:ru" -> "Прочитать этот счет";
      case "action:en" -> "Action"; case "action:fr" -> "Action"; case "action:uk" -> "Дія"; case "action:it" -> "Azione"; case "action:sv" -> "Åtgärd"; case "action:tr" -> "İşlem"; case "action:ru" -> "Действие";
      default -> switch (key) { case "pdfLanguage" -> "Rechnungs-/PDF-Sprache"; case "stopReading" -> "Vorlesen stoppen"; case "readInvoice" -> "Rechnung vorlesen"; case "action" -> "Aktion"; default -> key; };
    };
  }
  private static String jsString(Object value) { return "'" + escapeJs(value == null ? "" : String.valueOf(value)) + "'"; }
  private static String escapeJs(String s) { return s.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "\\r").replace("\n", "\\n"); }

  private static String escape(Object o){return o==null?"":String.valueOf(o).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("'","&#39;").replace("\"","&quot;");}
  private static String url(String s){return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);}
  private static String languageTag(String language) { return switch (normalizeLang(language)) { case "en" -> "en-US"; case "fr" -> "fr-FR"; case "uk" -> "uk-UA"; case "it" -> "it-IT"; case "sv" -> "sv-SE"; case "tr" -> "tr-TR"; case "ru" -> "ru-RU"; default -> "de-DE"; }; }
}
