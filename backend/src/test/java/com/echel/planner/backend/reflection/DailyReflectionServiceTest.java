package com.echel.planner.backend.reflection;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.reflection.dto.ReflectionRequest;
import com.echel.planner.backend.reflection.dto.ReflectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReflectionServiceTest {

    @Mock
    private DailyReflectionRepository repository;

    @InjectMocks
    private DailyReflectionService service;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser("test@example.com", "hash", "Test User", "UTC");
    }

    @Test
    void upsert_createsNewReflection_whenNoneExistsForToday() {
        ReflectionRequest request = new ReflectionRequest((short) 3, (short) 4, "Feeling good", false);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("UTC"));

        when(repository.findByUserAndReflectionDate(user, today)).thenReturn(Optional.empty());
        when(repository.save(any(DailyReflection.class))).thenAnswer(inv -> inv.getArgument(0));

        ReflectionResponse response = service.upsert(user, request);

        ArgumentCaptor<DailyReflection> captor = ArgumentCaptor.forClass(DailyReflection.class);
        verify(repository).save(captor.capture());
        DailyReflection saved = captor.getValue();

        assertThat(saved.getReflectionDate()).isEqualTo(today);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(response).isNotNull();
    }

    @Test
    void upsert_updatesExistingReflection_whenOneExistsForToday() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("UTC"));
        DailyReflection existing = new DailyReflection(user, today);
        existing.setEnergyRating((short) 1);
        existing.setMoodRating((short) 1);
        existing.setReflectionNotes("old notes");
        existing.setFinalized(false);

        ReflectionRequest request = new ReflectionRequest((short) 5, (short) 5, "Updated notes", true);

        when(repository.findByUserAndReflectionDate(user, today)).thenReturn(Optional.of(existing));
        when(repository.save(any(DailyReflection.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upsert(user, request);

        verify(repository, never()).save(argThat(r -> r != existing));
        ArgumentCaptor<DailyReflection> captor = ArgumentCaptor.forClass(DailyReflection.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void upsert_setsAllFieldsFromRequest() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("UTC"));
        ReflectionRequest request = new ReflectionRequest((short) 2, (short) 3, "Some notes", true);

        when(repository.findByUserAndReflectionDate(user, today)).thenReturn(Optional.empty());
        when(repository.save(any(DailyReflection.class))).thenAnswer(inv -> inv.getArgument(0));

        ReflectionResponse response = service.upsert(user, request);

        ArgumentCaptor<DailyReflection> captor = ArgumentCaptor.forClass(DailyReflection.class);
        verify(repository).save(captor.capture());
        DailyReflection saved = captor.getValue();

        assertThat(saved.getEnergyRating()).isEqualTo((short) 2);
        assertThat(saved.getMoodRating()).isEqualTo((short) 3);
        assertThat(saved.getReflectionNotes()).isEqualTo("Some notes");
        assertThat(saved.isFinalized()).isTrue();

        assertThat(response.energyRating()).isEqualTo((short) 2);
        assertThat(response.moodRating()).isEqualTo((short) 3);
        assertThat(response.reflectionNotes()).isEqualTo("Some notes");
        assertThat(response.isFinalized()).isTrue();
    }
}
