package de.kopfzentrum.gam.auth;

public record LoginRequest(String username, String password, String totpCode) {}
