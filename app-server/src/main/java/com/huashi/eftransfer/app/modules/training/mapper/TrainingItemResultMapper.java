package com.huashi.eftransfer.app.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.training.entity.TrainingItemResultEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrainingItemResultMapper extends BaseMapper<TrainingItemResultEntity> {

    @Update("""
            UPDATE training_item_result
            SET selected_answer_key = #{selectedAnswerKey},
                answer_payload_json = #{answerPayloadJson},
                submitted_at = #{submittedAt},
                reaction_time_ms = #{reactionTimeMs},
                hesitation_time_ms = #{hesitationTimeMs},
                is_correct = #{correct},
                detected_error_type = #{detectedErrorType},
                review_required = #{reviewRequired},
                adaptation_action = #{adaptationAction},
                answer_state = #{answerState},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{itemResultId}
              AND session_id = #{sessionId}
              AND answer_state = #{pendingState}
              AND deleted = FALSE
            """)
    int submitAnswer(
            @Param("itemResultId") Long itemResultId,
            @Param("sessionId") Long sessionId,
            @Param("pendingState") String pendingState,
            @Param("answerState") String answerState,
            @Param("selectedAnswerKey") String selectedAnswerKey,
            @Param("answerPayloadJson") String answerPayloadJson,
            @Param("submittedAt") java.time.LocalDateTime submittedAt,
            @Param("reactionTimeMs") Integer reactionTimeMs,
            @Param("hesitationTimeMs") Integer hesitationTimeMs,
            @Param("correct") Boolean correct,
            @Param("detectedErrorType") String detectedErrorType,
            @Param("reviewRequired") Boolean reviewRequired,
            @Param("adaptationAction") String adaptationAction
    );
}
