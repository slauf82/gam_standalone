package de.kopfzentrum.gam.invoice;

import java.util.Map;

public record InvoiceTextPreview(
  String language,
  Integer companyId,
  String documentTitle,
  String salutation,
  String invoiceText,
  String lawHint,
  String greetings,
  Map<String, String> labels,
  de.kopfzentrum.gam.invoice.lbd.LbdRecipient recipient,
  String treatmentDate,
  String paymentMethod
) {}
