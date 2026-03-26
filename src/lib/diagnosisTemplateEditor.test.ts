import { describe, expect, it } from 'vitest';
import {
  buildTemplateItemFromPair,
  parseTemplateItemsJson,
  validateTemplateBeforeSave,
} from './diagnosisTemplateEditor';

describe('diagnosisTemplateEditor', () => {
  it('normalizes editable JSON into request items', () => {
    const items = parseTemplateItemsJson(`
      [
        {
          "lexicalPairId": 1,
          "taskType": "reaction_time_task",
          "blockCode": "B1",
          "sortOrder": 1,
          "contextSupportLevel": "low",
          "expectedSemanticMatch": true,
          "stimulus": {
            "instruction": "Quickly decide",
            "contextSentence": "",
            "promptText": "Match?"
          },
          "options": [
            { "key": "semantic_match", "label": "语义一致", "semanticMatch": true, "ignoreContextTrap": false },
            { "key": "semantic_mismatch", "label": "语义不一致", "semanticMatch": false, "ignoreContextTrap": false }
          ],
          "correctAnswerKey": "semantic_match",
          "scoringProfile": {
            "formulaKey": "RULE_V1",
            "pairWeight": 1,
            "riskAmplifier": 1.4,
            "maxReactionTimeMs": 1500
          }
        }
      ]
    `);

    expect(items).toEqual([
      {
        lexicalPairId: 1,
        taskType: 'REACTION_TIME',
        blockCode: 'B1',
        sortOrder: 1,
        contextSupportLevel: 'LOW',
        expectedSemanticMatch: true,
        stimulus: {
          instruction: 'Quickly decide',
          contextSentence: '',
          promptText: 'Match?',
        },
        options: [
          { key: 'semantic_match', label: '语义一致', semanticMatch: true, ignoreContextTrap: false },
          { key: 'semantic_mismatch', label: '语义不一致', semanticMatch: false, ignoreContextTrap: false },
        ],
        correctAnswerKey: 'semantic_match',
        scoringProfile: {
          formulaKey: 'RULE_V1',
          pairWeight: 1,
          riskAmplifier: 1.4,
          maxReactionTimeMs: 1500,
        },
      },
    ]);
  });

  it('blocks publish when required coverage is incomplete', () => {
    const errors = validateTemplateBeforeSave({
      status: 'PUBLISHED',
      items: [
        {
          lexicalPairId: 1,
          taskType: 'REACTION_TIME',
          blockCode: 'B1',
          sortOrder: 1,
          contextSupportLevel: 'LOW',
          expectedSemanticMatch: true,
          stimulus: {
            instruction: 'Quickly decide',
            contextSentence: '',
            promptText: 'Match?',
          },
          options: [
            { key: 'semantic_match', label: '语义一致', semanticMatch: true, ignoreContextTrap: false },
            { key: 'semantic_mismatch', label: '语义不一致', semanticMatch: false, ignoreContextTrap: false },
          ],
          correctAnswerKey: 'semantic_match',
          scoringProfile: null,
        },
      ],
    });

    expect(errors).toContain('发布模板必须同时覆盖 REACTION_TIME 和 SEMANTIC_JUDGEMENT 两种题型。');
    expect(errors).toContain('发布模板必须覆盖 LOW、MEDIUM、HIGH 三档 contextSupportLevel。');
    expect(errors).toContain('发布模板必须同时覆盖 expectedSemanticMatch 为 true 和 false 的题目。');
  });

  it('builds draft skeletons with backend scoring profile fields', () => {
    const item = buildTemplateItemFromPair(
      {
        id: 7,
        englishWord: 'coin',
        frenchWord: 'coin',
        chineseGloss: '硬币；角落',
        lexicalPairType: 'FALSE_FRIEND',
        semanticOverlapScore: 0.1,
        falseFriendRisk: 0.92,
        riskLevel: 'HIGH',
        defaultContextSupport: 'medium',
        difficultyLevel: 4,
        source: null,
        active: true,
        knowledgeStatus: 'CURATED',
        embeddingStatus: 'COMPLETED',
        lastEmbeddedAt: null,
        tags: ['false-friend'],
      },
      2
    );

    expect(item.taskType).toBe('REACTION_TIME');
    expect(item.contextSupportLevel).toBe('MEDIUM');
    expect(item.scoringProfile).toEqual({
      formulaKey: 'RULE_V1',
      pairWeight: 0.6,
      riskAmplifier: 1.92,
      maxReactionTimeMs: 1500,
    });
  });
});
