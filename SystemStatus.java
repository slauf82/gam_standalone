package de.kopfzentrum.gam.invoice.zugferd;

public record ZugferdStatus(
  boolean enabled,
  String profile,
  boolean validationEnabled,
  String note
) {}
