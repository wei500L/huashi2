package com.huashi.eftransfer.app.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.ai.entity.AiGenerationRecordEntity;
import com.huashi.eftransfer.app.modules.ai.mapper.AiGenerationRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class AiGenerationRecordService {

    private final AiGenerationRecordMapper aiGenerationRecordMapper;

    public AiGenerationRecordService(AiGenerationRecordMapper aiGenerationRecordMapper) {
        this.aiGenerationRecordMapper = aiGenerationRecordMapper;
    }

    public void save(AiGenerationRecordEntity entity) {
        aiGenerationRecordMapper.insert(entity);
    }

    public AiGenerationRecordEntity findByRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return aiGenerationRecordMapper.selectOne(
                Wrappers.<AiGenerationRecordEntity>lambdaQuery()
                        .eq(AiGenerationRecordEntity::getRequestId, requestId)
                        .last("LIMIT 1")
        );
    }
}
