package com.huashi.eftransfer.app.modules.diagnosis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DiagnosisSessionMapper extends BaseMapper<DiagnosisSessionEntity> {

    @Update("""
            UPDATE diagnosis_session
            SET answered_items = answered_items + 1,
                current_item_order = #{currentItemOrder},
                last_saved_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{sessionId}
              AND status = #{status}
              AND deleted = FALSE
            """)
    int incrementAnsweredItems(
            @Param("sessionId") Long sessionId,
            @Param("currentItemOrder") Integer currentItemOrder,
            @Param("status") String status
    );

    @Update("""
            UPDATE diagnosis_session
            SET last_saved_at = #{heartbeatAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{sessionId}
              AND status = 'IN_PROGRESS'
              AND deleted = FALSE
            """)
    int touchIfInProgress(
            @Param("sessionId") Long sessionId,
            @Param("heartbeatAt") LocalDateTime heartbeatAt
    );

    @Update("""
            UPDATE diagnosis_session
            SET status = 'ABANDONED',
                current_item_order = NULL,
                last_saved_at = #{abandonedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{sessionId}
              AND status = 'IN_PROGRESS'
              AND deleted = FALSE
            """)
    int abandonIfInProgress(
            @Param("sessionId") Long sessionId,
            @Param("abandonedAt") LocalDateTime abandonedAt
    );

    @Select("""
            SELECT id
            FROM diagnosis_session
            WHERE deleted = FALSE
              AND status = 'IN_PROGRESS'
              AND answered_items >= total_items
              AND last_saved_at < #{cutoff}
            ORDER BY last_saved_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<Long> selectTimedOutReadySessionIds(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit
    );

    @Select("""
            SELECT id, template_id, owner_user_id, status, total_items, answered_items, current_item_order,
                   started_at, completed_at, progress_snapshot_json, last_saved_at, launch_context_json,
                   completion_hooks_status, completion_hooks_updated_at, completion_hooks_error,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM diagnosis_session
            WHERE id = #{sessionId}
              AND deleted = FALSE
            FOR UPDATE
            """)
    DiagnosisSessionEntity selectByIdForUpdate(@Param("sessionId") Long sessionId);

    @Update("""
            UPDATE diagnosis_session
            SET status = 'ABANDONED',
                current_item_order = NULL,
                last_saved_at = #{abandonedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id IN (
                SELECT id
                FROM (
                    SELECT id
                    FROM diagnosis_session
                    WHERE deleted = FALSE
                      AND status = 'IN_PROGRESS'
                      AND answered_items < total_items
                      AND last_saved_at < #{cutoff}
                    ORDER BY last_saved_at ASC, id ASC
                    LIMIT #{limit}
                ) timed_out_sessions
            )
            """)
    int batchAbandonTimedOutSessions(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("abandonedAt") LocalDateTime abandonedAt,
            @Param("limit") int limit
    );

    @Update("""
            UPDATE diagnosis_session
            SET completion_hooks_status = 'IN_PROGRESS',
                completion_hooks_updated_at = CURRENT_TIMESTAMP,
                completion_hooks_error = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{sessionId}
              AND status = 'COMPLETED'
              AND deleted = FALSE
              AND (
                  completion_hooks_status IN ('PENDING', 'FAILED')
                  OR (completion_hooks_status = 'IN_PROGRESS'
                      AND completion_hooks_updated_at IS NOT NULL
                      AND completion_hooks_updated_at <= #{staleBefore})
              )
            """)
    int claimCompletionHooks(
            @Param("sessionId") Long sessionId,
            @Param("staleBefore") LocalDateTime staleBefore
    );
}
