package de.kopfzentrum.gam.invoice;

import java.time.LocalDateTime;
import java.util.List;

public record InvoiceWorkflowState(
  String invoiceNumber,
  Integer companyId,
  String status,
  LocalDateTime updatedAt,
  String updatedBy,
  List<String> allowedTransitions,
  List<String> configuredSteps,
  InvoiceWorkflowSettings settings,
  List<InvoiceWorkflowEntry> history
) {}
