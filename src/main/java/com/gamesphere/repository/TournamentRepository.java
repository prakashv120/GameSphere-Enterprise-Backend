package com.gamesphere.repository;

import com.gamesphere.entity.Tournament;
import com.gamesphere.enums.TournamentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    boolean existsByName(String name);

    /**
     * Filter tournaments by status (paginated).
     */
    Page<Tournament> findByStatus(TournamentStatus status, Pageable pageable);

    /**
     * Search tournaments by name (case-insensitive, partial match).
     */
    @Query("SELECT t FROM Tournament t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Tournament> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Find tournaments by status and name combined.
     */
    @Query("SELECT t FROM Tournament t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:name IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Tournament> findByStatusAndNameContaining(
            @Param("status") TournamentStatus status,
            @Param("name") String name,
            Pageable pageable);

    /**
     * Count how many teams are currently registered in a tournament.
     */
    @Query("SELECT COUNT(team) FROM Tournament t JOIN t.registeredTeams team WHERE t.id = :tournamentId")
    long countRegisteredTeams(@Param("tournamentId") Long tournamentId);

    /**
     * Check whether a specific team is already registered in a tournament.
     */
    @Query("SELECT COUNT(team) > 0 FROM Tournament t JOIN t.registeredTeams team " +
           "WHERE t.id = :tournamentId AND team.id = :teamId")
    boolean isTeamRegistered(@Param("tournamentId") Long tournamentId, @Param("teamId") Long teamId);
}
