package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentSubmissionFileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssessmentSubmissionFileMapper extends BaseMapper<AssessmentSubmissionFileEntity> {

    @Select("""
            SELECT id
            FROM assessment_submission_file
            WHERE deleted = FALSE
              AND binding_status = 'TEMPORARY'
              AND uploaded_at <= #{deadline}
            ORDER BY uploaded_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<Long> selectOrphanCandidateIds(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);

    @Select("""
            SELECT id
            FROM assessment_submission_file
            WHERE deleted = FALSE
              AND binding_status = 'BOUND'
              AND COALESCE(bound_at, uploaded_at) <= #{deadline}
            ORDER BY uploaded_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<Long> selectExpiredBoundIds(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);
}
