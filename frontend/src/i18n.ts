export type UiLanguage = "de" | "en" | "fr" | "uk";

export const UI_LANGUAGES: { code: UiLanguage; label: string; native: string }[] = [
  { code: "de", label: "Deutsch", native: "Deutsch" },
  { code: "en", label: "English", native: "English" },
  { code: "fr", label: "Français", native: "Français" },
  { code: "uk", label: "Українська", native: "Українська" },
];

const DICT: Record<UiLanguage, Record<string, string>> = {
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
};

export function normalizeUiLanguage(value: string | null | undefined): UiLanguage {
  return value === "en" || value === "fr" || value === "uk" ? value : "de";
}

export function tUi(language: UiLanguage, key: string): string {
  return DICT[normalizeUiLanguage(language)]?.[key] ?? DICT.de[key] ?? key;
}

export function iconForModule(key: string): string {
  const normalized = key.toLowerCase();
  if (normalized.includes("rechnung")) return "/icons/invoice.svg";
  if (normalized.includes("gerät") || normalized.includes("geraet")) return "/icons/inventory.svg";
  if (normalized.includes("lager")) return "/icons/warehouse.svg";
  if (normalized.includes("kasse")) return "/icons/cashbook.svg";
  if (normalized.includes("aufgabe")) return "/icons/tasks.svg";
  if (normalized.includes("freigabe")) return "/icons/approval.svg";
  if (normalized.includes("bestell")) return "/icons/orders.svg";
  if (normalized.includes("personal")) return "/icons/personnel.svg";
  if (normalized.includes("arbeitsplatz")) return "/icons/workplace.svg";
  if (normalized.includes("preis")) return "/icons/price.svg";
  if (normalized.includes("report") || normalized.includes("bericht")) return "/icons/reports.svg";
  if (normalized.includes("admin")) return "/icons/admin.svg";
  return "/icons/invoice.svg";
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
  if (normalized.includes("personal")) return "personnel";
  if (normalized.includes("arbeitsplatz")) return "workplace";
  if (normalized.includes("preis")) return "price";
  if (normalized.includes("report") || normalized.includes("bericht")) return "reports";
  if (normalized.includes("admin")) return "admin";
  return "invoice";
}
