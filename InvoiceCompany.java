package de.kopfzentrum.gam.invoice;

public record ProductDto(Integer id, String code, String description, String category, Double price, Integer vat, Integer companyId) {}
