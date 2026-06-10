package de.kopfzentrum.gam.invoice;

import java.util.List;

public record InvoicePortalResponse(
  String token,
  String currentInvoiceNumber,
  Integer companyId,
  String companyName,
  Integer addressId,
  List<InvoicePortalInvoice> invoices
) {}
