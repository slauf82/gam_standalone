package de.kopfzentrum.gam.link;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-warehouse")
public class InventoryWarehouseController {
  private final InventoryWarehouseRepository repository;

  public InventoryWarehouseController(InventoryWarehouseRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/links")
  public List<DeviceMaterialLink> links(@RequestParam(required = false) Integer deviceId, @RequestParam(required = false) Integer materialId) {
    return repository.links(deviceId, materialId);
  }

  @PostMapping("/links")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','LAGER','INVENTAR')")
  public DeviceMaterialLink addLink(@RequestBody DeviceMaterialLinkRequest request) {
    return repository.addLink(request.deviceId(), request.materialId());
  }

  @DeleteMapping("/links/{linkId}")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','LAGER','INVENTAR')")
  public void removeLink(@PathVariable Integer linkId) {
    repository.removeLink(linkId);
  }

  @PostMapping("/book")
  @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','LAGER','INVENTAR')")
  public MaterialBookingResult book(@RequestBody MaterialBookingRequest request) {
    return repository.book(request);
  }

  @GetMapping("/movements")
  public List<MaterialMovement> movements(@RequestParam(required = false) Integer deviceId, @RequestParam(required = false) Integer materialId, @RequestParam(defaultValue = "100") int limit) {
    return repository.movements(deviceId, materialId, limit);
  }
}
