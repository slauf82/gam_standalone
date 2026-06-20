package de.kopfzentrum.gam.tts;

import java.util.List;
import java.util.Map;

public record TtsStatus(
  boolean enabled,
  String engine,
  String mode,
  boolean piperAvailable,
  String piperAvailabilityMessage,
  Map<String, Object> piperDiagnostics,
  String piperExecutable,
  String piperVoicesDir,
  Map<String, String> piperVoices,
  Map<String, String> languageLabels,
  List<String> installedLanguages,
  List<String> supportedLanguages,
  List<String> standardLanguages,
  List<String> downloadingLanguages,
  Map<String, String> downloadErrors,
  boolean autoDownload,
  boolean browserFallback,
  Map<String, List<String>> fallback
) {}
