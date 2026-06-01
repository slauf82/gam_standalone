package de.kopfzentrum.gam.invoice.zugferd;

import de.kopfzentrum.gam.invoice.InvoiceCompany;
import de.kopfzentrum.gam.invoice.InvoiceDetail;
import de.kopfzentrum.gam.invoice.InvoiceLine;
import de.kopfzentrum.gam.invoice.InvoiceTotals;
import de.kopfzentrum.gam.invoice.lbd.LbdRecipient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ZugferdXmlService {
  private static final DateTimeFormatter GERMAN = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  @Value("${zugferd.seller-country:DE}") private String sellerCountry;
  @Value("${zugferd.buyer-country:DE}") private String buyerCountry;
  @Value("${zugferd.buyer-email:}") private String buyerEmail;

  public byte[] buildXml(InvoiceDetail invoice, InvoiceCompany company, LbdRecipient recipient, InvoiceTotals totals) {
    String number = safe(invoice.summary().number());
    String date = toIsoDate(invoice.summary().invoiceDate());
    StringBuilder xml = new StringBuilder(16_000);
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<rsm:CrossIndustryInvoice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100\" ");
    xml.append("xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100\" ");
    xml.append("xmlns:udt=\"urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100\">\n");
    xml.append("  <rsm:ExchangedDocumentContext>\n");
    xml.append("    <ram:GuidelineSpecifiedDocumentContextParameter><ram:ID>urn:cen.eu:en16931:2017</ram:ID></ram:GuidelineSpecifiedDocumentContextParameter>\n");
    xml.append("  </rsm:ExchangedDocumentContext>\n");
    xml.append("  <rsm:ExchangedDocument>\n");
    tag(xml,"ram:ID", number, 4);
    xml.append("    <ram:TypeCode>380</ram:TypeCode>\n");
    xml.append("    <ram:IssueDateTime><udt:DateTimeString format=\"102\">").append(date.replace("-", "")).append("</udt:DateTimeString></ram:IssueDateTime>\n");
    xml.append("  </rsm:ExchangedDocument>\n");
    xml.append("  <rsm:SupplyChainTradeTransaction>\n");
    int pos = 1;
    for (InvoiceLine line : invoice.lines()) {
      double quantity = line.quantity() == null ? 1.0 : line.quantity();
      double price = line.price() == null ? 0.0 : line.price();
      double net = round2(quantity * price / (1.0 + ((line.vat() == null ? 0 : line.vat()) / 100.0)));
      xml.append("    <ram:IncludedSupplyChainTradeLineItem>\n");
      xml.append("      <ram:AssociatedDocumentLineDocument><ram:LineID>").append(pos++).append("</ram:LineID></ram:AssociatedDocumentLineDocument>\n");
      xml.append("      <ram:SpecifiedTradeProduct>"); tag(xml,"ram:Name", nonBlank(line.description(), line.code()), 0); xml.append("</ram:SpecifiedTradeProduct>\n");
      xml.append("      <ram:SpecifiedLineTradeAgreement><ram:NetPriceProductTradePrice><ram:ChargeAmount>").append(money(price)).append("</ram:ChargeAmount></ram:NetPriceProductTradePrice></ram:SpecifiedLineTradeAgreement>\n");
      xml.append("      <ram:SpecifiedLineTradeDelivery><ram:BilledQuantity unitCode=\"C62\">").append(money(quantity)).append("</ram:BilledQuantity></ram:SpecifiedLineTradeDelivery>\n");
      xml.append("      <ram:SpecifiedLineTradeSettlement><ram:ApplicableTradeTax><ram:TypeCode>VAT</ram:TypeCode><ram:CategoryCode>S</ram:CategoryCode><ram:RateApplicablePercent>").append(line.vat() == null ? 0 : line.vat()).append("</ram:RateApplicablePercent></ram:ApplicableTradeTax><ram:SpecifiedTradeSettlementLineMonetarySummation><ram:LineTotalAmount>").append(money(net)).append("</ram:LineTotalAmount></ram:SpecifiedTradeSettlementLineMonetarySummation></ram:SpecifiedLineTradeSettlement>\n");
      xml.append("    </ram:IncludedSupplyChainTradeLineItem>\n");
    }
    xml.append("    <ram:ApplicableHeaderTradeAgreement>\n");
    xml.append("      <ram:SellerTradeParty>"); tag(xml,"ram:Name", nonBlank(company == null ? null : company.name(), "Kopfzentrum"), 0); address(xml, company == null ? null : company.street(), company == null ? null : company.city(), sellerCountry); xml.append("</ram:SellerTradeParty>\n");
    xml.append("      <ram:BuyerTradeParty>"); tag(xml,"ram:Name", recipientName(recipient), 0); if (buyerEmail != null && !buyerEmail.isBlank()) xml.append("<ram:URIUniversalCommunication><ram:URIID schemeID=\"EM\">").append(esc(buyerEmail)).append("</ram:URIID></ram:URIUniversalCommunication>"); address(xml, recipient == null ? null : recipient.street(), recipientCity(recipient), buyerCountry); xml.append("</ram:BuyerTradeParty>\n");
    xml.append("    </ram:ApplicableHeaderTradeAgreement>\n");
    xml.append("    <ram:ApplicableHeaderTradeDelivery/>\n");
    xml.append("    <ram:ApplicableHeaderTradeSettlement>\n");
    xml.append("      <ram:InvoiceCurrencyCode>EUR</ram:InvoiceCurrencyCode>\n");
    for (var e : totals.vatByRate().entrySet()) {
      xml.append("      <ram:ApplicableTradeTax><ram:CalculatedAmount>").append(money(e.getValue())).append("</ram:CalculatedAmount><ram:TypeCode>VAT</ram:TypeCode><ram:BasisAmount>").append(money(totals.net())).append("</ram:BasisAmount><ram:CategoryCode>S</ram:CategoryCode><ram:RateApplicablePercent>").append(e.getKey()).append("</ram:RateApplicablePercent></ram:ApplicableTradeTax>\n");
    }
    xml.append("      <ram:SpecifiedTradeSettlementHeaderMonetarySummation>");
    xml.append("<ram:LineTotalAmount>").append(money(totals.net())).append("</ram:LineTotalAmount>");
    xml.append("<ram:TaxBasisTotalAmount>").append(money(totals.net())).append("</ram:TaxBasisTotalAmount>");
    xml.append("<ram:TaxTotalAmount currencyID=\"EUR\">").append(money(totals.vat())).append("</ram:TaxTotalAmount>");
    xml.append("<ram:GrandTotalAmount>").append(money(totals.gross())).append("</ram:GrandTotalAmount>");
    xml.append("<ram:DuePayableAmount>").append(money(totals.gross())).append("</ram:DuePayableAmount>");
    xml.append("</ram:SpecifiedTradeSettlementHeaderMonetarySummation>\n");
    xml.append("    </ram:ApplicableHeaderTradeSettlement>\n");
    xml.append("  </rsm:SupplyChainTradeTransaction>\n");
    xml.append("</rsm:CrossIndustryInvoice>\n");
    return xml.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void address(StringBuilder xml, String street, String city, String country) {
    xml.append("<ram:PostalTradeAddress>");
    tag(xml,"ram:LineOne", street, 0);
    String[] parts = splitZipCity(city);
    tag(xml,"ram:PostcodeCode", parts[0], 0);
    tag(xml,"ram:CityName", parts[1], 0);
    tag(xml,"ram:CountryID", nonBlank(country, "DE"), 0);
    xml.append("</ram:PostalTradeAddress>");
  }
  private static String recipientName(LbdRecipient r) { return r == null ? "Unbekannter Rechnungsempfänger" : nonBlank((safe(r.salutation())+" "+safe(r.title())+" "+safe(r.firstName())+" "+safe(r.lastName())).trim(), "Unbekannter Rechnungsempfänger"); }
  private static String recipientCity(LbdRecipient r) { return r == null ? "" : (safe(r.postalCode()) + " " + safe(r.city())).trim(); }
  private static String[] splitZipCity(String s) { if (s == null) return new String[]{"", ""}; String v=s.trim(); if(v.matches("^[0-9]{5}\\s+.+")) return new String[]{v.substring(0,5), v.substring(6).trim()}; return new String[]{"", v}; }
  private static String toIsoDate(String s) { try { if (s != null && s.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) return LocalDate.parse(s, GERMAN).format(ISO); } catch (Exception ignored) {} return LocalDate.now().format(ISO); }
  private static void tag(StringBuilder xml, String name, String value, int indent) { if(value == null || value.isBlank()) return; if(indent>0) xml.append(" ".repeat(indent)); xml.append("<").append(name).append(">").append(esc(value)).append("</").append(name).append(">"); if(indent>0) xml.append("\n"); }
  private static String esc(String s) { return safe(s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
  private static String safe(String s) { return s == null ? "" : s; }
  private static String nonBlank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
  private static String money(double v) { return String.format(Locale.US, "%.2f", v); }
  private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
