package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssessmentAttemptMapper extends BaseMapper<AssessmentAttemptEntity> {

    @Select("""
            SELECT id
            FROM assessment_attempt
            WHERE deleted = FALSE
              AND status = 'IN_PROGRESS'
              AND expires_at <= #{deadline}
            ORDER BY expires_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<Long> selectExpiredAttemptIds(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);

    @Select("""
            SELECT id, publish_id, paper_id, student_user_id, status, started_at, expires_at, submitted_at,
                   last_saved_at, answered_count, objective_score, total_score, created_at, created_by,
                   updated_at, updated_by, deleted
            FROM assessment_attempt
            WHERE id = #{attemptId}
              AND deleted = FALSE
            FOR UPDATE
            """)
    AssessmentAttemptEntity selectByIdForUpdate(@Param("attemptId") Long attemptId);

    @Update("""
            UPDATE assessment_attempt
            SET answered_count = #{attempt.answeredCount},
                objective_score = #{attempt.objectiveScore},
                total_score = #{attempt.totalScore},
                last_saved_at = #{attempt.lastSavedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{attempt.id}
              AND deleted = FALSE
              AND status = 'IN_PROGRESS'
            """)
    int updateProgressIfInProgress(@Param("attempt") AssessmentAttemptEntity attempt);

    @Update("""
            UPDATE assessment_attempt
            SET status = #{attempt.status},
                answered_count = #{attempt.answeredCount},
                objective_score = #{attempt.objectiveScore},
                total_score = #{attempt.totalScore},
                last_saved_at = #{attempt.lastSavedAt},
                submitted_at = #{attempt.submittedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{attempt.id}
              AND deleted = FALSE
              AND status = 'IN_PROGRESS'
            """)
    int submitIfInProgress(@Param("attempt") AssessmentAttemptEntity attempt);
}
