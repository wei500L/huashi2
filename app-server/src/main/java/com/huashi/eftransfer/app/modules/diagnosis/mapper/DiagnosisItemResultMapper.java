package com.huashi.eftransfer.app.modules.diagnosis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisItemResultEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DiagnosisItemResultMapper extends BaseMapper<DiagnosisItemResultEntity> {

    @Update("""
            UPDATE diagnosis_item_result
            SET answer_state = #{answerState},
                stimulus_started_at = COALESCE(stimulus_startedAt, #{stimulusStartedAt}),
                submitted_at = #{submittedAt},
                reaction_time_ms = #{reactionTimeMs},
                hesitation_time_ms = #{hesitationTimeMs},
                selected_answer_key = #{selectedAnswerKey},
                answer_payload_json = #{answerPayloadJson},
                is_correct = #{correct},
                detected_error_type = #{detectedErrorType},
                semantic_consistent = #{semanticConsistent},
                transfer_risk_score = #{transferRiskScore},
                item_score = #{itemScore},
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
            @Param("stimulusStartedAt") java.time.LocalDateTime stimulusStartedAt,
            @Param("submittedAt") java.time.LocalDateTime submittedAt,
            @Param("reactionTimeMs") Integer reactionTimeMs,
            @Param("hesitationTimeMs") Integer hesitationTimeMs,
            @Param("selectedAnswerKey") String selectedAnswerKey,
            @Param("answerPayloadJson") String answerPayloadJson,
            @Param("correct") Boolean correct,
            @Param("detectedErrorType") String detectedErrorType,
            @Param("semanticConsistent") Boolean semanticConsistent,
            @Param("transferRiskScore") java.math.BigDecimal transferRiskScore,
            @Param("itemScore") java.math.BigDecimal itemScore
    );
}
