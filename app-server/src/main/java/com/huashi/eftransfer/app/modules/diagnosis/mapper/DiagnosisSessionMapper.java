package com.huashi.eftransfer.app.modules.diagnosis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
            @Param("abandonedAt") java.time.LocalDateTime abandonedAt
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
            @Param("staleBefore") java.time.LocalDateTime staleBefore
    );
}
