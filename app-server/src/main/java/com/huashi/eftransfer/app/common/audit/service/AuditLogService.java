package com.huashi.eftransfer.app.common.audit.service;

import com.huashi.eftransfer.app.common.audit.entity.AuditLogEntity;
import com.huashi.eftransfer.app.common.audit.mapper.AuditLogMapper;
import com.huashi.eftransfer.app.common.trace.TraceIdSupport;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void record(String actionType, String targetType, String targetId, Object requestPayload, String responseCode) {
        HttpServletRequest request = currentRequest();
        AuditLogEntity entity = new AuditLogEntity();
        entity.setActorUserId(SecurityUtils.getCurrentUserId().orElse(null));
        entity.setActionType(actionType);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setTraceId(TraceIdSupport.currentOrResolve(request));
        entity.setRequestPayload(serializeSafely(requestPayload));
        entity.setResponseCode(responseCode);
        entity.setRequestPath(request == null ? "N/A" : request.getRequestURI());
        entity.setRequestMethod(request == null ? "N/A" : request.getMethod());
        auditLogMapper.insert(entity);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String serializeSafely(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            return "{\"serializationError\":true}";
        }
    }
}
