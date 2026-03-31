package com.planner.backend.deferred;

import com.planner.backend.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DeferredItemRepository extends JpaRepository<DeferredItem, UUID> {

    @Query("""
            SELECT d FROM DeferredItem d
            WHERE d.user = :user
              AND d.isProcessed = false
              AND (d.deferredUntilDate IS NULL OR d.deferredUntilDate <= :today)
            ORDER BY d.capturedAt ASC
            """)
    List<DeferredItem> findPendingForUser(@Param("user") AppUser user, @Param("today") LocalDate today);
}
