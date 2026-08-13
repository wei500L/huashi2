import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import type { PublicAssessmentQuestionVO } from '@/lib/contracts';
import { PublicQuestion, ResearchQuestionMap } from './index';

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

describe('ResearchQuestionMap navigation semantics', () => {
  it('exposes the current question and keeps every jump target named', () => {
    const onSelect = vi.fn();
    const profileQuestion = baseQuestion({ questionId: 2, questionOrder: 2, formalSection: false });
    const formalQuestion = baseQuestion({
      questionId: 10,
      questionOrder: 10,
      formalSection: true,
      sectionCode: 'P1A',
      sectionTitle: '正式题',
      itemCode: 'P1A-01',
      questionType: 'SINGLE_CHOICE',
      options: [{ key: 'A', label: '选项甲' }],
    });

    render(
      <ResearchQuestionMap
        questions={[profileQuestion, formalQuestion]}
        selectedOrder={10}
        responsesByOrder={{ 2: ['张三'] }}
        justificationsByOrder={{}}
        attachmentsByOrder={{}}
        onSelect={onSelect}
      />,
    );

    const navigation = screen.getByRole('navigation', { name: '题目跳转' });
    expect(navigation).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '资料1，已作答' })).not.toHaveAttribute('aria-current');
    expect(screen.getByRole('button', { name: '正式题1，未作答' })).toHaveAttribute('aria-current', 'step');

    fireEvent.click(screen.getByRole('button', { name: '正式题1，未作答' }));
    expect(onSelect).toHaveBeenCalledWith(1);
  });

  it('locks question jumps until the participant starts the timed section', () => {
    const onSelect = vi.fn();
    render(
      <ResearchQuestionMap
        questions={[baseQuestion({ questionId: 2, questionOrder: 2 })]}
        selectedOrder={2}
        responsesByOrder={{}}
        justificationsByOrder={{}}
        attachmentsByOrder={{}}
        navigationLocked
        onSelect={onSelect}
      />,
    );

    const jump = screen.getByRole('button', { name: '资料1，未作答' });
    expect(jump).toBeDisabled();
    fireEvent.click(jump);
    expect(onSelect).not.toHaveBeenCalled();
  });
});
