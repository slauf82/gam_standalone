package de.kopfzentrum.gam.auth;

public record TotpSetupResponse(String username, String secret, String otpauthUri, boolean alreadyConfigured) {}
