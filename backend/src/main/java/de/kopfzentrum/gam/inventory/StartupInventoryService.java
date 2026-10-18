package de.kopfzentrum.gam.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 40k36q13: zentrale, abschaltbare Startinventarisierung.
 * Läuft erst nach ApplicationReadyEvent und ausschließlich, wenn die persistente
 * Geräteverwaltungs-Einstellung aktiviert ist.
 */
@Service
public class StartupInventoryService {
  private static final Logger log=LoggerFactory.getLogger(StartupInventoryService.class);
  private final StartupInventorySettingsRepository settings;
  private final DeviceIdentityService identities;
  private final ExecutorService executor=Executors.newSingleThreadExecutor(Thread.ofVirtual().name("gam-startinventory-",0).factory());

  public StartupInventoryService(StartupInventorySettingsRepository settings,DeviceIdentityService identities){
    this.settings=settings;this.identities=identities;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void afterStartup(){
    if(!settings.load().enabled()){
      log.info("[START-INVENTORY] Startinventarisierung ist in den Geräteverwaltungseinstellungen deaktiviert.");
      return;
    }
    executor.submit(()->{
      var rows=identities.overview();
      log.info("[START-INVENTORY] Startinventarisierung für {} registrierte Geräte gestartet.",rows.size());
      int attempted=0,successful=0;
      for(var row:rows){
        if(Thread.currentThread().isInterrupted()){
          log.info("[START-INVENTORY] Startinventarisierung wurde vom Benutzer abgebrochen.");
          break;
        }
        try{
          var results=identities.runAllKnownPlatforms(row.identityKey(),"automatisch (Startinventarisierung)");
          for(var result:results){attempted++;if(result.success())successful++;}
        }catch(Exception e){
          log.warn("[START-INVENTORY] {} fehlgeschlagen: {}",row.identityKey(),e.getMessage());
        }
      }
      log.info("[START-INVENTORY] Abgeschlossen: {} Plattformläufe, {} erfolgreich.",attempted,successful);
    });
  }
}
