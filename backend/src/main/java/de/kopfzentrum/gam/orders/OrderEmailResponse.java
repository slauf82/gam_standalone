package de.kopfzentrum.gam.orders;

import java.time.Instant;

public record OrderEmailResponse(
    boolean sent,
    String message,
    String to,
    String subject,
    Instant sentAt
) {}
