package de.kopfzentrum.gam.invoice.lbd;

import java.util.Map;

public record LbdRecipient(
  boolean found,
  String file,
  String patientNumber,
  String nameSuffix,
  String lastName,
  String firstName,
  String birthDate,
  String title,
  String insuranceNumber,
  String postalCode,
  String city,
  String country,
  String street,
  String insuranceType,
  Integer salutationIndex,
  String salutation,
  Map<String, String> rawFields
) {}
