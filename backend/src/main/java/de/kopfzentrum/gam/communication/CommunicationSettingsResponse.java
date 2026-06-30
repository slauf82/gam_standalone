package de.kopfzentrum.gam.communication;

public record CommunicationSettingsResponse(
    boolean mailEnabled,
    String smtpHost,
    int smtpPort,
    String smtpUsername,
    boolean smtpPasswordConfigured,
    String defaultFrom,
    String defaultRecipient,
    String defaultSubject,
    String defaultText,
    boolean loginNewsEnabled,
    String loginNewsTitle,
    String loginNewsText,
    String loginNewsSeverity
) {}
