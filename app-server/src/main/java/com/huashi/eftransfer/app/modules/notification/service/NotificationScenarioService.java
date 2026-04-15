package com.huashi.eftransfer.app.modules.notification.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.integration.ai.dto.AiGatewayHealthResponse;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
                    student.getDisplayName() + " 已完成诊断，负迁移风险 "
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
            TeachingClassEntity teachingClass
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
                "ASSESSMENT_SUBMITTED",
                "INFO",
                "学生已提交课堂测评",
                student.getDisplayName() + " 已提交《" + publish.getPaperTitleSnapshot() + "》，可查看作答结果。",
                "/teacher/assessments/attempts/" + attempt.getId() + "/result",
                "查看结果",
                "{\"attemptId\":" + attempt.getId() + ",\"publishId\":" + publish.getId()
                        + ",\"studentUserId\":" + attempt.getStudentUserId() + "}"
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
        return user == null ? fallback : user.getDisplayName();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }
}
