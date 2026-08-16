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

const judgementQuestion = (overrides: Partial<PublicAssessmentQuestionVO> = {}): PublicAssessmentQuestionVO =>
  baseQuestion({
    questionId: 51,
    questionOrder: 51,
    questionType: 'TRUE_FALSE_WITH_JUSTIFICATION',
    itemCode: 'P4T3-01',
    sectionCode: 'P4T3',
    sectionTitle: '判断题',
    formalSection: true,
    stemText: 'Avignon était une destination qui plaisait à beaucoup de monde chaque année.',
    options: [
      { key: 'V', label: 'V' },
      { key: 'F', label: 'F' },
    ],
    justificationRequired: true,
    ...overrides,
  });

describe('TRUE_FALSE_WITH_JUSTIFICATION completion', () => {
  it('renders V and F option labels instead of 正确/错误', () => {
    render(
      <PublicQuestion
        question={judgementQuestion()}
        responses={[]}
        justification=""
        disabled={false}
        reducedMotion
        onResponsesChange={() => undefined}
        onJustificationChange={() => undefined}
        labelledBy="question-51"
      />,
    );

    expect(screen.getByRole('radio', { name: /V/ })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: /F/ })).toBeInTheDocument();
    expect(screen.queryByText('正确')).not.toBeInTheDocument();
    expect(screen.queryByText('错误')).not.toBeInTheDocument();
    const keys = [...document.querySelectorAll('.research-option-key')].map((node) => node.textContent);
    expect(keys).toEqual(['V', 'F']);
  });

  it('clears leftover justification when switching from F to V', () => {
    const onJustificationChange = vi.fn();
    render(
      <PublicQuestion
        question={judgementQuestion()}
        responses={['F']}
        justification="原文没有这么说"
        disabled={false}
        reducedMotion
        onResponsesChange={() => undefined}
        onJustificationChange={onJustificationChange}
        labelledBy="question-51"
      />,
    );

    fireEvent.click(screen.getByRole('radio', { name: /V/ }));
    expect(onJustificationChange).toHaveBeenCalledWith('');
  });
  it('treats selecting V as complete and hides the justification field', () => {
    const question = judgementQuestion();
    render(
      <>
        <ResearchQuestionMap
          questions={[question]}
          selectedOrder={51}
          responsesByOrder={{ 51: ['V'] }}
          justificationsByOrder={{}}
          attachmentsByOrder={{}}
          onSelect={() => undefined}
        />
        <PublicQuestion
          question={question}
          responses={['V']}
          justification=""
          disabled={false}
          reducedMotion
          onResponsesChange={() => undefined}
          onJustificationChange={() => undefined}
          labelledBy="question-51"
        />
      </>,
    );

    expect(screen.getByRole('button', { name: '正式题1，已作答' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: /V/ })).toBeChecked();
    expect(screen.queryByLabelText('请说明判断为错误的原因')).not.toBeInTheDocument();
  });

  it('shows the justification field and stays unanswered when F is selected without a reason', () => {
    const question = judgementQuestion();
    render(
      <>
        <ResearchQuestionMap
          questions={[question]}
          selectedOrder={51}
          responsesByOrder={{ 51: ['F'] }}
          justificationsByOrder={{}}
          attachmentsByOrder={{}}
          onSelect={() => undefined}
        />
        <PublicQuestion
          question={question}
          responses={['F']}
          justification=""
          disabled={false}
          reducedMotion
          onResponsesChange={() => undefined}
          onJustificationChange={() => undefined}
          labelledBy="question-51"
        />
      </>,
    );

    expect(screen.getByRole('button', { name: '正式题1，未作答' })).toBeInTheDocument();
    expect(screen.getByLabelText('请说明判断为错误的原因')).toBeInTheDocument();
  });

  it('marks F complete after a justification is filled', () => {
    const question = judgementQuestion();
    render(
      <ResearchQuestionMap
        questions={[question]}
        selectedOrder={51}
        responsesByOrder={{ 51: ['f'] }}
        justificationsByOrder={{ 51: '原文说每年吸引游客，不是令人不悦。' }}
        attachmentsByOrder={{}}
        onSelect={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: '正式题1，已作答' })).toBeInTheDocument();
  });
});
