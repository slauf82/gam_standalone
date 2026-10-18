# 40k36q2

SSH-Verbindungstests werden nicht mehr von einem vorgeschalteten Java-Socket-Test abgebrochen. Apache MINA SSHD versucht die Verbindung direkt; die TCP-Prüfung dient nur noch als ergänzende Diagnose und erhält bei Bedarf bis zu 10 Sekunden Zeit.
