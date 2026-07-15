package de.kopfzentrum.gam.settings;

public record MarketingWorkflowSettings(
    boolean enabled,
    boolean scannerEnabled,
    boolean warehouseCheckEnabled,
    boolean offerReorder,
    boolean receiptConfirmationRequired,
    boolean automaticStockUpdate,
    boolean createTasks,
    boolean showOnDashboard,
    int defaultQuantityPerBranch,
    int warningThreshold,
    boolean createPackingList,
    boolean createDistributionProtocol,
    boolean scanSoundEnabled,
    boolean errorSoundEnabled,
    int scanSoundVolume,
    int scanSoundDurationMs,
    int scanSoundFrequencyHz,
    int errorSoundFrequencyHz
) {
    public static MarketingWorkflowSettings defaults() {
        return new MarketingWorkflowSettings(
            true, true, true, true, true, true, true, true,
            25, 10, true, true,
            true, true, 55, 90, 1000, 440
        );
    }
}
