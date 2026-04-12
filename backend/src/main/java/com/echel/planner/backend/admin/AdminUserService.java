package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminUserRequest;
import com.echel.planner.backend.admin.dto.AdminUserResponse;
import com.echel.planner.backend.admin.dto.DependentCountResponse;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.task.TaskRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DeferredItemRepository deferredItemRepository;
    private final DailyReflectionRepository reflectionRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AppUserRepository userRepository,
                            ProjectRepository projectRepository,
                            TaskRepository taskRepository,
                            DeferredItemRepository deferredItemRepository,
                            DailyReflectionRepository reflectionRepository,
                            TimeBlockRepository timeBlockRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.deferredItemRepository = deferredItemRepository;
        this.reflectionRepository = reflectionRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) {
        return AdminUserResponse.from(findUser(id));
    }

    public AdminUserResponse create(AdminUserRequest request) {
        AppUser user = new AppUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                request.timezone() != null ? request.timezone() : "UTC"
        );
        return AdminUserResponse.from(userRepository.save(user));
    }

    public AdminUserResponse update(UUID id, AdminUserRequest request) {
        AppUser user = findUser(id);
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        if (request.timezone() != null) {
            user.setTimezone(request.timezone());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return AdminUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public DependentCountResponse getDependentCounts(UUID id) {
        findUser(id);
        return new DependentCountResponse(
                projectRepository.countByUserId(id),
                taskRepository.countByUserId(id),
                deferredItemRepository.countByUserId(id),
                reflectionRepository.countByUserId(id),
                timeBlockRepository.countByUserId(id)
        );
    }

    public void delete(UUID id) {
        findUser(id);
        timeBlockRepository.deleteByUserId(id);
        deferredItemRepository.deleteByUserId(id);
        reflectionRepository.deleteByUserId(id);
        taskRepository.deleteByUserId(id);
        projectRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    private AppUser findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }
}
