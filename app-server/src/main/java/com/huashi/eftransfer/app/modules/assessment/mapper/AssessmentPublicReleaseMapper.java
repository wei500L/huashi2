package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublicReleaseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AssessmentPublicReleaseMapper extends BaseMapper<AssessmentPublicReleaseEntity> {

    @Update("""
            UPDATE assessment_public_release
            SET code_count = code_count + #{increment}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{releaseId} AND deleted = FALSE
            """)
    int incrementCodeCount(@Param("releaseId") Long releaseId, @Param("increment") int increment);
}
