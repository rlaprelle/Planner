package com.echel.planner.backend.auth;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(name = "default_start_time", nullable = false)
    private LocalTime defaultStartTime = LocalTime.of(8, 0);

    @Column(name = "default_end_time", nullable = false)
    private LocalTime defaultEndTime = LocalTime.of(17, 0);

    @Column(name = "default_session_minutes", nullable = false)
    private short defaultSessionMinutes = 60;

    @Column(name = "week_start_day", nullable = false)
    private short weekStartDay = 1;

    @Column(name = "ceremony_day", nullable = false)
    private short ceremonyDay = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public AppUser() {}

    public AppUser(String email, String passwordHash, String displayName, String timezone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.timezone = timezone;
    }

    // UserDetails contract

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public LocalTime getDefaultStartTime() { return defaultStartTime; }
    public void setDefaultStartTime(LocalTime defaultStartTime) { this.defaultStartTime = defaultStartTime; }

    public LocalTime getDefaultEndTime() { return defaultEndTime; }
    public void setDefaultEndTime(LocalTime defaultEndTime) { this.defaultEndTime = defaultEndTime; }

    public int getDefaultSessionMinutes() { return defaultSessionMinutes; }
    public void setDefaultSessionMinutes(int defaultSessionMinutes) { this.defaultSessionMinutes = (short) defaultSessionMinutes; }

    public DayOfWeek getWeekStartDay() { return DayOfWeek.of(weekStartDay); }
    public void setWeekStartDay(DayOfWeek weekStartDay) { this.weekStartDay = (short) weekStartDay.getValue(); }

    public DayOfWeek getCeremonyDay() { return DayOfWeek.of(ceremonyDay); }
    public void setCeremonyDay(DayOfWeek ceremonyDay) { this.ceremonyDay = (short) ceremonyDay.getValue(); }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public enum Role { USER, ADMIN }
}
