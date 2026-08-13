package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantAccessEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssessmentParticipantAccessMapper extends BaseMapper<AssessmentParticipantAccessEntity> {

    @Select("""
            SELECT id
            FROM assessment_participant_access
            WHERE deleted = FALSE
              AND accessed_at <= #{deadline}
            ORDER BY accessed_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<Long> selectExpiredIds(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);

    @Delete("DELETE FROM assessment_participant_access WHERE id = #{id}")
    int deleteExpiredById(@Param("id") Long id);
}
