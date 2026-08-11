import type { PublicAssessmentProfileFieldVO } from '@/lib/contracts';

export type ProfileValues = Record<string, string>;

export function profileFieldVisible(field: PublicAssessmentProfileFieldVO, values: ProfileValues): boolean {
  if (!field.displayCondition) return true;
  return field.displayCondition.operator === 'EQ'
    && (values[field.displayCondition.fieldCode] || '').toUpperCase() === field.displayCondition.value.toUpperCase();
}

export function pruneHiddenProfileValues(
  fields: PublicAssessmentProfileFieldVO[],
  values: ProfileValues,
): ProfileValues {
  const next = { ...values };
  for (const field of fields) {
    if (!profileFieldVisible(field, next)) delete next[field.itemCode];
  }
  return next;
}

export function formatElapsed(milliseconds: number): string {
  const seconds = Math.max(0, Math.floor(milliseconds / 1000));
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')} 分钟 ${String(seconds % 60).padStart(2, '0')} 秒`;
}

export function findOccurrence(source: string, target: string, occurrence = 1): number {
  let from = 0;
  let found = -1;
  for (let count = 0; count < Math.max(1, occurrence); count += 1) {
    found = source.indexOf(target, from);
    if (found < 0) return -1;
    from = found + target.length;
  }
  return found;
}
