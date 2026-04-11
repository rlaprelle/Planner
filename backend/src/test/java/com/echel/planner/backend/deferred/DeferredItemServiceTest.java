package com.echel.planner.backend.deferred;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.DeferredItemService.DeferredItemNotFoundException;
import com.echel.planner.backend.deferred.dto.DeferRequest;
import com.echel.planner.backend.deferred.dto.DeferRequest.DeferDuration;
import com.echel.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemResponse;
import com.echel.planner.backend.event.EventRepository;
import com.echel.planner.backend.event.EventService;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeferredItemServiceTest {

    @Mock
    private DeferredItemRepository repository;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private DeferredItemService service;

    private AppUser user;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        user = new AppUser("test@example.com", "hash", "Test User", "UTC");
        itemId = UUID.randomUUID();
    }

    // --- create ---

    @Test
    void create_savesItemWithRawText() {
        DeferredItemCreateRequest request = new DeferredItemCreateRequest("Buy milk");

        ArgumentCaptor<DeferredItem> captor = ArgumentCaptor.forClass(DeferredItem.class);
        DeferredItem saved = new DeferredItem(user, "Buy milk");
        when(repository.save(any(DeferredItem.class))).thenReturn(saved);

        DeferredItemResponse response = service.create(user, request);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRawText()).isEqualTo("Buy milk");
        assertThat(response.rawText()).isEqualTo("Buy milk");
    }

    // --- defer ONE_DAY ---

    @Test
    void defer_oneDaySetsCorrectDate() {
        DeferredItem item = new DeferredItem(user, "Do laundry");
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        DeferredItemResponse response = service.defer(user, itemId, new DeferRequest(DeferDuration.ONE_DAY));

        LocalDate expectedDate = LocalDate.now(java.time.ZoneId.of("UTC")).plusDays(1);
        assertThat(item.getDeferredUntilDate()).isEqualTo(expectedDate);
        assertThat(response.deferredUntilDate()).isEqualTo(expectedDate);
    }

    // --- defer ONE_WEEK ---

    @Test
    void defer_oneWeekSetsCorrectDate() {
        DeferredItem item = new DeferredItem(user, "Call dentist");
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        service.defer(user, itemId, new DeferRequest(DeferDuration.ONE_WEEK));

        LocalDate expectedDate = LocalDate.now(java.time.ZoneId.of("UTC")).plusWeeks(1);
        assertThat(item.getDeferredUntilDate()).isEqualTo(expectedDate);
    }

    // --- defer ONE_MONTH ---

    @Test
    void defer_oneMonthSetsCorrectDate() {
        DeferredItem item = new DeferredItem(user, "Renew insurance");
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        service.defer(user, itemId, new DeferRequest(DeferDuration.ONE_MONTH));

        LocalDate expectedDate = LocalDate.now(java.time.ZoneId.of("UTC")).plusMonths(1);
        assertThat(item.getDeferredUntilDate()).isEqualTo(expectedDate);
    }

    // --- defer increments deferralCount ---

    @Test
    void defer_incrementsDeferralCountOnFirstDefer() {
        DeferredItem item = new DeferredItem(user, "Fix bike");
        assertThat(item.getDeferralCount()).isZero();
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        service.defer(user, itemId, new DeferRequest(DeferDuration.ONE_DAY));

        assertThat(item.getDeferralCount()).isEqualTo(1);
    }

    @Test
    void defer_incrementsDeferralCountOnEachSubsequentDefer() {
        DeferredItem item = new DeferredItem(user, "Write blog post");
        item.setDeferralCount(2);
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        service.defer(user, itemId, new DeferRequest(DeferDuration.ONE_WEEK));

        assertThat(item.getDeferralCount()).isEqualTo(3);
    }

    // --- dismiss ---

    @Test
    void dismiss_marksItemProcessedWithTimestamp() {
        DeferredItem item = new DeferredItem(user, "Old idea");
        assertThat(item.isProcessed()).isFalse();
        assertThat(item.getProcessedAt()).isNull();
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.of(item));

        DeferredItemResponse response = service.dismiss(user, itemId);

        assertThat(item.isProcessed()).isTrue();
        assertThat(item.getProcessedAt()).isNotNull();
        assertThat(response.isProcessed()).isTrue();
        assertThat(response.processedAt()).isNotNull();
    }

    // --- dismiss not found ---

    @Test
    void dismiss_throwsWhenItemNotFound() {
        when(repository.findByIdAndUserId(user, itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dismiss(user, itemId))
                .isInstanceOf(DeferredItemNotFoundException.class)
                .hasMessageContaining(itemId.toString());
    }
}
