package com.planner.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TimeBlockRepository extends JpaRepository<TimeBlock, UUID> {

    @Query("""
            SELECT tb FROM TimeBlock tb
            LEFT JOIN FETCH tb.task t
            LEFT JOIN FETCH t.project
            WHERE tb.user.id = :userId
              AND tb.blockDate = :blockDate
            ORDER BY tb.sortOrder ASC
            """)
    List<TimeBlock> findByUserIdAndBlockDateWithTask(@Param("userId") UUID userId,
                                                     @Param("blockDate") LocalDate blockDate);

    @Modifying
    @Query("DELETE FROM TimeBlock tb WHERE tb.user.id = :userId AND tb.blockDate = :blockDate")
    void deleteByUserIdAndBlockDate(@Param("userId") UUID userId, @Param("blockDate") LocalDate blockDate);

    @Query("""
            SELECT COUNT(tb) FROM TimeBlock tb
            WHERE tb.user.id = :userId
              AND tb.blockDate = :blockDate
            """)
    long countByUserIdAndBlockDate(@Param("userId") UUID userId, @Param("blockDate") LocalDate blockDate);

    @Query("""
            SELECT COUNT(tb) FROM TimeBlock tb
            WHERE tb.user.id = :userId
              AND tb.blockDate = :blockDate
              AND tb.task.status = com.planner.backend.task.TaskStatus.DONE
            """)
    long countCompletedByUserIdAndBlockDate(@Param("userId") UUID userId, @Param("blockDate") LocalDate blockDate);
}
