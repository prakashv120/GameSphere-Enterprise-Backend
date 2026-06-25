package com.gamesphere.repository;

import com.gamesphere.config.JpaConfig;
import com.gamesphere.entity.User;
import com.gamesphere.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUsername_ReturnsActiveUser() {
        User user = User.builder()
                .username("testplayer")
                .email("player@gamesphere.com")
                .password("hashed_password")
                .role(Role.PLAYER)
                .deleted(false)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        Optional<User> found = userRepository.findByUsername("testplayer");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("player@gamesphere.com");
    }

    @Test
    void findByUsername_DoesNotReturnDeletedUser() {
        User user = User.builder()
                .username("deletedplayer")
                .email("deleted@gamesphere.com")
                .password("hashed_password")
                .role(Role.PLAYER)
                .deleted(true)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        Optional<User> found = userRepository.findByUsername("deletedplayer");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_ReturnsTrueWhenEmailExists() {
        User user = User.builder()
                .username("user1")
                .email("user1@gamesphere.com")
                .password("password")
                .role(Role.PLAYER)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        assertThat(userRepository.existsByEmail("user1@gamesphere.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@gamesphere.com")).isFalse();
    }

    @Test
    void findByRole_ReturnsPaginatedUsers() {
        User user1 = User.builder().username("p1").email("p1@gamesphere.com").password("pass").role(Role.PLAYER).build();
        User user2 = User.builder().username("p2").email("p2@gamesphere.com").password("pass").role(Role.PLAYER).build();
        User admin = User.builder().username("adm").email("adm@gamesphere.com").password("pass").role(Role.ADMIN).build();

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(admin);
        entityManager.flush();

        Page<User> playersPage = userRepository.findByRole(Role.PLAYER, PageRequest.of(0, 10));
        assertThat(playersPage.getContent()).hasSize(2);
    }

    @Test
    void findByRoleAndUsernameContainingIgnoreCase_MatchesProperly() {
        User user1 = User.builder().username("AlphaGamer").email("a@g.com").password("pass").role(Role.PLAYER).build();
        User user2 = User.builder().username("BetaGamer").email("b@g.com").password("pass").role(Role.PLAYER).build();

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        Page<User> page = userRepository.findByRoleAndUsernameContainingIgnoreCase(
                Role.PLAYER, "alpha", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getUsername()).isEqualTo("AlphaGamer");
    }

    @Test
    void softDelete_UpdatesDeletedFlag() {
        User user = User.builder()
                .username("todelete")
                .email("todelete@gamesphere.com")
                .password("password")
                .role(Role.PLAYER)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        userRepository.delete(user);
        userRepository.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(user.getId());
        assertThat(found).isEmpty();
    }
}
