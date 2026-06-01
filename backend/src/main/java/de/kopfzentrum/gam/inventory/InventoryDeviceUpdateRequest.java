package de.kopfzentrum.gam.inventory;

public record InventoryDeviceUpdateRequest(
  String name,
  String type,
  String serialNumber,
  String inventoryNumber,
  String manufacturer,
  String ip,
  String location,
  Integer branchId,
  Boolean medicalDevice,
  Boolean electricalDevice,
  Boolean inventory,
  Boolean active,
  Boolean inUse,
  String acquisitionDate,
  String note
) {}
