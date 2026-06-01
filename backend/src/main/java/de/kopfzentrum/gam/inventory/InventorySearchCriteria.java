package de.kopfzentrum.gam.inventory;

public record InventorySearchCriteria(String q, String source, Integer branchId, Boolean activeOnly, int limit, int offset) {}
