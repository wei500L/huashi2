package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AssessmentAiAnalysisMapper extends BaseMapper<AssessmentAiAnalysisEntity> {

    @Select("""
            SELECT id
            FROM assessment_ai_analysis
            WHERE deleted = FALSE
              AND (
                (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP))
                OR (status = 'PROCESSING' AND updated_at <= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL #{staleSeconds} SECOND))
              )
            ORDER BY id
            LIMIT #{limit}
            """)
    List<Long> selectProcessableIds(@Param("limit") int limit, @Param("staleSeconds") long staleSeconds);

    @Update("""
            UPDATE assessment_ai_analysis
            SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = FALSE
              AND (
                (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP))
                OR (status = 'PROCESSING' AND updated_at <= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL #{staleSeconds} SECOND))
              )
            """)
    int claimForProcessing(@Param("id") Long id, @Param("staleSeconds") long staleSeconds);
}
