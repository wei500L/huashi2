package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchExportJobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ResearchExportJobMapper extends BaseMapper<ResearchExportJobEntity> {

    @Select("""
            SELECT id
            FROM research_export_job
            WHERE deleted = FALSE
              AND (
                status = 'PENDING'
                OR (status = 'PROCESSING' AND updated_at <= #{staleBefore})
              )
            ORDER BY id
            LIMIT #{limit}
            """)
    List<Long> selectProcessableIds(@Param("limit") int limit, @Param("staleBefore") java.time.LocalDateTime staleBefore);

    @Update("""
            UPDATE research_export_job
            SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = FALSE
              AND (
                status = 'PENDING'
                OR (status = 'PROCESSING' AND updated_at <= #{staleBefore})
              )
            """)
    int claimForProcessing(@Param("id") Long id, @Param("staleBefore") java.time.LocalDateTime staleBefore);
}
