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
  public String portal(@PathVariable String token, @RequestParam(defaultValue="de") String lang) {
    InvoiceAccessToken access = tokens.findByToken(token);
    List<InvoicePortalInvoice> invoices = tokens.listPatientInvoices(access);
    StringBuilder rows = new StringBuilder();
    for (InvoicePortalInvoice inv : invoices) {
      String title = title(inv.number(), lang);
      rows.append("<tr><td>").append(escape(title)).append(" ").append(escape(inv.number())).append("</td><td>").append(escape(inv.invoiceDate())).append("</td><td>").append(escape(inv.companyName())).append("</td><td>").append(inv.totalGross()==null?"—":String.format(java.util.Locale.GERMANY,"%.2f €", inv.totalGross())).append("</td><td><a href='/api/invoice-portal/").append(escape(token)).append("/pdf/").append(url(inv.number())).append("?lang=").append(escape(lang)).append("'>PDF</a></td></tr>");
    }
    return """
      <!doctype html><html lang='%s'><head><meta charset='utf-8'/><meta name='viewport' content='width=device-width,initial-scale=1'/>
      <title>%s</title><style>body{font-family:Arial,sans-serif;margin:2rem;line-height:1.4}table{border-collapse:collapse;width:100%%}td,th{border-bottom:1px solid #ddd;padding:.6rem;text-align:left}.card{max-width:900px;margin:auto}.lang a{margin-right:.7rem}</style></head>
      <body><main class='card'><h1>%s</h1><p>%s</p><p class='lang'><a href='?lang=de'>Deutsch</a><a href='?lang=en'>English</a><a href='?lang=fr'>Français</a><a href='?lang=uk'>Українська</a></p>
      <table><thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th><th>PDF</th></tr></thead><tbody>%s</tbody></table>
      <p><button onclick='readPage()'>%s</button> <button onclick='speechSynthesis.cancel()'>Stop</button></p>
      <script>function readPage(){speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(document.body.innerText);u.lang='%s';speechSynthesis.speak(u);}</script>
      </main></body></html>
      """.formatted(languageTag(lang), escape(translations.invoice("invoicePortalTitle", lang)), escape(translations.invoice("invoicePortalTitle", lang)),
      escape(translations.invoice("invoicePortalHelp", lang)), escape(translations.invoice("invoice", lang)), escape(translations.invoice("invoiceDate", lang)), escape(translations.invoice("company", lang)), escape(translations.invoice("gross", lang)), rows, escape(translations.invoice("invoiceReadAloud", lang)), languageTag(lang));
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
    boolean allowed = tokens.listPatientInvoices(access).stream().anyMatch(i -> number.equals(i.number()));
    if (!allowed) throw new IllegalArgumentException("Rechnung gehoert nicht zu diesem Portalzugriff");
    byte[] bytes = pdfService.render(number, lang);
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
  private static String escape(Object o){return o==null?"":String.valueOf(o).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("'","&#39;").replace("\"","&quot;");}
  private static String url(String s){return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);}
  private static String languageTag(String language) { return switch ((language==null?"de":language).toLowerCase(java.util.Locale.ROOT)) { case "en", "english" -> "en-US"; case "fr", "french" -> "fr-FR"; case "uk", "ukrainian" -> "uk-UA"; default -> "de-DE"; }; }
}
