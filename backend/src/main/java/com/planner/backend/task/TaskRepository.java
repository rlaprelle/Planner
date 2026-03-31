package com.planner.backend.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
}
