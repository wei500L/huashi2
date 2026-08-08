import { describe, expect, it } from 'vitest';
import { validateEditorPaperDraft } from './AssessmentEditor';

type Draft = Parameters<typeof validateEditorPaperDraft>[0];
type Question = Draft['questions'][number];

function question(overrides: Partial<Question>): Question {
  return {
    id: crypto.randomUUID(),
    questionType: 'SHORT_TEXT',
    stemText: '基本信息',
    promptText: '',
    options: [],
    correctAnswers: [],
    explanationText: '',
    score: 0,
    sectionCode: 'BASIC_INFO',
    requiredAnswer: true,
    weight: 1,
    optionExplanations: {},
    displayCondition: {},
    ...overrides,
  };
}

function draft(questions: Question[]): Draft {
  return {
    title: 'Lexi-Bridge research questionnaire',
    description: '',
    durationMinutes: 30,
    questions,
  };
}

describe('validateEditorPaperDraft research questionnaire rules', () => {
  it('accepts the eleven non-scored basic-information items used in production', () => {
    const questions = [
      question({ questionType: 'INSTRUCTION', stemText: '填写说明' }),
      question({ questionType: 'SHORT_TEXT', stemText: '姓名' }),
      question({ questionType: 'SHORT_TEXT', stemText: '学校' }),
      question({ questionType: 'SHORT_TEXT', stemText: '联系方式' }),
      question({ questionType: 'NUMBER', stemText: '年龄' }),
      question({ questionType: 'NUMBER', stemText: '英语学习年限' }),
      question({ questionType: 'NUMBER', stemText: '英语考试成绩' }),
      question({ questionType: 'NUMBER', stemText: '自评英语水平' }),
      question({ questionType: 'NUMBER', stemText: '每日英语接触时长' }),
      question({
        questionType: 'SINGLE_CHOICE',
        stemText: '是否有海外学习经历',
        options: [{ id: 'a', key: 'A', label: '是' }, { id: 'b', key: 'B', label: '否' }],
      }),
      question({
        questionType: 'SINGLE_CHOICE',
        stemText: '是否同意参与研究',
        options: [{ id: 'a', key: 'A', label: '同意' }, { id: 'b', key: 'B', label: '不同意' }],
      }),
    ];

    expect(validateEditorPaperDraft(draft(questions), true)).toEqual([]);
  });

  it('rejects a scored single-choice item without a correct answer', () => {
    const errors = validateEditorPaperDraft(draft([
      question({
        questionType: 'SINGLE_CHOICE',
        sectionCode: 'FORMAL',
        stemText: '正式单选题',
        score: 1,
        options: [{ id: 'a', key: 'A', label: '选项 A' }, { id: 'b', key: 'B', label: '选项 B' }],
      }),
    ]), true);

    expect(errors).toHaveLength(1);
  });

  it('rejects a scored true-false item without a correct answer', () => {
    const errors = validateEditorPaperDraft(draft([
      question({
        questionType: 'TRUE_FALSE_WITH_JUSTIFICATION',
        sectionCode: 'FORMAL',
        stemText: '正式判断题',
        score: 1,
        options: [{ id: 'a', key: 'TRUE', label: '正确' }, { id: 'b', key: 'FALSE', label: '错误' }],
      }),
    ]), true);

    expect(errors).toHaveLength(1);
  });
});
