package com.xtemper.connectme.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtemper.connectme.Helper.MessageEntry;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> chatRooms = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String messageId = extractMessageId(session);
        chatRooms.putIfAbsent(messageId, new CopyOnWriteArrayList<>());
        chatRooms.get(messageId).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String messageId = extractMessageId(session);
        List<WebSocketSession> sessions = chatRooms.getOrDefault(messageId, new CopyOnWriteArrayList<>());

        // Convert incoming JSON to MessageEntry
        MessageEntry msg = objectMapper.readValue(message.getPayload(), MessageEntry.class);
        msg.setTime(System.currentTimeMillis()); // Set the timestamp

        // Convert back to JSON
        String responseJson = objectMapper.writeValueAsString(msg);

        // Broadcast to all users in the chat room
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(responseJson));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String messageId = extractMessageId(session);
        chatRooms.getOrDefault(messageId, new CopyOnWriteArrayList<>()).remove(session);
    }

    private String extractMessageId(WebSocketSession session) {
        return session.getUri().getPath().split("/chat/")[1];
    }
}
