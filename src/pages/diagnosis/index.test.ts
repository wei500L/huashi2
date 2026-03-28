import { describe, expect, it } from 'vitest';
import type { DiagnosisRadarMetric } from '@/lib/contracts';
import { toDiagnosisRadarChartMetrics } from './index';

describe('diagnosis result helpers', () => {
  it('maps backend radar metrics into chart metrics with a local max', () => {
    const metrics: DiagnosisRadarMetric[] = [
      {
        code: 'positiveTransferScore',
        label: 'Positive Transfer',
        value: 0.72,
      },
      {
        code: 'cognitiveFluency',
        label: 'Cognitive Fluency',
        value: 0.88,
      },
    ];

    expect(toDiagnosisRadarChartMetrics(metrics)).toEqual([
      {
        key: 'positiveTransferScore',
        label: 'Positive Transfer',
        value: 0.72,
        max: 1,
      },
      {
        key: 'cognitiveFluency',
        label: 'Cognitive Fluency',
        value: 0.88,
        max: 1,
      },
    ]);
  });

  it('returns an empty list when no radar metrics are available', () => {
    expect(toDiagnosisRadarChartMetrics(null)).toEqual([]);
  });
});
