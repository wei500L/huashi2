package com.huashi.eftransfer.app.modules.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.analytics.entity.AnalyticsDailyAggregateEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface AnalyticsDailyAggregateMapper extends BaseMapper<AnalyticsDailyAggregateEntity> {

    @Delete("""
            DELETE FROM analytics_daily_aggregate
            WHERE owner_user_id = #{ownerUserId}
              AND stat_date = #{statDate}
              AND source_type = #{sourceType}
            """)
    int hardDeleteByOwnerDateAndSource(
            @Param("ownerUserId") Long ownerUserId,
            @Param("statDate") LocalDate statDate,
            @Param("sourceType") String sourceType
    );
}
