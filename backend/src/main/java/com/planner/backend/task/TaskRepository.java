package com.planner.backend.task;

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
              AND t.status = com.planner.backend.task.TaskStatus.DONE
              AND t.completedAt >= :startOfDay
              AND t.archivedAt IS NULL
            ORDER BY t.completedAt DESC
            """)
    List<Task> findCompletedTodayForUser(@Param("userId") UUID userId,
                                         @Param("startOfDay") Instant startOfDay);

    @Query("""
            SELECT DISTINCT t FROM Task t
            JOIN FETCH t.project p
            WHERE t.user.id = :userId
              AND t.status IN (
                  com.planner.backend.task.TaskStatus.TODO,
                  com.planner.backend.task.TaskStatus.IN_PROGRESS)
              AND t.archivedAt IS NULL
              AND t.parentTask IS NULL
              AND t.id NOT IN (
                  SELECT tb.task.id FROM com.planner.backend.schedule.TimeBlock tb
                  WHERE tb.user.id = :userId
                    AND tb.blockDate = :date
                    AND tb.task IS NOT NULL)
            """)
    List<Task> findSuggestedForUser(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("""
            SELECT t FROM Task t
            JOIN FETCH t.project p
            WHERE t.user.id = :userId
              AND t.dueDate IS NOT NULL
              AND t.status != com.planner.backend.task.TaskStatus.DONE
              AND t.archivedAt IS NULL
              AND t.parentTask IS NULL
            ORDER BY t.dueDate ASC
            """)
    List<Task> findUpcomingDeadlines(@Param("userId") UUID userId,
                                      org.springframework.data.domain.Pageable pageable);

    void deleteByProjectId(UUID projectId);
    void deleteByParentTaskId(UUID parentTaskId);

    long countByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
