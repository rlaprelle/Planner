package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.PreferencesResponse;
import com.echel.planner.backend.auth.dto.UpdatePreferencesRequest;
import com.echel.planner.backend.common.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Reads and updates persisted preferences for the authenticated user.
 *
 * <p>Validation rules enforced on update:
 * <ul>
 *   <li>Display name must not be blank and must be 100 characters or fewer.</li>
 *   <li>Timezone must be a valid {@link ZoneId}.</li>
 *   <li>Start and end times must be aligned to 15-minute increments.</li>
 *   <li>Start time must be strictly before end time (using effective values after the update).</li>
 *   <li>Session duration must be a multiple of 15 in the range 15–240 minutes.</li>
 * </ul>
 */
@Service
@Transactional
public class UserPreferencesService {

    private final AppUserRepository userRepository;

    public UserPreferencesService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Returns the preferences for the given user without modifying state. */
    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(AppUser user) {
        return PreferencesResponse.from(user);
    }

    /**
     * Applies all non-null fields from {@code request} to {@code user}, validates the result,
     * persists, and returns the updated preferences.
     *
     * @throws ValidationException if any validation rule is violated
     */
    public PreferencesResponse updatePreferences(AppUser user, UpdatePreferencesRequest request) {
        applyDisplayName(user, request);
        applyTimezone(user, request);
        applyTimeWindow(user, request);
        applySessionMinutes(user, request);
        applyDayFields(user, request);

        userRepository.save(user);
        return PreferencesResponse.from(user);
    }

    private void applyDisplayName(AppUser user, UpdatePreferencesRequest request) {
        if (request.displayName() == null) {
            return;
        }
        if (request.displayName().isBlank()) {
            throw new ValidationException("Preferred display name must not be blank");
        }
        String trimmed = request.displayName().trim();
        if (trimmed.length() > 100) {
            throw new ValidationException("Display name must be 100 characters or fewer");
        }
        user.setDisplayName(trimmed);
    }

    private void applyTimezone(AppUser user, UpdatePreferencesRequest request) {
        if (request.timezone() == null) {
            return;
        }
        try {
            ZoneId.of(request.timezone());
        } catch (Exception e) {
            throw new ValidationException("Invalid timezone: " + request.timezone());
        }
        user.setTimezone(request.timezone());
    }

    private void applyTimeWindow(AppUser user, UpdatePreferencesRequest request) {
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
            throw new ValidationException("Start time must be before end time");
        }
    }

    private void applySessionMinutes(AppUser user, UpdatePreferencesRequest request) {
        if (request.defaultSessionMinutes() == null) {
            return;
        }
        int minutes = request.defaultSessionMinutes();
        if (minutes < 15 || minutes > 240 || minutes % 15 != 0) {
            throw new ValidationException(
                    "Session duration must be a multiple of 15 minutes (15-240)");
        }
        user.setDefaultSessionMinutes(minutes);
    }

    private void applyDayFields(AppUser user, UpdatePreferencesRequest request) {
        if (request.weekStartDay() != null) {
            user.setWeekStartDay(request.weekStartDay());
        }
        if (request.ceremonyDay() != null) {
            user.setCeremonyDay(request.ceremonyDay());
        }
    }

    private void validateTimeAlignment(LocalTime time) {
        if (time.getMinute() % 15 != 0 || time.getSecond() != 0) {
            throw new ValidationException(
                    "Times must be aligned to 15-minute increments");
        }
    }
}
