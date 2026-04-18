package com.huashi.eftransfer.app.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.training.entity.TrainingSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrainingSessionMapper extends BaseMapper<TrainingSessionEntity> {

    @Update("""
            UPDATE training_session
            SET answered_items = answered_items + 1,
                current_item_order = #{currentItemOrder},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{sessionId}
              AND status = #{status}
              AND deleted = FALSE
            """)
    int incrementAnsweredItems(
            @Param("sessionId") Long sessionId,
            @Param("currentItemOrder") Integer currentItemOrder,
            @Param("status") String status
    );
}
