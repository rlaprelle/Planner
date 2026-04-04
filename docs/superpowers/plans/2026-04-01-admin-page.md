# Admin Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a standalone admin page at `/admin` with table-based CRUD for all 6 database entities (Users, Projects, Tasks, Deferred Items, Reflections, Time Blocks), with sidebar navigation and cascade delete with confirmation.

**Architecture:** Dedicated admin controllers under `/api/v1/admin/` with no authentication. Each entity gets its own controller + service pair in a new `admin` package. Frontend gets a self-contained admin layout at `/admin` with reusable table/modal components. No code sharing with existing user-scoped controllers.

**Tech Stack:** Spring Boot (controller/service/repository pattern), React 18, TanStack Query, Radix UI Dialog, Tailwind CSS.

**Spec:** `docs/superpowers/specs/2026-04-01-admin-page-design.md`

---

## File Structure

### Backend — new files in `backend/src/main/java/com/echel/planner/backend/admin/`

| File | Responsibility |
|------|---------------|
| `dto/AdminUserResponse.java` | User DTO with all fields (no passwordHash) |
| `dto/AdminUserRequest.java` | Create/update user request |
| `dto/AdminProjectResponse.java` | Project DTO with user email |
| `dto/AdminProjectRequest.java` | Create/update project request |
| `dto/AdminTaskResponse.java` | Task DTO with user email, project name |
| `dto/AdminTaskRequest.java` | Create/update task request |
| `dto/AdminDeferredItemResponse.java` | Deferred item DTO |
| `dto/AdminDeferredItemRequest.java` | Create/update deferred item request |
| `dto/AdminReflectionResponse.java` | Reflection DTO |
| `dto/AdminReflectionRequest.java` | Create/update reflection request |
| `dto/AdminTimeBlockResponse.java` | Time block DTO |
| `dto/AdminTimeBlockRequest.java` | Create/update time block request |
| `dto/DependentCountResponse.java` | Cascade delete preview counts |
| `AdminUserController.java` | CRUD endpoints for users + dependents count |
| `AdminUserService.java` | User CRUD logic + cascade delete |
| `AdminProjectController.java` | CRUD endpoints for projects |
| `AdminProjectService.java` | Project CRUD logic |
| `AdminTaskController.java` | CRUD endpoints for tasks |
| `AdminTaskService.java` | Task CRUD logic |
| `AdminDeferredItemController.java` | CRUD endpoints for deferred items |
| `AdminDeferredItemService.java` | Deferred item CRUD logic |
| `AdminReflectionController.java` | CRUD endpoints for reflections |
| `AdminReflectionService.java` | Reflection CRUD logic |
| `AdminTimeBlockController.java` | CRUD endpoints for time blocks |
| `AdminTimeBlockService.java` | Time block CRUD logic |
| `AdminExceptionHandler.java` | Not-found exception handler for admin endpoints |

### Backend — modified files

| File | Change |
|------|--------|
| `auth/SecurityConfig.java:25` | Add `.requestMatchers("/api/v1/admin/**").permitAll()` |

### Frontend — new files in `frontend/src/`

| File | Responsibility |
|------|---------------|
| `api/admin.js` | API client for all admin endpoints |
| `pages/admin/AdminPage.jsx` | Admin layout: sidebar + Outlet |
| `pages/admin/AdminUsersTable.jsx` | Users CRUD table |
| `pages/admin/AdminProjectsTable.jsx` | Projects CRUD table |
| `pages/admin/AdminTasksTable.jsx` | Tasks CRUD table |
| `pages/admin/AdminDeferredTable.jsx` | Deferred items CRUD table |
| `pages/admin/AdminReflectionsTable.jsx` | Reflections CRUD table |
| `pages/admin/AdminTimeBlocksTable.jsx` | Time blocks CRUD table |
| `pages/admin/components/AdminTable.jsx` | Reusable data table |
| `pages/admin/components/AdminFormModal.jsx` | Reusable create/edit modal |
| `pages/admin/components/DeleteConfirmDialog.jsx` | Cascade delete confirmation |

### Frontend — modified files

| File | Change |
|------|--------|
| `App.jsx` | Add `/admin/*` routes outside ProtectedRoute |

### E2E — new files

| File | Responsibility |
|------|---------------|
| `e2e/tests/admin.spec.ts` | Admin page E2E tests |
| `e2e/fixtures/admin-data.ts` | Mock data for admin API responses |

---

## Task 1: Open admin routes in Spring Security

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/auth/SecurityConfig.java:25-30`

- [ ] **Step 1: Add permitAll for admin endpoints**

In `SecurityConfig.java`, add the admin matcher before the authenticated catch-all:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/api/v1/admin/**").permitAll()
        .requestMatchers("/api/v1/**").authenticated()
        .anyRequest().denyAll()
)
```

- [ ] **Step 2: Verify backend compiles**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/auth/SecurityConfig.java
git commit -m "feat(admin): open /api/v1/admin/** routes without auth"
```

---

## Task 2: Admin User CRUD — Backend

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminUserResponse.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminUserRequest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/DependentCountResponse.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminUserService.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminUserController.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminExceptionHandler.java`

- [ ] **Step 1: Create AdminUserResponse DTO**

```java
package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.auth.AppUser;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        String timezone,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminUserResponse from(AppUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTimezone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 2: Create AdminUserRequest DTO**

```java
package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(min = 6) String password,
        @NotBlank @Size(max = 255) String displayName,
        @Size(max = 100) String timezone
) {}
```

- [ ] **Step 3: Create DependentCountResponse DTO**

```java
package com.echel.planner.backend.admin.dto;

public record DependentCountResponse(
        long projects,
        long tasks,
        long deferredItems,
        long reflections,
        long timeBlocks
) {}
```

- [ ] **Step 4: Create AdminExceptionHandler**

```java
package com.echel.planner.backend.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.echel.planner.backend.admin")
public class AdminExceptionHandler {

    @ExceptionHandler(AdminNotFoundException.class)
    ProblemDetail handleNotFound(AdminNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    public static class AdminNotFoundException extends RuntimeException {
        public AdminNotFoundException(String message) {
            super(message);
        }
    }
}
```

- [ ] **Step 5: Create AdminUserService**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminUserRequest;
import com.echel.planner.backend.admin.dto.AdminUserResponse;
import com.echel.planner.backend.admin.dto.DependentCountResponse;
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
        // AppUser has no setters for email/displayName/timezone — we need to add them
        // or use a native query. For now, delete + recreate is wrong.
        // We'll need to add setters to AppUser. See step below.
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
        findUser(id); // verify user exists
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
        // Delete in FK-safe order
        timeBlockRepository.deleteByUserId(id);
        deferredItemRepository.deleteByUserId(id);
        reflectionRepository.deleteByUserId(id);
        taskRepository.deleteByUserId(id);
        projectRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    private AppUser findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + id));
    }
}
```

- [ ] **Step 6: Add setters to AppUser entity**

Add these setters to `backend/src/main/java/com/echel/planner/backend/auth/AppUser.java` after the existing getters:

```java
public void setEmail(String email) { this.email = email; }
public void setDisplayName(String displayName) { this.displayName = displayName; }
public void setTimezone(String timezone) { this.timezone = timezone; }
public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
```

- [ ] **Step 7: Add count/delete-by-user queries to repositories**

Add to `ProjectRepository.java`:
```java
long countByUserId(UUID userId);
void deleteByUserId(UUID userId);
```

Add to `TaskRepository.java`:
```java
long countByUserId(UUID userId);
void deleteByUserId(UUID userId);
```

Add to `DeferredItemRepository.java`:
```java
long countByUserId(UUID userId);
void deleteByUserId(UUID userId);
```

Add to `DailyReflectionRepository.java`:
```java
long countByUserId(UUID userId);
void deleteByUserId(UUID userId);
```

Add to `TimeBlockRepository.java`:
```java
long countByUserId(UUID userId);
void deleteByUserId(UUID userId);
```

- [ ] **Step 8: Create AdminUserController**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminUserRequest;
import com.echel.planner.backend.admin.dto.AdminUserResponse;
import com.echel.planner.backend.admin.dto.DependentCountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listAll() {
        return ResponseEntity.ok(adminUserService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.ok(adminUserService.update(id, request));
    }

    @GetMapping("/{id}/dependents")
    public ResponseEntity<DependentCountResponse> getDependents(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getDependentCounts(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 9: Verify backend compiles**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/admin/ \
       backend/src/main/java/com/echel/planner/backend/auth/AppUser.java \
       backend/src/main/java/com/echel/planner/backend/project/ProjectRepository.java \
       backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java \
       backend/src/main/java/com/echel/planner/backend/deferred/DeferredItemRepository.java \
       backend/src/main/java/com/echel/planner/backend/reflection/DailyReflectionRepository.java \
       backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockRepository.java
git commit -m "feat(admin): add admin user CRUD with cascade delete"
```

---

## Task 3: Admin Project CRUD — Backend

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminProjectResponse.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminProjectRequest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminProjectService.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminProjectController.java`

- [ ] **Step 1: Create AdminProjectResponse DTO**

```java
package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.project.Project;
import java.time.Instant;
import java.util.UUID;

public record AdminProjectResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String name,
        String description,
        String color,
        String icon,
        boolean isActive,
        int sortOrder,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminProjectResponse from(Project project) {
        return new AdminProjectResponse(
                project.getId(),
                project.getUser().getId(),
                project.getUser().getEmail(),
                project.getName(),
                project.getDescription(),
                project.getColor(),
                project.getIcon(),
                project.isActive(),
                project.getSortOrder(),
                project.getArchivedAt(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 2: Create AdminProjectRequest DTO**

```java
package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminProjectRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 7) String color,
        @Size(max = 50) String icon,
        Boolean isActive,
        Integer sortOrder
) {}
```

- [ ] **Step 3: Create AdminProjectService**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminProjectRequest;
import com.echel.planner.backend.admin.dto.AdminProjectResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminProjectService {

    private final ProjectRepository projectRepository;
    private final AppUserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DeferredItemRepository deferredItemRepository;

    public AdminProjectService(ProjectRepository projectRepository,
                               AppUserRepository userRepository,
                               TaskRepository taskRepository,
                               DeferredItemRepository deferredItemRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.deferredItemRepository = deferredItemRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminProjectResponse> listAll() {
        return projectRepository.findAll().stream()
                .map(AdminProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminProjectResponse get(UUID id) {
        return AdminProjectResponse.from(findProject(id));
    }

    public AdminProjectResponse create(AdminProjectRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
        Project project = new Project(user, request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setIcon(request.icon());
        if (request.isActive() != null) {
            project.setActive(request.isActive());
        }
        if (request.sortOrder() != null) {
            project.setSortOrder(request.sortOrder());
        }
        return AdminProjectResponse.from(projectRepository.save(project));
    }

    public AdminProjectResponse update(UUID id, AdminProjectRequest request) {
        Project project = findProject(id);
        if (request.userId() != null) {
            AppUser user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
            project.setUser(user);
        }
        project.setName(request.name());
        project.setDescription(request.description());
        project.setColor(request.color());
        project.setIcon(request.icon());
        if (request.isActive() != null) {
            project.setActive(request.isActive());
            if (!request.isActive()) {
                project.setArchivedAt(Instant.now());
            }
        }
        if (request.sortOrder() != null) {
            project.setSortOrder(request.sortOrder());
        }
        return AdminProjectResponse.from(project);
    }

    public void delete(UUID id) {
        Project project = findProject(id);
        deferredItemRepository.deleteByResolvedProjectId(id);
        taskRepository.deleteByProjectId(id);
        projectRepository.delete(project);
    }

    private Project findProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Project not found: " + id));
    }
}
```

- [ ] **Step 4: Add delete-by-project queries to repositories**

Add to `TaskRepository.java`:
```java
void deleteByProjectId(UUID projectId);
```

Add to `DeferredItemRepository.java`:
```java
void deleteByResolvedProjectId(UUID projectId);
```

- [ ] **Step 5: Add setUser setter to Project entity**

Add to `backend/src/main/java/com/echel/planner/backend/project/Project.java`:
```java
public void setUser(AppUser user) { this.user = user; }
```

- [ ] **Step 6: Create AdminProjectController**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminProjectRequest;
import com.echel.planner.backend.admin.dto.AdminProjectResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/projects")
public class AdminProjectController {

    private final AdminProjectService adminProjectService;

    public AdminProjectController(AdminProjectService adminProjectService) {
        this.adminProjectService = adminProjectService;
    }

    @GetMapping
    public ResponseEntity<List<AdminProjectResponse>> listAll() {
        return ResponseEntity.ok(adminProjectService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminProjectResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminProjectService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminProjectResponse> create(@Valid @RequestBody AdminProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminProjectService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminProjectResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody AdminProjectRequest request) {
        return ResponseEntity.ok(adminProjectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminProjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 7: Verify backend compiles**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/admin/ \
       backend/src/main/java/com/echel/planner/backend/project/Project.java \
       backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java \
       backend/src/main/java/com/echel/planner/backend/deferred/DeferredItemRepository.java
git commit -m "feat(admin): add admin project CRUD with cascade delete"
```

---

## Task 4: Admin Task CRUD — Backend

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminTaskResponse.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminTaskRequest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminTaskService.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminTaskController.java`

- [ ] **Step 1: Create AdminTaskResponse DTO**

```java
package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.task.Task;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminTaskResponse(
        UUID id,
        UUID userId,
        String userEmail,
        UUID projectId,
        String projectName,
        String title,
        String description,
        UUID parentTaskId,
        String status,
        short priority,
        Short pointsEstimate,
        Integer actualMinutes,
        String energyLevel,
        LocalDate dueDate,
        int sortOrder,
        UUID blockedByTaskId,
        Instant archivedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminTaskResponse from(Task task) {
        return new AdminTaskResponse(
                task.getId(),
                task.getUser().getId(),
                task.getUser().getEmail(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getTitle(),
                task.getDescription(),
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                task.getStatus().name(),
                task.getPriority(),
                task.getPointsEstimate(),
                task.getActualMinutes(),
                task.getEnergyLevel() != null ? task.getEnergyLevel().name() : null,
                task.getDueDate(),
                task.getSortOrder(),
                task.getBlockedByTask() != null ? task.getBlockedByTask().getId() : null,
                task.getArchivedAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 2: Create AdminTaskRequest DTO**

```java
package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record AdminTaskRequest(
        @NotNull UUID userId,
        @NotNull UUID projectId,
        @NotBlank String title,
        String description,
        UUID parentTaskId,
        String status,
        Short priority,
        Short pointsEstimate,
        Integer actualMinutes,
        String energyLevel,
        LocalDate dueDate,
        Integer sortOrder,
        UUID blockedByTaskId
) {}
```

- [ ] **Step 3: Create AdminTaskService**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTaskRequest;
import com.echel.planner.backend.admin.dto.AdminTaskResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.task.EnergyLevel;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminTaskService {

    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final DeferredItemRepository deferredItemRepository;

    public AdminTaskService(TaskRepository taskRepository,
                            AppUserRepository userRepository,
                            ProjectRepository projectRepository,
                            TimeBlockRepository timeBlockRepository,
                            DeferredItemRepository deferredItemRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.deferredItemRepository = deferredItemRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTaskResponse> listAll() {
        return taskRepository.findAll().stream()
                .map(AdminTaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminTaskResponse get(UUID id) {
        return AdminTaskResponse.from(findTask(id));
    }

    public AdminTaskResponse create(AdminTaskRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Project not found: " + request.projectId()));
        Task task = new Task(user, project, request.title());
        applyFields(task, request);
        return AdminTaskResponse.from(taskRepository.save(task));
    }

    public AdminTaskResponse update(UUID id, AdminTaskRequest request) {
        Task task = findTask(id);
        if (request.userId() != null) {
            AppUser user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
            task.setUser(user);
        }
        if (request.projectId() != null) {
            Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Project not found: " + request.projectId()));
            task.setProject(project);
        }
        task.setTitle(request.title());
        applyFields(task, request);
        return AdminTaskResponse.from(task);
    }

    public void delete(UUID id) {
        findTask(id);
        timeBlockRepository.deleteByTaskId(id);
        deferredItemRepository.deleteByResolvedTaskId(id);
        taskRepository.deleteByParentTaskId(id);
        taskRepository.deleteById(id);
    }

    private void applyFields(Task task, AdminTaskRequest request) {
        task.setDescription(request.description());
        if (request.status() != null) {
            task.setStatus(TaskStatus.valueOf(request.status()));
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setPointsEstimate(request.pointsEstimate());
        task.setActualMinutes(request.actualMinutes());
        if (request.energyLevel() != null) {
            task.setEnergyLevel(EnergyLevel.valueOf(request.energyLevel()));
        } else {
            task.setEnergyLevel(null);
        }
        task.setDueDate(request.dueDate());
        if (request.sortOrder() != null) {
            task.setSortOrder(request.sortOrder());
        }
        if (request.parentTaskId() != null) {
            task.setParentTask(taskRepository.findById(request.parentTaskId()).orElse(null));
        } else {
            task.setParentTask(null);
        }
        if (request.blockedByTaskId() != null) {
            task.setBlockedByTask(taskRepository.findById(request.blockedByTaskId()).orElse(null));
        } else {
            task.setBlockedByTask(null);
        }
    }

    private Task findTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Task not found: " + id));
    }
}
```

- [ ] **Step 4: Add setUser setter to Task entity and required repository methods**

Add to `Task.java`:
```java
public void setUser(AppUser user) { this.user = user; }
```

Add to `TaskRepository.java`:
```java
void deleteByParentTaskId(UUID parentTaskId);
```

Add to `TimeBlockRepository.java`:
```java
void deleteByTaskId(UUID taskId);
```

Add to `DeferredItemRepository.java`:
```java
void deleteByResolvedTaskId(UUID taskId);
```

- [ ] **Step 5: Create AdminTaskController**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTaskRequest;
import com.echel.planner.backend.admin.dto.AdminTaskResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tasks")
public class AdminTaskController {

    private final AdminTaskService adminTaskService;

    public AdminTaskController(AdminTaskService adminTaskService) {
        this.adminTaskService = adminTaskService;
    }

    @GetMapping
    public ResponseEntity<List<AdminTaskResponse>> listAll() {
        return ResponseEntity.ok(adminTaskService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminTaskResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminTaskService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminTaskResponse> create(@Valid @RequestBody AdminTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTaskService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminTaskResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody AdminTaskRequest request) {
        return ResponseEntity.ok(adminTaskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminTaskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 6: Verify backend compiles**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/admin/ \
       backend/src/main/java/com/echel/planner/backend/task/Task.java \
       backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java \
       backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockRepository.java \
       backend/src/main/java/com/echel/planner/backend/deferred/DeferredItemRepository.java
git commit -m "feat(admin): add admin task CRUD with cascade delete"
```

---

## Task 5: Admin Deferred Item CRUD — Backend

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminDeferredItemResponse.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminDeferredItemRequest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminDeferredItemService.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminDeferredItemController.java`

- [ ] **Step 1: Create DTOs**

`AdminDeferredItemResponse.java`:
```java
package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.deferred.DeferredItem;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminDeferredItemResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String rawText,
        boolean isProcessed,
        Instant capturedAt,
        Instant processedAt,
        UUID resolvedTaskId,
        UUID resolvedProjectId,
        LocalDate deferredUntilDate,
        int deferralCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminDeferredItemResponse from(DeferredItem item) {
        return new AdminDeferredItemResponse(
                item.getId(),
                item.getUser().getId(),
                item.getUser().getEmail(),
                item.getRawText(),
                item.isProcessed(),
                item.getCapturedAt(),
                item.getProcessedAt(),
                item.getResolvedTask() != null ? item.getResolvedTask().getId() : null,
                item.getResolvedProject() != null ? item.getResolvedProject().getId() : null,
                item.getDeferredUntilDate(),
                item.getDeferralCount(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
```

`AdminDeferredItemRequest.java`:
```java
package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record AdminDeferredItemRequest(
        @NotNull UUID userId,
        @NotBlank String rawText,
        Boolean isProcessed,
        UUID resolvedTaskId,
        UUID resolvedProjectId,
        LocalDate deferredUntilDate,
        Integer deferralCount
) {}
```

- [ ] **Step 2: Create AdminDeferredItemService**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminDeferredItemRequest;
import com.echel.planner.backend.admin.dto.AdminDeferredItemResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.deferred.DeferredItem;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.TaskRepository;
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
            if (request.isProcessed()) {
                item.setProcessedAt(Instant.now());
            }
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
```

- [ ] **Step 3: Add setRawText setter to DeferredItem**

Add to `backend/src/main/java/com/echel/planner/backend/deferred/DeferredItem.java`:
```java
public void setRawText(String rawText) { this.rawText = rawText; }
```

- [ ] **Step 4: Create AdminDeferredItemController**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminDeferredItemRequest;
import com.echel.planner.backend.admin.dto.AdminDeferredItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/deferred-items")
public class AdminDeferredItemController {

    private final AdminDeferredItemService adminDeferredItemService;

    public AdminDeferredItemController(AdminDeferredItemService adminDeferredItemService) {
        this.adminDeferredItemService = adminDeferredItemService;
    }

    @GetMapping
    public ResponseEntity<List<AdminDeferredItemResponse>> listAll() {
        return ResponseEntity.ok(adminDeferredItemService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminDeferredItemResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminDeferredItemService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminDeferredItemResponse> create(@Valid @RequestBody AdminDeferredItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDeferredItemService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminDeferredItemResponse> update(@PathVariable UUID id,
                                                             @Valid @RequestBody AdminDeferredItemRequest request) {
        return ResponseEntity.ok(adminDeferredItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminDeferredItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 5: Verify backend compiles**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/admin/ \
       backend/src/main/java/com/echel/planner/backend/deferred/DeferredItem.java
git commit -m "feat(admin): add admin deferred item CRUD"
```

---

## Task 6: Admin Reflection CRUD — Backend

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminReflectionResponse.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminReflectionRequest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminReflectionService.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminReflectionController.java`

- [ ] **Step 1: Create DTOs**

`AdminReflectionResponse.java`:
```java
package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.reflection.DailyReflection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminReflectionResponse(
        UUID id,
        UUID userId,
        String userEmail,
        LocalDate reflectionDate,
        short energyRating,
        short moodRating,
        String reflectionNotes,
        boolean isFinalized,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminReflectionResponse from(DailyReflection r) {
        return new AdminReflectionResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getEmail(),
                r.getReflectionDate(),
                r.getEnergyRating(),
                r.getMoodRating(),
                r.getReflectionNotes(),
                r.isFinalized(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
```

`AdminReflectionRequest.java`:
```java
package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record AdminReflectionRequest(
        @NotNull UUID userId,
        @NotNull LocalDate reflectionDate,
        @NotNull @Min(1) @Max(5) Short energyRating,
        @NotNull @Min(1) @Max(5) Short moodRating,
        String reflectionNotes,
        Boolean isFinalized
) {}
```

- [ ] **Step 2: Create AdminReflectionService**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminReflectionRequest;
import com.echel.planner.backend.admin.dto.AdminReflectionResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.reflection.DailyReflection;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminReflectionService {

    private final DailyReflectionRepository reflectionRepository;
    private final AppUserRepository userRepository;

    public AdminReflectionService(DailyReflectionRepository reflectionRepository,
                                  AppUserRepository userRepository) {
        this.reflectionRepository = reflectionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminReflectionResponse> listAll() {
        return reflectionRepository.findAll().stream()
                .map(AdminReflectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminReflectionResponse get(UUID id) {
        return AdminReflectionResponse.from(findReflection(id));
    }

    public AdminReflectionResponse create(AdminReflectionRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
        DailyReflection reflection = new DailyReflection(user, request.reflectionDate());
        reflection.setEnergyRating(request.energyRating());
        reflection.setMoodRating(request.moodRating());
        reflection.setReflectionNotes(request.reflectionNotes());
        if (request.isFinalized() != null) {
            reflection.setFinalized(request.isFinalized());
        }
        return AdminReflectionResponse.from(reflectionRepository.save(reflection));
    }

    public AdminReflectionResponse update(UUID id, AdminReflectionRequest request) {
        DailyReflection reflection = findReflection(id);
        reflection.setEnergyRating(request.energyRating());
        reflection.setMoodRating(request.moodRating());
        reflection.setReflectionNotes(request.reflectionNotes());
        if (request.isFinalized() != null) {
            reflection.setFinalized(request.isFinalized());
        }
        return AdminReflectionResponse.from(reflection);
    }

    public void delete(UUID id) {
        findReflection(id);
        reflectionRepository.deleteById(id);
    }

    private DailyReflection findReflection(UUID id) {
        return reflectionRepository.findById(id)
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Reflection not found: " + id));
    }
}
```

- [ ] **Step 3: Create AdminReflectionController**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminReflectionRequest;
import com.echel.planner.backend.admin.dto.AdminReflectionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reflections")
public class AdminReflectionController {

    private final AdminReflectionService adminReflectionService;

    public AdminReflectionController(AdminReflectionService adminReflectionService) {
        this.adminReflectionService = adminReflectionService;
    }

    @GetMapping
    public ResponseEntity<List<AdminReflectionResponse>> listAll() {
        return ResponseEntity.ok(adminReflectionService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminReflectionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminReflectionService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminReflectionResponse> create(@Valid @RequestBody AdminReflectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminReflectionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminReflectionResponse> update(@PathVariable UUID id,
                                                           @Valid @RequestBody AdminReflectionRequest request) {
        return ResponseEntity.ok(adminReflectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminReflectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Verify backend compiles**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/admin/
git commit -m "feat(admin): add admin reflection CRUD"
```

---

## Task 7: Admin Time Block CRUD — Backend

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminTimeBlockResponse.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/dto/AdminTimeBlockRequest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminTimeBlockService.java`
- Create: `backend/src/main/java/com/echel/planner/backend/admin/AdminTimeBlockController.java`

- [ ] **Step 1: Create DTOs**

`AdminTimeBlockResponse.java`:
```java
package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.schedule.TimeBlock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AdminTimeBlockResponse(
        UUID id,
        UUID userId,
        String userEmail,
        LocalDate blockDate,
        UUID taskId,
        String taskTitle,
        LocalTime startTime,
        LocalTime endTime,
        int sortOrder,
        Instant actualStart,
        Instant actualEnd,
        boolean wasCompleted
) {
    public static AdminTimeBlockResponse from(TimeBlock tb) {
        return new AdminTimeBlockResponse(
                tb.getId(),
                tb.getUser().getId(),
                tb.getUser().getEmail(),
                tb.getBlockDate(),
                tb.getTask() != null ? tb.getTask().getId() : null,
                tb.getTask() != null ? tb.getTask().getTitle() : null,
                tb.getStartTime(),
                tb.getEndTime(),
                tb.getSortOrder(),
                tb.getActualStart(),
                tb.getActualEnd(),
                tb.isWasCompleted()
        );
    }
}
```

`AdminTimeBlockRequest.java`:
```java
package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AdminTimeBlockRequest(
        @NotNull UUID userId,
        @NotNull LocalDate blockDate,
        UUID taskId,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Integer sortOrder,
        Boolean wasCompleted
) {}
```

- [ ] **Step 2: Create AdminTimeBlockService**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTimeBlockRequest;
import com.echel.planner.backend.admin.dto.AdminTimeBlockResponse;
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
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("User not found: " + request.userId()));
        Task task = null;
        if (request.taskId() != null) {
            task = taskRepository.findById(request.taskId())
                    .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Task not found: " + request.taskId()));
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
                    .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Task not found: " + request.taskId()));
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
                .orElseThrow(() -> new AdminExceptionHandler.AdminNotFoundException("Time block not found: " + id));
    }
}
```

- [ ] **Step 3: Add setters to TimeBlock entity**

Add to `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlock.java`:
```java
public void setBlockDate(LocalDate blockDate) { this.blockDate = blockDate; }
public void setTask(Task task) { this.task = task; }
public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
```

- [ ] **Step 4: Create AdminTimeBlockController**

```java
package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTimeBlockRequest;
import com.echel.planner.backend.admin.dto.AdminTimeBlockResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/time-blocks")
public class AdminTimeBlockController {

    private final AdminTimeBlockService adminTimeBlockService;

    public AdminTimeBlockController(AdminTimeBlockService adminTimeBlockService) {
        this.adminTimeBlockService = adminTimeBlockService;
    }

    @GetMapping
    public ResponseEntity<List<AdminTimeBlockResponse>> listAll() {
        return ResponseEntity.ok(adminTimeBlockService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminTimeBlockResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminTimeBlockService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminTimeBlockResponse> create(@Valid @RequestBody AdminTimeBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTimeBlockService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminTimeBlockResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody AdminTimeBlockRequest request) {
        return ResponseEntity.ok(adminTimeBlockService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminTimeBlockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 5: Verify backend compiles**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Run all backend tests**

Run: `cd backend && mvn test`
Expected: All existing tests pass (admin endpoints don't break anything)

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/admin/ \
       backend/src/main/java/com/echel/planner/backend/schedule/TimeBlock.java
git commit -m "feat(admin): add admin time block CRUD — backend complete"
```

---

## Task 8: Frontend Admin API Client

**Files:**
- Create: `frontend/src/api/admin.js`

- [ ] **Step 1: Create admin API module**

Admin endpoints don't require auth, so use plain `fetch` instead of `authFetch`:

```javascript
const BASE = '/api/v1/admin'

async function handleResponse(res) {
  if (!res.ok) {
    let message = `HTTP ${res.status}`
    try {
      const body = await res.json()
      message = body.detail || body.message || body.error || message
    } catch {
      // ignore parse errors
    }
    const err = new Error(message)
    err.status = res.status
    throw err
  }
  if (res.status === 204) return null
  return res.json()
}

function adminFetch(path, options = {}) {
  return fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
}

// Users
export async function getUsers() {
  return handleResponse(await adminFetch('/users'))
}
export async function getUser(id) {
  return handleResponse(await adminFetch(`/users/${id}`))
}
export async function createUser(data) {
  return handleResponse(await adminFetch('/users', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateUser(id, data) {
  return handleResponse(await adminFetch(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function getUserDependents(id) {
  return handleResponse(await adminFetch(`/users/${id}/dependents`))
}
export async function deleteUser(id) {
  return handleResponse(await adminFetch(`/users/${id}`, { method: 'DELETE' }))
}

// Projects
export async function getProjects() {
  return handleResponse(await adminFetch('/projects'))
}
export async function createProject(data) {
  return handleResponse(await adminFetch('/projects', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateProject(id, data) {
  return handleResponse(await adminFetch(`/projects/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteProject(id) {
  return handleResponse(await adminFetch(`/projects/${id}`, { method: 'DELETE' }))
}

// Tasks
export async function getTasks() {
  return handleResponse(await adminFetch('/tasks'))
}
export async function createTask(data) {
  return handleResponse(await adminFetch('/tasks', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateTask(id, data) {
  return handleResponse(await adminFetch(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteTask(id) {
  return handleResponse(await adminFetch(`/tasks/${id}`, { method: 'DELETE' }))
}

// Deferred Items
export async function getDeferredItems() {
  return handleResponse(await adminFetch('/deferred-items'))
}
export async function createDeferredItem(data) {
  return handleResponse(await adminFetch('/deferred-items', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateDeferredItem(id, data) {
  return handleResponse(await adminFetch(`/deferred-items/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteDeferredItem(id) {
  return handleResponse(await adminFetch(`/deferred-items/${id}`, { method: 'DELETE' }))
}

// Reflections
export async function getReflections() {
  return handleResponse(await adminFetch('/reflections'))
}
export async function createReflection(data) {
  return handleResponse(await adminFetch('/reflections', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateReflection(id, data) {
  return handleResponse(await adminFetch(`/reflections/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteReflection(id) {
  return handleResponse(await adminFetch(`/reflections/${id}`, { method: 'DELETE' }))
}

// Time Blocks
export async function getTimeBlocks() {
  return handleResponse(await adminFetch('/time-blocks'))
}
export async function createTimeBlock(data) {
  return handleResponse(await adminFetch('/time-blocks', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateTimeBlock(id, data) {
  return handleResponse(await adminFetch(`/time-blocks/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteTimeBlock(id) {
  return handleResponse(await adminFetch(`/time-blocks/${id}`, { method: 'DELETE' }))
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/admin.js
git commit -m "feat(admin): add frontend admin API client"
```

---

## Task 9: Frontend Admin Shared Components

**Files:**
- Create: `frontend/src/pages/admin/components/AdminTable.jsx`
- Create: `frontend/src/pages/admin/components/AdminFormModal.jsx`
- Create: `frontend/src/pages/admin/components/DeleteConfirmDialog.jsx`

- [ ] **Step 1: Create AdminTable component**

```jsx
import { useState } from 'react'

export function AdminTable({ columns, data, onEdit, onDelete, entityName }) {
  const [sortCol, setSortCol] = useState(null)
  const [sortDir, setSortDir] = useState('asc')

  const handleSort = (col) => {
    if (sortCol === col) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(col)
      setSortDir('asc')
    }
  }

  const sorted = sortCol
    ? [...data].sort((a, b) => {
        const va = a[sortCol] ?? ''
        const vb = b[sortCol] ?? ''
        const cmp = String(va).localeCompare(String(vb), undefined, { numeric: true })
        return sortDir === 'asc' ? cmp : -cmp
      })
    : data

  const truncateId = (id) => id ? String(id).slice(0, 8) : ''

  return (
    <div className="overflow-x-auto border border-gray-200 rounded-lg">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-200">
            <th className="px-3 py-2 text-left text-xs font-semibold text-gray-600 cursor-pointer"
                onClick={() => handleSort('id')}>
              ID {sortCol === 'id' && (sortDir === 'asc' ? '↑' : '↓')}
            </th>
            {columns.map(col => (
              <th key={col.key}
                  className="px-3 py-2 text-left text-xs font-semibold text-gray-600 cursor-pointer"
                  onClick={() => handleSort(col.key)}>
                {col.label} {sortCol === col.key && (sortDir === 'asc' ? '↑' : '↓')}
              </th>
            ))}
            <th className="px-3 py-2 text-right text-xs font-semibold text-gray-600">Actions</th>
          </tr>
        </thead>
        <tbody>
          {sorted.length === 0 && (
            <tr>
              <td colSpan={columns.length + 2} className="px-3 py-8 text-center text-gray-400">
                No {entityName} found
              </td>
            </tr>
          )}
          {sorted.map((row, i) => (
            <tr key={row.id} className={i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}>
              <td className="px-3 py-2 text-xs text-gray-400 font-mono cursor-pointer"
                  title={row.id}
                  onClick={() => navigator.clipboard.writeText(row.id)}>
                {truncateId(row.id)}
              </td>
              {columns.map(col => (
                <td key={col.key} className="px-3 py-2 text-gray-700 max-w-[200px] truncate">
                  {col.render ? col.render(row[col.key], row) : formatValue(row[col.key])}
                </td>
              ))}
              <td className="px-3 py-2 text-right space-x-2">
                <button onClick={() => onEdit(row)}
                        className="text-xs text-blue-600 hover:text-blue-800 font-medium">
                  Edit
                </button>
                <button onClick={() => onDelete(row)}
                        className="text-xs text-red-600 hover:text-red-800 font-medium">
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function formatValue(val) {
  if (val == null) return '—'
  if (typeof val === 'boolean') return val ? 'Yes' : 'No'
  if (typeof val === 'string' && val.match(/^\d{4}-\d{2}-\d{2}T/)) {
    return new Date(val).toLocaleDateString()
  }
  return String(val)
}
```

- [ ] **Step 2: Create AdminFormModal component**

```jsx
import { useState, useEffect } from 'react'
import * as Dialog from '@radix-ui/react-dialog'

export function AdminFormModal({ open, onOpenChange, title, fields, initialValues, onSubmit, isPending }) {
  const [values, setValues] = useState({})

  useEffect(() => {
    if (open) {
      const defaults = {}
      fields.forEach(f => {
        defaults[f.name] = initialValues?.[f.name] ?? f.defaultValue ?? ''
      })
      setValues(defaults)
    }
  }, [open, initialValues, fields])

  const handleChange = (name, value) => {
    setValues(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    // Clean up empty strings to null for optional fields
    const cleaned = {}
    fields.forEach(f => {
      const v = values[f.name]
      if (v === '' && !f.required) {
        cleaned[f.name] = null
      } else if (f.type === 'number' && v !== '' && v != null) {
        cleaned[f.name] = Number(v)
      } else if (f.type === 'checkbox') {
        cleaned[f.name] = Boolean(v)
      } else {
        cleaned[f.name] = v
      }
    })
    onSubmit(cleaned)
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white rounded-xl shadow-xl p-6 w-full max-w-lg max-h-[85vh] overflow-y-auto">
          <Dialog.Title className="text-lg font-semibold text-gray-900 mb-4">{title}</Dialog.Title>
          <form onSubmit={handleSubmit} className="space-y-3">
            {fields.map(f => (
              <div key={f.name}>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  {f.label}{f.required && <span className="text-red-500 ml-0.5">*</span>}
                </label>
                {f.type === 'select' ? (
                  <select
                    value={values[f.name] ?? ''}
                    onChange={e => handleChange(f.name, e.target.value)}
                    required={f.required}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">— Select —</option>
                    {f.options?.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                ) : f.type === 'textarea' ? (
                  <textarea
                    value={values[f.name] ?? ''}
                    onChange={e => handleChange(f.name, e.target.value)}
                    required={f.required}
                    rows={3}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                ) : f.type === 'checkbox' ? (
                  <input
                    type="checkbox"
                    checked={!!values[f.name]}
                    onChange={e => handleChange(f.name, e.target.checked)}
                    className="rounded border-gray-300"
                  />
                ) : (
                  <input
                    type={f.type || 'text'}
                    value={values[f.name] ?? ''}
                    onChange={e => handleChange(f.name, e.target.value)}
                    required={f.required}
                    placeholder={f.placeholder}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                )}
              </div>
            ))}
            <div className="flex justify-end gap-2 pt-2">
              <Dialog.Close asChild>
                <button type="button" className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">
                  Cancel
                </button>
              </Dialog.Close>
              <button
                type="submit"
                disabled={isPending}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {isPending ? 'Saving...' : 'Save'}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
```

- [ ] **Step 3: Create DeleteConfirmDialog component**

```jsx
import * as Dialog from '@radix-ui/react-dialog'

export function DeleteConfirmDialog({ open, onOpenChange, entityName, item, dependentCounts, onConfirm, isPending }) {
  const hasDependents = dependentCounts && Object.values(dependentCounts).some(v => v > 0)

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white rounded-xl shadow-xl p-6 w-full max-w-md">
          <Dialog.Title className="text-lg font-semibold text-gray-900 mb-2">
            Delete {entityName}?
          </Dialog.Title>
          <Dialog.Description className="text-sm text-gray-600 mb-4">
            This action cannot be undone.
          </Dialog.Description>

          {hasDependents && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm">
              <p className="font-medium text-red-800 mb-1">This will also delete:</p>
              <ul className="text-red-700 space-y-0.5">
                {dependentCounts.projects > 0 && (
                  <li>• {dependentCounts.projects} project{dependentCounts.projects !== 1 ? 's' : ''}</li>
                )}
                {dependentCounts.tasks > 0 && (
                  <li>• {dependentCounts.tasks} task{dependentCounts.tasks !== 1 ? 's' : ''}</li>
                )}
                {dependentCounts.deferredItems > 0 && (
                  <li>• {dependentCounts.deferredItems} deferred item{dependentCounts.deferredItems !== 1 ? 's' : ''}</li>
                )}
                {dependentCounts.reflections > 0 && (
                  <li>• {dependentCounts.reflections} reflection{dependentCounts.reflections !== 1 ? 's' : ''}</li>
                )}
                {dependentCounts.timeBlocks > 0 && (
                  <li>• {dependentCounts.timeBlocks} time block{dependentCounts.timeBlocks !== 1 ? 's' : ''}</li>
                )}
              </ul>
            </div>
          )}

          <div className="flex justify-end gap-2">
            <Dialog.Close asChild>
              <button className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">Cancel</button>
            </Dialog.Close>
            <button
              onClick={onConfirm}
              disabled={isPending}
              className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
            >
              {isPending ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
```

- [ ] **Step 4: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/admin/components/
git commit -m "feat(admin): add shared AdminTable, AdminFormModal, DeleteConfirmDialog components"
```

---

## Task 10: Frontend Admin Layout + Routing

**Files:**
- Create: `frontend/src/pages/admin/AdminPage.jsx`
- Modify: `frontend/src/App.jsx`

- [ ] **Step 1: Create AdminPage layout with sidebar**

```jsx
import { NavLink, Outlet, Navigate } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/projects', label: 'Projects' },
  { to: '/admin/tasks', label: 'Tasks' },
  { to: '/admin/deferred', label: 'Deferred Items' },
  { to: '/admin/reflections', label: 'Reflections' },
  { to: '/admin/time-blocks', label: 'Time Blocks' },
]

export default function AdminPage() {
  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <nav className="w-56 bg-gray-900 text-gray-300 flex flex-col shrink-0">
        <div className="px-4 py-4 text-white font-bold text-lg border-b border-gray-700">
          Admin
        </div>
        <div className="flex-1 py-2">
          {NAV_ITEMS.map(item => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `block px-4 py-2 text-sm ${isActive ? 'bg-gray-700 text-white font-medium' : 'hover:bg-gray-800 hover:text-white'}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </div>
        <div className="px-4 py-3 border-t border-gray-700">
          <a href="/" className="text-xs text-gray-500 hover:text-gray-300">← Back to app</a>
        </div>
      </nav>

      {/* Main content */}
      <main className="flex-1 overflow-auto p-6">
        <Outlet />
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Add admin routes to App.jsx**

Add the import at the top of `App.jsx`:
```javascript
import AdminPage from '@/pages/admin/AdminPage'
```

Add the admin routes *before* the fallback route and *outside* the ProtectedRoute:
```jsx
{/* Admin routes — no auth, own layout */}
<Route path="/admin" element={<AdminPage />}>
  <Route index element={<Navigate to="/admin/users" replace />} />
</Route>
```

The full Routes section becomes:
```jsx
<Routes>
  {/* Public routes — no sidebar */}
  <Route path="/login" element={<LoginPage />} />
  <Route path="/register" element={<RegisterPage />} />

  {/* Admin routes — no auth, own layout */}
  <Route path="/admin" element={<AdminPage />}>
    <Route index element={<Navigate to="/admin/users" replace />} />
  </Route>

  {/* Protected routes */}
  <Route element={<ProtectedRoute />}>
    {/* Session page — full-screen, no sidebar */}
    <Route path="/session/:blockId" element={<ActiveSessionPage />} />

    {/* App routes — wrapped in AppLayout (sidebar + main area) */}
    <Route element={<AppLayout />}>
      <Route path="/" element={<DashboardPage />} />
      <Route path="/today" element={<TodayPage />} />
      <Route path="/projects" element={<ProjectsPage />} />
      <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
      <Route path="/inbox" element={<InboxPage />} />
      <Route path="/end-day" element={<EndDayPage />} />
      <Route path="/start-day" element={<StartDayPage />} />
    </Route>
  </Route>

  {/* Fallback */}
  <Route path="*" element={<Navigate to="/" replace />} />
</Routes>
```

- [ ] **Step 3: Verify lint passes and page loads**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/admin/AdminPage.jsx frontend/src/App.jsx
git commit -m "feat(admin): add admin layout with sidebar and routing"
```

---

## Task 11: Admin Users Table Page

**Files:**
- Create: `frontend/src/pages/admin/AdminUsersTable.jsx`
- Modify: `frontend/src/App.jsx` (add route)

- [ ] **Step 1: Create AdminUsersTable page**

```jsx
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getUsers, createUser, updateUser, deleteUser, getUserDependents } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'email', label: 'Email' },
  { key: 'displayName', label: 'Display Name' },
  { key: 'timezone', label: 'Timezone' },
  { key: 'createdAt', label: 'Created' },
]

const FORM_FIELDS = [
  { name: 'email', label: 'Email', type: 'email', required: true },
  { name: 'password', label: 'Password', type: 'password', required: false, placeholder: 'Leave blank to keep current' },
  { name: 'displayName', label: 'Display Name', required: true },
  { name: 'timezone', label: 'Timezone', defaultValue: 'UTC' },
]

export default function AdminUsersTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)
  const [dependents, setDependents] = useState(null)

  const { data: users = [], isLoading } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const createMutation = useMutation({
    mutationFn: (data) => createUser(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateUser(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteUser(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }); setDeleteItem(null) },
  })

  const handleEdit = (row) => { setEditItem(row); setFormOpen(true) }
  const handleDelete = async (row) => {
    const counts = await getUserDependents(row.id)
    setDependents(counts)
    setDeleteItem(row)
  }
  const handleSubmit = (data) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  const formFields = editItem
    ? FORM_FIELDS
    : FORM_FIELDS.map(f => f.name === 'password' ? { ...f, required: true } : f)

  if (isLoading) return <div className="text-gray-400 py-8 text-center">Loading...</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">Users</h1>
        <button
          onClick={() => { setEditItem(null); setFormOpen(true) }}
          className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          + Create User
        </button>
      </div>

      <AdminTable columns={COLUMNS} data={users} onEdit={handleEdit} onDelete={handleDelete} entityName="users" />

      <AdminFormModal
        open={formOpen}
        onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit User' : 'Create User'}
        fields={formFields}
        initialValues={editItem}
        onSubmit={handleSubmit}
        isPending={createMutation.isPending || updateMutation.isPending}
      />

      <DeleteConfirmDialog
        open={!!deleteItem}
        onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="user"
        item={deleteItem}
        dependentCounts={dependents}
        onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending}
      />
    </div>
  )
}
```

- [ ] **Step 2: Add route for users table**

In `App.jsx`, add inside the admin Route:
```jsx
import AdminUsersTable from '@/pages/admin/AdminUsersTable'
```

```jsx
<Route path="/admin" element={<AdminPage />}>
  <Route index element={<Navigate to="/admin/users" replace />} />
  <Route path="users" element={<AdminUsersTable />} />
</Route>
```

- [ ] **Step 3: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/admin/AdminUsersTable.jsx frontend/src/App.jsx
git commit -m "feat(admin): add admin users table page"
```

---

## Task 12: Remaining Entity Table Pages

**Files:**
- Create: `frontend/src/pages/admin/AdminProjectsTable.jsx`
- Create: `frontend/src/pages/admin/AdminTasksTable.jsx`
- Create: `frontend/src/pages/admin/AdminDeferredTable.jsx`
- Create: `frontend/src/pages/admin/AdminReflectionsTable.jsx`
- Create: `frontend/src/pages/admin/AdminTimeBlocksTable.jsx`
- Modify: `frontend/src/App.jsx` (add all remaining routes)

All entity pages follow the same pattern as AdminUsersTable. The key differences are the column definitions, form fields, and which API functions to call.

- [ ] **Step 1: Create AdminProjectsTable**

```jsx
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getProjects, createProject, updateProject, deleteProject, getUsers } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'name', label: 'Name' },
  { key: 'description', label: 'Description' },
  { key: 'color', label: 'Color', render: (v) => v ? <span className="inline-flex items-center gap-1"><span className="w-3 h-3 rounded-full inline-block" style={{ background: v }} />{v}</span> : '—' },
  { key: 'isActive', label: 'Active' },
  { key: 'createdAt', label: 'Created' },
]

export default function AdminProjectsTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: projects = [], isLoading } = useQuery({ queryKey: ['admin', 'projects'], queryFn: getProjects })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'name', label: 'Name', required: true },
    { name: 'description', label: 'Description', type: 'textarea' },
    { name: 'color', label: 'Color (hex)', placeholder: '#6b4c9a' },
    { name: 'icon', label: 'Icon' },
    { name: 'isActive', label: 'Active', type: 'checkbox', defaultValue: true },
    { name: 'sortOrder', label: 'Sort Order', type: 'number', defaultValue: 0 },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createProject(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'projects'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateProject(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'projects'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteProject(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'projects'] }); setDeleteItem(null) },
  })

  const handleEdit = (row) => { setEditItem(row); setFormOpen(true) }
  const handleDelete = (row) => setDeleteItem(row)
  const handleSubmit = (data) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  if (isLoading) return <div className="text-gray-400 py-8 text-center">Loading...</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">Projects</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Project
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={projects} onEdit={handleEdit} onDelete={handleDelete} entityName="projects" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Project' : 'Create Project'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="project" item={deleteItem} onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
```

- [ ] **Step 2: Create AdminTasksTable**

```jsx
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getTasks, createTask, updateTask, deleteTask, getUsers, getProjects } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'projectName', label: 'Project' },
  { key: 'title', label: 'Title' },
  { key: 'status', label: 'Status' },
  { key: 'priority', label: 'Priority' },
  { key: 'dueDate', label: 'Due' },
]

const STATUS_OPTIONS = [
  { value: 'TODO', label: 'Todo' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'BLOCKED', label: 'Blocked' },
  { value: 'DONE', label: 'Done' },
  { value: 'SKIPPED', label: 'Skipped' },
]

const ENERGY_OPTIONS = [
  { value: '', label: 'None' },
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
]

export default function AdminTasksTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: tasks = [], isLoading } = useQuery({ queryKey: ['admin', 'tasks'], queryFn: getTasks })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })
  const { data: projects = [] } = useQuery({ queryKey: ['admin', 'projects'], queryFn: getProjects })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const projectOptions = projects.map(p => ({ value: p.id, label: `${p.name} (${p.userEmail})` }))

  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'projectId', label: 'Project', type: 'select', options: projectOptions, required: true },
    { name: 'title', label: 'Title', required: true },
    { name: 'description', label: 'Description', type: 'textarea' },
    { name: 'status', label: 'Status', type: 'select', options: STATUS_OPTIONS, defaultValue: 'TODO' },
    { name: 'priority', label: 'Priority', type: 'number', defaultValue: 3 },
    { name: 'pointsEstimate', label: 'Points Estimate', type: 'number' },
    { name: 'energyLevel', label: 'Energy Level', type: 'select', options: ENERGY_OPTIONS },
    { name: 'dueDate', label: 'Due Date', type: 'date' },
    { name: 'sortOrder', label: 'Sort Order', type: 'number', defaultValue: 0 },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createTask(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'tasks'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateTask(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'tasks'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteTask(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'tasks'] }); setDeleteItem(null) },
  })

  const handleEdit = (row) => { setEditItem(row); setFormOpen(true) }
  const handleDelete = (row) => setDeleteItem(row)
  const handleSubmit = (data) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  if (isLoading) return <div className="text-gray-400 py-8 text-center">Loading...</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">Tasks</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Task
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={tasks} onEdit={handleEdit} onDelete={handleDelete} entityName="tasks" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Task' : 'Create Task'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="task" item={deleteItem} onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
```

- [ ] **Step 3: Create AdminDeferredTable**

```jsx
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getDeferredItems, createDeferredItem, updateDeferredItem, deleteDeferredItem, getUsers } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'rawText', label: 'Text' },
  { key: 'isProcessed', label: 'Processed' },
  { key: 'deferralCount', label: 'Deferrals' },
  { key: 'deferredUntilDate', label: 'Deferred Until' },
  { key: 'capturedAt', label: 'Captured' },
]

export default function AdminDeferredTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: items = [], isLoading } = useQuery({ queryKey: ['admin', 'deferred-items'], queryFn: getDeferredItems })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'rawText', label: 'Text', type: 'textarea', required: true },
    { name: 'isProcessed', label: 'Processed', type: 'checkbox' },
    { name: 'deferredUntilDate', label: 'Deferred Until', type: 'date' },
    { name: 'deferralCount', label: 'Deferral Count', type: 'number', defaultValue: 0 },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createDeferredItem(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'deferred-items'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateDeferredItem(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'deferred-items'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteDeferredItem(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'deferred-items'] }); setDeleteItem(null) },
  })

  const handleEdit = (row) => { setEditItem(row); setFormOpen(true) }
  const handleDelete = (row) => setDeleteItem(row)
  const handleSubmit = (data) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  if (isLoading) return <div className="text-gray-400 py-8 text-center">Loading...</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">Deferred Items</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Item
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={items} onEdit={handleEdit} onDelete={handleDelete} entityName="deferred items" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Deferred Item' : 'Create Deferred Item'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="deferred item" item={deleteItem} onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
```

- [ ] **Step 4: Create AdminReflectionsTable**

```jsx
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getReflections, createReflection, updateReflection, deleteReflection, getUsers } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'reflectionDate', label: 'Date' },
  { key: 'energyRating', label: 'Energy' },
  { key: 'moodRating', label: 'Mood' },
  { key: 'reflectionNotes', label: 'Notes' },
  { key: 'isFinalized', label: 'Finalized' },
]

export default function AdminReflectionsTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: reflections = [], isLoading } = useQuery({ queryKey: ['admin', 'reflections'], queryFn: getReflections })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'reflectionDate', label: 'Date', type: 'date', required: true },
    { name: 'energyRating', label: 'Energy Rating (1-5)', type: 'number', required: true },
    { name: 'moodRating', label: 'Mood Rating (1-5)', type: 'number', required: true },
    { name: 'reflectionNotes', label: 'Notes', type: 'textarea' },
    { name: 'isFinalized', label: 'Finalized', type: 'checkbox' },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createReflection(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'reflections'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateReflection(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'reflections'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteReflection(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'reflections'] }); setDeleteItem(null) },
  })

  const handleEdit = (row) => { setEditItem(row); setFormOpen(true) }
  const handleDelete = (row) => setDeleteItem(row)
  const handleSubmit = (data) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  if (isLoading) return <div className="text-gray-400 py-8 text-center">Loading...</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">Reflections</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Reflection
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={reflections} onEdit={handleEdit} onDelete={handleDelete} entityName="reflections" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Reflection' : 'Create Reflection'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="reflection" item={deleteItem} onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
```

- [ ] **Step 5: Create AdminTimeBlocksTable**

```jsx
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getTimeBlocks, createTimeBlock, updateTimeBlock, deleteTimeBlock, getUsers, getTasks } from '@/api/admin'
import { AdminTable } from './components/AdminTable'
import { AdminFormModal } from './components/AdminFormModal'
import { DeleteConfirmDialog } from './components/DeleteConfirmDialog'

const COLUMNS = [
  { key: 'userEmail', label: 'User' },
  { key: 'blockDate', label: 'Date' },
  { key: 'taskTitle', label: 'Task' },
  { key: 'startTime', label: 'Start' },
  { key: 'endTime', label: 'End' },
  { key: 'wasCompleted', label: 'Completed' },
]

export default function AdminTimeBlocksTable() {
  const queryClient = useQueryClient()
  const [formOpen, setFormOpen] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [deleteItem, setDeleteItem] = useState(null)

  const { data: blocks = [], isLoading } = useQuery({ queryKey: ['admin', 'time-blocks'], queryFn: getTimeBlocks })
  const { data: users = [] } = useQuery({ queryKey: ['admin', 'users'], queryFn: getUsers })
  const { data: tasks = [] } = useQuery({ queryKey: ['admin', 'tasks'], queryFn: getTasks })

  const userOptions = users.map(u => ({ value: u.id, label: u.email }))
  const taskOptions = [{ value: '', label: 'None' }, ...tasks.map(t => ({ value: t.id, label: `${t.title} (${t.userEmail})` }))]

  const formFields = [
    { name: 'userId', label: 'User', type: 'select', options: userOptions, required: true },
    { name: 'blockDate', label: 'Date', type: 'date', required: true },
    { name: 'taskId', label: 'Task', type: 'select', options: taskOptions },
    { name: 'startTime', label: 'Start Time', type: 'time', required: true },
    { name: 'endTime', label: 'End Time', type: 'time', required: true },
    { name: 'sortOrder', label: 'Sort Order', type: 'number', defaultValue: 0 },
    { name: 'wasCompleted', label: 'Completed', type: 'checkbox' },
  ]

  const createMutation = useMutation({
    mutationFn: (data) => createTimeBlock(data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'time-blocks'] }); setFormOpen(false) },
  })
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateTimeBlock(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'time-blocks'] }); setFormOpen(false); setEditItem(null) },
  })
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteTimeBlock(id),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin', 'time-blocks'] }); setDeleteItem(null) },
  })

  const handleEdit = (row) => { setEditItem(row); setFormOpen(true) }
  const handleDelete = (row) => setDeleteItem(row)
  const handleSubmit = (data) => {
    if (editItem) {
      updateMutation.mutate({ id: editItem.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  if (isLoading) return <div className="text-gray-400 py-8 text-center">Loading...</div>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-xl font-bold text-gray-900">Time Blocks</h1>
        <button onClick={() => { setEditItem(null); setFormOpen(true) }}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          + Create Time Block
        </button>
      </div>
      <AdminTable columns={COLUMNS} data={blocks} onEdit={handleEdit} onDelete={handleDelete} entityName="time blocks" />
      <AdminFormModal open={formOpen} onOpenChange={(open) => { setFormOpen(open); if (!open) setEditItem(null) }}
        title={editItem ? 'Edit Time Block' : 'Create Time Block'} fields={formFields} initialValues={editItem}
        onSubmit={handleSubmit} isPending={createMutation.isPending || updateMutation.isPending} />
      <DeleteConfirmDialog open={!!deleteItem} onOpenChange={(open) => { if (!open) setDeleteItem(null) }}
        entityName="time block" item={deleteItem} onConfirm={() => deleteMutation.mutate(deleteItem.id)}
        isPending={deleteMutation.isPending} />
    </div>
  )
}
```

- [ ] **Step 6: Add all remaining routes to App.jsx**

Add imports:
```javascript
import AdminProjectsTable from '@/pages/admin/AdminProjectsTable'
import AdminTasksTable from '@/pages/admin/AdminTasksTable'
import AdminDeferredTable from '@/pages/admin/AdminDeferredTable'
import AdminReflectionsTable from '@/pages/admin/AdminReflectionsTable'
import AdminTimeBlocksTable from '@/pages/admin/AdminTimeBlocksTable'
```

Update the admin Route block:
```jsx
<Route path="/admin" element={<AdminPage />}>
  <Route index element={<Navigate to="/admin/users" replace />} />
  <Route path="users" element={<AdminUsersTable />} />
  <Route path="projects" element={<AdminProjectsTable />} />
  <Route path="tasks" element={<AdminTasksTable />} />
  <Route path="deferred" element={<AdminDeferredTable />} />
  <Route path="reflections" element={<AdminReflectionsTable />} />
  <Route path="time-blocks" element={<AdminTimeBlocksTable />} />
</Route>
```

- [ ] **Step 7: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 8: Commit**

```bash
git add frontend/src/pages/admin/ frontend/src/App.jsx
git commit -m "feat(admin): add all entity table pages with CRUD"
```

---

## Task 13: E2E Tests

**Files:**
- Create: `e2e/fixtures/admin-data.ts`
- Create: `e2e/tests/admin.spec.ts`

- [ ] **Step 1: Create admin mock data**

```typescript
export const ADMIN_USERS = [
  { id: 'u1', email: 'alice@example.com', displayName: 'Alice', timezone: 'UTC', createdAt: '2026-03-15T10:00:00Z', updatedAt: '2026-03-15T10:00:00Z' },
  { id: 'u2', email: 'bob@example.com', displayName: 'Bob', timezone: 'America/New_York', createdAt: '2026-03-20T10:00:00Z', updatedAt: '2026-03-20T10:00:00Z' },
]

export const ADMIN_PROJECTS = [
  { id: 'p1', userId: 'u1', userEmail: 'alice@example.com', name: 'Work', description: null, color: '#6b4c9a', icon: null, isActive: true, sortOrder: 0, archivedAt: null, createdAt: '2026-03-15T10:00:00Z', updatedAt: '2026-03-15T10:00:00Z' },
]

export const ADMIN_TASKS = [
  { id: 't1', userId: 'u1', userEmail: 'alice@example.com', projectId: 'p1', projectName: 'Work', title: 'Fix bug', description: null, parentTaskId: null, status: 'TODO', priority: 3, pointsEstimate: null, actualMinutes: null, energyLevel: null, dueDate: null, sortOrder: 0, blockedByTaskId: null, archivedAt: null, completedAt: null, createdAt: '2026-03-15T10:00:00Z', updatedAt: '2026-03-15T10:00:00Z' },
]

export const ADMIN_DEFERRED = []
export const ADMIN_REFLECTIONS = []
export const ADMIN_TIME_BLOCKS = []

export const USER_DEPENDENTS = { projects: 1, tasks: 1, deferredItems: 0, reflections: 0, timeBlocks: 0 }
```

- [ ] **Step 2: Create admin E2E test**

```typescript
import { test, expect } from '@playwright/test'
import { ADMIN_USERS, ADMIN_PROJECTS, ADMIN_TASKS, ADMIN_DEFERRED, ADMIN_REFLECTIONS, ADMIN_TIME_BLOCKS, USER_DEPENDENTS } from '../fixtures/admin-data'

async function mockAdminApi(page) {
  await page.route('**/api/v1/admin/users', route => {
    if (route.request().method() === 'GET') return route.fulfill({ json: ADMIN_USERS })
    if (route.request().method() === 'POST') return route.fulfill({ status: 201, json: { ...ADMIN_USERS[0], id: 'u-new' } })
    return route.continue()
  })
  await page.route('**/api/v1/admin/users/*/dependents', route =>
    route.fulfill({ json: USER_DEPENDENTS })
  )
  await page.route('**/api/v1/admin/users/*', route => {
    if (route.request().method() === 'DELETE') return route.fulfill({ status: 204 })
    if (route.request().method() === 'PUT') return route.fulfill({ json: ADMIN_USERS[0] })
    return route.continue()
  })
  await page.route('**/api/v1/admin/projects', route => route.fulfill({ json: ADMIN_PROJECTS }))
  await page.route('**/api/v1/admin/tasks', route => route.fulfill({ json: ADMIN_TASKS }))
  await page.route('**/api/v1/admin/deferred-items', route => route.fulfill({ json: ADMIN_DEFERRED }))
  await page.route('**/api/v1/admin/reflections', route => route.fulfill({ json: ADMIN_REFLECTIONS }))
  await page.route('**/api/v1/admin/time-blocks', route => route.fulfill({ json: ADMIN_TIME_BLOCKS }))
}

test.describe('Admin Page', () => {
  test.beforeEach(async ({ page }) => {
    await mockAdminApi(page)
  })

  test('loads admin page and shows users table by default', async ({ page }) => {
    await page.goto('/admin')
    await expect(page).toHaveURL(/\/admin\/users/)
    await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible()
    await expect(page.getByText('alice@example.com')).toBeVisible()
    await expect(page.getByText('bob@example.com')).toBeVisible()
  })

  test('sidebar navigation switches between entity tables', async ({ page }) => {
    await page.goto('/admin')
    await page.getByRole('link', { name: 'Projects' }).click()
    await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible()
    await expect(page.getByText('Work')).toBeVisible()
  })

  test('create user modal opens and submits', async ({ page }) => {
    await page.goto('/admin/users')
    await page.getByRole('button', { name: '+ Create User' }).click()
    await expect(page.getByText('Create User')).toBeVisible()
    await page.getByLabel('Email').fill('new@example.com')
    await page.getByLabel('Password').fill('password123')
    await page.getByLabel('Display Name').fill('New User')
    await page.getByRole('button', { name: 'Save' }).click()
  })

  test('delete user shows cascade confirmation', async ({ page }) => {
    await page.goto('/admin/users')
    const row = page.getByText('alice@example.com').locator('..')
    await row.getByRole('button', { name: 'Delete' }).click()
    await expect(page.getByText('Delete user?')).toBeVisible()
    await expect(page.getByText('1 project')).toBeVisible()
    await expect(page.getByText('1 task')).toBeVisible()
  })

  test('has back to app link', async ({ page }) => {
    await page.goto('/admin')
    await expect(page.getByText('Back to app')).toBeVisible()
  })
})
```

- [ ] **Step 3: Run E2E tests**

Run: `cd e2e && npx playwright test tests/admin.spec.ts`
Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add e2e/fixtures/admin-data.ts e2e/tests/admin.spec.ts
git commit -m "test(admin): add E2E tests for admin page"
```

---

## Task 14: Final Verification

- [ ] **Step 1: Run backend compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run backend tests**

Run: `cd backend && mvn test`
Expected: All tests pass

- [ ] **Step 3: Run frontend lint**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 4: Run E2E tests**

Run: `cd e2e && npx playwright test`
Expected: All tests pass (including new admin tests)

- [ ] **Step 5: Manual smoke test**

Start the full stack (`./start.sh`) and verify:
1. Navigate to `http://localhost:5173/admin`
2. Sidebar shows all 6 entity links
3. Users table loads with data
4. Can create, edit, and delete a user
5. Delete shows cascade confirmation with counts
6. Other entity tables load and show data
7. Main app at `/` still works normally
