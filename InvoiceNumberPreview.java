package de.kopfzentrum.gam.invoice;

import java.util.Map;

public record InvoiceTotals(
  double net,
  double vat,
  double gross,
  Map<Integer, Double> vatByRate
) {}
