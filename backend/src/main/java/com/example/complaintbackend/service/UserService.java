package com.example.complaintbackend.service;

import com.example.complaintbackend.dto.auth.UserResponse;
import com.example.complaintbackend.entity.Role;
import com.example.complaintbackend.entity.User;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return new UserResponse(getUserEntity(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getSupportAgents() {
        return userRepository.findByRole(Role.ROLE_SUPPORT_AGENT).stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, Role newRole) {
        log.info("Updating role for user ID {} to {}", userId, newRole);
        User user = getUserEntity(userId);
        user.setRole(newRole);
        User saved = userRepository.save(user);
        return new UserResponse(saved);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, boolean enabled) {
        log.info("Updating enabled status for user ID {} to {}", userId, enabled);
        User user = getUserEntity(userId);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        return new UserResponse(saved);
    }
}
