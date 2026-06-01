package de.kopfzentrum.gam.link;

public record MaterialBookingResult(
  Integer materialId,
  String materialName,
  Integer previousStock,
  Integer delta,
  Integer newStock,
  Integer deviceId,
  String deviceName,
  String reason
) {}
