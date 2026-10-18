# 40k36t44 – markierter WinRM-Rückkanal

- Basis: 40k36t43.
- Jeder Base64-Rückgabechunk wird zwischen eindeutigen `GAM_B64_BEGIN_n` / `GAM_B64_END_n`-Markern ausgegeben.
- Java übernimmt ausschließlich den Inhalt zwischen diesen Markern.
- Whitespace innerhalb des Payloads wird entfernt.
- Vor dem Decode werden erwartete und tatsächliche Base64-Länge protokolliert.
- Bei ungültigen Zeichen werden Position und Hexwert des ersten ungültigen Zeichens ausgegeben.
- Der restliche Linux→Windows-Legacy-NTLM-Pfad bleibt unverändert.
