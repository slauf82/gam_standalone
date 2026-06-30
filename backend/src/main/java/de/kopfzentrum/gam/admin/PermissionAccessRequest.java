package de.kopfzentrum.gam.admin;

public record PermissionAccessRequest(
  String username,
  String application,
  Integer companyId,
  String role
) {}
