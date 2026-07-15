package de.kopfzentrum.gam.settings;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ComplianceWorkflowSettingsRepository {
 private final JdbcTemplate jdbc;
 public ComplianceWorkflowSettingsRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @PostConstruct void init(){
  jdbc.execute("""
   CREATE TABLE IF NOT EXISTS gam_settings (
    setting_key VARCHAR(120) NOT NULL,
    setting_value TEXT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (setting_key)
   )
   """);
  var d=ComplianceWorkflowSettings.defaults();
  putDefault("compliance.workflow.enabled",d.enabled()); putDefault("compliance.workflow.warningDays",d.warningDays());
  putDefault("compliance.workflow.overdueEscalationEnabled",d.overdueEscalationEnabled()); putDefault("compliance.workflow.automaticFollowUpDate",d.automaticFollowUpDate());
  putDefault("compliance.workflow.requireResponsiblePerson",d.requireResponsiblePerson()); putDefault("compliance.workflow.requireResultNote",d.requireResultNote());
  putDefault("compliance.workflow.createTaskOnWarning",d.createTaskOnWarning()); putDefault("compliance.workflow.createTaskWhenOverdue",d.createTaskWhenOverdue());
  putDefault("compliance.workflow.notifyOnWarning",d.notifyOnWarning()); putDefault("compliance.workflow.notifyWhenOverdue",d.notifyWhenOverdue());
  putDefault("compliance.workflow.showStatusInNavigation",d.showStatusInNavigation()); putDefault("compliance.workflow.showStatusOnDashboard",d.showStatusOnDashboard());
 }
 private void putDefault(String k,Object v){jdbc.update("INSERT IGNORE INTO gam_settings(setting_key,setting_value,updated_by) VALUES(?,?,'system')",k,String.valueOf(v));}
 public ComplianceWorkflowSettings load(){var d=ComplianceWorkflowSettings.defaults();return new ComplianceWorkflowSettings(
  bool("compliance.workflow.enabled",d.enabled()),integer("compliance.workflow.warningDays",d.warningDays()),
  bool("compliance.workflow.overdueEscalationEnabled",d.overdueEscalationEnabled()),bool("compliance.workflow.automaticFollowUpDate",d.automaticFollowUpDate()),
  bool("compliance.workflow.requireResponsiblePerson",d.requireResponsiblePerson()),bool("compliance.workflow.requireResultNote",d.requireResultNote()),
  bool("compliance.workflow.createTaskOnWarning",d.createTaskOnWarning()),bool("compliance.workflow.createTaskWhenOverdue",d.createTaskWhenOverdue()),
  bool("compliance.workflow.notifyOnWarning",d.notifyOnWarning()),bool("compliance.workflow.notifyWhenOverdue",d.notifyWhenOverdue()),
  bool("compliance.workflow.showStatusInNavigation",d.showStatusInNavigation()),bool("compliance.workflow.showStatusOnDashboard",d.showStatusOnDashboard()));}
 public ComplianceWorkflowSettings save(ComplianceWorkflowSettings v,String user){put("compliance.workflow.enabled",v.enabled(),user);put("compliance.workflow.warningDays",Math.max(1,v.warningDays()),user);put("compliance.workflow.overdueEscalationEnabled",v.overdueEscalationEnabled(),user);put("compliance.workflow.automaticFollowUpDate",v.automaticFollowUpDate(),user);put("compliance.workflow.requireResponsiblePerson",v.requireResponsiblePerson(),user);put("compliance.workflow.requireResultNote",v.requireResultNote(),user);put("compliance.workflow.createTaskOnWarning",v.createTaskOnWarning(),user);put("compliance.workflow.createTaskWhenOverdue",v.createTaskWhenOverdue(),user);put("compliance.workflow.notifyOnWarning",v.notifyOnWarning(),user);put("compliance.workflow.notifyWhenOverdue",v.notifyWhenOverdue(),user);put("compliance.workflow.showStatusInNavigation",v.showStatusInNavigation(),user);put("compliance.workflow.showStatusOnDashboard",v.showStatusOnDashboard(),user);return load();}
 private void put(String k,Object v,String u){jdbc.update("INSERT INTO gam_settings(setting_key,setting_value,updated_by) VALUES(?,?,?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value),updated_by=VALUES(updated_by),updated_at=CURRENT_TIMESTAMP",k,String.valueOf(v),u);}
 private String value(String k){var x=jdbc.query("SELECT setting_value FROM gam_settings WHERE setting_key=?",(ResultSet r,int n)->r.getString(1),k);return x.isEmpty()?null:x.get(0);}
 private boolean bool(String k,boolean f){var v=value(k);return v==null?f:Boolean.parseBoolean(v);} private int integer(String k,int f){try{var v=value(k);return v==null?f:Integer.parseInt(v);}catch(Exception e){return f;}}
}
