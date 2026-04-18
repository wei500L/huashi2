package com.huashi.eftransfer.app.common.audit;

import com.huashi.eftransfer.app.common.audit.entity.AuditLogEntity;
import com.huashi.eftransfer.app.common.audit.mapper.AuditLogMapper;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuditLogControllerIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldPageAuditLogsWithAdminFilters() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        UserEntity admin = userMapper.selectByUsernameOrEmail("admin");
        UserEntity teacher = userMapper.selectByUsernameOrEmail("teacher.zhang");

        insertAuditLog(admin.getId(), "admin_access_update", "user", "2", "/api/admin/users/2/access", "PUT", "SUCCESS", LocalDateTime.of(2026, 4, 15, 12, 0));
        insertAuditLog(teacher.getId(), "template_create", "diagnosis_template", "18", "/api/teacher/diagnosis-templates", "POST", "SUCCESS", LocalDateTime.of(2026, 4, 15, 10, 0));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .with(bearer(adminToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("userKeyword", "teacher")
                        .param("actionType", "template")
                        .param("startAt", "2026-04-15T09:00")
                        .param("endAt", "2026-04-15T11:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].actorUserId").value(teacher.getId()))
                .andExpect(jsonPath("$.data.records[0].actorUsername").value("teacher.zhang"))
                .andExpect(jsonPath("$.data.records[0].actionType").value("template_create"))
                .andExpect(jsonPath("$.data.records[0].requestPath").value("/api/teacher/diagnosis-templates"));
    }

    @Test
    void shouldRejectInvalidTimeRange() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        mockMvc.perform(get("/api/admin/audit-logs")
                        .with(bearer(adminToken))
                        .param("startAt", "2026-04-15T12:00")
                        .param("endAt", "2026-04-15T09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private void insertAuditLog(
            Long actorUserId,
            String actionType,
            String targetType,
            String targetId,
            String requestPath,
            String requestMethod,
            String responseCode,
            LocalDateTime createdAt
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setActorUserId(actorUserId);
        entity.setActionType(actionType);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setRequestPath(requestPath);
        entity.setRequestMethod(requestMethod);
        entity.setTraceId("trace-" + actionType);
        entity.setRequestPayload("{\"sample\":true}");
        entity.setResponseCode(responseCode);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        auditLogMapper.insert(entity);
    }
}
