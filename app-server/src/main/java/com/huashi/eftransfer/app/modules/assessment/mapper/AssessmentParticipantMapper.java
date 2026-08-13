package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssessmentParticipantMapper extends BaseMapper<AssessmentParticipantEntity> {

    @Select("""
            SELECT id
            FROM assessment_participant
            WHERE deleted = FALSE
              AND anonymized_at IS NULL
              AND COALESCE(consented_at, created_at) <= #{deadline}
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<Long> selectExpiredIds(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);
}
