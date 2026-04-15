package com.huashi.eftransfer.app.common.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.dto.AdminAuditLogPageQuery;
import com.huashi.eftransfer.app.common.audit.entity.AuditLogEntity;
import com.huashi.eftransfer.app.common.audit.mapper.AuditLogMapper;
import com.huashi.eftransfer.app.common.audit.vo.AdminAuditLogItemVO;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminAuditLogQueryService {

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;

    public AdminAuditLogQueryService(AuditLogMapper auditLogMapper, UserMapper userMapper) {
        this.auditLogMapper = auditLogMapper;
        this.userMapper = userMapper;
    }

    public PageResult<AdminAuditLogItemVO> page(AdminAuditLogPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        LocalDateTime startAt = parseDateTime(query.startAt(), "startAt");
        LocalDateTime endAt = parseDateTime(query.endAt(), "endAt");
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "endAt must be greater than or equal to startAt", 400);
        }

        Set<Long> filteredActorIds = resolveFilteredActorIds(query.userKeyword());
        if (filteredActorIds != null && filteredActorIds.isEmpty()) {
            return new PageResult<>(0, pageQuery.pageNo(), pageQuery.pageSize(), List.of());
        }

        long total = auditLogMapper.selectCount(buildQuery(query, startAt, endAt, filteredActorIds));
        if (total == 0) {
            return new PageResult<>(0, pageQuery.pageNo(), pageQuery.pageSize(), List.of());
        }

        LambdaQueryWrapper<AuditLogEntity> pageWrapper = buildQuery(query, startAt, endAt, filteredActorIds)
                .orderByDesc(AuditLogEntity::getCreatedAt)
                .orderByDesc(AuditLogEntity::getId)
                .last("LIMIT %d OFFSET %d".formatted(pageQuery.pageSize(), pageQuery.offset()));
        List<AuditLogEntity> auditLogs = auditLogMapper.selectList(pageWrapper);
        Map<Long, UserEntity> actors = loadActors(auditLogs);

        List<AdminAuditLogItemVO> records = auditLogs.stream()
                .map(auditLog -> toItem(auditLog, actors.get(auditLog.getActorUserId())))
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    private LambdaQueryWrapper<AuditLogEntity> buildQuery(
            AdminAuditLogPageQuery query,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Set<Long> filteredActorIds
    ) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = Wrappers.<AuditLogEntity>lambdaQuery();
        if (startAt != null) {
            wrapper.ge(AuditLogEntity::getCreatedAt, startAt);
        }
        if (endAt != null) {
            wrapper.le(AuditLogEntity::getCreatedAt, endAt);
        }
        if (query.actionType() != null && !query.actionType().isBlank()) {
            wrapper.like(AuditLogEntity::getActionType, query.actionType().trim());
        }
        if (filteredActorIds != null) {
            wrapper.in(AuditLogEntity::getActorUserId, filteredActorIds);
        }
        return wrapper;
    }

    private Set<Long> resolveFilteredActorIds(String userKeyword) {
        if (userKeyword == null || userKeyword.isBlank()) {
            return null;
        }
        String keyword = userKeyword.trim();
        Set<Long> matchedActorIds = new LinkedHashSet<>();
        if (keyword.chars().allMatch(Character::isDigit)) {
            matchedActorIds.add(Long.parseLong(keyword));
        }
        List<UserEntity> matchedUsers = userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                .select(UserEntity::getId, UserEntity::getUsername, UserEntity::getEmail, UserEntity::getDisplayName)
                .and(wrapper -> wrapper
                        .like(UserEntity::getUsername, keyword)
                        .or()
                        .like(UserEntity::getEmail, keyword)
                        .or()
                        .like(UserEntity::getDisplayName, keyword)));
        matchedUsers.stream()
                .map(UserEntity::getId)
                .filter(Objects::nonNull)
                .forEach(matchedActorIds::add);
        return matchedActorIds;
    }

    private Map<Long, UserEntity> loadActors(List<AuditLogEntity> auditLogs) {
        List<Long> actorIds = auditLogs.stream()
                .map(AuditLogEntity::getActorUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (actorIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserEntity> actors = new LinkedHashMap<>();
        for (UserEntity user : userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                .select(UserEntity::getId, UserEntity::getUsername, UserEntity::getDisplayName)
                .in(UserEntity::getId, actorIds))) {
            actors.put(user.getId(), user);
        }
        return actors;
    }

    private AdminAuditLogItemVO toItem(AuditLogEntity entity, UserEntity actor) {
        return new AdminAuditLogItemVO(
                entity.getId(),
                entity.getActorUserId(),
                actor == null ? null : actor.getUsername(),
                actor == null ? null : actor.getDisplayName(),
                entity.getActionType(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getRequestPath(),
                entity.getRequestMethod(),
                entity.getTraceId(),
                entity.getRequestPayload(),
                entity.getResponseCode(),
                entity.getCreatedAt()
        );
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    ResultCode.VALIDATION_ERROR,
                    "%s must be an ISO-8601 local date time, for example 2026-04-15T09:30".formatted(fieldName),
                    400
            );
        }
    }
}
