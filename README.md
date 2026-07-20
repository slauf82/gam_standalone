# GAM 2.0

**Modern Enterprise Management Platform**

🇩🇪 **Deutsch:** [README_DE.md](README_DE.md)

## Overview

GAM 2.0 is the successor to a long-running enterprise management
application. It started as a migration from a Java EE / JSF / GlassFish
system but has evolved into a modern platform with capabilities beyond
the original application.

Core technologies:

-   Java / Spring Boot
-   React + TypeScript
-   MariaDB
-   JWT, TOTP and WebAuthn Passkeys

## Highlights

-   Multilingual user interface
-   Enterprise administration
-   Workflow engine
-   Invoice administration
-   Accessible PDF generation (PDF/UA)
-   WCAG-oriented document workflows
-   ZUGFeRD / Factur-X support
-   Multilingual text-to-speech
-   Reporting and exports
-   Multi-source device discovery and inventory management
-   Modular architecture

## Multi-source Device Manager

Instead of manually maintaining devices, GAM collects information from
multiple sources, consolidates matching records and automatically
maintains the central inventory.

## Accessibility

GAM integrates accessibility into the document workflow:

-   Tagged PDF generation
-   PDF/UA compatible documents
-   WCAG-oriented document structure
-   Multilingual invoices
-   Text-to-speech support

## Architecture

Backend: - Spring Boot - REST APIs - MariaDB

Frontend: - React - TypeScript - Vite

## Vision

GAM is designed as a long-lived enterprise platform that combines
administration, workflows, inventory management and accessible document
generation within one modular system.

## License

GNU GPL v3.0
