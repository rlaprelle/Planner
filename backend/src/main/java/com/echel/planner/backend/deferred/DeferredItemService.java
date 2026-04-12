package com.echel.planner.backend.deferred;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.dto.ConvertToEventRequest;
import com.echel.planner.backend.deferred.dto.ConvertToTaskRequest;
import com.echel.planner.backend.deferred.dto.DeferRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemResponse;
import com.echel.planner.backend.event.Event;
import com.echel.planner.backend.event.EventRepository;
import com.echel.planner.backend.event.EventService;
import com.echel.planner.backend.event.dto.EventCreateRequest;
import com.echel.planner.backend.event.dto.EventResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskService;
import com.echel.planner.backend.task.dto.TaskCreateRequest;
import com.echel.planner.backend.task.dto.TaskResponse;
import com.echel.planner.backend.common.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeferredItemService {

    private final DeferredItemRepository repository;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final EventService eventService;
    private final EventRepository eventRepository;

    public DeferredItemService(DeferredItemRepository repository,
                               TaskService taskService,
                               TaskRepository taskRepository,
                               EventService eventService,
                               EventRepository eventRepository) {
        this.repository = repository;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    public DeferredItemResponse create(AppUser user, DeferredItemCreateRequest request) {
        DeferredItem item = new DeferredItem(user, request.rawText());
        return DeferredItemResponse.from(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<DeferredItemResponse> listPending(AppUser user) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        return repository.findPendingForUser(user, today).stream()
                .map(DeferredItemResponse::from)
                .toList();
    }

    public TaskResponse convert(AppUser user, UUID itemId, ConvertToTaskRequest request) {
        DeferredItem item = findOwnedItem(user, itemId);
        TaskCreateRequest taskReq = new TaskCreateRequest(
                request.title(), request.description(), null, null,
                request.priority(), request.pointsEstimate(), null, null,
                request.dueDate(), null);
        TaskResponse created = taskService.create(user, request.projectId(), taskReq);
        Task task = taskRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("Newly created task could not be reloaded: " + created.id()));
        item.setProcessed(true);
        item.setProcessedAt(Instant.now());
        item.setResolvedTask(task);
        return created;
    }

    /** Converts a deferred item into a calendar event and marks it as processed. */
    public EventResponse convertToEvent(AppUser user, UUID itemId, ConvertToEventRequest request) {
        DeferredItem item = findOwnedItem(user, itemId);
        EventCreateRequest eventReq = new EventCreateRequest(
                request.title(), request.description(), request.energyLevel(),
                request.blockDate(), request.startTime(), request.endTime());
        EventResponse created = eventService.create(user, request.projectId(), eventReq);
        Event event = eventRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("Newly created event could not be reloaded: " + created.id()));
        item.setProcessed(true);
        item.setProcessedAt(Instant.now());
        item.setResolvedEvent(event);
        return created;
    }

    public DeferredItemResponse defer(AppUser user, UUID itemId, DeferRequest request) {
        DeferredItem item = findOwnedItem(user, itemId);
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        LocalDate until = switch (request.deferFor()) {
            case ONE_DAY -> today.plusDays(1);
            case ONE_WEEK -> today.plusWeeks(1);
            case ONE_MONTH -> today.plusMonths(1);
        };
        item.setDeferredUntilDate(until);
        item.setDeferralCount(item.getDeferralCount() + 1);
        return DeferredItemResponse.from(item);
    }

    public DeferredItemResponse dismiss(AppUser user, UUID itemId) {
        DeferredItem item = findOwnedItem(user, itemId);
        item.setProcessed(true);
        item.setProcessedAt(Instant.now());
        return DeferredItemResponse.from(item);
    }

    private DeferredItem findOwnedItem(AppUser user, UUID itemId) {
        return repository.findByIdAndUserId(user, itemId)
                .orElseThrow(() -> new EntityNotFoundException("Deferred item not found: " + itemId));
    }

}
