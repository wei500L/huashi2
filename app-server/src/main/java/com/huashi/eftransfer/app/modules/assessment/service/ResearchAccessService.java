package com.huashi.eftransfer.app.modules.assessment.service;

import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPaperEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublicReleaseEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublishEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentSubmissionFileEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPaperMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublicReleaseMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublishMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentSubmissionFileMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.AssessmentDeliveryMode;
import com.huashi.eftransfer.shared.enums.AssessmentPaperPurpose;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ResearchAccessService {

    private final AssessmentPublishMapper publishMapper;
    private final AssessmentPaperMapper paperMapper;
    private final AssessmentPublicReleaseMapper publicReleaseMapper;
    private final AssessmentAttemptMapper attemptMapper;
    private final AssessmentSubmissionFileMapper fileMapper;

    public ResearchAccessService(
            AssessmentPublishMapper publishMapper,
            AssessmentPaperMapper paperMapper,
            AssessmentPublicReleaseMapper publicReleaseMapper,
            AssessmentAttemptMapper attemptMapper,
            AssessmentSubmissionFileMapper fileMapper
    ) {
        this.publishMapper = publishMapper;
        this.paperMapper = paperMapper;
        this.publicReleaseMapper = publicReleaseMapper;
        this.attemptMapper = attemptMapper;
        this.fileMapper = fileMapper;
    }

    public ResearchPublishAccess requireAccessibleResearchPublish(Long publishId) {
        AssessmentPublishEntity publish = publishMapper.selectById(publishId);
        if (publish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Research publish was not found", 404);
        }
        if (!AssessmentDeliveryMode.PUBLIC_CODE.name().equalsIgnoreCase(publish.getDeliveryMode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Publish is not a public research release", 403);
        }
        AssessmentPaperEntity paper = paperMapper.selectById(publish.getPaperId());
        if (paper == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Research paper was not found", 404);
        }
        if (AssessmentPaperPurpose.fromCode(paper.getPaperPurpose()) != AssessmentPaperPurpose.RESEARCH_SURVEY) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Paper is not a research survey", 403);
        }
        Long currentUserId = currentUserId();
        boolean owner = Objects.equals(paper.getOwnerUserId(), currentUserId);
        if (!owner && !isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have access to this research publish", 403);
        }
        AssessmentPublicReleaseEntity release = publicReleaseMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<AssessmentPublicReleaseEntity>lambdaQuery()
                        .eq(AssessmentPublicReleaseEntity::getPublishId, publish.getId())
                        .last("LIMIT 1")
        );
        if (release == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Public research release was not found", 404);
        }
        return new ResearchPublishAccess(publish, paper, release, owner);
    }

    public ResearchAttemptAccess requireAccessibleResearchAttempt(Long attemptId) {
        AssessmentAttemptEntity attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Research attempt was not found", 404);
        }
        ResearchPublishAccess publishAccess = requireAccessibleResearchPublish(attempt.getPublishId());
        return new ResearchAttemptAccess(attempt, publishAccess);
    }

    public ResearchFileAccess requireAccessibleResearchFile(Long fileId) {
        AssessmentSubmissionFileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Research attachment was not found", 404);
        }
        ResearchAttemptAccess attemptAccess = requireAccessibleResearchAttempt(file.getAttemptId());
        return new ResearchFileAccess(file, attemptAccess);
    }

    public boolean canViewSensitiveFields(ResearchPublishAccess access) {
        return access.owner();
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private boolean isAdmin() {
        return SecurityUtils.getCurrentPrincipal()
                .map(principal -> principal.roles().contains("ADMIN"))
                .orElse(false);
    }

    public record ResearchPublishAccess(
            AssessmentPublishEntity publish,
            AssessmentPaperEntity paper,
            AssessmentPublicReleaseEntity release,
            boolean owner
    ) {
    }

    public record ResearchAttemptAccess(
            AssessmentAttemptEntity attempt,
            ResearchPublishAccess publishAccess
    ) {
    }

    public record ResearchFileAccess(
            AssessmentSubmissionFileEntity file,
            ResearchAttemptAccess attemptAccess
    ) {
    }
}
