package de.kopfzentrum.gam.warehouse;

public record StockChangeRequest(String kind, Integer id, Double quantity, String reason) {}
