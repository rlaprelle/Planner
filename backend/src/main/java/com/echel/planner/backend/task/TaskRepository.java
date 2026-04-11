package com.echel.planner.backend.task;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    List<Task> findByProjectIdAndUserIdAndParentTaskIsNullAndArchivedAtIsNull(UUID projectId, UUID userId);

    List<Task> findByParentTaskIdAndUserIdAndArchivedAtIsNull(UUID parentTaskId, UUID userId);

    List<Task> findByParentTaskIdAndUserId(UUID parentTaskId, UUID userId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.user.id = :userId
              AND t.status = com.echel.planner.backend.task.TaskStatus.COMPLETED
              AND t.completedAt >= :startOfDay
              AND t.archivedAt IS NULL
            ORDER BY t.completedAt DESC
            """)
    List<Task> findCompletedTodayForUser(@Param("userId") UUID userId,
                                         @Param("startOfDay") Instant startOfDay);

    @Query("""
            SELECT t FROM Task t
            WHERE t.user.id = :userId
              AND t.status = com.echel.planner.backend.task.TaskStatus.COMPLETED
              AND t.completedAt >= :start
              AND t.completedAt < :end
              AND t.archivedAt IS NULL
            """)
    List<Task> findCompletedInRange(@Param("userId") UUID userId,
                                    @Param("start") Instant start,
                                    @Param("end") Instant end);

    @Query("""
            SELECT DISTINCT t FROM Task t
            JOIN FETCH t.project p
            WHERE t.user.id = :userId
              AND t.status = com.echel.planner.backend.task.TaskStatus.OPEN
              AND t.archivedAt IS NULL
              AND t.parentTask IS NULL
              AND (t.visibleFrom IS NULL OR t.visibleFrom <= :date)
              AND t.id NOT IN (
                  SELECT tb.task.id FROM com.echel.planner.backend.schedule.TimeBlock tb
                  WHERE tb.user.id = :userId
                    AND tb.blockDate = :date
                    AND tb.task IS NOT NULL)
            """)
    List<Task> findSuggestedForUser(@Param("userId") UUID userId, @Param("date") LocalDate date);

    /** All active top-level tasks visible as of the given date. Used by End Day triage and Start rituals. */
    @Query("""
            SELECT DISTINCT t FROM Task t
            JOIN FETCH t.project p
            WHERE t.user.id = :userId
              AND t.status = com.echel.planner.backend.task.TaskStatus.OPEN
              AND t.archivedAt IS NULL
              AND t.parentTask IS NULL
              AND (t.visibleFrom IS NULL OR t.visibleFrom <= :date)
            ORDER BY t.priority DESC, t.dueDate ASC NULLS LAST
            """)
    List<Task> findActiveForUser(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("""
            SELECT t FROM Task t
            JOIN FETCH t.project p
            WHERE t.user.id = :userId
              AND t.dueDate IS NOT NULL
              AND t.status = com.echel.planner.backend.task.TaskStatus.OPEN
              AND t.archivedAt IS NULL
              AND t.parentTask IS NULL
            ORDER BY t.dueDate ASC
            """)
    List<Task> findUpcomingDeadlines(@Param("userId") UUID userId,
                                      Pageable pageable);

    void deleteByProjectId(UUID projectId);
    void deleteByParentTaskId(UUID parentTaskId);

    long countByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
