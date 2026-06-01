package de.kopfzentrum.gam.workflow;

public record TaskDto(
  Integer id,
  String username,
  String date,
  String branchCode,
  Integer branchId,
  String department,
  String task,
  String responsible,
  String priority,
  String status,
  String dueDate,
  String doneBy,
  String note
) {}
