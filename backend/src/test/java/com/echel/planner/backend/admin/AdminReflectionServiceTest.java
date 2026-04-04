package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminReflectionRequest;
import com.echel.planner.backend.admin.dto.AdminReflectionResponse;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.reflection.DailyReflection;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReflectionServiceTest {

    @Mock
    private DailyReflectionRepository reflectionRepository;

    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private AdminReflectionService service;

    private AppUser user;
    private UUID userId;
    private UUID reflectionId;

    @BeforeEach
    void setUp() {
        user = new AppUser("admin@example.com", "hash", "Admin User", "UTC");
        userId = UUID.randomUUID();
        reflectionId = UUID.randomUUID();
    }

    // --- listAll ---

    @Test
    void listAll_returnsMappedResponses() {
        LocalDate date1 = LocalDate.of(2026, 3, 1);
        LocalDate date2 = LocalDate.of(2026, 3, 2);
        DailyReflection r1 = new DailyReflection(user, date1);
        r1.setEnergyRating((short) 3);
        r1.setMoodRating((short) 4);
        DailyReflection r2 = new DailyReflection(user, date2);
        r2.setEnergyRating((short) 5);
        r2.setMoodRating((short) 2);
        when(reflectionRepository.findAll()).thenReturn(List.of(r1, r2));

        List<AdminReflectionResponse> responses = service.listAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).reflectionDate()).isEqualTo(date1);
        assertThat(responses.get(1).reflectionDate()).isEqualTo(date2);
    }

    // --- get ---

    @Test
    void get_throwsAdminNotFoundExceptionWhenReflectionAbsent() {
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(reflectionId))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(reflectionId.toString());
    }

    // --- create ---

    @Test
    void create_looksUpUserAndCreatesReflectionWithDateAndRatingsAndNotes() {
        LocalDate reflectionDate = LocalDate.of(2026, 4, 1);
        AdminReflectionRequest request = new AdminReflectionRequest(
                userId, reflectionDate, (short) 4, (short) 3, "Felt good today", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DailyReflection saved = new DailyReflection(user, reflectionDate);
        saved.setEnergyRating((short) 4);
        saved.setMoodRating((short) 3);
        saved.setReflectionNotes("Felt good today");
        when(reflectionRepository.save(any(DailyReflection.class))).thenReturn(saved);

        AdminReflectionResponse response = service.create(request);

        ArgumentCaptor<DailyReflection> captor = ArgumentCaptor.forClass(DailyReflection.class);
        verify(reflectionRepository).save(captor.capture());
        DailyReflection captured = captor.getValue();
        assertThat(captured.getUser()).isSameAs(user);
        assertThat(captured.getReflectionDate()).isEqualTo(reflectionDate);
        assertThat(captured.getEnergyRating()).isEqualTo((short) 4);
        assertThat(captured.getMoodRating()).isEqualTo((short) 3);
        assertThat(captured.getReflectionNotes()).isEqualTo("Felt good today");
        assertThat(response.reflectionDate()).isEqualTo(reflectionDate);
    }

    @Test
    void create_throwsAdminNotFoundExceptionWhenUserAbsent() {
        AdminReflectionRequest request = new AdminReflectionRequest(
                userId, LocalDate.of(2026, 4, 1), (short) 3, (short) 3, null, null);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void create_setsIsFinalizedWhenProvided() {
        LocalDate reflectionDate = LocalDate.of(2026, 4, 2);
        AdminReflectionRequest request = new AdminReflectionRequest(
                userId, reflectionDate, (short) 5, (short) 5, "Great day", true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DailyReflection saved = new DailyReflection(user, reflectionDate);
        saved.setEnergyRating((short) 5);
        saved.setMoodRating((short) 5);
        saved.setReflectionNotes("Great day");
        saved.setFinalized(true);
        when(reflectionRepository.save(any(DailyReflection.class))).thenReturn(saved);

        ArgumentCaptor<DailyReflection> captor = ArgumentCaptor.forClass(DailyReflection.class);
        service.create(request);
        verify(reflectionRepository).save(captor.capture());

        assertThat(captor.getValue().isFinalized()).isTrue();
    }

    @Test
    void create_doesNotOverrideDefaultFinalizedWhenIsFinalizedIsNull() {
        LocalDate reflectionDate = LocalDate.of(2026, 4, 3);
        AdminReflectionRequest request = new AdminReflectionRequest(
                userId, reflectionDate, (short) 2, (short) 3, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        DailyReflection saved = new DailyReflection(user, reflectionDate);
        saved.setEnergyRating((short) 2);
        saved.setMoodRating((short) 3);
        when(reflectionRepository.save(any(DailyReflection.class))).thenReturn(saved);

        ArgumentCaptor<DailyReflection> captor = ArgumentCaptor.forClass(DailyReflection.class);
        service.create(request);
        verify(reflectionRepository).save(captor.capture());

        assertThat(captor.getValue().isFinalized()).isFalse();
    }

    // --- update ---

    @Test
    void update_setsRatingsAndNotes() {
        DailyReflection existing = new DailyReflection(user, LocalDate.of(2026, 3, 10));
        existing.setEnergyRating((short) 1);
        existing.setMoodRating((short) 1);
        existing.setReflectionNotes("Old notes");
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(existing));

        AdminReflectionRequest request = new AdminReflectionRequest(
                userId, LocalDate.of(2026, 3, 10), (short) 4, (short) 5, "New notes", null);

        service.update(reflectionId, request);

        assertThat(existing.getEnergyRating()).isEqualTo((short) 4);
        assertThat(existing.getMoodRating()).isEqualTo((short) 5);
        assertThat(existing.getReflectionNotes()).isEqualTo("New notes");
        assertThat(existing.isFinalized()).isFalse();
    }

    @Test
    void update_conditionallySetsIsFinalizedWhenProvided() {
        DailyReflection existing = new DailyReflection(user, LocalDate.of(2026, 3, 11));
        existing.setEnergyRating((short) 3);
        existing.setMoodRating((short) 3);
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(existing));

        AdminReflectionRequest request = new AdminReflectionRequest(
                userId, LocalDate.of(2026, 3, 11), (short) 3, (short) 3, null, true);

        service.update(reflectionId, request);

        assertThat(existing.isFinalized()).isTrue();
    }

    @Test
    void update_throwsAdminNotFoundExceptionWhenReflectionAbsent() {
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.empty());
        AdminReflectionRequest request = new AdminReflectionRequest(
                userId, LocalDate.of(2026, 3, 12), (short) 3, (short) 3, null, null);

        assertThatThrownBy(() -> service.update(reflectionId, request))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(reflectionId.toString());
    }

    // --- delete ---

    @Test
    void delete_removesReflectionById() {
        DailyReflection existing = new DailyReflection(user, LocalDate.of(2026, 3, 15));
        existing.setEnergyRating((short) 3);
        existing.setMoodRating((short) 3);
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.of(existing));

        service.delete(reflectionId);

        verify(reflectionRepository).deleteById(reflectionId);
    }

    @Test
    void delete_throwsAdminNotFoundExceptionWhenReflectionAbsent() {
        when(reflectionRepository.findById(reflectionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(reflectionId))
                .isInstanceOf(AdminExceptionHandler.AdminNotFoundException.class)
                .hasMessageContaining(reflectionId.toString());
    }
}
