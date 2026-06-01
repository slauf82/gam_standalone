package de.kopfzentrum.gam.auth;

public record LoginDecision(Account account, LoginMode mode) {}
