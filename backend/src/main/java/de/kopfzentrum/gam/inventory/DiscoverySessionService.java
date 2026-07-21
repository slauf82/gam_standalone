package de.kopfzentrum.gam.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.CancellationException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Schritt 40k30e: Entkoppelt die eigentliche Discovery vollständig von der HTTP-Verbindung.
 * Jede Session hält einen kleinen Ereignispuffer, damit der Browser nach dem Start-Endpunkt
 * den SSE-Kanal öffnen kann, ohne die ersten Treffer zu verlieren.
 */
@Service
public class DiscoverySessionService {
  private static final Logger log = LoggerFactory.getLogger(DiscoverySessionService.class);
  private static final AtomicLong SESSION_SEQUENCE = new AtomicLong();
  private static final AtomicLong WORKER_SEQUENCE = new AtomicLong();
  private static final long EMITTER_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(15);
  private static final int MAX_BUFFERED_EVENTS = 10_000;

  private final DeviceDiscoveryService discoveryService;
  private final ObjectMapper objectMapper;
  private final ExecutorService workers = Executors.newCachedThreadPool(runnable -> {
    Thread thread = new Thread(runnable, "gam-discovery-worker");
    thread.setDaemon(true);
    return thread;
  });
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();

  public DiscoverySessionService(DeviceDiscoveryService discoveryService, ObjectMapper objectMapper) {
    this.discoveryService = discoveryService;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> start() {
    String sessionId = UUID.randomUUID().toString();
    long sessionNo = SESSION_SEQUENCE.incrementAndGet();
    Session session = new Session(sessionId, sessionNo);
    sessions.put(sessionId, session);
    log.warn("[DISCOVERY-TRACE] SESSION_CREATE sessionNo={} sessionId={} thread={} activeSessions={} caller={}",
      sessionNo, sessionId, Thread.currentThread().getName(), sessions.size(), callerSummary());
    // 40k30i: Der Start-Endpunkt legt nur die Session an. Die Discovery beginnt
    // erst, nachdem der Browser den SSE-Kanal erfolgreich geöffnet hat. Dadurch
    // gibt es keinen unsichtbaren Vorlauf und exakt einen Scan pro Session.
    return Map.of("sessionId", sessionId, "sessionNo", sessionNo, "startedAt", session.startedAt);
  }


  /**
   * 40k30n: pufferungsunabhaengiger Live-Kanal. Der Browser holt kleine
   * Ereignispakete per Long-Polling ab. Jeder Aufruf wartet hoechstens eine
   * Sekunde auf das naechste Callback-Ereignis. Dadurch koennen weder Servlet-
   * noch Proxy-Puffer das UI bis zum globalen Abschluss blockieren.
   */
  public Map<String,Object> poll(String sessionId, long afterEventId) {
    Session session = sessions.get(sessionId);
    if (session == null) {
      return Map.of("sessionId", sessionId, "events", List.of(event("error", "message", "Discovery-Session wurde nicht gefunden.")), "finished", true, "lastEventId", afterEventId);
    }
    startWorkerIfNeeded(session, "LONG_POLL");
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
    synchronized (session.monitor) {
      while (!session.finished && session.lastEventId <= afterEventId) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) break;
        try { TimeUnit.NANOSECONDS.timedWait(session.monitor, remaining); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
      }
      List<Map<String,Object>> batch = new ArrayList<>();
      long last = afterEventId;
      for (PublishedEvent published : session.events) {
        if (published.id > afterEventId) {
          LinkedHashMap<String,Object> item = new LinkedHashMap<>(published.payload);
          item.put("eventId", published.id);
          batch.add(item);
          last = published.id;
        }
      }
      return Map.of(
        "sessionId", session.id,
        "sessionNo", session.sessionNo,
        "workerNo", session.workerNo,
        "events", batch,
        "finished", session.finished,
        "lastEventId", last
      );
    }
  }

  private void startWorkerIfNeeded(Session session, String trigger) {
    if (!session.started.compareAndSet(false, true)) return;
    long workerNo = WORKER_SEQUENCE.incrementAndGet();
    session.workerNo = workerNo;
    log.warn("[DISCOVERY-TRACE] WORKER_SUBMIT workerNo={} sessionNo={} sessionId={} trigger={} thread={}", workerNo, session.sessionNo, session.id, trigger, Thread.currentThread().getName());
    publish(session, event("trace", "event", "WORKER_SUBMIT", "workerNo", workerNo, "sessionNo", session.sessionNo, "sessionId", session.id, "trigger", trigger, "timestamp", OffsetDateTime.now().toString()));
    publish(session, event("progress", "progress", 0, "phase", "Gerätesuche wird vorbereitet"));
    session.workerFuture = workers.submit(() -> runDiscovery(session));
  }

  /**
   * 40k30m: echter ungepufferter SSE-Ausgabekanal. Die Antwort wird als
   * StreamingResponseBody geschrieben und nach jedem Block explizit geflusht.
   */
  public void stream(String sessionId, OutputStream output) throws IOException {
    Session session = sessions.get(sessionId);
    log.warn("[DISCOVERY-TRACE] SSE_STREAM_OPEN sessionId={} found={} thread={} caller={}",
      sessionId, session != null, Thread.currentThread().getName(), callerSummary());
    if (session == null) {
      writeBlock(output, 1, event("error", "message", "Discovery-Session wurde nicht gefunden.",
        "timestamp", OffsetDateTime.now().toString()));
      return;
    }

    // Sofortiger, hinreichend großer Kommentar gegen Proxy-/Servlet-Pufferung.
    output.write((":" + " ".repeat(8192) + "\n\n").getBytes(StandardCharsets.UTF_8));
    output.flush();

    StreamRegistration registration = new StreamRegistration(new LinkedBlockingQueue<>());
    boolean startWorker;
    synchronized (session.monitor) {
      for (PublishedEvent buffered : session.events) registration.queue.offer(buffered);
      if (!session.finished) session.streams.add(registration);
      startWorker = session.started.compareAndSet(false, true);
      log.warn("[DISCOVERY-TRACE] SSE_STREAM_READY sessionNo={} sessionId={} bufferedEvents={} streams={} workerStartWon={} thread={}",
        session.sessionNo, session.id, session.events.size(), session.streams.size(), startWorker, Thread.currentThread().getName());
    }

    if (startWorker) {
      // compareAndSet wurde bereits oben gewonnen; Worker deshalb hier direkt anlegen.
      long workerNo = WORKER_SEQUENCE.incrementAndGet();
      session.workerNo = workerNo;
      log.warn("[DISCOVERY-TRACE] WORKER_SUBMIT workerNo={} sessionNo={} sessionId={} trigger=SSE thread={}", workerNo, session.sessionNo, session.id, Thread.currentThread().getName());
      publish(session, event("trace", "event", "WORKER_SUBMIT", "workerNo", workerNo, "sessionNo", session.sessionNo, "sessionId", session.id, "trigger", "SSE", "timestamp", OffsetDateTime.now().toString()));
      publish(session, event("progress", "progress", 0, "phase", "Gerätesuche wird vorbereitet"));
      session.workerFuture = workers.submit(() -> runDiscovery(session));
    }

    long lastHeartbeat = System.nanoTime();
    try {
      while (true) {
        PublishedEvent next;
        try { next = registration.queue.poll(2, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
        if (next != null) {
          if (next == PublishedEvent.END) break;
          writeBlock(output, next.id, next.payload);
          lastHeartbeat = System.nanoTime();
        } else if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - lastHeartbeat) >= 5) {
          output.write((": heartbeat " + OffsetDateTime.now() + "\n\n").getBytes(StandardCharsets.UTF_8));
          output.flush();
          lastHeartbeat = System.nanoTime();
        }
        if (session.finished && registration.queue.isEmpty()) break;
      }
    } finally {
      synchronized (session.monitor) { session.streams.remove(registration); }
      log.warn("[DISCOVERY-TRACE] SSE_STREAM_CLOSED sessionNo={} sessionId={} thread={}",
        session.sessionNo, session.id, Thread.currentThread().getName());
    }
  }

  private void writeBlock(OutputStream output, long id, Map<String,Object> payload) throws IOException {
    String type = java.util.Objects.toString(payload.get("type"), "message");
    String json = objectMapper.writeValueAsString(payload);
    String block = "id: " + id + "\n" + "event: " + type + "\n" + "data: " + json + "\n\n";
    output.write(block.getBytes(StandardCharsets.UTF_8));
    output.flush();
  }

  public Map<String,Object> cancel(String sessionId) {
    Session session = sessions.get(sessionId);
    if (session == null) return Map.of("sessionId", sessionId, "cancelled", false, "message", "Discovery-Session wurde nicht gefunden.");
    synchronized (session.monitor) {
      if (session.finished) return Map.of("sessionId", session.id, "cancelled", session.cancelled.get(), "finished", true);
      session.cancelled.set(true);
      Future<?> worker = session.workerFuture;
      if (worker != null) worker.cancel(true);
      publishLocked(session, event("cancelled", "message", "Gerätesuche wurde abgebrochen.", "timestamp", OffsetDateTime.now().toString()));
      finishLocked(session, null);
    }
    log.warn("[DISCOVERY-TRACE] SESSION_CANCELLED sessionNo={} sessionId={} workerNo={}", session.sessionNo, session.id, session.workerNo);
    return Map.of("sessionId", session.id, "cancelled", true, "finished", true);
  }

  private void runDiscovery(Session session) {
    long startedNanos = System.nanoTime();
    log.warn("[DISCOVERY-TRACE] WORKER_START workerNo={} sessionNo={} sessionId={} thread={} caller={}", session.workerNo, session.sessionNo, session.id, Thread.currentThread().getName(), callerSummary());
    try {
      discoveryService.scanStreaming(
        device -> {
          publish(session, event("device", "device", device));
          publish(session, event("registered-count", "count", discoveryService.registeredCount()));
        },
        (progress, phase) -> publish(session, event("progress", "progress", progress, "phase", phase)),
        diagnostic -> publish(session, event("diagnostic", "diagnostic", diagnostic))
      );
      if (session.cancelled.get() || Thread.currentThread().isInterrupted()) return;
      log.warn("[DISCOVERY-TRACE] WORKER_COMPLETE workerNo={} sessionNo={} sessionId={} durationMs={} thread={}", session.workerNo, session.sessionNo, session.id, (System.nanoTime()-startedNanos)/1_000_000L, Thread.currentThread().getName());
      publish(session, event("complete", "workerNo", session.workerNo, "sessionNo", session.sessionNo, "sessionId", session.id, "timestamp", OffsetDateTime.now().toString()));
      publish(session, event("stream-closing", "timestamp", OffsetDateTime.now().toString()));
      finish(session, null);
    } catch (CancellationException failure) {
      // Der Abbruch wurde bereits ueber cancel(...) publiziert und abgeschlossen.
    } catch (RuntimeException failure) {
      if (session.cancelled.get() || Thread.currentThread().isInterrupted()) return;
      log.error("[DISCOVERY-TRACE] WORKER_FAILED workerNo={} sessionNo={} sessionId={} durationMs={} thread={}", session.workerNo, session.sessionNo, session.id, (System.nanoTime()-startedNanos)/1_000_000L, Thread.currentThread().getName(), failure);
      publish(session, event("error", "message", java.util.Objects.toString(failure.getMessage(), failure.getClass().getSimpleName()),
        "timestamp", OffsetDateTime.now().toString()));
      finish(session, failure);
    }
  }

  private void publish(Session session, Map<String, Object> payload) {
    synchronized (session.monitor) { publishLocked(session, payload); }
  }

  private void publishLocked(Session session, Map<String,Object> payload) {
    if (session.finished) return;
    PublishedEvent published = new PublishedEvent(++session.lastEventId, payload);
    session.events.add(published);
    if (session.events.size() > MAX_BUFFERED_EVENTS) session.events.remove(0);
    for (StreamRegistration registration : session.streams) registration.queue.offer(published);
    session.monitor.notifyAll();
  }

  private void finish(Session session, Throwable failure) {
    synchronized (session.monitor) { finishLocked(session, failure); }
  }

  private void finishLocked(Session session, Throwable failure) {
    if (!session.completionStarted.compareAndSet(false, true)) return;
    session.finished = true;
    session.failure = failure;
    for (StreamRegistration registration : List.copyOf(session.streams)) registration.queue.offer(PublishedEvent.END);
    session.monitor.notifyAll();
    workers.submit(() -> {
      try { Thread.sleep(TimeUnit.MINUTES.toMillis(10)); }
      catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
      sessions.remove(session.id, session);
    });
  }


  private static String callerSummary() {
    return StackWalker.getInstance().walk(stream -> stream
      .filter(frame -> !frame.getClassName().startsWith("java.") && !frame.getClassName().startsWith("org.springframework") && !frame.getClassName().equals(DiscoverySessionService.class.getName()))
      .limit(6).map(frame -> frame.getClassName()+"#"+frame.getMethodName()+":"+frame.getLineNumber())
      .reduce((a,b) -> a+" <- "+b).orElse("unknown"));
  }

  private static Map<String, Object> event(String type, Object... pairs) {
    LinkedHashMap<String, Object> value = new LinkedHashMap<>();
    value.put("type", type);
    for (int i = 0; i + 1 < pairs.length; i += 2) value.put(String.valueOf(pairs[i]), pairs[i + 1]);
    return value;
  }


  @PreDestroy
  void shutdown() {
    workers.shutdownNow();
  }

  private static final class Session {
    final String id;
    final long sessionNo;
    volatile long workerNo;
    final String startedAt = OffsetDateTime.now().toString();
    final Object monitor = new Object();
    final List<PublishedEvent> events = new ArrayList<>();
    final List<StreamRegistration> streams = new ArrayList<>();
    long lastEventId;
    final AtomicBoolean started = new AtomicBoolean(false);
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    final AtomicBoolean completionStarted = new AtomicBoolean(false);
    volatile Future<?> workerFuture;
    boolean finished;
    Throwable failure;
    Session(String id, long sessionNo) { this.id = id; this.sessionNo = sessionNo; }
  }

  private record PublishedEvent(long id, Map<String, Object> payload) {
    private static final PublishedEvent END = new PublishedEvent(-1, Map.of());
  }
  private record StreamRegistration(BlockingQueue<PublishedEvent> queue) { }
}
