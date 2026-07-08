package de.kopfzentrum.gam.invoice;

/**
 * Rechnungsprodukt mit stichtagsbezogen aufgeloestem Preis/MwSt.
 * price/vat enthalten bewusst die fuer das angefragte Rechnungsdatum wirksamen Werte,
 * damit die Rechnungserfassung diese Werte direkt in die Position uebernimmt und damit einfriert.
 */
public record ProductDto(
  Integer id,
  String code,
  String description,
  String category,
  Double price,
  Integer vat,
  Integer companyId,
  Double basePrice,
  Double newPrice,
  Double oldPrice,
  String priceValidFrom,
  Integer oldVat,
  String vatValidFrom,
  String validFrom,
  String validUntil,
  Boolean available,
  String effectiveNote
) {}
