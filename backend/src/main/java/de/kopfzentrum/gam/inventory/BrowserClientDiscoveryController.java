package de.kopfzentrum.gam.inventory;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory/discovery/browser-client")
public class BrowserClientDiscoveryController {
  private final BrowserClientDiscoveryRepository repository;
  public BrowserClientDiscoveryController(BrowserClientDiscoveryRepository repository){this.repository=repository;}

  @PostMapping
  public BrowserClientDiscoveryRepository.Client touch(@RequestBody Map<String,Object> body, HttpServletRequest request){
    return repository.touch(request.getRemoteAddr(),body);
  }
}
