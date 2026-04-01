package com.planner.backend.schedule;

import com.planner.backend.auth.AppUser;
import com.planner.backend.task.Task;
import jakarta.persistence.*;

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

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public TimeBlock() {}

    public TimeBlock(AppUser user, LocalDate blockDate, Task task, LocalTime startTime, LocalTime endTime, int sortOrder) {
        this.user = user;
        this.blockDate = blockDate;
        this.task = task;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalDate getBlockDate() { return blockDate; }
    public Task getTask() { return task; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getSortOrder() { return sortOrder; }
}
