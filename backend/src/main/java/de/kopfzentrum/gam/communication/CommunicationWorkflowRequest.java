package de.kopfzentrum.gam.communication;
public record CommunicationWorkflowRequest(String channel,String direction,String sender,String recipient,String subject,String message,String responsible,String priority,String status,String dueDate,String resultNote){}
