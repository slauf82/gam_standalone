package de.kopfzentrum.gam.auth;

import java.util.List;

public record RoleDto(String key, String label, boolean administrative, List<String> modules) {}
