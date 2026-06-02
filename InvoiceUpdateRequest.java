package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import java.util.List;

public record InvoiceDraft(
  String suggestedNumber,
  String invoiceDate,
  List<InvoiceCompany> companies,
  LbdRecipient lbdRecipient,
  InvoiceTotals totals
) {}
