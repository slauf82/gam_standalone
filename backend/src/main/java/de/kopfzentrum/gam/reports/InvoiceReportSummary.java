package de.kopfzentrum.gam.reports;

public record InvoiceReportSummary(
  long count,
  double grossTotal,
  long invoices,
  long cancellations,
  long creditNotes,
  long paymentAdvices,
  long proforma,
  String note
) {}
