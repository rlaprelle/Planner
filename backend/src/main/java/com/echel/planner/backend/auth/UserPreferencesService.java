package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.PreferencesResponse;
import com.echel.planner.backend.auth.dto.UpdatePreferencesRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;

@Service
@Transactional
public class UserPreferencesService {

    private final AppUserRepository userRepository;

    public UserPreferencesService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(AppUser user) {
        return PreferencesResponse.from(user);
    }

    public PreferencesResponse updatePreferences(AppUser user, UpdatePreferencesRequest request) {
        if (request.displayName() != null) {
            if (request.displayName().isBlank()) {
                throw new PreferencesValidationException("Preferred display name must not be blank");
            }
            user.setDisplayName(request.displayName().trim());
        }

        if (request.timezone() != null) {
            try {
                ZoneId.of(request.timezone());
            } catch (Exception e) {
                throw new PreferencesValidationException("Invalid timezone: " + request.timezone());
            }
            user.setTimezone(request.timezone());
        }

        LocalTime effectiveStart = request.defaultStartTime() != null
                ? request.defaultStartTime() : user.getDefaultStartTime();
        LocalTime effectiveEnd = request.defaultEndTime() != null
                ? request.defaultEndTime() : user.getDefaultEndTime();

        if (request.defaultStartTime() != null) {
            validateTimeAlignment(request.defaultStartTime());
            user.setDefaultStartTime(request.defaultStartTime());
        }

        if (request.defaultEndTime() != null) {
            validateTimeAlignment(request.defaultEndTime());
            user.setDefaultEndTime(request.defaultEndTime());
        }

        if (!effectiveStart.isBefore(effectiveEnd)) {
            throw new PreferencesValidationException("Start time must be before end time");
        }

        if (request.defaultSessionMinutes() != null) {
            int minutes = request.defaultSessionMinutes();
            if (minutes < 15 || minutes > 240 || minutes % 15 != 0) {
                throw new PreferencesValidationException(
                        "Session duration must be a multiple of 15 minutes (15-240)");
            }
            user.setDefaultSessionMinutes(minutes);
        }

        if (request.weekStartDay() != null) {
            user.setWeekStartDay(request.weekStartDay());
        }

        if (request.ceremonyDay() != null) {
            user.setCeremonyDay(request.ceremonyDay());
        }

        userRepository.save(user);
        return PreferencesResponse.from(user);
    }

    private void validateTimeAlignment(LocalTime time) {
        if (time.getMinute() % 15 != 0 || time.getSecond() != 0) {
            throw new PreferencesValidationException(
                    "Times must be aligned to 15-minute increments");
        }
    }

    static class PreferencesValidationException extends RuntimeException {
        PreferencesValidationException(String message) { super(message); }
    }
}
