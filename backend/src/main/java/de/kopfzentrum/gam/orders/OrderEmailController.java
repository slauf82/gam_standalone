package de.kopfzentrum.gam.orders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderEmailController {
  private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
  private final JavaMailSender mailSender;
  private final String from;
  private final String fromName;
  private final boolean enabled;

  public OrderEmailController(
      JavaMailSender mailSender,
      @Value("${gam.orders.mail.from:}") String from,
      @Value("${gam.orders.mail.from-name:GAM 2.0}") String fromName,
      @Value("${gam.orders.mail.enabled:false}") boolean enabled
  ) {
    this.mailSender = mailSender;
    this.from = from;
    this.fromName = fromName;
    this.enabled = enabled;
  }

  @PostMapping("/send-email")
  public OrderEmailResponse send(@RequestBody OrderEmailRequest request) {
    InternetAddress to = parseRequiredAddress(request.to(), "Empfänger");
    if (!StringUtils.hasText(request.subject())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Betreff darf nicht leer sein.");
    }
    if (!StringUtils.hasText(request.text())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Mailtext darf nicht leer sein.");
    }

    JavaMailSender sender = senderFor(request.smtp());
    String configuredFrom = request.smtp() != null && StringUtils.hasText(request.smtp().from()) ? request.smtp().from() : from;
    InternetAddress fromAddress = parseOptionalFrom(configuredFrom, fromName, request.smtp());
    InternetAddress replyToAddress = parseOptionalReplyTo(request.smtp());

    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setTo(to);
      helper.setSubject(cleanHeader(request.subject()));
      helper.setText(request.text(), false);
      if (fromAddress != null) {
        helper.setFrom(fromAddress);
      }
      if (replyToAddress != null) {
        helper.setReplyTo(replyToAddress.getAddress());
      }
      sender.send(message);
      return new OrderEmailResponse(true, "Bestell-E-Mail wurde direkt versendet.", to.getAddress(), cleanHeader(request.subject()), Instant.now());
    } catch (MailSendException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mailserver hat den Versand abgelehnt: " + safe(e.getMessage()), e);
    } catch (MailException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Bestell-E-Mail konnte nicht versendet werden: " + safe(e.getMessage()), e);
    } catch (MessagingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bestell-E-Mail konnte nicht erzeugt werden: " + safe(e.getMessage()), e);
    }
  }

  private JavaMailSender senderFor(OrderSmtpConfig smtp) {
    if (smtp == null || !StringUtils.hasText(smtp.host())) {
      if (!enabled) {
        throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
            "Direkter Mailversand ist noch nicht aktiviert. Bitte SMTP-Zugang in application-local.yml, Umgebungsvariablen oder im Kommunikationsassistenten hinterlegen.");
      }
      return mailSender;
    }
    if (!StringUtils.hasText(smtp.username()) || !StringUtils.hasText(smtp.password())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP-Benutzername und Passwort/App-Passwort müssen ausgefüllt sein.");
    }
    JavaMailSenderImpl dynamic = new JavaMailSenderImpl();
    dynamic.setHost(smtp.host().trim());
    dynamic.setPort(smtp.port() == null ? 587 : smtp.port());
    dynamic.setUsername(normalizeAddress(smtp.username()).getAddress());
    dynamic.setPassword(smtp.password());
    Properties props = dynamic.getJavaMailProperties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", String.valueOf(smtp.startTls() == null ? true : smtp.startTls()));
    props.put("mail.smtp.ssl.enable", String.valueOf(smtp.ssl() != null && smtp.ssl()));
    props.put("mail.smtp.connectiontimeout", "10000");
    props.put("mail.smtp.timeout", "10000");
    props.put("mail.smtp.writetimeout", "10000");
    return dynamic;
  }

  private InternetAddress parseRequiredAddress(String raw, String label) {
    if (!StringUtils.hasText(raw)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "-E-Mail-Adresse fehlt.");
    }
    try {
      return normalizeAddress(raw);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "-E-Mail-Adresse ist ungültig: " + e.getMessage(), e);
    }
  }

  private InternetAddress parseOptionalFrom(String raw, String personalName, OrderSmtpConfig smtp) {
    String fallback = smtp != null && StringUtils.hasText(smtp.username()) ? smtp.username() : "";
    String source = StringUtils.hasText(raw) ? raw : fallback;
    if (!StringUtils.hasText(source)) {
      return null;
    }
    try {
      InternetAddress parsed = normalizeAddress(source);
      String effectivePersonalName = smtp != null && StringUtils.hasText(smtp.fromName()) ? smtp.fromName() : personalName;
      if (parsed.getPersonal() == null && StringUtils.hasText(effectivePersonalName)) {
        try {
          parsed.setPersonal(cleanPersonalName(effectivePersonalName), StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
          // Falls ein exotischer Anzeigename Probleme macht, bleibt die reine Adresse gültig.
        }
      }
      return parsed;
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Absender-E-Mail-Adresse ist ungültig: " + e.getMessage(), e);
    }
  }


  private InternetAddress parseOptionalReplyTo(OrderSmtpConfig smtp) {
    if (smtp == null || !StringUtils.hasText(smtp.replyTo())) {
      return null;
    }
    try {
      return normalizeAddress(smtp.replyTo());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Antwortadresse ist ungültig: " + e.getMessage(), e);
    }
  }

  private InternetAddress normalizeAddress(String raw) {
    String value = raw == null ? "" : raw.trim();
    if (value.isBlank()) {
      throw new IllegalArgumentException("leer");
    }
    value = cleanHeader(value);

    // Schritt 38g10:
    // From/To/SMTP-Username dürfen aus UI/YAML/Copy-Paste niemals als
    // kompletter Header validiert werden. Wir extrahieren exakt eine echte
    // Mailadresse und setzen den Anzeigenamen separat über setPersonal().
    String extracted = extractSingleEmail(value);
    try {
      InternetAddress address = new InternetAddress(extracted, false);
      address.validate();
      if (!StringUtils.hasText(address.getAddress()) || !address.getAddress().contains("@")) {
        throw new IllegalArgumentException("Adresse enthält kein @");
      }
      return address;
    } catch (AddressException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  private String extractSingleEmail(String value) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException("leer");
    }
    String trimmed = cleanHeader(value)
        .replace(";", " ")
        .replace("\"", " ")
        .replace("'", " ")
        .trim();

    int lt = trimmed.indexOf('<');
    int gt = trimmed.indexOf('>');
    if (lt >= 0 && gt > lt) {
      trimmed = trimmed.substring(lt + 1, gt).trim();
    }

    Matcher matcher = EMAIL_PATTERN.matcher(trimmed);
    String found = null;
    int count = 0;
    while (matcher.find()) {
      String candidate = matcher.group().trim();
      if (!StringUtils.hasText(found) || !found.equalsIgnoreCase(candidate)) {
        found = candidate;
        count++;
      }
    }
    if (count == 1 && StringUtils.hasText(found)) {
      return found;
    }
    if (count > 1 || trimmed.contains(",")) {
      throw new IllegalArgumentException("bitte genau eine Adresse angeben");
    }
    return trimmed;
  }

  private String cleanHeader(String value) {
    return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
  }

  private String cleanPersonalName(String value) {
    return cleanHeader(value).replace("<", "").replace(">", "").trim();
  }

  private String safe(String value) {
    if (value == null) return "unbekannter Fehler";
    String cleaned = value.replace('\n', ' ').replace('\r', ' ').trim();
    return cleaned.length() > 300 ? cleaned.substring(0, 300) + "…" : cleaned;
  }
}
