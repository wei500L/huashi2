import type { AnalyticsRadarMetricVO, DiagnosisRadarMetric } from '@/lib/contracts';

const DIAGNOSIS_RADAR_MAX = 1;

export function toDiagnosisRadarChartMetrics(
  radarMetrics?: DiagnosisRadarMetric[] | null
): AnalyticsRadarMetricVO[] {
  return (radarMetrics || []).map((metric) => ({
    key: metric.code,
    label: metric.label,
    value: metric.value,
    max: DIAGNOSIS_RADAR_MAX,
  }));
}
