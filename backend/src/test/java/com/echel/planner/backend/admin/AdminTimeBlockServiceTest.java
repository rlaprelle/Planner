package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTimeBlockRequest;
import com.echel.planner.backend.admin.dto.AdminTimeBlockResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.schedule.TimeBlock;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTimeBlockServiceTest {

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private AdminTimeBlockService service;

    private AppUser user;
    private UUID userId;
    private UUID blockId;

    private static final LocalDate BLOCK_DATE = LocalDate.of(2026, 4, 1);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(10, 0);

    @BeforeEach
    void setUp() {
        user = new AppUser("admin@example.com", "hash", "Admin User", "UTC");
        userId = UUID.randomUUID();
        blockId = UUID.randomUUID();
    }

    // --- listAll ---

    @Test
    void listAll_returnsMappedResponses() {
        TimeBlock block1 = new TimeBlock(user, BLOCK_DATE, (Task) null, START_TIME, END_TIME, 0);
        TimeBlock block2 = new TimeBlock(user, BLOCK_DATE.plusDays(1), (Task) null,
                LocalTime.of(14, 0), LocalTime.of(15, 0), 1);
        when(timeBlockRepository.findAll()).thenReturn(List.of(block1, block2));

        List<AdminTimeBlockResponse> responses = service.listAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).blockDate()).isEqualTo(BLOCK_DATE);
        assertThat(responses.get(1).blockDate()).isEqualTo(BLOCK_DATE.plusDays(1));
    }

    // --- get ---

    @Test
    void get_throwsAdminNotFoundExceptionWhenBlockAbsent() {
        when(timeBlockRepository.findById(blockId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(blockId))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(blockId.toString());
    }

    // --- create ---

    @Test
    void create_looksUpUserAndOptionalTaskAndCreatesBlockWithAllFields() {
        UUID taskId = UUID.randomUUID();
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(taskId);
        when(task.getTitle()).thenReturn("Write tests");

        AdminTimeBlockRequest request = new AdminTimeBlockRequest(
                userId, BLOCK_DATE, taskId, START_TIME, END_TIME, 2, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TimeBlock saved = new TimeBlock(user, BLOCK_DATE, task, START_TIME, END_TIME, 2);
        saved.setWasCompleted(true);
        when(timeBlockRepository.save(any(TimeBlock.class))).thenReturn(saved);

        AdminTimeBlockResponse response = service.create(request);

        ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
        verify(timeBlockRepository).save(captor.capture());
        TimeBlock captured = captor.getValue();
        assertThat(captured.getUser()).isSameAs(user);
        assertThat(captured.getTask()).isSameAs(task);
        assertThat(captured.getBlockDate()).isEqualTo(BLOCK_DATE);
        assertThat(captured.getStartTime()).isEqualTo(START_TIME);
        assertThat(captured.getEndTime()).isEqualTo(END_TIME);
        assertThat(captured.getSortOrder()).isEqualTo(2);
        assertThat(captured.isWasCompleted()).isTrue();
        assertThat(response.taskId()).isEqualTo(taskId);
    }

    @Test
    void create_withNullTaskId_createsBlockWithoutTask() {
        AdminTimeBlockRequest request = new AdminTimeBlockRequest(
                userId, BLOCK_DATE, null, START_TIME, END_TIME, 0, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        TimeBlock saved = new TimeBlock(user, BLOCK_DATE, (Task) null, START_TIME, END_TIME, 0);
        when(timeBlockRepository.save(any(TimeBlock.class))).thenReturn(saved);

        service.create(request);

        ArgumentCaptor<TimeBlock> captor = ArgumentCaptor.forClass(TimeBlock.class);
        verify(timeBlockRepository).save(captor.capture());
        assertThat(captor.getValue().getTask()).isNull();
        verifyNoInteractions(taskRepository);
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenUserAbsent() {
        AdminTimeBlockRequest request = new AdminTimeBlockRequest(
                userId, BLOCK_DATE, null, START_TIME, END_TIME, 0, null);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenTaskAbsent() {
        UUID taskId = UUID.randomUUID();
        AdminTimeBlockRequest request = new AdminTimeBlockRequest(
                userId, BLOCK_DATE, taskId, START_TIME, END_TIME, 0, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(taskId.toString());
    }

    // --- update ---

    @Test
    void update_setsFieldsAndLooksUpNewTaskWhenTaskIdProvided() {
        UUID taskId = UUID.randomUUID();
        Task newTask = mock(Task.class);
        TimeBlock existing = new TimeBlock(user, BLOCK_DATE, (Task) null, START_TIME, END_TIME, 0);
        when(timeBlockRepository.findById(blockId)).thenReturn(Optional.of(existing));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(newTask));

        LocalDate newDate = BLOCK_DATE.plusDays(1);
        LocalTime newStart = LocalTime.of(11, 0);
        LocalTime newEnd = LocalTime.of(12, 0);
        AdminTimeBlockRequest request = new AdminTimeBlockRequest(
                userId, newDate, taskId, newStart, newEnd, 5, true);

        service.update(blockId, request);

        assertThat(existing.getBlockDate()).isEqualTo(newDate);
        assertThat(existing.getStartTime()).isEqualTo(newStart);
        assertThat(existing.getEndTime()).isEqualTo(newEnd);
        assertThat(existing.getTask()).isSameAs(newTask);
        assertThat(existing.getSortOrder()).isEqualTo(5);
        assertThat(existing.isWasCompleted()).isTrue();
    }

    @Test
    void update_clearsTaskWhenTaskIdIsNull() {
        Task existingTask = mock(Task.class);
        TimeBlock existing = new TimeBlock(user, BLOCK_DATE, existingTask, START_TIME, END_TIME, 0);
        when(timeBlockRepository.findById(blockId)).thenReturn(Optional.of(existing));

        AdminTimeBlockRequest request = new AdminTimeBlockRequest(
                userId, BLOCK_DATE, null, START_TIME, END_TIME, 0, null);

        service.update(blockId, request);

        assertThat(existing.getTask()).isNull();
        verifyNoInteractions(taskRepository);
    }

    @Test
    void update_throwsAdminNotFoundExceptionWhenBlockAbsent() {
        when(timeBlockRepository.findById(blockId)).thenReturn(Optional.empty());
        AdminTimeBlockRequest request = new AdminTimeBlockRequest(
                userId, BLOCK_DATE, null, START_TIME, END_TIME, 0, null);

        assertThatThrownBy(() -> service.update(blockId, request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(blockId.toString());
    }

    // --- delete ---

    @Test
    void delete_removesBlockById() {
        TimeBlock existing = new TimeBlock(user, BLOCK_DATE, (Task) null, START_TIME, END_TIME, 0);
        when(timeBlockRepository.findById(blockId)).thenReturn(Optional.of(existing));

        service.delete(blockId);

        verify(timeBlockRepository).deleteById(blockId);
    }

    @Test
    void delete_throwsAdminNotFoundExceptionWhenBlockAbsent() {
        when(timeBlockRepository.findById(blockId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(blockId))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(blockId.toString());
    }
}
