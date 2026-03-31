package com.planner.backend.stats;

import com.planner.backend.auth.AppUser;
import com.planner.backend.reflection.DailyReflectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final DailyReflectionRepository reflectionRepository;

    public StatsService(DailyReflectionRepository reflectionRepository) {
        this.reflectionRepository = reflectionRepository;
    }

    public int getStreak(AppUser user) {
        List<LocalDate> dates = reflectionRepository.findFinalizedDatesDesc(user);
        LocalDate expected = LocalDate.now(ZoneId.of(user.getTimezone()));
        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
