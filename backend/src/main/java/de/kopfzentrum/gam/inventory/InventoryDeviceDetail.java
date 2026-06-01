package de.kopfzentrum.gam.inventory;

import java.util.List;

public record InventoryDeviceDetail(
  InventoryDevice device,
  List<DeviceAssignment> assignments,
  List<DeviceConsumable> consumables,
  List<DeviceSoftware> software
) {}
