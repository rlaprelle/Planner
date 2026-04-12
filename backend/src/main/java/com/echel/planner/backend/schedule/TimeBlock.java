package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.event.Event;
import com.echel.planner.backend.task.Task;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "time_block")
public class TimeBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "block_date", nullable = false)
    private LocalDate blockDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "actual_end")
    private Instant actualEnd;

    @Column(name = "was_completed", nullable = false)
    private boolean wasCompleted = false;

    public TimeBlock() {}

    public TimeBlock(AppUser user, LocalDate blockDate, Task task, LocalTime startTime, LocalTime endTime, int sortOrder) {
        this.user = user;
        this.blockDate = blockDate;
        this.task = task;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sortOrder = sortOrder;
    }

    /** Constructs a time block for an event (no task). */
    public TimeBlock(AppUser user, LocalDate blockDate, Event event, LocalTime startTime, LocalTime endTime, int sortOrder) {
        this.user = user;
        this.blockDate = blockDate;
        this.event = event;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalDate getBlockDate() { return blockDate; }
    public Task getTask() { return task; }
    public Event getEvent() { return event; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getSortOrder() { return sortOrder; }

    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }

    public Instant getActualEnd() { return actualEnd; }
    public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }

    public boolean isWasCompleted() { return wasCompleted; }
    public void setWasCompleted(boolean wasCompleted) { this.wasCompleted = wasCompleted; }

    public void setBlockDate(LocalDate blockDate) { this.blockDate = blockDate; }
    public void setTask(Task task) { this.task = task; }
    public void setEvent(Event event) { this.event = event; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    /** Computes elapsed minutes between actualStart and actualEnd. Both must be set. */
    public int calculateElapsedMinutes() {
        if (actualEnd == null) {
            throw new IllegalStateException("Cannot calculate elapsed minutes: actualEnd is not set");
        }
        return (int) java.time.Duration.between(actualStart, actualEnd).toMinutes();
    }
}
