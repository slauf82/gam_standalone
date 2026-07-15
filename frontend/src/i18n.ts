export type UiLanguage = "de" | "en" | "fr" | "uk" | "it" | "sv" | "tr" | "ru" | "es" | "pt" | "nl" | "pl" | "cs";

export const UI_LANGUAGES: { code: UiLanguage; label: string; native: string }[] = [
  { code: "de", label: "Deutsch", native: "Deutsch" },
  { code: "en", label: "English", native: "English" },
  { code: "fr", label: "Français", native: "Français" },
  { code: "uk", label: "Українська", native: "Українська" },
  { code: "it", label: "Italiano", native: "Italiano" },
  { code: "es", label: "Español", native: "Español" },
  { code: "pt", label: "Português", native: "Português" },
  { code: "nl", label: "Nederlands", native: "Nederlands" },
  { code: "pl", label: "Polski", native: "Polski" },
  { code: "cs", label: "Čeština", native: "Čeština" },
  { code: "sv", label: "Svenska", native: "Svenska" },
  { code: "tr", label: "Türkçe", native: "Türkçe" },
  { code: "ru", label: "Русский", native: "Русский" },
];

const DICT: Record<string, Record<string, string>> = {
  de: {
    
    legacyLoginHint: "Kompatibler Login über bestehende accounts-Tabelle.",
    chooseApplication: "Anwendung wählen",
    moduleVisibilityHint: "Alle historischen GAM-Anwendungen sind sichtbar; noch nicht vollständig migrierte Module starten im Lesemodus.",
    passwordLogin: "Passwort",
    totpRegister: "2FA registrieren",
    totpLogin: "2FA-Login",
    passkeyRegister: "Passkey registrieren",
    passkeyLogin: "Passkey-Login",
    applicationChoose: "Anwendung wählen",
appTitle: "GAM 2.0",
    loginTitle: "Anmeldung",
    uiLanguage: "Oberflächensprache",
    application: "Anwendung",
    username: "Benutzername",
    password: "Passwort",
    login: "Anmelden",
    logout: "Abmelden",
    dashboard: "Dashboard",
    invoice: "Rechnungsprogramm",
    inventory: "Geräteverzeichnis",
    warehouse: "Lagerverwaltung",
    cashbook: "Kassenbuch",
    tasks: "Aufgabenverwaltung",
    approval: "Freigabemanagement",
    orders: "Bestelltool",
    communication: "Kommunikation",
    personnel: "Personaldaten",
    workplace: "Arbeitsplatzausstattung",
    price: "Preisliste",
    reports: "Reports",
    admin: "Administration",
    readOnly: "Lesemodus",
    modulePreview: "Modulübersicht",
    comingSoon: "Dieses Modul ist als Lesemodus-/Vorschau-Bereich vorbereitet.",
    pdfLanguage: "PDF-Sprache",
    invoiceLanguageHint: "Die PDF-Sprache im Rechnungsprogramm bleibt separat von der Oberflächensprache.",
  },
  en: {
    appTitle: "GAM 2.0",
    loginTitle: "Login",
    uiLanguage: "Interface language",
    application: "Application",
    username: "Username",
    password: "Password",
    login: "Sign in",
    logout: "Sign out",
    dashboard: "Dashboard",
    invoice: "Invoicing",
    inventory: "Device directory",
    warehouse: "Warehouse",
    cashbook: "Cashbook",
    tasks: "Task management",
    approval: "Approval management",
    orders: "Ordering tool",
    communication: "Communication",
    personnel: "Personnel data",
    workplace: "Workplace equipment",
    price: "Price list",
    reports: "Reports",
    admin: "Administration",
    readOnly: "Read-only mode",
    modulePreview: "Module overview",
    comingSoon: "This module is prepared as a read-only / preview area.",
    pdfLanguage: "PDF language",
    invoiceLanguageHint: "The invoice PDF language remains separate from the interface language.",
  },
  fr: {
    appTitle: "GAM 2.0",
    loginTitle: "Connexion",
    uiLanguage: "Langue de l’interface",
    application: "Application",
    username: "Nom d’utilisateur",
    password: "Mot de passe",
    login: "Se connecter",
    logout: "Se déconnecter",
    dashboard: "Tableau de bord",
    invoice: "Facturation",
    inventory: "Répertoire des appareils",
    warehouse: "Gestion du stock",
    cashbook: "Livre de caisse",
    tasks: "Gestion des tâches",
    approval: "Gestion des validations",
    orders: "Outil de commande",
    communication: "Communication",
    personnel: "Données du personnel",
    workplace: "Équipement du poste",
    price: "Liste de prix",
    reports: "Rapports",
    admin: "Administration",
    readOnly: "Mode lecture seule",
    modulePreview: "Aperçu des modules",
    comingSoon: "Ce module est préparé comme zone de prévisualisation en lecture seule.",
    pdfLanguage: "Langue du PDF",
    invoiceLanguageHint: "La langue du PDF de facture reste séparée de la langue de l’interface.",
  },
  uk: {
    appTitle: "GAM 2.0",
    loginTitle: "Вхід",
    uiLanguage: "Мова інтерфейсу",
    application: "Застосунок",
    username: "Ім’я користувача",
    password: "Пароль",
    login: "Увійти",
    logout: "Вийти",
    dashboard: "Панель",
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
    readOnly: "Режим перегляду",
    modulePreview: "Огляд модулів",
    comingSoon: "Цей модуль підготовлено як область попереднього перегляду без редагування.",
    pdfLanguage: "Мова PDF",
    invoiceLanguageHint: "Мова PDF-рахунку налаштовується окремо від мови інтерфейсу.",
  },
  it: {
    appTitle: "GAM 2.0", loginTitle: "Accesso", uiLanguage: "Lingua interfaccia", application: "Applicazione", username: "Nome utente", password: "Password", login: "Accedi", logout: "Esci", dashboard: "Dashboard", invoice: "Fatturazione", inventory: "Dispositivi", warehouse: "Magazzino", cashbook: "Cassa", tasks: "Attività", approval: "Approvazioni", orders: "Ordini", communication: "Comunicazione", personnel: "Personale", workplace: "Postazione", price: "Listino prezzi", reports: "Report", admin: "Amministrazione", readOnly: "Sola lettura", modulePreview: "Panoramica moduli", comingSoon: "Questo modulo è preparato come anteprima in sola lettura.", pdfLanguage: "Lingua PDF", invoiceLanguageHint: "La lingua del PDF rimane separata dalla lingua dell’interfaccia.",
  },
  sv: {
    appTitle: "GAM 2.0", loginTitle: "Inloggning", uiLanguage: "Gränssnittsspråk", application: "Applikation", username: "Användarnamn", password: "Lösenord", login: "Logga in", logout: "Logga ut", dashboard: "Dashboard", invoice: "Fakturering", inventory: "Enheter", warehouse: "Lager", cashbook: "Kassabok", tasks: "Uppgifter", approval: "Godkännanden", orders: "Beställningar", communication: "Kommunikation", personnel: "Personal", workplace: "Arbetsplats", price: "Prislista", reports: "Rapporter", admin: "Administration", readOnly: "Skrivskyddat", modulePreview: "Modulöversikt", comingSoon: "Denna modul är förberedd som skrivskyddad förhandsvisning.", pdfLanguage: "PDF-språk", invoiceLanguageHint: "Faktura-PDF-språket är separat från gränssnittsspråket.",
  },
  tr: {
    appTitle: "GAM 2.0", loginTitle: "Giriş", uiLanguage: "Arayüz dili", application: "Uygulama", username: "Kullanıcı adı", password: "Parola", login: "Giriş yap", logout: "Çıkış", dashboard: "Panel", invoice: "Faturalama", inventory: "Cihazlar", warehouse: "Depo", cashbook: "Kasa defteri", tasks: "Görevler", approval: "Onaylar", orders: "Siparişler", communication: "İletişim", personnel: "Personel", workplace: "Çalışma yeri", price: "Fiyat listesi", reports: "Raporlar", admin: "Yönetim", readOnly: "Salt okunur", modulePreview: "Modül özeti", comingSoon: "Bu modül salt okunur önizleme olarak hazırlandı.", pdfLanguage: "PDF dili", invoiceLanguageHint: "Fatura PDF dili arayüz dilinden ayrıdır.",
  },
  ru: {
    appTitle: "GAM 2.0", loginTitle: "Вход", uiLanguage: "Язык интерфейса", application: "Приложение", username: "Имя пользователя", password: "Пароль", login: "Войти", logout: "Выйти", dashboard: "Панель", invoice: "Счета", inventory: "Устройства", warehouse: "Склад", cashbook: "Кассовая книга", tasks: "Задачи", approval: "Согласования", orders: "Заказы", communication: "Коммуникация", personnel: "Персонал", workplace: "Рабочее место", price: "Прайс-лист", reports: "Отчеты", admin: "Администрирование", readOnly: "Только чтение", modulePreview: "Обзор модулей", comingSoon: "Этот модуль подготовлен как область предварительного просмотра только для чтения.", pdfLanguage: "Язык PDF", invoiceLanguageHint: "Язык PDF-счета настраивается отдельно от языка интерфейса.",
  },
};


export function germanUiEntries(): Record<string, string> {
  return { ...(DICT.de ?? {}) };
}

export function normalizeUiLanguage(value: string | null | undefined): UiLanguage {
  return value === "en" || value === "fr" || value === "uk" || value === "it" || value === "sv" || value === "tr" || value === "ru" || value === "es" || value === "pt" || value === "nl" || value === "pl" || value === "cs" ? value : "de";
}

export function tUi(language: UiLanguage, key: string): string {
  return DICT[normalizeUiLanguage(language)]?.[key] ?? DICT.de[key] ?? key;
}

export function iconForModule(key: string): string {
  const normalized = key.toLowerCase();
  if (normalized.includes("dashboard")) return "/icons/dashboard.svg";
  if (normalized.includes("einstellung") || normalized === "settings") return "/icons/settings.svg";
  if (normalized.includes("rechnungsadministration") || normalized.includes("invoice admin")) return "/icons/invoice-admin.svg";
  if (normalized.includes("rechnung") || normalized.includes("invoice")) return "/icons/invoice.svg";
  if (normalized.includes("patient")) return "/icons/patients.svg";
  if (normalized.includes("termin") || normalized.includes("appointment")) return "/icons/appointments.svg";
  if (normalized.includes("gerät") || normalized.includes("geraet") || normalized.includes("inventory")) return "/icons/inventory.svg";
  if (normalized.includes("lager") || normalized.includes("warehouse")) return "/icons/warehouse.svg";
  if (normalized.includes("kasse") || normalized.includes("cashbook")) return "/icons/cashbook.svg";
  if (normalized.includes("aufgabe") || normalized.includes("task")) return "/icons/tasks.svg";
  if (normalized.includes("freigabe") || normalized.includes("approval")) return "/icons/approval.svg";
  if (normalized.includes("bestell") || normalized.includes("order")) return "/icons/orders.svg";
  if (normalized.includes("marketing")) return "/icons/marketing.svg";
  if (normalized.includes("labor")) return "/icons/laboratory.svg";
  if (normalized.includes("wartezimmer") || normalized.includes("waiting")) return "/icons/waiting-room.svg";
  if (normalized.includes("kommunikation") || normalized.includes("communication") || normalized.includes("gcs")) return "/icons/communication.svg";
  if (normalized.includes("personal") || normalized.includes("personnel")) return "/icons/personnel.svg";
  if (normalized.includes("arbeitsplatz") || normalized.includes("workplace")) return "/icons/workplace.svg";
  if (normalized.includes("preis") || normalized.includes("price")) return "/icons/price.svg";
  if (normalized.includes("prüf") || normalized.includes("pruef") || normalized.includes("check") || normalized.includes("compliance")) return "/icons/compliance.svg";
  if (normalized.includes("report") || normalized.includes("bericht")) return "/icons/reports.svg";
  if (normalized.includes("modul-admin") || normalized.includes("module admin")) return "/icons/module-admin.svg";
  if (normalized.includes("benutzer") || normalized.includes("user") || normalized.includes("recht") || normalized.includes("permission")) return "/icons/users.svg";
  if (normalized.includes("admin")) return "/icons/admin.svg";
  return "/icons/default.svg";
}

export function moduleKeyFromLabel(label: string): string {
  const normalized = label.toLowerCase();
  if (normalized.includes("rechnung")) return "invoice";
  if (normalized.includes("gerät") || normalized.includes("geraet")) return "inventory";
  if (normalized.includes("lager")) return "warehouse";
  if (normalized.includes("kasse")) return "cashbook";
  if (normalized.includes("aufgabe")) return "tasks";
  if (normalized.includes("freigabe")) return "approval";
  if (normalized.includes("bestell")) return "orders";
  if (normalized.includes("kommunikation") || normalized.includes("communication") || normalized.includes("gcs")) return "communication";
  if (normalized.includes("recht") || normalized.includes("permission")) return "permissions";
  if (normalized.includes("personal")) return "personnel";
  if (normalized.includes("arbeitsplatz")) return "workplace";
  if (normalized.includes("preis")) return "price";
  if (normalized.includes("prüf") || normalized.includes("pruef") || normalized.includes("check") || normalized.includes("compliance")) return "compliance";
  if (normalized.includes("report") || normalized.includes("bericht")) return "reports";
  if (normalized.includes("admin")) return "admin";
  return "invoice";
}
