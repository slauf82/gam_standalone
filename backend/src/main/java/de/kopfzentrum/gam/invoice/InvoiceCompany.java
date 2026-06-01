package de.kopfzentrum.gam.invoice;

public record InvoiceCompany(
  Integer id,
  String code,
  String name,
  String address,
  String street,
  String city,
  String vatId,
  String register,
  String taxNumber,
  String court,
  String manager,
  String contact,
  String phone,
  String fax,
  String email,
  String accountHolder,
  String iban,
  String bic
) {}
