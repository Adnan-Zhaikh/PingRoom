package com.chatroom.app.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class PomodoroService {

    public record TimerState(String phase, int durationSeconds, Instant endsAt, boolean running) {}

    private final Map<String, TimerState> states = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public TimerState start(String roomId, int minutes) {
        int seconds = Math.max(1, minutes) * 60;
        TimerState state = new TimerState("FOCUS", seconds, Instant.now().plusSeconds(seconds), true);
        states.put(roomId, state);
        scheduler.schedule(() -> states.put(roomId, new TimerState("BREAK", 5 * 60,
                Instant.now().plusSeconds(5 * 60), true)), seconds, TimeUnit.SECONDS);
        return state;
    }

    public TimerState stop(String roomId) {
        TimerState stopped = new TimerState("IDLE", 0, null, false);
        states.put(roomId, stopped);
        return stopped;
    }

    public TimerState get(String roomId) {
        return states.getOrDefault(roomId, new TimerState("IDLE", 0, null, false));
    }
}
