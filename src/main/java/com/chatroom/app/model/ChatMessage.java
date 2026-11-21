package com.chatroom.app.model;

public class ChatMessage {
    private String id;
    private String sender;
    private String content;
    private boolean starred;

    public ChatMessage() {}

    public ChatMessage(String id, String sender, String content) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.starred = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isStarred() { return starred; }
    public void setStarred(boolean starred) { this.starred = starred; }
}
