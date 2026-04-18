import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { DiagnosisResultDetailVO } from '@/lib/contracts';
import { DiagnosisPdfReport } from './DiagnosisPdfReport';

vi.mock('@/components/common/EChart', () => ({
  EChart: () => <div>chart</div>,
}));

function createResult(itemCount: number): DiagnosisResultDetailVO {
  return {
    summaryId: 301,
    sessionId: 42,
    status: 'COMPLETED',
    templateId: 12,
    templateName: '法语迁移诊断',
    ownerUserId: 7,
    totalItems: itemCount,
    answeredItems: itemCount,
    startedAt: '2026-04-15T08:00:00Z',
    completedAt: '2026-04-15T08:12:00Z',
    metrics: {
      positiveTransferScore: 0.64,
      negativeTransferRisk: 0.31,
      contextSensitivity: 0.55,
      semanticDiscrimination: 0.72,
      overallAccuracy: 0.5,
      averageReactionTime: 912,
    },
    errorTypeDistribution: [],
    highRiskLexicalPairs: [],
    chartPayload: {
      radarMetrics: [
        {
          code: 'positiveTransferScore',
          label: 'Positive Transfer',
          value: 0.64,
        },
      ],
      errorTypeDistribution: [],
      contextPerformance: [],
      lexicalTypePerformance: [],
      topRiskPairs: [],
      responseTimeline: [],
    },
    items: Array.from({ length: itemCount }, (_, index) => ({
      itemResultId: index + 1,
      templateItemId: index + 100,
      presentationOrder: index + 1,
      taskType: 'REACTION_TIME',
      lexicalPairId: index + 200,
      englishWord: `word-${index + 1}`,
      frenchWord: `mot-${index + 1}`,
      chineseGloss: `释义-${index + 1}`,
      lexicalPairType: 'FALSE_FRIEND',
      contextSupportLevel: 'MEDIUM',
      expectedSemanticMatch: false,
      stimulus: {
        instruction: '选择词义',
        promptText: `prompt-${index + 1}`,
        contextSentence: '',
      },
      options: [
        { key: 'A', label: '选项 A' },
        { key: 'B', label: '选项 B' },
      ],
      correctAnswerKey: 'B',
      selectedAnswerKey: index % 2 === 0 ? 'A' : 'B',
      reactionTimeMs: 800 + index,
      hesitationTimeMs: 100,
      correct: index % 2 === 1,
      semanticConsistent: index % 2 === 1,
      detectedErrorType: index % 2 === 1 ? 'NONE' : 'FALSE_FRIEND_BIAS',
      transferRiskScore: 0.2 + index / 100,
      itemScore: index % 2 === 1 ? 1 : 0,
    })),
  };
}

function createExplanation() {
  return {
    requestId: 'req-1',
    generationSource: 'llm',
    promptVersion: 'v1',
    model: 'gpt-test',
    latencyMs: 320,
    recommendationPath: [],
    focusLexicalPairs: [],
    recommendedTrainingModes: [],
    explanation: '学生主要受假朋友词影响。',
    teacherNote: '讲评时重点区分形近词与义项差异。',
    diagnosisInsight: {
      strengths: ['在常规题上稳定'],
      weaknesses: ['假朋友词误判较多'],
      suggestions: ['建议先做专项训练'],
    },
    confidence: 0.78,
    fallbackReason: null,
  };
}

describe('DiagnosisPdfReport', () => {
  it('splits answer breakdown pages into chunks of eight items', () => {
    const reportRef = React.createRef<HTMLDivElement>();
    const { container } = render(
      <DiagnosisPdfReport
        reportRef={reportRef}
        generatedAt="2026-04-15T08:30:00Z"
        result={createResult(10)}
        explanation={createExplanation()}
      />
    );

    expect(container.querySelectorAll('[data-pdf-page="true"]')).toHaveLength(4);
    expect(screen.getByText('本页展示第 9 - 10 题')).toBeInTheDocument();
    expect(screen.getByText('word-10 / mot-10')).toBeInTheDocument();
  });
});
