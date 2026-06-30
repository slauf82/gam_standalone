package de.kopfzentrum.gam.orders;

public record OrderSmtpConfig(
    String host,
    Integer port,
    String username,
    String password,
    String from,
    String fromName,
    String replyTo,
    Boolean startTls,
    Boolean ssl
) {}
