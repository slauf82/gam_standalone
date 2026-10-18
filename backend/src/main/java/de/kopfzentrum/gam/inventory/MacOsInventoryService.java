package de.kopfzentrum.gam.inventory;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** 40k36l: macOS-Inventarisierung mit Berechtigungsprüfung und optional transient erhöhtem sudo-Lauf. */
@Service
public class MacOsInventoryService {
  private final LinuxSshSettingsRepository settings;
  private final EmbeddedSshClientService embeddedSsh;
  private final OsAccessProfileRepository accessProfiles;
  public MacOsInventoryService(LinuxSshSettingsRepository settings, EmbeddedSshClientService embeddedSsh, OsAccessProfileRepository accessProfiles){this.settings=settings;this.embeddedSsh=embeddedSsh;this.accessProfiles=accessProfiles;}
  private LinuxSshSettingsRepository.Settings sshFor(String host){var p=accessProfiles.resolve("SSH",host);if(p!=null)return new LinuxSshSettingsRepository.Settings(p.enabled(),p.username(),p.authMode(),p.password(),p.keyPath(),p.port());return settings.load();}

  /** 40k36t10: Prüft, ob für das konkrete Ziel ein aktivierter SSH-Zugang existiert. */
  public boolean hasConfiguredSshAccess(String host){
    if(host==null||host.isBlank()) return false;
    var cfg=sshFor(host);
    return cfg.enabled() && cfg.username()!=null && !cfg.username().isBlank();
  }

  public record TestResult(boolean sshConfigured, boolean reachable, boolean macOs, String message, String checkedAt){}
  public record SoftwareItem(String scopeType,String scopeName,String name,String version,String bundleOrPackage,String installPath,String source){}
  public record InventoryResult(DiscoveredDevice device,List<SoftwareItem> software,String userSoftwareScanStatus,String userSoftwareScanDetails){}

  public TestResult test(String ip){
    var cfg=sshFor(ip); String now=OffsetDateTime.now().toString();
    if(!cfg.enabled()||cfg.username().isBlank()) return new TestResult(false,false,false,"SSH-Zugang ist noch nicht konfiguriert.",now);
    String out=run(ip,cfg,"printf 'GAM_OS='; uname -s; sw_vers -productVersion 2>/dev/null | sed 's/^/GAM_VERSION=/'",10);
    if(out==null||out.isBlank()) return new TestResult(true,false,false,"Keine SSH-Verbindung. Auf dem Mac bitte Systemeinstellungen → Allgemein → Freigabe → Entfernte Anmeldung aktivieren sowie Port, Benutzername und Zugangsdaten prüfen.",now);
    boolean mac=out.contains("GAM_OS=Darwin")||out.contains("GAM_VERSION=");
    return new TestResult(true,true,mac,mac?"SSH-Verbindung erfolgreich; das Ziel wurde als macOS erkannt.":"SSH-Verbindung erfolgreich, das Ziel meldet jedoch kein macOS/Darwin.",now);
  }

  public Optional<DiscoveredDevice> inventory(String ip, String nameHint){ return inventoryDetailed(ip,nameHint,null).map(InventoryResult::device); }
  public Optional<DiscoveredDevice> inventory(String ip, String nameHint, String sudoPassword){ return inventoryDetailed(ip,nameHint,sudoPassword).map(InventoryResult::device); }
  public Optional<InventoryResult> inventoryDetailed(String ip, String nameHint, String sudoPassword){ return inventoryDetailedInternal(ip,nameHint,sudoPassword,false); }
  public Optional<InventoryResult> inventoryDetailedLocal(String knownAddress,String nameHint,String sudoPassword){ return inventoryDetailedInternal(knownAddress,nameHint,sudoPassword,true); }

  public boolean isBackendLocalTarget(String knownAddress,String knownSerial,String knownName){
    if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("mac")) return false;
    // 40k36r2: 127.0.0.1, ::1 und die ausgeschriebene IPv6-Loopback-Adresse
    // bezeichnen zwingend diesen Backend-Rechner und duerfen niemals in den SSH-Pfad fallen.
    if(isLoopbackAddress(knownAddress)) return true;
    String probe=runLocal("""
      printf 'HOST='; scutil --get ComputerName 2>/dev/null || hostname
      printf 'SERIAL='; system_profiler SPHardwareDataType 2>/dev/null | awk -F': ' '/Serial Number/{print $2;exit}'
      printf 'IPS='; ifconfig 2>/dev/null | awk '/inet / && $2 != "127.0.0.1" {if(n++)printf ",";printf $2}END{print ""}'
      """,15);
    if(probe==null||probe.isBlank()) return false;
    Map<String,String> local=parse(probe);
    String serial=local.get("SERIAL");
    if(knownSerial!=null&&!knownSerial.isBlank()&&serial!=null&&!serial.isBlank()&&knownSerial.trim().equalsIgnoreCase(serial.trim())) return true;
    String ips=local.get("IPS");
    if(knownAddress!=null&&!knownAddress.isBlank()&&ips!=null&&Arrays.stream(ips.split(",")).map(String::trim).anyMatch(knownAddress.trim()::equalsIgnoreCase)) return true;
    String host=local.get("HOST");
    return knownName!=null&&!knownName.isBlank()&&host!=null&&!host.isBlank()&&knownName.trim().equalsIgnoreCase(host.trim());
  }

  private Optional<InventoryResult> inventoryDetailedInternal(String ip, String nameHint, String sudoPassword, boolean backendLocal){
    var cfg=backendLocal?null:sshFor(ip);
    String script="""
      printf 'OS='; uname -s
      printf 'CURRENT_USER='; id -un 2>/dev/null
      printf 'CURRENT_UID='; id -u 2>/dev/null
      printf 'EFFECTIVE_UID='; id -u 2>/dev/null
      printf 'CURRENT_GID='; id -g 2>/dev/null
      printf 'ADMIN_MEMBER='; if dseditgroup -o checkmember -m "$(id -un)" admin 2>/dev/null | grep -qi 'yes'; then echo ja; else echo nein; fi
      printf 'SUDO_AVAILABLE='; if command -v sudo >/dev/null 2>&1; then echo ja; else echo nein; fi
      printf 'SUDO_NON_INTERACTIVE='; if sudo -n true >/dev/null 2>&1; then echo ja; else echo nein; fi
      printf 'ROOT_ACTIVE='; if [ "$(id -u 2>/dev/null)" = 0 ]; then echo ja; else echo nein; fi
      printf 'SIP_STATUS='; csrutil status 2>/dev/null | tr '\n' ' ' | sed 's/[[:space:]]*$//'; echo
      printf 'FULL_DISK_ACCESS_HINT='; if ls "$HOME/Library/Mail" >/dev/null 2>&1 || ls "$HOME/Library/Messages" >/dev/null 2>&1; then echo wahrscheinlich verfügbar; else echo eingeschränkt oder nicht prüfbar; fi
      printf 'TOOL_ACCESS='; for T in system_profiler ioreg diskutil pmset launchctl networksetup fdesetup; do if command -v "$T" >/dev/null 2>&1; then printf '%s=ja,' "$T"; else printf '%s=nein,' "$T"; fi; done; echo
      printf 'HOSTNAME='; scutil --get ComputerName 2>/dev/null || hostname
      printf 'PRODUCT_NAME='; sw_vers -productName 2>/dev/null
      printf 'PRODUCT_VERSION='; sw_vers -productVersion 2>/dev/null
      printf 'BUILD='; sw_vers -buildVersion 2>/dev/null
      printf 'ARCH='; uname -m
      printf 'MODEL='; sysctl -n hw.model 2>/dev/null
      printf 'MACHINE='; sysctl -n hw.machine 2>/dev/null
      printf 'CPU='; sysctl -n machdep.cpu.brand_string 2>/dev/null || true
      printf 'CPU_PHYSICAL='; sysctl -n hw.physicalcpu 2>/dev/null
      printf 'CPU_PHYSICAL_MAX='; sysctl -n hw.physicalcpu_max 2>/dev/null
      printf 'CPU_LOGICAL='; sysctl -n hw.logicalcpu 2>/dev/null
      printf 'CPU_LOGICAL_MAX='; sysctl -n hw.logicalcpu_max 2>/dev/null
      printf 'CPU_FREQUENCY='; sysctl -n hw.cpufrequency 2>/dev/null || true
      printf 'CACHE_L1I='; sysctl -n hw.l1icachesize 2>/dev/null || true
      printf 'CACHE_L1D='; sysctl -n hw.l1dcachesize 2>/dev/null || true
      printf 'CACHE_L2='; sysctl -n hw.l2cachesize 2>/dev/null || true
      printf 'CACHE_L3='; sysctl -n hw.l3cachesize 2>/dev/null || true
      printf 'MEMORY_BYTES='; sysctl -n hw.memsize 2>/dev/null
      HWPROF=$(LC_ALL=C LANG=C system_profiler SPHardwareDataType 2>/dev/null)
      printf 'MODEL_NAME='; printf '%s\n' "$HWPROF" | awk -F': ' '/Model Name:/{print $2; exit}'; echo
      printf 'MODEL_IDENTIFIER='; printf '%s\n' "$HWPROF" | awk -F': ' '/Model Identifier:/{print $2; exit}'; echo
      printf 'CHIP='; printf '%s\n' "$HWPROF" | awk -F': ' '/Chip:/{print $2; exit}'; echo
      printf 'PROCESSOR_NAME='; printf '%s\n' "$HWPROF" | awk -F': ' '/Processor Name:/{print $2; exit}'; echo
      printf 'PROCESSOR_SPEED='; printf '%s\n' "$HWPROF" | awk -F': ' '/Processor Speed:/{print $2; exit}'; echo
      printf 'PROCESSOR_COUNT='; printf '%s\n' "$HWPROF" | awk -F': ' '/Number of Processors:/{print $2; exit}'; echo
      printf 'TOTAL_CORES='; printf '%s\n' "$HWPROF" | awk -F': ' '/Total Number of Cores:/{print $2; exit}'; echo
      printf 'MEMORY_TEXT='; printf '%s\n' "$HWPROF" | awk -F': ' '/Memory:/{print $2; exit}'; echo
      printf 'SERIAL='; printf '%s\n' "$HWPROF" | awk -F': ' '/Serial Number/{print $2; exit}'; echo
      printf 'HARDWARE_UUID='; printf '%s\n' "$HWPROF" | awk -F': ' '/Hardware UUID/{print $2; exit}'; echo
      printf 'PROVISIONING_UDID='; printf '%s\n' "$HWPROF" | awk -F': ' '/Provisioning UDID/{print $2; exit}'; echo
      printf 'ACTIVATION_LOCK='; printf '%s\n' "$HWPROF" | awk -F': ' '/Activation Lock Status/{print $2; exit}'; echo
      printf 'BOOT_ROM='; printf '%s\n' "$HWPROF" | awk -F': ' '/Boot ROM Version/{print $2; exit}'; echo
      printf 'SYSTEM_FIRMWARE='; printf '%s\n' "$HWPROF" | awk -F': ' '/System Firmware Version/{print $2; exit}'; echo
      printf 'OS_LOADER='; printf '%s\n' "$HWPROF" | awk -F': ' '/OS Loader Version/{print $2; exit}'; echo
      printf 'SMC_VERSION='; printf '%s\n' "$HWPROF" | awk -F': ' '/SMC Version/{print $2; exit}'; echo
      printf 'DISK='; diskutil info / 2>/dev/null | awk -F': *' '/Device Identifier|Volume Name|File System Personality|Disk Size/{printf "%s=%s; ",$1,$2}' ; echo
      printf 'FILEVAULT='; fdesetup status 2>/dev/null | tr '\n' ' ' | sed 's/[[:space:]]*$//'; echo
      printf 'PHYSICAL_DISKS='; diskutil list physical 2>/dev/null | awk '/^\\/dev\\/disk/{if(n++)printf ", "; printf $1}' ; echo
      printf 'PHYSICAL_DISK_COUNT='; diskutil list physical 2>/dev/null | awk '/^\\/dev\\/disk/{n++}END{print n+0}'
      APFS_TEXT=$(LC_ALL=C LANG=C diskutil apfs list 2>/dev/null)
      printf 'APFS_CONTAINER_COUNT='; printf '%s\n' "$APFS_TEXT" | grep -c 'APFS Container Reference:' | tr -d ' '
      printf 'APFS_VOLUME_COUNT='; printf '%s\n' "$APFS_TEXT" | grep -c 'APFS Volume Disk' | tr -d ' '
      printf 'APFS_CONTAINERS='; printf '%s\n' "$APFS_TEXT" | awk -F': *' '/APFS Container Reference:/{if(n++)printf "; "; printf $2}' ; echo
      VOLUME_ROWS=''
      ENCRYPTED_COUNT=0
      LOCKED_COUNT=0
      VOLUME_COUNT=0
      for DEV in $(diskutil list 2>/dev/null | awk '/^[[:space:]]*[0-9]+:/{print $NF}' | grep '^disk' | sort -u); do
        INFO=$(LC_ALL=C LANG=C diskutil info "$DEV" 2>/dev/null) || continue
        [ -n "$INFO" ] || continue
        field(){ printf '%s\n' "$INFO" | awk -F': *' -v key="$1" '$1 ~ "^[[:space:]]*" key "[[:space:]]*$" {print $2; exit}'; }
        IDENT=$(field 'Device Identifier')
        NAME=$(field 'Volume Name')
        [ -n "$NAME" ] || NAME=$(field 'Media Name')
        FS=$(field 'File System Personality')
        [ -n "$FS" ] || FS=$(field 'Type (Bundle)')
        MOUNTED=$(field 'Mounted')
        MOUNT=$(field 'Mount Point')
        SIZE=$(field 'Disk Size')
        [ -n "$SIZE" ] || SIZE=$(field 'Volume Total Space')
        USED=$(field 'Volume Used Space')
        FREE=$(field 'Volume Free Space')
        CONTAINER=$(field 'APFS Container Reference')
        ROLE=$(field 'APFS Volume Disk (Role)')
        [ -n "$ROLE" ] || ROLE=$(field 'Roles')
        ENCRYPTED=$(field 'Encrypted')
        FILEVAULT_VOLUME=$(field 'FileVault')
        LOCKED=$(field 'Locked')
        SEALED=$(field 'Sealed')
        case "$ENCRYPTED $FILEVAULT_VOLUME" in *Yes*|*yes*) ENCRYPTED_COUNT=$((ENCRYPTED_COUNT+1));; esac
        case "$LOCKED" in Yes|yes) LOCKED_COUNT=$((LOCKED_COUNT+1));; esac
        VOLUME_COUNT=$((VOLUME_COUNT+1))
        ROW="${IDENT:-$DEV}|${NAME:--}|${FS:--}|${MOUNT:--}|${SIZE:--}|${USED:--}|${FREE:--}|${CONTAINER:--}|${ROLE:--}|${ENCRYPTED:--}|${FILEVAULT_VOLUME:--}|${LOCKED:--}|${MOUNTED:--}|${SEALED:--}"
        if [ -n "$VOLUME_ROWS" ]; then VOLUME_ROWS="$VOLUME_ROWS ; $ROW"; else VOLUME_ROWS="$ROW"; fi
      done
      printf 'VOLUME_COUNT=%s\n' "$VOLUME_COUNT"
      printf 'ENCRYPTED_VOLUME_COUNT=%s\n' "$ENCRYPTED_COUNT"
      printf 'LOCKED_VOLUME_COUNT=%s\n' "$LOCKED_COUNT"
      printf 'VOLUMES=%s\n' "$VOLUME_ROWS"

      # 40k36r3: vorgezogene macOS-Hardwareanalyse.
      # Nur lesen; keine Installation von smartmontools waehrend der Inventarisierung.
      SMARTMONTOOLS_PACKAGE=nein
      SMARTMONTOOLS_VERSION=''
      if command -v brew >/dev/null 2>&1 && brew list --versions smartmontools >/dev/null 2>&1; then
        SMARTMONTOOLS_PACKAGE=ja
        SMARTMONTOOLS_VERSION=$(brew list --versions smartmontools 2>/dev/null | head -n 1 | cut -d' ' -f2-)
      fi
      SMARTCTL_PATH=''
      if command -v smartctl >/dev/null 2>&1; then
        SMARTCTL_PATH=$(command -v smartctl)
      elif [ -x /opt/homebrew/bin/smartctl ]; then
        SMARTCTL_PATH=/opt/homebrew/bin/smartctl
      elif [ -x /usr/local/bin/smartctl ]; then
        SMARTCTL_PATH=/usr/local/bin/smartctl
      fi
      SMARTCTL_AVAILABLE=nein
      SMARTCTL_EXECUTABLE=nein
      SMARTCTL_DIAGNOSTIC=''
      if [ -n "$SMARTCTL_PATH" ]; then
        SMARTCTL_AVAILABLE=ja
        if [ -x "$SMARTCTL_PATH" ]; then
          SMARTCTL_EXECUTABLE=ja
        else
          SMARTCTL_DIAGNOSTIC="smartctl gefunden, aber nicht ausfuehrbar: $SMARTCTL_PATH"
        fi
      elif [ "$SMARTMONTOOLS_PACKAGE" = ja ]; then
        SMARTCTL_DIAGNOSTIC='smartmontools ist installiert, aber smartctl wurde nicht im PATH bzw. in Standardpfaden gefunden'
      else
        SMARTCTL_DIAGNOSTIC='smartmontools/smartctl nicht installiert; native macOS-SMART-Daten werden verwendet'
      fi
      # 40k36r9 smartmontools normalization
      if [ -n "${SMARTCTL_PATH:-}" ] && [ -x "${SMARTCTL_PATH}" ]; then
        SMARTMONTOOLS_PACKAGE='ja'
        SMARTCTL_AVAILABLE='ja'
        SMARTCTL_EXECUTABLE='ja'
        SMART_DIAGNOSIS='smartmontools/smartctl verfügbar; erweiterte SMART/NVMe-Daten werden verwendet'
      elif command -v smartctl >/dev/null 2>&1; then
        SMARTCTL_PATH="$(command -v smartctl)"
        SMARTMONTOOLS_PACKAGE='ja'
        SMARTCTL_AVAILABLE='ja'
        SMARTCTL_EXECUTABLE='ja'
        SMART_DIAGNOSIS='smartmontools/smartctl verfügbar; erweiterte SMART/NVMe-Daten werden verwendet'
      else
        SMARTMONTOOLS_PACKAGE='nein'
        SMARTCTL_AVAILABLE='nein'
        SMARTCTL_EXECUTABLE='nein'
        SMART_DIAGNOSIS='smartmontools/smartctl nicht installiert; native macOS-SMART-Daten werden verwendet'
      fi
      # 40k36r10: finale SMART-Diagnose direkt aus dem realen smartctl-Zustand
      if [ "${SMARTCTL_AVAILABLE:-nein}" = "ja" ] || [ "${SMARTCTL_EXECUTABLE:-nein}" = "ja" ] || { [ -n "${SMARTCTL_PATH:-}" ] && [ -x "${SMARTCTL_PATH}" ]; }; then
        SMART_DIAGNOSIS='smartmontools/smartctl verfügbar; erweiterte SMART/NVMe-Daten werden verwendet'
      else
        SMART_DIAGNOSIS='smartmontools/smartctl nicht installiert; native macOS-SMART-Daten werden verwendet'
      fi
      printf 'SMARTMONTOOLS_PACKAGE=%s\n' "$SMARTMONTOOLS_PACKAGE"
      printf 'SMARTMONTOOLS_VERSION=%s\n' "$SMARTMONTOOLS_VERSION"
      printf 'SMARTCTL_AVAILABLE=%s\n' "$SMARTCTL_AVAILABLE"
      printf 'SMARTCTL_EXECUTABLE=%s\n' "$SMARTCTL_EXECUTABLE"
      printf 'SMARTCTL_PATH=%s\n' "$SMARTCTL_PATH"
      printf 'SMARTCTL_DIAGNOSTIC=%s\n' "$SMART_DIAGNOSIS"
      SMART_ROWS=''
      SMART_DISK_COUNT=0
      SMART_HEALTH_OK=0
      SMART_HEALTH_WARN=0
      for PDEV in $(diskutil list 2>/dev/null | awk '$1 ~ "^/dev/disk[0-9]+$" && $2 ~ "^[(]" {print $1}' | sort -u); do
        [ -n "$PDEV" ] || continue
        PINFO=$(LC_ALL=C LANG=C diskutil info "$PDEV" 2>/dev/null)
        MEDIA=$(printf '%s\n' "$PINFO" | awk -F': *' '/Media Name/{print $2;exit}')
        PROTO=$(printf '%s\n' "$PINFO" | awk -F': *' '/Protocol/{print $2;exit}')
        INTERNAL=$(printf '%s\n' "$PINFO" | awk -F': *' '/Internal/{print $2;exit}')
        SMART_NATIVE=$(printf '%s\n' "$PINFO" | awk -F': *' '/SMART Status/{print $2;exit}')
        HEALTH="${SMART_NATIVE:--}"
        TEMP='-'; HOURS='-'; PERCENT_USED='-'; MEDIA_ERRORS='-'; CRITICAL='-'; POWER_CYCLES='-'; UNSAFE='-'; DATA_READ='-'; DATA_WRITTEN='-'
        SMART_MODE=nativ
        SMART_TOOL_RESULT='nicht ausgefuehrt'
        if [ "$SMARTCTL_EXECUTABLE" = ja ] && [ -n "$SMARTCTL_PATH" ]; then
          SOUT=$("$SMARTCTL_PATH" -a "$PDEV" 2>&1)
          SRC=$?
          if [ "$SRC" -ne 0 ] && sudo -n true >/dev/null 2>&1; then
            SOUT=$(sudo -n "$SMARTCTL_PATH" -a "$PDEV" 2>&1)
            SRC=$?
          fi
          if [ -n "$SOUT" ]; then
            if printf '%s\n' "$SOUT" | grep -qiE 'Unsupported|SMART support is: Unavailable|does not support SMART|Unknown USB bridge|No such device'; then
              SMART_MODE='nicht unterstützt'
              SMART_TOOL_RESULT=$(printf '%s\n' "$SOUT" | head -n 2 | tr '\n' ' ' | sed 's/[[:space:]]*$//')
            else
              SMART_MODE=smartctl
              SMART_TOOL_RESULT="Exit $SRC"
              PASSED=$(printf '%s\n' "$SOUT" | awk -F': *' '/SMART overall-health self-assessment test result|SMART Health Status/{print $2;exit}')
              [ -n "$PASSED" ] && HEALTH="$PASSED"
              TEMP=$(printf '%s\n' "$SOUT" | awk -F': *' '/^Temperature:/{print $2;exit} /Temperature_Celsius/{print $(NF-0);exit}')
              HOURS=$(printf '%s\n' "$SOUT" | awk -F': *' '/Power On Hours:/{print $2;exit} /Power_On_Hours/{print $(NF-0);exit}')
              PERCENT_USED=$(printf '%s\n' "$SOUT" | awk -F': *' '/Percentage Used:/{print $2;exit}')
              MEDIA_ERRORS=$(printf '%s\n' "$SOUT" | awk -F': *' '/Media and Data Integrity Errors:/{print $2;exit}')
              CRITICAL=$(printf '%s\n' "$SOUT" | awk -F': *' '/Critical Warning:/{print $2;exit}')
              POWER_CYCLES=$(printf '%s\n' "$SOUT" | awk -F': *' '/Power Cycles:/{print $2;exit} /Power_Cycle_Count/{print $(NF-0);exit}')
              UNSAFE=$(printf '%s\n' "$SOUT" | awk -F': *' '/Unsafe Shutdowns:/{print $2;exit}')
              DATA_READ=$(printf '%s\n' "$SOUT" | awk -F': *' '/Data Units Read:/{print $2;exit}')
              DATA_WRITTEN=$(printf '%s\n' "$SOUT" | awk -F': *' '/Data Units Written:/{print $2;exit}')
            fi
          else
            SMART_MODE='smartctl ohne Ausgabe'
            SMART_TOOL_RESULT="Exit $SRC"
          fi
        fi
        [ -n "$TEMP" ] || TEMP='-'; [ -n "$HOURS" ] || HOURS='-'; [ -n "$PERCENT_USED" ] || PERCENT_USED='-'; [ -n "$MEDIA_ERRORS" ] || MEDIA_ERRORS='-'; [ -n "$CRITICAL" ] || CRITICAL='-'
        case "$(printf '%s' "$HEALTH" | tr '[:upper:]' '[:lower:]')" in
          *verified*|*passed*|*ok*) SMART_HEALTH_OK=$((SMART_HEALTH_OK+1));;
          *fail*|*warn*|*fatal*) SMART_HEALTH_WARN=$((SMART_HEALTH_WARN+1));;
        esac
        SMART_DISK_COUNT=$((SMART_DISK_COUNT+1))
        ROW="$PDEV|${MEDIA:--}|${PROTO:--}|${INTERNAL:--}|${SMART_MODE:--}|${SMART_TOOL_RESULT:--}|${HEALTH:--}|${TEMP:--}|${HOURS:--}|${PERCENT_USED:--}|${MEDIA_ERRORS:--}|${CRITICAL:--}|${POWER_CYCLES:--}|${UNSAFE:--}|${DATA_READ:--}|${DATA_WRITTEN:--}"
        if [ -n "$SMART_ROWS" ]; then SMART_ROWS="$SMART_ROWS ; $ROW"; else SMART_ROWS="$ROW"; fi
      done
      printf 'SMART_DISK_COUNT=%s\n' "$SMART_DISK_COUNT"
      printf 'SMART_HEALTH_OK=%s\n' "$SMART_HEALTH_OK"
      printf 'SMART_HEALTH_WARN=%s\n' "$SMART_HEALTH_WARN"
      printf 'SMART_DETAILS=%s\n' "$SMART_ROWS"

      MEMPROF=$(LC_ALL=C LANG=C system_profiler SPMemoryDataType 2>/dev/null)
      printf 'MEMORY_MODULE_DETAILS='; printf '%s\n' "$MEMPROF" | awk '
        /BANK|DIMM/ {
          line=$0; gsub(/^[ \t]+|:$/,"",line);
          if (line ~ /BANK/ || line ~ /^DIMM/) { if(n++)printf " ; "; printf "%s",line }
        }
        /Size:|Type:|Speed:|Status:|Manufacturer:|Part Number:|Serial Number:/ {
          line=$0; gsub(/^[ \t]+/,"",line); printf "|%s",line
        }
        END{print ""}'
      printf 'MEMORY_ECC='; printf '%s\n' "$MEMPROF" | awk -F': *' '/ECC:/{print $2;exit}'; echo
      printf 'MEMORY_UPGRADEABLE_HINT='; if printf '%s\n' "$MEMPROF" | grep -qE 'BANK [0-9]+/DIMM|DIMM[0-9]*:'; then echo Module erkannt; else echo keine separaten DIMM-Module gemeldet; fi

      GPUPROF=$(LC_ALL=C LANG=C system_profiler SPDisplaysDataType 2>/dev/null)
      printf 'GPU_DETAILS='; printf '%s\n' "$GPUPROF" | awk '
        /Chipset Model:/{if(n++)printf " ; "; gsub(/^[ \t]+/,""); printf "%s",$0}
        /Type:|Bus:|VRAM|Metal Support:|Vendor:|Device ID:/{gsub(/^[ \t]+/,""); printf "|%s",$0}
        END{print ""}'
      printf 'DISPLAY_DETAILS='; printf '%s\n' "$GPUPROF" | awk '
        /Display Type:|Resolution:|UI Looks like:|Framebuffer Depth:|Main Display:|Mirror:|Online:|Rotation:|Automatically Adjust Brightness:/{gsub(/^[ \t]+/,""); if(n++)printf " ; "; printf "%s",$0}
        END{print ""}'

      USBPROF=$(LC_ALL=C LANG=C system_profiler SPUSBDataType 2>/dev/null)
      printf 'USB_DEVICE_COUNT='; printf '%s\n' "$USBPROF" | grep -cE '^[[:space:]]{8,}[^ ].*:$' | tr -d ' '
      printf 'USB_DETAILS='; printf '%s\n' "$USBPROF" | awk '
        /^[ \t]+[^ ].*:$/ && $0 !~ /USB [0-9.]+ Bus:/ {gsub(/^[ \t]+|:$/,""); if(n++)printf " ; "; printf "%s",$0}
        /Product ID:|Vendor ID:|Speed:|Manufacturer:|Location ID:|Current Available|Current Required/{gsub(/^[ \t]+/,""); printf "|%s",$0}
        END{print ""}'

      TBPROF=$(LC_ALL=C LANG=C system_profiler SPThunderboltDataType 2>/dev/null)
      printf 'THUNDERBOLT_DETAILS='; printf '%s\n' "$TBPROF" | awk '
        /Thunderbolt Bus:|Device Name:|Vendor Name:|Device UID:|Route String:|Firmware Version:|Port:|Status:/{gsub(/^[ \t]+/,""); if(n++)printf " ; "; printf "%s",$0}
        END{print ""}'

      PCIPROF=$(LC_ALL=C LANG=C system_profiler SPPCIDataType 2>/dev/null)
      printf 'PCI_DETAILS='; printf '%s\n' "$PCIPROF" | awk '
        /Name:|Type:|Driver Installed:|MSI:|Bus:|Slot:|Vendor ID:|Device ID:|Subsystem Vendor ID:|Subsystem ID:/{gsub(/^[ \t]+/,""); if(n++)printf " ; "; printf "%s",$0}
        END{print ""}'

      NETPROF=$(LC_ALL=C LANG=C system_profiler SPNetworkDataType 2>/dev/null)
      printf 'NETWORK_HARDWARE_DETAILS='; printf '%s\n' "$NETPROF" | awk '
        /Type:|Hardware:|BSD Device Name:|IPv4 Addresses:|IPv6 Addresses:|Ethernet:|MAC Address:|Media Options:|Media Subtype:/{gsub(/^[ \t]+/,""); if(n++)printf " ; "; printf "%s",$0}
        END{print ""}'

      printf 'FAN_COUNT='; ioreg -r -c AppleFan 2>/dev/null | grep -c '"IOClass"' | tr -d ' '
      printf 'THERMAL_LEVEL='; sysctl -n kern.thermal_pressure 2>/dev/null || pmset -g therm 2>/dev/null | tr '\n' ' ' | sed 's/[[:space:]]*$//'; echo
      CPU_THERMAL_LEVEL_VALUE=$(sysctl -n kern.cpu_thermal_level 2>/dev/null || true)
      [ -n "${CPU_THERMAL_LEVEL_VALUE}" ] || CPU_THERMAL_LEVEL_VALUE='nicht_verfügbar'
      printf 'CPU_THERMAL_LEVEL=%s\\n' "${CPU_THERMAL_LEVEL_VALUE}"
      GPU_THERMAL_LEVEL_VALUE=$(sysctl -n kern.gpu_thermal_level 2>/dev/null || true)
      [ -n "${GPU_THERMAL_LEVEL_VALUE}" ] || GPU_THERMAL_LEVEL_VALUE='nicht_verfügbar'
      printf 'GPU_THERMAL_LEVEL=%s\\n' "${GPU_THERMAL_LEVEL_VALUE}"
      SCHEDULER_THERMAL_LEVEL_VALUE=$(sysctl -n kern.sched_thermal_level 2>/dev/null || true)
      [ -n "${SCHEDULER_THERMAL_LEVEL_VALUE}" ] || SCHEDULER_THERMAL_LEVEL_VALUE='nicht_verfügbar'
      printf 'SCHEDULER_THERMAL_LEVEL=%s\\n' "${SCHEDULER_THERMAL_LEVEL_VALUE}"
      # 40k36r8: leere optionale Hardwarefelder explizit markieren, damit der Parser keine Folgefelder verschluckt.
      [ -n "${BATTERY_CHARGING:-}" ] || BATTERY_CHARGING='nicht_verfügbar'
      [ -n "${BATTERY_CYCLE_COUNT:-}" ] || BATTERY_CYCLE_COUNT='nicht_verfügbar'
      [ -n "${BATTERY_CONDITION:-}" ] || BATTERY_CONDITION='nicht_verfügbar'
      [ -n "${BATTERY_MAX_CAPACITY:-}" ] || BATTERY_MAX_CAPACITY='nicht_verfügbar'
      [ -n "${BATTERY_FULL_CHARGE_CAPACITY:-}" ] || BATTERY_FULL_CHARGE_CAPACITY='nicht_verfügbar'
      [ -n "${BATTERY_DESIGN_CAPACITY:-}" ] || BATTERY_DESIGN_CAPACITY='nicht_verfügbar'
      [ -n "${BATTERY_HEALTH:-}" ] || BATTERY_HEALTH='nicht_verfügbar'
      [ -n "${BATTERY_SERIAL:-}" ] || BATTERY_SERIAL='nicht_verfügbar'
      [ -n "${BATTERY_MANUFACTURER:-}" ] || BATTERY_MANUFACTURER='nicht_verfügbar'
      [ -n "${BATTERY_FIRMWARE:-}" ] || BATTERY_FIRMWARE='nicht_verfügbar'
      [ -n "${BATTERY_FULLY_CHARGED:-}" ] || BATTERY_FULLY_CHARGED='nicht_verfügbar'
      [ -n "${BATTERY_CHARGE_BELOW_WARNING:-}" ] || BATTERY_CHARGE_BELOW_WARNING='nicht_verfügbar'
      [ -n "${AC_CONNECTED:-}" ] || AC_CONNECTED='nicht_verfügbar'
      [ -n "${AC_WATTAGE:-}" ] || AC_WATTAGE='nicht_verfügbar'
      [ -n "${AC_SERIAL:-}" ] || AC_SERIAL='nicht_verfügbar'
      [ -n "${LOW_POWER_MODE:-}" ] || LOW_POWER_MODE='nicht_verfügbar'
      USER_ROWS=''
      USER_COUNT=0
      ADMIN_USERS=$(dscl . -read /Groups/admin GroupMembership 2>/dev/null | cut -d: -f2-)
      for U in $(dscl . list /Users UniqueID 2>/dev/null | awk '$2 >= 500 && $1 !~ /^_/ {print $1}'); do
        UIDV=$(id -u "$U" 2>/dev/null)
        GIDV=$(id -g "$U" 2>/dev/null)
        REAL=$(dscl . -read "/Users/$U" RealName 2>/dev/null | tail -n +2 | sed 's/^[[:space:]]*//' | paste -sd ' ' -)
        HOMEV=$(dscl . -read "/Users/$U" NFSHomeDirectory 2>/dev/null | awk '{print $2}')
        SHELLV=$(dscl . -read "/Users/$U" UserShell 2>/dev/null | awk '{print $2}')
        ADMIN=no; case " $ADMIN_USERS " in *" $U "*) ADMIN=yes;; esac
        ROW="$U|${REAL:--}|${UIDV:--}|${GIDV:--}|${HOMEV:--}|${SHELLV:--}|$ADMIN"
        if [ -n "$USER_ROWS" ]; then USER_ROWS="$USER_ROWS ; $ROW"; else USER_ROWS="$ROW"; fi
        USER_COUNT=$((USER_COUNT+1))
      done
      printf 'USER_COUNT=%s\n' "$USER_COUNT"
      printf 'USER_DETAILS=%s\n' "$USER_ROWS"
      printf 'ADMIN_USERS='; printf '%s\n' "$ADMIN_USERS" | xargs | tr ' ' ','; echo

      # 40k36q7: vollständiges, nach System und Benutzer gegliedertes Softwareinventar.
      SOFTWARE_TMP="/tmp/gam_macos_software_$$"
      mkdir -p "$SOFTWARE_TMP"
      : > "$SOFTWARE_TMP/system.tsv"
      : > "$SOFTWARE_TMP/users.tsv"

      add_app_dir() {
        OWNER="$1"; CLASS="$2"; ROOT="$3"; [ -d "$ROOT" ] || return 0
        find "$ROOT" -maxdepth 3 -type d -name '*.app' -print 2>/dev/null | while IFS= read -r APP; do
          PLIST="$APP/Contents/Info.plist"
          NAME=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleDisplayName' "$PLIST" 2>/dev/null || /usr/libexec/PlistBuddy -c 'Print :CFBundleName' "$PLIST" 2>/dev/null || basename "$APP" .app)
          VER=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$PLIST" 2>/dev/null || /usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$PLIST" 2>/dev/null || echo '-')
          BID=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$PLIST" 2>/dev/null || echo '-')
          SOURCE='Application'
          if [ -f "$APP/Contents/_MASReceipt/receipt" ] || [ -d "$APP/Contents/_MASReceipt" ]; then
            SOURCE='Mac App Store'
          fi
          if [ "$CLASS" = system ]; then
            printf '%s\\t%s\\t%s\\t%s\\t%s\\n' "$NAME" "$VER" "$BID" "$APP" "$SOURCE" >> "$SOFTWARE_TMP/system.tsv"
          else
            printf '%s\\t%s\\t%s\\t%s\\t%s\\t%s\\n' "$OWNER" "$NAME" "$VER" "$BID" "$APP" "$SOURCE" >> "$SOFTWARE_TMP/users.tsv"
          fi
        done
      }

      add_app_dir SYSTEM system /System/Applications
      add_app_dir SYSTEM system /Applications

      USER_SOFTWARE_SCAN_DETAILS=''
      USER_SOFTWARE_SCAN_COMPLETE=yes
      while IFS='|' read -r U HOMEV; do
        [ -n "$U" ] && [ -n "$HOMEV" ] || continue
        BEFORE=$(wc -l < "$SOFTWARE_TMP/users.tsv" | tr -d ' ')
        USER_STATE=vollständig-geprüft
        if [ ! -d "$HOMEV" ]; then
          USER_STATE=Home-Verzeichnis-fehlt; USER_SOFTWARE_SCAN_COMPLETE=no
        elif [ ! -r "$HOMEV" ]; then
          USER_STATE=Home-Verzeichnis-nicht-lesbar; USER_SOFTWARE_SCAN_COMPLETE=no
        elif [ -d "$HOMEV/Applications" ]; then
          if ! find "$HOMEV/Applications" -maxdepth 3 -type d -name '*.app' -print -quit >/dev/null 2>&1; then USER_STATE=Applications-nicht-lesbar; USER_SOFTWARE_SCAN_COMPLETE=no; fi
          add_app_dir "$U" user "$HOMEV/Applications"
        else
          USER_STATE=vollständig-geprüft-kein-Applications-Verzeichnis
        fi
        # Seltene benutzerspezifische Homebrew-Präfixe ebenfalls prüfen.
        for BREW in "$HOMEV/homebrew/bin/brew" "$HOMEV/.homebrew/bin/brew" "$HOMEV/.linuxbrew/bin/brew"; do
          [ -x "$BREW" ] || continue
          "$BREW" list --versions 2>/dev/null | while IFS= read -r ROW; do
            NAME=$(printf '%s' "$ROW" | awk '{print $1}'); VER=$(printf '%s' "$ROW" | cut -d' ' -f2-)
            [ -n "$NAME" ] && printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$U" "$NAME" "${VER:--}" "-" "$BREW" "Homebrew (Benutzer)" >> "$SOFTWARE_TMP/users.tsv"
          done
        done
        AFTER=$(wc -l < "$SOFTWARE_TMP/users.tsv" | tr -d ' ')
        FOUND=$((AFTER-BEFORE))
        ITEM="$U|$HOMEV|$USER_STATE|$FOUND"
        if [ -n "$USER_SOFTWARE_SCAN_DETAILS" ]; then USER_SOFTWARE_SCAN_DETAILS="$USER_SOFTWARE_SCAN_DETAILS ; $ITEM"; else USER_SOFTWARE_SCAN_DETAILS="$ITEM"; fi
      done <<EOF
$(dscl . -list /Users UniqueID 2>/dev/null | awk '$2>=500 && $1!="_mbsetupuser"{u=$1; cmd="dscl . -read /Users/"u" NFSHomeDirectory 2>/dev/null"; cmd|getline h; close(cmd); sub(/^NFSHomeDirectory:[[:space:]]*/,"",h); print u"|"h}')
EOF

      if command -v pkgutil >/dev/null 2>&1; then
        pkgutil --pkgs 2>/dev/null | while IFS= read -r PKG; do
          [ -n "$PKG" ] || continue
          VER=$(pkgutil --pkg-info "$PKG" 2>/dev/null | awk -F': ' '/^version:/{print $2;exit}')
          printf '%s\\t%s\\t%s\\t%s\\t%s\\n' "$PKG" "${VER:--}" "$PKG" "-" "Installer-Paket" >> "$SOFTWARE_TMP/system.tsv"
        done
      fi

      for BREW in /opt/homebrew/bin/brew /usr/local/bin/brew; do
        [ -x "$BREW" ] || continue
        "$BREW" list --versions 2>/dev/null | while IFS= read -r ROW; do
          NAME=$(printf '%s' "$ROW" | awk '{print $1}'); VER=$(printf '%s' "$ROW" | cut -d' ' -f2-)
          [ -n "$NAME" ] && printf '%s\\t%s\\t%s\\t%s\\t%s\\n' "$NAME" "${VER:--}" "-" "$BREW" "Homebrew" >> "$SOFTWARE_TMP/system.tsv"
        done
        "$BREW" list --cask --versions 2>/dev/null | while IFS= read -r ROW; do
          NAME=$(printf '%s' "$ROW" | awk '{print $1}'); VER=$(printf '%s' "$ROW" | cut -d' ' -f2-)
          [ -n "$NAME" ] && printf '%s\\t%s\\t%s\\t%s\\t%s\\n' "$NAME" "${VER:--}" "-" "$BREW" "Homebrew Cask" >> "$SOFTWARE_TMP/system.tsv"
        done
      done

      MAS_AVAILABLE=nein
      MAS_VERSION=''
      MAS_APP_COUNT=0
      MAS_APP_DETAILS=''
      if command -v mas >/dev/null 2>&1; then
        MAS_AVAILABLE=ja
        MAS_VERSION=$(mas version 2>/dev/null | head -n 1)
        while IFS= read -r MROW; do
          [ -n "$MROW" ] || continue
          MAS_APP_COUNT=$((MAS_APP_COUNT+1))
          MID=$(printf '%s' "$MROW" | awk '{print $1}')
          MREST=$(printf '%s' "$MROW" | cut -d' ' -f2-)
          MITEM="$MID|$MREST"
          if [ -n "$MAS_APP_DETAILS" ]; then MAS_APP_DETAILS="$MAS_APP_DETAILS ; $MITEM"; else MAS_APP_DETAILS="$MITEM"; fi
        done <<EOF
$(mas list 2>/dev/null || true)
EOF
      fi

      sort -fu "$SOFTWARE_TMP/system.tsv" -o "$SOFTWARE_TMP/system.tsv"
      sort -fu "$SOFTWARE_TMP/users.tsv" -o "$SOFTWARE_TMP/users.tsv"
      APP_SYSTEM_COUNT=$(wc -l < "$SOFTWARE_TMP/system.tsv" | tr -d ' ')
      APP_USER_COUNT=$(wc -l < "$SOFTWARE_TMP/users.tsv" | tr -d ' ')
      APP_COUNT=$((APP_SYSTEM_COUNT+APP_USER_COUNT))
      SOFTWARE_USER_COUNT=$(cut -f1 "$SOFTWARE_TMP/users.tsv" 2>/dev/null | sort -u | sed '/^$/d' | wc -l | tr -d ' ')
      SOFTWARE_SOURCE_APPLICATION=$(awk -F'\t' '$5=="Application"{n++}END{print n+0}' "$SOFTWARE_TMP/system.tsv")
      SOFTWARE_SOURCE_MAC_APP_STORE=$(awk -F'\t' '$5=="Mac App Store"{n++}END{print n+0}' "$SOFTWARE_TMP/system.tsv")
      SOFTWARE_SOURCE_PKG=$(awk -F'\t' '$5=="Installer-Paket"{n++}END{print n+0}' "$SOFTWARE_TMP/system.tsv")
      SOFTWARE_SOURCE_HOMEBREW=$(awk -F'\t' '$5=="Homebrew"{n++}END{print n+0}' "$SOFTWARE_TMP/system.tsv")
      SOFTWARE_SOURCE_HOMEBREW_CASK=$(awk -F'\t' '$5=="Homebrew Cask"{n++}END{print n+0}' "$SOFTWARE_TMP/system.tsv")

      SYSTEM_SOFTWARE_ROWS=''
      while IFS="$(printf '\\t')" read -r NAME VER BID PATHV SOURCE; do
        [ -n "$NAME" ] || continue
        ROW="$NAME|${VER:--}|${BID:--}|${PATHV:--}|${SOURCE:--}"
        if [ -n "$SYSTEM_SOFTWARE_ROWS" ]; then SYSTEM_SOFTWARE_ROWS="$SYSTEM_SOFTWARE_ROWS ; $ROW"; else SYSTEM_SOFTWARE_ROWS="$ROW"; fi
      done < "$SOFTWARE_TMP/system.tsv"

      USER_SOFTWARE_ROWS=''
      while IFS="$(printf '\\t')" read -r OWNER NAME VER BID PATHV SOURCE; do
        [ -n "$OWNER" ] && [ -n "$NAME" ] || continue
        ROW="$OWNER|$NAME|${VER:--}|${BID:--}|${PATHV:--}|${SOURCE:--}"
        if [ -n "$USER_SOFTWARE_ROWS" ]; then USER_SOFTWARE_ROWS="$USER_SOFTWARE_ROWS ; $ROW"; else USER_SOFTWARE_ROWS="$ROW"; fi
      done < "$SOFTWARE_TMP/users.tsv"

      printf 'APP_COUNT=%s\\n' "$APP_COUNT"
      printf 'APP_SYSTEM_COUNT=%s\\n' "$APP_SYSTEM_COUNT"
      printf 'APP_USER_COUNT=%s\\n' "$APP_USER_COUNT"
      printf 'SOFTWARE_USER_COUNT=%s\\n' "$SOFTWARE_USER_COUNT"
      printf 'USER_SOFTWARE_SCAN_STATUS=%s\\n' "$([ "$USER_SOFTWARE_SCAN_COMPLETE" = yes ] && echo vollständig-geprüft || echo teilweise-geprüft)"
      printf 'USER_SOFTWARE_SCAN_DETAILS=%s\\n' "$USER_SOFTWARE_SCAN_DETAILS"
      printf 'SYSTEM_SOFTWARE_DETAILS=%s\\n' "$SYSTEM_SOFTWARE_ROWS"
      printf 'USER_SOFTWARE_DETAILS=%s\\n' "$USER_SOFTWARE_ROWS"
      printf 'APP_DETAILS=%s\\n' "$SYSTEM_SOFTWARE_ROWS${USER_SOFTWARE_ROWS:+ ; $USER_SOFTWARE_ROWS}"
      printf 'MAS_AVAILABLE=%s\\n' "$MAS_AVAILABLE"
      printf 'MAS_VERSION=%s\\n' "$MAS_VERSION"
      printf 'MAS_APP_COUNT=%s\\n' "$MAS_APP_COUNT"
      printf 'MAS_APP_DETAILS=%s\\n' "$MAS_APP_DETAILS"
      printf 'SOFTWARE_SOURCE_APPLICATION=%s\\n' "$SOFTWARE_SOURCE_APPLICATION"
      printf 'SOFTWARE_SOURCE_MAC_APP_STORE=%s\\n' "$SOFTWARE_SOURCE_MAC_APP_STORE"
      printf 'SOFTWARE_SOURCE_PKG=%s\\n' "$SOFTWARE_SOURCE_PKG"
      printf 'SOFTWARE_SOURCE_HOMEBREW=%s\\n' "$SOFTWARE_SOURCE_HOMEBREW"
      printf 'SOFTWARE_SOURCE_HOMEBREW_CASK=%s\\n' "$SOFTWARE_SOURCE_HOMEBREW_CASK"
      rm -rf "$SOFTWARE_TMP"

      printf 'LAUNCHD_LOADED_COUNT='; launchctl list 2>/dev/null | tail -n +2 | wc -l | tr -d ' '
      printf 'LAUNCH_AGENTS_SYSTEM_COUNT='; find /Library/LaunchAgents /System/Library/LaunchAgents -maxdepth 1 -name '*.plist' 2>/dev/null | wc -l | tr -d ' '
      printf 'LAUNCH_AGENTS_USER_COUNT='; find "$HOME/Library/LaunchAgents" -maxdepth 1 -name '*.plist' 2>/dev/null | wc -l | tr -d ' '
      printf 'LAUNCH_DAEMONS_COUNT='; find /Library/LaunchDaemons /System/Library/LaunchDaemons -maxdepth 1 -name '*.plist' 2>/dev/null | wc -l | tr -d ' '
      printf 'LAUNCHD_DETAILS='; launchctl list 2>/dev/null | tail -n +2 | awk 'BEGIN{first=1}{if(!first)printf " ; "; first=0; printf "%s|%s|%s",$3,$1,$2}' ; echo
      printf 'LOGIN_ITEMS='; osascript -e 'tell application "System Events" to get the name of every login item' 2>/dev/null | sed 's/, /,/g'; echo
      printf 'HOMEBREW='; if command -v brew >/dev/null 2>&1; then brew --version 2>/dev/null | head -1; else echo 'nicht installiert'; fi
      printf 'MACPORTS='; if command -v port >/dev/null 2>&1; then port version 2>/dev/null | head -1; else echo 'nicht installiert'; fi
      printf 'XCODE_CLT='; xcode-select -p 2>/dev/null || echo 'nicht installiert'
      UPDATE_INFO=$(softwareupdate -l 2>/dev/null)
      printf 'AVAILABLE_UPDATES='; printf '%s\n' "$UPDATE_INFO" | grep -c '^\\* Label:' | tr -d ' '
      printf 'AVAILABLE_UPDATE_DETAILS='; printf '%s\n' "$UPDATE_INFO" | sed -n 's/^[[:space:]]*\\* Label: //p' | paste -sd ';' -; echo

      NETWORK_ROWS=''
      NETWORK_COUNT=0
      ACTIVE_NETWORK_COUNT=0
      while IFS= read -r IFACE; do
        [ -n "$IFACE" ] || continue
        FLAGS=$(ifconfig "$IFACE" 2>/dev/null | head -1)
        STATUS=inactive; printf '%s' "$FLAGS" | grep -q 'UP' && STATUS=active
        [ "$STATUS" = active ] && ACTIVE_NETWORK_COUNT=$((ACTIVE_NETWORK_COUNT+1))
        MAC=$(ifconfig "$IFACE" 2>/dev/null | awk '/ether /{print $2; exit}')
        IPV4=$(ipconfig getifaddr "$IFACE" 2>/dev/null || true)
        IPV6=$(ifconfig "$IFACE" 2>/dev/null | awk '/inet6 / && $2 !~ /^fe80/ {print $2; exit}')
        MTU=$(ifconfig "$IFACE" 2>/dev/null | awk 'NR==1{for(i=1;i<=NF;i++)if($i=="mtu"){print $(i+1);exit}}')
        MEDIA=$(ifconfig "$IFACE" 2>/dev/null | awk -F': ' '/^[[:space:]]*media:/{print $2;exit}')
        ROW="$IFACE|${STATUS}|${MAC:--}|${IPV4:--}|${IPV6:--}|${MTU:--}|${MEDIA:--}"
        if [ -n "$NETWORK_ROWS" ]; then NETWORK_ROWS="$NETWORK_ROWS ; $ROW"; else NETWORK_ROWS="$ROW"; fi
        NETWORK_COUNT=$((NETWORK_COUNT+1))
      done <<EOF
$(ifconfig -l 2>/dev/null | tr ' ' '\n')
EOF
      printf 'NETWORK_ADAPTER_COUNT=%s\n' "$NETWORK_COUNT"
      printf 'ACTIVE_NETWORK_ADAPTER_COUNT=%s\n' "$ACTIVE_NETWORK_COUNT"
      printf 'NETWORK_ADAPTER_DETAILS=%s\n' "$NETWORK_ROWS"
      printf 'DEFAULT_GATEWAY='; route -n get default 2>/dev/null | awk '/gateway:/{print $2;exit}'
      printf 'DEFAULT_INTERFACE='; route -n get default 2>/dev/null | awk '/interface:/{print $2;exit}'
      printf 'DNS_SERVERS='; scutil --dns 2>/dev/null | awk '/nameserver\\[[0-9]+\\]/{print $3}' | sort -u | paste -sd ',' -; echo
      printf 'SEARCH_DOMAINS='; scutil --dns 2>/dev/null | awk '/search domain\\[[0-9]+\\]/{print $4}' | sort -u | paste -sd ',' -; echo
      printf 'COMPUTER_NAME='; scutil --get ComputerName 2>/dev/null
      printf 'LOCAL_HOST_NAME='; scutil --get LocalHostName 2>/dev/null
      printf 'HOST_NAME='; scutil --get HostName 2>/dev/null || true; echo
      printf 'PROXY_CONFIGURATION='; scutil --proxy 2>/dev/null | awk '/HTTPEnable|HTTPProxy|HTTPPort|HTTPSEnable|HTTPSProxy|HTTPSPort|SOCKSEnable|SOCKSProxy|SOCKSPort/{gsub(/^[[:space:]]+/,"");printf "%s=%s; ",$1,$3}' ; echo
      printf 'WIFI_INTERFACE='; networksetup -listallhardwareports 2>/dev/null | awk '/Hardware Port: (Wi-Fi|AirPort)/{getline;print $2;exit}'
      WIFI_IF=$(networksetup -listallhardwareports 2>/dev/null | awk '/Hardware Port: (Wi-Fi|AirPort)/{getline;print $2;exit}')
      if [ -n "$WIFI_IF" ]; then
        WIFI_INFO=$(system_profiler SPAirPortDataType 2>/dev/null)
        printf 'WIFI_POWER='; networksetup -getairportpower "$WIFI_IF" 2>/dev/null | awk '{print $NF}'
        printf 'WIFI_SSID='; networksetup -getairportnetwork "$WIFI_IF" 2>/dev/null | sed 's/^Current Wi-Fi Network: //;s/^You are not associated.*$/nicht verbunden/'
        printf 'WIFI_MAC='; ifconfig "$WIFI_IF" 2>/dev/null | awk '/ether /{print $2;exit}'
        printf 'WIFI_PROTOCOLS='; printf '%s\n' "$WIFI_INFO" | awk -F': ' '/Supported PHY Modes:/{print $2;exit}'
        printf 'WIFI_CHANNEL='; printf '%s\n' "$WIFI_INFO" | awk -F': ' '/Channel:/{print $2;exit}'
        printf 'WIFI_SECURITY='; printf '%s\n' "$WIFI_INFO" | awk -F': ' '/Security:/{print $2;exit}'
      fi
      BT_INFO=$(LC_ALL=C LANG=C system_profiler SPBluetoothDataType 2>/dev/null)
      printf 'BLUETOOTH_PRESENT='; if [ -n "$BT_INFO" ]; then echo ja; else echo nein; fi
      printf 'BLUETOOTH_STATE='; printf '%s\n' "$BT_INFO" | awk -F': ' '/State:/{print $2;exit}'
      printf 'BLUETOOTH_CHIPSET='; printf '%s\n' "$BT_INFO" | awk -F': ' '/Chipset:/{print $2;exit}'
      printf 'BLUETOOTH_ADDRESS='; printf '%s\n' "$BT_INFO" | awk -F': ' '/Address:/{print $2;exit}'
      printf 'BLUETOOTH_VERSION='; printf '%s\n' "$BT_INFO" | awk -F': ' '/Bluetooth Core Spec:/{print $2;exit}'
      printf 'BLUETOOTH_CONNECTED_DEVICES='; printf '%s\n' "$BT_INFO" | awk '/Connected:/{f=1;next}/Not Connected:/{f=0}f&&/^[[:space:]]{12}[^:]+:/{gsub(/^[[:space:]]+|:$/ ,"");if(n++)printf ",";printf $0}' ; echo
      printf 'BONJOUR_MDNS='; if pgrep -x mDNSResponder >/dev/null 2>&1; then echo aktiv; else echo nicht erkannt; fi
      printf 'BONJOUR_HOST='; scutil --get LocalHostName 2>/dev/null | sed 's/$/.local/'
      printf 'LISTENING_TCP_PORTS='; lsof -nP -iTCP -sTCP:LISTEN 2>/dev/null | awk 'NR>1{split($9,a,":");print a[length(a)]}' | sort -nu | paste -sd ',' -; echo
      printf 'VPN_SERVICES='; scutil --nc list 2>/dev/null | awk -F'"' '/\\(Connected\\)|\\(Disconnected\\)/{if(n++)printf ",";printf $2}' ; echo
      printf 'ARP_NEIGHBOR_COUNT='; arp -an 2>/dev/null | wc -l | tr -d ' '

      POWER_INFO=$(LC_ALL=C LANG=C system_profiler SPPowerDataType 2>/dev/null)
      BATT_LINE=$(pmset -g batt 2>/dev/null | tail -n 1)
      if printf '%s\n' "$POWER_INFO $BATT_LINE" | grep -qiE 'Battery Information|InternalBattery|%'; then
        BATTERY_PRESENT='ja'
      else
        BATTERY_PRESENT='nein'
      fi
      printf 'BATTERY_PRESENT=%s\n' "$BATTERY_PRESENT"
      printf 'POWER_SOURCE='; pmset -g batt 2>/dev/null | head -n 1 | sed -E "s/.*Now drawing from '(.*)'.*/\1/"
      if [ "$BATTERY_PRESENT" = 'ja' ]; then
        printf 'BATTERY_PERCENT='; printf '%s\n' "$BATT_LINE" | sed -nE 's/.*[[:space:]]([0-9]+)%;.*/\1/p'; echo
        printf 'BATTERY_CHARGING='; case "$BATT_LINE" in *charging*|*Charging*) echo ja;; *discharging*|*Discharging*) echo nein;; *) echo unbekannt;; esac
        printf 'BATTERY_TIME_REMAINING='; printf '%s\n' "$BATT_LINE" | sed -nE 's/.*;[[:space:]]*([^;]+)[[:space:]]*remaining.*/\1/p'; echo
        printf 'BATTERY_CYCLE_COUNT='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Cycle Count:/{print $2;exit}'; echo
        printf 'BATTERY_CONDITION='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Condition:/{print $2;exit}'; echo
        printf 'BATTERY_MAX_CAPACITY='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Maximum Capacity:/{print $2;exit}'; echo
        printf 'BATTERY_FULL_CHARGE_CAPACITY='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Full Charge Capacity/{print $2;exit}'; echo
        printf 'BATTERY_DESIGN_CAPACITY='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Design Capacity/{print $2;exit}'; echo
        printf 'BATTERY_HEALTH='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/State of Health|Health Information/{print $2;exit}'; echo
        printf 'BATTERY_SERIAL='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Serial Number:/{print $2;exit}'; echo
        printf 'BATTERY_MANUFACTURER='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Manufacturer:/{print $2;exit}'; echo
        printf 'BATTERY_FIRMWARE='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Firmware Version:/{print $2;exit}'; echo
        printf 'BATTERY_FULLY_CHARGED='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Fully Charged:/{print $2;exit}'; echo
        printf 'BATTERY_CHARGE_BELOW_WARNING='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Charge Below Warning Level:/{print $2;exit}'; echo
      else
        printf 'BATTERY_PERCENT=nicht_vorhanden\n'
        printf 'BATTERY_CHARGING=nicht_vorhanden\n'
        printf 'BATTERY_TIME_REMAINING=nicht_vorhanden\n'
        printf 'BATTERY_CYCLE_COUNT=nicht_vorhanden\n'
        printf 'BATTERY_CONDITION=nicht_vorhanden\n'
        printf 'BATTERY_MAX_CAPACITY=nicht_vorhanden\n'
        printf 'BATTERY_FULL_CHARGE_CAPACITY=nicht_vorhanden\n'
        printf 'BATTERY_DESIGN_CAPACITY=nicht_vorhanden\n'
        printf 'BATTERY_HEALTH=nicht_vorhanden\n'
        printf 'BATTERY_SERIAL=nicht_vorhanden\n'
        printf 'BATTERY_MANUFACTURER=nicht_vorhanden\n'
        printf 'BATTERY_FIRMWARE=nicht_vorhanden\n'
        printf 'BATTERY_FULLY_CHARGED=nicht_vorhanden\n'
        printf 'BATTERY_CHARGE_BELOW_WARNING=nicht_vorhanden\n'
      fi
      printf 'AC_CONNECTED='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Connected:/{print $2;exit}'
      printf 'AC_WATTAGE='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Wattage/{print $2;exit}'
      printf 'AC_SERIAL='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Serial Number:/{n++; if(n>1){print $2;exit}}'
      printf 'LOW_POWER_MODE='; pmset -g custom 2>/dev/null | awk '/lowpowermode/{print $2;exit}' | sed 's/^0$/aus/;s/^1$/ein/'
      printf 'HIBERNATE_MODE='; pmset -g custom 2>/dev/null | awk '/hibernatemode/{print $2;exit}'
      printf 'SLEEP_MINUTES='; pmset -g custom 2>/dev/null | awk '/^[[:space:]]*sleep[[:space:]]/{print $2;exit}'
      printf 'DISPLAY_SLEEP_MINUTES='; pmset -g custom 2>/dev/null | awk '/displaysleep/{print $2;exit}'
      printf 'DISK_SLEEP_MINUTES='; pmset -g custom 2>/dev/null | awk '/disksleep/{print $2;exit}'
      printf 'POWER_NAP='; pmset -g custom 2>/dev/null | awk '/powernap/{print $2;exit}' | sed 's/^0$/aus/;s/^1$/ein/'
      printf 'WAKE_ON_NETWORK='; pmset -g custom 2>/dev/null | awk '/womp/{print $2;exit}' | sed 's/^0$/aus/;s/^1$/ein/'
      printf 'AUTO_POWER_OFF='; pmset -g custom 2>/dev/null | awk '/autopoweroff[[:space:]]/{print $2;exit}' | sed 's/^0$/aus/;s/^1$/ein/'
      printf 'CURRENT_POWER_ASSERTIONS='; pmset -g assertions 2>/dev/null | awk '/PreventUserIdleSystemSleep|PreventSystemSleep|PreventUserIdleDisplaySleep/{gsub(/^[[:space:]]+/,""); if(n++)printf "; "; printf "%s",$0}END{print ""}'
      printf 'LAST_WAKE_REASON='; pmset -g log 2>/dev/null | awk '/Wake from|DarkWake from/{line=$0}END{sub(/^.*Wake/,"Wake",line); print line}' | tail -c 500
      printf 'SCHEDULED_POWER_EVENTS='; pmset -g sched 2>/dev/null | tr '\n' ';' | sed 's/;*$//'; echo
      printf 'THERMAL_PRESSURE='; pmset -g therm 2>/dev/null | tr '\n' ';' | sed 's/;*$//'; echo
      printf 'UPTIME='; uptime 2>/dev/null
      """;
    String out;
    if(backendLocal){
      out=(sudoPassword!=null&&!sudoPassword.isBlank())?runLocalElevated(script,sudoPassword,120):runLocal(script,120);
    }else{
      out=(sudoPassword!=null&&!sudoPassword.isBlank())?runElevated(ip,cfg,script,sudoPassword,120):run(ip,cfg,script,90);
    }
    if(out==null||out.isBlank()) return Optional.empty();
    Map<String,String> m=parse(out); if(!"Darwin".equalsIgnoreCase(m.get("OS"))) return Optional.empty();
    String now=OffsetDateTime.now().toString(); String host=first(m.get("HOSTNAME"),nameHint,"Mac");
    String reportConnection="OK";
    String reportOs=present(m,"PRODUCT_VERSION","BUILD")?"OK":"TEILWEISE";
    String reportHardware=present(m,"MODEL_NAME","ARCH") && anyPresent(m,"CHIP","CPU","PROCESSOR_NAME") && anyPresent(m,"MEMORY_TEXT","MEMORY_BYTES")?"OK":"TEILWEISE";
    String reportStorage=positive(m.get("VOLUME_COUNT")) && anyPresent(m,"DISK","VOLUMES")?"OK":"TEILWEISE";
    String reportSecurity=anyPresent(m,"FILEVAULT","ENCRYPTED_VOLUME_COUNT")?"OK":"TEILWEISE";
    String reportUsersSoftware=anyPresent(m,"USER_COUNT","USER_DETAILS") && anyPresent(m,"APP_COUNT","APP_DETAILS")?"OK":"TEILWEISE";
    String reportNetwork=positive(m.get("NETWORK_ADAPTER_COUNT")) && anyPresent(m,"DEFAULT_INTERFACE","NETWORK_ADAPTER_DETAILS")?"OK":"TEILWEISE";
    boolean batteryExpected="ja".equalsIgnoreCase(m.get("BATTERY_PRESENT"));
    String reportEnergy=batteryExpected
      ? (anyPresent(m,"BATTERY_PERCENT","BATTERY_CYCLE_COUNT","BATTERY_CONDITION")?"OK":"TEILWEISE")
      : (anyPresent(m,"POWER_SOURCE","SLEEP_MINUTES","THERMAL_PRESSURE")?"OK (Desktop-Mac)":"TEILWEISE (Desktop-Mac)");
    List<String> reportWarnings=new ArrayList<>();
    if(!"OK".equals(reportOs)) reportWarnings.add("macOS-Version oder Build unvollständig");
    if(!"OK".equals(reportHardware)) reportWarnings.add("Hardwaredaten unvollständig");
    if(!"OK".equals(reportStorage)) reportWarnings.add("Datenträger-/Volume-Daten unvollständig");
    if(!"OK".equals(reportSecurity)) reportWarnings.add("Verschlüsselungsstatus nicht vollständig ermittelbar");
    if(!"OK".equals(reportUsersSoftware)) reportWarnings.add("Benutzer- oder Programminventar unvollständig");
    if(!"OK".equals(reportNetwork)) reportWarnings.add("Netzwerkdaten unvollständig");
    if(reportEnergy.startsWith("TEILWEISE")) reportWarnings.add("Energiedaten unvollständig");
    boolean rootActive="ja".equalsIgnoreCase(m.get("ROOT_ACTIVE")) || "0".equals(m.get("EFFECTIVE_UID"));
    boolean adminMember="ja".equalsIgnoreCase(m.get("ADMIN_MEMBER"));
    boolean sudoAvailable="ja".equalsIgnoreCase(m.get("SUDO_AVAILABLE"));
    boolean sudoNoPassword="ja".equalsIgnoreCase(m.get("SUDO_NON_INTERACTIVE"));
    String inventoryLevel=rootActive?"Vollzugriff":sudoNoPassword?"Administrator":adminMember&&sudoAvailable?"Erweitert":adminMember?"Standard (Administrator ohne sudo-Nachweis)":"Basis";
    String permissionMode=rootActive?"Root":sudoNoPassword?"Administrator":adminMember?"Erweitert":"Standard";
    List<String> limitations=new ArrayList<>();
    if(!adminMember&&!rootActive) limitations.add("Benutzer ist kein Mitglied der macOS-Administratorgruppe");
    if(sudoAvailable&&!sudoNoPassword&&!rootActive) limitations.add("sudo erfordert ein Kennwort; tiefer Lauf kann manuell gestartet werden");
    if(!sudoAvailable&&!rootActive) limitations.add("sudo ist nicht verfügbar");
    String fda=first(m.get("FULL_DISK_ACCESS_HINT"),"");
    if(fda.toLowerCase(Locale.ROOT).contains("eingeschränkt")) limitations.add("Full Disk Access ist eingeschränkt oder konnte nicht bestätigt werden");
    String reportPermissions=limitations.isEmpty()?"OK":"TEILWEISE";
    if(!limitations.isEmpty()) reportWarnings.add("Berechtigungen begrenzen möglicherweise einzelne Inventarbereiche");
    String reportOverall=reportWarnings.isEmpty()?"BESTANDEN":"BESTANDEN MIT HINWEISEN";
    String model=first(m.get("MODEL_NAME"),m.get("MODEL_IDENTIFIER"),m.get("MODEL"));
    String modelProbe=(model+" "+first(m.get("MODEL_IDENTIFIER"),m.get("MODEL"),"")).toLowerCase(Locale.ROOT);
    String type=modelProbe.contains("book")?"Notebook / macOS":modelProbe.contains("mini")?"Mini-PC / macOS":modelProbe.contains("studio")?"Workstation / macOS":"Computer / macOS";
    StringBuilder p=new StringBuilder(backendLocal?"Backend-lokal · macOS":"SSH · macOS");
    detail(p,"Inventarquelle",backendLocal?"macOS Backend-lokal":"macOS SSH-Inventar");
    if(backendLocal){
      detail(p,"Ausführung","direkt auf dem GAM-Backend-Rechner, kein SSH erforderlich");
    }else{
      detail(p,"SSH-Ziel",ip+":"+cfg.port());
      detail(p,"SSH-Benutzer",cfg.username());
      detail(p,"SSH-Authentifizierung",cfg.authMode());
    }
    detail(p,"SSH-Daten empfangen","ja");
    detail(p,"SSH-Datenfelder",String.valueOf(m.size()));
    detail(p,"macOS bestätigt","ja"); detail(p,"Produkt",m.get("PRODUCT_NAME")); detail(p,"macOS-Version",m.get("PRODUCT_VERSION")); detail(p,"Build",m.get("BUILD"));
    detail(p,"Berechtigungs-Benutzer",m.get("CURRENT_USER")); detail(p,"Benutzer-UID",m.get("CURRENT_UID")); detail(p,"Effektive UID",m.get("EFFECTIVE_UID")); detail(p,"Benutzer-GID",m.get("CURRENT_GID"));
    detail(p,"Administratorgruppe",m.get("ADMIN_MEMBER")); detail(p,"sudo verfügbar",m.get("SUDO_AVAILABLE")); detail(p,"sudo ohne Kennwort",m.get("SUDO_NON_INTERACTIVE")); detail(p,"Root aktiv",m.get("ROOT_ACTIVE"));
    detail(p,"SIP-Status",m.get("SIP_STATUS")); detail(p,"Full Disk Access",m.get("FULL_DISK_ACCESS_HINT")); detail(p,"Werkzeugzugriff",m.get("TOOL_ACCESS"));
    detail(p,"Berechtigungsmodus",permissionMode); detail(p,"Inventarisierungstiefe",inventoryLevel); detail(p,"Berechtigungseinschränkungen",limitations.isEmpty()?"keine":String.join("; ",limitations)); detail(p,"Erhöhter Lauf",sudoPassword!=null&&!sudoPassword.isBlank()?"ja":"nein");
    detail(p,"Modellname",m.get("MODEL_NAME")); detail(p,"Modellkennung",first(m.get("MODEL_IDENTIFIER"),m.get("MODEL"))); detail(p,"Maschinentyp",m.get("MACHINE"));
    detail(p,"Architektur",m.get("ARCH")); detail(p,"Chip",m.get("CHIP")); detail(p,"Prozessor",first(m.get("PROCESSOR_NAME"),m.get("CPU"),m.get("CHIP")));
    detail(p,"Prozessorgeschwindigkeit",m.get("PROCESSOR_SPEED")); detail(p,"Prozessoren",m.get("PROCESSOR_COUNT")); detail(p,"CPU-Kerne",first(m.get("TOTAL_CORES"),m.get("CPU_PHYSICAL")));
    detail(p,"Physische CPU-Kerne",m.get("CPU_PHYSICAL")); detail(p,"Maximale physische Kerne",m.get("CPU_PHYSICAL_MAX")); detail(p,"CPU-Threads",m.get("CPU_LOGICAL")); detail(p,"Maximale Threads",m.get("CPU_LOGICAL_MAX"));
    detail(p,"CPU-Frequenz",formatHertz(m.get("CPU_FREQUENCY"))); detail(p,"L1-Instruktionscache",formatBytes(m.get("CACHE_L1I"))); detail(p,"L1-Datencache",formatBytes(m.get("CACHE_L1D"))); detail(p,"L2-Cache",formatBytes(m.get("CACHE_L2"))); detail(p,"L3-Cache",formatBytes(m.get("CACHE_L3")));
    detail(p,"Arbeitsspeicher",first(m.get("MEMORY_TEXT"),formatBytes(m.get("MEMORY_BYTES")))); detail(p,"Arbeitsspeicher Bytes",m.get("MEMORY_BYTES"));
    detail(p,"Seriennummer",m.get("SERIAL")); detail(p,"Hardware-UUID",m.get("HARDWARE_UUID")); detail(p,"Provisioning-UDID",m.get("PROVISIONING_UDID")); detail(p,"Aktivierungssperre",m.get("ACTIVATION_LOCK"));
    detail(p,"Boot-ROM",m.get("BOOT_ROM")); detail(p,"System-Firmware",m.get("SYSTEM_FIRMWARE")); detail(p,"OS-Loader",m.get("OS_LOADER")); detail(p,"SMC-Version",m.get("SMC_VERSION")); detail(p,"Systemlaufwerk",m.get("DISK"));
    detail(p,"FileVault-Status",m.get("FILEVAULT")); detail(p,"Physische Datenträger",m.get("PHYSICAL_DISKS")); detail(p,"Physische Datenträger Anzahl",m.get("PHYSICAL_DISK_COUNT"));
    detail(p,"APFS-Container Anzahl",m.get("APFS_CONTAINER_COUNT")); detail(p,"APFS-Container",m.get("APFS_CONTAINERS")); detail(p,"APFS-Volumes Anzahl",m.get("APFS_VOLUME_COUNT"));
    detail(p,"Volumes Anzahl",m.get("VOLUME_COUNT")); detail(p,"Verschlüsselte Volumes",m.get("ENCRYPTED_VOLUME_COUNT")); detail(p,"Gesperrte Volumes",m.get("LOCKED_VOLUME_COUNT")); detail(p,"Volume-Details",m.get("VOLUMES"));
    detail(p,"smartmontools Paket",m.get("SMARTMONTOOLS_PACKAGE")); detail(p,"smartmontools Version",m.get("SMARTMONTOOLS_VERSION")); detail(p,"SMART-Werkzeug smartctl",m.get("SMARTCTL_AVAILABLE")); detail(p,"smartctl ausführbar",m.get("SMARTCTL_EXECUTABLE")); detail(p,"smartctl Pfad",m.get("SMARTCTL_PATH")); detail(p,"SMART-Diagnose", smartDiagnosis(m)); detail(p,"SMART-Datenträger Anzahl",m.get("SMART_DISK_COUNT")); detail(p,"SMART Gesundheit OK",m.get("SMART_HEALTH_OK")); detail(p,"SMART Warnungen",m.get("SMART_HEALTH_WARN")); detail(p,"SMART/NVMe-Details",m.get("SMART_DETAILS"));
    detail(p,"Speichermodule",m.get("MEMORY_MODULE_DETAILS")); detail(p,"ECC-Speicher",m.get("MEMORY_ECC")); detail(p,"RAM-Aufrüstbarkeit Hinweis",m.get("MEMORY_UPGRADEABLE_HINT"));
    detail(p,"GPU-Details",m.get("GPU_DETAILS")); detail(p,"Display-Details",m.get("DISPLAY_DETAILS"));
    detail(p,"USB-Geräte Anzahl",m.get("USB_DEVICE_COUNT")); detail(p,"USB-Hardware",m.get("USB_DETAILS")); detail(p,"Thunderbolt-Hardware",m.get("THUNDERBOLT_DETAILS")); detail(p,"PCI-Hardware",m.get("PCI_DETAILS"));
    detail(p,"Netzwerk-Hardware",m.get("NETWORK_HARDWARE_DETAILS")); detail(p,"Lüfter Anzahl",m.get("FAN_COUNT")); detail(p,"Thermik-Level",m.get("THERMAL_LEVEL")); detail(p,"CPU-Thermik-Level",m.get("CPU_THERMAL_LEVEL")); detail(p,"GPU-Thermik-Level",m.get("GPU_THERMAL_LEVEL")); detail(p,"Scheduler-Thermik-Level",m.get("SCHEDULER_THERMAL_LEVEL"));
    detail(p,"Lokale Benutzer Anzahl",m.get("USER_COUNT")); detail(p,"Administratoren",m.get("ADMIN_USERS")); detail(p,"Benutzer-Details",m.get("USER_DETAILS"));
    detail(p,"Programme Anzahl",m.get("APP_COUNT")); detail(p,"Systemprogramme Anzahl",m.get("APP_SYSTEM_COUNT")); detail(p,"Benutzerprogramme Anzahl",m.get("APP_USER_COUNT")); detail(p,"Software-Benutzer Anzahl",m.get("SOFTWARE_USER_COUNT")); detail(p,"Benutzersoftware Prüfstatus",m.get("USER_SOFTWARE_SCAN_STATUS")); detail(p,"Benutzersoftware Prüfdetails",m.get("USER_SOFTWARE_SCAN_DETAILS")); detail(p,"Systemsoftware-Details",m.get("SYSTEM_SOFTWARE_DETAILS")); detail(p,"Benutzersoftware-Details",m.get("USER_SOFTWARE_DETAILS")); detail(p,"Programm-Details",m.get("APP_DETAILS"));
    detail(p,"Geladene launchd-Dienste",m.get("LAUNCHD_LOADED_COUNT")); detail(p,"System-LaunchAgents",m.get("LAUNCH_AGENTS_SYSTEM_COUNT")); detail(p,"Benutzer-LaunchAgents",m.get("LAUNCH_AGENTS_USER_COUNT")); detail(p,"LaunchDaemons",m.get("LAUNCH_DAEMONS_COUNT")); detail(p,"launchd-Details",m.get("LAUNCHD_DETAILS")); detail(p,"Login-Elemente",m.get("LOGIN_ITEMS"));
    detail(p,"Homebrew",m.get("HOMEBREW")); detail(p,"MacPorts",m.get("MACPORTS")); detail(p,"Xcode Command Line Tools",m.get("XCODE_CLT")); detail(p,"Verfügbare Updates",m.get("AVAILABLE_UPDATES"));
    detail(p,"Mac App Store Werkzeug mas",m.get("MAS_AVAILABLE")); detail(p,"mas Version",m.get("MAS_VERSION")); detail(p,"Mac App Store Apps laut mas",m.get("MAS_APP_COUNT")); detail(p,"Mac App Store mas Details",m.get("MAS_APP_DETAILS"));
    detail(p,"Softwarequelle normale Apps",m.get("SOFTWARE_SOURCE_APPLICATION")); detail(p,"Softwarequelle Mac App Store",m.get("SOFTWARE_SOURCE_MAC_APP_STORE")); detail(p,"Softwarequelle Installer-Pakete",m.get("SOFTWARE_SOURCE_PKG")); detail(p,"Softwarequelle Homebrew Formulae",m.get("SOFTWARE_SOURCE_HOMEBREW")); detail(p,"Softwarequelle Homebrew Casks",m.get("SOFTWARE_SOURCE_HOMEBREW_CASK")); detail(p,"Softwareupdate-Details",m.get("AVAILABLE_UPDATE_DETAILS"));
    detail(p,"Netzwerkadapter Anzahl",m.get("NETWORK_ADAPTER_COUNT")); detail(p,"Aktive Netzwerkadapter",m.get("ACTIVE_NETWORK_ADAPTER_COUNT")); detail(p,"Netzwerkadapter-Details",m.get("NETWORK_ADAPTER_DETAILS"));
    detail(p,"Standardgateway",m.get("DEFAULT_GATEWAY")); detail(p,"Standardinterface",m.get("DEFAULT_INTERFACE")); detail(p,"DNS-Server",m.get("DNS_SERVERS")); detail(p,"DNS-Suchdomänen",m.get("SEARCH_DOMAINS"));
    detail(p,"Computername",m.get("COMPUTER_NAME")); detail(p,"Lokaler Hostname",m.get("LOCAL_HOST_NAME")); detail(p,"Konfigurierter Hostname",m.get("HOST_NAME")); detail(p,"Proxy-Konfiguration",m.get("PROXY_CONFIGURATION"));
    detail(p,"WLAN-Interface",m.get("WIFI_INTERFACE")); detail(p,"WLAN-Status",m.get("WIFI_POWER")); detail(p,"WLAN-Netzwerk",m.get("WIFI_SSID")); detail(p,"WLAN-MAC",m.get("WIFI_MAC")); detail(p,"WLAN-Standards",m.get("WIFI_PROTOCOLS")); detail(p,"WLAN-Kanal",m.get("WIFI_CHANNEL")); detail(p,"WLAN-Sicherheit",m.get("WIFI_SECURITY"));
    detail(p,"Bluetooth vorhanden",m.get("BLUETOOTH_PRESENT")); detail(p,"Bluetooth-Status",m.get("BLUETOOTH_STATE")); detail(p,"Bluetooth-Chipsatz",m.get("BLUETOOTH_CHIPSET")); detail(p,"Bluetooth-Adresse",m.get("BLUETOOTH_ADDRESS")); detail(p,"Bluetooth-Version",m.get("BLUETOOTH_VERSION")); detail(p,"Verbundene Bluetooth-Geräte",m.get("BLUETOOTH_CONNECTED_DEVICES"));
    detail(p,"Bonjour/mDNS",m.get("BONJOUR_MDNS")); detail(p,"Bonjour-Hostname",m.get("BONJOUR_HOST")); detail(p,"Lauschende TCP-Ports",m.get("LISTENING_TCP_PORTS")); detail(p,"VPN-Dienste",m.get("VPN_SERVICES")); detail(p,"ARP-Nachbarn",m.get("ARP_NEIGHBOR_COUNT"));
    detail(p,"Testreport Gesamtstatus",reportOverall); detail(p,"Testreport Verbindung",reportConnection); detail(p,"Testreport Berechtigungen",reportPermissions); detail(p,"Testreport Betriebssystem",reportOs); detail(p,"Testreport Hardware",reportHardware); detail(p,"Testreport Speicher",reportStorage); detail(p,"Testreport Verschlüsselung",reportSecurity); detail(p,"Testreport Benutzer und Software",reportUsersSoftware); detail(p,"Testreport Netzwerk",reportNetwork); detail(p,"Testreport Energie",reportEnergy); detail(p,"Testreport Hinweise",reportWarnings.isEmpty()?"keine":String.join("; ",reportWarnings));
    detail(p,"Akku vorhanden",m.get("BATTERY_PRESENT")); detail(p,"Stromquelle",m.get("POWER_SOURCE")); detail(p,"Akkuladestand",batteryDisplay(m,"BATTERY_PERCENT"," %")); detail(p,"Akku lädt",batteryDisplay(m,"BATTERY_CHARGING","")); detail(p,"Restlaufzeit",batteryDisplay(m,"BATTERY_TIME_REMAINING",""));
    detail(p,"Akku-Ladezyklen",batteryDisplay(m,"BATTERY_CYCLE_COUNT","")); detail(p,"Akkuzustand",m.get("BATTERY_CONDITION")); detail(p,"Maximale Akkukapazität",m.get("BATTERY_MAX_CAPACITY")); detail(p,"Vollladekapazität",m.get("BATTERY_FULL_CHARGE_CAPACITY")); detail(p,"Designkapazität",m.get("BATTERY_DESIGN_CAPACITY")); detail(p,"Akkugesundheit",m.get("BATTERY_HEALTH"));
    detail(p,"Akku-Seriennummer",m.get("BATTERY_SERIAL")); detail(p,"Akku-Hersteller",m.get("BATTERY_MANUFACTURER")); detail(p,"Akku-Firmware",m.get("BATTERY_FIRMWARE")); detail(p,"Vollständig geladen",m.get("BATTERY_FULLY_CHARGED")); detail(p,"Unter Warnschwelle",m.get("BATTERY_CHARGE_BELOW_WARNING"));
    detail(p,"Netzteil verbunden",m.get("AC_CONNECTED")); detail(p,"Netzteil-Leistung",m.get("AC_WATTAGE")); detail(p,"Netzteil-Seriennummer",m.get("AC_SERIAL")); detail(p,"Stromsparmodus",m.get("LOW_POWER_MODE")); detail(p,"Ruhezustandsmodus",m.get("HIBERNATE_MODE"));
    detail(p,"System-Ruhezustand",suffix(m.get("SLEEP_MINUTES")," min")); detail(p,"Display-Ruhezustand",suffix(m.get("DISPLAY_SLEEP_MINUTES")," min")); detail(p,"Festplatten-Ruhezustand",suffix(m.get("DISK_SLEEP_MINUTES")," min")); detail(p,"Power Nap",m.get("POWER_NAP")); detail(p,"Wake on Network",m.get("WAKE_ON_NETWORK")); detail(p,"Automatisches Ausschalten",m.get("AUTO_POWER_OFF"));
    detail(p,"Aktive Energieanforderungen",m.get("CURRENT_POWER_ASSERTIONS")); detail(p,"Letzter Aufwachgrund",m.get("LAST_WAKE_REASON")); detail(p,"Geplante Energieereignisse",m.get("SCHEDULED_POWER_EVENTS")); detail(p,"Thermischer Status",m.get("THERMAL_PRESSURE")); detail(p,"Laufzeit",m.get("UPTIME"));
    detail(p,"Inventarisierungsstatus","erfolgreich"); detail(p,"Letzter Inventarisierungsversuch",now);
    DiscoveredDevice device=new DiscoveredDevice((backendLocal?"macos-local:":"macos-ssh:")+ip,host,type,ip,null,p.toString(),"ONLINE","Apple",m.get("SERIAL"),now,false);
    return Optional.of(new InventoryResult(device,softwareItems(m),first(m.get("USER_SOFTWARE_SCAN_STATUS"),"unbekannt"),first(m.get("USER_SOFTWARE_SCAN_DETAILS"),"")));
  }

  private static List<SoftwareItem> softwareItems(Map<String,String> m){
    List<SoftwareItem> rows=new ArrayList<>();
    for(String item:splitSoftwareRows(m.get("SYSTEM_SOFTWARE_DETAILS"))){
      String[] c=item.split("\\|",-1); if(c.length<1||c[0].isBlank())continue;
      rows.add(new SoftwareItem("SYSTEM","",c[0],at(c,1),at(c,2),at(c,3),at(c,4)));
    }
    for(String item:splitSoftwareRows(m.get("USER_SOFTWARE_DETAILS"))){
      String[] c=item.split("\\|",-1); if(c.length<2||c[0].isBlank()||c[1].isBlank())continue;
      rows.add(new SoftwareItem("USER",c[0],c[1],at(c,2),at(c,3),at(c,4),at(c,5)));
    }
    return rows;
  }
  private static List<String> splitSoftwareRows(String raw){if(raw==null||raw.isBlank())return List.of();return Arrays.stream(raw.split("\\s+;\\s+")).map(String::trim).filter(x->!x.isBlank()).toList();}
  private static String at(String[] c,int i){if(i>=c.length)return null;String v=c[i]==null?null:c[i].trim();return v==null||v.isEmpty()||"-".equals(v)?null:v;}

  private static boolean isLoopbackAddress(String value){
    try{return value!=null&&!value.isBlank()&&java.net.InetAddress.getByName(value.trim()).isLoopbackAddress();}
    catch(Exception e){return false;}
  }

  private String runLocal(String command,int timeout){
    try{
      Process p=new ProcessBuilder("/bin/sh","-c",command).redirectErrorStream(true).start();
      boolean done=p.waitFor(timeout,TimeUnit.SECONDS);
      if(!done){p.destroyForcibly();return null;}
      return new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
    }catch(Exception e){return null;}
  }

  private String runLocalElevated(String command,String sudoPassword,int timeout){
    try{
      Process p=new ProcessBuilder("/usr/bin/sudo","-S","-p","","/bin/sh","-c",command).redirectErrorStream(true).start();
      try(OutputStream os=p.getOutputStream()){
        os.write(((sudoPassword==null?"":sudoPassword)+"\n").getBytes(StandardCharsets.UTF_8));
        os.flush();
      }
      boolean done=p.waitFor(timeout,TimeUnit.SECONDS);
      if(!done){p.destroyForcibly();return null;}
      if(p.exitValue()!=0)return null;
      return new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
    }catch(Exception e){return null;}
  }

  private String runElevated(String ip, LinuxSshSettingsRepository.Settings c, String command, String sudoPassword, int timeout){
    String input=(sudoPassword==null?"":sudoPassword)+"\n";
    var result=embeddedSsh.execute(ip,c.port(),c.username(),c.authMode(),c.password(),c.keyPath(),
      "sudo -S -p '' sh -c '"+command.replace("'","'\\''")+"'",input,timeout);
    return result.success()?result.stdout():null;
  }

  private String run(String ip, LinuxSshSettingsRepository.Settings c, String command, int timeout){
    var result=embeddedSsh.execute(ip,c.port(),c.username(),c.authMode(),c.password(),c.keyPath(),
      "sh -c '"+command.replace("'","'\\''")+"'",null,timeout);
    return result.success()?result.stdout():null;
  }
  private static Map<String,String> parse(String s){Map<String,String> m=new LinkedHashMap<>(); for(String l:s.split("\\R")){int i=l.indexOf('=');if(i>0)m.put(l.substring(0,i).trim(),l.substring(i+1).trim());}return m;}
  private static void detail(StringBuilder b,String k,String v){if(v!=null&&!v.isBlank())b.append(" · ").append(k).append(": ").append(v.replace(" · "," / "));}
  private static String first(String...v){for(String x:v)if(x!=null&&!x.isBlank())return x;return "Mac";}
  private static String smartDiagnosis(Map<String,String> m){
    String available=m.getOrDefault("SMARTCTL_AVAILABLE","");
    String executable=m.getOrDefault("SMARTCTL_EXECUTABLE","");
    String path=m.getOrDefault("SMARTCTL_PATH","");
    if("ja".equalsIgnoreCase(available)||"ja".equalsIgnoreCase(executable)||(path!=null&&!path.isBlank())){
      return "smartmontools/smartctl verfügbar; erweiterte SMART/NVMe-Daten werden verwendet";
    }
    return "smartmontools/smartctl nicht installiert; native macOS-SMART-Daten werden verwendet";
  }

  private static String batteryDisplay(Map<String,String> m,String key,String suffix){
    if(!"ja".equalsIgnoreCase(m.getOrDefault("BATTERY_PRESENT",""))){
      return "nicht vorhanden";
    }
    String value=m.get(key);
    if(value==null||value.isBlank()||value.startsWith("BATTERY_")||value.contains("BATTERY_")){
      return "nicht verfügbar";
    }
    return suffix(value,suffix);
  }

  private static String suffix(String value,String suffix){return value==null||value.isBlank()?value:value.endsWith(suffix.trim())?value:value+suffix;}
  private static String formatBytes(String s){try{long b=Long.parseLong(s);if(b>=1073741824L)return String.format(Locale.GERMANY,"%.1f GB",b/1073741824.0);if(b>=1048576L)return String.format(Locale.GERMANY,"%.1f MB",b/1048576.0);if(b>=1024L)return String.format(Locale.GERMANY,"%.1f KB",b/1024.0);return b+" B";}catch(Exception e){return s;}}
  private static boolean anyPresent(Map<String,String> m,String...keys){for(String k:keys){String v=m.get(k);if(v!=null&&!v.isBlank()&&!"-".equals(v))return true;}return false;}
  private static boolean present(Map<String,String> m,String...keys){for(String k:keys){String v=m.get(k);if(v==null||v.isBlank()||"-".equals(v))return false;}return true;}
  private static boolean positive(String value){try{return Integer.parseInt(value)>0;}catch(Exception e){return false;}}
  private static String formatHertz(String s){try{long hz=Long.parseLong(s);if(hz>=1000000000L)return String.format(Locale.GERMANY,"%.2f GHz",hz/1000000000.0);if(hz>=1000000L)return String.format(Locale.GERMANY,"%.1f MHz",hz/1000000.0);return hz+" Hz";}catch(Exception e){return s;}}
}
