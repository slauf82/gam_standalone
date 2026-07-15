package de.kopfzentrum.gam.settings;

public record ComplianceWorkflowSettings(
 boolean enabled,
 int warningDays,
 boolean overdueEscalationEnabled,
 boolean automaticFollowUpDate,
 boolean requireResponsiblePerson,
 boolean requireResultNote,
 boolean createTaskOnWarning,
 boolean createTaskWhenOverdue,
 boolean notifyOnWarning,
 boolean notifyWhenOverdue,
 boolean showStatusInNavigation,
 boolean showStatusOnDashboard
) {
 public static ComplianceWorkflowSettings defaults(){return new ComplianceWorkflowSettings(true,14,true,true,false,false,true,true,true,true,true,true);}
}
