package de.kopfzentrum.gam.marketing;

public record MarketingCampaignRequest(
    String name,
    Long actionTypeId,
    Long materialTypeId,
    String materialName,
    String materialCode,
    Long sourceWarehouseId,
    Long targetBranchId,
    Integer plannedQuantity,
    String note
) {}
