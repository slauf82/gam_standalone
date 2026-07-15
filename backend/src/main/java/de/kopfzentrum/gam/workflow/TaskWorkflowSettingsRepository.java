package de.kopfzentrum.gam.workflow;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TaskWorkflowSettingsRepository {
  private final JdbcTemplate jdbc;
  public TaskWorkflowSettingsRepository(JdbcTemplate jdbc) {
    this.jdbc=jdbc;
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS task_workflow_settings (
        setting_key VARCHAR(120) PRIMARY KEY,
        setting_value VARCHAR(255) NULL,
        updated_by VARCHAR(100) NULL,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
      )
      """);
  }
  private String get(String key,String fallback){var r=jdbc.query("SELECT setting_value FROM task_workflow_settings WHERE setting_key=?",(rs,n)->rs.getString(1),key);return r.isEmpty()?fallback:r.get(0);}
  private boolean bool(String key,boolean fallback){return Boolean.parseBoolean(get(key,String.valueOf(fallback)));}
  private int integer(String key,int fallback){try{return Integer.parseInt(get(key,String.valueOf(fallback)));}catch(Exception e){return fallback;}}
  private void put(String key,Object value,String user){jdbc.update("INSERT INTO task_workflow_settings(setting_key,setting_value,updated_by) VALUES(?,?,?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value),updated_by=VALUES(updated_by)",key,String.valueOf(value),user);}
  public TaskWorkflowSettings load(){var d=TaskWorkflowSettings.defaults();return new TaskWorkflowSettings(bool("enabled",d.enabled()),bool("requireResponsiblePerson",d.requireResponsiblePerson()),bool("requireDueDate",d.requireDueDate()),bool("requireCompletionNote",d.requireCompletionNote()),bool("showOnDashboard",d.showOnDashboard()),bool("overdueEscalationEnabled",d.overdueEscalationEnabled()),Math.max(0,integer("defaultDueDays",d.defaultDueDays())),Math.max(0,integer("warningDays",d.warningDays())));}
  public TaskWorkflowSettings save(TaskWorkflowSettings v,String user){put("enabled",v.enabled(),user);put("requireResponsiblePerson",v.requireResponsiblePerson(),user);put("requireDueDate",v.requireDueDate(),user);put("requireCompletionNote",v.requireCompletionNote(),user);put("showOnDashboard",v.showOnDashboard(),user);put("overdueEscalationEnabled",v.overdueEscalationEnabled(),user);put("defaultDueDays",Math.max(0,v.defaultDueDays()),user);put("warningDays",Math.max(0,v.warningDays()),user);return load();}
}
