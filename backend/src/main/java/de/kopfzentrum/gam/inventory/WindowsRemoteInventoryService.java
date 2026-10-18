package de.kopfzentrum.gam.inventory;

import java.nio.file.Files;
import java.nio.file.Path;
import io.cloudsoft.winrm4j.client.WinRmClientContext;
import io.cloudsoft.winrm4j.winrm.WinRmTool;
import io.cloudsoft.winrm4j.winrm.WinRmToolResponse;
import org.metricshub.winrm.AuthScheme;
import org.metricshub.winrm.CommandResult;
import org.metricshub.winrm.WinRMClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 40k34q: Remote-Windows-Inventarisierung über WinRM (Windows Remote Management) -
 * das von Microsoft bereitgestellte Standard-Fernverwaltungsprotokoll für Windows,
 * analog zu SSH bei Linux und ADB bei Android. Nutzt die etablierte, quelloffene
 * Bibliothek <a href="https://github.com/cloudsoft/winrm4j">winrm4j</a>
 * ({@code io.cloudsoft.windows:winrm4j}) - kein selbst entwickeltes WinRM-/SOAP-
 * Protokoll, keine proprietäre Eigenlösung.
 *
 * Architektur bewusst analog zu {@link LinuxNetworkDiscoveryService} gehalten:
 * EIN gemeinsam konfigurierter Zugang über dieselbe {@code setting()}-Konvention
 * (Property oder Umgebungsvariable, mit Fallback) wie bei SSH - keine zweite,
 * abweichende Zugangsdatenverwaltung. EIN einziges PowerShell-Sammelskript pro
 * Lauf statt vieler Einzelbefehle (Performance-Vorgabe: Remote-Aufrufe
 * minimieren). Rückgabeform {@code Optional<DiscoveredDevice>} - identisch zu
 * {@code LinuxNetworkDiscoveryService.inspectSingle()} - damit der zentrale
 * Plattform-Dispatcher ({@code DeviceIdentityService}) beide Plattformen mit
 * derselben, bereits bestehenden Übernahmelogik (recordDiscoveryHit()) behandeln
 * kann - keine Windows-Sonderbehandlung im Dispatcher.
 *
 * Architektur bewusst erweiterbar: {@link #inspectSingle} kapselt ausschließlich
 * die WinRM-spezifischen Details. Ein späteres zweites Remote-Verfahren (z.B.
 * eine reine CIM-über-HTTPS-Variante oder ein anderer Port) könnte als weitere
 * Methode dieser Klasse ergänzt werden, ohne den Dispatcher zu ändern.
 */
@Service
public class WindowsRemoteInventoryService {
  // 40k36t53: strukturierte Windows-Software bleibt ausserhalb des Discovery-Protokolls.
  private final ConcurrentHashMap<String,String> latestSoftwareDetails = new ConcurrentHashMap<>();
  // 40k36t114: zuletzt erfolgreicher Vollinventarisierungsweg je Ziel/Profil/Benutzer.
  // Nur Laufzeit-Cache: bei Fehlschlag wird der Eintrag verworfen und normal neu ermittelt.
  private final ConcurrentHashMap<String,String> preferredInventoryRoute = new ConcurrentHashMap<>();

  public String consumeLatestSoftwareDetails(String host){
    if(host==null) return null;
    return latestSoftwareDetails.remove(host);
  }


  private static final Logger log = LoggerFactory.getLogger(WindowsRemoteInventoryService.class);
  private final WinRmSettingsRepository settings;
  private final OsAccessProfileRepository accessProfiles;
  private final WinRmTrustedTargetRepository trustedTargets;

  public WindowsRemoteInventoryService(WinRmSettingsRepository settings, OsAccessProfileRepository accessProfiles, WinRmTrustedTargetRepository trustedTargets) {
    this.settings = settings; this.accessProfiles=accessProfiles; this.trustedTargets=trustedTargets;
  }

  private record ResolvedAccess(WinRmSettingsRepository.Settings settings,String connectionHost,String source,String profileName,Long profileId) {}

  private static String routeKey(String host,Long profileId,String username){
    return (host==null?"":host.trim().toLowerCase(Locale.ROOT))+"|"+(profileId==null?"-":profileId)+"|"+(username==null?"":username.trim().toLowerCase(Locale.ROOT));
  }

  private ResolvedAccess resolveAccess(String address,String hostnameHint,String identityKey){
    var p=accessProfiles.resolveTarget("WINRM",address,hostnameHint,identityKey);
    if(p!=null){
      String target=p.host()==null||p.host().isBlank()?address:p.host().trim();
      return new ResolvedAccess(new WinRmSettingsRepository.Settings(p.enabled(),p.username(),p.password(),p.port(),p.https()),
        target,"OS_ACCESS_PROFILE",p.name(),p.id());
    }
    var legacy=settings.load();
    return new ResolvedAccess(legacy,address,"LEGACY_GLOBAL_FALLBACK","",null);
  }

  private WinRmSettingsRepository.Settings settingsFor(String host){
    var p=accessProfiles.resolve("WINRM",host);
    if(p!=null)return new WinRmSettingsRepository.Settings(p.enabled(),p.username(),p.password(),p.port(),p.https());
    return settings.load();
  }

  /** 40k36t6: Startinventarisierung darf schwache Windows-Hinweise (z.B. Windows ARP)
   * nur dann für einen Remote-Lauf verwenden, wenn tatsächlich ein WinRM-Zugang existiert. */
  public boolean hasConfiguredAccess(String host){
    return configured(host);
  }

  public boolean hasConfiguredAccess(String address,String hostnameHint,String identityKey){
    if(address==null||address.isBlank()) return false;
    var resolved=resolveAccess(address,hostnameHint,identityKey);
    var cfg=resolved.settings();
    return cfg.enabled() && cfg.username()!=null && !cfg.username().isBlank();
  }

  /**
   * 40k34q: EIN PowerShell-Sammelskript für den gesamten, im Auftrag geforderten
   * Umfang (Computername, Betriebssystem/Edition/Version/Build/Architektur,
   * Hersteller/Modell, BIOS, CPU, RAM, Laufwerke, Netzwerkkarten, installierte
   * Software, angemeldete Benutzer) - EIN Remote-Aufruf statt vieler Einzelabfragen.
   * Nutzt CIM (Get-CimInstance), den von Microsoft empfohlenen Nachfolger von WMI -
   * funktioniert über WinRM ohne zusätzliche DCOM-Firewallregeln.
   */
  private static final String INVENTORY_SCRIPT = WindowsInventoryDiscoveryService.remoteFullInventoryScript();

  /** 40k36t53: Eine einzige Quelle der Wahrheit für WinRM-Zugänge.
   * Hostbezogene OS-Access-Profile haben Vorrang; nur wenn für den Host kein
   * solches Profil existiert, darf die alte globale WinRM-Konfiguration als
   * Rückwärtskompatibilitäts-Fallback verwendet werden. */
  private boolean configured(String host) {
    if(host==null || host.isBlank()) return false;
    var cfg=settingsFor(host);
    return cfg.enabled() && cfg.username()!=null && !cfg.username().isBlank();
  }

  private static final String AUTH_PRIMARY = "Negotiate";
  private static final String AUTH_LEGACY_FALLBACK = "NTLM";

  /**
   * 40k36t28: WinRM nutzt nicht mehr fest verdrahtet NTLM. Primär wird
   * Negotiate/SPNEGO verwendet (Kerberos bevorzugt); NTLM bleibt ausschließlich
   * als klar gekennzeichneter Legacy-Fallback erhalten. Das ist bewusst auch in
   * Diagnose und Protokoll sichtbar, damit ein Fallback nie unbemerkt bleibt.
   */
  private WinRmTool.Builder builderFor(String host, WinRmClientContext context) {
    var s=settingsFor(host);
    return builderFor(host,context,s.username(),s.password(),s.port(),s.https(),AUTH_PRIMARY);
  }

  private WinRmTool.Builder builderFor(String host, WinRmClientContext context, String username, String password, int port, boolean https) {
    return builderFor(host,context,username,password,port,https,AUTH_PRIMARY);
  }

  private WinRmTool.Builder builderFor(String host, WinRmClientContext context, String username, String password, int port, boolean https, String authScheme) {
    WinRmTool.Builder builder = WinRmTool.Builder.builder(host, username, password);
    builder.authenticationScheme(authScheme);
    builder.port(port);
    builder.useHttps(https);
    if (https) builder.disableCertificateChecks(true);
    builder.context(context);
    return builder;
  }

  public record WinRmTestResult(
    boolean configured,
    boolean reachable,
    String message,
    String checkedAt,
    boolean tcpReachable,
    boolean wsmanAttempted,
    String endpoint,
    String usernameFormat,
    String requestedAuthentication,
    String authenticationUsed,
    boolean legacyFallbackUsed,
    String errorType,
    String errorDetail,
    String clientStack,
    boolean trustedTarget,
    boolean trustRequired
  ) {
    public WinRmTestResult(boolean configured, boolean reachable, String message, String checkedAt) {
      this(configured, reachable, message, checkedAt, false, false, "—", "—",
        "Negotiate/SPNEGO (Kerberos bevorzugt)", "—", false, "", "", "Legacy winrm4j/CXF 3.x", false, false);
    }
  }

  private record WinRmAttempt(boolean success, WinRmToolResponse response, String authScheme, String errorType, String errorDetail) {}
  private record LegacyScriptResult(boolean success, String stdout, String errorType, String errorDetail) {}

  private boolean tcpReachable(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 1800);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isIpLiteral(String host){
    if(host==null) return false;
    String h=host.trim();
    return h.matches("(?:\\d{1,3}\\.){3}\\d{1,3}") || h.contains(":");
  }

  private boolean trustRequired(String host, boolean https){ return !https && isIpLiteral(host); }

  private String usernameFormat(String username) {
    if (username == null || username.isBlank()) return "nicht angegeben";
    String u = username.trim();
    if (u.regionMatches(true, 0, "MicrosoftAccount\\", 0, "MicrosoftAccount\\".length())) return "Microsoft-Konto (MicrosoftAccount\\…)";
    if (u.contains("\\")) return "Windows-Konto (DOMAIN\\Benutzer)";
    if (u.contains("@")) return "UPN / E-Mail-Adresse";
    return "lokaler/kurzer Benutzername";
  }

  private String credentialIdentity(String username) {
    if (username == null || username.isBlank()) return "Benutzer=<leer>";
    String u=username.trim();
    int slash=u.indexOf('\\');
    if(slash>0 && slash<u.length()-1) return "Domain/Computer="+u.substring(0,slash)+", Benutzer="+u.substring(slash+1);
    if(u.contains("@")) return "UPN="+u;
    return "Benutzer="+u;
  }

  private String errorSummary(Throwable t) {
    if (t == null) return "unbekannter Fehler";
    StringBuilder b = new StringBuilder();
    Throwable cur=t;
    int depth=0;
    while(cur!=null && depth<4){
      if(depth>0)b.append(" <- ");
      b.append(cur.getClass().getSimpleName());
      String m=cur.getMessage();
      if(m!=null&&!m.isBlank()) b.append(": ").append(m.replaceAll("[\r\n]+"," ").trim());
      cur=cur.getCause(); depth++;
    }
    return b.toString();
  }

  private record ModernWinRmAttempt(boolean success, String stdout, String stderr, int exitCode, String authentication, String errorType, String errorDetail) {}

  /** 40k36t30: moderner Standardpfad ohne CXF/JAXB/SAAJ.
   *  MetricsHub 2.1.x nutzt JDK-HTTP/XML. Kerberos ist dort absichtlich nur über HTTPS möglich;
   *  auf HTTP wird NTLM technisch zum Kompatibilitätsweg und in der Diagnose entsprechend markiert. */
  private ModernWinRmAttempt attemptModernSingle(String host, String username, String password, int port, boolean https, String command, boolean powershell, long timeout) {
    char[] secret = password == null ? new char[0] : password.toCharArray();
    try {
      var builder = WinRMClient.builder(host)
        .credentials(username, secret)
        .port(port)
        .timeout(Duration.ofMillis(timeout));
      String auth;
      if (https) {
        builder.https().authentication(AuthScheme.KERBEROS, AuthScheme.NTLM);
        auth = "Kerberos/SPNEGO → NTLM-Fallback";
      } else {
        builder.authentication(AuthScheme.NTLM);
        auth = "NTLM (HTTP-Kompatibilitätsweg)";
      }
      try (WinRMClient client = builder.build()) {
        CommandResult result = powershell ? client.powerShell(command).execute() : client.command(command).execute();
        return new ModernWinRmAttempt(result.exitCode()==0, result.stdout(), result.stderr(), result.exitCode(), auth,
          result.exitCode()==0 ? "" : "WinRmStatus", result.exitCode()==0 ? "" : "Status "+result.exitCode()+": "+trim(result.stderr()));
      }
    } catch (Exception e) {
      return new ModernWinRmAttempt(false, "", "", -1, https ? "Kerberos/SPNEGO → NTLM-Fallback" : "NTLM (HTTP-Kompatibilitätsweg)",
        e.getClass().getSimpleName(), credentialIdentity(username)+" · "+errorSummary(e));
    } finally {
      java.util.Arrays.fill(secret, '\0');
    }
  }

  private boolean isWindowsBackend() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    return os.contains("win");
  }

  private boolean isLocalWhitespaceAccount(String username) {
    if (username == null) return false;
    String u = username.trim();
    if (u.regionMatches(true, 0, "MicrosoftAccount\\", 0, "MicrosoftAccount\\".length()) || u.contains("@")) return false;
    int slash = u.indexOf('\\');
    return slash > 0 && slash < u.length() - 1 && u.substring(slash + 1).chars().anyMatch(Character::isWhitespace);
  }

  private String psBase64(String value) {
    return java.util.Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /**
   * 40k36t53: Windows-nativer WinRM/SSPI-Fallback fuer lokale Konten mit Leerzeichen.
   *
   * MetricsHub 2.1.00 bleibt der Standard. Der Fallback wird ausschliesslich aktiviert,
   * wenn (a) der Standardpfad fehlgeschlagen ist, (b) ein lokales HOST\\Benutzer-Konto
   * Whitespace im SAM-Namen enthaelt und (c) das GAM-Backend selbst unter Windows laeuft.
   *
   * Das Kennwort wird NICHT in der Prozesskommandozeile uebergeben. PowerShell liest das
   * erzeugte Skript ueber STDIN; Benutzer, Kennwort und Remote-Skript sind darin Base64-
   * kodiert. Dadurch bleibt der Benutzername bytegenau erhalten und Windows selbst fuehrt
   * Negotiate/NTLM ueber SSPI aus - derselbe Pfad wie ein manuelles Invoke-Command.
   */

  /**
   * 40k36t103: Preflight und Vollinventar sind absichtlich EIN Session-Lebenszyklus.
   * Die Session wird genau einmal aufgebaut. Der Preflight ist der erste echte Befehl
   * innerhalb dieser Session. Nur wenn dieser erfolgreich ist, bleibt exakt dieselbe
   * Session fuer Upload-Chunks, Ausfuehrung, Download-Chunks und Cleanup offen.
   */
  private ModernWinRmAttempt attemptWindowsNativeCurrentContextCoupledPreflightAndInventory(
      String host,String username,String password,int port,boolean https,String command,long timeout){
    if(!isWindowsBackend()){
      return new ModernWinRmAttempt(false,"","",-1,
        "Windows-native WinRM/PowerShell SSPI · t112 Current Context",
        "NATIVE_FALLBACK_UNAVAILABLE","GAM-Backend laeuft nicht unter Windows.");
    }

    // 40k36t106:
    // NICHT den SSPI-Verbindungsaufbau neu erfinden. Diese Methode kopiert absichtlich
    // den nachweislich funktionierenden kleinen attemptWindowsNative()-Preflight-Transport:
    //   powershell.exe ... -Command -
    //   Script per UTF-8 stdin
    //   identische PSCredential-Erzeugung
    //   identisches $invoke-Hashtable
    //   identisches Invoke-Command @invoke -ScriptBlock { param($remoteText) ... }
    // Nur $remoteText ist jetzt der komplette atomare Preflight+Inventar-Remote-Block.
    final int chunkSize=3072;
    java.nio.file.Path stdoutFile=null,stderrFile=null;
    try{
      String inventoryB64=java.util.Base64.getEncoder().encodeToString(
        command.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      int uploadChunks=(inventoryB64.length()+chunkSize-1)/chunkSize;

      // RemoteText selbst bleibt kompakt in seiner Logik; die Nutzlast wird dort als
      // nummerierte Strings zusammengesetzt. Alles wird in EINEM Invoke-Command ausgefuehrt.
      StringBuilder remote=new StringBuilder();
      remote.append("$ErrorActionPreference='Stop';");
      // 40k36t115: SSPI versucht denselben tiefen Benutzerumfang wie der moderne Pfad.
      // Das Inventarscript behandelt nicht verfuegbare AllUsers-/Hive-Lesepfade weiterhin best-effort.
      remote.append("$env:GAM_FORCE_DEEP_USERS='1';");
      remote.append("'GAM_PREFLIGHT_OK|'+[Environment]::MachineName+'|'+[Environment]::UserName;");
      remote.append("$parts=@(");
      for(int i=0;i<uploadChunks;i++){
        int from=i*chunkSize,to=Math.min(inventoryB64.length(),from+chunkSize);
        if(i>0)remote.append(",");
        remote.append("'").append(inventoryB64,from,to).append("'");
      }
      remote.append(");");
      remote.append("'GAM_CHUNK_UPLOAD_INIT|'+$parts.Count;");
      remote.append("$b=New-Object Text.StringBuilder;");
      remote.append("for($i=0;$i -lt $parts.Count;$i++){[void]$b.Append($parts[$i]);'GAM_CHUNK_UPLOAD|'+($i+1)+'/'+$parts.Count+'|'+$parts[$i].Length};");
      remote.append("$src=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b.ToString()));");
      remote.append("$o=&([ScriptBlock]::Create($src))*>&1|ForEach-Object{if($_ -is [string]){$_}else{($_|Out-String -Width 100000).TrimEnd()}};");
      remote.append("$txt=($o -join [Environment]::NewLine);");
      remote.append("$r64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($txt));");
      remote.append("$n=[int][Math]::Ceiling($r64.Length/").append(chunkSize).append(".0);");
      remote.append("'GAM_CHUNK_EXEC_OK|'+$txt.Length+'|'+$r64.Length;");
      remote.append("'GAM_CHUNK_DOWNLOAD_INIT|'+$n;");
      remote.append("for($i=0;$i -lt $n;$i++){$off=$i*").append(chunkSize)
        .append(";$take=[Math]::Min(").append(chunkSize)
        .append(",$r64.Length-$off);$p=$r64.Substring($off,$take);'GAM_RESULT_CHUNK|'+$i+'|'+$n+'|'+$p};");
      remote.append("'GAM_CHUNK_DONE'");

      String host64=psBase64(host);
      String remote64=psBase64(remote.toString());

      // 40k36t112: exakt der in t111 real erfolgreiche SSPI-Modus:
      // aktueller Windows-Sicherheitskontext, bewusst OHNE PSCredential/-Credential.
      String script=String.join("\n",
        "$ErrorActionPreference='Stop'",
        "[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)",
        "$OutputEncoding=[Text.UTF8Encoding]::new($false)",
        "$utf8=[Text.Encoding]::UTF8",
        "$hostName=$utf8.GetString([Convert]::FromBase64String('"+host64+"'))",
        "$remoteText=$utf8.GetString([Convert]::FromBase64String('"+remote64+"'))",
        "$invoke=@{ComputerName=$hostName;Port="+port+";ErrorAction='Stop'}",
        https?"$invoke.UseSSL=$true":"$null=$null",
        "$result=Invoke-Command @invoke -ScriptBlock { param($remoteText) $sb=[ScriptBlock]::Create($remoteText); & $sb } -ArgumentList $remoteText",
        "$result | ForEach-Object { $_ }"
      );

      stdoutFile=java.nio.file.Files.createTempFile("gam-winrm-native-out-",".txt");
      stderrFile=java.nio.file.Files.createTempFile("gam-winrm-native-err-",".txt");
      ProcessBuilder pb=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive",
        "-ExecutionPolicy","Bypass","-Command","-");
      pb.redirectOutput(stdoutFile.toFile());
      pb.redirectError(stderrFile.toFile());
      Process process=pb.start();
      try(var writer=new java.io.OutputStreamWriter(process.getOutputStream(),java.nio.charset.StandardCharsets.UTF_8)){
        writer.write(script);
      }
      boolean finished=process.waitFor(Math.max(15000L,timeout),java.util.concurrent.TimeUnit.MILLISECONDS);
      if(!finished){
        process.destroyForcibly();
        return new ModernWinRmAttempt(false,"","",-1,
          "Windows-native WinRM/PowerShell SSPI · t112 Current Context · Single-Invoke",
          "NATIVE_T106_TIMEOUT","t106 Single-Invoke lief in ein Timeout.");
      }

      String stdout=readWindowsPowerShellText(stdoutFile).trim();
      String stderr=readWindowsPowerShellText(stderrFile).trim();
      int exit=process.exitValue();

      java.util.TreeMap<Integer,String> pieces=new java.util.TreeMap<>();
      int expected=-1; boolean preflight=false,done=false;
      StringBuilder diag=new StringBuilder();
      for(String raw:stdout.split("\\R")){
        String line=raw==null?"":raw.trim();
        if(line.isEmpty())continue;
        if(line.startsWith("GAM_PREFLIGHT_OK|"))preflight=true;
        if(line.startsWith("GAM_CHUNK_DOWNLOAD_INIT|")){
          try{expected=Integer.parseInt(line.substring(line.indexOf('|')+1));}catch(Exception ignored){}
        }else if(line.startsWith("GAM_RESULT_CHUNK|")){
          String[] a=line.split("\\|",4);
          if(a.length==4)try{pieces.put(Integer.parseInt(a[1]),a[3]);}catch(Exception ignored){}
        }else{
          if(diag.length()>0)diag.append(" | ");
          diag.append(line);
          if("GAM_CHUNK_DONE".equals(line))done=true;
        }
      }

      String payload="";
      if(exit==0&&preflight&&done&&expected>=0&&pieces.size()==expected){
        StringBuilder joined=new StringBuilder();
        boolean ok=true;
        for(int i=0;i<expected;i++){String x=pieces.get(i);if(x==null){ok=false;break;}joined.append(x);}
        if(ok)payload=new String(java.util.Base64.getDecoder().decode(joined.toString()),
          java.nio.charset.StandardCharsets.UTF_8).trim();
      }

      log.info("[WINRM-T115-SSPI] Phase=FULL_CURRENT_CONTEXT Host={} Transport=CURRENT_CONTEXT_NO_CREDENTIAL SingleInvoke=true Preflight={} UploadChunks={} DownloadChunks={}/{} PayloadChars={} Exit={} Diagnose={}",
        host,preflight,uploadChunks,pieces.size(),expected,payload.length(),exit,
        diag.length()==0?"<keine>":diag.toString());

      if(exit==0&&preflight&&done&&!payload.isBlank()){
        return new ModernWinRmAttempt(true,payload,stderr,0,
          "Windows-native WinRM/PowerShell SSPI · Current Context · EIN Invoke-Command","","");
      }
      String detail=stderr.isBlank()?diag.toString():stderr;
      return new ModernWinRmAttempt(false,payload,stderr,exit,
        "Windows-native WinRM/PowerShell SSPI · Current Context · EIN Invoke-Command",
        exit==0?"NATIVE_T106_EMPTY":"NATIVE_T106_FAILED",
        detail.isBlank()?"t106 lieferte keine vollstaendige Nutzlast.":detail);
    }catch(Exception e){
      return new ModernWinRmAttempt(false,"","",-1,
        "Windows-native WinRM/PowerShell SSPI · Current Context · EIN Invoke-Command",
        e.getClass().getSimpleName(),errorSummary(e));
    }finally{
      try{if(stdoutFile!=null)java.nio.file.Files.deleteIfExists(stdoutFile);}catch(Exception ignored){}
      try{if(stderrFile!=null)java.nio.file.Files.deleteIfExists(stderrFile);}catch(Exception ignored){}
    }
  }
  /** 40k36t110: Strikte Bewertung desselben kleinen Invoke-Command-Aufbaus wie im alten "gruenen" Pfad. */
  private ModernWinRmAttempt attemptWindowsNativeT110Strict(String host,String username,String password,int port,boolean https,String command,boolean powershell,long timeout){
    if(!isWindowsBackend()) return new ModernWinRmAttempt(false,"","",-1,"Windows-native SSPI t110","NATIVE_FALLBACK_UNAVAILABLE","GAM-Backend laeuft nicht unter Windows.");
    java.nio.file.Path stdoutFile=null,stderrFile=null;
    try{
      stdoutFile=java.nio.file.Files.createTempFile("gam-winrm-t110-out-",".txt");
      stderrFile=java.nio.file.Files.createTempFile("gam-winrm-t110-err-",".txt");
      String host64=psBase64(host),user64=psBase64(username),password64=psBase64(password),command64=psBase64(command);
      String remoteBody=powershell?"$sb=[ScriptBlock]::Create($remoteText); & $sb":"& cmd.exe /d /s /c $remoteText";
      String script=String.join("\n",
        "$ErrorActionPreference='Stop'","[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)","$OutputEncoding=[Text.UTF8Encoding]::new($false)","$utf8=[Text.Encoding]::UTF8",
        "$hostName=$utf8.GetString([Convert]::FromBase64String('"+host64+"'))","$userName=$utf8.GetString([Convert]::FromBase64String('"+user64+"'))",
        "$plainPassword=$utf8.GetString([Convert]::FromBase64String('"+password64+"'))","$remoteText=$utf8.GetString([Convert]::FromBase64String('"+command64+"'))",
        "$securePassword=ConvertTo-SecureString $plainPassword -AsPlainText -Force","$credential=[PSCredential]::new($userName,$securePassword)","$plainPassword=$null",
        "$invoke=@{ComputerName=$hostName;Port="+port+";Credential=$credential;ErrorAction='Stop'}",https?"$invoke.UseSSL=$true":"$null=$null",
        "$result=Invoke-Command @invoke -ScriptBlock { param($remoteText) "+remoteBody+" } -ArgumentList $remoteText","$result | ForEach-Object { $_ }");
      ProcessBuilder pb=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-Command","-");
      pb.redirectOutput(stdoutFile.toFile()); pb.redirectError(stderrFile.toFile()); Process process=pb.start();
      try(var writer=new java.io.OutputStreamWriter(process.getOutputStream(),java.nio.charset.StandardCharsets.UTF_8)){writer.write(script);}
      boolean finished=process.waitFor(Math.max(5000L,timeout),java.util.concurrent.TimeUnit.MILLISECONDS);
      if(!finished){process.destroyForcibly();return new ModernWinRmAttempt(false,"","",-1,"Windows-native SSPI t110 strict","NATIVE_T110_TIMEOUT","t110 Probe Timeout");}
      String stdout=readWindowsPowerShellText(stdoutFile).trim(),stderr=readWindowsPowerShellText(stderrFile).trim(); int exit=process.exitValue();
      String low=stderr.toLowerCase(java.util.Locale.ROOT);
      boolean remotingError=stderr.contains("PSSessionStateBroken")||stderr.contains("AccessDenied")||low.contains("zugriff verweigert")||low.contains("psremotingtransportexception");
      boolean ok=exit==0&&!remotingError;
      return new ModernWinRmAttempt(ok,stdout,stderr,exit,"Windows-native SSPI t110 strict",ok?"":"NATIVE_T110_STRICT_FAILED",ok?"":(stderr.isBlank()?"Keine verwertbare Remote-Antwort":stderr.replaceAll("[\\r\\n]+"," ")));
    }catch(Exception e){return new ModernWinRmAttempt(false,"","",-1,"Windows-native SSPI t110 strict",e.getClass().getSimpleName(),errorSummary(e));}
    finally{try{if(stdoutFile!=null)java.nio.file.Files.deleteIfExists(stdoutFile);}catch(Exception ignored){} try{if(stderrFile!=null)java.nio.file.Files.deleteIfExists(stderrFile);}catch(Exception ignored){}}
  }

  /** 40k36t110: Re-Klassifiziert die vermeintlich gruene Referenz und mutiert nur den Remote-Inhalt. */
  private void runT110ExactGreenMutationProbe(String host,String username,String password,int port,boolean https){
    record Probe(String name,String command,boolean powershell){}
    java.util.List<Probe> probes=java.util.List.of(
      new Probe("M00_EXACT_CMD_WHOAMI","whoami",false),
      new Probe("M01_CMD_ECHO","echo GAM_T110_OK",false),
      new Probe("M02_PS_WHOAMI","whoami",true),
      new Probe("M03_PS_LITERAL","'GAM_T110_OK'",true),
      new Probe("M04_PS_PARAM_LITERAL","param($x) $x",true)
    );
    for(Probe probe:probes){
      ModernWinRmAttempt r=attemptWindowsNativeT110Strict(host,username,password,port,https,probe.command(),probe.powershell(),15000L);
      String err=r.errorDetail()==null?"":r.errorDetail(); if(err.length()>320)err=err.substring(0,320)+"...";
      log.info("[WINRM-T110-MUTATION] Name={} Mode={} Success={} Exit={} StdoutChars={} StderrChars={} ErrorType={} Error={}",probe.name(),probe.powershell()?"POWERSHELL":"CMD",r.success(),r.exitCode(),r.stdout()==null?0:r.stdout().length(),r.stderr()==null?0:r.stderr().length(),r.errorType(),err);
    }
  }

  /** 40k36t111: Authentifizierungsmatrix; strikt, Exit=0 allein reicht nicht. */
  private ModernWinRmAttempt attemptWindowsNativeT111Auth(String host,String username,String password,int port,boolean https,boolean useCredential,long timeout){
    if(!isWindowsBackend()) return new ModernWinRmAttempt(false,"","",-1,"Windows-native SSPI t111","NATIVE_FALLBACK_UNAVAILABLE","GAM-Backend laeuft nicht unter Windows.");
    java.nio.file.Path out=null,err=null;
    try{
      out=java.nio.file.Files.createTempFile("gam-winrm-t111-out-",".txt"); err=java.nio.file.Files.createTempFile("gam-winrm-t111-err-",".txt");
      StringBuilder ps=new StringBuilder();
      ps.append("$ErrorActionPreference='Stop'\n$utf8=[Text.Encoding]::UTF8\n");
      ps.append("$hostName=$utf8.GetString([Convert]::FromBase64String('").append(psBase64(host)).append("'))\n");
      ps.append("$invoke=@{ComputerName=$hostName;Port=").append(port).append(";ErrorAction='Stop'}\n");
      if(https) ps.append("$invoke.UseSSL=$true\n");
      if(useCredential){
        ps.append("$userName=$utf8.GetString([Convert]::FromBase64String('").append(psBase64(username)).append("'))\n");
        ps.append("$plainPassword=$utf8.GetString([Convert]::FromBase64String('").append(psBase64(password)).append("'))\n");
        ps.append("$securePassword=ConvertTo-SecureString $plainPassword -AsPlainText -Force\n$credential=[PSCredential]::new($userName,$securePassword)\n$plainPassword=$null\n$invoke.Credential=$credential\n");
      }
      ps.append("Invoke-Command @invoke -ScriptBlock { [Security.Principal.WindowsIdentity]::GetCurrent().Name }\n");
      ProcessBuilder pb=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-Command","-"); pb.redirectOutput(out.toFile()); pb.redirectError(err.toFile());
      Process pr=pb.start(); try(var w=new java.io.OutputStreamWriter(pr.getOutputStream(),java.nio.charset.StandardCharsets.UTF_8)){w.write(ps.toString());}
      if(!pr.waitFor(Math.max(5000L,timeout),java.util.concurrent.TimeUnit.MILLISECONDS)){pr.destroyForcibly();return new ModernWinRmAttempt(false,"","",-1,"Windows-native SSPI t111","NATIVE_T111_TIMEOUT","Timeout");}
      String stdout=readWindowsPowerShellText(out).trim(),stderr=readWindowsPowerShellText(err).trim(); int exit=pr.exitValue(); String low=stderr.toLowerCase(java.util.Locale.ROOT);
      boolean remotingError=stderr.contains("PSSessionStateBroken")||stderr.contains("AccessDenied")||low.contains("zugriff verweigert")||low.contains("psremotingtransportexception");
      boolean ok=exit==0&&!remotingError&&!stdout.isBlank();
      return new ModernWinRmAttempt(ok,stdout,stderr,exit,"Windows-native SSPI t111",ok?"":"NATIVE_T111_AUTH_FAILED",ok?"":(stderr.isBlank()?"Keine verwertbare Remote-Antwort":stderr.replaceAll("[\\r\\n]+"," ")));
    }catch(Exception e){return new ModernWinRmAttempt(false,"","",-1,"Windows-native SSPI t111",e.getClass().getSimpleName(),errorSummary(e));}
    finally{try{if(out!=null)java.nio.file.Files.deleteIfExists(out);}catch(Exception ignored){} try{if(err!=null)java.nio.file.Files.deleteIfExists(err);}catch(Exception ignored){}}
  }

  private void runT111AuthenticationMatrix(String ip,String hostnameHint,String configuredUsername,String password,int port,boolean https){
    String configured=configuredUsername==null?"":configuredUsername.trim(), leaf=configured; int slash=configured.indexOf('\\'); if(slash>=0&&slash+1<configured.length()) leaf=configured.substring(slash+1);
    String hn=hostnameHint==null?"":hostnameHint.trim(); java.util.LinkedHashMap<String,String> targets=new java.util.LinkedHashMap<>(); if(ip!=null&&!ip.isBlank())targets.put("IP",ip.trim()); if(!hn.isBlank()&&!hn.equalsIgnoreCase(ip))targets.put("HOSTNAME",hn);
    java.util.LinkedHashMap<String,String> users=new java.util.LinkedHashMap<>(); users.put("CONFIGURED",configured); if(!leaf.isBlank()){users.put("BARE",leaf);users.put("DOT_LOCAL",".\\"+leaf);if(!hn.isBlank())users.put("HOST_LOCAL",hn+"\\"+leaf);}
    for(var t:targets.entrySet()){logT111(t.getKey(),t.getValue(),"CURRENT_CONTEXT","<kein -Credential>",false,attemptWindowsNativeT111Auth(t.getValue(),"","",port,https,false,15000L));}
    for(var t:targets.entrySet())for(var u:users.entrySet())logT111(t.getKey(),t.getValue(),u.getKey(),u.getValue(),true,attemptWindowsNativeT111Auth(t.getValue(),u.getValue(),password,port,https,true,15000L));
  }
  private void logT111(String tk,String target,String uk,String user,boolean credential,ModernWinRmAttempt r){String e=r.errorDetail()==null?"":r.errorDetail();if(e.length()>320)e=e.substring(0,320)+"...";String o=r.stdout()==null?"":r.stdout().replaceAll("[\\r\\n]+"," ");log.info("[WINRM-T111-AUTH] TargetKind={} Target={} UserKind={} User='{}' Credential={} Success={} Exit={} Stdout='{}' StderrChars={} ErrorType={} Error={}",tk,target,uk,user,credential,r.success(),r.exitCode(),o,r.stderr()==null?0:r.stderr().length(),r.errorType(),e);}

  private ModernWinRmAttempt attemptWindowsNative(String host, String username, String password, int port, boolean https, String command, boolean powershell, long timeout) {
    if (!isWindowsBackend()) {
      return new ModernWinRmAttempt(false, "", "", -1, "Windows-native SSPI nicht verfuegbar",
        "NATIVE_FALLBACK_UNAVAILABLE", "GAM-Backend laeuft nicht unter Windows.");
    }

    // Kleine Preflight-/cmd-Kommandos bleiben auf dem direkten, bewaehrten SSPI-Pfad.
    // Nur grosse PowerShell-Nutzlasten verwenden den bidirektionalen Chunk-Transfer.
    if(!powershell || command==null || command.length()<4096){
      java.nio.file.Path stdoutFile = null;
      java.nio.file.Path stderrFile = null;
      try {
        stdoutFile = java.nio.file.Files.createTempFile("gam-winrm-native-out-", ".txt");
        stderrFile = java.nio.file.Files.createTempFile("gam-winrm-native-err-", ".txt");

        String host64 = psBase64(host);
        String user64 = psBase64(username);
        String password64 = psBase64(password);
        String command64 = psBase64(command);
        String remoteBody = powershell
          ? "$sb=[ScriptBlock]::Create($remoteText); & $sb"
          : "& cmd.exe /d /s /c $remoteText";
        String script = String.join("\n",
          "$ErrorActionPreference='Stop'",
          "[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)",
          "$OutputEncoding=[Text.UTF8Encoding]::new($false)",
          "$utf8=[Text.Encoding]::UTF8",
          "$hostName=$utf8.GetString([Convert]::FromBase64String('" + host64 + "'))",
          "$userName=$utf8.GetString([Convert]::FromBase64String('" + user64 + "'))",
          "$plainPassword=$utf8.GetString([Convert]::FromBase64String('" + password64 + "'))",
          "$remoteText=$utf8.GetString([Convert]::FromBase64String('" + command64 + "'))",
          "$securePassword=ConvertTo-SecureString $plainPassword -AsPlainText -Force",
          "$credential=[PSCredential]::new($userName,$securePassword)",
          "$plainPassword=$null",
          "$invoke=@{ComputerName=$hostName;Port=" + port + ";Credential=$credential;ErrorAction='Stop'}",
          https ? "$invoke.UseSSL=$true" : "$null=$null",
          "$result=Invoke-Command @invoke -ScriptBlock { param($remoteText) " + remoteBody + " } -ArgumentList $remoteText",
          "$result | ForEach-Object { $_ }"
        );

        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", "-");
        pb.redirectOutput(stdoutFile.toFile());
        pb.redirectError(stderrFile.toFile());
        Process process = pb.start();
        try (var writer = new java.io.OutputStreamWriter(process.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
          writer.write(script);
        }
        boolean finished = process.waitFor(Math.max(5000L, timeout), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
          process.destroyForcibly();
          return new ModernWinRmAttempt(false, "", "", -1, "Windows-native WinRM/SSPI (Whitespace-Fallback)",
            "NATIVE_TIMEOUT", "Windows-nativer PowerShell-WinRM-Fallback lief in ein Timeout.");
        }
        String stdout = readWindowsPowerShellText(stdoutFile).trim();
        String stderr = readWindowsPowerShellText(stderrFile).trim();
        int exit = process.exitValue();
        String low = stderr.toLowerCase(java.util.Locale.ROOT);
        boolean remotingError = stderr.contains("PSSessionStateBroken") || stderr.contains("AccessDenied") || low.contains("zugriff verweigert") || low.contains("psremotingtransportexception");
        if (exit == 0 && !remotingError) {
          log.info("[WINRM-AUTH] Windows-nativer SSPI-Fallback fuer {} strikt erfolgreich ({} Zeichen)", host, stdout==null?0:stdout.length());
          return new ModernWinRmAttempt(true, stdout, stderr, 0, "Windows-native WinRM/PowerShell SSPI · Strict", "", "");
        }
        return new ModernWinRmAttempt(false, stdout, stderr, exit, "Windows-native WinRM/PowerShell SSPI · Whitespace-Fallback",
          "NATIVE_AUTH_FAILED", stderr.isBlank() ? "PowerShell-WinRM-Fallback Exit-Code " + exit : stderr.replaceAll("[\\r\\n]+", " "));
      } catch (Exception e) {
        return new ModernWinRmAttempt(false, "", "", -1, "Windows-native WinRM/PowerShell SSPI · Whitespace-Fallback",
          e.getClass().getSimpleName(), errorSummary(e));
      } finally {
        try { if (stdoutFile != null) java.nio.file.Files.deleteIfExists(stdoutFile); } catch (Exception ignored) {}
        try { if (stderrFile != null) java.nio.file.Files.deleteIfExists(stderrFile); } catch (Exception ignored) {}
      }
    }

    // 40k36t101: Bidirektionaler SSPI-Chunk-Transfer.
    // Upload: UTF-8-Skript -> Base64 -> nummerierte kleine Invoke-Command-Blöcke.
    // Remote: zusammensetzen, dekodieren, ausfuehren, Ergebnis UTF-8/Base64 puffern.
    // Download: Ergebnis-Base64 in nummerierten kleinen Blöcken zurueckholen.
    final int chunkSize=3072;
    java.nio.file.Path launcher=null;
    java.nio.file.Path stdoutFile=null;
    java.nio.file.Path stderrFile=null;
    try{
      String payloadB64=java.util.Base64.getEncoder().encodeToString(command.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      int uploadChunks=(payloadB64.length()+chunkSize-1)/chunkSize;
      String token="gam_sspi_"+java.util.UUID.randomUUID().toString().replace("-","");

      String host64=psBase64(host);
      String user64=psBase64(username);
      String password64=psBase64(password);

      StringBuilder ps=new StringBuilder();
      ps.append("$ErrorActionPreference='Stop'\n");
      ps.append("[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)\n");
      ps.append("$OutputEncoding=[Text.UTF8Encoding]::new($false)\n");
      ps.append("$utf8=[Text.Encoding]::UTF8\n");
      ps.append("$hostName=$utf8.GetString([Convert]::FromBase64String('").append(host64).append("'))\n");
      ps.append("$userName=$utf8.GetString([Convert]::FromBase64String('").append(user64).append("'))\n");
      ps.append("$plainPassword=$utf8.GetString([Convert]::FromBase64String('").append(password64).append("'))\n");
      ps.append("$securePassword=ConvertTo-SecureString $plainPassword -AsPlainText -Force\n");
      ps.append("$credential=[PSCredential]::new($userName,$securePassword)\n");
      ps.append("$plainPassword=$null\n");
      ps.append("$sessionArgs=@{ComputerName=$hostName;Port=").append(port).append(";Credential=$credential;ErrorAction='Stop'}\n");
      if(https) ps.append("$sessionArgs.UseSSL=$true\n");
      ps.append("$session=$null\n");
      ps.append("try{\n");
      ps.append("$session=New-PSSession @sessionArgs\n");
      ps.append("if($null -eq $session -or $session.State -ne 'Opened'){throw 'PERSISTENT_SESSION_NOT_OPEN'}\n");
      ps.append("Write-Output ('GAM_SESSION_OPEN id='+$session.Id+' state='+$session.State)\n");
      ps.append("$token='").append(token).append("'\n");
      ps.append("$expectedUpload=").append(uploadChunks).append("\n");
      ps.append("Write-Output ('GAM_CHUNK_UPLOAD_INIT token='+$token+' chunks='+$expectedUpload)\n");

      // INIT remote buffer in current user temp / process-independent file set.
      ps.append("$initSb={ param($token,$count) $d=Join-Path $env:TEMP $token; New-Item -ItemType Directory -Path $d -Force|Out-Null; Set-Content -LiteralPath (Join-Path $d 'count.txt') -Value $count -Encoding ascii; Remove-Item -LiteralPath (Join-Path $d 'payload.b64') -Force -ErrorAction SilentlyContinue; Remove-Item -LiteralPath (Join-Path $d 'result.b64') -Force -ErrorAction SilentlyContinue; 'INIT_OK' }\n");
      ps.append("$r=Invoke-Command -Session $session -ScriptBlock $initSb -ArgumentList $token,$expectedUpload\n");
      ps.append("if(($r|Out-String) -notmatch 'INIT_OK'){throw 'REMOTE_INIT_FAILED'}\n");

      for(int idx=0; idx<uploadChunks; idx++){
        int from=idx*chunkSize;
        int to=Math.min(payloadB64.length(),from+chunkSize);
        String ch=payloadB64.substring(from,to);
        ps.append("$chunk='").append(ch).append("'\n");
        ps.append("$uploadSb={ param($token,$i,$chunk) $d=Join-Path $env:TEMP $token; if(!(Test-Path $d)){throw 'UPLOAD_DIR_MISSING'}; Add-Content -LiteralPath (Join-Path $d 'payload.b64') -Value $chunk -Encoding ascii; ('UP_OK:'+ $i +':'+$chunk.Length) }\n");
        ps.append("$u=Invoke-Command -Session $session -ScriptBlock $uploadSb -ArgumentList $token,").append(idx).append(",$chunk\n");
        ps.append("if(($u|Out-String) -notmatch 'UP_OK:").append(idx).append(":'){throw 'UPLOAD_CHUNK_FAILED_").append(idx).append("'}\n");
        ps.append("Write-Output ('GAM_CHUNK_UPLOAD ").append(idx+1).append("/").append(uploadChunks).append(" size='+$chunk.Length)\n");
      }

      // Execute and write result.b64 remotely.
      ps.append("$execSb={ param($token) $d=Join-Path $env:TEMP $token; $p=Join-Path $d 'payload.b64'; if(!(Test-Path $p)){throw 'PAYLOAD_FILE_MISSING'}; $b64=((Get-Content -LiteralPath $p -Raw)-replace '\\s',''); $script=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64)); $out=& ([ScriptBlock]::Create($script)) *>&1 | ForEach-Object { if($_ -is [string]){$_}else{$_|Out-String -Width 100000} }; $text=($out -join [Environment]::NewLine); $resultB64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($text)); Set-Content -LiteralPath (Join-Path $d 'result.b64') -Value $resultB64 -Encoding ascii; ('EXEC_OK:'+ $text.Length +':'+$resultB64.Length) }\n");
      ps.append("$e=Invoke-Command -Session $session -ScriptBlock $execSb -ArgumentList $token\n");
      ps.append("$eline=($e|Select-Object -First 1|Out-String).Trim()\n");
      ps.append("if($eline -notmatch '^EXEC_OK:(\\d+):(\\d+)$'){throw ('REMOTE_EXEC_FAILED '+$eline)}\n");
      ps.append("$resultChars=[int64]$Matches[1]; $resultB64Chars=[int64]$Matches[2]\n");
      ps.append("Write-Output ('GAM_CHUNK_EXEC_OK resultChars='+$resultChars+' resultB64Chars='+$resultB64Chars)\n");
      ps.append("$downloadChunks=[int][Math]::Ceiling($resultB64Chars/").append(chunkSize).append(".0)\n");
      ps.append("Write-Output ('GAM_CHUNK_DOWNLOAD_INIT chunks='+$downloadChunks)\n");
      ps.append("$all=New-Object Text.StringBuilder\n");

      // Since result size is only known at runtime, use a PowerShell loop, each iteration one small Invoke-Command return.
      ps.append("for($i=0;$i -lt $downloadChunks;$i++){ $readSb={ param($token,$offset,$len) $d=Join-Path $env:TEMP $token; $b=((Get-Content -LiteralPath (Join-Path $d 'result.b64') -Raw)-replace '\\s',''); if($offset -ge $b.Length){return ''}; $take=[Math]::Min($len,$b.Length-$offset); $b.Substring($offset,$take) }; $piece=Invoke-Command -Session $session -ScriptBlock $readSb -ArgumentList $token,($i*").append(chunkSize).append("),").append(chunkSize).append("; $piece=[string]($piece|Select-Object -First 1); if([string]::IsNullOrEmpty($piece) -and $resultB64Chars -gt 0){throw ('DOWNLOAD_CHUNK_EMPTY_'+$i)}; [void]$all.Append($piece); Write-Output ('GAM_CHUNK_DOWNLOAD '+($i+1)+'/'+$downloadChunks+' size='+$piece.Length) }\n");
      ps.append("$joined=$all.ToString()\n");
      ps.append("if($joined.Length -ne $resultB64Chars){throw ('DOWNLOAD_LENGTH_MISMATCH expected='+$resultB64Chars+' actual='+$joined.Length)}\n");
      ps.append("$decoded=$utf8.GetString([Convert]::FromBase64String($joined))\n");
      ps.append("$cleanupSb={ param($token) $d=Join-Path $env:TEMP $token; Remove-Item -LiteralPath $d -Recurse -Force -ErrorAction SilentlyContinue; 'CLEANUP_OK' }\n");
      ps.append("$c=Invoke-Command -Session $session -ScriptBlock $cleanupSb -ArgumentList $token\n");
      ps.append("Write-Output 'GAM_CHUNK_DONE'\n");
      // Put a unique marker around actual payload so Java can strip diagnostics safely.
      ps.append("[Console]::Out.WriteLine('GAM_PAYLOAD_BEGIN')\n");
      ps.append("[Console]::Out.Write($decoded)\n");
      ps.append("[Console]::Out.WriteLine('')\n");
      ps.append("[Console]::Out.WriteLine('GAM_PAYLOAD_END')\n");
      ps.append("} finally { if($null -ne $session){ $sid=$session.Id; Remove-PSSession -Session $session -ErrorAction SilentlyContinue; Write-Output ('GAM_SESSION_CLOSED id='+$sid) } }\n");

      launcher=java.nio.file.Files.createTempFile("gam-winrm-sspi-chunk-",".ps1");
      byte[] bytes=ps.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
      byte[] bom=new byte[bytes.length+3];
      bom[0]=(byte)0xEF; bom[1]=(byte)0xBB; bom[2]=(byte)0xBF;
      System.arraycopy(bytes,0,bom,3,bytes.length);
      java.nio.file.Files.write(launcher,bom);

      stdoutFile=java.nio.file.Files.createTempFile("gam-winrm-sspi-chunk-out-",".txt");
      stderrFile=java.nio.file.Files.createTempFile("gam-winrm-sspi-chunk-err-",".txt");
      ProcessBuilder pb=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-File",launcher.toAbsolutePath().toString());
      pb.redirectOutput(stdoutFile.toFile());
      pb.redirectError(stderrFile.toFile());
      Process process=pb.start();
      boolean finished=process.waitFor(Math.max(15000L,timeout),java.util.concurrent.TimeUnit.MILLISECONDS);
      if(!finished){
        process.destroyForcibly();
        return new ModernWinRmAttempt(false,"","",-1,"Windows-native WinRM/PowerShell SSPI · persistente Session · bidirektionaler Chunk-Transfer",
          "NATIVE_CHUNK_TIMEOUT","Bidirektionaler SSPI-Chunk-Transfer lief in ein Timeout.");
      }

      String stdout=readWindowsPowerShellText(stdoutFile);
      String stderr=readWindowsPowerShellText(stderrFile).trim();
      int exit=process.exitValue();
      int begin=stdout.indexOf("GAM_PAYLOAD_BEGIN");
      int finish=stdout.indexOf("GAM_PAYLOAD_END",begin<0?0:begin);
      String payload="";
      if(begin>=0 && finish>begin){
        int s=stdout.indexOf('\n',begin);
        if(s<0) s=begin+"GAM_PAYLOAD_BEGIN".length();
        else s++;
        payload=stdout.substring(s,finish).replaceFirst("[\\r\\n]+$","").trim();
      }

      String diag=stdout;
      if(begin>=0 && finish>begin){
        diag=(stdout.substring(0,begin)+stdout.substring(finish+"GAM_PAYLOAD_END".length())).replaceAll("[\\r\\n]+"," | ").trim();
      }else{
        diag=stdout.replaceAll("[\\r\\n]+"," | ").trim();
      }
      log.info("[WINRM-SSPI-CHUNK] Host={} Session=persistent UploadChunks={} PayloadChars={} Exit={} Diagnose={}",
        host,uploadChunks,payload.length(),exit,diag.isBlank()?"<keine>":diag);

      if(exit==0 && !payload.isBlank()){
        return new ModernWinRmAttempt(true,payload,stderr,0,
          "Windows-native WinRM/PowerShell SSPI · persistente Session · bidirektionaler Chunk-Transfer","","");
      }

      String detail=stderr.isBlank()?diag:stderr;
      return new ModernWinRmAttempt(false,payload,stderr,exit,
        "Windows-native WinRM/PowerShell SSPI · persistente Session · bidirektionaler Chunk-Transfer",
        exit==0?"NATIVE_CHUNK_EMPTY":"NATIVE_CHUNK_FAILED",
        detail.isBlank()?"Bidirektionaler SSPI-Chunk-Transfer lieferte keine Nutzdaten.":detail);
    }catch(Exception e){
      return new ModernWinRmAttempt(false,"","",-1,
        "Windows-native WinRM/PowerShell SSPI · persistente Session · bidirektionaler Chunk-Transfer",
        e.getClass().getSimpleName(),errorSummary(e));
    }finally{
      try{if(launcher!=null)java.nio.file.Files.deleteIfExists(launcher);}catch(Exception ignored){}
      try{if(stdoutFile!=null)java.nio.file.Files.deleteIfExists(stdoutFile);}catch(Exception ignored){}
      try{if(stderrFile!=null)java.nio.file.Files.deleteIfExists(stderrFile);}catch(Exception ignored){}
    }
  }

  /**
   * 40k36t94: Windows-nativer SSPI-Dateitransport fuer das grosse Vollinventar.
   *
   * Der normale SSPI-Preflight beweist, dass PSSession/Authentifizierung funktioniert,
   * aber Invoke-Command-stdout kommt in dieser Konstellation leer bei Java an.
   * Deshalb werden Skript und Ergebnisdatei ueber Copy-Item -ToSession/-FromSession
   * transportiert. Die Inventardaten laufen damit NICHT ueber den problematischen
   * Remoting-stdout-Kanal.
   */
  /**
   * 40k36t97: BOM-/Windows-PowerShell-sichere Textdekodierung.
   * PowerShell 5.1 kann je nach Cmdlet UTF-8+BOM, UTF-16LE/BE oder die lokale
   * Windows-Codepage erzeugen. Eine Diagnose-/Ergebnisdatei darf deshalb niemals
   * blind als UTF-8 gelesen werden.
   */
  private static String readWindowsPowerShellText(java.nio.file.Path path) throws java.io.IOException{
    if(path==null || !java.nio.file.Files.exists(path)) return "";
    byte[] b=java.nio.file.Files.readAllBytes(path);
    if(b.length==0) return "";
    int off=0;
    java.nio.charset.Charset cs=null;
    if(b.length>=3 && (b[0]&0xff)==0xef && (b[1]&0xff)==0xbb && (b[2]&0xff)==0xbf){
      cs=java.nio.charset.StandardCharsets.UTF_8; off=3;
    }else if(b.length>=2 && (b[0]&0xff)==0xff && (b[1]&0xff)==0xfe){
      cs=java.nio.charset.StandardCharsets.UTF_16LE; off=2;
    }else if(b.length>=2 && (b[0]&0xff)==0xfe && (b[1]&0xff)==0xff){
      cs=java.nio.charset.StandardCharsets.UTF_16BE; off=2;
    }
    if(cs!=null) return new String(b,off,b.length-off,cs).replace("\uFEFF","");
    try{
      java.nio.charset.CharsetDecoder dec=java.nio.charset.StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
      return dec.decode(java.nio.ByteBuffer.wrap(b)).toString().replace("\uFEFF","");
    }catch(java.nio.charset.CharacterCodingException invalidUtf8){
      // Windows PowerShell 5.1 kann BOM-lose Dateien in der aktiven ANSI-Codepage
      // erzeugen. Windows-1252 deckt die deutsch/westeuropaeische Zielumgebung ab;
      // ISO-8859-1 ist der verlustfreie letzte Ein-Byte-Fallback.
      try{
        return new String(b,java.nio.charset.Charset.forName("windows-1252")).replace("\uFEFF","");
      }catch(Exception ignored){
        return new String(b,java.nio.charset.StandardCharsets.ISO_8859_1).replace("\uFEFF","");
      }
    }
  }

  private ModernWinRmAttempt attemptWindowsNativeFileTransfer(String host,String username,String password,
                                                               int port,boolean https,String command,long timeout){
    if(!isWindowsBackend()){
      return new ModernWinRmAttempt(false,"","",-1,"Windows-native SSPI-Dateitransport nicht verfuegbar",
        "NATIVE_FILE_UNAVAILABLE","GAM-Backend laeuft nicht unter Windows.");
    }

    java.nio.file.Path localPayload=null;
    java.nio.file.Path localResult=null;
    java.nio.file.Path stdoutFile=null;
    java.nio.file.Path stderrFile=null;
    try{
      localPayload=java.nio.file.Files.createTempFile("gam-winrm-sspi-payload-",".ps1");
      localResult=java.nio.file.Files.createTempFile("gam-winrm-sspi-result-",".txt");
      stdoutFile=java.nio.file.Files.createTempFile("gam-winrm-sspi-launch-",".txt");
      stderrFile=java.nio.file.Files.createTempFile("gam-winrm-sspi-error-",".txt");
      java.nio.file.Files.writeString(localPayload,command,java.nio.charset.StandardCharsets.UTF_8);
      java.nio.file.Files.writeString(localResult,"",java.nio.charset.StandardCharsets.UTF_8);

      String host64=psBase64(host);
      String user64=psBase64(username);
      String password64=psBase64(password);
      String payload64=psBase64(localPayload.toAbsolutePath().toString());
      String result64=psBase64(localResult.toAbsolutePath().toString());
      String token="gam_inv_"+java.util.UUID.randomUUID().toString().replace("-","");

      String script=String.join("\n",
        "$ErrorActionPreference='Stop'",
        "[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)",
        "$OutputEncoding=[Text.UTF8Encoding]::new($false)",
        "$utf8=[Text.Encoding]::UTF8",
        "$hostName=$utf8.GetString([Convert]::FromBase64String('"+host64+"'))",
        "$userName=$utf8.GetString([Convert]::FromBase64String('"+user64+"'))",
        "$plainPassword=$utf8.GetString([Convert]::FromBase64String('"+password64+"'))",
        "$localPayload=$utf8.GetString([Convert]::FromBase64String('"+payload64+"'))",
        "$localResult=$utf8.GetString([Convert]::FromBase64String('"+result64+"'))",
        "$securePassword=ConvertTo-SecureString $plainPassword -AsPlainText -Force",
        "$credential=[PSCredential]::new($userName,$securePassword)",
        "$plainPassword=$null",
        "$sessionArgs=@{ComputerName=$hostName;Port="+port+";Credential=$credential;ErrorAction='Stop'}",
        https ? "$sessionArgs.UseSSL=$true" : "$null=$null",
        "$session=$null",
        "try{",
        "  $session=New-PSSession @sessionArgs",
        "  if($null -eq $session){throw 'SESSION_CREATE_FAILED'}",
        "  Write-Output 'GAM_STAGE=SESSION_CREATED'",
        "  $remoteDir=Invoke-Command -Session $session -ErrorAction Stop -ScriptBlock {",
        "    param($token)",
        "    $d=Join-Path $env:TEMP $token",
        "    New-Item -ItemType Directory -Path $d -Force -ErrorAction Stop|Out-Null",
        "    if(!(Test-Path -LiteralPath $d)){throw 'REMOTE_DIR_MISSING'}",
        "    $d",
        "  } -ArgumentList '"+token+"'",
        "  $remoteDir=[string]($remoteDir|Select-Object -First 1)",
        "  if([string]::IsNullOrWhiteSpace($remoteDir)){throw 'REMOTE_DIR_EMPTY'}",
        "  Write-Output ('GAM_STAGE=REMOTE_DIR_CREATED path='+$remoteDir)",
        "  $remotePayload=Join-Path $remoteDir 'payload.ps1'",
        "  $remoteResult=Join-Path $remoteDir 'result.txt'",
        "  Copy-Item -LiteralPath $localPayload -Destination $remotePayload -ToSession $session -Force -ErrorAction Stop",
        "  $remotePayloadSize=Invoke-Command -Session $session -ErrorAction Stop -ScriptBlock {",
        "    param($p)",
        "    if(!(Test-Path -LiteralPath $p)){throw 'PAYLOAD_MISSING_AFTER_COPY'}",
        "    (Get-Item -LiteralPath $p -ErrorAction Stop).Length",
        "  } -ArgumentList $remotePayload",
        "  $remotePayloadSize=[int64]($remotePayloadSize|Select-Object -First 1)",
        "  if($remotePayloadSize -le 0){throw 'PAYLOAD_ZERO_AFTER_COPY'}",
        "  Write-Output ('GAM_STAGE=PAYLOAD_COPIED size='+$remotePayloadSize)",
        "  Invoke-Command -Session $session -ErrorAction Stop -ScriptBlock {",
        "    param($payload,$result)",
        "    $ErrorActionPreference='Stop'",
        "    if(!(Test-Path -LiteralPath $payload)){throw 'PAYLOAD_NOT_FOUND_BEFORE_EXEC'}",
        "    Write-Output 'REMOTE_STAGE=PAYLOAD_STARTED'",
        "    try{",
        "      & powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $payload *>&1 |",
        "        ForEach-Object { $_ | Out-String -Width 100000 } |",
        "        Out-File -LiteralPath $result -Encoding utf8 -Width 100000 -ErrorAction Stop",
        "    }catch{",
        "      ('GAM_EXEC_EXCEPTION='+($_ | Out-String -Width 100000)) | Out-File -LiteralPath $result -Encoding utf8 -Append -Width 100000",
        "      throw",
        "    }",
        "    if(!(Test-Path -LiteralPath $result)){throw 'RESULT_FILE_MISSING'}",
        "    $len=(Get-Item -LiteralPath $result -ErrorAction Stop).Length",
        "    if($len -le 0){throw 'RESULT_FILE_EMPTY'}",
        "    Write-Output ('REMOTE_STAGE=PAYLOAD_FINISHED size='+$len)",
        "  } -ArgumentList $remotePayload,$remoteResult | ForEach-Object { Write-Output ('GAM_REMOTE='+$_) }",
        "  $remoteResultSize=Invoke-Command -Session $session -ErrorAction Stop -ScriptBlock {",
        "    param($r)",
        "    if(!(Test-Path -LiteralPath $r)){throw 'RESULT_MISSING_BEFORE_COPY'}",
        "    (Get-Item -LiteralPath $r -ErrorAction Stop).Length",
        "  } -ArgumentList $remoteResult",
        "  $remoteResultSize=[int64]($remoteResultSize|Select-Object -First 1)",
        "  if($remoteResultSize -le 0){throw 'RESULT_ZERO_BEFORE_COPY'}",
        "  Write-Output ('GAM_STAGE=RESULT_REMOTE size='+$remoteResultSize)",
        "  Copy-Item -LiteralPath $remoteResult -Destination $localResult -FromSession $session -Force -ErrorAction Stop",
        "  if(!(Test-Path -LiteralPath $localResult)){throw 'LOCAL_RESULT_MISSING_AFTER_COPY'}",
        "  $localSize=(Get-Item -LiteralPath $localResult -ErrorAction Stop).Length",
        "  if($localSize -le 0){throw 'LOCAL_RESULT_EMPTY_AFTER_COPY'}",
        "  Write-Output ('GAM_STAGE=RESULT_COPIED size='+$localSize)",
        "  Invoke-Command -Session $session -ErrorAction Stop -ScriptBlock { param($d) Remove-Item -LiteralPath $d -Recurse -Force -ErrorAction Stop } -ArgumentList $remoteDir | Out-Null",
        "  Write-Output 'GAM_STAGE=CLEANUP_OK'",
        "}catch{",
        "  Write-Error ('GAM_STAGE_ERROR='+($_.Exception.Message))",
        "  exit 91",
        "}finally{",
        "  if($session){Remove-PSSession $session -ErrorAction SilentlyContinue}",
        "}"
      );

      // 40k36t96: Den mehrzeiligen Launcher nicht mehr ueber "-Command -" / stdin
      // einspeisen. Windows PowerShell verarbeitet stdin dabei pseudo-interaktiv und
      // mehrzeilige Konstrukte koennen still unvollstaendig bleiben. Stattdessen wird
      // der komplette Launcher als lokale UTF-8-BOM-PS1 geschrieben und mit -File
      // ausgefuehrt.
      java.nio.file.Path launcherFile=java.nio.file.Files.createTempFile("gam-winrm-sspi-launcher-",".ps1");
      try{
        byte[] scriptBytes=script.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bomScript=new byte[scriptBytes.length+3];
        bomScript[0]=(byte)0xEF; bomScript[1]=(byte)0xBB; bomScript[2]=(byte)0xBF;
        System.arraycopy(scriptBytes,0,bomScript,3,scriptBytes.length);
        java.nio.file.Files.write(launcherFile,bomScript);

        ProcessBuilder pb=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive",
          "-ExecutionPolicy","Bypass","-File",launcherFile.toAbsolutePath().toString());
        pb.redirectOutput(stdoutFile.toFile());
        pb.redirectError(stderrFile.toFile());
        Process process=pb.start();
        boolean finished=process.waitFor(Math.max(10000L,timeout),java.util.concurrent.TimeUnit.MILLISECONDS);
      if(!finished){
        process.destroyForcibly();
        return new ModernWinRmAttempt(false,"","",-1,"Windows-native SSPI · Datei-Transfer",
          "NATIVE_FILE_TIMEOUT","SSPI-Datei-Vollinventar lief in ein Timeout.");
      }

      String launcherOut=readWindowsPowerShellText(stdoutFile).trim();
      String launcherErr=readWindowsPowerShellText(stderrFile).trim();
      int exit=process.exitValue();
      String result=java.nio.file.Files.exists(localResult)
        ? readWindowsPowerShellText(localResult).trim()
        : "";

      String stages=launcherOut.replaceAll("[\\r\\n]+"," | ");
      log.info("[WINRM-SSPI-FILE] Stufen Host={} Exit={} {}",host,exit,stages.isBlank()?"<keine>":stages);

      if(exit==0 && !result.isBlank()){
        log.info("[WINRM-SSPI-FILE] Vollinventar erfolgreich Host={} Ergebniszeichen={}",host,result.length());
        return new ModernWinRmAttempt(true,result,launcherErr,0,
          "Windows-native WinRM/PowerShell SSPI · Datei-Transfer","","");
      }

      String detail=launcherErr.isBlank()?launcherOut:launcherErr;
      if(exit==0 && result.isBlank()) detail="SSPI-Dateitransport endete ohne harte Exception, lokale Ergebnisdatei blieb aber leer. "+detail;
      log.warn("[WINRM-SSPI-FILE] Vollinventar fehlgeschlagen Host={} Exit={} Ergebniszeichen={} Detail={}",
        host,exit,result.length(),trim(detail));
      return new ModernWinRmAttempt(false,result,launcherErr,exit,
        "Windows-native WinRM/PowerShell SSPI · Datei-Transfer",
        exit==0?"NATIVE_FILE_EMPTY":"NATIVE_FILE_FAILED",detail);
      }finally{
        try{java.nio.file.Files.deleteIfExists(launcherFile);}catch(Exception ignored){}
      }
    }catch(Exception e){
      return new ModernWinRmAttempt(false,"","",-1,"Windows-native WinRM/PowerShell SSPI · Datei-Transfer",
        e.getClass().getSimpleName(),errorSummary(e));
    }finally{
      try{if(localPayload!=null)java.nio.file.Files.deleteIfExists(localPayload);}catch(Exception ignored){}
      try{if(localResult!=null)java.nio.file.Files.deleteIfExists(localResult);}catch(Exception ignored){}
      try{if(stdoutFile!=null)java.nio.file.Files.deleteIfExists(stdoutFile);}catch(Exception ignored){}
      try{if(stderrFile!=null)java.nio.file.Files.deleteIfExists(stderrFile);}catch(Exception ignored){}
    }
  }


  private ModernWinRmAttempt attemptModern(String host, String username, String password, int port, boolean https, String command, boolean powershell, long timeout) {
    ModernWinRmAttempt original = attemptModernSingle(host, username, password, port, https, command, powershell, timeout);
    if (original.success() || !isLocalWhitespaceAccount(username)) return original;

    ModernWinRmAttempt nativeAttempt = attemptWindowsNative(host, username, password, port, https, command, powershell, timeout);
    if (nativeAttempt.success()) return nativeAttempt;

    String detail = original.errorDetail() + " | Windows-native Whitespace-Fallback: " + nativeAttempt.errorDetail();
    return new ModernWinRmAttempt(false, "", "", -1, original.authentication(), original.errorType(), detail);
  }

  private WinRmAttempt attemptCommand(String host, String username, String password, int port, boolean https, String authScheme, String command, boolean powershell, long timeout) {
    WinRmClientContext context = WinRmClientContext.newInstance();
    try {
      WinRmTool tool = builderFor(host, context, username, password, port, https, authScheme).build();
      tool.setOperationTimeout(timeout);
      WinRmToolResponse response = powershell ? tool.executePs(command) : tool.executeCommand(command);
      return new WinRmAttempt(response.getStatusCode()==0, response, authScheme,
        response.getStatusCode()==0 ? "" : "WinRmStatus" ,
        response.getStatusCode()==0 ? "" : "Status "+response.getStatusCode()+": "+trim(response.getStdErr()));
    } catch (LinkageError e) {
      throw e;
    } catch (Exception e) {
      return new WinRmAttempt(false, null, authScheme, e.getClass().getSimpleName(), credentialIdentity(username)+" · "+errorSummary(e));
    } finally {
      context.shutdown();
    }
  }

  /**
   * 40k36t53: Bidirektionaler Legacy-Transport fuer grosse PowerShell-Inventare.
   * Upload UND Rueckgabe laufen blockweise ueber eine einzige WinRM-Session.
   */
  private LegacyScriptResult attemptLargePowerShellScript(String host, String username, String password,
                                                           int port, boolean https, String authScheme,
                                                           String script, long timeout) {
    String token="gam_inv_"+UUID.randomUUID().toString().replace("-","");
    String b64Name=token+".b64";
    String ps1Name=token+".ps1";
    String outName=token+".out";
    String encoded=Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8));
    final int uploadChunkSize=1800;
    final int chunks=(encoded.length()+uploadChunkSize-1)/uploadChunkSize;

    WinRmClientContext context=WinRmClientContext.newInstance();
    long legacyTotalStart=System.nanoTime();
    try {
      WinRmTool tool=builderFor(host,context,username,password,port,https,authScheme).build();
      tool.setOperationTimeout(timeout);

      String init="$b=Join-Path $env:TEMP '"+b64Name+"';"
        +"$p=Join-Path $env:TEMP '"+ps1Name+"';"
        +"$o=Join-Path $env:TEMP '"+outName+"';"
        +"Remove-Item -LiteralPath $b,$p,$o -Force -ErrorAction SilentlyContinue;"
        +"[IO.File]::WriteAllText($b,'',[Text.Encoding]::ASCII)";
      long stageStart=System.nanoTime();
      WinRmToolResponse response=tool.executePs(init);
      log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=INIT DurationMs={}",host,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stageStart));
      if(response.getStatusCode()!=0) return transferFailureResult("INIT",response);

      log.info("[WINRM-TRANSFER] INIT Host={} Upload-Chunks={}",host,chunks);
      stageStart=System.nanoTime();
      for(int index=0,pos=0;pos<encoded.length();index++,pos+=uploadChunkSize){
        String chunk=encoded.substring(pos,Math.min(encoded.length(),pos+uploadChunkSize));
        String upload="$b=Join-Path $env:TEMP '"+b64Name+"';"
          +"[IO.File]::AppendAllText($b,'"+chunk+"',[Text.Encoding]::ASCII)";
        response=tool.executePs(upload);
        if(response.getStatusCode()!=0) return transferFailureResult("UPLOAD "+(index+1)+"/"+chunks,response);
      }

      log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=UPLOAD Chunks={} DurationMs={}",host,chunks,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stageStart));

      // 40k36t53: Windows PowerShell 5.1 verarbeitet .ps1-Dateien am robustesten
      // als UTF-16LE ("Unicode"). Der hochgeladene Payload ist UTF-8; daher remote
      // explizit UTF-8 dekodieren und anschließend als UTF-16LE mit BOM speichern.
      String reconstruct="$b=Join-Path $env:TEMP '"+b64Name+"';"
        +"$p=Join-Path $env:TEMP '"+ps1Name+"';"
        +"try{"
        +"  $payload=[Convert]::FromBase64String([IO.File]::ReadAllText($b));"
        +"  $text=[Text.Encoding]::UTF8.GetString($payload);"
        +"  [IO.File]::WriteAllText($p,$text,[Text.Encoding]::Unicode)"
        +"}catch{Write-Error $_.Exception.Message;exit 41}";
      stageStart=System.nanoTime();
      response=tool.executePs(reconstruct);
      log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=RECONSTRUCT DurationMs={}",host,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stageStart));
      if(response.getStatusCode()!=0) return transferFailureResult("RECONSTRUCT",response);

      // Ausgabe NICHT direkt ueber die einzelne WinRM-Antwort schicken.
      // UTF-8 Ergebnisdatei erzeugen, danach Base64-segmentiert ruecklesen.
      String execute="$p=Join-Path $env:TEMP '"+ps1Name+"';"
        +"$o=Join-Path $env:TEMP '"+outName+"';"
        +"$ErrorActionPreference='Continue';"
        +"try{"
        +"  & powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $p *>&1"
        +"    | ForEach-Object { $_ | Out-String -Width 100000 }"
        +"    | Out-File -LiteralPath $o -Encoding utf8 -Width 100000"
        +"}catch{"
        +"  ('GAM_EXEC_EXCEPTION='+($_ | Out-String -Width 100000)) | Out-File -LiteralPath $o -Encoding utf8 -Append -Width 100000"
        +"};"
        +"if(!(Test-Path -LiteralPath $o)){"
        +"  'GAM_EXEC_EXCEPTION=Keine Ergebnisdatei erzeugt' | Out-File -LiteralPath $o -Encoding utf8"
        +"}";
      stageStart=System.nanoTime();
      response=tool.executePs(execute);
      log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=EXECUTE DurationMs={}",host,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stageStart));
      if(response.getStatusCode()!=0){
        log.warn("[WINRM-TRANSFER] EXECUTE-Wrapper meldet Status {}, Ergebnisdatei wird trotzdem abgeholt. stderr={}",
          response.getStatusCode(),trim(response.getStdErr()));
      }

      String meta="$o=Join-Path $env:TEMP '"+outName+"';"
        +"if(!(Test-Path -LiteralPath $o)){Write-Error 'Ergebnisdatei fehlt';exit 43};"
        +"$b=[Convert]::ToBase64String([IO.File]::ReadAllBytes($o));"
        +"Write-Output ('LEN='+$b.Length)";
      stageStart=System.nanoTime();
      response=tool.executePs(meta);
      log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=RESULT_META DurationMs={}",host,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stageStart));
      if(response.getStatusCode()!=0) return transferFailureResult("RESULT_META",response);
      String metaOut=response.getStdOut()==null?"":response.getStdOut();
      int resultLength=-1;
      try{
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("LEN\\s*=\\s*(\\d+)").matcher(metaOut);
        if(matcher.find()) resultLength=Integer.parseInt(matcher.group(1));
      }catch(Exception ignored){}
      if(resultLength<0) return new LegacyScriptResult(false,"","RESULT_META","RESULT_META · ungueltige Laenge · stdout="+trim(metaOut));

      final int resultChunkSize=1600;
      StringBuilder resultB64=new StringBuilder(resultLength);
      int resultChunks=(resultLength+resultChunkSize-1)/resultChunkSize;
      log.info("[WINRM-TRANSFER] RESULT Host={} Base64-Laenge={} Rueck-Chunks={}",host,resultLength,resultChunks);
      stageStart=System.nanoTime();
      for(int index=0,pos=0;pos<resultLength;index++,pos+=resultChunkSize){
        int take=Math.min(resultChunkSize,resultLength-pos);
        String begin="GAM_B64_BEGIN_"+index;
        String end="GAM_B64_END_"+index;
        String read="$o=Join-Path $env:TEMP '"+outName+"';"
          +"$b=[Convert]::ToBase64String([IO.File]::ReadAllBytes($o));"
          +"Write-Output '"+begin+"';"
          +"Write-Output $b.Substring("+pos+","+take+");"
          +"Write-Output '"+end+"'";
        response=tool.executePs(read);
        if(response.getStatusCode()!=0) return transferFailureResult("RESULT "+(index+1)+"/"+resultChunks,response);

        String raw=response.getStdOut()==null?"":response.getStdOut();
        int b=raw.indexOf(begin);
        int e=b<0?-1:raw.indexOf(end,b+begin.length());
        if(b<0 || e<0)
          return new LegacyScriptResult(false,"","RESULT_MARKER",
            "RESULT "+(index+1)+"/"+resultChunks+" · Marker fehlen · stdout="+trim(raw));
        String payload=raw.substring(b+begin.length(),e).replaceAll("\\s+","");
        resultB64.append(payload);
      }

      log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=DOWNLOAD Chunks={} DurationMs={}",host,resultChunks,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stageStart));

      String inventoryStdout;
      stageStart=System.nanoTime();
      try{
        String payload=resultB64.toString();
        log.info("[WINRM-TRANSFER] RESULT_DECODE Host={} Base64 erwartet={} tatsaechlich={}",host,resultLength,payload.length());
        int badIndex=firstInvalidBase64Index(payload);
        if(badIndex>=0){
          char bad=payload.charAt(badIndex);
          return new LegacyScriptResult(false,"","RESULT_DECODE",
            "RESULT_DECODE · ungueltiges Zeichen bei Position "+badIndex+" · hex="+Integer.toHexString(bad));
        }
        inventoryStdout=new String(Base64.getDecoder().decode(payload),StandardCharsets.UTF_8);
        if(!inventoryStdout.isEmpty() && inventoryStdout.charAt(0)=='\uFEFF') inventoryStdout=inventoryStdout.substring(1);
      }catch(Exception e){
        return new LegacyScriptResult(false,"","RESULT_DECODE","RESULT_DECODE · "+e.getMessage());
      }

      log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=DECODE DurationMs={}",host,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stageStart));

      try{
        stageStart=System.nanoTime();
        String cleanup="$b=Join-Path $env:TEMP '"+b64Name+"';"
          +"$p=Join-Path $env:TEMP '"+ps1Name+"';"
          +"$o=Join-Path $env:TEMP '"+outName+"';"
          +"Remove-Item -LiteralPath $b,$p,$o -Force -ErrorAction SilentlyContinue";
        tool.executePs(cleanup);
        log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=CLEANUP DurationMs={}",host,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-stageStart));
      }catch(Exception e){
        log.debug("[WINRM-TRANSFER] CLEANUP Host={} fehlgeschlagen: {}",host,errorSummary(e));
      }

      log.info("[WINRM-T120-LEGACY-TIME] Host={} Phase=TOTAL DurationMs={}",host,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-legacyTotalStart));
      log.info("[WINRM-TRANSFER] abgeschlossen Host={} Ausgabezeichen={}",host,inventoryStdout.length());
      return new LegacyScriptResult(true,inventoryStdout,"","");
    } catch (LinkageError e) {
      throw e;
    } catch (Exception e) {
      return new LegacyScriptResult(false,"",e.getClass().getSimpleName(),
        "TRANSFER · "+credentialIdentity(username)+" · "+errorSummary(e));
    } finally {
      context.shutdown();
    }
  }

  private int firstInvalidBase64Index(String value){
    if(value==null) return 0;
    for(int i=0;i<value.length();i++){
      char c=value.charAt(i);
      boolean ok=(c>='A'&&c<='Z')||(c>='a'&&c<='z')||(c>='0'&&c<='9')||c=='+'||c=='/'||c=='=';
      if(!ok) return i;
    }
    return -1;
  }

  private LegacyScriptResult transferFailureResult(String stage,WinRmToolResponse response){
    String stdout=trim(response==null?"":response.getStdOut());
    String stderr=trim(response==null?"":response.getStdErr());
    int status=response==null?-1:response.getStatusCode();
    String detail=stage+" · Status "+status+" · stdout="+(stdout.isBlank()?"<leer>":stdout)
      +" · stderr="+(stderr.isBlank()?"<leer>":stderr);
    log.warn("[WINRM-TRANSFER] {}",detail);
    return new LegacyScriptResult(false,"","TRANSFER_"+stage.replace(' ','_').replace('/','_'),detail);
  }

  /** 40k36t28: detaillierter Erreichbarkeits-/Anmeldetest mit sichtbarer Authentifizierungsstrategie. */
  public WinRmTestResult testConnection(String host) {
    var s=settingsFor(host);
    return testConnection(host,s.enabled(),s.username(),s.password(),s.port(),s.https());
  }

  /** 40k36q3/t28: Verbindungstest mit einem konkreten gespeicherten Mehrfachzugang. */
  public WinRmTestResult testConnection(String host, boolean enabled, String username, String password, int port, boolean https) {
    String now = OffsetDateTime.now().toString();
    String endpoint=(https?"https":"http")+"://"+host+":"+port+"/wsman";
    String userFormat=usernameFormat(username);
    boolean trustRequired=trustRequired(host,https);
    boolean trustedTarget=trustedTargets.isTrusted(host);
    if (!enabled || username == null || username.isBlank()) {
      log.debug("WinRM-Verbindungstest für {} übersprungen: kein gemeinsamer WinRM-Zugang konfiguriert", host);
      return new WinRmTestResult(false,false,"Keine aktive WinRM-Quelle mit Benutzername konfiguriert.",now,false,false,endpoint,userFormat,
        "Moderner WinRM-Client (Kerberos/SPNEGO bevorzugt)","—",false,"NOT_CONFIGURED","WinRM-Zugang ist deaktiviert oder ohne Benutzername.","MetricsHub winrm-java 2.1.00 (Standard)",trustedTarget,trustRequired);
    }

    boolean tcp=tcpReachable(host,port);
    if(!tcp){
      return new WinRmTestResult(true,false,"TCP-Port "+port+" ist nicht erreichbar; WS-Man wurde deshalb nicht gestartet.",now,false,false,endpoint,userFormat,
        "Moderner WinRM-Client (Kerberos/SPNEGO bevorzugt)","—",false,"TCP_UNREACHABLE","Keine TCP-Verbindung zu "+host+":"+port+" innerhalb 1,8 s.","MetricsHub winrm-java 2.1.00 (Standard)",trustedTarget,trustRequired);
    }


    if(trustRequired && !trustedTarget){
      String detail="HTTP-Zugriff auf eine IP-Adresse in einer Workgroup-/Nicht-Kerberos-Konstellation ist in GAM noch nicht explizit als vertrauenswuerdig freigegeben.";
      return new WinRmTestResult(true,false,"WinRM-Endpunkt ist per TCP erreichbar. Fuer HTTP ueber eine IP-Adresse muss dieses konkrete Ziel in GAM zuerst explizit als vertrauenswuerdig bestaetigt werden.",now,true,false,endpoint,userFormat,
        "Explizites Zielvertrauen erforderlich","keine",false,"TARGET_NOT_TRUSTED",detail,"MetricsHub winrm-java 2.1.00 (Standard)",false,true);
    }

    try {
      ModernWinRmAttempt modern=attemptModern(host,username,password,port,https,"hostname",false,5000L);
      if(modern.success()){
        boolean legacyAuth=!https;
        String msg=legacyAuth
          ? "WinRM erreichbar, Anmeldung über modernen WinRM-Stack erfolgreich. Auf HTTP/IP wurde NTLM als Kompatibilitätsauthentifizierung verwendet; für Kerberos ist HTTPS mit geeignetem Hostnamen erforderlich."
          : "WinRM erreichbar, Anmeldung über modernen WinRM-Stack erfolgreich. Kerberos/SPNEGO wurde priorisiert.";
        boolean nativeWhitespace = modern.authentication()!=null && modern.authentication().contains("Windows-native");
        if(nativeWhitespace)
          msg += " Lokaler Benutzername mit Leerzeichen wurde ueber den Windows-nativen SSPI-Fallback authentifiziert.";
        return new WinRmTestResult(true,true,msg,now,true,true,endpoint,userFormat,
          nativeWhitespace ? "MetricsHub Standard → Windows-native SSPI bei Whitespace" : "MetricsHub modern: "+(https?"Kerberos/SPNEGO → NTLM":"NTLM auf HTTP (Kompatibilität)"),modern.authentication(),legacyAuth,"","",
          nativeWhitespace ? "Windows PowerShell/SSPI (Whitespace-Fallback)" : "MetricsHub winrm-java 2.1.00 (Standard)",trustedTarget,trustRequired);
      }

      log.warn("[WINRM-AUTH] {} moderner WinRM-Stack fehlgeschlagen: {}", endpoint, modern.errorDetail());

      // 40k36t125: Verbindungstest verwendet dieselbe Reihenfolge wie die Inventarisierung:
      // MetricsHub modern -> Windows-native SSPI Current Context -> Legacy winrm4j/CXF.
      // Auf Nicht-Windows-Backends ist der SSPI-Schritt nicht verfuegbar und wird sauber uebersprungen.
      ModernWinRmAttempt sspiProbe=attemptWindowsNativeT111Auth(host,"","",port,https,false,15000L);
      log.info("[WINRM-T125-AUTH] Phase=SSPI_CURRENT_CONTEXT Host={} Credential=false Success={} Exit={} Stdout='{}' StderrChars={} ErrorType={} Error={}",
        host,sspiProbe.success(),sspiProbe.exitCode(),
        sspiProbe.stdout()==null?"":sspiProbe.stdout().replaceAll("[\r\n]+"," "),
        sspiProbe.stderr()==null?0:sspiProbe.stderr().length(),
        sspiProbe.errorType(),sspiProbe.errorDetail());
      if(sspiProbe.success()){
        String detail="Moderner Stack: "+modern.errorDetail();
        return new WinRmTestResult(true,true,
          "WinRM erreichbar. Der moderne Stack schlug fehl; Windows-native PowerShell/SSPI war im aktuellen Windows-Kontext erfolgreich.",
          now,true,true,endpoint,userFormat,
          "MetricsHub modern zuerst → Windows-native SSPI → Legacy",
          "Windows-native SSPI (Current Context)",false,"MODERN_STACK_FAILED",detail,
          "Windows PowerShell/SSPI (Current Context)",trustedTarget,trustRequired);
      }

      // 40k36t53: HTTP + IP ist typischer Workgroup-/Nicht-Kerberos-Betrieb.
      // Der Legacy-SPNEGO-Versuch erzeugte unter Linux nur einen lauten CXF/Kerberos-
      // Stacktrace. Nach MetricsHub deshalb direkt den bewiesenen NTLM-Pfad nutzen.
      boolean directLegacyNtlm=!https && isIpLiteral(host);
      WinRmAttempt primary=directLegacyNtlm
        ? new WinRmAttempt(false,null,AUTH_PRIMARY,"SKIPPED","Legacy Negotiate/SPNEGO für HTTP/IP bewusst übersprungen.")
        : attemptCommand(host,username,password,port,https,AUTH_PRIMARY,"hostname",false,5000L);
      if(primary.success()){
        return new WinRmTestResult(true,true,"Moderner WinRM-Stack schlug fehl; Legacy-winrm4j war über Negotiate/SPNEGO erfolgreich.",now,true,true,endpoint,userFormat,
          "MetricsHub modern zuerst → SSPI → Legacy","Negotiate/SPNEGO (Legacy-Stack)",true,"MODERN_STACK_FAILED",modern.errorDetail(),
          "Legacy winrm4j/CXF 3.x (Fallback)",trustedTarget,trustRequired);
      }
      WinRmAttempt fallback=attemptCommand(host,username,password,port,https,AUTH_LEGACY_FALLBACK,"hostname",false,5000L);
      if(fallback.success()){
        String detail="Moderner Stack: "+modern.errorDetail()+" | Legacy Negotiate: "+primary.errorDetail();
        return new WinRmTestResult(true,true,
          directLegacyNtlm
            ? "WinRM erreichbar. Nach dem modernen Stack wurde auf HTTP/IP direkt der NTLM-Legacy-Fallback verwendet."
            : "WinRM erreichbar, aber nur über den NTLM-Legacy-Fallback des alten Stacks.",
          now,true,true,endpoint,userFormat,
          directLegacyNtlm ? "MetricsHub modern zuerst → SSPI → NTLM Legacy direkt auf HTTP/IP" : "MetricsHub modern zuerst → SSPI → Legacy",
          "NTLM (Legacy-winrm4j)",true,"MODERN_AND_PRIMARY_FAILED",detail,
          "Legacy winrm4j/CXF 3.x (NTLM-Fallback)",trustedTarget,trustRequired);
      }
      String detail="Moderner Stack: "+modern.errorDetail()+" | Legacy Negotiate/SPNEGO: "+primary.errorDetail()+" | Legacy NTLM: "+fallback.errorDetail();
      return new WinRmTestResult(true,false,"WinRM-Endpunkt ist per TCP erreichbar, aber alle Authentifizierungs-/Clientpfade schlugen fehl. "+detail,now,true,true,endpoint,userFormat,
        "MetricsHub modern zuerst → SSPI → Legacy","keine",true,"AUTHENTICATION_FAILED",detail,"kein Stack erfolgreich",trustedTarget,trustRequired);
    } catch (LinkageError e) {
      log.error("WinRM-Laufzeitkomponente für {} konnte nicht geladen werden", host, e);
      String detail=e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());
      return new WinRmTestResult(true,false,"WinRM-Client konnte nicht gestartet werden: fehlende oder inkompatible Java-Laufzeitkomponente ("+detail+").",now,true,true,endpoint,userFormat,
        "MetricsHub modern zuerst","keine",false,"LINKAGE_ERROR",detail,"Legacy winrm4j/CXF 3.x",trustedTarget,trustRequired);
    }
  }

  /**
   * 40k34q: vollständige Remote-Inventarisierung über EIN PowerShell-Sammelskript.
   * Rückgabeform identisch zu LinuxNetworkDiscoveryService.inspectSingle() -
   * ermöglicht dieselbe Übernahmelogik (recordDiscoveryHit()) im Dispatcher.
   *
   * @param ip bekannte IPv4-Adresse des Zielgeräts
   * @param hostnameHint bereits bekannter Name, falls WinRM selbst keinen liefert
   */
  public Optional<DiscoveredDevice> inspectSingle(String ip, String hostnameHint) {
    return inspectSingle(ip,hostnameHint,null);
  }

  public Optional<DiscoveredDevice> inspectSingle(String ip,String hostnameHint,String identityKey) {
    var resolved=resolveAccess(ip,hostnameHint,identityKey);
    var cfg=resolved.settings();
    if(!cfg.enabled() || cfg.username()==null || cfg.username().isBlank()){
      log.debug("Windows-Ferninventarisierung für {} übersprungen: kein passender WinRM-Zugang konfiguriert (Hostname={}, Identity={})",
        ip,hostnameHint,identityKey);
      return Optional.empty();
    }
    String connectHost=resolved.connectionHost();
    log.info("[WINRM-INVENTORY] Zielauflösung Adresse={} Hostname={} Identity={} -> VerbindungHost={} ProfilId={} Profil='{}' Benutzer='{}' Port={} HTTPS={} Quelle={}",
      ip,hostnameHint,identityKey,connectHost,resolved.profileId(),resolved.profileName(),cfg.username(),cfg.port(),cfg.https(),resolved.source());

    // 40k36t114: Einen bereits erfolgreichen Vollinventarisierungsweg zuerst probieren.
    // Scheitert er, Eintrag verwerfen und sofort auf die normale Drei-Wege-Ermittlung zurueckfallen.
    if(isWindowsBackend()){
      String rk=routeKey(connectHost,resolved.profileId(),cfg.username());
      String remembered=preferredInventoryRoute.get(rk);
      if(remembered!=null){
        log.info("[WINRM-T114-ROUTE] PreferredRoute={} Host={} ProfilId={} User='{}' -- zuletzt erfolgreichen Vollinventarweg zuerst probieren.",
          remembered,connectHost,resolved.profileId(),cfg.username());
        String stack=switch(remembered){
          case "METRICSHUB_MODERN" -> "MetricsHub winrm-java 2.1.00 (Standard)";
          case "SSPI_CURRENT_CONTEXT" -> "Windows PowerShell/SSPI (t114 Current Context)";
          case "LEGACY" -> "Legacy winrm4j/CXF 3.x (Fallback)";
          default -> null;
        };
        String auth=switch(remembered){
          case "METRICSHUB_MODERN" -> "MetricsHub remembered";
          case "SSPI_CURRENT_CONTEXT" -> "Windows-native SSPI · Current Context · ohne -Credential";
          case "LEGACY" -> "NTLM (Legacy-winrm4j)";
          default -> "";
        };
        if(stack!=null){
          Optional<DiscoveredDevice> fast=inspectSingle(connectHost,ip,hostnameHint,cfg.username(),cfg.password(),cfg.port(),cfg.https(),
            stack,auth,resolved.source(),resolved.profileName(),resolved.profileId());
          if(fast.isPresent()) return fast;
          preferredInventoryRoute.remove(rk,remembered);
          log.warn("[WINRM-T114-ROUTE] PreferredRoute={} Host={} ist fehlgeschlagen; Cache verworfen, normale Drei-Wege-Ermittlung folgt.",remembered,connectHost);
        }
      }

      // Drei klar getrennte Windows-Pfade in fester Reihenfolge:
      // 1) MetricsHub modern mit gespeichertem Zugang (Standard)
      // 2) Windows-native SSPI im aktuellen Sicherheitskontext OHNE -Credential
      // 3) Legacy winrm4j/CXF als letzte Rueckfallebene
      ModernWinRmAttempt modernPreflight=attemptModernSingle(connectHost,cfg.username(),cfg.password(),
        cfg.port(),cfg.https(),"hostname",false,8000L);
      log.info("[WINRM-T113-ROUTE] Phase=MODERN_PREFLIGHT Host={} User='{}' Success={} Auth={} ErrorType={} Error={}",
        connectHost,cfg.username(),modernPreflight.success(),modernPreflight.authentication(),
        modernPreflight.errorType(),modernPreflight.errorDetail()==null?"":modernPreflight.errorDetail());
      if(modernPreflight.success()){
        log.info("[WINRM-T113-ROUTE] Route=METRICSHUB_MODERN Host={} -- Standardpfad ist erfolgreich und wird fuer das Vollinventar beibehalten.",connectHost);
        return inspectSingle(connectHost,ip,hostnameHint,cfg.username(),cfg.password(),cfg.port(),cfg.https(),
          "MetricsHub winrm-java 2.1.00 (Standard)",modernPreflight.authentication(),
          resolved.source(),resolved.profileName(),resolved.profileId());
      }

      ModernWinRmAttempt currentContextProbe=attemptWindowsNativeT111Auth(connectHost,"","",cfg.port(),cfg.https(),false,15000L);
      log.info("[WINRM-T113-ROUTE] Phase=SSPI_CURRENT_CONTEXT_PREFLIGHT Host={} Credential=false Success={} Exit={} Stdout='{}' StderrChars={} ErrorType={} Error={}",
        connectHost,currentContextProbe.success(),currentContextProbe.exitCode(),
        currentContextProbe.stdout()==null?"":currentContextProbe.stdout().replaceAll("[\r\n]+"," "),
        currentContextProbe.stderr()==null?0:currentContextProbe.stderr().length(),
        currentContextProbe.errorType(),currentContextProbe.errorDetail()==null?"":currentContextProbe.errorDetail());
      if(currentContextProbe.success()){
        log.info("[WINRM-T113-ROUTE] Route=SSPI_CURRENT_CONTEXT Host={} -- Vollinventar wird ebenfalls ohne -Credential in EINEM Invoke-Command ausgefuehrt.",connectHost);
        return inspectSingle(connectHost,ip,hostnameHint,cfg.username(),cfg.password(),cfg.port(),cfg.https(),
          "Windows PowerShell/SSPI (t112 Current Context)","Windows-native SSPI · Current Context · ohne -Credential",
          resolved.source(),resolved.profileName(),resolved.profileId());
      }

      log.warn("[WINRM-T113-ROUTE] Modern und SSPI Current Context fehlgeschlagen; erst jetzt wird Legacy als letzte Rueckfallebene versucht. Host={}",connectHost);
      return inspectSingle(connectHost,ip,hostnameHint,cfg.username(),cfg.password(),cfg.port(),cfg.https(),
        "Legacy winrm4j/CXF 3.x (Fallback)","NTLM (Legacy-winrm4j)",
        resolved.source(),resolved.profileName(),resolved.profileId());
    }

    // Nicht-Windows-Backends behalten die bewiesene Legacy-/Plattform-Preflight-Logik.
    WinRmTestResult preflight=testConnection(connectHost,cfg.enabled(),cfg.username(),cfg.password(),cfg.port(),cfg.https());
    if(!preflight.reachable()){
      log.warn("[WINRM-INVENTORY] Preflight fehlgeschlagen Adresse={} VerbindungHost={} ProfilId={} Benutzer='{}' Stack={} Fehler={}",
        ip,connectHost,resolved.profileId(),cfg.username(),preflight.clientStack(),preflight.errorDetail());
      return Optional.empty();
    }
    log.info("[WINRM-INVENTORY] Preflight erfolgreich Adresse={} VerbindungHost={} ProfilId={} Benutzer='{}' Stack={} Auth={}",
      ip,connectHost,resolved.profileId(),cfg.username(),preflight.clientStack(),preflight.authenticationUsed());
    return inspectSingle(connectHost,ip,hostnameHint,cfg.username(),cfg.password(),cfg.port(),cfg.https(),preflight.clientStack(),preflight.authenticationUsed(),
      resolved.source(),resolved.profileName(),resolved.profileId());
  }

  public Optional<DiscoveredDevice> inspectSingleWithCredentials(String ip,String hostnameHint,String username,String password){
    var cfg=settingsFor(ip);
    if(isWindowsBackend()){
      ModernWinRmAttempt modernPreflight=attemptModernSingle(ip,username,password,cfg.port(),cfg.https(),"hostname",false,8000L);
      if(modernPreflight.success())
        return inspectSingle(ip,ip,hostnameHint,username,password,cfg.port(),cfg.https(),
          "MetricsHub winrm-java 2.1.00 (Standard)",modernPreflight.authentication(),"EXPLICIT_CREDENTIALS","",null);
      ModernWinRmAttempt currentContextProbe=attemptWindowsNativeT111Auth(ip,"","",cfg.port(),cfg.https(),false,15000L);
      if(currentContextProbe.success())
        return inspectSingle(ip,ip,hostnameHint,username,password,cfg.port(),cfg.https(),
          "Windows PowerShell/SSPI (t112 Current Context)","Windows-native SSPI · Current Context · ohne -Credential",
          "EXPLICIT_CREDENTIALS","",null);
      return inspectSingle(ip,ip,hostnameHint,username,password,cfg.port(),cfg.https(),
        "Legacy winrm4j/CXF 3.x (Fallback)","NTLM (Legacy-winrm4j)","EXPLICIT_CREDENTIALS","",null);
    }
    WinRmTestResult preflight=testConnection(ip,true,username,password,cfg.port(),cfg.https());
    if(!preflight.reachable()) return Optional.empty();
    return inspectSingle(ip,ip,hostnameHint,username,password,cfg.port(),cfg.https(),preflight.clientStack(),preflight.authenticationUsed(),
      "EXPLICIT_CREDENTIALS","",null);
  }

  private Optional<DiscoveredDevice> inspectSingle(String connectHost,String identityAddress,String hostnameHint,String username,String password,int port,boolean https,String preferredStack,String preferredAuth,String accessSource,String profileName,Long profileId){
    try {
      String inventoryStdout=null;
      String authLabel=null;
      String successfulRoute=null;
      boolean legacyFallback=false;

      boolean preferLegacyNtlm=preferredStack!=null && preferredStack.contains("Legacy winrm4j")
        && preferredAuth!=null && preferredAuth.toUpperCase(java.util.Locale.ROOT).contains("NTLM");
      boolean preferLegacyNegotiate=preferredStack!=null && preferredStack.contains("Legacy winrm4j")
        && !preferLegacyNtlm;
      boolean sspiPreflight=preferredStack!=null && preferredStack.contains("Windows PowerShell/SSPI");

      // 40k36t99: Den bereits bewiesenen SSPI-Pfad wiederherstellen.
      // Der erfolgreiche Whitespace-Preflight und das Vollinventar verwenden wieder
      // denselben Windows-nativen Invoke-Command/SSPI-Mechanismus. Insbesondere wird
      // KEINE zweite New-PSSession mit abweichender Authentifizierung aufgebaut.
      // Das entspricht dem funktionierenden Referenzpfad aus 40k36t36:
      // $result=Invoke-Command ... ; $result | ForEach-Object { $_ }
      if(sspiPreflight){
        log.info("[WINRM-INVENTORY] t115: SSPI-Vollinventar nutzt CURRENT_CONTEXT ohne -Credential und fordert den tiefen Benutzerumfang best-effort an; Preflight und Inventar laufen im selben EINZIGEN Invoke-Command. Host={}",connectHost);
        ModernWinRmAttempt nativeInventory=attemptWindowsNativeCurrentContextCoupledPreflightAndInventory(
          connectHost,username,password,port,https,INVENTORY_SCRIPT,300000L);
        if(nativeInventory.success() && nativeInventory.stdout()!=null && !nativeInventory.stdout().isBlank()){
          inventoryStdout=nativeInventory.stdout();
          authLabel=nativeInventory.authentication()+" · Preflight + Vollinventar in einem einzigen SSPI-Aufruf";
          legacyFallback=false;
          successfulRoute="SSPI_CURRENT_CONTEXT";
          log.info("[WINRM-INVENTORY] SSPI-Vollinventar erfolgreich Host={} Ergebniszeichen={}",
            connectHost,inventoryStdout.length());
        }else{
          log.warn("[WINRM-INVENTORY] Der t113 SSPI-Current-Context-Vollinventarpfad lieferte keine verwertbaren Daten; Legacy-NTLM bleibt nur letzte Rückfallebene. Host={} Fehler={}",
            connectHost,nativeInventory.errorDetail());
          LegacyScriptResult attempt=attemptLargePowerShellScript(connectHost,username,password,port,https,AUTH_LEGACY_FALLBACK,INVENTORY_SCRIPT,300000L);
          if(!attempt.success()){
            log.warn("[WINRM-INVENTORY] Auch Legacy-NTLM-Chunk-Transfer fehlgeschlagen: Host={} Benutzer='{}' Fehler={}",
              connectHost,username,attempt.errorDetail());
            return Optional.empty();
          }
          inventoryStdout=attempt.stdout();
          authLabel="NTLM · Legacy winrm4j/CXF 3.x · letzter Fallback nach SSPI";
          successfulRoute="LEGACY";
          legacyFallback=true;
        }
      } else if(preferLegacyNtlm || preferLegacyNegotiate){
        String scheme=preferLegacyNtlm?AUTH_LEGACY_FALLBACK:AUTH_PRIMARY;
        LegacyScriptResult attempt=attemptLargePowerShellScript(connectHost,username,password,port,https,scheme,INVENTORY_SCRIPT,300000L);
        if(!attempt.success()){
          log.warn("[WINRM-INVENTORY] Der im Preflight erfolgreiche Legacy-Pfad schlug beim Vollinventar fehl: Host={} Benutzer='{}' Auth={} Fehler={}",
            connectHost,username,scheme,attempt.errorDetail());
          return Optional.empty();
        }
        inventoryStdout=attempt.stdout();
        legacyFallback=preferLegacyNtlm;
        authLabel=preferLegacyNtlm ? "NTLM · Legacy winrm4j/CXF 3.x" : "Negotiate/SPNEGO · Legacy winrm4j/CXF 3.x";
        successfulRoute="LEGACY";
        log.info("[WINRM-INVENTORY] Vollinventar verwendet exakt Preflight-Pfad: Host={} Benutzer='{}' {}",connectHost,username,authLabel);
      } else {
        ModernWinRmAttempt modern=attemptModern(connectHost,username,password,port,https,INVENTORY_SCRIPT,true,300000L);
        if(modern.success() && modern.stdout()!=null && !modern.stdout().isBlank()){
          inventoryStdout=modern.stdout();
          authLabel=modern.authentication().contains("Windows-native")
            ? modern.authentication()
            : modern.authentication()+" · MetricsHub 2.1.00";
          legacyFallback=!https;
          successfulRoute="METRICSHUB_MODERN";
        } else {
          log.warn("[WINRM-T113-ROUTE] Route=METRICSHUB_MODERN fehlgeschlagen oder ohne Nutzlast. Host={} Fehler={}",connectHost,modern.errorDetail());

          // 40k36t113: Nach einem echten Fehler des modernen Standardpfads darf nicht
          // direkt Legacy folgen. Zuerst wird der in t111 nachweislich funktionierende
          // Windows-Sicherheitskontext ohne explizites -Credential genutzt.
          ModernWinRmAttempt sspiFallback=attemptWindowsNativeCurrentContextCoupledPreflightAndInventory(
            connectHost,username,password,port,https,INVENTORY_SCRIPT,300000L);
          if(sspiFallback.success() && sspiFallback.stdout()!=null && !sspiFallback.stdout().isBlank()){
            inventoryStdout=sspiFallback.stdout();
            authLabel=sspiFallback.authentication()+" · t113 Fallback nach MetricsHub";
            legacyFallback=false;
            successfulRoute="SSPI_CURRENT_CONTEXT";
            log.info("[WINRM-T113-ROUTE] Route=SSPI_CURRENT_CONTEXT Host={} Success=true Ergebniszeichen={}",
              connectHost,inventoryStdout.length());
          }else{
            log.warn("[WINRM-T113-ROUTE] Route=SSPI_CURRENT_CONTEXT Host={} Success=false Fehler={}",
              connectHost,sspiFallback.errorDetail());

            boolean directLegacyNtlm=!https && isIpLiteral(connectHost);
            LegacyScriptResult attempt=directLegacyNtlm
              ? new LegacyScriptResult(false,"","SKIPPED","Legacy Negotiate/SPNEGO für HTTP/IP bewusst übersprungen.")
              : attemptLargePowerShellScript(connectHost,username,password,port,https,AUTH_PRIMARY,INVENTORY_SCRIPT,300000L);
            if(!attempt.success()){
              attempt=attemptLargePowerShellScript(connectHost,username,password,port,https,AUTH_LEGACY_FALLBACK,INVENTORY_SCRIPT,300000L);
              legacyFallback=true;
            }
            if(!attempt.success()){
              log.info("[WINDOWS-REMOTE] Inventarisierung für {} fehlgeschlagen: modern={} sspi={} legacy={}",
                connectHost,modern.errorDetail(),sspiFallback.errorDetail(),attempt.errorDetail());
              return Optional.empty();
            }
            inventoryStdout=attempt.stdout();
            authLabel=legacyFallback ? "NTLM · Legacy winrm4j/CXF 3.x" : "Negotiate/SPNEGO · Legacy winrm4j/CXF 3.x";
            successfulRoute="LEGACY";
            log.info("[WINRM-T113-ROUTE] Route=LEGACY Host={} Success=true Auth={}",connectHost,authLabel);
          }
        }
      }
      persistRawInventory(identityAddress,inventoryStdout);
      Map<String,String> data = parse(inventoryStdout);
      if(data.isEmpty() || !data.containsKey("COMPUTER_NAME")){
        String raw=inventoryStdout==null?"":inventoryStdout;
        String visible=raw.length()>4000?raw.substring(0,4000)+"…":raw;
        log.warn("[WINRM-INVENTORY] Keine verwertbaren GAMKV-Grunddaten von Host={}. Remote-Ausgabe ({} Zeichen): {}",
          connectHost,raw.length(),visible.replaceAll("[\\r\\n]+"," | "));
        return Optional.empty();
      }
      String softwareDetails=data.get("SOFTWARE_DETAILS");
      if(softwareDetails!=null && !softwareDetails.isBlank()) latestSoftwareDetails.put(identityAddress,softwareDetails);
      int softwareCount=parseInt(data.get("SOFTWARE_COUNT"));
      log.info("[WINRM-INVENTORY] Ergebnis Host={} Zeichen={} Grunddaten={} Software={} Registry={} AppX={}/{}/{} Benutzerumfang={} DiensteLesbar={} Laufwerke={} Netzwerkadapter={}",
        connectHost,inventoryStdout.length(),data.containsKey("COMPUTER_NAME"),softwareCount,
        data.get("SOFTWARE_REGISTRY_COUNT"),data.get("APPX_USER_COUNT"),data.get("APPX_OTHER_USERS_COUNT"),data.get("APPX_SYSTEM_COUNT"),
        data.get("SOFTWARE_USER_SCOPE"),data.get("PERMISSION_SERVICES"),data.containsKey("VOLUMES"),data.containsKey("ADAPTERS"));
      String computerName = data.getOrDefault("COMPUTER_NAME", hostnameHint != null ? hostnameHint : identityAddress);
      String displayManufacturer = joinRemote(" ",data.get("SYSTEM_MANUFACTURER"),data.get("SYSTEM_MODEL"));
      String protocol = "Windows-Inventarisierung / modulare PowerShell-Abfragen"
        + " · WinRM / PowerShell-Sammelabfrage"
        + detail("Edition", data.get("OS_CAPTION"))
        + detail("Version", data.get("OS_VERSION"))
        + detail("Build", data.get("OS_BUILD"))
        + detail("Architektur", data.get("OS_ARCH"))
        + detail("Installation", data.get("OS_INSTALL_DATE"))
        + detail("Letzter Neustart", data.get("OS_LAST_BOOT"))
        + detail("Systemtyp", data.get("SYSTEM_TYPE"))
        + detail("Domäne/Arbeitsgruppe", data.get("DOMAIN"))
        + detail("Benutzer", data.get("USER_NAME"))
        + detail("BIOS", joinRemote(" / ",data.get("BIOS_VERSION"),data.get("BIOS_DATE")))
        + detail("Mainboard", joinRemote(" ",data.get("BOARD_MANUFACTURER"),data.get("BOARD_PRODUCT")))
        + detail("Prozessor", data.get("CPU_NAME"))
        + detail("CPU", joinRemote(" / ",
            suffixRemote(data.get("CPU_CORES")," Kerne"),
            suffixRemote(data.get("CPU_THREADS")," Threads")))
        + detail("Arbeitsspeicher", data.get("RAM"))
        + detail("RAM-Typ", joinRemote(" / ",data.get("RAM_TYPE"),data.get("RAM_SPEED")))
        + detail("Maximal unterstützter RAM", data.get("RAM_MAX"))
        + detail("RAM-Steckplätze", slotSummaryRemote(data.get("RAM_SLOTS_USED"),data.get("RAM_SLOTS_TOTAL"),data.get("RAM_SLOTS_FREE")))
        + detail("RAM-Fehlerkorrektur", data.get("RAM_ECC"))
        + detail("RAM-Module", data.get("RAM_MODULES"))
        + detail("RAM-Erweiterbarkeit", data.get("RAM_ASSESSMENT"))
        + detail("Datenträger", data.get("DISKS"))
        + detail("Lokale Laufwerke", data.get("VOLUMES"))
        + detail("Netzlaufwerke", data.get("NETWORK_VOLUMES"))
        + detail("Optische Laufwerke", data.get("OPTICAL_VOLUMES"))
        + detail("Wechseldatenträger", data.get("REMOVABLE_VOLUMES"))
        + detail("Physische Datenträger", data.get("PHYSICAL_DISKS"))
        + detail("Storage Pools", data.get("STORAGE_POOLS"))
        + detail("Virtuelle Datenträger", data.get("VIRTUAL_DISKS"))
        + detail("IPv6", data.get("IPV6"))
        + detail("Netzwerkadapter", data.get("ADAPTERS"))
        + detail("Installierte Software", data.get("SOFTWARE_COUNT") == null ? data.get("SOFTWARE") :
            data.get("SOFTWARE_COUNT")+" Programme"+
            (data.get("SOFTWARE_REGISTRY_COUNT")==null||data.get("SOFTWARE_REGISTRY_COUNT").equals(data.get("SOFTWARE_COUNT"))?"":" ("+data.get("SOFTWARE_REGISTRY_COUNT")+" Registry-Einträge)")+
            (data.get("SOFTWARE")==null?"":" – "+data.get("SOFTWARE")))
        + detail("Softwarequellen", data.get("SOFTWARE_SOURCE_COUNTS"))
        + detail("Benutzer-Software-Erfassung", data.get("SOFTWARE_USER_SCOPE"))
        + detail("Zusätzlich geladene Benutzerprofile", data.get("SOFTWARE_PROFILE_HIVES"))
        + detail("Software-Basisstatus", data.get("SOFTWARE_BASELINE_STATUS"))
        + detail("Windows Store-/AppX-Apps",
            (data.get("APPX_USER_COUNT")==null&&data.get("APPX_OTHER_USERS_COUNT")==null&&data.get("APPX_SYSTEM_COUNT")==null)?null:
            (data.get("APPX_USER_COUNT")==null?"0":data.get("APPX_USER_COUNT"))+" aktueller Benutzer · "
            +(data.get("APPX_OTHER_USERS_COUNT")==null?"0":data.get("APPX_OTHER_USERS_COUNT"))+" weitere Benutzer · "
            +(data.get("APPX_SYSTEM_COUNT")==null?"0":data.get("APPX_SYSTEM_COUNT"))+" System-/Provisioned-Apps")
        + detail("AppX-Benutzerumfang", data.get("APPX_USER_SCOPE"))
        + detail("AppX AllUsers lesbar", data.get("APPX_ALLUSERS_READABLE"))
        + detail("Provisioned AppX lesbar", data.get("APPX_PROVISIONED_READABLE"))
        + detail("AppX/MSIX-Diagnose", data.get("APPX_DIAGNOSTIC"))
        + detail("Windows-Rollen/Features", data.get("FEATURES"))
        + detail("Antivirus", data.get("ANTIVIRUS"))
        + detail("Microsoft Defender", data.get("DEFENDER"))
        + detail("Firewall", data.get("FIREWALL"))
        + detail("TPM", data.get("TPM"))
        + detail("Secure Boot", data.get("SECURE_BOOT"))
        + detail("BitLocker-Zusammenfassung", data.get("BITLOCKER_SUMMARY"))
        + detail("BitLocker-Volumes", data.get("BITLOCKER_VOLUMES"))
        + detail("Systemlaufwerk verschlüsselt", data.get("SYSTEM_VOLUME_ENCRYPTED"))
        + detail("BitLocker-Diagnose", data.get("BITLOCKER_DIAGNOSTIC"))
        + detail("Ausstehende Updates", suffixRemote(data.get("PENDING_UPDATES")," Updates"))
        + detail("Letzter Hotfix", data.get("LAST_HOTFIX"))
        + detail("Neustart ausstehend", data.get("REBOOT_PENDING"))
        + detail("UAC", data.get("UAC"))
        + detail("Remote Desktop", data.get("RDP"))
        + detail("Lokale Administratoren", data.get("LOCAL_ADMINS"))
        + detail("Berechtigungsbenutzer", data.get("PERMISSION_USER"))
        + detail("Administratorrechte", boolLabel(data.get("PERMISSION_ADMIN")))
        + detail("SYSTEM-Rechte", boolLabel(data.get("PERMISSION_SYSTEM")))
        + detail("Integritätsstufe", data.get("PERMISSION_INTEGRITY"))
        + detail("CIM lesbar", boolLabel(data.get("PERMISSION_CIM")))
        + detail("BitLocker lesbar", boolLabel(data.get("PERMISSION_BITLOCKER")))
        + detail("Dienste lesbar", boolLabel(data.get("PERMISSION_SERVICES")))
        + detail("Inventarisierungstiefe", data.get("INVENTORY_LEVEL"))
        + detail("Berechtigungseinschränkungen", data.get("PERMISSION_LIMITATIONS"))
        + detail("WinRM-Authentifizierung", authLabel)
        + detail("WinRM-Zielhost", connectHost)
        + detail("WinRM-Zugangsquelle", accessSource)
        + detail("WinRM-Profil", profileName==null||profileName.isBlank()?null:profileName+(profileId==null?"":" (#"+profileId+")"))
        + detail("Inventarisierungsweg", "WinRM")
        + detail("Inventarisierungsstatus", "erfolgreich")
        + detail("Letzter Inventarisierungsversuch", OffsetDateTime.now().toString());
      String mac = blankToNull(data.get("MAC"));
      String serial = blankToNull(data.get("BIOS_SERIAL"));
      if(successfulRoute!=null && isWindowsBackend()){
        String rk=routeKey(connectHost,profileId,username);
        preferredInventoryRoute.put(rk,successfulRoute);
        log.info("[WINRM-T114-ROUTE] RememberedRoute={} Host={} ProfilId={} User='{}' -- Vollinventarisierung erfolgreich.",
          successfulRoute,connectHost,profileId,username);
      }
      return Optional.of(new DiscoveredDevice(
        "windows-remote:" + identityAddress, computerName, "Windows-PC", identityAddress, mac, protocol, "ONLINE",
        blankToNull(displayManufacturer), serial, OffsetDateTime.now().toString(), false));
    } catch (Exception e) {
      log.info("[WINDOWS-REMOTE] Inventarisierung für {} (VerbindungHost={}) fehlgeschlagen: {}", identityAddress, connectHost, errorSummary(e));
      return Optional.empty();
    }
  }

  private static Map<String,String> parse(String stdout) {
    Map<String,String> result = new LinkedHashMap<>();
    if(stdout==null) return result;
    String combined=stdout.replace("\uFEFF","").replace("#< CLIXML"," ");
    java.util.regex.Matcher marker=java.util.regex.Pattern
      .compile("GAMKV\\|([A-Z0-9_]+)\\|([A-Za-z0-9+/=]+)(?=\\s+GAMKV\\||\\s*<|\\s*$)")
      .matcher(combined);
    while(marker.find()){
      try{
        String value=new String(Base64.getDecoder().decode(marker.group(2)),StandardCharsets.UTF_8).trim();
        if(!value.isBlank()) result.put(marker.group(1),value);
      }catch(IllegalArgumentException ignored){}
    }
    if(!result.isEmpty()) return result;
    for(String line:stdout.split("\\r?\\n")){
      int eq=line.indexOf('=');
      if(eq<=0) continue;
      String key=line.substring(0,eq).trim(), value=line.substring(eq+1).trim();
      if(!value.isEmpty()) result.put(key,value);
    }
    return result;
  }

  private static String joinRemote(String sep,String... values){
    java.util.List<String> out=new java.util.ArrayList<>();
    for(String v:values) if(v!=null&&!v.isBlank()) out.add(v.trim());
    return out.isEmpty()?null:String.join(sep,out);
  }

  private static String suffixRemote(String value,String suffix){
    return value==null||value.isBlank()?null:value.trim()+suffix;
  }

  private static String slotSummaryRemote(String used,String total,String free){
    if(used==null&&total==null&&free==null) return null;
    if(used!=null&&total!=null) return used+" von "+total+" belegt"+(free==null?"":" ("+free+" frei)");
    return joinRemote(" / ",used,total,free);
  }

  private static String detail(String label, String value) {
    return (value == null || value.isBlank()) ? "" : " · " + label + ": " + value;
  }

  private static String boolLabel(String raw) { return raw == null ? null : ("true".equalsIgnoreCase(raw.trim()) ? "Ja" : "false".equalsIgnoreCase(raw.trim()) ? "Nein" : raw); }

  private static String plausibleGb(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try { return Integer.parseInt(raw.trim()) > 0 ? raw.trim() + " GB" : null; } catch (Exception e) { return null; }
  }

  private void persistRawInventory(String host,String raw){
    try{
      Path dir=Path.of(System.getProperty("user.home","."),".gam","inventory-raw","windows");
      Files.createDirectories(dir);
      String safe=(host==null?"unknown":host).replaceAll("[^A-Za-z0-9._-]","_");
      Path target=dir.resolve(safe+".gamkv.txt");
      Files.writeString(target,raw==null?"":raw,StandardCharsets.UTF_8);
      log.info("[WINRM-INVENTORY] Rohinventar dauerhaft gespeichert: {}",target);
    }catch(Exception e){
      log.warn("[WINRM-INVENTORY] Rohinventar konnte nicht gespeichert werden: {}",e.getMessage());
    }
  }

  private static int parseInt(String value){
    try{return value==null?0:Integer.parseInt(value.trim());}catch(Exception e){return 0;}
  }

  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

  private static String trim(String value) {
    if (value == null) return "";
    return value.length() > 300 ? value.substring(0, 300) + "…" : value;
  }
}
