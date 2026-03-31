package com.planner.backend.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    List<Task> findByProjectIdAndUserIdAndParentTaskIsNullAndArchivedAtIsNull(UUID projectId, UUID userId);

    List<Task> findByParentTaskIdAndUserIdAndArchivedAtIsNull(UUID parentTaskId, UUID userId);

    List<Task> findByParentTaskIdAndUserId(UUID parentTaskId, UUID userId);
}
