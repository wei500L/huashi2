package com.huashi.eftransfer.app.modules.lexicon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalTagEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LexicalTagMapper extends BaseMapper<LexicalTagEntity> {

    @Select("""
            SELECT id, tag_name, description, active, created_at, created_by, updated_at, updated_by, deleted
            FROM lexical_tag
            WHERE deleted = FALSE
              AND LOWER(tag_name) = LOWER(#{tagName})
            LIMIT 1
            """)
    LexicalTagEntity selectByTagName(@Param("tagName") String tagName);
}
