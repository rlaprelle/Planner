package com.echel.planner.backend.reflection;

import com.echel.planner.backend.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyReflectionRepository extends JpaRepository<DailyReflection, UUID> {

    Optional<DailyReflection> findByUserAndReflectionDateAndReflectionType(
            AppUser user, LocalDate date, ReflectionType reflectionType);

    @Query("""
            SELECT r.reflectionDate FROM DailyReflection r
            WHERE r.user = :user
              AND r.isFinalized = true
              AND r.reflectionType = com.echel.planner.backend.reflection.ReflectionType.DAILY
            ORDER BY r.reflectionDate DESC
            """)
    List<LocalDate> findFinalizedDatesDesc(@Param("user") AppUser user);

    @Query("""
            SELECT r FROM DailyReflection r
            WHERE r.user = :user
              AND r.isFinalized = true
              AND r.reflectionType = com.echel.planner.backend.reflection.ReflectionType.DAILY
              AND r.reflectionDate >= :start
              AND r.reflectionDate <= :end
            ORDER BY r.reflectionDate ASC
            """)
    List<DailyReflection> findFinalizedInDateRange(@Param("user") AppUser user,
                                                    @Param("start") LocalDate start,
                                                    @Param("end") LocalDate end);

    long countByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
