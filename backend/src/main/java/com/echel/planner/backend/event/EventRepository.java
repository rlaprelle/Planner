package com.echel.planner.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT e FROM Event e
            WHERE e.user.id = :userId
              AND e.blockDate = :blockDate
              AND e.archivedAt IS NULL
            ORDER BY e.startTime ASC
            """)
    List<Event> findByUserIdAndBlockDate(@Param("userId") UUID userId,
                                         @Param("blockDate") LocalDate blockDate);

    @Query("""
            SELECT e FROM Event e
            WHERE e.project.id = :projectId
              AND e.user.id = :userId
              AND e.archivedAt IS NULL
            ORDER BY e.blockDate ASC, e.startTime ASC
            """)
    List<Event> findByProjectIdAndUserId(@Param("projectId") UUID projectId,
                                          @Param("userId") UUID userId);

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.project p
            WHERE e.user.id = :userId
              AND e.blockDate = :blockDate
              AND e.archivedAt IS NULL
            ORDER BY e.startTime ASC
            """)
    List<Event> findForDate(@Param("userId") UUID userId,
                             @Param("blockDate") LocalDate blockDate);

    long countByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteByProjectId(UUID projectId);
}
