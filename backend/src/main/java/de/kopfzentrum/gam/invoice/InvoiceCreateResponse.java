package de.kopfzentrum.gam.invoice;

public record InvoiceCreateResponse(
  String number,
  Integer detailId,
  InvoiceTotals totals,
  InvoiceDetail invoice
) {}
