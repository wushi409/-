package com.travel.controller.user;

import com.travel.common.Result;
import com.travel.entity.ChatMessage;
import com.travel.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 类说明：ChatController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 方法说明：send
     * 1. 负责处理 send 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/messages")
    public Result<?> send(HttpServletRequest request, @RequestBody ChatMessage message) {
        Long userId = (Long) request.getAttribute("userId");
        message.setSenderId(userId);
        chatService.sendMessage(message);
        return Result.success("发送成功");
    }

    /**
     * 方法说明：getMessages
     * 1. 负责处理 getMessages 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/messages")
    public Result<?> getMessages(HttpServletRequest request, @RequestParam Long otherId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getMessages(userId, otherId));
    }

    /**
     * 方法说明：getSessions
     * 1. 负责处理 getSessions 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/sessions")
    public Result<?> getSessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        return Result.success(chatService.getSessions(userId, role));
    }
}
