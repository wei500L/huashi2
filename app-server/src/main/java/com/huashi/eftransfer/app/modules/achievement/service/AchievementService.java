package com.huashi.eftransfer.app.modules.achievement.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.achievement.entity.AchievementEntity;
import com.huashi.eftransfer.app.modules.achievement.mapper.AchievementMapper;
import com.huashi.eftransfer.app.modules.achievement.vo.StudentAchievementBadgeVO;
import com.huashi.eftransfer.app.modules.achievement.vo.StudentAchievementWallVO;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSummaryEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.training.entity.TrainingSessionEntity;
import com.huashi.eftransfer.app.modules.training.entity.WrongBookEntity;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingSessionMapper;
import com.huashi.eftransfer.app.modules.training.mapper.WrongBookMapper;
import com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec;
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionSummarySnapshot;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.enums.WrongBookMasteryStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AchievementService {

    private static final AchievementDefinition LOGIN_STREAK = new AchievementDefinition("LOGIN_STREAK", 3, 1);
    private static final AchievementDefinition DIAGNOSIS_FINISHER = new AchievementDefinition("DIAGNOSIS_FINISHER", 1, 2);
    private static final AchievementDefinition TRAINING_EXPERT = new AchievementDefinition("TRAINING_EXPERT", 90, 3);
    private static final AchievementDefinition VOCAB_MASTER = new AchievementDefinition("VOCAB_MASTER", 20, 4);
    private static final List<AchievementDefinition> DEFINITIONS = List.of(
            LOGIN_STREAK,
            DIAGNOSIS_FINISHER,
            TRAINING_EXPERT,
            VOCAB_MASTER
    );

    private final AchievementMapper achievementMapper;
    private final UserMapper userMapper;
    private final DiagnosisSummaryMapper diagnosisSummaryMapper;
    private final TrainingSessionMapper trainingSessionMapper;
    private final WrongBookMapper wrongBookMapper;
    private final TrainingJsonCodec trainingJsonCodec;
    private final ObjectMapper objectMapper;

    public AchievementService(
            AchievementMapper achievementMapper,
            UserMapper userMapper,
            DiagnosisSummaryMapper diagnosisSummaryMapper,
            TrainingSessionMapper trainingSessionMapper,
            WrongBookMapper wrongBookMapper,
            TrainingJsonCodec trainingJsonCodec,
            ObjectMapper objectMapper
    ) {
        this.achievementMapper = achievementMapper;
        this.userMapper = userMapper;
        this.diagnosisSummaryMapper = diagnosisSummaryMapper;
        this.trainingSessionMapper = trainingSessionMapper;
        this.wrongBookMapper = wrongBookMapper;
        this.trainingJsonCodec = trainingJsonCodec;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordLogin(Long userId, LocalDateTime previousLastLoginAt, LocalDateTime currentLoginAt) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        refreshAchievements(user, new LoginUpdate(previousLastLoginAt, currentLoginAt), currentLoginAt);
    }

    @Transactional
    public void refreshAchievementsForUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        refreshAchievements(user, null, LocalDateTime.now());
    }

    @Transactional
    public StudentAchievementWallVO getAchievementWall(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return new StudentAchievementWallVO(0, DEFINITIONS.size(), List.of());
        }
        return refreshAchievements(user, null, LocalDateTime.now());
    }

    private StudentAchievementWallVO refreshAchievements(UserEntity user, LoginUpdate loginUpdate, LocalDateTime now) {
        Map<String, AchievementEntity> existing = loadAchievementMap(user.getId());
        int unlockedCount = 0;
        List<StudentAchievementBadgeVO> badges = DEFINITIONS.stream()
                .map(definition -> {
                    AchievementEntity entity = existing.get(definition.code());
                    AchievementState state = evaluateAchievement(definition, user, entity, loginUpdate);
                    AchievementEntity persisted = saveAchievement(user.getId(), definition, entity, state, now);
                    if (Boolean.TRUE.equals(persisted.getUnlocked())) {
                        return new StudentAchievementBadgeVO(
                                persisted.getAchievementCode(),
                                true,
                                safeInt(persisted.getProgressValue()),
                                safeInt(persisted.getTargetValue()),
                                persisted.getAwardedAt()
                        );
                    }
                    return new StudentAchievementBadgeVO(
                            persisted.getAchievementCode(),
                            false,
                            safeInt(persisted.getProgressValue()),
                            safeInt(persisted.getTargetValue()),
                            persisted.getAwardedAt()
                    );
                })
                .sorted((left, right) -> Integer.compare(orderOf(left.code()), orderOf(right.code())))
                .toList();
        for (StudentAchievementBadgeVO badge : badges) {
            if (badge.unlocked()) {
                unlockedCount++;
            }
        }
        return new StudentAchievementWallVO(unlockedCount, DEFINITIONS.size(), badges);
    }

    private AchievementState evaluateAchievement(
            AchievementDefinition definition,
            UserEntity user,
            AchievementEntity entity,
            LoginUpdate loginUpdate
    ) {
        return switch (definition.code()) {
            case "LOGIN_STREAK" -> evaluateLoginStreak(user, entity, definition, loginUpdate);
            case "DIAGNOSIS_FINISHER" -> evaluateDiagnosisFinisher(user.getId(), definition);
            case "TRAINING_EXPERT" -> evaluateTrainingExpert(user.getId(), entity, definition);
            case "VOCAB_MASTER" -> evaluateVocabMaster(user.getId(), definition);
            default -> new AchievementState(false, 0, definition.targetValue(), null);
        };
    }

    private AchievementState evaluateLoginStreak(
            UserEntity user,
            AchievementEntity entity,
            AchievementDefinition definition,
            LoginUpdate loginUpdate
    ) {
        if (loginUpdate != null) {
            int streakDays = resolveLoginStreak(loginUpdate.previousLastLoginAt(), loginUpdate.currentLoginAt(), entity);
            return new AchievementState(
                    streakDays >= definition.targetValue(),
                    streakDays,
                    definition.targetValue(),
                    writeLoginMetadata(new LoginStreakMetadata(streakDays, loginUpdate.currentLoginAt().toLocalDate()))
            );
        }

        LoginStreakMetadata metadata = readLoginMetadata(entity);
        if (metadata != null) {
            return new AchievementState(
                    metadata.streakDays() >= definition.targetValue(),
                    metadata.streakDays(),
                    definition.targetValue(),
                    entity == null ? null : entity.getMetadataJson()
            );
        }

        if (user.getLastLoginAt() == null) {
            return new AchievementState(false, 0, definition.targetValue(), null);
        }

        int progressValue = user.getLastLoginAt().toLocalDate().equals(LocalDate.now()) ? 1 : 0;
        return new AchievementState(
                progressValue >= definition.targetValue(),
                progressValue,
                definition.targetValue(),
                progressValue > 0 ? writeLoginMetadata(new LoginStreakMetadata(progressValue, user.getLastLoginAt().toLocalDate())) : null
        );
    }

    private AchievementState evaluateDiagnosisFinisher(Long userId, AchievementDefinition definition) {
        int completedCount = Math.toIntExact(diagnosisSummaryMapper.selectCount(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getOwnerUserId, userId)
                .isNotNull(DiagnosisSummaryEntity::getGeneratedAt)));
        return new AchievementState(completedCount >= definition.targetValue(), completedCount, definition.targetValue(), null);
    }

    private AchievementState evaluateTrainingExpert(
            Long userId,
            AchievementEntity entity,
            AchievementDefinition definition
    ) {
        TrainingExpertMetadata metadata = readTrainingExpertMetadata(entity);
        int bestAccuracy = Math.max(
                safeInt(entity == null ? null : entity.getProgressValue()),
                metadata == null ? 0 : metadata.bestAccuracy()
        );
        Long lastProcessedSessionId = metadata == null ? bootstrapTrainingExpertCursor(userId, entity) : metadata.lastProcessedSessionId();

        List<TrainingSessionEntity> sessions = loadTrainingSessionsForTrainingExpert(userId, lastProcessedSessionId);
        for (TrainingSessionEntity session : sessions) {
            bestAccuracy = Math.max(bestAccuracy, toAccuracyPercent(trainingJsonCodec.readSummarySnapshot(session.getSummarySnapshotJson())));
            lastProcessedSessionId = session.getId();
        }

        String metadataJson = lastProcessedSessionId == null && bestAccuracy == 0
                ? null
                : writeTrainingExpertMetadata(new TrainingExpertMetadata(bestAccuracy, lastProcessedSessionId));
        return new AchievementState(bestAccuracy >= definition.targetValue(), bestAccuracy, definition.targetValue(), metadataJson);
    }

    private AchievementState evaluateVocabMaster(Long userId, AchievementDefinition definition) {
        int masteredCount = Math.toIntExact(wrongBookMapper.selectCount(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getOwnerUserId, userId)
                .eq(WrongBookEntity::getMasteryStatus, WrongBookMasteryStatus.MASTERED.name())));
        return new AchievementState(masteredCount >= definition.targetValue(), masteredCount, definition.targetValue(), null);
    }

    private AchievementEntity saveAchievement(
            Long userId,
            AchievementDefinition definition,
            AchievementEntity entity,
            AchievementState state,
            LocalDateTime now
    ) {
        boolean alreadyUnlocked = entity != null && Boolean.TRUE.equals(entity.getUnlocked());
        boolean unlocked = alreadyUnlocked || state.unlocked();
        int progressValue = alreadyUnlocked || state.unlocked()
                ? Math.max(Math.max(safeInt(entity == null ? null : entity.getProgressValue()), state.progressValue()), definition.targetValue())
                : state.progressValue();
        LocalDateTime awardedAt = entity == null ? null : entity.getAwardedAt();
        if (awardedAt == null && unlocked) {
            awardedAt = now;
        }

        if (entity == null) {
            AchievementEntity created = new AchievementEntity();
            created.setOwnerUserId(userId);
            created.setAchievementCode(definition.code());
            created.setUnlocked(unlocked);
            created.setProgressValue(progressValue);
            created.setTargetValue(definition.targetValue());
            created.setAwardedAt(awardedAt);
            created.setLastCalculatedAt(now);
            created.setMetadataJson(state.metadataJson());
            achievementMapper.insert(created);
            return created;
        }

        boolean dirty = !Objects.equals(entity.getUnlocked(), unlocked)
                || !Objects.equals(entity.getProgressValue(), progressValue)
                || !Objects.equals(entity.getTargetValue(), definition.targetValue())
                || !Objects.equals(entity.getAwardedAt(), awardedAt)
                || !Objects.equals(entity.getMetadataJson(), state.metadataJson());
        if (dirty) {
            entity.setUnlocked(unlocked);
            entity.setProgressValue(progressValue);
            entity.setTargetValue(definition.targetValue());
            entity.setAwardedAt(awardedAt);
            entity.setLastCalculatedAt(now);
            entity.setMetadataJson(state.metadataJson());
            achievementMapper.updateById(entity);
        }
        return entity;
    }

    private Map<String, AchievementEntity> loadAchievementMap(Long userId) {
        Map<String, AchievementEntity> result = new LinkedHashMap<>();
        achievementMapper.selectList(Wrappers.<AchievementEntity>lambdaQuery()
                        .eq(AchievementEntity::getOwnerUserId, userId))
                .forEach(item -> result.put(item.getAchievementCode(), item));
        return result;
    }

    private int resolveLoginStreak(
            LocalDateTime previousLastLoginAt,
            LocalDateTime currentLoginAt,
            AchievementEntity entity
    ) {
        if (currentLoginAt == null) {
            return 0;
        }
        LoginStreakMetadata metadata = readLoginMetadata(entity);
        int trackedStreak = metadata == null ? 0 : metadata.streakDays();
        if (previousLastLoginAt == null) {
            return Math.max(trackedStreak, 1);
        }
        LocalDate previousDate = previousLastLoginAt.toLocalDate();
        LocalDate currentDate = currentLoginAt.toLocalDate();
        if (previousDate.equals(currentDate)) {
            return Math.max(trackedStreak, 1);
        }
        if (previousDate.plusDays(1).equals(currentDate)) {
            return Math.max(trackedStreak, 1) + 1;
        }
        return 1;
    }

    private LoginStreakMetadata readLoginMetadata(AchievementEntity entity) {
        if (entity == null || entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(entity.getMetadataJson(), LoginStreakMetadata.class);
        } catch (JacksonException exception) {
            return null;
        }
    }

    private String writeLoginMetadata(LoginStreakMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize achievement metadata", exception);
        }
    }

    private TrainingExpertMetadata readTrainingExpertMetadata(AchievementEntity entity) {
        if (entity == null || entity.getMetadataJson() == null || entity.getMetadataJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(entity.getMetadataJson(), TrainingExpertMetadata.class);
        } catch (JacksonException exception) {
            return null;
        }
    }

    private String writeTrainingExpertMetadata(TrainingExpertMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize achievement metadata", exception);
        }
    }

    private Long bootstrapTrainingExpertCursor(Long userId, AchievementEntity entity) {
        if (entity == null || entity.getLastCalculatedAt() == null) {
            return null;
        }
        TrainingSessionEntity latestProcessedSession = trainingSessionMapper.selectOne(Wrappers.<TrainingSessionEntity>lambdaQuery()
                .select(TrainingSessionEntity::getId)
                .eq(TrainingSessionEntity::getOwnerUserId, userId)
                .isNotNull(TrainingSessionEntity::getCompletedAt)
                .isNotNull(TrainingSessionEntity::getSummarySnapshotJson)
                .le(TrainingSessionEntity::getCompletedAt, entity.getLastCalculatedAt())
                .orderByDesc(TrainingSessionEntity::getId)
                .last("LIMIT 1"));
        return latestProcessedSession == null ? null : latestProcessedSession.getId();
    }

    private List<TrainingSessionEntity> loadTrainingSessionsForTrainingExpert(Long userId, Long lastProcessedSessionId) {
        var query = Wrappers.<TrainingSessionEntity>lambdaQuery()
                .select(TrainingSessionEntity::getId, TrainingSessionEntity::getSummarySnapshotJson)
                .eq(TrainingSessionEntity::getOwnerUserId, userId)
                .isNotNull(TrainingSessionEntity::getCompletedAt)
                .isNotNull(TrainingSessionEntity::getSummarySnapshotJson)
                .orderByAsc(TrainingSessionEntity::getId);
        if (lastProcessedSessionId != null) {
            query.gt(TrainingSessionEntity::getId, lastProcessedSessionId);
        }
        return trainingSessionMapper.selectList(query);
    }

    private int toAccuracyPercent(TrainingSessionSummarySnapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, (int) Math.round(snapshot.accuracy() * 100)));
    }

    private int orderOf(String code) {
        return DEFINITIONS.stream()
                .filter(item -> item.code().equals(code))
                .map(AchievementDefinition::displayOrder)
                .findFirst()
                .orElse(Integer.MAX_VALUE);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record AchievementDefinition(String code, int targetValue, int displayOrder) {
    }

    private record AchievementState(boolean unlocked, int progressValue, int targetValue, String metadataJson) {
    }

    private record LoginUpdate(LocalDateTime previousLastLoginAt, LocalDateTime currentLoginAt) {
    }

    private record LoginStreakMetadata(int streakDays, LocalDate lastLoginDate) {
    }

    private record TrainingExpertMetadata(int bestAccuracy, Long lastProcessedSessionId) {
    }
}
