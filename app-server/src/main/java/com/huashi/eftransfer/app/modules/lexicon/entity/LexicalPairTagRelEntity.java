package com.huashi.eftransfer.app.modules.lexicon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

@TableName("lexical_pair_tag_rel")
public class LexicalPairTagRelEntity extends BaseAuditEntity {

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    @TableField("lexical_tag_id")
    private Long lexicalTagId;

    public Long getLexicalPairId() {
        return lexicalPairId;
    }

    public void setLexicalPairId(Long lexicalPairId) {
        this.lexicalPairId = lexicalPairId;
    }

    public Long getLexicalTagId() {
        return lexicalTagId;
    }

    public void setLexicalTagId(Long lexicalTagId) {
        this.lexicalTagId = lexicalTagId;
    }
}
