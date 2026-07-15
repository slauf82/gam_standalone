package de.kopfzentrum.gam.invoice;

public record InvoiceWorkflowSettings(
  boolean reviewEnabled,
  boolean approvalEnabled,
  boolean shippingEnabled,
  boolean autoCompleteAfterShipping
) {
  public static InvoiceWorkflowSettings defaults() {
    return new InvoiceWorkflowSettings(true, true, true, false);
  }
}
