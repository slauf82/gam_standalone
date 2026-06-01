package de.kopfzentrum.gam.link;

public record MaterialBookingRequest(
  Integer deviceId,
  Integer materialId,
  Integer quantity,
  String direction,
  String reason,
  String username
) {}
