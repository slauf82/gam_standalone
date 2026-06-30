package de.kopfzentrum.gam.warehouse;

public record WarehouseItemRequest(
  String kind,
  String name,
  String description,
  String location,
  Double quantity,
  String properties,
  String manufacturerEmail
) {}
