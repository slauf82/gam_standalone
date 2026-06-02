import React, {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {Download, FilePlus2, FileText, LogOut, Search, ShieldCheck, UserRound, UsersRound, LayoutDashboard, Package, Warehouse, CheckSquare, ClipboardCheck, BriefcaseBusiness, Landmark, FileBarChart, ClipboardList, KeyRound, QrCode, Smartphone} from 'lucide-react';
import {QRCodeSVG} from 'qrcode.react';
import {AccountAdminDto, AccountDto, InventoryDevice, InventoryDeviceDetail, InventoryStats, WarehouseItem, WarehouseStats, InvoiceCompany, InvoiceCreateLineRequest, InvoiceDetail, InvoiceSummary, LbdRecipient, ProductDto, RoleDto, SystemStatus, calculateInvoice, createInvoice, deleteInvoiceDraft, loadAccounts, loadCompanies, loadDraft, loadExportCheck, loadGamApprovals, loadGamCashbook, loadGamCompliance, loadGamModules, loadGamPersonnel, loadGamReportSummary, loadGamTasks, loadInventoryDevice, loadInventoryDevices, loadInventoryStats, loadWarehouseItems, loadWarehouseStats, updateWarehouseStock, loadInvoice, loadInvoices, loadLbdPreview, loadMenu, loadNextInvoiceNumber, loadProducts, loadRoles, loadSystemStatus, login, logout, me, pdfUrl, token, updateAccount, updateInvoice, updateInvoiceStatus, createCancellationInvoice, createCreditNote, createProformaInvoice, zugferdXmlUrl, loadDeviceMaterialLinks, loadMaterialMovements, bookMaterial, setupTotp, confirmTotp, loadPasskeyStatus, passkeyRegisterOptions, passkeyRegisterFinish, passkeyLoginOptions, passkeyLoginFinish} from './api/client';
import './style.css';


function b64urlToBuffer(value:string){
  const base64 = value.replace(/-/g,'+').replace(/_/g,'/') + '='.repeat((4 - value.length % 4) % 4);
  const raw = atob(base64);
  const out = new Uint8Array(raw.length);
  for(let i=0;i<raw.length;i++) out[i]=raw.charCodeAt(i);
  return out.buffer;
}
function bufferToB64url(buffer:ArrayBuffer){
  const bytes = new Uint8Array(buffer);
  let binary='';
  bytes.forEach(b=>binary+=String.fromCharCode(b));
  return btoa(binary).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
}
function usernameToUserId(username:string){
  return new TextEncoder().encode(username || 'gam-user');
}

type Page = 'dashboard'|'invoices'|'inventory'|'warehouse'|'users'|'tasks'|'approvals'|'personnel'|'cashbook'|'compliance'|'reports';

function Login({onLogin}:{onLogin:()=>void}) {
  const [tab,setTab]=useState<'password'|'totp-register'|'totp-login'|'passkey-register'|'passkey-login'>('password');
  const [username,setUsername]=useState('');
  const [password,setPassword]=useState('');
  const [totp,setTotp]=useState('');
  const [err,setErr]=useState('');
  const [info,setInfo]=useState('');
  const [retryAfter,setRetryAfter]=useState(0);
  const [setup,setSetup]=useState<{secret:string; otpauthUri:string; alreadyConfigured:boolean}|null>(null);

  useEffect(()=>{
    if (retryAfter <= 0) return;
    const timer = window.setInterval(()=>setRetryAfter(v=>Math.max(0, v-1)), 1000);
    return ()=>window.clearInterval(timer);
  },[retryAfter]);

  function handleLoginError(ex:any, fallback:string) {
    if (ex?.retryAfterSeconds) {
      setRetryAfter(Number(ex.retryAfterSeconds));
      setErr(`Zu viele Fehlversuche. Neuer Versuch in ${Number(ex.retryAfterSeconds)} Sekunden.`);
      return;
    }
    setErr(ex?.message ?? fallback);
  }

  async function passwordLogin(e:React.FormEvent){
    e.preventDefault();
    if (retryAfter > 0) return;
    setErr('');
    try{await login(username,password,''); onLogin();}
    catch(ex:any){handleLoginError(ex,'Login fehlgeschlagen');}
  }

  async function totpLogin(e:React.FormEvent){
    e.preventDefault();
    if (retryAfter > 0) return;
    setErr('');
    try{await login(username,'',totp); onLogin();}
    catch(ex:any){handleLoginError(ex,'2FA-Login fehlgeschlagen');}
  }

  async function startTotpSetup(e:React.FormEvent){
    e.preventDefault();
    setErr(''); setInfo(''); setSetup(null);
    try{
      const r=await setupTotp(username);
      setSetup({secret:r.secret, otpauthUri:r.otpauthUri, alreadyConfigured:r.alreadyConfigured});
      setInfo(r.alreadyConfigured?'Es existiert bereits ein 2FA-Secret. Mit Bestätigung wird es ersetzt.':'QR-Code erzeugt. Bitte mit Authenticator scannen und den Code bestätigen.');
    }catch(ex:any){setErr(ex.message??'2FA-Registrierung konnte nicht gestartet werden');}
  }

  async function confirmTotpSetup(){
    setErr(''); setInfo('');
    if(!setup){setErr('Bitte zuerst QR-Code erzeugen.'); return;}
    try{
      await confirmTotp(username,setup.secret,totp);
      setInfo('2FA wurde in accounts.secretkey gespeichert. Du kannst nun den 2FA-Login verwenden.');
      setTab('totp-login');
      setTotp('');
    }catch(ex:any){setErr(ex.message??'2FA-Code konnte nicht bestätigt werden');}
  }

  async function passkeyInfo(){
    setErr('');
    try{const r=await loadPasskeyStatus(); setInfo(String(r.note ?? 'Passkey/WebAuthn ist vorbereitet.'));}
    catch(ex:any){setErr(ex.message??'Passkey-Status konnte nicht geladen werden');}
  }

  async function registerPasskey(){
    setErr(''); setInfo('');
    if(!username.trim()){setErr('Bitte zuerst Benutzernamen eingeben.'); return;}
    if(!window.PublicKeyCredential){setErr('Dieser Browser unterstützt keine Passkeys/WebAuthn.'); return;}
    try{
      const opts = await passkeyRegisterOptions(username.trim());
      const credential = await navigator.credentials.create({
        publicKey: {
          challenge: b64urlToBuffer(opts.challenge),
          rp: {name: opts.rpName, id: location.hostname},
          user: {id: usernameToUserId(opts.username), name: opts.username, displayName: opts.username},
          pubKeyCredParams: [{type:'public-key', alg:-7}, {type:'public-key', alg:-257}],
          authenticatorSelection: {userVerification:'preferred'},
          timeout: 60000,
          attestation: 'none'
        }
      }) as PublicKeyCredential | null;
      if(!credential){setErr('Passkey-Registrierung wurde abgebrochen.'); return;}
      const response = credential.response as AuthenticatorAttestationResponse;
      await passkeyRegisterFinish(
        username.trim(),
        opts.challenge,
        bufferToB64url(credential.rawId),
        JSON.stringify({clientDataJSON: bufferToB64url(response.clientDataJSON), attestationObject: bufferToB64url(response.attestationObject)}),
        navigator.userAgent.slice(0,120)
      );
      setInfo('Passkey wurde gespeichert. Du kannst jetzt den Passkey-Login testen.');
      setTab('passkey-login');
    }catch(ex:any){setErr(ex.message??'Passkey-Registrierung fehlgeschlagen');}
  }

  async function loginWithPasskey(){
    setErr(''); setInfo('');
    if(!username.trim()){setErr('Bitte Benutzernamen eingeben.'); return;}
    if(!window.PublicKeyCredential){setErr('Dieser Browser unterstützt keine Passkeys/WebAuthn.'); return;}
    try{
      const opts = await passkeyLoginOptions(username.trim());
      if(!opts.allowCredentialIds?.length){setErr('Für diesen Benutzer ist kein Passkey gespeichert.'); return;}
      const assertion = await navigator.credentials.get({
        publicKey: {
          challenge: b64urlToBuffer(opts.challenge),
          allowCredentials: opts.allowCredentialIds.map(id=>({type:'public-key', id:b64urlToBuffer(id)})),
          userVerification: 'preferred',
          timeout: 60000
        }
      }) as PublicKeyCredential | null;
      if(!assertion){setErr('Passkey-Login wurde abgebrochen.'); return;}
      await passkeyLoginFinish(username.trim(), opts.challenge, bufferToB64url(assertion.rawId));
      onLogin();
    }catch(ex:any){setErr(ex.message??'Passkey-Login fehlgeschlagen');}
  }

  const locked = retryAfter > 0;

  return <main className="login"><section className="card login-card"><h1>GAM 2.0</h1><p>Kompatibler Login über bestehende <code>accounts</code>-Tabelle.</p><nav className="tabs login-tabs"><button type="button" className={tab==='password'?'active':''} onClick={()=>setTab('password')}><ShieldCheck size={16}/> Passwort</button><button type="button" className={tab==='totp-register'?'active':''} onClick={()=>setTab('totp-register')}><QrCode size={16}/> 2FA registrieren</button><button type="button" className={tab==='totp-login'?'active':''} onClick={()=>setTab('totp-login')}><Smartphone size={16}/> 2FA-Login</button><button type="button" className={tab==='passkey-register'?'active':''} onClick={()=>{setTab('passkey-register'); passkeyInfo();}}><KeyRound size={16}/> Passkey registrieren</button><button type="button" className={tab==='passkey-login'?'active':''} onClick={()=>{setTab('passkey-login'); passkeyInfo();}}><KeyRound size={16}/> Passkey-Login</button></nav>
  {locked&&<p className="note warn">Zu viele Fehlversuche. Neuer Versuch in <b>{retryAfter}</b> Sekunden.</p>}
  {tab==='password'&&<form onSubmit={passwordLogin} className="login-form"><input autoFocus placeholder="Benutzername" value={username} onChange={e=>setUsername(e.target.value)}/><input placeholder="Passwort" type="password" value={password} onChange={e=>setPassword(e.target.value)}/><button disabled={locked}><ShieldCheck size={18}/> {locked?`Warten ${retryAfter}s`:'Anmelden'}</button></form>}
  {tab==='totp-register'&&<section className="login-form"><form onSubmit={startTotpSetup} className="login-form"><input placeholder="Benutzername" value={username} onChange={e=>setUsername(e.target.value)}/><p className="muted">Für die 2FA-Registrierung wird nur der Benutzername benötigt. Das Passwortfeld ist bewusst ausgeblendet.</p><button><QrCode size={18}/> QR-Code erzeugen</button></form>{setup&&<div className="qr-box"><QRCodeSVG value={setup.otpauthUri} size={210}/><p><b>Secret:</b> <code>{setup.secret}</code></p><small>Authenticator: Google Authenticator, Microsoft Authenticator, Aegis, 2FAS, Bitwarden usw.</small><input placeholder="6-stelliger Code zur Bestätigung" value={totp} onChange={e=>setTotp(e.target.value)}/><button type="button" onClick={confirmTotpSetup}>2FA speichern</button></div>}</section>}
  {tab==='totp-login'&&<form onSubmit={totpLogin} className="login-form"><input placeholder="Benutzername" value={username} onChange={e=>setUsername(e.target.value)}/><input placeholder="6-stelliger Authenticator-Code" value={totp} onChange={e=>setTotp(e.target.value)}/><button disabled={locked}><Smartphone size={18}/> {locked?`Warten ${retryAfter}s`:'Mit 2FA anmelden'}</button></form>}
  {tab==='passkey-register'&&<section className="login-form"><input placeholder="Benutzername" value={username} onChange={e=>setUsername(e.target.value)}/><p className="note warn">Passkey-Testworkflow für localhost/Windows Hello/YubiKey. Für Produktivbetrieb wird die serverseitige WebAuthn-Signaturprüfung noch gehärtet.</p><button type="button" onClick={registerPasskey}><KeyRound size={18}/> Passkey registrieren</button><button type="button" className="secondary" onClick={passkeyInfo}>Passkey-Status prüfen</button></section>}
  {tab==='passkey-login'&&<section className="login-form"><input placeholder="Benutzername" value={username} onChange={e=>setUsername(e.target.value)}/><p className="note warn">Passkey-Login über WebAuthn. Lokal funktioniert das mit <code>localhost</code>.</p><button type="button" onClick={loginWithPasskey}><KeyRound size={18}/> Mit Passkey anmelden</button><button type="button" className="secondary" onClick={passkeyInfo}>Passkey-Status prüfen</button></section>}
  {info&&<p className="note ok">{info}</p>}{err&&<b className="error">{err}</b>}</section></main>
}

function App(){const [authed,setAuthed]=useState(!!token()); useEffect(()=>{const h=()=>setAuthed(false); window.addEventListener("gam-auth-expired",h); return()=>window.removeEventListener("gam-auth-expired",h)},[]); return authed?<Shell onLogout={()=>{logout();setAuthed(false)}}/>:<Login onLogin={()=>setAuthed(true)}/>}
function Shell({onLogout}:{onLogout:()=>void}){const [account,setAccount]=useState<AccountDto|null>(null); const [menu,setMenu]=useState<RoleDto|null>(null); const [page,setPage]=useState<Page>('dashboard');
 useEffect(()=>{me().then(setAccount).catch(()=>{logout(); onLogout();}); loadMenu().then(m=>{setMenu(m); if(m.modules.includes('invoices')) setPage('invoices')}).catch(()=>{logout(); onLogout();})},[]);
 const modules=menu?.modules??['dashboard'];
 const nav=[['dashboard','Dashboard',LayoutDashboard],['invoices','Rechnungen',FileText],['inventory','Inventar',Package],['warehouse','Lager',Warehouse],['tasks','Aufgaben',CheckSquare],['approvals','Freigaben',ClipboardCheck],['personnel','Personal',BriefcaseBusiness],['cashbook','Kassenbuch',Landmark],['compliance','Prüfungen',ClipboardList],['reports','Reports',FileBarChart],['users','Benutzer/Rechte',UsersRound]] as const;
 return <main><header><div><h1>GAM 2.0</h1><span>{account?.fullname||account?.username} · Rolle: {menu?.label||account?.role||'—'} · Schritt 10 Rechte/Sicherheit</span></div><button className="secondary" onClick={onLogout}><LogOut size={16}/> Logout</button></header><nav className="tabs">{nav.filter(n=>modules.includes(n[0]) || (n[0]==='users' && modules.includes('admin'))).map(([key,label,Icon])=><button key={key} className={page===key?'active':''} onClick={()=>setPage(key as Page)}><Icon size={16}/>{label}</button>)}</nav>{page==='dashboard'&&<DashboardHome/>}{page==='invoices'&&<InvoicesPage/>}{page==='users'&&<UsersPage/>}{page==='inventory'&&<InventoryPage/>}{page==='warehouse'&&<><WarehousePage/><InventoryWarehousePage/></>}{page==='tasks'&&<RecordsPage title='Aufgaben' loader={loadGamTasks}/>} {page==='approvals'&&<RecordsPage title='Freigaben' loader={loadGamApprovals}/>} {page==='personnel'&&<RecordsPage title='Personal' loader={loadGamPersonnel}/>} {page==='cashbook'&&<RecordsPage title='Kassenbuch' loader={loadGamCashbook}/>} {page==='compliance'&&<CompliancePage/>} {page==='reports'&&<ReportsPage/>}</main>}
function Placeholder({title,text}:{title:string;text:string}){return <section className="card"><h2>{title}</h2><p className="muted">{text}</p></section>}
function DashboardHome(){const [status,setStatus]=useState<SystemStatus|null>(null); useEffect(()=>{loadSystemStatus().then(setStatus).catch(()=>{})},[]); return <section className="card"><h2>Dashboard</h2>{status?<p><b>DB:</b> {status.databaseAvailable?'verbunden':'nicht verbunden'} · <b>Accounts:</b> {status.accountCount} · <b>.lbd:</b> {status.lbdAvailable?'gefunden':'nicht gefunden'}</p>:<p className="muted">Status wird geladen.</p>}<p>Schritt 10 härtet Login, Rollen und API-Zugriffe: Module sind serverseitig geschützt, 2FA bleibt kompatibel, Admin-Endpunkte sind begrenzt.</p><ModuleTiles/></section>}
function InvoicesPage(){
 const [rows,setRows]=useState<InvoiceSummary[]>([]); const [selected,setSelected]=useState<InvoiceDetail|null>(null); const [filter,setFilter]=useState(''); const [mode,setMode]=useState<'search'|'new'|'edit'>('search'); const [companies,setCompanies]=useState<InvoiceCompany[]>([]); const [searchCompanyId,setSearchCompanyId]=useState<number|undefined>(); const [searchInfo,setSearchInfo]=useState('');
 async function refresh(q=filter){if(!searchCompanyId){setRows([]); setSelected(null); setSearchInfo('Bitte zuerst eine Gesellschaft auswählen.'); return;} setSearchInfo(''); const r=await loadInvoices(100,q,searchCompanyId); setRows(r); if(r[0]) setSelected(await loadInvoice(r[0].number)); else setSelected(null);}
 async function select(number:string){const detail=await loadInvoice(number); setSelected(detail);}
 useEffect(()=>{loadCompanies().then(cs=>{setCompanies(cs); if(cs[0]) setSearchCompanyId(cs[0].id)}).catch(()=>{})},[]);
 return <>
  <section className="toolbar invoice-menu">
    <button className={mode==='new'?'active':''} onClick={()=>setMode('new')}><FilePlus2 size={16}/> Neue Rechnung</button>
    <button className={mode==='search'?'active secondary':'secondary'} onClick={()=>setMode('search')}><Search size={16}/> Rechnung suchen</button>
    <button className="secondary" disabled={!selected} onClick={()=>setMode('edit')}>Rechnung bearbeiten</button>
  </section>
  {mode==='new'&&<InvoiceEditor initialCompanyId={searchCompanyId} onSaved={async d=>{await refresh(); setSelected(d.invoice); setMode('search');}}/>}
  {mode==='edit'&&selected&&<InvoiceEditor existing={selected} onSaved={async d=>{await refresh(); setSelected(d.invoice); setMode('search');}}/>}
  {mode==='search'&&<section className="layout invoice-search-layout">
    <aside className="card search-panel">
      <h2>Rechnung suchen</h2>
      <label>Gesellschaft<select value={searchCompanyId??''} onChange={e=>setSearchCompanyId(e.target.value?Number(e.target.value):undefined)}><option value="">Bitte wählen</option>{companies.map(c=><option key={c.id} value={c.id}>{c.name??c.code??c.id}</option>)}</select></label>
      <label><Search size={16}/><input placeholder="Suche Nummer / Name / Grund" value={filter} onChange={e=>setFilter(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') refresh(filter)}}/></label>
      <button className="secondary" disabled={!searchCompanyId} onClick={()=>refresh(filter)}>Suchen</button>
      {searchInfo&&<p className="note warn">{searchInfo}</p>}
      <div className="list">{rows.map(r=><button key={r.id} onClick={()=>select(r.number)}><b>{r.number}</b><small>{r.invoiceDate} · {money(r.totalGross)} · {r.companyName}</small></button>)}</div>
    </aside>
    <section className="card detail">{selected?<><div className="row"><h2>Rechnung {selected.summary.number}</h2><div className="download-actions"><a className="buttonlink" target="_blank" href={pdfUrl(selected.summary.number)}><Download size={16}/> ZUGFeRD-PDF</a><a className="buttonlink secondarylink" target="_blank" href={zugferdXmlUrl(selected.summary.number)}><Download size={16}/> XML</a></div></div><p>{selected.summary.companyName} · {selected.summary.invoiceDate} · {money(selected.totals?.gross ?? selected.summary.totalGross)}</p><ExportCheck number={selected.summary.number}/><InvoiceStatusActions detail={selected} onChanged={setSelected}/><table><thead><tr><th>Menge</th><th>Code</th><th>Beschreibung</th><th>MwSt</th><th>Preis</th></tr></thead><tbody>{selected.lines.map(l=><tr key={l.id}><td>{l.quantity}</td><td>{l.code}</td><td>{l.description}</td><td>{l.vat}%</td><td>{money(l.price)}</td></tr>)}</tbody></table></>:<div className="empty">Bitte links eine Rechnung auswählen.</div>}</section>
  </section>}
 </>}

function InvoiceStatusActions({detail,onChanged}:{detail:InvoiceDetail; onChanged:(d:InvoiceDetail)=>void}){const [busy,setBusy]=useState(false); const n=detail.summary.number; const isSpecial=/[SGP]$/.test(n); async function cancel(){if(!confirm(`Stornorechnung ${n}S erstellen?`)) return; setBusy(true); try{const res=await createCancellationInvoice(n); onChanged(res.invoice);}finally{setBusy(false)}} async function credit(){if(!confirm(`Gutschrift ${n}G erstellen?`)) return; setBusy(true); try{const res=await createCreditNote(n); onChanged(res.invoice);}finally{setBusy(false)}} async function del(){if(!confirm('Diese Rechnung wirklich technisch löschen? Fachlich ist Storno meistens sicherer.')) return; setBusy(true); try{await deleteInvoiceDraft(detail.summary.number); location.reload();}finally{setBusy(false)}} return <div className="toolbar small"><button className="secondary" disabled={busy || isSpecial} onClick={cancel}>Stornorechnung {n}S</button><button className="secondary" disabled={busy || isSpecial} onClick={credit}>Gutschrift {n}G</button><button className="danger" disabled={busy} onClick={del}>Technisch löschen</button></div>}

function ExportCheck({number}:{number:string}){const [check,setCheck]=useState<any|null>(null); useEffect(()=>{loadExportCheck(number).then(setCheck).catch(()=>setCheck(null))},[number]); if(!check) return <p className="muted">Exportprüfung wird geladen.</p>; return <div className={check.exportable?'note ok':'note warn'}>{check.exportable?'ZUGFeRD-Export bereit':'ZUGFeRD-Export hat Hinweise'}{check.issues?.length?<ul>{check.issues.map((i:any,idx:number)=><li key={idx}>{i.severity}: {i.field} – {i.message}</li>)}</ul>:null}</div>}

function InvoiceEditor({existing,initialCompanyId,onSaved}:{existing?:InvoiceDetail; initialCompanyId?:number; onSaved:(r:any)=>void}){const [products,setProducts]=useState<ProductDto[]>([]); const [companies,setCompanies]=useState<InvoiceCompany[]>([]); const [companyId,setCompanyId]=useState(existing?.summary.companyId??initialCompanyId??2); const [productId,setProductId]=useState<number|undefined>(); const [qty,setQty]=useState(1); const [invoiceDate,setInvoiceDate]=useState(toInputDate(existing?.summary.invoiceDate)); const [paymentMethod,setPaymentMethod]=useState('unbekannt'); const [reason,setReason]=useState(''); const [remark,setRemark]=useState(''); const [err,setErr]=useState(''); const [next,setNext]=useState(existing?.summary.number??''); const [lbd,setLbd]=useState<LbdRecipient|null>(null); const [lines,setLines]=useState<InvoiceCreateLineRequest[]>(existing?.lines.map(l=>({productId:l.productId,quantity:l.quantity,price:l.price,vat:l.vat,branchId:l.branchId,client:l.client,performer:l.performer}))??[]); const [totals,setTotals]=useState<any>(null);
 useEffect(()=>{loadProducts('',120).then(ps=>{setProducts(ps); if(ps[0]) setProductId(ps[0].id)}); loadCompanies().then(cs=>{setCompanies(cs); if(!existing && cs[0]) setCompanyId(cs[0].id)}).catch(()=>{}); loadLbdPreview().then(setLbd).catch(()=>setLbd(null)); if(!existing) loadNextInvoiceNumber(companyId).then(n=>setNext(n.nextNumber)).catch(()=>{});},[]);
 useEffect(()=>{if(!existing) loadNextInvoiceNumber(companyId).then(n=>setNext(n.nextNumber)).catch(()=>{})},[companyId]);
 useEffect(()=>{if(lines.length) loadCalculate(lines,setTotals).catch(()=>{}); else setTotals(null)},[JSON.stringify(lines)]);
 const p=products.find(x=>x.id===productId); function addLine(){if(!p) return; setLines([...lines,{productId:p.id, quantity:qty, price:p.price??0, vat:p.vat??0}])}
 function updateLine(idx:number, patch:Partial<InvoiceCreateLineRequest>){setLines(lines.map((l,i)=>i===idx?{...l,...patch}:l))}
 function removeLine(idx:number){setLines(lines.filter((_,i)=>i!==idx))}
 function buildPayload(){const submitLines = lines.length ? lines : (p ? [{productId:p.id, quantity:qty, price:p.price??0, vat:p.vat??0}] : []); if(!submitLines.length) throw new Error('Bitte mindestens eine Position auswählen.'); return {invoiceDate, treatmentDate:invoiceDate, companyId, paymentMethod, reason, remark, lines:submitLines};}
 async function submit(e:React.FormEvent){e.preventDefault(); setErr(''); try{const payload=buildPayload(); const res=existing?await updateInvoice(existing.summary.number,payload):await createInvoice({...payload, number: next || undefined}); onSaved(res);}catch(ex:any){setErr(ex.message??'Speichern fehlgeschlagen');}}
 async function submitProforma(){setErr(''); try{const payload=buildPayload(); const res=await createProformaInvoice({...payload, number: undefined, paymentAdvice: true, reason: reason || 'Proforma-Rechnung'}); onSaved(res);}catch(ex:any){setErr(ex.message??'Proforma konnte nicht erstellt werden');}}
 return <section className="card new invoice-editor"><div className="row"><div><h2>{existing?'Rechnung bearbeiten':'Neue Rechnung'}</h2><p className="muted">Nummer: <b>{next||'wird geladen'}</b> · ZUGFeRD/Factur-X ist Pflicht-Export.</p></div>{lbd?.found&&<div className="lbd-compact"><UserRound size={16}/><div><b>{[lbd.salutation,lbd.title,lbd.firstName,lbd.lastName].filter(Boolean).join(' ')}</b><br/><small>{lbd.street} · {[lbd.postalCode,lbd.city].filter(Boolean).join(' ')}</small></div></div>}</div><form onSubmit={submit} className="newgrid"><label>Rechnungsdatum<input type="date" value={invoiceDate} onChange={e=>setInvoiceDate(e.target.value)}/></label><label>Gesellschaft<select value={companyId} onChange={e=>setCompanyId(Number(e.target.value))}>{companies.map(c=><option key={c.id} value={c.id}>{c.name??c.code??c.id}</option>)}</select></label><label>Zahlungsart<input value={paymentMethod} onChange={e=>setPaymentMethod(e.target.value)}/></label><label>Grund<input value={reason} onChange={e=>setReason(e.target.value)}/></label><label>Bemerkung<input value={remark} onChange={e=>setRemark(e.target.value)}/></label><label className="product-field">Produkt<select value={productId??''} onChange={e=>setProductId(Number(e.target.value))}>{products.map(p=><option key={p.id} value={p.id}>{p.code} · {p.description} · {money(p.price)}</option>)}</select></label><label className="quantity-field">Menge<input type="number" step="0.1" value={qty} onChange={e=>setQty(Number(e.target.value))}/></label><button type="button" className="secondary add-position" onClick={addLine}>+ Position übernehmen</button><button className="save-invoice">{existing?'Änderungen speichern':'Rechnung speichern'}</button>{!existing&&<button type="button" className="secondary" onClick={submitProforma}>Proforma +P</button>}</form>{lines.length>0&&<table><thead><tr><th>Menge</th><th>Produkt</th><th>MwSt</th><th>Preis</th><th></th></tr></thead><tbody>{lines.map((l,idx)=>{const prod=products.find(p=>p.id===l.productId); return <tr key={idx}><td><input type="number" step="0.1" value={l.quantity??1} onChange={e=>updateLine(idx,{quantity:Number(e.target.value)})}/></td><td className="product-cell"><b>{prod?.code??l.productId}</b><br/><span>{prod?.description??''}</span></td><td><input type="number" value={l.vat??0} onChange={e=>updateLine(idx,{vat:Number(e.target.value)})}/></td><td><input type="number" step="0.01" value={l.price??0} onChange={e=>updateLine(idx,{price:Number(e.target.value)})}/></td><td><button type="button" className="danger" onClick={()=>removeLine(idx)}>Entfernen</button></td></tr>})}</tbody></table>}{totals&&<p className="note ok">Netto {money(totals.net)} · MwSt {money(totals.vat)} · Brutto <b>{money(totals.gross)}</b></p>}{err&&<b className="error">{err}</b>}</section>}

async function loadCalculate(lines:InvoiceCreateLineRequest[], setter:(v:any)=>void){setter(await calculateInvoice(lines));}
function toInputDate(s?:string){if(!s) return new Date().toISOString().slice(0,10); if(/^\d{2}\.\d{2}\.\d{4}$/.test(s)){const [d,m,y]=s.split('.'); return `${y}-${m}-${d}`;} return s.slice(0,10)}

function ModuleTiles(){const [mods,setMods]=useState<any[]>([]); useEffect(()=>{loadGamModules().then(setMods).catch(()=>{})},[]); if(!mods.length) return null; return <div className="stats">{mods.map(m=><span key={m.key}><b>{m.count>=0?m.count:'—'}</b><br/>{m.label}<br/><small>{m.tableName}</small></span>)}</div>}
function RecordsPage({title,loader}:{title:string; loader:(limit?:number)=>Promise<any[]>}){const [rows,setRows]=useState<any[]>([]); const [err,setErr]=useState(''); useEffect(()=>{loader(150).then(setRows).catch((e:any)=>setErr(e.message??'Konnte Daten nicht laden'))},[title]); return <section className="card"><h2>{title}</h2><p className="muted">Schritt 5: sichere Leseansicht als Modulrahmen. Schreib-/Bearbeitungslogik wird erst nach Abgleich mit der alten Fachlogik aktiviert.</p>{err&&<b className="error">{err}</b>}<GenericTable rows={rows}/></section>}
function CompliancePage(){const [data,setData]=useState<Record<string,any[]>>({}); useEffect(()=>{loadGamCompliance(80).then(setData).catch(()=>{})},[]); return <section className="card"><h2>Prüfungen / Inbetriebnahmen / Einweisungen</h2><p className="muted">Rahmen für STK/MTK/BGV-A3, Inbetriebnahme und Einweisungen.</p>{Object.entries(data).map(([key,rows])=><div key={key}><h3>{key}</h3><GenericTable rows={rows}/></div>)}</section>}
function ReportsPage(){const [data,setData]=useState<Record<string,unknown>>({}); useEffect(()=>{loadGamReportSummary().then(setData).catch(()=>{})},[]); return <section className="card"><h2>Reports / Exporte</h2><p className="muted">Zentrale Zusammenfassung. Detailberichte folgen modulweise, damit alte Reportlogik nicht versehentlich verfälscht wird.</p><div className="stats">{Object.entries(data).filter(([k])=>k!=='note').map(([k,v])=><span key={k}>{k}<br/><b>{String(v)}</b></span>)}</div>{data.note&&<p className="note">{String(data.note)}</p>}</section>}
function GenericTable({rows}:{rows:any[]}){if(!rows?.length) return <p className="muted">Keine Daten gefunden.</p>; const mapped=rows.map(r=>r.values??r); const keys=Array.from(new Set(mapped.flatMap((r:any)=>Object.keys(r)))).slice(0,10); return <table><thead><tr>{keys.map(k=><th key={k}>{k}</th>)}</tr></thead><tbody>{mapped.map((r:any,idx:number)=><tr key={idx}>{keys.map(k=><td key={k}>{formatCell(r[k])}</td>)}</tr>)}</tbody></table>}
function formatCell(v:unknown){if(v==null) return '—'; if(typeof v==='object') return JSON.stringify(v); return String(v)}


// Schritt 9: Inventar/Lager-Verbindung
function InventoryWarehousePage(){const [links,setLinks]=useState<any[]>([]); const [mov,setMov]=useState<any[]>([]); const [materialId,setMaterialId]=useState<number>(1); const [deviceId,setDeviceId]=useState<number|undefined>(); const [qty,setQty]=useState(1); const [reason,setReason]=useState('GAM 2.0 Lagerbuchung'); const [msg,setMsg]=useState(''); const reload=()=>{loadDeviceMaterialLinks().then(setLinks).catch(()=>{}); loadMaterialMovements(80).then(setMov).catch(()=>{})}; useEffect(reload,[]); async function submit(direction:string){setMsg(''); try{await bookMaterial({deviceId, materialId, quantity:qty, direction, reason}); setMsg('Buchung gespeichert.'); reload();}catch(e:any){setMsg(e.message??'Buchung fehlgeschlagen')}} return <section className="grid"><section className="card"><h2>Inventar ↔ Lager</h2><p className="muted">Schritt 9 verbindet Geräte, Verbrauchsmaterial und Bestandsbewegungen. Bei Buchung mit Geräte-ID wird die Materialzuordnung automatisch angelegt.</p><div className="newgrid"><label>Geräte-ID optional<input type="number" value={deviceId??''} onChange={e=>setDeviceId(e.target.value?Number(e.target.value):undefined)}/></label><label>Material-ID<input type="number" value={materialId} onChange={e=>setMaterialId(Number(e.target.value))}/></label><label>Menge<input type="number" value={qty} onChange={e=>setQty(Number(e.target.value))}/></label><label>Grund<input value={reason} onChange={e=>setReason(e.target.value)}/></label><button onClick={()=>submit('OUT')}>Material entnehmen</button><button className="secondary" onClick={()=>submit('IN')}>Material Zugang</button></div>{msg&&<p className="note">{msg}</p>}<h3>Zuordnungen</h3><table><thead><tr><th>Gerät</th><th>Material</th><th>Typ</th><th>Bestand</th></tr></thead><tbody>{links.slice(0,80).map(l=><tr key={l.linkId}><td>{l.deviceName} #{l.deviceId}</td><td>{l.materialName} #{l.materialId}</td><td>{l.materialType??'—'}</td><td>{l.stock??'—'}</td></tr>)}</tbody></table></section><section className="card"><h2>Materialbewegungen</h2><table><thead><tr><th>Zeit</th><th>Gerät</th><th>Material</th><th>Δ</th><th>Bestand</th><th>Grund</th></tr></thead><tbody>{mov.map(m=><tr key={m.id}><td>{m.createdAt}</td><td>{m.deviceName??'—'}</td><td>{m.materialName}</td><td>{m.delta}</td><td>{m.previousStock??'—'} → {m.newStock??'—'}</td><td>{m.reason??'—'}</td></tr>)}</tbody></table></section></section>}

function money(v?:number){return v==null?'—':new Intl.NumberFormat('de-DE',{style:'currency',currency:'EUR'}).format(v)}
createRoot(document.getElementById('root')!).render(<App/>);
