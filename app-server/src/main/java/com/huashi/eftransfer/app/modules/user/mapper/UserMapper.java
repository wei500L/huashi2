package com.huashi.eftransfer.app.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("""
            SELECT id, username, email, password_hash, display_name, enabled, last_login_at,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM users
            WHERE deleted = FALSE
              AND (LOWER(username) = LOWER(#{loginId}) OR LOWER(email) = LOWER(#{loginId}))
            LIMIT 1
            """)
    UserEntity selectByUsernameOrEmail(@Param("loginId") String loginId);

    @Select("""
            SELECT id, username, email, password_hash, display_name, enabled, last_login_at,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM users
            WHERE deleted = FALSE
              AND LOWER(username) = LOWER(#{username})
            LIMIT 1
            """)
    UserEntity selectByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, email, password_hash, display_name, enabled, last_login_at,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM users
            WHERE deleted = FALSE
              AND LOWER(email) = LOWER(#{email})
            LIMIT 1
            """)
    UserEntity selectByEmail(@Param("email") String email);
}
