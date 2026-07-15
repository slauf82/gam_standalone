package de.kopfzentrum.gam.marketing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketing/references")
public class MarketingReferenceController {
    private final JdbcTemplate jdbc;

    public MarketingReferenceController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, List<Map<String, Object>>> references() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("branches", jdbc.queryForList(
            "SELECT FILIALE_ID AS id, COALESCE(NULLIF(FILIALENAME, ''), FILIALEKUERZEL, CONCAT('Filiale ', FILIALE_ID)) AS name " +
            "FROM filiale WHERE COALESCE(RELEVANT, 1) <> 0 ORDER BY name"
        ));
        result.put("warehouses", jdbc.queryForList(
            "SELECT id, name, warehouse_type_id AS typeId FROM gam_warehouse_location WHERE active = TRUE ORDER BY name"
        ));
        result.put("warehouseTypes", jdbc.queryForList(
            "SELECT id, name FROM gam_warehouse_type WHERE active = TRUE ORDER BY sort_order, name"
        ));
        result.put("actionTypes", jdbc.queryForList(
            "SELECT id, name FROM gam_marketing_action_type WHERE active = TRUE ORDER BY sort_order, name"
        ));
        result.put("materialTypes", jdbc.queryForList(
            "SELECT id, name FROM gam_marketing_material_type WHERE active = TRUE ORDER BY sort_order, name"
        ));
        return result;
    }
}
