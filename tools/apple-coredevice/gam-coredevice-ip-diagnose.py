import asyncio, json, re, sys, socket, ipaddress, subprocess

IPV4 = re.compile(r"^(?:\d{1,3}\.){3}\d{1,3}$")
IPV6 = re.compile(r"^[0-9a-fA-F:]+(?:%[0-9A-Za-z_.-]+)?$")

def scalar(v):
    return isinstance(v, (str, int, float, bool)) or v is None

def interesting_key(k):
    s=str(k).lower()
    return any(x in s for x in ("address","ip","host","interface","network","wifi","ethernet","bonjour","service"))

def collect(obj, path="", out=None, depth=0):
    if out is None: out=[]
    if depth>8 or len(out)>=300: return out
    if isinstance(obj, dict):
        for k,v in obj.items():
            p=f"{path}.{k}" if path else str(k)
            if scalar(v):
                sv="" if v is None else str(v)
                if interesting_key(k) or IPV4.match(sv) or (":" in sv and IPV6.match(sv)):
                    out.append({"path":p,"value":sv})
            else: collect(v,p,out,depth+1)
    elif isinstance(obj,(list,tuple)):
        for i,v in enumerate(obj[:100]): collect(v,f"{path}[{i}]",out,depth+1)
    return out

def safe_attr(obj, name):
    try:
        v=getattr(obj,name)
        if scalar(v): return v
        if isinstance(v,(tuple,list)) and all(scalar(x) for x in v): return list(v)
        return str(v)
    except Exception as e:
        return f"<error:{type(e).__name__}>"

def windows_bound_lan_peers(physical_peers):
    """40k37e53-fix7: Folge ausschliesslich der bereits aufgebauten localhost-Relay-Kette.
    Keine Netzwerkerkennung: Ausgangspunkt sind nur localhost-Ports, die UserspaceRsdTunnel
    selbst benutzt hat. Von deren Owner-PIDs werden bestehende TCP-Verbindungen rekursiv
    verfolgt, bis eine private IPv4-Gegenstelle des Apple-Transports sichtbar wird."""
    if not sys.platform.startswith("win"): return []
    cp=subprocess.run(["netstat","-ano","-p","tcp"],capture_output=True,text=True,timeout=10,encoding="utf-8",errors="replace")
    rows=[]
    for line in cp.stdout.splitlines():
        parts=line.split()
        if len(parts)<5 or parts[0].upper()!="TCP" or parts[3].upper()!="ESTABLISHED": continue
        rows.append({"local":parts[1],"remote":parts[2],"pid":parts[4]})
    def split_ep(ep):
        if ep.startswith("[") and "]" in ep:
            j=ep.index("]"); return ep[1:j], ep[j+2:] if len(ep)>j+2 else ""
        if ":" in ep: return ep.rsplit(":",1)
        return ep,""
    def is_loop(host): return host in ("127.0.0.1","::1","0:0:0:0:0:0:0:1")
    seed_ports={str(p.get("port")) for p in physical_peers if is_loop(str(p.get("host"))) and str(p.get("port","")).isdigit()}
    # PID des ersten lokalen Relays aus genau den vom Apple-Prozess beobachteten Ports.
    frontier=set()
    for row in rows:
        lh,lp=split_ep(row["local"]); rh,rp=split_ep(row["remote"])
        if (is_loop(lh) and lp in seed_ports) or (is_loop(rh) and rp in seed_ports): frontier.add(row["pid"])
    visited=set(); out=[]
    # Maximal sechs lokale Relay-Stufen; niemals fremde PIDs ohne localhost-Verbindung
    # zu einer bereits bewiesenen Apple-Relay-Stufe aufnehmen.
    for depth in range(6):
        current=frontier-visited
        if not current: break
        visited.update(current); next_pids=set()
        local_ports=set()
        for row in rows:
            if row["pid"] not in current: continue
            lh,lp=split_ep(row["local"]); rh,rp=split_ep(row["remote"])
            try:
                ip=ipaddress.ip_address(rh)
                if isinstance(ip,ipaddress.IPv4Address) and ip.is_private and not ip.is_loopback:
                    item={"pid":row["pid"],"host":rh,"local":row["local"],"remote":row["remote"],"depth":depth}
                    if item not in out: out.append(item)
            except Exception: pass
            if is_loop(lh): local_ports.add(lp)
            if is_loop(rh): local_ports.add(rp)
        # Nur ueber eine konkrete localhost-TCP-Kante zur naechsten Owner-PID gehen.
        for row in rows:
            if row["pid"] in visited: continue
            lh,lp=split_ep(row["local"]); rh,rp=split_ep(row["remote"])
            if (is_loop(lh) and lp in local_ports) or (is_loop(rh) and rp in local_ports): next_pids.add(row["pid"])
        frontier=next_pids
    return out

async def main():
    udid=sys.argv[1] if len(sys.argv)>1 else None
    result={"schema":"gam-coredevice-ip-diagnose-v3","requested_udid":udid}
    # 40k37e33-fix8: Die physische Gegenstelle muss VOR dem virtuellen Userspace-RSD-
    # Endpunkt sichtbar sein. Deshalb beobachten wir ausschliesslich in diesem isolierten
    # Diagnoseprozess die Socket-Verbindungen, die pymobiledevice3 beim Aufbau des Apple-
    # Tunnels selbst erzeugt. Es werden keine ARP-/Neighbor-/Routerdaten abgefragt.
    physical_peers=[]
    original_connect=socket.socket.connect
    original_connect_ex=socket.socket.connect_ex
    def record_peer(api, address):
        try:
            host=address[0] if isinstance(address,(tuple,list)) and address else None
            port=address[1] if isinstance(address,(tuple,list)) and len(address)>1 else None
            if host is not None: physical_peers.append({"api":api,"host":str(host),"port":port})
        except Exception: pass
    def traced_connect(sock, address):
        record_peer("socket.connect", address)
        return original_connect(sock,address)
    def traced_connect_ex(sock, address):
        record_peer("socket.connect_ex", address)
        return original_connect_ex(sock,address)
    socket.socket.connect=traced_connect
    socket.socket.connect_ex=traced_connect_ex
    try:
        from pymobiledevice3.remote.userspace_tunnel import UserspaceRsdTunnel
        async with UserspaceRsdTunnel(serial=udid) as rsd:
            result["rsd"]={k:safe_attr(rsd,k) for k in ("udid","product_version","service")}
            try:
                svc=getattr(rsd,"service",None)
                if svc is not None:
                    result["rsd_service"]={k:safe_attr(svc,k) for k in ("address","hostname","host","port")}
            except Exception: pass
            try:
                peer=getattr(rsd,"peer_info",{}) or {}
                result["peer_candidates"]=collect(peer)
            except Exception as e:
                result["peer_candidates_error"]=repr(e)
            # Inspect only public-ish scalar endpoint metadata; never dump secrets/certificates.
            endpoint=[]
            for owner_name,owner in (("rsd",rsd),("service",getattr(rsd,"service",None))):
                if owner is None: continue
                try: names=dir(owner)
                except Exception: continue
                for name in names:
                    if name.startswith("_") or not interesting_key(name): continue
                    try: v=getattr(owner,name)
                    except Exception: continue
                    if callable(v): continue
                    if scalar(v) or (isinstance(v,(tuple,list)) and all(scalar(x) for x in v)):
                        endpoint.append({"owner":owner_name,"name":name,"value":v})
            result["endpoint_metadata"]=endpoint[:100]
            result["ok"]=True
            # Nach erfolgreichem Tunnelaufbau auch den echten Peer des Service-Sockets
            # abfragen. Virtuelle fdxx::/localhost-Adressen bleiben sichtbar, werden aber
            # von Java niemals als physische WLAN-IP uebernommen.
            try:
                svc=getattr(rsd,"service",None)
                for owner_name,owner in (("service",svc),("rsd",rsd)):
                    if owner is None: continue
                    for attr in ("sock","socket","_sock","_socket"):
                        try:
                            so=getattr(owner,attr)
                            if hasattr(so,"getpeername"):
                                peer=so.getpeername()
                                physical_peers.append({"api":owner_name+"."+attr+".getpeername","host":str(peer[0]),"port":peer[1] if len(peer)>1 else None})
                        except Exception: pass
            except Exception: pass
            # Relay muss noch leben: netstat jetzt innerhalb des aktiven UserspaceRsdTunnel-Kontexts auswerten.
            try:
                result["bound_lan_peers"]=windows_bound_lan_peers(physical_peers)
            except Exception as e:
                result["relay_netstat_error"]=f"{type(e).__name__}: {e}"
    except Exception as e:
        result["ok"]=False
        result["error"]=f"{type(e).__name__}: {e}"
    socket.socket.connect=original_connect
    socket.socket.connect_ex=original_connect_ex
    # Deduplizieren, Reihenfolge erhalten. Nur die von diesem Apple-CoreDevice-Prozess
    # tatsaechlich verwendeten Verbindungsziele werden ausgegeben.
    unique=[]; seen_peers=set()
    for peer in physical_peers:
        key=(peer.get("api"),peer.get("host"),peer.get("port"))
        if key in seen_peers: continue
        seen_peers.add(key); unique.append(peer)
    result["physical_connection_candidates"]=unique

    # 40k37e53-fix3: Unter Windows verbindet sich UserspaceRsdTunnel im Python-Prozess
    # nur mit einem lokalen Relay (127.0.0.1). Die eigentliche WLAN-Verbindung zum iPad
    # gehoert dem Relay-Prozess. Binde deshalb die bereits beobachteten localhost-Ports
    # ueber netstat an den besitzenden PID und suche AUSSCHLIESSLICH bei demselben PID
    # nach einer ESTABLISHED-Verbindung zu einer privaten IPv4. So entsteht die fehlende
    # Bruecke UDID -> Userspace-RSD -> Relay-PID -> physische LAN-IP, ohne ARP/FRITZ!Box.
    result.setdefault("bound_lan_peers", [])
    print(json.dumps(result, ensure_ascii=False, default=str))
    # 40k37e33-fix8: Kandidaten einzeln ausgeben, damit Java/Logback keine lange JSON-Zeile
    # abschneiden kann. Nur bereits gefilterte, nicht-sensitive Netzwerkmetadaten.
    seen=set()
    for c in result.get("peer_candidates", []):
        path=str(c.get("path", "")); value=str(c.get("value", ""))
        key=(path,value)
        if key in seen: continue
        seen.add(key)
        print("GAM_NET_CANDIDATE\tPEER\t"+path.replace("\t"," ")+"\t"+value.replace("\t"," ").replace("\r"," ").replace("\n"," "))
    for c in result.get("physical_connection_candidates", []):
        host=str(c.get("host", "")); port=str(c.get("port", "")); api=str(c.get("api", ""))
        print("GAM_PHYSICAL_PEER\t"+api.replace("\t"," ")+"\t"+host.replace("\t"," ")+"\t"+port.replace("\t"," "))
    for c in result.get("bound_lan_peers", []):
        print("GAM_BOUND_LAN_PEER\t"+str(c.get("pid", ""))+"\t"+str(c.get("host", ""))+"\t"+str(c.get("remote", "")))
    for c in result.get("endpoint_metadata", []):
        path=str(c.get("owner", ""))+"."+str(c.get("name", "")); value=str(c.get("value", ""))
        key=(path,value)
        if key in seen: continue
        seen.add(key)
        print("GAM_NET_CANDIDATE\tENDPOINT\t"+path.replace("\t"," ")+"\t"+value.replace("\t"," ").replace("\r"," ").replace("\n"," "))

asyncio.run(main())
