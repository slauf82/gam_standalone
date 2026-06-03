package de.kopfzentrum.gam.invoice;

import java.util.List;

/**
 * Schritt 8: Bearbeitungsdaten fuer bestehende Rechnungen.
 * Bewusst kompatibel zu InvoiceCreateRequest gehalten, damit Create/Edit denselben Workflow nutzen.
 */
public record InvoiceUpdateRequest(
  String invoiceDate,
  String treatmentDate,
  Integer companyId,
  Integer addressId,
  Integer childAddressId,
  Integer firmAddressId,
  Integer branchId,
  String paymentMethod,
  String reason,
  String remark,
  Boolean creditNote,
  Boolean cancelled,
  Boolean paymentAdvice,
  String couponText,
  Double couponAmount,
  String discountType,
  Double discountValue,
  Integer installments,
  String lbdFile,
  List<InvoiceCreateLineRequest> lines
) {}
