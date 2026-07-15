package de.kopfzentrum.gam.marketing;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class MarketingCampaignRepository {
    private final JdbcTemplate jdbc;

    public MarketingCampaignRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void init() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS gam_marketing_campaign (
                id BIGINT NOT NULL AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL,
                action_type_id BIGINT NULL,
                material_type_id BIGINT NULL,
                material_name VARCHAR(255) NOT NULL,
                material_code VARCHAR(120) NULL,
                source_warehouse_id BIGINT NULL,
                source_warehouse VARCHAR(255) NULL,
                target_branch_id BIGINT NULL,
                target_branch VARCHAR(255) NOT NULL,
                planned_quantity INT NOT NULL DEFAULT 0,
                scanned_quantity INT NOT NULL DEFAULT 0,
                status VARCHAR(40) NOT NULL DEFAULT 'PLANNED',
                receipt_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
                note TEXT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id)
            )
            """);
        addColumn("action_type_id", "BIGINT NULL");
        addColumn("material_type_id", "BIGINT NULL");
        addColumn("source_warehouse_id", "BIGINT NULL");
        addColumn("target_branch_id", "BIGINT NULL");

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS gam_marketing_history (
                id BIGINT NOT NULL AUTO_INCREMENT,
                campaign_id BIGINT NOT NULL,
                event_type VARCHAR(80) NOT NULL,
                details TEXT NULL,
                created_by VARCHAR(255) NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                quantity_delta INT NULL,
                material_code VARCHAR(120) NULL,
                reverted BOOLEAN NOT NULL DEFAULT FALSE,
                PRIMARY KEY (id),
                INDEX idx_marketing_history_campaign (campaign_id)
            )
            """);
        addHistoryColumn("quantity_delta", "INT NULL");
        addHistoryColumn("material_code", "VARCHAR(120) NULL");
        addHistoryColumn("reverted", "BOOLEAN NOT NULL DEFAULT FALSE");
    }

    private void addHistoryColumn(String column, String definition) {
        Integer existing = jdbc.queryForObject(
            """
            SELECT COUNT(*)
              FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'gam_marketing_history'
               AND COLUMN_NAME = ?
            """,
            Integer.class,
            column
        );
        if (existing != null && existing > 0) return;
        jdbc.execute("ALTER TABLE gam_marketing_history ADD COLUMN " + column + " " + definition);
    }

    private void addColumn(String column, String definition) {
        Integer existing = jdbc.queryForObject(
            """
            SELECT COUNT(*)
              FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'gam_marketing_campaign'
               AND COLUMN_NAME = ?
            """,
            Integer.class,
            column
        );
        if (existing != null && existing > 0) {
            return;
        }
        jdbc.execute("ALTER TABLE gam_marketing_campaign ADD COLUMN " + column + " " + definition);
    }

    private MarketingCampaign map(ResultSet rs, int rowNum) throws SQLException {
        return new MarketingCampaign(
            rs.getLong("id"),
            rs.getString("name"),
            nullableLong(rs, "action_type_id"),
            rs.getString("action_type"),
            nullableLong(rs, "material_type_id"),
            rs.getString("material_type"),
            rs.getString("material_name"),
            rs.getString("material_code"),
            nullableLong(rs, "source_warehouse_id"),
            rs.getString("source_warehouse"),
            nullableLong(rs, "target_branch_id"),
            rs.getString("target_branch"),
            rs.getInt("planned_quantity"),
            rs.getInt("scanned_quantity"),
            rs.getString("status"),
            rs.getBoolean("receipt_confirmed"),
            rs.getString("note"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String selectSql() {
        return """
            SELECT c.*,
                   at.name AS action_type,
                   mt.name AS material_type,
                   COALESCE(w.name, c.source_warehouse) AS source_warehouse,
                   COALESCE(NULLIF(f.FILIALENAME, ''), f.FILIALEKUERZEL, c.target_branch) AS target_branch
              FROM gam_marketing_campaign c
              LEFT JOIN gam_marketing_action_type at ON at.id = c.action_type_id
              LEFT JOIN gam_marketing_material_type mt ON mt.id = c.material_type_id
              LEFT JOIN gam_warehouse_location w ON w.id = c.source_warehouse_id
              LEFT JOIN filiale f ON f.FILIALE_ID = c.target_branch_id
            """;
    }

    public List<MarketingCampaign> list() {
        return jdbc.query(selectSql() + " ORDER BY c.updated_at DESC, c.id DESC", this::map);
    }

    public MarketingCampaign get(long id) {
        return jdbc.queryForObject(selectSql() + " WHERE c.id = ?", this::map, id);
    }

    public MarketingCampaign create(MarketingCampaignRequest request, String user) {
        requireId(request.actionTypeId(), "Aktionsart");
        requireId(request.materialTypeId(), "Materialart");
        requireId(request.sourceWarehouseId(), "Lager");
        requireId(request.targetBranchId(), "Ziel-Filiale");
        String actionType = lookup("gam_marketing_action_type", request.actionTypeId(), "Aktionsart");
        String materialType = lookup("gam_marketing_material_type", request.materialTypeId(), "Materialart");
        String warehouse = lookup("gam_warehouse_location", request.sourceWarehouseId(), "Lager");
        String branch = jdbc.queryForObject(
            "SELECT COALESCE(NULLIF(FILIALENAME, ''), FILIALEKUERZEL, CONCAT('Filiale ', FILIALE_ID)) FROM filiale WHERE FILIALE_ID = ?",
            String.class, request.targetBranchId()
        );

        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO gam_marketing_campaign (
                    name, action_type_id, material_type_id, material_name, material_code,
                    source_warehouse_id, source_warehouse, target_branch_id, target_branch,
                    planned_quantity, note
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, normalized(request.name(), actionType));
            statement.setLong(2, request.actionTypeId());
            statement.setLong(3, request.materialTypeId());
            statement.setString(4, normalized(request.materialName(), materialType));
            statement.setString(5, request.materialCode());
            statement.setLong(6, request.sourceWarehouseId());
            statement.setString(7, warehouse);
            statement.setLong(8, request.targetBranchId());
            statement.setString(9, branch);
            statement.setInt(10, Math.max(1, request.plannedQuantity() == null ? 1 : request.plannedQuantity()));
            statement.setString(11, request.note());
            return statement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("Marketingaktion konnte nicht angelegt werden: keine ID erzeugt.");
        }
        long id = generatedKey.longValue();
        history(id, "CREATED", actionType + " · " + materialType + " · " + warehouse + " → " + branch, user);
        return get(id);
    }

    private void requireId(Long id, String label) {
        if (id == null || id <= 0) throw new IllegalArgumentException(label + " muss ausgewählt werden.");
    }

    private String lookup(String table, Long id, String label) {
        List<String> names = jdbc.queryForList("SELECT name FROM " + table + " WHERE id = ? AND active = TRUE", String.class, id);
        if (names.isEmpty()) throw new IllegalArgumentException(label + " ist nicht vorhanden oder inaktiv.");
        return names.get(0);
    }

    public MarketingCampaign scan(long id, MarketingScanRequest request, String user) {
        var campaign = get(id);
        int requested = request.quantity() == null ? 1 : request.quantity();
        if (requested == 0) throw new IllegalArgumentException("Die Scanmenge darf nicht 0 sein.");
        int nextQuantity = Math.max(0, Math.min(campaign.plannedQuantity(), campaign.scannedQuantity() + requested));
        int applied = nextQuantity - campaign.scannedQuantity();
        if (applied == 0) throw new IllegalArgumentException(requested > 0 ? "Die geplante Menge ist bereits vollständig erfasst." : "Es ist keine erfasste Menge vorhanden.");
        String status = nextQuantity >= campaign.plannedQuantity() ? "READY_TO_SHIP" : nextQuantity > 0 ? "PICKING" : "PLANNED";
        String code = normalized(request.code(), campaign.materialCode());
        jdbc.update("UPDATE gam_marketing_campaign SET scanned_quantity = ?, status = ? WHERE id = ?", nextQuantity, status, id);
        history(id, applied > 0 ? "SCAN" : "RETURN", (applied > 0 ? "Mengenscan +" : "Rücknahme ") + applied + " · Code " + code + " · Gesamt " + nextQuantity, user, applied, code);
        return get(id);
    }

    public MarketingCampaign undoLastScan(long id, String user) {
        var campaign = get(id);
        List<Map<String, Object>> rows = jdbc.queryForList(
            """
            SELECT id, quantity_delta, material_code
              FROM gam_marketing_history
             WHERE campaign_id = ?
               AND event_type = 'SCAN'
               AND reverted = FALSE
               AND quantity_delta > 0
             ORDER BY id DESC
             LIMIT 1
            """, id
        );
        if (rows.isEmpty()) throw new IllegalArgumentException("Kein Scan vorhanden, der zurückgenommen werden kann.");
        long historyId = ((Number) rows.get(0).get("id")).longValue();
        int quantity = ((Number) rows.get(0).get("quantity_delta")).intValue();
        String code = String.valueOf(rows.get(0).getOrDefault("material_code", ""));
        int nextQuantity = Math.max(0, campaign.scannedQuantity() - quantity);
        String status = nextQuantity >= campaign.plannedQuantity() ? "READY_TO_SHIP" : nextQuantity > 0 ? "PICKING" : "PLANNED";
        jdbc.update("UPDATE gam_marketing_campaign SET scanned_quantity = ?, status = ? WHERE id = ?", nextQuantity, status, id);
        jdbc.update("UPDATE gam_marketing_history SET reverted = TRUE WHERE id = ?", historyId);
        history(id, "UNDO_SCAN", "Letzten Scan zurückgenommen: -" + quantity + " · Code " + code + " · Gesamt " + nextQuantity, user, -quantity, code);
        return get(id);
    }

    public MarketingCampaign transition(long id, String status, String user) {
        var campaign = get(id);
        boolean receiptConfirmed = "RECEIVED".equals(status) || "COMPLETED".equals(status);
        jdbc.update("""
            UPDATE gam_marketing_campaign
               SET status = ?, receipt_confirmed = CASE WHEN ? THEN TRUE ELSE receipt_confirmed END
             WHERE id = ?
            """, status, receiptConfirmed, id);
        history(id, "STATUS", campaign.status() + " → " + status, user);
        return get(id);
    }

    public List<Map<String, Object>> history(long id) {
        return jdbc.queryForList("""
            SELECT event_type, details, created_by, created_at
              FROM gam_marketing_history
             WHERE campaign_id = ?
             ORDER BY id DESC
            """, id);
    }

    private void history(long id, String eventType, String details, String user) {
        history(id, eventType, details, user, null, null);
    }

    private void history(long id, String eventType, String details, String user, Integer quantityDelta, String materialCode) {
        jdbc.update("""
            INSERT INTO gam_marketing_history (campaign_id, event_type, details, created_by, quantity_delta, material_code)
            VALUES (?, ?, ?, ?, ?, ?)
            """, id, eventType, details, user, quantityDelta, materialCode);
    }

    private String normalized(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
