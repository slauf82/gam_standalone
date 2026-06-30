package de.kopfzentrum.gam.warehouse;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {
  private final WarehouseRepository repository;

  public WarehouseController(WarehouseRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/items")
  public List<WarehouseItem> items(@RequestParam(defaultValue = "") String q,
                                   @RequestParam(defaultValue = "all") String kind,
                                   @RequestParam(defaultValue = "false") Boolean onlyWithStock,
                                   @RequestParam(defaultValue = "100") int limit,
                                   @RequestParam(defaultValue = "0") int offset) {
    return repository.search(new WarehouseSearchCriteria(q, kind, onlyWithStock, limit, offset));
  }

  @GetMapping("/items/{kind}/{id}")
  public WarehouseItem detail(@PathVariable String kind, @PathVariable Integer id) {
    return repository.detail(kind, id);
  }

  @PostMapping("/items/{kind}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','LAGER')")
  public WarehouseItem create(@PathVariable String kind, @RequestBody WarehouseItemRequest request) {
    return repository.create(kind, request);
  }

  @PutMapping("/items/{kind}/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','LAGER')")
  public WarehouseItem update(@PathVariable String kind, @PathVariable Integer id, @RequestBody WarehouseItemRequest request) {
    return repository.update(kind, id, request);
  }

  @DeleteMapping("/items/{kind}/{id}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','LAGER')")
  public void delete(@PathVariable String kind, @PathVariable Integer id) {
    repository.delete(kind, id);
  }

  @PatchMapping("/items/{kind}/{id}/stock")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','LAGER')")
  public WarehouseItem updateStock(@PathVariable String kind, @PathVariable Integer id, @RequestBody StockChangeRequest request) {
    return repository.updateStock(kind, id, request.quantity());
  }

  @PatchMapping("/items/{kind}/{id}/move")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','LAGER')")
  public WarehouseItem moveStock(@PathVariable String kind, @PathVariable Integer id, @RequestBody StockMovementRequest request) {
    return repository.moveStock(kind, id, request.delta());
  }

  @GetMapping("/stats")
  public WarehouseStats stats() {
    return repository.stats();
  }
}
