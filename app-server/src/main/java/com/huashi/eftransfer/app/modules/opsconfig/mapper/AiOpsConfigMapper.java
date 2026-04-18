package com.huashi.eftransfer.app.modules.opsconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.opsconfig.entity.AiOpsConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiOpsConfigMapper extends BaseMapper<AiOpsConfigEntity> {

    @Select("""
            SELECT id, config_key, encrypted_config, version_number,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM admin_ai_config
            WHERE deleted = FALSE
              AND config_key = #{configKey}
            LIMIT 1
            """)
    AiOpsConfigEntity selectByConfigKey(@Param("configKey") String configKey);

    @Update("""
            UPDATE admin_ai_config
            SET encrypted_config = #{encryptedConfig},
                version_number = #{nextVersion},
                updated_at = CURRENT_TIMESTAMP,
                updated_by = #{updatedBy}
            WHERE deleted = FALSE
              AND config_key = #{configKey}
              AND version_number = #{expectedVersion}
            """)
    int updateByConfigKeyAndVersion(
            @Param("configKey") String configKey,
            @Param("encryptedConfig") String encryptedConfig,
            @Param("nextVersion") Long nextVersion,
            @Param("expectedVersion") Long expectedVersion,
            @Param("updatedBy") Long updatedBy
    );
}
