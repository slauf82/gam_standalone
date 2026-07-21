package de.kopfzentrum.gam.inventory;

public record DiscoveredDevice(
    String id,
    String name,
    String type,
    String address,
    String hardwareAddress,
    String protocol,
    String status,
    String manufacturer,
    String serialNumber,
    String lastSeen,
    boolean alreadyRegistered
) {}
