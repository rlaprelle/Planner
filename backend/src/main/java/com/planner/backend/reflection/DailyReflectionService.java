package com.planner.backend.reflection;

import com.planner.backend.auth.AppUser;
import com.planner.backend.reflection.dto.ReflectionRequest;
import com.planner.backend.reflection.dto.ReflectionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Transactional
public class DailyReflectionService {

    private final DailyReflectionRepository repository;

    public DailyReflectionService(DailyReflectionRepository repository) {
        this.repository = repository;
    }

    public ReflectionResponse upsert(AppUser user, ReflectionRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        DailyReflection reflection = repository
                .findByUserAndReflectionDate(user, today)
                .orElseGet(() -> new DailyReflection(user, today));
        reflection.setEnergyRating(request.energyRating());
        reflection.setMoodRating(request.moodRating());
        reflection.setReflectionNotes(request.reflectionNotes());
        reflection.setFinalized(request.isFinalized());
        return ReflectionResponse.from(repository.save(reflection));
    }
}
