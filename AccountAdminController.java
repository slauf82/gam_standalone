package de.kopfzentrum.gam.invoice;

import java.util.List;

public record InvoiceExportCheck(
  String number,
  boolean exportable,
  List<InvoiceValidationIssue> issues
) {}
