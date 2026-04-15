package com.huashi.eftransfer.app.modules.achievement;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.huashi.eftransfer.app.modules.achievement.entity.AchievementEntity;
import com.huashi.eftransfer.app.modules.achievement.mapper.AchievementMapper;
import com.huashi.eftransfer.app.modules.achievement.service.AchievementService;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.training.entity.TrainingSessionEntity;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingSessionMapper;
import com.huashi.eftransfer.app.modules.training.mapper.WrongBookMapper;
import com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec;
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionSummarySnapshot;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private AchievementMapper achievementMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DiagnosisSummaryMapper diagnosisSummaryMapper;

    @Mock
    private TrainingSessionMapper trainingSessionMapper;

    @Mock
    private WrongBookMapper wrongBookMapper;

    @Test
    void shouldQueryOnlySessionsNewerThanTrackedTrainingExpertCursor() {
        ObjectMapper objectMapper = new ObjectMapper();
        AchievementService achievementService = new AchievementService(
                achievementMapper,
                userMapper,
                diagnosisSummaryMapper,
                trainingSessionMapper,
                wrongBookMapper,
                new TrainingJsonCodec(objectMapper),
                objectMapper
        );

        UserEntity user = new UserEntity();
        user.setId(101L);

        AchievementEntity trainingExpert = new AchievementEntity();
        trainingExpert.setId(501L);
        trainingExpert.setOwnerUserId(101L);
        trainingExpert.setAchievementCode("TRAINING_EXPERT");
        trainingExpert.setProgressValue(0);
        trainingExpert.setUnlocked(false);
        trainingExpert.setTargetValue(90);
        trainingExpert.setLastCalculatedAt(LocalDateTime.now().minusDays(1));

        when(userMapper.selectById(101L)).thenReturn(user);
        when(achievementMapper.selectList(any())).thenReturn(List.of(trainingExpert));
        when(diagnosisSummaryMapper.selectCount(any())).thenReturn(0L);
        when(wrongBookMapper.selectCount(any())).thenReturn(0L);
        when(trainingSessionMapper.selectList(any()))
                .thenReturn(List.of(trainingSession(10L, 0.72d), trainingSession(11L, 0.91d)))
                .thenReturn(List.of(trainingSession(12L, 0.85d)));

        achievementService.refreshAchievementsForUser(101L);
        achievementService.refreshAchievementsForUser(101L);

        ArgumentCaptor<Wrapper<TrainingSessionEntity>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(trainingSessionMapper, times(2)).selectList(queryCaptor.capture());

        List<Wrapper<TrainingSessionEntity>> queries = queryCaptor.getAllValues();
        assertThat(queries.get(0).getSqlSegment()).doesNotContain(">");
        assertThat(queries.get(1).getSqlSegment()).contains(">");
        assertThat(trainingExpert.getMetadataJson()).contains("\"lastProcessedSessionId\":12");
        assertThat(trainingExpert.getProgressValue()).isEqualTo(91);
    }

    private TrainingSessionEntity trainingSession(Long id, double accuracy) {
        ObjectMapper objectMapper = new ObjectMapper();
        TrainingJsonCodec codec = new TrainingJsonCodec(objectMapper);

        TrainingSessionEntity session = new TrainingSessionEntity();
        session.setId(id);
        session.setSummarySnapshotJson(codec.write(new TrainingSessionSummarySnapshot(
                accuracy,
                900L,
                "hint",
                "FALSE_FRIEND_DISCRIM",
                List.of()
        )));
        return session;
    }
}
