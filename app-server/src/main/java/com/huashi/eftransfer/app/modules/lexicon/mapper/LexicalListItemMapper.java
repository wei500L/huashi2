package com.huashi.eftransfer.app.modules.lexicon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalListItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LexicalListItemMapper extends BaseMapper<LexicalListItemEntity> {

    @Select("""
            SELECT COALESCE(MAX(sort_order), 0)
            FROM lexical_list_item
            WHERE deleted = FALSE
              AND lexical_list_id = #{lexicalListId}
            """)
    Integer selectMaxSortOrder(@Param("lexicalListId") Long lexicalListId);
}
