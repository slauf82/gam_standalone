package de.kopfzentrum.gam.communication;
import java.time.LocalDate;import java.time.LocalDateTime;
public record CommunicationWorkflowItem(Long id,String channel,String direction,String sender,String recipient,String subject,String message,String responsible,String priority,String status,LocalDate dueDate,String resultNote,String createdBy,LocalDateTime createdAt,String updatedBy,LocalDateTime updatedAt){}
