package de.kopfzentrum.gam.invoice;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceCalculator {
  public InvoiceTotals calculate(List<InvoiceCreateLineRequest> lines) {
    Map<Integer, Double> vatByRate = new LinkedHashMap<>();
    double gross = 0.0;
    double vat = 0.0;
    if (lines != null) {
      for (InvoiceCreateLineRequest line : lines) {
        double quantity = line.quantity() == null ? 1.0 : line.quantity();
        double price = line.price() == null ? 0.0 : line.price();
        int rate = line.vat() == null ? 0 : line.vat();
        double lineGross = quantity * price;
        double lineVat = rate <= 0 ? 0.0 : lineGross - (lineGross / (1.0 + (rate / 100.0)));
        gross += lineGross;
        vat += lineVat;
        vatByRate.merge(rate, round(lineVat), Double::sum);
      }
    }
    return new InvoiceTotals(round(gross - vat), round(vat), round(gross), vatByRate);
  }
  private static double round(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue(); }
}
