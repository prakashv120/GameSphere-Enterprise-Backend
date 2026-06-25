package com.gamesphere.repository;

import com.gamesphere.config.JpaConfig;
import com.gamesphere.entity.Team;
import com.gamesphere.entity.Tournament;
import com.gamesphere.enums.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class TournamentRepositoryTest {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByStatus_ReturnsMatchingTournaments() {
        Tournament t1 = Tournament.builder()
                .name("T1")
                .status(TournamentStatus.UPCOMING)
                .maxTeams(16)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .build();
        Tournament t2 = Tournament.builder()
                .name("T2")
                .status(TournamentStatus.ACTIVE)
                .maxTeams(16)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();

        Page<Tournament> upcoming = tournamentRepository.findByStatus(TournamentStatus.UPCOMING, PageRequest.of(0, 10));
        assertThat(upcoming.getContent()).hasSize(1);
        assertThat(upcoming.getContent().getFirst().getName()).isEqualTo("T1");
    }

    @Test
    void findByStatusAndNameContaining_MatchesCorrectly() {
        Tournament t1 = Tournament.builder()
                .name("Winter Tournament")
                .status(TournamentStatus.UPCOMING)
                .maxTeams(16)
                .build();
        Tournament t2 = Tournament.builder()
                .name("Summer Tournament")
                .status(TournamentStatus.UPCOMING)
                .maxTeams(16)
                .build();

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.flush();

        Page<Tournament> result = tournamentRepository.findByStatusAndNameContaining(
                TournamentStatus.UPCOMING, "win", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Winter Tournament");
    }

    @Test
    void teamRegistrationState_ChecksCorrectly() {
        Team team = Team.builder().name("Alpha").tag("ALP").build();
        entityManager.persist(team);

        Tournament tournament = Tournament.builder()
                .name("Winter Clash")
                .maxTeams(16)
                .registeredTeams(new ArrayList<>(Collections.singletonList(team)))
                .build();

        entityManager.persist(tournament);
        entityManager.flush();

        long registeredCount = tournamentRepository.countRegisteredTeams(tournament.getId());
        assertThat(registeredCount).isEqualTo(1L);

        boolean isRegistered = tournamentRepository.isTeamRegistered(tournament.getId(), team.getId());
        assertThat(isRegistered).isTrue();

        boolean notRegistered = tournamentRepository.isTeamRegistered(tournament.getId(), 999L);
        assertThat(notRegistered).isFalse();
    }
}
