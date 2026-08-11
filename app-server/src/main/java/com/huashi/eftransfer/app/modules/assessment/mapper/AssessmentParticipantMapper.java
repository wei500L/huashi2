package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssessmentParticipantMapper extends BaseMapper<AssessmentParticipantEntity> {
    @Select("SELECT * FROM assessment_participant WHERE id = #{id} AND deleted = FALSE FOR UPDATE")
    AssessmentParticipantEntity selectByIdForUpdate(@Param("id") Long id);
}
