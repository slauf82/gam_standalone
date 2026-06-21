package de.kopfzentrum.gam.invoice;

/**
 * Schritt 36g: editierbarer Rechnungsempfaenger.
 * Daten koennen aus LBD stammen, vor Uebernahme angepasst oder komplett manuell erfasst werden.
 */
public record InvoiceRecipientRequest(
  Boolean manual,
  String salutation,
  String title,
  String firstName,
  String lastName,
  String nameSuffix,
  String street,
  String postalCode,
  String city,
  String country,
  String email,
  String patientNumber,
  String lbdFile
) {}
