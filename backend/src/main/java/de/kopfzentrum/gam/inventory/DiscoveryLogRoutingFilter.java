package de.kopfzentrum.gam.inventory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Routes ordinary [DISCOVERY] detail messages away from the main backend log.
 * [DISCOVERY-TRACE] is intentionally NOT matched and therefore remains in the
 * normal backend log. The dedicated GAM_DISCOVERY_DIAGNOSTICS logger is also
 * accepted so existing explicitly routed discovery diagnostics keep working.
 * Per-target SNMP probe details ([SNMP]) are discovery details as well and
 * are routed to discovery-debug.log instead of the normal backend log.
 */
public final class DiscoveryLogRoutingFilter extends Filter<ILoggingEvent> {
    private boolean discoveryFile;

    public void setDiscoveryFile(boolean discoveryFile) {
        this.discoveryFile = discoveryFile;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event == null) {
            return FilterReply.NEUTRAL;
        }
        String logger = event.getLoggerName();
        String message = event.getFormattedMessage();
        boolean dedicated = "GAM_DISCOVERY_DIAGNOSTICS".equals(logger);
        boolean ordinaryDiscovery = message != null && message.startsWith("[DISCOVERY]");
        boolean snmpDiscoveryDetail = message != null && message.startsWith("[SNMP]");
        boolean routedDiscovery = dedicated || ordinaryDiscovery || snmpDiscoveryDetail;

        if (discoveryFile) {
            return routedDiscovery ? FilterReply.ACCEPT : FilterReply.DENY;
        }
        return (ordinaryDiscovery || snmpDiscoveryDetail) ? FilterReply.DENY : FilterReply.NEUTRAL;
    }
}
