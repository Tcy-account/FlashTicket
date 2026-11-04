package com.flashticket.notification.api;

import com.flashticket.common.api.ApiResponse;
import com.flashticket.common.api.ErrorCode;
import com.flashticket.common.exception.BizException;
import com.flashticket.common.security.SecurityUtils;
import com.flashticket.notification.dto.NotificationListResult;
import com.flashticket.notification.dto.NotificationResponse;
import com.flashticket.notification.dto.StreamTokenResponse;
import com.flashticket.notification.service.NotificationQueryService;
import com.flashticket.notification.service.SseConnectionRegistry;
import com.flashticket.notification.service.StreamTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 用户端消息中心入口（M14）：列表、详情、未读数、单条/全部已读、SSE 实时流。
 *
 * <p>个人消息属用户基础能力，登录即可用，不设独立权限点；
 * 所有操作经 userId 归属校验，只能访问/修改本人的接收记录。
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class UserNotificationController {

    private final NotificationQueryService queryService;
    private final StreamTokenService streamTokenService;
    private final SseConnectionRegistry sseRegistry;

    public UserNotificationController(NotificationQueryService queryService,
                                      StreamTokenService streamTokenService,
                                      SseConnectionRegistry sseRegistry) {
        this.queryService = queryService;
        this.streamTokenService = streamTokenService;
        this.sseRegistry = sseRegistry;
    }

    /** 我的消息游标分页。 */
    @GetMapping
    public ApiResponse<NotificationListResult> listMine(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(queryService.listMine(
                SecurityUtils.currentUserId(), type, cursor, limit));
    }

    /** 未读数。 */
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ok(queryService.unreadCount(SecurityUtils.currentUserId()));
    }

    /** 消息详情。 */
    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(queryService.getDetail(SecurityUtils.currentUserId(), id));
    }

    /** 单条已读。 */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        queryService.markRead(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok(null);
    }

    /** 全部已读。 */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        queryService.markAllRead(SecurityUtils.currentUserId());
        return ApiResponse.ok(null);
    }

    /** 换取短期 SSE 流连接凭证（需 Bearer 认证）。 */
    @PostMapping("/stream-token")
    public ApiResponse<StreamTokenResponse> issueStreamToken() {
        StreamTokenService.StreamTokenInfo info =
                streamTokenService.issue(SecurityUtils.currentUserId());
        return ApiResponse.ok(new StreamTokenResponse(info.token(), info.expiresInSeconds()));
    }

    /**
     * SSE 实时通知流（EventSource 无自定义请求头，经短期凭证鉴权，SecurityConfig 放行）。
     */
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String token) {
        Long userId = streamTokenService.resolve(token);
        if (userId == null) {
            throw new BizException(ErrorCode.NOTIFICATION_STREAM_TOKEN_INVALID);
        }
        SseEmitter emitter = sseRegistry.register(userId);
        try {
            emitter.send(SseEmitter.event().name("connected").data(""));
        } catch (Exception ignored) {
            // 连接已关闭，忽略
        }
        return emitter;
    }
}
