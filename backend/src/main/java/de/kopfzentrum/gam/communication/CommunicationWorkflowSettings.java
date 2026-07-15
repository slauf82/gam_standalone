package de.kopfzentrum.gam.communication;

public record CommunicationWorkflowSettings(
    boolean enabled,
    boolean requireResponsiblePerson,
    boolean requireDueDate,
    boolean createTaskForFollowUp,
    boolean notifyResponsiblePerson,
    boolean showOnDashboard,
    int warningDays,
    int defaultDueDays
) {
  public static CommunicationWorkflowSettings defaults() {
    return new CommunicationWorkflowSettings(true, true, false, true, true, true, 2, 3);
  }
}
