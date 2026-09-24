package com.shiptrack.controller;

import com.shiptrack.dto.CommonDtos;
import com.shiptrack.dto.UserDtos;
import com.shiptrack.model.ActivityLog;
import com.shiptrack.model.User;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDtos.UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserDtos.UserResponse.from(principal.getUser());
    }

    @PutMapping("/me")
    public UserDtos.UserResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody UserDtos.UpdateProfileRequest request) {
        return UserDtos.UserResponse.from(userService.updateProfile(principal.getUser(), request));
    }

    @GetMapping("/me/activity")
    public CommonDtos.PageResponse<ActivityLog> myActivity(@AuthenticationPrincipal UserPrincipal principal,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLog> result = userService.activity(principal.getId(), PageRequest.of(page, size));
        return CommonDtos.PageResponse.of(result, result.getContent());
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR','SUPPORT_AGENT')")
    public List<UserDtos.UserResponse> drivers() {
        return userService.drivers().stream().map(UserDtos.UserResponse::from).toList();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public CommonDtos.PageResponse<UserDtos.UserResponse> list(@RequestParam(required = false) String query,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        Page<User> result = userService.search(query, PageRequest.of(page, size));
        return CommonDtos.PageResponse.of(result,
                result.getContent().stream().map(UserDtos.UserResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public UserDtos.UserResponse get(@PathVariable String id) {
        return UserDtos.UserResponse.from(userService.getById(id));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDtos.UserResponse updateRole(@PathVariable String id,
                                            @Valid @RequestBody UserDtos.UpdateRoleRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return UserDtos.UserResponse.from(userService.updateRole(id, request.role(), principal.getUsername()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDtos.UserResponse updateStatus(@PathVariable String id,
                                              @RequestBody UserDtos.UpdateStatusRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return UserDtos.UserResponse.from(userService.updateStatus(id, request.active(), principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonDtos.ApiMessage delete(@PathVariable String id) {
        userService.delete(id);
        return new CommonDtos.ApiMessage("Account removed");
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public CommonDtos.PageResponse<ActivityLog> allActivity(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "30") int size) {
        Page<ActivityLog> result = userService.allActivity(PageRequest.of(page, size));
        return CommonDtos.PageResponse.of(result, result.getContent());
    }
}
