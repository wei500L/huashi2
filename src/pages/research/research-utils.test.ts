import { describe, expect, it } from 'vitest';
import type { PublicAssessmentProfileFieldVO } from '@/lib/contracts';
import { findOccurrence, formatElapsed, profileFieldVisible, pruneHiddenProfileValues } from './research-utils';

const fields: PublicAssessmentProfileFieldVO[] = [
  {
    itemCode: 'BASIC-ENGLISH-MAJOR',
    questionType: 'SINGLE_CHOICE',
    label: '英语专业背景',
    options: [],
    required: true,
  },
  {
    itemCode: 'BASIC-CET4',
    questionType: 'NUMBER',
    label: 'CET4',
    options: [],
    required: false,
  },
  {
    itemCode: 'BASIC-TEM4',
    questionType: 'NUMBER',
    label: 'TEM4',
    options: [],
    required: false,
    displayCondition: {
      fieldCode: 'BASIC-ENGLISH-MAJOR',
      operator: 'EQ',
      value: 'ENGLISH_MAJOR',
    },
  },
];

describe('research questionnaire utilities', () => {
  it('shows TEM fields only for English majors and prunes them after switching branches', () => {
    expect(profileFieldVisible(fields[2], { 'BASIC-ENGLISH-MAJOR': 'ENGLISH_MAJOR' })).toBe(true);
    expect(profileFieldVisible(fields[2], { 'BASIC-ENGLISH-MAJOR': 'NON_ENGLISH_MAJOR' })).toBe(false);
    expect(pruneHiddenProfileValues(fields, {
      'BASIC-ENGLISH-MAJOR': 'NON_ENGLISH_MAJOR',
      'BASIC-CET4': '510',
      'BASIC-TEM4': '70',
    })).toEqual({
      'BASIC-ENGLISH-MAJOR': 'NON_ENGLISH_MAJOR',
      'BASIC-CET4': '510',
    });
  });

  it('formats dual timers and resolves an explicit emphasis occurrence', () => {
    expect(formatElapsed(65_999)).toBe('01 分钟 05 秒');
    expect(formatElapsed(-1)).toBe('00 分钟 00 秒');
    expect(findOccurrence('important puis important', 'important', 2)).toBe(15);
    expect(findOccurrence('important', 'absent')).toBe(-1);
  });
});
