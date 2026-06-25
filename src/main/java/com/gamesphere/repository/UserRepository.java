package com.gamesphere.repository;

import com.gamesphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    org.springframework.data.domain.Page<User> findByRole(com.gamesphere.enums.Role role, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<User> findByRoleAndUsernameContainingIgnoreCase(com.gamesphere.enums.Role role, String username, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<User> findByRoleAndTeamId(com.gamesphere.enums.Role role, Long teamId, org.springframework.data.domain.Pageable pageable);
}
