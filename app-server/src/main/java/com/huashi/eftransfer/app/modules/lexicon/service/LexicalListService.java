package com.huashi.eftransfer.app.modules.lexicon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.lexicon.dto.AddLexicalListItemsRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.CreateLexicalListRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalListItemsPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalListPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.dto.ReorderLexicalListItemsRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.UpdateLexicalListRequest;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalListEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalListItemEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalListItemMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalListMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.lexicon.support.LexicalRiskSupport;
import com.huashi.eftransfer.app.modules.lexicon.vo.AddLexicalListItemsResultVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalListDetailVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalListItemVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalListSummaryVO;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LexicalListService {

    private static final Logger log = LoggerFactory.getLogger(LexicalListService.class);

    private final LexicalListMapper lexicalListMapper;
    private final LexicalListItemMapper lexicalListItemMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final UserMapper userMapper;

    public LexicalListService(
            LexicalListMapper lexicalListMapper,
            LexicalListItemMapper lexicalListItemMapper,
            LexicalPairMapper lexicalPairMapper,
            UserMapper userMapper
    ) {
        this.lexicalListMapper = lexicalListMapper;
        this.lexicalListItemMapper = lexicalListItemMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public Long create(CreateLexicalListRequest request) {
        JwtPrincipal principal = requirePrincipal();
        LexicalListEntity entity = new LexicalListEntity();
        entity.setListName(request.listName().trim());
        entity.setDescription(trimToNull(request.description()));
        entity.setOwnerUserId(principal.userId());
        entity.setActive(request.active() == null || request.active());
        lexicalListMapper.insert(entity);
        log.info("event=lexical_list_created listId={} ownerUserId={}", entity.getId(), principal.userId());
        return entity.getId();
    }

    public PageResult<LexicalListSummaryVO> pageQuery(LexicalListPageQuery query) {
        JwtPrincipal principal = requirePrincipal();
        PageQuery pageQuery = query.toPageQuery();
        LambdaQueryWrapper<LexicalListEntity> countWrapper = buildListPageWrapper(query, principal);
        long total = lexicalListMapper.selectCount(countWrapper);

        LambdaQueryWrapper<LexicalListEntity> pageWrapper = buildListPageWrapper(query, principal)
                .orderByDesc(LexicalListEntity::getUpdatedAt)
                .orderByDesc(LexicalListEntity::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset());

        List<LexicalListEntity> lists = lexicalListMapper.selectList(pageWrapper);
        Map<Long, String> ownerNameMap = loadOwnerNameMap(lists.stream().map(LexicalListEntity::getOwnerUserId).toList());
        Map<Long, Long> itemCountMap = loadListItemCountMap(lists.stream().map(LexicalListEntity::getId).toList());

        List<LexicalListSummaryVO> records = lists.stream()
                .map(list -> new LexicalListSummaryVO(
                        list.getId(),
                        list.getListName(),
                        list.getDescription(),
                        list.getOwnerUserId(),
                        ownerNameMap.getOrDefault(list.getOwnerUserId(), "Unknown"),
                        list.getActive(),
                        itemCountMap.getOrDefault(list.getId(), 0L),
                        list.getCreatedAt(),
                        list.getUpdatedAt()
                ))
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    public LexicalListDetailVO getDetail(Long lexicalListId) {
        LexicalListEntity list = requireList(lexicalListId);
        Map<Long, String> ownerNameMap = loadOwnerNameMap(List.of(list.getOwnerUserId()));
        List<LexicalListItemEntity> items = lexicalListItemMapper.selectList(Wrappers.<LexicalListItemEntity>lambdaQuery()
                .eq(LexicalListItemEntity::getLexicalListId, lexicalListId)
                .orderByAsc(LexicalListItemEntity::getSortOrder)
                .orderByAsc(LexicalListItemEntity::getId));

        Map<Long, LexicalPairEntity> pairMap = items.isEmpty()
                ? Map.of()
                : lexicalPairMapper.selectBatchIds(items.stream()
                        .map(LexicalListItemEntity::getLexicalPairId)
                        .distinct()
                        .toList())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(LexicalPairEntity::getId, entity -> entity));

        List<LexicalListItemVO> itemVOs = items.stream()
                .map(item -> {
                    LexicalPairEntity pair = pairMap.get(item.getLexicalPairId());
                    if (pair == null) {
                        return null;
                    }
                    return new LexicalListItemVO(
                            item.getId(),
                            item.getLexicalPairId(),
                            item.getSortOrder(),
                            item.getNotes(),
                            pair.getEnglishWord(),
                            pair.getFrenchWord(),
                            pair.getChineseGloss(),
                            pair.getLexicalPairType(),
                            pair.getDefaultContextSupport(),
                            pair.getDifficultyLevel(),
                            LexicalRiskSupport.resolve(pair.getFalseFriendRisk()).name()
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return new LexicalListDetailVO(
                list.getId(),
                list.getListName(),
                list.getDescription(),
                list.getOwnerUserId(),
                ownerNameMap.getOrDefault(list.getOwnerUserId(), "Unknown"),
                list.getActive(),
                itemVOs.size(),
                list.getCreatedAt(),
                list.getUpdatedAt(),
                itemVOs
        );
    }

    @Transactional
    public LexicalListDetailVO update(Long lexicalListId, UpdateLexicalListRequest request) {
        LexicalListEntity list = requireManageableList(lexicalListId);
        list.setListName(request.listName().trim());
        list.setDescription(trimToNull(request.description()));
        list.setActive(request.active());
        lexicalListMapper.updateById(list);
        return getDetail(lexicalListId);
    }

    @Transactional
    public void delete(Long lexicalListId) {
        requireManageableList(lexicalListId);
        lexicalListItemMapper.delete(Wrappers.<LexicalListItemEntity>lambdaQuery()
                .eq(LexicalListItemEntity::getLexicalListId, lexicalListId));
        lexicalListMapper.deleteById(lexicalListId);
        log.info("event=lexical_list_deleted listId={}", lexicalListId);
    }

    @Transactional
    public AddLexicalListItemsResultVO addItems(Long lexicalListId, AddLexicalListItemsRequest request) {
        LexicalListEntity list = requireManageableList(lexicalListId);
        Set<Long> requestedPairIds = new LinkedHashSet<>(request.lexicalPairIds());
        List<LexicalPairEntity> pairs = lexicalPairMapper.selectBatchIds(requestedPairIds);
        if (pairs.size() != requestedPairIds.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "One or more lexical pairs were not found", 404);
        }

        Map<Long, LexicalListItemEntity> existingItems = lexicalListItemMapper.selectList(Wrappers.<LexicalListItemEntity>lambdaQuery()
                        .eq(LexicalListItemEntity::getLexicalListId, lexicalListId)
                        .in(LexicalListItemEntity::getLexicalPairId, requestedPairIds))
                .stream()
                .collect(Collectors.toMap(LexicalListItemEntity::getLexicalPairId, entity -> entity));

        int nextSortOrder = lexicalListItemMapper.selectMaxSortOrder(lexicalListId);
        List<Long> skippedPairIds = new ArrayList<>();
        int addedCount = 0;
        for (Long lexicalPairId : requestedPairIds) {
            if (existingItems.containsKey(lexicalPairId)) {
                skippedPairIds.add(lexicalPairId);
                continue;
            }
            LexicalListItemEntity item = new LexicalListItemEntity();
            item.setLexicalListId(list.getId());
            item.setLexicalPairId(lexicalPairId);
            item.setSortOrder(++nextSortOrder);
            lexicalListItemMapper.insert(item);
            addedCount++;
        }
        log.info("event=lexical_list_items_added listId={} addedCount={} skippedCount={}",
                lexicalListId, addedCount, skippedPairIds.size());
        return new AddLexicalListItemsResultVO(addedCount, skippedPairIds);
    }

    @Transactional
    public void deleteItem(Long lexicalListId, Long itemId) {
        requireManageableList(lexicalListId);
        LexicalListItemEntity item = lexicalListItemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getLexicalListId(), lexicalListId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Lexical list item was not found", 404);
        }
        lexicalListItemMapper.deleteById(itemId);
        log.info("event=lexical_list_item_deleted listId={} itemId={}", lexicalListId, itemId);
    }

    public PageResult<LexicalListItemVO> pageItems(Long lexicalListId, LexicalListItemsPageQuery query) {
        requireList(lexicalListId);
        PageQuery pageQuery = query.toPageQuery();
        List<LexicalListItemVO> items = buildItemVOs(lexicalListId);
        List<LexicalListItemVO> filtered = items.stream()
                .filter(item -> matchesKeyword(item, query.keyword()))
                .toList();
        int fromIndex = (int) Math.min(filtered.size(), pageQuery.offset());
        int toIndex = Math.min(filtered.size(), fromIndex + pageQuery.pageSize());
        return new PageResult<>(filtered.size(), pageQuery.pageNo(), pageQuery.pageSize(), filtered.subList(fromIndex, toIndex));
    }

    @Transactional
    public LexicalListDetailVO reorderItems(Long lexicalListId, ReorderLexicalListItemsRequest request) {
        requireManageableList(lexicalListId);
        List<LexicalListItemEntity> items = lexicalListItemMapper.selectList(Wrappers.<LexicalListItemEntity>lambdaQuery()
                .eq(LexicalListItemEntity::getLexicalListId, lexicalListId)
                .orderByAsc(LexicalListItemEntity::getSortOrder)
                .orderByAsc(LexicalListItemEntity::getId));
        List<Long> orderedItemIds = request.orderedItemIds();
        Set<Long> requestedIds = new LinkedHashSet<>(orderedItemIds);
        if (requestedIds.size() != orderedItemIds.size()) {
            throw new BusinessException(ResultCode.CONFLICT, "orderedItemIds must not contain duplicate lexical list item ids", 409);
        }
        Set<Long> existingIds = items.stream().map(LexicalListItemEntity::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (orderedItemIds.size() != items.size() || !existingIds.equals(requestedIds)) {
            throw new BusinessException(ResultCode.CONFLICT, "orderedItemIds must match the full set of current lexical list item ids", 409);
        }

        Map<Long, LexicalListItemEntity> itemMap = items.stream()
                .collect(Collectors.toMap(LexicalListItemEntity::getId, entity -> entity));
        int sortOrder = 1;
        for (Long itemId : orderedItemIds) {
            LexicalListItemEntity item = itemMap.get(itemId);
            item.setSortOrder(sortOrder++);
            lexicalListItemMapper.updateById(item);
        }
        return getDetail(lexicalListId);
    }

    private LambdaQueryWrapper<LexicalListEntity> buildListPageWrapper(LexicalListPageQuery query, JwtPrincipal principal) {
        LambdaQueryWrapper<LexicalListEntity> wrapper = Wrappers.lambdaQuery();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            String normalizedKeyword = "%" + query.keyword().trim().toLowerCase() + "%";
            wrapper.and(condition -> condition
                    .apply("LOWER(list_name) LIKE {0}", normalizedKeyword)
                    .or()
                    .apply("LOWER(description) LIKE {0}", normalizedKeyword));
        }
        if (Boolean.TRUE.equals(query.mineOnly())) {
            wrapper.eq(LexicalListEntity::getOwnerUserId, principal.userId());
        }
        if (query.active() != null) {
            wrapper.eq(LexicalListEntity::getActive, query.active());
        }
        return wrapper;
    }

    private Map<Long, String> loadOwnerNameMap(Collection<Long> ownerUserIds) {
        if (ownerUserIds == null || ownerUserIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ownerUserIds.stream().distinct().toList())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getDisplayName));
    }

    private Map<Long, Long> loadListItemCountMap(Collection<Long> lexicalListIds) {
        if (lexicalListIds == null || lexicalListIds.isEmpty()) {
            return Map.of();
        }
        return lexicalListItemMapper.selectList(Wrappers.<LexicalListItemEntity>lambdaQuery()
                        .in(LexicalListItemEntity::getLexicalListId, lexicalListIds))
                .stream()
                .collect(Collectors.groupingBy(
                        LexicalListItemEntity::getLexicalListId,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private List<LexicalListItemVO> buildItemVOs(Long lexicalListId) {
        List<LexicalListItemEntity> items = lexicalListItemMapper.selectList(Wrappers.<LexicalListItemEntity>lambdaQuery()
                .eq(LexicalListItemEntity::getLexicalListId, lexicalListId)
                .orderByAsc(LexicalListItemEntity::getSortOrder)
                .orderByAsc(LexicalListItemEntity::getId));
        Map<Long, LexicalPairEntity> pairMap = items.isEmpty()
                ? Map.of()
                : lexicalPairMapper.selectBatchIds(items.stream().map(LexicalListItemEntity::getLexicalPairId).distinct().toList()).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(LexicalPairEntity::getId, entity -> entity));
        return items.stream()
                .map(item -> {
                    LexicalPairEntity pair = pairMap.get(item.getLexicalPairId());
                    if (pair == null) {
                        return null;
                    }
                    return new LexicalListItemVO(
                            item.getId(),
                            item.getLexicalPairId(),
                            item.getSortOrder(),
                            item.getNotes(),
                            pair.getEnglishWord(),
                            pair.getFrenchWord(),
                            pair.getChineseGloss(),
                            pair.getLexicalPairType(),
                            pair.getDefaultContextSupport(),
                            pair.getDifficultyLevel(),
                            LexicalRiskSupport.resolve(pair.getFalseFriendRisk()).name()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean matchesKeyword(LexicalListItemVO item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return item.englishWord().toLowerCase().contains(normalized)
                || item.frenchWord().toLowerCase().contains(normalized)
                || item.chineseGloss().toLowerCase().contains(normalized);
    }

    private LexicalListEntity requireList(Long lexicalListId) {
        LexicalListEntity entity = lexicalListMapper.selectById(lexicalListId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Lexical list was not found", 404);
        }
        return entity;
    }

    private LexicalListEntity requireManageableList(Long lexicalListId) {
        LexicalListEntity entity = requireList(lexicalListId);
        JwtPrincipal principal = requirePrincipal();
        boolean isAdmin = principal.roles().contains("ADMIN");
        boolean isOwner = Objects.equals(entity.getOwnerUserId(), principal.userId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to manage this lexical list", 403);
        }
        return entity;
    }

    private JwtPrincipal requirePrincipal() {
        return SecurityUtils.getCurrentPrincipal()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
