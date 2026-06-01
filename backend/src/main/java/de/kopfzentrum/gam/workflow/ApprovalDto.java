package de.kopfzentrum.gam.workflow;

public record ApprovalDto(
  Integer id,
  String date,
  String creator,
  String description,
  Integer companyId,
  Integer branchId,
  String status,
  String note
) {}
