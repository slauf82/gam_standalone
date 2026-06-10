package de.kopfzentrum.gam.invoice;

public record InvoicePortalInvoice(
  String number,
  String invoiceDate,
  Double totalGross,
  Integer companyId,
  String companyName,
  Boolean creditNote,
  Boolean cancelled,
  Boolean paymentAdvice
) {}
