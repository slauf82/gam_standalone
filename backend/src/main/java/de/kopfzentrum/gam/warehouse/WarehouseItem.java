package de.kopfzentrum.gam.warehouse;

public record WarehouseItem(
  Integer id,
  String kind,
  String name,
  String description,
  String location,
  Double quantity,
  String properties,
  String manufacturerEmail,
  Long linkedDeviceCount
) {}
