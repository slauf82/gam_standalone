package de.kopfzentrum.gam.invoice;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import de.kopfzentrum.gam.invoice.zugferd.ZugferdExportResult;
import de.kopfzentrum.gam.invoice.zugferd.ZugferdExportService;

import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@RestController
@RequestMapping("/api/invoice-portal")
public class InvoicePortalController {
  private final InvoiceAccessTokenRepository tokens;
  private final ZugferdExportService zugferdService;
  private final TranslationService translations;
  private final QrCodeService qrCodeService;
  private final PaymentWorkflowRepository payments;
  private final PaymentDocumentService paymentDocuments;

  public InvoicePortalController(InvoiceAccessTokenRepository tokens, ZugferdExportService zugferdService, TranslationService translations, QrCodeService qrCodeService, PaymentWorkflowRepository payments, PaymentDocumentService paymentDocuments) {
    this.tokens = tokens; this.zugferdService = zugferdService; this.translations = translations; this.qrCodeService = qrCodeService; this.payments = payments; this.paymentDocuments = paymentDocuments;
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
      String title = title(inv, uiLang);
      String invoiceStatus = cancelledStatus(inv, uiLang);
      String rowText = title + " " + (inv.number()==null?"":inv.number()) + (invoiceStatus.isBlank()?"":", "+invoiceStatus) + ", " + (inv.invoiceDate()==null?"":formatPortalDate(parsePortalDate(inv.invoiceDate()), uiLang)) + ", " + (inv.companyName()==null?"":inv.companyName()) + ", " + (inv.totalGross()==null?"":String.format(java.util.Locale.GERMANY,"%.2f €", inv.totalGross()));
      String pdfUrl = "/api/invoice-portal/" + escape(token) + "/pdf/" + url(inv.number()) + "?lang=" + escape(invoiceLang);
      rows.append("<tr data-read='").append(escape(rowText)).append("' data-read-url='/api/invoice-portal/").append(escape(token)).append("/read/pdf/").append(url(inv.number())).append("?lang=").append(escape(invoiceLang)).append("'><td>").append(escape(title)).append(" ").append(escape(inv.number())).append(invoiceStatus.isBlank()?"":"<br/><small><b>"+escape(invoiceStatus)+"</b></small>").append("</td><td>").append(escape(formatPortalDate(parsePortalDate(inv.invoiceDate()), uiLang))).append("</td><td>").append(escape(inv.companyName())).append("</td><td>").append(inv.totalGross()==null?"—":String.format(java.util.Locale.GERMANY,"%.2f €", inv.totalGross())).append("</td><td><a href='").append(pdfUrl).append("' target='_blank'>PDF</a></td><td><button type='button' class='read-invoice'>").append(escape(portalLabel("readInvoice", uiLang))).append("</button></td></tr>");
      for (PaymentDocument doc : payments.documents(inv.number(), inv.companyId())) {
        String localizedDocumentTitle = PaymentDocumentService.localizedTitle(doc.documentType(), uiLang);
        String relation = paymentDocumentRelation(uiLang);
        String docText = localizedDocumentTitle + " " + relation + " " + inv.number() + ", " + (doc.createdAt()==null?"":formatPortalDate(doc.createdAt().toLocalDate(), uiLang));
        String docUrl = "/api/invoice-portal/" + escape(token) + "/payment-document/" + doc.id() + "?lang=" + escape(invoiceLang);
        rows.append("<tr data-read='").append(escape(docText)).append("' data-read-url='/api/invoice-portal/").append(escape(token)).append("/read/payment-document/").append(doc.id()).append("?lang=").append(escape(invoiceLang)).append("'><td>").append(escape(localizedDocumentTitle)).append(" ").append(escape(relation)).append(" ").append(escape(inv.number())).append("</td><td>").append(escape(formatPortalDate(doc.createdAt()==null?null:doc.createdAt().toLocalDate(), uiLang))).append("</td><td>").append(escape(inv.companyName())).append("</td><td>—</td><td><a href='").append(docUrl).append("' target='_blank'>PDF</a></td><td><button type='button' class='read-invoice'>").append(escape(portalLabel("readInvoice", uiLang))).append("</button></td></tr>");
      }
    }
    String langLinks = langLinks(uiLang, invoiceLang);
    String pdfOptions = languageOptions(invoiceLang);
    return """
      <!doctype html><html lang='%s'><head><meta charset='utf-8'/><meta name='viewport' content='width=device-width,initial-scale=1'/>
      <title>%s</title><style>body{font-family:Arial,sans-serif;margin:2rem;line-height:1.4;color:#111;background:#f6f7f9}table{border-collapse:collapse;width:100%%;background:white}td,th{border-bottom:1px solid #ddd;padding:.6rem;text-align:left}.card{max-width:1000px;margin:auto;background:white;border-radius:14px;padding:1.5rem;box-shadow:0 2px 12px #0001}.lang a{margin-right:.7rem}.toolbar{display:flex;gap:.75rem;align-items:end;flex-wrap:wrap;margin:1rem 0}button,.button{border:1px solid #aaa;border-radius:8px;background:#f3f4f6;padding:.45rem .7rem;cursor:pointer}button.active{background:#fee2e2;border-color:#b91c1c}.muted{color:#444}select{padding:.45rem;border-radius:8px;border:1px solid #aaa}</style></head>
      <body><main class='card'><h1>%s</h1><p>%s</p><p class='lang'>%s</p>
      <form class='toolbar' method='get'><input type='hidden' name='lang' value='%s'/><label>%s<br/><select name='pdfLang' onchange='this.form.submit()'>%s</select></label><label>%s<br/><select id='ttsEngine'><option value='piper'>Piper TTS</option><option value='browser'>%s</option></select></label></form>
      <table id='invoiceTable'><thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th><th>PDF</th><th>%s</th></tr></thead><tbody>%s</tbody></table>
      <p><button id='readPortal' type='button' onclick='togglePortalReading()'>%s</button></p>
      <script>
      const portalLanguage = '%s';
      const portalSpeechLang = '%s';
      const selectedPdfLanguage = '%s';
      const pdfSpeechLang = '%s';
      const readLabel = %s;
      const stopLabel = %s;
      let speaking=false;
      let activeAudio=null;
      const engineSelect=document.getElementById('ttsEngine');
      const savedEngine=localStorage.getItem('gam.portal.tts.engine')||'piper';
      engineSelect.value=(savedEngine==='browser'?'browser':'piper');
      engineSelect.addEventListener('change',()=>{localStorage.setItem('gam.portal.tts.engine',engineSelect.value); if(speaking) stopReadingNow();});
      function selectedReadingEngine(){return engineSelect && engineSelect.value==='browser'?'browser':'piper';}
      function stopReadingNow(){
        speechSynthesis.cancel();
        if(activeAudio){try{activeAudio.pause();activeAudio.src='';}catch(e){} activeAudio=null;}
        speaking=false; resetButtons();
      }
      async function speakText(text, button, contentLanguage, browserLanguage){
        if(speaking){stopReadingNow();return;}
        stopReadingNow();
        speaking=true; resetButtons(); if(button){button.classList.add('active');button.textContent=stopLabel;}
        const readingEngine=selectedReadingEngine();
        if(readingEngine==='piper'){
          try{
            const response=await fetch('/api/tts/audio',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({language:contentLanguage,text:text,engine:'piper'})});
            if(!response.ok){
              const reason=response.headers.get('X-GAM-TTS-Error')||('HTTP '+response.status);
              throw new Error(reason);
            }
            const blob=await response.blob();
            const objectUrl=URL.createObjectURL(blob);
            const audio=new Audio(objectUrl); activeAudio=audio;
            const done=()=>{URL.revokeObjectURL(objectUrl);activeAudio=null;speaking=false;resetButtons();};
            audio.onended=done; audio.onerror=()=>{done();window.alert('Piper TTS konnte die Audiodatei nicht wiedergeben.');};
            await audio.play(); return;
          }catch(e){
            activeAudio=null; speaking=false; resetButtons();
            const msg=(e&&e.message?e.message:String(e)); if(msg.includes('HTTP 403')) window.alert('Piper TTS: Zugriff auf den TTS-Dienst verweigert (HTTP 403).'); else if(msg.includes('unsupported-language')||msg.includes('piper-voice-missing')) window.alert('Für die gewählte Sprache ist keine Piper-Stimme konfiguriert.'); else if(msg.includes('piper-voice-download-started')) window.alert('Die Piper-Stimme wird installiert. Bitte versuchen Sie es anschließend erneut.'); else window.alert('Piper TTS konnte nicht gestartet werden: '+msg);
            return;
          }
        }
        const u=new SpeechSynthesisUtterance(text); u.lang=browserLanguage; u.onend=()=>{speaking=false;resetButtons();}; u.onerror=()=>{speaking=false;resetButtons();}; speechSynthesis.speak(u);
      }
      function resetButtons(){document.querySelectorAll('button').forEach(b=>{if(b.id==='readPortal')b.textContent=readLabel; if(b.classList.contains('read-invoice'))b.textContent=%s; b.classList.remove('active');});}
      function togglePortalReading(){speakText(document.querySelector('main').innerText, document.getElementById('readPortal'), portalLanguage, portalSpeechLang);}
      document.querySelectorAll('.read-invoice').forEach(btn=>btn.addEventListener('click',async()=>{const row=btn.closest('tr');let text=row.dataset.read||'';const readUrl=row.dataset.readUrl;if(readUrl){try{const r=await fetch(readUrl);if(r.ok)text=await r.text();}catch(e){}}speakText(text,btn,selectedPdfLanguage,pdfSpeechLang);}));
      </script>
      </main></body></html>
      """.formatted(languageTag(uiLang), escape(translations.invoice("invoicePortalTitle", uiLang)), escape(translations.invoice("invoicePortalTitle", uiLang)),
      escape(translations.invoice("invoicePortalHelp", uiLang)), langLinks, escape(uiLang), escape(portalLabel("pdfLanguage", uiLang)), pdfOptions, escape(portalLabel("readingEngine", uiLang)), escape(portalLabel("browserTts", uiLang)),
      escape(translations.invoice("invoice", uiLang)), escape(translations.invoice("invoiceDate", uiLang)), escape(translations.invoice("company", uiLang)), escape(translations.invoice("gross", uiLang)), escape(portalLabel("action", uiLang)), rows,
      escape(translations.invoice("invoiceReadAloud", uiLang)), escape(uiLang), languageTag(uiLang), escape(invoiceLang), languageTag(invoiceLang), jsString(translations.invoice("invoiceReadAloud", uiLang)), jsString(portalLabel("stopReading", uiLang)), jsString(portalLabel("readInvoice", uiLang)));
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
    ZugferdExportResult result = zugferdService.export(number, selected.companyId(), lang);
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + result.filename()).contentType(MediaType.APPLICATION_PDF).body(result.pdfBytes());
  }


  @GetMapping(value="/{token}/read/pdf/{number}", produces=MediaType.TEXT_PLAIN_VALUE)
  public String readPdf(@PathVariable String token, @PathVariable String number, @RequestParam(defaultValue="de") String lang) throws Exception {
    InvoiceAccessToken access = tokens.findByToken(token);
    InvoicePortalInvoice selected = tokens.listPatientInvoices(access).stream()
      .filter(i -> number.equals(i.number()))
      .filter(i -> access.companyId() == null || java.util.Objects.equals(access.companyId(), i.companyId()))
      .findFirst().orElseThrow(() -> new IllegalArgumentException("Rechnung gehoert nicht zu diesem Portalzugriff"));
    byte[] pdf = zugferdService.export(number, selected.companyId(), lang).pdfBytes();
    return extractPdfText(pdf);
  }

  @GetMapping(value="/{token}/read/payment-document/{id}", produces=MediaType.TEXT_PLAIN_VALUE)
  public String readPaymentDocument(@PathVariable String token, @PathVariable long id,
                                    @RequestParam(required=false) String lang) throws Exception {
    InvoiceAccessToken access = tokens.findByToken(token);
    PaymentDocument doc = payments.document(id);
    boolean allowed = tokens.listPatientInvoices(access).stream()
      .anyMatch(i -> java.util.Objects.equals(i.number(), doc.invoiceNumber()) && java.util.Objects.equals(i.companyId(), doc.companyId()));
    if (!allowed) throw new IllegalArgumentException("Dokument gehoert nicht zu diesem Portalzugriff");
    PaymentWorkflowState current = payments.getOrCreate(doc.invoiceNumber(), doc.companyId(), null, null, "portal");
    String documentLanguage = normalizeLang(lang == null || lang.isBlank() ? doc.language() : lang);
    byte[] pdf = paymentDocuments.render(doc.invoiceNumber(), doc.companyId(), doc.documentType(), documentLanguage, current.openAmount(), current.dueDate());
    payments.updateDocumentPdf(id, pdf);
    return extractPdfText(pdf);
  }

  private static String extractPdfText(byte[] pdf) throws Exception {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return new PDFTextStripper().getText(document).replaceAll("[\t ]+", " ").replaceAll("\n{3,}", "\n\n").trim();
    }
  }

  @GetMapping("/{token}/payment-document/{id}")
  public ResponseEntity<byte[]> paymentDocument(@PathVariable String token, @PathVariable long id,
                                                @RequestParam(required=false) String lang) {
    InvoiceAccessToken access = tokens.findByToken(token);
    PaymentDocument doc = payments.document(id);
    boolean allowed = tokens.listPatientInvoices(access).stream()
      .anyMatch(i -> java.util.Objects.equals(i.number(), doc.invoiceNumber()) && java.util.Objects.equals(i.companyId(), doc.companyId()));
    if (!allowed) throw new IllegalArgumentException("Dokument gehoert nicht zu diesem Portalzugriff");
    PaymentWorkflowState current = payments.getOrCreate(doc.invoiceNumber(), doc.companyId(), null, null, "portal");
    String documentLanguage = normalizeLang(lang == null || lang.isBlank() ? doc.language() : lang);
    byte[] pdf = paymentDocuments.render(doc.invoiceNumber(), doc.companyId(), doc.documentType(), documentLanguage, current.openAmount(), current.dueDate());
    payments.updateDocumentPdf(id, pdf);
    String filenameTitle = PaymentDocumentService.localizedTitle(doc.documentType(), documentLanguage);
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filenameTitle.replace(' ','_') + ".pdf\"")
      .contentType(MediaType.APPLICATION_PDF).body(pdf);
  }

  private static String paymentDocumentRelation(String lang) {
    return switch (normalizeLang(lang)) {
      case "en" -> "for invoice"; case "fr" -> "pour la facture"; case "uk" -> "до рахунку";
      case "it" -> "per la fattura"; case "sv" -> "för faktura"; case "tr" -> "numaralı fatura için";
      case "ru" -> "по счёту"; case "es" -> "de la factura"; case "pt" -> "da fatura";
      case "nl" -> "voor factuur"; case "pl" -> "do faktury"; case "cs" -> "k faktuře";
      default -> "zu";
    };
  }

  private String title(InvoicePortalInvoice invoice, String lang) {
    String number = invoice == null ? null : invoice.number();
    if (number == null) return translations.invoice("invoice", lang);
    if (number.endsWith("S")) return translations.invoice("cancellation", lang);
    if (number.endsWith("G") || Boolean.TRUE.equals(invoice.creditNote())) return translations.invoice("credit", lang);
    if (number.endsWith("P")) return translations.invoice("proformaInvoice", lang);
    if (number.matches(".*Z\\d*$") || Boolean.TRUE.equals(invoice.paymentAdvice())) return translations.invoice("paymentAdvice", lang);
    return translations.invoice("invoice", lang);
  }

  private static String cancelledStatus(InvoicePortalInvoice invoice, String lang) {
    if (invoice == null || !Boolean.TRUE.equals(invoice.cancelled()) || (invoice.number()!=null && invoice.number().endsWith("S"))) return "";
    return switch (normalizeLang(lang)) {
      case "en" -> "already cancelled"; case "fr" -> "déjà annulée"; case "it" -> "già stornata";
      case "sv" -> "redan makulerad"; case "tr" -> "zaten iptal edildi"; case "ru" -> "уже сторнирован"; case "uk" -> "вже сторновано";
      default -> "bereits storniert";
    };
  }


  private static java.time.LocalDate parsePortalDate(String value) {
    if (value == null || value.isBlank()) return null;
    String v = value.trim();
    try { return java.time.LocalDate.parse(v); } catch (Exception ignored) {}
    for (String pattern : new String[]{"dd.MM.yyyy", "MM/dd/yyyy", "dd/MM/yyyy"}) {
      try { return java.time.LocalDate.parse(v, java.time.format.DateTimeFormatter.ofPattern(pattern)); } catch (Exception ignored) {}
    }
    return null;
  }

  private static String formatPortalDate(java.time.LocalDate date, String lang) {
    if (date == null) return "";
    String l = normalizeLang(lang);
    return switch (l) {
      case "en" -> date.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
      case "fr", "it", "es", "pt", "nl", "pl", "cs" -> date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      case "sv" -> date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
      default -> date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    };
  }

  private static String normalizeLang(String language) {
    String l = (language == null || language.isBlank() ? "de" : language).toLowerCase(java.util.Locale.ROOT);
    return switch (l) { case "en", "english" -> "en"; case "fr", "french" -> "fr"; case "uk", "ua", "ukrainian" -> "uk"; case "it", "italian" -> "it"; case "sv", "swedish" -> "sv"; case "tr", "turkish" -> "tr"; case "ru", "russian" -> "ru"; case "es", "spanish" -> "es"; case "pt", "portuguese" -> "pt"; case "nl", "dutch" -> "nl"; case "pl", "polish" -> "pl"; case "cs", "czech" -> "cs"; default -> "de"; };
  }
  private static String langLinks(String uiLang, String pdfLang) {
    String[][] langs = {{"de","Deutsch"},{"en","English"},{"fr","Français"},{"uk","Українська"},{"it","Italiano"},{"sv","Svenska"},{"tr","Türkçe"},{"ru","Русский"},{"es","Español"},{"pt","Português"},{"nl","Nederlands"},{"pl","Polski"},{"cs","Čeština"}};
    StringBuilder out = new StringBuilder();
    for (String[] l : langs) out.append("<a href='?lang=").append(l[0]).append("&pdfLang=").append(pdfLang).append("'>").append(l[1]).append("</a>");
    return out.toString();
  }
  private static String languageOptions(String selected) {
    String[][] langs = {{"de","Deutsch"},{"en","English"},{"fr","Français"},{"uk","Українська"},{"it","Italiano"},{"sv","Svenska"},{"tr","Türkçe"},{"ru","Русский"},{"es","Español"},{"pt","Português"},{"nl","Nederlands"},{"pl","Polski"},{"cs","Čeština"}};
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
      case "readingEngine:en" -> "Reading engine"; case "readingEngine:fr" -> "Technique de lecture"; case "readingEngine:uk" -> "Технологія озвучення"; case "browserTts:en" -> "Browser/Windows"; case "browserTts:fr" -> "Navigateur/Windows"; case "browserTts:uk" -> "Браузер/Windows";
      case "action:en" -> "Action"; case "action:fr" -> "Action"; case "action:uk" -> "Дія"; case "action:it" -> "Azione"; case "action:sv" -> "Åtgärd"; case "action:tr" -> "İşlem"; case "action:ru" -> "Действие";
      default -> switch (key) { case "pdfLanguage" -> "Rechnungs-/PDF-Sprache"; case "stopReading" -> "Vorlesen stoppen"; case "readInvoice" -> "Rechnung vorlesen"; case "readingEngine" -> "Vorlesetechnik"; case "browserTts" -> "Browser/Windows"; case "action" -> "Aktion"; default -> key; };
    };
  }
  private static String jsString(Object value) { return "'" + escapeJs(value == null ? "" : String.valueOf(value)) + "'"; }
  private static String escapeJs(String s) { return s.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "\\r").replace("\n", "\\n"); }

  private static String escape(Object o){return o==null?"":String.valueOf(o).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("'","&#39;").replace("\"","&quot;");}
  private static String url(String s){return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);}
  private static String languageTag(String language) { return switch (normalizeLang(language)) { case "en" -> "en-US"; case "fr" -> "fr-FR"; case "uk" -> "uk-UA"; case "it" -> "it-IT"; case "sv" -> "sv-SE"; case "tr" -> "tr-TR"; case "ru" -> "ru-RU"; case "es" -> "es-ES"; case "pt" -> "pt-PT"; case "nl" -> "nl-NL"; case "pl" -> "pl-PL"; case "cs" -> "cs-CZ"; default -> "de-DE"; }; }
}
