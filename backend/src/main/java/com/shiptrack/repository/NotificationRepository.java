package com.shiptrack.repository;

import com.shiptrack.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<Notification> findByUserIdAndReadFalse(String userId);
    long countByUserIdAndReadFalse(String userId);
    void deleteByUserId(String userId);
}
