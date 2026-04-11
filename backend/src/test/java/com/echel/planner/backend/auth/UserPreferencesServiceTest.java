package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.PreferencesResponse;
import com.echel.planner.backend.auth.dto.UpdatePreferencesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceTest {

    @Mock
    private AppUserRepository userRepository;

    private UserPreferencesService service;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        service = new UserPreferencesService(userRepository);
        testUser = new AppUser("test@example.com", "hash", "Test User", "UTC");
    }

    @Test
    void getPreferences_returnsCurrentValues() {
        PreferencesResponse response = service.getPreferences(testUser);
        assertThat(response.displayName()).isEqualTo("Test User");
        assertThat(response.timezone()).isEqualTo("UTC");
        assertThat(response.defaultStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.defaultEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(response.defaultSessionMinutes()).isEqualTo(60);
        assertThat(response.weekStartDay()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.ceremonyDay()).isEqualTo(DayOfWeek.FRIDAY);
    }

    @Test
    void updatePreferences_partialUpdate_onlyChangesSpecifiedFields() {
        var request = new UpdatePreferencesRequest(null, null, null, null, null, null, DayOfWeek.SUNDAY);
        when(userRepository.save(testUser)).thenReturn(testUser);
        PreferencesResponse response = service.updatePreferences(testUser, request);
        assertThat(response.ceremonyDay()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(response.displayName()).isEqualTo("Test User");
        verify(userRepository).save(testUser);
    }

    @Test
    void updatePreferences_invalidTimezone_throws() {
        var request = new UpdatePreferencesRequest(null, "Not/A/Zone", null, null, null, null, null);
        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("timezone");
    }

    @Test
    void updatePreferences_startTimeAfterEndTime_throws() {
        var request = new UpdatePreferencesRequest(null, null, LocalTime.of(18, 0), LocalTime.of(9, 0), null, null, null);
        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("before");
    }

    @Test
    void updatePreferences_sessionMinutesNotMultipleOf15_throws() {
        var request = new UpdatePreferencesRequest(null, null, null, null, 25, null, null);
        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("multiple of 15");
    }

    @Test
    void updatePreferences_blankDisplayName_throws() {
        var request = new UpdatePreferencesRequest("   ", null, null, null, null, null, null);
        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("display name");
    }

    @Test
    void updatePreferences_timeNotAlignedTo15Minutes_throws() {
        var request = new UpdatePreferencesRequest(null, null, LocalTime.of(8, 7), null, null, null, null);
        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("15-minute");
    }

    @Test
    void updatePreferences_allFieldsSet_updatesEverything() {
        var request = new UpdatePreferencesRequest(
                "New Name", "America/New_York",
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                45, DayOfWeek.SUNDAY, DayOfWeek.THURSDAY
        );
        when(userRepository.save(testUser)).thenReturn(testUser);
        PreferencesResponse response = service.updatePreferences(testUser, request);
        assertThat(response.displayName()).isEqualTo("New Name");
        assertThat(response.timezone()).isEqualTo("America/New_York");
        assertThat(response.defaultStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.defaultEndTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.defaultSessionMinutes()).isEqualTo(45);
        assertThat(response.weekStartDay()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(response.ceremonyDay()).isEqualTo(DayOfWeek.THURSDAY);
    }
}
