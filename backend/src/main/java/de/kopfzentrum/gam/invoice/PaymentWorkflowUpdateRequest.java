package de.kopfzentrum.gam.invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PaymentWorkflowUpdateRequest(String action,BigDecimal amount,BigDecimal amountDue,LocalDate dueDate,String note) {}
