package de.kopfzentrum.gam.invoice;

import java.util.List;
public record InvoiceDetail(InvoiceSummary summary, List<InvoiceLine> lines, InvoiceTotals totals) {
  public InvoiceDetail(InvoiceSummary summary, List<InvoiceLine> lines) {
    this(summary, lines, null);
  }
}
