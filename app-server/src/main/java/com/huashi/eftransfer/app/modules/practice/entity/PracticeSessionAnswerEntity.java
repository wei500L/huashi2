package com.huashi.eftransfer.app.modules.practice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

/**
 * One question of a practice session, snapshotted from the question bank at
 * session start so later bank edits never alter an in-progress practice.
 */
@TableName("practice_session_answer")
public class PracticeSessionAnswerEntity extends BaseAuditEntity {

    @TableField("session_id")
    private Long sessionId;

    @TableField("question_order")
    private Integer questionOrder;

    @TableField("question_version_id")
    private Long questionVersionId;

    @TableField("question_code")
    private String questionCode;

    @TableField("question_type")
    private String questionType;

    @TableField("section_code")
    private String sectionCode;

    @TableField("construct_code")
    private String constructCode;

    @TableField("transfer_category")
    private String transferCategory;

    @TableField("target_word")
    private String targetWord;

    @TableField("stem_text_snapshot")
    private String stemTextSnapshot;

    @TableField("prompt_text_snapshot")
    private String promptTextSnapshot;

    @TableField("options_json_snapshot")
    private String optionsJsonSnapshot;

    @TableField("correct_answer_json")
    private String correctAnswerJson;

    @TableField("explanation_text_snapshot")
    private String explanationTextSnapshot;

    @TableField("option_explanations_json")
    private String optionExplanationsJson;

    @TableField("response_json")
    private String responseJson;

    @TableField("is_correct")
    private Boolean isCorrect;

    @TableField("wrong_attempt_count")
    private Integer wrongAttemptCount;

    @TableField("spelling_hint_shown")
    private Boolean spellingHintShown;

    @TableField("answered_at")
    private LocalDateTime answeredAt;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public Long getQuestionVersionId() {
        return questionVersionId;
    }

    public void setQuestionVersionId(Long questionVersionId) {
        this.questionVersionId = questionVersionId;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public String getConstructCode() {
        return constructCode;
    }

    public void setConstructCode(String constructCode) {
        this.constructCode = constructCode;
    }

    public String getTransferCategory() {
        return transferCategory;
    }

    public void setTransferCategory(String transferCategory) {
        this.transferCategory = transferCategory;
    }

    public String getTargetWord() {
        return targetWord;
    }

    public void setTargetWord(String targetWord) {
        this.targetWord = targetWord;
    }

    public String getStemTextSnapshot() {
        return stemTextSnapshot;
    }

    public void setStemTextSnapshot(String stemTextSnapshot) {
        this.stemTextSnapshot = stemTextSnapshot;
    }

    public String getPromptTextSnapshot() {
        return promptTextSnapshot;
    }

    public void setPromptTextSnapshot(String promptTextSnapshot) {
        this.promptTextSnapshot = promptTextSnapshot;
    }

    public String getOptionsJsonSnapshot() {
        return optionsJsonSnapshot;
    }

    public void setOptionsJsonSnapshot(String optionsJsonSnapshot) {
        this.optionsJsonSnapshot = optionsJsonSnapshot;
    }

    public String getCorrectAnswerJson() {
        return correctAnswerJson;
    }

    public void setCorrectAnswerJson(String correctAnswerJson) {
        this.correctAnswerJson = correctAnswerJson;
    }

    public String getExplanationTextSnapshot() {
        return explanationTextSnapshot;
    }

    public void setExplanationTextSnapshot(String explanationTextSnapshot) {
        this.explanationTextSnapshot = explanationTextSnapshot;
    }

    public String getOptionExplanationsJson() {
        return optionExplanationsJson;
    }

    public void setOptionExplanationsJson(String optionExplanationsJson) {
        this.optionExplanationsJson = optionExplanationsJson;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Integer getWrongAttemptCount() {
        return wrongAttemptCount;
    }

    public void setWrongAttemptCount(Integer wrongAttemptCount) {
        this.wrongAttemptCount = wrongAttemptCount;
    }

    public Boolean getSpellingHintShown() {
        return spellingHintShown;
    }

    public void setSpellingHintShown(Boolean spellingHintShown) {
        this.spellingHintShown = spellingHintShown;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}
