package de.kopfzentrum.gam.invoice;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Filters only the known pre-render PDF/UA description warning.
 * GAM writes the description into XHTML and finalizes PDF/UA metadata with PDFBox.
 */
final class OpenHtmlPdfLogging {
  private static final AtomicBoolean CONFIGURED = new AtomicBoolean();
  private OpenHtmlPdfLogging() {}

  static void configure() {
    if (!CONFIGURED.compareAndSet(false, true)) return;
    Logger logger = Logger.getLogger("com.openhtmltopdf.general");
    Filter previous = logger.getFilter();
    logger.setFilter(record -> !isHandledDescriptionWarning(record) && (previous == null || previous.isLoggable(record)));
  }

  private static boolean isHandledDescriptionWarning(LogRecord record) {
    if (record == null || record.getMessage() == null) return false;
    return record.getMessage().contains("No document description provided")
        && record.getMessage().contains("PDF/UA");
  }
}
