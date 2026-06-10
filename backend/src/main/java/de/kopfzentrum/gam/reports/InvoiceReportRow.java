package de.kopfzentrum.gam.reports;

public record InvoiceReportRow(
  String invoiceDate,
  Integer companyId,
  String companyName,
  String documentType,
  String number,
  String username,
  String reason,
  Double gross,
  Boolean cancelled,
  Boolean creditNote,
  Boolean paymentAdvice
) {}
