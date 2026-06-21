@echo off
setlocal
chcp 65001 >nul

echo ============================================================
echo GAM 2.0 - PDF/UA + ZUGFeRD Checkhinweise
echo ============================================================
echo.
echo 1. Backend starten:
echo    start-backend.bat
echo.
echo 2. Rechnung als PDF erzeugen, z.B.:
echo    http://localhost:8080/api/invoices/3670/pdf?lang=de^&companyId=1
echo.
echo 3. PDF mit PAC 2024 oder Acrobat Preflight pruefen:
echo    - PDF/UA-1
 echo   - Tags / Lesereihenfolge
 echo   - Dokumentensprache
 echo   - Tabellenstruktur
 echo   - Alternativtexte
 echo   - eingebettetes ZUGFeRD/Factur-X XML
 echo.
echo Hinweis: Dieser Batch kann PAC nicht automatisch ausfuehren, weil PAC ein externes Windows-Pruefprogramm ist.
echo Das PDF wird von GAM mit PDF/UA-Metadaten und Tagged-PDF-Grundstruktur erzeugt; die finale Freigabe sollte per PAC erfolgen.
echo.
pause
