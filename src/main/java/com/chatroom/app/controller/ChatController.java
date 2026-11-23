package com.chatroom.app.controller;

import com.chatroom.app.model.ChatMessage;
import com.chatroom.app.service.PomodoroService;
import com.chatroom.app.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChatController {

    public record PresenceUpdate(List<String> members) {}

    private final SimpMessagingTemplate broker;
    private final PresenceService presence;
    private final PomodoroService pomodoro;

    private final Map<String, Map<String, ChatMessage>> roomMessages = new ConcurrentHashMap<>();

    public ChatController(SimpMessagingTemplate broker, PresenceService presence, PomodoroService pomodoro) {
        this.broker = broker;
        this.presence = presence;
        this.pomodoro = pomodoro;
    }

    @MessageMapping("/room/{roomId}/join")
    public void join(@DestinationVariable String roomId, @Payload Map<String, String> body,
                     SimpMessageHeaderAccessor headers) {
        String sessionId = headers.getSessionId();
        String name = body.getOrDefault("name", "anon");
        List<String> members = presence.join(roomId, sessionId, name);
        broker.convertAndSend("/topic/room/" + roomId + "/presence", new PresenceUpdate(members));
        broker.convertAndSend("/topic/room/" + roomId + "/timer", pomodoro.get(roomId));
    }

    @MessageMapping("/room/{roomId}/chat")
    public void chat(@DestinationVariable String roomId, ChatMessage message) {
        message.setId(UUID.randomUUID().toString());
        message.setStarred(false);
        roomMessages
            .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
            .put(message.getId(), message);

        broker.convertAndSend("/topic/room/" + roomId + "/chat", message);
    }

    @MessageMapping("/room/{roomId}/star/{messageId}")
    public void star(@DestinationVariable String roomId,
                     @DestinationVariable String messageId,
                     @Payload Map<String, Boolean> payload) {
        Map<String, ChatMessage> msgs = roomMessages.get(roomId);
        if (msgs == null) return;

        ChatMessage msg = msgs.get(messageId);
        if (msg == null) return;

        msg.setStarred(Boolean.TRUE.equals(payload.get("starred")));
        broker.convertAndSend("/topic/room/" + roomId + "/star", msg);
    }

    @MessageMapping("/room/{roomId}/timer/start")
    public void startTimer(@DestinationVariable String roomId, @Payload Map<String, Integer> body) {
        broker.convertAndSend("/topic/room/" + roomId + "/timer",
                pomodoro.start(roomId, body.getOrDefault("minutes", 25)));
    }

    @MessageMapping("/room/{roomId}/timer/stop")
    public void stopTimer(@DestinationVariable String roomId) {
        broker.convertAndSend("/topic/room/" + roomId + "/timer", pomodoro.stop(roomId));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String roomId = presence.leave(event.getSessionId());
        if (roomId != null) {
            broker.convertAndSend("/topic/room/" + roomId + "/presence",
                    new PresenceUpdate(presence.members(roomId)));
        }
    }

    @GetMapping("/")
    public String index() { return "redirect:/chat"; }

    @GetMapping("/chat")
    public String chat() { return "redirect:/room/lobby"; }

    @GetMapping("/room/{roomId}")
    public String room(@PathVariable String roomId, Model model) {
        model.addAttribute("roomId", roomId);
        return "room";
    }
}
