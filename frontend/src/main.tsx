import React, {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {Download, FilePlus2, FileText, LogOut, Search, ShieldCheck, UserRound, UsersRound, LayoutDashboard, Package, Warehouse, CheckSquare, ClipboardCheck, BriefcaseBusiness, Landmark, FileBarChart, ClipboardList, KeyRound, QrCode, Smartphone} from 'lucide-react';
import {QRCodeSVG} from 'qrcode.react';
import {AccountAdminDto, AccountDto, InventoryDevice, InventoryDeviceDetail, InventoryStats, WarehouseItem, WarehouseStats, InvoiceCompany, InvoiceCreateLineRequest, InvoiceDetail, InvoiceSummary, LbdRecipient, ProductDto, RoleDto, SystemStatus, calculateInvoice, createInvoice, deleteInvoiceDraft, loadAccounts, loadCompanies, loadDraft, loadExportCheck, loadGamApprovals, loadGamCashbook, loadGamCompliance, loadGamModules, loadGamPersonnel, loadGamReportSummary, loadGamTasks, loadInventoryDevice, loadInventoryDevices, loadInventoryStats, loadWarehouseItems, loadWarehouseStats, updateWarehouseStock, loadInvoice, loadInvoices, loadLbdPreview, loadMenu, loadNextInvoiceNumber, loadProducts, loadRoles, loadSystemStatus, login, logout, me, pdfUrl, token, updateAccount, updateInvoice, updateInvoiceStatus, createCancellationInvoice, createCreditNote, createProformaInvoice, zugferdXmlUrl, loadDeviceMaterialLinks, loadMaterialMovements, bookMaterial, setupTotp, confirmTotp, loadPasskeyStatus, passkeyRegisterOptions, passkeyRegisterFinish, passkeyLoginOptions, passkeyLoginFinish, loadInvoiceTextPreview, InvoiceTextPreview, loadInvoiceAccess, invoiceAccessQrUrl, loadInvoiceReportRows, loadInvoiceReportSummary, downloadInvoiceReport, InvoiceReportRow, InvoiceReportSummary, InvoiceTotals, loadTtsAudio, loadTtsStatus, loadUiTranslations, loadUiTranslationsLive} from './api/client';
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
    demoReadOnlyShell: "Lesemodus-Shell",
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
    zugferdPdf: "ZUGFeRD-PDF",
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
    step31ModuleOverview: "Schritt 31 stellt alle historischen GAM-Anwendungen sichtbar dar. Vollständig migrierte Bereiche sind nutzbar, noch offene Module erscheinen bewusst als Lesemodus-Shells.",
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
    moduleVisibilityHint: "Alle historischen GAM-Anwendungen sind sichtbar; noch nicht vollständig migrierte Module starten im Lesemodus.",
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
    language: "Sprache",
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
    demoReadOnlyShell: "read-only shell",
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
    zugferdPdf: "ZUGFeRD PDF",
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
    step31ModuleOverview: "Step 31 makes all historical GAM applications visible. Fully migrated areas are usable; open modules are intentionally shown as read-only shells.",
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
    moduleVisibilityHint: "All historical GAM applications are visible; modules that are not fully migrated yet start in read-only mode.",
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
    zugferdPdf: "PDF ZUGFeRD",
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
    moduleVisibilityHint: "Toutes les applications historiques de GAM sont visibles ; les modules pas encore entièrement migrés démarrent en mode lecture seule.",
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
    zugferdPdf: "ZUGFeRD PDF",
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
    moduleVisibilityHint: "Усі історичні застосунки GAM видимі; модулі, які ще не повністю перенесені, запускаються в режимі перегляду.",
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
    moduleVisibilityHint: "Tutte le applicazioni GAM storiche sono visibili; i moduli non ancora completamente migrati si avviano in modalità sola lettura.",
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
    moduleVisibilityHint: "Alla historiska GAM-program är synliga; moduler som ännu inte är helt migrerade startar i läsläge.",
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
    moduleVisibilityHint: "Tüm geçmiş GAM uygulamaları görünür; henüz tamamen taşınmamış modüller salt okunur modda başlar.",
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
    moduleVisibilityHint: "Все исторические приложения GAM видимы; модули, которые еще не полностью перенесены, запускаются в режиме чтения.",
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
  const v = raw.toLowerCase();
  if (v === "invoices") return "invoice";
  if (v === "users" || v === "usersrights" || v === "users/rights" || v === "benutzer/rechte") return "usersRights";
  if (v === "pricelist") return "price";
  if (v === "approvals") return "approval";
  if (["invoice","inventory","warehouse","cashbook","tasks","approval","orders","personnel","workplace","price","reports","admin","dashboard","checks","usersRights","compliance"].includes(v)) return v;
  if (v.includes("rechnung")) return "invoice";
  if (v.includes("gerät") || v.includes("geraet") || v.includes("device")) return "inventory";
  if (v.includes("lager") || v.includes("warehouse")) return "warehouse";
  if (v.includes("kasse") || v.includes("cash")) return "cashbook";
  if (v.includes("aufgabe") || v.includes("task")) return "tasks";
  if (v.includes("freigabe") || v.includes("approval")) return "approval";
  if (v.includes("bestell") || v.includes("order")) return "orders";
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
    demoReadOnlyShell: "Lesemodus-Shell",
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
    zugferdPdf: "ZUGFeRD-PDF",
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
    step31ModuleOverview: "Schritt 31 stellt alle historischen GAM-Anwendungen sichtbar dar. Vollständig migrierte Bereiche sind nutzbar, noch offene Module erscheinen bewusst als Lesemodus-Shells.",
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
    moduleVisibilityHint: "Alle historischen GAM-Anwendungen sind sichtbar; noch nicht vollständig migrierte Module starten im Lesemodus.",
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
    demoReadOnlyShell: "read-only shell",
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
    zugferdPdf: "ZUGFeRD PDF",
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
    step31ModuleOverview: "Step 31 makes all historical GAM applications visible. Fully migrated areas are usable; open modules are intentionally shown as read-only shells.",
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
    moduleVisibilityHint: "All historical GAM applications are visible; modules that are not fully migrated yet start in read-only mode.",
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
    zugferdPdf: "PDF ZUGFeRD",
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
    moduleVisibilityHint: "Toutes les applications historiques de GAM sont visibles ; les modules pas encore entièrement migrés démarrent en mode lecture seule.",
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
    zugferdPdf: "ZUGFeRD PDF",
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
    moduleVisibilityHint: "Усі історичні застосунки GAM видимі; модулі, які ще не повністю перенесені, запускаються в режимі перегляду.",
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
  if (["invoice","inventory","warehouse","cashbook","tasks","approval","orders","personnel","workplace","price","reports","admin","dashboard","checks","usersRights","compliance"].includes(value)) return value;
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

type Page = 'dashboard'|'invoices'|'inventory'|'warehouse'|'users'|'tasks'|'approvals'|'personnel'|'cashbook'|'compliance'|'reports'|'orders'|'priceList'|'workplace';
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
const LOGIN_APPLICATIONS = ['Rechnungsprogramm','Geräteverzeichnis','Lagerverwaltung','Kassenbuch','Aufgabenverwaltung','Freigabemanagement','Bestelltool','Personaldaten','Arbeitsplatzausstattung','Preisliste','Reports','Administration'];
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
    ['module.inventory', 'Geräteverzeichnis'],
    ['module.warehouse', 'Lagerverwaltung'],
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
    ['moduleVisibilityHint', 'Alle historischen GAM-Anwendungen sind sichtbar; noch nicht vollständig migrierte Module starten im Lesemodus.'],
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
    ['readonly.orders.description', 'Historisches GAM-Modul für Beschaffung und Bestellungen. Schritt 31 zeigt das Modul bereits als Lesemodus-Shell; Schreibfunktionen folgen nach Rekonstruktion der Alt-GAM-Fachlogik.'],
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
    ['module.inventory', moduleText(lang, 'inventory')],
    ['module.warehouse', moduleText(lang, 'warehouse')],
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
  if (id === 1 || name.includes('amae')) return '/images/logo_AMAE_blau.png';
  if (id === 2 || id === 3 || name.includes('acqua') || name.includes('aqua')) return '/images/logo_ACQUA_blau.png';
  if (id === 6 || name.includes('healthcode')) return '/images/Healthcode_logo_blau.png';
  return '/images/KOPFZENTRUM_LOGO.png';
}


function Login({onLogin}:{onLogin:()=>void}) {
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
const [selectedApplication,setSelectedApplication]=useState('Rechnungsprogramm');
  function changeUiLanguage(value: GamLanguage){ setUiLanguage(value); localStorage.setItem('gam_ui_language', value); localStorage.setItem('gam.uiLanguage', value); window.dispatchEvent(new CustomEvent('gam-ui-language-changed', {detail:value})); }

  useEffect(()=>{
    if (retryAfter <= 0) return;
    const timer = window.setInterval(()=>setRetryAfter(v=>Math.max(0, v-1)), 1000);
    return ()=>window.clearInterval(timer);
  },[retryAfter]);

  function handleLoginError(ex:any, fallback:string) {
    if (ex?.retryAfterSeconds) {
      setRetryAfter(Number(ex.retryAfterSeconds));
      setErr(`${ui('tooManyAttempts')} ${Number(ex.retryAfterSeconds)} ${ui('seconds')}.`);
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
      setInfo(r.alreadyConfigured ? ui('setupExisting') : ui('setupCreated'));
    }catch(ex:any){setErr(ex.message??'2FA-Registrierung konnte nicht gestartet werden');}
  }

  async function confirmTotpSetup(){
    setErr(''); setInfo('');
    if(!setup){setErr(ui('setupFirst')); return;}
    try{
      await confirmTotp(username,setup.secret,totp);
      setInfo(ui('totpSaved'));
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
    if(!username.trim()){setErr(ui('usernameRequired')); return;}
    if(!window.PublicKeyCredential){setErr(ui('passkeyUnsupported')); return;}
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
    if(!window.PublicKeyCredential){setErr(ui('passkeyUnsupported')); return;}
    try{
      const opts = await passkeyLoginOptions(username.trim());
      if(!opts.allowCredentialIds?.length){setErr(ui('noPasskey')); return;}
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

  return <main className="login"><section className="card login-card"><img className="login-logo" src="/images/GAM.png" alt="GAM 2.0" /><h1>GAM 2.0</h1><p>{ui("legacyLoginHint")}</p><label className="language-select">{ui("uiLanguage")}<select value={uiLanguage} onChange={e=>changeUiLanguage(e.target.value as GamLanguage)}>{LANGUAGES.map(l=><option key={l.value} value={l.value}>{l.label}</option>)}</select></label><div className="login-application-select"><span>{ui("chooseApplication")}</span><div className="login-app-buttons">{LOGIN_APPLICATIONS.map(app=><button type="button" key={app} className={selectedApplication===app?'active':''} onClick={()=>setSelectedApplication(app)}><img src={iconForModule(app)} alt="" className="module-button-icon" /> <span>{uiModule(app)}</span></button>)}</div><small>{ui("moduleVisibilityHint")}</small></div><nav className="tabs login-tabs"><button type="button" className={tab==='password'?'active':''} onClick={()=>setTab('password')}><ShieldCheck size={16}/> {ui("passwordLogin")}</button><button type="button" className={tab==='totp-register'?'active':''} onClick={()=>setTab('totp-register')}><QrCode size={16}/> {ui("totpRegister")}</button><button type="button" className={tab==='totp-login'?'active':''} onClick={()=>setTab('totp-login')}><Smartphone size={16}/> {ui("totpLogin")}</button><button type="button" className={tab==='passkey-register'?'active':''} onClick={()=>{setTab('passkey-register'); passkeyInfo();}}><KeyRound size={16}/> {ui("passkeyRegister")}</button><button type="button" className={tab==='passkey-login'?'active':''} onClick={()=>{setTab('passkey-login'); passkeyInfo();}}><KeyRound size={16}/> {ui("passkeyLogin")}</button></nav>
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

function App(){const [authed,setAuthed]=useState(!!token()); const [uiLanguage,setUiLanguage]=useState<GamLanguage>(currentUiLanguage()); useEffect(()=>{const h=()=>setAuthed(false); window.addEventListener("gam-auth-expired",h); return()=>window.removeEventListener("gam-auth-expired",h)},[]); useEffect(()=>{const h=(e:any)=>setUiLanguage(normalizeGamLanguage(e?.detail || currentUiLanguage())); window.addEventListener('gam-ui-language-changed', h as any); window.addEventListener('gam-ui-translations-loaded', h as any); window.addEventListener('storage', h as any); return()=>{window.removeEventListener('gam-ui-language-changed', h as any); window.removeEventListener('gam-ui-translations-loaded', h as any); window.removeEventListener('storage', h as any);};},[]); return authed?<Shell uiLanguage={uiLanguage} onLogout={()=>{logout();setAuthed(false)}}/>:<Login onLogin={()=>{setUiLanguage(currentUiLanguage());setAuthed(true)}}/>}

function effectiveModules(account: AccountDto|null, menu: RoleDto|null): string[] {
  const fallback = ['dashboard','invoices','inventory','warehouse','tasks','approvals','orders','personnel','cashbook','workplace','priceList','compliance','reports','users'];
  const roleText = `${account?.role ?? ''} ${menu?.label ?? ''}`.toLowerCase();
  const isSuperAdmin = roleText.includes('super') || roleText.includes('admin');
  const fromMenu = menu?.modules ?? [];
  if (isSuperAdmin) return Array.from(new Set([...fallback, ...fromMenu]));
  return fromMenu.length ? fromMenu : ['dashboard'];
}


function fallbackSuperadminMenu(): RoleDto {
  return {
    label: 'Super-Administration',
    modules: ['dashboard','invoices','inventory','warehouse','tasks','approvals','orders','personnel','cashbook','workplace','priceList','compliance','reports','users']
  } as RoleDto;
}

function Shell({onLogout,uiLanguage}:{onLogout:()=>void; uiLanguage:GamLanguage}){const [account,setAccount]=useState<AccountDto|null>(null); const [menu,setMenu]=useState<RoleDto|null>(null); const [page,setPage]=useState<Page>('dashboard');
 useUiTranslationCache(uiLanguage, true, 0);
 useEffect(()=>{me().then(setAccount).catch(()=>{logout(); onLogout();}); loadMenu().then(m=>{setMenu(m); if((m.modules??[]).includes('invoices')) setPage('invoices')}).catch(()=>{const fallback=fallbackSuperadminMenu(); setMenu(fallback); setPage('invoices');})},[]);
 const modules=effectiveModules(account, menu);
 const nav=[['dashboard','Dashboard',LayoutDashboard],['invoices','Rechnungsprogramm',FileText],['inventory','Geräteverzeichnis',Package],['warehouse','Lagerverwaltung',Warehouse],['tasks','Aufgabenverwaltung',CheckSquare],['approvals','Freigabemanagement',ClipboardCheck],['orders','Bestelltool',ClipboardList],['personnel','Personaldaten',BriefcaseBusiness],['cashbook','Kassenbuch',Landmark],['workplace','Arbeitsplatzausstattung',Package],['priceList','Preisliste',FileText],['compliance','Prüfungen',ClipboardList],['reports','Reports',FileBarChart],['users','Benutzer/Rechte',UsersRound]] as const;
 return <main><header><div><h1>GAM 2.0</h1><span>{account?.fullname||account?.username} · {ui('role', uiLanguage)}: {menu?.label||account?.role||'—'} · {ui('step31ModuleOverviewShort', uiLanguage)}</span></div><button className="secondary" onClick={onLogout}><LogOut size={16}/> {ui("logout", uiLanguage)}</button></header><nav className="tabs module-tabs">{nav.map(([key,label,Icon])=><button key={key} className={page===key?'active':''} onClick={()=>setPage(key as Page)}><img src={iconForModule(label)} alt="" className="module-button-icon nav-module-icon" />{moduleText(uiLanguage, label)}</button>)}</nav>{page==='dashboard'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'dashboard')}><DashboardHome/></ModuleErrorBoundary>}{page==='invoices'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'invoices')}><InvoicesPage/></ModuleErrorBoundary>}{page==='users'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'admin')}><UsersPage/></ModuleErrorBoundary>}{page==='inventory'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'inventory')}><InventoryPage/></ModuleErrorBoundary>}{page==='warehouse'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'warehouse')}><><WarehousePage/><InventoryWarehousePage/></></ModuleErrorBoundary>}{page==='tasks'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'tasks')}><RecordsPage title={moduleText(uiLanguage,'tasks')} loader={loadGamTasks}/></ModuleErrorBoundary>} {page==='approvals'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'approval')}><RecordsPage title={moduleText(uiLanguage,'approval')} loader={loadGamApprovals}/></ModuleErrorBoundary>} {page==='orders'&&<ReadOnlyModuleShell title='Bestelltool' description='Historisches GAM-Modul für Beschaffung und Bestellungen. Schritt 31 zeigt das Modul bereits als Lesemodus-Shell; Schreibfunktionen folgen nach Rekonstruktion der Alt-GAM-Fachlogik.'/>}{page==='personnel'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'personnel')}><RecordsPage title={moduleText(uiLanguage,'personnel')} loader={loadGamPersonnel}/></ModuleErrorBoundary>} {page==='cashbook'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'cashbook')}><RecordsPage title={moduleText(uiLanguage,'cashbook')} loader={loadGamCashbook}/></ModuleErrorBoundary>} {page==='workplace'&&<ReadOnlyModuleShell title='Arbeitsplatzausstattung' description='Historisches Modul für Arbeitsplatz-, Raum- und Geräteausstattung. In Schritt 31 bewusst sichtbar, aber noch ohne Bearbeitungsfunktionen.'/>}{page==='priceList'&&<ReadOnlyModuleShell title='Preisliste' description='Historische Preislisten- und Produktübersicht. Administration und Bearbeitung werden später separat rekonstruiert.'/>}{page==='compliance'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'checks')}><CompliancePage/></ModuleErrorBoundary>} {page==='reports'&&<ModuleErrorBoundary title={moduleText(uiLanguage,'reports')}><ReportsPage/></ModuleErrorBoundary>}</main>}
function Placeholder({title,text}:{title:string;text:string}){return <section className="card"><h2>{title}</h2><p className="muted">{text}</p></section>}
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
    lbdMissingPlaceholder: "lbd – .lbd-Empfängerdatei wurde nicht gefunden; Export nutzt Platzhalter.", demoReadOnlyShell:'Lesemodus-Shell', proformaFailed:'Proforma konnte nicht erstellt werden', saveFailed:'Speichern fehlgeschlagen', selectAtLeastOneLine:'Bitte mindestens eine Position auswählen.', mandatoryZugferd:'ZUGFeRD/Factur-X ist Pflicht-Export.', loadingNumber:'wird geladen', gross:'Brutto', net:'Netto', remove:'Entfernen', addPosition:'+ Position übernehmen', quantity:'Menge', product:'Produkt', installmentCount:'Ratenanzahl', amount:'Betrag', percent:'Prozent', discountValue:'Rabattwert', discountType:'Rabattart', voucherAmount:'Gutscheinbetrag', voucherText:'Gutscheintext', remark:'Bemerkung', reason:'Grund', paymentMethod:'Zahlungsart', paymentCash:'Barzahlung', paymentCard:'Kartenzahlung', paymentTransfer:'Überweisung', paymentUnknown:'unbekannt', invoiceDate:'Rechnungsdatum', saveInvoice:'Rechnung speichern', editChangesSave:'Änderungen speichern', cancelInvoice:'Stornorechnung', createCreditNote:'Gutschrift erstellen', creditNote:'Gutschrift', invoiceTypeInvoice:'Rechnung', technicalDelete:'Technisch löschen', accessLinkError:'Abruflink konnte nicht erzeugt werden', qrAltPortal:'QR-Code Rechnungsportal', openPortal:'Portal öffnen', patientPortalHint:'QR-Code für Patientenabruf mit Sprachwahl und Rechnungshistorie.', patientPortalDigital:'Digitales Rechnungsportal', zugferdIssues:'ZUGFeRD-Export hat Hinweise', zugferdReady:'ZUGFeRD-Export bereit', exportCheckLoading:'Exportprüfung wird geladen.', xml:'XML', zugferdPdf:'ZUGFeRD-PDF', pdfLanguage:'PDF-Sprache', pleaseSelectInvoice:'Bitte links eine Rechnung auswählen.', search:'Suchen', searchPlaceholder:'Suche Nummer / Name / Grund', pleaseChoose:'Bitte wählen', pleaseSelectCompany:'Bitte zuerst eine Gesellschaft auswählen.', invoiceEdit:'Rechnung bearbeiten', usersRights:'Benutzer/Rechte', checks:'Prüfungen', role:'Rolle', logout:'Logout', step31ModuleOverview:'Schritt 31 stellt alle historischen GAM-Anwendungen sichtbar dar. Vollständig migrierte Bereiche sind nutzbar, noch offene Module erscheinen bewusst als Lesemodus-Shells.', statusLoading:'Status wird geladen.', notFound:'nicht gefunden', found:'gefunden', notConnected:'nicht verbunden', connected:'verbunden', lbd:'.lbd', accounts:'Accounts', db:'DB',discount:'Rabatt', voucher:'Gutschein', voucherEnable:'Gutschein', discountEnable:'Rabatt', installmentsEnable:'Ratenzahlung', rateSingular:'Rate', ratePlural:'Raten', reducedTotal:'Endbetrag nach Abzug', installments:'Ratenzahlung', approx:'Raten à ca.', previewTitle:'Verbindliche Rechnungsvorschau', previewHelp:'Diese Vorschau soll dem späteren PDF entsprechen: Texte, Positionen, Rabatt/Gutschein, Ratenzahlung, Hinweise und Bankdaten.', readAloud:'Rechnung vorlesen', stopReading:'Vorlesen stoppen', accessibilityNote:'PDF/UA-Vorbereitung: Sprache, Titel, Metadaten und Lesereihenfolge werden gesetzt.', invoice:'Rechnung', company:'Gesellschaft', qty:'Menge', code:'Code', description:'Beschreibung', tax:'MwSt', price:'Preis', lineTotal:'Gesamt', noLines:'Noch keine Positionen übernommen.', total:'Gesamt', recipient:'Empfänger', date:'Datum', language:'Sprache', payment:'Zahlungsart', notes:'Hinweise', bank:'Bankverbindung', iban:'IBAN', bic:'BIC', taxNo:'Steuer/VAT', noRecipient:'Keine Empfängerdatei geladen'},
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
    lbdMissingPlaceholder: "lbd – .lbd recipient file was not found; export uses placeholders.", demoReadOnlyShell:'read-only shell', proformaFailed:'Proforma could not be created', saveFailed:'Saving failed', selectAtLeastOneLine:'Please select at least one item.', mandatoryZugferd:'ZUGFeRD/Factur-X is the mandatory export.', loadingNumber:'loading', gross:'Gross', net:'Net', remove:'Remove', addPosition:'+ Add item', quantity:'Quantity', product:'Product', installmentCount:'Number of installments', amount:'Amount', percent:'Percent', discountValue:'Discount value', discountType:'Discount type', voucherAmount:'Voucher amount', voucherText:'Voucher text', remark:'Remark', reason:'Reason', paymentMethod:'Payment method', paymentCash:'Cash payment', paymentCard:'Card payment', paymentTransfer:'Bank transfer', paymentUnknown:'unknown', invoiceDate:'Invoice date', saveInvoice:'Save invoice', editChangesSave:'Save changes', cancelInvoice:'Cancellation invoice', createCreditNote:'Create credit note', creditNote:'Credit note', invoiceTypeInvoice:'Invoice', technicalDelete:'Technical delete', accessLinkError:'Access link could not be created', qrAltPortal:'Invoice portal QR code', openPortal:'Open portal', patientPortalHint:'QR code for patient access with language selection and invoice history.', patientPortalDigital:'Digital invoice portal', zugferdIssues:'ZUGFeRD export has notes', zugferdReady:'ZUGFeRD export ready', exportCheckLoading:'Loading export check.', xml:'XML', zugferdPdf:'ZUGFeRD PDF', pdfLanguage:'PDF language', pleaseSelectInvoice:'Please select an invoice on the left.', search:'Search', searchPlaceholder:'Search number / name / reason', pleaseChoose:'Please choose', pleaseSelectCompany:'Please select a company first.', invoiceEdit:'Edit invoice', usersRights:'Users/permissions', checks:'Checks', role:'Role', logout:'Logout', step31ModuleOverview:'Step 31 makes all historical GAM applications visible. Fully migrated areas are usable; open modules are intentionally shown as read-only shells.', statusLoading:'Loading status.', notFound:'not found', found:'found', notConnected:'not connected', connected:'connected', lbd:'.lbd', accounts:'Accounts', db:'DB',discount:'Discount', voucher:'Voucher', voucherEnable:'Voucher', discountEnable:'Discount', installmentsEnable:'Installments', rateSingular:'installment', ratePlural:'installments', reducedTotal:'Total after deduction', installments:'Installment payment', approx:'installments of approx.', previewTitle:'Binding invoice preview', previewHelp:'This preview should match the later PDF: texts, items, discount/voucher, installments, notices and bank details.', readAloud:'Read invoice aloud', stopReading:'Stop reading', accessibilityNote:'PDF/UA preparation: language, title, metadata and reading order are set.', invoice:'Invoice', company:'Company', qty:'Quantity', code:'Code', description:'Description', tax:'VAT', price:'Price', lineTotal:'Total', noLines:'No items have been added yet.', total:'Total', recipient:'Recipient', date:'Date', language:'Language', payment:'Payment method', notes:'Notes', bank:'Bank details', iban:'IBAN', bic:'BIC', taxNo:'Tax/VAT', noRecipient:'No recipient file loaded'},
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



function UsersPage(){return <RecordsPage title={moduleText(currentUiLanguage(),'admin')} loader={async()=>[]}/>;}
function InventoryPage(){return <RecordsPage title={moduleText(currentUiLanguage(),'inventory')} loader={async()=>[]}/>;}
function WarehousePage(){return <RecordsPage title={moduleText(currentUiLanguage(),'warehouse')} loader={async()=>[]}/>;}

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
 const [rows,setRows]=useState<InvoiceSummary[]>([]); const [selected,setSelected]=useState<InvoiceDetail|null>(null); const [filter,setFilter]=useState(''); const [mode,setMode]=useState<'search'|'new'|'edit'>('search'); const [companies,setCompanies]=useState<InvoiceCompany[]>([]); const [products,setProducts]=useState<ProductDto[]>([]); const [lbd,setLbd]=useState<LbdRecipient|null>(null); const [textPreview,setTextPreview]=useState<InvoiceTextPreview|null>(null); const [searchCompanyId,setSearchCompanyId]=useState<number|undefined>(); const [searchInfo,setSearchInfo]=useState(''); const [pdfLanguage,setPdfLanguage]=useState<GamLanguage>('de');
 async function refresh(q=filter){if(!searchCompanyId){setRows([]); setSelected(null); setSearchInfo(ui('pleaseSelectCompany')); return;} setSearchInfo(''); const r=await loadInvoices(100,q,searchCompanyId); setRows(r); if(r[0]) setSelected(await loadInvoice(r[0].number, r[0].companyId)); else setSelected(null);}
 async function select(number:string){const row=rows.find(r=>r.number===number); const detail=await loadInvoice(number, row?.companyId ?? searchCompanyId); setSelected(detail);}
 useEffect(()=>{loadCompanies().then(cs=>{setCompanies(cs); if(cs[0]) setSearchCompanyId(cs[0].id)}).catch(()=>{}); loadProducts('',200).then(setProducts).catch(()=>{}); loadLbdPreview().then(setLbd).catch(()=>setLbd(null));},[]);
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
      <div className="list">{(rows ?? []).map(r=><button key={r.id} onClick={()=>select(r.number)}><b>{translatedInvoiceLabel(r.number)}</b><small>{r.invoiceDate} · {money(r.totalGross)} · {r.companyName}</small></button>)}</div>
    </aside>
    <section className="card detail">{selected?<><div className="row"><h2>{translatedInvoiceLabel(selected.summary.number)}</h2><div className="download-actions"><label className="pdf-language-select">{ui("pdfLanguage")}<select value={pdfLanguage} onChange={e=>setPdfLanguage(e.target.value as GamLanguage)}>{LANGUAGES.map(l=><option key={l.value} value={l.value}>{l.label}</option>)}</select></label><a className="buttonlink" target="_blank" href={pdfUrl(selected.summary.number,pdfLanguage,selected.summary.companyId)}><Download size={16}/> {ui("zugferdPdf")}</a><a className="buttonlink secondarylink" target="_blank" href={zugferdXmlUrl(selected.summary.number,selected.summary.companyId)}><Download size={16}/> {ui("xml")}</a></div></div><p>{selected.summary.companyName} · {selected.summary.invoiceDate} · {money(selected.totals?.gross ?? selected.summary.totalGross)}</p><ExportCheck number={selected.summary.number} companyId={selected.summary.companyId}/><InvoiceAccessBox number={selected.summary.number} companyId={selected.summary.companyId}/><PreviewErrorBoundary><InvoiceTextPreviewPanel preview={textPreview} lines={(selected.lines ?? []).map(l=>({productId:l.productId,quantity:l.quantity,price:l.price,vat:l.vat,description:l.description} as any))} products={products ?? []} company={companies.find(c=>c.id===selected.summary.companyId)} number={selected.summary.number} totals={selected.totals} lang={pdfLanguage} summary={selected.summary} recipient={lbd} invoiceDate={selected.summary.invoiceDate??''} treatmentDate={selected.summary.invoiceDate??''} paymentMethod={(selected.summary as any).paymentMethod??'—'}/></PreviewErrorBoundary><InvoiceStatusActions detail={selected} onChanged={setSelected} onCreated={invoice=>{setRows(prev=>[invoice.summary, ...prev.filter(r=>!(r.id===invoice.summary.id || (r.number===invoice.summary.number && r.companyId===invoice.summary.companyId)))]); setSelected(invoice);}}/></>:<div className="empty">{ui("pleaseSelectInvoice")}</div>}</section>
  </section>}
 </>}

function InvoiceStatusActions({detail,onChanged,onCreated}:{detail:InvoiceDetail; onChanged:(d:InvoiceDetail)=>void; onCreated?:(d:InvoiceDetail)=>Promise<void> | void}){const [busy,setBusy]=useState(false); const n=detail.summary.number; const companyId=detail.summary.companyId; const isSpecial=/[SGP]$/.test(n) || /Z\d*$/.test(n); const lang=currentUiLanguage(); async function cancel(){if(!confirm(`${ui('cancelInvoice', lang)} ${n}S fuer ${ui('company', lang)} ${companyId ?? '—'} erstellen?`)) return; setBusy(true); try{const res=await createCancellationInvoice(n, companyId); onChanged(res.invoice); await onCreated?.(res.invoice);}finally{setBusy(false)}} async function credit(){if(!confirm(`${ui('createCreditNote', lang)} zu ${n} fuer ${ui('company', lang)} ${companyId ?? '—'} erstellen?`)) return; setBusy(true); try{const res=await createCreditNote(n, companyId); onChanged(res.invoice); await onCreated?.(res.invoice);}finally{setBusy(false)}} async function del(){if(!confirm(`${ui('technicalDelete', lang)}?`)) return; setBusy(true); try{await deleteInvoiceDraft(detail.summary.number); location.reload();}finally{setBusy(false)}} return <div className="toolbar small"><button className="secondary" disabled={busy || isSpecial} onClick={cancel}>{ui('cancelInvoice', lang)} {n}S</button><button className="secondary" disabled={busy || isSpecial} onClick={credit}>{ui("createCreditNote", lang)}</button><button className="danger" disabled={busy} onClick={del}>{ui("technicalDelete", lang)}</button></div>}

function ExportCheck({number,companyId}:{number:string; companyId?:number}){const [check,setCheck]=useState<any|null>(null); useEffect(()=>{loadExportCheck(number, companyId).then(setCheck).catch(()=>setCheck(null))},[number,companyId]); if(!check) return <p className="muted">{ui("exportCheckLoading")}</p>; return <div className={check.exportable?'note ok':'note warn'}>{check.exportable?ui('zugferdReady'):ui('zugferdIssues')}{check.issues?.length?<ul>{check.issues.map((i:any,idx:number)=>{const msg=String(i.message??''); const translatedMsg=msg.includes('.lbd-Empfängerdatei')?ui('lbdMissingPlaceholder'):msg; const sev=String(i.severity??''); return <li key={idx}>{sev==='WARN'?'WARN':sev}: {i.field} – {translatedMsg}</li>})}</ul>:null}</div>}

function InvoiceEditor({existing,initialCompanyId,onSaved}:{existing?:InvoiceDetail; initialCompanyId?:number; onSaved:(r:any)=>void}){const [products,setProducts]=useState<ProductDto[]>([]); const [productDescTranslations,setProductDescTranslations]=useState<Record<string,string>>({}); const [companies,setCompanies]=useState<InvoiceCompany[]>([]); const [companyId,setCompanyId]=useState(existing?.summary.companyId??initialCompanyId??2); const [productId,setProductId]=useState<number|undefined>(); const [qty,setQty]=useState(1); const [invoiceDate,setInvoiceDate]=useState(toInputDate(existing?.summary.invoiceDate)); const [treatmentDate,setTreatmentDate]=useState(toInputDate(existing?.summary.invoiceDate)); const [paymentMethod,setPaymentMethod]=useState('unbekannt'); const [pdfLanguage,setPdfLanguage]=useState<GamLanguage>('de'); const [reason,setReason]=useState(''); const [remark,setRemark]=useState(''); const [voucherEnabled,setVoucherEnabled]=useState(false); const [couponText,setCouponText]=useState(''); const [couponAmount,setCouponAmount]=useState(0); const [discountEnabled,setDiscountEnabled]=useState(false); const [discountType,setDiscountType]=useState<'percent'|'amount'>('percent'); const [discountValue,setDiscountValue]=useState(0); const [installmentsEnabled,setInstallmentsEnabled]=useState(false); const [installments,setInstallments]=useState(1); const [err,setErr]=useState(''); const [next,setNext]=useState(existing?.summary.number??''); const [lbd,setLbd]=useState<LbdRecipient|null>(null); const [textPreview,setTextPreview]=useState<InvoiceTextPreview|null>(null); const [lines,setLines]=useState<InvoiceCreateLineRequest[]>(existing?.lines.map(l=>({productId:l.productId,quantity:l.quantity,price:l.price,vat:l.vat,branchId:l.branchId,client:l.client,performer:l.performer}))??[]); const [totals,setTotals]=useState<any>(null);
 useEffect(()=>{loadProducts('',120).then(ps=>{setProducts(ps); if(ps[0]) setProductId(ps[0].id)}); loadCompanies().then(cs=>{setCompanies(cs); if(!existing && cs[0]) setCompanyId(cs[0].id)}).catch(()=>{}); loadLbdPreview().then(setLbd).catch(()=>setLbd(null)); if(!existing) loadNextInvoiceNumber(companyId).then(n=>setNext(n.nextNumber)).catch(()=>{});},[]);
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
 useEffect(()=>{if(lines.length) loadCalculate(lines,setTotals).catch(()=>{}); else setTotals(null)},[JSON.stringify(lines)]);
 useEffect(()=>{loadInvoiceTextPreview(companyId,pdfLanguage,treatmentDate,lbd?.file??'').then(setTextPreview).catch(()=>setTextPreview(null));},[companyId,pdfLanguage,treatmentDate,lbd?.file]);
 const p=products.find(x=>x.id===productId); const selectedCompany=companies.find(c=>c.id===companyId); function addLine(){if(!p) return; const next=[...lines,{productId:p.id, quantity:qty, price:p.price??0, vat:p.vat??0}]; const fee=products.find(isPrescriptionFeeProduct); if(isTrainingCompany(selectedCompany) && isGkvProduct(p) && fee && !next.some(l=>l.productId===fee.id)){ next.push({productId:fee.id, quantity:1, price:fee.price??0, vat:fee.vat??0}); } setLines(next)}
 function updateLine(idx:number, patch:Partial<InvoiceCreateLineRequest>){setLines(lines.map((l,i)=>i===idx?{...l,...patch}:l))}
 function removeLine(idx:number){setLines(lines.filter((_,i)=>i!==idx))}
 function buildPayload(){const submitLines = lines.length ? lines : (p ? [{productId:p.id, quantity:qty, price:p.price??0, vat:p.vat??0}] : []); if(!submitLines.length) throw new Error(ui('selectAtLeastOneLine')); return {invoiceDate, treatmentDate, companyId, paymentMethod, reason, remark, couponText: voucherEnabled ? couponText : undefined, couponAmount: voucherEnabled ? couponAmount : undefined, discountType: discountEnabled ? discountType : undefined, discountValue: discountEnabled ? discountValue : undefined, installments: installmentsEnabled ? installments : undefined, lines:submitLines};}
 async function submit(e:React.FormEvent){e.preventDefault(); setErr(''); try{const payload=buildPayload(); const res=existing?await updateInvoice(existing.summary.number,payload):await createInvoice({...payload, number: next || undefined}); onSaved(res);}catch(ex:any){setErr(ex.message??ui('saveFailed'));}}
 async function submitProforma(){setErr(''); try{const payload=buildPayload(); const res=await createProformaInvoice({...payload, number: undefined, paymentAdvice: true, reason: reason || 'Proforma-Rechnung'}); onSaved(res);}catch(ex:any){setErr(ex.message??ui('proformaFailed'));}}
 const previewSummary = {id:0, number: next, invoiceDate, companyId, companyName: companies.find(c=>c.id===companyId)?.name, couponAmount: voucherEnabled ? couponAmount : undefined, discountPercent: discountEnabled && discountType==='percent' ? discountValue : undefined, discountRemark: (voucherEnabled && couponText) ? couponText : undefined, installments: installmentsEnabled ? installments : undefined} as InvoiceSummary;
 const translatedProductDescription = (p?:ProductDto, fallbackId?:number|string, lineDescription?:string) => productDescriptionForLanguage(pdfLanguage, p, fallbackId, lineDescription, productDescTranslations);
 return <section className="invoice-editor-layout"><section className="card new invoice-editor"><div className="row"><div><h2>{existing?ui('invoiceEdit'):ui('newInvoice')}</h2><p className="muted">Nummer: <b>{next||ui('loadingNumber')}</b> · {ui('mandatoryZugferd')}</p></div>{lbd?.found&&<div className="lbd-compact"><UserRound size={16}/><div><b>{[lbd.salutation,lbd.title,lbd.firstName,lbd.lastName].filter(Boolean).join(' ')}</b><br/><small>{lbd.street} · {[lbd.postalCode,lbd.city].filter(Boolean).join(' ')}</small></div></div>}</div><form onSubmit={submit} className="newgrid"><label>{ui("invoiceDate")}<input type="date" value={invoiceDate} onChange={e=>setInvoiceDate(e.target.value)}/><small>{formatGamDate(invoiceDate,currentUiLanguage())}</small></label><label>{ui("treatmentDate")}<input type="date" value={treatmentDate} onChange={e=>setTreatmentDate(e.target.value)}/><small>{formatGamDate(treatmentDate,currentUiLanguage())}</small></label><label>{ui("company")}<select value={companyId} onChange={e=>setCompanyId(Number(e.target.value))}>{companies.map(c=><option key={c.id} value={c.id}>{c.name??c.code??c.id}</option>)}</select></label><label>{ui("paymentMethod")}<select value={paymentMethod} onChange={e=>setPaymentMethod(e.target.value)}><option value="unbekannt">{ui("paymentUnknown")}</option><option value="Barzahlung">{ui("paymentCash")}</option><option value="Kartenzahlung">{ui("paymentCard")}</option><option value="Überweisung">{ui("paymentTransfer")}</option></select></label><label>{ui("pdfLanguage")}<select value={pdfLanguage} onChange={e=>setPdfLanguage(e.target.value as GamLanguage)}>{LANGUAGES.map(l=><option key={l.value} value={l.value}>{l.label}</option>)}</select></label><label>{ui("reason")}<input value={reason} onChange={e=>setReason(e.target.value)}/></label><label>{ui("remark")}<input value={remark} onChange={e=>setRemark(e.target.value)}/></label><label className="toggle-field"><input type="checkbox" checked={voucherEnabled} onChange={e=>setVoucherEnabled(e.target.checked)}/> {ui("voucherEnable")}</label>{voucherEnabled&&<><label>{ui("voucherText")}<input value={couponText} onChange={e=>setCouponText(e.target.value)} placeholder={ui("voucher")}/></label><label>{ui("voucherAmount")}<input type="number" step="0.01" value={couponAmount} onChange={e=>setCouponAmount(Number(e.target.value))}/></label></>}<label className="toggle-field"><input type="checkbox" checked={discountEnabled} onChange={e=>setDiscountEnabled(e.target.checked)}/> {ui("discountEnable")}</label>{discountEnabled&&<><label>{ui("discountType")}<select value={discountType} onChange={e=>setDiscountType(e.target.value as any)}><option value="percent">{ui("percent")}</option><option value="amount">{ui("amount")}</option></select></label><label>{ui("discountValue")}<input type="number" step="0.01" value={discountValue} onChange={e=>setDiscountValue(Number(e.target.value))}/></label></>}<label className="toggle-field"><input type="checkbox" checked={installmentsEnabled} onChange={e=>setInstallmentsEnabled(e.target.checked)}/> {ui("installmentsEnable")}</label>{installmentsEnabled&&<label>{ui("installmentCount")}<select value={installments} onChange={e=>setInstallments(Number(e.target.value))}>{[1,2,3,4,5].map(n=><option key={n} value={n}>{n} {n>1?ui('ratePlural'):ui('rateSingular')}</option>)}</select></label>}<label className="product-field">{ui("product")}<select value={productId??''} onChange={e=>setProductId(Number(e.target.value))}>{products.map(p=><option key={p.id} value={p.id}>{p.code} · {translatedProductDescription(p)} · {money(p.price)}</option>)}</select></label><label className="quantity-field">{ui("quantity")}<input type="number" step="0.1" value={qty} onChange={e=>setQty(Number(e.target.value))}/></label><button type="button" className="secondary add-position" onClick={addLine}>{ui("addPosition")}</button><button className="save-invoice">{existing?ui('editChangesSave'):ui('saveInvoice')}</button>{!existing&&<button type="button" className="secondary" onClick={submitProforma}>Proforma +P</button>}</form>{lines.length>0&&<table><thead><tr><th>{ui("quantity")}</th><th>{ui("product")}</th><th>{ui('tax')}</th><th>{ui('price')}</th><th></th></tr></thead><tbody>{lines.map((l,idx)=>{const prod=products.find(p=>p.id===l.productId); return <tr key={idx}><td><input type="number" step="0.1" value={l.quantity??1} onChange={e=>updateLine(idx,{quantity:Number(e.target.value)})}/></td><td className="product-cell"><b>{prod?.code??l.productId}</b><br/><span>{translatedProductDescription(prod, l.productId, (l as any).description)}</span></td><td><input type="number" value={l.vat??0} onChange={e=>updateLine(idx,{vat:Number(e.target.value)})}/></td><td><input type="number" step="0.01" value={l.price??0} onChange={e=>updateLine(idx,{price:Number(e.target.value)})}/></td><td><button type="button" className="danger" onClick={()=>removeLine(idx)}>{ui("remove")}</button></td></tr>})}</tbody></table>}{commercialRows(previewSummary, totals, lines, pdfLanguage)}{totals&&<p className="note ok">{ui('net')} {money(totals.net)} · {ui('tax')} {money(totals.vat)} · {ui('gross')} <b>{money(totals.gross)}</b></p>}{err&&<b className="error">{err}</b>}</section><InvoiceTextPreviewPanel preview={textPreview} lines={lines} products={products} company={companies.find(c=>c.id===companyId)} number={next} totals={totals} lang={pdfLanguage} summary={previewSummary} recipient={lbd} invoiceDate={invoiceDate} treatmentDate={treatmentDate} paymentMethod={paymentMethod}/></section>}


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
 const userName = summary.username || '—';
 const taxInfo = [company?.taxNumber, company?.vatId].filter(Boolean).join(' ') || '—';
 const speechText = [documentTitle + ' ' + (number || ''), company?.name, recipientName, recipientAddress.join(' '), preview?.salutation, preview?.invoiceText, `${t('invoiceDate')}: ${formatGamDate(invoiceDate, lang)}`, `${t('treatmentDate', ui('treatmentDate', lang))}: ${formatGamDate(treatmentDate || invoiceDate, lang)}`, `${t('invoiceCustomerFile')}: ${customerFile}`, `${t('invoiceUser')}: ${userName}`, `${t('invoicePaymentMethod')}: ${paymentMethod || '—'}`, ...lineTexts, totals ? `${t('net')} ${money(totals.net)}. ${t('vat')} ${money(totals.vat)}. ${t('gross')} ${money(totals.gross)}` : '', preview?.lawHint, preview?.greetings, company ? `${t('bank')} ${company.name} IBAN ${company.iban || ''} BIC ${company.bic || ''}. ${t('invoiceTaxNumberVatId')}: ${taxInfo}` : ''].filter(Boolean).join('. ');
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
  <div className="preview-meta"><span>{t('treatmentDate', ui('treatmentDate', lang))}: {formatGamDate(treatmentDate || invoiceDate, lang)}</span><span>{t('invoiceCustomerFile', ui('invoiceCustomerFile', lang))}: {customerFile}</span><span>{t('invoiceUser', ui('invoiceUser', lang))}: {userName}</span></div>
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
  ['Rechnungsprogramm','vollständig nutzbar','Rechnungen, Storno, Gutschrift, Proforma, Zahlungsavis, QR-Portal, Reports'],
  ['Geräteverzeichnis','teilmigriert','Geräte, Details und Lagerbezug im Lesemodus/Teilbetrieb'],
  ['Lagerverwaltung','teilmigriert','Bestände, Materialbewegungen und Gerätezuordnung'],
  ['Kassenbuch','Lesemodus','Historische Tabellen werden angezeigt, Bearbeitung folgt später'],
  ['Aufgabenverwaltung','Lesemodus','Aufgaben- und Workflowdaten als sichere Ansicht'],
  ['Freigabemanagement','Lesemodus','Freigabeprozesse sichtbar, Schreiblogik folgt nach Fachabgleich'],
  ['Bestelltool','Shell','Modul sichtbar, Fachlogik wird noch rekonstruiert'],
  ['Personaldaten','Lesemodus','Personaldatenmodul sichtbar, Bearbeitung bewusst deaktiviert'],
  ['Arbeitsplatzausstattung','Shell','Historisches Ausstattungsmodul als Platzhalter/Übersicht'],
  ['Preisliste','Shell','Preislistenmodul sichtbar, Admin-Funktionen folgen'],
  ['Reports','nutzbar','Alt-GAM-Reportarten mit XLS/XLSX/CSV/DATEV-Vorbereitung'],
  ['Benutzer/Rechte','nutzbar','accounts, Rollen und userapplication-Modell']
];
function HistoricalModuleOverview(){return <div className="module-overview"><h3>Historische GAM-Anwendungen</h3><p className="muted">Schritt 31 macht die vollständige GAM-Struktur sichtbar. Noch nicht fertig migrierte Module sind als sichere Lesemodus-/Übersichtsbereiche vorhanden.</p><div className="module-grid">{HISTORICAL_MODULES.map(([name,status,desc])=><div className="module-card-mini" key={name}><b>{name}</b><span>{status}</span><small>{desc}</small></div>)}</div></div>}
function ReadOnlyModuleShell({title,description}:{title:string;description:string}){return <section className="card readonly-shell"><h2>{title}</h2><p>{description}</p><p className="note warn">Lesemodus / Modulrahmen: Dieses historische GAM-Modul ist in der Navigation sichtbar. Bearbeiten, Löschen und produktive Schreibaktionen bleiben deaktiviert, bis die Alt-GAM-Fachlogik vollständig rekonstruiert ist.</p><div className="module-grid"><div className="module-card-mini"><b>Status</b><span>Read-only</span><small>Übersicht vorhanden</small></div><div className="module-card-mini"><b>Rechte</b><span>userapplication</span><small>Superadmin-Bypass bleibt erhalten</small></div><div className="module-card-mini"><b>Nächster Schritt</b><span>Fachlogik</span><small>Rekonstruktion aus Alt-GAM-Quellcode</small></div></div></section>}
function ModuleTiles(){const [mods,setMods]=useState<any[]>([]); useEffect(()=>{loadGamModules().then(setMods).catch(()=>{})},[]); if(!mods.length) return null; return <div className="stats">{mods.map(m=><span key={m.key}><b>{m.count>=0?m.count:'—'}</b><br/>{m.label}<br/><small>{m.tableName}</small></span>)}</div>}
function RecordsPage({title,loader}:{title:string; loader:(limit?:number)=>Promise<any[]>}){const [rows,setRows]=useState<any[]>([]); const [err,setErr]=useState(''); useEffect(()=>{loader(150).then(setRows).catch((e:any)=>setErr(e.message??'Konnte Daten nicht laden'))},[title]); return <section className="card"><h2>{title}</h2><p className="muted">Schritt 5: sichere Leseansicht als Modulrahmen. Schreib-/Bearbeitungslogik wird erst nach Abgleich mit der alten Fachlogik aktiviert.</p>{err&&<b className="error">{err}</b>}<GenericTable rows={rows}/></section>}
function CompliancePage(){const [data,setData]=useState<Record<string,any[]>>({}); useEffect(()=>{loadGamCompliance(80).then(setData).catch(()=>{})},[]); return <section className="card"><h2>Prüfungen / Inbetriebnahmen / Einweisungen</h2><p className="muted">Rahmen für STK/MTK/BGV-A3, Inbetriebnahme und Einweisungen.</p>{Object.entries(data).map(([key,rows])=><div key={key}><h3>{key}</h3><GenericTable rows={rows}/></div>)}</section>}
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
function GenericTable({rows}:{rows:any[]}){if(!rows?.length) return <p className="muted">Keine Daten gefunden.</p>; const mapped=(rows ?? []).map(r=>r.values??r); const keys=Array.from(new Set(mapped.flatMap((r:any)=>Object.keys(r)))).slice(0,10); return <table><thead><tr>{keys.map(k=><th key={k}>{k}</th>)}</tr></thead><tbody>{mapped.map((r:any,idx:number)=><tr key={idx}>{keys.map(k=><td key={k}>{formatCell(r[k])}</td>)}</tr>)}</tbody></table>}
function formatCell(v:unknown){if(v==null) return '—'; if(typeof v==='object') return JSON.stringify(v); return String(v)}


// Schritt 9: Inventar/Lager-Verbindung
function InventoryWarehousePage(){const [links,setLinks]=useState<any[]>([]); const [mov,setMov]=useState<any[]>([]); const [materialId,setMaterialId]=useState<number>(1); const [deviceId,setDeviceId]=useState<number|undefined>(); const [qty,setQty]=useState(1); const [reason,setReason]=useState('GAM 2.0 Lagerbuchung'); const [msg,setMsg]=useState(''); const reload=()=>{loadDeviceMaterialLinks().then(setLinks).catch(()=>{}); loadMaterialMovements(80).then(setMov).catch(()=>{})}; useEffect(reload,[]); async function submit(direction:string){setMsg(''); try{await bookMaterial({deviceId, materialId, quantity:qty, direction, reason}); setMsg('Buchung gespeichert.'); reload();}catch(e:any){setMsg(e.message??'Buchung fehlgeschlagen')}} return <section className="grid"><section className="card"><h2>Inventar ↔ Lager</h2><p className="muted">Schritt 9 verbindet Geräte, Verbrauchsmaterial und Bestandsbewegungen. Bei Buchung mit Geräte-ID wird die Materialzuordnung automatisch angelegt.</p><div className="newgrid"><label>Geräte-ID optional<input type="number" value={deviceId??''} onChange={e=>setDeviceId(e.target.value?Number(e.target.value):undefined)}/></label><label>Material-ID<input type="number" value={materialId} onChange={e=>setMaterialId(Number(e.target.value))}/></label><label>{ui("quantity")}<input type="number" value={qty} onChange={e=>setQty(Number(e.target.value))}/></label><label>{ui("reason")}<input value={reason} onChange={e=>setReason(e.target.value)}/></label><button onClick={()=>submit('OUT')}>Material entnehmen</button><button className="secondary" onClick={()=>submit('IN')}>Material Zugang</button></div>{msg&&<p className="note">{msg}</p>}<h3>Zuordnungen</h3><table><thead><tr><th>Gerät</th><th>Material</th><th>Typ</th><th>Bestand</th></tr></thead><tbody>{links.slice(0,80).map(l=><tr key={l.linkId}><td>{l.deviceName} #{l.deviceId}</td><td>{l.materialName} #{l.materialId}</td><td>{l.materialType??'—'}</td><td>{l.stock??'—'}</td></tr>)}</tbody></table></section><section className="card"><h2>Materialbewegungen</h2><table><thead><tr><th>Zeit</th><th>Gerät</th><th>Material</th><th>Δ</th><th>Bestand</th><th>{ui("reason")}</th></tr></thead><tbody>{mov.map(m=><tr key={m.id}><td>{m.createdAt}</td><td>{m.deviceName??'—'}</td><td>{m.materialName}</td><td>{m.delta}</td><td>{m.previousStock??'—'} → {m.newStock??'—'}</td><td>{m.reason??'—'}</td></tr>)}</tbody></table></section></section>}


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
