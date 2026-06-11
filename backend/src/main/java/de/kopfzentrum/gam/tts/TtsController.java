package de.kopfzentrum.gam.tts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tts")
public class TtsController {
  private final TtsService tts;
  public TtsController(TtsService tts) { this.tts = tts; }

  @GetMapping("/status")
  public TtsStatus status() { return tts.status(); }

  @PostMapping("/audio")
  public ResponseEntity<byte[]> audio(@RequestBody TtsRequest request) { return tts.audio(request); }
}
