package de.kopfzentrum.gam.invoice;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * OpenPDF-free compatibility facade.
 *
 * The old OpenPDF based class name is intentionally kept because a few newer
 * OpenHTML/PDF-UA callers still use this static helper.  The implementation is
 * now pure PDFBox and restores the 37h8 PAC-relevant metadata/outline cleanup:
 * PDF/UA identifier, XMP title, document title display and bookmarks.
 */
public final class InvoicePdfAccessibility {

  private InvoicePdfAccessibility() {
    // utility class
  }

  public static byte[] applyBasicAccessibility(byte[] pdfBytes, String title, String language) {
    return finalizePdfUaMetadata(pdfBytes, title, language, "GAM 2.0 Standalone");
  }

  public static byte[] finalizePdfUaMetadata(
      byte[] pdfBytes,
      String title,
      String language,
      String author
  ) {
    if (pdfBytes == null || pdfBytes.length == 0) {
      return new byte[0];
    }

    try (PDDocument document = Loader.loadPDF(pdfBytes); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      String safeTitle = normalizeTitle(title);
      String safeLang = normalizeLanguage(language);
      String safeAuthor = (author == null || author.isBlank()) ? "GAM 2.0 Standalone" : author.trim();

      PDDocumentInformation info = document.getDocumentInformation();
      if (info == null) {
        info = new PDDocumentInformation();
        document.setDocumentInformation(info);
      }
      info.setTitle(safeTitle);
      info.setSubject("ZUGFeRD/Factur-X Rechnung");
      info.setAuthor(safeAuthor);
      info.setCreator("GAM 2.0 Standalone");
      info.setProducer("GAM 2.0 Standalone / OpenHTMLtoPDF / MustangProject");

      PDDocumentCatalog catalog = document.getDocumentCatalog();
      if (catalog != null) {
        catalog.setLanguage(safeLang);

        PDViewerPreferences preferences = catalog.getViewerPreferences();
        if (preferences == null) {
          preferences = new PDViewerPreferences(new COSDictionary());
          catalog.setViewerPreferences(preferences);
        }
        preferences.setDisplayDocTitle(true);

        PDMarkInfo markInfo = catalog.getMarkInfo();
        if (markInfo == null) {
          markInfo = new PDMarkInfo();
          catalog.setMarkInfo(markInfo);
        }
        markInfo.setMarked(true);

        ensureSimpleOutline(document, catalog, safeTitle);
        applyPdfUaXmpMetadata(document, safeTitle, safeLang, safeAuthor);
      }

      document.save(out);
      return out.toByteArray();
    } catch (Exception ex) {
      // The PDF export itself must stay stable; if PDFBox post-processing fails,
      // return the generated PDF rather than breaking invoice creation.
      return pdfBytes;
    }
  }

  private static void applyPdfUaXmpMetadata(PDDocument document, String title, String language, String author) throws Exception {
    String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    String safeTitle = xmlEscape(normalizeTitle(title));
    String safeLang = xmlEscape(normalizeLanguage(language));
    String safeAuthor = xmlEscape(author == null || author.isBlank() ? "GAM 2.0 Standalone" : author.trim());

    String xmp = """
      <?xpacket begin='﻿' id='W5M0MpCehiHzreSzNTczkc9d'?>
      <x:xmpmeta xmlns:x='adobe:ns:meta/'>
        <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>
          <rdf:Description rdf:about=''
              xmlns:dc='http://purl.org/dc/elements/1.1/'
              xmlns:xmp='http://ns.adobe.com/xap/1.0/'
              xmlns:pdf='http://ns.adobe.com/pdf/1.3/'
              xmlns:pdfaid='http://www.aiim.org/pdfa/ns/id/'
              xmlns:pdfuaid='http://www.aiim.org/pdfua/ns/id/'>
            <dc:title><rdf:Alt><rdf:li xml:lang='x-default'>%s</rdf:li><rdf:li xml:lang='%s'>%s</rdf:li></rdf:Alt></dc:title>
            <dc:creator><rdf:Seq><rdf:li>%s</rdf:li></rdf:Seq></dc:creator>
            <dc:description><rdf:Alt><rdf:li xml:lang='x-default'>ZUGFeRD/Factur-X Rechnung</rdf:li></rdf:Alt></dc:description>
            <xmp:CreatorTool>GAM 2.0 Standalone / OpenHTMLtoPDF</xmp:CreatorTool>
            <xmp:CreateDate>%s</xmp:CreateDate>
            <xmp:ModifyDate>%s</xmp:ModifyDate>
            <xmp:MetadataDate>%s</xmp:MetadataDate>
            <pdf:Producer>GAM 2.0 Standalone / OpenHTMLtoPDF / MustangProject</pdf:Producer>
            <pdfaid:part>3</pdfaid:part>
            <pdfaid:conformance>U</pdfaid:conformance>
            <pdfuaid:part>1</pdfuaid:part>
          </rdf:Description>
        </rdf:RDF>
      </x:xmpmeta>
      <?xpacket end='w'?>
      """.formatted(safeTitle, safeLang, safeTitle, safeAuthor, now, now, now);

    PDMetadata metadata = new PDMetadata(document);
    metadata.importXMPMetadata(xmp.getBytes(StandardCharsets.UTF_8));
    document.getDocumentCatalog().setMetadata(metadata);
  }

  private static void ensureSimpleOutline(PDDocument document, PDDocumentCatalog catalog, String title) {
    try {
      if (document.getNumberOfPages() < 1) {
        return;
      }

      PDDocumentOutline outline = catalog.getDocumentOutline();
      if (outline == null) {
        outline = new PDDocumentOutline();
        catalog.setDocumentOutline(outline);
      }

      // Avoid duplicate outline items when the helper is called twice.
      if (outline.getFirstChild() != null) {
        outline.openNode();
        return;
      }

      PDPageFitDestination destination = new PDPageFitDestination();
      destination.setPage(document.getPage(0));

      PDOutlineItem item = new PDOutlineItem();
      item.setTitle(normalizeTitle(title));
      item.setDestination(destination);
      outline.addLast(item);
      outline.openNode();
      item.openNode();
    } catch (Exception ignored) {
      // Bookmark creation is only PAC/navigation cleanup.
    }
  }

  private static String normalizeTitle(String title) {
    return (title == null || title.isBlank()) ? "Rechnung" : title.trim();
  }

  private static String normalizeLanguage(String language) {
    if (language == null || language.isBlank()) {
      return "de";
    }
    String l = language.toLowerCase(java.util.Locale.ROOT);
    return switch (l.substring(0, Math.min(2, l.length()))) {
      case "en" -> "en";
      case "fr" -> "fr";
      case "it" -> "it";
      case "uk" -> "uk";
      default -> "de";
    };
  }

  private static String xmlEscape(String s) {
    if (s == null) {
      return "";
    }
    return s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
