package com.shiptrack.controller;

import com.shiptrack.dto.CommonDtos;
import com.shiptrack.model.Notification;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public CommonDtos.PageResponse<Notification> list(@AuthenticationPrincipal UserPrincipal principal,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Page<Notification> result = notificationService.list(principal.getId(), PageRequest.of(page, size));
        return CommonDtos.PageResponse.of(result, result.getContent());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("count", notificationService.unreadCount(principal.getId()));
    }

    @PatchMapping("/{id}/read")
    public CommonDtos.ApiMessage markRead(@PathVariable String id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.markRead(id, principal.getId());
    }

    @PatchMapping("/read-all")
    public CommonDtos.ApiMessage markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.markAllRead(principal.getId());
    }

    @DeleteMapping("/{id}")
    public CommonDtos.ApiMessage delete(@PathVariable String id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.delete(id, principal.getId());
        return new CommonDtos.ApiMessage("Notification removed");
    }
}
