package com.shiptrack.service;

import com.shiptrack.dto.UserDtos;
import com.shiptrack.exception.ApiException;
import com.shiptrack.model.ActivityLog;
import com.shiptrack.model.Role;
import com.shiptrack.model.User;
import com.shiptrack.repository.ActivityLogRepository;
import com.shiptrack.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    public UserService(UserRepository userRepository, ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
    }

    public User getById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    public Page<User> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable);
    }

    public List<User> drivers() {
        return userRepository.findByRole(Role.LOGISTICS_OPERATOR);
    }

    public User updateProfile(User user, UserDtos.UpdateProfileRequest request) {
        if (request.fullName() != null && !request.fullName().isBlank()) user.setFullName(request.fullName());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.companyName() != null) user.setCompanyName(request.companyName());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        if (request.defaultAddress() != null) user.setDefaultAddress(request.defaultAddress());
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    public User updateRole(String userId, Role role, String actorEmail) {
        User user = getById(userId);
        Role previous = user.getRole();
        user.setRole(role);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        activityLogRepository.save(ActivityLog.builder()
                .userId(saved.getId())
                .userEmail(saved.getEmail())
                .action("ROLE_CHANGE")
                .detail(actorEmail + " changed the role from " + previous + " to " + role)
                .build());
        return saved;
    }

    public User updateStatus(String userId, boolean active, String actorEmail) {
        User user = getById(userId);
        user.setActive(active);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        activityLogRepository.save(ActivityLog.builder()
                .userId(saved.getId())
                .userEmail(saved.getEmail())
                .action(active ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED")
                .detail("Changed by " + actorEmail)
                .build());
        return saved;
    }

    public void delete(String userId) {
        userRepository.delete(getById(userId));
    }

    public Page<ActivityLog> activity(String userId, Pageable pageable) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<ActivityLog> allActivity(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public long count() {
        return userRepository.count();
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public List<User> all() {
        return userRepository.findAll();
    }
}
