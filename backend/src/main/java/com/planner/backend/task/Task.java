package com.planner.backend.task;

import com.planner.backend.auth.AppUser;
import com.planner.backend.project.Project;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(nullable = false)
    private short priority = 3;

    @Column(name = "points_estimate")
    private Short pointsEstimate;

    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_level", length = 10)
    private EnergyLevel energyLevel;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_task_id")
    private Task blockedByTask;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public Task() {}

    public Task(AppUser user, Project project, String title) {
        this.user = user;
        this.project = project;
        this.title = title;
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public AppUser getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Task getParentTask() {
        return parentTask;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public short getPriority() {
        return priority;
    }

    public Short getPointsEstimate() {
        return pointsEstimate;
    }

    public Integer getActualMinutes() {
        return actualMinutes;
    }

    public EnergyLevel getEnergyLevel() {
        return energyLevel;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Task getBlockedByTask() {
        return blockedByTask;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters

    public void setProject(Project project) {
        this.project = project;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setParentTask(Task parentTask) {
        this.parentTask = parentTask;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setPriority(short priority) {
        this.priority = priority;
    }

    public void setPointsEstimate(Short pointsEstimate) {
        this.pointsEstimate = pointsEstimate;
    }

    public void setActualMinutes(Integer actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    public void setEnergyLevel(EnergyLevel energyLevel) {
        this.energyLevel = energyLevel;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setBlockedByTask(Task blockedByTask) {
        this.blockedByTask = blockedByTask;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
