GAM Standalone
Version 2.1.0 · Cross-platform device inventory and management software
[Deutsch](README.md) · [Release Notes 2.1.0](docs/RELEASE_NOTES_2.1.0_EN.md) · [Development Roadmap](docs/DEVELOPMENT_PLAN.md)
GAM (GAM Standalone / GAM 2.0) combines device discovery and inventory with administrative functionality in a web-based application. Its goal is reliable, transparent management of heterogeneous devices and business workflows, with a long-term vision of a modular platform.
Current status: GAM 2.1.0 provides a stabilized technical foundation. The future GAM Core, .gam module manager, extension catalog, and free/paid extension marketplace are planned, not features already shipped in 2.1.0.

What GAM 2.1.0 provides
Device discovery and inventory
Platform	Supported methods
Windows	Backend-local inventory; remote inventory through WinRM, including native Windows PowerShell/SSPI fallback
Linux	Backend-local inventory; remote inventory through SSH
macOS	Local inventory and SSH access
Android	ADB over USB and Wi-Fi
iOS / iPadOS	Apple device discovery and USB/Wi-Fi inventory paths, subject to OS version, permissions, and available transport


Device management also includes:
- Configuration of discovery sources and OS access profiles, connection tests, and inventory runs.
- Association of records collected from multiple sources, with visibility into device addresses and provenance.
- Conservative automatic merge suggestions based on strong device identity evidence, not just similar names or broad device types.
- Detailed platform-specific operating system, hardware, software, and other inventory information.
- Stable device-list row widths even when large sets of inventory details are expanded.
Other existing administrative features
In addition to its device manager, GAM already includes invoicing and invoice-data features, user and permission management, and other existing administration areas, including stock/material-related functionality. Availability and completeness vary by feature and configuration.
Technology stack
- Backend: Java 21, Spring Boot, Maven
- Frontend: React, TypeScript, Vite
- Database: MariaDB/MySQL
- Distribution: complete project with launch scripts for Windows, Linux, and macOS
The downloadable ZIP contains the project/source code, not precompiled installers. Initial setup may download runtime components and dependencies.
Quick start
1. Extract the full release archive into a dedicated directory.
2. Start the backend:
   - Windows: start_backend.bat
   - Linux/macOS: ./start_backend.sh
3. Complete initial setup, including database configuration. See env.example for manual configuration options.
4. Start the frontend:
   - Windows: start_frontend.bat
   - Linux/macOS: ./start_frontend.sh
5. Open the local address printed by the frontend in your browser.
Notes: Initial setup may require an internet connection. Remote inventory requires appropriate permissions and enabled services (such as WinRM, SSH, or ADB). Back up databases, configuration, and local changes before upgrading. Launch scripts can automatically provision parts of the required toolchain.
What's new in 2.1.0?
This release consolidates cross-platform inventory and improves Windows WinRM with native SSPI fallback, reliable device-merge candidate selection, and frontend startup. The final stabilization fixes address misleading historical source entries, an unnecessary whoami.exe helper invocation, and layout issues caused by unusually long inventory details.
For the full change summary, tested paths, and limitations, read the [English release notes](docs/RELEASE_NOTES_2.1.0_EN.md) or [German release notes](docs/RELEASE_NOTES_2.1.0_DE.md).
Roadmap: a modular GAM platform
Future versions aim to evolve GAM into an extensible administrative and integration platform:
Planned release	Focus
2.1.5	GAM Core, module boundaries, integration APIs, and the foundation of .gam packages
2.2.x	Additional infrastructure/virtualization integrations and deeper hardware analysis
2.3–2.4	Business modules, third-party integrations, permissions, and scalability
2.5.0	Documentation, quality assurance, and product maturity
3.0–3.5	AI assistance and controlled automation


Product vision: A standalone, freely accessible GAM Core with both free and optional paid extensions. Extensions are intended to be independently versioned, installed, and managed, with support for publicly maintained as well as privately developed modules. This extension and licensing ecosystem is not yet included in 2.1.0.
Roadmap versions and scope may change. See the [development plan](docs/DEVELOPMENT_PLAN.md) for details.
Documentation
- [Release Notes 2.1.0 (English)](docs/RELEASE_NOTES_2.1.0_EN.md)
- [Release Notes 2.1.0 (Deutsch)](docs/RELEASE_NOTES_2.1.0_DE.md)
- [Development Plan / Roadmap](docs/DEVELOPMENT_PLAN.md)
- [GitHub release text](docs/GITHUB_RELEASE_2.1.0.md)
- [Additional technical documentation](docs/)
Project and contributions
GitHub: slauf82/gam_standalone
Bug reports and reproducible test cases are welcome, especially for different platforms and connection methods. Changes should be regression-tested against existing inventory, authentication, and device-identity flows.
License, authorship, and official releases
GAM Standalone 2.1.0 is released as free software under the GNU General Public License Version 3 (GPL-3.0-only). The complete license text is available in LICENSE. Third-party components may be subject to their own licenses.
GAM was originally developed and is officially released by Sebastian Lauf. When redistributing the software, retain all copyright, license, and other legal notices required by GPLv3.
Extensions and derivative versions are welcome: Under GPLv3, GAM may be used, studied, modified, and redistributed. This includes developing extensions and using AI tools to modify or compile the software. When distributing modified versions, clearly identify changes as required by GPLv3 and comply with the applicable license obligations.
Official GAM releases are published only by the original project or explicitly authorized publishers, particularly through the official repository. Modified versions or third-party builds are unofficial and must not be misleadingly represented as official GAM releases.
Licensing the source code under GPLv3 does not itself grant trademark rights to the GAM name, logos, or project identifiers. Separate rules may apply to their use as brand identifiers. This notice does not imply that a trademark is registered.
AI tools: There is no prohibition on AI-assisted modifications or recompilation. For traceability and support purposes, the project recommends official releases for regular use. Properly identified third-party builds remain permitted under GPLv3.
Future extension modules: Their respective licenses will be identified separately. License compatibility must be reviewed before distributing modules that are tightly integrated with the GPLv3 Core; keeping source code in a private repository does not, by itself, remove GPL obligations.
