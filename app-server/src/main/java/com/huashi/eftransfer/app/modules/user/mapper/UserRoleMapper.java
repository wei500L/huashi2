package com.huashi.eftransfer.app.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {

    @Delete("""
            DELETE FROM user_role
            WHERE user_id = #{userId}
            """)
    int hardDeleteByUserId(@Param("userId") Long userId);
}
