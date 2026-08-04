package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AssessmentAttemptAnswerMapper extends BaseMapper<AssessmentAttemptAnswerEntity> {

    @Update("""
            UPDATE assessment_attempt_answer
            SET first_presented_at = COALESCE(first_presented_at, CURRENT_TIMESTAMP),
                effective_duration_ms = effective_duration_ms + #{deltaMs},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{answerId}
              AND attempt_id = #{attemptId}
              AND deleted = FALSE
            """)
    int recordEffectiveDuration(@Param("answerId") Long answerId,
                                @Param("attemptId") Long attemptId,
                                @Param("deltaMs") long deltaMs);

    @Update("""
            UPDATE assessment_attempt_answer
            SET response_json = #{answer.responseJson},
                justification_text = #{answer.justificationText},
                first_answered_at = #{answer.firstAnsweredAt},
                response_change_count = COALESCE(#{answer.responseChangeCount}, response_change_count),
                answered = #{answer.answered},
                correct = #{answer.correct},
                score_awarded = #{answer.scoreAwarded},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{answer.id}
              AND attempt_id = #{answer.attemptId}
              AND deleted = FALSE
            """)
    int updateResponseSnapshot(@Param("answer") AssessmentAttemptAnswerEntity answer);
}
