package com.gamesphere.repository;

import com.gamesphere.config.JpaConfig;
import com.gamesphere.entity.Team;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void searchByName_ReturnsMatchedTeams() {
        Team t1 = Team.builder().name("Alpha Squad").tag("ALP").build();
        Team t2 = Team.builder().name("Beta Squad").tag("BET").build();
        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();

        Page<Team> results = teamRepository.searchByName("alpha", PageRequest.of(0, 10));
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().getFirst().getName()).isEqualTo("Alpha Squad");
    }

    @Test
    void findAllOrderByWinRateDesc_ReturnsTeamsInCorrectOrder() {
        Team t1 = Team.builder().name("Team A").tag("TA").winRate(50.0).build();
        Team t2 = Team.builder().name("Team B").tag("TB").winRate(90.0).build();
        Team t3 = Team.builder().name("Team C").tag("TC").winRate(75.0).build();

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.persist(t3);
        entityManager.flush();

        Page<Team> results = teamRepository.findAllOrderByWinRateDesc(PageRequest.of(0, 10));
        List<Team> teams = results.getContent();
        assertThat(teams).hasSize(3);
        assertThat(teams.get(0).getName()).isEqualTo("Team B");
        assertThat(teams.get(1).getName()).isEqualTo("Team C");
        assertThat(teams.get(2).getName()).isEqualTo("Team A");
    }

    @Test
    void isUserInAnyTeam_ChecksCorrectly() {
        User user1 = User.builder().username("p1").email("p1@g.com").password("pwd").role(Role.PLAYER).build();
        User user2 = User.builder().username("p2").email("p2@g.com").password("pwd").role(Role.PLAYER).build();

        Team t = Team.builder().name("Alpha").tag("ALP").build();
        entityManager.persist(t);
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        // Assign user1 to team
        user1.setTeam(t);
        entityManager.merge(user1);
        entityManager.flush();

        assertThat(teamRepository.isUserInAnyTeam(user1.getId())).isTrue();
        assertThat(teamRepository.isUserInAnyTeam(user2.getId())).isFalse();
    }

    @Test
    void findByTag_ReturnsActiveTeam() {
        Team t = Team.builder().name("Alpha").tag("ALP").deleted(false).build();
        entityManager.persist(t);
        entityManager.flush();

        Optional<Team> found = teamRepository.findByTag("ALP");
        assertThat(found).isPresent();

        t.setDeleted(true);
        entityManager.merge(t);
        entityManager.flush();
        entityManager.clear();

        Optional<Team> foundDeleted = teamRepository.findByTag("ALP");
        assertThat(foundDeleted).isEmpty();
    }
}
