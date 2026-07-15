package de.kopfzentrum.gam.communication;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CommunicationWorkflowSettingsRepository {
  private final JdbcTemplate jdbc;
  public CommunicationWorkflowSettingsRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS communication_workflow_settings (
        setting_key VARCHAR(120) PRIMARY KEY,
        setting_value VARCHAR(255) NULL,
        updated_by VARCHAR(100) NULL,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
      )
      """);
  }
  private String get(String key, String fallback) {
    var rows=jdbc.query("SELECT setting_value FROM communication_workflow_settings WHERE setting_key=?",(rs,n)->rs.getString(1),key);
    return rows.isEmpty()?fallback:rows.get(0);
  }
  private boolean bool(String key, boolean fallback){return Boolean.parseBoolean(get(key,String.valueOf(fallback)));}
  private int integer(String key,int fallback){try{return Integer.parseInt(get(key,String.valueOf(fallback)));}catch(Exception e){return fallback;}}
  private void put(String key,Object value,String user){jdbc.update("INSERT INTO communication_workflow_settings(setting_key,setting_value,updated_by) VALUES(?,?,?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value),updated_by=VALUES(updated_by)",key,String.valueOf(value),user);}
  public CommunicationWorkflowSettings load(){var d=CommunicationWorkflowSettings.defaults();return new CommunicationWorkflowSettings(bool("enabled",d.enabled()),bool("requireResponsiblePerson",d.requireResponsiblePerson()),bool("requireDueDate",d.requireDueDate()),bool("createTaskForFollowUp",d.createTaskForFollowUp()),bool("notifyResponsiblePerson",d.notifyResponsiblePerson()),bool("showOnDashboard",d.showOnDashboard()),Math.max(0,integer("warningDays",d.warningDays())),Math.max(0,integer("defaultDueDays",d.defaultDueDays())));}
  public CommunicationWorkflowSettings save(CommunicationWorkflowSettings v,String user){put("enabled",v.enabled(),user);put("requireResponsiblePerson",v.requireResponsiblePerson(),user);put("requireDueDate",v.requireDueDate(),user);put("createTaskForFollowUp",v.createTaskForFollowUp(),user);put("notifyResponsiblePerson",v.notifyResponsiblePerson(),user);put("showOnDashboard",v.showOnDashboard(),user);put("warningDays",Math.max(0,v.warningDays()),user);put("defaultDueDays",Math.max(0,v.defaultDueDays()),user);return load();}
}
