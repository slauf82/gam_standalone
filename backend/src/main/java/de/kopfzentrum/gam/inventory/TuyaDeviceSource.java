package de.kopfzentrum.gam.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class TuyaDeviceSource {
  private static final Logger log = LoggerFactory.getLogger(TuyaDeviceSource.class);
  private static final int PAGE_SIZE = 100;
  private static final int MAX_PAGES = 100;

  private final TuyaSettingsRepository settings;
  private final ObjectMapper json;
  private final HomeAssistantDeviceSource homeAssistant;

  public TuyaDeviceSource(TuyaSettingsRepository settings, ObjectMapper json, HomeAssistantDeviceSource homeAssistant) {
    this.settings = settings;
    this.json = json;
    this.homeAssistant = homeAssistant;
  }

  public Result load() {
    var sources = settings.loadEnabledInternal();
    if (sources.isEmpty()) {
      return new Result(List.of(), "SKIPPED", "Keine aktive Smart-Life-/Tuya-Quelle konfiguriert");
    }
    List<TuyaDevice> all = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    int ok = 0;
    for (var source : sources) {
      try {
        all.addAll(loadOne(source).devices());
        ok++;
      } catch (Exception e) {
        log.warn("Tuya {} fehlgeschlagen: {}", source.name(), e.toString());
        errors.add(source.name() + ": " + friendly(e));
      }
    }
    Map<String, TuyaDevice> unique = new LinkedHashMap<>();
    for (var device : all) {
      unique.putIfAbsent(device.identityKey(), device);
    }
    String status = ok == 0 ? "FAILED" : "COMPLETED";
    return new Result(new ArrayList<>(unique.values()), status,
        ok + " von " + sources.size() + " Smart-Life-/Tuya-Quellen geladen, " + unique.size() + " Geräte" + (errors.isEmpty() ? "" : "; " + String.join(" | ", errors)));
  }

  public TestResult test(long id) {
    var source = settings.loadOne(id);
    if (!source.enabled()) {
      return new TestResult(id, source.name(), false, "SKIPPED", "Quelle ist deaktiviert.", source.region(), endpoint(source.region()), source.userUid(), "NONE", false, 0, 0, List.of(), List.of("Quelle deaktiviert"));
    }
    if ("HOME_ASSISTANT".equals(source.connectionMode()) && source.homeAssistantSourceId() == null) {
      return new TestResult(id, source.name(), false, "CONFIGURATION", "Bitte eine vorhandene Home-Assistant-Verbindung auswählen.", source.region(), "Home Assistant", source.userUid(), "NONE", false, 0, 0, List.of(), List.of("Keine Home-Assistant-Quelle ausgewählt"));
    }
    if (!"HOME_ASSISTANT".equals(source.connectionMode()) && (source.clientId().isBlank() || source.clientSecret().isBlank())) {
      return new TestResult(id, source.name(), false, "CONFIGURATION", "Für die direkte Tuya-Anmeldung werden die einmalig hinterlegten Tuya-Projektzugangsdaten benötigt.", source.region(), endpoint(source.region()), source.userUid(), "NONE", false, 0, 0, List.of(), List.of("Client ID / Access ID oder Client Secret fehlt"));
    }
    if ("ACCOUNT_LOGIN".equals(source.connectionMode()) && (source.accountUsername().isBlank() || source.accountPassword().isBlank())) {
      return new TestResult(id, source.name(), false, "CONFIGURATION", "Benutzername oder Kennwort fehlt.", source.region(), endpoint(source.region()), source.userUid(), "NONE", false, 0, 0, List.of(), List.of("Smart-Life-/Tuya-Kontodaten unvollständig"));
    }
    try {
      LoadResult loaded = loadOne(source);
      long online = loaded.devices().stream().filter(TuyaDevice::online).count();
      List<String> preview = loaded.devices().stream().map(d -> d.name() + " · " + d.type()).distinct().limit(10).toList();
      boolean success = !loaded.devices().isEmpty();
      String status = success ? "COMPLETED" : "EMPTY";
      boolean viaHa = "HOME_ASSISTANT".equals(source.connectionMode());
      boolean viaAccount = "ACCOUNT_LOGIN".equals(source.connectionMode());
      String message = success
          ? (viaHa ? "Smart Life wurde über Home Assistant verbunden. " : viaAccount ? "Das Smart-Life-/Tuya-Konto wurde angemeldet. " : "Tuya Cloud wurde authentifiziert. ") + loaded.devices().size() + " Geräte wurden geladen."
          : (viaHa ? "Home Assistant ist erreichbar, aber es wurden keine Geräte geliefert. Die ausgewählte Home-Assistant-Verbindung wird jetzt ohne Tuya-Namensfilter ausgewertet." : viaAccount ? "Die Anmeldung war erfolgreich, aber das Konto lieferte keine Geräte. Prüfe App-Auswahl, Land und Kontozuordnung." : "Tuya Cloud wurde authentifiziert, lieferte aber keine Geräte. Prüfe die Smart-Life-UID und die App-Kontoverknüpfung im Tuya-Cloud-Projekt.");
      return new TestResult(id, source.name(), success, status, message, source.region(), viaHa ? "Home Assistant" : endpoint(source.region()), source.userUid(), loaded.apiMethod(), true, loaded.devices().size(), online, preview, loaded.diagnostics());
    } catch (Exception e) {
      return new TestResult(id, source.name(), false, "FAILED", friendly(e), source.region(), "HOME_ASSISTANT".equals(source.connectionMode()) ? "Home Assistant" : endpoint(source.region()), source.userUid(), "FAILED", false, 0, 0, List.of(), List.of(friendly(e)));
    }
  }

  private LoadResult loadOne(TuyaSettingsRepository.Settings source) throws Exception {
    if ("HOME_ASSISTANT".equals(source.connectionMode())) {
      if (source.homeAssistantSourceId() == null) throw new IllegalStateException("Keine Home-Assistant-Quelle ausgewählt");
      List<HomeAssistantDeviceSource.HaDevice> haDevices = homeAssistant.loadSource(source.homeAssistantSourceId());
      List<TuyaDevice> devices = new ArrayList<>();
      for (HomeAssistantDeviceSource.HaDevice d : haDevices) {
        if (!isLikelyTuyaDevice(d)) continue;
        String id = d.entityId();
        devices.add(new TuyaDevice(source.id(), source.name(), source.location(), id, "", d.name(), d.type(), d.ip(), d.mac(), d.manufacturer().isBlank()?"Smart Life / Tuya":d.manufacturer(), d.model(), "", "HOME_ASSISTANT", d.online()));
      }
      List<String> diagnostics = new ArrayList<>();
      diagnostics.add("Home-Assistant-Verbindung erfolgreich verwendet");
      diagnostics.add(haDevices.size()+" Geräte/Entitäten von Home Assistant geprüft");
      diagnostics.add(devices.size()+" eindeutig Smart Life / Tuya zuordenbare Geräte übernommen");
      diagnostics.add((haDevices.size()-devices.size())+" nicht eindeutig zuordenbare Home-Assistant-Geräte verworfen");
      return new LoadResult(devices, "HOME_ASSISTANT_BRIDGE", diagnostics);
    }
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
    List<String> diagnostics = new ArrayList<>();
    String token;
    String effectiveUid = source.userUid();
    if ("ACCOUNT_LOGIN".equals(source.connectionMode())) {
      String projectToken = token(client, source);
      AccountLogin login = accountLogin(client, source, projectToken);
      token = login.accessToken();
      effectiveUid = login.uid();
      diagnostics.add("Smart-Life-/Tuya-Konto erfolgreich angemeldet");
      diagnostics.add("App: " + source.appType() + ", Land: +" + source.countryCode());
      diagnostics.add("Konto-UID automatisch ermittelt");
    } else {
      token = token(client, source);
      diagnostics.add("Authentifizierung erfolgreich");
    }

    if (!effectiveUid.isBlank()) {
      try {
        List<TuyaDevice> devices = loadTuyaUserDimension(client, source, token, effectiveUid);
        diagnostics.add("Smart-Life-UID verwendet: " + maskUid(effectiveUid));
        diagnostics.add("API-Weg: iot-03 / tuyaUser");
        diagnostics.add(devices.size() + " Geräte empfangen");
        if (!devices.isEmpty()) {
          return new LoadResult(devices, "TUYA_USER_DIMENSION", diagnostics);
        }
        diagnostics.add("Benutzerdimension war leer; Benutzer-Geräteliste wird versucht");
      } catch (Exception e) {
        diagnostics.add("Benutzerdimension fehlgeschlagen: " + friendly(e));
      }

      try {
        List<TuyaDevice> devices = loadLegacyUserDevices(client, source, token, effectiveUid);
        diagnostics.add("API-Weg: users/{uid}/devices");
        diagnostics.add(devices.size() + " Geräte empfangen");
        if (!devices.isEmpty()) {
          return new LoadResult(devices, "USER_DEVICE_LIST", diagnostics);
        }
        diagnostics.add("Benutzer-Geräteliste war leer; Projektgeräteliste wird versucht");
      } catch (Exception e) {
        diagnostics.add("Benutzer-Geräteliste fehlgeschlagen: " + friendly(e));
      }
    } else {
      diagnostics.add("Keine Smart-Life-UID konfiguriert; Projektgeräteliste wird als Fallback verwendet");
    }

    List<TuyaDevice> devices = loadProjectDevices(client, source, token);
    diagnostics.add("API-Weg: Projektgeräteliste");
    diagnostics.add(devices.size() + " Geräte empfangen");
    return new LoadResult(devices, "PROJECT_DEVICE_LIST", diagnostics);
  }

  private static boolean isLikelyTuyaDevice(HomeAssistantDeviceSource.HaDevice d) {
    String value = String.join(" ", d.entityId(), d.name(), d.type(), d.manufacturer(), d.model()).toLowerCase(Locale.ROOT);
    return value.contains("tuya") || value.contains("smart life") || value.contains("smartlife")
        || value.contains("_ty_") || value.contains("moes") || value.contains("nous")
        || value.contains("avatto") || value.contains("zemismart") || value.contains("lsc smart")
        || value.contains("gosund") || value.contains("woox") || value.contains("blitzwolf");
  }

  private List<TuyaDevice> loadTuyaUserDimension(HttpClient client, TuyaSettingsRepository.Settings source, String token, String uid) throws Exception {
    List<JsonNode> rows = new ArrayList<>();
    String cursor = "";
    for (int page = 0; page < MAX_PAGES; page++) {
      StringBuilder path = new StringBuilder("/v1.3/iot-03/devices?source_type=tuyaUser&source_id=")
          .append(url(uid)).append("&page_size=").append(PAGE_SIZE);
      if (!cursor.isBlank()) {
        path.append("&last_row_key=").append(url(cursor));
      }
      JsonNode root = authorizedGet(client, source, token, path.toString());
      JsonNode result = root.path("result");
      addRows(rows, result);
      String next = first(result, "last_row_key", "lastRowKey", "next_row_key");
      boolean hasMore = result.path("has_more").asBoolean(result.path("hasMore").asBoolean(false));
      if (!hasMore || next.isBlank() || next.equals(cursor)) {
        break;
      }
      cursor = next;
    }
    return mapDevices(source, rows);
  }

  private List<TuyaDevice> loadLegacyUserDevices(HttpClient client, TuyaSettingsRepository.Settings source, String token, String uid) throws Exception {
    String path = "/v1.0/users/" + url(uid) + "/devices";
    JsonNode root = authorizedGet(client, source, token, path);
    List<JsonNode> rows = new ArrayList<>();
    addRows(rows, root.path("result"));
    return mapDevices(source, rows);
  }

  private List<TuyaDevice> loadProjectDevices(HttpClient client, TuyaSettingsRepository.Settings source, String token) throws Exception {
    List<JsonNode> rows = new ArrayList<>();
    String cursor = "";
    for (int page = 0; page < MAX_PAGES; page++) {
      StringBuilder path = new StringBuilder("/v1.3/iot-03/devices?page_size=").append(PAGE_SIZE);
      if (!cursor.isBlank()) {
        path.append("&last_row_key=").append(url(cursor));
      }
      JsonNode root = authorizedGet(client, source, token, path.toString());
      JsonNode result = root.path("result");
      addRows(rows, result);
      String next = first(result, "last_row_key", "lastRowKey", "next_row_key");
      boolean hasMore = result.path("has_more").asBoolean(result.path("hasMore").asBoolean(false));
      if (!hasMore || next.isBlank() || next.equals(cursor)) {
        break;
      }
      cursor = next;
    }
    return mapDevices(source, rows);
  }

  private static void addRows(List<JsonNode> target, JsonNode result) {
    JsonNode rows = result;
    if (!rows.isArray()) rows = result.path("list");
    if (!rows.isArray()) rows = result.path("devices");
    if (!rows.isArray()) rows = result.path("data");
    if (rows.isArray()) rows.forEach(target::add);
  }

  private List<TuyaDevice> mapDevices(TuyaSettingsRepository.Settings source, List<JsonNode> rows) {
    Map<String, TuyaDevice> unique = new LinkedHashMap<>();
    for (JsonNode node : rows) {
      String id = first(node, "id", "device_id", "deviceId");
      if (id.isBlank()) continue;
      String name = first(node, "name", "device_name", "product_name");
      String category = first(node, "category", "category_code", "product_category");
      String product = first(node, "product_name", "product_id");
      String model = first(node, "model", "model_name", "product_model");
      String ip = first(node, "ip", "ip_address", "local_ip");
      String mac = normalizeMac(first(node, "mac", "mac_address"));
      String uuid = first(node, "uuid", "uid");
      String firmware = first(node, "firmware_version", "version", "v");
      boolean online = node.path("online").asBoolean(!"offline".equalsIgnoreCase(first(node, "status")));
      String type = classify(category, name, product, model);
      String manufacturer = manufacturer(name, product, model);
      TuyaDevice device = new TuyaDevice(source.id(), source.name(), source.location(), id, uuid,
          name.isBlank() ? "Tuya-Gerät " + id : name, type, ip, mac, manufacturer, model, firmware, category, online);
      unique.putIfAbsent(device.identityKey(), device);
    }
    return new ArrayList<>(unique.values());
  }


  private AccountLogin accountLogin(HttpClient client, TuyaSettingsRepository.Settings source, String projectToken) throws Exception {
    String schema = source.appSchema().isBlank() ? ("TUYA_SMART".equals(source.appType()) ? "tuyaSmart" : "smartlife") : source.appSchema();
    String body = json.createObjectNode()
        .put("country_code", source.countryCode())
        .put("username", source.accountUsername())
        .put("password", md5(source.accountPassword()))
        .put("schema", schema)
        .toString();
    JsonNode root = signed(client, source, "POST", "/v1.0/iot-01/associated-users/actions/authorized-login", body, projectToken);
    JsonNode result = root.path("result");
    String accessToken = first(result, "access_token", "accessToken");
    String uid = first(result, "uid", "user_id", "userId");
    if (accessToken.isBlank() || uid.isBlank()) throw apiError(root, "Tuya-Kontoanmeldung lieferte kein Token oder keine UID");
    return new AccountLogin(accessToken, uid);
  }

  private static String md5(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8))).toLowerCase(Locale.ROOT);
  }

  private static String maskUid(String uid) {
    if (uid == null || uid.length() < 8) return "***";
    return uid.substring(0, 3) + "…" + uid.substring(uid.length() - 3);
  }

  private String token(HttpClient client, TuyaSettingsRepository.Settings source) throws Exception {
    JsonNode root = signed(client, source, "GET", "/v1.0/token?grant_type=1", "", null);
    String token = root.path("result").path("access_token").asText("");
    if (token.isBlank()) throw apiError(root, "Kein Zugriffstoken erhalten");
    return token;
  }

  private JsonNode authorizedGet(HttpClient client, TuyaSettingsRepository.Settings source, String token, String path) throws Exception {
    return signed(client, source, "GET", path, "", token);
  }

  private JsonNode signed(HttpClient client, TuyaSettingsRepository.Settings source, String method, String path, String body, String accessToken) throws Exception {
    long timestamp = System.currentTimeMillis();
    String contentHash = sha256(body);
    String stringToSign = method + "\n" + contentHash + "\n\n" + path;
    String payload = source.clientId() + (accessToken == null ? "" : accessToken) + timestamp + stringToSign;
    String sign = hmac(payload, source.clientSecret());
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint(source.region()) + path))
        .timeout(Duration.ofSeconds(20))
        .header("client_id", source.clientId())
        .header("sign", sign)
        .header("t", Long.toString(timestamp))
        .header("sign_method", "HMAC-SHA256")
        .header("Content-Type", "application/json");
    if (accessToken != null) builder.header("access_token", accessToken);
    HttpResponse<String> response = client.send(builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("Tuya Cloud antwortet mit HTTP " + response.statusCode());
    }
    JsonNode root = json.readTree(response.body());
    if (!root.path("success").asBoolean(false)) throw apiError(root, "Tuya-API-Aufruf fehlgeschlagen");
    return root;
  }

  private static IllegalStateException apiError(JsonNode root, String fallback) {
    String code = root.path("code").asText("");
    String message = root.path("msg").asText(fallback);
    return new IllegalStateException((code.isBlank() ? "" : code + ": ") + message);
  }

  private static String endpoint(String region) {
    return switch (region == null ? "" : region.toUpperCase(Locale.ROOT)) {
      case "AMERICA", "WESTERN_AMERICA" -> "https://openapi.tuyaus.com";
      case "EASTERN_AMERICA" -> "https://openapi-ueaz.tuyaus.com";
      case "CHINA" -> "https://openapi.tuyacn.com";
      case "INDIA" -> "https://openapi.tuyain.com";
      default -> "https://openapi.tuyaeu.com";
    };
  }

  public static String classify(String category, String name, String product, String model) {
    String x = (category + " " + name + " " + product + " " + model).toLowerCase(Locale.ROOT);
    if (x.matches(".*\\b(dj|cz|kg|pc|switch|socket|plug)\\b.*") || x.contains("steckdose") || x.contains("smart plug")) return "Steckdose / Schalter";
    if (x.contains("water") || x.contains("leak") || x.contains("flood") || x.contains("wassermelder") || x.contains("wassersensor")) return "Sensor";
    if (x.contains("ventilator") || x.contains("ventilation") || x.contains("air exchanger") || x.contains("wärmetauscher") || x.contains("waermetauscher")) return "Klima & Gebäudetechnik";
    if (x.contains("light") || x.contains("bulb") || x.contains("lamp") || x.contains("dimmer") || x.contains("beleuchtung")) return "Beleuchtung";
    if (x.contains("vacuum") || x.contains("robot") || x.contains("lefant") || x.contains("mower")) return "Robotik";
    if (x.contains("camera") || x.contains("ipc") || x.contains("kamera") || x.contains("doorbell")) return "Kamera / Videoüberwachung";
    if (x.contains("thermostat") || x.contains("climate") || x.contains("heater") || x.contains("trv")) return "Klima & Gebäudetechnik";
    if (x.contains("curtain") || x.contains("cover") || x.contains("blind") || x.contains("shutter") || x.contains("rollladen")) return "Rollladen / Beschattung";
    if (x.contains("lock") || x.contains("schloss")) return "Türschloss / Zutritt";
    if (x.contains("gateway") || x.contains("hub") || x.contains("bridge")) return "Gateway / Zentrale";
    if (x.contains("sensor") || x.contains("pir") || x.contains("temperature") || x.contains("humidity") || x.contains("smoke") || x.contains("motion")) return "Sensor";
    if (x.contains("dishwasher") || x.contains("washing") || x.contains("dryer") || x.contains("appliance")) return "Haushaltsgeräte";
    if (x.contains("energy") || x.contains("meter") || x.contains("inverter") || x.contains("battery") || x.contains("solar")) return "Energie / Wechselrichter";
    if (x.contains("ir") || x.contains("remote")) return "IR-Hub / Fernbedienung";
    return "Smart Life / Tuya Gerät";
  }

  private static String manufacturer(String name, String product, String model) {
    String x = (name + " " + product + " " + model).toLowerCase(Locale.ROOT);
    if (x.contains("lefant")) return "Lefant";
    if (x.contains("moes")) return "Moes";
    if (x.contains("gosund")) return "Gosund";
    if (x.contains("nous")) return "Nous";
    return "Tuya";
  }

  private static String friendly(Exception e) {
    String message = Objects.toString(e.getMessage(), "");
    String lower = message.toLowerCase(Locale.ROOT);
    if (message.contains("1010") || lower.contains("token invalid") || lower.contains("permission")) {
      return "Tuya-Authentifizierung oder API-Berechtigung fehlgeschlagen: " + message;
    }
    if (message.contains("1106") || lower.contains("permission deny")) {
      return "Die Tuya-API-Berechtigung für die Geräteabfrage fehlt: " + message;
    }
    if (message.contains("Connect") || lower.contains("timed out")) {
      return "Die gewählte Tuya-Cloud-Region ist nicht erreichbar.";
    }
    return message.isBlank() ? e.getClass().getSimpleName() : message;
  }

  private static String url(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static String sha256(String value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  private static String hmac(String value, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().withUpperCase().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  }

  private static String first(JsonNode node, String... keys) {
    for (String key : keys) {
      String value = node.path(key).asText("");
      if (!value.isBlank()) return value;
    }
    return "";
  }

  private static String normalizeMac(String value) {
    return value == null ? "" : value.replace('-', ':').toUpperCase(Locale.ROOT);
  }

  public record TuyaDevice(long sourceId, String sourceName, String location, String deviceId, String uuid, String name, String type, String ip, String mac, String manufacturer, String model, String firmware, String category, boolean online) {
    String identityKey() {
      if (!mac.isBlank()) return "mac:" + mac;
      if (!uuid.isBlank()) return "uuid:" + uuid;
      return "tuya:" + sourceId + ":" + deviceId;
    }
  }

  private record AccountLogin(String accessToken, String uid) {}
  private record LoadResult(List<TuyaDevice> devices, String apiMethod, List<String> diagnostics) {}
  public record Result(List<TuyaDevice> devices, String status, String message) {}
  public record TestResult(long sourceId, String sourceName, boolean success, String status, String message, String region, String endpoint, String userUid, String apiMethod, boolean authenticated, int deviceCount, long onlineDeviceCount, List<String> devicePreview, List<String> diagnostics) {}
}
