package de.kopfzentrum.gam.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class BuiltinDiscoverySettingsRepository {
  private final JdbcTemplate jdbc;
  public BuiltinDiscoverySettingsRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

  @PostConstruct void init(){
    jdbc.execute("CREATE TABLE IF NOT EXISTS gam_discovery_builtin_sources (source_key VARCHAR(80) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, PRIMARY KEY(source_key))");
    for(var entry: defaults().entrySet()) jdbc.update("INSERT IGNORE INTO gam_discovery_builtin_sources(source_key,enabled,updated_by) VALUES(?,?, 'system')",entry.getKey(),entry.getValue());
  }

  public Map<String,Boolean> load(){
    LinkedHashMap<String,Boolean> result=new LinkedHashMap<>(defaults());
    jdbc.query("SELECT source_key,enabled FROM gam_discovery_builtin_sources",rs->{result.put(rs.getString(1),rs.getBoolean(2));});
    return result;
  }

  public Map<String,Boolean> save(Map<String,Boolean> values,String actor){
    if(values!=null) values.forEach((key,enabled)->{
      if(defaults().containsKey(key)) jdbc.update("INSERT INTO gam_discovery_builtin_sources(source_key,enabled,updated_by) VALUES(?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),updated_by=VALUES(updated_by)",key,Boolean.TRUE.equals(enabled),actor);
    });
    return load();
  }

  public boolean enabled(String key){return load().getOrDefault(key,true);}
  private static Map<String,Boolean> defaults(){
    LinkedHashMap<String,Boolean> m=new LinkedHashMap<>();
    m.put("LOCAL_ADAPTERS",true);
    m.put("NEIGHBOR",true);
    m.put("ACTIVE_SCAN",true);
    m.put("SSDP",true);
    m.put("DNS_NAMES",true);
    m.put("MDNS",true);
    m.put("WS_DISCOVERY",true);
    m.put("WINDOWS_INVENTORY",true);
    m.put("LINUX_INVENTORY",true);
    m.put("LINUX_NETWORK",true);
    m.put("NETBIOS",true);
    m.put("DHCP_LEASES",true);
    m.put("USB_LOCAL",false);
    m.put("BLUETOOTH_LOCAL",false);
    m.put("DOCKER_LOCAL",false);
    m.put("SNMP",true);
    m.put("AUTO_REGISTER",true);
    m.put("AUTO_INVENTORY",false);
    m.put("AUTO_CATEGORY",true);

    // Schritt 40k31: Anzeige- und Sensordaten-Konfiguration.
    m.put("VIEW_COMPACT",false);
    m.put("VIEW_HIDE_EMPTY",true);
    m.put("VIEW_HIDE_UNKNOWN",true);
    m.put("VIEW_SHOW_TIMESTAMPS",true);
    m.put("VIEW_SHOW_UNITS",true);
    m.put("VIEW_SHOW_ENTITY_IDS",false);
    m.put("VIEW_FAVORITES_FIRST",true);
    m.put("VIEW_HIGHLIGHT_CHANGES",true);
    m.put("VIEW_SHOW_DIAGNOSTICS",true);
    m.put("VIEW_SHOW_CONFIG_ENTITIES",false);
    m.put("VIEW_SHOW_DISABLED_ENTITIES",false);
    m.put("VIEW_SHOW_HIDDEN_ENTITIES",false);
    m.put("VIEW_DOMAIN_SENSOR",true);
    m.put("VIEW_DOMAIN_BINARY_SENSOR",true);
    m.put("VIEW_DOMAIN_SWITCH",true);
    m.put("VIEW_DOMAIN_NUMBER",true);
    m.put("VIEW_DOMAIN_SELECT",true);
    m.put("VIEW_DOMAIN_BUTTON",false);
    m.put("VIEW_DOMAIN_CLIMATE",true);
    m.put("VIEW_DOMAIN_COVER",true);
    m.put("VIEW_DOMAIN_LIGHT",true);
    m.put("VIEW_DOMAIN_FAN",true);
    m.put("VIEW_DOMAIN_CAMERA",true);
    m.put("VIEW_DOMAIN_UPDATE",true);

    // Bereits vorbereitete Integrationskarten. Die Schalter aktivieren nur die
    // spätere Anzeige/Übernahme, solange noch kein Adapter implementiert ist.
    for(String key : new String[]{
      "PLAN_LLDP","PLAN_DNS","PLAN_DHCP","PLAN_RADIUS","PLAN_IPMI",
      "PLAN_WMI","PLAN_WINRM","PLAN_ACTIVE_DIRECTORY","PLAN_ENTRA_ID","PLAN_INTUNE","PLAN_MECM",
      "PLAN_SSH","PLAN_MACOS","PLAN_JAMF","PLAN_ANSIBLE","PLAN_CHROME_ENTERPRISE",
      "PLAN_MQTT","PLAN_ZIGBEE2MQTT","PLAN_ZHA","PLAN_MATTER","PLAN_THREAD","PLAN_HOMEKIT","PLAN_OPENHAB","PLAN_SHELLY","PLAN_TASMOTA","PLAN_ESPHOME",
      "PLAN_PROXMOX","PLAN_VMWARE","PLAN_HYPERV","PLAN_VIRTUALBOX","PLAN_NUTANIX",
      "PLAN_DOCKER","PLAN_DOCKER_COMPOSE","PLAN_KUBERNETES","PLAN_PORTAINER","PLAN_OPENSHIFT","PLAN_RANCHER",
      "PLAN_AZURE","PLAN_AWS","PLAN_GCP","PLAN_MICROSOFT_365","PLAN_GOOGLE_WORKSPACE","PLAN_ORACLE_CLOUD",
      "PLAN_SYNOLOGY","PLAN_QNAP","PLAN_TRUENAS","PLAN_OPENMEDIAVAULT","PLAN_NETAPP","PLAN_DELL_EMC","PLAN_VEEAM",
      "PLAN_UNIFI","PLAN_OMADA","PLAN_MIKROTIK","PLAN_CISCO","PLAN_ARUBA","PLAN_PFSENSE","PLAN_OPNSENSE","PLAN_FORTINET","PLAN_SOPHOS",
      "PLAN_PROMETHEUS","PLAN_GRAFANA","PLAN_ZABBIX","PLAN_CHECKMK","PLAN_PRTG","PLAN_NAGIOS","PLAN_LANSWEEPER","PLAN_GLPI",
      "PLAN_FELICITY","PLAN_KOSTAL","PLAN_MODBUS","PLAN_BACNET","PLAN_KNX","PLAN_VICTRON","PLAN_SMA","PLAN_FRONIUS","PLAN_SOLAREDGE","PLAN_HUAWEI_SOLAR"
    }) m.put(key,false);
    return m;
  }
}
