package de.kopfzentrum.gam.auth;

public record TotpConfirmRequest(String username, String secret, String totpCode) {}
