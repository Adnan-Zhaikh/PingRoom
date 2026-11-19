package com.chatroom.app.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    /** roomId -> (sessionId -> display name) */
    private final Map<String, Map<String, String>> rooms = new ConcurrentHashMap<>();
    /** sessionId -> roomId, so disconnect can find the room */
    private final Map<String, String> sessionRoom = new ConcurrentHashMap<>();

    public List<String> join(String roomId, String sessionId, String name) {
        sessionRoom.put(sessionId, roomId);
        rooms.computeIfAbsent(roomId, r -> new ConcurrentHashMap<>()).put(sessionId, name);
        return members(roomId);
    }

    /** Returns the room the session was in, or null if it wasn't tracked. */
    public String leave(String sessionId) {
        String roomId = sessionRoom.remove(sessionId);
        if (roomId == null) return null;
        Map<String, String> members = rooms.get(roomId);
        if (members != null) {
            members.remove(sessionId);
            if (members.isEmpty()) rooms.remove(roomId);
        }
        return roomId;
    }

    public List<String> members(String roomId) {
        return rooms.getOrDefault(roomId, Map.of())
                .values().stream().sorted().toList();
    }
}
