# 40k37e56 – macOS Apple-Mobile Vorbereitung

Dieser Stand bereitet den unter Windows und Linux bereits verwendeten Apple-Mobile-Pfad für macOS vor. Der reale iPad-Test auf macOS steht ausdrücklich noch aus.

- macOS wird bei der Toolchain-Installation separat erkannt.
- Homebrew stellt `libimobiledevice`, `ideviceinstaller` und Python 3.13 bereit.
- `pymobiledevice3==11.16.3` wird isoliert unter `tools/apple-coredevice/runtime/macos-venv` installiert.
- Intel/x86_64 und Apple-Silicon/arm64 verwenden jeweils die native Homebrew-/Python-Architektur.
- Die bestehende CoreDevice-/RemotePairing-/USERSPACE_RSD-Discovery bleibt unverändert und verwendet den neuen macOS-Plattformadapter.
- FRITZ!Box-Treffer und manueller Merge aus 40k37e55k bleiben unverändert erhalten.

Status: vorbereitet; macOS-Realtest ausstehend.
