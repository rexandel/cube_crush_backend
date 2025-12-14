package com.cubecrush.user.service;

import com.cubecrush.user.model.User;
import com.cubecrush.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByNickname(String nickname) {
        return userRepository.findByNickname(nickname);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        log.info("Changing password for user {}. Hash in DB: {}", userId, user.getPasswordHash());
        boolean matches = validatePassword(currentPassword, user.getPasswordHash());
        log.info("Password match result: {}", matches);

        if (!matches) {
            throw new IllegalArgumentException("Invalid current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user id: {}", userId);
    }

    @Transactional
    public User updateNickname(Long userId, String newNickname) {
        if (userRepository.existsByNickname(newNickname)) {
            throw new IllegalArgumentException("Nickname '" + newNickname + "' is already taken");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setNickname(newNickname);
        User updatedUser = userRepository.save(user);
        log.info("Nickname updated for user id: {} to: {}", userId, newNickname);
        return updatedUser;
    }

    @Transactional
    public User createUser(String nickname, String password) {
        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("User with nickname '" + nickname + "' already exists");
        }

        User user = User.builder()
                .nickname(nickname)
                .passwordHash(passwordEncoder.encode(password))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created new user with id: {} and nickname: {}", savedUser.getId(), savedUser.getNickname());
        return savedUser;
    }
}