package com.huashi.eftransfer.app.modules.lexicon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LexicalPairMapper extends BaseMapper<LexicalPairEntity> {

    @Select("""
            SELECT id, english_word, french_word, chinese_gloss, lexical_pair_type, semantic_overlap_score,
                   false_friend_risk, default_context_support, difficulty_level, notes, source, searchable_text,
                   knowledge_status, embedding_status, last_embedded_at, active,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM lexical_pair
            WHERE deleted = FALSE
              AND LOWER(english_word) = LOWER(#{englishWord})
              AND LOWER(french_word) = LOWER(#{frenchWord})
            LIMIT 1
            """)
    LexicalPairEntity selectByWords(@Param("englishWord") String englishWord, @Param("frenchWord") String frenchWord);
}
