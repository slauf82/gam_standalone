package de.kopfzentrum.gam.inventory;

public record DeviceConsumable(
  Integer id,
  Integer materialId,
  String name,
  String properties,
  Integer amount,
  String manufacturerEmail
) {}
