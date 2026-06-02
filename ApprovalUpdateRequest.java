package de.kopfzentrum.gam.inventory;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
  private final InventoryRepository repository;

  public InventoryController(InventoryRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/devices")
  public List<InventoryDevice> devices(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "all") String source,
      @RequestParam(required = false) Integer branchId,
      @RequestParam(defaultValue = "false") boolean activeOnly,
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    return repository.search(new InventorySearchCriteria(q, source, branchId, activeOnly, limit, offset));
  }

  @GetMapping("/devices/{source}/{id}")
  public InventoryDeviceDetail device(@PathVariable String source, @PathVariable Integer id) {
    return repository.detail(source, id);
  }

  @GetMapping("/stats")
  public InventoryStats stats() {
    return repository.stats();
  }
}
