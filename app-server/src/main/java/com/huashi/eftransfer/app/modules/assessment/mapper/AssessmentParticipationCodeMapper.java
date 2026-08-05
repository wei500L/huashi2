package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipationCodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssessmentParticipationCodeMapper extends BaseMapper<AssessmentParticipationCodeEntity> {

    @Select("""
            SELECT id,public_release_id,code_digest,status,export_batch_id,exported_at,first_verified_at,
                   last_verified_at,submitted_at,created_at,created_by,updated_at,updated_by,deleted
            FROM assessment_participation_code
            WHERE public_release_id = #{releaseId}
              AND code_digest = #{digest}
              AND deleted = FALSE
            LIMIT 1
            FOR UPDATE
            """)
    AssessmentParticipationCodeEntity selectByReleaseAndDigestForUpdate(
            @Param("releaseId") Long releaseId,
            @Param("digest") String digest
    );
}
