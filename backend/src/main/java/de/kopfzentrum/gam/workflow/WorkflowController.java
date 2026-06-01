package de.kopfzentrum.gam.workflow;

import de.kopfzentrum.gam.auth.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
  private final WorkflowRepository repository;

  public WorkflowController(WorkflowRepository repository) { this.repository = repository; }

  @GetMapping("/tasks")
  public List<TaskDto> tasks(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "all") String status, @RequestParam(required = false) Integer branchId, @RequestParam(defaultValue = "100") int limit) {
    return repository.tasks(q, status, branchId, limit);
  }

  @GetMapping("/tasks/{id}") public TaskDto task(@PathVariable Integer id) { return repository.task(id); }

  @PostMapping("/tasks")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
  public TaskDto createTask(@RequestBody TaskUpdateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    return repository.createTask(request, user == null ? null : user.getUsername());
  }

  @PutMapping("/tasks/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
  public TaskDto updateTask(@PathVariable Integer id, @RequestBody TaskUpdateRequest request) { return repository.updateTask(id, request); }

  @DeleteMapping("/tasks/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public void deleteTask(@PathVariable Integer id) { repository.deleteTask(id); }

  @GetMapping("/approvals")
  public List<ApprovalDto> approvals(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "all") String status, @RequestParam(required = false) Integer branchId, @RequestParam(defaultValue = "100") int limit) {
    return repository.approvals(q, status, branchId, limit);
  }

  @GetMapping("/approvals/{id}") public ApprovalDto approval(@PathVariable Integer id) { return repository.approval(id); }

  @PostMapping("/approvals")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
  public ApprovalDto createApproval(@RequestBody ApprovalUpdateRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
    return repository.createApproval(request, user == null ? null : user.getUsername());
  }

  @PutMapping("/approvals/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','USER')")
  public ApprovalDto updateApproval(@PathVariable Integer id, @RequestBody ApprovalUpdateRequest request) { return repository.updateApproval(id, request); }

  @DeleteMapping("/approvals/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  public void deleteApproval(@PathVariable Integer id) { repository.deleteApproval(id); }

  @GetMapping("/stats") public WorkflowStats stats() { return repository.stats(); }
}
