package com.huashi.eftransfer.app.modules.notification.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.shared.ai.AiGatewayHealthResponse;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.service.TeachingClassService;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublishEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublishRecipientEntity;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEvent;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.app.modules.user.service.DisplayNameNormalizer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationScenarioService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final NotificationService notificationService;
    private final TeachingClassService teachingClassService;
    private final TeachingClassMapper teachingClassMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public NotificationScenarioService(
            NotificationService notificationService,
            TeachingClassService teachingClassService,
            TeachingClassMapper teachingClassMapper,
            UserMapper userMapper,
            UserRoleMapper userRoleMapper
    ) {
        this.notificationService = notificationService;
        this.teachingClassService = teachingClassService;
        this.teachingClassMapper = teachingClassMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @EventListener
    public void onDiagnosisCompleted(DiagnosisCompletedEvent event) {
        notifyDiagnosisCompleted(event);
    }

    public void notifyDiagnosisCompleted(DiagnosisCompletedEvent event) {
        UserEntity student = userMapper.selectById(event.ownerUserId());
        if (student == null) {
            return;
        }
        List<Long> classIds = teachingClassService.listActiveClassIdsByStudent(
                event.ownerUserId(),
                event.completedAt() == null ? LocalDateTime.now() : event.completedAt()
        );
        if (classIds.isEmpty()) {
            return;
        }
        List<TeachingClassEntity> classes = teachingClassMapper.selectBatchIds(classIds);
        List<NotificationCreateCommand> commands = new ArrayList<>();
        for (TeachingClassEntity teachingClass : classes) {
            if (teachingClass == null || teachingClass.getTeacherUserId() == null) {
                continue;
            }
            commands.add(new NotificationCreateCommand(
                    teachingClass.getTeacherUserId(),
                    "DIAGNOSIS_COMPLETED",
                    "INFO",
                    "学生已完成诊断",
                    displayName(student.getDisplayName(), "学生") + " 已完成诊断，负迁移风险 "
                            + Math.round(event.negativeTransferRisk() * 100)
                            + "%，可进入学情详情查看结果。",
                    "/teacher/classes/" + teachingClass.getId() + "/students/" + event.ownerUserId(),
                    "查看学生详情",
                    "{\"studentUserId\":" + event.ownerUserId() + ",\"classId\":" + teachingClass.getId()
                            + ",\"summaryId\":" + event.summaryId() + "}"
            ));
        }
        notificationService.createBatch(commands);
    }

    public void notifyAssessmentPublished(
            AssessmentPublishEntity publish,
            TeachingClassEntity teachingClass,
            List<AssessmentPublishRecipientEntity> recipients
    ) {
        if (publish == null || teachingClass == null || recipients == null || recipients.isEmpty()) {
            return;
        }
        String teacherName = displayName(publish.getPublishedBy(), "教师");
        String dueLabel = formatDateTime(publish.getDueAt());
        List<NotificationCreateCommand> commands = recipients.stream()
                .map(recipient -> new NotificationCreateCommand(
                        recipient.getStudentUserId(),
                        "ASSESSMENT_PUBLISHED",
                        "INFO",
                        "你有新的课堂测评",
                        teacherName + " 已发布《" + publish.getPaperTitleSnapshot() + "》"
                                + (dueLabel == null ? "" : "，截止时间 " + dueLabel)
                                + "。",
                        "/assessments",
                        "查看测评",
                        "{\"publishId\":" + publish.getId() + ",\"paperId\":" + publish.getPaperId()
                                + ",\"teachingClassId\":" + publish.getTeachingClassId() + "}"
                ))
                .toList();
        notificationService.createBatch(commands);
    }

    public void notifyAssessmentSubmitted(
            AssessmentAttemptEntity attempt,
            AssessmentPublishEntity publish,
            TeachingClassEntity teachingClass,
            boolean timeoutSubmitted
    ) {
        if (attempt == null || publish == null || teachingClass == null || teachingClass.getTeacherUserId() == null) {
            return;
        }
        UserEntity student = userMapper.selectById(attempt.getStudentUserId());
        if (student == null) {
            return;
        }
        notificationService.create(new NotificationCreateCommand(
                teachingClass.getTeacherUserId(),
                timeoutSubmitted ? "ASSESSMENT_TIMEOUT_SUBMITTED" : "ASSESSMENT_SUBMITTED",
                "INFO",
                timeoutSubmitted ? "学生测评已超时自动交卷" : "学生已提交课堂测评",
                timeoutSubmitted
                        ? displayName(student.getDisplayName(), "学生") + " 的《" + publish.getPaperTitleSnapshot() + "》已在截止后由系统自动交卷，可查看作答结果。"
                        : displayName(student.getDisplayName(), "学生") + " 已提交《" + publish.getPaperTitleSnapshot() + "》，可查看作答结果。",
                "/teacher/assessments/attempts/" + attempt.getId() + "/result",
                "查看结果",
                "{\"attemptId\":" + attempt.getId() + ",\"publishId\":" + publish.getId()
                        + ",\"studentUserId\":" + attempt.getStudentUserId()
                        + ",\"timeoutSubmitted\":" + timeoutSubmitted + "}"
        ));
    }

    public void notifyAiGatewayUnhealthy(String reason) {
        List<Long> adminIds = listAdminUserIds();
        if (adminIds.isEmpty()) {
            return;
        }
        notificationService.createBatch(adminIds.stream()
                .map(adminId -> new NotificationCreateCommand(
                        adminId,
                        "SYSTEM_ALERT",
                        "ERROR",
                        "系统异常告警",
                        "AI 网关健康检查失败" + (reason == null || reason.isBlank() ? "。" : "：" + reason),
                        "/admin/config-center",
                        "查看配置中心",
                        null
                ))
                .toList());
    }

    public void notifyAiGatewayRecovered(AiGatewayHealthResponse healthResponse) {
        List<Long> adminIds = listAdminUserIds();
        if (adminIds.isEmpty()) {
            return;
        }
        String provider = healthResponse == null || healthResponse.provider() == null
                ? "unknown"
                : healthResponse.provider();
        notificationService.createBatch(adminIds.stream()
                .map(adminId -> new NotificationCreateCommand(
                        adminId,
                        "SYSTEM_RECOVERY",
                        "SUCCESS",
                        "系统已恢复",
                        "AI 网关健康检查恢复正常，当前 Provider：" + provider + "。",
                        "/admin/config-center",
                        "查看配置中心",
                        null
                ))
                .toList());
    }

    public void notifyAiRuntimeSyncRetrying(Long targetVersion, String reason, OffsetDateTime nextAttemptAt) {
        List<Long> adminIds = listAdminUserIds();
        if (adminIds.isEmpty() || targetVersion == null) {
            return;
        }
        String nextAttemptLabel = formatDateTime(nextAttemptAt);
        notificationService.createBatch(adminIds.stream()
                .map(adminId -> new NotificationCreateCommand(
                        adminId,
                        "AI_RUNTIME_SYNC_RETRYING",
                        "WARNING",
                        "AI 配置运行态同步待重试",
                        "AI 配置版本 v" + targetVersion + " 尚未同步到 ai-gateway 运行态。"
                                + (reason == null || reason.isBlank() ? "" : " 原因：" + reason)
                                + (nextAttemptLabel == null ? "" : " 下次重试：" + nextAttemptLabel)
                                + "。",
                        "/admin/config-center",
                        "查看配置中心",
                        "{\"targetVersion\":" + targetVersion + "}"
                ))
                .toList());
    }

    public void notifyAiRuntimeSyncDlq(Long targetVersion, String reason) {
        List<Long> adminIds = listAdminUserIds();
        if (adminIds.isEmpty() || targetVersion == null) {
            return;
        }
        notificationService.createBatch(adminIds.stream()
                .map(adminId -> new NotificationCreateCommand(
                        adminId,
                        "AI_RUNTIME_SYNC_DLQ",
                        "ERROR",
                        "AI 配置运行态同步失败",
                        "AI 配置版本 v" + targetVersion + " 已进入终态失败队列。"
                                + (reason == null || reason.isBlank() ? "" : " 原因：" + reason)
                                + "，需要人工重放。",
                        "/admin/config-center",
                        "查看配置中心",
                        "{\"targetVersion\":" + targetVersion + "}"
                ))
                .toList());
    }

    public void notifyAiRuntimeSyncRecovered(Long targetVersion) {
        List<Long> adminIds = listAdminUserIds();
        if (adminIds.isEmpty() || targetVersion == null) {
            return;
        }
        notificationService.createBatch(adminIds.stream()
                .map(adminId -> new NotificationCreateCommand(
                        adminId,
                        "AI_RUNTIME_SYNC_RECOVERED",
                        "SUCCESS",
                        "AI 配置运行态同步已恢复",
                        "AI 配置版本 v" + targetVersion + " 已同步到 ai-gateway 运行态。",
                        "/admin/config-center",
                        "查看配置中心",
                        "{\"targetVersion\":" + targetVersion + "}"
                ))
                .toList());
    }

    private List<Long> listAdminUserIds() {
        return List.copyOf(userRoleMapper.selectList(Wrappers.<UserRoleEntity>lambdaQuery()
                        .eq(UserRoleEntity::getRoleCode, "ADMIN")
                        .orderByAsc(UserRoleEntity::getUserId))
                .stream()
                .map(UserRoleEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    private String displayName(Long userId, String fallback) {
        if (userId == null) {
            return fallback;
        }
        UserEntity user = userMapper.selectById(userId);
        return user == null ? fallback : displayName(user.getDisplayName(), fallback);
    }

    private String displayName(String rawDisplayName, String fallback) {
        if (rawDisplayName == null || rawDisplayName.isBlank()) {
            return fallback;
        }
        return DisplayNameNormalizer.normalize(rawDisplayName.trim());
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value.toLocalDateTime());
    }
}
