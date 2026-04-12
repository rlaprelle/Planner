package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTimeBlockRequest;
import com.echel.planner.backend.admin.dto.AdminTimeBlockResponse;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.schedule.TimeBlock;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminTimeBlockService {

    private final TimeBlockRepository timeBlockRepository;
    private final AppUserRepository userRepository;
    private final TaskRepository taskRepository;

    public AdminTimeBlockService(TimeBlockRepository timeBlockRepository,
                                 AppUserRepository userRepository,
                                 TaskRepository taskRepository) {
        this.timeBlockRepository = timeBlockRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTimeBlockResponse> listAll() {
        return timeBlockRepository.findAll().stream()
                .map(AdminTimeBlockResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminTimeBlockResponse get(UUID id) {
        return AdminTimeBlockResponse.from(findBlock(id));
    }

    public AdminTimeBlockResponse create(AdminTimeBlockRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.userId()));
        Task task = null;
        if (request.taskId() != null) {
            task = taskRepository.findById(request.taskId())
                    .orElseThrow(() -> new EntityNotFoundException("Task not found: " + request.taskId()));
        }
        TimeBlock block = new TimeBlock(
                user,
                request.blockDate(),
                task,
                request.startTime(),
                request.endTime(),
                request.sortOrder() != null ? request.sortOrder() : 0
        );
        if (request.wasCompleted() != null) {
            block.setWasCompleted(request.wasCompleted());
        }
        return AdminTimeBlockResponse.from(timeBlockRepository.save(block));
    }

    public AdminTimeBlockResponse update(UUID id, AdminTimeBlockRequest request) {
        TimeBlock block = findBlock(id);
        block.setBlockDate(request.blockDate());
        block.setStartTime(request.startTime());
        block.setEndTime(request.endTime());
        if (request.taskId() != null) {
            Task task = taskRepository.findById(request.taskId())
                    .orElseThrow(() -> new EntityNotFoundException("Task not found: " + request.taskId()));
            block.setTask(task);
        } else {
            block.setTask(null);
        }
        if (request.sortOrder() != null) {
            block.setSortOrder(request.sortOrder());
        }
        if (request.wasCompleted() != null) {
            block.setWasCompleted(request.wasCompleted());
        }
        return AdminTimeBlockResponse.from(block);
    }

    public void delete(UUID id) {
        findBlock(id);
        timeBlockRepository.deleteById(id);
    }

    private TimeBlock findBlock(UUID id) {
        return timeBlockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Time block not found: " + id));
    }
}
