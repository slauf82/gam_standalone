package de.kopfzentrum.gam.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UiTranslationService {
  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String libreTranslateUrl;
  private final String libreTranslateFallbackUrl;
  private final String libreTranslateApiKey;
  private final boolean libreTranslateEnabled;
  private final boolean libreTranslateJavaClientEnabled;
  private final boolean diagnostics;
  private final int maxSynchronousAutoTranslations;
  private final int maxBackgroundAutoTranslations;
  private static final Set<String> BACKGROUND_RECONCILE_LANGUAGES = ConcurrentHashMap.newKeySet();

  public UiTranslationService(
      JdbcTemplate jdbc,
      ObjectMapper objectMapper,
      @Value("${app.translation.libretranslate.url:http://localhost:5000/translate}") String libreTranslateUrl,
      @Value("${app.translation.libretranslate.fallback-url:https://translate.fedilab.app/translate}") String libreTranslateFallbackUrl,
      @Value("${app.translation.libretranslate.api-key:}") String libreTranslateApiKey,
      @Value("${app.translation.libretranslate.enabled:true}") boolean libreTranslateEnabled,
      @Value("${app.translation.libretranslate.java-client-enabled:true}") boolean libreTranslateJavaClientEnabled,
      @Value("${app.translation.libretranslate.diagnostics:false}") boolean diagnostics,
      @Value("${app.translation.libretranslate.max-sync-auto-translations:3}") int maxSynchronousAutoTranslations,
      @Value("${app.translation.libretranslate.max-background-auto-translations:120}") int maxBackgroundAutoTranslations) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    this.libreTranslateUrl = libreTranslateUrl;
    this.libreTranslateFallbackUrl = libreTranslateFallbackUrl;
    this.libreTranslateApiKey = libreTranslateApiKey == null ? "" : libreTranslateApiKey;
    this.libreTranslateEnabled = libreTranslateEnabled;
    this.libreTranslateJavaClientEnabled = libreTranslateJavaClientEnabled;
    this.diagnostics = diagnostics;
    this.maxSynchronousAutoTranslations = Math.max(0, maxSynchronousAutoTranslations);
    this.maxBackgroundAutoTranslations = Math.max(0, maxBackgroundAutoTranslations);
  }

  public synchronized Map<String, String> translateUi(String language, Map<String, String> germanEntries, Map<String, String> knownTranslations) {
    String lang = normalize(language);
    Map<String, String> result = new LinkedHashMap<>();

    // Schritt 34n:
    // Die Alt-GAM-Übersetzungstabellen arbeiten mit Keys im Format
    // GERMAN.key / ENGLISH.key / FRENCH.key / UKRAINIAN.key.
    // Deshalb wird der vorhandene translation_german-Bestand als zusätzliche
    // Quelle geladen und nicht mehr ein neues ".UI."-Namensschema erfunden.
    // Schritt 34t:
    // translation_german ist jetzt der harte Referenzbestand. Die neuen Sprachen
    // sollen nicht nur ähnliche Keys bekommen, sondern dieselbe Key-/ID-Struktur
    // wie GERMAN/ENGLISH/FRENCH/UKRAINIAN. Deshalb werden zuerst die vorhandenen
    // deutschen DB-Zeilen mit ID geladen. Nur wenn diese Tabelle leer ist, greifen
    // die eingebauten Defaults als Fallback.
    List<TranslationRow> germanRows = loadGermanSourceRowsFromDatabase();

    Map<String, String> sourceEntries = new LinkedHashMap<>();
    for (TranslationRow row : germanRows) sourceEntries.put(row.key(), row.text());

    // Schritt 34u:
    // translation_german ist die Referenz, aber Login-/Modul-/Rechnungs-UI kann
    // zusätzliche deutsche Keys aus dem Frontend liefern. Diese dürfen nicht nur
    // live angezeigt werden, sondern müssen ebenfalls persistiert werden.
    // Deshalb wird der Quellbestand immer aus DB + eingebauten Defaults +
    // Frontend-Einträgen vereinigt. Für DB-Referenzeinträge bleiben die IDs
    // erhalten; zusätzliche Keys bekommen stabile synthetische IDs oberhalb
    // des aktuellen translation_german-Maximums.
    Map<String, String> defaultEntries = defaultGermanUiEntries();
    for (Map.Entry<String, String> entry : defaultEntries.entrySet()) {
      sourceEntries.putIfAbsent(safeKey(entry.getKey()), entry.getValue());
    }
    if (germanEntries != null) {
      for (Map.Entry<String, String> entry : germanEntries.entrySet()) {
        sourceEntries.putIfAbsent(safeKey(entry.getKey()), entry.getValue());
      }
    }

    // Schritt 34z7e:
    // Der deutsche Master-Katalog muss die inzwischen deutlich gewachsene UI-Keymenge
    // dauerhaft kennen. Sonst bleiben neue Keys wie invoicePortalTitle/invoicePortalQrHint
    // nur live sichtbar, landen aber nicht in translation_* und erscheinen beim ersten Rendern
    // kurz als technische Keys. Produktbeschreibungen bleiben davon ausgenommen, weil sie
    // ausschließlich über /ui-translations/live laufen.
    ensureGermanMasterEntries(sourceEntries);

    germanRows = alignSourceRows(loadGermanSourceRowsFromDatabase(), sourceEntries);
    if (sourceEntries.isEmpty()) return result;

    if ("de".equals(lang)) {
      result.putAll(sourceEntries);
      return result;
    }

    String table = table(lang);
    ensureTable(table);
    // Duplikatbereinigung nicht mehr pro Request ausführen; sie war bei Login/Sprachwechsel zu teuer.
    // Das gezielte Upsert anhand des fachlichen Keys verhindert neue Duplikate.

    // Das Frontend kann bereits sichtbare Zieltexte kennen (z. B. eingebaute
    // Login-/Modulübersetzungen). Diese werden genutzt, aber ab 34t mit der
    // Referenz-ID aus translation_german persistiert, sofern diese existiert.
    Map<String, String> known = normalizeKnownTranslations(knownTranslations);
    int autoTranslationsDone = 0;

    if (!germanRows.isEmpty()) {
      for (TranslationRow row : germanRows) {
        String key = safeKey(row.key());
        String germanText = row.text() == null ? key : row.text();
        if (key.isBlank()) continue;

        String description = prefix(lang) + "." + key;
        String legacyUiDescription = prefix(lang) + ".UI." + key;

        String knownText = known.get(key);
        if (isUsableTranslation(knownText, germanText)) {
          upsertAligned(table, row.id(), description, knownText);
          result.put(key, knownText);
          continue;
        }

        String cached = find(table, description);
        if (!isUsableTranslation(cached, germanText)) cached = find(table, legacyUiDescription);
        if (isUsableTranslation(cached, germanText)) {
          upsertAligned(table, row.id(), description, cached);
          result.put(key, cached);
          continue;
        }

        String translated = manualTranslationFallback(key, lang, germanText);
        if (!isUsableTranslation(translated, germanText) && autoTranslationsDone < maxSynchronousAutoTranslations) {
          translated = translateText(germanText, lang);
          if (isUsableTranslation(translated, germanText)) autoTranslationsDone++;
        }
        if (!isUsableTranslation(translated, germanText)) {
          // Schritt 34x: Keine langsame Massenübersetzung im Loginpfad.
          // Fehlende Texte bleiben temporär beim vorhandenen UI-Fallback und werden nicht als Deutsch-Füllung gespeichert.
          result.put(key, germanText);
          continue;
        }

        upsertAligned(table, row.id(), description, translated);
        result.put(key, translated);
      }
    } else {
      for (Map.Entry<String, String> entry : sourceEntries.entrySet()) {
        String key = safeKey(entry.getKey());
        String germanText = entry.getValue() == null ? key : entry.getValue();
        if (key.isBlank()) continue;

        String description = prefix(lang) + "." + key;
        String legacyUiDescription = prefix(lang) + ".UI." + key;

        String knownText = known.get(key);
        if (isUsableTranslation(knownText, germanText)) {
          upsert(table, description, knownText);
          result.put(key, knownText);
          continue;
        }

        String cached = find(table, description);
        if (!isUsableTranslation(cached, germanText)) cached = find(table, legacyUiDescription);
        if (isUsableTranslation(cached, germanText)) {
          upsert(table, description, cached);
          result.put(key, cached);
          continue;
        }

        String translated = translateText(germanText, lang);
        if (!isUsableTranslation(translated, germanText)) {
          translated = manualTranslationFallback(key, lang, germanText);
        }
        if (!isUsableTranslation(translated, germanText)) {
          result.put(key, germanText);
          continue;
        }

        upsert(table, description, translated);
        result.put(key, translated);
      }
    }

    // Zusätzliche bekannte Zieltexte persistieren, die nicht im deutschen
    // Referenzkatalog enthalten waren, z. B. neu auftauchende Modulbuttons.
    // Diese bekommen reguläre Auto-IDs, bis sie später in translation_german
    // aufgenommen werden.
    for (Map.Entry<String, String> entry : known.entrySet()) {
      String key = safeKey(entry.getKey());
      if (key.isBlank() || result.containsKey(key)) continue;
      String text = entry.getValue();
      String germanText = sourceEntries.getOrDefault(key, key);
      if (!isUsableTranslation(text, germanText)) continue;
      upsert(table, prefix(lang) + "." + key, text);
      result.put(key, text);
    }

    // Schritt 34z2:
    // GAM-1.0-Logik: translation_german liefert die Master-Keyliste, aber
    // gepflegt wird ausschließlich die aktuell angeforderte UI-Sprache.
    // Reihenfolge: DB-Cache der aktiven Sprache -> LibreTranslate nur für
    // fehlende/deutsch gefüllte Keys dieser Sprache -> Speicherung in genau
    // dieser translation_<sprache>-Tabelle. Kein Massenlauf über weitere Sprachen.
    startBackgroundReconcile(lang, sourceEntries, known);
    return result;
  }

  public Map<String, String> translateLive(String language, Map<String, String> germanEntries) {
    String lang = normalize(language);
    Map<String, String> result = new LinkedHashMap<>();
    if (germanEntries == null || germanEntries.isEmpty()) return result;
    for (Map.Entry<String, String> entry : germanEntries.entrySet()) {
      String key = safeKey(entry.getKey());
      String germanText = entry.getValue();
      if (blank(key) || blank(germanText)) continue;
      if ("de".equals(lang)) {
        result.put(key, germanText);
        continue;
      }
      String translated = translateText(germanText, lang);
      if (!isUsableTranslation(translated, germanText)) {
        translated = manualTranslationFallback(key, lang, germanText);
      }
      result.put(key, isUsableTranslation(translated, germanText) ? translated : germanText);
    }
    return result;
  }

  private void startBackgroundReconcile(String lang, Map<String, String> sourceEntries, Map<String, String> known) {
    if ("de".equals(lang) || blank(lang)) return;
    if (!BACKGROUND_RECONCILE_LANGUAGES.add(lang)) return;

    Map<String, String> sourceCopy = new LinkedHashMap<>(sourceEntries == null ? Map.of() : sourceEntries);
    Map<String, String> knownCopy = new LinkedHashMap<>(known == null ? Map.of() : known);
    Thread worker = new Thread(() -> {
      try {
        reconcileLanguageInBackground(lang, sourceCopy, knownCopy);
      } catch (Exception ex) {
        if (diagnostics) ex.printStackTrace();
      } finally {
        BACKGROUND_RECONCILE_LANGUAGES.remove(lang);
      }
    }, "gam-ui-translation-reconcile-" + lang);
    worker.setDaemon(true);
    worker.start();
  }

  private void reconcileLanguageInBackground(String lang, Map<String, String> requestSourceEntries, Map<String, String> knownTranslations) {
    String table = table(lang);
    ensureTable(table);

    Map<String, String> source = new LinkedHashMap<>();
    for (TranslationRow row : loadGermanSourceRowsFromDatabase()) {
      if (!blank(row.key()) && !blank(row.text())) source.putIfAbsent(safeKey(row.key()), row.text());
    }
    defaultGermanUiEntries().forEach((k, v) -> source.putIfAbsent(safeKey(k), v));
    if (requestSourceEntries != null) {
      requestSourceEntries.forEach((k, v) -> {
        if (!blank(k) && !blank(v)) source.putIfAbsent(safeKey(k), v);
      });
    }
    if (source.isEmpty()) return;

    int translatedCount = 0;
    for (Map.Entry<String, String> entry : source.entrySet()) {
      String key = safeKey(entry.getKey());
      String germanText = entry.getValue();
      if (blank(key) || blank(germanText)) continue;

      String description = prefix(lang) + "." + key;
      ExistingTranslationRow existing = findExistingByKey(table, key, description);
      if (existing != null && isUsableTranslation(existing.text(), germanText)) {
        if (!description.equalsIgnoreCase(existing.description())) {
          upsert(table, description, existing.text());
        }
        continue;
      }

      String known = knownTranslations == null ? null : knownTranslations.get(key);
      if (isUsableTranslation(known, germanText)) {
        upsert(table, description, known);
        continue;
      }

      String translated = manualTranslationFallback(key, lang, germanText);
      if (!isUsableTranslation(translated, germanText)) {
        if (translatedCount >= maxBackgroundAutoTranslations) continue;
        translated = translateText(germanText, lang);
        if (isUsableTranslation(translated, germanText)) translatedCount++;
      }
      if (isUsableTranslation(translated, germanText)) {
        upsert(table, description, translated);
      }
    }
  }

  private String translateText(String text, String target) {
    if (!libreTranslateEnabled || blank(text) || "de".equals(target)) return text;

    // 1) Wenn die alte GAM-1.0-JAR im Classpath liegt, wird sie bevorzugt genutzt.
    //    Keine harte Maven-Abhängigkeit: der Build bleibt stabil und das Paket bleibt klein.
    String translated = tryLibreTranslateJava(text, target, libreTranslateUrl);
    if (isUsableTranslation(translated, text)) return translated;
    translated = tryLibreTranslateJava(text, target, libreTranslateFallbackUrl);
    if (isUsableTranslation(translated, text)) return translated;

    // 2) Fallback auf denselben LibreTranslate-REST-Endpunkt im form-urlencoded-Format
    //    (kompatibel zur libretranslate-java-Bibliothek).
    translated = translateWithFormRest(text, target, libreTranslateUrl);
    if (isUsableTranslation(translated, text)) return translated;
    translated = translateWithFormRest(text, target, libreTranslateFallbackUrl);
    if (isUsableTranslation(translated, text)) return translated;

    // 3) Kompatibilitätsfallback für lokale LibreTranslate-Server, die JSON erwarten.
    translated = translateWithJsonRest(text, target, libreTranslateUrl);
    if (isUsableTranslation(translated, text)) return translated;
    translated = translateWithJsonRest(text, target, libreTranslateFallbackUrl);
    if (isUsableTranslation(translated, text)) return translated;

    return null;
  }

  private String tryLibreTranslateJava(String text, String target, String url) {
    if (!libreTranslateJavaClientEnabled || blank(url)) return null;
    PrintStream oldErr = System.err;
    try {
      // Die Bibliothek gibt bei Verbindungsfehlern selbst Stacktraces auf System.err aus.
      // Im Normalbetrieb unterdrücken wir das, damit der erste Eindruck sauber bleibt.
      if (!diagnostics) System.setErr(new PrintStream(OutputStream.nullOutputStream()));

      Class<?> translator = Class.forName("space.dynomake.libretranslate.Translator");
      Method setUrlApi = translator.getMethod("setUrlApi", String.class);
      setUrlApi.invoke(null, url);
      if (!blank(libreTranslateApiKey)) {
        try {
          Method setApiKey = translator.getMethod("setApiKey", String.class);
          setApiKey.invoke(null, libreTranslateApiKey);
        } catch (NoSuchMethodException ignored) {
          // Ältere JAR ohne API-Key-Setter bleibt nutzbar.
        }
      }
      Method translate = translator.getMethod("translate", String.class, String.class, String.class);
      Object value = translate.invoke(null, "de", target, text);
      return value == null ? null : String.valueOf(value);
    } catch (ClassNotFoundException ignored) {
      return null;
    } catch (Exception ex) {
      if (diagnostics) ex.printStackTrace();
      return null;
    } finally {
      System.setErr(oldErr);
    }
  }

  private String translateWithFormRest(String text, String target, String url) {
    if (blank(url)) return null;
    try {
      StringBuilder body = new StringBuilder();
      appendForm(body, "q", text);
      appendForm(body, "source", "de");
      appendForm(body, "target", target);
      appendForm(body, "format", "text");
      if (!blank(libreTranslateApiKey)) appendForm(body, "api_key", libreTranslateApiKey);

      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .timeout(Duration.ofSeconds(12))
          .header("Accept", "application/json")
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
      JsonNode json = objectMapper.readTree(response.body());
      JsonNode translatedText = json.get("translatedText");
      return translatedText == null ? null : translatedText.asText();
    } catch (Exception ex) {
      if (diagnostics) ex.printStackTrace();
      return null;
    }
  }

  private String translateWithJsonRest(String text, String target, String url) {
    if (blank(url)) return null;
    try {
      Map<String, String> payload = new LinkedHashMap<>();
      payload.put("q", text);
      payload.put("source", "de");
      payload.put("target", target);
      payload.put("format", "text");
      if (!blank(libreTranslateApiKey)) payload.put("api_key", libreTranslateApiKey);

      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .timeout(Duration.ofSeconds(12))
          .header("Accept", "application/json")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
      JsonNode json = objectMapper.readTree(response.body());
      JsonNode translatedText = json.get("translatedText");
      return translatedText == null ? null : translatedText.asText();
    } catch (Exception ex) {
      if (diagnostics) ex.printStackTrace();
      return null;
    }
  }

  private static void appendForm(StringBuilder body, String key, String value) {
    if (!body.isEmpty()) body.append('&');
    body.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
    body.append('=');
    body.append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
  }

  private Map<String, String> normalizeKnownTranslations(Map<String, String> knownTranslations) {
    Map<String, String> known = new LinkedHashMap<>();
    if (knownTranslations == null || knownTranslations.isEmpty()) return known;
    for (Map.Entry<String, String> entry : knownTranslations.entrySet()) {
      String key = safeKey(keyFromDescription(entry.getKey()));
      if (key.isBlank()) key = safeKey(entry.getKey());
      String value = entry.getValue();
      if (!key.isBlank() && !blank(value)) known.put(key, value.trim());
    }
    return known;
  }

  private Map<String, String> defaultGermanUiEntries() {
    Map<String, String> entries = new LinkedHashMap<>();
    entries.put("accessLinkError", "Abruflink konnte nicht erzeugt werden");
    entries.put("accessibilityNote", "PDF/UA-Vorbereitung: Sprache, Titel, Metadaten und Lesereihenfolge werden gesetzt.");
    entries.put("accounts", "Accounts");
    entries.put("addPosition", "+ Position übernehmen");
    entries.put("admin", "Administration");
    entries.put("amount", "Betrag");
    entries.put("appTitle", "GAM 2.0");
    entries.put("application", "Anwendung");
    entries.put("applicationChoose", "Anwendung wählen");
    entries.put("approval", "Freigabemanagement");
    entries.put("approx", "Raten à ca.");
    entries.put("bank", "Bankverbindung");
    entries.put("bic", "BIC");
    entries.put("cancelInvoice", "Stornorechnung");
    entries.put("cashbook", "Kassenbuch");
    entries.put("checks", "Prüfungen");
    entries.put("chooseApplication", "Anwendung wählen");
    entries.put("code", "Code");
    entries.put("comingSoon", "Dieses Modul ist als Lesemodus-/Vorschau-Bereich vorbereitet.");
    entries.put("company", "Gesellschaft");
    entries.put("compliance", "Prüfungen");
    entries.put("connected", "verbunden");
    entries.put("createCreditNote", "Gutschrift erstellen");
    entries.put("dashboard", "Dashboard");
    entries.put("date", "Datum");
    entries.put("db", "DB");
    entries.put("demoReadOnlyShell", "Lesemodus-Shell");
    entries.put("description", "Beschreibung");
    entries.put("discount", "Rabatt");
    entries.put("discountType", "Rabattart");
    entries.put("discountValue", "Rabattwert");
    entries.put("editChangesSave", "Änderungen speichern");
    entries.put("exportCheckLoading", "Exportprüfung wird geladen.");
    entries.put("found", "gefunden");
    entries.put("gross", "Brutto");
    entries.put("iban", "IBAN");
    entries.put("installmentCount", "Ratenanzahl");
    entries.put("installments", "Ratenzahlung");
    entries.put("inventory", "Geräteverzeichnis");
    entries.put("invoice", "Rechnungsprogramm");
    entries.put("invoiceDate", "Rechnungsdatum");
    entries.put("treatmentDate", "Behandlungsdatum");
    entries.put("serviceDate", "Leistungsdatum");
    entries.put("dueDate", "Fälligkeitsdatum");
    entries.put("invoiceCustomerFile", "Kundendatei");
    entries.put("invoiceUser", "Benutzer");
    entries.put("invoicePaymentMethod", "Zahlungsart");
    entries.put("invoiceTaxNumberVatId", "Steuer-/USt-ID");
    entries.put("invoiceRecipient", "Rechnungsempfänger");
    entries.put("invoiceEdit", "Rechnung bearbeiten");
    entries.put("invoiceLanguageHint", "Die PDF-Sprache im Rechnungsprogramm bleibt separat von der Oberflächensprache.");
    entries.put("invoiceList", "Rechnungsliste");
    entries.put("invoicePreview", "Rechnungsvorschau");
    entries.put("invoicePreviewTitle", "Verbindliche Rechnungsvorschau");
    entries.put("invoicePreviewHelp", "Diese Vorschau soll dem späteren PDF entsprechen: Texte, Positionen, Rabatt/Gutschein, Ratenzahlung, Hinweise und Bankdaten.");
    entries.put("invoiceSearch", "Rechnung suchen");
    entries.put("invoiceTypePaymentAdvice", "Zahlungsavis");
    entries.put("language", "Sprache");
    entries.put("lbd", ".lbd");
    entries.put("lbdMissingPlaceholder", "lbd – .lbd-Empfängerdatei wurde nicht gefunden; Export nutzt Platzhalter.");
    entries.put("legacyLoginHint", "Kompatibler Login über bestehende accounts-Tabelle.");
    entries.put("lineTotal", "Gesamt");
    entries.put("loadingNumber", "wird geladen");
    entries.put("login", "Anmelden");
    entries.put("loginTitle", "Anmeldung");
    entries.put("logout", "Abmelden");
    entries.put("mandatoryZugferd", "ZUGFeRD/Factur-X ist Pflicht-Export.");
    entries.put("module.admin", "Administration");
    entries.put("module.approval", "Freigabemanagement");
    entries.put("module.approvals", "Freigabemanagement");
    entries.put("module.cashbook", "Kassenbuch");
    entries.put("module.checks", "Prüfungen");
    entries.put("module.compliance", "Prüfungen");
    entries.put("module.dashboard", "Dashboard");
    entries.put("module.inventory", "Geräteverzeichnis");
    entries.put("module.invoice", "Rechnungsprogramm");
    entries.put("module.invoices", "Rechnungsprogramm");
    entries.put("module.orders", "Bestelltool");
    entries.put("module.personnel", "Personaldaten");
    entries.put("module.price", "Preisliste");
    entries.put("module.priceList", "Preisliste");
    entries.put("module.reports", "Reports");
    entries.put("module.tasks", "Aufgabenverwaltung");
    entries.put("module.users", "Benutzer/Rechte");
    entries.put("module.usersRights", "Benutzer/Rechte");
    entries.put("module.warehouse", "Lagerverwaltung");
    entries.put("module.workplace", "Arbeitsplatzausstattung");
    entries.put("modulePreview", "Modulübersicht");
    entries.put("moduleVisibilityHint", "Alle historischen GAM-Anwendungen sind sichtbar; noch nicht vollständig migrierte Module starten im Lesemodus.");
    entries.put("net", "Netto");
    entries.put("newInvoice", "Neue Rechnung");
    entries.put("noLines", "Noch keine Positionen übernommen.");
    entries.put("noPasskey", "Für diesen Benutzer ist kein Passkey gespeichert.");
    entries.put("noRecipient", "Keine Empfängerdatei geladen");
    entries.put("notConnected", "nicht verbunden");
    entries.put("notFound", "nicht gefunden");
    entries.put("notes", "Hinweise");
    entries.put("openPortal", "Portal öffnen");
    entries.put("orders", "Bestelltool");
    entries.put("passkeyHint", "Passkey-Testworkflow für localhost/Windows Hello/YubiKey. Für Produktivbetrieb wird die serverseitige WebAuthn-Signaturprüfung noch gehärtet.");
    entries.put("passkeyLogin", "Passkey-Login");
    entries.put("passkeyLoginButton", "Mit Passkey anmelden");
    entries.put("passkeyLoginHint", "Passkey-Login über WebAuthn. Lokal funktioniert das mit localhost.");
    entries.put("passkeyRegister", "Passkey registrieren");
    entries.put("passkeyStatus", "Passkey-Status prüfen");
    entries.put("passkeyUnsupported", "Dieser Browser unterstützt keine Passkeys/WebAuthn.");
    entries.put("password", "Passwort");
    entries.put("passwordLogin", "Passwort");
    entries.put("patientPortal", "Patientenportal");
    entries.put("patientPortalDigital", "Digitales Rechnungsportal");
    entries.put("patientPortalHint", "QR-Code für Patientenabruf mit Sprachwahl und Rechnungshistorie.");
    entries.put("invoicePortalTitle", "Digitales Rechnungsportal");
    entries.put("invoicePortalQrHint", "QR-Code für Patientenabruf mit Sprachwahl und Rechnungshistorie.");
    entries.put("invoicePortalHelp", "Wählen Sie die gewünschte Sprache und laden Sie Ihre Rechnung erneut herunter.");
    entries.put("portalLanguage", "Portalsprache");
    entries.put("invoicePdfLanguage", "Rechnungs-/PDF-Sprache");
    entries.put("readInvoice", "Rechnung vorlesen");
    entries.put("payment", "Zahlungsart");
    entries.put("paymentAdvice", "Zahlungsavis");
    entries.put("paymentMethod", "Zahlungsart");
    entries.put("paymentUnknown", "unbekannt");
    entries.put("paymentCash", "Barzahlung");
    entries.put("paymentCard", "Kartenzahlung");
    entries.put("paymentTransfer", "Überweisung");
    entries.put("pdfLanguage", "PDF-Sprache");
    entries.put("percent", "Prozent");
    entries.put("personnel", "Personaldaten");
    entries.put("pleaseChoose", "Bitte wählen");
    entries.put("pleaseSelectCompany", "Bitte zuerst eine Gesellschaft auswählen.");
    entries.put("pleaseSelectInvoice", "Bitte links eine Rechnung auswählen.");
    entries.put("previewHelp", "Diese Vorschau soll dem späteren PDF entsprechen: Texte, Positionen, Rabatt/Gutschein, Ratenzahlung, Hinweise und Bankdaten.");
    entries.put("previewTitle", "Verbindliche Rechnungsvorschau");
    entries.put("price", "Preisliste");
    entries.put("product", "Produkt");
    entries.put("proformaFailed", "Proforma konnte nicht erstellt werden");
    entries.put("qrAltPortal", "QR-Code Rechnungsportal");
    entries.put("qty", "Menge");
    entries.put("quantity", "Menge");
    entries.put("readAloud", "Rechnung vorlesen");
    entries.put("readOnly", "Lesemodus");
    entries.put("readonly.orders.description", "Historisches GAM-Modul für Beschaffung und Bestellungen. Schritt 31 zeigt das Modul bereits als Lesemodus-Shell; Schreibfunktionen folgen nach Rekonstruktion der Alt-GAM-Fachlogik.");
    entries.put("readonly.priceList.description", "Historische Preislisten- und Produktübersicht. Administration und Bearbeitung werden später separat rekonstruiert.");
    entries.put("readonly.workplace.description", "Historisches Modul für Arbeitsplatz-, Raum- und Geräteausstattung. In Schritt 31 bewusst sichtbar, aber noch ohne Bearbeitungsfunktionen.");
    entries.put("reason", "Grund");
    entries.put("recipient", "Empfänger");
    entries.put("reducedTotal", "Endbetrag nach Abzug");
    entries.put("remark", "Bemerkung");
    entries.put("remove", "Entfernen");
    entries.put("reports", "Reports");
    entries.put("role", "Rolle");
    entries.put("saveFailed", "Speichern fehlgeschlagen");
    entries.put("saveInvoice", "Rechnung speichern");
    entries.put("search", "Suchen");
    entries.put("searchPlaceholder", "Suche Nummer / Name / Grund");
    entries.put("seconds", "Sekunden");
    entries.put("selectAtLeastOneLine", "Bitte mindestens eine Position auswählen.");
    entries.put("setupCreated", "QR-Code erzeugt. Bitte mit Authenticator scannen und den Code bestätigen.");
    entries.put("setupExisting", "Es existiert bereits ein 2FA-Secret. Mit Bestätigung wird es ersetzt.");
    entries.put("setupFirst", "Bitte zuerst QR-Code erzeugen.");
    entries.put("statusLoading", "Status wird geladen.");
    entries.put("step31ModuleOverview", "Schritt 31 stellt alle historischen GAM-Anwendungen sichtbar dar. Vollständig migrierte Bereiche sind nutzbar, noch offene Module erscheinen bewusst als Lesemodus-Shells.");
    entries.put("step31ModuleOverviewShort", "Schritt 31 Modulübersicht");
    entries.put("stopReading", "Vorlesen stoppen");
    entries.put("tasks", "Aufgabenverwaltung");
    entries.put("tax", "MwSt");
    entries.put("taxNo", "Steuer/VAT");
    entries.put("technicalDelete", "Technisch löschen");
    entries.put("tooManyAttempts", "Zu viele Fehlversuche. Neuer Versuch in");
    entries.put("total", "Gesamt");
    entries.put("totpCode", "6-stelliger Authenticator-Code");
    entries.put("totpCodeConfirm", "6-stelliger Code zur Bestätigung");
    entries.put("totpLogin", "2FA-Login");
    entries.put("totpLoginButton", "Mit 2FA anmelden");
    entries.put("totpQrCreate", "QR-Code erzeugen");
    entries.put("totpRegister", "2FA registrieren");
    entries.put("totpRegisterHint", "Für die 2FA-Registrierung wird nur der Benutzername benötigt. Das Passwortfeld ist bewusst ausgeblendet.");
    entries.put("totpSave", "2FA speichern");
    entries.put("totpSaved", "2FA wurde in accounts.secretkey gespeichert. Du kannst nun den 2FA-Login verwenden.");
    entries.put("ttsAuto", "Automatisch");
    entries.put("ttsBrowser", "Browser/Windows");
    entries.put("ttsEngine", "Vorlesetechnik");
    entries.put("ttsLanguage", "Vorlesesprache");
    entries.put("ttsMary", "MaryTTS");
    entries.put("ttsRate", "Geschwindigkeit");
    entries.put("ttsSettings", "Vorleseinstellungen");
    entries.put("ttsVoice", "Stimme");
    entries.put("ttsVoiceAuto", "Automatische Stimme");
    entries.put("uiLanguage", "Oberflächensprache");
    entries.put("username", "Benutzername");
    entries.put("usernameRequired", "Bitte zuerst Benutzernamen eingeben.");
    entries.put("users", "Benutzer/Rechte");
    entries.put("usersRights", "Benutzer/Rechte");
    entries.put("voucher", "Gutschein");
    entries.put("voucherAmount", "Gutscheinbetrag");
    entries.put("voucherText", "Gutscheintext");
    entries.put("wait", "Warten");
    entries.put("warehouse", "Lagerverwaltung");
    entries.put("workplace", "Arbeitsplatzausstattung");
    entries.put("xml", "XML");
    entries.put("zugferdIssues", "ZUGFeRD-Export hat Hinweise");
    entries.put("zugferdPdf", "ZUGFeRD-PDF");
    entries.put("zugferdReady", "ZUGFeRD-Export bereit");
    return entries;
  }



  private void ensureGermanMasterEntries(Map<String, String> sourceEntries) {
    if (sourceEntries == null || sourceEntries.isEmpty()) return;
    String table = "translation_german";
    ensureTable(table);
    for (Map.Entry<String, String> entry : sourceEntries.entrySet()) {
      String key = safeKey(entry.getKey());
      String text = entry.getValue();
      if (blank(key) || blank(text)) continue;
      upsert(table, "GERMAN." + key, text);
    }
  }

  private List<TranslationRow> alignSourceRows(List<TranslationRow> databaseRows, Map<String, String> sourceEntries) {
    List<TranslationRow> rows = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    long maxId = 0;

    for (TranslationRow row : databaseRows == null ? List.<TranslationRow>of() : databaseRows) {
      String key = safeKey(row.key());
      if (key.isBlank() || !seen.add(key)) continue;
      long id = row.id() > 0 ? row.id() : 0;
      if (id > maxId) maxId = id;
      String text = !blank(row.text()) ? row.text() : sourceEntries.getOrDefault(key, key);
      rows.add(new TranslationRow(id, key, text));
    }

    long syntheticId = maxId + 1;
    for (Map.Entry<String, String> entry : sourceEntries.entrySet()) {
      String key = safeKey(entry.getKey());
      if (key.isBlank() || !seen.add(key)) continue;
      String text = !blank(entry.getValue()) ? entry.getValue() : key;
      rows.add(new TranslationRow(syntheticId++, key, text));
    }
    return rows;
  }


  private record TranslationRow(long id, String key, String text) {}
  private record ExistingTranslationRow(long id, String description, String key, String text) {}

  private List<TranslationRow> loadGermanSourceRowsFromDatabase() {
    List<TranslationRow> rows = new ArrayList<>();
    loadSourceRows(rows, "translation_german");
    if (rows.isEmpty()) loadSourceRows(rows, "translate_GERMAN");
    return rows;
  }

  private void loadSourceRows(List<TranslationRow> rows, String table) {
    try {
      if (!tableExists(table)) return;
      jdbc.query("SELECT * FROM " + q(table) + " ORDER BY ID ASC", rs -> {
        ResultSetMetaData meta = rs.getMetaData();
        int idColumn = findColumn(meta, "ID");
        int descriptionColumn = findColumn(meta, "TRANSLATE_DESCRIPTION", "DESCRIPTION", "KEY", "NAME");
        int textColumn = findColumn(meta, "TRANSLATED_TEXT", "GERMAN_TEXT", "TEXT", "VALUE", "TRANSLATION");
        if (idColumn <= 0 || descriptionColumn <= 0 || textColumn <= 0) return null;

        while (rs.next()) {
          long id = rs.getLong(idColumn);
          String description = rs.getString(descriptionColumn);
          String text = rs.getString(textColumn);
          String key = keyFromDescription(description);
          if (!blank(key) && !blank(text)) rows.add(new TranslationRow(id, key, text));
        }
        return null;
      });
    } catch (Exception ignored) {
    }
  }

  private Map<String, String> loadGermanSourceEntriesFromDatabase() {
    Map<String, String> entries = new LinkedHashMap<>();
    // Schritt 34q: Alt-GAM verwendet translation_german, nicht translate_GERMAN.
    // Die fehlerhaften translate_* Tabellen aus Zwischenständen werden nur noch
    // optional als Fallback gelesen, aber nicht mehr beschrieben.
    loadSourceTable(entries, "translation_german");
    loadSourceTable(entries, "translate_GERMAN");
    return entries;
  }

  private void loadSourceTable(Map<String, String> entries, String table) {
    try {
      if (!tableExists(table)) return;
      jdbc.query("SELECT * FROM " + q(table) + " ORDER BY ID ASC", rs -> {
        ResultSetMetaData meta = rs.getMetaData();
        int descriptionColumn = findColumn(meta, "TRANSLATE_DESCRIPTION", "DESCRIPTION", "KEY", "NAME");
        int textColumn = findColumn(meta, "TRANSLATED_TEXT", "GERMAN_TEXT", "TEXT", "VALUE", "TRANSLATION");
        if (descriptionColumn <= 0 || textColumn <= 0) return null;

        while (rs.next()) {
          String description = rs.getString(descriptionColumn);
          String text = rs.getString(textColumn);
          String key = keyFromDescription(description);
          if (!blank(key) && !blank(text)) entries.put(key, text);
        }
        return null;
      });
    } catch (Exception ignored) {
    }
  }

  private boolean tableExists(String table) {
    try {
      jdbc.queryForObject("SELECT 1 FROM " + q(table) + " LIMIT 1", Integer.class);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private static int findColumn(ResultSetMetaData meta, String... candidates) throws SQLException {
    int count = meta.getColumnCount();
    for (String candidate : candidates) {
      for (int i = 1; i <= count; i++) {
        String name = meta.getColumnLabel(i);
        if (name == null || name.isBlank()) name = meta.getColumnName(i);
        if (name != null && name.equalsIgnoreCase(candidate)) return i;
      }
    }
    for (String candidate : candidates) {
      for (int i = 1; i <= count; i++) {
        String name = meta.getColumnLabel(i);
        if (name == null || name.isBlank()) name = meta.getColumnName(i);
        if (name != null && name.toUpperCase(Locale.ROOT).contains(candidate.toUpperCase(Locale.ROOT))) return i;
      }
    }
    return -1;
  }

  private static String keyFromDescription(String description) {
    if (blank(description)) return "";
    String d = description.trim();
    String[] prefixes = {
        "GERMAN.", "ENGLISH.", "FRENCH.", "UKRAINIAN.",
        "ITALIAN.", "SWEDISH.", "TURKISH.", "RUSSIAN.",
        "DEUTSCH.", "DE.", "EN.", "FR.", "UK.", "UA.", "IT.", "SV.", "SE.", "TR.", "RU.",
        "UI.GERMAN.", "UI.ENGLISH.", "UI.FRENCH.", "UI.UKRAINIAN.",
        "UI.ITALIAN.", "UI.SWEDISH.", "UI.TURKISH.", "UI.RUSSIAN.",
        "GERMAN.UI.", "ENGLISH.UI.", "FRENCH.UI.", "UKRAINIAN.UI.",
        "ITALIAN.UI.", "SWEDISH.UI.", "TURKISH.UI.", "RUSSIAN.UI."
    };
    boolean changed;
    do {
      changed = false;
      for (String prefix : prefixes) {
        if (d.regionMatches(true, 0, prefix, 0, prefix.length()) && d.length() > prefix.length()) {
          d = d.substring(prefix.length());
          changed = true;
          break;
        }
      }
    } while (changed);

    // Wenn eine Alt-GAM-Tabelle bereits reine Keys ohne Sprachpräfix enthält,
    // verwenden wir diese ebenfalls als Quellkatalog.
    if (!d.contains(" ") && d.length() <= 180) return safeKey(d);
    return "";
  }


  private void ensureTable(String table) {
    try {
      jdbc.execute("CREATE TABLE IF NOT EXISTS " + q(table) + " (" +
          "ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
          "TRANSLATE_DESCRIPTION VARCHAR(255) NOT NULL," +
          "TRANSLATED_TEXT TEXT NOT NULL," +
          "UNIQUE KEY uk_translation_description (TRANSLATE_DESCRIPTION)" +
          ")");
    } catch (Exception ignored) {
      // Bestehende Alt-GAM-Datenbanken können leicht abweichende Tabellenstrukturen haben.
      // Dann versuchen wir trotzdem, daraus zu lesen bzw. einzufügen.
    }
  }

  private String find(String table, String description) {
    try {
      return jdbc.query("SELECT TRANSLATED_TEXT FROM " + q(table) + " WHERE TRANSLATE_DESCRIPTION = ? ORDER BY ID DESC LIMIT 1", rs -> {
        if (rs.next()) return rs.getString(1);
        return null;
      }, description);
    } catch (Exception ignored) {
      return null;
    }
  }

  private void upsert(String table, String description, String text) {
    if (blank(description) || blank(text)) return;
    String key = safeKey(keyFromDescription(description));
    try {
      ExistingTranslationRow existing = findExistingByKey(table, key, description);
      if (existing != null) {
        jdbc.update("UPDATE " + q(table) + " SET TRANSLATED_TEXT = ?, TRANSLATE_DESCRIPTION = ? WHERE ID = ?", text, description, existing.id());
        return;
      }
      jdbc.update("INSERT INTO " + q(table) + " (TRANSLATED_TEXT, TRANSLATE_DESCRIPTION) VALUES (?, ?)", text, description);
    } catch (Exception ignored) {
    }
  }

  private void upsertAligned(String table, long referenceId, String description, String text) {
    // Schritt 34w:
    // Die strikte ID-Angleichung aus 34t/34v war in bestehenden Datenbanken zu aggressiv:
    // Sobald eine Referenz-ID in der Zieltabelle schon existierte, entstanden Primary-Key-Konflikte
    // (Duplicate entry ... for key PRIMARY) und der Login wurde extrem langsam.
    // Deshalb gilt jetzt wieder: Key/Description ist fachlich maßgeblich, die ID bleibt DB-intern.
    // Vorhandene Datensätze werden aktualisiert, fehlende Datensätze ohne feste ID eingefügt.
    upsert(table, description, text);
  }

  private ExistingTranslationRow findExistingByKey(String table, String key, String description) {
    String normalizedKey = safeKey(key);
    try {
      if (tableExists(table)) {
        List<ExistingTranslationRow> rows = jdbc.query("SELECT * FROM " + q(table) + " ORDER BY ID ASC", rs -> {
          List<ExistingTranslationRow> result = new ArrayList<>();
          ResultSetMetaData meta = rs.getMetaData();
          int idColumn = findColumn(meta, "ID");
          int descriptionColumn = findColumn(meta, "TRANSLATE_DESCRIPTION", "DESCRIPTION", "KEY", "NAME");
          int textColumn = findColumn(meta, "TRANSLATED_TEXT", "GERMAN_TEXT", "TEXT", "VALUE", "TRANSLATION");
          if (idColumn <= 0 || descriptionColumn <= 0 || textColumn <= 0) return result;
          while (rs.next()) {
            long id = rs.getLong(idColumn);
            String d = rs.getString(descriptionColumn);
            String text = rs.getString(textColumn);
            String k = safeKey(keyFromDescription(d));
            result.add(new ExistingTranslationRow(id, d, k, text));
          }
          return result;
        });
        for (ExistingTranslationRow row : rows) {
          if (!blank(normalizedKey) && normalizedKey.equalsIgnoreCase(row.key())) return row;
        }
        for (ExistingTranslationRow row : rows) {
          if (!blank(description) && description.equalsIgnoreCase(row.description())) return row;
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  private void cleanupDuplicateKeys(String table) {
    try {
      if (!tableExists(table)) return;
      List<ExistingTranslationRow> rows = jdbc.query("SELECT * FROM " + q(table) + " ORDER BY ID ASC", rs -> {
        List<ExistingTranslationRow> result = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int idColumn = findColumn(meta, "ID");
        int descriptionColumn = findColumn(meta, "TRANSLATE_DESCRIPTION", "DESCRIPTION", "KEY", "NAME");
        int textColumn = findColumn(meta, "TRANSLATED_TEXT", "GERMAN_TEXT", "TEXT", "VALUE", "TRANSLATION");
        if (idColumn <= 0 || descriptionColumn <= 0 || textColumn <= 0) return result;
        while (rs.next()) {
          long id = rs.getLong(idColumn);
          String d = rs.getString(descriptionColumn);
          String text = rs.getString(textColumn);
          String k = safeKey(keyFromDescription(d));
          if (!blank(k)) result.add(new ExistingTranslationRow(id, d, k, text));
        }
        return result;
      });
      Set<String> seen = new LinkedHashSet<>();
      for (ExistingTranslationRow row : rows) {
        String marker = row.key().toLowerCase(Locale.ROOT);
        if (seen.add(marker)) continue;
        jdbc.update("DELETE FROM " + q(table) + " WHERE ID = ?", row.id());
      }
    } catch (Exception ignored) {
    }
  }

  private boolean idExists(String table, long id) {
    try {
      Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + q(table) + " WHERE ID = ?", Integer.class, id);
      return count != null && count > 0;
    } catch (Exception ignored) {
      return false;
    }
  }

  private String manualTranslationFallback(String key, String lang, String germanText) {
    String k = safeKey(key);
    if (blank(k)) return null;
    if ("module.checks".equalsIgnoreCase(k) || "module.compliance".equalsIgnoreCase(k) || "checks".equalsIgnoreCase(k) || "compliance".equalsIgnoreCase(k)) {
      return switch (lang) {
        case "en" -> "Checks";
        case "fr" -> "Contrôles";
        case "uk" -> "Перевірки";
        case "it" -> "Controlli";
        case "sv" -> "Kontroller";
        case "tr" -> "Kontroller";
        case "ru" -> "Проверки";
        default -> null;
      };
    }
    if ("cancelInvoice".equalsIgnoreCase(k) || "createCancellation".equalsIgnoreCase(k)) {
      return switch (lang) {
        case "en" -> "Cancellation invoice";
        case "fr" -> "Facture d’annulation";
        case "uk" -> "Сторно-рахунок";
        case "it" -> "Fattura di storno";
        case "sv" -> "Kreditfaktura";
        case "tr" -> "İptal faturası";
        case "ru" -> "Сторнировочный счёт";
        default -> null;
      };
    }
    return null;
  }


  private static boolean isUsableTranslation(String value, String germanText) {
    if (blank(value)) return false;
    if (blank(germanText)) return true;
    return !value.trim().equalsIgnoreCase(germanText.trim());
  }

  private static String q(String identifier) {
    return "`" + identifier.replace("`", "") + "`";
  }

  private static String normalize(String language) {
    if (language == null || language.isBlank()) return "de";
    String l = language.trim().toLowerCase(Locale.ROOT);
    if (l.startsWith("en")) return "en";
    if (l.startsWith("fr")) return "fr";
    if (l.startsWith("uk") || l.startsWith("ua")) return "uk";
    if (l.startsWith("it")) return "it";
    if (l.startsWith("sv") || l.startsWith("se")) return "sv";
    if (l.startsWith("tr")) return "tr";
    if (l.startsWith("ru")) return "ru";
    return "de";
  }

  private static String table(String lang) {
    return switch (lang) {
      case "en" -> "translation_english";
      case "fr" -> "translation_french";
      case "uk" -> "translation_ukrainian";
      case "it" -> "translation_italian";
      case "sv" -> "translation_swedish";
      case "tr" -> "translation_turkish";
      case "ru" -> "translation_russian";
      default -> "translation_german";
    };
  }

  private static String prefix(String lang) {
    return switch (lang) {
      case "en" -> "ENGLISH";
      case "fr" -> "FRENCH";
      case "uk" -> "UKRAINIAN";
      case "it" -> "ITALIAN";
      case "sv" -> "SWEDISH";
      case "tr" -> "TURKISH";
      case "ru" -> "RUSSIAN";
      default -> "GERMAN";
    };
  }

  private static String safeKey(String key) {
    return key == null ? "" : key.replaceAll("[^A-Za-z0-9_.-]", "_");
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
