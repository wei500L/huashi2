package com.huashi.eftransfer.app.modules.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.practice.entity.PracticeSessionAnswerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PracticeSessionAnswerMapper extends BaseMapper<PracticeSessionAnswerEntity> {

    @Select("""
            SELECT id, session_id, question_order, question_version_id, question_code, question_type,
                   section_code, construct_code, transfer_category, target_word,
                   stem_text_snapshot, prompt_text_snapshot, options_json_snapshot, correct_answer_json,
                   explanation_text_snapshot, option_explanations_json, response_json, is_correct,
                   wrong_attempt_count, spelling_hint_shown, answered_at,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM practice_session_answer
            WHERE session_id = #{sessionId}
              AND deleted = FALSE
            ORDER BY question_order ASC
            """)
    List<PracticeSessionAnswerEntity> selectBySessionId(@Param("sessionId") Long sessionId);

    @Select("""
            SELECT id, session_id, question_order, question_version_id, question_code, question_type,
                   section_code, construct_code, transfer_category, target_word,
                   stem_text_snapshot, prompt_text_snapshot, options_json_snapshot, correct_answer_json,
                   explanation_text_snapshot, option_explanations_json, response_json, is_correct,
                   wrong_attempt_count, spelling_hint_shown, answered_at,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM practice_session_answer
            WHERE session_id = #{sessionId}
              AND question_order = #{questionOrder}
              AND deleted = FALSE
            FOR UPDATE
            """)
    PracticeSessionAnswerEntity selectBySessionAndOrderForUpdate(
            @Param("sessionId") Long sessionId,
            @Param("questionOrder") Integer questionOrder
    );
}
