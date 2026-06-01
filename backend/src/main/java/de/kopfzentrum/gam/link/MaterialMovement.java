package de.kopfzentrum.gam.link;

import java.time.LocalDateTime;

public record MaterialMovement(
  Long id,
  LocalDateTime createdAt,
  Integer deviceId,
  String deviceName,
  Integer materialId,
  String materialName,
  Integer delta,
  Integer previousStock,
  Integer newStock,
  String reason,
  String username
) {}
