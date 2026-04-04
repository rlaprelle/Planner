package com.planner.backend.deferred;

import com.planner.backend.auth.AppUser;
import com.planner.backend.project.Project;
import com.planner.backend.task.Task;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "deferred_item")
public class DeferredItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "is_processed", nullable = false)
    private boolean isProcessed = false;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_task_id")
    private Task resolvedTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_project_id")
    private Project resolvedProject;

    @Column(name = "deferred_until_date")
    private LocalDate deferredUntilDate;

    @Column(name = "deferral_count", nullable = false)
    private int deferralCount = 0;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public DeferredItem() {}

    public DeferredItem(AppUser user, String rawText) {
        this.user = user;
        this.rawText = rawText;
        this.capturedAt = Instant.now();
    }

    // Getters

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getRawText() { return rawText; }
    public boolean isProcessed() { return isProcessed; }
    public Instant getCapturedAt() { return capturedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public Task getResolvedTask() { return resolvedTask; }
    public Project getResolvedProject() { return resolvedProject; }
    public LocalDate getDeferredUntilDate() { return deferredUntilDate; }
    public int getDeferralCount() { return deferralCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters (only fields mutated in this slice and future Evening Ritual slice)

    public void setProcessed(boolean processed) { isProcessed = processed; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public void setResolvedTask(Task resolvedTask) { this.resolvedTask = resolvedTask; }
    public void setResolvedProject(Project resolvedProject) { this.resolvedProject = resolvedProject; }
    public void setDeferredUntilDate(LocalDate deferredUntilDate) { this.deferredUntilDate = deferredUntilDate; }
    public void setDeferralCount(int deferralCount) { this.deferralCount = deferralCount; }
    public void setRawText(String rawText) { this.rawText = rawText; }
}
