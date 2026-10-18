-- ============================================================
-- GAM 2.1.0 Preview 2 / Schritt 40k33a10
-- Vollstaendige additive Laufzeit- und Modultabellen
-- Quelle der Tabellenstruktur: exportierte funktionierende GAM-Datenbank
-- Enthält KEINE Bestands-, Patienten-, Konto- oder Praxisdaten.
-- Kann unter Linux direkt in kopfzentruminventardb importiert werden.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;
CREATE TABLE IF NOT EXISTS `account_passkeys` (
  `id` bigint(20) NOT NULL,
  `account_id` int(11) NOT NULL,
  `credential_id` varchar(512) NOT NULL,
  `public_key` longtext DEFAULT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `sign_count` bigint(20) NOT NULL DEFAULT 0,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_used_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `communication_workflow_item` (
  `id` bigint(20) NOT NULL,
  `channel` varchar(30) NOT NULL DEFAULT 'E-Mail',
  `direction` varchar(20) NOT NULL DEFAULT 'EINGANG',
  `sender` varchar(255) DEFAULT NULL,
  `recipient` varchar(255) DEFAULT NULL,
  `subject` varchar(500) NOT NULL,
  `message` text DEFAULT NULL,
  `responsible` varchar(120) DEFAULT NULL,
  `priority` varchar(20) NOT NULL DEFAULT 'mittel',
  `status` varchar(30) NOT NULL DEFAULT 'NEU',
  `due_date` date DEFAULT NULL,
  `result_note` text DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `communication_workflow_settings` (
  `setting_key` varchar(120) NOT NULL,
  `setting_value` varchar(255) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_discovery_builtin_sources` (
  `source_key` varchar(80) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_discovery_device_type_history` (
  `id` bigint(20) NOT NULL,
  `identity_key` varchar(255) NOT NULL,
  `old_device_type` varchar(255) DEFAULT NULL,
  `new_device_type` varchar(255) NOT NULL,
  `changed_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `changed_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_discovery_merge_settings` (
  `id` int(11) NOT NULL,
  `auto_merge_threshold` int(11) NOT NULL DEFAULT 95,
  `duplicate_threshold` int(11) NOT NULL DEFAULT 60,
  `mac_weight` int(11) NOT NULL DEFAULT 85,
  `hardware_serial_weight` int(11) NOT NULL DEFAULT 95,
  `snmp_serial_weight` int(11) NOT NULL DEFAULT 90,
  `device_id_weight` int(11) NOT NULL DEFAULT 80,
  `hostname_weight` int(11) NOT NULL DEFAULT 50,
  `ip_weight` int(11) NOT NULL DEFAULT 5,
  `manufacturer_weight` int(11) NOT NULL DEFAULT 10,
  `type_weight` int(11) NOT NULL DEFAULT 15,
  `two_source_bonus` int(11) NOT NULL DEFAULT 5,
  `three_source_bonus` int(11) NOT NULL DEFAULT 10,
  `automatic_merge_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `hard_conflicts_block_merge` tinyint(1) NOT NULL DEFAULT 1,
  `ip_never_merges_alone` tinyint(1) NOT NULL DEFAULT 1,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_discovery_registered_devices` (
  `identity_key` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `hardware_address` varchar(100) DEFAULT NULL,
  `serial_number` varchar(255) DEFAULT NULL,
  `registered_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `device_type` varchar(255) DEFAULT NULL,
  `manufacturer` varchar(255) DEFAULT NULL,
  `discovery_protocol` longtext DEFAULT NULL,
  `device_status` varchar(100) DEFAULT NULL,
  `first_seen_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_seen_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `detection_count` bigint(20) NOT NULL DEFAULT 0,
  `last_scan_hits` int(11) NOT NULL DEFAULT 0,
  `manual_device_type` tinyint(1) NOT NULL DEFAULT 0,
  `pending_discovery_protocol` longtext DEFAULT NULL,
  `manual_name` tinyint(1) NOT NULL DEFAULT 0,
  `adb_host` varchar(255) DEFAULT NULL,
  `adb_port` int(11) DEFAULT NULL,
  `adb_last_connected_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- 40k36t18: Diese Spalten werden vom aktuellen DiscoveryRegistrationRepository
-- unmittelbar abgefragt. CREATE TABLE IF NOT EXISTS allein erweitert bestehende
-- Tabellen nicht, deshalb zusätzlich additive/idempotente Migration.
ALTER TABLE `gam_discovery_registered_devices` ADD COLUMN IF NOT EXISTS `pending_discovery_protocol` longtext DEFAULT NULL;
ALTER TABLE `gam_discovery_registered_devices` ADD COLUMN IF NOT EXISTS `manual_name` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `gam_discovery_registered_devices` ADD COLUMN IF NOT EXISTS `adb_host` varchar(255) DEFAULT NULL;
ALTER TABLE `gam_discovery_registered_devices` ADD COLUMN IF NOT EXISTS `adb_port` int(11) DEFAULT NULL;
ALTER TABLE `gam_discovery_registered_devices` ADD COLUMN IF NOT EXISTS `adb_last_connected_at` timestamp NULL DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `gam_fritzbox_sources` (
  `id` bigint(20) NOT NULL,
  `name` varchar(120) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `host` varchar(255) NOT NULL,
  `port` int(11) NOT NULL DEFAULT 49000,
  `username` varchar(255) DEFAULT NULL,
  `password` text DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_homeassistant_sources` (
  `id` bigint(20) NOT NULL,
  `name` varchar(120) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `base_url` varchar(500) NOT NULL,
  `access_token` text DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_marketing_action_type` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 100
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_marketing_campaign` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `material_name` varchar(255) NOT NULL,
  `material_code` varchar(120) DEFAULT NULL,
  `source_warehouse` varchar(255) DEFAULT NULL,
  `target_branch` varchar(255) NOT NULL,
  `planned_quantity` int(11) NOT NULL DEFAULT 0,
  `scanned_quantity` int(11) NOT NULL DEFAULT 0,
  `status` varchar(40) NOT NULL DEFAULT 'PLANNED',
  `receipt_confirmed` tinyint(1) NOT NULL DEFAULT 0,
  `note` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `action_type_id` bigint(20) DEFAULT NULL,
  `material_type_id` bigint(20) DEFAULT NULL,
  `source_warehouse_id` bigint(20) DEFAULT NULL,
  `target_branch_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_marketing_history` (
  `id` bigint(20) NOT NULL,
  `campaign_id` bigint(20) NOT NULL,
  `event_type` varchar(80) NOT NULL,
  `details` text DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `quantity_delta` int(11) DEFAULT NULL,
  `material_code` varchar(120) DEFAULT NULL,
  `reverted` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_marketing_material_type` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 100
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_settings` (
  `setting_key` varchar(120) NOT NULL,
  `setting_value` text DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_tuya_sources` (
  `id` bigint(20) NOT NULL,
  `name` varchar(120) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `connection_mode` varchar(30) NOT NULL DEFAULT 'DIRECT_CLOUD',
  `homeassistant_source_id` bigint(20) DEFAULT NULL,
  `app_type` varchar(30) NOT NULL DEFAULT 'SMART_LIFE',
  `region` varchar(30) NOT NULL DEFAULT 'EUROPE',
  `client_id` varchar(255) NOT NULL,
  `client_secret` text DEFAULT NULL,
  `user_uid` varchar(255) DEFAULT NULL,
  `account_username` varchar(255) DEFAULT NULL,
  `account_password` text DEFAULT NULL,
  `country_code` varchar(12) DEFAULT NULL,
  `app_schema` varchar(80) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_warehouse_location` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `code` varchar(80) DEFAULT NULL,
  `warehouse_type_id` bigint(20) DEFAULT NULL,
  `branch_id` bigint(20) DEFAULT NULL,
  `address` varchar(500) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `note` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `gam_warehouse_type` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `sort_order` int(11) NOT NULL DEFAULT 100
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `invoice_workflow_history` (
  `id` bigint(20) NOT NULL,
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) DEFAULT NULL,
  `from_status` varchar(32) DEFAULT NULL,
  `to_status` varchar(32) NOT NULL,
  `note` text DEFAULT NULL,
  `changed_by` varchar(255) DEFAULT NULL,
  `changed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `invoice_workflow_state` (
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ENTWURF',
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `payment_workflow_document` (
  `id` bigint(20) NOT NULL,
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) DEFAULT NULL,
  `document_type` varchar(32) NOT NULL,
  `title` varchar(255) NOT NULL,
  `language` varchar(8) NOT NULL DEFAULT 'de',
  `pdf_data` longblob NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `created_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `payment_workflow_history` (
  `id` bigint(20) NOT NULL,
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) DEFAULT NULL,
  `from_status` varchar(32) DEFAULT NULL,
  `to_status` varchar(32) NOT NULL,
  `amount` decimal(15,2) DEFAULT NULL,
  `note` text DEFAULT NULL,
  `changed_by` varchar(255) DEFAULT NULL,
  `changed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `payment_workflow_state` (
  `invoice_number` varchar(80) NOT NULL,
  `company_id` int(11) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'OFFEN',
  `amount_due` decimal(15,2) NOT NULL DEFAULT 0.00,
  `paid_amount` decimal(15,2) NOT NULL DEFAULT 0.00,
  `due_date` date DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `updated_by` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `rechnungstext_gesellschaft_zuordnung` (
  `ID` int(11) NOT NULL,
  `RGESELLSCHAFTS_ID` int(11) NOT NULL,
  `ANREDE_ID` int(11) DEFAULT NULL,
  `RECHNUNGSTEXT_ID` int(11) DEFAULT NULL,
  `RECHTLICHER_HINWEIS_ID` int(11) DEFAULT NULL,
  `GRUSSFORMEL_ID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `task_workflow_settings` (
  `setting_key` varchar(120) NOT NULL,
  `setting_value` varchar(255) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_czech` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_dutch` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_italian` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_polish` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_portuguese` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_russian` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_spanish` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_swedish` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE IF NOT EXISTS `translation_turkish` (
  `ID` bigint(20) NOT NULL,
  `TRANSLATE_DESCRIPTION` varchar(255) NOT NULL,
  `TRANSLATED_TEXT` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

ALTER TABLE `account_passkeys`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_account_passkeys_credential_id` (`credential_id`),
  ADD KEY `idx_account_passkeys_account_id` (`account_id`),
  ADD KEY `idx_account_passkeys_active` (`active`);

ALTER TABLE `communication_workflow_item`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_comm_status` (`status`),
  ADD KEY `idx_comm_due` (`due_date`),
  ADD KEY `idx_comm_responsible` (`responsible`);

ALTER TABLE `communication_workflow_settings`
  ADD PRIMARY KEY (`setting_key`);

ALTER TABLE `gam_discovery_builtin_sources`
  ADD PRIMARY KEY (`source_key`);

ALTER TABLE `gam_discovery_device_type_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_gam_discovery_type_history_identity` (`identity_key`);

ALTER TABLE `gam_discovery_merge_settings`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_discovery_registered_devices`
  ADD PRIMARY KEY (`identity_key`);

ALTER TABLE `gam_fritzbox_sources`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_homeassistant_sources`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_marketing_action_type`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_marketing_action_type_name` (`name`);

ALTER TABLE `gam_marketing_campaign`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_marketing_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_marketing_history_campaign` (`campaign_id`);

ALTER TABLE `gam_marketing_material_type`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_marketing_material_type_name` (`name`);

ALTER TABLE `gam_settings`
  ADD PRIMARY KEY (`setting_key`);

ALTER TABLE `gam_tuya_sources`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `gam_warehouse_location`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_warehouse_location_name` (`name`);

ALTER TABLE `gam_warehouse_type`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_warehouse_type_name` (`name`);

ALTER TABLE `invoice_workflow_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_invoice_workflow_history_invoice` (`invoice_number`,`company_id`,`changed_at`);

ALTER TABLE `invoice_workflow_state`
  ADD PRIMARY KEY (`invoice_number`,`company_id`);

ALTER TABLE `payment_workflow_document`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_payment_document_invoice` (`invoice_number`,`company_id`,`created_at`);

ALTER TABLE `payment_workflow_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_payment_workflow_history` (`invoice_number`,`company_id`,`changed_at`);

ALTER TABLE `payment_workflow_state`
  ADD PRIMARY KEY (`invoice_number`,`company_id`);

ALTER TABLE `rechnungstext_gesellschaft_zuordnung`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `ux_rechnungstext_gesellschaft` (`RGESELLSCHAFTS_ID`);

ALTER TABLE `task_workflow_settings`
  ADD PRIMARY KEY (`setting_key`);

ALTER TABLE `translation_czech`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_dutch`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_italian`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_polish`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_portuguese`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_russian`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_spanish`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_swedish`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `translation_turkish`
  ADD PRIMARY KEY (`ID`),
  ADD UNIQUE KEY `uk_translation_description` (`TRANSLATE_DESCRIPTION`);

ALTER TABLE `account_passkeys`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `communication_workflow_item`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `gam_discovery_device_type_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

ALTER TABLE `gam_fritzbox_sources`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `gam_homeassistant_sources`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `gam_marketing_action_type`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=269;

ALTER TABLE `gam_marketing_campaign`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `gam_marketing_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `gam_marketing_material_type`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=403;

ALTER TABLE `gam_tuya_sources`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

ALTER TABLE `gam_warehouse_location`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=135;

ALTER TABLE `gam_warehouse_type`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=135;

ALTER TABLE `invoice_workflow_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

ALTER TABLE `payment_workflow_document`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

ALTER TABLE `payment_workflow_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=65;

ALTER TABLE `rechnungstext_gesellschaft_zuordnung`
  MODIFY `ID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `translation_czech`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_dutch`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_italian`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_polish`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_portuguese`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_russian`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_spanish`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_swedish`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `translation_turkish`
  MODIFY `ID` bigint(20) NOT NULL AUTO_INCREMENT;

-- Sichere technische Grundwerte ohne Praxis- oder Zugangsdaten.
INSERT IGNORE INTO `gam_discovery_merge_settings` (`id`,`updated_by`) VALUES (1,'system');

SET FOREIGN_KEY_CHECKS=1;


-- 40k36q26: aktuelle additive Runtime-Ergaenzungen

-- aktuelle technische Runtime-Tabelle `gam_browser_discovery_clients`
CREATE TABLE IF NOT EXISTS gam_browser_discovery_clients (
        client_id VARCHAR(120) NOT NULL,
        address VARCHAR(80) NOT NULL,
        platform VARCHAR(80) NULL,
        platform_version VARCHAR(80) NULL,
        browser VARCHAR(80) NULL,
        browser_version VARCHAR(80) NULL,
        architecture VARCHAR(80) NULL,
        model VARCHAR(255) NULL,
        language VARCHAR(80) NULL,
        timezone VARCHAR(120) NULL,
        screen VARCHAR(80) NULL,
        device_class VARCHAR(40) NULL,
        hardware_concurrency INT NULL,
        device_memory DOUBLE NULL,
        touch BOOLEAN NOT NULL DEFAULT FALSE,
        user_agent TEXT NULL,
        last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(client_id)
      );

-- aktuelle technische Runtime-Tabelle `gam_discovery_app_inventory_runs`
CREATE TABLE IF NOT EXISTS gam_discovery_app_inventory_runs (
        id BIGINT NOT NULL AUTO_INCREMENT,
        identity_key VARCHAR(255) NOT NULL,
        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        finished_at TIMESTAMP NULL,
        status VARCHAR(40) NOT NULL,
        app_count INT NOT NULL DEFAULT 0,
        user_app_count INT NOT NULL DEFAULT 0,
        system_app_count INT NOT NULL DEFAULT 0,
        changes_summary VARCHAR(500) NULL,
        message VARCHAR(500) NULL,
        PRIMARY KEY(id),
        INDEX idx_app_runs_identity(identity_key, started_at)
      );

-- aktuelle technische Runtime-Tabelle `gam_discovery_device_aliases`
CREATE TABLE IF NOT EXISTS gam_discovery_device_aliases (
        identity_key VARCHAR(255) NOT NULL,
        alias_name VARCHAR(255) NOT NULL,
        alias_semantic VARCHAR(255) NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(identity_key, alias_name),
        INDEX idx_gam_discovery_device_aliases_semantic(alias_semantic)
      );

-- aktuelle technische Runtime-Tabelle `gam_discovery_device_network_identities`
CREATE TABLE IF NOT EXISTS gam_discovery_device_network_identities (
        identity_key VARCHAR(255) NOT NULL,
        address VARCHAR(255) NOT NULL DEFAULT '',
        hardware_address VARCHAR(100) NOT NULL DEFAULT '',
        source_identity_key VARCHAR(255) NULL,
        first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(identity_key, address, hardware_address),
        INDEX idx_gam_discovery_network_address(address),
        INDEX idx_gam_discovery_network_mac(hardware_address)
      );

-- aktuelle technische Runtime-Tabelle `gam_discovery_installed_apps`
CREATE TABLE IF NOT EXISTS gam_discovery_installed_apps (
        identity_key VARCHAR(255) NOT NULL,
        package_name VARCHAR(255) NOT NULL,
        android_user_id INT NOT NULL DEFAULT 0,
        android_user_name VARCHAR(255) NULL,
        display_name VARCHAR(255) NULL,
        version_name VARCHAR(100) NULL,
        version_code BIGINT NULL,
        install_time TIMESTAMP NULL,
        update_time TIMESTAMP NULL,
        installer_package VARCHAR(255) NULL,
        is_system BOOLEAN NOT NULL DEFAULT FALSE,
        is_updated_system BOOLEAN NOT NULL DEFAULT FALSE,
        is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
        is_debuggable BOOLEAN NOT NULL DEFAULT FALSE,
        is_test_only BOOLEAN NOT NULL DEFAULT FALSE,
        min_sdk INT NULL,
        target_sdk INT NULL,
        roles VARCHAR(255) NULL,
        first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        removed_at TIMESTAMP NULL,
        PRIMARY KEY(identity_key, package_name, android_user_id)
      );

-- aktuelle technische Runtime-Tabelle `gam_discovery_merge_ignored`
CREATE TABLE IF NOT EXISTS gam_discovery_merge_ignored (
        pair_key VARCHAR(600) NOT NULL,
        key_a VARCHAR(255) NOT NULL,
        key_b VARCHAR(255) NOT NULL,
        signature VARCHAR(64) NOT NULL,
        ignored_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        ignored_by VARCHAR(255) NULL,
        PRIMARY KEY(pair_key)
      );

-- aktuelle technische Runtime-Tabelle `gam_discovery_merge_log`
CREATE TABLE IF NOT EXISTS gam_discovery_merge_log (
        id BIGINT NOT NULL AUTO_INCREMENT,
        target_identity_key VARCHAR(255) NOT NULL,
        merged_identity_keys TEXT NOT NULL,
        summary TEXT NULL,
        performed_by VARCHAR(255) NULL,
        performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(id)
      );

-- aktuelle technische Runtime-Tabelle `gam_discovery_platform_inventory_runs`
CREATE TABLE IF NOT EXISTS gam_discovery_platform_inventory_runs (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        identity_key VARCHAR(255) NOT NULL,
        platform VARCHAR(64) NOT NULL,
        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        finished_at TIMESTAMP NULL,
        status VARCHAR(32) NOT NULL,
        message VARCHAR(1000) NULL,
        triggered_by VARCHAR(255) NULL,
        INDEX idx_platform_runs_identity (identity_key),
        INDEX idx_platform_runs_identity_platform (identity_key, platform)
      );

-- aktuelle technische Runtime-Tabelle `gam_discovery_software_inventory`
CREATE TABLE IF NOT EXISTS gam_discovery_software_inventory (
        identity_key VARCHAR(255) NOT NULL,
        platform VARCHAR(32) NOT NULL,
        scope_type VARCHAR(32) NOT NULL,
        scope_name VARCHAR(191) NOT NULL DEFAULT '',
        software_key CHAR(64) NOT NULL,
        display_name VARCHAR(512) NOT NULL,
        version VARCHAR(255) NULL,
        bundle_or_package VARCHAR(512) NULL,
        install_path TEXT NULL,
        source VARCHAR(128) NULL,
        first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        removed_at TIMESTAMP NULL,
        PRIMARY KEY(identity_key, platform, scope_type, scope_name, software_key),
        INDEX idx_software_identity(identity_key, platform, removed_at),
        INDEX idx_software_scope(identity_key, scope_type, scope_name)
      );

-- aktuelle technische Runtime-Tabelle `gam_inventory_automation_run`
CREATE TABLE IF NOT EXISTS gam_inventory_automation_run (
        id BIGINT NOT NULL AUTO_INCREMENT,
        mode VARCHAR(32) NOT NULL,
        status VARCHAR(32) NOT NULL,
        started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        finished_at TIMESTAMP NULL,
        duration_ms BIGINT NULL,
        discovered_devices INT NOT NULL DEFAULT 0,
        registered_devices INT NOT NULL DEFAULT 0,
        inventory_attempts INT NOT NULL DEFAULT 0,
        inventory_successes INT NOT NULL DEFAULT 0,
        inventory_failures INT NOT NULL DEFAULT 0,
        message TEXT NULL,
        PRIMARY KEY(id)
      );

-- aktuelle technische Runtime-Tabelle `gam_inventory_identity_links`
CREATE TABLE IF NOT EXISTS gam_inventory_identity_links (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        source VARCHAR(32) NOT NULL,
        inventory_device_id INT NOT NULL,
        identity_key VARCHAR(255) NOT NULL,
        matched_by VARCHAR(64) NOT NULL,
        matched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        ambiguous BOOLEAN NOT NULL DEFAULT FALSE,
        UNIQUE KEY uq_gam_inventory_identity_link (source, inventory_device_id)
      );

-- aktuelle technische Runtime-Tabelle `gam_inventory_startup_settings`
CREATE TABLE IF NOT EXISTS gam_inventory_startup_settings (
        id INT NOT NULL PRIMARY KEY,
        enabled BOOLEAN NOT NULL DEFAULT TRUE,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        updated_by VARCHAR(255) NULL
      );

-- aktuelle technische Runtime-Tabelle `gam_linux_ssh_settings`
CREATE TABLE IF NOT EXISTS gam_linux_ssh_settings (id INT NOT NULL PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT FALSE, username VARCHAR(255) NOT NULL DEFAULT '', auth_mode VARCHAR(32) NOT NULL DEFAULT 'KEY', password TEXT NULL, key_path TEXT NULL, port INT NOT NULL DEFAULT 22, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE gam_linux_ssh_settings ADD COLUMN IF NOT EXISTS key_path TEXT NULL AFTER password;
ALTER TABLE gam_linux_ssh_settings ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NULL AFTER updated_at;

-- aktuelle technische Runtime-Tabelle `gam_winrm_settings`
CREATE TABLE IF NOT EXISTS gam_winrm_settings (id INT NOT NULL PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT FALSE, username VARCHAR(255) NOT NULL DEFAULT '', password TEXT NULL, port INT NOT NULL DEFAULT 5985, use_https BOOLEAN NOT NULL DEFAULT FALSE, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE gam_winrm_settings ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NULL AFTER updated_at;

-- aktuelle technische Runtime-Tabelle `gam_os_access_profile`
CREATE TABLE IF NOT EXISTS gam_os_access_profile (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, type VARCHAR(16) NOT NULL, name VARCHAR(255) NOT NULL DEFAULT '', host VARCHAR(255) NOT NULL DEFAULT '', enabled BOOLEAN NOT NULL DEFAULT TRUE, username VARCHAR(255) NOT NULL DEFAULT '', auth_mode VARCHAR(32) NOT NULL DEFAULT '', password TEXT NULL, key_path TEXT NULL, port INT NOT NULL DEFAULT 0, use_https BOOLEAN NOT NULL DEFAULT FALSE, pairing_port INT NULL, pairing_code TEXT NULL, identity_key VARCHAR(512) NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, updated_by VARCHAR(255) NULL, INDEX idx_os_access_type(type), INDEX idx_os_access_host(type,host)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS key_path TEXT NULL AFTER password;
ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS use_https BOOLEAN NOT NULL DEFAULT FALSE AFTER port;
ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS pairing_port INT NULL AFTER use_https;
ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS pairing_code TEXT NULL AFTER pairing_port;
ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS identity_key VARCHAR(512) NULL AFTER pairing_code;
ALTER TABLE gam_os_access_profile ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NULL AFTER updated_at;

SET FOREIGN_KEY_CHECKS=1;

-- 40k36t31: explizites GAM-Vertrauen fuer konkrete WinRM-Ziele (keine Wildcards)
CREATE TABLE IF NOT EXISTS gam_winrm_trusted_targets (
  host VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(255) NULL,
  PRIMARY KEY(host)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
