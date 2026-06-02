package de.kopfzentrum.gam.invoice;

public record InvoiceSearchCriteria(
  String q,
  Integer companyId,
  String fromDate,
  String toDate,
  Boolean creditNote,
  Boolean cancelled,
  Boolean paymentAdvice,
  int limit,
  int offset
) {}
