package de.kopfzentrum.gam.admin;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import de.kopfzentrum.gam.auth.RoleCatalog;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/permissions")
public class UserApplicationAdminController {
  private final JdbcTemplate jdbc;
  private final RoleCatalog roles;

  public UserApplicationAdminController(JdbcTemplate jdbc, RoleCatalog roles) {
    this.jdbc = jdbc;
    this.roles = roles;
  }

  @GetMapping
  public List<PermissionAccessDto> list(@AuthenticationPrincipal AuthenticatedUser user,
                                        @RequestParam(defaultValue = "") String q,
                                        @RequestParam(defaultValue = "200") int limit) {
    requireAdmin(user);
    String like = "%" + (q == null ? "" : q.trim()) + "%";
    int safeLimit = Math.max(1, Math.min(limit <= 0 ? 200 : limit, 1000));
    return jdbc.query("""
      SELECT ID, USERNAME, APPLICATION, RGESELLSCHAFTS_ID, ROLE
      FROM userapplication
      WHERE (? = '%%' OR USERNAME LIKE ? OR APPLICATION LIKE ? OR ROLE LIKE ? OR CAST(RGESELLSCHAFTS_ID AS CHAR) LIKE ?)
      ORDER BY USERNAME, APPLICATION, RGESELLSCHAFTS_ID, ROLE
      LIMIT ?
      """, (rs, row) -> new PermissionAccessDto(
        rs.getInt("ID"), rs.getString("USERNAME"), rs.getString("APPLICATION"),
        (Integer) rs.getObject("RGESELLSCHAFTS_ID"), rs.getString("ROLE")
      ), like, like, like, like, like, safeLimit);
  }

  @GetMapping("/applications")
  public List<PermissionApplicationDto> applications(@AuthenticationPrincipal AuthenticatedUser user) {
    requireAdmin(user);
    return jdbc.query("""
      SELECT APPLICATION, MAX(FG_SELECT) AS FG_SELECT
      FROM (
        SELECT APPLICATION, FG_SELECT FROM application
        UNION ALL
        SELECT APPLICATION, NULL AS FG_SELECT FROM userapplication
      ) x
      WHERE APPLICATION IS NOT NULL AND APPLICATION <> ''
      GROUP BY APPLICATION
      ORDER BY APPLICATION
      """, (rs, row) -> new PermissionApplicationDto(
        rs.getString("APPLICATION"), toBoolean(rs.getObject("FG_SELECT"))
      ));
  }

  @PostMapping
  public PermissionAccessDto create(@AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestBody PermissionAccessRequest request) {
    requireAdmin(user);
    String username = clean(request == null ? null : request.username());
    String application = clean(request == null ? null : request.application());
    String role = clean(request == null ? null : request.role());
    if (username == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benutzername fehlt");
    if (application == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anwendung fehlt");
    if (role == null) role = "user";
    jdbc.update("""
      INSERT INTO userapplication (USERNAME, APPLICATION, RGESELLSCHAFTS_ID, ROLE)
      VALUES (?, ?, ?, ?)
      """, username, application, request.companyId(), role);
    Integer id = jdbc.queryForObject("SELECT MAX(ID) FROM userapplication WHERE USERNAME = ? AND APPLICATION = ?", Integer.class, username, application);
    return byId(id == null ? -1 : id);
  }

  @PatchMapping("/{id}")
  public PermissionAccessDto update(@AuthenticationPrincipal AuthenticatedUser user,
                                    @PathVariable int id,
                                    @RequestBody PermissionAccessRequest request) {
    requireAdmin(user);
    String username = clean(request == null ? null : request.username());
    String application = clean(request == null ? null : request.application());
    String role = clean(request == null ? null : request.role());
    if (username == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benutzername fehlt");
    if (application == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anwendung fehlt");
    if (role == null) role = "user";
    int changed = jdbc.update("""
      UPDATE userapplication
      SET USERNAME = ?, APPLICATION = ?, RGESELLSCHAFTS_ID = ?, ROLE = ?
      WHERE ID = ?
      """, username, application, request.companyId(), role, id);
    if (changed != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rechtezuordnung nicht gefunden");
    return byId(id);
  }

  @DeleteMapping("/{id}")
  public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable int id) {
    requireAdmin(user);
    jdbc.update("DELETE FROM userapplication WHERE ID = ?", id);
  }

  private PermissionAccessDto byId(int id) {
    return jdbc.queryForObject("""
      SELECT ID, USERNAME, APPLICATION, RGESELLSCHAFTS_ID, ROLE
      FROM userapplication
      WHERE ID = ?
      """, (rs, row) -> new PermissionAccessDto(
        rs.getInt("ID"), rs.getString("USERNAME"), rs.getString("APPLICATION"),
        (Integer) rs.getObject("RGESELLSCHAFTS_ID"), rs.getString("ROLE")
      ), id);
  }

  private String clean(String value) {
    if (value == null) return null;
    String v = value.trim();
    return v.isBlank() ? null : v;
  }

  private Boolean toBoolean(Object value) {
    if (value == null) return null;
    if (value instanceof Boolean b) return b;
    if (value instanceof Number n) return n.intValue() != 0;
    String s = String.valueOf(value).trim();
    if (s.isBlank()) return null;
    return s.equals("1") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("ja") || s.equalsIgnoreCase("yes");
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user == null || !roles.isAdmin(user.account())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nur Administratoren duerfen Rechte verwalten");
    }
  }
}
