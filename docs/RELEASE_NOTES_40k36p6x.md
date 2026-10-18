# 40k36p6x – LAN-fähiger Login

- Frontend-API-Ziel wird standardmäßig aus dem Hostnamen der geöffneten GAM-Oberfläche abgeleitet.
- `http://localhost:5173` verwendet automatisch `http://localhost:8080/api`.
- `http://192.168.x.x:5173` verwendet automatisch denselben Host auf Port 8080.
- `VITE_GAM_API` bleibt als expliziter Override erhalten.
- Bestehende CORS-Freigaben für private IPv4-Netze bleiben unverändert.
