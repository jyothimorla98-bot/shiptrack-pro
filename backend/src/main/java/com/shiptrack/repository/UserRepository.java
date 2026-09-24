package com.shiptrack.repository;

import com.shiptrack.model.Role;
import com.shiptrack.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPasswordResetToken(String token);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
}
