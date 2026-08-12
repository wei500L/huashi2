package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchAiReportEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ResearchAiReportMapper extends BaseMapper<ResearchAiReportEntity> {

    @Select("""
            SELECT id
            FROM research_ai_report
            WHERE deleted = FALSE
              AND (
                (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP))
                OR (status = 'PROCESSING' AND updated_at <= #{staleBefore})
              )
            ORDER BY id
            LIMIT #{limit}
            """)
    List<Long> selectProcessableIds(@Param("limit") int limit, @Param("staleBefore") java.time.LocalDateTime staleBefore);

    @Update("""
            UPDATE research_ai_report
            SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = FALSE
              AND (
                (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP))
                OR (status = 'PROCESSING' AND updated_at <= #{staleBefore})
              )
            """)
    int claimForProcessing(@Param("id") Long id, @Param("staleBefore") java.time.LocalDateTime staleBefore);
}
