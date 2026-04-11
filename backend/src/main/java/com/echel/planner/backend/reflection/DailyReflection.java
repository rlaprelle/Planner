package com.echel.planner.backend.reflection;

import com.echel.planner.backend.auth.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.echel.planner.backend.reflection.ReflectionType;

@Entity
@Table(name = "daily_reflection")
public class DailyReflection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "reflection_date", nullable = false)
    private LocalDate reflectionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reflection_type", nullable = false, length = 10)
    private ReflectionType reflectionType = ReflectionType.DAILY;

    @Column(name = "energy_rating", nullable = false)
    private short energyRating;

    @Column(name = "mood_rating", nullable = false)
    private short moodRating;

    @Column(name = "reflection_notes", columnDefinition = "TEXT")
    private String reflectionNotes;

    @Column(name = "is_finalized", nullable = false)
    private boolean isFinalized = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public DailyReflection() {}

    public DailyReflection(AppUser user, LocalDate reflectionDate) {
        this.user = user;
        this.reflectionDate = reflectionDate;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalDate getReflectionDate() { return reflectionDate; }
    public ReflectionType getReflectionType() { return reflectionType; }
    public short getEnergyRating() { return energyRating; }
    public short getMoodRating() { return moodRating; }
    public String getReflectionNotes() { return reflectionNotes; }
    public boolean isFinalized() { return isFinalized; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEnergyRating(short energyRating) { this.energyRating = energyRating; }
    public void setMoodRating(short moodRating) { this.moodRating = moodRating; }
    public void setReflectionNotes(String reflectionNotes) { this.reflectionNotes = reflectionNotes; }
    public void setReflectionType(ReflectionType reflectionType) { this.reflectionType = reflectionType; }
    public void setFinalized(boolean finalized) { isFinalized = finalized; }
}
