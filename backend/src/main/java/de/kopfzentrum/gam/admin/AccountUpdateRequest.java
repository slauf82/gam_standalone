package de.kopfzentrum.gam.admin;

public record AccountUpdateRequest(String fullname, String role, String email, String secretkey) {}
