package de.kopfzentrum.gam.tts;

import java.util.List;
import java.util.Map;

public record TtsStatus(
  boolean enabled,
  String engine,
  String mode,
  boolean maryTtsEnabled,
  boolean maryTtsBundled,
  boolean maryTtsBundledStarted,
  boolean maryTtsBundledAvailable,
  String maryTtsBundledError,
  String maryTtsHome,
  String maryTtsEndpoint,
  boolean maryTtsReachable,
  boolean browserFallback,
  Map<String, List<String>> fallback
) {}
