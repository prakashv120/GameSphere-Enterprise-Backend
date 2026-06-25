package com.gamesphere.repository;

import com.gamesphere.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);

    boolean existsByTag(String tag);

    Optional<Team> findByName(String name);

    Optional<Team> findByTag(String tag);

    /**
     * Search teams by name (case-insensitive, partial match).
     */
    @Query("SELECT t FROM Team t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Team> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Find all teams ordered by winRate descending (leaderboard support).
     */
    @Query("SELECT t FROM Team t ORDER BY t.winRate DESC")
    Page<Team> findAllOrderByWinRateDesc(Pageable pageable);

    /**
     * Check if a user is already a member of any active team.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :userId AND u.team IS NOT NULL")
    boolean isUserInAnyTeam(@Param("userId") Long userId);
}
