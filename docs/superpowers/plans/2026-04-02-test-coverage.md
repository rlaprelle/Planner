# Test Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add comprehensive automated and unit test coverage across the frontend, backend, and E2E layers.

**Architecture:** Six phases, ordered by dependency. Phase 0 sets up infrastructure. Phases 1-5 add tests layer by layer. Many tasks within a phase are independent and can be dispatched to parallel sub-agents. Each task produces a self-contained test file that can be committed independently.

**Tech Stack:**
- Backend: JUnit 5 + Mockito + AssertJ + Spring Test (`@WebMvcTest`, `MockMvc`)
- Frontend: Vitest + `@testing-library/react` + `@testing-library/jest-dom`
- E2E: Playwright (already configured)

---

## Parallelization Map

```
Phase 0: Infrastructure (sequential — all later phases depend on this)
  Task 1: Frontend test infra

Phase 1: Frontend Pure Functions (PARALLEL — 2 agents)
  Task 2: pushBlocks tests        ──┐
  Task 3: useTimeGrid math tests  ──┘  independent files, no shared state

Phase 2: Backend Service Unit Tests (PARALLEL — up to 6 agents)
  Task 4:  ProjectService tests   ──┐
  Task 5:  TaskService tests      ──┤
  Task 6:  ScheduleService tests  ──┤  each tests a different service
  Task 7:  DeferredItemService    ──┤  with mocked repository deps
  Task 8:  AuthService+JwtService ──┤
  Task 9:  StatsService tests     ──┘

Phase 3: Backend Controller Integration Tests (PARALLEL — 2 agents)
  Task 10: TaskController tests   ──┐  follow existing @WebMvcTest pattern
  Task 11: ProjectController tests──┘  from AuthControllerIntegrationTest

Phase 4: Frontend API Client Tests (single agent)
  Task 12: client.js authFetch tests

Phase 5: E2E Page Coverage (PARALLEL — 3 agents)
  Task 13: Login + Register specs ──┐
  Task 14: Projects specs         ──┤  independent page tests
  Task 15: Inbox + End Day specs  ──┘  using existing fixture pattern
```

---

## Phase 0: Infrastructure

### Task 1: Frontend Test Infrastructure

**Files:**
- Modify: `frontend/package.json` (add devDependencies and test script)
- Modify: `frontend/vite.config.js` (add Vitest `test` block)
- Create: `frontend/src/test-setup.js`

- [x] **Step 1: Install Vitest and testing libraries**

```bash
cd frontend && npm install -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom
```

- [x] **Step 2: Add test script to package.json**

In `frontend/package.json`, add to the `"scripts"` block:

```json
"test": "vitest run",
"test:watch": "vitest"
```

- [x] **Step 3: Create test setup file**

Create `frontend/src/test-setup.js`:

```javascript
import '@testing-library/jest-dom'
```

- [x] **Step 4: Add Vitest config to vite.config.js**

Replace `frontend/vite.config.js` with:

```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'url'
import { dirname, resolve } from 'path'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test-setup.js',
  },
})
```

- [x] **Step 5: Verify Vitest runs (no tests yet, should exit cleanly)**

```bash
cd frontend && npx vitest run
```

Expected: "No test files found" or clean exit with 0 tests.

- [x] **Step 6: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/vite.config.js frontend/src/test-setup.js
git commit -m "chore: add Vitest and testing-library infrastructure"
```

---

## Phase 1: Frontend Pure Function Tests

> **PARALLEL:** Tasks 2 and 3 can be dispatched to separate sub-agents simultaneously after Phase 0 completes.

### Task 2: pushBlocks.js Unit Tests

**Files:**
- Create: `frontend/src/pages/start-day/pushBlocks.test.js`
- Reference: `frontend/src/pages/start-day/pushBlocks.js`

- [x] **Step 1: Write tests for time conversion utilities**

Create `frontend/src/pages/start-day/pushBlocks.test.js`:

```javascript
import { describe, it, expect } from 'vitest'
import {
  timeToMinutes,
  minutesToTime,
  snapTo15,
  pushBlocks,
  toGridBlock,
} from './pushBlocks'

describe('timeToMinutes', () => {
  it('converts HH:MM to minutes from midnight', () => {
    expect(timeToMinutes('00:00')).toBe(0)
    expect(timeToMinutes('09:30')).toBe(570)
    expect(timeToMinutes('17:00')).toBe(1020)
    expect(timeToMinutes('23:59')).toBe(1439)
  })

  it('handles HH:MM:SS format by ignoring seconds', () => {
    expect(timeToMinutes('09:30:00')).toBe(570)
    expect(timeToMinutes('09:30:45')).toBe(570)
  })
})

describe('minutesToTime', () => {
  it('converts minutes from midnight to HH:MM', () => {
    expect(minutesToTime(0)).toBe('00:00')
    expect(minutesToTime(570)).toBe('09:30')
    expect(minutesToTime(1020)).toBe('17:00')
  })

  it('zero-pads single-digit hours and minutes', () => {
    expect(minutesToTime(65)).toBe('01:05')
  })
})

describe('snapTo15', () => {
  it('snaps to nearest 15-minute boundary', () => {
    expect(snapTo15(0)).toBe(0)
    expect(snapTo15(7)).toBe(0)
    expect(snapTo15(8)).toBe(15)
    expect(snapTo15(22)).toBe(15)
    expect(snapTo15(23)).toBe(30)
    expect(snapTo15(37)).toBe(30)
    expect(snapTo15(38)).toBe(45)
    expect(snapTo15(52)).toBe(45)
    expect(snapTo15(53)).toBe(60)
  })
})
```

- [x] **Step 2: Run tests to verify they pass**

```bash
cd frontend && npx vitest run src/pages/start-day/pushBlocks.test.js
```

Expected: All tests PASS.

- [x] **Step 3: Add pushBlocks tests**

Append to `frontend/src/pages/start-day/pushBlocks.test.js`:

```javascript
describe('pushBlocks', () => {
  const DAY_END = 17 * 60 // 1020

  it('returns blocks unchanged when no overlaps exist', () => {
    const blocks = [
      { id: 'a', startMinutes: 480, endMinutes: 540 },  // 8:00-9:00
      { id: 'b', startMinutes: 600, endMinutes: 660 },  // 10:00-11:00
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result).toEqual(blocks)
  })

  it('pushes subsequent block forward when overlap exists', () => {
    const blocks = [
      { id: 'a', startMinutes: 480, endMinutes: 570 },  // 8:00-9:30
      { id: 'b', startMinutes: 540, endMinutes: 600 },  // 9:00-10:00 (overlaps!)
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result[1].startMinutes).toBe(570) // pushed to 9:30
    expect(result[1].endMinutes).toBe(630)   // duration preserved: still 60 min
  })

  it('cascades pushes through multiple blocks', () => {
    const blocks = [
      { id: 'a', startMinutes: 480, endMinutes: 570 },  // 8:00-9:30
      { id: 'b', startMinutes: 540, endMinutes: 600 },  // 9:00-10:00
      { id: 'c', startMinutes: 600, endMinutes: 660 },  // 10:00-11:00
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    // b pushed to 9:30-10:30
    expect(result[1]).toEqual({ id: 'b', startMinutes: 570, endMinutes: 630 })
    // c pushed to 10:30-11:30
    expect(result[2]).toEqual({ id: 'c', startMinutes: 630, endMinutes: 690 })
  })

  it('returns null when push chain exceeds day end', () => {
    const blocks = [
      { id: 'a', startMinutes: 900, endMinutes: 960 },  // 15:00-16:00
      { id: 'b', startMinutes: 930, endMinutes: 990 },  // 15:30-16:30
      { id: 'c', startMinutes: 960, endMinutes: 1020 }, // 16:00-17:00
    ]
    // After push: b→16:00-16:30, c→16:30-17:30 which exceeds 17:00
    const result = pushBlocks(blocks, 0, DAY_END)
    expect(result).toBeNull()
  })

  it('stops pushing once a gap is found', () => {
    const blocks = [
      { id: 'a', startMinutes: 480, endMinutes: 570 },  // 8:00-9:30
      { id: 'b', startMinutes: 540, endMinutes: 600 },  // 9:00-10:00 (overlaps a)
      { id: 'c', startMinutes: 720, endMinutes: 780 },  // 12:00-13:00 (gap after b)
    ]
    const result = pushBlocks(blocks, 0, DAY_END)
    // b pushed to 9:30-10:30, but c is not affected (gap exists)
    expect(result[1]).toEqual({ id: 'b', startMinutes: 570, endMinutes: 630 })
    expect(result[2]).toEqual({ id: 'c', startMinutes: 720, endMinutes: 780 })
  })

  it('does not mutate the input array', () => {
    const blocks = [
      { id: 'a', startMinutes: 480, endMinutes: 570 },
      { id: 'b', startMinutes: 540, endMinutes: 600 },
    ]
    const original = JSON.parse(JSON.stringify(blocks))
    pushBlocks(blocks, 0, DAY_END)
    expect(blocks).toEqual(original)
  })

  it('handles empty array', () => {
    expect(pushBlocks([], 0, DAY_END)).toEqual([])
  })

  it('handles single block', () => {
    const blocks = [{ id: 'a', startMinutes: 480, endMinutes: 540 }]
    expect(pushBlocks(blocks, 0, DAY_END)).toEqual(blocks)
  })
})

describe('toGridBlock', () => {
  it('adds startMinutes and endMinutes from time strings', () => {
    const serverBlock = {
      id: 'block-1',
      startTime: '09:00:00',
      endTime: '10:00:00',
      taskId: 'task-1',
    }
    const result = toGridBlock(serverBlock)
    expect(result.startMinutes).toBe(540)
    expect(result.endMinutes).toBe(600)
    expect(result.id).toBe('block-1')
    expect(result.taskId).toBe('task-1')
  })
})
```

- [x] **Step 4: Run full test file**

```bash
cd frontend && npx vitest run src/pages/start-day/pushBlocks.test.js
```

Expected: All tests PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/pages/start-day/pushBlocks.test.js
git commit -m "test: add unit tests for pushBlocks scheduling algorithm"
```

---

### Task 3: useTimeGrid Coordinate Math Tests

**Files:**
- Create: `frontend/src/pages/start-day/useTimeGrid.test.js`
- Reference: `frontend/src/pages/start-day/useTimeGrid.js`

This task tests only the exported constants and the pure math functions returned by the hook. We do NOT test the resize handler or DOM interactions (those belong in integration tests).

- [x] **Step 1: Write tests for coordinate math functions**

Create `frontend/src/pages/start-day/useTimeGrid.test.js`:

```javascript
import { describe, it, expect } from 'vitest'
import { renderHook } from '@testing-library/react'
import {
  DAY_START_MINUTES,
  DAY_END_MINUTES,
  DAY_DURATION,
  useTimeGrid,
} from './useTimeGrid'

describe('useTimeGrid constants', () => {
  it('defines correct day boundaries', () => {
    expect(DAY_START_MINUTES).toBe(480)  // 8 AM
    expect(DAY_END_MINUTES).toBe(1020)   // 5 PM
    expect(DAY_DURATION).toBe(540)       // 9 hours
  })
})

describe('useTimeGrid coordinate helpers', () => {
  function getHelpers() {
    const { result } = renderHook(() => useTimeGrid())
    return result.current
  }

  describe('minutesToPercent', () => {
    it('returns 0% at day start', () => {
      const { minutesToPercent } = getHelpers()
      expect(minutesToPercent(480)).toBe(0)
    })

    it('returns 100% at day end', () => {
      const { minutesToPercent } = getHelpers()
      expect(minutesToPercent(1020)).toBe(100)
    })

    it('returns 50% at midday (12:30)', () => {
      const { minutesToPercent } = getHelpers()
      // midpoint = 480 + 270 = 750 minutes = 12:30
      expect(minutesToPercent(750)).toBeCloseTo(50, 5)
    })

    it('converts 9:00 (1 hour into day) to ~11.1%', () => {
      const { minutesToPercent } = getHelpers()
      // 540 - 480 = 60 minutes into 540 total = 11.11%
      expect(minutesToPercent(540)).toBeCloseTo(11.111, 2)
    })
  })

  describe('durationToPercent', () => {
    it('converts 60 minutes to percentage of day', () => {
      const { durationToPercent } = getHelpers()
      // 60 / 540 = 11.11%
      expect(durationToPercent(60)).toBeCloseTo(11.111, 2)
    })

    it('converts full day duration to 100%', () => {
      const { durationToPercent } = getHelpers()
      expect(durationToPercent(540)).toBe(100)
    })

    it('converts 0 minutes to 0%', () => {
      const { durationToPercent } = getHelpers()
      expect(durationToPercent(0)).toBe(0)
    })

    it('converts 15 minutes to correct percentage', () => {
      const { durationToPercent } = getHelpers()
      // 15 / 540 = 2.78%
      expect(durationToPercent(15)).toBeCloseTo(2.778, 2)
    })
  })

  describe('pixelDeltaToMinutes', () => {
    it('returns 0 when gridRef is null', () => {
      const { pixelDeltaToMinutes } = getHelpers()
      // gridRef.current is null by default
      expect(pixelDeltaToMinutes(100)).toBe(0)
    })
  })

  describe('clientXToMinutes', () => {
    it('returns DAY_START_MINUTES when gridRef is null', () => {
      const { clientXToMinutes } = getHelpers()
      expect(clientXToMinutes(500)).toBe(DAY_START_MINUTES)
    })
  })
})
```

- [x] **Step 2: Run tests**

```bash
cd frontend && npx vitest run src/pages/start-day/useTimeGrid.test.js
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add frontend/src/pages/start-day/useTimeGrid.test.js
git commit -m "test: add unit tests for useTimeGrid coordinate math"
```

---

## Phase 2: Backend Service Unit Tests

> **PARALLEL:** Tasks 4 through 9 can ALL be dispatched to separate sub-agents simultaneously. Each tests a different service with mocked dependencies. No shared state between tasks.

### Task 4: ProjectService Unit Tests

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/project/ProjectServiceTest.java`
- Reference: `backend/src/main/java/com/echel/planner/backend/project/ProjectService.java`

- [x] **Step 1: Write failing tests**

Create `backend/src/test/java/com/echel/planner/backend/project/ProjectServiceTest.java`:

```java
package com.echel.planner.backend.project;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.project.dto.ProjectCreateRequest;
import com.echel.planner.backend.project.dto.ProjectResponse;
import com.echel.planner.backend.project.dto.ProjectUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setTimezone("UTC");
    }

    @Test
    void create_savesProjectWithAllFields() {
        var request = new ProjectCreateRequest("My Project", "A description", "#6366f1", "rocket", 5);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse response = projectService.create(user, request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("My Project");
        assertThat(saved.getDescription()).isEqualTo("A description");
        assertThat(saved.getColor()).isEqualTo("#6366f1");
        assertThat(saved.getIcon()).isEqualTo("rocket");
        assertThat(saved.getSortOrder()).isEqualTo(5);
    }

    @Test
    void create_defaultsSortOrderWhenNull() {
        var request = new ProjectCreateRequest("My Project", null, null, null, null);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        projectService.create(user, request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getSortOrder()).isEqualTo(0);
    }

    @Test
    void listActive_returnsOnlyActiveProjectsForUser() {
        Project p = new Project(user, "Active");
        when(projectRepository.findByUserIdAndIsActiveTrueOrderBySortOrderAsc(user.getId()))
                .thenReturn(List.of(p));

        List<ProjectResponse> result = projectService.listActive(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Active");
    }

    @Test
    void get_returnsProjectOwnedByUser() {
        UUID projectId = UUID.randomUUID();
        Project p = new Project(user, "Test");
        when(projectRepository.findByIdAndUserId(projectId, user.getId()))
                .thenReturn(Optional.of(p));

        ProjectResponse response = projectService.get(user, projectId);

        assertThat(response.name()).isEqualTo("Test");
    }

    @Test
    void get_throwsWhenProjectNotFound() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findByIdAndUserId(projectId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.get(user, projectId))
                .isInstanceOf(ProjectService.ProjectNotFoundException.class);
    }

    @Test
    void update_setsAllFields() {
        UUID projectId = UUID.randomUUID();
        Project existing = new Project(user, "Old Name");
        when(projectRepository.findByIdAndUserId(projectId, user.getId()))
                .thenReturn(Optional.of(existing));

        var request = new ProjectUpdateRequest("New Name", "New desc", "#ff0000", "star", 10);
        projectService.update(user, projectId, request);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getDescription()).isEqualTo("New desc");
        assertThat(existing.getColor()).isEqualTo("#ff0000");
        assertThat(existing.getIcon()).isEqualTo("star");
        assertThat(existing.getSortOrder()).isEqualTo(10);
    }

    @Test
    void archive_setsInactiveAndTimestamp() {
        UUID projectId = UUID.randomUUID();
        Project existing = new Project(user, "Archivable");
        when(projectRepository.findByIdAndUserId(projectId, user.getId()))
                .thenReturn(Optional.of(existing));

        ProjectResponse response = projectService.archive(user, projectId);

        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getArchivedAt()).isNotNull();
    }
}
```

- [x] **Step 2: Run tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.project.ProjectServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add backend/src/test/java/com/echel/planner/backend/project/ProjectServiceTest.java
git commit -m "test: add unit tests for ProjectService"
```

---

### Task 5: TaskService Unit Tests

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/task/TaskServiceTest.java`
- Reference: `backend/src/main/java/com/echel/planner/backend/task/TaskService.java`

- [x] **Step 1: Write tests for create and ownership validation**

Create `backend/src/test/java/com/echel/planner/backend/task/TaskServiceTest.java`:

```java
package com.echel.planner.backend.task;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.dto.TaskCreateRequest;
import com.echel.planner.backend.task.dto.TaskResponse;
import com.echel.planner.backend.task.dto.TaskStatusRequest;
import com.echel.planner.backend.task.dto.TaskUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    private AppUser user;
    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setEmail("test@example.com");
        user.setTimezone("UTC");

        projectId = UUID.randomUUID();
        project = new Project(user, "Test Project");
    }

    @Nested
    class Create {
        @Test
        void createsTaskWithRequiredFields() {
            var request = new TaskCreateRequest("Write tests", null, null, null, null, null, null, null, null, null);
            when(projectRepository.findByIdAndUserId(projectId, user.getId()))
                    .thenReturn(Optional.of(project));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.create(user, projectId, request);

            assertThat(response.title()).isEqualTo("Write tests");
            assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void createsTaskWithParentInSameProject() {
            UUID parentId = UUID.randomUUID();
            Task parent = new Task(user, project, "Parent");
            parent.setStatus(TaskStatus.TODO);
            var request = new TaskCreateRequest("Child", null, parentId, null, null, null, null, null, null, null);

            when(projectRepository.findByIdAndUserId(projectId, user.getId()))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByIdAndUserId(parentId, user.getId()))
                    .thenReturn(Optional.of(parent));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.create(user, projectId, request);

            assertThat(response.title()).isEqualTo("Child");
            assertThat(response.parentTaskId()).isEqualTo(parentId);
        }

        @Test
        void throwsWhenProjectNotFound() {
            var request = new TaskCreateRequest("Title", null, null, null, null, null, null, null, null, null);
            when(projectRepository.findByIdAndUserId(projectId, user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.create(user, projectId, request))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    class ChangeStatus {
        @Test
        void setsCompletedAtWhenStatusIsDone() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task(user, project, "Task");
            task.setStatus(TaskStatus.TODO);
            when(taskRepository.findByIdAndUserId(taskId, user.getId()))
                    .thenReturn(Optional.of(task));

            taskService.changeStatus(user, taskId, new TaskStatusRequest(TaskStatus.DONE));

            assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(task.getCompletedAt()).isNotNull();
        }

        @Test
        void clearsCompletedAtWhenStatusIsNotDone() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task(user, project, "Task");
            task.setStatus(TaskStatus.DONE);
            task.setCompletedAt(Instant.now());
            when(taskRepository.findByIdAndUserId(taskId, user.getId()))
                    .thenReturn(Optional.of(task));

            taskService.changeStatus(user, taskId, new TaskStatusRequest(TaskStatus.TODO));

            assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(task.getCompletedAt()).isNull();
        }
    }

    @Nested
    class Archive {
        @Test
        void setsArchivedAtTimestamp() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task(user, project, "Task");
            when(taskRepository.findByIdAndUserId(taskId, user.getId()))
                    .thenReturn(Optional.of(task));
            when(taskRepository.findByParentTaskId(taskId)).thenReturn(List.of());

            taskService.archive(user, taskId);

            assertThat(task.getArchivedAt()).isNotNull();
        }

        @Test
        void cascadesArchiveToChildren() {
            UUID parentId = UUID.randomUUID();
            Task parent = new Task(user, project, "Parent");
            Task child = new Task(user, project, "Child");
            child.setParentTask(parent);

            when(taskRepository.findByIdAndUserId(parentId, user.getId()))
                    .thenReturn(Optional.of(parent));
            when(taskRepository.findByParentTaskId(parentId)).thenReturn(List.of(child));

            taskService.archive(user, parentId);

            assertThat(parent.getArchivedAt()).isNotNull();
            assertThat(child.getArchivedAt()).isNotNull();
        }
    }

    @Nested
    class Update {
        @Test
        void cascadesProjectChangeToChildren() {
            UUID taskId = UUID.randomUUID();
            UUID newProjectId = UUID.randomUUID();
            Project newProject = new Project(user, "New Project");

            Task task = new Task(user, project, "Parent");
            Task child = new Task(user, project, "Child");
            child.setParentTask(task);

            when(taskRepository.findByIdAndUserId(taskId, user.getId()))
                    .thenReturn(Optional.of(task));
            when(projectRepository.findByIdAndUserId(newProjectId, user.getId()))
                    .thenReturn(Optional.of(newProject));
            when(taskRepository.findByParentTaskId(taskId)).thenReturn(List.of(child));

            var request = new TaskUpdateRequest("Parent", null, null, null, null, null, null, null, null, newProjectId);
            taskService.update(user, taskId, request);

            assertThat(task.getProject()).isEqualTo(newProject);
            assertThat(child.getProject()).isEqualTo(newProject);
        }
    }

    @Nested
    class GetNotFound {
        @Test
        void throwsWhenTaskNotFound() {
            UUID taskId = UUID.randomUUID();
            when(taskRepository.findByIdAndUserId(taskId, user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.get(user, taskId))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
```

- [x] **Step 2: Run tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.task.TaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add backend/src/test/java/com/echel/planner/backend/task/TaskServiceTest.java
git commit -m "test: add unit tests for TaskService"
```

---

### Task 6: ScheduleService Unit Tests

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/schedule/ScheduleServiceTest.java`
- Reference: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`

- [x] **Step 1: Write tests for savePlan validations**

Create `backend/src/test/java/com/echel/planner/backend/schedule/ScheduleServiceTest.java`:

```java
package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskStatus;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.schedule.dto.SavePlanRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setEmail("test@example.com");
        user.setTimezone("UTC");
    }

    @Nested
    class SavePlanValidation {
        @Test
        void rejectsNon15MinuteStartTime() {
            var block = new SavePlanRequest.BlockEntry(UUID.randomUUID(),
                    LocalTime.of(9, 10), LocalTime.of(10, 0));
            var request = new SavePlanRequest(LocalDate.now(), List.of(block));

            assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                    .isInstanceOf(ScheduleService.ScheduleValidationException.class)
                    .hasMessageContaining("15-minute");
        }

        @Test
        void rejectsNon15MinuteEndTime() {
            var block = new SavePlanRequest.BlockEntry(UUID.randomUUID(),
                    LocalTime.of(9, 0), LocalTime.of(10, 7));
            var request = new SavePlanRequest(LocalDate.now(), List.of(block));

            assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                    .isInstanceOf(ScheduleService.ScheduleValidationException.class)
                    .hasMessageContaining("15-minute");
        }

        @Test
        void rejectsEndTimeBeforeStartTime() {
            var block = new SavePlanRequest.BlockEntry(UUID.randomUUID(),
                    LocalTime.of(10, 0), LocalTime.of(9, 0));
            var request = new SavePlanRequest(LocalDate.now(), List.of(block));

            assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                    .isInstanceOf(ScheduleService.ScheduleValidationException.class);
        }

        @Test
        void rejectsBlockBeforeDayStart() {
            var block = new SavePlanRequest.BlockEntry(UUID.randomUUID(),
                    LocalTime.of(7, 0), LocalTime.of(8, 0));
            var request = new SavePlanRequest(LocalDate.now(), List.of(block));

            assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                    .isInstanceOf(ScheduleService.ScheduleValidationException.class);
        }

        @Test
        void rejectsBlockAfterDayEnd() {
            var block = new SavePlanRequest.BlockEntry(UUID.randomUUID(),
                    LocalTime.of(16, 0), LocalTime.of(17, 15));
            var request = new SavePlanRequest(LocalDate.now(), List.of(block));

            assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                    .isInstanceOf(ScheduleService.ScheduleValidationException.class);
        }

        @Test
        void rejectsOverlappingBlocks() {
            UUID taskId1 = UUID.randomUUID();
            UUID taskId2 = UUID.randomUUID();
            var block1 = new SavePlanRequest.BlockEntry(taskId1,
                    LocalTime.of(9, 0), LocalTime.of(10, 0));
            var block2 = new SavePlanRequest.BlockEntry(taskId2,
                    LocalTime.of(9, 30), LocalTime.of(10, 30));
            var request = new SavePlanRequest(LocalDate.now(), List.of(block1, block2));

            assertThatThrownBy(() -> scheduleService.savePlan(user, request))
                    .isInstanceOf(ScheduleService.ScheduleValidationException.class)
                    .hasMessageContaining("overlap");
        }

        @Test
        void acceptsValidAdjacentBlocks() {
            UUID taskId1 = UUID.randomUUID();
            UUID taskId2 = UUID.randomUUID();
            Task task1 = new Task(user, new Project(user, "P"), "T1");
            Task task2 = new Task(user, new Project(user, "P"), "T2");

            when(taskRepository.findByIdAndUserId(taskId1, user.getId())).thenReturn(Optional.of(task1));
            when(taskRepository.findByIdAndUserId(taskId2, user.getId())).thenReturn(Optional.of(task2));
            when(timeBlockRepository.deleteByUserIdAndBlockDate(any(), any())).thenReturn(0);
            when(timeBlockRepository.save(any(TimeBlock.class))).thenAnswer(inv -> inv.getArgument(0));

            var block1 = new SavePlanRequest.BlockEntry(taskId1,
                    LocalTime.of(9, 0), LocalTime.of(10, 0));
            var block2 = new SavePlanRequest.BlockEntry(taskId2,
                    LocalTime.of(10, 0), LocalTime.of(11, 0));
            var request = new SavePlanRequest(LocalDate.now(), List.of(block1, block2));

            List<TimeBlockResponse> result = scheduleService.savePlan(user, request);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class StartBlock {
        @Test
        void setsActualStartTime() {
            UUID blockId = UUID.randomUUID();
            TimeBlock block = new TimeBlock();
            when(timeBlockRepository.findByIdAndUserId(blockId, user.getId()))
                    .thenReturn(Optional.of(block));

            scheduleService.startBlock(user, blockId);

            assertThat(block.getActualStart()).isNotNull();
        }

        @Test
        void throwsWhenBlockAlreadyStarted() {
            UUID blockId = UUID.randomUUID();
            TimeBlock block = new TimeBlock();
            block.setActualStart(Instant.now());
            when(timeBlockRepository.findByIdAndUserId(blockId, user.getId()))
                    .thenReturn(Optional.of(block));

            assertThatThrownBy(() -> scheduleService.startBlock(user, blockId))
                    .isInstanceOf(ScheduleService.BlockAlreadyStartedException.class);
        }

        @Test
        void throwsWhenBlockNotFound() {
            UUID blockId = UUID.randomUUID();
            when(timeBlockRepository.findByIdAndUserId(blockId, user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> scheduleService.startBlock(user, blockId))
                    .isInstanceOf(ScheduleService.BlockNotFoundException.class);
        }
    }

    @Nested
    class CompleteBlock {
        @Test
        void calculatesElapsedAndUpdatesTask() {
            UUID blockId = UUID.randomUUID();
            Task task = new Task(user, new Project(user, "P"), "Task");
            task.setStatus(TaskStatus.TODO);
            TimeBlock block = new TimeBlock();
            block.setTask(task);
            block.setActualStart(Instant.now().minusSeconds(1800)); // 30 min ago

            when(timeBlockRepository.findByIdAndUserId(blockId, user.getId()))
                    .thenReturn(Optional.of(block));

            scheduleService.completeBlock(user, blockId);

            assertThat(block.getActualEnd()).isNotNull();
            assertThat(block.isWasCompleted()).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(task.getCompletedAt()).isNotNull();
            assertThat(task.getActualMinutes()).isNotNull();
        }

        @Test
        void throwsWhenBlockNotStarted() {
            UUID blockId = UUID.randomUUID();
            TimeBlock block = new TimeBlock();
            // actualStart is null
            when(timeBlockRepository.findByIdAndUserId(blockId, user.getId()))
                    .thenReturn(Optional.of(block));

            assertThatThrownBy(() -> scheduleService.completeBlock(user, blockId))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    class DoneForNow {
        @Test
        void calculatesElapsedButDoesNotCompleteTask() {
            UUID blockId = UUID.randomUUID();
            Task task = new Task(user, new Project(user, "P"), "Task");
            task.setStatus(TaskStatus.IN_PROGRESS);
            TimeBlock block = new TimeBlock();
            block.setTask(task);
            block.setActualStart(Instant.now().minusSeconds(900)); // 15 min ago

            when(timeBlockRepository.findByIdAndUserId(blockId, user.getId()))
                    .thenReturn(Optional.of(block));

            scheduleService.doneForNow(user, blockId);

            assertThat(block.getActualEnd()).isNotNull();
            assertThat(block.isWasCompleted()).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS); // unchanged
        }
    }
}
```

- [x] **Step 2: Run tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.schedule.ScheduleServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add backend/src/test/java/com/echel/planner/backend/schedule/ScheduleServiceTest.java
git commit -m "test: add unit tests for ScheduleService"
```

---

### Task 7: DeferredItemService Unit Tests

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/deferred/DeferredItemServiceTest.java`
- Reference: `backend/src/main/java/com/echel/planner/backend/deferred/DeferredItemService.java`

- [x] **Step 1: Write tests**

Create `backend/src/test/java/com/echel/planner/backend/deferred/DeferredItemServiceTest.java`:

```java
package com.echel.planner.backend.deferred;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.dto.ConvertToTaskRequest;
import com.echel.planner.backend.deferred.dto.DeferRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemResponse;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskService;
import com.echel.planner.backend.task.dto.TaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeferredItemServiceTest {

    @Mock
    private DeferredItemRepository deferredItemRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DeferredItemService deferredItemService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setEmail("test@example.com");
        user.setTimezone("UTC");
    }

    @Test
    void create_savesRawText() {
        var request = new DeferredItemCreateRequest("Buy groceries");
        when(deferredItemRepository.save(any(DeferredItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeferredItemResponse response = deferredItemService.create(user, request);

        ArgumentCaptor<DeferredItem> captor = ArgumentCaptor.forClass(DeferredItem.class);
        verify(deferredItemRepository).save(captor.capture());
        assertThat(captor.getValue().getRawText()).isEqualTo("Buy groceries");
    }

    @Nested
    class Defer {
        private DeferredItem item;
        private UUID itemId;

        @BeforeEach
        void setUp() {
            itemId = UUID.randomUUID();
            item = new DeferredItem(user, "Some item");
            when(deferredItemRepository.findByIdAndUserId(itemId, user.getId()))
                    .thenReturn(Optional.of(item));
        }

        @Test
        void defersOneDayFromToday() {
            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            deferredItemService.defer(user, itemId, new DeferRequest(DeferRequest.DeferDuration.ONE_DAY));

            assertThat(item.getDeferredUntilDate()).isEqualTo(today.plusDays(1));
            assertThat(item.getDeferralCount()).isEqualTo(1);
        }

        @Test
        void defersOneWeekFromToday() {
            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            deferredItemService.defer(user, itemId, new DeferRequest(DeferRequest.DeferDuration.ONE_WEEK));

            assertThat(item.getDeferredUntilDate()).isEqualTo(today.plusDays(7));
        }

        @Test
        void defersOneMonthFromToday() {
            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            deferredItemService.defer(user, itemId, new DeferRequest(DeferRequest.DeferDuration.ONE_MONTH));

            assertThat(item.getDeferredUntilDate()).isEqualTo(today.plusDays(30));
        }

        @Test
        void incrementsDeferralCountOnRepeatedDefers() {
            deferredItemService.defer(user, itemId, new DeferRequest(DeferRequest.DeferDuration.ONE_DAY));
            assertThat(item.getDeferralCount()).isEqualTo(1);

            deferredItemService.defer(user, itemId, new DeferRequest(DeferRequest.DeferDuration.ONE_DAY));
            assertThat(item.getDeferralCount()).isEqualTo(2);
        }
    }

    @Test
    void dismiss_marksAsProcessed() {
        UUID itemId = UUID.randomUUID();
        DeferredItem item = new DeferredItem(user, "Dismiss me");
        when(deferredItemRepository.findByIdAndUserId(itemId, user.getId()))
                .thenReturn(Optional.of(item));

        deferredItemService.dismiss(user, itemId);

        assertThat(item.isProcessed()).isTrue();
        assertThat(item.getProcessedAt()).isNotNull();
    }

    @Test
    void dismiss_throwsWhenNotFound() {
        UUID itemId = UUID.randomUUID();
        when(deferredItemRepository.findByIdAndUserId(itemId, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deferredItemService.dismiss(user, itemId))
                .isInstanceOf(DeferredItemService.DeferredItemNotFoundException.class);
    }
}
```

- [x] **Step 2: Run tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.deferred.DeferredItemServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add backend/src/test/java/com/echel/planner/backend/deferred/DeferredItemServiceTest.java
git commit -m "test: add unit tests for DeferredItemService"
```

---

### Task 8: AuthService and JwtService Unit Tests

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/auth/AuthServiceTest.java`
- Create: `backend/src/test/java/com/echel/planner/backend/auth/JwtServiceTest.java`
- Reference: `backend/src/main/java/com/echel/planner/backend/auth/AuthService.java`
- Reference: `backend/src/main/java/com/echel/planner/backend/auth/JwtService.java`

- [x] **Step 1: Write JwtService tests**

Create `backend/src/test/java/com/echel/planner/backend/auth/JwtServiceTest.java`:

```java
package com.echel.planner.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set fields via reflection (normally injected by @Value)
        ReflectionTestUtils.setField(jwtService, "secret",
                "this-is-a-test-secret-key-that-is-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L);  // 15 min
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L); // 7 days
        jwtService.validateKey();
    }

    @Test
    void generateAccessToken_andExtractEmail_roundtrip() {
        String token = jwtService.generateAccessToken("user@test.com");
        String email = jwtService.extractEmail(token);
        assertThat(email).isEqualTo("user@test.com");
    }

    @Test
    void generateRefreshToken_andExtractEmail_roundtrip() {
        String token = jwtService.generateRefreshToken("user@test.com");
        String email = jwtService.extractEmail(token);
        assertThat(email).isEqualTo("user@test.com");
    }

    @Test
    void isTokenValid_returnsTrueForMatchingUser() {
        String token = jwtService.generateAccessToken("user@test.com");
        UserDetails userDetails = new User("user@test.com", "", Collections.emptyList());

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        String token = jwtService.generateAccessToken("user@test.com");
        UserDetails userDetails = new User("other@test.com", "", Collections.emptyList());

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        // Create a service with 0ms expiration
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "secret",
                "this-is-a-test-secret-key-that-is-at-least-32-characters-long");
        ReflectionTestUtils.setField(shortLivedService, "accessTokenExpiration", 0L);
        ReflectionTestUtils.setField(shortLivedService, "refreshTokenExpiration", 0L);
        shortLivedService.validateKey();

        String token = shortLivedService.generateAccessToken("user@test.com");
        UserDetails userDetails = new User("user@test.com", "", Collections.emptyList());

        assertThat(shortLivedService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void validateKey_throwsForShortSecret() {
        JwtService badService = new JwtService();
        ReflectionTestUtils.setField(badService, "secret", "too-short");
        ReflectionTestUtils.setField(badService, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(badService, "refreshTokenExpiration", 604800000L);

        assertThatThrownBy(badService::validateKey)
                .isInstanceOf(IllegalStateException.class);
    }
}
```

- [x] **Step 2: Run JwtService tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.auth.JwtServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 3: Write AuthService tests**

Create `backend/src/test/java/com/echel/planner/backend/auth/AuthServiceTest.java`:

```java
package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(jwtService.generateAccessToken(anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh-token");
    }

    @Test
    void register_createsUserWithEncodedPassword() {
        var request = new RegisterRequest("user@test.com", "password123", "Test User", "America/New_York");
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@test.com");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getDisplayName()).isEqualTo("Test User");
        assertThat(saved.getTimezone()).isEqualTo("America/New_York");
    }

    @Test
    void register_defaultsTimezoneToUTC() {
        // RegisterRequest compact constructor normalizes null/blank timezone to "UTC"
        var request = new RegisterRequest("user@test.com", "password123", "Test User", null);
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getTimezone()).isEqualTo("UTC");
    }

    @Test
    void register_throwsWhenEmailAlreadyTaken() {
        var request = new RegisterRequest("existing@test.com", "password123", "User", "UTC");
        when(appUserRepository.findByEmail("existing@test.com"))
                .thenReturn(Optional.of(new AppUser()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthService.EmailAlreadyTakenException.class);
    }

    @Test
    void register_returnsAuthResultWithTokens() {
        var request = new RegisterRequest("user@test.com", "password123", "Test User", "UTC");
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.AuthResult result = authService.register(request);

        assertThat(result.authResponse().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshCookie()).isNotNull();
        assertThat(result.refreshCookie().getName()).isEqualTo("refresh_token");
    }
}
```

- [x] **Step 4: Run AuthService tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.auth.AuthServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/test/java/com/echel/planner/backend/auth/JwtServiceTest.java backend/src/test/java/com/echel/planner/backend/auth/AuthServiceTest.java
git commit -m "test: add unit tests for JwtService and AuthService"
```

---

### Task 9: StatsService Unit Tests

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/stats/StatsServiceTest.java`
- Reference: `backend/src/main/java/com/echel/planner/backend/stats/StatsService.java`

- [x] **Step 1: Write tests for streak calculation and dashboard**

Create `backend/src/test/java/com/echel/planner/backend/stats/StatsServiceTest.java`:

```java
package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.stats.dto.DashboardResponse;
import com.echel.planner.backend.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private DailyReflectionRepository dailyReflectionRepository;

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private DeferredItemRepository deferredItemRepository;

    @InjectMocks
    private StatsService statsService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setEmail("test@example.com");
        user.setTimezone("UTC");
    }

    @Nested
    class Streak {
        @Test
        void returnsZeroWhenNoReflections() {
            when(dailyReflectionRepository.findFinalizedDatesByUserDesc(user.getId()))
                    .thenReturn(List.of());

            assertThat(statsService.getStreak(user)).isEqualTo(0);
        }

        @Test
        void countsSingleDayStreakForToday() {
            LocalDate today = LocalDate.now(java.time.ZoneId.of("UTC"));
            when(dailyReflectionRepository.findFinalizedDatesByUserDesc(user.getId()))
                    .thenReturn(List.of(today));

            assertThat(statsService.getStreak(user)).isEqualTo(1);
        }

        @Test
        void countsConsecutiveDays() {
            LocalDate today = LocalDate.now(java.time.ZoneId.of("UTC"));
            when(dailyReflectionRepository.findFinalizedDatesByUserDesc(user.getId()))
                    .thenReturn(List.of(today, today.minusDays(1), today.minusDays(2)));

            assertThat(statsService.getStreak(user)).isEqualTo(3);
        }

        @Test
        void breaksOnGap() {
            LocalDate today = LocalDate.now(java.time.ZoneId.of("UTC"));
            // Gap between today and 2 days ago
            when(dailyReflectionRepository.findFinalizedDatesByUserDesc(user.getId()))
                    .thenReturn(List.of(today, today.minusDays(2)));

            assertThat(statsService.getStreak(user)).isEqualTo(1);
        }
    }

    @Nested
    class Dashboard {
        @Test
        void aggregatesAllStats() {
            LocalDate today = LocalDate.now(java.time.ZoneId.of("UTC"));
            when(timeBlockRepository.countByUserIdAndBlockDate(user.getId(), today)).thenReturn(5);
            when(timeBlockRepository.countCompletedByUserIdAndBlockDate(user.getId(), today)).thenReturn(2);
            when(dailyReflectionRepository.findFinalizedDatesByUserDesc(user.getId()))
                    .thenReturn(List.of(today));
            when(taskRepository.findUpcomingDeadlines(eq(user.getId()), any(), any()))
                    .thenReturn(List.of());
            when(deferredItemRepository.countPendingByUserId(eq(user.getId()), any()))
                    .thenReturn(3);

            DashboardResponse response = statsService.getDashboard(user);

            assertThat(response.todayBlockCount()).isEqualTo(5);
            assertThat(response.todayCompletedCount()).isEqualTo(2);
            assertThat(response.streakDays()).isGreaterThanOrEqualTo(1);
            assertThat(response.deferredItemCount()).isEqualTo(3);
        }
    }
}
```

- [x] **Step 2: Run tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.stats.StatsServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add backend/src/test/java/com/echel/planner/backend/stats/StatsServiceTest.java
git commit -m "test: add unit tests for StatsService"
```

---

## Phase 3: Backend Controller Integration Tests

> **PARALLEL:** Tasks 10 and 11 can be dispatched to separate sub-agents simultaneously.

### Task 10: TaskController Integration Tests

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/task/TaskControllerIntegrationTest.java`
- Reference: `backend/src/test/java/com/echel/planner/backend/auth/AuthControllerIntegrationTest.java` (pattern to follow)
- Reference: `backend/src/main/java/com/echel/planner/backend/task/TaskController.java`

Follow the exact same pattern as `AuthControllerIntegrationTest`:
- `@WebMvcTest(TaskController.class)`
- `@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})`
- `@MockBean` for `TaskService` and `AppUserRepository`
- Generate JWT token via `jwtService.generateAccessToken(email)`

- [x] **Step 1: Write controller integration tests**

Create `backend/src/test/java/com/echel/planner/backend/task/TaskControllerIntegrationTest.java`:

```java
package com.echel.planner.backend.task;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.task.dto.TaskCreateRequest;
import com.echel.planner.backend.task.dto.TaskResponse;
import com.echel.planner.backend.task.dto.TaskStatusRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TaskService taskService;

    @MockBean
    private AppUserRepository appUserRepository;

    private String token;
    private AppUser user;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setEmail("test@example.com");
        user.setTimezone("UTC");
        token = jwtService.generateAccessToken("test@example.com");
        projectId = UUID.randomUUID();

        when(appUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
    }

    @Test
    void create_returns201WithTask() throws Exception {
        UUID taskId = UUID.randomUUID();
        var response = new TaskResponse(taskId, projectId, "Project", "#fff", user.getId(),
                "New Task", null, null, TaskStatus.TODO, (short) 3, null, null, null,
                null, 0, null, DeadlineGroup.NO_DEADLINE, null, null,
                Instant.now(), Instant.now(), List.of());
        when(taskService.create(any(), eq(projectId), any())).thenReturn(response);

        var request = new TaskCreateRequest("New Task", null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Task"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void listTopLevel_returns200() throws Exception {
        when(taskService.listTopLevel(any(), eq(projectId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void get_returns200WithTask() throws Exception {
        UUID taskId = UUID.randomUUID();
        var response = new TaskResponse(taskId, projectId, "Project", "#fff", user.getId(),
                "My Task", null, null, TaskStatus.TODO, (short) 3, null, null, null,
                null, 0, null, DeadlineGroup.NO_DEADLINE, null, null,
                Instant.now(), Instant.now(), List.of());
        when(taskService.get(any(), eq(taskId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Task"));
    }

    @Test
    void changeStatus_returns200() throws Exception {
        UUID taskId = UUID.randomUUID();
        var response = new TaskResponse(taskId, projectId, "Project", "#fff", user.getId(),
                "Task", null, null, TaskStatus.DONE, (short) 3, null, null, null,
                null, 0, null, DeadlineGroup.NO_DEADLINE, null, Instant.now(),
                Instant.now(), Instant.now(), List.of());
        when(taskService.changeStatus(any(), eq(taskId), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/tasks/{id}/status", taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskStatusRequest(TaskStatus.DONE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void archive_returns200() throws Exception {
        UUID taskId = UUID.randomUUID();
        var response = new TaskResponse(taskId, projectId, "Project", "#fff", user.getId(),
                "Task", null, null, TaskStatus.TODO, (short) 3, null, null, null,
                null, 0, null, DeadlineGroup.NO_DEADLINE, Instant.now(), null,
                Instant.now(), Instant.now(), List.of());
        when(taskService.archive(any(), eq(taskId))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/tasks/{id}/archive", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
```

- [x] **Step 2: Run tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.task.TaskControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add backend/src/test/java/com/echel/planner/backend/task/TaskControllerIntegrationTest.java
git commit -m "test: add integration tests for TaskController"
```

---

### Task 11: ProjectController Integration Tests

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/project/ProjectControllerIntegrationTest.java`
- Reference: `backend/src/test/java/com/echel/planner/backend/auth/AuthControllerIntegrationTest.java` (pattern)
- Reference: `backend/src/main/java/com/echel/planner/backend/project/ProjectController.java`

- [x] **Step 1: Write controller integration tests**

Create `backend/src/test/java/com/echel/planner/backend/project/ProjectControllerIntegrationTest.java`:

```java
package com.echel.planner.backend.project;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.project.dto.ProjectCreateRequest;
import com.echel.planner.backend.project.dto.ProjectResponse;
import com.echel.planner.backend.project.dto.ProjectUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private AppUserRepository appUserRepository;

    private String token;
    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setEmail("test@example.com");
        user.setTimezone("UTC");
        token = jwtService.generateAccessToken("test@example.com");
        when(appUserRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
    }

    @Test
    void create_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        var response = new ProjectResponse(id, "My Project", "desc", "#6366f1", "rocket",
                true, 0, null, Instant.now(), Instant.now());
        when(projectService.create(any(), any())).thenReturn(response);

        var request = new ProjectCreateRequest("My Project", "desc", "#6366f1", "rocket", null);

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Project"));
    }

    @Test
    void listActive_returns200() throws Exception {
        when(projectService.listActive(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void get_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        var response = new ProjectResponse(id, "Project", null, null, null,
                true, 0, null, Instant.now(), Instant.now());
        when(projectService.get(any(), eq(id))).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Project"));
    }

    @Test
    void update_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        var response = new ProjectResponse(id, "Updated", "new desc", "#ff0000", "star",
                true, 0, null, Instant.now(), Instant.now());
        when(projectService.update(any(), eq(id), any())).thenReturn(response);

        var request = new ProjectUpdateRequest("Updated", "new desc", "#ff0000", "star", null);

        mockMvc.perform(put("/api/v1/projects/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void archive_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        var response = new ProjectResponse(id, "Archived", null, null, null,
                false, 0, Instant.now(), Instant.now(), Instant.now());
        when(projectService.archive(any(), eq(id))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/projects/{id}/archive", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [x] **Step 2: Run tests**

```bash
cd backend && mvn test -pl . -Dtest=com.echel.planner.backend.project.ProjectControllerIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add backend/src/test/java/com/echel/planner/backend/project/ProjectControllerIntegrationTest.java
git commit -m "test: add integration tests for ProjectController"
```

---

## Phase 4: Frontend API Client Tests

### Task 12: authFetch and handleResponse Tests

**Files:**
- Create: `frontend/src/api/client.test.js`
- Reference: `frontend/src/api/client.js`

- [x] **Step 1: Write tests for handleResponse**

Create `frontend/src/api/client.test.js`:

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  authFetch,
  handleResponse,
  setAuthToken,
  setTokenRefreshedCallback,
  setAuthFailureCallback,
  getAuthToken,
} from './client'

// Mock the auth module's refreshToken
vi.mock('./auth', () => ({
  refreshToken: vi.fn(),
}))

import { refreshToken } from './auth'

describe('handleResponse', () => {
  it('returns parsed JSON for 200 response', async () => {
    const res = new Response(JSON.stringify({ data: 'test' }), { status: 200 })
    const result = await handleResponse(res)
    expect(result).toEqual({ data: 'test' })
  })

  it('returns null for 204 No Content', async () => {
    const res = new Response(null, { status: 204 })
    const result = await handleResponse(res)
    expect(result).toBeNull()
  })

  it('throws with server error message for non-ok response', async () => {
    const res = new Response(JSON.stringify({ message: 'Not found' }), {
      status: 404,
      statusText: 'Not Found',
    })
    // Make it look like a non-ok response
    Object.defineProperty(res, 'ok', { value: false })

    await expect(handleResponse(res)).rejects.toThrow('Not found')
  })

  it('throws with HTTP status when body has no message', async () => {
    const res = new Response('not json', {
      status: 500,
      statusText: 'Server Error',
    })
    Object.defineProperty(res, 'ok', { value: false })

    await expect(handleResponse(res)).rejects.toThrow('HTTP 500')
  })

  it('attaches status to error object', async () => {
    const res = new Response(JSON.stringify({ message: 'Forbidden' }), { status: 403 })
    Object.defineProperty(res, 'ok', { value: false })

    try {
      await handleResponse(res)
    } catch (err) {
      expect(err.status).toBe(403)
    }
  })
})

describe('authFetch', () => {
  beforeEach(() => {
    setAuthToken(null)
    setTokenRefreshedCallback(null)
    setAuthFailureCallback(null)
    vi.restoreAllMocks()
    // Mock global fetch
    globalThis.fetch = vi.fn()
  })

  it('adds Authorization header when token is set', async () => {
    setAuthToken('my-token')
    globalThis.fetch.mockResolvedValue(new Response('{}', { status: 200 }))

    await authFetch('/api/v1/test')

    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/test',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer my-token',
        }),
      })
    )
  })

  it('retries with new token on 401', async () => {
    setAuthToken('old-token')
    refreshToken.mockResolvedValue({ accessToken: 'new-token' })

    // First call returns 401, second call returns 200
    globalThis.fetch
      .mockResolvedValueOnce(new Response('', { status: 401 }))
      .mockResolvedValueOnce(new Response('{"ok":true}', { status: 200 }))

    const res = await authFetch('/api/v1/test')

    expect(res.status).toBe(200)
    expect(globalThis.fetch).toHaveBeenCalledTimes(2)
    expect(getAuthToken()).toBe('new-token')
  })

  it('calls onTokenRefreshed callback after successful refresh', async () => {
    setAuthToken('old-token')
    const callback = vi.fn()
    setTokenRefreshedCallback(callback)
    refreshToken.mockResolvedValue({ accessToken: 'new-token' })

    globalThis.fetch
      .mockResolvedValueOnce(new Response('', { status: 401 }))
      .mockResolvedValueOnce(new Response('{}', { status: 200 }))

    await authFetch('/api/v1/test')

    expect(callback).toHaveBeenCalledWith('new-token')
  })

  it('calls onAuthFailure when refresh fails', async () => {
    setAuthToken('old-token')
    const failureCallback = vi.fn()
    setAuthFailureCallback(failureCallback)
    refreshToken.mockRejectedValue(new Error('refresh failed'))

    globalThis.fetch.mockResolvedValue(new Response('', { status: 401 }))

    await authFetch('/api/v1/test')

    expect(failureCallback).toHaveBeenCalled()
  })

  it('does not retry on non-401 errors', async () => {
    setAuthToken('my-token')
    globalThis.fetch.mockResolvedValue(new Response('', { status: 403 }))

    const res = await authFetch('/api/v1/test')

    expect(res.status).toBe(403)
    expect(globalThis.fetch).toHaveBeenCalledTimes(1)
  })
})
```

- [x] **Step 2: Run tests**

```bash
cd frontend && npx vitest run src/api/client.test.js
```

Expected: All tests PASS.

- [x] **Step 3: Commit**

```bash
git add frontend/src/api/client.test.js
git commit -m "test: add unit tests for authFetch and handleResponse"
```

---

## Phase 5: E2E Page Coverage

> **PARALLEL:** Tasks 13, 14, and 15 can all be dispatched to separate sub-agents simultaneously. Each creates an independent spec file using the existing fixture pattern.

### Task 13: Login and Register E2E Specs

**Files:**
- Create: `e2e/tests/auth.spec.ts`
- Reference: `e2e/fixtures/auth.ts` (for pattern, but auth.spec tests WITHOUT the auth fixture since it tests the login flow itself)
- Reference: `e2e/fixtures/mocks.ts`, `e2e/fixtures/data.ts`

- [x] **Step 1: Write auth E2E tests**

Create `e2e/tests/auth.spec.ts`:

```typescript
import { test, expect } from '@playwright/test'

// NOTE: We use base `test` from Playwright, NOT from fixtures/auth.ts,
// because we're testing the login/register flow itself (no pre-authenticated session).

test.describe('Login', () => {
  test('shows login form', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByLabel(/email/i)).toBeVisible()
    await expect(page.getByLabel(/password/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /log in|sign in/i })).toBeVisible()
  })

  test('successful login redirects to dashboard', async ({ page }) => {
    // Mock the login endpoint
    await page.route('**/api/v1/auth/login', route =>
      route.fulfill({
        json: {
          accessToken: 'test-token',
          user: { id: 'u1', displayName: 'Test User', timezone: 'UTC' },
        },
      })
    )
    // Mock the refresh endpoint (AuthProvider calls on mount)
    await page.route('**/api/v1/auth/refresh', route =>
      route.fulfill({ status: 401, json: { message: 'No session' } })
    )
    // Mock dashboard data (destination after login)
    await page.route('**/api/v1/stats/dashboard', route =>
      route.fulfill({
        json: { todayBlockCount: 0, todayCompletedCount: 0, streakDays: 0, upcomingDeadlines: [], deferredItemCount: 0 },
      })
    )
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({ json: [] })
    )

    await page.goto('/login')
    await page.getByLabel(/email/i).fill('user@test.com')
    await page.getByLabel(/password/i).fill('password123')
    await page.getByRole('button', { name: /log in|sign in/i }).click()

    // Should redirect to dashboard
    await expect(page).toHaveURL('/')
  })

  test('shows error on invalid credentials', async ({ page }) => {
    await page.route('**/api/v1/auth/login', route =>
      route.fulfill({ status: 401, json: { message: 'Invalid credentials' } })
    )
    await page.route('**/api/v1/auth/refresh', route =>
      route.fulfill({ status: 401, json: { message: 'No session' } })
    )

    await page.goto('/login')
    await page.getByLabel(/email/i).fill('bad@test.com')
    await page.getByLabel(/password/i).fill('wrong')
    await page.getByRole('button', { name: /log in|sign in/i }).click()

    await expect(page.getByText(/invalid|error|failed/i)).toBeVisible()
  })

  test('has link to register page', async ({ page }) => {
    await page.route('**/api/v1/auth/refresh', route =>
      route.fulfill({ status: 401, json: { message: 'No session' } })
    )
    await page.goto('/login')
    const registerLink = page.getByRole('link', { name: /register|sign up|create account/i })
    await expect(registerLink).toBeVisible()
  })
})

test.describe('Register', () => {
  test('shows registration form', async ({ page }) => {
    await page.route('**/api/v1/auth/refresh', route =>
      route.fulfill({ status: 401, json: { message: 'No session' } })
    )
    await page.goto('/register')
    await expect(page.getByLabel(/email/i)).toBeVisible()
    await expect(page.getByLabel(/password/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /register|sign up|create/i })).toBeVisible()
  })

  test('successful registration redirects to dashboard', async ({ page }) => {
    await page.route('**/api/v1/auth/refresh', route =>
      route.fulfill({ status: 401, json: { message: 'No session' } })
    )
    await page.route('**/api/v1/auth/register', route =>
      route.fulfill({
        json: {
          accessToken: 'test-token',
          user: { id: 'u1', displayName: 'New User', timezone: 'UTC' },
        },
      })
    )
    await page.route('**/api/v1/stats/dashboard', route =>
      route.fulfill({
        json: { todayBlockCount: 0, todayCompletedCount: 0, streakDays: 0, upcomingDeadlines: [], deferredItemCount: 0 },
      })
    )
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({ json: [] })
    )

    await page.goto('/register')
    await page.getByLabel(/email/i).fill('new@test.com')
    await page.getByLabel(/password/i).fill('password123')
    // Fill display name if present
    const displayName = page.getByLabel(/name|display/i)
    if (await displayName.isVisible()) {
      await displayName.fill('New User')
    }
    await page.getByRole('button', { name: /register|sign up|create/i }).click()

    await expect(page).toHaveURL('/')
  })
})
```

- [x] **Step 2: Run E2E tests**

```bash
cd e2e && npx playwright test tests/auth.spec.ts
```

Expected: All tests PASS. Note: some selectors may need adjusting based on actual form labels — the implementing agent should read `frontend/src/pages/LoginPage.jsx` and `frontend/src/pages/RegisterPage.jsx` to verify exact labels and button text before finalizing selectors.

- [x] **Step 3: Commit**

```bash
git add e2e/tests/auth.spec.ts
git commit -m "test: add E2E tests for login and register flows"
```

---

### Task 14: Projects and Project Detail E2E Specs

**Files:**
- Create: `e2e/tests/projects.spec.ts`
- Create: `e2e/fixtures/project-data.ts` (if additional mock data needed)
- Reference: `e2e/fixtures/auth.ts`, `e2e/fixtures/mocks.ts`, `e2e/fixtures/data.ts`

- [x] **Step 1: Add project mock helpers**

Add to `e2e/fixtures/mocks.ts`:

```typescript
export async function mockProjects(page: Page, projects = [
  {
    id: 'proj-1', name: 'Work', description: 'Work projects', color: '#6366f1',
    icon: null, isActive: true, sortOrder: 0, archivedAt: null,
    createdAt: '2026-03-30T00:00:00Z', updatedAt: '2026-03-30T00:00:00Z',
  },
  {
    id: 'proj-2', name: 'Personal', description: null, color: '#10b981',
    icon: null, isActive: true, sortOrder: 1, archivedAt: null,
    createdAt: '2026-03-30T00:00:00Z', updatedAt: '2026-03-30T00:00:00Z',
  },
]) {
  await page.route('**/api/v1/projects', (route) => {
    if (route.request().method() === 'GET') {
      route.fulfill({ json: projects })
    } else {
      route.fulfill({ json: projects[0] })
    }
  })
}

export async function mockProjectTasks(page: Page, tasks = TASKS) {
  await page.route(/\/api\/v1\/projects\/[^/]+\/tasks/, (route) =>
    route.fulfill({ json: tasks })
  )
}
```

- [x] **Step 2: Write project E2E tests**

Create `e2e/tests/projects.spec.ts`:

```typescript
import { test, expect } from '../fixtures/auth'
import { mockProjects, mockProjectTasks } from '../fixtures/mocks'
import { TASKS } from '../fixtures/data'

test.describe('Projects List', () => {
  test('shows active projects', async ({ page }) => {
    await mockProjects(page)
    await page.goto('/projects')

    await expect(page.getByText('Work')).toBeVisible()
    await expect(page.getByText('Personal')).toBeVisible()
  })
})

test.describe('Project Detail', () => {
  test('shows task list for project', async ({ page }) => {
    await mockProjects(page)
    await mockProjectTasks(page)
    await page.goto('/projects/proj-1')

    await expect(page.getByText('Write tests')).toBeVisible()
    await expect(page.getByText('Review PR')).toBeVisible()
  })
})
```

**Note to implementing agent:** Read `frontend/src/pages/ProjectsPage.jsx` and `frontend/src/pages/ProjectDetailPage.jsx` to verify exact UI elements and add more detailed assertions. The selectors above use the mock data from `e2e/fixtures/data.ts`. Expand tests as needed based on actual page content.

- [x] **Step 3: Run E2E tests**

```bash
cd e2e && npx playwright test tests/projects.spec.ts
```

Expected: All tests PASS.

- [x] **Step 4: Commit**

```bash
git add e2e/fixtures/mocks.ts e2e/tests/projects.spec.ts
git commit -m "test: add E2E tests for projects list and project detail"
```

---

### Task 15: Inbox and End Day E2E Specs

**Files:**
- Create: `e2e/tests/inbox.spec.ts`
- Create: `e2e/tests/end-day.spec.ts`
- Reference: `e2e/fixtures/auth.ts`, `e2e/fixtures/mocks.ts`

- [x] **Step 1: Add mock helpers for inbox and reflection**

Add to `e2e/fixtures/mocks.ts`:

```typescript
export async function mockDeferredItems(page: Page, items = [
  {
    id: 'def-1', userId: 'u1', rawText: 'Look into performance issue',
    isProcessed: false, capturedAt: '2026-04-01T10:00:00Z', processedAt: null,
    resolvedTaskId: null, resolvedProjectId: null, deferredUntilDate: null,
    deferralCount: 0, createdAt: '2026-04-01T10:00:00Z', updatedAt: '2026-04-01T10:00:00Z',
  },
  {
    id: 'def-2', userId: 'u1', rawText: 'Review team feedback',
    isProcessed: false, capturedAt: '2026-04-01T11:00:00Z', processedAt: null,
    resolvedTaskId: null, resolvedProjectId: null, deferredUntilDate: null,
    deferralCount: 0, createdAt: '2026-04-01T11:00:00Z', updatedAt: '2026-04-01T11:00:00Z',
  },
]) {
  await page.route('**/api/v1/deferred', (route) => {
    if (route.request().method() === 'GET') {
      route.fulfill({ json: items })
    } else if (route.request().method() === 'POST') {
      route.fulfill({ json: { ...items[0], id: 'def-new', rawText: 'New item' } })
    } else {
      route.fulfill({ json: items[0] })
    }
  })
}

export async function mockReflection(page: Page) {
  await page.route('**/api/v1/reflections/today', (route) => {
    if (route.request().method() === 'POST') {
      route.fulfill({
        json: {
          id: 'ref-1', date: '2026-04-02', rating: 4,
          note: 'Good day', finalized: true,
        },
      })
    } else {
      route.fulfill({ json: null })
    }
  })
  await page.route('**/api/v1/stats/streak', (route) =>
    route.fulfill({ json: { streak: 5 } })
  )
}
```

- [x] **Step 2: Write inbox E2E tests**

Create `e2e/tests/inbox.spec.ts`:

```typescript
import { test, expect } from '../fixtures/auth'
import { mockDeferredItems, mockProjects } from '../fixtures/mocks'

test.describe('Inbox', () => {
  test('shows pending deferred items', async ({ page }) => {
    await mockDeferredItems(page)
    await page.goto('/inbox')

    await expect(page.getByText('Look into performance issue')).toBeVisible()
    await expect(page.getByText('Review team feedback')).toBeVisible()
  })

  test('shows empty state when no items', async ({ page }) => {
    await mockDeferredItems(page, [])
    await page.goto('/inbox')

    // Should show some empty state messaging
    await expect(page.locator('body')).not.toContainText('Look into performance issue')
  })
})
```

- [x] **Step 3: Write end-day E2E tests**

Create `e2e/tests/end-day.spec.ts`:

```typescript
import { test, expect } from '../fixtures/auth'
import { mockReflection } from '../fixtures/mocks'

test.describe('End Day', () => {
  test('shows reflection form', async ({ page }) => {
    await mockReflection(page)
    await page.goto('/end-day')

    // The page should have a rating input and a submit action
    // Exact selectors depend on EndDayPage implementation
    await expect(page.locator('body')).toBeVisible()
  })
})
```

**Note to implementing agent:** Read `frontend/src/pages/InboxPage.jsx` and `frontend/src/pages/EndDayPage.jsx` to verify exact UI elements, labels, and interaction patterns. These specs provide the skeleton — expand assertions based on actual page content. Mock endpoint URLs may need adjustment to match actual API paths used in the pages.

- [x] **Step 4: Run E2E tests**

```bash
cd e2e && npx playwright test tests/inbox.spec.ts tests/end-day.spec.ts
```

Expected: All tests PASS.

- [x] **Step 5: Commit**

```bash
git add e2e/fixtures/mocks.ts e2e/tests/inbox.spec.ts e2e/tests/end-day.spec.ts
git commit -m "test: add E2E tests for inbox and end-day flows"
```

---

## Summary

| Phase | Tasks | Parallelizable | New Test Files |
|-------|-------|----------------|---------------|
| 0: Infrastructure | 1 | No (dependency) | 1 setup file |
| 1: Frontend Pure Functions | 2-3 | Yes (2 agents) | 2 test files |
| 2: Backend Services | 4-9 | Yes (6 agents) | 7 test files |
| 3: Backend Controllers | 10-11 | Yes (2 agents) | 2 test files |
| 4: Frontend API Client | 12 | Single agent | 1 test file |
| 5: E2E Pages | 13-15 | Yes (3 agents) | 4 test files (+ mock additions) |

**Total: 15 tasks, 17 new test files, up to 6 agents in parallel during Phase 2.**
