import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { PublicAssessmentQuestionVO } from '@/lib/contracts';
import { PublicQuestion } from './index';

const baseQuestion = (overrides: Partial<PublicAssessmentQuestionVO>): PublicAssessmentQuestionVO => ({
  questionId: 1,
  questionOrder: 2,
  questionType: 'SHORT_TEXT',
  itemCode: 'BASIC-NAME',
  sectionCode: 'BASIC_INFO',
  sectionTitle: '基本信息',
  sharedMaterial: null,
  formalSection: false,
  stemText: '您的姓名：',
  promptText: null,
  options: [],
  required: true,
  justificationRequired: false,
  displayCondition: null,
  responses: [],
  justificationText: null,
  ...overrides,
});

const renderNameField = (questionOrder: number, stemText: string) =>
  render(
    <>
      <h1 id={`question-${questionOrder}`}>{stemText}</h1>
      <PublicQuestion
        question={baseQuestion({ questionOrder, stemText })}
        responses={[]}
        justification=""
        disabled={false}
        reducedMotion
        onResponsesChange={() => undefined}
        onJustificationChange={() => undefined}
        labelledBy={`question-${questionOrder}`}
      />
    </>,
  );

describe('PublicQuestion profile field accessibility', () => {
  it('gives the name input a readable accessible name from the question stem', () => {
    renderNameField(2, '您的姓名：');
    const input = screen.getByRole('textbox');
    expect(input).toHaveAccessibleName('您的姓名：');
    expect(document.querySelector('label[for="text-answer-2"]')).not.toBeNull();
  });

  it('gives the contact input a readable accessible name from the question stem', () => {
    renderNameField(3, '您的联系方式是（电话/QQ/……）：');
    expect(screen.getByRole('textbox')).toHaveAccessibleName('您的联系方式是（电话/QQ/……）：');
  });

  it('renders text inputs with the touch-friendly research-text-input style class', () => {
    const { container } = renderNameField(2, '您的姓名：');
    const input = container.querySelector('input[type="text"]');
    expect(input).not.toBeNull();
    expect(input).toHaveClass('research-text-input');
  });
});
