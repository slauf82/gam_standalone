package de.kopfzentrum.gam.invoice.zugferd;

public record ZugferdExportResult(
  String invoiceNumber,
  String profile,
  String filename,
  byte[] pdfBytes,
  byte[] xmlBytes,
  boolean zugferdEmbedded,
  boolean valid,
  String validationMessage
) {}
