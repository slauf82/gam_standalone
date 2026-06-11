export type AccountDto = { id: number; username: string; fullname?: string; role?: string; email?: string; twoFactorConfigured: boolean };
export type LoginResponse = { token: string; account: AccountDto; loginMode: string; expiresInMinutes: number };
export type SystemStatus = { databaseAvailable: boolean; accountCount: number; lbdAvailable: boolean; lbdFile?: string; lbdSearchFolders: string[]; modules: Record<string, unknown> };
export type InvoiceSummary = { id: number; number: string; invoiceDate?: string; totalGross?: number; companyId?: number; companyName?: string; username?: string; creditNote?: boolean; cancelled?: boolean; paymentAdvice?: boolean; couponAmount?: number; discountPercent?: number; discountRemark?: string; installments?: number };
export type InvoiceLine = { id: number; number?: string; quantity?: number; productId?: number; code?: string; description?: string; vat?: number; price?: number; branchId?: number; client?: string; performer?: string };
export type InvoiceDetail = { summary: InvoiceSummary; lines: InvoiceLine[]; totals?: InvoiceTotals };
export type InvoiceNumberPreview = { nextNumber: string; currentMaxNumber: string; numericSequence: boolean; note: string };
export type InvoiceValidationIssue = { severity: string; field: string; message: string };
export type InvoiceExportCheck = { number: string; exportable: boolean; issues: InvoiceValidationIssue[] };
export type ProductDto = { id: number; code?: string; description?: string; category?: string; price?: number; vat?: number; companyId?: number };
export type InvoiceCompany = { id: number; code?: string; name?: string; address?: string; street?: string; city?: string; email?: string; iban?: string; bic?: string; accountHolder?: string; taxNumber?: string; vatId?: string };
export type InvoiceCreateLineRequest = { productId?: number; quantity?: number; price?: number; vat?: number; branchId?: number; client?: string; performer?: string };
export type InvoiceCreateRequest = { number?: string; invoiceDate?: string; treatmentDate?: string; companyId?: number; addressId?: number; childAddressId?: number; firmAddressId?: number; branchId?: number; paymentMethod?: string; reason?: string; remark?: string; creditNote?: boolean; cancelled?: boolean; paymentAdvice?: boolean; couponText?: string; couponAmount?: number; discountType?: string; discountValue?: number; installments?: number; lbdFile?: string; lines: InvoiceCreateLineRequest[] };
export type InvoiceTotals = { net: number; vat: number; gross: number; vatByRate: Record<string, number> };
export type InvoiceCreateResponse = { number: string; detailId?: number; totals: InvoiceTotals; invoice: InvoiceDetail };
export type InvoiceUpdateRequest = Omit<InvoiceCreateRequest, "number">;
export type InvoiceStatusUpdateRequest = { cancelled?: boolean; creditNote?: boolean; paymentAdvice?: boolean; reason?: string };
export type InvoiceDraft = { suggestedNumber: string; invoiceDate: string; companies: InvoiceCompany[]; lbdRecipient: LbdRecipient; totals: InvoiceTotals };
export type InvoiceTextPreview = { language:string; companyId?:number; documentTitle?:string; salutation:string; invoiceText:string; lawHint:string; greetings:string; labels?: Record<string,string> };
export type LbdRecipient = { found: boolean; file?: string; patientNumber?: string; nameSuffix?: string; lastName?: string; firstName?: string; birthDate?: string; title?: string; insuranceNumber?: string; postalCode?: string; city?: string; country?: string; street?: string; insuranceType?: string; salutationIndex?: number; salutation?: string; rawFields: Record<string,string> };

const API = import.meta.env.VITE_GAM_API ?? "http://localhost:8080/api";
export function apiBase() { return API; }
export function token() { return localStorage.getItem("gam_token") ?? ""; }
export function setToken(value: string) { localStorage.setItem("gam_token", value); }
export function logout() { localStorage.removeItem("gam_token"); localStorage.removeItem("gam_user"); }

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API}${path}`, {
    ...options,
    headers: {"Content-Type": "application/json", ...(token() ? {Authorization: `Bearer ${token()}`} : {}), ...(options.headers ?? {})}
  });
  if (!res.ok) {
    const text = await res.text();
    let data: any = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = null; }

    // Schritt 31u:
    // Nicht jeder 401/403 nach dem Login darf sofort die komplette Oberfläche ausloggen.
    // Einige noch nicht vollständig migrierte Demo-/Modulendpunkte können temporär 401/403 liefern.
    // Nur /auth/me ist der harte Sitzungsnachweis. Alle anderen Aufrufer sollen ihren Fehler lokal behandeln.
    if ((res.status === 401 || res.status === 403) && path.startsWith("/auth/me")) {
      logout();
      window.dispatchEvent(new CustomEvent("gam-auth-expired"));
      throw new Error("Sitzung abgelaufen. Bitte erneut anmelden.");
    }

    const err: any = new Error(data?.message || text || `HTTP ${res.status}`);
    err.status = res.status;
    const retryAfter =
      data?.retryAfterSeconds ??
      Number(res.headers.get("Retry-After") || 0);

    err.retryAfterSeconds = retryAfter || undefined;
    throw err;
  }
  return res.json();
}

export async function login(username: string, password: string, totpCode: string) {
  const data = await request<LoginResponse>("/auth/login", {method: "POST", body: JSON.stringify({username, password, totpCode})});
  setToken(data.token); localStorage.setItem("gam_user", JSON.stringify(data.account)); return data;
}
export type TotpSetupResponse = { username:string; secret:string; otpauthUri:string; alreadyConfigured:boolean };
export const setupTotp = (username:string) => request<TotpSetupResponse>("/auth/totp/setup", {method:"POST", body: JSON.stringify({username})});
export const confirmTotp = (username:string, secret:string, totpCode:string) => request<AccountDto>("/auth/totp/confirm", {method:"POST", body: JSON.stringify({username,secret,totpCode})});
export type PasskeyOptionsResponse = { username:string; userId:string; challenge:string; rpId:string; rpName:string; allowCredentialIds:string[] };
export const loadPasskeyStatus = () => request<Record<string,unknown>>("/auth/passkey/status");
export const passkeyRegisterOptions = (username:string) => request<PasskeyOptionsResponse>("/auth/passkey/register/options", {method:"POST", body: JSON.stringify({username})});
export const passkeyRegisterFinish = (username:string, challenge:string, credentialId:string, publicKey:string, deviceName:string) => request<AccountDto>("/auth/passkey/register/finish", {method:"POST", body: JSON.stringify({username, challenge, credentialId, publicKey, deviceName})});
export const passkeyLoginOptions = (username:string) => request<PasskeyOptionsResponse>("/auth/passkey/login/options", {method:"POST", body: JSON.stringify({username})});
export async function passkeyLoginFinish(username:string, challenge:string, credentialId:string) {
  const data = await request<LoginResponse>("/auth/passkey/login/finish", {method:"POST", body: JSON.stringify({username, challenge, credentialId})});
  setToken(data.token); localStorage.setItem("gam_user", JSON.stringify(data.account)); return data;
}
export const me = () => request<AccountDto>("/auth/me");
export const loadSystemStatus = () => request<SystemStatus>("/system/status");
export const loadInvoices = (limit = 100, q = "", companyId?: number) => request<InvoiceSummary[]>(`/invoices?limit=${limit}&q=${encodeURIComponent(q)}${companyId ? `&companyId=${companyId}` : ""}`);
export const loadInvoice = (number: string, companyId?: number) => request<InvoiceDetail>(`/invoices/${encodeURIComponent(number)}${companyId ? `?companyId=${companyId}` : ""}`);
export const loadLbdPreview = (file = "") => request<LbdRecipient>(`/invoices/lbd/preview${file ? `?file=${encodeURIComponent(file)}` : ""}`);
export const loadProducts = (q = "", limit = 50) => request<ProductDto[]>(`/invoices/products?q=${encodeURIComponent(q)}&limit=${limit}`);
export const loadInvoiceTextPreview = (companyId?: number, lang = "de", treatmentDate = "", lbdFile = "") => { const p = new URLSearchParams(); if (companyId) p.set("companyId", String(companyId)); p.set("lang", lang); if (treatmentDate) p.set("treatmentDate", treatmentDate); if (lbdFile) p.set("lbdFile", lbdFile); return request<InvoiceTextPreview>(`/invoices/text-preview?${p}`); };
export const loadDraft = () => request<InvoiceDraft>("/invoices/draft");
export const loadCompanies = () => request<InvoiceCompany[]>("/invoices/companies");
export const loadNextInvoiceNumber = (companyId?: number) => request<InvoiceNumberPreview>(`/invoices/numbers/next${companyId ? `?companyId=${companyId}` : ""}`);
export const loadExportCheck = (number: string, companyId?: number) => request<InvoiceExportCheck>(`/invoices/${encodeURIComponent(number)}/export-check${companyId ? `?companyId=${companyId}` : ""}`);
export const calculateInvoice = (lines: InvoiceCreateLineRequest[]) => request<InvoiceTotals>("/invoices/calculate", {method: "POST", body: JSON.stringify(lines)});
export const createInvoice = (payload: InvoiceCreateRequest) => request<InvoiceCreateResponse>("/invoices", {method: "POST", body: JSON.stringify(payload)});
export const updateInvoice = (number: string, payload: InvoiceUpdateRequest) => request<InvoiceCreateResponse>(`/invoices/${encodeURIComponent(number)}`, {method: "PUT", body: JSON.stringify(payload)});
export const updateInvoiceStatus = (number: string, payload: InvoiceStatusUpdateRequest) => request<InvoiceDetail>(`/invoices/${encodeURIComponent(number)}/status`, {method: "PATCH", body: JSON.stringify(payload)});
export const createCancellationInvoice = (number: string, companyId?: number) => request<InvoiceCreateResponse>(`/invoices/${encodeURIComponent(number)}/cancel${companyId ? `?companyId=${companyId}` : ""}`, {method: "POST"});
export const createCreditNote = (number: string, companyId?: number) => request<InvoiceCreateResponse>(`/invoices/${encodeURIComponent(number)}/credit${companyId ? `?companyId=${companyId}` : ""}`, {method: "POST"});
export const createProformaInvoice = (payload: InvoiceCreateRequest) => request<InvoiceCreateResponse>("/invoices/proforma", {method: "POST", body: JSON.stringify(payload)});
export const deleteInvoiceDraft = (number: string) => fetch(`${API}/invoices/${encodeURIComponent(number)}/draft`, {method: "DELETE", headers: {...(token() ? {Authorization: `Bearer ${token()}`} : {})}}).then(r => { if(!r.ok) throw new Error("Loeschen fehlgeschlagen"); });
export const pdfUrl = (number: string, lang = "de") => `${API}/invoices/${encodeURIComponent(number)}/pdf?lang=${encodeURIComponent(lang)}`;
export const pdfDebugUrl = (number: string) => `${API}/invoices/${encodeURIComponent(number)}/pdf-debug`;
export const zugferdXmlUrl = (number: string) => `${API}/invoices/${encodeURIComponent(number)}/zugferd.xml`;
export type InvoiceAccessInfo = { token:string; url:string; invoiceNumber:string; companyId?:number };
export const loadInvoiceAccess = (number:string, companyId?:number) => request<InvoiceAccessInfo>(`/invoices/${encodeURIComponent(number)}/access${companyId ? `?companyId=${companyId}` : ""}`);
export const invoiceAccessQrUrl = (number:string, companyId?:number) => `${API}/invoices/${encodeURIComponent(number)}/access-qr${companyId ? `?companyId=${companyId}` : ""}${token() ? `${companyId ? "&" : "?"}t=${encodeURIComponent(token())}` : ""}`;
export type ZugferdStatus = { enabled: boolean; profile: string; validationEnabled: boolean; note: string };
export const loadZugferdStatus = () => request<ZugferdStatus>("/invoices/zugferd/status");
export type RoleDto = { key: string; label: string; administrative: boolean; modules: string[] };
export type AccountAdminDto = { id: number; username: string; fullname?: string; role?: string; email?: string; twoFactorConfigured: boolean; passwordPresent: boolean };
export type AccountUpdateRequest = { fullname?: string; role?: string; email?: string; secretkey?: string };
export const loadMenu = () => request<RoleDto>("/auth/menu");
export const loadRoles = () => request<RoleDto[]>("/admin/roles");
export const loadAccounts = (q = "", limit = 100) => request<AccountAdminDto[]>(`/admin/accounts?q=${encodeURIComponent(q)}&limit=${limit}`);
export const updateAccount = (id: number, payload: AccountUpdateRequest) => request<AccountAdminDto>(`/admin/accounts/${id}`, {method:"PATCH", body: JSON.stringify(payload)});


export type InventoryDevice = { id:number; source:string; name?:string; type?:string; serialNumber?:string; inventoryNumber?:string; manufacturer?:string; ip?:string; location?:string; branchId?:number; branchCode?:string; branchName?:string; medicalDevice?:boolean; electricalDevice?:boolean; inventoryRelevant?:boolean; active?:boolean; inUse?:boolean; acquiredAt?:string; note?:string };
export type DeviceAssignment = { id:number; branchId?:number; branchCode?:string; branchName?:string; companyId?:number; companyName?:string };
export type DeviceConsumable = { id:number; materialId?:number; name?:string; properties?:string; amount?:number; manufacturerEmail?:string };
export type DeviceSoftware = { id:number; name?:string; workplaceId?:number };
export type InventoryDeviceDetail = { device: InventoryDevice; assignments: DeviceAssignment[]; consumables: DeviceConsumable[]; software: DeviceSoftware[] };
export type InventoryStats = { legacyDeviceCount:number; newDeviceCount:number; branchAssignmentCount:number; medicalDeviceCount:number; electricalDeviceCount:number; outOfServiceCount:number };
export const loadInventoryDevices = (q = "", source = "all", activeOnly = false, limit = 100) => request<InventoryDevice[]>(`/inventory/devices?q=${encodeURIComponent(q)}&source=${encodeURIComponent(source)}&activeOnly=${activeOnly}&limit=${limit}`);
export const loadInventoryDevice = (source: string, id: number) => request<InventoryDeviceDetail>(`/inventory/devices/${encodeURIComponent(source)}/${id}`);
export const loadInventoryStats = () => request<InventoryStats>("/inventory/stats");

export type WarehouseItem = { id:number; kind:string; name?:string; description?:string; location?:string; quantity?:number; properties?:string; manufacturerEmail?:string; linkedDeviceCount?:number };
export type WarehouseStats = { storageItemCount:number; consumableCount:number; consumablesWithStock:number; consumablesWithoutStock:number; deviceConsumableLinks:number; totalStorageQuantity:number; totalConsumableQuantity:number };
export type StockChangeRequest = { kind?:string; id?:number; quantity:number; reason?:string };
export const loadWarehouseItems = (q = "", kind = "all", onlyWithStock = false, limit = 100) => request<WarehouseItem[]>(`/warehouse/items?q=${encodeURIComponent(q)}&kind=${encodeURIComponent(kind)}&onlyWithStock=${onlyWithStock}&limit=${limit}`);
export const loadWarehouseItem = (kind: string, id: number) => request<WarehouseItem>(`/warehouse/items/${encodeURIComponent(kind)}/${id}`);
export const loadWarehouseStats = () => request<WarehouseStats>("/warehouse/stats");
export const updateWarehouseStock = (kind: string, id: number, quantity: number, reason = "") => request<WarehouseItem>(`/warehouse/items/${encodeURIComponent(kind)}/${id}/stock`, {method:"PATCH", body: JSON.stringify({kind,id,quantity,reason})});


export type ModuleOverview = { key:string; label:string; tableName:string; count:number; status:string; note:string };
export type ModuleRecord = { module:string; values:Record<string, unknown> };
export const loadGamModules = () => request<ModuleOverview[]>('/gam/modules');
export const loadGamTasks = (limit=100) => request<ModuleRecord[]>(`/gam/tasks?limit=${limit}`);
export const loadGamApprovals = (limit=100) => request<ModuleRecord[]>(`/gam/approvals?limit=${limit}`);
export const loadGamPersonnel = (limit=150) => request<ModuleRecord[]>(`/gam/personnel?limit=${limit}`);
export const loadGamCashbook = (limit=100) => request<ModuleRecord[]>(`/gam/cashbook?limit=${limit}`);
export const loadGamCompliance = (limit=100) => request<Record<string, ModuleRecord[]>>(`/gam/compliance?limit=${limit}`);
export const loadGamFolders = (limit=100) => request<ModuleRecord[]>(`/gam/folders?limit=${limit}`);
export const loadGamNews = (limit=50) => request<ModuleRecord[]>(`/gam/news?limit=${limit}`);
export const loadGamReportSummary = () => request<Record<string, unknown>>('/gam/reports/summary');

export type InvoiceReportRow = { invoiceDate?:string; companyId?:number; companyName?:string; documentType?:string; number?:string; username?:string; reason?:string; gross?:number; cancelled?:boolean; creditNote?:boolean; paymentAdvice?:boolean };
export type InvoiceReportSummary = { count:number; grossTotal:number; invoices:number; cancellations:number; creditNotes:number; paymentAdvices:number; proforma:number; note?:string };
export const loadInvoiceReportRows = (from:string, to:string, companyId?:number, reportType:string='overview') => { const p = new URLSearchParams(); if(from) p.set('from', from); if(to) p.set('to', to); if(companyId) p.set('companyId', String(companyId)); if(reportType) p.set('reportType', reportType); return request<InvoiceReportRow[]>(`/gam/reports/invoices?${p}`); };
export const loadInvoiceReportSummary = (from:string, to:string, companyId?:number, reportType:string='overview') => { const p = new URLSearchParams(); if(from) p.set('from', from); if(to) p.set('to', to); if(companyId) p.set('companyId', String(companyId)); if(reportType) p.set('reportType', reportType); return request<InvoiceReportSummary>(`/gam/reports/invoices/summary?${p}`); };
export async function downloadInvoiceReport(from:string, to:string, companyId:number|undefined, format:string, reportType:string='overview') { const p = new URLSearchParams(); if(from) p.set('from', from); if(to) p.set('to', to); if(companyId) p.set('companyId', String(companyId)); if(reportType) p.set('reportType', reportType); p.set('format', format); const res = await fetch(`${API}/gam/reports/invoices/export?${p}`, {headers: {...(token() ? {Authorization: `Bearer ${token()}`} : {})}}); if(!res.ok) throw new Error(await res.text() || 'Report-Export fehlgeschlagen'); const blob = await res.blob(); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `gam-rechnungsreport.${format === 'datev' ? 'csv' : format}`; document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url); }



// Schritt 9: Inventar/Lager-Verbindung
export type DeviceMaterialLink = {linkId:number; deviceId:number; deviceName:string; materialId:number; materialName:string; materialType?:string; stock?:number; manufacturerEmail?:string};
export type MaterialMovement = {id:number; createdAt:string; deviceId?:number; deviceName?:string; materialId:number; materialName:string; delta:number; previousStock?:number; newStock?:number; reason?:string; username?:string};
export type MaterialBookingRequest = {deviceId?:number; materialId:number; quantity:number; direction:string; reason?:string; username?:string};
export const loadDeviceMaterialLinks = (deviceId?:number, materialId?:number) => { const p = new URLSearchParams(); if (deviceId) p.set('deviceId', String(deviceId)); if (materialId) p.set('materialId', String(materialId)); return request<DeviceMaterialLink[]>(`/inventory-warehouse/links?${p}`); };
export const loadMaterialMovements = (limit=100) => request<MaterialMovement[]>(`/inventory-warehouse/movements?limit=${limit}`);
export const bookMaterial = (payload: MaterialBookingRequest) => request('/inventory-warehouse/book', {method:'POST', body: JSON.stringify(payload)});

export type SecurityStatus = { authenticated:boolean; username:string; role:string; legacySha256PasswordAllowed:boolean; totpOnlyAllowed:boolean; totpRequiredWhenSecretExists:boolean };
export const loadSecurityStatus = () => request<SecurityStatus>("/security/status");


export type TtsStatus = { enabled: boolean; engine: string; maryTtsEnabled: boolean; maryTtsEmbedded: boolean; maryTtsEmbeddedAvailable: boolean; maryTtsEmbeddedError: string; maryTtsEmbeddedVoices: string[]; maryTtsBundled: boolean; maryTtsHome: string; maryTtsEndpoint: string; maryTtsReachable: boolean; browserFallback: boolean; fallback: Record<string, string[]> };
export const loadTtsStatus = () => request<TtsStatus>('/tts/status');
export async function loadTtsAudio(language: string, text: string, engine = 'marytts') {
  const res = await fetch(`${API}/tts/audio`, {
    method: 'POST',
    headers: {"Content-Type": "application/json", ...(token() ? {Authorization: `Bearer ${token()}`} : {})},
    body: JSON.stringify({language, text, engine})
  });
  if (!res.ok) throw new Error('TTS audio unavailable');
  return await res.blob();
}
