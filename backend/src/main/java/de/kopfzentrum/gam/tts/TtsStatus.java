package de.kopfzentrum.gam.tts;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record TtsStatus(
  boolean enabled,
  String engine,
  boolean maryTtsEnabled,
  boolean maryTtsEmbedded,
  boolean maryTtsEmbeddedAvailable,
  String maryTtsEmbeddedError,
  Set<String> maryTtsEmbeddedVoices,
  boolean maryTtsBundled,
  String maryTtsHome,
  String maryTtsEndpoint,
  boolean maryTtsReachable,
  boolean browserFallback,
  Map<String, List<String>> fallback
) {}
