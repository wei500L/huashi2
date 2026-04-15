package com.huashi.eftransfer.app.modules.notification.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.notification.dto.NotificationPageQuery;
import com.huashi.eftransfer.app.modules.notification.entity.NotificationEntity;
import com.huashi.eftransfer.app.modules.notification.mapper.NotificationMapper;
import com.huashi.eftransfer.app.modules.notification.vo.NotificationItemVO;
import com.huashi.eftransfer.app.modules.notification.vo.NotificationUnreadCountVO;
import com.huashi.eftransfer.app.modules.notification.ws.NotificationWebSocketHandler;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final String STATUS_UNREAD = "UNREAD";
    private static final String STATUS_READ = "READ";

    private final NotificationMapper notificationMapper;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    public NotificationService(
            NotificationMapper notificationMapper,
            NotificationWebSocketHandler notificationWebSocketHandler
    ) {
        this.notificationMapper = notificationMapper;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    public PageResult<NotificationItemVO> pageMine(NotificationPageQuery query) {
        Long userId = currentUserId();
        PageQuery pageQuery = query.toPageQuery();
        var wrapper = Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getRecipientUserId, userId)
                .orderByDesc(NotificationEntity::getCreatedAt)
                .orderByDesc(NotificationEntity::getId);
        if (Boolean.TRUE.equals(query.unreadOnly())) {
            wrapper.eq(NotificationEntity::getStatus, STATUS_UNREAD);
        }
        long total = notificationMapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResult<>(0, pageQuery.pageNo(), pageQuery.pageSize(), List.of());
        }
        List<NotificationItemVO> records = notificationMapper.selectList(wrapper
                        .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()))
                .stream()
                .map(this::toItem)
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    public NotificationUnreadCountVO getUnreadCount() {
        return new NotificationUnreadCountVO(countUnread(currentUserId()));
    }

    @Transactional
    public NotificationItemVO markRead(Long notificationId) {
        NotificationEntity entity = requireOwnedNotification(notificationId);
        if (STATUS_READ.equalsIgnoreCase(entity.getStatus())) {
            return toItem(entity);
        }
        entity.setStatus(STATUS_READ);
        entity.setReadAt(LocalDateTime.now());
        notificationMapper.updateById(entity);
        return toItem(entity);
    }

    @Transactional
    public NotificationUnreadCountVO markAllRead() {
        Long userId = currentUserId();
        List<NotificationEntity> unread = notificationMapper.selectList(Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getRecipientUserId, userId)
                .eq(NotificationEntity::getStatus, STATUS_UNREAD)
                .orderByAsc(NotificationEntity::getId));
        if (unread.isEmpty()) {
            return new NotificationUnreadCountVO(0);
        }
        LocalDateTime now = LocalDateTime.now();
        for (NotificationEntity entity : unread) {
            entity.setStatus(STATUS_READ);
            entity.setReadAt(now);
            notificationMapper.updateById(entity);
        }
        return new NotificationUnreadCountVO(0);
    }

    @Transactional
    public void create(NotificationCreateCommand command) {
        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientUserId(command.recipientUserId());
        entity.setCategory(command.category());
        entity.setLevel(command.level());
        entity.setTitle(command.title());
        entity.setContent(command.content());
        entity.setActionUrl(command.actionUrl());
        entity.setActionLabel(command.actionLabel());
        entity.setStatus(STATUS_UNREAD);
        entity.setPayloadJson(command.payloadJson());
        notificationMapper.insert(entity);

        NotificationItemVO item = toItem(entity);
        runAfterCommit(() -> notificationWebSocketHandler.pushNotificationCreated(
                entity.getRecipientUserId(),
                item,
                countUnread(entity.getRecipientUserId())
        ));
    }

    @Transactional
    public void createBatch(Collection<NotificationCreateCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        Map<Long, List<NotificationItemVO>> createdItemsByUser = new LinkedHashMap<>();
        for (NotificationCreateCommand command : commands) {
            NotificationEntity entity = new NotificationEntity();
            entity.setRecipientUserId(command.recipientUserId());
            entity.setCategory(command.category());
            entity.setLevel(command.level());
            entity.setTitle(command.title());
            entity.setContent(command.content());
            entity.setActionUrl(command.actionUrl());
            entity.setActionLabel(command.actionLabel());
            entity.setStatus(STATUS_UNREAD);
            entity.setPayloadJson(command.payloadJson());
            notificationMapper.insert(entity);
            createdItemsByUser.computeIfAbsent(entity.getRecipientUserId(), ignored -> new java.util.ArrayList<>())
                    .add(toItem(entity));
        }
        runAfterCommit(() -> {
            for (Map.Entry<Long, List<NotificationItemVO>> entry : createdItemsByUser.entrySet()) {
                Long userId = entry.getKey();
                long unreadCount = countUnread(userId);
                for (NotificationItemVO item : entry.getValue()) {
                    notificationWebSocketHandler.pushNotificationCreated(userId, item, unreadCount);
                }
            }
        });
    }

    public long countUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        Long count = notificationMapper.selectCount(Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getRecipientUserId, userId)
                .eq(NotificationEntity::getStatus, STATUS_UNREAD));
        return count == null ? 0 : count;
    }

    private NotificationEntity requireOwnedNotification(Long notificationId) {
        NotificationEntity entity = notificationMapper.selectById(notificationId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Notification was not found", 404);
        }
        if (!currentUserId().equals(entity.getRecipientUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have access to this notification", 403);
        }
        return entity;
    }

    private NotificationItemVO toItem(NotificationEntity entity) {
        return new NotificationItemVO(
                entity.getId(),
                entity.getCategory(),
                entity.getLevel(),
                entity.getTitle(),
                entity.getContent(),
                entity.getActionUrl(),
                entity.getActionLabel(),
                entity.getStatus(),
                entity.getPayloadJson(),
                entity.getCreatedAt(),
                entity.getReadAt()
        );
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
            return;
        }
        runnable.run();
    }
}
