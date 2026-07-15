package de.kopfzentrum.gam.invoice;

import java.time.LocalDateTime;

public record InvoiceWorkflowEntry(
  long id,
  String invoiceNumber,
  Integer companyId,
  String fromStatus,
  String toStatus,
  String note,
  String changedBy,
  LocalDateTime changedAt
) {}
