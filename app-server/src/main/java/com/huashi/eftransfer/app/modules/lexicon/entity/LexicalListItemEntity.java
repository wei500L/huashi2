package com.huashi.eftransfer.app.modules.lexicon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

@TableName("lexical_list_item")
public class LexicalListItemEntity extends BaseAuditEntity {

    @TableField("lexical_list_id")
    private Long lexicalListId;

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    private Integer sortOrder;
    private String notes;

    public Long getLexicalListId() {
        return lexicalListId;
    }

    public void setLexicalListId(Long lexicalListId) {
        this.lexicalListId = lexicalListId;
    }

    public Long getLexicalPairId() {
        return lexicalPairId;
    }

    public void setLexicalPairId(Long lexicalPairId) {
        this.lexicalPairId = lexicalPairId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
