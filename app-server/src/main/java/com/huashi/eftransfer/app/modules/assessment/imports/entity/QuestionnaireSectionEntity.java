package com.huashi.eftransfer.app.modules.assessment.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("assessment_questionnaire_section")
public class QuestionnaireSectionEntity extends BaseAuditEntity {
    @TableField("questionnaire_version_id") private Long questionnaireVersionId;
    @TableField("section_code") private String sectionCode;
    private String title;
    private String description;
    @TableField("shared_material") private String sharedMaterial;
    @TableField("sort_order") private Integer sortOrder;
    @TableField("formal_section") private Boolean formalSection;
    @TableField("scored_item_count") private Integer scoredItemCount;
    public Long getQuestionnaireVersionId() { return questionnaireVersionId; }
    public void setQuestionnaireVersionId(Long value) { this.questionnaireVersionId = value; }
    public String getSectionCode() { return sectionCode; }
    public void setSectionCode(String value) { this.sectionCode = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public String getSharedMaterial() { return sharedMaterial; }
    public void setSharedMaterial(String value) { this.sharedMaterial = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { this.sortOrder = value; }
    public Boolean getFormalSection() { return formalSection; }
    public void setFormalSection(Boolean value) { this.formalSection = value; }
    public Integer getScoredItemCount() { return scoredItemCount; }
    public void setScoredItemCount(Integer value) { this.scoredItemCount = value; }
}
