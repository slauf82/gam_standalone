package de.kopfzentrum.gam.invoice;

public record InvoiceCreateLineRequest(
  Integer productId,
  Double quantity,
  Double price,
  Integer vat,
  Integer branchId,
  String client,
  String performer
) {}
