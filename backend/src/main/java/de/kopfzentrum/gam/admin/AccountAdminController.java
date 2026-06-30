package de.kopfzentrum.gam.admin;

import de.kopfzentrum.gam.auth.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountAdminController {
  private final AccountRepository accounts;
  private final RoleCatalog roles;

  public AccountAdminController(AccountRepository accounts, RoleCatalog roles) {
    this.accounts = accounts;
    this.roles = roles;
  }

  @GetMapping
  public List<AccountAdminDto> list(@AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "100") int limit) {
    requireAdmin(user);
    return accounts.findAll(q, limit).stream().map(AccountAdminDto::from).toList();
  }

  @PostMapping
  public AccountAdminDto create(@AuthenticationPrincipal AuthenticatedUser user,
                                @RequestBody AccountCreateRequest request) {
    requireAdmin(user);
    if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leere Anfrage");
    return AccountAdminDto.from(accounts.create(request.username(), request.password(), request.fullname(), request.role(), request.email()));
  }

  @PatchMapping("/{id}")
  public AccountAdminDto update(@AuthenticationPrincipal AuthenticatedUser user,
                                @PathVariable int id,
                                @RequestBody AccountUpdateRequest request) {
    requireAdmin(user);
    if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leere Anfrage");
    return AccountAdminDto.from(accounts.updateMetadata(id, request.fullname(), request.role(), request.email(), request.secretkey()));
  }

  @PatchMapping("/{id}/password")
  public AccountAdminDto password(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable int id,
                                  @RequestBody AccountPasswordRequest request) {
    requireAdmin(user);
    if (request == null || request.password() == null || request.password().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwort fehlt");
    }
    return AccountAdminDto.from(accounts.updatePassword(id, request.password()));
  }

  @DeleteMapping("/{id}")
  public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                     @PathVariable int id) {
    requireAdmin(user);
    if (user.account().id() != null && user.account().id() == id) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der eigene Benutzer darf nicht gelöscht werden");
    }
    accounts.deleteById(id);
  }

  private void requireAdmin(AuthenticatedUser user) {
    if (user == null || !roles.isAdmin(user.account())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nur Administratoren duerfen Benutzer verwalten");
    }
  }
}
