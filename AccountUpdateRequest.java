package de.kopfzentrum.gam.invoice;

public record InvoiceNumberPreview(
  String nextNumber,
  String currentMaxNumber,
  boolean numericSequence,
  String note
) {}
