package com.planner.backend.admin;

import com.planner.backend.admin.dto.AdminDeferredItemRequest;
import com.planner.backend.admin.dto.AdminDeferredItemResponse;
import com.planner.backend.auth.AppUser;
import com.planner.backend.auth.AppUserRepository;
import com.planner.backend.deferred.DeferredItem;
import com.planner.backend.deferred.DeferredItemRepository;
import com.planner.backend.project.ProjectRepository;
import com.planner.backend.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminDeferredItemService {

    private final DeferredItemRepository deferredItemRepository;
    private final AppUserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public AdminDeferredItemService(DeferredItemRepository deferredItemRepository,
                                    AppUserRepository userRepository,
                                    TaskRepository taskRepository,
                                    ProjectRepository projectRepository) {
        this.deferredItemRepository = deferredItemRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDeferredItemResponse> listAll() {
        return deferredItemRepository.findAll().stream()
                .map(AdminDeferredItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDeferredItemResponse get(UUID id) {
        return AdminDeferredItemResponse.from(findItem(id));
    }

    public AdminDeferredItemResponse create(AdminDeferredItemRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
        DeferredItem item = new DeferredItem(user, request.rawText());
        applyFields(item, request);
        return AdminDeferredItemResponse.from(deferredItemRepository.save(item));
    }

    public AdminDeferredItemResponse update(UUID id, AdminDeferredItemRequest request) {
        DeferredItem item = findItem(id);
        item.setRawText(request.rawText());
        applyFields(item, request);
        return AdminDeferredItemResponse.from(item);
    }

    public void delete(UUID id) {
        findItem(id);
        deferredItemRepository.deleteById(id);
    }

    private void applyFields(DeferredItem item, AdminDeferredItemRequest request) {
        if (request.isProcessed() != null) {
            item.setProcessed(request.isProcessed());
            item.setProcessedAt(request.isProcessed() ? Instant.now() : null);
        }
        if (request.resolvedTaskId() != null) {
            item.setResolvedTask(taskRepository.findById(request.resolvedTaskId()).orElse(null));
        }
        if (request.resolvedProjectId() != null) {
            item.setResolvedProject(projectRepository.findById(request.resolvedProjectId()).orElse(null));
        }
        item.setDeferredUntilDate(request.deferredUntilDate());
        if (request.deferralCount() != null) {
            item.setDeferralCount(request.deferralCount());
        }
    }

    private DeferredItem findItem(UUID id) {
        return deferredItemRepository.findById(id)
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Deferred item not found: " + id));
    }
}
