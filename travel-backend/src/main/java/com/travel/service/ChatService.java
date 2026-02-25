package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Constants;
import com.travel.entity.ChatMessage;
import com.travel.entity.ChatSession;
import com.travel.entity.User;
import com.travel.mapper.ChatMessageMapper;
import com.travel.mapper.ChatSessionMapper;
import com.travel.mapper.UserMapper;
import com.travel.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final UserMapper userMapper;

    public void sendMessage(ChatMessage message) {
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        updateSession(message);

        User sender = userMapper.selectById(message.getSenderId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", message.getId());
        payload.put("senderId", message.getSenderId());
        payload.put("receiverId", message.getReceiverId());
        payload.put("content", message.getContent());
        payload.put("msgType", message.getMsgType());
        payload.put("isRead", message.getIsRead());
        payload.put("createTime", message.getCreateTime());
        if (sender != null) {
            payload.put("senderName", sender.getNickname());
            payload.put("senderAvatar", sender.getAvatar());
        }

        ChatWebSocketHandler.sendToUser(String.valueOf(message.getReceiverId()), payload);
        ChatWebSocketHandler.sendToUser(String.valueOf(message.getSenderId()), payload);
    }

    public List<ChatMessage> getMessages(Long userId, Long otherId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, otherId))
                .or(w2 -> w2.eq(ChatMessage::getSenderId, otherId).eq(ChatMessage::getReceiverId, userId)));
        wrapper.orderByAsc(ChatMessage::getCreateTime);
        List<ChatMessage> messages = messageMapper.selectList(wrapper);

        messages.stream()
                .filter(m -> m.getReceiverId().equals(userId) && m.getIsRead() == 0)
                .forEach(m -> {
                    m.setIsRead(1);
                    messageMapper.updateById(m);
                });

        // 打开会话后将该会话未读数清零
        ChatSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .and(w -> w
                        .and(w1 -> w1.eq(ChatSession::getUserId, userId).eq(ChatSession::getProviderId, otherId))
                        .or(w2 -> w2.eq(ChatSession::getUserId, otherId).eq(ChatSession::getProviderId, userId)))
                .last("LIMIT 1"));
        if (session != null && session.getUnreadCount() != null && session.getUnreadCount() > 0) {
            session.setUnreadCount(0);
            sessionMapper.updateById(session);
        }

        messages.forEach(m -> {
            User sender = userMapper.selectById(m.getSenderId());
            if (sender != null) {
                m.setSenderName(sender.getNickname());
                m.setSenderAvatar(sender.getAvatar());
            }
        });

        return messages;
    }

    public List<ChatSession> getSessions(Long userId, Integer role) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        if (role == 1) {
            wrapper.eq(ChatSession::getUserId, userId);
        } else {
            wrapper.eq(ChatSession::getProviderId, userId);
        }
        wrapper.orderByDesc(ChatSession::getLastTime);
        List<ChatSession> sessions = sessionMapper.selectList(wrapper);

        sessions.forEach(s -> {
            Long otherId = role == 1 ? s.getProviderId() : s.getUserId();
            User other = userMapper.selectById(otherId);
            if (other != null) {
                s.setOtherName(other.getNickname());
                s.setOtherAvatar(other.getAvatar());
            }
        });

        return sessions;
    }

        private void updateSession(ChatMessage message) {
        User sender = userMapper.selectById(message.getSenderId());
        User receiver = userMapper.selectById(message.getReceiverId());

        Long userId;
        Long providerId;
        if (sender != null && receiver != null) {
            if (sender.getRole() != null && receiver.getRole() != null) {
                if (sender.getRole() == Constants.ROLE_USER && receiver.getRole() == Constants.ROLE_PROVIDER) {
                    userId = sender.getId();
                    providerId = receiver.getId();
                } else if (sender.getRole() == Constants.ROLE_PROVIDER && receiver.getRole() == Constants.ROLE_USER) {
                    userId = receiver.getId();
                    providerId = sender.getId();
                } else {
                    userId = Math.min(message.getSenderId(), message.getReceiverId());
                    providerId = Math.max(message.getSenderId(), message.getReceiverId());
                }
            } else {
                userId = Math.min(message.getSenderId(), message.getReceiverId());
                providerId = Math.max(message.getSenderId(), message.getReceiverId());
            }
        } else {
            userId = Math.min(message.getSenderId(), message.getReceiverId());
            providerId = Math.max(message.getSenderId(), message.getReceiverId());
        }

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId).eq(ChatSession::getProviderId, providerId);
        ChatSession session = sessionMapper.selectOne(wrapper);

        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setProviderId(providerId);
            session.setLastMessage(message.getContent());
            session.setLastTime(LocalDateTime.now());
            session.setUnreadCount(1);
            sessionMapper.insert(session);
        } else {
            session.setLastMessage(message.getContent());
            session.setLastTime(LocalDateTime.now());
            session.setUnreadCount(session.getUnreadCount() + 1);
            sessionMapper.updateById(session);
        }
    }
}
