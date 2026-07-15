package de.kopfzentrum.gam.waitingroom;
public record WaitingRoomVisitRequest(String patientNumber,String patientName,String appointmentType,String practitioner,String room,String priority,String status,String appointmentAt,String nextStep,String note){}
