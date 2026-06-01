package de.kopfzentrum.gam.warehouse;

public record WarehouseSearchCriteria(String q, String kind, Boolean onlyWithStock, int limit, int offset) {}
