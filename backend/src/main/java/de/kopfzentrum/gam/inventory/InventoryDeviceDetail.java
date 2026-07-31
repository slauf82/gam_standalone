package de.kopfzentrum.gam.inventory;

import java.util.List;
import java.util.Map;

/**
 * 40k34t: additiv erweitert um die verknüpfte Discovery-/Plattforminventarisierungs-
 * Sicht - EIN gemeinsames DTO statt mehrerer Einzelabfragen pro Gerät. `identityKey`
 * ist `null`, wenn (noch) keine eindeutige Verknüpfung zur Discovery-Identität
 * besteht (siehe InventoryIdentityLinkRepository) - in diesem Fall bleiben auch
 * `platform`, `platformStatus` und `discoveryProtocol` leer/null, ohne Fehler.
 */
public record InventoryDeviceDetail(
  InventoryDevice device,
  List<DeviceAssignment> assignments,
  List<DeviceConsumable> consumables,
  List<DeviceSoftware> software,
  String identityKey,
  String platform,
  List<Map<String,Object>> platformStatus,
  String discoveryProtocol
) {}
