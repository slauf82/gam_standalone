# GAM Standalone 2.1.0 – Release Notes (English)

**Release preparation date:** October 18, 2026  
**Technical baseline:** 40k37e56c-fix48  
**Version:** 2.1.0

## Overview

GAM Standalone 2.1.0 provides cross-platform device discovery and inventory with a consolidated device management interface. Existing functionality covering invoicing, user access management, and device administration is retained.

## Discovery and inventory

- **Windows:** local inventory and remote inventory via WinRM, including a Windows-native PowerShell/SSPI fallback.
- **Linux:** local inventory and remote inventory over SSH.
- **macOS:** local and SSH-based inventory paths.
- **Android:** ADB-based discovery/inventory over USB or Wi-Fi.
- **iOS/iPadOS:** supported USB and Wi-Fi based Apple discovery/inventory paths, depending on OS version, access rights, and transport availability.
- Multiple discovery sources can be associated with a device. Automatic merge suggestions require strong identity evidence from successfully inventoried devices; an identical broad device category alone is insufficient.

## Final fixes

- Removed the overwhelming per-entry removal UI for historical sources while preserving underlying records (fix47).
- Suppressed contradictory historical Linux analysis display fields for positively identified Windows devices, without deleting data (fix47).
- Filtered empty inventory detail rows (fix47).
- Replaced an unnecessary external `whoami.exe` invocation in the native SSPI check path (fix47).
- Prevented expanded inventory details from stretching device-list main-row columns; long content remains accessible within details (fix48).
- Earlier fixes addressed reusable Node.js runtime/downloads, Windows frontend startup, platform/merge classification safeguards, and Java compiler warnings (fix38–fix46).

## Verification

Reported successful hands-on tests include Windows local inventory, WinRM with native SSPI, Linux backend-local and SSH inventory, and the corrected device row layout. macOS and mobile support paths were tested during development. This is not a claim of full automated regression coverage.

## Installation and upgrades

The archive contains the **complete source/project package**, not precompiled installers. Use the bundled backend/frontend launcher scripts for your platform. Back up configuration, databases, and local changes before replacing an existing installation. See `docs/` for project guidance.

## Build verification

ZIP integrity has been checked. Full Maven and TypeScript builds were not confirmed in the preparation environment; verify them on the target machine or in CI before publication.

## Roadmap

A separately versioned modular platform/core/extension architecture and a `.gam` package format are planned **after** 2.1.0 and are not included in this release.
