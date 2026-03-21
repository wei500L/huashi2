package com.huashi.eftransfer.app.modules.lexicon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("lexical_pair_example")
public class LexicalPairExampleEntity extends BaseAuditEntity {

    @TableField("lexical_pair_sense_id")
    private Long lexicalPairSenseId;

    private Integer sortOrder;
    private String englishExample;
    private String frenchExample;
    private String chineseTranslation;
    private String contextSupportLevel;
    private String source;
    private String searchableText;

    public Long getLexicalPairSenseId() {
        return lexicalPairSenseId;
    }

    public void setLexicalPairSenseId(Long lexicalPairSenseId) {
        this.lexicalPairSenseId = lexicalPairSenseId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getEnglishExample() {
        return englishExample;
    }

    public void setEnglishExample(String englishExample) {
        this.englishExample = englishExample;
    }

    public String getFrenchExample() {
        return frenchExample;
    }

    public void setFrenchExample(String frenchExample) {
        this.frenchExample = frenchExample;
    }

    public String getChineseTranslation() {
        return chineseTranslation;
    }

    public void setChineseTranslation(String chineseTranslation) {
        this.chineseTranslation = chineseTranslation;
    }

    public String getContextSupportLevel() {
        return contextSupportLevel;
    }

    public void setContextSupportLevel(String contextSupportLevel) {
        this.contextSupportLevel = contextSupportLevel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSearchableText() {
        return searchableText;
    }

    public void setSearchableText(String searchableText) {
        this.searchableText = searchableText;
    }
}
