package de.kopfzentrum.gam.invoice;

public record InvoiceValidationIssue(
  String severity,
  String field,
  String message
) {}
