package com.huashi.eftransfer.app.modules.diagnosis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DiagnosisSessionMapper extends BaseMapper<DiagnosisSessionEntity> {

    @Update("""
            UPDATE diagnosis_session
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
