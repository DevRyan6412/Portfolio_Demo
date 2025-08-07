package com.example.demo.service;

import com.example.demo.model.ChatMessage;
import com.google.firebase.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class ChatService {

    private final FirebaseDatabase firebaseDatabase;

    public ChatService(FirebaseDatabase firebaseDatabase) {
        this.firebaseDatabase = firebaseDatabase;
    }

    // 메시지 전송
    public CompletableFuture<String> sendMessage(ChatMessage message) {
        DatabaseReference chatRef = firebaseDatabase.getReference("chats/" + message.getRoomId());
        DatabaseReference newMessageRef = chatRef.push();

        CompletableFuture<String> future = new CompletableFuture<>();

        newMessageRef.setValueAsync(message).addListener(() -> {
            message.setMessageId(newMessageRef.getKey());
            future.complete(newMessageRef.getKey());
        }, Runnable::run);

        return future;
    }

    // 채팅방의 메시지 가져오기
    public CompletableFuture<List<ChatMessage>> getMessages(String roomId, int limit) {
        DatabaseReference chatRef = firebaseDatabase.getReference("chats/" + roomId);
        Query query = chatRef.orderByChild("timestamp").limitToLast(limit);

        CompletableFuture<List<ChatMessage>> future = new CompletableFuture<>();

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        message.setMessageId(snapshot.getKey());
                        messages.add(message);
                    }
                }
                future.complete(messages);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }

    // 실시간 메시지 리스너 등록
    public void subscribeToMessages(String roomId, Consumer<ChatMessage> onMessageReceived) {
        DatabaseReference chatRef = firebaseDatabase.getReference("chats/" + roomId);

        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    message.setMessageId(snapshot.getKey());
                    onMessageReceived.accept(message);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                // 필요한 경우 구현
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // 필요한 경우 구현
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // 필요한 경우 구현
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // 에러 처리
            }
        });
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 프로젝트 멤버십 확인
    public CompletableFuture<Boolean> isProjectMember(String projectId, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM project_member WHERE project_id = ? AND user_id = ?";
            int count = jdbcTemplate.queryForObject(sql, Integer.class, projectId, userId);
            return count > 0;
        });
    }

//    // 프로젝트 멤버 역할 확인 (선택적)
//    public CompletableFuture<String> getProjectMemberRole(String projectId, String userId) {
//        return CompletableFuture.supplyAsync(() -> {
//            String sql = "SELECT project_role FROM project_member WHERE project_id = ? AND user_id = ?";
//            try {
//                return jdbcTemplate.queryForObject(sql, String.class, projectId, userId);
//            } catch (EmptyResultDataAccessException e) {
//                return null;
//            }
//        });
//    }
}