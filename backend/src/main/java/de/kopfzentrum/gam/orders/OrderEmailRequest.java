package de.kopfzentrum.gam.orders;

import java.util.List;

public record OrderEmailRequest(
    String to,
    String subject,
    String text,
    Long draftId,
    Long workflowTaskId,
    List<OrderEmailLine> lines,
    OrderSmtpConfig smtp
) {
  public record OrderEmailLine(Long materialId, String name, Integer quantity, Integer stock) {}
}
