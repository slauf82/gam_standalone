package de.kopfzentrum.gam.inventory;

public record InventoryStats(
  long legacyDeviceCount,
  long newDeviceCount,
  long branchAssignmentCount,
  long medicalDeviceCount,
  long electricalDeviceCount,
  long outOfServiceCount
) {}
