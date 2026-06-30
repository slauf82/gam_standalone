package de.kopfzentrum.gam.admin;

public record AccountCreateRequest(String username, String password, String fullname, String role, String email) {}
