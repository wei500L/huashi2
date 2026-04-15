package com.huashi.eftransfer.app.modules.lexicon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("lexical_pair")
public class LexicalPairEntity extends BaseAuditEntity {

    private String englishWord;
    private String frenchWord;
    private String chineseGloss;
    private String lexicalPairType;
    private BigDecimal semanticOverlapScore;
    private BigDecimal falseFriendRisk;

    @TableField("default_context_support")
    private String defaultContextSupport;

    private Integer difficultyLevel;
    private String notes;
    private String source;
    private String searchableText;
    private String searchPinyin;
    private String searchInitials;
    private String knowledgeStatus;
    private String embeddingStatus;

    @TableField("last_embedded_at")
    private LocalDateTime lastEmbeddedAt;

    private Boolean active;

    public String getEnglishWord() {
        return englishWord;
    }

    public void setEnglishWord(String englishWord) {
        this.englishWord = englishWord;
    }

    public String getFrenchWord() {
        return frenchWord;
    }

    public void setFrenchWord(String frenchWord) {
        this.frenchWord = frenchWord;
    }

    public String getChineseGloss() {
        return chineseGloss;
    }

    public void setChineseGloss(String chineseGloss) {
        this.chineseGloss = chineseGloss;
    }

    public String getLexicalPairType() {
        return lexicalPairType;
    }

    public void setLexicalPairType(String lexicalPairType) {
        this.lexicalPairType = lexicalPairType;
    }

    public BigDecimal getSemanticOverlapScore() {
        return semanticOverlapScore;
    }

    public void setSemanticOverlapScore(BigDecimal semanticOverlapScore) {
        this.semanticOverlapScore = semanticOverlapScore;
    }

    public BigDecimal getFalseFriendRisk() {
        return falseFriendRisk;
    }

    public void setFalseFriendRisk(BigDecimal falseFriendRisk) {
        this.falseFriendRisk = falseFriendRisk;
    }

    public String getDefaultContextSupport() {
        return defaultContextSupport;
    }

    public void setDefaultContextSupport(String defaultContextSupport) {
        this.defaultContextSupport = defaultContextSupport;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getSearchPinyin() {
        return searchPinyin;
    }

    public void setSearchPinyin(String searchPinyin) {
        this.searchPinyin = searchPinyin;
    }

    public String getSearchInitials() {
        return searchInitials;
    }

    public void setSearchInitials(String searchInitials) {
        this.searchInitials = searchInitials;
    }

    public String getKnowledgeStatus() {
        return knowledgeStatus;
    }

    public void setKnowledgeStatus(String knowledgeStatus) {
        this.knowledgeStatus = knowledgeStatus;
    }

    public String getEmbeddingStatus() {
        return embeddingStatus;
    }

    public void setEmbeddingStatus(String embeddingStatus) {
        this.embeddingStatus = embeddingStatus;
    }

    public LocalDateTime getLastEmbeddedAt() {
        return lastEmbeddedAt;
    }

    public void setLastEmbeddedAt(LocalDateTime lastEmbeddedAt) {
        this.lastEmbeddedAt = lastEmbeddedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
