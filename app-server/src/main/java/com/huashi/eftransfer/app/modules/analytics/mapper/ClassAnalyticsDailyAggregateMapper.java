package com.huashi.eftransfer.app.modules.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.analytics.entity.ClassAnalyticsDailyAggregateEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface ClassAnalyticsDailyAggregateMapper extends BaseMapper<ClassAnalyticsDailyAggregateEntity> {

    @Delete("""
            DELETE FROM class_analytics_daily_aggregate
            WHERE teaching_class_id = #{classId}
              AND stat_date = #{statDate}
              AND source_type = #{sourceType}
            """)
    int hardDeleteByClassDateAndSource(
            @Param("classId") Long classId,
            @Param("statDate") LocalDate statDate,
            @Param("sourceType") String sourceType
    );
}
