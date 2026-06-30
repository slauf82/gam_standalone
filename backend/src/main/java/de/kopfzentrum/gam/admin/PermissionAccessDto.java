package de.kopfzentrum.gam.admin;

public record PermissionAccessDto(
  Integer id,
  String username,
  String application,
  Integer companyId,
  String role
) {}
