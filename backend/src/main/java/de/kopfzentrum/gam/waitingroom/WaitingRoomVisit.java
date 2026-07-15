package de.kopfzentrum.gam.waitingroom;
import java.time.LocalDateTime;
public record WaitingRoomVisit(Long id,String patientNumber,String patientName,String appointmentType,String practitioner,String room,String priority,String status,LocalDateTime appointmentAt,LocalDateTime arrivedAt,LocalDateTime calledAt,LocalDateTime treatmentStartedAt,LocalDateTime completedAt,String nextStep,String note,String createdBy,LocalDateTime createdAt,String updatedBy,LocalDateTime updatedAt){}
