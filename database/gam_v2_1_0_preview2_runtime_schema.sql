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
  `manual_device_type` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

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
