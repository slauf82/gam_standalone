package de.kopfzentrum.gam.invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
public record PaymentWorkflowState(String invoiceNumber,Integer companyId,String status,BigDecimal amountDue,BigDecimal paidAmount,BigDecimal openAmount,LocalDate dueDate,LocalDateTime updatedAt,String updatedBy,List<String> allowedActions,List<PaymentWorkflowEntry> history) {}
