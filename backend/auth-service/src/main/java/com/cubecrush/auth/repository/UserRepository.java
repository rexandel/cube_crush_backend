package com.cubecrush.auth.repository;

import com.cubecrush.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNickname(String nickname);

    boolean existsByNickname(String nickname);

    @Query("SELECT u FROM User u WHERE u.nickname = :nickname")
    Optional<User> findByNicknameForUpdate(@Param("nickname") String nickname);
}