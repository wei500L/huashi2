package com.huashi.eftransfer.app.modules.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.practice.entity.PracticeSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * Mapper for {@link PracticeSessionEntity}. The active-owner unique key
 * ({@code uk_practice_session_active_owner}) enforces a single IN_PROGRESS
 * practice session per student.
 */
@Mapper
public interface PracticeSessionMapper extends BaseMapper<PracticeSessionEntity> {

    @Select("""
            SELECT id, owner_user_id, bank_code, section_code, status, total_count, answered_count,
                   correct_count, started_at, completed_at,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM practice_session
            WHERE id = #{sessionId}
              AND deleted = FALSE
            FOR UPDATE
            """)
    PracticeSessionEntity selectByIdForUpdate(@Param("sessionId") Long sessionId);

    @Update("""
            UPDATE practice_session
            SET answered_count = #{answeredCount},
                correct_count = #{correctCount},
                status = 'COMPLETED',
                completed_at = #{completedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{sessionId}
              AND status = 'IN_PROGRESS'
              AND deleted = FALSE
            """)
    int complete(
            @Param("sessionId") Long sessionId,
            @Param("answeredCount") Integer answeredCount,
            @Param("correctCount") Integer correctCount,
            @Param("completedAt") LocalDateTime completedAt
    );

    @Update("""
            UPDATE practice_session
            SET answered_count = #{answeredCount},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{sessionId}
              AND status = 'IN_PROGRESS'
              AND deleted = FALSE
            """)
    int syncAnsweredCount(
            @Param("sessionId") Long sessionId,
            @Param("answeredCount") Integer answeredCount
    );
}
