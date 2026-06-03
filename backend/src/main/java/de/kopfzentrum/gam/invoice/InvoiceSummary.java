package de.kopfzentrum.gam.invoice;

public record InvoiceSummary(
  Integer id,
  String number,
  String invoiceDate,
  Double totalGross,
  Integer companyId,
  String companyName,
  String username,
  Boolean creditNote,
  Boolean cancelled,
  Boolean paymentAdvice,
  Double couponAmount,
  Integer discountPercent,
  String discountRemark,
  Integer installments
) {}
