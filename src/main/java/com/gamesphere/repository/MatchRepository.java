package com.gamesphere.repository;

import com.gamesphere.entity.Match;
import com.gamesphere.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    /**
     * Find all matches for a specific tournament (paginated).
     */
    Page<Match> findByTournamentId(Long tournamentId, Pageable pageable);

    /**
     * Find all matches with a specific status (paginated).
     */
    Page<Match> findByStatus(MatchStatus status, Pageable pageable);

    /**
     * Find all matches for a tournament filtered by status (paginated).
     */
    Page<Match> findByTournamentIdAndStatus(Long tournamentId, MatchStatus status, Pageable pageable);

    /**
     * Find all matches involving a specific team (either as teamA or teamB).
     */
    @Query("SELECT m FROM Match m WHERE m.teamA.id = :teamId OR m.teamB.id = :teamId")
    Page<Match> findByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    /**
     * Count wins for a specific team.
     */
    @Query("SELECT COUNT(m) FROM Match m WHERE m.winner.id = :teamId AND m.status = 'COMPLETED'")
    long countWinsByTeamId(@Param("teamId") Long teamId);

    /**
     * Count losses for a specific team (played but didn't win).
     */
    @Query("SELECT COUNT(m) FROM Match m WHERE " +
           "(m.teamA.id = :teamId OR m.teamB.id = :teamId) AND " +
           "m.status = 'COMPLETED' AND m.winner.id != :teamId")
    long countLossesByTeamId(@Param("teamId") Long teamId);

    /**
     * Count completed matches in a tournament.
     */
    @Query("SELECT COUNT(m) FROM Match m WHERE m.tournament.id = :tournamentId AND m.status = 'COMPLETED'")
    long countCompletedMatchesByTournament(@Param("tournamentId") Long tournamentId);
}
