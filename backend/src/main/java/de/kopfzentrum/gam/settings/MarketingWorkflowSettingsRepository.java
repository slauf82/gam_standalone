package de.kopfzentrum.gam.settings;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketingWorkflowSettingsRepository {
    private final JdbcTemplate jdbc;

    public MarketingWorkflowSettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void init() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS gam_settings (
                setting_key VARCHAR(120) NOT NULL,
                setting_value TEXT NULL,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                updated_by VARCHAR(255) NULL,
                PRIMARY KEY (setting_key)
            )
            """);

        var defaults = MarketingWorkflowSettings.defaults();
        putDefault("marketing.workflow.enabled", defaults.enabled());
        putDefault("marketing.workflow.scannerEnabled", defaults.scannerEnabled());
        putDefault("marketing.workflow.warehouseCheckEnabled", defaults.warehouseCheckEnabled());
        putDefault("marketing.workflow.offerReorder", defaults.offerReorder());
        putDefault("marketing.workflow.receiptConfirmationRequired", defaults.receiptConfirmationRequired());
        putDefault("marketing.workflow.automaticStockUpdate", defaults.automaticStockUpdate());
        putDefault("marketing.workflow.createTasks", defaults.createTasks());
        putDefault("marketing.workflow.showOnDashboard", defaults.showOnDashboard());
        putDefault("marketing.workflow.defaultQuantityPerBranch", defaults.defaultQuantityPerBranch());
        putDefault("marketing.workflow.warningThreshold", defaults.warningThreshold());
        putDefault("marketing.workflow.createPackingList", defaults.createPackingList());
        putDefault("marketing.workflow.createDistributionProtocol", defaults.createDistributionProtocol());
        putDefault("marketing.workflow.scanSoundEnabled", defaults.scanSoundEnabled());
        putDefault("marketing.workflow.errorSoundEnabled", defaults.errorSoundEnabled());
        putDefault("marketing.workflow.scanSoundVolume", defaults.scanSoundVolume());
        putDefault("marketing.workflow.scanSoundDurationMs", defaults.scanSoundDurationMs());
        putDefault("marketing.workflow.scanSoundFrequencyHz", defaults.scanSoundFrequencyHz());
        putDefault("marketing.workflow.errorSoundFrequencyHz", defaults.errorSoundFrequencyHz());
    }

    private void putDefault(String key, Object value) {
        jdbc.update(
            "INSERT IGNORE INTO gam_settings(setting_key, setting_value, updated_by) VALUES (?, ?, 'system')",
            key,
            String.valueOf(value)
        );
    }

    private String value(String key) {
        var values = jdbc.query(
            "SELECT setting_value FROM gam_settings WHERE setting_key = ?",
            (ResultSet resultSet, int rowNum) -> resultSet.getString(1),
            key
        );
        return values.isEmpty() ? null : values.get(0);
    }

    private boolean bool(String key, boolean fallback) {
        var storedValue = value(key);
        return storedValue == null ? fallback : Boolean.parseBoolean(storedValue);
    }

    private int integer(String key, int fallback) {
        try {
            var storedValue = value(key);
            return storedValue == null ? fallback : Integer.parseInt(storedValue);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private void put(String key, Object value, String user) {
        jdbc.update(
            """
            INSERT INTO gam_settings(setting_key, setting_value, updated_by)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                setting_value = VALUES(setting_value),
                updated_by = VALUES(updated_by),
                updated_at = CURRENT_TIMESTAMP
            """,
            key,
            String.valueOf(value),
            user
        );
    }

    public MarketingWorkflowSettings load() {
        var defaults = MarketingWorkflowSettings.defaults();
        return new MarketingWorkflowSettings(
            bool("marketing.workflow.enabled", defaults.enabled()),
            bool("marketing.workflow.scannerEnabled", defaults.scannerEnabled()),
            bool("marketing.workflow.warehouseCheckEnabled", defaults.warehouseCheckEnabled()),
            bool("marketing.workflow.offerReorder", defaults.offerReorder()),
            bool("marketing.workflow.receiptConfirmationRequired", defaults.receiptConfirmationRequired()),
            bool("marketing.workflow.automaticStockUpdate", defaults.automaticStockUpdate()),
            bool("marketing.workflow.createTasks", defaults.createTasks()),
            bool("marketing.workflow.showOnDashboard", defaults.showOnDashboard()),
            integer("marketing.workflow.defaultQuantityPerBranch", defaults.defaultQuantityPerBranch()),
            integer("marketing.workflow.warningThreshold", defaults.warningThreshold()),
            bool("marketing.workflow.createPackingList", defaults.createPackingList()),
            bool("marketing.workflow.createDistributionProtocol", defaults.createDistributionProtocol()),
            bool("marketing.workflow.scanSoundEnabled", defaults.scanSoundEnabled()),
            bool("marketing.workflow.errorSoundEnabled", defaults.errorSoundEnabled()),
            integer("marketing.workflow.scanSoundVolume", defaults.scanSoundVolume()),
            integer("marketing.workflow.scanSoundDurationMs", defaults.scanSoundDurationMs()),
            integer("marketing.workflow.scanSoundFrequencyHz", defaults.scanSoundFrequencyHz()),
            integer("marketing.workflow.errorSoundFrequencyHz", defaults.errorSoundFrequencyHz())
        );
    }

    public MarketingWorkflowSettings save(MarketingWorkflowSettings settings, String user) {
        put("marketing.workflow.enabled", settings.enabled(), user);
        put("marketing.workflow.scannerEnabled", settings.scannerEnabled(), user);
        put("marketing.workflow.warehouseCheckEnabled", settings.warehouseCheckEnabled(), user);
        put("marketing.workflow.offerReorder", settings.offerReorder(), user);
        put("marketing.workflow.receiptConfirmationRequired", settings.receiptConfirmationRequired(), user);
        put("marketing.workflow.automaticStockUpdate", settings.automaticStockUpdate(), user);
        put("marketing.workflow.createTasks", settings.createTasks(), user);
        put("marketing.workflow.showOnDashboard", settings.showOnDashboard(), user);
        put("marketing.workflow.defaultQuantityPerBranch", Math.max(1, settings.defaultQuantityPerBranch()), user);
        put("marketing.workflow.warningThreshold", Math.max(0, settings.warningThreshold()), user);
        put("marketing.workflow.createPackingList", settings.createPackingList(), user);
        put("marketing.workflow.createDistributionProtocol", settings.createDistributionProtocol(), user);
        put("marketing.workflow.scanSoundEnabled", settings.scanSoundEnabled(), user);
        put("marketing.workflow.errorSoundEnabled", settings.errorSoundEnabled(), user);
        put("marketing.workflow.scanSoundVolume", Math.max(0, Math.min(100, settings.scanSoundVolume())), user);
        put("marketing.workflow.scanSoundDurationMs", Math.max(30, Math.min(500, settings.scanSoundDurationMs())), user);
        put("marketing.workflow.scanSoundFrequencyHz", Math.max(100, Math.min(3000, settings.scanSoundFrequencyHz())), user);
        put("marketing.workflow.errorSoundFrequencyHz", Math.max(100, Math.min(3000, settings.errorSoundFrequencyHz())), user);
        return load();
    }
}
