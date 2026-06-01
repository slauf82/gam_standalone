package de.kopfzentrum.gam.auth;

public record LoginResponse(
    String token,
    AccountDto account,
    String loginMode,
    long expiresInMinutes
) {}
