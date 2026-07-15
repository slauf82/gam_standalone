package de.kopfzentrum.gam.invoice;

import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;

/**
 * Zentrale, fachlich aufgeloeste Dokumentdaten einer Rechnung.
 *
 * <p>Rollen aus GAM 1.0:</p>
 * <ul>
 *   <li>ADRESSID: erwachsene Hauptperson bzw. Elternteil/Ansprechpartner</li>
 *   <li>KINDADRESSID: behandeltes Kind</li>
 *   <li>FIRMAADRESSID: Firma/Kostenuebernehmer</li>
 *   <li>RDATUM: Rechnungsdatum</li>
 *   <li>BDATUM: Behandlungsdatum</li>
 * </ul>
 */
public record InvoiceDocumentData(
    String invoiceNumber,
    Integer companyId,
    Integer addressId,
    Integer childAddressId,
    Integer companyAddressId,
    String invoiceDate,
    String treatmentDate,
    Double couponAmount,
    String couponRemark,
    Integer discountPercent,
    String discountRemark,
    String paymentMethod,
    String payerRole,
    LbdRecipient recipient,
    LbdRecipient treatedChild,
    LbdRecipient company,
    LbdRecipient invoiceRecipient,
    LbdRecipient treatedPerson,
    String email,
    boolean addressResolvedFromMasterData
) {
  public boolean childCase() { return childAddressId != null && treatedChild != null && treatedChild.found(); }
  public boolean companyCase() { return companyAddressId != null && company != null && company.found(); }
}
