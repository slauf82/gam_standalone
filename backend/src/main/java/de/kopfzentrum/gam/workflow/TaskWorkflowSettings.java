package de.kopfzentrum.gam.workflow;

public record TaskWorkflowSettings(
  boolean enabled,
  boolean requireResponsiblePerson,
  boolean requireDueDate,
  boolean requireCompletionNote,
  boolean showOnDashboard,
  boolean overdueEscalationEnabled,
  int defaultDueDays,
  int warningDays
) {
  public static TaskWorkflowSettings defaults() {
    return new TaskWorkflowSettings(true, true, false, true, true, true, 7, 2);
  }
}
