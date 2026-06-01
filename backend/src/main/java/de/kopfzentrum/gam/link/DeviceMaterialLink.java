package de.kopfzentrum.gam.link;

public record DeviceMaterialLink(
  Integer linkId,
  Integer deviceId,
  String deviceName,
  Integer materialId,
  String materialName,
  String materialType,
  Integer stock,
  String manufacturerEmail
) {}
