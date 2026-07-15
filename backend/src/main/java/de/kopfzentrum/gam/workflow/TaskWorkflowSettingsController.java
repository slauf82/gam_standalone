package de.kopfzentrum.gam.workflow;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/task-workflow")
public class TaskWorkflowSettingsController {
  private final TaskWorkflowSettingsRepository repository;
  public TaskWorkflowSettingsController(TaskWorkflowSettingsRepository repository){this.repository=repository;}
  @GetMapping public TaskWorkflowSettings load(){return repository.load();}
  @PutMapping @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public TaskWorkflowSettings save(@RequestBody TaskWorkflowSettings value,@AuthenticationPrincipal AuthenticatedUser user){return repository.save(value,user==null?null:user.getUsername());}
}
