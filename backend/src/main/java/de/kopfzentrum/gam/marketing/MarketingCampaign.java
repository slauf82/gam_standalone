package de.kopfzentrum.gam.marketing;

import java.time.LocalDateTime;

public record MarketingCampaign(
    Long id,
    String name,
    Long actionTypeId,
    String actionType,
    Long materialTypeId,
    String materialType,
    String materialName,
    String materialCode,
    Long sourceWarehouseId,
    String sourceWarehouse,
    Long targetBranchId,
    String targetBranch,
    int plannedQuantity,
    int scannedQuantity,
    String status,
    boolean receiptConfirmed,
    String note,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
