package de.kopfzentrum.gam.invoice;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PaymentWorkflowEntry(Long id,String invoiceNumber,Integer companyId,String fromStatus,String toStatus,BigDecimal amount,String note,String changedBy,LocalDateTime changedAt) {}
