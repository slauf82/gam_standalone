package de.kopfzentrum.gam.communication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/communication")
public class CommunicationController {
  private final boolean mailEnabled;
  private final String smtpHost;
  private final int smtpPort;
  private final String smtpUsername;
  private final String smtpPassword;
  private final String defaultFrom;
  private final String defaultRecipient;
  private final String defaultSubject;
  private final String defaultText;
  private final boolean loginNewsEnabled;
  private final String loginNewsTitle;
  private final String loginNewsText;
  private final String loginNewsSeverity;
  private final JdbcTemplate jdbc;

  public CommunicationController(
      @Value("${gam.orders.mail.enabled:false}") boolean mailEnabled,
      @Value("${spring.mail.host:mail.gmx.net}") String smtpHost,
      @Value("${spring.mail.port:587}") int smtpPort,
      @Value("${spring.mail.username:}") String smtpUsername,
      @Value("${spring.mail.password:}") String smtpPassword,
      @Value("${gam.orders.mail.from:}") String defaultFrom,
      @Value("${gam.orders.mail.default-recipient:}") String defaultRecipient,
      @Value("${gam.orders.mail.default-subject:Materialbestellung GAM}") String defaultSubject,
      @Value("${gam.orders.mail.default-text:Hallo,\n\nbitte liefern Sie die folgenden Materialien.\n\nVielen Dank.}") String defaultText,
      @Value("${gam.login.news.enabled:false}") boolean loginNewsEnabled,
      @Value("${gam.login.news.title:Hinweis}") String loginNewsTitle,
      @Value("${gam.login.news.text:}") String loginNewsText,
      @Value("${gam.login.news.severity:info}") String loginNewsSeverity,
      JdbcTemplate jdbc
  ) {
    this.mailEnabled = mailEnabled;
    this.smtpHost = smtpHost;
    this.smtpPort = smtpPort;
    this.smtpUsername = smtpUsername;
    this.smtpPassword = smtpPassword;
    this.defaultFrom = defaultFrom;
    this.defaultRecipient = defaultRecipient;
    this.defaultSubject = defaultSubject;
    this.defaultText = defaultText;
    this.loginNewsEnabled = loginNewsEnabled;
    this.loginNewsTitle = loginNewsTitle;
    this.loginNewsText = loginNewsText;
    this.loginNewsSeverity = loginNewsSeverity;
    this.jdbc = jdbc;
  }

  @GetMapping("/settings")
  public CommunicationSettingsResponse settings() {
    return new CommunicationSettingsResponse(
        mailEnabled,
        smtpHost,
        smtpPort,
        smtpUsername,
        StringUtils.hasText(smtpPassword),
        defaultFrom,
        defaultRecipient,
        defaultSubject,
        defaultText,
        loginNewsEnabled && StringUtils.hasText(loginNewsText),
        loginNewsTitle,
        loginNewsText,
        loginNewsSeverity
    );
  }

  @GetMapping("/login-news")
  public LoginNewsResponse loginNews() {
    String latestNews = latestLegacyNews();
    if (StringUtils.hasText(latestNews)) {
      return new LoginNewsResponse(true, loginNewsTitle, latestNews, loginNewsSeverity);
    }
    return new LoginNewsResponse(
        loginNewsEnabled && StringUtils.hasText(loginNewsText),
        loginNewsTitle,
        loginNewsText,
        loginNewsSeverity
    );
  }

  private String latestLegacyNews() {
    // Schritt 38g7a:
    // Die Login-News sollen nicht aus einer neuen Konfiguration kommen,
    // sondern aus der bestehenden GAM-1.0-Tabelle `news`, die bereits im
    // Adminbereich bearbeitet wird. Historische Datenbanken können sich bei
    // Groß-/Kleinschreibung oder zusätzlichen Spalten leicht unterscheiden,
    // deshalb lesen wir robust über queryForList und suchen das NEWS-Feld
    // case-insensitive.
    try {
      List<Map<String, Object>> rows = jdbc.queryForList(
          "SELECT * FROM news ORDER BY ID DESC LIMIT 10"
      );
      for (Map<String, Object> row : rows) {
        String text = firstTextValue(row, "NEWS", "news", "MELDUNG", "meldung", "TEXT", "text", "HINWEIS", "hinweis");
        if (StringUtils.hasText(text)) {
          return text.trim();
        }
      }
    } catch (Exception ignored) {
      // Fallback auf optionale application-local.yml-Konfiguration erfolgt im Aufrufer.
    }
    return "";
  }

  private String firstTextValue(Map<String, Object> row, String... preferredKeys) {
    for (String key : preferredKeys) {
      Object value = row.get(key);
      if (value == null) {
        // Einige JDBC-Treiber liefern Spaltennamen anders kapitalisiert.
        for (Map.Entry<String, Object> entry : row.entrySet()) {
          if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
            value = entry.getValue();
            break;
          }
        }
      }
      if (value != null && StringUtils.hasText(String.valueOf(value))) {
        return String.valueOf(value);
      }
    }
    return "";
  }
}
