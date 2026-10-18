package de.kopfzentrum.gam.system;
import java.io.*; import java.nio.charset.StandardCharsets; import java.util.*; import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/setup/macos-tools")
public class MacOsToolSetupController {
 private static final List<String> ALLOWED=List.of("homebrew","mariadb","smartmontools","mas");
 @GetMapping("/status") public Map<String,Object> status(){
  Map<String,Object> r=new LinkedHashMap<>(); boolean mac=System.getProperty("os.name","").toLowerCase().contains("mac");
  r.put("macOs",mac); r.put("homebrew",tool("brew","--version")); r.put("mariadb",tool("mariadb","--version"));
  r.put("smartmontools",tool("smartctl","--version")); r.put("mas",tool("mas","version"));
  r.put("mariadbService",mac?shell("brew services list 2>/dev/null | awk '$1==\"mariadb\"{print $2;exit}'"):""); return r;
 }
 @PostMapping("/install") public Map<String,Object> install(@RequestBody Map<String,String> body)throws Exception{
  if(!System.getProperty("os.name","").toLowerCase().contains("mac")) throw new IllegalStateException("Nur unter macOS verfügbar.");
  String tool=body.getOrDefault("tool","").trim().toLowerCase(); if(!ALLOWED.contains(tool))throw new IllegalArgumentException("Unbekanntes Werkzeug.");
  String cmd;
  if("homebrew".equals(tool)) cmd="/bin/bash -c '$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)'";
  else {cmd="BREW=$(command -v brew || { [ -x /opt/homebrew/bin/brew ] && echo /opt/homebrew/bin/brew; } || { [ -x /usr/local/bin/brew ] && echo /usr/local/bin/brew; }); if [ -z \"$BREW\" ]; then echo 'Homebrew fehlt.'; exit 2; fi; \"$BREW\" install "+tool;
   if("mariadb".equals(tool))cmd+=" && \"$BREW\" services start mariadb"; }
  String esc=cmd.replace("\\","\\\\").replace("\"","\\\"");
  new ProcessBuilder("osascript","-e","tell application \"Terminal\" to do script \""+esc+"\"","-e","tell application \"Terminal\" to activate").start();
  return Map.of("started",true,"tool",tool,"message","Installation im Terminal gestartet.");
 }
 private Map<String,Object> tool(String c,String arg){Map<String,Object>r=new LinkedHashMap<>();String path=shell("command -v "+c+" 2>/dev/null || { [ -x /opt/homebrew/bin/"+c+" ] && echo /opt/homebrew/bin/"+c+"; } || { [ -x /usr/local/bin/"+c+" ] && echo /usr/local/bin/"+c+"; }");r.put("installed",!path.isBlank());r.put("path",path);r.put("version",path.isBlank()?"":shell("'"+path.replace("'","'\\''")+"' "+arg+" 2>/dev/null | head -n 1"));return r;}
 private String shell(String c){try{Process p=new ProcessBuilder("/bin/sh","-lc",c).redirectErrorStream(true).start();if(!p.waitFor(5,TimeUnit.SECONDS)){p.destroyForcibly();return "";}try(BufferedReader b=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){String l=b.readLine();return l==null?"":l.trim();}}catch(Exception e){return "";}}
}
