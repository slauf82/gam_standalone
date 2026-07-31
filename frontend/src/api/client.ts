export type AccountDto = { id: number; username: string; fullname?: string; role?: string; email?: string; twoFactorConfigured: boolean };
export type LoginResponse = { token: string; account: AccountDto; loginMode: string; expiresInMinutes: number };
export type SystemStatus = { databaseAvailable: boolean; accountCount: number; lbdAvailable: boolean; lbdFile?: string; lbdSearchFolders: string[]; modules: Record<string, unknown> };
export type InvoiceSummary = { id: number; number: string; invoiceDate?: string; totalGross?: number; companyId?: number; companyName?: string; username?: string; creditNote?: boolean; cancelled?: boolean; paymentAdvice?: boolean; couponAmount?: number; discountPercent?: number; discountRemark?: string; installments?: number };
export type InvoiceLine = { id: number; number?: string; quantity?: number; productId?: number; code?: string; description?: string; vat?: number; price?: number; branchId?: number; client?: string; performer?: string };
export type InvoiceDetail = { summary: InvoiceSummary; lines: InvoiceLine[]; totals?: InvoiceTotals };
export type InvoiceNumberPreview = { nextNumber: string; currentMaxNumber: string; numericSequence: boolean; note: string };
export type InvoiceValidationIssue = { severity: string; field: string; message: string };
export type InvoiceExportCheck = { number: string; exportable: boolean; issues: InvoiceValidationIssue[] };
export type ProductDto = { id: number; code?: string; description?: string; category?: string; price?: number; vat?: number; companyId?: number; basePrice?: number; newPrice?: number; oldPrice?: number; priceValidFrom?: string; oldVat?: number; vatValidFrom?: string; validFrom?: string; validUntil?: string; available?: boolean; effectiveNote?: string };
export type InvoiceCompany = { id: number; code?: string; name?: string; address?: string; street?: string; city?: string; email?: string; iban?: string; bic?: string; accountHolder?: string; taxNumber?: string; vatId?: string; logoId?: number; logoUrl?: string };
export type InvoiceCreateLineRequest = { productId?: number; quantity?: number; price?: number; vat?: number; branchId?: number; client?: string; performer?: string };
export type InvoiceRecipientRequest = { manual?: boolean; salutation?: string; title?: string; firstName?: string; lastName?: string; nameSuffix?: string; street?: string; postalCode?: string; city?: string; country?: string; email?: string; patientNumber?: string; lbdFile?: string };
export type InvoiceCreateRequest = { number?: string; invoiceDate?: string; treatmentDate?: string; companyId?: number; addressId?: number; childAddressId?: number; firmAddressId?: number; branchId?: number; paymentMethod?: string; reason?: string; remark?: string; creditNote?: boolean; cancelled?: boolean; paymentAdvice?: boolean; couponText?: string; couponAmount?: number; discountType?: string; discountValue?: number; installments?: number; lbdFile?: string; recipient?: InvoiceRecipientRequest; lines: InvoiceCreateLineRequest[] };
export type InvoiceTotals = { net: number; vat: number; gross: number; vatByRate: Record<string, number> };
export type InvoiceCreateResponse = { number: string; detailId?: number; totals: InvoiceTotals; invoice: InvoiceDetail };
export type InvoiceUpdateRequest = Omit<InvoiceCreateRequest, "number">;
export type InvoiceStatusUpdateRequest = { cancelled?: boolean; creditNote?: boolean; paymentAdvice?: boolean; reason?: string };
export type InvoiceDraft = { suggestedNumber: string; invoiceDate: string; companies: InvoiceCompany[]; lbdRecipient: LbdRecipient; totals: InvoiceTotals };
export type InvoiceTextPreview = { language:string; companyId?:number; documentTitle?:string; salutation:string; invoiceText:string; lawHint:string; greetings:string; labels?: Record<string,string>; recipient?: LbdRecipient; treatmentDate?:string; paymentMethod?:string };
export type LbdRecipient = { found: boolean; file?: string; patientNumber?: string; nameSuffix?: string; lastName?: string; firstName?: string; birthDate?: string; title?: string; insuranceNumber?: string; postalCode?: string; city?: string; country?: string; street?: string; insuranceType?: string; salutationIndex?: number; salutation?: string; rawFields: Record<string,string> };

const API = import.meta.env.VITE_GAM_API ?? "http://localhost:8080/api";
export function apiBase() { return API; }
export function token() { return localStorage.getItem("gam_token") ?? ""; }
export function setToken(value: string) { localStorage.setItem("gam_token", value); }
export function logout() { localStorage.removeItem("gam_token"); localStorage.removeItem("gam_user"); }

function isPublicAuthPath(path: string) {
  return path.startsWith("/setup/")
    || path.startsWith("/auth/login")
    || path.startsWith("/auth/totp/")
    || path.startsWith("/auth/passkey/");
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const sendToken = token() && !isPublicAuthPath(path);
  const res = await fetch(`${API}${path}`, {
    ...options,
    headers: {"Content-Type": "application/json", ...(sendToken ? {Authorization: `Bearer ${token()}`} : {}), ...(options.headers ?? {})}
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
export const loadProducts = (q = "", limit = 50, invoiceDate = "") => request<ProductDto[]>(`/invoices/products?q=${encodeURIComponent(q)}&limit=${limit}${invoiceDate ? `&invoiceDate=${encodeURIComponent(invoiceDate)}` : ""}`);
export const loadInvoiceTextPreview = (companyId?: number, lang = "de", treatmentDate = "", lbdFile = "", invoiceNumber = "") => { const p = new URLSearchParams(); if (companyId) p.set("companyId", String(companyId)); p.set("lang", lang); if (treatmentDate) p.set("treatmentDate", treatmentDate); if (lbdFile) p.set("lbdFile", lbdFile); if (invoiceNumber) p.set("invoiceNumber", invoiceNumber); return request<InvoiceTextPreview>(`/invoices/text-preview?${p}`); };
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
export const pdfUrl = (number: string, lang = "de", companyId?: number) => { const p = new URLSearchParams(); p.set("lang", lang); if (companyId) p.set("companyId", String(companyId)); return `${API}/invoices/${encodeURIComponent(number)}/pdf?${p}`; };
export const zugferdXmlUrl = (number: string, companyId?: number) => `${API}/invoices/${encodeURIComponent(number)}/zugferd.xml${companyId ? `?companyId=${companyId}` : ""}`;
export type InvoiceAccessInfo = { token:string; url:string; invoiceNumber:string; companyId?:number };
export const loadInvoiceAccess = (number:string, companyId?:number) => request<InvoiceAccessInfo>(`/invoices/${encodeURIComponent(number)}/access${companyId ? `?companyId=${companyId}` : ""}`);
export const invoiceAccessQrUrl = (number:string, companyId?:number) => `${API}/invoices/${encodeURIComponent(number)}/access-qr${companyId ? `?companyId=${companyId}` : ""}${token() ? `${companyId ? "&" : "?"}t=${encodeURIComponent(token())}` : ""}`;
export type ZugferdStatus = { enabled: boolean; profile: string; validationEnabled: boolean; note: string };
export const loadZugferdStatus = () => request<ZugferdStatus>("/invoices/zugferd/status");
export type RoleDto = { key: string; label: string; administrative: boolean; modules: string[] };
export type AccountAdminDto = { id: number; username: string; fullname?: string; role?: string; email?: string; twoFactorConfigured: boolean; passwordPresent: boolean };
export type AccountUpdateRequest = { fullname?: string; role?: string; email?: string; secretkey?: string };
export type AccountCreateRequest = { username:string; password?:string; fullname?:string; role?:string; email?:string };
export type PermissionAccessDto = { id:number; username:string; application:string; companyId?:number; role?:string };
export type PermissionApplicationDto = { application:string; selectable?:boolean };
export type PermissionAccessRequest = { username:string; application:string; companyId?:number|null; role?:string };
export const loadMenu = () => request<RoleDto>("/auth/menu");
export const loadRoles = () => request<RoleDto[]>("/admin/roles");
export const loadAccounts = (q = "", limit = 100) => request<AccountAdminDto[]>(`/admin/accounts?q=${encodeURIComponent(q)}&limit=${limit}`);
export const updateAccount = (id: number, payload: AccountUpdateRequest) => request<AccountAdminDto>(`/admin/accounts/${id}`, {method:"PATCH", body: JSON.stringify(payload)});
export const createAccount = (payload: AccountCreateRequest) => request<AccountAdminDto>("/admin/accounts", {method:"POST", body: JSON.stringify(payload)});
export const updateAccountPassword = (id: number, password: string) => request<AccountAdminDto>(`/admin/accounts/${id}/password`, {method:"PATCH", body: JSON.stringify({password})});
export const deleteAccount = (id: number) => request<void>(`/admin/accounts/${id}`, {method:"DELETE"});
export const loadPermissionAccess = (q = "", limit = 200) => request<PermissionAccessDto[]>(`/admin/permissions?q=${encodeURIComponent(q)}&limit=${limit}`);
export const loadPermissionApplications = () => request<PermissionApplicationDto[]>("/admin/permissions/applications");
export const createPermissionAccess = (payload: PermissionAccessRequest) => request<PermissionAccessDto>("/admin/permissions", {method:"POST", body: JSON.stringify(payload)});
export const updatePermissionAccess = (id:number, payload: PermissionAccessRequest) => request<PermissionAccessDto>(`/admin/permissions/${id}`, {method:"PATCH", body: JSON.stringify(payload)});
export const deletePermissionAccess = (id:number) => request<void>(`/admin/permissions/${id}`, {method:"DELETE"});


export type InventoryDevice = { id:number; source:string; name?:string; type?:string; serialNumber?:string; inventoryNumber?:string; manufacturer?:string; ip?:string; location?:string; branchId?:number; branchCode?:string; branchName?:string; medicalDevice?:boolean; electricalDevice?:boolean; inventoryRelevant?:boolean; active?:boolean; inUse?:boolean; acquiredAt?:string; note?:string };
export type DeviceAssignment = { id:number; branchId?:number; branchCode?:string; branchName?:string; companyId?:number; companyName?:string };
export type DeviceConsumable = { id:number; materialId?:number; name?:string; properties?:string; amount?:number; manufacturerEmail?:string };
export type DeviceSoftware = { id:number; name?:string; workplaceId?:number };
export type InventoryDeviceDetail = { device: InventoryDevice; assignments: DeviceAssignment[]; consumables: DeviceConsumable[]; software: DeviceSoftware[]; identityKey?: string; platform?: string; platformStatus?: PlatformInventoryStatusRow[]; discoveryProtocol?: string };
export type InventoryStats = { legacyDeviceCount:number; newDeviceCount:number; branchAssignmentCount:number; medicalDeviceCount:number; electricalDeviceCount:number; outOfServiceCount:number };
export const loadInventoryDevices = (q = "", source = "all", activeOnly = false, limit = 100) => request<InventoryDevice[]>(`/inventory/devices?q=${encodeURIComponent(q)}&source=${encodeURIComponent(source)}&activeOnly=${activeOnly}&limit=${limit}`);
export const loadInventoryDevice = (source: string, id: number) => request<InventoryDeviceDetail>(`/inventory/devices/${encodeURIComponent(source)}/${id}`);
export const loadInventoryStats = () => request<InventoryStats>("/inventory/stats");

export type WarehouseItem = { id:number; kind:string; name?:string; description?:string; location?:string; quantity?:number; properties?:string; manufacturerEmail?:string; linkedDeviceCount?:number };
export type WarehouseItemRequest = { kind?:string; name?:string; description?:string; location?:string; quantity?:number; properties?:string; manufacturerEmail?:string };
export type WarehouseStats = { storageItemCount:number; consumableCount:number; consumablesWithStock:number; consumablesWithoutStock:number; deviceConsumableLinks:number; totalStorageQuantity:number; totalConsumableQuantity:number };
export type StockChangeRequest = { kind?:string; id?:number; quantity:number; reason?:string };
export const loadWarehouseItems = (q = "", kind = "all", onlyWithStock = false, limit = 100) => request<WarehouseItem[]>(`/warehouse/items?q=${encodeURIComponent(q)}&kind=${encodeURIComponent(kind)}&onlyWithStock=${onlyWithStock}&limit=${limit}`);
export const loadWarehouseItem = (kind: string, id: number) => request<WarehouseItem>(`/warehouse/items/${encodeURIComponent(kind)}/${id}`);
export const loadWarehouseStats = () => request<WarehouseStats>("/warehouse/stats");
export const createWarehouseItem = (kind: string, payload: WarehouseItemRequest) => request<WarehouseItem>(`/warehouse/items/${encodeURIComponent(kind)}`, {method:"POST", body: JSON.stringify({...payload, kind})});
export const updateWarehouseItem = (kind: string, id: number, payload: WarehouseItemRequest) => request<WarehouseItem>(`/warehouse/items/${encodeURIComponent(kind)}/${id}`, {method:"PUT", body: JSON.stringify({...payload, kind})});
export const deleteWarehouseItem = (kind: string, id: number) => request<void>(`/warehouse/items/${encodeURIComponent(kind)}/${id}`, {method:"DELETE"});
export const updateWarehouseStock = (kind: string, id: number, quantity: number, reason = "") => request<WarehouseItem>(`/warehouse/items/${encodeURIComponent(kind)}/${id}/stock`, {method:"PATCH", body: JSON.stringify({kind,id,quantity,reason})});


export type ModuleOverview = { key:string; label:string; tableName:string; count:number; status:string; note:string };
export type ModuleRecord = { module:string; values:Record<string, unknown> };
export const loadGamModules = () => request<ModuleOverview[]>('/gam/modules');
export const loadGamTasks = (limit=100) => request<ModuleRecord[]>(`/gam/tasks?limit=${limit}`);
export const loadGamApprovals = (limit=100) => request<ModuleRecord[]>(`/gam/approvals?limit=${limit}`);
export const loadGamPersonnel = (limit=150) => request<ModuleRecord[]>(`/gam/personnel?limit=${limit}`);
export const loadGamCashbook = (limit=100) => request<ModuleRecord[]>(`/gam/cashbook?limit=${limit}`);

export type WorkflowTask = { id?:number; username?:string; date?:string; branchCode?:string; branchId?:number; department?:string; task?:string; responsible?:string; priority?:string; status?:string; dueDate?:string; doneBy?:string; note?:string };
export type WorkflowTaskRequest = { username?:string; branchCode?:string; branchId?:number; department?:string; task?:string; responsible?:string; priority?:string; status?:string; dueDate?:string; doneBy?:string; note?:string };
export type WorkflowApproval = { id?:number; date?:string; creator?:string; description?:string; companyId?:number; branchId?:number; status?:string; note?:string };
export type WorkflowApprovalRequest = { creator?:string; description?:string; companyId?:number; branchId?:number; status?:string; note?:string };
export type WorkflowStats = { tasksOpen:number; tasksDone:number; approvalsOpen:number; approvalsDone:number };
export const loadWorkflowTasks = (q='', status='all', branchId?:number, limit=150) => { const p = new URLSearchParams(); p.set('q', q); p.set('status', status); p.set('limit', String(limit)); if(branchId) p.set('branchId', String(branchId)); return request<WorkflowTask[]>(`/workflow/tasks?${p}`); };
export const createWorkflowTask = (payload: WorkflowTaskRequest) => request<WorkflowTask>('/workflow/tasks', {method:'POST', body: JSON.stringify(payload)});
export const updateWorkflowTask = (id:number, payload: WorkflowTaskRequest) => request<WorkflowTask>(`/workflow/tasks/${id}`, {method:'PUT', body: JSON.stringify(payload)});
export const deleteWorkflowTask = (id:number) => request<void>(`/workflow/tasks/${id}`, {method:'DELETE'});
export const loadWorkflowApprovals = (q='', status='all', branchId?:number, limit=150) => { const p = new URLSearchParams(); p.set('q', q); p.set('status', status); p.set('limit', String(limit)); if(branchId) p.set('branchId', String(branchId)); return request<WorkflowApproval[]>(`/workflow/approvals?${p}`); };
export const createWorkflowApproval = (payload: WorkflowApprovalRequest) => request<WorkflowApproval>('/workflow/approvals', {method:'POST', body: JSON.stringify(payload)});
export const updateWorkflowApproval = (id:number, payload: WorkflowApprovalRequest) => request<WorkflowApproval>(`/workflow/approvals/${id}`, {method:'PUT', body: JSON.stringify(payload)});
export const deleteWorkflowApproval = (id:number) => request<void>(`/workflow/approvals/${id}`, {method:'DELETE'});
export const loadWorkflowStats = () => request<WorkflowStats>('/workflow/stats');
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


export type TtsStatus = { enabled: boolean; engine: string; mode: string; piperAvailable: boolean; piperAvailabilityMessage?: string; piperDiagnostics?: Record<string, unknown>; piperExecutable: string; piperVoicesDir: string; piperVoices: Record<string,string>; languageLabels?: Record<string,string>; installedLanguages: string[]; supportedLanguages?: string[]; standardLanguages?: string[]; downloadingLanguages?: string[]; downloadErrors?: Record<string,string>; autoDownload?: boolean; browserFallback: boolean; fallback: Record<string, string[]> };
export const loadTtsStatus = () => request<TtsStatus>('/tts/status');
export const installTtsLanguage = (language: string) => request<Record<string, unknown>>(`/tts/install/${encodeURIComponent(language)}`, {method:'POST'});
export async function loadTtsAudio(language: string, text: string, engine = 'piper', signal?: AbortSignal) {
  const res = await fetch(`${API}/tts/audio`, {
    method: 'POST',
    headers: {"Content-Type": "application/json", ...(token() ? {Authorization: `Bearer ${token()}`} : {})},
    body: JSON.stringify({language, text, engine}),
    signal
  });
  if (!res.ok) throw new Error('TTS audio unavailable');
  return await res.blob();
}

export type UiTranslationRequest = { language: string; entries: Record<string, string>; knownTranslations?: Record<string, string> };
export const loadUiTranslations = (language: string, entries: Record<string, string>, knownTranslations?: Record<string, string>) =>
  request<Record<string, string>>("/ui-translations", {method:"POST", body: JSON.stringify({language, entries, knownTranslations: knownTranslations ?? {}})});

export const loadUiTranslationsLive = (language: string, entries: Record<string, string>) =>
  request<Record<string, string>>("/ui-translations/live", {method:"POST", body: JSON.stringify({language, entries, knownTranslations: {}})});

// Schritt 38: Administrationsbereiche außerhalb des Rechnungsmoduls
export type MasterDataCatalog = { key:string; label:string; module:string; tableName:string; primaryKey:string; fields:string[]; searchFields:string[]; note?:string };
export type MasterDataRows = { catalog: MasterDataCatalog; rows: Record<string, unknown>[] };
export const loadMasterDataCatalogs = () => request<MasterDataCatalog[]>('/gam/admin/masterdata/catalogs');
export const loadMasterDataRows = (key:string, q='', limit=150) => request<MasterDataRows>(`/gam/admin/masterdata/${encodeURIComponent(key)}?q=${encodeURIComponent(q)}&limit=${limit}`);
export const createMasterDataRow = (key:string, payload:Record<string, unknown>) => request<Record<string, unknown>>(`/gam/admin/masterdata/${encodeURIComponent(key)}`, {method:'POST', body: JSON.stringify(payload)});
export const updateMasterDataRow = (key:string, id:number|string, payload:Record<string, unknown>) => request<Record<string, unknown>>(`/gam/admin/masterdata/${encodeURIComponent(key)}/${encodeURIComponent(String(id))}`, {method:'PUT', body: JSON.stringify(payload)});
export const deleteMasterDataRow = (key:string, id:number|string) => request<Record<string, unknown>>(`/gam/admin/masterdata/${encodeURIComponent(key)}/${encodeURIComponent(String(id))}`, {method:'DELETE'});

// Schritt 38d: Geräteverzeichnis vollständig
export type InventoryBranchOption = { id:number; code?:string; name?:string };
export type InventoryCompanyOption = { id:number; name?:string };
export type InventoryMaterialOption = { id:number; name?:string; properties?:string; stock?:number; manufacturerEmail?:string };
export type InventoryDeviceUpdateRequest = { name?:string; type?:string; serialNumber?:string; inventoryNumber?:string; manufacturer?:string; ip?:string; location?:string; branchId?:number; medicalDevice?:boolean; electricalDevice?:boolean; inventory?:boolean; active?:boolean; inUse?:boolean; acquisitionDate?:string; note?:string };
export const loadInventoryBranches = () => request<InventoryBranchOption[]>('/inventory/branches');
export const loadInventoryCompanies = () => request<InventoryCompanyOption[]>('/inventory/companies');
export const loadInventoryMaterials = (q='', limit=200) => request<InventoryMaterialOption[]>(`/inventory/materials?q=${encodeURIComponent(q)}&limit=${limit}`);
export const createInventoryDevice = (payload: InventoryDeviceUpdateRequest) => request<InventoryDeviceDetail>('/inventory/devices/new', {method:'POST', body: JSON.stringify(payload)});
export const updateInventoryDevice = (source:string, id:number, payload: InventoryDeviceUpdateRequest) => request<InventoryDeviceDetail>(`/inventory/devices/${encodeURIComponent(source)}/${id}`, {method:'PUT', body: JSON.stringify(payload)});
export const deleteInventoryDevice = (id:number) => request<Record<string, unknown>>(`/inventory/devices/new/${id}`, {method:'DELETE'});
export const setInventoryDeviceAssignment = (id:number, branchId?:number, companyId?:number) => request<InventoryDeviceDetail>(`/inventory/devices/new/${id}/assignments`, {method:'POST', body: JSON.stringify({branchId, companyId})});
export const addInventoryDeviceMaterial = (id:number, materialId:number) => request<InventoryDeviceDetail>(`/inventory/devices/new/${id}/materials`, {method:'POST', body: JSON.stringify({materialId})});
export const removeInventoryDeviceMaterial = (id:number, linkId:number) => request<InventoryDeviceDetail>(`/inventory/devices/new/${id}/materials/${linkId}`, {method:'DELETE'});

// Schritt 38g6: direkter Bestell-E-Mailversand
export type OrderEmailLineRequest = { materialId?:number; name?:string; stock?:number; quantity:number };
export type OrderSmtpConfig = { host?:string; port?:number; username?:string; password?:string; from?:string; fromName?:string; replyTo?:string; startTls?:boolean; ssl?:boolean };
export type OrderEmailRequest = { to:string; subject:string; text:string; draftId?:number; workflowTaskId?:number; lines?:OrderEmailLineRequest[]; smtp?:OrderSmtpConfig };
export type OrderEmailResponse = { sent:boolean; message:string; to:string; subject:string; sentAt:string };
export const sendOrderEmail = (payload: OrderEmailRequest) => request<OrderEmailResponse>('/orders/send-email', {method:'POST', body: JSON.stringify(payload)});

// Schritt 38g7: Kommunikationsassistent und Login-News
export type CommunicationSettings = {
  mailEnabled:boolean;
  smtpHost:string;
  smtpPort:number;
  smtpUsername:string;
  smtpPasswordConfigured:boolean;
  defaultFrom:string;
  defaultRecipient:string;
  defaultSubject:string;
  defaultText:string;
  loginNewsEnabled:boolean;
  loginNewsTitle:string;
  loginNewsText:string;
  loginNewsSeverity:string;
};
export type LoginNews = { enabled:boolean; title:string; text:string; severity:string };
export const loadCommunicationSettings = () => request<CommunicationSettings>('/communication/settings');
export const loadLoginNews = () => request<LoginNews>('/communication/login-news');

// Schritt 39k: Logo-Upload bleibt erhalten; Logos sind normalisierte Stammdaten und können wiederverwendet werden.
export async function uploadInvoiceLogo(file: File): Promise<{url:string; filename:string; previewUrl?:string}> {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(`${API}/gam/admin/masterdata/invoice-logos/upload`, {
    method: 'POST',
    headers: token() ? {Authorization: `Bearer ${token()}`} : {},
    body: form
  });
  if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
  return await res.json();
}

// Schritt 40a: Rechnungsworkflow

export type PaymentWorkflowEntry = { id:number; invoiceNumber:string; companyId?:number; fromStatus?:string; toStatus:string; amount?:number; note?:string; changedBy?:string; changedAt:string };
export type PaymentWorkflowState = { invoiceNumber:string; companyId?:number; status:string; amountDue:number; paidAmount:number; openAmount:number; dueDate?:string; updatedAt:string; updatedBy?:string; allowedActions:string[]; history:PaymentWorkflowEntry[] };
export const loadPaymentWorkflow = (number:string, companyId:number|undefined, amountDue:number, dueDate?:string) => request<PaymentWorkflowState>(`/invoices/${encodeURIComponent(number)}/payment-workflow?${new URLSearchParams({...(companyId?{companyId:String(companyId)}:{}),amountDue:String(amountDue),...(dueDate?{dueDate}:{})})}`);
export const updatePaymentWorkflow = (number:string, companyId:number|undefined, action:string, amount?:number, note?:string, amountDue?:number, dueDate?:string) => request<PaymentWorkflowState>(`/invoices/${encodeURIComponent(number)}/payment-workflow${companyId?`?companyId=${companyId}`:''}`, {method:'POST', body:JSON.stringify({action,amount,note,amountDue,dueDate})});
export type InvoiceWorkflowEntry = { id:number; invoiceNumber:string; companyId?:number; fromStatus?:string; toStatus:string; note?:string; changedBy?:string; changedAt:string };
export type InvoiceWorkflowState = { invoiceNumber:string; companyId?:number; status:string; updatedAt:string; updatedBy?:string; allowedTransitions:string[]; configuredSteps:string[]; settings:InvoiceWorkflowSettings; history:InvoiceWorkflowEntry[] };
export const loadInvoiceWorkflow = (number:string, companyId?:number) => request<InvoiceWorkflowState>(`/invoices/${encodeURIComponent(number)}/workflow${companyId ? `?companyId=${companyId}` : ""}`);
export const transitionInvoiceWorkflow = (number:string, status:string, note="", companyId?:number) => request<InvoiceWorkflowState>(`/invoices/${encodeURIComponent(number)}/workflow/transition${companyId ? `?companyId=${companyId}` : ""}`, {method:"POST", body:JSON.stringify({status,note})});

export type InvoiceWorkflowSettings = {
  reviewEnabled:boolean;
  approvalEnabled:boolean;
  shippingEnabled:boolean;
  autoCompleteAfterShipping:boolean;
};
export const loadInvoiceWorkflowSettings = () => request<InvoiceWorkflowSettings>('/settings/invoice-workflow');
export const saveInvoiceWorkflowSettings = (settings:InvoiceWorkflowSettings) => request<InvoiceWorkflowSettings>('/settings/invoice-workflow', {method:'PUT', body:JSON.stringify(settings)});


export type PaymentWorkflowSettings = {partialPaymentsEnabled:boolean;defaultInstallmentCount:number;installmentIntervalDays:number;minimumInstallmentAmount:number;paymentTermDays:number;reminderEnabled:boolean;reminderDaysAfterDue:number;dunning1Enabled:boolean;dunning1DaysAfterReminder:number;dunning1Fee:number;dunning2Enabled:boolean;dunning2DaysAfterDunning1:number;dunning2Fee:number;dunning3Enabled:boolean;dunning3DaysAfterDunning2:number;dunning3Fee:number;collectionEnabled:boolean;collectionDaysAfterDunning3:number;annualInterestPercent:number;automaticDocumentCreation:boolean;manualApprovalRequired:boolean;publishToPortal:boolean;portalReadAloudEnabled:boolean;automaticEmailDispatch:boolean};
export const loadPaymentWorkflowSettings=()=>request<PaymentWorkflowSettings>('/settings/payment-workflow');
export const savePaymentWorkflowSettings=(settings:PaymentWorkflowSettings)=>request<PaymentWorkflowSettings>('/settings/payment-workflow',{method:'PUT',body:JSON.stringify(settings)});

export type ComplianceWorkflowSettings = {enabled:boolean;warningDays:number;overdueEscalationEnabled:boolean;automaticFollowUpDate:boolean;requireResponsiblePerson:boolean;requireResultNote:boolean;createTaskOnWarning:boolean;createTaskWhenOverdue:boolean;notifyOnWarning:boolean;notifyWhenOverdue:boolean;showStatusInNavigation:boolean;showStatusOnDashboard:boolean};
export const loadComplianceWorkflowSettings=()=>request<ComplianceWorkflowSettings>('/settings/compliance-workflow');
export const saveComplianceWorkflowSettings=(settings:ComplianceWorkflowSettings)=>request<ComplianceWorkflowSettings>('/settings/compliance-workflow',{method:'PUT',body:JSON.stringify(settings)});

export type ModuleSettings = Record<string, boolean>;
export const loadPublicModuleSettings = () => request<ModuleSettings>('/public/module-settings');
export const loadModuleSettings = () => request<ModuleSettings>('/settings/modules');
export const saveModuleSettings = (settings:ModuleSettings) => request<ModuleSettings>('/settings/modules', {method:'PUT', body:JSON.stringify(settings)});

export type PaymentDocument = { id:number; invoiceNumber:string; companyId?:number; documentType:string; title:string; language:string; createdAt:string; createdBy?:string };
export const loadPaymentDocuments = (number:string, companyId?:number) => request<PaymentDocument[]>(`/invoices/${encodeURIComponent(number)}/payment-workflow/documents${companyId?`?companyId=${companyId}`:''}`);
export const createPaymentDocument = (number:string, companyId:number|undefined, type:string, lang='de', repeat=false) => request<PaymentDocument>(`/invoices/${encodeURIComponent(number)}/payment-workflow/documents/${encodeURIComponent(type)}?${new URLSearchParams({...(companyId?{companyId:String(companyId)}:{}),lang,repeat:String(repeat)})}`, {method:'POST'});
export const paymentDocumentPdfUrl = (number:string, id:number, companyId?:number, lang='de') => `${API}/invoices/${encodeURIComponent(number)}/payment-workflow/documents/${id}/pdf?${new URLSearchParams({...(companyId?{companyId:String(companyId)}:{}),lang})}`;
export async function openPaymentDocumentPdf(number:string, id:number, companyId?:number, lang='de'){ const target=window.open('about:blank','_blank'); const path=`/invoices/${encodeURIComponent(number)}/payment-workflow/documents/${id}/pdf?${new URLSearchParams({...(companyId?{companyId:String(companyId)}:{}),lang})}`; try{ const res=await fetch(`${API}${path}`,{headers:{Authorization:`Bearer ${token()}`}}); if(!res.ok){const text=await res.text(); throw new Error(text||`HTTP ${res.status}`);} const blob=await res.blob(); const url=URL.createObjectURL(blob); if(target) target.location.href=url; else window.location.href=url; window.setTimeout(()=>URL.revokeObjectURL(url),60000); }catch(e){if(target)target.close();throw e;} }

export type MarketingWorkflowSettings={enabled:boolean;scannerEnabled:boolean;warehouseCheckEnabled:boolean;offerReorder:boolean;receiptConfirmationRequired:boolean;automaticStockUpdate:boolean;createTasks:boolean;showOnDashboard:boolean;defaultQuantityPerBranch:number;warningThreshold:number;createPackingList:boolean;createDistributionProtocol:boolean;scanSoundEnabled:boolean;errorSoundEnabled:boolean;scanSoundVolume:number;scanSoundDurationMs:number;scanSoundFrequencyHz:number;errorSoundFrequencyHz:number};
export const loadMarketingWorkflowSettings=()=>request<MarketingWorkflowSettings>('/settings/marketing-workflow');
export const saveMarketingWorkflowSettings=(settings:MarketingWorkflowSettings)=>request<MarketingWorkflowSettings>('/settings/marketing-workflow',{method:'PUT',body:JSON.stringify(settings)});
export type MarketingCampaign={id:number;name:string;actionTypeId?:number;actionType?:string;materialTypeId?:number;materialType?:string;materialName:string;materialCode?:string;sourceWarehouseId?:number;sourceWarehouse?:string;targetBranchId?:number;targetBranch:string;plannedQuantity:number;scannedQuantity:number;status:string;receiptConfirmed:boolean;note?:string;createdAt:string;updatedAt:string};
export type MarketingReference={id:number;name:string;typeId?:number};
export type MarketingReferences={branches:MarketingReference[];warehouses:MarketingReference[];warehouseTypes:MarketingReference[];actionTypes:MarketingReference[];materialTypes:MarketingReference[]};
export const loadMarketingReferences=()=>request<MarketingReferences>('/marketing/references');
export const loadMarketingCampaigns=()=>request<MarketingCampaign[]>('/marketing/campaigns');
export const createMarketingCampaign=(v:{name:string;actionTypeId:number;materialTypeId:number;materialName:string;materialCode?:string;sourceWarehouseId:number;targetBranchId:number;plannedQuantity:number;note?:string})=>request<MarketingCampaign>('/marketing/campaigns',{method:'POST',body:JSON.stringify(v)});
export const scanMarketingCampaign=(id:number,code:string,quantity=1)=>request<MarketingCampaign>(`/marketing/campaigns/${id}/scan`,{method:'POST',body:JSON.stringify({code,quantity})});
export const undoLastMarketingScan=(id:number)=>request<MarketingCampaign>(`/marketing/campaigns/${id}/scan/undo-last`,{method:'POST'});
export const transitionMarketingCampaign=(id:number,status:string)=>request<MarketingCampaign>(`/marketing/campaigns/${id}/status/${encodeURIComponent(status)}`,{method:'POST'});
export const loadMarketingCampaignHistory=(id:number)=>request<any[]>(`/marketing/campaigns/${id}/history`);

// Schritt 40e: Kommunikationsworkflow
export type CommunicationWorkflowSettings = {enabled:boolean;requireResponsiblePerson:boolean;requireDueDate:boolean;createTaskForFollowUp:boolean;notifyResponsiblePerson:boolean;showOnDashboard:boolean;warningDays:number;defaultDueDays:number};
export type CommunicationWorkflowItem = {id:number;channel:string;direction:string;sender?:string;recipient?:string;subject:string;message?:string;responsible?:string;priority:string;status:string;dueDate?:string;resultNote?:string;createdBy?:string;createdAt:string;updatedBy?:string;updatedAt:string};
export type CommunicationWorkflowRequest = {channel?:string;direction?:string;sender?:string;recipient?:string;subject:string;message?:string;responsible?:string;priority?:string;status?:string;dueDate?:string;resultNote?:string};
export const loadCommunicationWorkflowSettings=()=>request<CommunicationWorkflowSettings>('/settings/communication-workflow');
export const saveCommunicationWorkflowSettings=(settings:CommunicationWorkflowSettings)=>request<CommunicationWorkflowSettings>('/settings/communication-workflow',{method:'PUT',body:JSON.stringify(settings)});
export const loadCommunicationWorkflowItems=(q='',status='all',responsible='')=>request<CommunicationWorkflowItem[]>(`/communication/workflow?${new URLSearchParams({q,status,responsible})}`);
export const createCommunicationWorkflowItem=(payload:CommunicationWorkflowRequest)=>request<CommunicationWorkflowItem>('/communication/workflow',{method:'POST',body:JSON.stringify(payload)});
export const updateCommunicationWorkflowItem=(id:number,payload:CommunicationWorkflowRequest)=>request<CommunicationWorkflowItem>(`/communication/workflow/${id}`,{method:'PUT',body:JSON.stringify(payload)});
export const deleteCommunicationWorkflowItem=(id:number)=>request<void>(`/communication/workflow/${id}`,{method:'DELETE'});

// Schritt 40f: Aufgabenworkflow
export type TaskWorkflowSettings={enabled:boolean;requireResponsiblePerson:boolean;requireDueDate:boolean;requireCompletionNote:boolean;showOnDashboard:boolean;overdueEscalationEnabled:boolean;defaultDueDays:number;warningDays:number};
export const loadTaskWorkflowSettings=()=>request<TaskWorkflowSettings>('/settings/task-workflow');
export const saveTaskWorkflowSettings=(settings:TaskWorkflowSettings)=>request<TaskWorkflowSettings>('/settings/task-workflow',{method:'PUT',body:JSON.stringify(settings)});

// Schritt 40g: Labormodul und Laborworkflow
export type LaboratoryOrder={id:number;patientNumber?:string;patientName:string;requestedBy?:string;externalLaboratory?:string;examinations:string;specimenMaterial?:string;priority:string;status:string;dueDate?:string;collectedAt?:string;collectedBy?:string;specimenId?:string;sentAt?:string;resultReceivedAt?:string;resultSummary?:string;reviewedBy?:string;reviewedAt?:string;patientInformation?:string;note?:string;createdBy?:string;createdAt:string;updatedBy?:string;updatedAt:string};
export type LaboratoryOrderRequest=Omit<LaboratoryOrder,'id'|'createdAt'|'updatedAt'|'createdBy'|'updatedBy'>;
export const loadLaboratoryOrders=(q='',status='all')=>request<LaboratoryOrder[]>(`/laboratory/orders?${new URLSearchParams({q,status})}`);
export const createLaboratoryOrder=(payload:LaboratoryOrderRequest)=>request<LaboratoryOrder>('/laboratory/orders',{method:'POST',body:JSON.stringify(payload)});
export const updateLaboratoryOrder=(id:number,payload:LaboratoryOrderRequest)=>request<LaboratoryOrder>(`/laboratory/orders/${id}`,{method:'PUT',body:JSON.stringify(payload)});
export const deleteLaboratoryOrder=(id:number)=>request<void>(`/laboratory/orders/${id}`,{method:'DELETE'});


// Schritt 40h: Wartezimmer und Patientenfluss
export type WaitingRoomVisit={id:number;patientNumber?:string;patientName:string;appointmentType?:string;practitioner?:string;room?:string;priority:string;status:string;appointmentAt?:string;arrivedAt?:string;calledAt?:string;treatmentStartedAt?:string;completedAt?:string;nextStep?:string;note?:string;createdBy?:string;createdAt:string;updatedBy?:string;updatedAt:string};
export type WaitingRoomVisitRequest={patientNumber?:string;patientName:string;appointmentType?:string;practitioner?:string;room?:string;priority?:string;status?:string;appointmentAt?:string;nextStep?:string;note?:string};
export const loadWaitingRoomVisits=(q='',status='all')=>request<WaitingRoomVisit[]>(`/waiting-room?${new URLSearchParams({q,status})}`);
export const createWaitingRoomVisit=(payload:WaitingRoomVisitRequest)=>request<WaitingRoomVisit>('/waiting-room',{method:'POST',body:JSON.stringify(payload)});
export const transitionWaitingRoomVisit=(id:number,payload:{status:string;room?:string;practitioner?:string;nextStep?:string;note?:string})=>request<WaitingRoomVisit>(`/waiting-room/${id}/transition`,{method:'POST',body:JSON.stringify(payload)});
export const deleteWaitingRoomVisit=(id:number)=>request<void>(`/waiting-room/${id}`,{method:'DELETE'});

export type FirstRunStatus = {
  required:boolean; databaseExists:boolean; initialized:boolean; accountCount:number; database:string;
  emptyDatabaseAvailable:boolean; demoDatabaseAvailable:boolean; error?:string;
};
export type FirstRunSetupRequest = {
  mode:'empty'|'demo'; language:string; adminUsername:string; adminPassword:string;
  adminName?:string; adminEmail?:string; practiceName?:string; country?:string; timezone?:string;
};
export const loadFirstRunStatus = () => request<FirstRunStatus>("/setup/status");
export const initializeFirstRun = (payload:FirstRunSetupRequest) => request<Record<string,unknown>>("/setup/initialize", {method:"POST", body:JSON.stringify(payload)});

// Schritt 40k33a11: Beispieldaten nach der Ersteinrichtung importieren
export type DemoDatabaseStatus={available:boolean;file:string;accountCount:number;patientCount:number;invoiceCount:number;message:string};
export const loadDemoDatabaseStatus=()=>request<DemoDatabaseStatus>("/database/demo/status");
export const importDemoDatabase=()=>request<Record<string,unknown>>("/database/demo/import",{method:"POST"});

// Schritt 40k: Gerätemanager und Discovery-Grundlage
export type DiscoveredDevice = {
  id:string;
  name:string;
  type:string;
  address?:string;
  hardwareAddress?:string;
  protocol:string;
  status:string;
  manufacturer?:string;
  serialNumber?:string;
  lastSeen?:string;
  alreadyRegistered:boolean;
};
export type DeviceDiscoveryCapabilities = Record<string, boolean|string>;
export const loadDeviceDiscoveryCapabilities = () => request<DeviceDiscoveryCapabilities>('/inventory/discovery/capabilities');
export type BuiltinDiscoverySources=Record<string,boolean>;
export const loadBuiltinDiscoverySources=()=>request<BuiltinDiscoverySources>('/inventory/discovery/builtin-sources');
export const saveBuiltinDiscoverySources=(payload:BuiltinDiscoverySources)=>request<BuiltinDiscoverySources>('/inventory/discovery/builtin-sources',{method:'PUT',body:JSON.stringify(payload)});
export type WinRmDiscoverySettings={enabled:boolean;username:string;passwordConfigured:boolean;port:number;https:boolean};
export type WinRmDiscoveryTest={configured:boolean;reachable:boolean;message:string;checkedAt:string};
export const loadWinRmDiscoverySettings=()=>request<WinRmDiscoverySettings>('/inventory/discovery/winrm');
export const saveWinRmDiscoverySettings=(payload:{enabled:boolean;username:string;password?:string;clearPassword?:boolean;port:number;https:boolean})=>request<WinRmDiscoverySettings>('/inventory/discovery/winrm',{method:'PUT',body:JSON.stringify(payload)});
export const testWinRmDiscovery=(host:string)=>request<WinRmDiscoveryTest>('/inventory/discovery/winrm/test',{method:'POST',body:JSON.stringify({host})});
export type DeviceIdentityMergeSettings={autoMergeThreshold:number;possibleDuplicateThreshold:number;macWeight:number;hardwareSerialWeight:number;snmpSerialWeight:number;deviceIdWeight:number;hostnameWeight:number;ipWeight:number;manufacturerWeight:number;typeWeight:number;twoSourceBonus:number;threeSourceBonus:number;automaticMergeEnabled:boolean;hardConflictsBlockMerge:boolean;ipNeverMergesAlone:boolean};
export const loadDeviceIdentityMergeSettings=()=>request<DeviceIdentityMergeSettings>('/inventory/discovery/merge-settings');
export const saveDeviceIdentityMergeSettings=(payload:DeviceIdentityMergeSettings)=>request<DeviceIdentityMergeSettings>('/inventory/discovery/merge-settings',{method:'PUT',body:JSON.stringify(payload)});
export const resetDeviceIdentityMergeSettings=()=>request<DeviceIdentityMergeSettings>('/inventory/discovery/merge-settings/reset',{method:'POST'});
export const scanForDevices = () => request<DiscoveredDevice[]>('/inventory/discovery/scan', {method:'POST'});
export const registerDiscoveredDevice = (device:Pick<DiscoveredDevice,'address'|'name'|'hardwareAddress'|'serialNumber'>) => request<Record<string,unknown>>('/inventory/discovery/register',{method:'POST',body:JSON.stringify(device)});
export const deregisterDiscoveredDevice = (device:Pick<DiscoveredDevice,'address'|'name'|'hardwareAddress'|'serialNumber'>) => request<Record<string,unknown>>('/inventory/discovery/deregister',{method:'POST',body:JSON.stringify(device)});
export const registerAllDiscoveredDevices=(devices:DiscoveredDevice[])=>request<{registered:number;requested:number}>('/inventory/discovery/register-all',{method:'POST',body:JSON.stringify(devices)});
export type RegisteredDiscoveryDevice={identityKey:string;name?:string;deviceType?:string;address?:string;hardwareAddress?:string;serialNumber?:string;manufacturer?:string;protocol?:string;status?:string;firstSeenAt?:string;lastSeenAt?:string;registeredAt?:string;detectionCount?:number;lastScanHits?:number;manualDeviceType?:boolean;manualName?:boolean;adbHost?:string;adbPort?:number;adbLastConnectedAt?:string};
export const loadRegisteredDiscoveryDevices=()=>request<RegisteredDiscoveryDevice[]>('/inventory/discovery/registered');
export const updateRegisteredDeviceType=(identityKey:string,deviceType:string)=>request<RegisteredDiscoveryDevice>('/inventory/discovery/registered/device-type',{method:'PUT',body:JSON.stringify({identityKey,deviceType})});
export const updateRegisteredDeviceName=(identityKey:string,name:string)=>request<RegisteredDiscoveryDevice>('/inventory/discovery/registered/device-name',{method:'PUT',body:JSON.stringify({identityKey,name})});
export const moveRegisteredDeviceToInventory=(identityKey:string)=>request<InventoryDeviceDetail>(`/inventory/discovery/registered/${encodeURIComponent(identityKey)}/inventory`,{method:'POST'});
export const revokeRegisteredDevice=(identityKey:string)=>request<Record<string,unknown>>('/inventory/discovery/registered/revoke',{method:'POST',body:JSON.stringify({identityKey})});
export const revokeAllRegisteredDevices=()=>request<Record<string,unknown>>('/inventory/discovery/registered',{method:'DELETE'});

// 40k33b4: Manuelle Gerätezusammenführung.
export type DeviceMergeFieldRow={label:string;valueA?:string;valueB?:string;differs:boolean};
export type DeviceMergeCandidate={keyA:string;nameA?:string;typeA?:string;addressA?:string;macA?:string;keyB:string;nameB?:string;typeB?:string;addressB?:string;macB?:string;score:number;confidencePercent:number;decision:string;warningLevel:'RED'|'YELLOW'|'GREEN'|string;matchedSignals:string[];conflicts:string[];criticalityLevel?:'KRITISCH'|'HOCH'|'PRUEFHINWEIS'|string;criticalMessage?:string;fieldComparison:DeviceMergeFieldRow[];manualConfirmationRequired:boolean};
export type DeviceMergePreview={targetKey:string;sourceKeys:string[];name?:string;manualName:boolean;deviceType?:string;manualDeviceType:boolean;address?:string;mac?:string;serialNumber?:string;manufacturer?:string;protocol?:string;status?:string;detectionCount:number;lastScanHits:number;macConflicts:string[];macConflictConfirmationRequired:boolean;criticalityLevel?:'KRITISCH'|'HOCH'|'PRUEFHINWEIS'|string;criticalMessage?:string;fieldComparison:DeviceMergeFieldRow[];manualConfirmationRequired:boolean};
export type DeviceMergeLogEntry={id:number;targetIdentityKey:string;mergedIdentityKeys:string;summary?:string;performedBy?:string;performedAt:string};
export const loadDeviceMergeCandidates=()=>request<DeviceMergeCandidate[]>('/inventory/discovery/merge/candidates');
export const previewDeviceMerge=(targetKey:string,sourceKeys:string[],overrides?:Record<string,unknown>)=>request<DeviceMergePreview>('/inventory/discovery/merge/preview',{method:'POST',body:JSON.stringify({targetKey,sourceKeys,overrides:overrides||{}})});
export const confirmDeviceMerge=(targetKey:string,sourceKeys:string[],overrides?:Record<string,unknown>)=>request<RegisteredDiscoveryDevice>('/inventory/discovery/merge/confirm',{method:'POST',body:JSON.stringify({targetKey,sourceKeys,overrides:overrides||{}})});
export const ignoreDeviceMergeCandidate=(keyA:string,keyB:string)=>request<Record<string,unknown>>('/inventory/discovery/merge/ignore',{method:'POST',body:JSON.stringify({keyA,keyB})});
export const loadDeviceMergeLog=(limit=50)=>request<DeviceMergeLogEntry[]>(`/inventory/discovery/merge/log?limit=${limit}`);

// 40k33b5: Geräteidentität, Quellenübersicht und Identitätsverwaltung.
export type DeviceIdentityRow={identityKey:string;name?:string;manualName:boolean;deviceType?:string;manualDeviceType:boolean;category?:string;subcategory?:string;platform?:string;address?:string;hardwareAddress?:string;serialNumber?:string;manufacturer?:string;status?:string;firstSeenAt?:string;lastSeenAt?:string;detectionCount:number;lastScanHits:number;sources:string[];sourceCount:number;aliases:string[];aliasCount:number;mergeCount:number;confidenceLabel:string;confidenceReasons:string[];hasOpenCandidate:boolean;protocol?:string;adbHost?:string;adbPort?:number;adbLastConnectedAt?:string};
export type DeviceIdentityHistoryEntry={timestamp:string;kind:'KATEGORIE'|'ALIAS'|'MERGE'|string;description:string};
export type DeviceIdentityReassessResult={identity:DeviceIdentityRow;openCandidates:DeviceMergeCandidate[]};
export const loadDeviceIdentityOverview=()=>request<DeviceIdentityRow[]>('/inventory/discovery/identity/overview');
export const loadDeviceIdentityDetail=(identityKey:string)=>request<DeviceIdentityRow>(`/inventory/discovery/identity/detail?identityKey=${encodeURIComponent(identityKey)}`);
export const loadDeviceIdentityHistory=(identityKey:string)=>request<DeviceIdentityHistoryEntry[]>(`/inventory/discovery/identity/history?identityKey=${encodeURIComponent(identityKey)}`);

// 40k33b6b: Erweiterte Linux-Analyse (Lazy Loading je einklappbarem Bereich).
export type LinuxOnDemandSection={section:string;label:string;content:string;available:boolean;fetchedAt:string;fromCache:boolean};
export const loadLinuxOnDemandSection=(identityKey:string,section:string)=>request<LinuxOnDemandSection>(`/inventory/discovery/linux/section?identityKey=${encodeURIComponent(identityKey)}&section=${encodeURIComponent(section)}`);

// 40k33b7: Geräteintegritätsprüfung und Wiederauftrennung.
export type IntegrityFieldRow={label:string;currentValue?:string;comparedValue?:string;differs:boolean};
export type IntegrityResult={status:'Integrität hoch'|'Bitte überprüfen'|'Integritätswarnung'|string;reasons:string[];comparisonTable:IntegrityFieldRow[];comparedIdentityKey?:string;checkedAt:string;conflictCount:number;criticalityLevel?:'KRITISCH'|'HOCH'|'PRUEFHINWEIS'|string;criticalMessage?:string;manualConfirmationRequired:boolean};
export const loadDeviceIdentityIntegrity=(identityKey:string)=>request<IntegrityResult>(`/inventory/discovery/identity/integrity?identityKey=${encodeURIComponent(identityKey)}`);
export type SplitCandidate={ref:string;label:string;name?:string;deviceType?:string;address?:string;hardwareAddress?:string;serialNumber?:string;manufacturer?:string;fullSnapshot:boolean};
export const loadDeviceIdentitySplitCandidates=(identityKey:string)=>request<SplitCandidate[]>(`/inventory/discovery/identity/split-candidates?identityKey=${encodeURIComponent(identityKey)}`);
export const splitDeviceIdentity=(identityKey:string,ref:string)=>request<RegisteredDiscoveryDevice>('/inventory/discovery/identity/split',{method:'POST',body:JSON.stringify({identityKey,ref})});

// 40k33b8: Manuelle Linux-Aktionen (gezielt für ein Gerät, ohne kompletten Suchlauf).
export type LinuxActionResult={success:boolean;message:string;identity:DeviceIdentityRow};
export const runLinuxInventory=(identityKey:string)=>request<LinuxActionResult>('/inventory/discovery/identity/linux/inventory',{method:'POST',body:JSON.stringify({identityKey})});
export type LinuxSshTestResult={sshConfigured:boolean;reachable:boolean;message:string;checkedAt:string};
export const testLinuxSsh=(identityKey:string)=>request<LinuxSshTestResult>('/inventory/discovery/identity/linux/ssh-test',{method:'POST',body:JSON.stringify({identityKey})});
export const refreshLinuxCache=(identityKey:string)=>request<{refreshed:boolean}>('/inventory/discovery/identity/linux/refresh-cache',{method:'POST',body:JSON.stringify({identityKey})});

// 40k35i1: Zentrale SSH-Konfiguration für Linux und künftig macOS.
export type LinuxSshSettings={enabled:boolean;username:string;authMode:'KEY'|'PASSWORD'|string;passwordConfigured:boolean;keyPath:string;port:number;password?:string;clearPassword?:boolean};
export const loadLinuxSshSettings=()=>request<LinuxSshSettings>('/inventory/discovery/ssh');
export const saveLinuxSshSettings=(settings:LinuxSshSettings)=>request<LinuxSshSettings>('/inventory/discovery/ssh',{method:'PUT',body:JSON.stringify(settings)});

// 40k34b: ADB-Status, Pairing, Verbindung, Inventarisierung.
export type AdbDevice={serial:string;state:string;transportId?:string;model?:string;product?:string;device?:string;network:boolean};
export type AdbStatus={available:boolean;path?:string;version?:string;serverActive:boolean;devices:AdbDevice[];authorized:number;unauthorized:number;offline:number;otherState:number;checkedAt:string};
export type AdbActionResult={success:boolean;message:string};
export const loadAndroidAdbStatus=()=>request<AdbStatus>('/inventory/discovery/identity/android/adb-status');
export const pairAndroidDevice=(host:string,pairingPort:number,pairingCode:string)=>request<AdbActionResult>('/inventory/discovery/identity/android/pair',{method:'POST',body:JSON.stringify({host,pairingPort,pairingCode})});
export const connectAndroidDevice=(host:string,port:number,identityKey?:string)=>request<AdbActionResult>('/inventory/discovery/identity/android/connect',{method:'POST',body:JSON.stringify({host,port,identityKey})});
export const disconnectAndroidDevice=(host:string,port:number)=>request<AdbActionResult>('/inventory/discovery/identity/android/disconnect',{method:'POST',body:JSON.stringify({host,port})});
export const runAndroidInventory=(identityKey:string)=>request<LinuxActionResult>('/inventory/discovery/identity/android/inventory',{method:'POST',body:JSON.stringify({identityKey})});
export const reconnectKnownAndroidDevices=()=>request<{results:string[]}>('/inventory/discovery/identity/android/reconnect-known',{method:'POST'});

// 40k34c: Android-App-/Paketinventar.
export type InstalledApp={packageName:string;androidUserId?:number;androidUserName?:string;displayName?:string;versionName?:string;versionCode?:number;installTime?:string;updateTime?:string;installerPackage?:string;system:boolean;updatedSystem:boolean;enabled:boolean;debuggable:boolean;testOnly:boolean;minSdk?:number;targetSdk?:number;roles?:string;firstSeenAt:string;lastSeenAt:string;removedAt?:string};
export type AppInventoryRun={id:number;startedAt:string;finishedAt?:string;status:string;appCount:number;userAppCount:number;systemAppCount:number;changesSummary?:string;message?:string};
export type AppInventoryRunResult={success:boolean;status:string;message:string;appCount:number;userAppCount:number;systemAppCount:number;changesSummary?:string};
export const loadAndroidApps=(identityKey:string)=>request<InstalledApp[]>(`/inventory/discovery/identity/android/apps?identityKey=${encodeURIComponent(identityKey)}`);
export const loadAndroidAppRuns=(identityKey:string,limit=10)=>request<AppInventoryRun[]>(`/inventory/discovery/identity/android/apps/runs?identityKey=${encodeURIComponent(identityKey)}&limit=${limit}`);
export const runAndroidAppInventory=(identityKey:string)=>request<AppInventoryRunResult>('/inventory/discovery/identity/android/apps/inventory',{method:'POST',body:JSON.stringify({identityKey})});

// 40k34m: Plattforminventarisierung - Erst-/Nachinventarisierung je Plattform, unabhängig von Discovery.
export type SupportedPlatform='Windows'|'Linux'|'Android'|'macOS'|'iOS';
export type PlatformInventoryResult={success:boolean;status:string;message?:string;platform:string};
export type PlatformInventoryStatusRow={platform:string;status:string;startedAt?:string;finishedAt?:string;message?:string;durationSeconds?:number;triggeredBy?:string};
export const runPlatformInventory=(identityKey:string,platform:SupportedPlatform)=>request<PlatformInventoryResult>('/inventory/discovery/identity/platform-inventory',{method:'POST',body:JSON.stringify({identityKey,platform})});
export const runAllKnownPlatforms=(identityKey:string)=>request<PlatformInventoryResult[]>('/inventory/discovery/identity/platform-inventory/all',{method:'POST',body:JSON.stringify({identityKey})});
export const loadPlatformInventoryStatus=(identityKey:string)=>request<PlatformInventoryStatusRow[]>(`/inventory/discovery/identity/platform-inventory/status?identityKey=${encodeURIComponent(identityKey)}`);

// 40k34f: Aggregierte App-Zusammenfassung für den Android-Report (eine Sammelabfrage).
export type AndroidAppsGlobalSummary={totalApps?:number;deviceCount?:number;userApps?:number;systemApps?:number;disabledApps?:number;updatedSystemApps?:number;installerDistribution:{installer:string;count:number}[]};
export const loadAndroidAppsGlobalSummary=()=>request<AndroidAppsGlobalSummary>('/inventory/discovery/identity/android/apps/global-summary');
export const reassessDeviceIdentity=(identityKey:string)=>request<DeviceIdentityReassessResult>('/inventory/discovery/identity/reassess',{method:'POST',body:JSON.stringify({identityKey})});
export const addDeviceIdentityAlias=(identityKey:string,alias:string)=>request<{identityKey:string;aliases:string[]}>('/inventory/discovery/identity/alias',{method:'POST',body:JSON.stringify({identityKey,alias})});
export const renameDeviceIdentityAlias=(identityKey:string,oldAlias:string,newAlias:string)=>request<{identityKey:string;aliases:string[]}>('/inventory/discovery/identity/alias',{method:'PUT',body:JSON.stringify({identityKey,oldAlias,newAlias})});
export const removeDeviceIdentityAlias=(identityKey:string,alias:string)=>request<{identityKey:string;aliases:string[]}>('/inventory/discovery/identity/alias/remove',{method:'POST',body:JSON.stringify({identityKey,alias})});
export const setDeviceIdentityMainName=(identityKey:string,name:string)=>request<RegisteredDiscoveryDevice>('/inventory/discovery/identity/main-name',{method:'PUT',body:JSON.stringify({identityKey,name})});
export const emptyEntireInventoryToRegistered=()=>request<Record<string,unknown>>('/inventory/devices/empty-to-registered',{method:'POST'});
// 40k34t: Best-Effort-Nachverknuepfung bestehender Gerätebestand-Einträge mit der Discovery-Identität.
export type InventoryIdentityLinkSummary={linked:number;alreadyLinked:number;ambiguous:number;unresolved:number};
export const linkExistingInventoryDevices=()=>request<InventoryIdentityLinkSummary>('/inventory/devices/link-existing',{method:'POST'});
export const removeInventoryDeviceToRegistered=(id:number)=>request<Record<string,unknown>>(`/inventory/devices/new/${id}/remove-from-inventory`,{method:'POST'});

export type DeviceDiscoveryDiagnostic = {sessionId:string;timestamp:string;phase:string;status:'STARTED'|'RUNNING'|'COMPLETED'|'SKIPPED'|'FAILED';message:string;deviceCount:number};
export type DeviceDiscoveryStreamEvent = {type:'cancelled';message:string;timestamp?:string}|{type:'trace';event:string;workerNo?:number;sessionNo?:number;sessionId?:string;timestamp?:string}|{type:'device';device:DiscoveredDevice}|{type:'progress';progress:number;phase:string}|{type:'diagnostic';diagnostic:DeviceDiscoveryDiagnostic}|{type:'registered-count';count:number}|{type:'complete';timestamp?:string}|{type:'stream-closing';timestamp?:string}|{type:'error';message:string;timestamp?:string};
export async function scanForDevicesStreaming(onEvent:(event:DeviceDiscoveryStreamEvent)=>void,onSession?:(sessionId:string)=>void):Promise<void>{
  // 40k30n: Der eigentliche Discovery-Service liefert weiterhin sofortige
  // Callback-Ereignisse. Die Übertragung zum Browser erfolgt nun bewusst per
  // Long-Polling statt als dauerhaft offener SSE-Body. Damit kann kein
  // Servlet-/Proxy-/Security-Puffer die Anzeige bis zum Scanende zurückhalten.
  const clientTrace=`browser-${Date.now()}-${Math.random().toString(36).slice(2,9)}`;
  const trace=(step:string,details:Record<string,unknown>={})=>console.warn('[DISCOVERY-TRACE]',{clientTrace,step,time:new Date().toISOString(),...details});
  const authHeaders=token()?{Authorization:`Bearer ${token()}`}:{ };
  trace('BUTTON_PIPELINE_ENTER');
  trace('SESSION_POST_BEGIN');
  const startResponse=await fetch(`${API}/inventory/discovery/sessions`,{
    method:'POST',headers:{Accept:'application/json','X-GAM-Discovery-Trace':clientTrace,...authHeaders},cache:'no-store'
  });
  if(!startResponse.ok){throw new Error((await startResponse.text())||`HTTP ${startResponse.status}`);}
  const session=await startResponse.json() as {sessionId?:string;sessionNo?:number};
  trace('SESSION_POST_COMPLETE',{httpStatus:startResponse.status,sessionId:session.sessionId,sessionNo:session.sessionNo});
  if(!session.sessionId)throw new Error('Discovery-Session konnte nicht angelegt werden.');
  onSession?.(session.sessionId);

  let after=0;
  let finished=false;
  while(!finished){
    trace('LONG_POLL_BEGIN',{sessionId:session.sessionId,sessionNo:session.sessionNo,after});
    const response=await fetch(`${API}/inventory/discovery/sessions/${encodeURIComponent(session.sessionId)}/events-poll?after=${after}`,{
      method:'GET',headers:{Accept:'application/json','X-GAM-Discovery-Trace':clientTrace,...authHeaders},cache:'no-store'
    });
    if(!response.ok){throw new Error((await response.text())||`HTTP ${response.status}`);}
    const packet=await response.json() as {events?:DeviceDiscoveryStreamEvent[];finished?:boolean;lastEventId?:number};
    const events=Array.isArray(packet.events)?packet.events:[];
    for(let index=0;index<events.length;index++){
      const event=events[index];
      const eventId=Number((event as DeviceDiscoveryStreamEvent&{eventId?:number}).eventId||0);
      if(eventId>after)after=eventId;
      trace('LIVE_EVENT',{eventType:event.type,eventId,sessionId:session.sessionId,sessionNo:session.sessionNo});
      onEvent(event);
      // 40k35: Niemals auf requestAnimationFrame warten. Browser halten RAF in
      // inaktiven Tabs an oder drosseln ihn stark. Der Scan läuft serverseitig
      // weiter und die Ereignisse werden nun auch bei einem Tabwechsel vollständig
      // abgeholt. Nach größeren Paketen geben wir nur per Microtask kurz frei.
      if(index>0&&index%50===0)await Promise.resolve();
    }
    if(typeof packet.lastEventId==='number'&&packet.lastEventId>after)after=packet.lastEventId;
    finished=packet.finished===true;
    if(!finished&&events.length===0){await new Promise<void>(resolve=>setTimeout(resolve,100));}
  }
  trace('LIVE_CHANNEL_FINISHED',{sessionId:session.sessionId,sessionNo:session.sessionNo,lastEventId:after});
}

export const cancelDeviceDiscoverySession=(sessionId:string)=>request<Record<string,unknown>>(`/inventory/discovery/sessions/${encodeURIComponent(sessionId)}/cancel`,{method:'POST'});


// Schritt 40k19: mehrere FRITZ!Box-Identitätsquellen
export type FritzBoxDiscoverySource={id:number;name:string;enabled:boolean;host:string;port:number;username:string;passwordConfigured:boolean;location:string};
export type FritzBoxDiscoverySourceUpdate={name:string;enabled:boolean;host:string;port:number;username:string;password?:string;clearPassword?:boolean;location:string};
export type FritzBoxDiscoveryTest={sourceId:number;sourceName:string;success:boolean;status:string;message:string;host:string;modelName:string;softwareVersion:string;deviceCount:number;activeDeviceCount:number;devicePreview:string[]};
export const loadFritzBoxDiscoverySources=()=>request<FritzBoxDiscoverySource[]>('/inventory/discovery/fritzbox/sources');
export const createFritzBoxDiscoverySource=(payload:FritzBoxDiscoverySourceUpdate)=>request<FritzBoxDiscoverySource>('/inventory/discovery/fritzbox/sources',{method:'POST',body:JSON.stringify(payload)});
export const saveFritzBoxDiscoverySource=(id:number,payload:FritzBoxDiscoverySourceUpdate)=>request<FritzBoxDiscoverySource>(`/inventory/discovery/fritzbox/sources/${id}`,{method:'PUT',body:JSON.stringify(payload)});
export const deleteFritzBoxDiscoverySource=(id:number)=>request<void>(`/inventory/discovery/fritzbox/sources/${id}`,{method:'DELETE'});
export const testFritzBoxDiscoverySource=(id:number)=>request<FritzBoxDiscoveryTest>(`/inventory/discovery/fritzbox/sources/${id}/test`,{method:'POST'});
// Schritt 40k20: mehrere Home-Assistant-Identitätsquellen
export type HomeAssistantDiscoverySource={id:number;name:string;enabled:boolean;baseUrl:string;accessTokenConfigured:boolean;location:string};
export type HomeAssistantDiscoverySourceUpdate={name:string;enabled:boolean;baseUrl:string;accessToken?:string;clearAccessToken?:boolean;location:string};
export type HomeAssistantDiscoveryTest={sourceId:number;sourceName:string;success:boolean;status:string;message:string;baseUrl:string;version:string;deviceCount:number;devicePreview:string[]};
export const loadHomeAssistantDiscoverySources=()=>request<HomeAssistantDiscoverySource[]>('/inventory/discovery/homeassistant/sources');
export const createHomeAssistantDiscoverySource=(payload:HomeAssistantDiscoverySourceUpdate)=>request<HomeAssistantDiscoverySource>('/inventory/discovery/homeassistant/sources',{method:'POST',body:JSON.stringify(payload)});
export const saveHomeAssistantDiscoverySource=(id:number,payload:HomeAssistantDiscoverySourceUpdate)=>request<HomeAssistantDiscoverySource>(`/inventory/discovery/homeassistant/sources/${id}`,{method:'PUT',body:JSON.stringify(payload)});
export const deleteHomeAssistantDiscoverySource=(id:number)=>request<void>(`/inventory/discovery/homeassistant/sources/${id}`,{method:'DELETE'});
export const testHomeAssistantDiscoverySource=(id:number)=>request<HomeAssistantDiscoveryTest>(`/inventory/discovery/homeassistant/sources/${id}/test`,{method:'POST'});
// Schritt 40k21: Smart Life / Tuya Cloud als Identitätsquelle
export type TuyaDiscoverySource={id:number;name:string;enabled:boolean;connectionMode:string;homeAssistantSourceId:number|null;appType:string;region:string;clientId:string;clientSecretConfigured:boolean;userUid:string;accountUsername:string;accountPasswordConfigured:boolean;countryCode:string;appSchema:string;location:string};
export type TuyaDiscoverySourceUpdate={name:string;enabled:boolean;connectionMode:string;homeAssistantSourceId:number|null;appType:string;region:string;clientId:string;clientSecret?:string;clearClientSecret?:boolean;userUid:string;accountUsername:string;accountPassword?:string;clearAccountPassword?:boolean;countryCode:string;appSchema:string;location:string};
export type TuyaDiscoveryTest={sourceId:number;sourceName:string;success:boolean;status:string;message:string;region:string;endpoint:string;userUid:string;apiMethod:string;authenticated:boolean;deviceCount:number;onlineDeviceCount:number;devicePreview:string[];diagnostics:string[]};
export const loadTuyaDiscoverySources=()=>request<TuyaDiscoverySource[]>('/inventory/discovery/tuya/sources');
export const createTuyaDiscoverySource=(payload:TuyaDiscoverySourceUpdate)=>request<TuyaDiscoverySource>('/inventory/discovery/tuya/sources',{method:'POST',body:JSON.stringify(payload)});
export const saveTuyaDiscoverySource=(id:number,payload:TuyaDiscoverySourceUpdate)=>request<TuyaDiscoverySource>(`/inventory/discovery/tuya/sources/${id}`,{method:'PUT',body:JSON.stringify(payload)});
export type TuyaDeleteResult={id:number;deleted:boolean;message:string};
export type TuyaDeleteAllResult={deletedCount:number;message:string};
export const deleteTuyaDiscoverySource=(id:number)=>request<TuyaDeleteResult>(`/inventory/discovery/tuya/sources/${id}`,{method:'DELETE'});
export const deleteAllTuyaDiscoverySources=()=>request<TuyaDeleteAllResult>('/inventory/discovery/tuya/sources',{method:'DELETE'});
export const testTuyaDiscoverySource=(id:number)=>request<TuyaDiscoveryTest>(`/inventory/discovery/tuya/sources/${id}/test`,{method:'POST'});
