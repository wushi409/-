package com.travel.controller.user;

import com.travel.common.Result;
import com.travel.entity.ChatMessage;
import com.travel.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    public Result<?> send(HttpServletRequest request, @RequestBody ChatMessage message) {
        Long userId = (Long) request.getAttribute("userId");
        message.setSenderId(userId);
        chatService.sendMessage(message);
        return Result.success("发送成功");
    }

    @GetMapping("/messages")
    public Result<?> getMessages(HttpServletRequest request, @RequestParam Long otherId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getMessages(userId, otherId));
    }

    @GetMapping("/sessions")
    public Result<?> getSessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        return Result.success(chatService.getSessions(userId, role));
    }
}
