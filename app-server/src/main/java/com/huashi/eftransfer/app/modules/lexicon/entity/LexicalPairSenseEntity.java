package com.huashi.eftransfer.app.modules.lexicon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("lexical_pair_sense")
public class LexicalPairSenseEntity extends BaseAuditEntity {

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    private Integer sortOrder;
    private String englishDefinition;
    private String frenchDefinition;
    private String chineseDefinition;
    private String searchableText;

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

    public String getEnglishDefinition() {
        return englishDefinition;
    }

    public void setEnglishDefinition(String englishDefinition) {
        this.englishDefinition = englishDefinition;
    }

    public String getFrenchDefinition() {
        return frenchDefinition;
    }

    public void setFrenchDefinition(String frenchDefinition) {
        this.frenchDefinition = frenchDefinition;
    }

    public String getChineseDefinition() {
        return chineseDefinition;
    }

    public void setChineseDefinition(String chineseDefinition) {
        this.chineseDefinition = chineseDefinition;
    }

    public String getSearchableText() {
        return searchableText;
    }

    public void setSearchableText(String searchableText) {
        this.searchableText = searchableText;
    }
}
