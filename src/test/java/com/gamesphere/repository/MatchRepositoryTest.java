package com.gamesphere.repository;

import com.gamesphere.config.JpaConfig;
import com.gamesphere.entity.Match;
import com.gamesphere.entity.Team;
import com.gamesphere.entity.Tournament;
import com.gamesphere.enums.MatchStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class MatchRepositoryTest {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByTeamId_ReturnsMatchesWhereTeamIsAOrB() {
        Team tA = Team.builder().name("Team A").tag("TA").build();
        Team tB = Team.builder().name("Team B").tag("TB").build();
        Team tC = Team.builder().name("Team C").tag("TC").build();

        entityManager.persist(tA);
        entityManager.persist(tB);
        entityManager.persist(tC);

        Tournament tournament = Tournament.builder().name("T").maxTeams(8).build();
        entityManager.persist(tournament);

        Match m1 = Match.builder().tournament(tournament).teamA(tA).teamB(tB).status(MatchStatus.SCHEDULED).build();
        Match m2 = Match.builder().tournament(tournament).teamA(tB).teamB(tC).status(MatchStatus.SCHEDULED).build();
        Match m3 = Match.builder().tournament(tournament).teamA(tA).teamB(tC).status(MatchStatus.SCHEDULED).build();

        entityManager.persist(m1);
        entityManager.persist(m2);
        entityManager.persist(m3);
        entityManager.flush();

        Page<Match> teamAMatches = matchRepository.findByTeamId(tA.getId(), PageRequest.of(0, 10));
        assertThat(teamAMatches.getContent()).hasSize(2);
    }

    @Test
    void winLossCounting_CalculatesCorrectly() {
        Team tA = Team.builder().name("Team A").tag("TA").build();
        Team tB = Team.builder().name("Team B").tag("TB").build();

        entityManager.persist(tA);
        entityManager.persist(tB);

        Tournament tournament = Tournament.builder().name("T").maxTeams(8).build();
        entityManager.persist(tournament);

        // Match 1: tA vs tB, tA wins
        Match m1 = Match.builder()
                .tournament(tournament)
                .teamA(tA)
                .teamB(tB)
                .winner(tA)
                .status(MatchStatus.COMPLETED)
                .build();

        // Match 2: tA vs tB, tB wins
        Match m2 = Match.builder()
                .tournament(tournament)
                .teamA(tA)
                .teamB(tB)
                .winner(tB)
                .status(MatchStatus.COMPLETED)
                .build();

        // Match 3: tA vs tB, SCHEDULED (not completed)
        Match m3 = Match.builder()
                .tournament(tournament)
                .teamA(tA)
                .teamB(tB)
                .status(MatchStatus.SCHEDULED)
                .build();

        entityManager.persist(m1);
        entityManager.persist(m2);
        entityManager.persist(m3);
        entityManager.flush();

        long winsA = matchRepository.countWinsByTeamId(tA.getId());
        long lossesA = matchRepository.countLossesByTeamId(tA.getId());
        long completedMatches = matchRepository.countCompletedMatchesByTournament(tournament.getId());

        assertThat(winsA).isEqualTo(1L);
        assertThat(lossesA).isEqualTo(1L);
        assertThat(completedMatches).isEqualTo(2L);
    }
}
