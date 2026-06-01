package de.kopfzentrum.gam.invoice;

/** Markiert Sonderfaelle ohne die Rechnung physisch zu loeschen. */
public record InvoiceStatusUpdateRequest(
  Boolean cancelled,
  Boolean creditNote,
  Boolean paymentAdvice,
  String reason
) {}
