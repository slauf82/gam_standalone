package de.kopfzentrum.gam.workflow;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
public class WorkflowRepository {
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate named;
  private final TaskWorkflowSettingsRepository taskSettings;

  public WorkflowRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, TaskWorkflowSettingsRepository taskSettings) {
    this.jdbc = jdbc;
    this.named = named;
    this.taskSettings = taskSettings;
  }

  public List<TaskDto> tasks(String q, String status, Integer branchId, int limit) {
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", normalizeLimit(limit));
    StringBuilder sql = new StringBuilder("""
      SELECT `ID`,`USERNAME`,`TAGESDATUM`,`KÜRZEL`,`FILIALE_ID`,`FACHBEREICH`,`AUFGABE`,`VERANTWORTLICHER`,`PRIORITÄT`,`STATUS`,`FRIST`,`ERLEDIGT`,`BEMERKUNG`
      FROM `aufgaben` WHERE 1=1
      """);
    if (q != null && !q.isBlank()) { sql.append(" AND (`AUFGABE` LIKE :q OR `BEMERKUNG` LIKE :q OR `USERNAME` LIKE :q OR `VERANTWORTLICHER` LIKE :q) "); p.addValue("q", "%" + q.trim() + "%"); }
    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) { sql.append(" AND COALESCE(`STATUS`,'') = :status "); p.addValue("status", status); }
    if (branchId != null) { sql.append(" AND `FILIALE_ID` = :branchId "); p.addValue("branchId", branchId); }
    sql.append(" ORDER BY COALESCE(`FRIST`,`TAGESDATUM`) DESC, `ID` DESC LIMIT :limit");
    return named.query(sql.toString(), p, (rs, row) -> mapTask(rs));
  }

  public TaskDto task(Integer id) {
    return jdbc.queryForObject("""
      SELECT `ID`,`USERNAME`,`TAGESDATUM`,`KÜRZEL`,`FILIALE_ID`,`FACHBEREICH`,`AUFGABE`,`VERANTWORTLICHER`,`PRIORITÄT`,`STATUS`,`FRIST`,`ERLEDIGT`,`BEMERKUNG`
      FROM `aufgaben` WHERE `ID` = ?
      """, (rs, row) -> mapTask(rs), id);
  }

  public TaskDto createTask(TaskUpdateRequest r, String currentUser) {
    TaskUpdateRequest v = validateTask(r);
    jdbc.update("""
      INSERT INTO `aufgaben` (`USERNAME`,`TAGESDATUM`,`KÜRZEL`,`FILIALE_ID`,`FACHBEREICH`,`AUFGABE`,`VERANTWORTLICHER`,`PRIORITÄT`,`STATUS`,`FRIST`,`ERLEDIGT`,`BEMERKUNG`)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
      """, valueOr(v.username(), currentUser), LocalDate.now(), v.branchCode(), v.branchId(), v.department(), v.task(), v.responsible(), valueOr(v.priority(), "mittel"), valueOr(v.status(), "offen"), parseDate(v.dueDate()), v.doneBy(), v.note());
    Integer id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    return task(id);
  }

  public TaskDto updateTask(Integer id, TaskUpdateRequest r) {
    TaskUpdateRequest v = validateTask(r);
    jdbc.update("""
      UPDATE `aufgaben` SET `KÜRZEL`=?, `FILIALE_ID`=?, `FACHBEREICH`=?, `AUFGABE`=?, `VERANTWORTLICHER`=?, `PRIORITÄT`=?, `STATUS`=?, `FRIST`=?, `ERLEDIGT`=?, `BEMERKUNG`=?
      WHERE `ID`=?
      """, v.branchCode(), v.branchId(), v.department(), v.task(), v.responsible(), v.priority(), v.status(), parseDate(v.dueDate()), v.doneBy(), v.note(), id);
    return task(id);
  }

  public void deleteTask(Integer id) {
    jdbc.update("DELETE FROM `aufgaben` WHERE `ID`=?", id);
  }

  public List<ApprovalDto> approvals(String q, String status, Integer branchId, int limit) {
    MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", normalizeLimit(limit));
    StringBuilder sql = new StringBuilder("""
      SELECT `ID`,`DATUM`,`EINTRAGENDER`,`BESCHREIBUNG`,`GESELLSCHAFT_ID`,`FILIALE_ID`,`STATUS`,`BEMERKUNG`
      FROM `freigabe` WHERE 1=1
      """);
    if (q != null && !q.isBlank()) { sql.append(" AND (`BESCHREIBUNG` LIKE :q OR `BEMERKUNG` LIKE :q OR `EINTRAGENDER` LIKE :q) "); p.addValue("q", "%" + q.trim() + "%"); }
    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) { sql.append(" AND COALESCE(`STATUS`,'') = :status "); p.addValue("status", status); }
    if (branchId != null) { sql.append(" AND `FILIALE_ID` = :branchId "); p.addValue("branchId", branchId); }
    sql.append(" ORDER BY `DATUM` DESC, `ID` DESC LIMIT :limit");
    return named.query(sql.toString(), p, (rs, row) -> mapApproval(rs));
  }

  public ApprovalDto approval(Integer id) {
    return jdbc.queryForObject("""
      SELECT `ID`,`DATUM`,`EINTRAGENDER`,`BESCHREIBUNG`,`GESELLSCHAFT_ID`,`FILIALE_ID`,`STATUS`,`BEMERKUNG`
      FROM `freigabe` WHERE `ID` = ?
      """, (rs, row) -> mapApproval(rs), id);
  }

  public ApprovalDto createApproval(ApprovalUpdateRequest r, String currentUser) {
    jdbc.update("""
      INSERT INTO `freigabe` (`DATUM`,`EINTRAGENDER`,`BESCHREIBUNG`,`GESELLSCHAFT_ID`,`FILIALE_ID`,`STATUS`,`BEMERKUNG`)
      VALUES (?,?,?,?,?,?,?)
      """, LocalDate.now(), valueOr(r.creator(), currentUser), r.description(), r.companyId(), r.branchId(), valueOr(r.status(), "offen"), r.note());
    Integer id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    return approval(id);
  }

  public ApprovalDto updateApproval(Integer id, ApprovalUpdateRequest r) {
    jdbc.update("""
      UPDATE `freigabe` SET `BESCHREIBUNG`=?, `GESELLSCHAFT_ID`=?, `FILIALE_ID`=?, `STATUS`=?, `BEMERKUNG`=?
      WHERE `ID`=?
      """, r.description(), r.companyId(), r.branchId(), r.status(), r.note(), id);
    return approval(id);
  }

  public void deleteApproval(Integer id) { jdbc.update("DELETE FROM `freigabe` WHERE `ID`=?", id); }

  public WorkflowStats stats() {
    return new WorkflowStats(
      count("SELECT COUNT(*) FROM `aufgaben` WHERE COALESCE(`STATUS`,'') NOT IN ('erledigt','abgeschlossen')"),
      count("SELECT COUNT(*) FROM `aufgaben` WHERE COALESCE(`STATUS`,'') IN ('erledigt','abgeschlossen')"),
      count("SELECT COUNT(*) FROM `freigabe` WHERE COALESCE(`STATUS`,'') NOT IN ('abgeschlossen','freigegeben')"),
      count("SELECT COUNT(*) FROM `freigabe` WHERE COALESCE(`STATUS`,'') IN ('abgeschlossen','freigegeben')")
    );
  }


  private TaskUpdateRequest validateTask(TaskUpdateRequest r) {
    var s=taskSettings.load();
    if(!s.enabled()) throw new IllegalStateException("Aufgabenworkflow ist deaktiviert.");
    String task=valueOr(r.task(), "").trim();
    if(task.isBlank()) throw new IllegalArgumentException("Aufgabe fehlt.");
    String responsible=r.responsible()==null?"":r.responsible().trim();
    if(s.requireResponsiblePerson() && responsible.isBlank()) throw new IllegalArgumentException("Verantwortlicher fehlt.");
    String status=valueOr(r.status(), "offen");
    String due=r.dueDate();
    if((due==null||due.isBlank()) && s.requireDueDate()) throw new IllegalArgumentException("Frist fehlt.");
    if((due==null||due.isBlank()) && s.defaultDueDays()>0) due=LocalDate.now().plusDays(s.defaultDueDays()).toString();
    boolean done="erledigt".equalsIgnoreCase(status)||"abgeschlossen".equalsIgnoreCase(status);
    if(done && s.requireCompletionNote() && (r.note()==null||r.note().isBlank())) throw new IllegalArgumentException("Für erledigte Aufgaben ist eine Abschlussnotiz erforderlich.");
    return new TaskUpdateRequest(r.username(),r.branchCode(),r.branchId(),r.department(),task,responsible,r.priority(),status,due,r.doneBy(),r.note());
  }

  private TaskDto mapTask(ResultSet rs) throws SQLException {
    return new TaskDto(getInt(rs,"ID"), rs.getString("USERNAME"), getStringDate(rs,"TAGESDATUM"), rs.getString("KÜRZEL"), getInt(rs,"FILIALE_ID"), rs.getString("FACHBEREICH"), rs.getString("AUFGABE"), rs.getString("VERANTWORTLICHER"), rs.getString("PRIORITÄT"), rs.getString("STATUS"), getStringDate(rs,"FRIST"), rs.getString("ERLEDIGT"), rs.getString("BEMERKUNG"));
  }

  private ApprovalDto mapApproval(ResultSet rs) throws SQLException {
    return new ApprovalDto(getInt(rs,"ID"), getStringDate(rs,"DATUM"), rs.getString("EINTRAGENDER"), rs.getString("BESCHREIBUNG"), getInt(rs,"GESELLSCHAFT_ID"), getInt(rs,"FILIALE_ID"), rs.getString("STATUS"), rs.getString("BEMERKUNG"));
  }

  private long count(String sql) { Long v = jdbc.queryForObject(sql, Long.class); return v == null ? 0L : v; }
  private int normalizeLimit(int requested) { return Math.min(Math.max(requested <= 0 ? 100 : requested, 1), 500); }
  private String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
  private LocalDate parseDate(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
  private static Integer getInt(ResultSet rs, String col) throws SQLException { int v = rs.getInt(col); return rs.wasNull() ? null : v; }
  private static String getStringDate(ResultSet rs, String col) throws SQLException { java.sql.Date d = rs.getDate(col); return d == null ? null : d.toLocalDate().toString(); }
}
