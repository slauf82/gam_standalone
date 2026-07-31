package de.kopfzentrum.gam.inventory;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** 40k36f: schreibgeschützte macOS-Inventarisierung einschließlich Hardware, Storage, Software, Netzwerk und Energie über SSH. */
@Service
public class MacOsInventoryService {
  private final LinuxSshSettingsRepository settings;
  public MacOsInventoryService(LinuxSshSettingsRepository settings){this.settings=settings;}

  public record TestResult(boolean sshConfigured, boolean reachable, boolean macOs, String message, String checkedAt){}

  public TestResult test(String ip){
    var cfg=settings.load(); String now=OffsetDateTime.now().toString();
    if(!cfg.enabled()||cfg.username().isBlank()) return new TestResult(false,false,false,"SSH-Zugang ist noch nicht konfiguriert.",now);
    String out=run(ip,cfg,"printf 'GAM_OS='; uname -s; sw_vers -productVersion 2>/dev/null | sed 's/^/GAM_VERSION=/'",10);
    if(out==null||out.isBlank()) return new TestResult(true,false,false,"Keine SSH-Verbindung. Auf dem Mac bitte Systemeinstellungen → Allgemein → Freigabe → Entfernte Anmeldung aktivieren sowie Port, Benutzername und Zugangsdaten prüfen.",now);
    boolean mac=out.contains("GAM_OS=Darwin")||out.contains("GAM_VERSION=");
    return new TestResult(true,true,mac,mac?"SSH-Verbindung erfolgreich; das Ziel wurde als macOS erkannt.":"SSH-Verbindung erfolgreich, das Ziel meldet jedoch kein macOS/Darwin.",now);
  }

  public Optional<DiscoveredDevice> inventory(String ip, String nameHint){
    var cfg=settings.load();
    String script="""
      printf 'OS='; uname -s
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
      printf 'MODEL_NAME='; printf '%s\n' "$HWPROF" | awk -F': ' '/Model Name:/{print $2; exit}'
      printf 'MODEL_IDENTIFIER='; printf '%s\n' "$HWPROF" | awk -F': ' '/Model Identifier:/{print $2; exit}'
      printf 'CHIP='; printf '%s\n' "$HWPROF" | awk -F': ' '/Chip:/{print $2; exit}'
      printf 'PROCESSOR_NAME='; printf '%s\n' "$HWPROF" | awk -F': ' '/Processor Name:/{print $2; exit}'
      printf 'PROCESSOR_SPEED='; printf '%s\n' "$HWPROF" | awk -F': ' '/Processor Speed:/{print $2; exit}'
      printf 'PROCESSOR_COUNT='; printf '%s\n' "$HWPROF" | awk -F': ' '/Number of Processors:/{print $2; exit}'
      printf 'TOTAL_CORES='; printf '%s\n' "$HWPROF" | awk -F': ' '/Total Number of Cores:/{print $2; exit}'
      printf 'MEMORY_TEXT='; printf '%s\n' "$HWPROF" | awk -F': ' '/Memory:/{print $2; exit}'
      printf 'SERIAL='; printf '%s\n' "$HWPROF" | awk -F': ' '/Serial Number/{print $2; exit}'
      printf 'HARDWARE_UUID='; printf '%s\n' "$HWPROF" | awk -F': ' '/Hardware UUID/{print $2; exit}'
      printf 'PROVISIONING_UDID='; printf '%s\n' "$HWPROF" | awk -F': ' '/Provisioning UDID/{print $2; exit}'
      printf 'ACTIVATION_LOCK='; printf '%s\n' "$HWPROF" | awk -F': ' '/Activation Lock Status/{print $2; exit}'
      printf 'BOOT_ROM='; printf '%s\n' "$HWPROF" | awk -F': ' '/Boot ROM Version/{print $2; exit}'
      printf 'SYSTEM_FIRMWARE='; printf '%s\n' "$HWPROF" | awk -F': ' '/System Firmware Version/{print $2; exit}'
      printf 'OS_LOADER='; printf '%s\n' "$HWPROF" | awk -F': ' '/OS Loader Version/{print $2; exit}'
      printf 'SMC_VERSION='; printf '%s\n' "$HWPROF" | awk -F': ' '/SMC Version/{print $2; exit}'
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

      APP_ROWS=''
      APP_COUNT=0
      APP_SYSTEM_COUNT=0
      APP_USER_COUNT=0
      for ROOT in /Applications /System/Applications "$HOME/Applications"; do
        [ -d "$ROOT" ] || continue
        find "$ROOT" -maxdepth 2 -type d -name '*.app' -print 2>/dev/null | while IFS= read -r APP; do
          PLIST="$APP/Contents/Info.plist"
          NAME=$(defaults read "$PLIST" CFBundleDisplayName 2>/dev/null || defaults read "$PLIST" CFBundleName 2>/dev/null || basename "$APP" .app)
          VER=$(defaults read "$PLIST" CFBundleShortVersionString 2>/dev/null || defaults read "$PLIST" CFBundleVersion 2>/dev/null)
          BID=$(defaults read "$PLIST" CFBundleIdentifier 2>/dev/null)
          printf '%s|%s|%s|%s\n' "$NAME" "${VER:--}" "${BID:--}" "$APP"
        done
      done | sort -u > /tmp/gam_macos_apps_$$.txt
      while IFS='|' read -r NAME VER BID PATHV; do
        [ -n "$NAME" ] || continue
        ROW="$NAME|$VER|$BID|$PATHV"
        if [ -n "$APP_ROWS" ]; then APP_ROWS="$APP_ROWS ; $ROW"; else APP_ROWS="$ROW"; fi
        APP_COUNT=$((APP_COUNT+1))
        case "$PATHV" in /System/Applications/*) APP_SYSTEM_COUNT=$((APP_SYSTEM_COUNT+1));; "$HOME"/Applications/*) APP_USER_COUNT=$((APP_USER_COUNT+1));; esac
      done < /tmp/gam_macos_apps_$$.txt
      rm -f /tmp/gam_macos_apps_$$.txt
      printf 'APP_COUNT=%s\n' "$APP_COUNT"
      printf 'APP_SYSTEM_COUNT=%s\n' "$APP_SYSTEM_COUNT"
      printf 'APP_USER_COUNT=%s\n' "$APP_USER_COUNT"
      printf 'APP_DETAILS=%s\n' "$APP_ROWS"

      printf 'LAUNCHD_LOADED_COUNT='; launchctl list 2>/dev/null | tail -n +2 | wc -l | tr -d ' '
      printf 'LAUNCH_AGENTS_SYSTEM_COUNT='; find /Library/LaunchAgents /System/Library/LaunchAgents -maxdepth 1 -name '*.plist' 2>/dev/null | wc -l | tr -d ' '
      printf 'LAUNCH_AGENTS_USER_COUNT='; find "$HOME/Library/LaunchAgents" -maxdepth 1 -name '*.plist' 2>/dev/null | wc -l | tr -d ' '
      printf 'LAUNCH_DAEMONS_COUNT='; find /Library/LaunchDaemons /System/Library/LaunchDaemons -maxdepth 1 -name '*.plist' 2>/dev/null | wc -l | tr -d ' '
      printf 'LAUNCHD_DETAILS='; launchctl list 2>/dev/null | tail -n +2 | awk 'BEGIN{first=1}{if(!first)printf " ; "; first=0; printf "%s|%s|%s",$3,$1,$2}' ; echo
      printf 'LOGIN_ITEMS='; osascript -e 'tell application "System Events" to get the name of every login item' 2>/dev/null | sed 's/, /,/g'; echo
      printf 'HOMEBREW='; if command -v brew >/dev/null 2>&1; then brew --version 2>/dev/null | head -1; else echo 'nicht installiert'; fi
      printf 'MACPORTS='; if command -v port >/dev/null 2>&1; then port version 2>/dev/null | head -1; else echo 'nicht installiert'; fi
      printf 'XCODE_CLT='; xcode-select -p 2>/dev/null || echo 'nicht installiert'
      printf 'AVAILABLE_UPDATES='; softwareupdate -l 2>/dev/null | grep -c '^\\* Label:' | tr -d ' '

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
      printf 'HOST_NAME='; scutil --get HostName 2>/dev/null || true
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
      printf 'BATTERY_PRESENT='; if printf '%s\n' "$POWER_INFO $BATT_LINE" | grep -qiE 'Battery Information|InternalBattery|%'; then echo ja; else echo nein; fi
      printf 'POWER_SOURCE='; pmset -g batt 2>/dev/null | head -n 1 | sed -E "s/.*Now drawing from '(.*)'.*/\1/"
      printf 'BATTERY_PERCENT='; printf '%s\n' "$BATT_LINE" | sed -nE 's/.*[[:space:]]([0-9]+)%;.*/\1/p'
      printf 'BATTERY_CHARGING='; case "$BATT_LINE" in *charging*|*Charging*) echo ja;; *discharging*|*Discharging*) echo nein;; *) echo unbekannt;; esac
      printf 'BATTERY_TIME_REMAINING='; printf '%s\n' "$BATT_LINE" | sed -nE 's/.*;[[:space:]]*([^;]+)[[:space:]]*remaining.*/\1/p'
      printf 'BATTERY_CYCLE_COUNT='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Cycle Count:/{print $2;exit}'
      printf 'BATTERY_CONDITION='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Condition:/{print $2;exit}'
      printf 'BATTERY_MAX_CAPACITY='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Maximum Capacity:/{print $2;exit}'
      printf 'BATTERY_FULL_CHARGE_CAPACITY='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Full Charge Capacity/{print $2;exit}'
      printf 'BATTERY_DESIGN_CAPACITY='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Design Capacity/{print $2;exit}'
      printf 'BATTERY_HEALTH='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/State of Health|Health Information/{print $2;exit}'
      printf 'BATTERY_SERIAL='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Serial Number:/{print $2;exit}'
      printf 'BATTERY_MANUFACTURER='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Manufacturer:/{print $2;exit}'
      printf 'BATTERY_FIRMWARE='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Firmware Version:/{print $2;exit}'
      printf 'BATTERY_FULLY_CHARGED='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Fully Charged:/{print $2;exit}'
      printf 'BATTERY_CHARGE_BELOW_WARNING='; printf '%s\n' "$POWER_INFO" | awk -F': ' '/Charge Below Warning Level:/{print $2;exit}'
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
    String out=run(ip,cfg,script,30); if(out==null||out.isBlank()) return Optional.empty();
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
    String reportOverall=reportWarnings.isEmpty()?"BESTANDEN":"BESTANDEN MIT HINWEISEN";
    String model=first(m.get("MODEL_NAME"),m.get("MODEL_IDENTIFIER"),m.get("MODEL"));
    String modelProbe=(model+" "+first(m.get("MODEL_IDENTIFIER"),m.get("MODEL"),"")).toLowerCase(Locale.ROOT);
    String type=modelProbe.contains("book")?"Notebook / macOS":modelProbe.contains("mini")?"Mini-PC / macOS":modelProbe.contains("studio")?"Workstation / macOS":"Computer / macOS";
    StringBuilder p=new StringBuilder("macOS SSH-Inventar");
    detail(p,"macOS bestätigt","ja"); detail(p,"Produkt",m.get("PRODUCT_NAME")); detail(p,"macOS-Version",m.get("PRODUCT_VERSION")); detail(p,"Build",m.get("BUILD"));
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
    detail(p,"Lokale Benutzer Anzahl",m.get("USER_COUNT")); detail(p,"Administratoren",m.get("ADMIN_USERS")); detail(p,"Benutzer-Details",m.get("USER_DETAILS"));
    detail(p,"Programme Anzahl",m.get("APP_COUNT")); detail(p,"Systemprogramme Anzahl",m.get("APP_SYSTEM_COUNT")); detail(p,"Benutzerprogramme Anzahl",m.get("APP_USER_COUNT")); detail(p,"Programm-Details",m.get("APP_DETAILS"));
    detail(p,"Geladene launchd-Dienste",m.get("LAUNCHD_LOADED_COUNT")); detail(p,"System-LaunchAgents",m.get("LAUNCH_AGENTS_SYSTEM_COUNT")); detail(p,"Benutzer-LaunchAgents",m.get("LAUNCH_AGENTS_USER_COUNT")); detail(p,"LaunchDaemons",m.get("LAUNCH_DAEMONS_COUNT")); detail(p,"launchd-Details",m.get("LAUNCHD_DETAILS")); detail(p,"Login-Elemente",m.get("LOGIN_ITEMS"));
    detail(p,"Homebrew",m.get("HOMEBREW")); detail(p,"MacPorts",m.get("MACPORTS")); detail(p,"Xcode Command Line Tools",m.get("XCODE_CLT")); detail(p,"Verfügbare Updates",m.get("AVAILABLE_UPDATES"));
    detail(p,"Netzwerkadapter Anzahl",m.get("NETWORK_ADAPTER_COUNT")); detail(p,"Aktive Netzwerkadapter",m.get("ACTIVE_NETWORK_ADAPTER_COUNT")); detail(p,"Netzwerkadapter-Details",m.get("NETWORK_ADAPTER_DETAILS"));
    detail(p,"Standardgateway",m.get("DEFAULT_GATEWAY")); detail(p,"Standardinterface",m.get("DEFAULT_INTERFACE")); detail(p,"DNS-Server",m.get("DNS_SERVERS")); detail(p,"DNS-Suchdomänen",m.get("SEARCH_DOMAINS"));
    detail(p,"Computername",m.get("COMPUTER_NAME")); detail(p,"Lokaler Hostname",m.get("LOCAL_HOST_NAME")); detail(p,"Konfigurierter Hostname",m.get("HOST_NAME")); detail(p,"Proxy-Konfiguration",m.get("PROXY_CONFIGURATION"));
    detail(p,"WLAN-Interface",m.get("WIFI_INTERFACE")); detail(p,"WLAN-Status",m.get("WIFI_POWER")); detail(p,"WLAN-Netzwerk",m.get("WIFI_SSID")); detail(p,"WLAN-MAC",m.get("WIFI_MAC")); detail(p,"WLAN-Standards",m.get("WIFI_PROTOCOLS")); detail(p,"WLAN-Kanal",m.get("WIFI_CHANNEL")); detail(p,"WLAN-Sicherheit",m.get("WIFI_SECURITY"));
    detail(p,"Bluetooth vorhanden",m.get("BLUETOOTH_PRESENT")); detail(p,"Bluetooth-Status",m.get("BLUETOOTH_STATE")); detail(p,"Bluetooth-Chipsatz",m.get("BLUETOOTH_CHIPSET")); detail(p,"Bluetooth-Adresse",m.get("BLUETOOTH_ADDRESS")); detail(p,"Bluetooth-Version",m.get("BLUETOOTH_VERSION")); detail(p,"Verbundene Bluetooth-Geräte",m.get("BLUETOOTH_CONNECTED_DEVICES"));
    detail(p,"Bonjour/mDNS",m.get("BONJOUR_MDNS")); detail(p,"Bonjour-Hostname",m.get("BONJOUR_HOST")); detail(p,"Lauschende TCP-Ports",m.get("LISTENING_TCP_PORTS")); detail(p,"VPN-Dienste",m.get("VPN_SERVICES")); detail(p,"ARP-Nachbarn",m.get("ARP_NEIGHBOR_COUNT"));
    detail(p,"Testreport Gesamtstatus",reportOverall); detail(p,"Testreport Verbindung",reportConnection); detail(p,"Testreport Betriebssystem",reportOs); detail(p,"Testreport Hardware",reportHardware); detail(p,"Testreport Speicher",reportStorage); detail(p,"Testreport Verschlüsselung",reportSecurity); detail(p,"Testreport Benutzer und Software",reportUsersSoftware); detail(p,"Testreport Netzwerk",reportNetwork); detail(p,"Testreport Energie",reportEnergy); detail(p,"Testreport Hinweise",reportWarnings.isEmpty()?"keine":String.join("; ",reportWarnings));
    detail(p,"Akku vorhanden",m.get("BATTERY_PRESENT")); detail(p,"Stromquelle",m.get("POWER_SOURCE")); detail(p,"Akkuladestand",suffix(m.get("BATTERY_PERCENT")," %")); detail(p,"Akku lädt",m.get("BATTERY_CHARGING")); detail(p,"Restlaufzeit",m.get("BATTERY_TIME_REMAINING"));
    detail(p,"Akku-Ladezyklen",m.get("BATTERY_CYCLE_COUNT")); detail(p,"Akkuzustand",m.get("BATTERY_CONDITION")); detail(p,"Maximale Akkukapazität",m.get("BATTERY_MAX_CAPACITY")); detail(p,"Vollladekapazität",m.get("BATTERY_FULL_CHARGE_CAPACITY")); detail(p,"Designkapazität",m.get("BATTERY_DESIGN_CAPACITY")); detail(p,"Akkugesundheit",m.get("BATTERY_HEALTH"));
    detail(p,"Akku-Seriennummer",m.get("BATTERY_SERIAL")); detail(p,"Akku-Hersteller",m.get("BATTERY_MANUFACTURER")); detail(p,"Akku-Firmware",m.get("BATTERY_FIRMWARE")); detail(p,"Vollständig geladen",m.get("BATTERY_FULLY_CHARGED")); detail(p,"Unter Warnschwelle",m.get("BATTERY_CHARGE_BELOW_WARNING"));
    detail(p,"Netzteil verbunden",m.get("AC_CONNECTED")); detail(p,"Netzteil-Leistung",m.get("AC_WATTAGE")); detail(p,"Netzteil-Seriennummer",m.get("AC_SERIAL")); detail(p,"Stromsparmodus",m.get("LOW_POWER_MODE")); detail(p,"Ruhezustandsmodus",m.get("HIBERNATE_MODE"));
    detail(p,"System-Ruhezustand",suffix(m.get("SLEEP_MINUTES")," min")); detail(p,"Display-Ruhezustand",suffix(m.get("DISPLAY_SLEEP_MINUTES")," min")); detail(p,"Festplatten-Ruhezustand",suffix(m.get("DISK_SLEEP_MINUTES")," min")); detail(p,"Power Nap",m.get("POWER_NAP")); detail(p,"Wake on Network",m.get("WAKE_ON_NETWORK")); detail(p,"Automatisches Ausschalten",m.get("AUTO_POWER_OFF"));
    detail(p,"Aktive Energieanforderungen",m.get("CURRENT_POWER_ASSERTIONS")); detail(p,"Letzter Aufwachgrund",m.get("LAST_WAKE_REASON")); detail(p,"Geplante Energieereignisse",m.get("SCHEDULED_POWER_EVENTS")); detail(p,"Thermischer Status",m.get("THERMAL_PRESSURE")); detail(p,"Laufzeit",m.get("UPTIME"));
    detail(p,"Inventarisierungsstatus","erfolgreich"); detail(p,"Letzter Inventarisierungsversuch",now);
    return Optional.of(new DiscoveredDevice("macos-ssh:"+ip,host,type,ip,null,p.toString(),"ONLINE","Apple",m.get("SERIAL"),now,false));
  }

  private static String run(String ip, LinuxSshSettingsRepository.Settings c, String command, int timeout){
    List<String> cmd=new ArrayList<>();
    if("PASSWORD".equals(c.authMode())){cmd.addAll(List.of("sshpass","-p",c.password()==null?"":c.password()));}
    cmd.addAll(List.of("ssh","-o","BatchMode="+("PASSWORD".equals(c.authMode())?"no":"yes"),"-o","ConnectTimeout=5","-o","ConnectionAttempts=1","-o","StrictHostKeyChecking=accept-new","-p",String.valueOf(c.port())));
    if(c.keyPath()!=null&&!c.keyPath().isBlank()) cmd.addAll(List.of("-i",c.keyPath()));
    cmd.add(c.username()+"@"+ip); cmd.add("sh -c '"+command.replace("'","'\\''")+"'");
    try{Process p=new ProcessBuilder(cmd).redirectErrorStream(true).start(); StringBuilder sb=new StringBuilder(); Thread reader=new Thread(()->{try(var r=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){for(String line;(line=r.readLine())!=null;) synchronized(sb){sb.append(line).append('\n');}}catch(Exception ignored){}}); reader.start(); if(!p.waitFor(timeout,TimeUnit.SECONDS)){p.destroyForcibly();reader.join(1000);return null;} reader.join(1000); return sb.toString();}catch(Exception e){return null;}
  }
  private static Map<String,String> parse(String s){Map<String,String> m=new LinkedHashMap<>(); for(String l:s.split("\\R")){int i=l.indexOf('=');if(i>0)m.put(l.substring(0,i).trim(),l.substring(i+1).trim());}return m;}
  private static void detail(StringBuilder b,String k,String v){if(v!=null&&!v.isBlank())b.append(" · ").append(k).append(": ").append(v.replace(" · "," / "));}
  private static String first(String...v){for(String x:v)if(x!=null&&!x.isBlank())return x;return "Mac";}
  private static String suffix(String value,String suffix){return value==null||value.isBlank()?value:value.endsWith(suffix.trim())?value:value+suffix;}
  private static String formatBytes(String s){try{long b=Long.parseLong(s);if(b>=1073741824L)return String.format(Locale.GERMANY,"%.1f GB",b/1073741824.0);if(b>=1048576L)return String.format(Locale.GERMANY,"%.1f MB",b/1048576.0);if(b>=1024L)return String.format(Locale.GERMANY,"%.1f KB",b/1024.0);return b+" B";}catch(Exception e){return s;}}
  private static boolean anyPresent(Map<String,String> m,String...keys){for(String k:keys){String v=m.get(k);if(v!=null&&!v.isBlank()&&!"-".equals(v))return true;}return false;}
  private static boolean present(Map<String,String> m,String...keys){for(String k:keys){String v=m.get(k);if(v==null||v.isBlank()||"-".equals(v))return false;}return true;}
  private static boolean positive(String value){try{return Integer.parseInt(value)>0;}catch(Exception e){return false;}}
  private static String formatHertz(String s){try{long hz=Long.parseLong(s);if(hz>=1000000000L)return String.format(Locale.GERMANY,"%.2f GHz",hz/1000000000.0);if(hz>=1000000L)return String.format(Locale.GERMANY,"%.1f MHz",hz/1000000.0);return hz+" Hz";}catch(Exception e){return s;}}
}
