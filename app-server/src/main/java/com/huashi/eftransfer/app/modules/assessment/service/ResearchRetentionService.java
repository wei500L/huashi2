package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentSubmissionFileEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantAccessMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentSubmissionFileMapper;
import com.huashi.eftransfer.shared.enums.AssessmentFileBindingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ResearchRetentionService {

    private static final Logger log = LoggerFactory.getLogger(ResearchRetentionService.class);
    private static final int BATCH_SIZE = 100;

    private final ResearchAnalyticsProperties properties;
    private final AssessmentParticipantMapper participantMapper;
    private final AssessmentParticipantAccessMapper accessMapper;
    private final AssessmentSubmissionFileMapper fileMapper;
    private final ObjectStorageService objectStorageService;

    public ResearchRetentionService(
            ResearchAnalyticsProperties properties,
            AssessmentParticipantMapper participantMapper,
            AssessmentParticipantAccessMapper accessMapper,
            AssessmentSubmissionFileMapper fileMapper,
            ObjectStorageService objectStorageService
    ) {
        this.properties = properties;
        this.participantMapper = participantMapper;
        this.accessMapper = accessMapper;
        this.fileMapper = fileMapper;
        this.objectStorageService = objectStorageService;
    }

    @Scheduled(fixedDelayString = "PT30M")
    @Transactional
    public void purgeExpired() {
        Duration retention = properties.getRetention();
        if (retention == null || retention.isNegative() || retention.isZero()) {
            return;
        }
        LocalDateTime deadline = LocalDateTime.now().minus(retention);
        int participants = anonymizeExpiredParticipants(deadline);
        int accessRows = deleteExpiredAccessLogs(deadline);
        int files = deleteExpiredBoundFiles(deadline);
        if (participants > 0 || accessRows > 0 || files > 0) {
            log.info(
                    "event=research_retention_purged participants={} accessRows={} files={} deadline={}",
                    participants,
                    accessRows,
                    files,
                    deadline
            );
        }
    }

    int anonymizeExpiredParticipants(LocalDateTime deadline) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Long id : participantMapper.selectExpiredIds(deadline, BATCH_SIZE)) {
            AssessmentParticipantEntity participant = participantMapper.selectById(id);
            if (participant == null || participant.getAnonymizedAt() != null) {
                continue;
            }
            participantMapper.update(
                    null,
                    Wrappers.<AssessmentParticipantEntity>lambdaUpdate()
                            .eq(AssessmentParticipantEntity::getId, id)
                            .set(AssessmentParticipantEntity::getSensitiveProfileCiphertext, null)
                            .set(AssessmentParticipantEntity::getSensitiveProfileIv, null)
                            .set(AssessmentParticipantEntity::getSensitiveProfileKeyVersion, null)
                            .set(AssessmentParticipantEntity::getAnonymizedAt, now)
            );
            count++;
        }
        return count;
    }

    int deleteExpiredAccessLogs(LocalDateTime deadline) {
        int count = 0;
        for (Long id : accessMapper.selectExpiredIds(deadline, BATCH_SIZE)) {
            count += accessMapper.deleteExpiredById(id);
        }
        return count;
    }

    int deleteExpiredBoundFiles(LocalDateTime deadline) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Long id : fileMapper.selectExpiredBoundIds(deadline, BATCH_SIZE)) {
            AssessmentSubmissionFileEntity file = fileMapper.selectById(id);
            if (file == null || !AssessmentFileBindingStatus.BOUND.name().equals(file.getBindingStatus())) {
                continue;
            }
            if (file.getObjectKey() != null) {
                objectStorageService.delete(file.getObjectKey());
            }
            file.setBindingStatus(AssessmentFileBindingStatus.DELETED.name());
            file.setDeletedAt(now);
            file.setDeleted(Boolean.TRUE);
            fileMapper.updateById(file);
            count++;
        }
        return count;
    }
}
