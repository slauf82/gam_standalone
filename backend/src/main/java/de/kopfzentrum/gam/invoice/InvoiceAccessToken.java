package de.kopfzentrum.gam.invoice;

import java.time.LocalDateTime;

public record InvoiceAccessToken(
  Long id,
  String invoiceNumber,
  Integer companyId,
  Integer addressId,
  String token,
  LocalDateTime createdAt,
  LocalDateTime expiresAt,
  boolean active,
  Integer accessCount,
  LocalDateTime lastAccess
) {}
