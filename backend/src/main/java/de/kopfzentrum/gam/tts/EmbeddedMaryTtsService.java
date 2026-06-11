package de.kopfzentrum.gam.tts;

import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

@Service
public class EmbeddedMaryTtsService {
  private Object mary;
  private Method generateAudio;
  private Method setLocale;
  private Method setVoice;
  private Method getAvailableVoices;
  private boolean initialized = false;
  private String initError = "";

  public synchronized boolean available() {
    ensureInitialized();
    return initialized;
  }

  public synchronized String error() {
    ensureInitialized();
    return initError;
  }

  public synchronized Set<String> voices() {
    ensureInitialized();
    if (!initialized || getAvailableVoices == null) return Set.of();
    try {
      Object result = getAvailableVoices.invoke(mary);
      if (result instanceof Set<?> set) {
        return set.stream().map(String::valueOf).collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
      }
    } catch (Exception ignored) {}
    return Set.of();
  }

  public synchronized byte[] synthesize(String lang, String text) {
    ensureInitialized();
    if (!initialized) throw new IllegalStateException("Embedded MaryTTS not available: " + initError);
    try {
      String voice = voiceFor(lang);
      Locale locale = localeFor(lang);
      if (setLocale != null) setLocale.invoke(mary, locale);
      if (setVoice != null && voice != null && !voice.isBlank()) setVoice.invoke(mary, voice);

      Object audio = generateAudio.invoke(mary, text == null ? "" : text);
      if (!(audio instanceof AudioInputStream ais)) {
        throw new IllegalStateException("MaryTTS did not return AudioInputStream");
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Embedded MaryTTS synthesis failed: " + ex.getMessage(), ex);
    }
  }

  private void ensureInitialized() {
    if (initialized || !initError.isBlank()) return;
    try {
      Class<?> clazz = Class.forName("marytts.LocalMaryInterface");
      mary = clazz.getConstructor().newInstance();
      generateAudio = clazz.getMethod("generateAudio", String.class);
      setLocale = clazz.getMethod("setLocale", Locale.class);
      setVoice = clazz.getMethod("setVoice", String.class);
      getAvailableVoices = clazz.getMethod("getAvailableVoices");
      initialized = true;
    } catch (Exception ex) {
      initialized = false;
      initError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }
  }

  private Locale localeFor(String lang) {
    String l = lang == null ? "de" : lang.toLowerCase(Locale.ROOT);
    if (l.startsWith("en")) return Locale.US;
    if (l.startsWith("fr")) return Locale.FRENCH;
    if (l.startsWith("uk") || l.startsWith("ru")) return new Locale("ru");
    return Locale.GERMAN;
  }

  private String voiceFor(String lang) {
    Set<String> voices = voices();
    String l = lang == null ? "de" : lang.toLowerCase(Locale.ROOT);

    if (l.startsWith("de")) {
      return firstAvailable(voices, "bits3-hsmm", "bits1-hsmm");
    }
    if (l.startsWith("fr")) {
      return firstAvailable(voices, "enst-camille-hsmm", "upmc-pierre-hsmm");
    }
    if (l.startsWith("uk") || l.startsWith("ru")) {
      return firstAvailable(voices, "russian", "voice-russian", "cmu-slt-hsmm");
    }
    return firstAvailable(voices, "cmu-slt-hsmm");
  }

  private String firstAvailable(Set<String> voices, String... preferred) {
    for (String p : preferred) {
      for (String v : voices) {
        if (v.equalsIgnoreCase(p) || v.toLowerCase(Locale.ROOT).contains(p.toLowerCase(Locale.ROOT))) return v;
      }
    }
    return voices.stream().findFirst().orElse(null);
  }
}
