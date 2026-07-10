import React, {useEffect, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {Download, FilePlus2, FileText, LogOut, Search, ShieldCheck, UserRound, UsersRound, LayoutDashboard, Package, Warehouse, CheckSquare, ClipboardCheck, BriefcaseBusiness, Landmark, FileBarChart, ClipboardList, KeyRound, QrCode, Smartphone, CalendarDays} from 'lucide-react';
import {QRCodeSVG} from 'qrcode.react';
import {AccountAdminDto, AccountDto, ModuleRecord, InventoryDevice, InventoryDeviceDetail, InventoryStats, WarehouseItem, WarehouseStats, InvoiceCompany, InvoiceCreateLineRequest, InvoiceDetail, InvoiceSummary, LbdRecipient, ProductDto, RoleDto, SystemStatus, calculateInvoice, createInvoice, deleteInvoiceDraft, loadAccounts, createAccount, deleteAccount, updateAccountPassword, loadPermissionAccess, loadPermissionApplications, createPermissionAccess, updatePermissionAccess, deletePermissionAccess, PermissionAccessDto, PermissionApplicationDto, loadCompanies, loadDraft, loadExportCheck, loadGamApprovals, loadGamCashbook, loadGamCompliance, loadGamModules, loadGamNews, loadGamPersonnel, loadGamReportSummary, loadGamTasks, loadInventoryDevice, loadInventoryDevices, loadInventoryStats, loadInventoryBranches, loadInventoryCompanies, loadInventoryMaterials, createInventoryDevice, updateInventoryDevice, deleteInventoryDevice, setInventoryDeviceAssignment, addInventoryDeviceMaterial, removeInventoryDeviceMaterial, loadWarehouseItems, loadWarehouseStats, createWarehouseItem, updateWarehouseItem, deleteWarehouseItem, updateWarehouseStock, loadInvoice, loadInvoices, loadLbdPreview, loadMenu, loadNextInvoiceNumber, loadProducts, loadRoles, loadSystemStatus, login, logout, me, pdfUrl, token, updateAccount, updateInvoice, updateInvoiceStatus, createCancellationInvoice, createCreditNote, createProformaInvoice, zugferdXmlUrl, loadDeviceMaterialLinks, loadMaterialMovements, bookMaterial, setupTotp, confirmTotp, loadPasskeyStatus, passkeyRegisterOptions, passkeyRegisterFinish, passkeyLoginOptions, passkeyLoginFinish, loadInvoiceTextPreview, InvoiceTextPreview, loadInvoiceAccess, invoiceAccessQrUrl, loadInvoiceReportRows, loadInvoiceReportSummary, downloadInvoiceReport, InvoiceReportRow, InvoiceReportSummary, InvoiceTotals, loadTtsAudio, loadTtsStatus, loadUiTranslations, loadUiTranslationsLive, MasterDataCatalog, loadMasterDataCatalogs, loadMasterDataRows, createMasterDataRow, updateMasterDataRow, deleteMasterDataRow, WorkflowTask, WorkflowApproval, WorkflowStats, loadWorkflowTasks, createWorkflowTask, updateWorkflowTask, deleteWorkflowTask, loadWorkflowApprovals, createWorkflowApproval, updateWorkflowApproval, deleteWorkflowApproval, loadWorkflowStats, sendOrderEmail, loadCommunicationSettings, loadLoginNews, CommunicationSettings, LoginNews, uploadInvoiceLogo} from './api/client';
import './style.css';
import { UI_LANGUAGES, normalizeUiLanguage, tUi, iconForModule, moduleKeyFromLabel, germanUiEntries, type UiLanguage } from "./i18n";

const UI_LABELS: Record<string, Record<string, string>> = {
  de: {
    ttsRate: "Geschwindigkeit",
    ttsMary: "Piper",
    ttsBrowser: "Browser/Windows",
    ttsVoiceAuto: "Automatische Stimme",
    ttsVoice: "Stimme",
    ttsEngine: "Vorlesetechnik",
    ttsSettings: "Vorleseinstellungen",
    ttsAuto: "Automatisch",
    ttsLanguage: "Vorlesesprache",
    invoiceTypePaymentAdvice: "Zahlungsavis",
    paymentAdvice: "Zahlungsavis",
    users: "Benutzer/Rechte",
    compliance: "Prüfungen",
    invoicePreviewTitle: "Verbindliche Rechnungsvorschau",
    previewTitle: "Verbindliche Rechnungsvorschau",
    step31ModuleOverviewShort: "Schritt 31 Modulübersicht",
    demoReadOnlyShell: "Modulansicht",
    proformaFailed: "Proforma konnte nicht erstellt werden",
    saveFailed: "Speichern fehlgeschlagen",
    selectAtLeastOneLine: "Bitte mindestens eine Position auswählen.",
    mandatoryZugferd: "ZUGFeRD/Factur-X ist Pflicht-Export.",
    loadingNumber: "wird geladen",
    gross: "Brutto",
    net: "Netto",
    remove: "Entfernen",
    addPosition: "+ Position übernehmen",
    quantity: "Menge",
    product: "Produkt",
    installmentCount: "Ratenanzahl",
    installments: "Ratenzahlung",
    amount: "Betrag",
    percent: "Prozent",
    discountValue: "Rabattwert",
    discountType: "Rabattart",
    discount: "Rabatt",
    voucherAmount: "Gutscheinbetrag",
    voucherText: "Gutscheintext",
    voucher: "Gutschein",
    voucherEnable: "Gutschein",
    discountEnable: "Rabatt",
    installmentsEnable: "Ratenzahlung",
    rateSingular: "Rate",
    ratePlural: "Raten",
    remark: "Bemerkung",
    reason: "Grund",
    paymentMethod: "Zahlungsart",
    paymentCash: "Barzahlung",
    paymentCard: "Kartenzahlung",
    paymentTransfer: "Überweisung",
    paymentUnknown: "unbekannt",
    invoiceDate: "Rechnungsdatum",
    treatmentDate: "Behandlungsdatum",
    serviceDate: "Leistungsdatum",
    dueDate: "Fälligkeitsdatum",
    invoiceCustomerFile: "Kundendatei",
    invoiceUser: "Benutzer",
    invoicePaymentMethod: "Zahlungsart",
    invoiceRecipient: "Rechnungsempfänger",
    language: "Sprache",
    saveInvoice: "Rechnung speichern",
    editChangesSave: "Änderungen speichern",
    cancelInvoice: "Stornorechnung",
    createCreditNote: "Gutschrift erstellen",
    creditNote: "Gutschrift",
    invoiceTypeInvoice: "Rechnung",
    technicalDelete: "Technisch löschen",
    accessLinkError: "Abruflink konnte nicht erzeugt werden",
    qrAltPortal: "QR-Code Rechnungsportal",
    openPortal: "Portal öffnen",
    patientPortalHint: "QR-Code für Patientenabruf mit Sprachwahl und Rechnungshistorie.",
    patientPortalDigital: "Digitales Rechnungsportal",
    zugferdIssues: "ZUGFeRD-Export hat Hinweise",
    zugferdReady: "ZUGFeRD-Export bereit",
    exportCheckLoading: "Exportprüfung wird geladen.",
    xml: "XML",
    zugferdPdf: "ZUGFeRD-PDF", zugferdPdfOpenHtml: "ZUGFeRD-PDF (PDF/UA Test)",
    pleaseSelectInvoice: "Bitte links eine Rechnung auswählen.",
    search: "Suchen",
    searchPlaceholder: "Suche Nummer / Name / Grund",
    pleaseChoose: "Bitte wählen",
    pleaseSelectCompany: "Bitte zuerst eine Gesellschaft auswählen.",
    invoiceEdit: "Rechnung bearbeiten",
    usersRights: "Benutzer/Rechte",
    checks: "Prüfungen",
    role: "Rolle",
    logout: "Logout",
    step31ModuleOverview: "Die GAM2-Migration ist abgeschlossen. Alle Module sind verfügbar und für die kommenden Workflow-Erweiterungen vorbereitet.",
    statusLoading: "Status wird geladen.",
    notFound: "nicht gefunden",
    found: "gefunden",
    notConnected: "nicht verbunden",
    connected: "verbunden",
    lbd: ".lbd",
    accounts: "Accounts",
    db: "DB",
    noPasskey: "Für diesen Benutzer ist kein Passkey gespeichert.",
    passkeyUnsupported: "Dieser Browser unterstützt keine Passkeys/WebAuthn.",
    usernameRequired: "Bitte zuerst Benutzernamen eingeben.",
    setupFirst: "Bitte zuerst QR-Code erzeugen.",
    totpSaved: "2FA wurde in accounts.secretkey gespeichert. Du kannst nun den 2FA-Login verwenden.",
    setupCreated: "QR-Code erzeugt. Bitte mit Authenticator scannen und den Code bestätigen.",
    setupExisting: "Es existiert bereits ein 2FA-Secret. Mit Bestätigung wird es ersetzt.",
    seconds: "Sekunden",
    tooManyAttempts: "Zu viele Fehlversuche. Neuer Versuch in",
    wait: "Warten",
    passkeyStatus: "Passkey-Status prüfen",
    passkeyLoginButton: "Mit Passkey anmelden",
    passkeyLoginHint: "Passkey-Login über WebAuthn. Lokal funktioniert das mit localhost.",
    passkeyHint: "Passkey-Testworkflow für localhost/Windows Hello/YubiKey. Für Produktivbetrieb wird die serverseitige WebAuthn-Signaturprüfung noch gehärtet.",
    totpLoginButton: "Mit 2FA anmelden",
    totpCode: "6-stelliger Authenticator-Code",
    totpSave: "2FA speichern",
    totpCodeConfirm: "6-stelliger Code zur Bestätigung",
    totpQrCreate: "QR-Code erzeugen",
    totpRegisterHint: "Für die 2FA-Registrierung wird nur der Benutzername benötigt. Das Passwortfeld ist bewusst ausgeblendet.",
    lbdMissingPlaceholder:'lbd – .lbd-Empfängerdatei wurde nicht gefunden; Export nutzt Platzhalter.', legacyLoginHint: "Kompatibler Login über bestehende accounts-Tabelle.",
    uiLanguage: "Sprache der Oberfläche",
    chooseApplication: "Anwendung wählen",
    moduleVisibilityHint: "GAM2-Plattform vollständig migriert. Alle Module sind verfügbar – die nächste Entwicklungsstufe erweitert GAM um intelligente Workflows.",
    username: "Benutzername",
    password: "Passwort",
    login: "Anmelden",
    passwordLogin: "Passwort",
    totpRegister: "2FA registrieren",
    totpLogin: "2FA-Login",
    passkeyRegister: "Passkey registrieren",
    passkeyLogin: "Passkey-Login",
    invoice: "Rechnungsprogramm",
    invoiceAdmin: "Rechnungsadministration",
    inventory: "Geräteverzeichnis",
    warehouse: "Lagerverwaltung",
    cashbook: "Kassenbuch",
    tasks: "Aufgabenverwaltung",
    approval: "Freigabemanagement",
    orders: "Bestelltool",
    communication: "Kommunikation",
    permissions: "Benutzer/Rechte",
    personnel: "Personaldaten",
    workplace: "Arbeitsplatzausstattung",
    price: "Preisliste",
    reports: "Reports",
    admin: "Administration",
    dashboard: "Dashboard",
    readOnly: "Lesemodus",
    invoiceSearch: "Rechnung suchen",
    newInvoice: "Neue Rechnung",
    invoicePreview: "Rechnungsvorschau",
    invoiceList: "Rechnungsliste",
    company: "Gesellschaft",
    patientPortal: "Patientenportal",
    patients: "Patientenverwaltung",
    appointments: "Terminverwaltung",
    pdfLanguage: "PDF-Sprache",
  },
  en: {
    ttsRate: "Speed",
    ttsMary: "Piper",
    ttsBrowser: "Browser/Windows",
    ttsVoiceAuto: "Automatic voice",
    ttsVoice: "Voice",
    ttsEngine: "Reading engine",
    ttsSettings: "Reading settings",
    ttsAuto: "Automatic",
    ttsLanguage: "Reading language",
    invoiceTypePaymentAdvice: "Payment advice",
    paymentAdvice: "Payment advice",
    users: "Users/permissions",
    compliance: "Checks",
    invoicePreviewTitle: "Binding invoice preview",
    previewTitle: "Binding invoice preview",
    lbdMissingPlaceholder: "lbd – .lbd recipient file was not found; export uses placeholders.",
    step31ModuleOverviewShort: "Step 31 module overview",
    demoReadOnlyShell: "module view",
    proformaFailed: "Proforma could not be created",
    saveFailed: "Saving failed",
    selectAtLeastOneLine: "Please select at least one item.",
    mandatoryZugferd: "ZUGFeRD/Factur-X is the mandatory export.",
    loadingNumber: "loading",
    gross: "Gross",
    net: "Net",
    remove: "Remove",
    addPosition: "+ Add item",
    quantity: "Quantity",
    product: "Product",
    installmentCount: "Number of installments",
    installments: "Installment payment",
    amount: "Amount",
    percent: "Percent",
    discountValue: "Discount value",
    discountType: "Discount type",
    discount: "Discount",
    voucherAmount: "Voucher amount",
    voucherText: "Voucher text",
    voucher: "Voucher",
    voucherEnable: "Voucher",
    discountEnable: "Discount",
    installmentsEnable: "Installments",
    rateSingular: "installment",
    ratePlural: "installments",
    remark: "Remark",
    reason: "Reason",
    paymentMethod: "Payment method",
    paymentCash: "Cash payment",
    paymentCard: "Card payment",
    paymentTransfer: "Bank transfer",
    paymentUnknown: "unknown",
    invoiceDate: "Invoice date",
    saveInvoice: "Save invoice",
    editChangesSave: "Save changes",
    cancelInvoice: "Cancellation invoice",
    createCreditNote: "Create credit note",
    creditNote: "Credit note",
    invoiceTypeInvoice: "Invoice",
    technicalDelete: "Technical delete",
    accessLinkError: "Access link could not be created",
    qrAltPortal: "Invoice portal QR code",
    openPortal: "Open portal",
    patientPortalHint: "QR code for patient access with language selection and invoice history.",
    patientPortalDigital: "Digital invoice portal",
    zugferdIssues: "ZUGFeRD export has notes",
    zugferdReady: "ZUGFeRD export ready",
    exportCheckLoading: "Loading export check.",
    xml: "XML",
    zugferdPdf: "ZUGFeRD PDF", zugferdPdfOpenHtml: "ZUGFeRD PDF (PDF/UA test)",
    pleaseSelectInvoice: "Please select an invoice on the left.",
    search: "Search",
    searchPlaceholder: "Search number / name / reason",
    pleaseChoose: "Please choose",
    pleaseSelectCompany: "Please select a company first.",
    invoiceEdit: "Edit invoice",
    usersRights: "Users/permissions",
    checks: "Checks",
    role: "Role",
    logout: "Logout",
    step31ModuleOverview: "GAM2 migration is complete. All modules are available and ready for the upcoming workflow extensions.",
    statusLoading: "Loading status.",
    notFound: "not found",
    found: "found",
    notConnected: "not connected",
    connected: "connected",
    lbd: ".lbd",
    accounts: "Accounts",
    db: "DB",
    noPasskey: "No passkey is stored for this user.",
    passkeyUnsupported: "This browser does not support passkeys/WebAuthn.",
    usernameRequired: "Please enter the username first.",
    setupFirst: "Please generate the QR code first.",
    totpSaved: "2FA has been saved in accounts.secretkey. You can now use 2FA login.",
    setupCreated: "QR code created. Please scan it with an authenticator and confirm the code.",
    setupExisting: "A 2FA secret already exists. Confirming will replace it.",
    seconds: "seconds",
    tooManyAttempts: "Too many failed attempts. Try again in",
    wait: "Wait",
    passkeyStatus: "Check passkey status",
    passkeyLoginButton: "Sign in with passkey",
    passkeyLoginHint: "Passkey login via WebAuthn. This works locally with localhost.",
    passkeyHint: "Passkey test workflow for localhost/Windows Hello/YubiKey. Server-side WebAuthn signature verification will be hardened for production.",
    totpLoginButton: "Sign in with 2FA",
    totpCode: "6-digit authenticator code",
    totpSave: "Save 2FA",
    totpCodeConfirm: "6-digit confirmation code",
    totpQrCreate: "Generate QR code",
    totpRegisterHint: "Only the username is required to register 2FA. The password field is intentionally hidden.",
    legacyLoginHint: "Compatible login using the existing accounts table.",
    uiLanguage: "Interface language",
    chooseApplication: "Choose application",
    moduleVisibilityHint: "GAM2 platform migration completed. All modules are available – the next development stage adds intelligent workflows.",
    username: "Username",
    password: "Password",
    login: "Sign in",
    passwordLogin: "Password",
    totpRegister: "Register 2FA",
    totpLogin: "2FA login",
    passkeyRegister: "Register passkey",
    passkeyLogin: "Passkey login",
    invoice: "Invoicing",
    invoiceAdmin: "Invoice administration",
    inventory: "Device directory",
    warehouse: "Warehouse",
    cashbook: "Cashbook",
    tasks: "Task management",
    approval: "Approval management",
    orders: "Ordering tool",
    communication: "Communication",
    permissions: "Users/permissions",
    personnel: "Personnel data",
    workplace: "Workplace equipment",
    price: "Price list",
    reports: "Reports",
    admin: "Administration",
    dashboard: "Dashboard",
    readOnly: "Read-only mode",
    invoiceSearch: "Search invoice",
    newInvoice: "New invoice",
    invoicePreview: "Invoice preview",
    invoiceList: "Invoice list",
    company: "Company",
    patientPortal: "Patient portal",
    patients: "Patient management",
    appointments: "Appointments",
    language: "Language",
    pdfLanguage: "PDF language",
  },
  fr: {
    ttsRate: "Vitesse",
    ttsMary: "Piper",
    ttsBrowser: "Navigateur/Windows",
    ttsVoiceAuto: "Voix automatique",
    ttsVoice: "Voix",
    ttsEngine: "Technique de lecture",
    ttsSettings: "Paramètres de lecture",
    ttsAuto: "Automatique",
    ttsLanguage: "Langue de lecture",
    invoiceTypePaymentAdvice: "Avis de paiement",
    paymentAdvice: "Avis de paiement",
    users: "Utilisateurs/droits",
    compliance: "Contrôles",
    invoicePreviewTitle: "Aperçu de facture contraignant",
    previewTitle: "Aperçu de facture contraignant",
    lbdMissingPlaceholder: "lbd – le fichier destinataire .lbd est introuvable ; l’export utilise des valeurs de remplacement.",
    step31ModuleOverviewShort: "Aperçu des modules étape 31",
    demoReadOnlyShell: "module en lecture seule",
    proformaFailed: "La proforma n’a pas pu être créée",
    saveFailed: "Échec de l’enregistrement",
    selectAtLeastOneLine: "Veuillez sélectionner au moins un poste.",
    mandatoryZugferd: "ZUGFeRD/Factur-X est l’export obligatoire.",
    loadingNumber: "chargement",
    gross: "Brut",
    net: "Net",
    remove: "Supprimer",
    addPosition: "+ Ajouter le poste",
    quantity: "Quantité",
    product: "Produit",
    installmentCount: "Nombre d’échéances",
    installments: "Paiement échelonné",
    amount: "Montant",
    percent: "Pourcentage",
    discountValue: "Valeur de remise",
    discountType: "Type de remise",
    discount: "Remise",
    voucherAmount: "Montant du bon",
    voucherText: "Texte du bon",
    voucher: "Bon",
    voucherEnable: "Bon",
    discountEnable: "Remise",
    installmentsEnable: "Paiement échelonné",
    rateSingular: "versement",
    ratePlural: "versements",
    remark: "Remarque",
    reason: "Motif",
    paymentMethod: "Mode de paiement",
    paymentCash: "Paiement en espèces",
    paymentCard: "Paiement par carte",
    paymentTransfer: "Virement bancaire",
    paymentUnknown: "inconnu",
    invoiceDate: "Date de facture",
    saveInvoice: "Enregistrer la facture",
    editChangesSave: "Enregistrer les modifications",
    cancelInvoice: "Facture d’annulation",
    createCreditNote: "Créer un avoir",
    creditNote: "Avoir",
    invoiceTypeInvoice: "Facture",
    technicalDelete: "Suppression technique",
    accessLinkError: "Le lien d’accès n’a pas pu être créé",
    qrAltPortal: "Code QR du portail factures",
    openPortal: "Ouvrir le portail",
    patientPortalHint: "Code QR pour l’accès patient avec choix de langue et historique des factures.",
    patientPortalDigital: "Portail numérique de factures",
    zugferdIssues: "L’export ZUGFeRD contient des remarques",
    zugferdReady: "Export ZUGFeRD prêt",
    exportCheckLoading: "Chargement de la vérification d’export.",
    xml: "XML",
    zugferdPdf: "PDF ZUGFeRD", zugferdPdfOpenHtml: "PDF ZUGFeRD (test PDF/UA)",
    pleaseSelectInvoice: "Veuillez sélectionner une facture à gauche.",
    search: "Rechercher",
    searchPlaceholder: "Recherche numéro / nom / motif",
    pleaseChoose: "Veuillez choisir",
    pleaseSelectCompany: "Veuillez d’abord sélectionner une société.",
    invoiceEdit: "Modifier la facture",
    usersRights: "Utilisateurs/droits",
    checks: "Contrôles",
    role: "Rôle",
    logout: "Déconnexion",
    step31ModuleOverview: "L’étape 31 rend visibles toutes les applications GAM historiques. Les zones entièrement migrées sont utilisables ; les modules encore ouverts sont affichés volontairement comme des modules en lecture seule.",
    statusLoading: "Chargement du statut.",
    notFound: "non trouvé",
    found: "trouvé",
    notConnected: "non connecté",
    connected: "connecté",
    lbd: ".lbd",
    accounts: "Comptes",
    db: "BD",
    noPasskey: "Aucune passkey n’est enregistrée pour cet utilisateur.",
    passkeyUnsupported: "Ce navigateur ne prend pas en charge les passkeys/WebAuthn.",
    usernameRequired: "Veuillez d’abord saisir le nom d’utilisateur.",
    setupFirst: "Veuillez d’abord générer le code QR.",
    totpSaved: "La 2FA a été enregistrée dans accounts.secretkey. Vous pouvez maintenant utiliser la connexion 2FA.",
    setupCreated: "Code QR généré. Veuillez le scanner avec un authenticator et confirmer le code.",
    setupExisting: "Un secret 2FA existe déjà. La confirmation le remplacera.",
    seconds: "secondes",
    tooManyAttempts: "Trop de tentatives échouées. Nouvel essai dans",
    wait: "Attendre",
    passkeyStatus: "Vérifier le statut passkey",
    passkeyLoginButton: "Se connecter avec passkey",
    passkeyLoginHint: "Connexion par passkey via WebAuthn. Cela fonctionne localement avec localhost.",
    passkeyHint: "Workflow de test Passkey pour localhost/Windows Hello/YubiKey. La vérification serveur WebAuthn sera renforcée pour la production.",
    totpLoginButton: "Se connecter avec 2FA",
    totpCode: "Code d’authentification à 6 chiffres",
    totpSave: "Enregistrer 2FA",
    totpCodeConfirm: "Code de confirmation à 6 chiffres",
    totpQrCreate: "Générer le code QR",
    totpRegisterHint: "Seul le nom d’utilisateur est nécessaire pour enregistrer la 2FA. Le champ mot de passe est volontairement masqué.",
    legacyLoginHint: "Connexion compatible via la table accounts existante.",
    uiLanguage: "Langue de l’interface",
    chooseApplication: "Choisir l’application",
    moduleVisibilityHint: "La migration de la plateforme GAM2 est terminée. Tous les modules sont disponibles – la prochaine étape ajoute des workflows intelligents.",
    username: "Nom d’utilisateur",
    password: "Mot de passe",
    login: "Se connecter",
    passwordLogin: "Mot de passe",
    totpRegister: "Enregistrer 2FA",
    totpLogin: "Connexion 2FA",
    passkeyRegister: "Enregistrer une passkey",
    passkeyLogin: "Connexion par passkey",
    invoice: "Facturation",
    invoiceAdmin: "Administration des factures",
    inventory: "Répertoire des appareils",
    warehouse: "Gestion du stock",
    cashbook: "Livre de caisse",
    tasks: "Gestion des tâches",
    approval: "Gestion des validations",
    orders: "Outil de commande",
    communication: "Communication",
    permissions: "Users/permissions",
    personnel: "Données du personnel",
    workplace: "Équipement du poste",
    price: "Liste de prix",
    reports: "Rapports",
    admin: "Administration",
    dashboard: "Tableau de bord",
    readOnly: "Mode lecture seule",
    invoiceSearch: "Rechercher une facture",
    newInvoice: "Nouvelle facture",
    invoicePreview: "Aperçu de la facture",
    invoiceList: "Liste des factures",
    company: "Société",
    patientPortal: "Portail patient",
    language: "Langue",
    pdfLanguage: "Langue du PDF",
  },
  uk: {
    ttsRate: "Швидкість",
    ttsMary: "Piper",
    ttsBrowser: "Браузер/Windows",
    ttsVoiceAuto: "Автоматичний голос",
    ttsVoice: "Голос",
    ttsEngine: "Рушій озвучення",
    ttsSettings: "Налаштування озвучення",
    ttsAuto: "Автоматично",
    ttsLanguage: "Мова озвучення",
    invoiceTypePaymentAdvice: "Платіжне повідомлення",
    paymentAdvice: "Платіжне повідомлення",
    users: "Користувачі/права",
    compliance: "Перевірки",
    invoicePreviewTitle: "Обов’язковий попередній перегляд рахунку",
    previewTitle: "Обов’язковий попередній перегляд рахунку",
    lbdMissingPlaceholder: "lbd – файл одержувача .lbd не знайдено; експорт використовує заповнювачі.",
    step31ModuleOverviewShort: "Огляд модулів кроку 31",
    demoReadOnlyShell: "модуль лише для перегляду",
    proformaFailed: "Не вдалося створити проформу",
    saveFailed: "Не вдалося зберегти",
    selectAtLeastOneLine: "Виберіть принаймні одну позицію.",
    mandatoryZugferd: "ZUGFeRD/Factur-X є обов’язковим експортом.",
    loadingNumber: "завантаження",
    gross: "Брутто",
    net: "Нетто",
    remove: "Видалити",
    addPosition: "+ Додати позицію",
    quantity: "Кількість",
    product: "Продукт",
    installmentCount: "Кількість платежів",
    installments: "Оплата частинами",
    amount: "Сума",
    percent: "Відсоток",
    discountValue: "Значення знижки",
    discountType: "Тип знижки",
    discount: "Знижка",
    voucherAmount: "Сума ваучера",
    voucherText: "Текст ваучера",
    voucher: "Ваучер",
    voucherEnable: "Ваучер",
    discountEnable: "Знижка",
    installmentsEnable: "Оплата частинами",
    rateSingular: "платіж",
    ratePlural: "платежі",
    remark: "Примітка",
    reason: "Причина",
    paymentMethod: "Спосіб оплати",
    paymentCash: "Оплата готівкою",
    paymentCard: "Оплата карткою",
    paymentTransfer: "Банківський переказ",
    paymentUnknown: "невідомо",
    invoiceDate: "Дата рахунку",
    saveInvoice: "Зберегти рахунок",
    editChangesSave: "Зберегти зміни",
    cancelInvoice: "Рахунок скасування",
    createCreditNote: "Створити кредит-ноту",
    creditNote: "Кредит-нота",
    invoiceTypeInvoice: "Рахунок",
    technicalDelete: "Технічно видалити",
    accessLinkError: "Не вдалося створити посилання доступу",
    qrAltPortal: "QR-код порталу рахунків",
    openPortal: "Відкрити портал",
    patientPortalHint: "QR-код для доступу пацієнта з вибором мови та історією рахунків.",
    patientPortalDigital: "Цифровий портал рахунків",
    zugferdIssues: "Експорт ZUGFeRD має примітки",
    zugferdReady: "Експорт ZUGFeRD готовий",
    exportCheckLoading: "Завантаження перевірки експорту.",
    xml: "XML",
    zugferdPdf: "ZUGFeRD PDF", zugferdPdfOpenHtml: "ZUGFeRD PDF (PDF/UA test)",
    pleaseSelectInvoice: "Виберіть рахунок ліворуч.",
    search: "Пошук",
    searchPlaceholder: "Пошук номера / імені / причини",
    pleaseChoose: "Будь ласка, виберіть",
    pleaseSelectCompany: "Спочатку виберіть організацію.",
    invoiceEdit: "Редагувати рахунок",
    usersRights: "Користувачі/права",
    checks: "Перевірки",
    role: "Роль",
    logout: "Вийти",
    step31ModuleOverview: "Крок 31 робить видимими всі історичні застосунки GAM. Повністю перенесені області доступні; відкриті модулі навмисно показані як модулі лише для перегляду.",
    statusLoading: "Завантаження статусу.",
    notFound: "не знайдено",
    found: "знайдено",
    notConnected: "не підключено",
    connected: "підключено",
    lbd: ".lbd",
    accounts: "Облікові записи",
    db: "БД",
    noPasskey: "Для цього користувача passkey не збережено.",
    passkeyUnsupported: "Цей браузер не підтримує passkeys/WebAuthn.",
    usernameRequired: "Спочатку введіть ім’я користувача.",
    setupFirst: "Спочатку створіть QR-код.",
    totpSaved: "2FA збережено в accounts.secretkey. Тепер можна використовувати вхід через 2FA.",
    setupCreated: "QR-код створено. Проскануйте його в автентифікаторі та підтвердьте код.",
    setupExisting: "Секрет 2FA вже існує. Підтвердження замінить його.",
    seconds: "секунд",
    tooManyAttempts: "Забагато невдалих спроб. Повторіть через",
    wait: "Очікувати",
    passkeyStatus: "Перевірити статус passkey",
    passkeyLoginButton: "Увійти через passkey",
    passkeyLoginHint: "Вхід через passkey за допомогою WebAuthn. Локально це працює з localhost.",
    passkeyHint: "Тестовий процес passkey для localhost/Windows Hello/YubiKey. Для продуктивного режиму серверну перевірку WebAuthn буде посилено.",
    totpLoginButton: "Увійти через 2FA",
    totpCode: "6-значний код автентифікатора",
    totpSave: "Зберегти 2FA",
    totpCodeConfirm: "6-значний код підтвердження",
    totpQrCreate: "Створити QR-код",
    totpRegisterHint: "Для реєстрації 2FA потрібне лише ім’я користувача. Поле пароля навмисно приховане.",
    legacyLoginHint: "Сумісний вхід через наявну таблицю accounts.",
    uiLanguage: "Мова інтерфейсу",
    chooseApplication: "Вибрати застосунок",
    moduleVisibilityHint: "Міграцію платформи GAM2 завершено. Усі модулі доступні – наступний етап додасть інтелектуальні робочі процеси.",
    username: "Ім’я користувача",
    password: "Пароль",
    login: "Увійти",
    passwordLogin: "Пароль",
    totpRegister: "Зареєструвати 2FA",
    totpLogin: "Вхід через 2FA",
    passkeyRegister: "Зареєструвати passkey",
    passkeyLogin: "Вхід через passkey",
    invoice: "Рахунки",
    invoiceAdmin: "Адміністрування рахунків",
    inventory: "Каталог пристроїв",
    warehouse: "Склад",
    cashbook: "Касова книга",
    tasks: "Керування завданнями",
    approval: "Керування погодженнями",
    orders: "Інструмент замовлень",
    communication: "Комунікація",
    personnel: "Дані персоналу",
    workplace: "Оснащення робочого місця",
    price: "Прайс-лист",
    reports: "Звіти",
    admin: "Адміністрування",
    dashboard: "Панель",
    readOnly: "Режим перегляду",
    invoiceSearch: "Пошук рахунку",
    newInvoice: "Новий рахунок",
    invoicePreview: "Попередній перегляд рахунку",
    invoiceList: "Список рахунків",
    company: "Організація",
    patientPortal: "Портал пацієнта",
    language: "Мова",
    pdfLanguage: "Мова PDF",
  }
};


// Schritt 34z5:
// LoginDialog-Completion für die vier neuen UI-Sprachen.
// Diese Einträge sind bewusst lokale UI-Fallbacks: Die DB bleibt weiterhin erste
// Quelle, aber deutsche Fehlfüllungen aus alten Caches dürfen den LoginDialog
// nicht mehr sichtbar auf Deutsch zurückfallen lassen.
const LOGIN_DIALOG_COMPLETION_LABELS: Record<string, Record<string, string>> = {
  it: {
    legacyLoginHint: "Accesso compatibile tramite la tabella accounts esistente.",
    uiLanguage: "Lingua dell'interfaccia",
    step31ModuleOverviewShort: "Panoramica moduli passo 31",
    chooseApplication: "Seleziona applicazione",
    moduleVisibilityHint: "La migrazione della piattaforma GAM2 è completata. Tutti i moduli sono disponibili – la prossima fase aggiunge workflow intelligenti.",
    username: "Nome utente",
    password: "Password",
    login: "Accedi",
    passwordLogin: "Password",
    totpRegister: "Registra 2FA",
    totpLogin: "Accesso 2FA",
    passkeyRegister: "Registra passkey",
    passkeyLogin: "Accesso passkey",
    totpRegisterHint: "Per registrare la 2FA è richiesto solo il nome utente. Il campo password è nascosto intenzionalmente.",
    totpQrCreate: "Crea codice QR",
    totpSave: "Salva 2FA",
    totpCode: "Codice Authenticator a 6 cifre",
    totpCodeConfirm: "Codice a 6 cifre per conferma",
    totpLoginButton: "Accedi con 2FA",
    passkeyStatus: "Controlla stato passkey",
    passkeyLoginButton: "Accedi con passkey",
    passkeyHint: "Flusso di test passkey per localhost/Windows Hello/YubiKey. La verifica WebAuthn lato server sarà rafforzata per l'uso produttivo.",
    passkeyLoginHint: "Accesso passkey tramite WebAuthn. In locale funziona con localhost.",
    noPasskey: "Per questo utente non è memorizzata alcuna passkey.",
    passkeyUnsupported: "Questo browser non supporta passkey/WebAuthn.",
    usernameRequired: "Inserisci prima il nome utente.",
    setupFirst: "Crea prima il codice QR.",
    setupCreated: "Codice QR creato. Scansionalo con l'Authenticator e conferma il codice.",
    setupExisting: "Esiste già un segreto 2FA. La conferma lo sostituirà.",
    tooManyAttempts: "Troppi tentativi non riusciti. Nuovo tentativo tra",
    seconds: "secondi",
    wait: "Attendere"
  },
  sv: {
    legacyLoginHint: "Kompatibel inloggning via den befintliga accounts-tabellen.",
    uiLanguage: "Gränssnittsspråk",
    step31ModuleOverviewShort: "Steg 31 modulöversikt",
    chooseApplication: "Välj program",
    moduleVisibilityHint: "Migreringen av GAM2-plattformen är slutförd. Alla moduler är tillgängliga – nästa steg lägger till intelligenta arbetsflöden.",
    username: "Användarnamn",
    password: "Lösenord",
    login: "Logga in",
    passwordLogin: "Lösenord",
    totpRegister: "Registrera 2FA",
    totpLogin: "2FA-inloggning",
    passkeyRegister: "Registrera passkey",
    passkeyLogin: "Passkey-inloggning",
    totpRegisterHint: "För 2FA-registrering krävs endast användarnamnet. Lösenordsfältet är avsiktligt dolt.",
    totpQrCreate: "Skapa QR-kod",
    totpSave: "Spara 2FA",
    totpCode: "6-siffrig Authenticator-kod",
    totpCodeConfirm: "6-siffrig bekräftelsekod",
    totpLoginButton: "Logga in med 2FA",
    passkeyStatus: "Kontrollera passkey-status",
    passkeyLoginButton: "Logga in med passkey",
    passkeyHint: "Testflöde för passkey med localhost/Windows Hello/YubiKey. Serverns WebAuthn-verifiering härdas senare för produktion.",
    passkeyLoginHint: "Passkey-inloggning via WebAuthn. Lokalt fungerar det med localhost.",
    noPasskey: "Ingen passkey är sparad för den här användaren.",
    passkeyUnsupported: "Den här webbläsaren stöder inte passkeys/WebAuthn.",
    usernameRequired: "Ange först användarnamnet.",
    setupFirst: "Skapa först QR-koden.",
    setupCreated: "QR-kod skapad. Skanna den med Authenticator och bekräfta koden.",
    setupExisting: "En 2FA-hemlighet finns redan. Bekräftelse ersätter den.",
    tooManyAttempts: "För många misslyckade försök. Nytt försök om",
    seconds: "sekunder",
    wait: "Vänta"
  },
  tr: {
    legacyLoginHint: "Mevcut accounts tablosu üzerinden uyumlu giriş.",
    uiLanguage: "Arayüz dili",
    step31ModuleOverviewShort: "Adım 31 modül genel bakışı",
    chooseApplication: "Uygulama seç",
    moduleVisibilityHint: "GAM2 platform geçişi tamamlandı. Tüm modüller kullanılabilir – sonraki aşama akıllı iş akışları ekler.",
    username: "Kullanıcı adı",
    password: "Parola",
    login: "Giriş yap",
    passwordLogin: "Parola",
    totpRegister: "2FA kaydet",
    totpLogin: "2FA girişi",
    passkeyRegister: "Passkey kaydet",
    passkeyLogin: "Passkey girişi",
    totpRegisterHint: "2FA kaydı için yalnızca kullanıcı adı gerekir. Parola alanı bilinçli olarak gizlenmiştir.",
    totpQrCreate: "QR kodu oluştur",
    totpSave: "2FA'yı kaydet",
    totpCode: "6 haneli Authenticator kodu",
    totpCodeConfirm: "Onay için 6 haneli kod",
    totpLoginButton: "2FA ile giriş yap",
    passkeyStatus: "Passkey durumunu kontrol et",
    passkeyLoginButton: "Passkey ile giriş yap",
    passkeyHint: "localhost/Windows Hello/YubiKey için passkey test akışı. Sunucu tarafı WebAuthn imza doğrulaması üretim için daha sonra güçlendirilecektir.",
    passkeyLoginHint: "WebAuthn üzerinden passkey girişi. Yerel kullanımda localhost ile çalışır.",
    noPasskey: "Bu kullanıcı için kayıtlı passkey yok.",
    passkeyUnsupported: "Bu tarayıcı passkey/WebAuthn desteklemiyor.",
    usernameRequired: "Lütfen önce kullanıcı adını girin.",
    setupFirst: "Lütfen önce QR kodunu oluşturun.",
    setupCreated: "QR kodu oluşturuldu. Authenticator ile tarayın ve kodu onaylayın.",
    setupExisting: "Zaten bir 2FA gizli anahtarı var. Onaylarsanız değiştirilecektir.",
    tooManyAttempts: "Çok fazla başarısız deneme. Yeni deneme için kalan süre",
    seconds: "saniye",
    wait: "Bekle"
  },
  ru: {
    legacyLoginHint: "Совместимый вход через существующую таблицу accounts.",
    uiLanguage: "Язык интерфейса",
    step31ModuleOverviewShort: "Обзор модулей, шаг 31",
    chooseApplication: "Выберите приложение",
    moduleVisibilityHint: "Миграция платформы GAM2 завершена. Все модули доступны – следующий этап добавит интеллектуальные рабочие процессы.",
    username: "Имя пользователя",
    password: "Пароль",
    login: "Войти",
    passwordLogin: "Пароль",
    totpRegister: "Зарегистрировать 2FA",
    totpLogin: "Вход через 2FA",
    passkeyRegister: "Зарегистрировать passkey",
    passkeyLogin: "Вход через passkey",
    totpRegisterHint: "Для регистрации 2FA требуется только имя пользователя. Поле пароля намеренно скрыто.",
    totpQrCreate: "Создать QR-код",
    totpSave: "Сохранить 2FA",
    totpCode: "6-значный код Authenticator",
    totpCodeConfirm: "6-значный код подтверждения",
    totpLoginButton: "Войти с 2FA",
    passkeyStatus: "Проверить статус passkey",
    passkeyLoginButton: "Войти с passkey",
    passkeyHint: "Тестовый процесс passkey для localhost/Windows Hello/YubiKey. Серверная проверка WebAuthn будет усилена для промышленного использования.",
    passkeyLoginHint: "Вход с passkey через WebAuthn. Локально это работает с localhost.",
    noPasskey: "Для этого пользователя passkey не сохранен.",
    passkeyUnsupported: "Этот браузер не поддерживает passkey/WebAuthn.",
    usernameRequired: "Сначала введите имя пользователя.",
    setupFirst: "Сначала создайте QR-код.",
    setupCreated: "QR-код создан. Отсканируйте его в Authenticator и подтвердите код.",
    setupExisting: "Секрет 2FA уже существует. Подтверждение заменит его.",
    tooManyAttempts: "Слишком много неудачных попыток. Новая попытка через",
    seconds: "секунд",
    wait: "Ждите"
  }
};
Object.entries(LOGIN_DIALOG_COMPLETION_LABELS).forEach(([lang, labels]) => {
  UI_LABELS[lang] = {...(UI_LABELS[lang] ?? {}), ...labels};
});


// Schritt 34z6: Rechnungsprogramm-Completion-Fallbacks.
// Diese lokalen Fallbacks schließen sichtbare UI-Lücken aus den Sprachtest-Exports.
const INVOICE_PROGRAM_COMPLETION_LABELS: Record<string, Record<string, string>> = {
  en: {compliance:"Checks", checks:"Checks", users:"Users/permissions", usersRights:"Users/permissions", newInvoice:"New invoice", invoiceSearch:"Search invoice", invoiceEdit:"Edit invoice", search:"Search", searchPlaceholder:"Search number / name / reason", company:"Company", invoiceTypeInvoice:"Invoice", invoice:"Invoice", paymentAdvice:"Payment advice", invoiceTypePaymentAdvice:"Payment advice", creditNote:"Credit note", cancelInvoice:"Cancellation invoice", cancellation:"Cancellation invoice", createCreditNote:"Create credit note", technicalDelete:"Technical delete", invoicePreviewTitle:"Binding invoice preview", previewTitle:"Binding invoice preview", previewHelp:"This preview should match the later PDF: texts, items, discount/voucher, installments, notices and bank details.", paymentMethod:"Payment method", paymentCash:"Cash payment", paymentCard:"Card payment", paymentTransfer:"Bank transfer", paymentUnknown:"unknown", invoiceDate:"Invoice date", treatmentDate:"Treatment date", serviceDate:"Service date", dueDate:"Due date", saveInvoice:"Save invoice", editChangesSave:"Save changes", addPosition:"+ Add item", quantity:"Quantity", product:"Product", reason:"Reason", remark:"Remark", pdfLanguage:"PDF language", zugferdPdf:"ZUGFeRD PDF", xml:"XML", pleaseSelectCompany:"Please select a company first.", pleaseSelectInvoice:"Please select an invoice on the left.", recipient:"Recipient", date:"Date", language:"Language", payment:"Payment method", bank:"Bank details", taxNo:"Tax/VAT", qty:"Quantity", code:"Code", description:"Description", tax:"VAT", price:"Price", lineTotal:"Total", noLines:"No items have been added yet.", net:"Net", gross:"Gross", total:"Total", remove:"Remove", discount:"Discount", voucher:"Voucher", installments:"Installments", readAloud:"Read invoice aloud", stopReading:"Stop reading", accessibilityNote:"PDF/UA preparation: language, title, metadata and reading order are set."},
  fr: {compliance:"Contrôles", checks:"Contrôles", users:"Utilisateurs/droits", usersRights:"Utilisateurs/droits", newInvoice:"Nouvelle facture", invoiceSearch:"Rechercher une facture", invoiceEdit:"Modifier la facture", search:"Rechercher", searchPlaceholder:"Recherche numéro / nom / motif", company:"Société", invoiceTypeInvoice:"Facture", invoice:"Facture", paymentAdvice:"Avis de paiement", invoiceTypePaymentAdvice:"Avis de paiement", creditNote:"Avoir", cancelInvoice:"Facture d’annulation", cancellation:"Facture d’annulation", createCreditNote:"Créer un avoir", technicalDelete:"Suppression technique", invoicePreviewTitle:"Aperçu de facture contraignant", previewTitle:"Aperçu de facture contraignant", previewHelp:"Cet aperçu doit correspondre au PDF final : textes, postes, remise/bon, paiements échelonnés, avis et coordonnées bancaires.", paymentMethod:"Mode de paiement", paymentCash:"Paiement en espèces", paymentCard:"Paiement par carte", paymentTransfer:"Virement bancaire", paymentUnknown:"inconnu", invoiceDate:"Date de facture", treatmentDate:"Date de traitement", serviceDate:"Date de prestation", dueDate:"Date d’échéance", saveInvoice:"Enregistrer la facture", editChangesSave:"Enregistrer les modifications", addPosition:"+ Ajouter le poste", quantity:"Quantité", product:"Produit", reason:"Motif", remark:"Remarque", pdfLanguage:"Langue du PDF", zugferdPdf:"PDF ZUGFeRD", xml:"XML", pleaseSelectCompany:"Veuillez d’abord sélectionner une société.", pleaseSelectInvoice:"Veuillez sélectionner une facture à gauche.", recipient:"Destinataire", date:"Date", language:"Langue", payment:"Mode de paiement", bank:"Coordonnées bancaires", taxNo:"Fiscal/TVA", qty:"Quantité", code:"Code", description:"Description", tax:"TVA", price:"Prix", lineTotal:"Total", noLines:"Aucun poste n’a encore été ajouté.", net:"Net", gross:"Brut", total:"Total", remove:"Supprimer", discount:"Remise", voucher:"Bon", installments:"Paiement échelonné", readAloud:"Lire la facture", stopReading:"Arrêter la lecture", accessibilityNote:"Préparation PDF/UA : langue, titre, métadonnées et ordre de lecture sont définis."},
  uk: {compliance:"Перевірки", checks:"Перевірки", users:"Користувачі/права", usersRights:"Користувачі/права", newInvoice:"Новий рахунок", invoiceSearch:"Пошук рахунку", invoiceEdit:"Редагувати рахунок", search:"Пошук", searchPlaceholder:"Пошук номера / імені / причини", company:"Організація", invoiceTypeInvoice:"Рахунок", invoice:"Рахунок", paymentAdvice:"Платіжне повідомлення", invoiceTypePaymentAdvice:"Платіжне повідомлення", creditNote:"Кредит-нота", cancelInvoice:"Рахунок скасування", cancellation:"Рахунок скасування", createCreditNote:"Створити кредит-ноту", technicalDelete:"Технічно видалити", invoicePreviewTitle:"Обов’язковий попередній перегляд рахунку", previewTitle:"Обов’язковий попередній перегляд рахунку", previewHelp:"Цей перегляд має відповідати майбутньому PDF: тексти, позиції, знижка/ваучер, оплата частинами, примітки та банківські дані.", paymentMethod:"Спосіб оплати", paymentCash:"Оплата готівкою", paymentCard:"Оплата карткою", paymentTransfer:"Банківський переказ", paymentUnknown:"невідомо", invoiceDate:"Дата рахунку", treatmentDate:"Дата лікування", serviceDate:"Дата послуги", dueDate:"Дата оплати", saveInvoice:"Зберегти рахунок", editChangesSave:"Зберегти зміни", addPosition:"+ Додати позицію", quantity:"Кількість", product:"Продукт", reason:"Причина", remark:"Примітка", pdfLanguage:"Мова PDF", zugferdPdf:"ZUGFeRD PDF", xml:"XML", pleaseSelectCompany:"Спочатку виберіть організацію.", pleaseSelectInvoice:"Виберіть рахунок ліворуч.", recipient:"Одержувач", date:"Дата", language:"Мова", payment:"Спосіб оплати", bank:"Банківські реквізити", taxNo:"Податок/VAT", qty:"Кількість", code:"Код", description:"Опис", tax:"ПДВ", price:"Ціна", lineTotal:"Разом", noLines:"Позиції ще не додано.", net:"Нетто", gross:"Брутто", total:"Разом", remove:"Видалити", discount:"Знижка", voucher:"Ваучер", installments:"Оплата частинами", readAloud:"Зачитати рахунок", stopReading:"Зупинити читання", accessibilityNote:"Підготовка PDF/UA: встановлено мову, назву, метадані та порядок читання."},
  it: {compliance:"Controlli", checks:"Controlli", users:"Utenti/diritti", usersRights:"Utenti/diritti", newInvoice:"Nuova fattura", invoiceSearch:"Cerca fattura", invoiceEdit:"Modifica fattura", search:"Cerca", searchPlaceholder:"Cerca numero / nome / motivo", company:"Società", invoiceTypeInvoice:"Fattura", invoice:"Fattura", paymentAdvice:"Avviso di pagamento", invoiceTypePaymentAdvice:"Avviso di pagamento", creditNote:"Nota di credito", cancelInvoice:"Fattura di storno", cancellation:"Fattura di storno", createCreditNote:"Crea nota di credito", technicalDelete:"Eliminazione tecnica", invoicePreviewTitle:"Anteprima fattura vincolante", previewTitle:"Anteprima fattura vincolante", previewHelp:"Questa anteprima deve corrispondere al PDF finale: testi, posizioni, sconto/buono, rate, avvisi e dati bancari.", paymentMethod:"Metodo di pagamento", paymentCash:"Pagamento in contanti", paymentCard:"Pagamento con carta", paymentTransfer:"Bonifico bancario", paymentUnknown:"sconosciuto", invoiceDate:"Data fattura", treatmentDate:"Data trattamento", serviceDate:"Data prestazione", dueDate:"Scadenza", saveInvoice:"Salva fattura", editChangesSave:"Salva modifiche", addPosition:"+ Aggiungi posizione", quantity:"Quantità", product:"Prodotto", reason:"Motivo", remark:"Nota", pdfLanguage:"Lingua PDF", zugferdPdf:"PDF ZUGFeRD", xml:"XML", pleaseSelectCompany:"Seleziona prima una società.", pleaseSelectInvoice:"Seleziona una fattura a sinistra.", recipient:"Destinatario", date:"Data", language:"Lingua", payment:"Metodo di pagamento", bank:"Coordinate bancarie", taxNo:"Codice fiscale/IVA", qty:"Quantità", code:"Codice", description:"Descrizione", tax:"IVA", price:"Prezzo", lineTotal:"Totale", noLines:"Nessuna posizione aggiunta.", net:"Netto", gross:"Totale lordo", total:"Totale", remove:"Rimuovi", discount:"Sconto", voucher:"Buono", installments:"Pagamento rateale", readAloud:"Leggi fattura", stopReading:"Ferma lettura", accessibilityNote:"Preparazione PDF/UA: lingua, titolo, metadati e ordine di lettura sono impostati."},
  sv: {compliance:"Kontroller", checks:"Kontroller", users:"Användare/rättigheter", usersRights:"Användare/rättigheter", newInvoice:"Ny faktura", invoiceSearch:"Sök faktura", invoiceEdit:"Redigera faktura", search:"Sök", searchPlaceholder:"Sök nummer / namn / orsak", company:"Bolag", invoiceTypeInvoice:"Faktura", invoice:"Faktura", paymentAdvice:"Betalningsavisering", invoiceTypePaymentAdvice:"Betalningsavisering", creditNote:"Kreditnota", cancelInvoice:"Stornofaktura", cancellation:"Stornofaktura", createCreditNote:"Skapa kreditnota", technicalDelete:"Teknisk radering", invoicePreviewTitle:"Bindande fakturaförhandsvisning", previewTitle:"Bindande fakturaförhandsvisning", previewHelp:"Förhandsvisningen ska motsvara den senare PDF-filen: texter, poster, rabatt/kupong, delbetalning, anmärkningar och bankuppgifter.", paymentMethod:"Betalningssätt", paymentCash:"Kontant betalning", paymentCard:"Kortbetalning", paymentTransfer:"Banköverföring", paymentUnknown:"okänt", invoiceDate:"Fakturadatum", treatmentDate:"Behandlingsdatum", serviceDate:"Tjänstedatum", dueDate:"Förfallodatum", saveInvoice:"Spara faktura", editChangesSave:"Spara ändringar", addPosition:"+ Lägg till post", quantity:"Antal", product:"Produkt", reason:"Orsak", remark:"Anmärkning", pdfLanguage:"PDF-språk", zugferdPdf:"ZUGFeRD-PDF", xml:"XML", pleaseSelectCompany:"Välj först ett bolag.", pleaseSelectInvoice:"Välj en faktura till vänster.", recipient:"Mottagare", date:"Datum", language:"Språk", payment:"Betalningssätt", bank:"Bankuppgifter", taxNo:"Skatt/moms", qty:"Antal", code:"Kod", description:"Beskrivning", tax:"Moms", price:"Pris", lineTotal:"Totalt", noLines:"Inga poster har lagts till.", net:"Netto", gross:"Brutto", total:"Totalt", remove:"Ta bort", discount:"Rabatt", voucher:"Kupong", installments:"Delbetalning", readAloud:"Läs upp faktura", stopReading:"Stoppa uppläsning", accessibilityNote:"PDF/UA-förberedelse: språk, titel, metadata och läsordning är inställda."},
  tr: {compliance:"Kontroller", checks:"Kontroller", users:"Kullanıcılar/yetkiler", usersRights:"Kullanıcılar/yetkiler", newInvoice:"Yeni fatura", invoiceSearch:"Fatura ara", invoiceEdit:"Faturayı düzenle", search:"Ara", searchPlaceholder:"Numara / ad / neden ara", company:"Şirket", invoiceTypeInvoice:"Fatura", invoice:"Fatura", paymentAdvice:"Ödeme bildirimi", invoiceTypePaymentAdvice:"Ödeme bildirimi", creditNote:"Alacak dekontu", cancelInvoice:"İptal faturası", cancellation:"İptal faturası", createCreditNote:"Alacak dekontu oluştur", technicalDelete:"Teknik silme", invoicePreviewTitle:"Bağlayıcı fatura önizlemesi", previewTitle:"Bağlayıcı fatura önizlemesi", previewHelp:"Bu önizleme sonraki PDF ile eşleşmelidir: metinler, kalemler, indirim/kupon, taksitler, notlar ve banka bilgileri.", paymentMethod:"Ödeme yöntemi", paymentCash:"Nakit ödeme", paymentCard:"Kartla ödeme", paymentTransfer:"Banka havalesi", paymentUnknown:"bilinmiyor", invoiceDate:"Fatura tarihi", treatmentDate:"Tedavi tarihi", serviceDate:"Hizmet tarihi", dueDate:"Vade tarihi", saveInvoice:"Faturayı kaydet", editChangesSave:"Değişiklikleri kaydet", addPosition:"+ Kalem ekle", quantity:"Miktar", product:"Ürün", reason:"Neden", remark:"Not", pdfLanguage:"PDF dili", zugferdPdf:"ZUGFeRD PDF", xml:"XML", pleaseSelectCompany:"Lütfen önce bir şirket seçin.", pleaseSelectInvoice:"Lütfen soldan bir fatura seçin.", recipient:"Alıcı", date:"Tarih", language:"Dil", payment:"Ödeme yöntemi", bank:"Banka bilgileri", taxNo:"Vergi/KDV", qty:"Miktar", code:"Kod", description:"Açıklama", tax:"KDV", price:"Fiyat", lineTotal:"Toplam", noLines:"Henüz kalem eklenmedi.", net:"Net", gross:"Brüt", total:"Toplam", remove:"Kaldır", discount:"İndirim", voucher:"Kupon", installments:"Taksitli ödeme", readAloud:"Faturayı oku", stopReading:"Okumayı durdur", accessibilityNote:"PDF/UA hazırlığı: dil, başlık, meta veriler ve okuma sırası ayarlandı."},
  ru: {compliance:"Проверки", checks:"Проверки", users:"Пользователи/права", usersRights:"Пользователи/права", newInvoice:"Новый счёт", invoiceSearch:"Поиск счёта", invoiceEdit:"Редактировать счёт", search:"Поиск", searchPlaceholder:"Поиск номера / имени / причины", company:"Организация", invoiceTypeInvoice:"Счёт", invoice:"Счёт", paymentAdvice:"Платёжное уведомление", invoiceTypePaymentAdvice:"Платёжное уведомление", creditNote:"Кредит-нота", cancelInvoice:"Сторнировочный счёт", cancellation:"Сторнировочный счёт", createCreditNote:"Создать кредит-ноту", technicalDelete:"Техническое удаление", invoicePreviewTitle:"Обязательный предпросмотр счёта", previewTitle:"Обязательный предпросмотр счёта", previewHelp:"Этот предпросмотр должен соответствовать будущему PDF: тексты, позиции, скидки/купоны, рассрочка, примечания и банковские реквизиты.", paymentMethod:"Способ оплаты", paymentCash:"Оплата наличными", paymentCard:"Оплата картой", paymentTransfer:"Банковский перевод", paymentUnknown:"неизвестно", invoiceDate:"Дата счёта", treatmentDate:"Дата лечения", serviceDate:"Дата услуги", dueDate:"Срок оплаты", saveInvoice:"Сохранить счёт", editChangesSave:"Сохранить изменения", addPosition:"+ Добавить позицию", quantity:"Количество", product:"Продукт", reason:"Причина", remark:"Примечание", pdfLanguage:"Язык PDF", zugferdPdf:"ZUGFeRD PDF", xml:"XML", pleaseSelectCompany:"Сначала выберите организацию.", pleaseSelectInvoice:"Выберите счёт слева.", recipient:"Получатель", date:"Дата", language:"Язык", payment:"Способ оплаты", bank:"Банковские реквизиты", taxNo:"Налог/НДС", qty:"Количество", code:"Код", description:"Описание", tax:"НДС", price:"Цена", lineTotal:"Итого", noLines:"Позиции ещё не добавлены.", net:"Нетто", gross:"Брутто", total:"Итого", remove:"Удалить", discount:"Скидка", voucher:"Купон", installments:"Оплата частями", readAloud:"Прочитать счёт", stopReading:"Остановить чтение", accessibilityNote:"Подготовка PDF/UA: язык, заголовок, метаданные и порядок чтения установлены."}
};
Object.entries(INVOICE_PROGRAM_COMPLETION_LABELS).forEach(([lang, labels]) => {
  UI_LABELS[lang] = {...(UI_LABELS[lang] ?? {}), ...labels};
});

const uiLang = (value?: string | null) => {
  const v = value || localStorage.getItem("gam.uiLanguage") || "de";
  return ["de","en","fr","uk","it","sv","tr","ru"].includes(v) ? v : "de";
};

const UI_RUNTIME_TRANSLATIONS: Record<string, Record<string, string>> = {};

const stripLanguagePrefix = (key: string): string => {
  return String(key || '').replace(/^(GERMAN|ENGLISH|FRENCH|UKRAINIAN|ITALIAN|SWEDISH|TURKISH|RUSSIAN)\.(?:UI\.)?/i, '');
};

const mergeRuntimeTranslations = (lang: string, translations?: Record<string, string>) => {
  const normalized = uiLang(lang);
  const current = UI_RUNTIME_TRANSLATIONS[normalized] ?? {};
  Object.entries(translations ?? {}).forEach(([rawKey, rawValue]) => {
    const value = String(rawValue ?? '').trim();
    if (!value) return;
    const key = stripLanguagePrefix(rawKey);
    if (!key) return;
    if (normalized !== 'de') {
      const germanSource = collectUiTranslationEntries();
      const german = String(germanSource[key] ?? germanSource[rawKey] ?? '').trim();
      if (german && german.localeCompare(value, undefined, {sensitivity: 'base'}) === 0) return;
    }
    current[key] = value;
    current[rawKey] = value;
  });
  UI_RUNTIME_TRANSLATIONS[normalized] = current;
};

const uiText = (language: string | undefined | null, key: string) => {
  const lang = uiLang(language);
  const cleanKey = stripLanguagePrefix(key);
  const runtime = UI_RUNTIME_TRANSLATIONS[lang]?.[cleanKey] ?? UI_RUNTIME_TRANSLATIONS[lang]?.[key];
  if (runtime && runtime.trim()) return runtime;
  const builtIn = UI_LABELS[lang]?.[cleanKey] ?? tUi(normalizeUiLanguage(lang), cleanKey);
  if (builtIn && builtIn !== cleanKey) return builtIn;
  return UI_LABELS.de[cleanKey] ?? cleanKey;
};

const moduleKey = (labelOrKey: string) => {
  const raw = labelOrKey || "";
  const v = raw.toLowerCase().trim();
  if (v === "invoices") return "invoice";
  if (v === "invoiceadmin" || v === "rechnungverwaltung" || v === "rechnungsverwaltung") return "invoiceAdmin";
  if (v === "patients" || v === "patient" || v === "patientenverwaltung") return "patients";
  if (v === "appointments" || v === "appointment" || v === "terminverwaltung") return "appointments";
  if (v === "users" || v === "usersrights" || v === "users/rights" || v === "benutzer/rechte") return "usersRights";
  if (v === "pricelist") return "price";
  if (v === "approvals") return "approval";
  if (["invoice","invoiceAdmin","inventory","warehouse","cashbook","tasks","approval","orders","personnel","workplace","price","reports","admin","dashboard","checks","usersRights","compliance","communication","patients","appointments"].includes(v)) return v;
  if (v.includes("rechnungsverwaltung") || v.includes("rechnungsadministration") || v.includes("rechnung admin") || v.includes("invoice admin")) return "invoiceAdmin";
  if (v.includes("rechnung")) return "invoice";
  if (v.includes("gerät") || v.includes("geraet") || v.includes("device")) return "inventory";
  if (v.includes("lager") || v.includes("warehouse")) return "warehouse";
  if (v.includes("kasse") || v.includes("cash")) return "cashbook";
  if (v.includes("aufgabe") || v.includes("task")) return "tasks";
  if (v.includes("freigabe") || v.includes("approval")) return "approval";
  if (v.includes("bestell") || v.includes("order")) return "orders";
  if (v.includes("kommunikation") || v.includes("communication") || v.includes("gcs")) return "communication";
  if (v.includes("patient") || v.includes("adresse") || v.includes("address")) return "patients";
  if (v.includes("termin") || v.includes("appointment") || v.includes("kalender") || v.includes("calendar")) return "appointments";
  if (v.includes("personal") || v.includes("personnel")) return "personnel";
  if (v.includes("arbeitsplatz") || v.includes("workplace")) return "workplace";
  if (v.includes("preis") || v.includes("price")) return "price";
  if (v.includes("prüfung") || v.includes("pruefung") || v.includes("check") || v.includes("compliance")) return "checks";
  if (v.includes("benutzer") || v.includes("rechte") || v.includes("right") || v.includes("permission") || v.includes("user")) return "usersRights";
  if (v.includes("report") || v.includes("bericht")) return "reports";
  if (v.includes("admin")) return "admin";
  if (v.includes("dashboard")) return "dashboard";
  return raw;
};

const moduleText = (language: string | undefined | null, labelOrKey: string) => {
  const key = moduleKey(labelOrKey);
  const lang = uiLang(language);
  return UI_RUNTIME_TRANSLATIONS[lang]?.[`module.${key}`] ?? uiText(language, key);
};


const GAM_UI_LABELS: Record<string, Record<string, string>> = {
  de: {
    ttsRate: "Geschwindigkeit",
    ttsMary: "Piper",
    ttsBrowser: "Browser/Windows",
    ttsVoiceAuto: "Automatische Stimme",
    ttsVoice: "Stimme",
    ttsEngine: "Vorlesetechnik",
    ttsSettings: "Vorleseinstellungen",
    ttsAuto: "Automatisch",
    ttsLanguage: "Vorlesesprache",
    invoiceTypePaymentAdvice: "Zahlungsavis",
    paymentAdvice: "Zahlungsavis",
    invoiceSearch: "Rechnung suchen",
    newInvoice: "Neue Rechnung",
    users: "Benutzer/Rechte",
    compliance: "Prüfungen",
    invoicePreviewTitle: "Verbindliche Rechnungsvorschau",
    previewTitle: "Verbindliche Rechnungsvorschau",
    lbdMissingPlaceholder: "lbd – .lbd-Empfängerdatei wurde nicht gefunden; Export nutzt Platzhalter.",
    demoReadOnlyShell: "Modulansicht",
    proformaFailed: "Proforma konnte nicht erstellt werden",
    saveFailed: "Speichern fehlgeschlagen",
    selectAtLeastOneLine: "Bitte mindestens eine Position auswählen.",
    mandatoryZugferd: "ZUGFeRD/Factur-X ist Pflicht-Export.",
    loadingNumber: "wird geladen",
    gross: "Brutto",
    net: "Netto",
    remove: "Entfernen",
    addPosition: "+ Position übernehmen",
    quantity: "Menge",
    product: "Produkt",
    installmentCount: "Ratenanzahl",
    installments: "Ratenzahlung",
    amount: "Betrag",
    percent: "Prozent",
    discountValue: "Rabattwert",
    discountType: "Rabattart",
    discount: "Rabatt",
    voucherAmount: "Gutscheinbetrag",
    voucherText: "Gutscheintext",
    voucher: "Gutschein",
    voucherEnable: "Gutschein",
    discountEnable: "Rabatt",
    installmentsEnable: "Ratenzahlung",
    rateSingular: "Rate",
    ratePlural: "Raten",
    remark: "Bemerkung",
    reason: "Grund",
    paymentMethod: "Zahlungsart",
    paymentCash: "Barzahlung",
    paymentCard: "Kartenzahlung",
    paymentTransfer: "Überweisung",
    paymentUnknown: "unbekannt",
    invoiceDate: "Rechnungsdatum",
    treatmentDate: "Behandlungsdatum",
    serviceDate: "Leistungsdatum",
    dueDate: "Fälligkeitsdatum",
    invoiceCustomerFile: "Kundendatei",
    invoiceUser: "Benutzer",
    invoicePaymentMethod: "Zahlungsart",
    invoiceRecipient: "Rechnungsempfänger",
    language: "Sprache",
    saveInvoice: "Rechnung speichern",
    editChangesSave: "Änderungen speichern",
    cancelInvoice: "Stornorechnung",
    createCreditNote: "Gutschrift erstellen",
    creditNote: "Gutschrift",
    invoiceTypeInvoice: "Rechnung",
    technicalDelete: "Technisch löschen",
    accessLinkError: "Abruflink konnte nicht erzeugt werden",
    qrAltPortal: "QR-Code Rechnungsportal",
    openPortal: "Portal öffnen",
    patientPortalHint: "QR-Code für Patientenabruf mit Sprachwahl und Rechnungshistorie.",
    patientPortalDigital: "Digitales Rechnungsportal",
    zugferdIssues: "ZUGFeRD-Export hat Hinweise",
    zugferdReady: "ZUGFeRD-Export bereit",
    exportCheckLoading: "Exportprüfung wird geladen.",
    xml: "XML",
    zugferdPdf: "ZUGFeRD-PDF", zugferdPdfOpenHtml: "ZUGFeRD-PDF (PDF/UA Test)",
    pdfLanguage: "PDF-Sprache",
    pleaseSelectInvoice: "Bitte links eine Rechnung auswählen.",
    search: "Suchen",
    searchPlaceholder: "Suche Nummer / Name / Grund",
    pleaseChoose: "Bitte wählen",
    company: "Gesellschaft",
    pleaseSelectCompany: "Bitte zuerst eine Gesellschaft auswählen.",
    invoiceEdit: "Rechnung bearbeiten",
    usersRights: "Benutzer/Rechte",
    checks: "Prüfungen",
    role: "Rolle",
    logout: "Logout",
    step31ModuleOverview: "Die GAM2-Migration ist abgeschlossen. Alle Module sind verfügbar und für die kommenden Workflow-Erweiterungen vorbereitet.",
    statusLoading: "Status wird geladen.",
    notFound: "nicht gefunden",
    found: "gefunden",
    notConnected: "nicht verbunden",
    connected: "verbunden",
    lbd: ".lbd",
    accounts: "Accounts",
    db: "DB",
    legacyLoginHint: "Kompatibler Login über bestehende accounts-Tabelle.",
    uiLanguage: "Sprache der Oberfläche",
    chooseApplication: "Anwendung wählen",
    moduleVisibilityHint: "GAM2-Plattform vollständig migriert. Alle Module sind verfügbar – die nächste Entwicklungsstufe erweitert GAM um intelligente Workflows.",
    username: "Benutzername",
    password: "Passwort",
    login: "Anmelden",
    passwordLogin: "Passwort",
    totpRegister: "2FA registrieren",
    totpLogin: "2FA-Login",
    passkeyRegister: "Passkey registrieren",
    passkeyLogin: "Passkey-Login",
    invoice: "Rechnungsprogramm",
    inventory: "Geräteverzeichnis",
    warehouse: "Lagerverwaltung",
    cashbook: "Kassenbuch",
    tasks: "Aufgabenverwaltung",
    approval: "Freigabemanagement",
    orders: "Bestelltool",
    communication: "Kommunikation",
    permissions: "Benutzer/Rechte",
    personnel: "Personaldaten",
    workplace: "Arbeitsplatzausstattung",
    price: "Preisliste",
    reports: "Reports",
    admin: "Administration",
    dashboard: "Dashboard",
    readonly: "Lesemodus",
    readOnly: "Lesemodus"
  },
  en: {
    ttsRate: "Speed",
    ttsMary: "Piper",
    ttsBrowser: "Browser/Windows",
    ttsVoiceAuto: "Automatic voice",
    ttsVoice: "Voice",
    ttsEngine: "Reading engine",
    ttsSettings: "Reading settings",
    ttsAuto: "Automatic",
    ttsLanguage: "Reading language",
    invoiceTypePaymentAdvice: "Payment advice",
    paymentAdvice: "Payment advice",
    invoiceSearch: "Search invoice",
    newInvoice: "New invoice",
    users: "Users/permissions",
    compliance: "Checks",
    invoicePreviewTitle: "Binding invoice preview",
    previewTitle: "Binding invoice preview",
    lbdMissingPlaceholder: "lbd – .lbd recipient file was not found; export uses placeholders.",
    demoReadOnlyShell: "module view",
    proformaFailed: "Proforma could not be created",
    saveFailed: "Saving failed",
    selectAtLeastOneLine: "Please select at least one item.",
    mandatoryZugferd: "ZUGFeRD/Factur-X is the mandatory export.",
    loadingNumber: "loading",
    gross: "Gross",
    net: "Net",
    remove: "Remove",
    addPosition: "+ Add item",
    quantity: "Quantity",
    product: "Product",
    installmentCount: "Number of installments",
    installments: "Installment payment",
    amount: "Amount",
    percent: "Percent",
    discountValue: "Discount value",
    discountType: "Discount type",
    discount: "Discount",
    voucherAmount: "Voucher amount",
    voucherText: "Voucher text",
    voucher: "Voucher",
    voucherEnable: "Voucher",
    discountEnable: "Discount",
    installmentsEnable: "Installments",
    rateSingular: "installment",
    ratePlural: "installments",
    remark: "Remark",
    reason: "Reason",
    paymentMethod: "Payment method",
    paymentCash: "Cash payment",
    paymentCard: "Card payment",
    paymentTransfer: "Bank transfer",
    paymentUnknown: "unknown",
    invoiceDate: "Invoice date",
    saveInvoice: "Save invoice",
    editChangesSave: "Save changes",
    cancelInvoice: "Cancellation invoice",
    createCreditNote: "Create credit note",
    creditNote: "Credit note",
    invoiceTypeInvoice: "Invoice",
    technicalDelete: "Technical delete",
    accessLinkError: "Access link could not be created",
    qrAltPortal: "Invoice portal QR code",
    openPortal: "Open portal",
    patientPortalHint: "QR code for patient access with language selection and invoice history.",
    patientPortalDigital: "Digital invoice portal",
    zugferdIssues: "ZUGFeRD export has notes",
    zugferdReady: "ZUGFeRD export ready",
    exportCheckLoading: "Loading export check.",
    xml: "XML",
    zugferdPdf: "ZUGFeRD PDF", zugferdPdfOpenHtml: "ZUGFeRD PDF (PDF/UA test)",
    pdfLanguage: "PDF language",
    pleaseSelectInvoice: "Please select an invoice on the left.",
    search: "Search",
    searchPlaceholder: "Search number / name / reason",
    pleaseChoose: "Please choose",
    company: "Company",
    pleaseSelectCompany: "Please select a company first.",
    invoiceEdit: "Edit invoice",
    usersRights: "Users/permissions",
    checks: "Checks",
    role: "Role",
    logout: "Logout",
    step31ModuleOverview: "GAM2 migration is complete. All modules are available and ready for the upcoming workflow extensions.",
    statusLoading: "Loading status.",
    notFound: "not found",
    found: "found",
    notConnected: "not connected",
    connected: "connected",
    lbd: ".lbd",
    accounts: "Accounts",
    db: "DB",
    legacyLoginHint: "Compatible login using the existing accounts table.",
    uiLanguage: "Interface language",
    chooseApplication: "Choose application",
    moduleVisibilityHint: "GAM2 platform migration completed. All modules are available – the next development stage adds intelligent workflows.",
    username: "Username",
    password: "Password",
    login: "Sign in",
    passwordLogin: "Password",
    totpRegister: "Register 2FA",
    totpLogin: "2FA login",
    passkeyRegister: "Register passkey",
    passkeyLogin: "Passkey login",
    invoice: "Invoicing",
    inventory: "Device directory",
    warehouse: "Warehouse",
    cashbook: "Cashbook",
    tasks: "Task management",
    approval: "Approval management",
    orders: "Ordering tool",
    communication: "Communication",
    permissions: "Users/permissions",
    personnel: "Personnel data",
    workplace: "Workplace equipment",
    price: "Price list",
    reports: "Reports",
    admin: "Administration",
    dashboard: "Dashboard",
    readonly: "Read-only mode",
    readOnly: "Read-only mode"
  },
  fr: {
    ttsRate: "Vitesse",
    ttsMary: "Piper",
    ttsBrowser: "Navigateur/Windows",
    ttsVoiceAuto: "Voix automatique",
    ttsVoice: "Voix",
    ttsEngine: "Technique de lecture",
    ttsSettings: "Paramètres de lecture",
    ttsAuto: "Automatique",
    ttsLanguage: "Langue de lecture",
    invoiceTypePaymentAdvice: "Avis de paiement",
    paymentAdvice: "Avis de paiement",
    invoiceSearch: "Rechercher une facture",
    newInvoice: "Nouvelle facture",
    users: "Utilisateurs/droits",
    compliance: "Contrôles",
    invoicePreviewTitle: "Aperçu de facture contraignant",
    previewTitle: "Aperçu de facture contraignant",
    lbdMissingPlaceholder: "lbd – le fichier destinataire .lbd est introuvable ; l’export utilise des valeurs de remplacement.",
    demoReadOnlyShell: "module en lecture seule",
    proformaFailed: "La proforma n’a pas pu être créée",
    saveFailed: "Échec de l’enregistrement",
    selectAtLeastOneLine: "Veuillez sélectionner au moins un poste.",
    mandatoryZugferd: "ZUGFeRD/Factur-X est l’export obligatoire.",
    loadingNumber: "chargement",
    gross: "Brut",
    net: "Net",
    remove: "Supprimer",
    addPosition: "+ Ajouter le poste",
    quantity: "Quantité",
    product: "Produit",
    installmentCount: "Nombre d’échéances",
    installments: "Paiement échelonné",
    amount: "Montant",
    percent: "Pourcentage",
    discountValue: "Valeur de remise",
    discountType: "Type de remise",
    discount: "Remise",
    voucherAmount: "Montant du bon",
    voucherText: "Texte du bon",
    voucher: "Bon",
    voucherEnable: "Bon",
    discountEnable: "Remise",
    installmentsEnable: "Paiement échelonné",
    rateSingular: "versement",
    ratePlural: "versements",
    remark: "Remarque",
    reason: "Motif",
    paymentMethod: "Mode de paiement",
    paymentCash: "Paiement en espèces",
    paymentCard: "Paiement par carte",
    paymentTransfer: "Virement bancaire",
    paymentUnknown: "inconnu",
    invoiceDate: "Date de facture",
    saveInvoice: "Enregistrer la facture",
    editChangesSave: "Enregistrer les modifications",
    cancelInvoice: "Facture d’annulation",
    createCreditNote: "Créer un avoir",
    creditNote: "Avoir",
    invoiceTypeInvoice: "Facture",
    technicalDelete: "Suppression technique",
    accessLinkError: "Le lien d’accès n’a pas pu être créé",
    qrAltPortal: "Code QR du portail factures",
    openPortal: "Ouvrir le portail",
    patientPortalHint: "Code QR pour l’accès patient avec choix de langue et historique des factures.",
    patientPortalDigital: "Portail numérique de factures",
    zugferdIssues: "L’export ZUGFeRD contient des remarques",
    zugferdReady: "Export ZUGFeRD prêt",
    exportCheckLoading: "Chargement de la vérification d’export.",
    xml: "XML",
    zugferdPdf: "PDF ZUGFeRD", zugferdPdfOpenHtml: "PDF ZUGFeRD (test PDF/UA)",
    pdfLanguage: "Langue du PDF",
    pleaseSelectInvoice: "Veuillez sélectionner une facture à gauche.",
    search: "Rechercher",
    searchPlaceholder: "Recherche numéro / nom / motif",
    pleaseChoose: "Veuillez choisir",
    company: "Société",
    pleaseSelectCompany: "Veuillez d’abord sélectionner une société.",
    invoiceEdit: "Modifier la facture",
    usersRights: "Utilisateurs/droits",
    checks: "Contrôles",
    role: "Rôle",
    logout: "Déconnexion",
    step31ModuleOverview: "L’étape 31 rend visibles toutes les applications GAM historiques. Les zones entièrement migrées sont utilisables ; les modules encore ouverts sont affichés volontairement comme des modules en lecture seule.",
    statusLoading: "Chargement du statut.",
    notFound: "non trouvé",
    found: "trouvé",
    notConnected: "non connecté",
    connected: "connecté",
    lbd: ".lbd",
    accounts: "Comptes",
    db: "BD",
    legacyLoginHint: "Connexion compatible via la table accounts existante.",
    uiLanguage: "Langue de l’interface",
    chooseApplication: "Choisir l’application",
    moduleVisibilityHint: "La migration de la plateforme GAM2 est terminée. Tous les modules sont disponibles – la prochaine étape ajoute des workflows intelligents.",
    username: "Nom d’utilisateur",
    password: "Mot de passe",
    login: "Se connecter",
    passwordLogin: "Mot de passe",
    totpRegister: "Enregistrer 2FA",
    totpLogin: "Connexion 2FA",
    passkeyRegister: "Enregistrer une passkey",
    passkeyLogin: "Connexion par passkey",
    invoice: "Facturation",
    inventory: "Répertoire des appareils",
    warehouse: "Gestion du stock",
    cashbook: "Livre de caisse",
    tasks: "Gestion des tâches",
    approval: "Gestion des validations",
    orders: "Outil de commande",
    communication: "Communication",
    permissions: "Users/permissions",
    personnel: "Données du personnel",
    workplace: "Équipement du poste",
    price: "Liste de prix",
    reports: "Rapports",
    admin: "Administration",
    dashboard: "Tableau de bord",
    readonly: "Mode lecture seule",
    readOnly: "Mode lecture seule"
  },
  uk: {
    ttsRate: "Швидкість",
    ttsMary: "Piper",
    ttsBrowser: "Браузер/Windows",
    ttsVoiceAuto: "Автоматичний голос",
    ttsVoice: "Голос",
    ttsEngine: "Рушій озвучення",
    ttsSettings: "Налаштування озвучення",
    ttsAuto: "Автоматично",
    ttsLanguage: "Мова озвучення",
    invoiceTypePaymentAdvice: "Платіжне повідомлення",
    paymentAdvice: "Платіжне повідомлення",
    invoiceSearch: "Пошук рахунку",
    newInvoice: "Новий рахунок",
    users: "Користувачі/права",
    compliance: "Перевірки",
    invoicePreviewTitle: "Обов’язковий попередній перегляд рахунку",
    previewTitle: "Обов’язковий попередній перегляд рахунку",
    lbdMissingPlaceholder: "lbd – файл одержувача .lbd не знайдено; експорт використовує заповнювачі.",
    demoReadOnlyShell: "модуль лише для перегляду",
    proformaFailed: "Не вдалося створити проформу",
    saveFailed: "Не вдалося зберегти",
    selectAtLeastOneLine: "Виберіть принаймні одну позицію.",
    mandatoryZugferd: "ZUGFeRD/Factur-X є обов’язковим експортом.",
    loadingNumber: "завантаження",
    gross: "Брутто",
    net: "Нетто",
    remove: "Видалити",
    addPosition: "+ Додати позицію",
    quantity: "Кількість",
    product: "Продукт",
    installmentCount: "Кількість платежів",
    installments: "Оплата частинами",
    amount: "Сума",
    percent: "Відсоток",
    discountValue: "Значення знижки",
    discountType: "Тип знижки",
    discount: "Знижка",
    voucherAmount: "Сума ваучера",
    voucherText: "Текст ваучера",
    voucher: "Ваучер",
    voucherEnable: "Ваучер",
    discountEnable: "Знижка",
    installmentsEnable: "Оплата частинами",
    rateSingular: "платіж",
    ratePlural: "платежі",
    remark: "Примітка",
    reason: "Причина",
    paymentMethod: "Спосіб оплати",
    paymentCash: "Оплата готівкою",
    paymentCard: "Оплата карткою",
    paymentTransfer: "Банківський переказ",
    paymentUnknown: "невідомо",
    invoiceDate: "Дата рахунку",
    saveInvoice: "Зберегти рахунок",
    editChangesSave: "Зберегти зміни",
    cancelInvoice: "Рахунок скасування",
    createCreditNote: "Створити кредит-ноту",
    creditNote: "Кредит-нота",
    invoiceTypeInvoice: "Рахунок",
    technicalDelete: "Технічно видалити",
    accessLinkError: "Не вдалося створити посилання доступу",
    qrAltPortal: "QR-код порталу рахунків",
    openPortal: "Відкрити портал",
    patientPortalHint: "QR-код для доступу пацієнта з вибором мови та історією рахунків.",
    patientPortalDigital: "Цифровий портал рахунків",
    zugferdIssues: "Експорт ZUGFeRD має примітки",
    zugferdReady: "Експорт ZUGFeRD готовий",
    exportCheckLoading: "Завантаження перевірки експорту.",
    xml: "XML",
    zugferdPdf: "ZUGFeRD PDF", zugferdPdfOpenHtml: "ZUGFeRD PDF (PDF/UA test)",
    pdfLanguage: "Мова PDF",
    pleaseSelectInvoice: "Виберіть рахунок ліворуч.",
    search: "Пошук",
    searchPlaceholder: "Пошук номера / імені / причини",
    pleaseChoose: "Будь ласка, виберіть",
    company: "Організація",
    pleaseSelectCompany: "Спочатку виберіть організацію.",
    invoiceEdit: "Редагувати рахунок",
    usersRights: "Користувачі/права",
    checks: "Перевірки",
    role: "Роль",
    logout: "Вийти",
    step31ModuleOverview: "Крок 31 робить видимими всі історичні застосунки GAM. Повністю перенесені області доступні; відкриті модулі навмисно показані як модулі лише для перегляду.",
    statusLoading: "Завантаження статусу.",
    notFound: "не знайдено",
    found: "знайдено",
    notConnected: "не підключено",
    connected: "підключено",
    accounts: "Облікові записи",
    db: "БД",
    legacyLoginHint: "Сумісний вхід через наявну таблицю accounts.",
    uiLanguage: "Мова інтерфейсу",
    chooseApplication: "Вибрати застосунок",
    moduleVisibilityHint: "Міграцію платформи GAM2 завершено. Усі модулі доступні – наступний етап додасть інтелектуальні робочі процеси.",
    username: "Ім’я користувача",
    password: "Пароль",
    login: "Увійти",
    passwordLogin: "Пароль",
    totpRegister: "Зареєструвати 2FA",
    totpLogin: "Вхід через 2FA",
    passkeyRegister: "Зареєструвати passkey",
    passkeyLogin: "Вхід через passkey",
    invoice: "Рахунки",
    inventory: "Каталог пристроїв",
    warehouse: "Склад",
    cashbook: "Касова книга",
    tasks: "Керування завданнями",
    approval: "Керування погодженнями",
    orders: "Інструмент замовлень",
    communication: "Комунікація",
    personnel: "Дані персоналу",
    workplace: "Оснащення робочого місця",
    price: "Прайс-лист",
    reports: "Звіти",
    admin: "Адміністрування",
    dashboard: "Панель",
    readonly: "Режим перегляду",
    readOnly: "Режим перегляду"
  }
};

const gamUiLang = () => {
  try {
    const stored = localStorage.getItem("gam.uiLanguage") || "de";
    return ["de","en","fr","uk","it","sv","tr","ru"].includes(stored) ? stored : "de";
  } catch {
    return "de";
  }
};

const gamUi = (key: string) => {
  const lang = gamUiLang();
  return GAM_UI_LABELS[lang]?.[key] || GAM_UI_LABELS.de[key] || key;
};

const gamModuleKey = (labelOrKey: string) => {
  const value = (labelOrKey || "").toLowerCase();
  if (value === "invoiceadmin" || value.includes("rechnungsverwaltung") || value.includes("rechnungsadministration") || value.includes("rechnung admin") || value.includes("invoice admin")) return "invoiceAdmin";
  if (["invoice","inventory","warehouse","cashbook","tasks","approval","orders","personnel","workplace","price","reports","admin","dashboard","checks","compliance"].includes(value)) return value;
  if (value.includes("rechnung")) return "invoice";
  if (value.includes("gerät") || value.includes("geraet") || value.includes("device")) return "inventory";
  if (value.includes("lager") || value.includes("warehouse")) return "warehouse";
  if (value.includes("kasse") || value.includes("cash")) return "cashbook";
  if (value.includes("aufgabe") || value.includes("task")) return "tasks";
  if (value.includes("freigabe") || value.includes("approval")) return "approval";
  if (value.includes("bestell") || value.includes("order")) return "orders";
  if (value.includes("personal") || value.includes("personnel")) return "personnel";
  if (value.includes("arbeitsplatz") || value.includes("workplace")) return "workplace";
  if (value.includes("preis") || value.includes("price")) return "price";
  if (value.includes("report") || value.includes("bericht")) return "reports";
  if (value.includes("admin")) return "admin";
  if (value.includes("dashboard")) return "dashboard";
  return labelOrKey;
};

const gamModuleLabel = (labelOrKey: string) => gamUi(gamModuleKey(labelOrKey));



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


class ModuleErrorBoundary extends React.Component<{children: React.ReactNode; title?: string}, {hasError: boolean; message: string}> {
  constructor(props: {children: React.ReactNode; title?: string}) {
    super(props);
    this.state = {hasError: false, message: ""};
  }

  static getDerivedStateFromError(error: any) {
    return {hasError: true, message: error?.message ?? String(error)};
  }

  componentDidCatch(error: any) {
    console.error("GAM module render error", this.props.title, error);
  }

  render() {
    if (this.state.hasError) {
      return <section className="card">
        <h2>{this.props.title ?? "Modul"}</h2>
        <p className="note warn">Dieses Modul konnte nicht vollständig angezeigt werden. Die Anwendung bleibt nutzbar.</p>
        {this.state.message && <small className="muted">Details: {this.state.message}</small>}
      </section>;
    }
    return this.props.children;
  }
}

type Page = 'dashboard'|'invoices'|'invoiceAdmin'|'inventory'|'warehouse'|'patients'|'appointments'|'users'|'tasks'|'approvals'|'personnel'|'cashbook'|'compliance'|'reports'|'orders'|'priceList'|'workplace'|'communication'|'permissions'|'moduleAdmin';
type GamLanguage = 'de'|'en'|'fr'|'uk'|'it'|'sv'|'tr'|'ru'|'es'|'pt'|'nl'|'pl'|'cs';
const LANGUAGES: {value: GamLanguage; label: string}[] = [
  {value:'de', label:'Deutsch'},
  {value:'en', label:'English'},
  {value:'fr', label:'Français'},
  {value:'it', label:'Italiano'},
  {value:'es', label:'Español'},
  {value:'pt', label:'Português'},
  {value:'nl', label:'Nederlands'},
  {value:'pl', label:'Polski'},
  {value:'cs', label:'Čeština'},
  {value:'sv', label:'Svenska'},
  {value:'tr', label:'Türkçe'},
  {value:'ru', label:'Русский'},
  {value:'uk', label:'Українська'},
];
const LOGIN_APPLICATIONS = ['Rechnungsprogramm','Rechnungsadministration','Geräteverzeichnis','Lagerverwaltung','Patientenverwaltung','Terminverwaltung','Kassenbuch','Aufgabenverwaltung','Freigabemanagement','Bestelltool','Kommunikation','Benutzer/Rechte','Personaldaten','Arbeitsplatzausstattung','Preisliste','Prüfungen','Reports','Administration'];

const LOGIN_APPLICATION_TO_PAGE: Record<string, Page> = {
  'Rechnungsprogramm': 'invoices',
  'Rechnungsadministration': 'invoiceAdmin',
  'Rechnungsverwaltung': 'invoiceAdmin',
  'Geräteverzeichnis': 'inventory',
  'Lagerverwaltung': 'warehouse',
  'Patientenverwaltung': 'patients',
  'Terminverwaltung': 'appointments',
  'Kassenbuch': 'cashbook',
  'Aufgabenverwaltung': 'tasks',
  'Freigabemanagement': 'approvals',
  'Bestelltool': 'orders',
  'Kommunikation': 'communication',
  'Benutzer/Rechte': 'users',
  'Personaldaten': 'personnel',
  'Arbeitsplatzausstattung': 'workplace',
  'Preisliste': 'priceList',
  'Prüfungen': 'compliance',
  'Reports': 'reports',
  'Administration': 'moduleAdmin',
};

const HARD_LOGIN_TARGET_PAGES: Page[] = ['dashboard','invoices','invoiceAdmin','inventory','warehouse','patients','appointments','tasks','approvals','orders','communication','users','personnel','cashbook','workplace','priceList','compliance','reports','moduleAdmin'];
const HARD_LOGIN_APPLICATION_ALIASES: Record<string, Page> = {
  'patientenverwaltung': 'patients',
  'patienten': 'patients',
  'patient': 'patients',
  'patients': 'patients',
  'module.patients': 'patients',
  'terminverwaltung': 'appointments',
  'termine': 'appointments',
  'termin': 'appointments',
  'kalender': 'appointments',
  'appointment': 'appointments',
  'appointments': 'appointments',
  'module.appointments': 'appointments',
  'prüfungen': 'compliance',
  'pruefungen': 'compliance',
  'geräteprüfungen': 'compliance',
  'geraetepruefungen': 'compliance',
  'compliance': 'compliance',
  'checks': 'compliance',
  'module.compliance': 'compliance',
  'module.checks': 'compliance',
  'rechnungsverwaltung': 'invoiceAdmin',
  'rechnungsadministration': 'invoiceAdmin',
  'rechnung admin': 'invoiceAdmin',
  'rechnungsadmin': 'invoiceAdmin',
  'invoiceadmin': 'invoiceAdmin',
  'invoice admin': 'invoiceAdmin',
  'module.invoiceAdmin': 'invoiceAdmin',
};
function hardLoginPage(value?: string | null): Page | undefined {
  const raw = String(value ?? '').trim();
  if (!raw) return undefined;
  const exact = LOGIN_APPLICATION_TO_PAGE[raw];
  if (exact) return exact;
  const lower = raw.toLowerCase();
  if (HARD_LOGIN_APPLICATION_ALIASES[lower]) return HARD_LOGIN_APPLICATION_ALIASES[lower];
  const normalized = normalizeStartPage(raw);
  return HARD_LOGIN_TARGET_PAGES.includes(normalized) ? normalized : undefined;
}



type LocalSmtpSettings = {
  host:string;
  port:number;
  username:string;
  password:string;
  from:string;
  fromName:string;
  replyTo:string;
  startTls:boolean;
  ssl:boolean;
};
const DEFAULT_SMTP_SETTINGS: LocalSmtpSettings = {
  host: 'mail.gmx.net',
  port: 587,
  username: '',
  password: '',
  from: '',
  fromName: 'GAM 2.0',
  replyTo: '',
  startTls: true,
  ssl: false,
};
function loadLocalSmtpSettings(): LocalSmtpSettings {
  try { return {...DEFAULT_SMTP_SETTINGS, ...(JSON.parse(localStorage.getItem('gam.communication.smtp') || '{}'))}; }
  catch { return DEFAULT_SMTP_SETTINGS; }
}
function saveLocalSmtpSettings(settings: LocalSmtpSettings) {
  localStorage.setItem('gam.communication.smtp', JSON.stringify(settings));
}
function effectiveSmtpForDirectSend(settings?: LocalSmtpSettings) {
  const s = settings ?? loadLocalSmtpSettings();
  if (!s.host || !s.username || !s.password) return undefined;
  return {host:s.host.trim(), port:s.port, username:extractSingleEmailClient(s.username) || s.username.trim(), password:s.password, from:extractSingleEmailClient(s.from || s.username) || (s.from || s.username).trim(), fromName:cleanMailHeaderClient(s.fromName || 'GAM 2.0'), replyTo:extractSingleEmailClient(s.replyTo) || '', startTls:s.startTls, ssl:s.ssl};
}

function cleanMailHeaderClient(value?: string) {
  return String(value || '').replace(/[\r\n]+/g, ' ').trim();
}
function extractSingleEmailClient(value?: string): string {
  let raw = cleanMailHeaderClient(value).replace(/[;]+/g, ' ').replace(/["']/g, ' ').trim();
  if (!raw) return '';
  const lt = raw.indexOf('<');
  const gt = raw.indexOf('>');
  if (lt >= 0 && gt > lt) raw = raw.slice(lt + 1, gt).trim();
  const matches = raw.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi) || [];
  const unique = Array.from(new Set(matches.map(m => m.toLowerCase())));
  if (unique.length === 1) return (matches[0] ?? "").trim();
  if (unique.length > 1 || raw.includes(',')) return '';
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(raw) ? raw : '';
}
function emailValidationMessage(value?: string, label='E-Mail-Adresse', required=true): string {
  const raw = cleanMailHeaderClient(value);
  if (!raw) return required ? `${label} fehlt.` : '';
  const normalized = extractSingleEmailClient(raw);
  if (!normalized) return `${label} ist ungültig oder enthält mehrere Adressen.`;
  return '';
}
function smtpValidationMessages(smtp: LocalSmtpSettings, testTo?: string): string[] {
  const errors: string[] = [];
  if (!cleanMailHeaderClient(smtp.host)) errors.push('SMTP-Server fehlt.');
  if (!Number(smtp.port)) errors.push('SMTP-Port fehlt.');
  const userError=emailValidationMessage(smtp.username, 'SMTP-Benutzer/E-Mail'); if(userError) errors.push(userError);
  if (!smtp.password) errors.push('SMTP-Passwort/App-Passwort fehlt.');
  const fromError=emailValidationMessage(smtp.from || smtp.username, 'Absender-E-Mail-Adresse'); if(fromError) errors.push(fromError);
  const replyError=emailValidationMessage(smtp.replyTo, 'Antwortadresse', false); if(replyError) errors.push(replyError);
  const toError=emailValidationMessage(testTo || smtp.username, 'Test-Empfänger'); if(toError) errors.push(toError);
  return errors;
}

function pageForLoginApplication(app: string): Page {
  return hardLoginPage(app) ?? 'invoices';
}
function normalizeStartPage(raw: string | null | undefined): Page {
  const value = String(raw || '').trim();
  const aliases: Record<string, Page> = {
    invoice: 'invoices',
    rechnung: 'invoices',
    rechnungsprogramm: 'invoices',
    rechnungsverwaltung: 'invoiceAdmin',
    rechnungsadministration: 'invoiceAdmin',
    rechnungsadmin: 'invoiceAdmin',
    invoiceadmin: 'invoiceAdmin',
    'invoice admin': 'invoiceAdmin',
    'module.invoiceAdmin': 'invoiceAdmin',
    patient: 'patients',
    patienten: 'patients',
    patientenverwaltung: 'patients',
    'module.patients': 'patients',
    termin: 'appointments',
    termine: 'appointments',
    termin: 'appointments',
    kalender: 'appointments',
    terminverwaltung: 'appointments',
    'module.appointments': 'appointments',
    appointment: 'appointments',
    appointments: 'appointments',
    kommunikation: 'communication',
    administration: 'moduleAdmin',
    moduladmin: 'moduleAdmin',
    'benutzer/rechte': 'users',
    prüfungen: 'compliance',
    pruefungen: 'compliance',
    compliance: 'compliance',
    checks: 'compliance',
    'module.compliance': 'compliance',
    'module.checks': 'compliance',
    usersRights: 'users' as Page
  };
  const allowed: Page[] = ['dashboard','invoices','invoiceAdmin','inventory','warehouse','patients','appointments','users','tasks','approvals','personnel','cashbook','compliance','reports','orders','priceList','workplace','communication','moduleAdmin'];
  if (allowed.includes(value as Page)) return value as Page;
  const lower = value.toLowerCase();
  return aliases[value] ?? aliases[lower] ?? 'dashboard';
}
function storedStartPage(): Page {
  return normalizeStartPage(localStorage.getItem('gam_start_page'));
}
function storedLoginApplication(): string {
  const raw = localStorage.getItem('gam_selected_application') || '';
  return LOGIN_APPLICATIONS.includes(raw) ? raw : 'Rechnungsprogramm';
}
function storeLoginTarget(app: string): Page {
  const page = pageForLoginApplication(app);
  // Schritt 38k6: Der kanonische Page-Key wird parallel gespeichert, damit neue Module
  // exakt so gestartet werden wie Geräteverzeichnis und Lagerverwaltung.
  localStorage.setItem('gam_selected_application', app);
  localStorage.setItem('gam_selected_application_page', page);
  localStorage.setItem('gam_start_page', page);
  // Schritt 38k5:
  // Patientenverwaltung und Terminverwaltung werden als harte Login-Ziele
  // zusätzlich gepuffert. Damit können spätere Initialisierungen oder alte
  // Rechnungsprogramm-Fallbacks das Ziel nicht überschreiben.
  localStorage.setItem('gam_forced_login_target_page', page);
  // Schritt 38k4:
  // Das Login-Ziel zusätzlich kurzfristig in sessionStorage sichern.
  // Dadurch geht das Ziel nicht verloren, falls React den Auth-State schneller
  // umschaltet als der neue startPage-State in App/Shell sichtbar wird.
  sessionStorage.setItem('gam_login_target_page', page);
  sessionStorage.setItem('gam_login_target_application', app);
  return page;
}
function consumeLoginTargetPage(fallback?: Page): Page {
  const forcedTarget = localStorage.getItem('gam_forced_login_target_page');
  const sessionTarget = sessionStorage.getItem('gam_login_target_page');
  // Wichtig für Schritt 38k5:
  // Hier NICHT mehr sofort aus sessionStorage löschen. Die Shell muss das Ziel
  // beim ersten Rendern und nach loadMenu() noch sicher lesen können.
  return normalizeStartPage(forcedTarget || sessionTarget || localStorage.getItem('gam_selected_application_page') || fallback || localStorage.getItem('gam_start_page')); 
}
function currentUiLanguage(): GamLanguage { return normalizeGamLanguage(localStorage.getItem('gam_ui_language') || localStorage.getItem('gam.uiLanguage') || 'de'); }

function collectUiTranslationEntries(): Record<string, string> {
  // Schritt 34m:
  // Nicht nur die zuletzt ergänzten TTS-Keys übersetzen, sondern den kompletten
  // deutschen UI-Quelltextbestand sammeln. Deutsch bleibt dabei ausschließlich
  // Quellsprache; die Zieltexte kommen aus DB-Cache oder LibreTranslate.
  const entries: Record<string, string> = {};
  const add = (source?: Record<string, string>) => {
    Object.entries(source ?? {}).forEach(([key, value]) => {
      if (!key || value == null) return;
      const text = String(value).trim();
      if (!text) return;
      entries[key] = text;
    });
  };
  add(germanUiEntries());
  add(UI_LABELS.de);
  add(typeof GAM_UI_LABELS !== 'undefined' ? GAM_UI_LABELS.de : undefined);
  add(UI_TEXT.de);

  // Modul-/Navigationslabels und Lesemodus-Texte werden im Code teilweise als
  // deutsche Literale übergeben. Damit sie beim ersten Sprachwechsel ebenfalls
  // in translation_italian usw. landen, werden sie hier mit stabilen Keys ergänzt.
  [
    ['module.dashboard', 'Dashboard'],
    ['module.invoice', 'Rechnungsprogramm'],
    ['module.invoices', 'Rechnungsprogramm'],
    ['module.invoiceAdmin', 'Rechnungsadministration'],
    ['module.inventory', 'Geräteverzeichnis'],
    ['module.warehouse', 'Lagerverwaltung'],
    ['module.patients', 'Patientenverwaltung'],
    ['module.appointments', 'Terminverwaltung'],
    ['module.cashbook', 'Kassenbuch'],
    ['module.tasks', 'Aufgabenverwaltung'],
    ['module.approvals', 'Freigabemanagement'],
    ['module.orders', 'Bestelltool'],
    ['module.personnel', 'Personaldaten'],
    ['module.workplace', 'Arbeitsplatzausstattung'],
    ['module.priceList', 'Preisliste'],
    ['module.compliance', 'Prüfungen'],
    ['module.reports', 'Reports'],
    ['module.users', 'Benutzer/Rechte'],
    ['module.usersRights', 'Benutzer/Rechte'],
    ['module.admin', 'Administration'],
    ['invoiceTypeInvoice', 'Rechnung'],
    ['creditNote', 'Gutschrift'],
    ['invoicePortalTitle', 'Digitales Rechnungsportal'],
    ['invoicePortalQrHint', 'QR-Code für Patientenabruf mit Sprachwahl und Rechnungshistorie.'],
    ['invoicePortalHelp', 'Wählen Sie die gewünschte Sprache und laden Sie Ihre Rechnung erneut herunter.'],
    ['paymentCash', 'Barzahlung'],
    ['paymentCard', 'Kartenzahlung'],
    ['paymentTransfer', 'Überweisung'],
    ['paymentUnknown', 'unbekannt'],
    ['treatmentDate', 'Behandlungsdatum'],
    ['serviceDate', 'Leistungsdatum'],
    ['dueDate', 'Fälligkeitsdatum'],
    ['invoiceDateFormatted', 'Rechnungsdatum'],
    ['voucherEnable', 'Gutschein'],
    ['discountEnable', 'Rabatt'],
    ['installmentsEnable', 'Ratenzahlung'],
    ['rateSingular', 'Rate'],
    ['ratePlural', 'Raten'],
    ['legacyLoginHint', 'Kompatibler Login über bestehende accounts-Tabelle.'],
    ['uiLanguage', 'Sprache der Oberfläche'],
    ['chooseApplication', 'Anwendung wählen'],
    ['moduleVisibilityHint', 'GAM2-Plattform vollständig migriert. Alle Module sind verfügbar – die nächste Entwicklungsstufe erweitert GAM um intelligente Workflows.'],
    ['username', 'Benutzername'],
    ['password', 'Passwort'],
    ['login', 'Anmelden'],
    ['passwordLogin', 'Passwort'],
    ['totpRegister', '2FA registrieren'],
    ['totpLogin', '2FA-Login'],
    ['passkeyRegister', 'Passkey registrieren'],
    ['passkeyLogin', 'Passkey-Login'],
    ['totpRegisterHint', 'Für die 2FA-Registrierung wird nur der Benutzername benötigt. Das Passwortfeld ist bewusst ausgeblendet.'],
    ['totpQrCreate', 'QR-Code erzeugen'],
    ['totpSave', '2FA speichern'],
    ['totpCode', '6-stelliger Authenticator-Code'],
    ['totpCodeConfirm', '6-stelliger Code zur Bestätigung'],
    ['totpLoginButton', 'Mit 2FA anmelden'],
    ['passkeyStatus', 'Passkey-Status prüfen'],
    ['passkeyLoginButton', 'Mit Passkey anmelden'],
    ['passkeyHint', 'Passkey-Testworkflow für localhost/Windows Hello/YubiKey. Für Produktivbetrieb wird die serverseitige WebAuthn-Signaturprüfung noch gehärtet.'],
    ['passkeyLoginHint', 'Passkey-Login über WebAuthn. Lokal funktioniert das mit localhost.'],
    ['readonly.orders.description', 'Beschaffungs- und Bestellmodul mit vollständiger GAM2-Verfügbarkeit und Vorbereitung auf die kommenden Workflow-Erweiterungen.'],
    ['readonly.workplace.description', 'Historisches Modul für Arbeitsplatz-, Raum- und Geräteausstattung. In Schritt 31 bewusst sichtbar, aber noch ohne Bearbeitungsfunktionen.'],
    ['readonly.priceList.description', 'Historische Preislisten- und Produktübersicht. Administration und Bearbeitung werden später separat rekonstruiert.'],
  ].forEach(([key, value]) => { entries[key] = value; });
  return entries;
}

function collectKnownTargetTranslations(language: GamLanguage | string): Record<string, string> {
  const lang = normalizeGamLanguage(language);
  if (lang === 'de') return {};
  const known: Record<string, string> = {};
  const add = (source?: Record<string, string>) => {
    Object.entries(source ?? {}).forEach(([key, value]) => {
      const text = String(value ?? '').trim();
      if (!key || !text) return;
      const german = collectUiTranslationEntries()[key];
      if (german && german.trim().toLowerCase() === text.toLowerCase()) return;
      known[key] = text;
    });
  };
  add(UI_LABELS[lang]);
  add((typeof GAM_UI_LABELS !== 'undefined' ? GAM_UI_LABELS[lang] : undefined));
  add(UI_TEXT[lang]);

  // LoginDialog-Modulbuttons: sichtbar übersetzte Modulnamen ebenfalls
  // persistieren, damit translation_swedish/turkish/russian nicht leer bleiben,
  // nur weil die UI aktuell einen eingebauten Fallback anzeigen kann.
  [
    ['module.dashboard', moduleText(lang, 'dashboard')],
    ['module.invoice', moduleText(lang, 'invoice')],
    ['module.invoices', moduleText(lang, 'invoices')],
    ['module.invoiceAdmin', moduleText(lang, 'invoiceAdmin')],
    ['module.inventory', moduleText(lang, 'inventory')],
    ['module.warehouse', moduleText(lang, 'warehouse')],
    ['module.patients', moduleText(lang, 'patients')],
    ['module.appointments', moduleText(lang, 'appointments')],
    ['module.cashbook', moduleText(lang, 'cashbook')],
    ['module.tasks', moduleText(lang, 'tasks')],
    ['module.approval', moduleText(lang, 'approval')],
    ['module.approvals', moduleText(lang, 'approval')],
    ['module.orders', moduleText(lang, 'orders')],
    ['module.personnel', moduleText(lang, 'personnel')],
    ['module.workplace', moduleText(lang, 'workplace')],
    ['module.price', moduleText(lang, 'price')],
    ['module.priceList', moduleText(lang, 'priceList')],
    ['module.reports', moduleText(lang, 'reports')],
    ['module.admin', moduleText(lang, 'admin')],
    ['module.checks', moduleText(lang, 'checks')],
    ['module.compliance', moduleText(lang, 'compliance')],
  ].forEach(([key, value]) => {
    const text = String(value ?? '').trim();
    const k = String(key);
    const german = collectUiTranslationEntries()[k];
    if (!text || text === k) return;
    if (german && german.trim().toLowerCase() === text.toLowerCase()) return;
    known[k] = text;
  });
  return known;
}

const UI_TRANSLATION_REQUESTED: Record<string, boolean> = {};

function useUiTranslationCache(language: GamLanguage | string, enabled = true, delayMs = 0) {
  const [, setRefresh] = useState(0);
  useEffect(() => {
    const lang = normalizeGamLanguage(language);
    if (!enabled || lang === 'de') return;

    // Schritt 34z7d:
    // Nicht nur einmal pro Sprache sperren, sondern pro tatsächlichem Key-Katalog.
    // Sonst bleiben nach neuen UI-Keys alte Browser-Sessions bei 25 DB-Einträgen hängen.
    const entries = collectUiTranslationEntries();
    const knownTranslations = collectKnownTargetTranslations(lang);
    const catalogKey = `${lang}:${Object.keys(entries).sort().join('|')}`;
    if (UI_TRANSLATION_REQUESTED[catalogKey]) return;
    UI_TRANSLATION_REQUESTED[catalogKey] = true;

    let cancelled = false;
    const timer = window.setTimeout(() => {
      if (cancelled) return;
      loadUiTranslations(lang, entries, knownTranslations)
        .then(translations => {
          if (cancelled) return;
          if (translations && Object.keys(translations).length) {
            mergeRuntimeTranslations(lang, translations);
            setRefresh(v => v + 1);
            window.dispatchEvent(new CustomEvent('gam-ui-translations-loaded', {detail: lang}));
          }

          // Schritt 34z:
          // Zurück zur bewährten GAM-1.0-Logik: Pro UI-Sprachwechsel wird nur
          // die aktuell gewählte Sprache gepflegt. Keine Nachlade-Schleifen und
          // kein Vorbereiten mehrerer Zielsprachen im Login-/Modulpfad.
        })
        .catch(() => {
          // Übersetzungspflege ist Komfort/Cache-Aufbau. Die Oberfläche bleibt nutzbar.
        });
    }, Math.max(0, delayMs));

    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [language, enabled, delayMs]);
}


type TtsEngineChoice = 'auto' | 'piper' | 'browser';
type TtsSettings = {
  language: string;
  engine: TtsEngineChoice;
  voiceURI: string;
  rate: number;
};

const DEFAULT_TTS_SETTINGS: TtsSettings = {
  language: 'auto',
  engine: 'auto',
  voiceURI: 'auto',
  rate: 1
};

function loadTtsSettings(scope: string): TtsSettings {
  try {
    const saved = JSON.parse(localStorage.getItem(`gam.tts.${scope}`) || '{}');
    const next: TtsSettings = {...DEFAULT_TTS_SETTINGS, ...saved};
    if (!['auto','piper','browser'].includes(next.engine)) next.engine = 'auto';
    return next;
  } catch {
    return DEFAULT_TTS_SETTINGS;
  }
}

function saveTtsSettings(scope: string, settings: TtsSettings) {
  localStorage.setItem(`gam.tts.${scope}`, JSON.stringify(settings));
}


// Keep utterances alive while the browser is speaking. Chromium/Edge can
// otherwise garbage-collect SpeechSynthesisUtterance objects and silently stop.
let gamActiveSpeechUtterances: SpeechSynthesisUtterance[] = [];
let gamActivePiperAudios: HTMLAudioElement[] = [];
let gamActiveTtsAbortControllers: AbortController[] = [];
let gamSpeechRunId = 0;

function stopAllGamSpeech() {
  gamSpeechRunId += 1;
  const synth = (window as any).speechSynthesis;
  try { synth?.cancel?.(); } catch {}
  // Manche Browser starten direkt nach cancel noch queued utterances; ein zweites cancel
  // nach dem aktuellen Eventloop beendet diese Restinstanzen zuverlässig.
  window.setTimeout(() => { try { synth?.cancel?.(); } catch {} }, 0);
  window.setTimeout(() => { try { synth?.cancel?.(); } catch {} }, 120);

  gamActiveSpeechUtterances.forEach(u => {
    try { u.onend = null; u.onerror = null; } catch {}
  });
  gamActiveSpeechUtterances = [];

  gamActiveTtsAbortControllers.forEach(c => { try { c.abort(); } catch {} });
  gamActiveTtsAbortControllers = [];

  gamActivePiperAudios.forEach(audio => {
    try {
      audio.pause();
      audio.currentTime = 0;
      audio.src = '';
      audio.load?.();
      audio.onended = null;
      audio.onerror = null;
    } catch {}
  });
  gamActivePiperAudios = [];
}

function clearGamSpeechQueue() {
  stopAllGamSpeech();
}

function allBrowserVoices(): SpeechSynthesisVoice[] {
  const synth = (window as any).speechSynthesis;
  return synth?.getVoices?.() ?? [];
}

function browserVoicesForLanguage(appLang: GamLanguage | string) {
  const voices = allBrowserVoices();
  const chain = ttsFallbackChain(appLang).map(x => x.toLowerCase());
  const matches = voices.filter(v => {
    const l = v.lang?.toLowerCase() ?? '';
    return chain.some(c => l === c || l.startsWith(c));
  });

  // Wichtig für den zuverlässigen Browser-Fallback:
  // Wenn Windows/Browser keine Stimme für die gewählte Sprache anbietet,
  // darf die Auswahlliste nicht leer wirken. Dann zeigen wir alle Stimmen an
  // und sortieren passende Stimmen nur nach oben.
  if (!matches.length) return voices;

  const matchKeys = new Set(matches.map(v => v.voiceURI || v.name));
  const others = voices.filter(v => !matchKeys.has(v.voiceURI || v.name));
  return [...matches, ...others];
}

function selectedBrowserVoice(appLang: GamLanguage | string, voiceURI?: string) {
  const voices: SpeechSynthesisVoice[] = allBrowserVoices();
  if (voiceURI && voiceURI !== 'auto') {
    const selected = voices.find(v => v.voiceURI === voiceURI || v.name === voiceURI);
    if (selected) return selected;
  }
  return preferredSpeechVoice(appLang);
}

function TtsSettingsControls({scope, lang, pdfLang, settings, setSettings}:{scope:string; lang:GamLanguage; pdfLang?:GamLanguage|string; settings:TtsSettings; setSettings:(s:TtsSettings)=>void}) {
  const [voices,setVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [ttsStatus,setTtsStatus] = useState<any|null>(null);
  const effectiveLang = effectiveTtsLanguage(settings.language, pdfLang, lang);
  const normalizedTtsLang = normalizeGamLanguage(effectiveLang);
  const piperLanguageInstalled = Array.isArray(ttsStatus?.installedLanguages) && ttsStatus.installedLanguages.includes(normalizedTtsLang);
  const piperLanguageSupported = Array.isArray(ttsStatus?.supportedLanguages) && ttsStatus.supportedLanguages.includes(normalizedTtsLang);
  const piperEngineAvailable = !!ttsStatus?.piperAvailable;
  const piperUsable = piperEngineAvailable && (piperLanguageInstalled || (!!ttsStatus?.autoDownload && piperLanguageSupported));
  // Schritt 36a: Piper nicht mehr hart ausgrauen, wenn die Sprache unterstützt ist.
  // Falls Engine/Voice beim Klick noch fehlt, startet der Audio-Aufruf den Auto-Download
  // oder fällt sauber auf BrowserTTS zurück.
  const piperSelectable = piperLanguageSupported || piperEngineAvailable;

  useEffect(()=>{
    let cancelled=false;
    const refresh=()=>loadTtsStatus().then(s=>{ if(!cancelled) setTtsStatus(s); }).catch(()=>{ if(!cancelled) setTtsStatus(null); });
    refresh();
    const timer=window.setInterval(refresh, 5000);
    return ()=>{cancelled=true; window.clearInterval(timer);};
  },[]);

  useEffect(()=>{
    let cancelled = false;
    const refresh=()=>{ if(!cancelled) setVoices(browserVoicesForLanguage(effectiveLang)); };
    refresh();

    // Browser laden Stimmen oft asynchron. Mehrere Refreshes verhindern,
    // dass die Auswahlliste beim ersten Öffnen leer bleibt.
    const timers = [100, 500, 1500].map(ms => window.setTimeout(refresh, ms));
    const synth=(window as any).speechSynthesis;
    if(synth?.addEventListener) synth.addEventListener('voiceschanged', refresh);
    else if(synth) synth.onvoiceschanged=refresh;

    return ()=>{
      cancelled = true;
      timers.forEach(t => window.clearTimeout(t));
      if(synth?.removeEventListener) synth.removeEventListener('voiceschanged', refresh);
      else if(synth) synth.onvoiceschanged=null;
    };
  },[effectiveLang]);

  function update(patch: Partial<TtsSettings>) {
    const next = {...settings, ...patch};
    if (next.engine === 'piper') next.voiceURI = 'auto';
    setSettings(next);
    saveTtsSettings(scope, next);
  }

  return <div className="tts-settings-panel">
    <strong>{ui('ttsSettings', lang)}</strong>
    <label>{ui('ttsLanguage', lang)}<select value={settings.language} onChange={e=>update({language:e.target.value, voiceURI:'auto'})}>
      <option value="auto">{ui('ttsAuto', lang)}</option>
      {LANGUAGES.map(l=><option key={l.value} value={l.value}>{l.label}</option>)}
    </select></label>
    <label>{ui('ttsEngine', lang)}<select value={settings.engine} onChange={e=>update({engine:e.target.value as TtsEngineChoice})}>
      <option value="auto">{piperUsable ? 'Piper / BrowserTTS' : ui('ttsAuto', lang)}</option>
      <option value="piper" disabled={!piperSelectable}>Piper{piperUsable ? (piperLanguageInstalled ? '' : ' (Download bei Bedarf)') : (piperSelectable ? ' (prüfen / Fallback aktiv)' : ' (nicht verfügbar)')}</option>
      <option value="browser">{ui('ttsBrowser', lang)}</option>
    </select></label>
    {settings.engine !== 'piper' && <label>{ui('ttsVoice', lang)}<select value={settings.voiceURI} onChange={e=>update({voiceURI:e.target.value})}>
      <option value="auto">{ui('ttsVoiceAuto', lang)}</option>
      {voices.map(v=><option key={v.voiceURI || v.name} value={v.voiceURI || v.name}>{v.name} ({v.lang})</option>)}
    </select></label>}
    <label>{ui('ttsRate', lang)}<input type="range" min="0.6" max="1.5" step="0.05" value={settings.rate} onChange={e=>update({rate:Number(e.target.value)})}/><span>{settings.rate.toFixed(2)}x</span></label>
  </div>;
}

function normalizeGamLanguage(value: GamLanguage | string | undefined): GamLanguage {
  const v = String(value || 'de').toLowerCase();
  if (v.startsWith('en')) return 'en';
  if (v.startsWith('fr')) return 'fr';
  if (v.startsWith('uk') || v.startsWith('ua')) return 'uk';
  if (v.startsWith('it')) return 'it';
  if (v.startsWith('sv') || v.startsWith('se')) return 'sv';
  if (v.startsWith('tr')) return 'tr';
  if (v.startsWith('ru')) return 'ru';
  if (v.startsWith('es')) return 'es';
  if (v.startsWith('pt')) return 'pt';
  if (v.startsWith('nl')) return 'nl';
  if (v.startsWith('pl')) return 'pl';
  if (v.startsWith('cs') || v.startsWith('cz')) return 'cs';
  return 'de';
}

function effectiveTtsLanguage(ttsLang: string | undefined, pdfLang: GamLanguage | string | undefined, uiLang: GamLanguage | string | undefined): GamLanguage {
  const selected = ttsLang && ttsLang !== 'auto' ? ttsLang : (pdfLang || uiLang || currentUiLanguage());
  return normalizeGamLanguage(selected);
}

async function playPiperTtsAudio(lang: GamLanguage | string, text: string, runId: number, onDone?:()=>void) {
  const controller = new AbortController();
  gamActiveTtsAbortControllers.push(controller);
  const blob = await loadTtsAudio(String(lang), text, 'piper', controller.signal);
  gamActiveTtsAbortControllers = gamActiveTtsAbortControllers.filter(c => c !== controller);
  if (runId !== gamSpeechRunId || controller.signal.aborted) return null;

  const url = URL.createObjectURL(blob);
  const audio = new Audio(url);
  gamActivePiperAudios.push(audio);
  const cleanup = () => {
    URL.revokeObjectURL(url);
    gamActivePiperAudios = gamActivePiperAudios.filter(a => a !== audio);
    if (runId === gamSpeechRunId) onDone?.();
  };
  audio.onended = cleanup;
  audio.onerror = cleanup;
  await audio.play();
  return audio;
}

function piperSupportsLanguage(lang: GamLanguage | string) {
  return ['de','en','fr','it','es','pt','nl','pl','cs','sv','tr','ru','uk'].includes(normalizeGamLanguage(lang));
}

function ttsFallbackChain(appLang: GamLanguage | string): string[] {
  const lang = String(appLang || 'de').toLowerCase();

  // Fallback chain:
  // 1. target language
  // 2. close/acceptable fallback
  // 3. English
  // 4. German
  if (lang === 'de') return ['de-DE', 'de-AT', 'de-CH', 'de', 'en-US', 'en-GB', 'en'];
  if (lang === 'en') return ['en-US', 'en-GB', 'en-AU', 'en-CA', 'en', 'de-DE', 'de'];
  if (lang === 'fr') return ['fr-FR', 'fr-CA', 'fr-BE', 'fr', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'uk') return ['uk-UA', 'uk', 'ru-RU', 'ru', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'it') return ['it-IT', 'it', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'sv') return ['sv-SE', 'sv', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'tr') return ['tr-TR', 'tr', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'ru') return ['ru-RU', 'ru', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'es') return ['es-ES', 'es-MX', 'es', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'pt') return ['pt-PT', 'pt-BR', 'pt', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'nl') return ['nl-NL', 'nl-BE', 'nl', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'pl') return ['pl-PL', 'pl', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  if (lang === 'cs') return ['cs-CZ', 'cs', 'en-US', 'en-GB', 'en', 'de-DE', 'de'];
  return ['de-DE', 'de', 'en-US', 'en-GB', 'en'];
}

function ttsVoiceNameHints(appLang: GamLanguage | string): string[] {
  const lang = String(appLang || 'de').toLowerCase();
  if (lang === 'de') return ['german', 'deutsch', 'hedda', 'katja'];
  if (lang === 'en') return ['english', 'david', 'zira', 'mark', 'aria', 'jenny'];
  if (lang === 'fr') return ['french', 'français', 'hortense', 'denise'];
  if (lang === 'uk') return ['ukrainian', 'україн', 'russian', 'русск', 'irina', 'pavel', 'english'];
  if (lang === 'it') return ['italian', 'italiano', 'elsa', 'cosimo'];
  if (lang === 'sv') return ['swedish', 'svenska', 'astrid'];
  if (lang === 'tr') return ['turkish', 'türk', 'türkçe'];
  if (lang === 'ru') return ['russian', 'русск', 'irina', 'pavel'];
  if (lang === 'es') return ['spanish', 'español', 'helena', 'elvira', 'sabina'];
  if (lang === 'pt') return ['portuguese', 'português', 'maria'];
  if (lang === 'nl') return ['dutch', 'nederlands', 'frank'];
  if (lang === 'pl') return ['polish', 'polski', 'paulina'];
  if (lang === 'cs') return ['czech', 'čeština', 'jakub'];
  return [];
}

function preferredSpeechVoice(appLang: GamLanguage | string) {
  const voices: SpeechSynthesisVoice[] = allBrowserVoices();
  if (!voices.length) return null;

  const chain = ttsFallbackChain(appLang);

  // Exact language match first.
  for (const candidate of chain) {
    const c = candidate.toLowerCase();
    const exact = voices.find(v => v.lang?.toLowerCase() === c);
    if (exact) return exact;
  }

  // Prefix match next, e.g. en-US for en.
  for (const candidate of chain) {
    const c = candidate.toLowerCase();
    const prefix = voices.find(v => v.lang?.toLowerCase().startsWith(c));
    if (prefix) return prefix;
  }

  // Some voices expose better information in their names than in lang.
  for (const hint of ttsVoiceNameHints(appLang)) {
    const byName = voices.find(v => v.name?.toLowerCase().includes(hint.toLowerCase()));
    if (byName) return byName;
  }

  return voices.find(v => v.default) ?? voices[0] ?? null;
}


function describePreferredSpeechVoice(appLang: GamLanguage | string): string {
  const voice = preferredSpeechVoice(appLang);
  return voice ? `${voice.name} (${voice.lang})` : 'keine passende Stimme gefunden';
}

function prepareSpeechVoices() {
  const synth = (window as any).speechSynthesis;
  if (!synth?.getVoices) return;
  synth.getVoices();
}

function speechLang(lang: GamLanguage): string {
  switch (lang) {
    case 'en': return 'en-US';
    case 'fr': return 'fr-FR';
    case 'uk': return 'uk-UA';
    case 'it': return 'it-IT';
    case 'sv': return 'sv-SE';
    case 'tr': return 'tr-TR';
    case 'ru': return 'ru-RU';
    case 'es': return 'es-ES';
    case 'pt': return 'pt-PT';
    case 'nl': return 'nl-NL';
    case 'pl': return 'pl-PL';
    case 'cs': return 'cs-CZ';
    case 'de':
    default: return 'de-DE';
  }
}

function companyLogoSrc(company?: InvoiceCompany): string {
  const id = company?.id;
  const name = (company?.name ?? '').toLowerCase();
  if (company?.logoUrl) return company.logoUrl;
  if (id === 1 || name.includes('amae')) return '/images/logo_AMAE_blau.png';
  if (id === 2 || id === 3 || name.includes('acqua') || name.includes('aqua')) return '/images/logo_ACQUA_blau.png';
  if (id === 6 || name.includes('healthcode')) return '/images/Healthcode_logo_blau.png';
  return '/images/KOPFZENTRUM_LOGO.png';
}



function LoginNewsBox(){
  const [news,setNews]=useState<LoginNews|null>(null);
  useEffect(()=>{loadLoginNews().then(setNews).catch(()=>setNews(null));},[]);
  if(!news?.enabled) return null;
  const kind = ['warning','error','success','info'].includes(news.severity || '') ? news.severity : 'info';
  return <div className={`login-news-box note ${kind==='error'?'warn':kind}`}>
    <strong>{news.title || 'Hinweis'}</strong>
    <p>{news.text}</p>
  </div>;
}

function Login({onLogin}:{onLogin:(startPage?:Page)=>void}) {
  const [tab,setTab]=useState<'password'|'totp-register'|'totp-login'|'passkey-register'|'passkey-login'>('password');
  const [username,setUsername]=useState('');
  const [password,setPassword]=useState('');
  const [totp,setTotp]=useState('');
  const [err,setErr]=useState('');
  const [info,setInfo]=useState('');
  const [retryAfter,setRetryAfter]=useState(0);
  const [setup,setSetup]=useState<{secret:string; otpauthUri:string; alreadyConfigured:boolean}|null>(null);
  const [uiLanguage,setUiLanguage]=useState<GamLanguage>(currentUiLanguage());
  useUiTranslationCache(uiLanguage, false);
  
  const ui = React.useCallback((key: string) => uiText(uiLanguage, key), [uiLanguage]);
  const uiModule = React.useCallback((labelOrKey: string) => moduleText(uiLanguage, labelOrKey), [uiLanguage]);
const [selectedApplication,setSelectedApplication]=useState(storedLoginApplication());
  function changeUiLanguage(value: GamLanguage){ setUiLanguage(value); localStorage.setItem('gam_ui_language', value); localStorage.setItem('gam.uiLanguage', value); window.dispatchEvent(new CustomEvent('gam-ui-language-changed', {detail:value})); }

  useEffect(()=>{
    if (retryAfter <= 0) return;
    const timer = window.setInterval(()=>setRetryAfter(v=>Math.max(0, v-1)), 1000);
    return ()=>window.clearInterval(timer);
  },[retryAfter]);

  function handleLoginError(ex:any, fallback:string) {
    if (ex?.retryAfterSeconds) {
      setRetryAfter(Number(ex.retryAfterSeconds));
      const message = `${ui('tooManyAttempts')} ${Number(ex.retryAfterSeconds)} ${ui('seconds')}.`;
      setErr(message);
      gamNotify('warning', message, 0);
      return;
    }
    const message = ex?.message ?? fallback;
    setErr(message);
    notifyLoginError(ex, fallback);
  }

  async function passwordLogin(e:React.FormEvent){
    e.preventDefault();
    if (retryAfter > 0) return;
    setErr('');
    try{await login(username,password,''); const target=storeLoginTarget(selectedApplication); notifyLoginSuccess(uiModule(selectedApplication)); onLogin(target);}
    catch(ex:any){handleLoginError(ex,'Login fehlgeschlagen');}
  }

  async function totpLogin(e:React.FormEvent){
    e.preventDefault();
    if (retryAfter > 0) return;
    setErr('');
    try{await login(username,'',totp); const target=storeLoginTarget(selectedApplication); notifyLoginSuccess(uiModule(selectedApplication)); onLogin(target);}
    catch(ex:any){handleLoginError(ex,'2FA-Login fehlgeschlagen');}
  }

  async function startTotpSetup(e:React.FormEvent){
    e.preventDefault();
    setErr(''); setInfo(''); setSetup(null);
    try{
      const r=await setupTotp(username);
      setSetup({secret:r.secret, otpauthUri:r.otpauthUri, alreadyConfigured:r.alreadyConfigured});
      const message = r.alreadyConfigured ? ui('setupExisting') : ui('setupCreated');
      setInfo(message);
      gamNotify('info', message);
    }catch(ex:any){const message=ex.message??'2FA-Registrierung konnte nicht gestartet werden'; setErr(message); gamNotify('error', message, 0);}
  }

  async function confirmTotpSetup(){
    setErr(''); setInfo('');
    if(!setup){const message=ui('setupFirst'); setErr(message); gamNotify('warning', message); return;}
    try{
      await confirmTotp(username,setup.secret,totp);
      const message=ui('totpSaved');
      setInfo(message);
      gamNotify('success', message);
      setTab('totp-login');
      setTotp('');
    }catch(ex:any){const message=ex.message??'2FA-Code konnte nicht bestätigt werden'; setErr(message); gamNotify('error', message, 0);}
  }

  async function passkeyInfo(){
    setErr('');
    try{const r=await loadPasskeyStatus(); const message=String(r.note ?? 'Passkey/WebAuthn ist vorbereitet.'); setInfo(message); gamNotify('info', message);}
    catch(ex:any){const message=ex.message??'Passkey-Status konnte nicht geladen werden'; setErr(message); gamNotify('error', message, 0);}
  }

  async function registerPasskey(){
    setErr(''); setInfo('');
    if(!username.trim()){const message=ui('usernameRequired'); setErr(message); gamNotify('warning', message); return;}
    if(!window.PublicKeyCredential){const message=ui('passkeyUnsupported'); setErr(message); gamNotify('error', message, 0); return;}
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
      if(!credential){const message='Passkey-Registrierung wurde abgebrochen.'; setErr(message); gamNotify('warning', message); return;}
      const response = credential.response as AuthenticatorAttestationResponse;
      await passkeyRegisterFinish(
        username.trim(),
        opts.challenge,
        bufferToB64url(credential.rawId),
        JSON.stringify({clientDataJSON: bufferToB64url(response.clientDataJSON), attestationObject: bufferToB64url(response.attestationObject)}),
        navigator.userAgent.slice(0,120)
      );
      const message='Passkey wurde gespeichert. Du kannst jetzt den Passkey-Login testen.';
      setInfo(message);
      gamNotify('success', message);
      setTab('passkey-login');
    }catch(ex:any){const message=ex.message??'Passkey-Registrierung fehlgeschlagen'; setErr(message); gamNotify('error', message, 0);}
  }

  async function loginWithPasskey(){
    setErr(''); setInfo('');
    if(!username.trim()){const message='Bitte Benutzernamen eingeben.'; setErr(message); gamNotify('warning', message); return;}
    if(!window.PublicKeyCredential){const message=ui('passkeyUnsupported'); setErr(message); gamNotify('error', message, 0); return;}
    try{
      const opts = await passkeyLoginOptions(username.trim());
      if(!opts.allowCredentialIds?.length){const message=ui('noPasskey'); setErr(message); gamNotify('warning', message, 0); return;}
      const assertion = await navigator.credentials.get({
        publicKey: {
          challenge: b64urlToBuffer(opts.challenge),
          allowCredentials: opts.allowCredentialIds.map(id=>({type:'public-key', id:b64urlToBuffer(id)})),
          userVerification: 'preferred',
          timeout: 60000
        }
      }) as PublicKeyCredential | null;
      if(!assertion){const message='Passkey-Login wurde abgebrochen.'; setErr(message); gamNotify('warning', message); return;}
      await passkeyLoginFinish(username.trim(), opts.challenge, bufferToB64url(assertion.rawId));
      const target=storeLoginTarget(selectedApplication);
      notifyLoginSuccess(uiModule(selectedApplication));
      onLogin(target);
    }catch(ex:any){const message=ex.message??'Passkey-Login fehlgeschlagen'; setErr(message); gamNotify('error', message, 0);}
  }

  const locked = retryAfter > 0;

  return <main className="login"><section className="card login-card"><img className="login-logo" src="/images/GAM.png" alt="GAM 2.0" /><h1>GAM 2.0</h1><p>{ui("legacyLoginHint")}</p><LoginNewsBox/><label className="language-select">{ui("uiLanguage")}<select value={uiLanguage} onChange={e=>changeUiLanguage(e.target.value as GamLanguage)}>{LANGUAGES.map(l=><option key={l.value} value={l.value}>{l.label}</option>)}</select></label><div className="login-application-select"><span>{ui("chooseApplication")}</span><div className="login-app-buttons">{LOGIN_APPLICATIONS.map(app=><button type="button" key={app} className={selectedApplication===app?'active':''} onClick={()=>{setSelectedApplication(app); storeLoginTarget(app);}}><img src={iconForModule(app)} alt="" className="module-button-icon" /> <span>{uiModule(app)}</span></button>)}</div><small>{ui("moduleVisibilityHint")}</small></div><nav className="tabs login-tabs"><button type="button" className={tab==='password'?'active':''} onClick={()=>setTab('password')}><ShieldCheck size={16}/> {ui("passwordLogin")}</button><button type="button" className={tab==='totp-register'?'active':''} onClick={()=>setTab('totp-register')}><QrCode size={16}/> {ui("totpRegister")}</button><button type="button" className={tab==='totp-login'?'active':''} onClick={()=>setTab('totp-login')}><Smartphone size={16}/> {ui("totpLogin")}</button><button type="button" className={tab==='passkey-register'?'active':''} onClick={()=>{setTab('passkey-register'); passkeyInfo();}}><KeyRound size={16}/> {ui("passkeyRegister")}</button><button type="button" className={tab==='passkey-login'?'active':''} onClick={()=>{setTab('passkey-login'); passkeyInfo();}}><KeyRound size={16}/> {ui("passkeyLogin")}</button></nav>
  {locked&&<p className="note warn">{ui("tooManyAttempts")} <b>{retryAfter}</b> {ui("seconds")}.</p>}
  {tab==='password'&&<form onSubmit={passwordLogin} className="login-form"><input autoFocus placeholder={ui("username")} value={username} onChange={e=>setUsername(e.target.value)}/><input placeholder={ui("password")} type="password" value={password} onChange={e=>setPassword(e.target.value)}/><button disabled={locked}><ShieldCheck size={18}/> {locked?`${ui("wait")} ${retryAfter}s`:ui("login")}</button></form>}
  {tab==='totp-register'&&<section className="login-form"><form onSubmit={startTotpSetup} className="login-form"><input placeholder={ui("username")} value={username} onChange={e=>setUsername(e.target.value)}/><p className="muted">{ui("totpRegisterHint")}</p><button><QrCode size={18}/> {ui("totpQrCreate")}</button></form>{setup&&<div className="qr-box"><QRCodeSVG value={setup.otpauthUri} size={210}/><p><b>Secret:</b> <code>{setup.secret}</code></p><small>Authenticator: Google Authenticator, Microsoft Authenticator, Aegis, 2FAS, Bitwarden usw.</small><input placeholder={ui("totpCodeConfirm")} value={totp} onChange={e=>setTotp(e.target.value)}/><button type="button" onClick={confirmTotpSetup}>{ui("totpSave")}</button></div>}</section>}
  {tab==='totp-login'&&<form onSubmit={totpLogin} className="login-form"><input placeholder={ui("username")} value={username} onChange={e=>setUsername(e.target.value)}/><input placeholder={ui("totpCode")} value={totp} onChange={e=>setTotp(e.target.value)}/><button disabled={locked}><Smartphone size={18}/> {locked?`${ui("wait")} ${retryAfter}s`:ui("totpLoginButton")}</button></form>}
  {tab==='passkey-register'&&<section className="login-form"><input placeholder={ui("username")} value={username} onChange={e=>setUsername(e.target.value)}/><p className="note warn">{ui("passkeyHint")}</p><button type="button" onClick={registerPasskey}><KeyRound size={18}/> {ui("passkeyRegister")}</button><button type="button" className="secondary" onClick={passkeyInfo}>{ui("passkeyStatus")}</button></section>}
  {tab==='passkey-login'&&<section className="login-form"><input placeholder={ui("username")} value={username} onChange={e=>setUsername(e.target.value)}/><p className="note warn">{ui("passkeyLoginHint")}</p><button type="button" onClick={loginWithPasskey}><KeyRound size={18}/> {ui("passkeyLoginButton")}</button><button type="button" className="secondary" onClick={passkeyInfo}>{ui("passkeyStatus")}</button></section>}
  {info&&<p className="note ok">{info}</p>}{err&&<b className="error">{err}</b>}</section></main>
}


const authButtonLabel = (language: string | undefined | null, labelOrKey: string) => {
  const value = (labelOrKey || "").toLowerCase();
  if (value.includes("2fa") && value.includes("registr")) return uiText(language, "totpRegister");
  if (value.includes("2fa")) return uiText(language, "totpLogin");
  if (value.includes("passkey") && value.includes("registr")) return uiText(language, "passkeyRegister");
  if (value.includes("passkey")) return uiText(language, "passkeyLogin");
  if (value.includes("passwort") || value.includes("password")) return uiText(language, "passwordLogin");
  return labelOrKey;
};



type GamNotificationKind = 'success' | 'error' | 'warning' | 'info';
type GamChangeField = { field:string; before:string; after:string };
type GamChangeDetails = { module:string; action:string; record?:string; fields?:GamChangeField[]; summary?:string };
type GamNotification = { id:number; kind:GamNotificationKind; text:string; createdAt:string; timeoutMs?:number; details?:GamChangeDetails };

function stringifyChangeValue(value:any):string{
  if(value === undefined || value === null || value === '') return '—';
  if(Array.isArray(value)) return value.map(v=>stringifyChangeValue(v)).join(', ');
  if(typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

function buildChangeDetails(module:string, action:string, before:any, after:any, labels:Record<string,string>={}, record?:string):GamChangeDetails{
  const left = before ?? {};
  const right = after ?? {};
  const keys = Array.from(new Set([...Object.keys(left), ...Object.keys(right)])).filter(k=>!['id'].includes(k));
  const fields = keys.map(k=>({field:labels[k]||k, before:stringifyChangeValue(left[k]), after:stringifyChangeValue(right[k])})).filter(x=>x.before!==x.after).slice(0,60);
  return { module, action, record, fields, summary: fields.length ? `${fields.length} Feld(er) geändert.` : 'Keine Feldänderung ermittelt.' };
}

function gamNotify(kind:GamNotificationKind, text:string, timeoutMs?:number, details?:GamChangeDetails){
  window.dispatchEvent(new CustomEvent('gam-notification', { detail: { kind, text, timeoutMs, details } }));
}

function loadGcsProtocol(): GamNotification[] {
  try { return JSON.parse(localStorage.getItem('gam.communication.protocol') || '[]'); }
  catch { return []; }
}
function saveGcsProtocol(items: GamNotification[]) {
  localStorage.setItem('gam.communication.protocol', JSON.stringify(items.slice(0,200)));
}

function notifyLoginSuccess(target?: string){
  gamNotify('success', target ? `Login erfolgreich – ${target} wird geöffnet.` : 'Login erfolgreich.');
}

function notifyLoginError(error:any, fallback='Login fehlgeschlagen.'){
  gamNotify('error', error?.message ?? fallback, 0);
}

function notifyLogoutSuccess(){
  gamNotify('success', 'Logout erfolgreich.');
}

function notifyLogoutError(error:any){
  gamNotify('error', error?.message ?? 'Logout fehlgeschlagen.', 0);
}

function notifySessionExpired(){
  gamNotify('warning', 'Sitzung abgelaufen. Bitte erneut anmelden.', 0);
}

function GamNotificationCenter({defaultTimeoutMs=5000,onLogout}:{defaultTimeoutMs?:number; onLogout?:()=>void}){
  const [items,setItems]=useState<GamNotification[]>([]);
  const [history,setHistory]=useState<GamNotification[]>([]);
  const [open,setOpen]=useState(false);
  const [selected,setSelected]=useState<GamNotification|null>(null);
  useEffect(()=>{
    const handler=(event:any)=>{
      const detail=event?.detail ?? {};
      const item:GamNotification={
        id: Date.now() + Math.floor(Math.random()*10000),
        kind: detail.kind || 'info',
        text: detail.text || '',
        timeoutMs: typeof detail.timeoutMs === 'number' ? detail.timeoutMs : defaultTimeoutMs,
        createdAt: new Date().toLocaleTimeString('de-DE', {hour:'2-digit', minute:'2-digit', second:'2-digit'}),
        details: detail.details
      };
      if(!item.text) return;
      setItems(prev=>[item, ...prev].slice(0,4));
      setHistory(prev=>[item, ...prev].slice(0,25));
      saveGcsProtocol([item, ...loadGcsProtocol()]);
      if(item.timeoutMs && item.timeoutMs > 0){
        window.setTimeout(()=>setItems(prev=>prev.filter(x=>x.id!==item.id)), item.timeoutMs);
      }
    };
    window.addEventListener('gam-notification', handler as any);
    return()=>window.removeEventListener('gam-notification', handler as any);
  },[defaultTimeoutMs]);
  const close=(id:number)=>setItems(prev=>prev.filter(x=>x.id!==id));
  return <div className="gam-notification-area">
    <div className="gam-top-actions">
      <button className="gam-notification-menu" type="button" onClick={()=>setOpen(v=>!v)}>Meldungen</button>
      {onLogout&&<button className="gam-top-logout secondary" type="button" onClick={onLogout}><LogOut size={16}/> Logout</button>}
    </div>
    {open&&<div className="gam-notification-history"><div className="modal-title-row"><b>Letzte Meldungen</b><button className="secondary icon-only" type="button" onClick={()=>setOpen(false)} aria-label="Meldungen schließen">×</button></div>{history.length===0?<p className="muted">Noch keine Meldungen.</p>:history.map(n=><button key={n.id} type="button" className={`gam-toast gam-toast-${n.kind} ${n.details?'gam-toast-clickable':''}`} onClick={()=>n.details?setSelected(n):undefined}><small>{n.createdAt}</small><span>{n.text}</span>{n.details&&<em>Details ansehen</em>}</button>)}</div>}
    <div className="gam-toast-stack">{items.map(n=><div key={n.id} className={`gam-toast gam-toast-${n.kind} ${n.details?'gam-toast-clickable':''}`} onClick={()=>n.details?setSelected(n):undefined}><button type="button" aria-label="Meldung schließen" onClick={(e)=>{e.stopPropagation(); close(n.id)}}>×</button><small>{n.createdAt}</small><span>{n.text}</span>{n.details&&<em>Details</em>}</div>)}</div>
    {selected&&<div className="gam-modal-backdrop" onClick={()=>setSelected(null)}><section className="card gam-modal-card change-detail-card" onClick={e=>e.stopPropagation()}><div className="modal-title-row"><h2>Änderungsdetails</h2><button className="secondary icon-only" type="button" onClick={()=>setSelected(null)} aria-label="Details schließen">×</button></div><p className="muted"><b>{selected.details?.module}</b> · {selected.details?.action}<br/>Zeitpunkt: {selected.createdAt}{selected.details?.record?<> · Datensatz: {selected.details.record}</>:null}</p>{selected.details?.summary&&<p className="note">{selected.details.summary}</p>}{selected.details?.fields?.length?<table className="compact-table change-details-table"><thead><tr><th>Feld</th><th>Vorher</th><th>Nachher</th></tr></thead><tbody>{selected.details.fields.map((f,idx)=><tr key={idx}><td>{f.field}</td><td>{f.before}</td><td>{f.after}</td></tr>)}</tbody></table>:<p className="muted">Zu dieser Meldung liegen keine einzelnen Feldänderungen vor.</p>}</section></div>}
  </div>;
}



type GamDialogSize = 'small' | 'medium' | 'large' | 'wide';

function GamDialog({
  open,
  title,
  children,
  footer,
  onClose,
  size='large',
  className='',
  firstFocusRef,
}: {
  open: boolean;
  title: React.ReactNode;
  children: React.ReactNode;
  footer?: React.ReactNode;
  onClose: () => void;
  size?: GamDialogSize;
  className?: string;
  firstFocusRef?: React.RefObject<HTMLElement | null>;
}){
  const onCloseRef = useRef(onClose);
  const focusedForOpenRef = useRef(false);

  useEffect(()=>{
    onCloseRef.current = onClose;
  },[onClose]);

  useEffect(()=>{
    if(!open){
      focusedForOpenRef.current = false;
      document.body.classList.remove('gam-modal-open');
      return;
    }

    document.body.classList.add('gam-modal-open');
    const keyHandler=(event: KeyboardEvent)=>{
      if(event.key === 'Escape') onCloseRef.current();
    };
    window.addEventListener('keydown', keyHandler);

    const focusTimer = window.setTimeout(()=>{
      if(!focusedForOpenRef.current){
        firstFocusRef?.current?.focus?.();
        focusedForOpenRef.current = true;
      }
    }, 80);

    return()=>{
      window.removeEventListener('keydown', keyHandler);
      window.clearTimeout(focusTimer);
    };
  },[open]);

  if(!open) return null;

  return <>
    <div className="gam-modal-backdrop" onClick={onClose} />
    <section className={`card gam-modal-card gam-dialog gam-dialog-${size} ${className}`} role="dialog" aria-modal="true">
      <div className="modal-title-row"><h2>{title}</h2><button className="secondary icon-only" type="button" onClick={onClose} aria-label="Dialog schließen">×</button></div>
      <div className="gam-dialog-body">{children}</div>
      {footer&&<div className="gam-dialog-footer">{footer}</div>}
    </section>
  </>;
}

function GamStickyToolbar({children,className=''}:{children:React.ReactNode; className?:string}){
  return <div className={`toolbar sticky-actionbar gam-sticky-toolbar ${className}`}>{children}</div>;
}

function GamScrollArea({children,tall=false,className=''}:{children:React.ReactNode; tall?:boolean; className?:string}){
  return <div className={`scroll-table gam-scroll-area ${tall?'scroll-table-tall':''} ${className}`}>{children}</div>;
}

function notifySaved(text='Änderungen erfolgreich gespeichert.', details?:GamChangeDetails){
  gamNotify('success', text, undefined, details);
}

function notifySaveError(error:any, fallback='Speichern fehlgeschlagen.'){
  gamNotify('error', error?.message ?? fallback, 0);
}


type PatientRecord = {
  id:number;
  addressId:number;
  patientNo:string;
  addressRole:string;
  recipientType:string;
  companyName:string;
  contactPerson:string;
  additionalLine:string;
  lastName:string;
  firstName:string;
  birthDate:string;
  email:string;
  phone:string;
  mobile:string;
  street:string;
  zip:string;
  city:string;
  insurance:string;
  privacyConsent:boolean;
  communicationChannel:string;
  note:string;
};

const PATIENT_STORAGE_KEY = 'gam38j2.address-patients';
const legacyPatientStorageKey = 'gam38j.patients';
const patientDisplayName=(r:Partial<PatientRecord>)=>[r.lastName,r.firstName].filter(Boolean).join(', ') || r.companyName || r.additionalLine || '—';
const invoiceRecipientName=(r:Partial<PatientRecord>)=>r.recipientType==='Firma' ? (r.companyName || 'Firma') : [r.firstName,r.lastName].filter(Boolean).join(' ') || r.companyName || '—';
const emptyPatient = (): Partial<PatientRecord> => ({addressId:0,patientNo:'',addressRole:'Patient',recipientType:'Person',companyName:'',contactPerson:'',additionalLine:'',lastName:'',firstName:'',birthDate:'',email:'',phone:'',mobile:'',street:'',zip:'',city:'',insurance:'',privacyConsent:false,communicationChannel:'E-Mail',note:''});
const demoPatients = (): PatientRecord[] => [
  {id:1,addressId:1001,patientNo:'P-1001',addressRole:'Patient',recipientType:'Person',companyName:'',contactPerson:'',additionalLine:'',lastName:'Mustermann',firstName:'Max',birthDate:'1980-04-12',email:'max.mustermann@example.de',phone:'0341 000000',mobile:'',street:'Musterstraße 1',zip:'04109',city:'Leipzig',insurance:'privat',privacyConsent:true,communicationChannel:'E-Mail',note:'Demo aus adressen: Patient ist selbst Rechnungsempfänger.'},
  {id:2,addressId:1002,patientNo:'P-1002',addressRole:'Patient mit Firmenrechnung',recipientType:'Firma',companyName:'Muster GmbH',contactPerson:'Frau Müller',additionalLine:'für Erika Musterfrau',lastName:'Musterfrau',firstName:'Erika',birthDate:'1975-09-03',email:'buchhaltung@muster-gmbh.example',phone:'0341 111111',mobile:'0176 000000',street:'Beispielweg 2',zip:'04275',city:'Leipzig',insurance:'Firma / privat',privacyConsent:false,communicationChannel:'Post',note:'GAM-1.0-Fall: Rechnung an Firma, Patient steht als Zusatzzeile.'},
];
function normalizePatientAddress(raw:any, fallbackId:number): PatientRecord{
  const id=Number(raw?.id ?? fallbackId);
  const patientNo=String(raw?.patientNo ?? raw?.PATIENTENNUMMER ?? `P-${String(1000+id)}`);
  const companyName=String(raw?.companyName ?? raw?.FIRMA ?? '');
  return {
    id,
    addressId:Number(raw?.addressId ?? raw?.ID ?? id),
    patientNo,
    addressRole:String(raw?.addressRole ?? raw?.rolle ?? (companyName?'Patient mit Firmenrechnung':'Patient')),
    recipientType:String(raw?.recipientType ?? (companyName?'Firma':'Person')),
    companyName,
    contactPerson:String(raw?.contactPerson ?? raw?.ANSPRECHPARTNER ?? ''),
    additionalLine:String(raw?.additionalLine ?? raw?.ZUSATZZEILE ?? ''),
    lastName:String(raw?.lastName ?? raw?.NACHNAME ?? ''),
    firstName:String(raw?.firstName ?? raw?.VORNAME ?? ''),
    birthDate:String(raw?.birthDate ?? raw?.GEBDATUM ?? ''),
    email:String(raw?.email ?? raw?.EMAIL ?? ''),
    phone:String(raw?.phone ?? raw?.TELEFON ?? ''),
    mobile:String(raw?.mobile ?? raw?.MOBIL ?? ''),
    street:String(raw?.street ?? raw?.STRASSE ?? ''),
    zip:String(raw?.zip ?? raw?.PLZ ?? ''),
    city:String(raw?.city ?? raw?.ORT ?? ''),
    insurance:String(raw?.insurance ?? raw?.KOSTENTRAEGER ?? ''),
    privacyConsent:!!raw?.privacyConsent,
    communicationChannel:String(raw?.communicationChannel ?? 'E-Mail'),
    note:String(raw?.note ?? raw?.BEMERKUNG ?? ''),
  };
}
function loadLocalPatients(): PatientRecord[]{
  try{const raw=localStorage.getItem(PATIENT_STORAGE_KEY); if(raw){const parsed=JSON.parse(raw); if(Array.isArray(parsed)) return parsed.map((x,i)=>normalizePatientAddress(x,i+1));}}catch{}
  try{const legacy=localStorage.getItem(legacyPatientStorageKey); if(legacy){const parsed=JSON.parse(legacy); if(Array.isArray(parsed)){const migrated=parsed.map((x,i)=>normalizePatientAddress(x,i+1)); saveLocalPatients(migrated); return migrated;}}}catch{}
  return demoPatients();
}
function saveLocalPatients(rows: PatientRecord[]){localStorage.setItem(PATIENT_STORAGE_KEY, JSON.stringify(rows));}

function patientFromMasterDataAddress(raw:any, fallbackId:number): PatientRecord{
  const id=Number(raw?.ID ?? raw?.id ?? fallbackId);
  const hasCompany=String(raw?.FIRMA ?? raw?.companyName ?? '').trim().length>0;
  return normalizePatientAddress({
    ...raw,
    id,
    addressId: raw?.ID ?? raw?.addressId ?? id,
    patientNo: raw?.PATIENTENNUMMER ?? raw?.patientNo ?? `P-${String(id)}`,
    addressRole: hasCompany ? 'Patient mit Firmenrechnung' : 'Patient',
    recipientType: hasCompany ? 'Firma' : 'Person',
    companyName: raw?.FIRMA ?? raw?.companyName ?? '',
    contactPerson: raw?.ANSPRECHPARTNER ?? raw?.contactPerson ?? '',
    additionalLine: raw?.ZUSATZZEILE ?? raw?.additionalLine ?? '',
    lastName: raw?.NACHNAME ?? raw?.lastName ?? '',
    firstName: raw?.VORNAME ?? raw?.firstName ?? '',
    birthDate: raw?.GEBDATUM ?? raw?.birthDate ?? '',
    email: raw?.EMAIL ?? raw?.email ?? '',
    phone: raw?.TELEFON ?? raw?.phone ?? '',
    mobile: raw?.MOBIL ?? raw?.mobile ?? '',
    street: raw?.STRASSE ?? raw?.street ?? '',
    zip: raw?.PLZ ?? raw?.zip ?? '',
    city: raw?.ORT ?? raw?.city ?? '',
    insurance: raw?.VERSICHERTENART ?? raw?.KOSTENTRAEGER ?? raw?.insurance ?? '',
    note: raw?.VERSICHERTENNUMMER ? `Versichertennummer: ${raw.VERSICHERTENNUMMER}` : (raw?.BEMERKUNG ?? raw?.note ?? ''),
  }, fallbackId);
}

async function loadAddressPatientsFromBackend(): Promise<PatientRecord[]>{
  const result = await loadMasterDataRows('patient-addresses','',500);
  const rows = Array.isArray(result?.rows) ? result.rows : [];
  return rows.map((row,idx)=>patientFromMasterDataAddress(row, idx+1));
}

function PatientsPage(){
  const [rows,setRows]=useState<PatientRecord[]>(()=>loadLocalPatients());
  useEffect(()=>{let alive=true; loadAddressPatientsFromBackend().then(next=>{if(alive && next.length){setRows(next); saveLocalPatients(next);}}).catch(()=>{}); return()=>{alive=false};},[]);
  const [q,setQ]=useState('');
  const [open,setOpen]=useState(false);
  const [form,setForm]=useState<Partial<PatientRecord>>(emptyPatient());
  const firstRef=useRef<HTMLInputElement|null>(null);
  const filtered=rows.filter(r=>[r.patientNo,r.addressId,r.addressRole,r.recipientType,r.companyName,r.contactPerson,r.additionalLine,r.lastName,r.firstName,r.email,r.phone,r.mobile,r.city,r.insurance,r.note].join(' ').toLowerCase().includes(q.toLowerCase()));
  const persist=(next:PatientRecord[])=>{setRows(next); saveLocalPatients(next);};
  const nextNo=()=>`P-${String(Math.max(1000,...rows.map(r=>Number((r.patientNo||'').replace(/\D/g,''))||1000))+1)}`;
  const nextAddressId=()=>Math.max(1000,...rows.map(r=>Number(r.addressId)||1000))+1;
  const startNew=()=>{setForm({...emptyPatient(),patientNo:nextNo(),addressId:nextAddressId()}); setOpen(true);};
  const startEdit=(r:PatientRecord)=>{setForm({...r}); setOpen(true);};
  const save=()=>{
    const before=form.id?rows.find(r=>r.id===form.id):undefined;
    const after:PatientRecord={id:form.id??(Math.max(0,...rows.map(r=>r.id))+1),addressId:Number(form.addressId)||nextAddressId(),patientNo:form.patientNo||nextNo(),addressRole:form.addressRole||'Patient',recipientType:form.recipientType||'Person',companyName:form.companyName||'',contactPerson:form.contactPerson||'',additionalLine:form.additionalLine||'',lastName:form.lastName||'',firstName:form.firstName||'',birthDate:form.birthDate||'',email:form.email||'',phone:form.phone||'',mobile:form.mobile||'',street:form.street||'',zip:form.zip||'',city:form.city||'',insurance:form.insurance||'',privacyConsent:!!form.privacyConsent,communicationChannel:form.communicationChannel||'E-Mail',note:form.note||''};
    const next=before?rows.map(r=>r.id===after.id?after:r):[after,...rows];
    persist(next); setOpen(false);
    notifySaved(before?'Adresse/Patient geändert.':'Adresse/Patient angelegt.', buildChangeDetails('Patientenverwaltung', before?'Adresse/Patient geändert':'Adresse/Patient angelegt', before??{}, after, {addressId:'Adressen-ID',patientNo:'Patienten-Nr.',addressRole:'Adressrolle',recipientType:'Rechnungsempfänger-Typ',companyName:'Firma/Rechnungsempfänger',contactPerson:'Ansprechpartner',additionalLine:'Zusatzzeile / Patient auf Rechnung',lastName:'Nachname Patient',firstName:'Vorname Patient',birthDate:'Geburtsdatum',email:'E-Mail',phone:'Telefon',mobile:'Mobil',street:'Straße',zip:'PLZ',city:'Ort',insurance:'Kostenträger',privacyConsent:'Datenschutzfreigabe',communicationChannel:'Kommunikationsweg',note:'Bemerkung'}, `${after.patientNo} ${patientDisplayName(after)}`));
  };
  const remove=()=>{
    if(!form.id) return;
    const record=rows.find(r=>r.id===form.id);
    if(!confirm(`Adresse/Patient wirklich löschen?\n\n${record?.patientNo??''} ${record?.firstName??''} ${record?.lastName??''}\n${record?.companyName??''}`)) return;
    persist(rows.filter(r=>r.id!==form.id)); setOpen(false);
    notifySaved('Adresse/Patient gelöscht.', {module:'Patientenverwaltung',action:'Adresse/Patient gelöscht',record:record?`${record.patientNo} ${patientDisplayName(record)}`:String(form.id),summary:'Datensatz wurde aus der lokalen adressen-basierten Patientenliste entfernt.'});
  };
  return <section className="card"><h2>Patientenverwaltung</h2><p className="muted">Schritt 38j2: Patientenverwaltung basiert auf der vorhandenen GAM-1.0-Adresslogik. Patient, Firma/Rechnungsempfänger, Ansprechpartner und Zusatzzeile werden getrennt geführt, damit alte Rechnungsfälle erhalten bleiben.</p><GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neue Adresse / Patient</button><label><Search size={16}/><input placeholder="Patient, Adresse, Firma, Ansprechpartner, Nummer, E-Mail, Telefon, Ort suchen" value={q} onChange={e=>setQ(e.target.value)}/></label><button className="secondary" type="button" onClick={()=>setQ('')}>Suche leeren</button></GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr><th>Adressen-ID</th><th>Patient</th><th>Rechnungsempfänger</th><th>Ansprechpartner / Zusatzzeile</th><th>Kontakt</th><th>Adresse</th><th>Kostenträger</th><th>Aktion</th></tr></thead><tbody>{filtered.map(r=><tr key={r.id} className="clickable-row" title="Anklicken zum Bearbeiten" onClick={()=>startEdit(r)}><td>{r.addressId}<br/><small>{r.patientNo}</small></td><td><b>{patientDisplayName(r)}</b><br/><small>{r.addressRole} · {r.privacyConsent?'Datenschutz ok':'Datenschutz offen'}</small></td><td>{invoiceRecipientName(r)}<br/><small>{r.recipientType}</small></td><td>{r.contactPerson||'—'}<br/><small>{r.additionalLine||'—'}</small></td><td>{r.email||r.phone||r.mobile||'—'}<br/><small>{[r.phone,r.mobile].filter(Boolean).join(' · ')}</small></td><td>{[r.street,r.zip,r.city].filter(Boolean).join(', ')||'—'}</td><td>{r.insurance||'—'}</td><td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>)}</tbody></table></GamScrollArea><GamDialog open={open} title={form.id?`Adresse/Patient ${form.patientNo||''} bearbeiten`:'Neue Adresse / Patient'} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{form.id?'Änderungen speichern':'Adresse/Patient anlegen'}</button>{form.id&&<button className="danger" type="button" onClick={remove}>Adresse/Patient löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid"><label>Adressen-ID<input ref={firstRef} type="number" value={form.addressId??''} onChange={e=>setForm({...form,addressId:Number(e.target.value)})}/></label><label>Patienten-Nr.<input value={form.patientNo??''} onChange={e=>setForm({...form,patientNo:e.target.value})}/></label><label>Adressrolle<select value={form.addressRole??'Patient'} onChange={e=>setForm({...form,addressRole:e.target.value})}><option>Patient</option><option>Patient mit Firmenrechnung</option><option>Rechnungsempfänger</option><option>Ansprechpartner</option><option>Firma</option></select></label><label>Rechnungsempfänger-Typ<select value={form.recipientType??'Person'} onChange={e=>setForm({...form,recipientType:e.target.value})}><option>Person</option><option>Firma</option></select></label><label>Nachname Patient<input value={form.lastName??''} onChange={e=>setForm({...form,lastName:e.target.value})}/></label><label>Vorname Patient<input value={form.firstName??''} onChange={e=>setForm({...form,firstName:e.target.value})}/></label><label>Geburtsdatum<input type="date" value={form.birthDate??''} onChange={e=>setForm({...form,birthDate:e.target.value})}/></label><label>Firma / Rechnungsempfänger<input value={form.companyName??''} onChange={e=>setForm({...form,companyName:e.target.value})}/></label><label>Ansprechpartner<input value={form.contactPerson??''} onChange={e=>setForm({...form,contactPerson:e.target.value})}/></label><label className="wide-field">Zusatzzeile / Patient auf Rechnung<input value={form.additionalLine??''} onChange={e=>setForm({...form,additionalLine:e.target.value})} placeholder="z. B. für Max Mustermann"/></label><label>E-Mail<input type="email" value={form.email??''} onChange={e=>setForm({...form,email:e.target.value})}/></label><label>Telefon<input value={form.phone??''} onChange={e=>setForm({...form,phone:e.target.value})}/></label><label>Mobil<input value={form.mobile??''} onChange={e=>setForm({...form,mobile:e.target.value})}/></label><label>Kommunikationsweg<select value={form.communicationChannel??'E-Mail'} onChange={e=>setForm({...form,communicationChannel:e.target.value})}><option>E-Mail</option><option>Telefon</option><option>Post</option><option>Patientenportal</option></select></label><label className="wide-field">Straße<input value={form.street??''} onChange={e=>setForm({...form,street:e.target.value})}/></label><label>PLZ<input value={form.zip??''} onChange={e=>setForm({...form,zip:e.target.value})}/></label><label>Ort<input value={form.city??''} onChange={e=>setForm({...form,city:e.target.value})}/></label><label>Kostenträger<input value={form.insurance??''} onChange={e=>setForm({...form,insurance:e.target.value})}/></label><label className="checkbox-label"><input type="checkbox" checked={!!form.privacyConsent} onChange={e=>setForm({...form,privacyConsent:e.target.checked})}/> Datenschutz-/Kommunikationsfreigabe liegt vor</label><label className="wide-field">Bemerkung<textarea value={form.note??''} onChange={e=>setForm({...form,note:e.target.value})}/></label></div></GamDialog></section>;
}

type AppointmentRecord = {
  id:number;
  appointmentNo:string;
  patientNo:string;
  patientName:string;
  date:string;
  startTime:string;
  endTime:string;
  type:string;
  status:string;
  room:string;
  practitioner:string;
  reminder:boolean;
  targetScope?: string;
  targetUser?: string;
  targetRole?: string;
  note:string;
};

type AppointmentDueLevel = 'overdue' | 'soon' | null;
const APPOINTMENT_STORAGE_KEY = 'gam38k.appointments';
const appointmentToday = () => { const d=new Date(); d.setHours(0,0,0,0); return d; };
function appointmentDayDiff(dateText?:string){
  if(!dateText) return null;
  const d=new Date(`${dateText}T00:00:00`);
  if(Number.isNaN(d.getTime())) return null;
  const today=appointmentToday();
  return Math.round((d.getTime()-today.getTime())/86400000);
}
function appointmentDoneStatus(status?:string){
  const s=String(status||'').toLowerCase();
  return ['wahrgenommen','erledigt','abgeschlossen','abgesagt','ausgefallen','storniert'].includes(s);
}
function appointmentDueInfo(r:AppointmentRecord): {level:AppointmentDueLevel; days:number; label:string}|null{
  if(appointmentDoneStatus(r.status)) return null;
  const days=appointmentDayDiff(r.date);
  if(days===null) return null;
  if(days<0) return {level:'overdue', days, label:`seit ${Math.abs(days)} Tag${Math.abs(days)===1?'':'en'} überfällig`};
  if(days<=14) return {level:'soon', days, label:days===0?'heute fällig':`in ${days} Tag${days===1?'':'en'} fällig`};
  return null;
}
function appointmentVisibleForCurrentUser(r:AppointmentRecord, account?:AccountDto|null){
  const scope=String(r.targetScope||'alle').toLowerCase();
  if(scope==='alle' || scope==='global' || scope==='') return true;
  if(scope.includes('benutzer')) return !r.targetUser || String(r.targetUser).toLowerCase()===String(account?.username||'').toLowerCase() || String(r.targetUser).toLowerCase()===String(account?.fullname||'').toLowerCase();
  if(scope.includes('rolle')) return !r.targetRole || String(r.targetRole).toLowerCase()===String(account?.role||'').toLowerCase();
  return true;
}
const emptyAppointment = (): Partial<AppointmentRecord> => ({appointmentNo:'',patientNo:'',patientName:'',date:'',startTime:'09:00',endTime:'09:30',type:'Interner Termin',status:'geplant',room:'',practitioner:'',reminder:true,targetScope:'alle',targetUser:'',targetRole:'',note:''});
const demoAppointments = (): AppointmentRecord[] => [
  {id:1,appointmentNo:'T-1001',patientNo:'',patientName:'',date:new Date().toISOString().slice(0,10),startTime:'09:00',endTime:'09:30',type:'Interner Termin',status:'geplant',room:'Besprechungsraum',practitioner:'',reminder:true,targetScope:'alle',targetUser:'',targetRole:'',note:'Demo: interner Termin für Schritt 38q'},
  {id:2,appointmentNo:'T-1002',patientNo:'',patientName:'',date:new Date(Date.now()+86400000*7).toISOString().slice(0,10),startTime:'10:00',endTime:'10:45',type:'Wartung',status:'geplant',room:'Technik',practitioner:'',reminder:true,targetScope:'alle',targetUser:'',targetRole:'',note:'Demo: bald fälliger interner Termin'},
];
function loadLocalAppointments(): AppointmentRecord[]{
  try{const raw=localStorage.getItem(APPOINTMENT_STORAGE_KEY); if(raw){const parsed=JSON.parse(raw); if(Array.isArray(parsed)) return parsed;}}catch{}
  return demoAppointments();
}
function saveLocalAppointments(rows: AppointmentRecord[]){localStorage.setItem(APPOINTMENT_STORAGE_KEY, JSON.stringify(rows));}

function AppointmentsPage({account}:{account?:AccountDto|null}){
  const [rows,setRows]=useState<AppointmentRecord[]>(()=>loadLocalAppointments());
  const [patients,setPatients]=useState<PatientRecord[]>(()=>loadLocalPatients());
  useEffect(()=>{let alive=true; loadAddressPatientsFromBackend().then(next=>{if(alive && next.length) setPatients(next);}).catch(()=>{}); return()=>{alive=false};},[]);
  const [q,setQ]=useState('');
  const [dueFilter,setDueFilter]=useState<'all'|'overdue'|'soon'>('all');
  const [open,setOpen]=useState(false);
  const [form,setForm]=useState<Partial<AppointmentRecord>>(emptyAppointment());
  const firstRef=useRef<HTMLInputElement|null>(null);
  const relevantRows=rows.filter(r=>appointmentVisibleForCurrentUser(r, account));
  const overdueRows=relevantRows.filter(r=>appointmentDueInfo(r)?.level==='overdue');
  const soonRows=relevantRows.filter(r=>appointmentDueInfo(r)?.level==='soon');
  useEffect(()=>{ if(overdueRows.length>0) gamNotify('error', `Terminverwaltung: ${overdueRows.length} überfällige interne Termin${overdueRows.length===1?'':'e'} gefunden.`, 0); else if(soonRows.length>0) gamNotify('warning', `Terminverwaltung: ${soonRows.length} interne Termin${soonRows.length===1?'':'e'} in den nächsten 14 Tagen.`, 7000); },[]);
  const filtered=relevantRows.filter(r=>{
    const due=appointmentDueInfo(r);
    if(dueFilter==='overdue' && due?.level!=='overdue') return false;
    if(dueFilter==='soon' && due?.level!=='soon') return false;
    return [r.appointmentNo,r.patientNo,r.patientName,r.date,r.startTime,r.endTime,r.type,r.status,r.room,r.practitioner,r.targetScope,r.targetUser,r.targetRole,r.note].join(' ').toLowerCase().includes(q.toLowerCase());
  });
  const persist=(next:AppointmentRecord[])=>{setRows(next); saveLocalAppointments(next);};
  const nextNo=()=>`T-${String(Math.max(1000,...rows.map(r=>Number((r.appointmentNo||'').replace(/\D/g,''))||1000))+1)}`;
  const startNew=()=>{setForm({...emptyAppointment(),appointmentNo:nextNo(),date:new Date().toISOString().slice(0,10)}); setOpen(true);};
  const startEdit=(r:AppointmentRecord)=>{setForm({...r}); setOpen(true);};
  const applyPatient=(patientNo:string)=>{ const p=patients.find(x=>x.patientNo===patientNo); setForm({...form,patientNo,patientName:p?[p.firstName,p.lastName].filter(Boolean).join(' '):form.patientName}); };
  const save=()=>{
    const before=form.id?rows.find(r=>r.id===form.id):undefined;
    const after:AppointmentRecord={id:form.id??(Math.max(0,...rows.map(r=>r.id))+1),appointmentNo:form.appointmentNo||nextNo(),patientNo:form.patientNo||'',patientName:form.patientName||'',date:form.date||'',startTime:form.startTime||'',endTime:form.endTime||'',type:form.type||'Interner Termin',status:form.status||'geplant',room:form.room||'',practitioner:form.practitioner||'',reminder:!!form.reminder,targetScope:form.targetScope||'alle',targetUser:form.targetUser||'',targetRole:form.targetRole||'',note:form.note||''};
    const next=before?rows.map(r=>r.id===after.id?after:r):[after,...rows];
    persist(next); setOpen(false);
    notifySaved(before?'Termin geändert.':'Termin angelegt.', buildChangeDetails('Terminverwaltung', before?'Termin geändert':'Termin angelegt', before??{}, after, {appointmentNo:'Termin-Nr.',patientNo:'Patienten-Nr.',patientName:'Patient',date:'Datum',startTime:'Beginn',endTime:'Ende',type:'Terminart',status:'Status',room:'Raum',practitioner:'Verantwortlich',targetScope:'Zielgruppe',targetUser:'Benutzer',targetRole:'Rolle',reminder:'Erinnerung',note:'Bemerkung'}, `${after.appointmentNo} ${after.type}`));
  };
  const remove=()=>{ if(!form.id) return; const record=rows.find(r=>r.id===form.id); if(!confirm(`Termin wirklich löschen?\n\n${record?.appointmentNo??''} ${record?.type??''} ${record?.date??''}`)) return; persist(rows.filter(r=>r.id!==form.id)); setOpen(false); notifySaved('Termin gelöscht.', {module:'Terminverwaltung',action:'Termin gelöscht',record:record?`${record.appointmentNo} ${record.type}`:String(form.id),summary:'Termin wurde aus der lokalen Terminliste entfernt.'}); };
  return <section className="card"><h2>Terminverwaltung</h2><p className="muted">Schritt 38q: interne Terminverwaltung mit Frühwarnsystem. GAM warnt rot vor überfälligen und gelb vor bald fälligen internen Terminen; Patiententermine bleiben später OpenReception vorbehalten.</p><div className="appointment-warning-panel">{overdueRows.length>0&&<div className="note internal-overdue" role="alert" onClick={()=>setDueFilter('overdue')}><b>🔴 Überfällige interne Termine</b><br/>{overdueRows.slice(0,5).map(r=><small key={r.id}>{r.date} · {r.type} · {appointmentDueInfo(r)?.label}</small>)}{overdueRows.length>5&&<small>… und {overdueRows.length-5} weitere</small>}</div>}{soonRows.length>0&&<div className="note internal-soon" onClick={()=>setDueFilter('soon')}><b>🟡 Interne Termine in den nächsten 14 Tagen</b><br/>{soonRows.slice(0,5).map(r=><small key={r.id}>{r.date} · {r.type} · {appointmentDueInfo(r)?.label}</small>)}{soonRows.length>5&&<small>… und {soonRows.length-5} weitere</small>}</div>}</div><GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neuer interner Termin</button><label><Search size={16}/><input placeholder="Termin, Datum, Status, Raum, Benutzer, Rolle suchen" value={q} onChange={e=>setQ(e.target.value)}/></label><button className={dueFilter==='all'?'secondary active':'secondary'} type="button" onClick={()=>setDueFilter('all')}>Alle</button><button className={dueFilter==='overdue'?'danger':'secondary'} type="button" onClick={()=>setDueFilter('overdue')}>Überfällig</button><button className="secondary" type="button" onClick={()=>setDueFilter('soon')}>Nächste 14 Tage</button><button className="secondary" type="button" onClick={()=>setQ('')}>Suche leeren</button></GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr><th>Fälligkeit</th><th>Nr.</th><th>Datum</th><th>Zeit</th><th>Termin</th><th>Status</th><th>Raum</th><th>Zielgruppe</th><th>Aktion</th></tr></thead><tbody>{filtered.map(r=>{const due=appointmentDueInfo(r); return <tr key={r.id} className={due?.level==='overdue'?"clickable-row appointment-overdue":due?.level==='soon'?"clickable-row appointment-soon":"clickable-row"} title="Anklicken zum Bearbeiten" onClick={()=>startEdit(r)}><td>{due?.level==='overdue'?'🔴':due?.level==='soon'?'🟡':'—'} {due?.label||''}</td><td>{r.appointmentNo}</td><td>{r.date||'—'}</td><td>{[r.startTime,r.endTime].filter(Boolean).join(' – ')||'—'}</td><td><b>{r.type||'—'}</b><br/><small>{r.note||r.patientName||'—'}</small></td><td>{r.status||'—'}</td><td>{r.room||'—'}</td><td>{r.targetScope||'alle'}{r.targetUser?` · ${r.targetUser}`:''}{r.targetRole?` · ${r.targetRole}`:''}</td><td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>})}</tbody></table></GamScrollArea><GamDialog open={open} title={form.id?`Termin ${form.appointmentNo||''} bearbeiten`:'Neuer interner Termin'} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{form.id?'Änderungen speichern':'Termin anlegen'}</button>{form.id&&<button className="danger" type="button" onClick={remove}>Termin löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid"><label>Termin-Nr.<input ref={firstRef} value={form.appointmentNo??''} onChange={e=>setForm({...form,appointmentNo:e.target.value})}/></label><label>Terminart<select value={form.type??'Interner Termin'} onChange={e=>setForm({...form,type:e.target.value})}><option>Interner Termin</option><option>Teamsitzung</option><option>Wartung</option><option>Geräteprüfung</option><option>Einweisung</option><option>Inbetriebnahme</option><option>Fortbildung</option><option>Liefertermin</option><option>Sonstiges</option></select></label><label>Datum<input type="date" value={form.date??''} onChange={e=>setForm({...form,date:e.target.value})}/></label><label>Beginn<input type="time" value={form.startTime??''} onChange={e=>setForm({...form,startTime:e.target.value})}/></label><label>Ende<input type="time" value={form.endTime??''} onChange={e=>setForm({...form,endTime:e.target.value})}/></label><label>Status<select value={form.status??'geplant'} onChange={e=>setForm({...form,status:e.target.value})}><option>geplant</option><option>bestätigt</option><option>in Arbeit</option><option>wahrgenommen</option><option>erledigt</option><option>abgeschlossen</option><option>abgesagt</option><option>ausgefallen</option></select></label><label>Raum / Ort<input value={form.room??''} onChange={e=>setForm({...form,room:e.target.value})}/></label><label>Verantwortlich<input value={form.practitioner??''} onChange={e=>setForm({...form,practitioner:e.target.value})}/></label><label>Zielgruppe<select value={form.targetScope??'alle'} onChange={e=>setForm({...form,targetScope:e.target.value})}><option value="alle">alle Benutzer</option><option value="Benutzer">einzelner Benutzer</option><option value="Rolle">bestimmte Rolle</option></select></label><label>Benutzer<input value={form.targetUser??''} onChange={e=>setForm({...form,targetUser:e.target.value})} placeholder="z. B. admin"/></label><label>Rolle<input value={form.targetRole??''} onChange={e=>setForm({...form,targetRole:e.target.value})} placeholder="z. B. superadmin"/></label><label className="checkbox-label"><input type="checkbox" checked={!!form.reminder} onChange={e=>setForm({...form,reminder:e.target.checked})}/> Beim Start/Login erinnern</label><label className="wide-field">Bemerkung<textarea value={form.note??''} onChange={e=>setForm({...form,note:e.target.value})}/></label></div></GamDialog></section>;
}

function App(){
  const [authed,setAuthed]=useState(!!token());
  const [uiLanguage,setUiLanguage]=useState<GamLanguage>(currentUiLanguage());
  const [startPage,setStartPage]=useState<Page>(storedStartPage());
  useEffect(()=>{document.body.classList.toggle('gam-authenticated', authed); return()=>document.body.classList.remove('gam-authenticated');},[authed]);
  useEffect(()=>{const h=()=>{setAuthed(false); notifySessionExpired();}; window.addEventListener("gam-auth-expired",h); return()=>window.removeEventListener("gam-auth-expired",h)},[]);
  useEffect(()=>{const h=(e:any)=>setUiLanguage(normalizeGamLanguage(e?.detail || currentUiLanguage())); window.addEventListener('gam-ui-language-changed', h as any); window.addEventListener('gam-ui-translations-loaded', h as any); window.addEventListener('storage', h as any); return()=>{window.removeEventListener('gam-ui-language-changed', h as any); window.removeEventListener('gam-ui-translations-loaded', h as any); window.removeEventListener('storage', h as any);};},[]);
  const handleLogout = async()=>{
    try{
      await logout();
      setAuthed(false);
      notifyLogoutSuccess();
    }catch(ex:any){
      setAuthed(false);
      notifyLogoutError(ex);
    }
  };
  return <>
    <GamNotificationCenter defaultTimeoutMs={5000} onLogout={authed ? handleLogout : undefined}/>
    {authed?<Shell key={`shell-${startPage}`} uiLanguage={uiLanguage} initialPage={startPage} onLogout={handleLogout}/>:<Login onLogin={(targetPage)=>{const finalTarget=consumeLoginTargetPage(targetPage); setUiLanguage(currentUiLanguage()); setStartPage(finalTarget); setAuthed(true)}}/>}
  </>
}

function effectiveModules(account: AccountDto|null, menu: RoleDto|null): string[] {
  const fallback = ['dashboard','invoices','invoiceAdmin','inventory','warehouse','patients','appointments','tasks','approvals','orders','personnel','cashbook','workplace','priceList','compliance','reports','communication','users'];
  const roleText = `${account?.role ?? ''} ${menu?.label ?? ''}`.toLowerCase();
  const isSuperAdmin = roleText.includes('superadmin') || roleText.includes('super-administrator');
  const fromMenu = menu?.modules ?? [];
  if (isSuperAdmin) return Array.from(new Set([...fallback, ...fromMenu]));
  return fromMenu.length ? fromMenu : ['dashboard'];
}


function fallbackSuperadminMenu(): RoleDto {
  return {
    label: 'Super-Administration',
    modules: ['dashboard','invoices','invoiceAdmin','inventory','warehouse','patients','appointments','tasks','approvals','orders','personnel','cashbook','workplace','priceList','compliance','reports','communication','users']
  } as RoleDto;
}

function Shell({onLogout,uiLanguage,initialPage}:{onLogout:()=>void; uiLanguage:GamLanguage; initialPage:Page}){const [account,setAccount]=useState<AccountDto|null>(null); const [menu,setMenu]=useState<RoleDto|null>(null); const readHardStartPage=()=>normalizeStartPage(localStorage.getItem('gam_forced_login_target_page') || sessionStorage.getItem('gam_login_target_page') || localStorage.getItem('gam_selected_application_page') || initialPage || storedStartPage()); const [page,setPage]=useState<Page>(()=>readHardStartPage());
 useEffect(()=>{
   // Schritt 38k6: harte Login-Zielmodule sofort setzen, bevor Rollen-/Menü-Fallbacks greifen.
   const desired = readHardStartPage();
   if (['patients','appointments','inventory','warehouse','invoices','invoiceAdmin','personnel'].includes(desired)) setPage(desired);
 },[initialPage]);
 useUiTranslationCache(uiLanguage, true, 0);
 useEffect(()=>{
   const selectStartPage = (m: RoleDto) => {
     const desired = normalizeStartPage(localStorage.getItem('gam_forced_login_target_page') || sessionStorage.getItem('gam_login_target_page') || initialPage || storedStartPage());
     const modules = m.modules ?? [];
     // Direkte Login-Ziele sind absichtlich vorrangig.
     // Patientenverwaltung und Terminverwaltung dürfen nicht über das alte
     // Rechnungsprogramm-Fallback zurückgesetzt werden, nur weil sie noch nicht
     // in jeder historischen Role/Menu-Antwort enthalten sind.
     const directLoginPages: Page[] = HARD_LOGIN_TARGET_PAGES;
     if (directLoginPages.includes(desired) || modules.includes(desired)) {
       setPage(desired);
       sessionStorage.removeItem('gam_login_target_page');
       localStorage.removeItem('gam_forced_login_target_page');
     } else if (modules.includes('invoices')) {
       setPage('invoices');
     } else if (modules.length) {
       setPage(modules[0] as Page);
     } else {
       setPage('dashboard');
     }
   };
   me().then(setAccount).catch(()=>{notifySessionExpired(); onLogout();});
   loadMenu().then(m=>{setMenu(m); selectStartPage(m);}).catch(()=>{const fallback=fallbackSuperadminMenu(); setMenu(fallback); selectStartPage(fallback);})
 },[initialPage]);
 const modules=effectiveModules(account, menu);
 const nav=[['dashboard','Dashboard',LayoutDashboard],['invoices','Rechnungsprogramm',FileText],['invoiceAdmin','Rechnungsadministration',FileText],['inventory','Geräteverzeichnis',Package],['warehouse','Lagerverwaltung',Warehouse],['patients','Patientenverwaltung',UserRound],['appointments','Terminverwaltung',CalendarDays],['tasks','Aufgabenverwaltung',CheckSquare],['approvals','Freigabemanagement',ClipboardCheck],['orders','Bestelltool',ClipboardList],['personnel','Personaldaten',BriefcaseBusiness],['cashbook','Kassenbuch',Landmark],['workplace','Arbeitsplatzausstattung',Package],['priceList','Preisliste',FileText],['compliance','Prüfungen',ClipboardList],['reports','Reports',FileBarChart],['communication','Kommunikation',ClipboardList],['moduleAdmin','Modul-Admin',ShieldCheck],['users','Benutzer/Rechte',UsersRound]] as const;
 return <main><header><div><h1>GAM 2.0</h1><span>{account?.fullname||account?.username} · {ui('role', uiLanguage)}: {menu?.label||account?.role||'—'} · {ui('step31ModuleOverviewShort', uiLanguage)}</span></div></header><nav className="tabs module-tabs">{nav.map(([key,label,Icon])=><button key={key} className={page===key?'active':''} onClick={()=>setPage(key as Page)}><img src={iconForModule(label)} alt="" className="module-button-icon nav-module-icon" />{moduleText(uiLanguage, label)}</button>)}</nav>{page==='dashboard'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'dashboard')}><DashboardHome/></ModuleErrorBoundary>}{page==='invoices'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'invoices')}><InvoicesPage/></ModuleErrorBoundary>}{page==='invoiceAdmin'&&<ModuleErrorBoundary title='Rechnungsadministration'><InvoiceAdminPage/></ModuleErrorBoundary>}{page==='users'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'admin')}><UsersPage/></ModuleErrorBoundary>}{page==='inventory'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'inventory')}><InventoryPage/></ModuleErrorBoundary>}{page==='warehouse'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'warehouse')}><><WarehousePage/><InventoryWarehousePage/></></ModuleErrorBoundary>}{page==='patients'&&<ModuleErrorBoundary title='Patientenverwaltung'><PatientsPage/></ModuleErrorBoundary>}{page==='appointments'&&<ModuleErrorBoundary title='Terminverwaltung'><AppointmentsPage account={account}/></ModuleErrorBoundary>}{page==='tasks'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'tasks')}><WorkflowTasksPage/></ModuleErrorBoundary>} {page==='approvals'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'approval')}><WorkflowApprovalsPage/></ModuleErrorBoundary>} {page==='orders'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'orders')}><OrdersPage/></ModuleErrorBoundary>}{page==='personnel'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'personnel')}><PersonnelPage account={account}/></ModuleErrorBoundary>} {page==='cashbook'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'cashbook')}><CashbookPage/></ModuleErrorBoundary>} {page==='workplace'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'workplace')}><WorkplacePage/></ModuleErrorBoundary>}{page==='priceList'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'price')}><PriceListPage/></ModuleErrorBoundary>}{page==='compliance'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'checks')}><CompliancePage/></ModuleErrorBoundary>} {page==='reports'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'reports')}><ReportsPage/></ModuleErrorBoundary>}{page==='communication'&&<ModuleErrorBoundary title='Kommunikation'><CommunicationCenterPage/></ModuleErrorBoundary>}{page==='moduleAdmin'&&<ModuleErrorBoundary title='Modul-Administration'><ModuleAdminPage/></ModuleErrorBoundary>}</main>}
function Placeholder({title,text}:{title:string;text:string}){return <section className="card"><h2>{title}</h2><p className="muted">{text}</p></section>}

const INVOICE_ADMIN_CATALOGS = [
  {key:'invoice-companies', label:'Gesellschaften'},
  {key:'invoice-products', label:'Produkte / Preise / MwSt'},
  {key:'company-branch-links', label:'Gesellschaft/Filiale'},
  {key:'invoice-salutations', label:'Anreden'},
  {key:'invoice-texts', label:'Rechnungstexte'},
  {key:'invoice-legal-notes', label:'Rechtliche Hinweise'},
  {key:'invoice-greetings', label:'Grußformeln'},
  {key:'invoice-text-assignments', label:'Textzuordnung Gesellschaft'},
  {key:'invoice-logos', label:'Logos'}
];

const invoiceAdminHints: Record<string,string> = {
  'invoice-products': 'GAM-1.0-nahe Produktpflege: Produkt anlegen/bearbeiten, Preiswechsel, MwSt-Wechsel sowie Angebot sichtbar ab/bis. Die Rechnungserfassung nutzt diese Werte stichtagsbezogen.',
  'invoice-companies': 'Rechnungsrelevante Gesellschaftsdaten inklusive Steuer-, Kontakt-, Bankdaten und Logo-Zuordnung. Logos werden nur einmal hochgeladen und hier ausgewählt.',
  'invoice-salutations': 'Anreden werden als einzelne wiederverwendbare Textbausteine gepflegt.',
  'invoice-texts': 'Rechnungstexte werden als einzelne wiederverwendbare Textbausteine gepflegt.',
  'invoice-legal-notes': 'Rechtliche Hinweise werden als einzelne wiederverwendbare Textbausteine gepflegt.',
  'invoice-greetings': 'Grußformeln werden als einzelne wiederverwendbare Textbausteine gepflegt.',
  'invoice-text-assignments': 'Hier wird je Gesellschaft ausgewählt, welche vorhandene Anrede, welcher Rechnungstext, welcher rechtliche Hinweis und welche Grußformel verwendet werden.',
  'company-branch-links': 'Zuordnung von Rechnungsgesellschaften zu Filialen.',
};

function invoiceAdminFieldType(name:string){
  const n=name.toLowerCase();
  if(n.includes('datum') || n.includes('gueltig') || n.includes('gültig')) return 'date';
  if(n.includes('preis') || n.includes('mwst') || n.includes('konto') || n.endsWith('_id') || n==='id' || n.includes('filiale')) return 'number';
  if(n.includes('email')) return 'email';
  return 'text';
}
function invoiceAdminLabel(row:Record<string,unknown>, catalog?:MasterDataCatalog|null){
  if(!row) return 'Datensatz';
  return String(row['gesellschaftsname'] ?? row['beschreibung'] ?? row['code'] ?? row['NAME'] ?? row['name'] ?? row['RECHNUNGSTEXT'] ?? row['ANREDE'] ?? row['TEXT'] ?? row['URL'] ?? row[catalog?.primaryKey || 'ID'] ?? 'Datensatz');
}
function invoiceAdminLongField(name:string){
  const n=name.toLowerCase();
  return (n.includes('text') && !n.endsWith('_id')) || n.includes('beschreibung') || n.includes('adresse') || (n.includes('hinweis') && !n.endsWith('_id')) || (n.includes('anrede') && !n.endsWith('_id')) || n.includes('gruss') || n.includes('gruß');
}
function invoiceAdminEmpty(fields:string[]){ const row:Record<string,unknown>={}; fields.forEach(f=>row[f]=''); return row; }
function parseInvoiceAdminValue(key:string, value:unknown){
  const type=invoiceAdminFieldType(key);
  const raw=String(value ?? '').trim();
  if(raw==='') return null;
  if(type==='number'){
    const n=Number(raw.replace(',','.'));
    return Number.isFinite(n) ? n : raw;
  }
  return raw;
}
const INVOICE_TEXT_ADMIN_CATALOGS = new Set(['invoice-salutations','invoice-texts','invoice-legal-notes','invoice-greetings']);
const INVOICE_TEXT_ASSIGNMENT_CATALOG = 'invoice-text-assignments';
const INVOICE_LOGO_CATALOG = 'invoice-logos';

type InvoiceAdminOption = { id:string; label:string; companyId?:string; url?:string };
function invoiceAdminName(row:Record<string,unknown>, keys:string[], fallback:string){
  for(const k of keys){ const v=row[k]; if(v!==undefined && v!==null && String(v).trim()!=='') return String(v).trim(); }
  return fallback;
}
function invoiceAdminOptionId(row:Record<string,unknown>, keys:string[]){
  for(const k of keys){ const v=row[k]; if(v!==undefined && v!==null && String(v).trim()!=='') return String(v).trim(); }
  return '';
}
function buildInvoiceCompanyOptions(rows:Record<string,unknown>[]):InvoiceAdminOption[]{
  return rows.map(r=>{
    const id=invoiceAdminOptionId(r,['id','ID','GESELLSCHAFTS_ID','gesellschafts_id']);
    const name=invoiceAdminName(r,['gesellschaftsname','GESELLSCHAFTSNAME','name','Name'],id||'Gesellschaft');
    const short=invoiceAdminName(r,['gesellschaftskürzel','GESELLSCHAFTSKUERZEL','gesellschaftskürzel'], '');
    const place=invoiceAdminName(r,['post_plz_ort','POST_PLZ_ORT','ORT'], '');
    return {id, label:[name, short && short!==name ? short : '', place && place!==name ? place : ''].filter(Boolean).join(' · ')};
  }).filter(o=>o.id).sort((a,b)=>a.label.localeCompare(b.label,'de'));
}
function buildInvoiceBranchOptions(rows:Record<string,unknown>[], links:Record<string,unknown>[]):InvoiceAdminOption[]{
  const companyByBranch=new Map<string,string>();
  links.forEach(l=>{
    const branch=invoiceAdminOptionId(l,['FILIALE_ID','filiale_id']);
    const company=invoiceAdminOptionId(l,['RGESELLSCHAFTS_ID','rgesellschafts_id','GESELLSCHAFT_ID','gesellschaft_id']);
    if(branch && company && !companyByBranch.has(branch)) companyByBranch.set(branch, company);
  });
  return rows.map(r=>{
    const id=invoiceAdminOptionId(r,['FILIALE_ID','filiale_id','id','ID']);
    const name=invoiceAdminName(r,['FILIALENAME','filialename','name','Name'],id||'Filiale');
    const short=invoiceAdminName(r,['FILIALEKUERZEL','filialekuerzel'], '');
    const place=invoiceAdminName(r,['ORT','ort'], '');
    return {id, label:[name, short && short!==name ? short : '', place && place!==name ? place : ''].filter(Boolean).join(' · '), companyId:companyByBranch.get(id)};
  }).filter(o=>o.id).sort((a,b)=>a.label.localeCompare(b.label,'de'));
}

function buildInvoiceTextOptions(rows:Record<string,unknown>[]):InvoiceAdminOption[]{
  return rows.map(r=>{
    const id=invoiceAdminOptionId(r,['ID','id']);
    const text=invoiceAdminName(r,['TEXT','text'], id||'Textbaustein');
    const short=text.length>120 ? text.slice(0,117)+'…' : text;
    return {id, label:id==='0' ? `🔒 Systemstandard · ${short}` : short};
  }).filter(o=>o.id).sort((a,b)=>a.id==='0'?-1:b.id==='0'?1:a.label.localeCompare(b.label,'de'));
}
function buildInvoiceLogoOptions(rows:Record<string,unknown>[]):InvoiceAdminOption[]{
  return rows.map(r=>{
    const id=invoiceAdminOptionId(r,['ID','id']);
    const url=invoiceAdminName(r,['URL','url'], '');
    const name=invoiceAdminName(r,['NAME','name'], '');
    const file=(url || id || 'Logo').split('/').filter(Boolean).pop() || url || id || 'Logo';
    const rawLabel=name || file;
    const label=id==='0' ? `🔒 Systemstandard · ${rawLabel}` : rawLabel;
    return {id, url, label:label.length>100 ? label.slice(0,97)+'…' : label};
  }).filter(o=>o.id).sort((a,b)=>a.id==='0'?-1:b.id==='0'?1:a.label.localeCompare(b.label,'de'));
}
function invoiceLogoPreviewUrlById(options:InvoiceAdminOption[], value:unknown){
  const id=String(value??'');
  const opt=options.find(o=>String(o.id)===id);
  return opt?.url || '';
}
function invoiceLogoImage(src:string, alt='Logo-Vorschau', small=false){
  if(!src) return null;
  return <img src={src} alt={alt} onError={(e)=>{(e.currentTarget as HTMLImageElement).style.display='none';}} style={{maxWidth:small?'120px':'280px',maxHeight:small?'54px':'120px',objectFit:'contain',background:'#fff',padding:'6px',borderRadius:'6px',border:'1px solid #d0d7de',marginTop:'6px'}}/>;
}
function isInvoiceCompanyField(name:string){ const n=name.toLowerCase(); return n==='rgesellschafts_id' || n==='rgesellschaft_id' || n==='gesellschaft_id'; }
function isInvoiceBranchField(name:string){ return name.toLowerCase()==='filiale_id'; }
function invoiceAdminOptionLabel(options:InvoiceAdminOption[], value:unknown){ const id=String(value??''); return options.find(o=>String(o.id)===id)?.label || (id ? `ID ${id}` : ''); }
function invoiceAdminFriendlyField(name:string){
  const labels:Record<string,string>={anrede_id:'Anrede', rechnungstext_id:'Rechnungstext', rechtlicher_hinweis_id:'Rechtlicher Hinweis', grussformel_id:'Grußformel', LOGO_ID:'Logo', logo_id:'Logo', rdaten_id:'Produkt-ID',filiale_id:'Filiale',code:'Produktcode',beschreibung:'Produktdetails / Rechnungstext', 'abkürzung':'Abkürzung', kategorie:'Kategorie', preis1:'Aktueller Preis', preisneu:'Neuer Preis', preisalt:'Alter Preis', preis_gueltigab:'Neuer Preis gültig ab', mwst:'Aktuelle MwSt', mwstalt:'Alte MwSt', mwst_gueltigab:'Neue MwSt gültig ab', konto:'Buchungskonto', rgesellschafts_id:'Rechnungsgesellschaft', rgesellschaft_id:'Rechnungsgesellschaft', gesellschaft_id:'Gesellschaft', auftraggeber:'Auftraggeber', 'durchführender':'Durchführender', 'gültig_ab':'Produkt sichtbar / verkaufbar ab', 'gültig_bis':'Produkt sichtbar / verkaufbar bis', text:'Text', name:'Name', url:'Logo-/Bildpfad', anrede:'Anrede', rechnungstext:'Rechnungstext', rechtlicher_hinweis:'Rechtlicher Hinweis', grussformel:'Grußformel'};
  return labels[name.toLowerCase()] ?? name;
}
function invoiceAdminSectionForField(name:string){
  const n=name.toLowerCase();
  if(['code','beschreibung','abkürzung','kategorie','filiale_id','rgesellschafts_id','auftraggeber','durchführender'].includes(n)) return 'Produkt';
  if(n.includes('preis')) return 'Preise';
  if(n.includes('mwst') || n.includes('konto')) return 'Steuer / Konto';
  if(n.includes('gültig') || n.includes('gueltig')) return 'Angebotszeitraum';
  return 'Weitere Felder';
}

function invoiceAdminEditorFieldClass(key:string){
  const n=key.toLowerCase();
  const parts=['invoice-admin-editor-field'];
  if(invoiceAdminLongField(key) || n.includes('rechnungstext') || n.includes('rechtlicher') || n.includes('hinweis')) parts.push('invoice-admin-field-long');
  else if(n.includes('anrede') || n.includes('gruss') || n.includes('gruß')) parts.push('invoice-admin-field-medium');
  else if(isInvoiceCompanyField(key) || isInvoiceBranchField(key)) parts.push('invoice-admin-field-medium');
  else parts.push('invoice-admin-field-short');
  return parts.join(' ');
}
function invoiceAdminIsTextCatalog(key:string){ return INVOICE_TEXT_ADMIN_CATALOGS.has(key); }
function invoiceAdminIsLogoCatalog(key:string){ return key==='invoice-logos'; }
function invoiceAdminIsProtectedSystemFallback(selected:string, row:Record<string,unknown>, catalog?:MasterDataCatalog|null){ return (invoiceAdminIsTextCatalog(selected) || invoiceAdminIsLogoCatalog(selected)) && String(rowValue(row, catalog?.primaryKey || 'ID'))==='0'; }
async function translateInvoiceAdminTextIfNeeded(catalogKey:string, saved:Record<string,unknown>){
  if(!invoiceAdminIsTextCatalog(catalogKey)) return;
  const entries:Record<string,string>={};
  const baseId=String(saved?.ID ?? saved?.id ?? Date.now());
  const text=String(saved?.TEXT ?? saved?.text ?? '').trim();
  if(text) entries[`invoiceAdmin.${catalogKey}.${baseId}`]=text;
  if(!Object.keys(entries).length) return;
  const lang=currentUiLanguage();
  if(lang==='de') return;
  try{ await loadUiTranslationsLive(lang, entries); gamNotify('info', `Text wurde für ${lang.toUpperCase()} übersetzt.`); }catch{ gamNotify('warning', 'Text gespeichert; automatische Übersetzung konnte nicht sofort abgeschlossen werden.'); }
}
function InvoiceAdminPage(){
  const [selected,setSelected]=useState(INVOICE_ADMIN_CATALOGS[0].key);
  const [catalog,setCatalog]=useState<MasterDataCatalog|null>(null);
  const [rows,setRows]=useState<Record<string,unknown>[]>([]);
  const [q,setQ]=useState('');
  const [err,setErr]=useState('');
  const [open,setOpen]=useState(false);
  const [form,setForm]=useState<Record<string,unknown>>({});
  const [companyOptions,setCompanyOptions]=useState<InvoiceAdminOption[]>([]);
  const [branchOptions,setBranchOptions]=useState<InvoiceAdminOption[]>([]);
  const [salutationOptions,setSalutationOptions]=useState<InvoiceAdminOption[]>([]);
  const [invoiceTextOptions,setInvoiceTextOptions]=useState<InvoiceAdminOption[]>([]);
  const [legalNoteOptions,setLegalNoteOptions]=useState<InvoiceAdminOption[]>([]);
  const [greetingOptions,setGreetingOptions]=useState<InvoiceAdminOption[]>([]);
  const [logoOptions,setLogoOptions]=useState<InvoiceAdminOption[]>([]);
  const firstRef=useRef<HTMLInputElement|HTMLTextAreaElement|HTMLSelectElement|null>(null);
  async function reloadReferences(){
    try{
      const [companies, branches, links, salutations, invoiceTexts, legalNotes, greetings, logos] = await Promise.all([
        loadMasterDataRows('invoice-companies','',500),
        loadMasterDataRows('branches','',500),
        loadMasterDataRows('company-branch-links','',500).catch(()=>({rows:[]} as any)),
        loadMasterDataRows('invoice-salutations','',500).catch(()=>({rows:[]} as any)),
        loadMasterDataRows('invoice-texts','',500).catch(()=>({rows:[]} as any)),
        loadMasterDataRows('invoice-legal-notes','',500).catch(()=>({rows:[]} as any)),
        loadMasterDataRows('invoice-greetings','',500).catch(()=>({rows:[]} as any)),
        loadMasterDataRows('invoice-logos','',500).catch(()=>({rows:[]} as any))
      ]);
      setCompanyOptions(buildInvoiceCompanyOptions(companies.rows||[]));
      setBranchOptions(buildInvoiceBranchOptions(branches.rows||[], links.rows||[]));
      setSalutationOptions(buildInvoiceTextOptions(salutations.rows||[]));
      setInvoiceTextOptions(buildInvoiceTextOptions(invoiceTexts.rows||[]));
      setLegalNoteOptions(buildInvoiceTextOptions(legalNotes.rows||[]));
      setGreetingOptions(buildInvoiceTextOptions(greetings.rows||[]));
      setLogoOptions(buildInvoiceLogoOptions(logos.rows||[]));
    }catch{
      // Die Rechnungsadministration bleibt bedienbar; im Fehlerfall fallen die Felder auf ID-Anzeige zurück.
    }
  }
  async function reload(search=q){
    setErr('');
    setRows([]);
    setCatalog(null);
    try{
      const relationSearch=['invoice-products','company-branch-links','invoice-text-assignments'].includes(selected) && !!search.trim();
      const data=await loadMasterDataRows(selected, relationSearch ? '' : search, 400);
      let nextRows=data.rows||[];
      if(relationSearch){
        const needle=search.trim().toLowerCase();
        nextRows=nextRows.filter(row=>{
          const text=Object.entries(row).map(([k,v])=>String(renderInvoiceAdminCell(k,v))).join(' ').toLowerCase();
          return text.includes(needle);
        });
      }
      setCatalog(data.catalog); setRows(nextRows);
    }
    catch(e:any){ setErr(e.message??'Rechnungsverwaltung konnte nicht geladen werden'); }
  }
  useEffect(()=>{ setOpen(false); setForm({}); setQ(''); reload(''); reloadReferences(); },[selected]);
  const fields = catalog ? [catalog.primaryKey, ...catalog.fields] : [];
  const editable = catalog ? catalog.fields : [];
  function startNew(){ setForm(invoiceAdminEmpty(editable)); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
  function startEdit(row:Record<string,unknown>){ setForm(row); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
  function setField(key:string,value:unknown){
    setForm(prev=>{
      const next={...prev,[key]:value};
      if(isInvoiceCompanyField(key)){
        const branchKey=Object.keys(next).find(isInvoiceBranchField);
        if(branchKey){
          const currentBranch=String(next[branchKey]??'');
          const branch=branchOptions.find(o=>o.id===currentBranch);
          if(branch?.companyId && String(branch.companyId)!==String(value??'')) next[branchKey]='';
        }
      }
      return next;
    });
  }
  function invoiceAdminVisibleBranches(){
    const companyKey=Object.keys(form).find(isInvoiceCompanyField);
    const companyId=companyKey ? String(form[companyKey]??'') : '';
    if(!companyId) return branchOptions;
    const filtered=branchOptions.filter(o=>!o.companyId || String(o.companyId)===companyId);
    return filtered.length ? filtered : branchOptions;
  }
  function renderInvoiceAdminCell(key:string, value:unknown){
    if(isInvoiceCompanyField(key)) return invoiceAdminOptionLabel(companyOptions,value);
    if(isInvoiceBranchField(key)) return invoiceAdminOptionLabel(branchOptions,value);
    if(selected===INVOICE_TEXT_ASSIGNMENT_CATALOG && key==='ANREDE_ID') return invoiceAdminOptionLabel(salutationOptions,value);
    if(selected===INVOICE_TEXT_ASSIGNMENT_CATALOG && key==='RECHNUNGSTEXT_ID') return invoiceAdminOptionLabel(invoiceTextOptions,value);
    if(selected===INVOICE_TEXT_ASSIGNMENT_CATALOG && key==='RECHTLICHER_HINWEIS_ID') return invoiceAdminOptionLabel(legalNoteOptions,value);
    if(selected===INVOICE_TEXT_ASSIGNMENT_CATALOG && key==='GRUSSFORMEL_ID') return invoiceAdminOptionLabel(greetingOptions,value);
    if(selected==='invoice-companies' && key.toUpperCase()==='LOGO_ID') {
      const preview=invoiceLogoPreviewUrlById(logoOptions,value);
      return <><span>{invoiceAdminOptionLabel(logoOptions,value)}</span>{preview&&<br/>}{preview&&invoiceLogoImage(preview,'Logo',true)}</>;
    }
    if((invoiceAdminIsTextCatalog(selected) || invoiceAdminIsLogoCatalog(selected)) && key.toLowerCase()==='id' && String(value??'')==='0') return '0 · 🔒 Systemstandard';
    if(invoiceAdminIsLogoCatalog(selected) && key.toLowerCase()==='url' && value) return <><span>{formatCell(value)}</span><br/>{invoiceLogoImage(String(value),'Logo',true)}</>;
    return formatCell(value);
  }
  async function handleInvoiceLogoUpload(file?:File|null){
    if(!file) return;
    try{
      const uploaded=await uploadInvoiceLogo(file);
      setField('URL', uploaded.url);
      const currentName=String(form['NAME']??'').trim();
      if(selected==='invoice-logos' && !currentName) setField('NAME', uploaded.filename || 'Logo');
      await reloadReferences();
      gamNotify('success','Logo hochgeladen und in den Datensatz übernommen.');
    }catch(e:any){ const m=e.message||'Logo-Upload fehlgeschlagen'; setErr(m); gamNotify('error',m,0); }
  }

  async function handleCompanyLogoUploadAndSelect(file?:File|null){
    if(!file) return;
    try{
      const uploaded=await uploadInvoiceLogo(file);
      const saved=await createMasterDataRow('invoice-logos', {NAME: uploaded.filename || 'Logo', URL: uploaded.url});
      const id=String(saved?.ID ?? saved?.id ?? '');
      await reloadReferences();
      if(id) setField('LOGO_ID', id);
      gamNotify('success','Logo einmalig im Logokatalog angelegt und dieser Gesellschaft zugeordnet.');
    }catch(e:any){ const m=e.message||'Logo-Upload fehlgeschlagen'; setErr(m); gamNotify('error',m,0); }
  }
  function renderInvoiceAdminSelect(key:string, idx:number, options:InvoiceAdminOption[], placeholder:string){
    return <select ref={idx===0?firstRef:undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}><option value="">{placeholder}</option>{options.map(o=><option key={o.id} value={o.id}>{o.label}</option>)}</select>;
  }
  function renderInvoiceAdminEditor(key:string, idx:number){
    if(isInvoiceCompanyField(key)) return <select ref={idx===0?firstRef:undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}><option value="">Bitte Gesellschaft wählen</option>{companyOptions.map(o=><option key={o.id} value={o.id}>{o.label}</option>)}</select>;
    if(isInvoiceBranchField(key)){ const options=invoiceAdminVisibleBranches(); return <select ref={idx===0?firstRef:undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}><option value="">Bitte Filiale wählen</option>{options.map(o=><option key={o.id} value={o.id}>{o.label}</option>)}</select>; }
    if(selected===INVOICE_TEXT_ASSIGNMENT_CATALOG && key==='ANREDE_ID') return renderInvoiceAdminSelect(key, idx, salutationOptions, 'Bitte Anrede wählen');
    if(selected===INVOICE_TEXT_ASSIGNMENT_CATALOG && key==='RECHNUNGSTEXT_ID') return renderInvoiceAdminSelect(key, idx, invoiceTextOptions, 'Bitte Rechnungstext wählen');
    if(selected===INVOICE_TEXT_ASSIGNMENT_CATALOG && key==='RECHTLICHER_HINWEIS_ID') return renderInvoiceAdminSelect(key, idx, legalNoteOptions, 'Bitte rechtlichen Hinweis wählen');
    if(selected===INVOICE_TEXT_ASSIGNMENT_CATALOG && key==='GRUSSFORMEL_ID') return renderInvoiceAdminSelect(key, idx, greetingOptions, 'Bitte Grußformel wählen');
    if(selected==='invoice-companies' && key.toUpperCase()==='LOGO_ID') {
      const preview=invoiceLogoPreviewUrlById(logoOptions, form[key]);
      return <div className="wide-field">
        {renderInvoiceAdminSelect(key, idx, logoOptions, 'Vorhandenes Logo wählen')}
        {preview ? invoiceLogoImage(preview,'Ausgewähltes Logo') : <small className="muted">Kein Logo ausgewählt – es wird der Systemstandard ID 0/Fallback verwendet.</small>}
        <small className="muted">Vorhandene Logos können mehrfach verwendet werden. Neues Logo nur hochladen, wenn es noch nicht im Logokatalog existiert.</small>
        <input type="file" accept="image/png,image/jpeg,image/gif,image/webp" onChange={e=>handleCompanyLogoUploadAndSelect(e.target.files?.[0])}/>
      </div>;
    }
    if(invoiceAdminIsLogoCatalog(selected) && key.toLowerCase()==='url') return <div className="wide-field"><input ref={idx===0?firstRef:undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)} placeholder="/images/uploads/logo.png oder http://..."/><input type="file" accept="image/png,image/jpeg,image/gif,image/webp" onChange={e=>handleInvoiceLogoUpload(e.target.files?.[0])}/>{form[key]&&invoiceLogoImage(String(form[key]),'Logo-Vorschau')}<small className="muted">Dieses Logo wird im Logokatalog gespeichert und kann anschließend in mehreren Gesellschaften ausgewählt werden.</small></div>;
    return invoiceAdminLongField(key)?<textarea ref={idx===0?firstRef:undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>:<input ref={idx===0?firstRef:undefined} type={invoiceAdminFieldType(key)} step={invoiceAdminFieldType(key)==='number'?'0.01':undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>;
  }
  async function save(){
    if(!catalog) return;
    setErr('');
    const id=rowValue(form,catalog.primaryKey);
    if(id && invoiceAdminIsProtectedSystemFallback(selected, form, catalog)){ gamNotify('warning','Systemstandard ID 0 darf nicht geändert werden.'); return; }
    const payload:Record<string,unknown>={};
    editable.forEach(f=>payload[f]=parseInvoiceAdminValue(f, form[f]));
    try{
      const before=id ? rows.find(r=>String(rowValue(r,catalog.primaryKey))===String(id)) : undefined;
      const saved=id ? await updateMasterDataRow(selected, String(id), payload) : await createMasterDataRow(selected, payload);
      notifySaved(id?'Rechnungs-Stammdatum gespeichert.':'Rechnungs-Stammdatum angelegt.', buildChangeDetails('Rechnungsadministration', id?'Datensatz gespeichert':'Datensatz angelegt', before, saved, {}, invoiceAdminLabel(saved,catalog)));
      await translateInvoiceAdminTextIfNeeded(selected, saved);
      setOpen(false); await reload(q);
    }catch(e:any){ const m=e.message??'Speichern fehlgeschlagen'; setErr(m); gamNotify('error',m,0); }
  }
  async function remove(){
    if(!catalog) return; const id=rowValue(form,catalog.primaryKey); if(!id) return;
    if(invoiceAdminIsProtectedSystemFallback(selected, form, catalog)){ gamNotify('warning','Systemstandard ID 0 darf nicht gelöscht werden.'); return; }
    if(!confirm(`Datensatz wirklich löschen?\n\n${invoiceAdminLabel(form,catalog)}`)) return;
    try{ await deleteMasterDataRow(selected,String(id)); notifySaved('Rechnungs-Stammdatum gelöscht.'); setOpen(false); await reload(q); }
    catch(e:any){ const m=e.message??'Löschen fehlgeschlagen'; setErr(m); gamNotify('error',m,0); }
  }
  const activeMeta=INVOICE_ADMIN_CATALOGS.find(c=>c.key===selected);
  return <section className="card"><h2>Rechnungsadministration</h2><p className="muted">Schritt 39k: GAM-1.0-nahe finale Rechnungsadministration des Rechnungsprogramms als eigenes Modul. Produkte, Preise, MwSt, Angebotszeiträume und Rechnungstexte werden hier gepflegt; die Rechnungserfassung nutzt diese Daten nur stichtagsbezogen.</p>{err&&<b className="error">{err}</b>}<div className="tabs sub-tabs">{INVOICE_ADMIN_CATALOGS.map(c=><button key={c.key} className={selected===c.key?'active':''} type="button" onClick={()=>{setRows([]); setCatalog(null); setOpen(false); setSelected(c.key); setQ(''); setForm({});}}>{c.label}</button>)}</div>{catalog&&<p className="note"><b>{activeMeta?.label||catalog.label}</b> · Tabelle <code>{catalog.tableName}</code> · Primärschlüssel <code>{catalog.primaryKey}</code><br/>{invoiceAdminHints[selected] || catalog.note}</p>}{selected==='invoice-products'&&<div className="note warn"><b>Stichtagslogik:</b> Preiswechsel über <code>preisneu</code> + <code>preis_gueltigab</code>, MwSt-Wechsel über <code>mwst</code>/<code>mwstalt</code> + <code>mwst_gueltigab</code>, Verkaufbarkeit über <code>gültig_ab</code> und <code>gültig_bis</code>. Zeitpunkte gelten ab 00:00 Uhr beziehungsweise zum angegebenen Datum/Uhrzeit-Wert.</div>} {invoiceAdminIsTextCatalog(selected)&&<div className="note ok"><b>Normalisierte Textbausteine:</b> Jeder Text wird nur einmal angelegt. ID 0 ist der geschützte Systemstandard und dient als Fallback, falls keine Gesellschaftszuordnung vorhanden ist.</div>}{selected===INVOICE_TEXT_ASSIGNMENT_CATALOG&&<div className="note ok"><b>Gesellschaftszuordnung:</b> Pro Gesellschaft werden vorhandene Bausteine ausgewählt. Fehlt eine Zuordnung oder verweist sie auf gelöschte Texte, verwendet GAM automatisch den Systemstandard ID 0 und erst im absoluten Notfall den internen Notfalltext.</div>}{invoiceAdminIsLogoCatalog(selected)&&<div className="note ok"><b>Logo-Normalisierung:</b> Logos werden einmal im Logokatalog angelegt und können anschließend von beliebig vielen Gesellschaften ausgewählt werden. ID 0 ist der Systemstandard/Fallback.</div>}<GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> {selected==='invoice-products'?'Neues Produkt':'Neuer Datensatz'}</button><label><Search size={16}/><input placeholder={selected==='invoice-products'?'Produktcode, Beschreibung, Kategorie, Gesellschaft, Filiale suchen':'Suchen'} value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload(q)}}/></label><button className="secondary" type="button" onClick={()=>reload(q)}>Suchen / Aktualisieren</button></GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr>{fields.map(k=><th key={k}>{invoiceAdminFriendlyField(k)}</th>)}<th>Aktion</th></tr></thead><tbody>{rows.length?rows.map((r,idx)=><tr key={String(rowValue(r,catalog?.primaryKey||'ID')||idx)} className="clickable-row" onClick={()=>startEdit(r)}>{fields.map(k=><td key={k}>{renderInvoiceAdminCell(k,rowValue(r,k))}</td>)}<td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>):<tr><td colSpan={fields.length+1} className="muted">Keine Daten gefunden.</td></tr>}</tbody></table></GamScrollArea><GamDialog open={open} title={invoiceAdminLabel(form,catalog)} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{rowValue(form,catalog?.primaryKey||'')?'Änderungen speichern':'Datensatz anlegen'}</button>{rowValue(form,catalog?.primaryKey||'')&&!invoiceAdminIsProtectedSystemFallback(selected, form, catalog)&&<button className="danger" type="button" onClick={remove}>Datensatz löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid invoice-admin-editor-grid">{editable.map((key,idx)=><label key={key} className={invoiceAdminEditorFieldClass(key)}><span>{invoiceAdminFriendlyField(key)}</span>{selected==='invoice-products'&&<small>{invoiceAdminSectionForField(key)}</small>}{renderInvoiceAdminEditor(key,idx)}</label>)}</div></GamDialog></section>;
}

function DashboardHome(){const [status,setStatus]=useState<SystemStatus|null>(null); useEffect(()=>{loadSystemStatus().then(setStatus).catch(()=>{})},[]); return <section className="card"><h2>{ui("dashboard")}</h2>{status?<p><b>{ui('db')}:</b> {status.databaseAvailable?ui('connected'):ui('notConnected')} · <b>{ui('accounts')}:</b> {status.accountCount} · <b>{ui('lbd')}:</b> {status.lbdAvailable?ui('found'):ui('notFound')}</p>:<p className="muted">{ui("statusLoading")}</p>}<p>{ui("step31ModuleOverview")}</p><HistoricalModuleOverview/><ModuleTiles/></section>}
const UI_TEXT: Record<string, Record<string,string>> = {
  de: {
    ttsRate: "Geschwindigkeit",
    ttsMary: "Piper",
    ttsBrowser: "Browser/Windows",
    ttsVoiceAuto: "Automatische Stimme",
    ttsVoice: "Stimme",
    ttsEngine: "Vorlesetechnik",
    ttsSettings: "Vorleseinstellungen",
    ttsAuto: "Automatisch",
    ttsLanguage: "Vorlesesprache",
    invoiceTypePaymentAdvice: "Zahlungsavis",
    paymentAdvice: "Zahlungsavis",
    invoiceSearch: "Rechnung suchen",
    newInvoice: "Neue Rechnung",
    users: "Benutzer/Rechte",
    reports: "Reports",
    compliance: "Prüfungen",
    invoicePreviewTitle: "Verbindliche Rechnungsvorschau",
    lbdMissingPlaceholder: "lbd – .lbd-Empfängerdatei wurde nicht gefunden; Export nutzt Platzhalter.", demoReadOnlyShell:'Modulansicht', proformaFailed:'Proforma konnte nicht erstellt werden', saveFailed:'Speichern fehlgeschlagen', selectAtLeastOneLine:'Bitte mindestens eine Position auswählen.', mandatoryZugferd:'ZUGFeRD/Factur-X ist Pflicht-Export.', loadingNumber:'wird geladen', gross:'Brutto', net:'Netto', remove:'Entfernen', addPosition:'+ Position übernehmen', quantity:'Menge', product:'Produkt', installmentCount:'Ratenanzahl', amount:'Betrag', percent:'Prozent', discountValue:'Rabattwert', discountType:'Rabattart', voucherAmount:'Gutscheinbetrag', voucherText:'Gutscheintext', remark:'Bemerkung', reason:'Grund', paymentMethod:'Zahlungsart', paymentCash:'Barzahlung', paymentCard:'Kartenzahlung', paymentTransfer:'Überweisung', paymentUnknown:'unbekannt', invoiceDate:'Rechnungsdatum', saveInvoice:'Rechnung speichern', editChangesSave:'Änderungen speichern', cancelInvoice:'Stornorechnung', createCreditNote:'Gutschrift erstellen', creditNote:'Gutschrift', invoiceTypeInvoice:'Rechnung', technicalDelete:'Technisch löschen', accessLinkError:'Abruflink konnte nicht erzeugt werden', qrAltPortal:'QR-Code Rechnungsportal', openPortal:'Portal öffnen', patientPortalHint:'QR-Code für Patientenabruf mit Sprachwahl und Rechnungshistorie.', patientPortalDigital:'Digitales Rechnungsportal', zugferdIssues:'ZUGFeRD-Export hat Hinweise', zugferdReady:'ZUGFeRD-Export bereit', exportCheckLoading:'Exportprüfung wird geladen.', xml:'XML', zugferdPdf:'ZUGFeRD-PDF', pdfLanguage:'PDF-Sprache', pleaseSelectInvoice:'Bitte links eine Rechnung auswählen.', search:'Suchen', searchPlaceholder:'Suche Nummer / Name / Grund', pleaseChoose:'Bitte wählen', pleaseSelectCompany:'Bitte zuerst eine Gesellschaft auswählen.', invoiceEdit:'Rechnung bearbeiten', usersRights:'Benutzer/Rechte', checks:'Prüfungen', role:'Rolle', logout:'Logout', step31ModuleOverview:'Die GAM2-Migration ist abgeschlossen. Alle Module sind verfügbar und für die kommenden Workflow-Erweiterungen vorbereitet.', statusLoading:'Status wird geladen.', notFound:'nicht gefunden', found:'gefunden', notConnected:'nicht verbunden', connected:'verbunden', lbd:'.lbd', accounts:'Accounts', db:'DB',discount:'Rabatt', voucher:'Gutschein', voucherEnable:'Gutschein', discountEnable:'Rabatt', installmentsEnable:'Ratenzahlung', rateSingular:'Rate', ratePlural:'Raten', reducedTotal:'Endbetrag nach Abzug', installments:'Ratenzahlung', approx:'Raten à ca.', previewTitle:'Verbindliche Rechnungsvorschau', previewHelp:'Diese Vorschau soll dem späteren PDF entsprechen: Texte, Positionen, Rabatt/Gutschein, Ratenzahlung, Hinweise und Bankdaten.', readAloud:'Rechnung vorlesen', stopReading:'Vorlesen stoppen', accessibilityNote:'PDF/UA-Vorbereitung: Sprache, Titel, Metadaten und Lesereihenfolge werden gesetzt.', invoice:'Rechnung', company:'Gesellschaft', qty:'Menge', code:'Code', description:'Beschreibung', tax:'MwSt', price:'Preis', lineTotal:'Gesamt', noLines:'Noch keine Positionen übernommen.', total:'Gesamt', recipient:'Empfänger', date:'Datum', language:'Sprache', payment:'Zahlungsart', notes:'Hinweise', bank:'Bankverbindung', iban:'IBAN', bic:'BIC', taxNo:'Steuer/VAT', noRecipient:'Keine Empfängerdatei geladen'},
  en: {
    ttsRate: "Speed",
    ttsMary: "Piper",
    ttsBrowser: "Browser/Windows",
    ttsVoiceAuto: "Automatic voice",
    ttsVoice: "Voice",
    ttsEngine: "Reading engine",
    ttsSettings: "Reading settings",
    ttsAuto: "Automatic",
    ttsLanguage: "Reading language",
    invoiceTypePaymentAdvice: "Payment advice",
    paymentAdvice: "Payment advice",
    invoiceSearch: "Search invoice",
    newInvoice: "New invoice",
    users: "Users/permissions",
    reports: "Reports",
    compliance: "Checks",
    invoicePreviewTitle: "Binding invoice preview",
    lbdMissingPlaceholder: "lbd – .lbd recipient file was not found; export uses placeholders.", demoReadOnlyShell:'module view', proformaFailed:'Proforma could not be created', saveFailed:'Saving failed', selectAtLeastOneLine:'Please select at least one item.', mandatoryZugferd:'ZUGFeRD/Factur-X is the mandatory export.', loadingNumber:'loading', gross:'Gross', net:'Net', remove:'Remove', addPosition:'+ Add item', quantity:'Quantity', product:'Product', installmentCount:'Number of installments', amount:'Amount', percent:'Percent', discountValue:'Discount value', discountType:'Discount type', voucherAmount:'Voucher amount', voucherText:'Voucher text', remark:'Remark', reason:'Reason', paymentMethod:'Payment method', paymentCash:'Cash payment', paymentCard:'Card payment', paymentTransfer:'Bank transfer', paymentUnknown:'unknown', invoiceDate:'Invoice date', saveInvoice:'Save invoice', editChangesSave:'Save changes', cancelInvoice:'Cancellation invoice', createCreditNote:'Create credit note', creditNote:'Credit note', invoiceTypeInvoice:'Invoice', technicalDelete:'Technical delete', accessLinkError:'Access link could not be created', qrAltPortal:'Invoice portal QR code', openPortal:'Open portal', patientPortalHint:'QR code for patient access with language selection and invoice history.', patientPortalDigital:'Digital invoice portal', zugferdIssues:'ZUGFeRD export has notes', zugferdReady:'ZUGFeRD export ready', exportCheckLoading:'Loading export check.', xml:'XML', zugferdPdf:'ZUGFeRD PDF', pdfLanguage:'PDF language', pleaseSelectInvoice:'Please select an invoice on the left.', search:'Search', searchPlaceholder:'Search number / name / reason', pleaseChoose:'Please choose', pleaseSelectCompany:'Please select a company first.', invoiceEdit:'Edit invoice', usersRights:'Users/permissions', checks:'Checks', role:'Role', logout:'Logout', step31ModuleOverview:'GAM2 migration is complete. All modules are available and ready for the upcoming workflow extensions.', statusLoading:'Loading status.', notFound:'not found', found:'found', notConnected:'not connected', connected:'connected', lbd:'.lbd', accounts:'Accounts', db:'DB',discount:'Discount', voucher:'Voucher', voucherEnable:'Voucher', discountEnable:'Discount', installmentsEnable:'Installments', rateSingular:'installment', ratePlural:'installments', reducedTotal:'Total after deduction', installments:'Installment payment', approx:'installments of approx.', previewTitle:'Binding invoice preview', previewHelp:'This preview should match the later PDF: texts, items, discount/voucher, installments, notices and bank details.', readAloud:'Read invoice aloud', stopReading:'Stop reading', accessibilityNote:'PDF/UA preparation: language, title, metadata and reading order are set.', invoice:'Invoice', company:'Company', qty:'Quantity', code:'Code', description:'Description', tax:'VAT', price:'Price', lineTotal:'Total', noLines:'No items have been added yet.', total:'Total', recipient:'Recipient', date:'Date', language:'Language', payment:'Payment method', notes:'Notes', bank:'Bank details', iban:'IBAN', bic:'BIC', taxNo:'Tax/VAT', noRecipient:'No recipient file loaded'},
  fr: {
    ttsRate: "Vitesse",
    ttsMary: "Piper",
    ttsBrowser: "Navigateur/Windows",
    ttsVoiceAuto: "Voix automatique",
    ttsVoice: "Voix",
    ttsEngine: "Technique de lecture",
    ttsSettings: "Paramètres de lecture",
    ttsAuto: "Automatique",
    ttsLanguage: "Langue de lecture",
    invoiceTypePaymentAdvice: "Avis de paiement",
    paymentAdvice: "Avis de paiement",
    invoiceSearch: "Rechercher une facture",
    newInvoice: "Nouvelle facture",
    users: "Utilisateurs/droits",
    reports: "Rapports",
    compliance: "Contrôles",
    invoicePreviewTitle: "Aperçu de facture contraignant",
    lbdMissingPlaceholder: "lbd – le fichier destinataire .lbd est introuvable ; l’export utilise des valeurs de remplacement.", demoReadOnlyShell:'module en lecture seule', proformaFailed:'La proforma n’a pas pu être créée', saveFailed:'Échec de l’enregistrement', selectAtLeastOneLine:'Veuillez sélectionner au moins un poste.', mandatoryZugferd:'ZUGFeRD/Factur-X est l’export obligatoire.', loadingNumber:'chargement', gross:'Brut', net:'Net', remove:'Supprimer', addPosition:'+ Ajouter le poste', quantity:'Quantité', product:'Produit', installmentCount:'Nombre d’échéances', amount:'Montant', percent:'Pourcentage', discountValue:'Valeur de remise', discountType:'Type de remise', voucherAmount:'Montant du bon', voucherText:'Texte du bon', remark:'Remarque', reason:'Motif', paymentMethod:'Mode de paiement', paymentCash:'Paiement en espèces', paymentCard:'Paiement par carte', paymentTransfer:'Virement bancaire', paymentUnknown:'inconnu', invoiceDate:'Date de facture', saveInvoice:'Enregistrer la facture', editChangesSave:'Enregistrer les modifications', cancelInvoice:'Facture d’annulation', createCreditNote:'Créer un avoir', creditNote:'Avoir', invoiceTypeInvoice:'Facture', technicalDelete:'Suppression technique', accessLinkError:'Le lien d’accès n’a pas pu être créé', qrAltPortal:'Code QR du portail factures', openPortal:'Ouvrir le portail', patientPortalHint:'Code QR pour l’accès patient avec choix de langue et historique des factures.', patientPortalDigital:'Portail numérique de factures', zugferdIssues:'L’export ZUGFeRD contient des remarques', zugferdReady:'Export ZUGFeRD prêt', exportCheckLoading:'Chargement de la vérification d’export.', xml:'XML', zugferdPdf:'PDF ZUGFeRD', pdfLanguage:'Langue du PDF', pleaseSelectInvoice:'Veuillez sélectionner une facture à gauche.', search:'Rechercher', searchPlaceholder:'Recherche numéro / nom / motif', pleaseChoose:'Veuillez choisir', pleaseSelectCompany:'Veuillez d’abord sélectionner une société.', invoiceEdit:'Modifier la facture', usersRights:'Utilisateurs/droits', checks:'Contrôles', role:'Rôle', logout:'Déconnexion', step31ModuleOverview:'L’étape 31 rend visibles toutes les applications GAM historiques. Les zones entièrement migrées sont utilisables ; les modules encore ouverts sont affichés volontairement comme des modules en lecture seule.', statusLoading:'Chargement du statut.', notFound:'non trouvé', found:'trouvé', notConnected:'non connecté', connected:'connecté', lbd:'.lbd', accounts:'Comptes', db:'BD',discount:'Remise', voucher:'Bon', reducedTotal:'Total après déduction', installments:'Paiement échelonné', approx:"échéances d'environ", previewTitle:'Aperçu de facture contraignant', previewHelp:"Cet aperçu doit correspondre au PDF final : textes, postes, remise/bon, paiements échelonnés, avis et coordonnées bancaires.", readAloud:'Lire la facture', stopReading:'Arrêter la lecture', accessibilityNote:'Préparation PDF/UA : langue, titre, métadonnées et ordre de lecture sont définis.', invoice:'Facture', company:'Société', qty:'Quantité', code:'Code', description:'Description', tax:'TVA', price:'Prix', lineTotal:'Total', noLines:"Aucun poste n'a encore été ajouté.", total:'Total', recipient:'Destinataire', date:'Date', language:'Langue', payment:'Mode de paiement', notes:'Notes', bank:'Coordonnées bancaires', iban:'IBAN', bic:'BIC', taxNo:'Fiscal/TVA', noRecipient:'Aucun destinataire chargé'},
  uk: {
    ttsRate: "Швидкість",
    ttsMary: "Piper",
    ttsBrowser: "Браузер/Windows",
    ttsVoiceAuto: "Автоматичний голос",
    ttsVoice: "Голос",
    ttsEngine: "Рушій озвучення",
    ttsSettings: "Налаштування озвучення",
    ttsAuto: "Автоматично",
    ttsLanguage: "Мова озвучення",
    invoiceTypePaymentAdvice: "Платіжне повідомлення",
    paymentAdvice: "Платіжне повідомлення",
    invoiceSearch: "Пошук рахунку",
    newInvoice: "Новий рахунок",
    users: "Користувачі/права",
    reports: "Звіти",
    compliance: "Перевірки",
    invoicePreviewTitle: "Обов’язковий попередній перегляд рахунку",
    lbdMissingPlaceholder: "lbd – файл одержувача .lbd не знайдено; експорт використовує заповнювачі.", demoReadOnlyShell:'модуль лише для перегляду', proformaFailed:'Не вдалося створити проформу', saveFailed:'Не вдалося зберегти', selectAtLeastOneLine:'Виберіть принаймні одну позицію.', mandatoryZugferd:'ZUGFeRD/Factur-X є обов’язковим експортом.', loadingNumber:'завантаження', gross:'Брутто', net:'Нетто', remove:'Видалити', addPosition:'+ Додати позицію', product:'Продукт', installmentCount:'Кількість платежів', amount:'Сума', percent:'Відсоток', voucherAmount:'Сума ваучера', voucherText:'Текст ваучера', remark:'Примітка', saveInvoice:'Зберегти рахунок', editChangesSave:'Зберегти зміни', cancelInvoice:'Рахунок скасування', createCreditNote:'Створити кредит-ноту', creditNote:'Кредит-нота', invoiceTypeInvoice:'Рахунок', technicalDelete:'Технічно видалити', accessLinkError:'Не вдалося створити посилання доступу', qrAltPortal:'QR-код порталу рахунків', openPortal:'Відкрити портал', patientPortalHint:'QR-код для доступу пацієнта з вибором мови та історією рахунків.', patientPortalDigital:'Цифровий портал рахунків', zugferdIssues:'Експорт ZUGFeRD має примітки', zugferdReady:'Експорт ZUGFeRD готовий', exportCheckLoading:'Завантаження перевірки експорту.', xml:'XML', zugferdPdf:'ZUGFeRD PDF', pdfLanguage:'Мова PDF', pleaseSelectInvoice:'Виберіть рахунок ліворуч.', search:'Пошук', searchPlaceholder:'Пошук номера / імені / причини', pleaseChoose:'Будь ласка, виберіть', pleaseSelectCompany:'Спочатку виберіть організацію.', invoiceEdit:'Редагувати рахунок', usersRights:'Користувачі/права', checks:'Перевірки', role:'Роль', logout:'Вийти', step31ModuleOverview:'Крок 31 робить видимими всі історичні застосунки GAM. Повністю перенесені області доступні; відкриті модулі навмисно показані як модулі лише для перегляду.', statusLoading:'Завантаження статусу.', notFound:'не знайдено', found:'знайдено', notConnected:'не підключено', connected:'підключено', lbd:'.lbd', accounts:'Облікові записи', db:'БД',discount:'Знижка', voucher:'Ваучер', reducedTotal:'Сума після вирахування', installments:'Оплата частинами', approx:'платежі приблизно по', previewTitle:'Обов’язковий попередній перегляд рахунку', previewHelp:'Цей перегляд має відповідати майбутньому PDF: тексти, позиції, знижка/ваучер, оплата частинами, примітки та банківські дані.', readAloud:'Зачитати рахунок', stopReading:'Зупинити читання', accessibilityNote:'Підготовка PDF/UA: встановлено мову, назву, метадані та порядок читання.', invoice:'Рахунок', company:'Організація', qty:'Кількість', code:'Код', description:'Опис', tax:'ПДВ', price:'Ціна', lineTotal:'Разом', noLines:'Позиції ще не додано.', total:'Разом', recipient:'Одержувач', date:'Дата', language:'Мова', payment:'Спосіб оплати', notes:'Примітки', bank:'Банківські реквізити', iban:'IBAN', bic:'BIC', taxNo:'Податок/VAT', noRecipient:'Файл одержувача не завантажено'}
};
Object.entries(INVOICE_PROGRAM_COMPLETION_LABELS).forEach(([lang, labels]) => {
  UI_TEXT[lang] = {...(UI_TEXT[lang] ?? {}), ...labels};
  GAM_UI_LABELS[lang] = {...(GAM_UI_LABELS[lang] ?? {}), ...labels};
});

// Schritt 34z7c: feste Rechnungs-/Portal-Metadatenlabels gehören in den UI-Keykatalog.
// Produktbeschreibungen bleiben davon bewusst ausgenommen und werden nur live nach PDF-Sprache übersetzt.
const INVOICE_METADATA_LABELS: Record<string, Record<string,string>> = {
  de: {invoiceCustomerFile:"Kundendatei", invoiceUser:"Benutzer", invoicePaymentMethod:"Zahlungsart", invoiceTaxNumberVatId:"Steuer-/USt-ID", invoiceRecipient:"Rechnungsempfänger", invoiceDate:"Rechnungsdatum", treatmentDate:"Behandlungsdatum", serviceDate:"Leistungsdatum", dueDate:"Fälligkeitsdatum", paymentUnknown:"unbekannt", paymentCash:"Barzahlung", paymentCard:"Kartenzahlung", paymentTransfer:"Überweisung", readInvoice:"Rechnung vorlesen", portalLanguage:"Portalsprache", invoicePdfLanguage:"Rechnungs-/PDF-Sprache"},
  en: {invoiceCustomerFile:"Customer file", invoiceUser:"User", invoicePaymentMethod:"Payment method", invoiceTaxNumberVatId:"Tax/VAT ID", invoiceRecipient:"Invoice recipient", invoiceDate:"Invoice date", treatmentDate:"Treatment date", serviceDate:"Service date", dueDate:"Due date", paymentUnknown:"unknown", paymentCash:"Cash payment", paymentCard:"Card payment", paymentTransfer:"Bank transfer", readInvoice:"Read invoice", portalLanguage:"Portal language", invoicePdfLanguage:"Invoice/PDF language"},
  fr: {invoiceCustomerFile:"Dossier client", invoiceUser:"Utilisateur", invoicePaymentMethod:"Mode de paiement", invoiceTaxNumberVatId:"N° fiscal/TVA", invoiceRecipient:"Destinataire de la facture", invoiceDate:"Date de facture", treatmentDate:"Date de traitement", serviceDate:"Date de prestation", dueDate:"Date d’échéance", paymentUnknown:"inconnu", paymentCash:"Paiement en espèces", paymentCard:"Paiement par carte", paymentTransfer:"Virement bancaire", readInvoice:"Lire la facture", portalLanguage:"Langue du portail", invoicePdfLanguage:"Langue facture/PDF"},
  uk: {invoiceCustomerFile:"Файл клієнта", invoiceUser:"Користувач", invoicePaymentMethod:"Спосіб оплати", invoiceTaxNumberVatId:"Податковий/VAT номер", invoiceRecipient:"Одержувач рахунку", invoiceDate:"Дата рахунку", treatmentDate:"Дата лікування", serviceDate:"Дата послуги", dueDate:"Дата оплати", paymentUnknown:"невідомо", paymentCash:"Оплата готівкою", paymentCard:"Оплата карткою", paymentTransfer:"Банківський переказ", readInvoice:"Зачитати рахунок", portalLanguage:"Мова порталу", invoicePdfLanguage:"Мова рахунку/PDF"},
  it: {invoiceCustomerFile:"File cliente", invoiceUser:"Utente", invoicePaymentMethod:"Metodo di pagamento", invoiceTaxNumberVatId:"Codice fiscale/IVA", invoiceRecipient:"Destinatario fattura", invoiceDate:"Data fattura", treatmentDate:"Data trattamento", serviceDate:"Data prestazione", dueDate:"Scadenza", paymentUnknown:"sconosciuto", paymentCash:"Pagamento in contanti", paymentCard:"Pagamento con carta", paymentTransfer:"Bonifico bancario", readInvoice:"Leggi fattura", portalLanguage:"Lingua portale", invoicePdfLanguage:"Lingua fattura/PDF"},
  sv: {invoiceCustomerFile:"Kundfil", invoiceUser:"Användare", invoicePaymentMethod:"Betalningssätt", invoiceTaxNumberVatId:"Skatte-/momsnummer", invoiceRecipient:"Fakturamottagare", invoiceDate:"Fakturadatum", treatmentDate:"Behandlingsdatum", serviceDate:"Utförandedatum", dueDate:"Förfallodatum", paymentUnknown:"okänt", paymentCash:"Kontant betalning", paymentCard:"Kortbetalning", paymentTransfer:"Banköverföring", readInvoice:"Läs faktura", portalLanguage:"Portalspråk", invoicePdfLanguage:"Faktura-/PDF-språk"},
  tr: {invoiceCustomerFile:"Müşteri dosyası", invoiceUser:"Kullanıcı", invoicePaymentMethod:"Ödeme yöntemi", invoiceTaxNumberVatId:"Vergi/KDV no.", invoiceRecipient:"Fatura alıcısı", invoiceDate:"Fatura tarihi", treatmentDate:"Tedavi tarihi", serviceDate:"Hizmet tarihi", dueDate:"Vade tarihi", paymentUnknown:"bilinmiyor", paymentCash:"Nakit ödeme", paymentCard:"Kartla ödeme", paymentTransfer:"Banka havalesi", readInvoice:"Faturayı oku", portalLanguage:"Portal dili", invoicePdfLanguage:"Fatura/PDF dili"},
  ru: {invoiceCustomerFile:"Файл клиента", invoiceUser:"Пользователь", invoicePaymentMethod:"Способ оплаты", invoiceTaxNumberVatId:"Налоговый/VAT номер", invoiceRecipient:"Получатель счёта", invoiceDate:"Дата счёта", treatmentDate:"Дата лечения", serviceDate:"Дата услуги", dueDate:"Срок оплаты", paymentUnknown:"неизвестно", paymentCash:"Оплата наличными", paymentCard:"Оплата картой", paymentTransfer:"Банковский перевод", readInvoice:"Прочитать счёт", portalLanguage:"Язык портала", invoicePdfLanguage:"Язык счёта/PDF"}
};
Object.entries(INVOICE_METADATA_LABELS).forEach(([lang, labels]) => {
  UI_TEXT[lang] = {...(UI_TEXT[lang] ?? {}), ...labels};
  GAM_UI_LABELS[lang] = {...(GAM_UI_LABELS[lang] ?? {}), ...labels};
});

// Schritt 34z7f: Alias-Keys für Vorschau und Vorlesen müssen direkt zum Start vorhanden sein.
// Dadurch erscheinen keine technischen Keys wie invoicePreviewHelp/readAloud/stopReading mehr,
// selbst wenn die DB-Übersetzung noch erzeugt wird.
Object.keys(UI_TEXT).forEach((lang) => {
  const previewHelp = UI_TEXT[lang]?.previewHelp ?? UI_TEXT.de.previewHelp;
  const readAloud = UI_TEXT[lang]?.readAloud ?? UI_TEXT.de.readAloud;
  const stopReading = UI_TEXT[lang]?.stopReading ?? UI_TEXT.de.stopReading;
  const accessibilityNote = UI_TEXT[lang]?.accessibilityNote ?? UI_TEXT.de.accessibilityNote;
  const aliases = { invoicePreviewHelp: previewHelp, readAloud, stopReading, accessibilityNote };
  UI_LABELS[lang] = {...(UI_LABELS[lang] ?? {}), ...aliases};
  UI_TEXT[lang] = {...(UI_TEXT[lang] ?? {}), ...aliases};
  GAM_UI_LABELS[lang] = {...(GAM_UI_LABELS[lang] ?? {}), ...aliases};
});


const PORTAL_COMPLETION_LABELS: Record<string, Record<string,string>> = {
  de: {invoicePortalTitle:'Digitales Rechnungsportal', invoicePortalQrHint:'QR-Code für Patientenabruf mit Sprachwahl und Rechnungshistorie.', invoicePortalHelp:'Wählen Sie die gewünschte Sprache und laden Sie Ihre Rechnung erneut herunter.'},
  en: {invoicePortalTitle:'Digital invoice portal', invoicePortalQrHint:'QR code for patient access with language selection and invoice history.', invoicePortalHelp:'Select the desired language and download your invoice again.'},
  fr: {invoicePortalTitle:'Portail numérique de factures', invoicePortalQrHint:'Code QR pour l’accès patient avec choix de langue et historique des factures.', invoicePortalHelp:'Sélectionnez la langue souhaitée et téléchargez à nouveau votre facture.'},
  uk: {invoicePortalTitle:'Цифровий портал рахунків', invoicePortalQrHint:'QR-код для доступу пацієнта з вибором мови та історією рахунків.', invoicePortalHelp:'Виберіть потрібну мову та завантажте рахунок ще раз.'},
  it: {invoicePortalTitle:'Portale digitale fatture', invoicePortalQrHint:'Codice QR per l’accesso paziente con scelta della lingua e storico fatture.', invoicePortalHelp:'Selezionare la lingua desiderata e scaricare nuovamente la fattura.'},
  sv: {invoicePortalTitle:'Digital fakturaportal', invoicePortalQrHint:'QR-kod för patientåtkomst med språkval och fakturahistorik.', invoicePortalHelp:'Välj önskat språk och ladda ner fakturan igen.'},
  tr: {invoicePortalTitle:'Dijital fatura portalı', invoicePortalQrHint:'Dil seçimi ve fatura geçmişiyle hasta erişimi için QR kodu.', invoicePortalHelp:'İstediğiniz dili seçin ve faturayı yeniden indirin.'},
  ru: {invoicePortalTitle:'Цифровой портал счетов', invoicePortalQrHint:'QR-код для доступа пациента с выбором языка и историей счетов.', invoicePortalHelp:'Выберите нужный язык и скачайте счёт снова.'}
};
Object.entries(PORTAL_COMPLETION_LABELS).forEach(([lang, labels]) => {
  UI_LABELS[lang] = {...(UI_LABELS[lang] ?? {}), ...labels};
  UI_TEXT[lang] = {...(UI_TEXT[lang] ?? {}), ...labels};
  GAM_UI_LABELS[lang] = {...(GAM_UI_LABELS[lang] ?? {}), ...labels};
});

function ui(key:string, lang:GamLanguage=currentUiLanguage()) {
  return uiText(lang, key);
}

function commercialRows(summary?:InvoiceSummary, totals?:InvoiceTotals, lines?:Array<{quantity?:number; price?:number}>, lang:GamLanguage='de', labels?:Record<string,string>) {
  const lt = (key:string, fallbackKey:string) => labels?.[key] ?? ui(fallbackKey, lang);
  if (!summary) return null;
  const lineGross = (lines ?? []).reduce((sum,l)=>sum + ((l.quantity ?? 1) * (l.price ?? 0)), 0);
  const startGross = totals?.gross ?? summary.totalGross ?? lineGross;
  let adjustedGross = startGross;
  const rows:string[] = [];
  const hasDiscount = (summary.discountPercent ?? 0) > 0 && lineGross > 0;
  const hasCoupon = (summary.couponAmount ?? 0) > 0;
  if (hasDiscount) {
    const d = lineGross * (summary.discountPercent ?? 0) / 100;
    adjustedGross -= d;
    rows.push(`${lt('invoiceReducement','discount')} ${summary.discountPercent}%: -${money(d)}`);
  }
  if (hasCoupon) {
    adjustedGross -= summary.couponAmount ?? 0;
    rows.push(`${lt('invoiceCoupon','voucher')}: -${money(summary.couponAmount)}`);
  }
  if ((hasDiscount || hasCoupon) && summary.discountRemark) rows.push(summary.discountRemark);
  adjustedGross = Math.max(0, adjustedGross);
  if (hasDiscount || hasCoupon) rows.push(`${lt('invoiceReducedTotal','reducedTotal')}: ${money(adjustedGross)}`);
  if ((summary.installments ?? 1) > 1) rows.push(`${lt('invoiceInstallments','installments')}: ${summary.installments} ${lt('invoiceInstallmentApprox','approx')} ${money(adjustedGross / (summary.installments ?? 1))}`);
  return rows.length ? <div className="note commercial-info">{(rows ?? []).map((r,i)=><div key={i}>{r}</div>)}</div> : null;
}

function isGkvProduct(p?:ProductDto) {
  const text = `${p?.code ?? ''} ${p?.description ?? ''} ${p?.category ?? ''}`.toLowerCase();
  return text.includes('gkv');
}
function isPrescriptionFeeProduct(p?:ProductDto) {
  const text = `${p?.code ?? ''} ${p?.description ?? ''} ${p?.category ?? ''}`.toLowerCase();
  return text.includes('verordnungsgebühr') || text.includes('verordnungsgebuehr');
}
function isTrainingCompany(c?:InvoiceCompany) {
  const text = `${c?.name ?? ''} ${c?.code ?? ''}`.toLowerCase();
  return text.includes('training');
}



function UsersPage(){
  const [rows,setRows]=useState<AccountAdminDto[]>([]);
  const [roles,setRoles]=useState<RoleDto[]>([]);
  const [applications,setApplications]=useState<PermissionApplicationDto[]>([]);
  const [rights,setRights]=useState<PermissionAccessDto[]>([]);
  const [q,setQ]=useState('');
  const [err,setErr]=useState('');
  const [open,setOpen]=useState(false);
  const [passwordOpen,setPasswordOpen]=useState(false);
  const [form,setForm]=useState<Partial<AccountAdminDto & {password?:string}>>({});
  const [rightForm,setRightForm]=useState<Partial<PermissionAccessDto>>({role:'user'});
  const [password,setPassword]=useState('');
  const [activeTab,setActiveTab]=useState<'user'|'rights'>('user');
  const firstRef=useRef<HTMLInputElement|null>(null);
  const rightsFirstRef=useRef<HTMLSelectElement|null>(null);
  async function reload(){
    setErr('');
    try{
      const [accounts,roleList,apps,access]=await Promise.all([
        loadAccounts(q,500),
        loadRoles().catch(()=>[] as RoleDto[]),
        loadPermissionApplications().catch(()=>[] as PermissionApplicationDto[]),
        loadPermissionAccess('',1000).catch(()=>[] as PermissionAccessDto[])
      ]);
      setRows(accounts); setRoles(roleList); setApplications(apps); setRights(access);
    }catch(e:any){ const m=e?.message??'Benutzer konnten nicht geladen werden.'; setErr(m); gamNotify('error',m,0); }
  }
  useEffect(()=>{ reload(); },[]);
  const roleOptions = (()=>{
    const base = (roles.length ? roles : [
      {key:'user',label:'Benutzer',administrative:false,modules:[]},
      {key:'admin',label:'Admin',administrative:true,modules:[]}
    ]).filter(r=>r.key!=='administrator' && r.key!=='permissions');
    return base.some(r=>r.key==='superadmin') ? base : [{key:'superadmin',label:'Superadmin',administrative:true,modules:[]}, ...base];
  })();
  const permissionRoleOptions=[
    {key:'user',label:'Benutzer'},
    {key:'mainuser',label:'Hauptbenutzer · user + Reports'},
    {key:'admin',label:'Admin'},
  ];
  function isSuperAdminUser(u?:Partial<AccountAdminDto>){ return String(u?.role??'').toLowerCase().includes('superadmin'); }
  function startNew(){ setForm({role:'user'}); setRightForm({role:'user'}); setActiveTab('user'); setOpen(true); }
  function startEdit(row:AccountAdminDto){ setForm({...row}); setRightForm({username:row.username, application:applications[0]?.application??'Rechnungsprogramm', role:'user'}); setActiveTab('user'); setOpen(true); }
  function labels(){return {username:'Benutzername',fullname:'Name',role:'Rolle',email:'E-Mail',twoFactorConfigured:'2FA',passwordPresent:'Passwort vorhanden'};}
  function rightLabels(){ return {username:'Benutzer',application:'Anwendung',companyId:'Gesellschaft',role:'Rolle'}; }
  function rightsForUser(username?:string){ const u=(username??'').toLowerCase(); return rights.filter(r=>(r.username??'').toLowerCase()===u); }
  function startNewRight(){ setRightForm({username:form.username??'', application:applications[0]?.application??'Rechnungsprogramm', role:'user'}); setActiveTab('rights'); setTimeout(()=>rightsFirstRef.current?.focus(),60); }
  function startEditRight(row:PermissionAccessDto){ setRightForm({...row}); setActiveTab('rights'); setTimeout(()=>rightsFirstRef.current?.focus(),60); }
  function normalizedRightPayload(){ return {username:(rightForm.username??form.username??'').trim(), application:(rightForm.application??'').trim(), companyId: rightForm.companyId===undefined || rightForm.companyId===null || String(rightForm.companyId)==='' ? null : Number(rightForm.companyId), role:(rightForm.role??'user').trim()}; }
  async function saveRight(){
    const payload=normalizedRightPayload();
    if(!payload.username){ gamNotify('warning','Bitte zuerst einen Benutzer speichern oder auswählen.',0); return; }
    if(isSuperAdminUser(form)){ gamNotify('warning','Superadmins brauchen keine userapplication-Zuordnung, weil sie überall Zugriff haben.',0); return; }
    if(!payload.application){ gamNotify('warning','Bitte Anwendung auswählen oder eingeben.',0); return; }
    const duplicate = rights.find(r =>
      r.id !== rightForm.id &&
      String(r.username ?? '').toLowerCase() === payload.username.toLowerCase() &&
      String(r.application ?? '').toLowerCase() === payload.application.toLowerCase() &&
      String(r.companyId ?? '') === String(payload.companyId ?? '')
    );
    if(duplicate){ gamNotify('warning','Diese Rechtezuordnung existiert bereits. Bitte vorhandenes Recht bearbeiten oder zuerst löschen.',0); return; }
    try{
      const before=rightForm.id?rights.find(r=>r.id===rightForm.id):undefined;
      const saved=rightForm.id?await updatePermissionAccess(rightForm.id,payload):await createPermissionAccess(payload);
      notifySaved(rightForm.id?'Rechtezuordnung gespeichert.':'Rechtezuordnung angelegt.', buildChangeDetails('Benutzer/Rechte', rightForm.id?'Rechte gespeichert':'Rechte angelegt', before, saved, rightLabels(), `${saved.username} · ${saved.application}`));
      setRightForm({username:payload.username, application:applications[0]?.application??'Rechnungsprogramm', role:'user'});
      await reload();
    }catch(e:any){ notifySaveError(e,'Rechtezuordnung konnte nicht gespeichert werden.'); }
  }
  async function removeRight(row?:PermissionAccessDto){
    const target=row??(rightForm.id?rights.find(r=>r.id===rightForm.id):undefined);
    if(!target?.id) return;
    if(!confirm(`Rechtezuordnung für ${target.username} / ${target.application} wirklich löschen?`)) return;
    try{
      await deletePermissionAccess(target.id);
      gamNotify('success','Rechtezuordnung wurde gelöscht.', undefined, buildChangeDetails('Benutzer/Rechte','Rechte gelöscht', target, {}, rightLabels(), `${target.username} · ${target.application}`));
      setRightForm({username:form.username??'', application:applications[0]?.application??'Rechnungsprogramm', role:'user'});
      await reload();
    }catch(e:any){ gamNotify('error',e?.message??'Rechtezuordnung konnte nicht gelöscht werden.',0); }
  }
  async function save(){
    const username=(form.username??'').trim();
    const email=(form.email??'').trim();
    if(!username){ gamNotify('warning','Bitte Benutzernamen eingeben.',0); return; }
    if(email){ const mailError=emailValidationMessage(email,'E-Mail-Adresse',false); if(mailError){ gamNotify('warning',mailError,0); return; }}
    try{
      const before=form.id?rows.find(r=>r.id===form.id):undefined;
      const payload={fullname:form.fullname, role:form.role, email: email || undefined};
      let saved:AccountAdminDto;
      if(form.id){ saved=await updateAccount(form.id,payload); }
      else { saved=await createAccount({username, password:(form.password as string)||'changeme', fullname:form.fullname, role:form.role||'user', email: email || undefined}); }
      notifySaved(form.id?'Benutzer erfolgreich gespeichert.':'Benutzer erfolgreich angelegt.', buildChangeDetails('Benutzer/Rechte', form.id?'Benutzer gespeichert':'Benutzer angelegt', before, saved, labels(), saved.username));
      setForm(saved); setRightForm({...rightForm, username:saved.username}); await reload();
      if(!form.id) setActiveTab('rights'); else setOpen(false);
    }catch(e:any){ notifySaveError(e,'Benutzer konnte nicht gespeichert werden.'); }
  }
  async function reset2fa(){
    if(!form.id) return;
    try{
      const before=rows.find(r=>r.id===form.id);
      const saved=await updateAccount(form.id,{secretkey:''});
      notifySaved('2FA für Benutzer wurde zurückgesetzt.', buildChangeDetails('Benutzer/Rechte','2FA zurückgesetzt', before, saved, labels(), saved.username));
      setForm(saved); await reload();
    }catch(e:any){ notifySaveError(e,'2FA konnte nicht zurückgesetzt werden.'); }
  }
  function openPassword(row?:AccountAdminDto){ const target=row??(form.id?rows.find(r=>r.id===form.id):undefined); if(!target) return; setForm({...target}); setPassword(''); setPasswordOpen(true); }
  async function savePassword(){
    if(!form.id) return;
    if(!password || password.length<4){ gamNotify('warning','Bitte ein Passwort mit mindestens 4 Zeichen eingeben.',0); return; }
    try{
      const before=rows.find(r=>r.id===form.id);
      const saved=await updateAccountPassword(form.id,password);
      notifySaved('Passwort wurde neu gesetzt.', buildChangeDetails('Benutzer/Rechte','Passwort gesetzt', before, {...saved,passwordPresent:true}, {passwordPresent:'Passwort vorhanden'}, saved.username));
      setPasswordOpen(false); await reload();
    }catch(e:any){ notifySaveError(e,'Passwort konnte nicht gesetzt werden.'); }
  }
  async function remove(){
    if(!form.id) return;
    if(!confirm(`Benutzer ${form.username} wirklich löschen?`)) return;
    try{
      const before=rows.find(r=>r.id===form.id);
      await deleteAccount(form.id);
      gamNotify('success','Benutzer wurde gelöscht.', undefined, buildChangeDetails('Benutzer/Rechte','Benutzer gelöscht', before, {}, labels(), form.username));
      setOpen(false); await reload();
    }catch(e:any){ gamNotify('error',e?.message??'Benutzer konnte nicht gelöscht werden.',0); }
  }
  const filtered=rows.filter(r=>{
    const text=[r.username,r.fullname,r.role,r.email].join(' ').toLowerCase();
    return !q || text.includes(q.toLowerCase());
  });
  const currentRights=rightsForUser(form.username);
  return <section className="card"><h2>Benutzer/Rechte</h2><p className="muted">Schritt 38i6: Benutzer und Rechte sind zusammengeführt; im Rechte-Tab steht die Rechte-Tabelle jetzt breit oberhalb der Bearbeitungsfelder. <code>user</code> ist normaler Zugriff, <code>mainuser</code> ist user plus Reports nach GAM 1.0, <code>admin</code> ist administrativer Modulzugriff; <code>superadmin</code> bleibt zentral in <code>accounts</code> und hat Vollzugriff.</p><div className="stats"><span>Benutzer<br/><b>{rows.length}</b></span><span>Superadmins<br/><b>{rows.filter(r=>isSuperAdminUser(r)).length}</b></span><span>Rechtezuordnungen<br/><b>{rights.length}</b></span><span>Ohne E-Mail<br/><b>{rows.filter(r=>!r.email).length}</b></span></div><GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neuer Benutzer</button><label><Search size={16}/><input placeholder="Benutzer, Name, Rolle oder E-Mail suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload();}}/></label><button className="secondary" type="button" onClick={reload}>Suchen</button></GamStickyToolbar>{err&&<p className="note warn">{err}</p>}<GamScrollArea tall><table className="compact-table"><thead><tr><th>ID</th><th>Benutzer</th><th>Name</th><th>Kontorolle</th><th>userapplication</th><th>E-Mail</th><th>2FA</th><th>Aktion</th></tr></thead><tbody>{filtered.map(r=>{const rc=rightsForUser(r.username).length; return <tr key={r.id} className="clickable-row" title="Anklicken zum Bearbeiten" onClick={()=>startEdit(r)}><td>{r.id}</td><td><b>{r.username}</b></td><td>{r.fullname||'—'}</td><td><span className="badge">{r.role||'user'}</span></td><td>{isSuperAdminUser(r)?'Vollzugriff':`${rc} Zuordnung${rc===1?'':'en'}`}</td><td>{r.email||'—'}</td><td>{r.twoFactorConfigured?'aktiv':'—'}</td><td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); openPassword(r);}}>Passwort</button></td></tr>})}</tbody></table></GamScrollArea><GamDialog open={open} title={form.id?`Benutzer ${form.username} bearbeiten`:'Neuer Benutzer'} onClose={()=>setOpen(false)} firstFocusRef={firstRef} size="large" footer={<><button type="button" onClick={save}>{form.id?'Änderungen speichern':'Benutzer anlegen'}</button>{form.id&&<button className="secondary" type="button" onClick={()=>openPassword()}>Passwort setzen</button>}{form.id&&<button className="secondary" type="button" onClick={reset2fa}>2FA zurücksetzen</button>}{form.id&&<button className="danger" type="button" onClick={remove}>Benutzer löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Schließen</button></>}><div className="tabs"><button type="button" className={activeTab==='user'?'active':''} onClick={()=>setActiveTab('user')}>Benutzer</button><button type="button" className={activeTab==='rights'?'active':''} onClick={()=>setActiveTab('rights')} disabled={!form.username}>Rechte</button></div>{activeTab==='user'&&<div className="newgrid"><label>Benutzername<input ref={firstRef} disabled={!!form.id} value={form.username??''} onChange={e=>setForm({...form,username:e.target.value})}/></label>{!form.id&&<label>Startpasswort<input type="password" value={(form.password as string)??''} placeholder="Standard: changeme" onChange={e=>setForm({...form,password:e.target.value})}/></label>}<label>Name<input value={form.fullname??''} onChange={e=>setForm({...form,fullname:e.target.value})}/></label><label>Kontorolle / Superadmin<select value={form.role??'user'} onChange={e=>setForm({...form,role:e.target.value})}>{roleOptions.map(r=><option key={r.key} value={r.key}>{r.label} ({r.key})</option>)}</select></label><label className="wide-field">E-Mail-Adresse<input value={form.email??''} onChange={e=>setForm({...form,email:e.target.value})}/></label>{isSuperAdminUser(form)&&<p className="note wide-field"><b>Superadmin:</b> Dieser Benutzer bekommt absichtlich Vollzugriff über <code>accounts</code>. Für ihn sind keine <code>userapplication</code>-Einträge nötig.</p>}{form.id&&<p className="note wide-field">2FA: {form.twoFactorConfigured?'aktiv – kann bei Gerätewechsel zurückgesetzt werden.':'nicht eingerichtet'} · Passwort: {form.passwordPresent?'gesetzt':'nicht gesetzt'}</p>}</div>}{activeTab==='rights'&&<div className="rights-editor-stack"><p className="note wide-field">Normale Rechte werden in <code>userapplication</code> gepflegt. Superadmins werden nicht über diese Tabelle eingeschränkt.</p>{isSuperAdminUser(form)?<p className="note warn wide-field">Dieser Benutzer ist Superadmin und hat überall Zugriff. userapplication-Zuordnungen sind deshalb nicht erforderlich.</p>:<><div className="rights-table-panel"><GamScrollArea><table className="compact-table rights-table"><thead><tr><th>ID</th><th>Anwendung</th><th>Gesellschaft</th><th>Rolle</th><th>Aktion</th></tr></thead><tbody>{currentRights.map(r=><tr key={r.id} className="clickable-row" onClick={()=>startEditRight(r)}><td>{r.id}</td><td>{r.application}</td><td>{r.companyId??'alle'}</td><td><span className="badge">{r.role||'user'}</span></td><td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEditRight(r);}}>Bearbeiten</button><button className="danger" type="button" onClick={(e)=>{e.stopPropagation(); removeRight(r);}}>Löschen</button></td></tr>)}{currentRights.length===0&&<tr><td colSpan={5} className="muted">Noch keine userapplication-Rechte für diesen Benutzer.</td></tr>}</tbody></table></GamScrollArea></div><div className="newgrid rights-editor-grid rights-fields-below"><label className="rights-application-field">Anwendung<select ref={rightsFirstRef} value={rightForm.application??''} onChange={e=>setRightForm({...rightForm,application:e.target.value})}><option value="">Bitte wählen</option>{applications.map(a=><option key={a.application} value={a.application}>{a.application}{a.selectable===false?' (nicht auswählbar)':''}</option>)}</select></label><label className="rights-manual-application-field">oder Anwendung manuell<input value={rightForm.application??''} onChange={e=>setRightForm({...rightForm,application:e.target.value})}/></label><label className="rights-company-field">Gesellschafts-ID<input type="number" placeholder="leer = alle Gesellschaften" value={rightForm.companyId??''} onChange={e=>setRightForm({...rightForm,companyId:e.target.value===''?undefined:Number(e.target.value)})}/></label><label className="rights-role-field">Rolle<select value={rightForm.role??'user'} onChange={e=>setRightForm({...rightForm,role:e.target.value})}>{permissionRoleOptions.map(r=><option key={r.key} value={r.key}>{r.label} ({r.key})</option>)}</select></label><div className="wide-field rights-action-row rights-assignment-actions"><button type="button" onClick={saveRight}>{rightForm.id?'Rechte ändern':'Rechte hinzufügen'}</button>{rightForm.id&&<button className="danger" type="button" onClick={()=>removeRight()}>Recht löschen</button>}{rightForm.id&&<button className="secondary" type="button" onClick={startNewRight}>Neue Zuordnung</button>}</div></div></>}</div>}</GamDialog><GamDialog open={passwordOpen} title={`Passwort für ${form.username||'Benutzer'} setzen`} onClose={()=>setPasswordOpen(false)} size="small" footer={<><button type="button" onClick={savePassword}>Passwort speichern</button><button className="secondary" type="button" onClick={()=>setPasswordOpen(false)}>Abbrechen</button></>}><label>Neues Passwort<input type="password" value={password} onChange={e=>setPassword(e.target.value)} autoFocus/></label><p className="muted">Das Passwort wird kompatibel zur bestehenden GAM-accounts-Tabelle als SHA-256 gespeichert.</p></GamDialog></section>;
}

function PermissionsPage(){
  return <section className="card"><h2>Benutzer/Rechte</h2><p className="muted">Schritt 38i4: Der separate Rechtebereich ist in Benutzer/Rechte aufgegangen. Normale Rechte werden über <code>userapplication</code> gepflegt; <code>mainuser</code> bekommt zusätzlich Reports, <code>superadmin</code> kommt zentral aus <code>accounts</code>.</p><button type="button" onClick={()=>gamNotify('info','Bitte links in „Benutzer/Rechte“ den gewünschten Benutzer öffnen und den Tab „Rechte“ verwenden.',0)}>Hinweis anzeigen</button></section>;
}

function InventoryPage(){
 const [rows,setRows]=useState<InventoryDevice[]>([]);
 const [stats,setStats]=useState<InventoryStats|null>(null);
 const [selected,setSelected]=useState<InventoryDeviceDetail|null>(null);
 const [q,setQ]=useState('');
 const [source,setSource]=useState('all');
 const [activeOnly,setActiveOnly]=useState(false);
 const [branches,setBranches]=useState<any[]>([]);
 const [companies,setCompanies]=useState<any[]>([]);
 const [materials,setMaterials]=useState<any[]>([]);
 const [materialId,setMaterialId]=useState<number|undefined>();
 const [form,setForm]=useState<any>({source:'new', name:'', type:'', serialNumber:'', inventoryNumber:'', manufacturer:'', ip:'', location:'', branchId:'', medicalDevice:false, electricalDevice:false, inventory:false, active:true, inUse:true, acquisitionDate:'', note:''});
 const [msg,setMsg]=useState('');
 const [err,setErr]=useState('');
 const [inventoryModalOpen,setInventoryModalOpen]=useState(false);
 const inventoryFormRef = useRef<HTMLElement | null>(null);
 const inventoryFirstInputRef = useRef<HTMLInputElement | null>(null);
 const scrollToInventoryForm=(focus=false)=>{setInventoryModalOpen(true); setTimeout(()=>{if(focus) inventoryFirstInputRef.current?.focus();},140);};
 const branchLabel=(id?:number)=>{const b=branches.find((x:any)=>x.id===id); return b ? `${b.code??''} ${b.name??''}`.trim() : (id?`#${id}`:'—')};
 const companyLabel=(id?:number)=>{const c=companies.find((x:any)=>x.id===id); return c ? (c.name??`#${id}`) : (id?`#${id}`:'—')};
 const loadAll=async()=>{setErr(''); try{const [r,s]=await Promise.all([loadInventoryDevices(q,source,activeOnly,250),loadInventoryStats()]); setRows(r); setStats(s);}catch(e:any){setErr(e.message??'Geräte konnten nicht geladen werden')}};
 const loadDetail=async(d:InventoryDevice)=>{setErr(''); try{const detail=await loadInventoryDevice(d.source,d.id); setSelected(detail); setFormFromDevice(detail.device); setInventoryModalOpen(true); setTimeout(()=>inventoryFirstInputRef.current?.focus(),140);}catch(e:any){setErr(e.message??'Detail konnte nicht geladen werden')}};
 const setFormFromDevice=(d:InventoryDevice)=>setForm({source:d.source, id:d.id, name:d.name??'', type:d.type??'', serialNumber:d.serialNumber??'', inventoryNumber:d.inventoryNumber??'', manufacturer:d.manufacturer??'', ip:d.ip??'', location:d.location??'', branchId:d.branchId??'', medicalDevice:!!d.medicalDevice, electricalDevice:!!d.electricalDevice, inventory:!!d.inventoryRelevant, active:d.active!==false, inUse:d.inUse!==false, acquisitionDate:(d.acquiredAt??'').slice(0,10), note:d.note??''});
 const newDevice=()=>{setSelected(null); setForm({source:'new', name:'', type:'Drucker', serialNumber:'', inventoryNumber:'', manufacturer:'', ip:'', location:'', branchId:'', medicalDevice:false, electricalDevice:false, inventory:true, active:true, inUse:true, acquisitionDate:'', note:''}); setMsg('Neues Gerät vorbereiten.'); scrollToInventoryForm(true);};
 const payload=()=>({name:form.name,type:form.type,serialNumber:form.serialNumber,inventoryNumber:form.inventoryNumber,manufacturer:form.manufacturer,ip:form.ip,location:form.location,branchId:form.branchId?Number(form.branchId):undefined,medicalDevice:!!form.medicalDevice,electricalDevice:!!form.electricalDevice,inventory:!!form.inventory,active:!!form.active,inUse:!!form.inUse,acquisitionDate:form.acquisitionDate||undefined,note:form.note});
 const save=async()=>{setErr(''); setMsg(''); try{let detail:any; if(form.id){detail=await updateInventoryDevice(form.source, Number(form.id), payload());} else {detail=await createInventoryDevice(payload());} setSelected(detail); setFormFromDevice(detail.device); setMsg('Gerät gespeichert.'); gamNotify('success','Gerät erfolgreich gespeichert.'); setInventoryModalOpen(false); await loadAll();}catch(e:any){const m=e.message??'Speichern fehlgeschlagen'; setErr(m); gamNotify('error',m,0)}};
 const remove=async()=>{if(!form.id || form.source!=='new') return; if(!confirm('Dieses neue Gerät wirklich löschen?')) return; setErr(''); try{await deleteInventoryDevice(Number(form.id)); setSelected(null); newDevice(); await loadAll(); setMsg('Gerät gelöscht.');}catch(e:any){setErr(e.message??'Löschen fehlgeschlagen')}};
 const saveAssignment=async()=>{if(!selected?.device?.id || selected.device.source!=='new') return; try{const detail=await setInventoryDeviceAssignment(selected.device.id, form.branchId?Number(form.branchId):undefined, selected.assignments?.[0]?.companyId); setSelected(detail); setFormFromDevice(detail.device); setMsg('Filialzuordnung gespeichert.'); await loadAll();}catch(e:any){setErr(e.message??'Zuordnung fehlgeschlagen')}};
 const saveCompanyAssignment=async(companyId?:number)=>{if(!selected?.device?.id || selected.device.source!=='new') return; try{const detail=await setInventoryDeviceAssignment(selected.device.id, form.branchId?Number(form.branchId):selected.device.branchId, companyId); setSelected(detail); setMsg('Gesellschaftszuordnung gespeichert.');}catch(e:any){setErr(e.message??'Gesellschaft konnte nicht gespeichert werden')}};
 const addMaterial=async()=>{if(!selected?.device?.id || selected.device.source!=='new' || !materialId) return; try{const detail=await addInventoryDeviceMaterial(selected.device.id,materialId); setSelected(detail); setMsg('Material verknüpft.');}catch(e:any){setErr(e.message??'Material konnte nicht verknüpft werden')}};
 const delMaterial=async(linkId:number)=>{if(!selected?.device?.id || selected.device.source!=='new') return; try{const detail=await removeInventoryDeviceMaterial(selected.device.id,linkId); setSelected(detail); setMsg('Materialverknüpfung entfernt.');}catch(e:any){setErr(e.message??'Material konnte nicht entfernt werden')}};
 useEffect(()=>{loadAll(); loadInventoryBranches().then(setBranches).catch(()=>{}); loadInventoryCompanies().then(setCompanies).catch(()=>{}); loadInventoryMaterials('',300).then(ms=>{setMaterials(ms); if(ms[0]) setMaterialId(ms[0].id)}).catch(()=>{});},[]);
 return <section className="grid inventory-admin-grid">
  <section className="card inventory-list-card">
   <div className="row"><h2>{moduleText(currentUiLanguage(),'inventory')}</h2></div>
   <p className="muted">Schritt 38d: Geräteverzeichnis im vollständigen Administrationsbetrieb mit Suche, Detailansicht, Bearbeitung, Filial-/Gesellschaftszuordnung und Materialbezug.</p>
   {stats&&<div className="stats"><span>Altgeräte: <b>{stats.legacyDeviceCount}</b></span><span>Neue Geräte: <b>{stats.newDeviceCount}</b></span><span>Zuordnungen: <b>{stats.branchAssignmentCount}</b></span><span>Medizin: <b>{stats.medicalDeviceCount}</b></span><span>Elektro: <b>{stats.electricalDeviceCount}</b></span><span>Außer Betrieb: <b>{stats.outOfServiceCount}</b></span></div>}
   <div className="toolbar sticky-actionbar"><button onClick={newDevice}><FilePlus2 size={16}/> Neues Gerät</button><label><Search size={16}/><input placeholder="Gerät suchen: Typ, Seriennummer, Hersteller, Standort" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') loadAll()}}/></label><select value={source} onChange={e=>setSource(e.target.value)}><option value="all">Alle Geräte</option><option value="legacy">Alt-GAM-Geräte</option><option value="new">geräte_neu</option></select><label className="toggle-field"><input type="checkbox" checked={activeOnly} onChange={e=>setActiveOnly(e.target.checked)}/> nur aktiv</label><button className="secondary" onClick={loadAll}>Suchen</button></div>
   {selected&&<div className="jump-actions"><button className="secondary" type="button" onClick={()=>scrollToInventoryForm(true)}>📝 Bearbeiten</button></div>}<div className="scroll-table scroll-table-tall"><table className="compact-table"><thead><tr><th>ID</th><th>Quelle</th><th>Name</th><th>Typ</th><th>Serie</th><th>Filiale</th><th>Status</th></tr></thead><tbody>{rows.map(d=><tr key={`${d.source}-${d.id}`} className={selected?.device?.id===d.id && selected?.device?.source===d.source?'active-row':''} onClick={()=>loadDetail(d)}><td>{d.id}</td><td>{d.source==='new'?'neu':'alt'}</td><td><b>{d.name||'—'}</b><br/><small>{d.manufacturer||d.ip||'—'}</small></td><td>{d.type||'—'}</td><td>{d.serialNumber||d.inventoryNumber||'—'}</td><td>{d.branchCode||d.branchName||'—'}</td><td>{d.active===false?'außer Betrieb':'aktiv'}</td></tr>)}</tbody></table></div>
  </section>
  {inventoryModalOpen&&<div className="gam-modal-backdrop" onClick={()=>setInventoryModalOpen(false)} />}
  <section className={inventoryModalOpen?"card inventory-detail-card gam-modal-card":"card inventory-detail-card gam-modal-card gam-modal-hidden"} ref={inventoryFormRef}>
   <div className="modal-title-row"><h2>{form.id?`Gerät #${form.id}`:'Neues Gerät'}</h2><button className="secondary icon-only" type="button" onClick={()=>setInventoryModalOpen(false)} aria-label="Dialog schließen">×</button></div>
   <div className="newgrid inventory-form"><label>Name<input ref={inventoryFirstInputRef} value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label><label>Typ<input value={form.type} onChange={e=>setForm({...form,type:e.target.value})}/></label><label>Seriennummer<input value={form.serialNumber} onChange={e=>setForm({...form,serialNumber:e.target.value})}/></label>{form.source==='legacy'&&<><label>Inventarnummer<input value={form.inventoryNumber} onChange={e=>setForm({...form,inventoryNumber:e.target.value})}/></label><label>Hersteller<input value={form.manufacturer} onChange={e=>setForm({...form,manufacturer:e.target.value})}/></label></>} {form.source==='new'&&<label>IP / Netzwerk<input value={form.ip} onChange={e=>setForm({...form,ip:e.target.value})}/></label>}<label>Standort<input value={form.location} onChange={e=>setForm({...form,location:e.target.value})}/></label><label>Filiale<select value={form.branchId} onChange={e=>setForm({...form,branchId:e.target.value})}><option value="">—</option>{branches.map((b:any)=><option key={b.id} value={b.id}>{b.code} · {b.name}</option>)}</select></label>{form.source==='legacy'&&<label>Anschaffung<input type="date" value={form.acquisitionDate} onChange={e=>setForm({...form,acquisitionDate:e.target.value})}/></label>}<label className="toggle-field"><input type="checkbox" checked={form.active} onChange={e=>setForm({...form,active:e.target.checked})}/> aktiv</label><label className="toggle-field"><input type="checkbox" checked={form.inUse} onChange={e=>setForm({...form,inUse:e.target.checked})}/> im Einsatz</label><label className="toggle-field"><input type="checkbox" checked={form.medicalDevice} onChange={e=>setForm({...form,medicalDevice:e.target.checked})}/> Medizinprodukt</label><label className="toggle-field"><input type="checkbox" checked={form.electricalDevice} onChange={e=>setForm({...form,electricalDevice:e.target.checked})}/> Elektrogerät</label><label className="toggle-field"><input type="checkbox" checked={form.inventory} onChange={e=>setForm({...form,inventory:e.target.checked})}/> Inventar</label><label className="wide">Bemerkung<textarea value={form.note} onChange={e=>setForm({...form,note:e.target.value})}/></label></div>
   <div className="toolbar"><button onClick={save}>Speichern</button><button className="secondary" type="button" onClick={()=>setInventoryModalOpen(false)}>Abbrechen</button>{form.source==='new'&&form.id&&<button className="danger" onClick={remove}>Löschen</button>}{form.source==='new'&&form.id&&<button className="secondary" onClick={saveAssignment}>Filiale speichern</button>}</div>
   {msg&&<p className="note ok">{msg}</p>}{err&&<p className="error">{err}</p>}
   {selected&&<div className="inventory-subcards"><section><h3>Zuordnungen</h3>{selected.device.source==='new'?<><label>Gesellschaft<select value={selected.assignments?.[0]?.companyId??''} onChange={e=>saveCompanyAssignment(e.target.value?Number(e.target.value):undefined)}><option value="">—</option>{companies.map((c:any)=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label><table><thead><tr><th>Filiale</th><th>Gesellschaft</th></tr></thead><tbody>{(selected.assignments??[]).map(a=><tr key={a.id}><td>{branchLabel(a.branchId)}</td><td>{companyLabel(a.companyId)}</td></tr>)}</tbody></table></>:<p className="muted">Altgeräte speichern die Filiale direkt im Gerätedatensatz.</p>}</section><section><h3>Lager-/Materialbezug</h3>{selected.device.source==='new'?<><div className="toolbar"><select value={materialId??''} onChange={e=>setMaterialId(Number(e.target.value))}>{materials.map((m:any)=><option key={m.id} value={m.id}>{m.name} {m.properties?`· ${m.properties}`:''} ({m.stock??0})</option>)}</select><button className="secondary" onClick={addMaterial}>Material hinzufügen</button></div><table><thead><tr><th>Material</th><th>Bestand</th><th></th></tr></thead><tbody>{(selected.consumables??[]).map(c=><tr key={c.id}><td><b>{c.name}</b><br/><small>{c.properties||c.manufacturerEmail||''}</small></td><td>{c.amount??'—'}</td><td><button className="danger" onClick={()=>delMaterial(c.id)}>Entfernen</button></td></tr>)}</tbody></table></>:<p className="muted">Materialverknüpfungen sind für das moderne Geräteverzeichnis aktiv.</p>}</section></div>}
  </section>
 </section>
}
function WarehousePage(){
 const empty={kind:'lager', name:'', description:'', location:'', quantity:0, properties:'', manufacturerEmail:''};
 const [rows,setRows]=useState<WarehouseItem[]>([]); const [stats,setStats]=useState<WarehouseStats|null>(null); const [selected,setSelected]=useState<WarehouseItem|null>(null);
 const [q,setQ]=useState(''); const [kind,setKind]=useState('all'); const [onlyStock,setOnlyStock]=useState(false); const [form,setForm]=useState<any>(empty); const [movement,setMovement]=useState(1); const [reason,setReason]=useState('Lagerverwaltung GAM 2.0'); const [msg,setMsg]=useState(''); const [err,setErr]=useState(''); const [warehouseModalOpen,setWarehouseModalOpen]=useState(false);
 const warehouseFormRef = useRef<HTMLElement | null>(null);
 const warehouseFirstInputRef = useRef<HTMLInputElement | null>(null);
 const scrollToWarehouseForm=(focus=false)=>{setWarehouseModalOpen(true); setTimeout(()=>{if(focus) warehouseFirstInputRef.current?.focus();},140);};
 const titleOf=(r?:WarehouseItem|null)=>r ? `${r.kind==='verbrauchsmaterial'?'Material':'Lager'} #${r.id} · ${r.name||r.description||'—'}` : 'Neuer Eintrag';
 async function reload(){setErr(''); try{const [r,s]=await Promise.all([loadWarehouseItems(q,kind,onlyStock,300), loadWarehouseStats()]); setRows(r); setStats(s);}catch(e:any){setErr(e.message??'Lagerdaten konnten nicht geladen werden')}}
 function setFormFromItem(item:WarehouseItem){setForm({kind:item.kind||'lager', name:item.name??'', description:item.description??item.name??'', location:item.location??'', quantity:item.quantity??0, properties:item.properties??'', manufacturerEmail:item.manufacturerEmail??''});}
 function selectItem(item:WarehouseItem){setSelected(item); setFormFromItem(item); setMsg(''); setErr(''); setWarehouseModalOpen(true); setTimeout(()=>warehouseFirstInputRef.current?.focus(),140);}
 function newItem(nextKind=form.kind||'lager'){setSelected(null); setForm({...empty, kind:nextKind}); setMsg(''); setErr(''); scrollToWarehouseForm(true);}
 const payload=()=>({kind:form.kind, name:form.name, description:form.description, location:form.location, quantity:Number(form.quantity||0), properties:form.properties, manufacturerEmail:form.manufacturerEmail});
 async function save(){setErr(''); setMsg(''); try{const saved=selected?.id ? await updateWarehouseItem(form.kind, selected.id, payload()) : await createWarehouseItem(form.kind, payload()); setSelected(saved); setFormFromItem(saved); setMsg('Eintrag gespeichert.'); gamNotify('success','Lager-Eintrag erfolgreich gespeichert.'); setWarehouseModalOpen(false); await reload();}catch(e:any){const m=e.message??'Speichern fehlgeschlagen'; setErr(m); gamNotify('error',m,0)}}
 async function remove(){if(!selected?.id) return; if(!confirm('Diesen Lagereintrag wirklich löschen?')) return; setErr(''); setMsg(''); try{await deleteWarehouseItem(selected.kind, selected.id); setSelected(null); setForm(empty); setMsg('Eintrag gelöscht.'); await reload();}catch(e:any){setErr(e.message??'Löschen fehlgeschlagen')}}
 async function adjust(delta:number){if(!selected?.id) return; setErr(''); setMsg(''); try{const next=(Number(form.quantity||0)+delta); const saved=await updateWarehouseStock(selected.kind, selected.id, next, reason); setSelected(saved); setFormFromItem(saved); setMsg('Bestand aktualisiert.'); await reload();}catch(e:any){setErr(e.message??'Bestandsbuchung fehlgeschlagen')}}
 useEffect(()=>{reload();},[]);
 return <section className="grid warehouse-admin"><section className="card warehouse-list-card"><div className="row"><h2>Lagerverwaltung</h2></div>{stats&&<p className="note">Lagerpositionen: <b>{stats.storageItemCount}</b> · Verbrauchsmaterial: <b>{stats.consumableCount}</b> · mit Bestand: <b>{stats.consumablesWithStock}</b> · Gerätebezüge: <b>{stats.deviceConsumableLinks}</b></p>}<div className="toolbar sticky-actionbar"><button onClick={()=>newItem(form.kind)}><FilePlus2 size={16}/> Neuer Eintrag</button><label><Search size={16}/><input placeholder="Lager/Material suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload()}}/></label><select value={kind} onChange={e=>setKind(e.target.value)}><option value="all">Alle</option><option value="lager">Lager</option><option value="verbrauchsmaterial">Verbrauchsmaterial</option></select><label className="toggle-field"><input type="checkbox" checked={onlyStock} onChange={e=>setOnlyStock(e.target.checked)}/> nur mit Bestand</label><button className="secondary" onClick={reload}>Suchen</button></div>{selected&&<div className="jump-actions"><button className="secondary" type="button" onClick={()=>scrollToWarehouseForm(true)}>📝 Bearbeiten</button></div>}{err&&<p className="note warn">{err}</p>}{msg&&<p className="note ok">{msg}</p>}<div className="scroll-table scroll-table-tall"><table><thead><tr><th>Typ</th><th>Name/Beschreibung</th><th>Lagerort/Eigenschaft</th><th>Bestand</th><th>Geräte</th></tr></thead><tbody>{rows.map(r=><tr key={`${r.kind}-${r.id}`} className={selected?.id===r.id&&selected?.kind===r.kind?'invoice-row-active':''} onClick={()=>selectItem(r)}><td>{r.kind==='verbrauchsmaterial'?'Material':'Lager'}</td><td><b>{r.name||r.description}</b><br/><small>#{r.id}</small></td><td>{r.location||r.properties||'—'}</td><td>{r.quantity??0}</td><td>{r.linkedDeviceCount??0}</td></tr>)}</tbody></table></div></section>{warehouseModalOpen&&<div className="gam-modal-backdrop" onClick={()=>setWarehouseModalOpen(false)} />}<section className={warehouseModalOpen?"card detail gam-modal-card":"card detail gam-modal-card gam-modal-hidden"} ref={warehouseFormRef}><div className="modal-title-row"><h2>{titleOf(selected)}</h2><button className="secondary icon-only" type="button" onClick={()=>setWarehouseModalOpen(false)} aria-label="Dialog schließen">×</button></div><div className="newgrid"><label>Typ<select value={form.kind} onChange={e=>{const k=e.target.value; setForm({...form,kind:k}); if(!selected) newItem(k)}} disabled={!!selected}><option value="lager">Lagerposition</option><option value="verbrauchsmaterial">Verbrauchsmaterial</option></select></label><label>Name<input ref={warehouseFirstInputRef} value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label><label>Beschreibung<input value={form.description} onChange={e=>setForm({...form,description:e.target.value})}/></label>{form.kind==='lager'?<label>Lagerort<input value={form.location} onChange={e=>setForm({...form,location:e.target.value})}/></label>:<><label>Eigenschaften<input value={form.properties} onChange={e=>setForm({...form,properties:e.target.value})}/></label><label>Hersteller-/Bestell-E-Mail<input value={form.manufacturerEmail} onChange={e=>setForm({...form,manufacturerEmail:e.target.value})}/></label></>}<label>Bestand<input type="number" step="1" value={form.quantity} onChange={e=>setForm({...form,quantity:Number(e.target.value)})}/></label></div><div className="toolbar"><button onClick={save}>{selected?'Änderungen speichern':'Eintrag anlegen'}</button><button className="secondary" onClick={()=>newItem(form.kind)}>Zurücksetzen</button><button className="secondary" type="button" onClick={()=>setWarehouseModalOpen(false)}>Abbrechen</button>{selected&&<button className="danger" onClick={remove}>Löschen</button>}</div>{selected&&<section className="subcard"><h3>Bestandsbuchung</h3><div className="newgrid"><label>Menge<input type="number" step="1" value={movement} onChange={e=>setMovement(Number(e.target.value))}/></label><label>Grund<input value={reason} onChange={e=>setReason(e.target.value)}/></label><button className="secondary" onClick={()=>adjust(Math.abs(movement))}>Zugang buchen</button><button className="secondary" onClick={()=>adjust(-Math.abs(movement))}>Abgang buchen</button></div></section>}<p className="muted">Schritt 38e macht Lagerpositionen und Verbrauchsmaterial vollständig bearbeitbar. Materialbewegungen und Gerätezuordnungen stehen darunter im Inventar↔Lager-Bereich zur Verfügung.</p></section></section>}


function oldGamIcon(name: string) {
  return `/old-gam-icons/${name}`;
}


function uiSafe(keyOrText: string, lang: GamLanguage = currentUiLanguage()) {
  const translated = ui(keyOrText, lang);
  if (translated !== keyOrText) return translated;
  const mapped: Record<string,string> = {
    newInvoice: 'newInvoice',
    invoiceSearch: 'invoiceSearch',
    reports: 'reports',
    users: 'usersRights',
    usersRights: 'usersRights',
    compliance: 'checks',
    checks: 'checks',
    paymentAdvice: 'paymentAdvice'
  };
  const mappedKey = mapped[keyOrText] ?? mapped[moduleKey(keyOrText)] ?? '';
  return mappedKey ? ui(mappedKey, lang) : keyOrText;
}

function translatedInvoiceLabel(number?: string, lang: GamLanguage = currentUiLanguage()): string {
  const n = number ?? '';
  if (/S$/.test(n)) return `${ui('cancelInvoice', lang)} ${n}`;
  if (/G$/.test(n)) return `${ui('creditNote', lang)} ${n}`;
  if (/P$/.test(n)) return `Proforma-${ui('invoiceTypeInvoice', lang)} ${n}`;
  if (/Z\d*$/.test(n)) return `${ui('invoiceTypePaymentAdvice', lang)} ${n}`;
  return `${ui('invoiceTypeInvoice', lang)} ${n}`;
}

function paymentMethodKey(value?: string) {
  const v = String(value ?? '').toLowerCase();
  if (v.includes('bar')) return 'paymentCash';
  if (v.includes('karte') || v.includes('card')) return 'paymentCard';
  if (v.includes('überweisung') || v.includes('ueberweisung') || v.includes('transfer')) return 'paymentTransfer';
  return 'paymentUnknown';
}

function InvoicesPage(){
 const [rows,setRows]=useState<InvoiceSummary[]>([]); const [selected,setSelected]=useState<InvoiceDetail|null>(null); const [selectedRowKey,setSelectedRowKey]=useState<string>(''); const [filter,setFilter]=useState(''); const [mode,setMode]=useState<'search'|'new'|'edit'>('search'); const [companies,setCompanies]=useState<InvoiceCompany[]>([]); const [products,setProducts]=useState<ProductDto[]>([]); const [lbd,setLbd]=useState<LbdRecipient|null>(null); const [textPreview,setTextPreview]=useState<InvoiceTextPreview|null>(null); const [searchCompanyId,setSearchCompanyId]=useState<number|undefined>(); const [searchInfo,setSearchInfo]=useState(''); const [pdfLanguage,setPdfLanguage]=useState<GamLanguage>('de');
 const invoiceRowKey=(r:InvoiceSummary)=>`${r.companyId ?? searchCompanyId ?? ''}:${r.number}`;
 async function refresh(q=filter){if(!searchCompanyId){setRows([]); setSelected(null); setSelectedRowKey(''); setSearchInfo(ui('pleaseSelectCompany')); return;} setSearchInfo(''); const r=await loadInvoices(100,q,searchCompanyId); setRows(r); if(r[0]){setSelectedRowKey(invoiceRowKey(r[0])); setSelected(await loadInvoice(r[0].number, r[0].companyId));} else {setSelected(null); setSelectedRowKey('');}}
 async function select(number:string){const row=rows.find(r=>r.number===number); if(row) setSelectedRowKey(invoiceRowKey(row)); const detail=await loadInvoice(number, row?.companyId ?? searchCompanyId); setSelected(detail); setSelectedRowKey(`${detail.summary.companyId ?? row?.companyId ?? searchCompanyId ?? ''}:${detail.summary.number}`);}
 useEffect(()=>{loadCompanies().then(cs=>{setCompanies(cs); if(cs[0]) setSearchCompanyId(cs[0].id)}).catch(()=>{}); loadProducts('',200, new Date().toISOString().slice(0,10)).then(setProducts).catch(()=>{}); loadLbdPreview().then(setLbd).catch(()=>setLbd(null));},[]);
 useEffect(()=>{if(selected) loadInvoiceTextPreview(selected.summary.companyId,pdfLanguage,selected.summary.invoiceDate??'',lbd?.file??'').then(setTextPreview).catch(()=>setTextPreview(null)); else setTextPreview(null)},[selected?.summary.number,selected?.summary.companyId,pdfLanguage,lbd?.file]);
 return <>
  <section className="toolbar invoice-menu">
    <button className={mode==='new'?'active':''} onClick={()=>setMode('new')}><img src={oldGamIcon("save.png")} alt="" className="action-icon" /> {ui("newInvoice")}</button>
    <button className={mode==='search'?'active secondary':'secondary'} onClick={()=>setMode('search')}><img src={oldGamIcon("search.png")} alt="" className="action-icon" /> {ui("invoiceSearch")}</button>
    <button className="secondary" disabled={!selected} onClick={()=>setMode('edit')}>{ui("invoiceEdit")}</button>
  </section>
  {mode==='new'&&<InvoiceEditor initialCompanyId={searchCompanyId} onSaved={async d=>{await refresh(); setSelected(d.invoice); setMode('search');}}/>}
  {mode==='edit'&&selected&&<InvoiceEditor existing={selected} onSaved={async d=>{await refresh(); setSelected(d.invoice); setMode('search');}}/>}
  {mode==='search'&&<section className="layout invoice-search-layout">
    <aside className="card search-panel">
      <h2>{ui("invoiceSearch")}</h2>
      <label>{ui("company")}<select value={searchCompanyId??''} onChange={e=>setSearchCompanyId(e.target.value?Number(e.target.value):undefined)}><option value="">{ui("pleaseChoose")}</option>{companies.map(c=><option key={c.id} value={c.id}>{c.name??c.code??c.id}</option>)}</select></label>
      <label><Search size={16}/><input placeholder={ui("searchPlaceholder")} value={filter} onChange={e=>setFilter(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') refresh(filter)}}/></label>
      <button className="secondary" disabled={!searchCompanyId} onClick={()=>refresh(filter)}>{ui("search")}</button>
      {searchInfo&&<p className="note warn">{searchInfo}</p>}
      <div className="list invoice-result-list">{(rows ?? []).map(r=>{const active=selectedRowKey===invoiceRowKey(r); return <button key={r.id} className={active?'invoice-row-active':''} aria-current={active?'true':undefined} onClick={()=>select(r.number)}><b>{translatedInvoiceLabel(r.number)}</b><small>{r.invoiceDate} · {money(r.totalGross)} · {r.companyName}</small></button>})}</div>
    </aside>
    <section className="card detail">{selected?<><div className="row"><h2>{translatedInvoiceLabel(selected.summary.number)}</h2><div className="download-actions"><label className="pdf-language-select">{ui("pdfLanguage")}<select value={pdfLanguage} onChange={e=>setPdfLanguage(e.target.value as GamLanguage)}>{LANGUAGES.map(l=><option key={l.value} value={l.value}>{l.label}</option>)}</select></label><a className="buttonlink" target="_blank" href={pdfUrl(selected.summary.number,pdfLanguage,selected.summary.companyId)}><Download size={16}/> {ui("zugferdPdf")}</a><a className="buttonlink secondarylink" target="_blank" href={zugferdXmlUrl(selected.summary.number,selected.summary.companyId)}><Download size={16}/> {ui("xml")}</a></div></div><p>{selected.summary.companyName} · {selected.summary.invoiceDate} · {money(selected.totals?.gross ?? selected.summary.totalGross)}</p><ExportCheck number={selected.summary.number} companyId={selected.summary.companyId}/><InvoiceAccessBox number={selected.summary.number} companyId={selected.summary.companyId}/><PreviewErrorBoundary><InvoiceTextPreviewPanel preview={textPreview} lines={(selected.lines ?? []).map(l=>({productId:l.productId,quantity:l.quantity,price:l.price,vat:l.vat,description:l.description} as any))} products={products ?? []} company={companies.find(c=>c.id===selected.summary.companyId)} number={selected.summary.number} totals={selected.totals} lang={pdfLanguage} summary={selected.summary} recipient={lbd} invoiceDate={selected.summary.invoiceDate??''} treatmentDate={selected.summary.invoiceDate??''} paymentMethod={(selected.summary as any).paymentMethod??'—'}/></PreviewErrorBoundary><InvoiceStatusActions detail={selected} onChanged={setSelected} onCreated={invoice=>{setRows(prev=>[invoice.summary, ...prev.filter(r=>!(r.id===invoice.summary.id || (r.number===invoice.summary.number && r.companyId===invoice.summary.companyId)))]); setSelected(invoice);}}/></>:<div className="empty">{ui("pleaseSelectInvoice")}</div>}</section>
  </section>}
 </>}

function InvoiceStatusActions({detail,onChanged,onCreated}:{detail:InvoiceDetail; onChanged:(d:InvoiceDetail)=>void; onCreated?:(d:InvoiceDetail)=>Promise<void> | void}){const [busy,setBusy]=useState(false); const n=detail.summary.number; const companyId=detail.summary.companyId; const isSpecial=/[SGP]$/.test(n) || /Z\d*$/.test(n); const lang=currentUiLanguage(); async function cancel(){if(!confirm(`${ui('cancelInvoice', lang)} ${n}S fuer ${ui('company', lang)} ${companyId ?? '—'} erstellen?`)) return; setBusy(true); try{const res=await createCancellationInvoice(n, companyId); onChanged(res.invoice); await onCreated?.(res.invoice);}finally{setBusy(false)}} async function credit(){if(!confirm(`${ui('createCreditNote', lang)} zu ${n} fuer ${ui('company', lang)} ${companyId ?? '—'} erstellen?`)) return; setBusy(true); try{const res=await createCreditNote(n, companyId); onChanged(res.invoice); await onCreated?.(res.invoice);}finally{setBusy(false)}} async function del(){if(!confirm(`${ui('technicalDelete', lang)}?`)) return; setBusy(true); try{await deleteInvoiceDraft(detail.summary.number); location.reload();}finally{setBusy(false)}} return <div className="toolbar small"><button className="secondary" disabled={busy || isSpecial} onClick={cancel}>{ui('cancelInvoice', lang)} {n}S</button><button className="secondary" disabled={busy || isSpecial} onClick={credit}>{ui("createCreditNote", lang)}</button><button className="danger" disabled={busy} onClick={del}>{ui("technicalDelete", lang)}</button></div>}

function ExportCheck({number,companyId}:{number:string; companyId?:number}){const [check,setCheck]=useState<any|null>(null); useEffect(()=>{loadExportCheck(number, companyId).then(setCheck).catch(()=>setCheck(null))},[number,companyId]); if(!check) return <p className="muted">{ui("exportCheckLoading")}</p>; return <div className={check.exportable?'note ok':'note warn'}>{check.exportable?ui('zugferdReady'):ui('zugferdIssues')}{check.issues?.length?<ul>{check.issues.map((i:any,idx:number)=>{const msg=String(i.message??''); const translatedMsg=msg.includes('.lbd-Empfängerdatei')?ui('lbdMissingPlaceholder'):msg; const sev=String(i.severity??''); return <li key={idx}>{sev==='WARN'?'WARN':sev}: {i.field} – {translatedMsg}</li>})}</ul>:null}</div>}

function InvoiceEditor({existing,initialCompanyId,onSaved}:{existing?:InvoiceDetail; initialCompanyId?:number; onSaved:(r:any)=>void}){const [products,setProducts]=useState<ProductDto[]>([]); const [productDescTranslations,setProductDescTranslations]=useState<Record<string,string>>({}); const [companies,setCompanies]=useState<InvoiceCompany[]>([]); const [companyId,setCompanyId]=useState(existing?.summary.companyId??initialCompanyId??2); const [productId,setProductId]=useState<number|undefined>(); const [qty,setQty]=useState(1); const [invoiceDate,setInvoiceDate]=useState(toInputDate(existing?.summary.invoiceDate)); const [treatmentDate,setTreatmentDate]=useState(toInputDate(existing?.summary.invoiceDate)); const [paymentMethod,setPaymentMethod]=useState('unbekannt'); const [pdfLanguage,setPdfLanguage]=useState<GamLanguage>('de'); const [reason,setReason]=useState(''); const [remark,setRemark]=useState(''); const [voucherEnabled,setVoucherEnabled]=useState(false); const [couponText,setCouponText]=useState(''); const [couponAmount,setCouponAmount]=useState(0); const [discountEnabled,setDiscountEnabled]=useState(false); const [discountType,setDiscountType]=useState<'percent'|'amount'>('percent'); const [discountValue,setDiscountValue]=useState(0); const [installmentsEnabled,setInstallmentsEnabled]=useState(false); const [installments,setInstallments]=useState(1); const [err,setErr]=useState(''); const [next,setNext]=useState(existing?.summary.number??''); const [lbd,setLbd]=useState<LbdRecipient|null>(null); const [textPreview,setTextPreview]=useState<InvoiceTextPreview|null>(null); const [lines,setLines]=useState<InvoiceCreateLineRequest[]>(existing?.lines.map(l=>({productId:l.productId,quantity:l.quantity,price:l.price,vat:l.vat,branchId:l.branchId,client:l.client,performer:l.performer}))??[]); const [totals,setTotals]=useState<any>(null); const [recipientMode,setRecipientMode]=useState<'lbd'|'manual'>('lbd'); const [recSalutation,setRecSalutation]=useState(''); const [recTitle,setRecTitle]=useState(''); const [recFirstName,setRecFirstName]=useState(''); const [recLastName,setRecLastName]=useState(''); const [recStreet,setRecStreet]=useState(''); const [recPostalCode,setRecPostalCode]=useState(''); const [recCity,setRecCity]=useState(''); const [recCountry,setRecCountry]=useState('Deutschland'); const [recEmail,setRecEmail]=useState('');
 useEffect(()=>{loadProducts('',120,invoiceDate).then(ps=>{setProducts(ps); if(ps[0]) setProductId(ps[0].id)}); loadCompanies().then(cs=>{setCompanies(cs); if(!existing && cs[0]) setCompanyId(cs[0].id)}).catch(()=>{}); loadLbdPreview().then(setLbd).catch(()=>setLbd(null)); if(!existing) loadNextInvoiceNumber(companyId).then(n=>setNext(n.nextNumber)).catch(()=>{});},[]);
 useEffect(()=>{
   if(!lbd?.found || recipientMode !== 'lbd') return;
   setRecSalutation(lbd.salutation || ''); setRecTitle(lbd.title || ''); setRecFirstName(lbd.firstName || ''); setRecLastName(lbd.lastName || '');
   setRecStreet(lbd.street || ''); setRecPostalCode(lbd.postalCode || ''); setRecCity(lbd.city || ''); setRecCountry(lbd.country || 'Deutschland');
   setRecEmail((lbd.rawFields as any)?.email || '');
 },[lbd?.found,lbd?.file,recipientMode]);
 useEffect(()=>{
   let cancelled = false;
   const targetLang = normalizeGamLanguage(pdfLanguage);
   const entries: Record<string,string> = {};
   (products ?? []).forEach(p=>{ if(p?.description) entries[productTranslationKey(p)] = p.description; });
   if(Object.keys(entries).length && targetLang !== 'de') {
     loadUiTranslationsLive(targetLang, entries)
       .then(res=>{ if(!cancelled) setProductDescTranslations(res); })
       .catch(()=>{ if(!cancelled) setProductDescTranslations({}); });
   } else setProductDescTranslations({});
   return ()=>{ cancelled = true; };
 }, [pdfLanguage, JSON.stringify((products ?? []).map(p=>[p.id,p.code,p.description]))]);
 useEffect(()=>{if(!existing) loadNextInvoiceNumber(companyId).then(n=>setNext(n.nextNumber)).catch(()=>{})},[companyId]);
 useEffect(()=>{loadProducts('',120,invoiceDate).then(ps=>{setProducts(ps); if(ps.length && !ps.some(p=>p.id===productId)) setProductId(ps[0].id);}).catch(()=>{});},[invoiceDate]);
 useEffect(()=>{if(lines.length) loadCalculate(lines,setTotals).catch(()=>{}); else setTotals(null)},[JSON.stringify(lines)]);
 useEffect(()=>{loadInvoiceTextPreview(companyId,pdfLanguage,treatmentDate,lbd?.file??'').then(setTextPreview).catch(()=>setTextPreview(null));},[companyId,pdfLanguage,treatmentDate,lbd?.file]);
 const editableRecipient = {found: true, file: recipientMode==='lbd' ? (lbd?.file||'') : '', salutation: recSalutation, title: recTitle, firstName: recFirstName, lastName: recLastName, street: recStreet, postalCode: recPostalCode, city: recCity, country: recCountry, rawFields:{email:recEmail}} as any as LbdRecipient;
 const recipientPayload = {manual: recipientMode==='manual', salutation: recSalutation, title: recTitle, firstName: recFirstName, lastName: recLastName, street: recStreet, postalCode: recPostalCode, city: recCity, country: recCountry, email: recEmail, lbdFile: lbd?.file};
 const p=products.find(x=>x.id===productId); const selectedCompany=companies.find(c=>c.id===companyId); function addLine(){if(!p) return; const next=[...lines,{productId:p.id, quantity:qty, price:p.price??0, vat:p.vat??0}]; const fee=products.find(isPrescriptionFeeProduct); if(isTrainingCompany(selectedCompany) && isGkvProduct(p) && fee && !next.some(l=>l.productId===fee.id)){ next.push({productId:fee.id, quantity:1, price:fee.price??0, vat:fee.vat??0}); } setLines(next)}
 function updateLine(idx:number, patch:Partial<InvoiceCreateLineRequest>){setLines(lines.map((l,i)=>i===idx?{...l,...patch}:l))}
 function removeLine(idx:number){setLines(lines.filter((_,i)=>i!==idx))}
 function buildPayload(){const submitLines = lines.length ? lines : (p ? [{productId:p.id, quantity:qty, price:p.price??0, vat:p.vat??0}] : []); if(!submitLines.length) throw new Error(ui('selectAtLeastOneLine')); return {invoiceDate, treatmentDate, companyId, paymentMethod, reason, remark, couponText: voucherEnabled ? couponText : undefined, couponAmount: voucherEnabled ? couponAmount : undefined, discountType: discountEnabled ? discountType : undefined, discountValue: discountEnabled ? discountValue : undefined, installments: installmentsEnabled ? installments : undefined, lbdFile: lbd?.file, recipient: recipientPayload, lines:submitLines};}
 async function submit(e:React.FormEvent){e.preventDefault(); setErr(''); try{const payload=buildPayload(); const res=existing?await updateInvoice(existing.summary.number,payload):await createInvoice({...payload, number: next || undefined}); gamNotify('success','Rechnung erfolgreich gespeichert.'); onSaved(res);}catch(ex:any){const m=ex.message??ui('saveFailed'); setErr(m); gamNotify('error',m,0);}}
 async function submitProforma(){setErr(''); try{const payload=buildPayload(); const res=await createProformaInvoice({...payload, number: undefined, paymentAdvice: true, reason: reason || 'Proforma-Rechnung'}); gamNotify('success','Proforma erfolgreich erstellt.'); onSaved(res);}catch(ex:any){const m=ex.message??ui('proformaFailed'); setErr(m); gamNotify('error',m,0);}}
 const previewSummary = {id:0, number: next, invoiceDate, companyId, companyName: companies.find(c=>c.id===companyId)?.name, couponAmount: voucherEnabled ? couponAmount : undefined, discountPercent: discountEnabled && discountType==='percent' ? discountValue : undefined, discountRemark: (voucherEnabled && couponText) ? couponText : undefined, installments: installmentsEnabled ? installments : undefined} as InvoiceSummary;
 const translatedProductDescription = (p?:ProductDto, fallbackId?:number|string, lineDescription?:string) => productDescriptionForLanguage(pdfLanguage, p, fallbackId, lineDescription, productDescTranslations);
 return <section className="invoice-editor-layout"><section className="card new invoice-editor"><div className="row"><div><h2>{existing?ui('invoiceEdit'):ui('newInvoice')}</h2><p className="muted">Nummer: <b>{next||ui('loadingNumber')}</b> · {ui('mandatoryZugferd')}</p></div><div className="lbd-compact"><UserRound size={16}/><div><b>{[recSalutation,recTitle,recFirstName,recLastName].filter(Boolean).join(' ')||'Manuelle Adresse'}</b><br/><small>{recStreet} · {[recPostalCode,recCity].filter(Boolean).join(' ')}</small></div></div></div><form onSubmit={submit} className="newgrid"><label>Adresse<input type="radio" checked={recipientMode==='lbd'} onChange={()=>setRecipientMode('lbd')}/> LBD <input type="radio" checked={recipientMode==='manual'} onChange={()=>setRecipientMode('manual')}/> Manuell</label><label>Anrede<input value={recSalutation} onChange={e=>setRecSalutation(e.target.value)}/></label><label>Titel<input value={recTitle} onChange={e=>setRecTitle(e.target.value)}/></label><label>Vorname<input value={recFirstName} onChange={e=>setRecFirstName(e.target.value)}/></label><label>Nachname<input value={recLastName} onChange={e=>setRecLastName(e.target.value)}/></label><label>Straße<input value={recStreet} onChange={e=>setRecStreet(e.target.value)}/></label><label>PLZ<input value={recPostalCode} onChange={e=>setRecPostalCode(e.target.value)}/></label><label>Ort<input value={recCity} onChange={e=>setRecCity(e.target.value)}/></label><label>Land<input value={recCountry} onChange={e=>setRecCountry(e.target.value)}/></label><label>E-Mail<input value={recEmail} onChange={e=>setRecEmail(e.target.value)}/></label><label>{ui("invoiceDate")}<input type="date" value={invoiceDate} onChange={e=>setInvoiceDate(e.target.value)}/><small>{formatGamDate(invoiceDate,currentUiLanguage())}</small></label><label>{ui("treatmentDate")}<input type="date" value={treatmentDate} onChange={e=>setTreatmentDate(e.target.value)}/><small>{formatGamDate(treatmentDate,currentUiLanguage())}</small></label><label>{ui("company")}<select value={companyId} onChange={e=>setCompanyId(Number(e.target.value))}>{companies.map(c=><option key={c.id} value={c.id}>{c.name??c.code??c.id}</option>)}</select></label><label>{ui("paymentMethod")}<select value={paymentMethod} onChange={e=>setPaymentMethod(e.target.value)}><option value="unbekannt">{ui("paymentUnknown")}</option><option value="Barzahlung">{ui("paymentCash")}</option><option value="Kartenzahlung">{ui("paymentCard")}</option><option value="Überweisung">{ui("paymentTransfer")}</option></select></label><label>{ui("pdfLanguage")}<select value={pdfLanguage} onChange={e=>setPdfLanguage(e.target.value as GamLanguage)}>{LANGUAGES.map(l=><option key={l.value} value={l.value}>{l.label}</option>)}</select></label><label>{ui("reason")}<input value={reason} onChange={e=>setReason(e.target.value)}/></label><label>{ui("remark")}<input value={remark} onChange={e=>setRemark(e.target.value)}/></label><label className="toggle-field"><input type="checkbox" checked={voucherEnabled} onChange={e=>setVoucherEnabled(e.target.checked)}/> {ui("voucherEnable")}</label>{voucherEnabled&&<><label>{ui("voucherText")}<input value={couponText} onChange={e=>setCouponText(e.target.value)} placeholder={ui("voucher")}/></label><label>{ui("voucherAmount")}<input type="number" step="0.01" value={couponAmount} onChange={e=>setCouponAmount(Number(e.target.value))}/></label></>}<label className="toggle-field"><input type="checkbox" checked={discountEnabled} onChange={e=>setDiscountEnabled(e.target.checked)}/> {ui("discountEnable")}</label>{discountEnabled&&<><label>{ui("discountType")}<select value={discountType} onChange={e=>setDiscountType(e.target.value as any)}><option value="percent">{ui("percent")}</option><option value="amount">{ui("amount")}</option></select></label><label>{ui("discountValue")}<input type="number" step="0.01" value={discountValue} onChange={e=>setDiscountValue(Number(e.target.value))}/></label></>}<label className="toggle-field"><input type="checkbox" checked={installmentsEnabled} onChange={e=>setInstallmentsEnabled(e.target.checked)}/> {ui("installmentsEnable")}</label>{installmentsEnabled&&<label>{ui("installmentCount")}<select value={installments} onChange={e=>setInstallments(Number(e.target.value))}>{[1,2,3,4,5].map(n=><option key={n} value={n}>{n} {n>1?ui('ratePlural'):ui('rateSingular')}</option>)}</select></label>}<label className="product-field">{ui("product")}<select value={productId??''} onChange={e=>setProductId(Number(e.target.value))}>{products.map(p=><option key={p.id} value={p.id}>{p.code} · {translatedProductDescription(p)} · {money(p.price)} · {p.vat??0}%{p.effectiveNote?` · ${p.effectiveNote}`:''}</option>)}</select></label><label className="quantity-field">{ui("quantity")}<input type="number" step="0.1" value={qty} onChange={e=>setQty(Number(e.target.value))}/></label><button type="button" className="secondary add-position" onClick={addLine}>{ui("addPosition")}</button><button className="save-invoice">{existing?ui('editChangesSave'):ui('saveInvoice')}</button>{!existing&&<button type="button" className="secondary" onClick={submitProforma}>Proforma +P</button>}</form>{lines.length>0&&<table><thead><tr><th>{ui("quantity")}</th><th>{ui("product")}</th><th>{ui('tax')}</th><th>{ui('price')}</th><th></th></tr></thead><tbody>{lines.map((l,idx)=>{const prod=products.find(p=>p.id===l.productId); return <tr key={idx}><td><input type="number" step="0.1" value={l.quantity??1} onChange={e=>updateLine(idx,{quantity:Number(e.target.value)})}/></td><td className="product-cell"><b>{prod?.code??l.productId}</b><br/><span>{translatedProductDescription(prod, l.productId, (l as any).description)}</span></td><td><input type="number" value={l.vat??0} onChange={e=>updateLine(idx,{vat:Number(e.target.value)})}/></td><td><input type="number" step="0.01" value={l.price??0} onChange={e=>updateLine(idx,{price:Number(e.target.value)})}/></td><td><button type="button" className="danger" onClick={()=>removeLine(idx)}>{ui("remove")}</button></td></tr>})}</tbody></table>}{commercialRows(previewSummary, totals, lines, pdfLanguage)}{totals&&<p className="note ok">{ui('net')} {money(totals.net)} · {ui('tax')} {money(totals.vat)} · {ui('gross')} <b>{money(totals.gross)}</b></p>}{err&&<b className="error">{err}</b>}</section><InvoiceTextPreviewPanel preview={textPreview} lines={lines} products={products} company={companies.find(c=>c.id===companyId)} number={next} totals={totals} lang={pdfLanguage} summary={previewSummary} recipient={editableRecipient} invoiceDate={invoiceDate} treatmentDate={treatmentDate} paymentMethod={paymentMethod}/></section>}


function InvoiceTextPreviewPanel({preview,lines,products,company,number,totals,lang,summary,recipient,invoiceDate,treatmentDate,paymentMethod}:{preview:InvoiceTextPreview|null; lines:InvoiceCreateLineRequest[]; products:ProductDto[]; company?:InvoiceCompany; number:string; totals:any; lang:GamLanguage; summary:InvoiceSummary; recipient:LbdRecipient|null; invoiceDate:string; treatmentDate?:string; paymentMethod:string}){
 useEffect(()=>{prepareSpeechVoices(); const synth=(window as any).speechSynthesis; if(synth) synth.onvoiceschanged=()=>prepareSpeechVoices(); return ()=>{ if(synth) synth.onvoiceschanged=null; };},[]);
 const [speaking,setSpeaking]=useState(false);
 const [ttsSettings,setTtsSettings]=useState<TtsSettings>(()=>loadTtsSettings('invoice-preview'));
 const [productDescTranslations,setProductDescTranslations]=useState<Record<string,string>>({});
 useEffect(()=>{
   let cancelled = false;
   const targetLang = normalizeGamLanguage(lang);
   const entries: Record<string,string> = {};
   (lines ?? []).forEach(l => {
     const prod = products.find(p => p.id === l.productId);
     const text = (prod?.description || (l as any).description || '').trim();
     if (text) entries[productTranslationKey(prod, l.productId)] = text;
   });
   if(Object.keys(entries).length && targetLang !== 'de') {
     loadUiTranslationsLive(targetLang, entries)
       .then(res=>{ if(!cancelled) setProductDescTranslations(res); })
       .catch(()=>{ if(!cancelled) setProductDescTranslations({}); });
   } else setProductDescTranslations({});
   return ()=>{ cancelled = true; };
 }, [lang, JSON.stringify((lines ?? []).map(l=>[l.productId,(l as any).description])), JSON.stringify((products ?? []).map(p=>[p.id,p.code,p.description]))]);
 const labels = preview?.labels ?? {};
 const uiLangForPreview = currentUiLanguage();
 const isTechnicalLabel = (value: unknown, key: string) => {
   const text = String(value ?? '').trim();
   if (!text) return true;
   if (text === key) return true;
   // Backend-/JSON-Keys wie treatmentDate, invoiceCustomerFile usw. niemals anzeigen.
   if (/^[a-z][A-Za-z0-9]*$/.test(text) && /[A-Z]/.test(text)) return true;
   return false;
 };
 const t = (key:string, fallback?:string) => !isTechnicalLabel(labels[key], key) ? String(labels[key]).trim() : (INVOICE_METADATA_LABELS[lang]?.[key] || ui(key, lang) || fallback || UI_LABELS.de[key] || key);
 const tui = (key:string, fallback?:string) => { const value = ui(key, uiLangForPreview); return value && value !== key ? value : (fallback ?? key); };
 const translatedProductDescription = (p?:ProductDto, fallbackId?:number|string, lineDescription?:string) => productDescriptionForLanguage(lang, p, fallbackId, lineDescription, productDescTranslations);
 const documentTitle = preview?.documentTitle ?? t('invoice', ui('invoice', lang));
 const recipientName = recipient?.found ? [recipient.salutation,recipient.title,recipient.firstName,recipient.lastName].filter(Boolean).join(' ') : '';
 const recipientAddress = recipient?.found ? [recipient.street, [recipient.postalCode, recipient.city].filter(Boolean).join(' ')].filter(Boolean) : [];
 const lineTexts = lines.map((l)=>{const prod=products.find(p=>p.id===l.productId); const q=l.quantity??1; const price=l.price??prod?.price??0; return `${q} ${translatedProductDescription(prod, l.productId, (l as any).description)}, ${money(q*price)}`;});
 const zugferdNote = t('invoiceZugferdNote', 'ZUGFeRD/Factur-X');
 const customerFile = recipient?.found ? (recipient.file || '—') : '—';
 const taxInfo = [company?.taxNumber, company?.vatId].filter(Boolean).join(' ') || '—';
 const speechText = [documentTitle + ' ' + (number || ''), company?.name, recipientName, recipientAddress.join(' '), preview?.salutation, preview?.invoiceText, `${t('invoiceDate')}: ${formatGamDate(invoiceDate, lang)}`, `${t('treatmentDate', ui('treatmentDate', lang))}: ${formatGamDate(treatmentDate || invoiceDate, lang)}`, `${t('invoiceCustomerFile')}: ${customerFile}`, `${t('invoicePaymentMethod')}: ${paymentMethod || '—'}`, ...lineTexts, totals ? `${t('net')} ${money(totals.net)}. ${t('vat')} ${money(totals.vat)}. ${t('gross')} ${money(totals.gross)}` : '', preview?.lawHint, preview?.greetings, company ? `${t('bank')} ${company.name} IBAN ${company.iban || ''} BIC ${company.bic || ''}. ${t('invoiceTaxNumberVatId')}: ${taxInfo}` : ''].filter(Boolean).join('. ');
 async function toggleReading(){
   const synth=(window as any).speechSynthesis;

   if(speaking){
     stopAllGamSpeech();
     setSpeaking(false);
     return;
   }

   const speechLanguage=effectiveTtsLanguage(ttsSettings.language, lang, currentUiLanguage());
   const cleanText = (speechText || '').replace(/\s+/g, ' ').trim();
   if(!cleanText) return;

   const wantsPiper = ttsSettings.engine === 'piper' || (ttsSettings.engine === 'auto' && piperSupportsLanguage(speechLanguage));
   if (wantsPiper) {
     let runId = 0;
     try {
       stopAllGamSpeech();
       runId = gamSpeechRunId;
       setSpeaking(true);
       await playPiperTtsAudio(speechLanguage, cleanText, runId, () => setSpeaking(false));
       if (runId === gamSpeechRunId) return;
       return;
     } catch (e) {
       // Wenn der Benutzer inzwischen Stop gedrückt hat, darf der abgebrochene
       // Piper-Request nicht mehr in den Browser-Fallback springen.
       if (runId && runId !== gamSpeechRunId) return;
       // Piper ist Standard. Wenn Engine oder Voice noch nicht installiert sind,
       // fällt GAM sofort auf BrowserTTS zurück.
       setSpeaking(false);
     }
   }

   if(!synth) return;
   const voice=selectedBrowserVoice(speechLanguage, ttsSettings.voiceURI);
   const langCode = voice?.lang || speechLang(speechLanguage);

   const chunks = cleanText
     .split(/(?<=[.!?])\s+/)
     .reduce<string[]>((acc, sentence) => {
       const part = sentence.trim();
       if(!part) return acc;
       const last = acc[acc.length - 1] || '';
       if (last && (last + ' ' + part).length <= 180) {
         acc[acc.length - 1] = (last + ' ' + part).trim();
       } else if (!last) {
         acc[acc.length - 1] = part;
       } else {
         acc.push(part);
       }
       return acc;
     }, [''])
     .filter(Boolean);

   stopAllGamSpeech();
   const runId = gamSpeechRunId;
   let idx = 0;
   setSpeaking(true);

   const speakNext = () => {
     if (runId !== gamSpeechRunId) return;
     if (idx >= chunks.length) {
       clearGamSpeechQueue();
       setSpeaking(false);
       return;
     }

     const u = new SpeechSynthesisUtterance(chunks[idx++]);
     u.rate = ttsSettings.rate || 1;
     u.lang = langCode;
     if (voice) u.voice = voice;
     u.onend = speakNext;
     u.onerror = () => window.setTimeout(speakNext, 50);
     gamActiveSpeechUtterances.push(u);
     synth.speak(u);
   };

   prepareSpeechVoices();
   window.setTimeout(speakNext, 80);
 }

 return <section className="card invoice-preview-card"><div className="row"><h2>{tui('invoicePreviewTitle', ui('previewTitle', uiLangForPreview))}</h2></div><div className="invoice-preview-settings"><TtsSettingsControls scope="invoice-preview" lang={currentUiLanguage()} pdfLang={lang} settings={ttsSettings} setSettings={setTtsSettings}/><button type="button" className={speaking?"danger speech-toggle":"secondary speech-toggle speech-start"} onClick={toggleReading}>{speaking?ui("stopReading", currentUiLanguage()):ui("readAloud", currentUiLanguage())}</button></div><p className="muted">{tui('invoicePreviewHelp', ui('previewHelp', uiLangForPreview))}</p><p className="note ok">{tui('accessibilityNote')}</p><div className="invoice-preview-box" aria-label={tui('invoicePreviewTitle', ui('previewTitle', uiLangForPreview))} lang={speechLang(lang)}>
  <div className="row invoice-preview-head"><div><img className="company-logo-preview" src={companyLogoSrc(company)} alt={company?.name ?? 'GAM Gesellschaft'} /><h3>{documentTitle} {number || '—'}</h3></div><span>{company?.name ?? ui('company', lang)}</span></div>
  <div className="preview-meta"><span>{t('invoiceDate', ui('date', lang))}: {formatGamDate(invoiceDate, lang)}</span><span>{t('language', ui('language', lang))}: {LANGUAGES.find(l=>l.value===lang)?.label ?? lang}</span><span>{t('invoicePaymentMethod', ui('payment', lang))}: {ui(paymentMethodKey(paymentMethod), lang) || paymentMethod || '—'}</span></div>
  <div className="preview-meta"><span>{t('treatmentDate', ui('treatmentDate', lang))}: {formatGamDate(treatmentDate || invoiceDate, lang)}</span><span>{t('invoiceCustomerFile', ui('invoiceCustomerFile', lang))}: {customerFile}</span></div>
  <div className="preview-recipient"><b>{t('invoiceRecipient', ui('recipient', lang))}</b>{recipient?.found?<>{recipientName&&<div>{recipientName}</div>}{recipientAddress.map((line,i)=><div key={i}>{line}</div>)}</>:<div className="muted">{t('invoiceNoRecipient', ui('noRecipient', lang))}</div>}</div>
  {preview?<><div className="preview-block">{preview.salutation}</div><div className="preview-block">{preview.invoiceText}</div></>:<p className="muted">{tui('invoiceNoLines', 'Rechnungstexte werden geladen.')}</p>}
  <table><thead><tr><th>{t('invoiceAmount', ui('qty', lang))}</th><th>{t('invoiceProductCode', ui('code', lang))}</th><th>{t('invoiceDescription', ui('description', lang))}</th><th>{t('invoiceTaxRate', ui('tax', lang))}</th><th>{t('invoiceSinglePrice', ui('price', lang))}</th><th>{t('invoiceTotalPrice', ui('lineTotal', lang))}</th></tr></thead><tbody>{lines.length?lines.map((l,idx)=>{const prod=products.find(p=>p.id===l.productId); const q=l.quantity??1; const price=l.price??prod?.price??0; return <tr key={idx}><td>{q}</td><td>{prod?.code ?? l.productId}</td><td>{translatedProductDescription(prod, l.productId, (l as any).description)}</td><td>{l.vat ?? prod?.vat ?? 0}%</td><td>{money(price)}</td><td>{money(q*price)}</td></tr>}):<tr><td colSpan={6} className="muted">{t('invoiceNoLines', ui('noLines', lang))}</td></tr>}</tbody></table>
  {commercialRows(summary, totals, lines, lang, labels)}
  {totals&&<><p className="note ok">{t('net')}: {money(totals.net)} · {t('vat')}: {money(totals.vat)} · {t('gross')}: <b>{money(totals.gross)}</b></p><p className="muted">{zugferdNote}</p></>}
  {preview&&<><div className="preview-block muted">{preview.lawHint}</div><div className="preview-block">{preview.greetings}</div></>}
  {company&&<div className="preview-bank"><b>{t('bank', ui('bank', lang))}</b><br/>{company.accountHolder || company.name}<br/>IBAN: {company.iban || '—'} · BIC: {company.bic || '—'}<br/>{t('invoiceTaxNumberVatId', ui('taxNo', lang))}: {taxInfo}</div>}
  {number&&<div className="preview-qr"><b>{tui('invoicePortalTitle','Digitales Rechnungsportal')}</b><p className="muted">{tui('invoicePortalQrHint','Diese Rechnung digital abrufen: QR-Code wird nach dem Speichern in PDF und Detailansicht erzeugt.')}</p></div>}
 </div></section>
}


function InvoiceAccessBox({number, companyId}:{number:string; companyId?:number}){
 const [info,setInfo]=useState<any>(null);
 const [err,setErr]=useState('');
 useEffect(()=>{ if(!number) return; loadInvoiceAccess(number, companyId).then(setInfo).catch((e:any)=>setErr(e.message??'Abruflink konnte nicht erzeugt werden')); },[number, companyId]);
 return <div className="invoice-access-box"><div><b>{ui("patientPortalDigital")}</b><br/><small>{ui("patientPortalHint")}</small></div>{info&&<><img alt={ui("qrAltPortal")} src={`${info.url}/qr`} /><a className="buttonlink secondarylink" target="_blank" href={info.url}>{ui("openPortal")}</a></>}{err&&<span className="error">{err}</span>}</div>
}

async function loadCalculate(lines:InvoiceCreateLineRequest[], setter:(v:any)=>void){setter(await calculateInvoice(lines));}
function toInputDate(s?:string){if(!s) return new Date().toISOString().slice(0,10); if(/^\d{2}\.\d{2}\.\d{4}$/.test(s)){const [d,m,y]=s.split('.'); return `${y}-${m}-${d}`;} return s.slice(0,10)}
function formatGamDate(value?: string, lang: GamLanguage = currentUiLanguage()) {
  if (!value) return '—';
  const iso = toInputDate(value);
  const d = new Date(`${iso}T00:00:00`);
  if (Number.isNaN(d.getTime())) return value;
  const locale = lang === 'en' ? 'en-US' : lang === 'fr' ? 'fr-FR' : lang === 'it' ? 'it-IT' : lang === 'sv' ? 'sv-SE' : lang === 'tr' ? 'tr-TR' : lang === 'ru' ? 'ru-RU' : lang === 'uk' ? 'uk-UA' : 'de-DE';
  return new Intl.DateTimeFormat(locale, {year:'numeric', month:'2-digit', day:'2-digit'}).format(d);
}
function productTranslationKey(p?: ProductDto, fallbackId?: number | string) {
  const code = String(p?.code ?? '').trim().replace(/[^A-Za-z0-9_-]/g, '_');
  return `productDescription.${code || p?.id || fallbackId || 'unknown'}`;
}

function productDescriptionForLanguage(lang: GamLanguage | string, p?: ProductDto, fallbackId?: number | string, lineDescription?: string, translations?: Record<string,string>) {
  const target = normalizeGamLanguage(lang);
  const text = (p?.description ?? lineDescription ?? String(fallbackId ?? '')).trim();
  if (!text) return '';
  if (target === 'de') return text;
  const key = productTranslationKey(p, fallbackId);
  const translated = translations?.[key];
  if (isUsableProductTranslation(translated, text)) return translated!.trim();
  const local = localProductDescriptionFallback(text, target);
  if (isUsableProductTranslation(local, text)) return local!.trim();
  // Schritt 36d: In fremdsprachigen Rechnungen darf kein deutscher Produkttext
  // durchrutschen. Bis LibreTranslate geantwortet hat, zeigen wir einen neutralen
  // Hinweis in der Zielsprache statt des deutschen Datenbanktexts.
  return pendingProductDescription(target);
}

function isUsableProductTranslation(value: string | undefined | null, source: string) {
  return !!value && !!value.trim() && value.trim().toLowerCase() !== source.trim().toLowerCase();
}

function localProductDescriptionFallback(source: string, lang: GamLanguage) {
  const s = source.trim().toLowerCase();
  if (s.includes('botox') && s.includes('gezielten entspannung') && s.includes('flächenmuskeln')) {
    const map: Record<string,string> = {
      fr: 'Botox pour la relaxation ciblée des grands muscles de surface - 50 unités (Vistabel)',
      en: 'Botox for targeted relaxation of large superficial muscles - 50 units (Vistabel)',
      it: 'Botox per il rilassamento mirato dei grandi muscoli superficiali - 50 unità (Vistabel)',
      es: 'Botox para la relajación dirigida de grandes músculos superficiales - 50 unidades (Vistabel)',
      pt: 'Botox para o relaxamento direcionado de grandes músculos superficiais - 50 unidades (Vistabel)',
      nl: 'Botox voor gerichte ontspanning van grote oppervlakkige spieren - 50 eenheden (Vistabel)',
      pl: 'Botox do ukierunkowanego rozluźnienia dużych mięśni powierzchownych - 50 jednostek (Vistabel)',
      cs: 'Botox k cílenému uvolnění velkých povrchových svalů - 50 jednotek (Vistabel)',
      sv: 'Botox för riktad avslappning av stora ytliga muskler - 50 enheter (Vistabel)',
      tr: 'Büyük yüzey kaslarının hedefli gevşetilmesi için Botox - 50 ünite (Vistabel)',
      ru: 'Ботокс для целевого расслабления крупных поверхностных мышц - 50 единиц (Vistabel)',
      uk: 'Ботокс для цілеспрямованого розслаблення великих поверхневих м’язів - 50 одиниць (Vistabel)'
    };
    return map[lang];
  }
  return '';
}

function pendingProductDescription(lang: GamLanguage) {
  const map: Record<string,string> = {
    fr: 'Traduction de la description du produit en cours',
    en: 'Product description translation pending',
    it: 'Traduzione della descrizione del prodotto in corso',
    es: 'Traducción de la descripción del producto pendiente',
    pt: 'Tradução da descrição do produto pendente',
    nl: 'Vertaling van de productbeschrijving in behandeling',
    pl: 'Tłumaczenie opisu produktu w toku',
    cs: 'Překlad popisu produktu čeká na zpracování',
    sv: 'Översättning av produktbeskrivning pågår',
    tr: 'Ürün açıklaması çevirisi bekleniyor',
    ru: 'Перевод описания продукта ожидается',
    uk: 'Переклад опису продукту очікується',
    de: 'Produktbeschreibung wird übersetzt'
  };
  return map[lang] || map.en;
}


const HISTORICAL_MODULES = [
  ['Rechnungsprogramm','produktiv nutzbar','Rechnungen, Storno, Gutschrift, Proforma, Zahlungsavis, QR-Portal, PDF/ZUGFeRD und Vorschau'],
  ['Rechnungsadministration','produktiv nutzbar','Gesellschaften, Filialen, Produkte, Preise, MwSt., Textbausteine, Logo-Katalog und Fallbacks'],
  ['Geräteverzeichnis','produktiv nutzbar','Geräte, Details, Bearbeitung, Gesellschafts-/Filialzuordnung und Lagerbezug'],
  ['Lagerverwaltung','produktiv nutzbar','Lagerpositionen, Verbrauchsmaterial, Bestände, Bewegungen und Gerätezuordnung'],
  ['Patientenverwaltung','umgesetzt','Patientenstammdaten als GDS-Modul; interne Patientenakte folgt später mit GAM 2.5'],
  ['Terminverwaltung','umgesetzt','Termine als GDS-Modul mit Login-Zielmodul-Unterstützung und Bearbeitung'],
  ['Aufgabenverwaltung','umgesetzt','Aufgaben, Workflowdaten, Bearbeitung und Vorbereitung für 40h Workflow-Kommunikation'],
  ['Freigabemanagement','umgesetzt','Freigabeübersicht und Workflow-Grundlage für 40e'],
  ['Bestelltool','umgesetzt','Bestellentwürfe, Maildialog, Lieferantenkommunikation und Aufgabenverknüpfung'],
  ['Kommunikation','umgesetzt','Login-News, Meldungen, Änderungsverlauf und Kommunikationszentrale'],
  ['Personaldaten','umgesetzt','Personaldatenmodul mit vollständiger Tabellenanzeige und Bearbeitung'],
  ['Kassenbuch','umgesetzt','Historisches Kassenbuch als GDS-Modul mit Tabellen-/Bearbeitungslogik'],
  ['Arbeitsplatzausstattung','umgesetzt','Arbeitsplatz- und Ausstattungstabellen als GDS-Modul'],
  ['Preisliste','umgesetzt','Bearbeitbare Preisliste mit Suche, Dialogen und Tabellenanzeige'],
  ['Prüfungen','umgesetzt','Kontrolle, Inbetriebnahme und Einweisung korrekt getrennt und zusammengefasst'],
  ['Reports','nutzbar','Alt-GAM-Reportarten mit XLS/XLSX/CSV/DATEV-Vorbereitung'],
  ['Benutzer/Rechte','produktiv nutzbar','Accounts, Rollen, Modulrechte, 2FA, Passkey/WebAuthn und userapplication-Modell']
];
function HistoricalModuleOverview(){return <div className="module-overview"><h3>GAM-Module: aktueller Umsetzungsstand</h3><p className="muted">Stand 39p: Die migrierten GAM2-Module werden nicht mehr als Platzhalter/Lesemodus geführt. Die nächsten Schritte 40a–40o ergänzen darauf aufbauend die Workflow-Schicht.</p><div className="module-grid">{HISTORICAL_MODULES.map(([name,status,desc])=><div className="module-card-mini" key={name}><b>{name}</b><span>{status}</span><small>{desc}</small></div>)}</div></div>}
function ReadOnlyModuleShell({title,description}:{title:string;description:string}){return <section className="card readonly-shell"><h2>{title}</h2><p>{description}</p><p className="note warn">Lesemodus / Modulrahmen: Dieses historische GAM-Modul ist in der Navigation sichtbar. Bearbeiten, Löschen und produktive Schreibaktionen bleiben deaktiviert, bis die Alt-GAM-Fachlogik vollständig rekonstruiert ist.</p><div className="module-grid"><div className="module-card-mini"><b>Status</b><span>Read-only</span><small>Übersicht vorhanden</small></div><div className="module-card-mini"><b>Rechte</b><span>userapplication</span><small>Superadmin-Bypass bleibt erhalten</small></div><div className="module-card-mini"><b>Nächster Schritt</b><span>Fachlogik</span><small>Rekonstruktion aus Alt-GAM-Quellcode</small></div></div></section>}

type OrderLineDraft = { materialId?:number; name?:string; stock?:number; quantity:number; supplierEmail?:string };
type OrderDraft = { id:number; createdAt:string; supplierEmail?:string; status:string; note?:string; subject?:string; mailText?:string; workflowTaskId?:number; orderedAt?:string; lines:OrderLineDraft[] };
const ORDER_DRAFT_STORAGE_KEY = 'gam_order_drafts_v1';
function readOrderDrafts():OrderDraft[]{
  try{ const raw=localStorage.getItem(ORDER_DRAFT_STORAGE_KEY); return raw?JSON.parse(raw):[]; }catch{return [];}
}
function writeOrderDrafts(rows:OrderDraft[]){ localStorage.setItem(ORDER_DRAFT_STORAGE_KEY, JSON.stringify(rows.slice(0,200))); }
function orderDraftSubject(d:OrderDraft){
  const first=(d.lines||[])[0];
  const label=first?.name||first?.materialId?`Material ${first?.name||'#'+first?.materialId}`:'Verbrauchsmaterial';
  return d.subject?.trim() || `Bestellung ${label} - GAM 2.0`;
}
function orderDraftText(d:OrderDraft){
  if(d.mailText?.trim()) return d.mailText;
  const lines=(d.lines||[]).map(l=>`- ${l.name||('Material #'+(l.materialId??''))}: ${l.quantity} Stück`).join('\n');
  return `Sehr geehrte Damen und Herren,\n\nbitte liefern Sie folgende Positionen:\n${lines}\n\nHinweis: ${d.note||''}\n\nVielen Dank.\n\nMit freundlichen Grüßen`;
}
function buildOrderWorkflowTask(d:OrderDraft):WorkflowTask{
  const lines=(d.lines||[]).map(l=>`${l.name||('Material #'+(l.materialId??''))} (${l.quantity} Stück)`).join(', ');
  return {
    username:'GAM Bestelltool',
    department:'Bestellung / Beschaffung',
    task:`Bestellung auslösen: ${lines||'Verbrauchsmaterial'}`,
    responsible:d.supplierEmail||'Beschaffung',
    priority:'mittel',
    status:'offen',
    dueDate:new Date().toISOString().slice(0,10),
    note:`Bestellentwurf #${d.id} wurde ausgelöst. Empfänger: ${d.supplierEmail||'—'}\nBetreff: ${orderDraftSubject(d)}\n\n${orderDraftText(d)}`
  };
}
function openOrderMail(d:OrderDraft){
  const to=(d.supplierEmail||'').trim();
  const subject=encodeURIComponent(orderDraftSubject(d));
  const body=encodeURIComponent(orderDraftText(d));
  const url=`mailto:${encodeURIComponent(to)}?subject=${subject}&body=${body}`;
  window.location.href=url;
}

function CommunicationAssistant(){
 const [settings,setSettings]=useState<CommunicationSettings|null>(null);
 const [smtp,setSmtp]=useState<LocalSmtpSettings>(()=>loadLocalSmtpSettings());
 const [open,setOpen]=useState(false);
 const [testTo,setTestTo]=useState('');
 useEffect(()=>{loadCommunicationSettings().then(cfg=>{
   setSettings(cfg);
   setTestTo(cfg.defaultRecipient || cfg.defaultFrom || cfg.smtpUsername || '');
   setSmtp(prev=>{
     const next={...prev, host:prev.host||cfg.smtpHost||'mail.gmx.net', port:prev.port||cfg.smtpPort||587, username:prev.username||cfg.smtpUsername||'', from:prev.from||cfg.defaultFrom||cfg.smtpUsername||'', fromName:prev.fromName||'GAM 2.0', replyTo:prev.replyTo||''};
     saveLocalSmtpSettings(next);
     return next;
   });
 }).catch(()=>{});},[]);
 function update(patch:Partial<LocalSmtpSettings>){ const next={...smtp,...patch}; setSmtp(next); saveLocalSmtpSettings(next); }
 const normalizedFrom=extractSingleEmailClient(smtp.from||smtp.username);
 const normalizedUser=extractSingleEmailClient(smtp.username);
 const normalizedReplyTo=extractSingleEmailClient(smtp.replyTo);
 const normalizedTestTo=extractSingleEmailClient(testTo || smtp.username);
 const validation=smtpValidationMessages(smtp,testTo);
 const ready=validation.length===0;
 async function sendTest(){
  const currentErrors=smtpValidationMessages(smtp,testTo);
  if(currentErrors.length){ gamNotify('warning','Maildaten sind noch nicht vollständig: '+currentErrors.join(' '),0); return; }
  try{
    await sendOrderEmail({to:normalizedTestTo, subject:'GAM Testmail', text:'Diese Testmail wurde aus dem GAM Kommunikationsassistenten gesendet.', smtp: effectiveSmtpForDirectSend(smtp)});
    gamNotify('success','Testmail wurde gesendet.', undefined, {module:'Kommunikation', action:'SMTP-Testmail', record:normalizedTestTo, fields:[{field:'Server', before:'—', after:`${smtp.host}:${smtp.port}`},{field:'SMTP-Benutzer', before:'—', after:normalizedUser},{field:'Absender', before:'—', after:normalizedFrom},{field:'Antwortadresse', before:'—', after:normalizedReplyTo||'—'}], summary:'Direkter Mailversand wurde mit normalisierten Maildaten getestet.'});
  }catch(e:any){ gamNotify('error', e.message || 'Testmail konnte nicht gesendet werden.', 0); }
 }
 const yamlSnippet=`spring:\n  mail:\n    host: ${smtp.host||'mail.gmx.net'}\n    port: ${smtp.port||587}\n    username: ${normalizedUser||smtp.username||'<gmx-adresse>'}\n    password: <app-passwort>\n    properties:\n      mail.smtp.auth: true\n      mail.smtp.starttls.enable: ${smtp.startTls}\n\ngam:\n  orders:\n    mail:\n      enabled: true\n      from: ${normalizedFrom||normalizedUser||'<gmx-adresse>'}\n      from-name: ${cleanMailHeaderClient(smtp.fromName)||'GAM 2.0'}\n      default-recipient: ${normalizedTestTo||'<empfaenger>'}\n  login:\n    news:\n      enabled: true\n      title: Wichtiger Hinweis\n      text: Bitte aktuelle Hinweise beachten.\n      severity: info\n`;
 return <section className="card communication-assistant"><div className="row"><div><h2>Kommunikation</h2><p className="muted">Schritt 38g11: verbesserter Maildialog mit Live-Prüfung, normalisierten Adressen, Anzeigename, Reply-To und Versandvorschau.</p></div><button className="secondary" type="button" onClick={()=>setOpen(!open)}>{open?'Ausblenden':'Einrichten'}</button></div>{settings&&<p className="note">Backend-Konfiguration: {settings.mailEnabled?'aktiv':'nicht aktiv'} · Server {settings.smtpHost||'mail.gmx.net'}:{settings.smtpPort||587} · Passwort {settings.smtpPasswordConfigured?'gesetzt':'nicht gesetzt'}</p>}{open&&<div className="newgrid"><label>SMTP-Server<input value={smtp.host} onChange={e=>update({host:e.target.value})}/></label><label>Port<input type="number" value={smtp.port} onChange={e=>update({port:Number(e.target.value)||587})}/></label><label>Benutzer / E-Mail<input value={smtp.username} onChange={e=>update({username:e.target.value, from:smtp.from||e.target.value})}/><small className="muted">verwendet: {normalizedUser||'—'}</small></label><label>Passwort / App-Passwort<input type="password" value={smtp.password} onChange={e=>update({password:e.target.value})}/></label><label>Absender-E-Mail<input value={smtp.from} onChange={e=>update({from:e.target.value})}/><small className="muted">verwendet: {normalizedFrom||'—'}</small></label><label>Anzeigename<input value={smtp.fromName} onChange={e=>update({fromName:e.target.value})}/><small className="muted">z. B. GAM 2.0</small></label><label>Antwortadresse / Reply-To<input value={smtp.replyTo} onChange={e=>update({replyTo:e.target.value})}/><small className="muted">optional: {normalizedReplyTo||'—'}</small></label><label>Test-Empfänger<input value={testTo} onChange={e=>setTestTo(e.target.value)}/><small className="muted">verwendet: {normalizedTestTo||'—'}</small></label><label className="toggle-field"><input type="checkbox" checked={smtp.startTls} onChange={e=>update({startTls:e.target.checked})}/> STARTTLS</label><label className="toggle-field"><input type="checkbox" checked={smtp.ssl} onChange={e=>update({ssl:e.target.checked})}/> SSL</label><div className="wide-field note"><b>Versandprüfung</b><br/>{ready?<span>✓ Maildaten sind syntaktisch vollständig. Der Versand kann getestet werden.</span>:<ul>{validation.map((v,i)=><li key={i}>{v}</li>)}</ul>}</div><button type="button" disabled={!ready} onClick={sendTest}>Testmail senden</button><details className="wide-field"><summary>application-local.yml Vorschlag anzeigen</summary><pre>{yamlSnippet}</pre></details></div>}</section>
}

function mailDialogValidationForOrder(d: OrderDraft, mode: 'mailto'|'direct') {
  const errors: string[] = [];
  const toError=emailValidationMessage(d.supplierEmail, 'Empfänger/Lieferanten-E-Mail'); if(toError) errors.push(toError);
  if(!cleanMailHeaderClient(d.subject || orderDraftSubject(d))) errors.push('Betreff fehlt.');
  if(!String(d.mailText || orderDraftText(d) || '').trim()) errors.push('Mailtext fehlt.');
  if(mode==='direct') errors.push(...smtpValidationMessages(loadLocalSmtpSettings(), d.supplierEmail).filter(e=>!e.startsWith('Test-Empfänger')));
  return errors;
}
function MailPreviewPanel({draft,mode}:{draft:OrderDraft; mode:'mailto'|'direct'}){
 const smtp=loadLocalSmtpSettings();
 const to=extractSingleEmailClient(draft.supplierEmail);
 const from=extractSingleEmailClient(smtp.from||smtp.username);
 const errors=mailDialogValidationForOrder(draft,mode);
 return <div className="wide-field note"><b>Mailprüfung</b><br/><span>Versandart: {mode==='direct'?'Direkt per GAM':'Mailclient öffnen'}</span><br/><span>Empfänger: {to||'—'}</span><br/>{mode==='direct'?<><span>Absender: {from||'—'}</span><br/><span>Anzeigename: {cleanMailHeaderClient(smtp.fromName)||'—'}</span><br/><span>Reply-To: {extractSingleEmailClient(smtp.replyTo)||'—'}</span><br/></>:null}<span>Betreff: {cleanMailHeaderClient(draft.subject||orderDraftSubject(draft))||'—'}</span>{errors.length?<ul>{errors.map((e,i)=><li key={i}>{e}</li>)}</ul>:<p>✓ Maildaten sehen gut aus.</p>}</div>;
}

function OrdersPage(){
 const [materials,setMaterials]=useState<WarehouseItem[]>([]);
 const [drafts,setDrafts]=useState<OrderDraft[]>(()=>readOrderDrafts());
 const [q,setQ]=useState('');
 const [onlyNeed,setOnlyNeed]=useState(true);
 const [threshold,setThreshold]=useState(5);
 const [target,setTarget]=useState(20);
 const [open,setOpen]=useState(false);
 const [sendMode,setSendMode]=useState<'mailto'|'direct'>('mailto');
 const [form,setForm]=useState<OrderDraft>({id:0,createdAt:'',supplierEmail:'',status:'Entwurf',note:'',lines:[{quantity:1}]});
 const firstRef=useRef<HTMLInputElement|null>(null);
 async function reload(){
  try{ const rows=await loadWarehouseItems(q,'verbrauchsmaterial',false,300); setMaterials(rows); }
  catch(e:any){ notifySaveError(e,'Bestelltool konnte Materialdaten nicht laden.'); }
 }
 useEffect(()=>{reload()},[]);
 const needRows=materials.filter(m=>!onlyNeed || Number(m.quantity??0)<=threshold);
 const needCount=materials.filter(m=>Number(m.quantity??0)<=threshold).length;
 const totalStock=materials.reduce((a,m)=>a+Number(m.quantity??0),0);
 function suggestedQty(m:WarehouseItem){ return Math.max(1, target-Number(m.quantity??0)); }
 function startNew(){ const base={id:0,createdAt:new Date().toISOString().slice(0,10),supplierEmail:'',status:'Entwurf',note:'',subject:'',mailText:'',lines:[{quantity:1}]}; setForm(base); setOpen(true); }
 function startFromMaterial(m:WarehouseItem){ const base:OrderDraft={id:0,createdAt:new Date().toISOString().slice(0,10),supplierEmail:m.manufacturerEmail||'',status:'Entwurf',note:'Aus Bestellvorschlag erzeugt.',subject:'',mailText:'',lines:[{materialId:m.id,name:m.name||m.description,stock:m.quantity??0,quantity:suggestedQty(m),supplierEmail:m.manufacturerEmail||''}]}; setForm({...base, subject:orderDraftSubject(base), mailText:orderDraftText(base)}); setOpen(true); }
 function editDraft(d:OrderDraft){ setForm({...d, lines:(d.lines&&d.lines.length?d.lines:[{quantity:1}] )}); setOpen(true); }
 function updateLine(idx:number, patch:Partial<OrderLineDraft>){ setForm(prev=>({...prev, lines:prev.lines.map((l,i)=>i===idx?{...l,...patch}:l)})); }
 function addLine(){ setForm(prev=>({...prev, lines:[...prev.lines,{quantity:1}]})); }
 function removeLine(idx:number){ setForm(prev=>({...prev, lines:prev.lines.filter((_,i)=>i!==idx)})); }
 function normalizeDraft(d:OrderDraft):OrderDraft{ return {...d, id:d.id||Date.now(), createdAt:d.createdAt||new Date().toISOString().slice(0,10), subject:(d.subject||orderDraftSubject(d)), mailText:(d.mailText||orderDraftText(d)), lines:(d.lines||[]).filter(l=>(l.name||l.materialId||l.quantity))}; }
 function upsertDraft(d:OrderDraft){ const cleaned=normalizeDraft(d); const next=[cleaned, ...drafts.filter(row=>row.id!==cleaned.id)].sort((a,b)=>b.id-a.id); setDrafts(next); writeOrderDrafts(next); return cleaned; }
 function save(){
  const cleaned=upsertDraft(form); const before=drafts.find(d=>d.id===cleaned.id); notifySaved(cleaned.status==='bestellt'?'Bestellung gespeichert.':'Bestellentwurf gespeichert.', buildChangeDetails('Bestelltool', before?'Bestellentwurf gespeichert':'Bestellentwurf angelegt', before, cleaned, {createdAt:'Datum',supplierEmail:'Lieferant / E-Mail',status:'Status',subject:'Betreff',mailText:'Mailtext',note:'Bemerkung',workflowTaskId:'Workflow-Aufgabe',orderedAt:'Ausgelöst am',lines:'Positionen'}, '#'+cleaned.id)); setOpen(false);
 }
 async function sendOrderMailDirect(d:OrderDraft){
  return await sendOrderEmail({
    to:d.supplierEmail||'',
    subject:d.subject||orderDraftSubject(d),
    text:d.mailText||orderDraftText(d),
    draftId:d.id,
    workflowTaskId:d.workflowTaskId,
    lines:(d.lines||[]).map(l=>({materialId:l.materialId,name:l.name,stock:l.stock,quantity:l.quantity})),
    smtp: effectiveSmtpForDirectSend()
  });
 }
 async function triggerOrder(d?:OrderDraft, mode: 'mailto'|'direct'=sendMode){
  const source=normalizeDraft(d||form);
  const mailErrors=mailDialogValidationForOrder(source, mode);
  if(mailErrors.length){ gamNotify('error','Bestellung kann noch nicht versendet werden: '+mailErrors.join(' '),0); return; }
  try{
    const task=await createWorkflowTask(buildOrderWorkflowTask(source));
    const triggered={...source,status:'bestellt',workflowTaskId:task.id,orderedAt:new Date().toISOString()};
    const saved=upsertDraft(triggered);
    if(mode==='direct'){
      const result=await sendOrderMailDirect(saved);
      notifySaved('Bestellung ausgelöst: Workflow gestartet und E-Mail direkt versendet.', buildChangeDetails('Bestelltool', 'Bestellung direkt versendet', source, {...saved, directMailStatus:result.message}, {status:'Status',supplierEmail:'E-Mail',subject:'Betreff',mailText:'Mailtext',workflowTaskId:'Workflow-Aufgabe',orderedAt:'Ausgelöst am',lines:'Positionen',directMailStatus:'Mailversand'}, '#'+saved.id));
    } else {
      notifySaved('Bestellung ausgelöst: Workflow gestartet und E-Mail vorbereitet.', buildChangeDetails('Bestelltool', 'Bestellung ausgelöst', source, saved, {status:'Status',supplierEmail:'E-Mail',subject:'Betreff',mailText:'Mailtext',workflowTaskId:'Workflow-Aufgabe',orderedAt:'Ausgelöst am',lines:'Positionen'}, '#'+saved.id));
      openOrderMail(saved);
    }
    setOpen(false);
  }catch(e:any){
    notifySaveError(e, mode==='direct' ? 'Direkter Mailversand konnte nicht ausgeführt werden. SMTP prüfen oder externe Mailoption verwenden.' : 'Bestellung konnte nicht ausgelöst werden.');
  }
 }
 function removeDraft(){ if(!form.id) return; const next=drafts.filter(d=>d.id!==form.id); setDrafts(next); writeOrderDrafts(next); notifySaved('Bestellentwurf gelöscht.'); setOpen(false); }
 async function copyDraft(d:OrderDraft){ const text=orderDraftText(d); try{ await navigator.clipboard?.writeText(text); gamNotify('success','Bestelltext wurde in die Zwischenablage kopiert.'); }catch{ gamNotify('info',text,0); } }
 const draftOpen=drafts.filter(d=>d.status!=='erledigt'&&d.status!=='storniert').length;
 return <section className="grid"><section className="card"><h2>Bestelltool</h2><p className="muted">Schritt 38g: Bestellvorschläge aus Lager/Verbrauchsmaterial, Bestellentwürfe, bearbeitbarer Betreff/Mailtext und Versand per Mailclient oder direktem GAM-SMTP-Versand. Die Entwürfe werden zunächst lokal im Browser gespeichert; Lagerbestände selbst bleiben unverändert.</p><CommunicationAssistant/><div className="stats"><span>Materialpositionen<br/><b>{materials.length}</b></span><span>Bedarf ≤ {threshold}<br/><b>{needCount}</b></span><span>Offene Entwürfe<br/><b>{draftOpen}</b></span><span>Gesamtbestand<br/><b>{totalStock}</b></span></div><GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neuer Bestellentwurf</button><label><Search size={16}/><input placeholder="Material, Eigenschaften oder Hersteller-E-Mail suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload()}}/></label><label>Meldebestand<input type="number" value={threshold} onChange={e=>setThreshold(Number(e.target.value)||0)}/></label><label>Zielbestand<input type="number" value={target} onChange={e=>setTarget(Number(e.target.value)||1)}/></label><label className="toggle-field"><input type="checkbox" checked={onlyNeed} onChange={e=>setOnlyNeed(e.target.checked)}/> nur Bedarf</label><button className="secondary" type="button" onClick={reload}>Suchen</button></GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr><th>Material</th><th>Eigenschaften</th><th>Bestand</th><th>Vorschlag</th><th>Hersteller / Lieferant</th><th>Aktion</th></tr></thead><tbody>{needRows.map(m=><tr key={m.id} className="clickable-row" title="Anklicken, um daraus einen Bestellentwurf zu erstellen" onClick={()=>startFromMaterial(m)}><td><b>{m.name||m.description||'—'}</b><br/><small>#{m.id}</small></td><td>{m.properties||'—'}</td><td>{m.quantity??0}</td><td>{suggestedQty(m)}</td><td>{m.manufacturerEmail||'—'}</td><td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startFromMaterial(m);}}>Bestellen</button></td></tr>)}</tbody></table></GamScrollArea></section><section className="card"><h2>Bestellentwürfe</h2><p className="muted">Diese Liste dient als schneller Test- und Arbeitsbereich für Beschaffung. Später kann daraus eine echte DB-Tabelle mit Versand-/Freigabelogik werden.</p><GamScrollArea tall><table className="compact-table"><thead><tr><th>Datum</th><th>Status</th><th>Lieferant</th><th>Positionen</th><th>Hinweis</th><th>Aktionen</th></tr></thead><tbody>{drafts.map(d=><tr key={d.id} className="clickable-row" title="Anklicken zum Bearbeiten" onClick={()=>editDraft(d)}><td>{d.createdAt}</td><td><span className="badge">{d.status}</span></td><td>{d.supplierEmail||'—'}</td><td>{(d.lines||[]).map(l=>`${l.name||l.materialId}: ${l.quantity}`).join(', ')}</td><td>{d.note||'—'}</td><td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); editDraft(d);}}>Bearbeiten</button> <button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); copyDraft(d);}}>Text</button> <button type="button" onClick={(e)=>{e.stopPropagation(); triggerOrder(d,'mailto');}}>Mailclient</button> <button type="button" onClick={(e)=>{e.stopPropagation(); triggerOrder(d,'direct');}}>Direkt senden</button></td></tr>)}</tbody></table></GamScrollArea></section><GamDialog open={open} title={form.id?`Bestellentwurf #${form.id} bearbeiten`:'Neuer Bestellentwurf'} onClose={()=>setOpen(false)} size="wide" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{form.id?'Änderungen speichern':'Entwurf speichern'}</button><button type="button" onClick={()=>triggerOrder(form,sendMode)}>Bestellung auslösen</button>{form.id? <button className="danger" type="button" onClick={removeDraft}>Löschen</button>: null}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid"><label>Datum<input ref={firstRef} type="date" value={form.createdAt||''} onChange={e=>setForm({...form,createdAt:e.target.value})}/></label><label>Lieferant / E-Mail<input type="email" value={form.supplierEmail||''} onChange={e=>setForm({...form,supplierEmail:e.target.value})}/></label><label>Versandoption<select value={sendMode} onChange={e=>setSendMode(e.target.value as 'mailto'|'direct')}><option value="mailto">Mailclient öffnen</option><option value="direct">Direkt per GAM senden</option></select></label><label className="wide-field">Betreff<input value={form.subject||orderDraftSubject(form)} onChange={e=>setForm({...form,subject:e.target.value})}/></label><label>Status<select value={form.status||'Entwurf'} onChange={e=>setForm({...form,status:e.target.value})}><option value="Entwurf">Entwurf</option><option value="bestellt">bestellt</option><option value="geliefert">geliefert</option><option value="erledigt">erledigt</option><option value="storniert">storniert</option></select></label><label className="wide-field">Bemerkung<textarea value={form.note||''} onChange={e=>setForm({...form,note:e.target.value})}/></label><label className="wide-field">Mailtext<textarea value={form.mailText||orderDraftText(form)} onChange={e=>setForm({...form,mailText:e.target.value})}/></label><MailPreviewPanel draft={form} mode={sendMode}/></div><h3>Positionen</h3>{form.lines.map((l,idx)=><div className="newgrid" key={idx}><label>Material-ID<input type="number" value={l.materialId??''} onChange={e=>updateLine(idx,{materialId:e.target.value?Number(e.target.value):undefined})}/></label><label>Material / Beschreibung<input value={l.name||''} onChange={e=>updateLine(idx,{name:e.target.value})}/></label><label>Bestand<input type="number" value={l.stock??''} onChange={e=>updateLine(idx,{stock:e.target.value?Number(e.target.value):undefined})}/></label><label>Menge<input type="number" value={l.quantity} onChange={e=>updateLine(idx,{quantity:Number(e.target.value)||1})}/></label><label>Lieferant/E-Mail<input value={l.supplierEmail||''} onChange={e=>updateLine(idx,{supplierEmail:e.target.value})}/></label><button className="secondary" type="button" onClick={()=>removeLine(idx)}>Position entfernen</button></div>)}<div className="download-actions"><button className="secondary" type="button" onClick={addLine}>+ Position</button><button className="secondary" type="button" onClick={()=>copyDraft(form)}>Bestelltext kopieren</button></div></GamDialog></section>
}
function ModuleTiles(){const [mods,setMods]=useState<any[]>([]); useEffect(()=>{loadGamModules().then(setMods).catch(()=>{})},[]); if(!mods.length) return null; return <div className="stats">{mods.map(m=><span key={m.key}><b>{m.count>=0?m.count:'—'}</b><br/>{m.label}<br/><small>{m.tableName}</small></span>)}</div>}

function WorkflowTasksPage(){
 const emptyTask:WorkflowTask={priority:'mittel',status:'offen',task:'',department:'',responsible:'',dueDate:'',branchCode:'',note:''};
 const [rows,setRows]=useState<WorkflowTask[]>([]);
 const [stats,setStats]=useState<WorkflowStats|null>(null);
 const [q,setQ]=useState('');
 const [status,setStatus]=useState('all');
 const [branchId,setBranchId]=useState<number|undefined>();
 const [form,setForm]=useState<WorkflowTask>(emptyTask);
 const [open,setOpen]=useState(false);
 const [err,setErr]=useState('');
 const firstRef=useRef<HTMLInputElement|null>(null);
 async function reload(){ setErr(''); try{ const [r,s]=await Promise.all([loadWorkflowTasks(q,status,branchId,250),loadWorkflowStats()]); setRows(r); setStats(s); }catch(e:any){ const m=e.message??'Aufgaben konnten nicht geladen werden'; setErr(m); gamNotify('error',m,0); }}
 useEffect(()=>{reload()},[]);
 function startNew(){ setForm({...emptyTask}); setOpen(true); }
 function startEdit(row:WorkflowTask){ setForm({...emptyTask,...row}); setOpen(true); }
 function payload():any{ return {username:form.username||undefined, branchCode:form.branchCode||undefined, branchId:form.branchId?Number(form.branchId):undefined, department:form.department||undefined, task:form.task||'', responsible:form.responsible||undefined, priority:form.priority||'mittel', status:form.status||'offen', dueDate:form.dueDate||undefined, doneBy:form.doneBy||undefined, note:form.note||undefined}; }
 async function save(){ setErr(''); try{ const before=form.id?rows.find(r=>r.id===form.id):undefined; const after=payload(); if(form.id) await updateWorkflowTask(form.id,after); else await createWorkflowTask(after); notifySaved(form.id?'Aufgabe erfolgreich gespeichert.':'Aufgabe erfolgreich angelegt.', buildChangeDetails('Aufgaben', form.id?'Aufgabe gespeichert':'Aufgabe angelegt', before, {...after,id:form.id}, {task:'Aufgabe',department:'Fachbereich',responsible:'Verantwortlich',priority:'Priorität',status:'Status',dueDate:'Frist',branchCode:'Filialkürzel',branchId:'Filiale-ID',doneBy:'Erledigt von',note:'Bemerkung'}, form.id?'#'+form.id:after.task)); setOpen(false); await reload(); }catch(e:any){ notifySaveError(e,'Aufgabe konnte nicht gespeichert werden.'); }}
 async function remove(){ if(!form.id) return; if(!confirm('Diese Aufgabe wirklich löschen?')) return; try{ await deleteWorkflowTask(form.id); gamNotify('success','Aufgabe gelöscht.'); setOpen(false); await reload(); }catch(e:any){ gamNotify('error',e.message??'Aufgabe konnte nicht gelöscht werden.',0); }}
 const statusLabel=(v?:string)=>v||'offen';
 return <section className="card"><h2>{moduleText(currentUiLanguage(),'tasks')}</h2><p className="muted">Schritt 38f: Aufgabenverwaltung als erstes vollständiges Workflow-Modul nach GDS 1.0. Suche, Filter, Dialoge, Toasts und Listen bleiben einheitlich bedienbar.</p>{stats&&<div className="stats"><span>Offene Aufgaben<br/><b>{stats.tasksOpen}</b></span><span>Erledigte Aufgaben<br/><b>{stats.tasksDone}</b></span><span>Offene Freigaben<br/><b>{stats.approvalsOpen}</b></span><span>Erledigte Freigaben<br/><b>{stats.approvalsDone}</b></span></div>}<GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neue Aufgabe</button><label><Search size={16}/><input placeholder="Aufgabe, Bemerkung, Benutzer, Verantwortlicher suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload()}}/></label><select value={status} onChange={e=>setStatus(e.target.value)}><option value="all">Alle Status</option><option value="offen">offen</option><option value="in Arbeit">in Arbeit</option><option value="erledigt">erledigt</option><option value="abgeschlossen">abgeschlossen</option></select><label>Filiale-ID<input type="number" value={branchId??''} onChange={e=>setBranchId(e.target.value?Number(e.target.value):undefined)}/></label><button className="secondary" type="button" onClick={reload}>Suchen</button></GamStickyToolbar>{err&&<p className="note warn">{err}</p>}<GamScrollArea tall><table className="compact-table"><thead><tr><th>ID</th><th>Aufgabe</th><th>Fachbereich</th><th>Verantwortlich</th><th>Priorität</th><th>Status</th><th>Frist</th><th>Aktion</th></tr></thead><tbody>{rows.map(r=><tr key={r.id} className="clickable-row" title="Anklicken zum Bearbeiten" onClick={()=>startEdit(r)}><td>{r.id}</td><td><b>{r.task||'—'}</b><br/><small>{r.note||r.username||'—'}</small></td><td>{r.department||'—'}</td><td>{r.responsible||'—'}</td><td><span className="badge">{r.priority||'—'}</span></td><td>{statusLabel(r.status)}</td><td>{r.dueDate||'—'}</td><td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>)}</tbody></table></GamScrollArea><GamDialog open={open} title={form.id?`Aufgabe #${form.id} bearbeiten`:'Neue Aufgabe'} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{form.id?'Änderungen speichern':'Aufgabe anlegen'}</button>{form.id&&<button className="danger" type="button" onClick={remove}>Benutzer löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid"><label>Aufgabe<input ref={firstRef} value={form.task??''} onChange={e=>setForm({...form,task:e.target.value})}/></label><label>Fachbereich<input value={form.department??''} onChange={e=>setForm({...form,department:e.target.value})}/></label><label>Verantwortlicher<input value={form.responsible??''} onChange={e=>setForm({...form,responsible:e.target.value})}/></label><label>Priorität<select value={form.priority??'mittel'} onChange={e=>setForm({...form,priority:e.target.value})}><option value="niedrig">niedrig</option><option value="mittel">mittel</option><option value="hoch">hoch</option><option value="kritisch">kritisch</option></select></label><label>Status<select value={form.status??'offen'} onChange={e=>setForm({...form,status:e.target.value})}><option value="offen">offen</option><option value="in Arbeit">in Arbeit</option><option value="erledigt">erledigt</option><option value="abgeschlossen">abgeschlossen</option></select></label><label>Frist<input type="date" value={form.dueDate??''} onChange={e=>setForm({...form,dueDate:e.target.value})}/></label><label>Filialkürzel<input value={form.branchCode??''} onChange={e=>setForm({...form,branchCode:e.target.value})}/></label><label>Filiale-ID<input type="number" value={form.branchId??''} onChange={e=>setForm({...form,branchId:e.target.value?Number(e.target.value):undefined})}/></label><label>Erledigt von<input value={form.doneBy??''} onChange={e=>setForm({...form,doneBy:e.target.value})}/></label><label className="wide-field">Bemerkung<textarea value={form.note??''} onChange={e=>setForm({...form,note:e.target.value})}/></label></div></GamDialog></section>
}

function WorkflowApprovalsPage(){
 const emptyApproval:WorkflowApproval={status:'offen',description:'',creator:'',companyId:undefined,branchId:undefined,note:''};
 const [rows,setRows]=useState<WorkflowApproval[]>([]);
 const [stats,setStats]=useState<WorkflowStats|null>(null);
 const [q,setQ]=useState('');
 const [status,setStatus]=useState('all');
 const [branchId,setBranchId]=useState<number|undefined>();
 const [form,setForm]=useState<WorkflowApproval>(emptyApproval);
 const [open,setOpen]=useState(false);
 const [err,setErr]=useState('');
 const firstRef=useRef<HTMLInputElement|null>(null);
 async function reload(){ setErr(''); try{ const [r,s]=await Promise.all([loadWorkflowApprovals(q,status,branchId,250),loadWorkflowStats()]); setRows(r); setStats(s); }catch(e:any){ const m=e.message??'Freigaben konnten nicht geladen werden'; setErr(m); gamNotify('error',m,0); }}
 useEffect(()=>{reload()},[]);
 function startNew(){ setForm({...emptyApproval}); setOpen(true); }
 function startEdit(row:WorkflowApproval){ setForm({...emptyApproval,...row}); setOpen(true); }
 function payload():any{ return {creator:form.creator||undefined, description:form.description||'', companyId:form.companyId?Number(form.companyId):undefined, branchId:form.branchId?Number(form.branchId):undefined, status:form.status||'offen', note:form.note||undefined}; }
 async function save(){ try{ const before=form.id?rows.find(r=>r.id===form.id):undefined; const after=payload(); if(form.id) await updateWorkflowApproval(form.id,after); else await createWorkflowApproval(after); notifySaved(form.id?'Freigabe erfolgreich gespeichert.':'Freigabe erfolgreich angelegt.', buildChangeDetails('Freigaben', form.id?'Freigabe gespeichert':'Freigabe angelegt', before, {...after,id:form.id}, {description:'Beschreibung',creator:'Eintragender',companyId:'Gesellschaft-ID',branchId:'Filiale-ID',status:'Status',note:'Bemerkung'}, form.id?'#'+form.id:after.description)); setOpen(false); await reload(); }catch(e:any){ notifySaveError(e,'Freigabe konnte nicht gespeichert werden.'); }}
 async function remove(){ if(!form.id) return; if(!confirm('Diese Freigabe wirklich löschen?')) return; try{ await deleteWorkflowApproval(form.id); gamNotify('success','Freigabe gelöscht.'); setOpen(false); await reload(); }catch(e:any){ gamNotify('error',e.message??'Freigabe konnte nicht gelöscht werden.',0); }}
 return <section className="card"><h2>{moduleText(currentUiLanguage(),'approval')}</h2><p className="muted">Schritt 38f: Freigabemanagement mit GDS-Dialog, Statusfilter, Toast-Meldungen und dauerhaft sichtbarer Aktionsleiste.</p>{stats&&<div className="stats"><span>Offene Aufgaben<br/><b>{stats.tasksOpen}</b></span><span>Erledigte Aufgaben<br/><b>{stats.tasksDone}</b></span><span>Offene Freigaben<br/><b>{stats.approvalsOpen}</b></span><span>Erledigte Freigaben<br/><b>{stats.approvalsDone}</b></span></div>}<GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neue Freigabe</button><label><Search size={16}/><input placeholder="Beschreibung, Bemerkung oder Eintragenden suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload()}}/></label><select value={status} onChange={e=>setStatus(e.target.value)}><option value="all">Alle Status</option><option value="offen">offen</option><option value="in Prüfung">in Prüfung</option><option value="freigegeben">freigegeben</option><option value="abgelehnt">abgelehnt</option><option value="abgeschlossen">abgeschlossen</option></select><label>Filiale-ID<input type="number" value={branchId??''} onChange={e=>setBranchId(e.target.value?Number(e.target.value):undefined)}/></label><button className="secondary" type="button" onClick={reload}>Suchen</button></GamStickyToolbar>{err&&<p className="note warn">{err}</p>}<GamScrollArea tall><table className="compact-table"><thead><tr><th>ID</th><th>Datum</th><th>Beschreibung</th><th>Eintragender</th><th>Gesellschaft</th><th>Filiale</th><th>Status</th><th>Aktion</th></tr></thead><tbody>{rows.map(r=><tr key={r.id} className="clickable-row" title="Anklicken zum Bearbeiten" onClick={()=>startEdit(r)}><td>{r.id}</td><td>{r.date||'—'}</td><td><b>{r.description||'—'}</b><br/><small>{r.note||'—'}</small></td><td>{r.creator||'—'}</td><td>{r.companyId??'—'}</td><td>{r.branchId??'—'}</td><td><span className="badge">{r.status||'offen'}</span></td><td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>)}</tbody></table></GamScrollArea><GamDialog open={open} title={form.id?`Freigabe #${form.id} bearbeiten`:'Neue Freigabe'} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{form.id?'Änderungen speichern':'Freigabe anlegen'}</button>{form.id&&<button className="danger" type="button" onClick={remove}>Benutzer löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid"><label className="wide-field">Beschreibung<input ref={firstRef} value={form.description??''} onChange={e=>setForm({...form,description:e.target.value})}/></label><label>Eintragender<input value={form.creator??''} onChange={e=>setForm({...form,creator:e.target.value})}/></label><label>Gesellschaft-ID<input type="number" value={form.companyId??''} onChange={e=>setForm({...form,companyId:e.target.value?Number(e.target.value):undefined})}/></label><label>Filiale-ID<input type="number" value={form.branchId??''} onChange={e=>setForm({...form,branchId:e.target.value?Number(e.target.value):undefined})}/></label><label>Status<select value={form.status??'offen'} onChange={e=>setForm({...form,status:e.target.value})}><option value="offen">offen</option><option value="in Prüfung">in Prüfung</option><option value="freigegeben">freigegeben</option><option value="abgelehnt">abgelehnt</option><option value="abgeschlossen">abgeschlossen</option></select></label><label className="wide-field">Bemerkung<textarea value={form.note??''} onChange={e=>setForm({...form,note:e.target.value})}/></label></div></GamDialog></section>
}


type PersonnelEntry = Record<string, unknown>;

const PERSONNEL_CATALOG_KEY = 'personnel';
const PERSONNEL_PRIMARY_KEY = 'ID';

// Schritt 38l4:
// Die echte Tabelle `personal` wird vollständig ausgewertet.
// HR sieht ID bis BEMERKUNG_PERSONAL. Alle Spalten danach sind IT-/Superadmin-Spalten.
// Die vorhandene Spalte BEMERKUNG bleibt die IT-Bemerkung.
const PERSONNEL_HR_NOTE_COLUMN = 'BEMERKUNG_PERSONAL';
const PERSONNEL_IT_NOTE_COLUMN = 'BEMERKUNG';
const personnelHrColumns = ['ID','NAME','VORNAME','STATUS','POSITION','FILIALE_ID','EMAIL','TELEFON','TELEFON2',PERSONNEL_HR_NOTE_COLUMN];
const personnelSuperadminOnlyColumns = [
  'EMAIL_PW_EXTERN','EMAIL_PW_INTERN','RECHNER_IP','OFFICE_LIZENZ',
  'PLONE_BENUTZER','PLONE_PW','MICROSOFT_KONTO','MICROSOFT_PW',
  'DIENSTHANDY','VPN_TOKEN','VPN_PIN','QNAP_BENUTZER','QNAP_PW',
  'DIENSTLAPTOP',PERSONNEL_IT_NOTE_COLUMN
];
const personnelBooleanColumns = ['DIENSTHANDY','VPN_TOKEN','DIENSTLAPTOP'];
const personnelColumnLabels: Record<string,string> = {
  ID:'ID', NAME:'Name', VORNAME:'Vorname', STATUS:'Status', POSITION:'Position', FILIALE_ID:'Filiale', EMAIL:'E-Mail',
  TELEFON:'Telefon', TELEFON2:'Telefon 2', BEMERKUNG_PERSONAL:'Bemerkung Personal',
  EMAIL_PW_EXTERN:'E-Mail-PW extern', EMAIL_PW_INTERN:'E-Mail-PW intern', RECHNER_IP:'Rechner-IP', OFFICE_LIZENZ:'Office-Lizenz',
  PLONE_BENUTZER:'Plone-Benutzer', PLONE_PW:'Plone-PW', MICROSOFT_KONTO:'Microsoft-Konto', MICROSOFT_PW:'Microsoft-PW',
  DIENSTHANDY:'Diensthandy', VPN_TOKEN:'VPN-Token', VPN_PIN:'VPN-PIN', QNAP_BENUTZER:'QNAP-Benutzer', QNAP_PW:'QNAP-PW',
  DIENSTLAPTOP:'Dienstlaptop', BEMERKUNG:'IT-Bemerkung'
};
function isSuperAdminAccount(account?: AccountDto|null){ return String(account?.role||'').toLowerCase()==='superadmin' || String(account?.username||'').toLowerCase()==='admin'; }
function personnelCanonicalFields(catalog:MasterDataCatalog|null){
  const available = catalog?.fields?.length ? catalog.fields : [...personnelHrColumns.filter(f=>f!==PERSONNEL_PRIMARY_KEY), ...personnelSuperadminOnlyColumns];
  const ordered = [...personnelHrColumns, ...personnelSuperadminOnlyColumns].filter(f=>f===PERSONNEL_PRIMARY_KEY || available.includes(f));
  return [...ordered, ...available.filter(f=>!ordered.includes(f))];
}
function personnelVisibleFields(catalog:MasterDataCatalog|null, superadmin:boolean){
  const canonical = personnelCanonicalFields(catalog);
  return superadmin ? canonical : canonical.filter(f=>!personnelSuperadminOnlyColumns.includes(f));
}
function personnelTableColumns(fields:string[], superadmin:boolean){
  // Schritt 38l5:
  // Die Tabellenansicht folgt derselben Rollensicht wie der Bearbeiten-Dialog.
  // Superadmin sieht hier bewusst alle verfügbaren personal-Spalten, nicht nur eine Kurzliste.
  if(superadmin) return fields;
  const preferred = ['ID','NAME','VORNAME','STATUS','POSITION','FILIALE_ID','EMAIL','TELEFON','TELEFON2',PERSONNEL_HR_NOTE_COLUMN];
  return preferred.filter(c=>c===PERSONNEL_PRIMARY_KEY || fields.includes(c));
}
function rowValue(row:Record<string,unknown>, key:string){
  const direct = row[key];
  if(direct!==undefined) return direct;
  const found = Object.keys(row).find(k=>k.toLowerCase()===key.toLowerCase());
  return found ? row[found] : '';
}
function personnelLabel(row:Record<string,unknown>){
  const first = String(rowValue(row,'VORNAME')??'').trim();
  const last = String(rowValue(row,'NAME')??'').trim();
  const id = String(rowValue(row,PERSONNEL_PRIMARY_KEY)??'').trim();
  return [first,last].filter(Boolean).join(' ') || (id ? `Personal #${id}` : 'Neue Personaldaten');
}
function emptyPersonnel(fields:string[]):PersonnelEntry{
  const base:PersonnelEntry = {};
  fields.forEach(f=>base[f]='');
  return base;
}
function PersonnelPage({account}:{account?:AccountDto|null}){
 const superadmin=isSuperAdminAccount(account);
 const [catalog,setCatalog]=useState<MasterDataCatalog|null>(null);
 const [rows,setRows]=useState<Record<string,unknown>[]>([]);
 const [q,setQ]=useState('');
 const [form,setForm]=useState<PersonnelEntry>({});
 const [open,setOpen]=useState(false);
 const [err,setErr]=useState('');
 const firstRef=useRef<HTMLInputElement|null>(null);
 const fields=personnelVisibleFields(catalog, superadmin);
 const tableColumns=personnelTableColumns(fields, superadmin);
 async function reload(search=q){
   setErr('');
   try{
     const data=await loadMasterDataRows(PERSONNEL_CATALOG_KEY, search, 250);
     setCatalog(data.catalog);
     setRows(data.rows||[]);
   }catch(e:any){ const m=e.message??'Personaldaten konnten nicht geladen werden'; setErr(m); gamNotify('error',m,0); }
 }
 useEffect(()=>{reload('')},[]);
 function startNew(){ setForm(emptyPersonnel(fields)); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
 function startEdit(row:Record<string,unknown>){ const next=emptyPersonnel(fields); fields.forEach(f=>next[f]=rowValue(row,f)??''); next[PERSONNEL_PRIMARY_KEY]=rowValue(row,PERSONNEL_PRIMARY_KEY); setForm(next); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
 function setField(key:string, value:any){ setForm(prev=>({...prev,[key]:value})); }
 async function save(){
   const id=rowValue(form,PERSONNEL_PRIMARY_KEY);
   const payload:Record<string,unknown>={};
   fields.forEach(f=>payload[f]=form[f]??null);
   if(!superadmin){ personnelSuperadminOnlyColumns.forEach(f=>delete payload[f]); }
   const before=id?rows.find(r=>String(rowValue(r,PERSONNEL_PRIMARY_KEY))===String(id)):undefined;
   try{
     const saved=id ? await updateMasterDataRow(PERSONNEL_CATALOG_KEY, String(id), payload) : await createMasterDataRow(PERSONNEL_CATALOG_KEY, payload);
     setOpen(false);
     await reload(q);
     notifySaved(id?'Personaldaten gespeichert.':'Personal angelegt.', buildChangeDetails('Personaldaten', id?'Personaldaten gespeichert':'Personal angelegt', before, saved, personnelColumnLabels, personnelLabel(saved)));
   }catch(e:any){ gamNotify('error', e.message??'Personaldaten konnten nicht gespeichert werden', 0); }
 }
 async function remove(){
   const id=rowValue(form,PERSONNEL_PRIMARY_KEY);
   if(!id) return;
   if(!confirm(`Personaldaten wirklich löschen?\n\n${personnelLabel(form as Record<string,unknown>)}`)) return;
   try{
     await deleteMasterDataRow(PERSONNEL_CATALOG_KEY, String(id));
     setOpen(false);
     await reload(q);
     gamNotify('success','Personaldaten gelöscht.');
   }catch(e:any){ gamNotify('error', e.message??'Personaldaten konnten nicht gelöscht werden', 0); }
 }
 const filtered=rows;
 return <section className="card"><h2>Personaldaten</h2><p className="muted">Schritt 38l5: Tabellenansicht und Bearbeiten-Dialog nutzen dieselbe Rollensicht. Superadmin sieht auch in der Tabelle alle verfügbaren IT-Spalten.</p>{err&&<b className="error">{err}</b>}<div className="stats"><span>Datensätze<br/><b>{rows.length}</b></span><span>Tabelle<br/><b>{catalog?.tableName||'personal'}</b></span><span>Sicht<br/><b>{superadmin?'Superadmin vollständig':'Personalabteilung'}</b></span></div><GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neues Personal</button><label><Search size={16}/><input placeholder="Name, Vorname, Status, Position suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload(q)}}/></label><button className="secondary" type="button" onClick={()=>reload(q)}>Suchen / Aktualisieren</button></GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr>{tableColumns.map(k=><th key={k}>{personnelColumnLabels[k]||k}</th>)}<th>Aktion</th></tr></thead><tbody>{filtered.length?filtered.map((r,idx)=><tr key={String(rowValue(r,PERSONNEL_PRIMARY_KEY)||idx)} className="clickable-row" onClick={()=>startEdit(r)}>{tableColumns.map(k=><td key={k}>{formatCell(rowValue(r,k))}</td>)}<td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>):<tr><td colSpan={tableColumns.length+1} className="muted">Keine Personaldaten gefunden.</td></tr>}</tbody></table></GamScrollArea><GamDialog open={open} title={personnelLabel(form as Record<string,unknown>)} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{rowValue(form,PERSONNEL_PRIMARY_KEY)?'Änderungen speichern':'Personal anlegen'}</button>{rowValue(form,PERSONNEL_PRIMARY_KEY)&&<button className="danger" type="button" onClick={remove}>Personaldaten löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><h3>HR-Sicht · Tabelle personal bis Bemerkung Personal</h3><div className="newgrid">{fields.filter(f=>f!==PERSONNEL_PRIMARY_KEY && !personnelSuperadminOnlyColumns.includes(f)).map((key,idx)=><label key={key} className={(key===PERSONNEL_HR_NOTE_COLUMN || key===PERSONNEL_IT_NOTE_COLUMN)?'wide-field':undefined}>{personnelColumnLabels[key]||key}{(key===PERSONNEL_HR_NOTE_COLUMN || key===PERSONNEL_IT_NOTE_COLUMN)?<textarea value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>:key==='STATUS'?<select value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}><option value="">—</option><option value="aktiv">aktiv</option><option value="inaktiv">inaktiv</option><option value="ausgeschieden">ausgeschieden</option></select>:<input ref={idx===0?firstRef:undefined} type={key==='EMAIL'?'email':'text'} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>}</label>)}</div>{superadmin?<><h3>IT-Administration · nur Superadmin</h3><p className="note warn">Alle IT-Spalten aus der echten personal-Tabelle sind hier sichtbar: Mail-Passwörter, Rechner, Office, Plone, Microsoft, VPN, QNAP, Dienstgeräte und IT-Bemerkung.</p><div className="newgrid">{fields.filter(f=>f!==PERSONNEL_PRIMARY_KEY && personnelSuperadminOnlyColumns.includes(f)).map(key=><label key={key} className={key===PERSONNEL_IT_NOTE_COLUMN?'wide-field':undefined}>{personnelColumnLabels[key]||key}{key===PERSONNEL_IT_NOTE_COLUMN?<textarea value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>:personnelBooleanColumns.includes(key)?<select value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}><option value="">—</option><option value="1">Ja</option><option value="0">Nein</option></select>:<input type={key.includes('PW')||key.includes('PIN')?'password':'text'} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>}</label>)}</div></>:<p className="note">Vorhandene IT-/Admin-Spalten der Tabelle personal sind für diese Rolle ausgeblendet.</p>}</GamDialog></section>;
}


// Schritt 38m: Kassenbuch als bearbeitbares GDS-Modul
const CASHBOOK_CATALOG_KEY = 'cashbook';
const CASHBOOK_PRIMARY_KEY = 'ID';
const cashbookColumnLabels: Record<string,string> = {
  ID:'ID', DATUM:'Datum', 'GESCHÄFTSVORGANG':'Geschäftsvorgang', GESCHAEFTSVORGANG:'Geschäftsvorgang',
  STEUER:'Steuer', EINNAHMEN:'Einnahmen', AUSGABEN:'Ausgaben', BESTAND:'Bestand', GEGENKONTO:'Gegenkonto',
  MANDANTENNUMMER:'Mandantennummer', FIRMA:'Firma'
};
const cashbookPreferredFields = ['ID','DATUM','GESCHÄFTSVORGANG','STEUER','EINNAHMEN','AUSGABEN','BESTAND','GEGENKONTO','MANDANTENNUMMER'];
function cashbookFieldType(name:string){
  const n=name.toUpperCase();
  if(n==='DATUM' || n.includes('DATUM')) return 'date';
  if(['STEUER','EINNAHMEN','AUSGABEN','BESTAND','MANDANTENNUMMER','GEGENKONTO'].includes(n) || n.includes('BETRAG') || n.includes('PREIS')) return 'number';
  return 'text';
}
function cashbookFields(catalog:MasterDataCatalog|null){
  const available=catalog?.fields?.length ? catalog.fields : cashbookPreferredFields.filter(f=>f!==CASHBOOK_PRIMARY_KEY);
  const ordered=cashbookPreferredFields.filter(f=>f===CASHBOOK_PRIMARY_KEY || available.includes(f));
  return [...ordered, ...available.filter(f=>!ordered.includes(f))];
}
function cashbookEditableFields(catalog:MasterDataCatalog|null){ return cashbookFields(catalog).filter(f=>f!==CASHBOOK_PRIMARY_KEY); }
function cashbookEmpty(fields:string[]){ const row:Record<string,unknown>={}; fields.forEach(f=>row[f]=''); row.DATUM=new Date().toISOString().slice(0,10); return row; }
function cashbookLabel(row:Record<string,unknown>){
  const id=String(rowValue(row,CASHBOOK_PRIMARY_KEY)??'').trim();
  const date=String(rowValue(row,'DATUM')??'').trim();
  const text=String(rowValue(row,'GESCHÄFTSVORGANG')??rowValue(row,'GESCHAEFTSVORGANG')??'').trim();
  return [date,text].filter(Boolean).join(' · ') || (id?`Kassenbucheintrag #${id}`:'Neuer Kassenbucheintrag');
}
function parseCashbookNumber(value:any){
  if(value==='' || value==null) return null;
  const n=Number(String(value).replace(',','.'));
  return Number.isFinite(n) ? n : value;
}
function CashbookPage(){
 const [catalog,setCatalog]=useState<MasterDataCatalog|null>(null);
 const [rows,setRows]=useState<Record<string,unknown>[]>([]);
 const [q,setQ]=useState('');
 const [form,setForm]=useState<Record<string,unknown>>({});
 const [open,setOpen]=useState(false);
 const [err,setErr]=useState('');
 const firstRef=useRef<any>(null);
 const fields=cashbookFields(catalog);
 const editable=cashbookEditableFields(catalog);
 const totals=rows.reduce((a,r)=>({in:a.in+Number(rowValue(r,'EINNAHMEN')||0), out:a.out+Number(rowValue(r,'AUSGABEN')||0)}),{in:0,out:0});
 async function reload(search=q){
   setErr('');
   try{ const data=await loadMasterDataRows(CASHBOOK_CATALOG_KEY, search, 300); setCatalog(data.catalog); setRows(data.rows||[]); }
   catch(e:any){ const m=e.message??'Kassenbuch konnte nicht geladen werden'; setErr(m); gamNotify('error',m,0); }
 }
 useEffect(()=>{reload('')},[]);
 function startNew(){ setForm(cashbookEmpty(editable)); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
 function startEdit(row:Record<string,unknown>){ const next:Record<string,unknown>={}; fields.forEach(f=>next[f]=rowValue(row,f)??''); setForm(next); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
 function setField(key:string,value:any){ setForm(prev=>({...prev,[key]:value})); }
 async function save(){
   const id=rowValue(form,CASHBOOK_PRIMARY_KEY);
   const payload:Record<string,unknown>={};
   editable.forEach(f=>{ payload[f]=cashbookFieldType(f)==='number'?parseCashbookNumber(form[f]):(form[f]??null); });
   const before=id?rows.find(r=>String(rowValue(r,CASHBOOK_PRIMARY_KEY))===String(id)):undefined;
   try{
     const saved=id ? await updateMasterDataRow(CASHBOOK_CATALOG_KEY, String(id), payload) : await createMasterDataRow(CASHBOOK_CATALOG_KEY, payload);
     setOpen(false); await reload(q);
     notifySaved(id?'Kassenbucheintrag gespeichert.':'Kassenbucheintrag angelegt.', buildChangeDetails('Kassenbuch', id?'Kassenbucheintrag gespeichert':'Kassenbucheintrag angelegt', before, saved, cashbookColumnLabels, cashbookLabel(saved)));
   }catch(e:any){ gamNotify('error', e.message??'Kassenbucheintrag konnte nicht gespeichert werden', 0); }
 }
 async function remove(){
   const id=rowValue(form,CASHBOOK_PRIMARY_KEY); if(!id) return;
   if(!confirm(`Kassenbucheintrag wirklich löschen?\n\n${cashbookLabel(form)}`)) return;
   try{ await deleteMasterDataRow(CASHBOOK_CATALOG_KEY, String(id)); setOpen(false); await reload(q); gamNotify('success','Kassenbucheintrag gelöscht.'); }
   catch(e:any){ gamNotify('error', e.message??'Kassenbucheintrag konnte nicht gelöscht werden', 0); }
 }
 return <section className="card"><h2>Kassenbuch</h2><p className="muted">Schritt 38m: Kassenbuch als bearbeitbares GDS-Modul auf Basis der bestehenden Tabelle <code>kassenbuch</code>. Neu, Bearbeiten, Löschen, Suche, Toasts und Änderungsdetails sind aktiv.</p>{err&&<b className="error">{err}</b>}<div className="stats"><span>Datensätze<br/><b>{rows.length}</b></span><span>Einnahmen<br/><b>{money(totals.in)}</b></span><span>Ausgaben<br/><b>{money(totals.out)}</b></span><span>Saldo Auswahl<br/><b>{money(totals.in-totals.out)}</b></span></div><GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neuer Kassenbucheintrag</button><label><Search size={16}/><input placeholder="Geschäftsvorgang oder Gegenkonto suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload(q)}}/></label><button className="secondary" type="button" onClick={()=>reload(q)}>Suchen / Aktualisieren</button></GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr>{fields.map(k=><th key={k}>{cashbookColumnLabels[k]||k}</th>)}<th>Aktion</th></tr></thead><tbody>{rows.length?rows.map((r,idx)=><tr key={String(rowValue(r,CASHBOOK_PRIMARY_KEY)||idx)} className="clickable-row" onClick={()=>startEdit(r)}>{fields.map(k=><td key={k}>{renderInvoiceAdminCell(k,rowValue(r,k))}</td>)}<td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>):<tr><td colSpan={fields.length+1} className="muted">Keine Kassenbucheinträge gefunden.</td></tr>}</tbody></table></GamScrollArea><GamDialog open={open} title={cashbookLabel(form)} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{rowValue(form,CASHBOOK_PRIMARY_KEY)?'Änderungen speichern':'Kassenbucheintrag anlegen'}</button>{rowValue(form,CASHBOOK_PRIMARY_KEY)&&<button className="danger" type="button" onClick={remove}>Kassenbucheintrag löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid">{editable.map((key,idx)=><label key={key} className={key==='GESCHÄFTSVORGANG'?'wide-field':undefined}>{cashbookColumnLabels[key]||key}{key==='GESCHÄFTSVORGANG'?<textarea ref={idx===0?firstRef:undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>:<input ref={idx===0?firstRef:undefined} type={cashbookFieldType(key)} step={cashbookFieldType(key)==='number'?'0.01':undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>}</label>)}</div></GamDialog></section>;
}


// Schritt 38n: Arbeitsplatzausstattung als bearbeitbares GDS-Modul
const WORKPLACE_CATALOG_KEY = 'workplaces';
const WORKPLACE_PRIMARY_KEY = 'ID';
const workplaceColumnLabels: Record<string,string> = {
  ID:'ID', FILIALE_ID:'Filiale', ARBEITSPLATZ:'Arbeitsplatz', TELEFON:'Telefon',
  DATUM_EINRICHTUNG:'Datum Einrichtung', DATUM_ANTRAGSTELLUNG:'Datum Antragstellung', DATUM_ANTRAGGENEHMIGT:'Datum genehmigt',
  MITARBEITER:'Mitarbeiter', FERTIGGESTELLT:'Fertiggestellt'
};
const workplacePreferredFields = ['ID','FILIALE_ID','ARBEITSPLATZ','TELEFON','DATUM_EINRICHTUNG','DATUM_ANTRAGSTELLUNG','DATUM_ANTRAGGENEHMIGT','MITARBEITER','FERTIGGESTELLT'];
function workplaceFieldType(name:string){
  const n=name.toUpperCase();
  if(n.includes('DATUM')) return 'date';
  if(n==='FILIALE_ID') return 'number';
  if(n==='TELEFON' || n==='FERTIGGESTELLT') return 'boolean';
  return 'text';
}
function workplaceFields(catalog:MasterDataCatalog|null){
  const available=catalog?.fields?.length ? catalog.fields : workplacePreferredFields.filter(f=>f!==WORKPLACE_PRIMARY_KEY);
  const ordered=workplacePreferredFields.filter(f=>f===WORKPLACE_PRIMARY_KEY || available.includes(f));
  return [...ordered, ...available.filter(f=>!ordered.includes(f))];
}
function workplaceEditableFields(catalog:MasterDataCatalog|null){ return workplaceFields(catalog).filter(f=>f!==WORKPLACE_PRIMARY_KEY); }
function workplaceEmpty(fields:string[]){ const row:Record<string,unknown>={}; fields.forEach(f=>row[f]=''); return row; }
function workplaceLabel(row:Record<string,unknown>){
  const place=String(rowValue(row,'ARBEITSPLATZ')??'').trim();
  const user=String(rowValue(row,'MITARBEITER')??'').trim();
  const id=String(rowValue(row,WORKPLACE_PRIMARY_KEY)??'').trim();
  return [place,user].filter(Boolean).join(' · ') || (id?`Arbeitsplatz #${id}`:'Neue Arbeitsplatzausstattung');
}
function normalizeWorkplaceValue(key:string,value:any){
  const type=workplaceFieldType(key);
  if(type==='boolean'){
    if(value==='' || value==null) return null;
    return String(value)==='1' || String(value).toLowerCase()==='true';
  }
  if(type==='number'){
    if(value==='' || value==null) return null;
    const n=Number(String(value).replace(',','.'));
    return Number.isFinite(n) ? n : value;
  }
  return value==='' ? null : value;
}
function WorkplacePage(){
 const [catalog,setCatalog]=useState<MasterDataCatalog|null>(null);
 const [rows,setRows]=useState<Record<string,unknown>[]>([]);
 const [q,setQ]=useState('');
 const [err,setErr]=useState('');
 const [open,setOpen]=useState(false);
 const [form,setForm]=useState<Record<string,unknown>>({});
 const firstRef=useRef<HTMLInputElement|HTMLTextAreaElement|null>(null);
 const fields=workplaceFields(catalog);
 const editable=workplaceEditableFields(catalog);
 async function reload(search=q){
   setErr('');
   try{ const data=await loadMasterDataRows(WORKPLACE_CATALOG_KEY, search, 250); setCatalog(data.catalog); setRows(data.rows); }
   catch(e:any){ const m=e.message??'Arbeitsplatzausstattung konnte nicht geladen werden'; setErr(m); gamNotify('error',m,0); }
 }
 useEffect(()=>{reload('')},[]);
 function startNew(){ setForm(workplaceEmpty(editable)); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
 function startEdit(row:Record<string,unknown>){ const next:Record<string,unknown>={}; fields.forEach(f=>next[f]=rowValue(row,f)??''); setForm(next); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
 function setField(key:string,value:any){ setForm(prev=>({...prev,[key]:value})); }
 async function save(){
   const id=rowValue(form,WORKPLACE_PRIMARY_KEY);
   const payload:Record<string,unknown>={};
   editable.forEach(f=>{ payload[f]=normalizeWorkplaceValue(f,form[f]); });
   const before=id?rows.find(r=>String(rowValue(r,WORKPLACE_PRIMARY_KEY))===String(id)):undefined;
   try{
     const saved=id ? await updateMasterDataRow(WORKPLACE_CATALOG_KEY, String(id), payload) : await createMasterDataRow(WORKPLACE_CATALOG_KEY, payload);
     setOpen(false); await reload(q);
     notifySaved(id?'Arbeitsplatzausstattung gespeichert.':'Arbeitsplatzausstattung angelegt.', buildChangeDetails('Arbeitsplatzausstattung', id?'Arbeitsplatzausstattung gespeichert':'Arbeitsplatzausstattung angelegt', before, saved, workplaceColumnLabels, workplaceLabel(saved)));
   }catch(e:any){ gamNotify('error', e.message??'Arbeitsplatzausstattung konnte nicht gespeichert werden', 0); }
 }
 async function remove(){
   const id=rowValue(form,WORKPLACE_PRIMARY_KEY); if(!id) return;
   if(!confirm(`Arbeitsplatzausstattung wirklich löschen?\n\n${workplaceLabel(form)}`)) return;
   try{ await deleteMasterDataRow(WORKPLACE_CATALOG_KEY, String(id)); setOpen(false); await reload(q); gamNotify('success','Arbeitsplatzausstattung gelöscht.'); }
   catch(e:any){ gamNotify('error', e.message??'Arbeitsplatzausstattung konnte nicht gelöscht werden', 0); }
 }
 const ready=rows.filter(r=>String(rowValue(r,'FERTIGGESTELLT')??'').toLowerCase()==='true' || String(rowValue(r,'FERTIGGESTELLT')??'')==='1').length;
 const withPhone=rows.filter(r=>String(rowValue(r,'TELEFON')??'').toLowerCase()==='true' || String(rowValue(r,'TELEFON')??'')==='1').length;
 return <section className="card"><h2>Arbeitsplatzausstattung</h2><p className="muted">Schritt 38n: Arbeitsplatzausstattung als bearbeitbares GDS-Modul auf Basis der bestehenden Tabelle <code>arbeitsplatz</code>. Tabelle und Dialog nutzen alle verfügbaren Arbeitsplatz-Spalten.</p>{err&&<b className="error">{err}</b>}<div className="stats"><span>Datensätze<br/><b>{rows.length}</b></span><span>Mit Telefon<br/><b>{withPhone}</b></span><span>Fertiggestellt<br/><b>{ready}</b></span><span>Tabelle<br/><b>{catalog?.tableName||'arbeitsplatz'}</b></span></div><GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neuer Arbeitsplatz</button><label><Search size={16}/><input placeholder="Arbeitsplatz oder Mitarbeiter suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload(q)}}/></label><button className="secondary" type="button" onClick={()=>reload(q)}>Suchen / Aktualisieren</button></GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr>{fields.map(k=><th key={k}>{workplaceColumnLabels[k]||k}</th>)}<th>Aktion</th></tr></thead><tbody>{rows.length?rows.map((r,idx)=><tr key={String(rowValue(r,WORKPLACE_PRIMARY_KEY)||idx)} className="clickable-row" onClick={()=>startEdit(r)}>{fields.map(k=><td key={k}>{renderInvoiceAdminCell(k,rowValue(r,k))}</td>)}<td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>):<tr><td colSpan={fields.length+1} className="muted">Keine Arbeitsplatzausstattung gefunden.</td></tr>}</tbody></table></GamScrollArea><GamDialog open={open} title={workplaceLabel(form)} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{rowValue(form,WORKPLACE_PRIMARY_KEY)?'Änderungen speichern':'Arbeitsplatz anlegen'}</button>{rowValue(form,WORKPLACE_PRIMARY_KEY)&&<button className="danger" type="button" onClick={remove}>Arbeitsplatz löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid">{editable.map((key,idx)=><label key={key}>{workplaceColumnLabels[key]||key}{workplaceFieldType(key)==='boolean'?<select value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}><option value="">—</option><option value="1">Ja</option><option value="0">Nein</option></select>:<input ref={idx===0?firstRef:undefined} type={workplaceFieldType(key)} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>}</label>)}</div></GamDialog></section>;
}

function RecordsPage({title,loader}:{title:string; loader:(limit?:number)=>Promise<any[]>}){const [rows,setRows]=useState<any[]>([]); const [err,setErr]=useState(''); useEffect(()=>{loader(150).then(setRows).catch((e:any)=>setErr(e.message??'Konnte Daten nicht laden'))},[title]); return <section className="card"><h2>{title}</h2><p className="muted">Schritt 5: sichere Leseansicht als Modulrahmen. Schreib-/Bearbeitungslogik wird erst nach Abgleich mit der alten Fachlogik aktiviert.</p>{err&&<b className="error">{err}</b>}<GenericTable rows={rows}/></section>}
// Schritt 38p: Prüfungen als bearbeitbares GDS-Modul über vorhandene MasterData-Tabellen
const COMPLIANCE_CATALOGS = [
  {catalogKey:'device-checks', title:'Geräteprüfungen', empty:'Keine Geräteprüfungen gefunden.', add:'Neue Geräteprüfung'},
  {catalogKey:'commissioning', title:'Inbetriebnahmen', empty:'Keine Inbetriebnahmen gefunden.', add:'Neue Inbetriebnahme'},
  {catalogKey:'instructions', title:'Einweisungen', empty:'Keine Einweisungen gefunden.', add:'Neue Einweisung'},
] as const;
const complianceColumnLabels: Record<string,string> = {
 ID:'ID', KONTROLL_ID:'ID', INBETRIEBNAHME_ID:'ID', EINWEISUNGS_ID:'ID',
 GERAET_ID:'Gerät', PERSONAL_ID:'Personal', PATIENT_ID:'Patient', FILIALE_ID:'Filiale', GESELLSCHAFT_ID:'Gesellschaft',
 DATUM:'Datum', PRUEFDATUM:'Prüfdatum', NAECHSTE_PRUEFUNG:'Nächste Prüfung', FAELLIG_AM:'Fällig am',
 ART:'Art', TYP:'Typ', STATUS:'Status', ERGEBNIS:'Ergebnis', BEZEICHNUNG:'Bezeichnung', BESCHREIBUNG:'Beschreibung',
 PRUEFER:'Prüfer', DURCHGEFUEHRT_VON:'Durchgeführt von', TEILNEHMER:'Teilnehmer', ORT:'Ort',
 BEMERKUNG:'Bemerkung', NOTIZ:'Notiz'
};
function complianceFieldType(name:string){ const n=name.toUpperCase(); if(n.includes('DATUM')||n.includes('DATE')||n.includes('FAELLIG')) return 'date'; if(n.endsWith('_ID') || n==='ID' || n.includes('ANZAHL')) return 'number'; return 'text'; }
function complianceLabel(row:Record<string,unknown>, catalog?:MasterDataCatalog|null){
 const id=rowValue(row,catalog?.primaryKey||'ID') ?? rowValue(row,'ID') ?? '';
 const parts=['BEZEICHNUNG','ART','TYP','STATUS','DATUM','PRUEFDATUM'].map(k=>rowValue(row,k)).filter(Boolean).map(String);
 return parts.join(' · ') || (id?`${catalog?.label||'Prüfung'} #${id}`:'Neuer Eintrag');
}
function complianceFields(catalog:MasterDataCatalog|null){ return catalog?.fields?.length ? catalog.fields : []; }
function complianceEditableFields(catalog:MasterDataCatalog|null){ return complianceFields(catalog).filter(f=>f!==catalog?.primaryKey); }
function complianceEmpty(fields:string[]){ const row:Record<string,unknown>={}; fields.forEach(f=>row[f]=''); return row; }

// Schritt 38p4: Frühwarnsystem für Prüfungen
// GAM soll den Benutzer aktiv davor schützen, überfällige oder bald fällige Prüfungen zu übersehen.
type ComplianceDueLevel = 'overdue' | 'soon' | null;
type ComplianceDueInfo = { level: ComplianceDueLevel; date?: Date; label: string; days: number; source: string };
function complianceToday(){ const d=new Date(); d.setHours(0,0,0,0); return d; }
function complianceParseDate(value:unknown):Date|null{
 const raw=String(value??'').trim(); if(!raw || raw==='—') return null;
 let m=raw.match(/^(\d{4})-(\d{2})-(\d{2})/);
 if(m){ const d=new Date(Number(m[1]),Number(m[2])-1,Number(m[3])); return isNaN(d.getTime())?null:d; }
 m=raw.match(/^(\d{1,2})\.(\d{1,2})\.(\d{2,4})$/);
 if(m){ const y=Number(m[3].length===2?'20'+m[3]:m[3]); const d=new Date(y,Number(m[2])-1,Number(m[1])); return isNaN(d.getTime())?null:d; }
 const d=new Date(raw); if(isNaN(d.getTime())) return null; d.setHours(0,0,0,0); return d;
}
function complianceAddInterval(base:Date|null, intervalValue:unknown):Date|null{
 if(!base) return null;
 const raw=String(intervalValue??'').trim(); if(!raw) return null;
 const m=raw.match(/\d+/); if(!m) return null;
 const amount=Number(m[0]); if(!amount) return null;
 const lower=raw.toLowerCase();
 const d=new Date(base.getTime());
 if(lower.includes('jahr') || lower.includes('year') || lower.includes('j.')) d.setFullYear(d.getFullYear()+amount);
 else if(lower.includes('tag') || lower.includes('day')) d.setDate(d.getDate()+amount);
 else d.setMonth(d.getMonth()+amount); // GAM 1.0 Intervalle sind hier typischerweise Monatsintervalle.
 return d;
}
function complianceDaysUntil(date:Date){ const today=complianceToday(); return Math.floor((date.getTime()-today.getTime())/86400000); }
function complianceDueFromDate(date:Date|null, source:string):ComplianceDueInfo|null{
 if(!date) return null; date.setHours(0,0,0,0);
 const days=complianceDaysUntil(date);
 if(days<0) return {level:'overdue', date, days, source, label:`${source}: seit ${Math.abs(days)} Tag${Math.abs(days)===1?'':'en'} überfällig`};
 if(days<=14) return {level:'soon', date, days, source, label:`${source}: in ${days} Tag${days===1?'':'en'} fällig`};
 return null;
}
function complianceRowDueInfo(row:Record<string,unknown>, catalogKey:string):ComplianceDueInfo|null{
 const candidates:ComplianceDueInfo[]=[];
 const add=(date:Date|null, source:string)=>{ const info=complianceDueFromDate(date,source); if(info) candidates.push(info); };
 if(catalogKey==='device-checks'){
   add(complianceAddInterval(complianceParseDate(rowValue(row,'DATUMLETZTEPRÜFUNG_STK')), rowValue(row,'INTERVALL_STK') || rowValue(row,'INTERVALL')), 'STK');
   add(complianceAddInterval(complianceParseDate(rowValue(row,'DATUMLETZTEPRÜFUNG_MTK')), rowValue(row,'INTERVALL_MTK') || rowValue(row,'INTERVALL')), 'MTK');
   add(complianceAddInterval(complianceParseDate(rowValue(row,'DATUMLETZTEPRÜFUNG_BGV_A3')), rowValue(row,'INTERVALL_BGV_A3') || rowValue(row,'INTERVALL')), 'BGV-A3');
 }
 if(catalogKey==='instructions'){
   add(complianceParseDate(rowValue(row,'DATUMFOLGEEINWEISUNG')), 'Folgeeinweisung');
 }
 // Falls spätere Tabellen echte Fälligkeits-/Nächste-Prüfung-Spalten bekommen, werden sie automatisch berücksichtigt.
 Object.keys(row).forEach(k=>{
   const up=k.toUpperCase();
   if(up.includes('NAECHSTE') || up.includes('NÄCHSTE') || up.includes('FAELLIG') || up.includes('FÄLLIG')) add(complianceParseDate(rowValue(row,k)), complianceColumnLabels[k]||k);
 });
 candidates.sort((a,b)=>a.days-b.days);
 return candidates[0]||null;
}
function complianceFilterRows(rows:Record<string,unknown>[], catalogKey:string, filter:'all'|'overdue'|'soon'){
 if(filter==='all') return rows;
 return rows.filter(r=>complianceRowDueInfo(r,catalogKey)?.level===filter);
}
function complianceDueName(row:Record<string,unknown>, catalog?:MasterDataCatalog|null){
 return String(rowValue(row,'GERÄTE') || rowValue(row,'GERAET') || rowValue(row,'BEZEICHNUNG') || complianceLabel(row,catalog));
}

function ComplianceCatalogEditor({catalogKey,title,empty,add}:{catalogKey:string; title:string; empty:string; add:string}){
 const [catalog,setCatalog]=useState<MasterDataCatalog|null>(null);
 const [rows,setRows]=useState<Record<string,unknown>[]>([]);
 const [q,setQ]=useState(''); const [err,setErr]=useState(''); const [open,setOpen]=useState(false);
 const [dueFilter,setDueFilter]=useState<'all'|'overdue'|'soon'>('all');
 const [form,setForm]=useState<Record<string,unknown>>({}); const firstRef=useRef<HTMLInputElement|HTMLTextAreaElement|null>(null);
 const warnedKeyRef=useRef('');
 const fields=complianceFields(catalog); const editable=complianceEditableFields(catalog); const primary=catalog?.primaryKey||'ID';
 const dueRows = rows.map(r=>({row:r, due: complianceRowDueInfo(r,catalogKey)}));
 const overdueRows = dueRows.filter(x=>x.due?.level==='overdue');
 const soonRows = dueRows.filter(x=>x.due?.level==='soon');
 const shownRows = complianceFilterRows(rows, catalogKey, dueFilter);
 async function reload(query=q){
   try{
     const catalogs=await loadMasterDataCatalogs();
     const cat=catalogs.find(c=>c.key===catalogKey) || null;
     setCatalog(cat);
     if(cat){
       const data = await loadMasterDataRows(catalogKey, query||'', 300);
       const loadedRows=(data.rows || []) as Record<string,unknown>[];
       setRows(loadedRows);
       const warningKey = `${catalogKey}:${loadedRows.length}:${query||''}`;
       if(warnedKeyRef.current!==warningKey){
         warnedKeyRef.current=warningKey;
         const overdue=loadedRows.filter(r=>complianceRowDueInfo(r,catalogKey)?.level==='overdue').length;
         const soon=loadedRows.filter(r=>complianceRowDueInfo(r,catalogKey)?.level==='soon').length;
         if(overdue>0) gamNotify('error', `${title}: ${overdue} überfällige Prüfung${overdue===1?'':'en'} gefunden.`, 0);
         else if(soon>0) gamNotify('warning', `${title}: ${soon} Prüfung${soon===1?'':'en'} in den nächsten 14 Tagen fällig.`);
       }
       setErr('');
     }
     else { setRows([]); setErr(`MasterData-Katalog ${catalogKey} nicht gefunden.`); }
   }catch(e:any){ const m=e.message??`${title} konnte nicht geladen werden`; setErr(m); gamNotify('error',m,0); }
 }
 useEffect(()=>{reload('')},[catalogKey]);
 function startNew(){ setForm(complianceEmpty(editable)); setOpen(true); }
 function startEdit(r:Record<string,unknown>){ setForm({...r}); setOpen(true); }
 function setField(key:string,value:any){ setForm(prev=>({...prev,[key]:value})); }
 async function save(){
   const id=rowValue(form,primary); const payload:Record<string,unknown>={}; editable.forEach(f=>payload[f]=form[f]??'');
   const before=id?rows.find(r=>String(rowValue(r,primary))===String(id)):undefined;
   try{
     const saved=id ? await updateMasterDataRow(catalogKey, String(id), payload) : await createMasterDataRow(catalogKey, payload);
     setOpen(false); await reload(q);
     notifySaved(id?`${title} gespeichert.`:`${title} angelegt.`, buildChangeDetails(title, id?`${title} gespeichert`:`${title} angelegt`, before, saved, complianceColumnLabels, complianceLabel(saved,catalog)));
   }catch(e:any){ gamNotify('error', e.message??`${title} konnte nicht gespeichert werden`, 0); }
 }
 async function remove(){
   const id=rowValue(form,primary); if(!id) return;
   if(!confirm(`${title} wirklich löschen?\n\n${complianceLabel(form,catalog)}`)) return;
   try{ await deleteMasterDataRow(catalogKey, String(id)); setOpen(false); await reload(q); gamNotify('success',`${title} gelöscht.`); }
   catch(e:any){ gamNotify('error', e.message??`${title} konnte nicht gelöscht werden`, 0); }
 }
 return <div className="card subtle-card"><h3>{title}</h3>{err&&<b className="error">{err}</b>}<div className="stats"><button className={dueFilter==='all'?'stat-button active':'stat-button'} type="button" onClick={()=>setDueFilter('all')}>Datensätze<br/><b>{rows.length}</b></button><button className={overdueRows.length?'stat-button danger-stat':'stat-button'} type="button" onClick={()=>setDueFilter('overdue')}>Überfällig<br/><b>{overdueRows.length}</b></button><button className={soonRows.length?'stat-button warning-stat':'stat-button'} type="button" onClick={()=>setDueFilter('soon')}>Nächste 14 Tage<br/><b>{soonRows.length}</b></button><span>Tabelle<br/><b>{catalog?.tableName||catalogKey}</b></span></div>{overdueRows.length>0&&<div className="note overdue-note" role="alert" onClick={()=>setDueFilter('overdue')}><b>🔴 Überfällige Prüfungen</b><br/>{overdueRows.slice(0,5).map(({row,due})=><small key={String(rowValue(row,primary))}>{complianceDueName(row,catalog)} · {due?.label}</small>)}{overdueRows.length>5&&<small>… und {overdueRows.length-5} weitere</small>}</div>}{soonRows.length>0&&<div className="note soon-note" onClick={()=>setDueFilter('soon')}><b>🟡 Bald fällige Prüfungen</b><br/>{soonRows.slice(0,5).map(({row,due})=><small key={String(rowValue(row,primary))}>{complianceDueName(row,catalog)} · {due?.label}</small>)}{soonRows.length>5&&<small>… und {soonRows.length-5} weitere</small>}</div>}<GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> {add}</button><label><Search size={16}/><input placeholder="Suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload(q)}}/></label><button className="secondary" type="button" onClick={()=>reload(q)}>Suchen / Aktualisieren</button>{dueFilter!=='all'&&<button className="secondary" type="button" onClick={()=>setDueFilter('all')}>Filter zurücksetzen</button>}</GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr><th>Fälligkeit</th>{fields.map(k=><th key={k}>{complianceColumnLabels[k]||k}</th>)}<th>Aktion</th></tr></thead><tbody>{shownRows.length?shownRows.map((r,idx)=>{const due=complianceRowDueInfo(r,catalogKey); return <tr key={String(rowValue(r,primary)||idx)} className={due?.level==='overdue'?"clickable-row due-overdue":due?.level==='soon'?"clickable-row due-soon":"clickable-row"} onClick={()=>startEdit(r)}><td>{due?.level==='overdue'?'🔴':due?.level==='soon'?'🟡':'—'} {due?.label||''}</td>{fields.map(k=><td key={k}>{renderInvoiceAdminCell(k,rowValue(r,k))}</td>)}<td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>}):<tr><td colSpan={fields.length+2} className="muted">{dueFilter==='overdue'?'Keine überfälligen Prüfungen.':dueFilter==='soon'?'Keine bald fälligen Prüfungen.':empty}</td></tr>}</tbody></table></GamScrollArea><GamDialog open={open} title={complianceLabel(form,catalog)} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{rowValue(form,primary)?'Änderungen speichern':'Eintrag anlegen'}</button>{rowValue(form,primary)&&<button className="danger" type="button" onClick={remove}>Eintrag löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid">{editable.map((key,idx)=><label key={key} className={(key.toUpperCase().includes('BEMERKUNG')||key.toUpperCase().includes('BESCHREIBUNG'))?'wide-field':undefined}>{complianceColumnLabels[key]||key}{(key.toUpperCase().includes('BEMERKUNG')||key.toUpperCase().includes('BESCHREIBUNG'))?<textarea ref={idx===0?firstRef:undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>:<input ref={idx===0?firstRef:undefined} type={complianceFieldType(key)} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>}</label>)}</div></GamDialog></div>;
}
function CompliancePage(){return <section className="card"><h2>Prüfungen</h2><p className="muted">Schritt 38p4: Prüfungen mit Frühwarnsystem. Überfällige Prüfungen werden rot angemahnt, Prüfungen der nächsten 14 Tage gelb vorgewarnt.</p>{COMPLIANCE_CATALOGS.map(c=><ComplianceCatalogEditor key={c.catalogKey} {...c}/>)}</section>}

function ReportsPage(){
 const today = new Date().toISOString().slice(0,10);
 const start = new Date(); start.setDate(start.getDate()-30);
 const defaultFrom = start.toISOString().slice(0,10);
 const [from,setFrom]=useState(defaultFrom);
 const [to,setTo]=useState(today);
 const [companyId,setCompanyId]=useState<number|undefined>();
 const [companies,setCompanies]=useState<InvoiceCompany[]>([]);
 const [summary,setSummary]=useState<InvoiceReportSummary|null>(null);
 const [rows,setRows]=useState<InvoiceReportRow[]>([]);
 const [reportType,setReportType]=useState('overview');
 const [err,setErr]=useState('');
 useEffect(()=>{loadCompanies().then(setCompanies).catch(()=>{})},[]);
 async function run(){setErr(''); try{setSummary(await loadInvoiceReportSummary(from,to,companyId,reportType)); setRows(await loadInvoiceReportRows(from,to,companyId,reportType));}catch(e:any){setErr(e.message??'Report konnte nicht geladen werden')}}
 useEffect(()=>{run()},[]);
 async function dl(format:string){setErr(''); try{await downloadInvoiceReport(from,to,companyId,format,reportType)}catch(e:any){setErr(e.message??'Export fehlgeschlagen')}}
 return <section className="card"><h2>Reports / Exporte</h2><p className="muted">Reports nach Alt-GAM-Logik: Alle Daten, DATEV, Debitoren, Umsatz, Umsatz je Arzt, Umsatz je Filiale, Tagesliste und Produktranking. XLS/XLSX bleiben verfügbar; DATEV-CSV ist als direkte Exportvorbereitung ergänzt.</p><div className="newgrid"><label>Von<input type="date" value={from} onChange={e=>setFrom(e.target.value)}/></label><label>Bis<input type="date" value={to} onChange={e=>setTo(e.target.value)}/></label><label>{ui("company")}<select value={companyId??''} onChange={e=>setCompanyId(e.target.value?Number(e.target.value):undefined)}><option value="">Alle Gesellschaften</option>{companies.map(c=><option key={c.id} value={c.id}>{c.name??c.code??c.id}</option>)}</select></label><label>Reportart<select value={reportType} onChange={e=>setReportType(e.target.value)}><option value="overview">Übersicht Belege</option><option value="allData">Alle Daten / Positionen</option><option value="datev">DATEV</option><option value="debitoren">Debitoren</option><option value="umsatz">Umsatz</option><option value="umsatzArzt">Umsatz je Arzt</option><option value="umsatzFiliale">Umsatz je Filiale</option><option value="tagesliste">Tagesliste</option><option value="produktranking">Produktranking</option></select></label><button type="button" onClick={run}>Report laden</button></div>{err&&<b className="error">{err}</b>}{summary&&<div className="stats"><span>Belege<br/><b>{summary.count}</b></span><span>Rechnungen<br/><b>{summary.invoices}</b></span><span>Stornos<br/><b>{summary.cancellations}</b></span><span>Gutschriften<br/><b>{summary.creditNotes}</b></span><span>Zahlungsavis<br/><b>{summary.paymentAdvices}</b></span><span>Brutto gesamt<br/><b>{money(summary.grossTotal)}</b></span></div>}<div className="download-actions"><button className="secondary" type="button" onClick={()=>dl('xlsx')}>XLSX herunterladen</button><button className="secondary" type="button" onClick={()=>dl('xls')}>XLS herunterladen</button><button className="secondary" type="button" onClick={()=>dl('csv')}>CSV herunterladen</button><button className="secondary" type="button" onClick={()=>dl('datev')}>DATEV-CSV vorbereiten</button></div>{summary?.note&&<p className="note">{summary.note}</p>}<table><thead><tr><th>Datum</th><th>{ui("company")}</th><th>Belegart</th><th>Nummer</th><th>Benutzer</th><th>{ui("reason")}</th><th>{ui("gross")}</th></tr></thead><tbody>{rows.length?rows.slice(0,200).map((r,idx)=><tr key={idx}><td>{r.invoiceDate??'—'}</td><td>{r.companyName??'—'}</td><td>{r.documentType??'—'}</td><td>{r.number??'—'}</td><td>{r.username??'—'}</td><td>{r.reason??'—'}</td><td>{money(r.gross)}</td></tr>):<tr><td colSpan={7} className="muted">Keine Reportdaten gefunden.</td></tr>}</tbody></table></section>
}


// Schritt 38: Administrationsbereiche außerhalb des Rechnungsmoduls

function CommunicationCenterPage(){
  const [protocol,setProtocol]=useState<GamNotification[]>([]);
  const [news,setNews]=useState<ModuleRecord[]>([]);
  const [loginNews,setLoginNews]=useState<LoginNews|null>(null);
  const [settings,setSettings]=useState<CommunicationSettings|null>(null);
  const [selected,setSelected]=useState<GamNotification|null>(null);
  const [q,setQ]=useState('');
  const reload=()=>{
    setProtocol(loadGcsProtocol());
    loadGamNews(25).then(setNews).catch(()=>setNews([]));
    loadLoginNews().then(setLoginNews).catch(()=>setLoginNews(null));
    loadCommunicationSettings().then(setSettings).catch(()=>setSettings(null));
  };
  useEffect(reload,[]);
  const filtered=protocol.filter(p=>`${p.kind} ${p.text} ${p.details?.module||''} ${p.details?.action||''}`.toLowerCase().includes(q.toLowerCase()));
  async function sendTest(){
    const smtp = effectiveSmtpForDirectSend();
    const to = loadLocalSmtpSettings().from || loadLocalSmtpSettings().username;
    if(!to){ gamNotify('warning','Bitte zuerst SMTP-Benutzer/Absender eintragen.',0); return; }
    try{
      const result=await sendOrderEmail({to,subject:'GAM Testmail',text:'Diese Testmail wurde vom GAM Communication Service gesendet.',smtp});
      gamNotify('success','Testmail erfolgreich versendet.', undefined, {module:'Kommunikation', action:'Testmail', record:to, fields:[{field:'Empfänger',before:'—',after:to},{field:'Status',before:'—',after:result.message||'OK'}], summary:'SMTP-Test wurde ausgeführt.'});
      reload();
    }catch(e:any){ gamNotify('error',e.message||'Testmail fehlgeschlagen.',0); }
  }
  return <section className="grid"><section className="card"><h2>GCS · GAM Communication Service</h2><p className="muted">Schritt 38g8 konsolidiert SMTP, Login-News, Meldungsverlauf, Änderungsdetails und Kommunikationsprotokoll an einer zentralen Stelle.</p><div className="stats"><span>Protokoll<br/><b>{protocol.length}</b></span><span>News<br/><b>{news.length}</b></span><span>SMTP<br/><b>{settings?.smtpPasswordConfigured?'konfiguriert':'lokal/ausstehend'}</b></span><span>Login-News<br/><b>{loginNews?.enabled?'aktiv':'inaktiv'}</b></span></div><GamStickyToolbar><button type="button" onClick={reload}>Aktualisieren</button><button className="secondary" type="button" onClick={sendTest}>Testmail senden</button><label><Search size={16}/><input placeholder="Protokoll suchen" value={q} onChange={e=>setQ(e.target.value)}/></label><button className="danger" type="button" onClick={()=>{localStorage.removeItem('gam.communication.protocol'); reload();}}>Lokales Protokoll leeren</button></GamStickyToolbar><CommunicationAssistant/></section><section className="card"><h2>Login-News aus GAM-1.0-News-Tabelle</h2>{loginNews?.enabled?<div className="note"><b>{loginNews.title||'Hinweis'}</b><br/>{loginNews.text}</div>:<p className="muted">Keine aktive Login-News gefunden.</p>}<GamScrollArea><table className="compact-table"><thead><tr><th>ID</th><th>News</th></tr></thead><tbody>{news.map((n:any)=><tr key={n.id||n.ID}><td>{n.id||n.ID}</td><td>{n.news||n.NEWS||n.text||JSON.stringify(n)}</td></tr>)}</tbody></table></GamScrollArea></section><section className="card"><h2>Kommunikationsprotokoll</h2><p className="muted">Enthält Toasts, Workflow-/Mailaktionen und anklickbare Änderungsdetails. Aktuell lokal im Browser, später als echte DB-Historie erweiterbar.</p><GamScrollArea tall><table className="compact-table"><thead><tr><th>Zeit</th><th>Typ</th><th>Meldung</th><th>Modul</th><th>Aktion</th></tr></thead><tbody>{filtered.map(p=><tr key={p.id} className={p.details?'clickable-row':''} onClick={()=>p.details&&setSelected(p)}><td>{p.createdAt}</td><td><span className={`badge gam-toast-${p.kind}`}>{p.kind}</span></td><td>{p.text}</td><td>{p.details?.module||'—'}</td><td>{p.details?.action||'—'}</td></tr>)}</tbody></table></GamScrollArea></section>{selected&&<div className="gam-modal-backdrop" onClick={()=>setSelected(null)}><section className="card gam-modal-card change-detail-card" onClick={e=>e.stopPropagation()}><div className="modal-title-row"><h2>Kommunikationsdetails</h2><button className="secondary icon-only" type="button" onClick={()=>setSelected(null)}>×</button></div><p className="muted"><b>{selected.details?.module}</b> · {selected.details?.action}<br/>Zeitpunkt: {selected.createdAt}{selected.details?.record?<> · Datensatz: {selected.details.record}</>:null}</p>{selected.details?.summary&&<p className="note">{selected.details.summary}</p>}{selected.details?.fields?.length?<table className="compact-table change-details-table"><thead><tr><th>Feld</th><th>Vorher</th><th>Nachher</th></tr></thead><tbody>{selected.details.fields.map((f,idx)=><tr key={idx}><td>{f.field}</td><td>{f.before}</td><td>{f.after}</td></tr>)}</tbody></table>:<p className="muted">Keine Detailfelder vorhanden.</p>}</section></div>}</section>;
}

function ModuleAdminPage(){
 const [catalogs,setCatalogs]=useState<MasterDataCatalog[]>([]);
 const [selected,setSelected]=useState('branches');
 const [q,setQ]=useState('');
 const [rows,setRows]=useState<Record<string,unknown>[]>([]);
 const [form,setForm]=useState<Record<string,unknown>>({});
 const [editId,setEditId]=useState<number|string|undefined>();
 const [msg,setMsg]=useState('');
 const [err,setErr]=useState('');
 const [adminModalOpen,setAdminModalOpen]=useState(false);
 const adminFormRef = useRef<HTMLElement | null>(null);
 const adminFirstInputRef = useRef<HTMLInputElement | null>(null);
 const scrollToAdminForm=(focus=false)=>{setAdminModalOpen(true); setTimeout(()=>{if(focus) adminFirstInputRef.current?.focus();},140);};
 const catalog = catalogs.find(c=>c.key===selected);
 useEffect(()=>{loadMasterDataCatalogs().then(c=>{setCatalogs(c); if(c.length && !c.find(x=>x.key===selected)) setSelected(c[0].key)}).catch((e:any)=>setErr(e.message??'Admin-Katalog konnte nicht geladen werden'))},[]);
 async function load(){ if(!selected) return; setErr(''); try{ const data = await loadMasterDataRows(selected,q,200); setRows(data.rows); }catch(e:any){ setErr(e.message??'Admin-Daten konnten nicht geladen werden'); }}
 useEffect(()=>{load()},[selected]);
 function manualPrimaryKey(c?:MasterDataCatalog){ return !!c && (c.primaryKey==='CODE' || c.primaryKey==='MANDANTENNUMMER' || c.tableName==='lager' || c.tableName==='kassenbuchoben'); }
 function editableFields(c?:MasterDataCatalog){ if(!c) return []; return manualPrimaryKey(c) ? [c.primaryKey, ...c.fields] : c.fields; }
 function startNew(){ setEditId(undefined); const next:Record<string,unknown>={}; editableFields(catalog).forEach(f=>next[f]=''); setForm(next); setMsg('Neuer Datensatz vorbereitet.'); scrollToAdminForm(true); }
 function startEdit(row:Record<string,unknown>){ if(!catalog) return; setEditId(row[catalog.primaryKey] as any); const next:Record<string,unknown>={}; editableFields(catalog).forEach(f=>next[f]=row[f]??''); setForm(next); setMsg(`Datensatz ${row[catalog.primaryKey] ?? ''} geladen.`); setAdminModalOpen(true); setTimeout(()=>adminFirstInputRef.current?.focus(),140); }
 async function save(){ if(!catalog) return; setErr(''); setMsg(''); try{ const before=editId===undefined?undefined:rows.find(r=>String(r[catalog.primaryKey])===String(editId)); if(editId===undefined) await createMasterDataRow(catalog.key, form); else await updateMasterDataRow(catalog.key, editId, form); setMsg('Gespeichert.'); gamNotify('success','Datensatz erfolgreich gespeichert.', undefined, buildChangeDetails('Modul-Administration', editId===undefined?'Datensatz angelegt':'Datensatz gespeichert', before, form, {}, editId===undefined?catalog.label:String(editId))); setAdminModalOpen(false); await load(); }catch(e:any){ const m=e.message??'Speichern fehlgeschlagen'; setErr(m); gamNotify('error',m,0); }}
 function fieldInputType(name:string){ const n=name.toLowerCase(); if(n.includes('datum')||n==='frist') return 'date'; if(n.includes('menge')||n.includes('anzahl')||n.includes('id')||n.includes('steuer')||n.includes('kosten')||n.includes('preis')||n.includes('bestand')||n.includes('intervall')) return 'number'; if(n.includes('email')) return 'email'; return 'text'; }
 function formValue(name:string){ const v=form[name]; return v==null?'':String(v); }
 return <section className="grid"><section className="card"><h2>Modul-Administration</h2><p className="muted">Schritt 38g2: Tabellenzeilen sind direkt anklickbar. Die separate Aktionsliste entfällt, damit kein Datensatz zweimal gesucht werden muss.</p>{err&&<b className="error">{err}</b>}{msg&&<p className="note">{msg}</p>}<GamStickyToolbar><label>Adminbereich<select value={selected} onChange={e=>{setSelected(e.target.value); setForm({}); setEditId(undefined);}}>{catalogs.map(c=><option key={c.key} value={c.key}>{c.module} · {c.label}</option>)}</select></label><label>Suche<input value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') load()}}/></label><button type="button" onClick={load}>Laden</button><button className="secondary" type="button" onClick={startNew}>Neu</button></GamStickyToolbar>{catalog&&<p className="muted"><b>{catalog.label}</b> · Tabelle <code>{catalog.tableName}</code> · Primärschlüssel <code>{catalog.primaryKey}</code><br/>{catalog.note}</p>}<GamScrollArea tall><GenericTable rows={rows.slice(0,120)} onRowClick={(row)=>startEdit(row)} primaryKey={catalog?.primaryKey}/></GamScrollArea></section>{adminModalOpen&&<div className="gam-modal-backdrop" onClick={()=>setAdminModalOpen(false)} />}<section className={adminModalOpen?"card admin-detail-card gam-modal-card":"card admin-detail-card gam-modal-card gam-modal-hidden"} ref={adminFormRef}><div className="modal-title-row"><h2>{editId===undefined?'Neuer Datensatz':'Datensatz bearbeiten'}</h2><button className="secondary icon-only" type="button" onClick={()=>setAdminModalOpen(false)} aria-label="Dialog schließen">×</button></div>{!catalog?<p className="muted">Kein Adminbereich gewählt.</p>:<><div className="newgrid">{editableFields(catalog).map((f,idx)=><label key={f}>{f}<input ref={idx===0?adminFirstInputRef:undefined} disabled={editId!==undefined && f===catalog.primaryKey} type={fieldInputType(f)} value={formValue(f)} onChange={e=>setForm(prev=>({...prev,[f]: e.target.value}))}/></label>)}</div><div className="download-actions"><button type="button" onClick={save}>{editId===undefined?'Anlegen':'Änderungen speichern'}</button><button className="secondary" type="button" onClick={()=>{setForm({}); setEditId(undefined);}}>Zurücksetzen</button><button className="secondary" type="button" onClick={()=>setAdminModalOpen(false)}>Abbrechen</button></div></>}</section></section>
}
function adminRowLabel(row:Record<string,unknown>, catalog?:MasterDataCatalog){
 if(!catalog) return '';
 const fields = catalog.searchFields.length ? catalog.searchFields : catalog.fields;
 return fields.slice(0,3).map(f=>row[f]).filter(v=>v!=null && String(v).trim()).map(v=>String(v)).join(' · ') || catalog.label;
}

function GenericTable({rows,onRowClick,primaryKey}:{rows:any[]; onRowClick?:(row:any)=>void; primaryKey?:string}){if(!rows?.length) return <p className="muted">Keine Daten gefunden.</p>; const mapped=(rows ?? []).map(r=>r.values??r); const keys=Array.from(new Set(mapped.flatMap((r:any)=>Object.keys(r)))).filter(k=>k!=='Aktion').slice(0,10); return <table className="compact-table"><thead><tr>{keys.map(k=><th key={k}>{k}</th>)}{onRowClick&&<th>Aktion</th>}</tr></thead><tbody>{mapped.map((r:any,idx:number)=><tr key={idx} className={onRowClick?"clickable-row":undefined} title={onRowClick?"Anklicken zum Bearbeiten":undefined} onClick={onRowClick?()=>onRowClick(r):undefined}>{keys.map(k=><td key={k}>{formatCell(r[k])}</td>)}{onRowClick&&<td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); onRowClick(r);}}>Bearbeiten</button></td>}</tr>)}</tbody></table>}
function formatCell(v:unknown){if(v==null) return '—'; if(typeof v==='object') return JSON.stringify(v); return String(v)}




// Schritt 38o: Preisliste als bearbeitbares GDS-Modul auf Basis der bestehenden Tabelle preisliste
const PRICELIST_CATALOG_KEY = 'pricelist';
const PRICELIST_PRIMARY_KEY = 'ID';
const priceListColumnLabels: Record<string,string> = {
  ID:'ID', ARTIKEL:'Artikel', LIEFERANT:'Lieferant', LETZTER_EINKAUFSPREIS:'Letzter Einkaufspreis',
  MONATLICHE_KOSTEN:'Monatliche Kosten', BEMERKUNG:'Bemerkung'
};
const priceListPreferredFields = ['ID','ARTIKEL','LIEFERANT','LETZTER_EINKAUFSPREIS','MONATLICHE_KOSTEN','BEMERKUNG'];
function priceListFieldType(name:string){
  const n=name.toUpperCase();
  if(n.includes('DATUM')) return 'date';
  if(n.includes('PREIS') || n.includes('KOSTEN') || n.includes('BETRAG') || n.includes('MWST') || n.includes('STEUER')) return 'number';
  return 'text';
}
function priceListFields(catalog:MasterDataCatalog|null){
  const available=catalog?.fields?.length ? catalog.fields : priceListPreferredFields.filter(f=>f!==PRICELIST_PRIMARY_KEY);
  const ordered=priceListPreferredFields.filter(f=>f===PRICELIST_PRIMARY_KEY || available.includes(f));
  return [...ordered, ...available.filter(f=>!ordered.includes(f))];
}
function priceListEditableFields(catalog:MasterDataCatalog|null){ return priceListFields(catalog).filter(f=>f!==PRICELIST_PRIMARY_KEY); }
function priceListEmpty(fields:string[]){ const row:Record<string,unknown>={}; fields.forEach(f=>row[f]=''); return row; }
function priceListLabel(row:Record<string,unknown>){
  const id=String(rowValue(row,PRICELIST_PRIMARY_KEY)??'').trim();
  const article=String(rowValue(row,'ARTIKEL')??'').trim();
  const supplier=String(rowValue(row,'LIEFERANT')??'').trim();
  return [article,supplier].filter(Boolean).join(' · ') || (id?`Preislisteneintrag #${id}`:'Neuer Preislisteneintrag');
}
function normalizePriceListValue(key:string,value:any){
  if(value==='' || value==null) return null;
  if(priceListFieldType(key)==='number'){
    const n=Number(String(value).replace(',','.'));
    return Number.isFinite(n) ? n : value;
  }
  return value;
}
function PriceListPage(){
 const [catalog,setCatalog]=useState<MasterDataCatalog|null>(null);
 const [rows,setRows]=useState<Record<string,unknown>[]>([]);
 const [q,setQ]=useState('');
 const [form,setForm]=useState<Record<string,unknown>>({});
 const [open,setOpen]=useState(false);
 const [err,setErr]=useState('');
 const firstRef=useRef<any>(null);
 const fields=priceListFields(catalog);
 const editable=priceListEditableFields(catalog);
 const monthlyTotal=rows.reduce((sum,r)=>sum+Number(rowValue(r,'MONATLICHE_KOSTEN')||0),0);
 const withSupplier=rows.filter(r=>String(rowValue(r,'LIEFERANT')??'').trim()).length;
 async function reload(search=q){
   setErr('');
   try{ const data=await loadMasterDataRows(PRICELIST_CATALOG_KEY, search, 300); setCatalog(data.catalog); setRows(data.rows||[]); }
   catch(e:any){ const m=e.message??'Preisliste konnte nicht geladen werden'; setErr(m); gamNotify('error',m,0); }
 }
 useEffect(()=>{reload('')},[]);
 function startNew(){ setForm(priceListEmpty(editable)); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
 function startEdit(row:Record<string,unknown>){ const next:Record<string,unknown>={}; fields.forEach(f=>next[f]=rowValue(row,f)??''); setForm(next); setOpen(true); setTimeout(()=>firstRef.current?.focus(),120); }
 function setField(key:string,value:any){ setForm(prev=>({...prev,[key]:value})); }
 async function save(){
   const id=rowValue(form,PRICELIST_PRIMARY_KEY);
   const payload:Record<string,unknown>={};
   editable.forEach(f=>{ payload[f]=normalizePriceListValue(f,form[f]); });
   const before=id?rows.find(r=>String(rowValue(r,PRICELIST_PRIMARY_KEY))===String(id)):undefined;
   try{
     const saved=id ? await updateMasterDataRow(PRICELIST_CATALOG_KEY, String(id), payload) : await createMasterDataRow(PRICELIST_CATALOG_KEY, payload);
     setOpen(false); await reload(q);
     notifySaved(id?'Preislisteneintrag gespeichert.':'Preislisteneintrag angelegt.', buildChangeDetails('Preisliste', id?'Preislisteneintrag gespeichert':'Preislisteneintrag angelegt', before, saved, priceListColumnLabels, priceListLabel(saved)));
   }catch(e:any){ gamNotify('error', e.message??'Preislisteneintrag konnte nicht gespeichert werden', 0); }
 }
 async function remove(){
   const id=rowValue(form,PRICELIST_PRIMARY_KEY);
   if(!id) return;
   if(!confirm(`Preislisteneintrag wirklich löschen?\n\n${priceListLabel(form)}`)) return;
   try{ await deleteMasterDataRow(PRICELIST_CATALOG_KEY, String(id)); setOpen(false); await reload(q); gamNotify('success','Preislisteneintrag gelöscht.'); }
   catch(e:any){ gamNotify('error', e.message??'Preislisteneintrag konnte nicht gelöscht werden', 0); }
 }
 return <section className="card"><h2>Preisliste</h2><p className="muted">Schritt 38o: Preisliste als bearbeitbares GDS-Modul auf Basis der bestehenden Tabelle <code>preisliste</code>. Es werden vorerst ausschließlich die vorhandenen Spalten umgesetzt.</p>{err&&<b className="error">{err}</b>}<div className="stats"><span>Datensätze<br/><b>{rows.length}</b></span><span>Mit Lieferant<br/><b>{withSupplier}</b></span><span>Monatliche Kosten<br/><b>{money(monthlyTotal)}</b></span><span>Tabelle<br/><b>{catalog?.tableName||'preisliste'}</b></span></div><GamStickyToolbar><button type="button" onClick={startNew}><FilePlus2 size={16}/> Neuer Preislisteneintrag</button><label><Search size={16}/><input placeholder="Artikel, Lieferant oder Bemerkung suchen" value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==='Enter') reload(q)}}/></label><button className="secondary" type="button" onClick={()=>reload(q)}>Suchen / Aktualisieren</button></GamStickyToolbar><GamScrollArea tall><table className="compact-table"><thead><tr>{fields.map(k=><th key={k}>{priceListColumnLabels[k]||k}</th>)}<th>Aktion</th></tr></thead><tbody>{rows.length?rows.map((r,idx)=><tr key={String(rowValue(r,PRICELIST_PRIMARY_KEY)||idx)} className="clickable-row" onClick={()=>startEdit(r)}>{fields.map(k=><td key={k}>{renderInvoiceAdminCell(k,rowValue(r,k))}</td>)}<td><button className="secondary" type="button" onClick={(e)=>{e.stopPropagation(); startEdit(r);}}>Bearbeiten</button></td></tr>):<tr><td colSpan={fields.length+1} className="muted">Keine Preislisteneinträge gefunden.</td></tr>}</tbody></table></GamScrollArea><GamDialog open={open} title={priceListLabel(form)} onClose={()=>setOpen(false)} size="large" firstFocusRef={firstRef} footer={<><button type="button" onClick={save}>{rowValue(form,PRICELIST_PRIMARY_KEY)?'Änderungen speichern':'Preislisteneintrag anlegen'}</button>{rowValue(form,PRICELIST_PRIMARY_KEY)&&<button className="danger" type="button" onClick={remove}>Preislisteneintrag löschen</button>}<button className="secondary" type="button" onClick={()=>setOpen(false)}>Abbrechen</button></>}><div className="newgrid">{editable.map((key,idx)=><label key={key} className={key==='BEMERKUNG'?'wide-field':undefined}>{priceListColumnLabels[key]||key}{key==='BEMERKUNG'?<textarea ref={idx===0?firstRef:undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>:<input ref={idx===0?firstRef:undefined} type={priceListFieldType(key)} step={priceListFieldType(key)==='number'?'0.01':undefined} value={String(form[key]??'')} onChange={e=>setField(key,e.target.value)}/>}</label>)}</div></GamDialog></section>;
}

// Schritt 9: Inventar/Lager-Verbindung
function InventoryWarehousePage(){const [links,setLinks]=useState<any[]>([]); const [mov,setMov]=useState<any[]>([]); const [materialId,setMaterialId]=useState<number>(1); const [deviceId,setDeviceId]=useState<number|undefined>(); const [qty,setQty]=useState(1); const [reason,setReason]=useState('GAM 2.0 Lagerbuchung'); const [msg,setMsg]=useState(''); const reload=()=>{loadDeviceMaterialLinks().then(setLinks).catch(()=>{}); loadMaterialMovements(80).then(setMov).catch(()=>{})}; useEffect(reload,[]); async function submit(direction:string){setMsg(''); try{await bookMaterial({deviceId, materialId, quantity:qty, direction, reason}); setMsg('Buchung gespeichert.'); gamNotify('success','Materialbuchung erfolgreich gespeichert.', undefined, {module:'Inventar ↔ Lager', action:'Materialbuchung', record: deviceId?`Gerät #${deviceId} / Material #${materialId}`:`Material #${materialId}`, fields:[{field:'Richtung', before:'—', after:direction},{field:'Menge', before:'—', after:String(qty)},{field:'Grund', before:'—', after:reason||'—'}], summary:'Materialbewegung wurde gespeichert.'}); reload();}catch(e:any){const m=e.message??'Buchung fehlgeschlagen'; setMsg(m); gamNotify('error',m,0)}} return <section className="grid"><section className="card"><h2>Inventar ↔ Lager</h2><p className="muted">Schritt 9 verbindet Geräte, Verbrauchsmaterial und Bestandsbewegungen. Bei Buchung mit Geräte-ID wird die Materialzuordnung automatisch angelegt.</p><div className="newgrid sticky-actionbar"><label>Geräte-ID optional<input type="number" value={deviceId??''} onChange={e=>setDeviceId(e.target.value?Number(e.target.value):undefined)}/></label><label>Material-ID<input type="number" value={materialId} onChange={e=>setMaterialId(Number(e.target.value))}/></label><label>{ui("quantity")}<input type="number" value={qty} onChange={e=>setQty(Number(e.target.value))}/></label><label>{ui("reason")}<input value={reason} onChange={e=>setReason(e.target.value)}/></label><button onClick={()=>submit('OUT')}>Material entnehmen</button><button className="secondary" onClick={()=>submit('IN')}>Material Zugang</button></div>{msg&&<p className="note">{msg}</p>}<h3>Zuordnungen</h3><div className="scroll-table"><table><thead><tr><th>Gerät</th><th>Material</th><th>Typ</th><th>Bestand</th></tr></thead><tbody>{links.slice(0,80).map(l=><tr key={l.linkId}><td>{l.deviceName} #{l.deviceId}</td><td>{l.materialName} #{l.materialId}</td><td>{l.materialType??'—'}</td><td>{l.stock??'—'}</td></tr>)}</tbody></table></div></section><section className="card"><h2>Materialbewegungen</h2><div className="scroll-table scroll-table-tall"><table><thead><tr><th>Zeit</th><th>Gerät</th><th>Material</th><th>Δ</th><th>Bestand</th><th>{ui("reason")}</th></tr></thead><tbody>{mov.map(m=><tr key={m.id}><td>{m.createdAt}</td><td>{m.deviceName??'—'}</td><td>{m.materialName}</td><td>{m.delta}</td><td>{m.previousStock??'—'} → {m.newStock??'—'}</td><td>{m.reason??'—'}</td></tr>)}</tbody></table></div></section></section>}


class PreviewErrorBoundary extends React.Component<{children: React.ReactNode}, {hasError:boolean; message:string}> {
  constructor(props:{children: React.ReactNode}) {
    super(props);
    this.state = {hasError:false, message:""};
  }
  static getDerivedStateFromError(error:any) {
    return {hasError:true, message: error?.message ?? "Vorschau konnte nicht gerendert werden"};
  }
  componentDidCatch(error:any) {
    console.error("Invoice preview crashed", error);
  }
  render() {
    if (this.state.hasError) {
      return <div className="note warn">Die erweiterte Rechnungsvorschau konnte nicht angezeigt werden. Die Rechnungsliste bleibt nutzbar. Details: {this.state.message}</div>;
    }
    return this.props.children;
  }
}

function invoiceLabel(number?: string, lang: GamLanguage = currentUiLanguage()){
  if(!number) return ui('invoice', lang);
  if(number.endsWith('S')) return `${ui('cancelInvoice', lang)} ${number}`;
  if(number.endsWith('G')) return `${ui('creditNote', lang)} ${number}`;
  if(number.endsWith('P')) return `Proforma-${ui('invoiceTypeInvoice', lang)} ${number}`;
  if(/Z\d*$/.test(number)) return `${ui('invoiceTypePaymentAdvice', lang)} ${number}`;
  return `${ui('invoiceTypeInvoice', lang)} ${number}`;
}
function money(v?:number){return v==null?'—':new Intl.NumberFormat('de-DE',{style:'currency',currency:'EUR'}).format(v)}
createRoot(document.getElementById('root')!).render(<App/>);
