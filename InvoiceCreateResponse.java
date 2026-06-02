package de.kopfzentrum.gam.invoice;

public record InvoiceLine(
  Integer id,
  String number,
  Double quantity,
  Integer productId,
  String code,
  String description,
  Integer vat,
  Double price,
  Integer branchId,
  String client,
  String performer
) {}
