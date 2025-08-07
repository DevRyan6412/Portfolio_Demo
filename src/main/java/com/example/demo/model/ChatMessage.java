package com.example.demo.model;

import org.springframework.util.StringUtils;
import java.util.Date;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String content;
    private Date timestamp;
    private String roomId;
            private String projectId;
    private MessageType type;

    // 메시지 타입 열거형
    public enum MessageType {
        TEXT, FILE, SYSTEM
    }

    // 기본 생성자 (Firebase에서 필요)
    public ChatMessage() {
        this.timestamp = new Date();
        this.type = MessageType.TEXT;
    }

    // 생성자
    public ChatMessage(String senderId, String senderName, String content, String roomId, String projectId) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = new Date();
        this.roomId = roomId;
        this.projectId = projectId;
        this.type = MessageType.TEXT;
    }

    // 유효성 검사 메서드
    public void validate() {
        if (!StringUtils.hasText(this.projectId)) {
            throw new IllegalArgumentException("프로젝트 ID는 필수입니다.");
        }
        if (!StringUtils.hasText(this.content)) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다.");
        }
    }

    // Getter와 Setter 메서드들
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    // toString 메서드 추가 (디버깅에 유용)
    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId='" + messageId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", roomId='" + roomId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", type=" + type +
                '}';
    }
}