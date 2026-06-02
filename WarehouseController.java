package de.kopfzentrum.gam.inventory;

public record InventoryDevice(
  Integer id,
  String source,
  String name,
  String type,
  String serialNumber,
  String inventoryNumber,
  String manufacturer,
  String ip,
  String location,
  Integer branchId,
  String branchCode,
  String branchName,
  Boolean medicalDevice,
  Boolean electricalDevice,
  Boolean inventoryRelevant,
  Boolean active,
  Boolean inUse,
  String acquiredAt,
  String note
) {}
