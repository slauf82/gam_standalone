package de.kopfzentrum.gam.admin;

import de.kopfzentrum.gam.auth.RoleCatalog;
import de.kopfzentrum.gam.auth.RoleDto;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/roles")
public class RoleController {
  private final RoleCatalog roles;
  public RoleController(RoleCatalog roles) { this.roles = roles; }

  @GetMapping
  public List<RoleDto> roles() { return roles.roles(); }
}
