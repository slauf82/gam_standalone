package de.kopfzentrum.gam.inventory;

public record DeviceAssignment(
  Integer id,
  Integer branchId,
  String branchCode,
  String branchName,
  Integer companyId,
  String companyName
) {}
