package de.kopfzentrum.gam.warehouse;

public record WarehouseStats(
  long storageItemCount,
  long consumableCount,
  long consumablesWithStock,
  long consumablesWithoutStock,
  long deviceConsumableLinks,
  double totalStorageQuantity,
  double totalConsumableQuantity
) {}
