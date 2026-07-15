package de.kopfzentrum.gam.invoice;

import java.time.LocalDateTime;

public record PaymentDocument(
    long id,
    String invoiceNumber,
    Integer companyId,
    String documentType,
    String title,
    String language,
    LocalDateTime createdAt,
    String createdBy
) {}
