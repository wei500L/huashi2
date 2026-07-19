import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDown, ArrowUp, Plus, Send, Trash2 } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { assessmentPaperStatusLabel, assessmentPaperStatusTone, assessmentQuestionTypeLabel, formatDateTime } from '@/lib/format';
import { assessmentService, teacherAnalyticsService } from '@/lib/services';
import type {
  AssessmentPaperDetailVO,
  AssessmentPaperSaveRequest,
  AssessmentQuestionRequest,
  AssessmentQuestionType,
  AssessmentResultReleasePolicy,
} from '@/lib/contracts';

type OptionDraft = {
  id: string;
  key: string;
  label: string;
};

type QuestionDraft = {
  id: string;
  questionType: AssessmentQuestionType;
  stemText: string;
  promptText: string;
  options: OptionDraft[];
  correctAnswers: string[];
  explanationText: string;
  score: number;
};

type PaperDraft = {
  title: string;
  description: string;
  durationMinutes: number;
  questions: QuestionDraft[];
};

type PublishDraft = {
  teachingClassId: string;
  startsAt: string;
  dueAt: string;
  instructionsText: string;
  resultReleasePolicy: AssessmentResultReleasePolicy;
};

const QUESTION_TYPE_OPTIONS: Array<{ value: AssessmentQuestionType; label: string }> = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'FILL_BLANK', label: '填空题' },
];

function createId() {
  return Math.random().toString(36).slice(2, 10);
}

function createOption(index: number): OptionDraft {
  return {
    id: createId(),
    key: String.fromCharCode(65 + index),
    label: '',
  };
}

function createQuestion(questionType: AssessmentQuestionType = 'SINGLE_CHOICE'): QuestionDraft {
  return {
    id: createId(),
    questionType,
    stemText: '',
    promptText: '',
    options: questionType === 'FILL_BLANK' ? [] : [createOption(0), createOption(1), createOption(2), createOption(3)],
    correctAnswers: [],
    explanationText: '',
    score: 10,
  };
}

function createEmptyDraft(): PaperDraft {
  return {
    title: '',
    description: '',
    durationMinutes: 30,
    questions: [createQuestion()],
  };
}

function toDraft(detail: AssessmentPaperDetailVO): PaperDraft {
  return {
    title: detail.title,
    description: detail.description || '',
    durationMinutes: detail.durationMinutes,
    questions: detail.questions.map((question) => ({
      id: createId(),
      questionType: question.questionType,
      stemText: question.stemText,
      promptText: question.promptText || '',
      options: question.options.map((option) => ({ id: createId(), key: option.key, label: option.label })),
      correctAnswers: question.correctAnswers.slice(),
      explanationText: question.explanationText || '',
      score: question.score,
    })),
  };
}

function serializeDraft(draft: PaperDraft): AssessmentPaperSaveRequest {
  return {
    title: draft.title.trim(),
    description: draft.description.trim() || undefined,
    durationMinutes: Number(draft.durationMinutes),
    questions: draft.questions.map<AssessmentQuestionRequest>((question) => ({
      questionType: question.questionType,
      stemText: question.stemText.trim(),
      promptText: question.promptText.trim() || undefined,
      options:
        question.questionType === 'FILL_BLANK'
          ? undefined
          : question.options.map((option) => ({
              key: option.key,
              label: option.label.trim(),
            })),
      correctAnswers:
        question.questionType === 'FILL_BLANK'
          ? question.correctAnswers.map((answer) => answer.trim()).filter(Boolean)
          : question.correctAnswers,
      explanationText: question.explanationText.trim() || undefined,
      score: Number(question.score),
    })),
  };
}

const TeacherAssessmentEditorPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const params = useParams<{ paperId?: string }>();
  const paperId = params.paperId ? Number(params.paperId) : null;
  const isCreateMode = !paperId;
  const [draft, setDraft] = React.useState<PaperDraft>(createEmptyDraft);
  const [publishDraft, setPublishDraft] = React.useState<PublishDraft>({
    teachingClassId: '',
    startsAt: '',
    dueAt: '',
    instructionsText: '',
    resultReleasePolicy: 'AFTER_DUE',
  });
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const hydratedPaperIdRef = React.useRef<number | null>(null);

  const detailQuery = useQuery({
    queryKey: ['teacher-assessment-paper', paperId],
    queryFn: ({ signal }) => assessmentService.getTeacherPaper(paperId as number, { signal }),
    enabled: !!paperId,
  });

  const classesQuery = useQuery({
    queryKey: ['teacher-assessment-classes'],
    queryFn: ({ signal }) => teacherAnalyticsService.listClasses({ signal }),
  });

  React.useEffect(() => {
    if (!detailQuery.data) {
      return;
    }
    if (hydratedPaperIdRef.current === detailQuery.data.paperId) {
      return;
    }
    hydratedPaperIdRef.current = detailQuery.data.paperId;
    setDraft(toDraft(detailQuery.data));
  }, [detailQuery.data]);

  const isEditLocked = !!detailQuery.data?.publishes.length;

  const saveMutation = useMutation({
    mutationFn: (payload: AssessmentPaperSaveRequest) =>
      paperId ? assessmentService.updateTeacherPaper(paperId, payload) : assessmentService.createTeacherPaper(payload),
    onSuccess: async (data) => {
      hydratedPaperIdRef.current = data.paperId;
      setDraft(toDraft(data));
      setFeedback(paperId ? '试卷已保存。' : '试卷已创建。');
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['teacher-assessment-papers'] });
      await queryClient.invalidateQueries({ queryKey: ['teacher-assessment-paper', data.paperId] });
      if (!paperId) {
        navigate(`/teacher/assessments/${data.paperId}`, { replace: true });
      }
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '保存试卷失败'));
    },
  });

  const publishMutation = useMutation({
    mutationFn: () =>
      assessmentService.publishTeacherPaper(paperId as number, {
        teachingClassId: Number(publishDraft.teachingClassId),
        startsAt: publishDraft.startsAt || undefined,
        dueAt: publishDraft.dueAt || undefined,
        instructionsText: publishDraft.instructionsText.trim() || undefined,
        resultReleasePolicy: publishDraft.resultReleasePolicy,
      }),
    onSuccess: async () => {
      setFeedback('试卷已发布到班级。');
      setErrorMessage(null);
      setPublishDraft({
        teachingClassId: '',
        startsAt: '',
        dueAt: '',
        instructionsText: '',
        resultReleasePolicy: 'AFTER_DUE',
      });
      await queryClient.invalidateQueries({ queryKey: ['teacher-assessment-paper', paperId] });
      await queryClient.invalidateQueries({ queryKey: ['teacher-assessment-papers'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '发布试卷失败'));
    },
  });

  const updateQuestion = React.useCallback((questionId: string, updater: (question: QuestionDraft) => QuestionDraft) => {
    setDraft((current) => ({
      ...current,
      questions: current.questions.map((question) => (question.id === questionId ? updater(question) : question)),
    }));
  }, []);

  const moveQuestion = React.useCallback((index: number, direction: -1 | 1) => {
    setDraft((current) => {
      const nextIndex = index + direction;
      if (nextIndex < 0 || nextIndex >= current.questions.length) {
        return current;
      }
      const questions = current.questions.slice();
      [questions[index], questions[nextIndex]] = [questions[nextIndex], questions[index]];
      return { ...current, questions };
    });
  }, []);

  const renderQuestionEditor = (question: QuestionDraft, index: number) => {
    const isSingleChoice = question.questionType === 'SINGLE_CHOICE';
    const isFillBlank = question.questionType === 'FILL_BLANK';

    return (
      <div key={question.id} className="rounded-[2rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <SectionEyebrow>第 {index + 1} 题</SectionEyebrow>
            <div className="mt-2 text-lg font-black text-slate-900 dark:text-white">{assessmentQuestionTypeLabel(question.questionType)}</div>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={isEditLocked || index === 0}
              onClick={() => moveQuestion(index, -1)}
              className="rounded-2xl border border-slate-200 px-3 py-2 text-sm disabled:opacity-40 dark:border-white/10"
            >
              <ArrowUp size={14} />
            </button>
            <button
              type="button"
              disabled={isEditLocked || index === draft.questions.length - 1}
              onClick={() => moveQuestion(index, 1)}
              className="rounded-2xl border border-slate-200 px-3 py-2 text-sm disabled:opacity-40 dark:border-white/10"
            >
              <ArrowDown size={14} />
            </button>
            <button
              type="button"
              disabled={isEditLocked || draft.questions.length === 1}
              onClick={() =>
                setDraft((current) => ({
                  ...current,
                  questions: current.questions.filter((item) => item.id !== question.id),
                }))
              }
              className="rounded-2xl border border-rose-500/20 px-3 py-2 text-sm text-rose-500 disabled:opacity-40"
            >
              <Trash2 size={14} />
            </button>
          </div>
        </div>

        <div className="mt-5 grid gap-4 md:grid-cols-[1fr_180px_140px]">
          <label className="space-y-2 text-sm">
            <span className="text-slate-500 dark:text-white/45">题干</span>
            <textarea
              value={question.stemText}
              onChange={(event) => updateQuestion(question.id, (current) => ({ ...current, stemText: event.target.value }))}
              rows={3}
              disabled={isEditLocked}
              className="w-full rounded-[1.4rem] border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
            />
          </label>

          <label className="space-y-2 text-sm">
            <span className="text-slate-500 dark:text-white/45">题型</span>
            <select
              value={question.questionType}
              disabled={isEditLocked}
              onChange={(event) =>
                updateQuestion(question.id, (current) => {
                  const nextType = event.target.value as AssessmentQuestionType;
                  if (nextType === 'FILL_BLANK') {
                    return {
                      ...current,
                      questionType: nextType,
                      options: [],
                      correctAnswers: [],
                    };
                  }
                  const nextOptions = current.options.length >= 2 ? current.options : [createOption(0), createOption(1), createOption(2), createOption(3)];
                  return {
                    ...current,
                    questionType: nextType,
                    options: nextOptions,
                    correctAnswers: nextType === 'SINGLE_CHOICE' ? current.correctAnswers.slice(0, 1) : current.correctAnswers,
                  };
                })
              }
              className="native-select w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
            >
              {QUESTION_TYPE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="space-y-2 text-sm">
            <span className="text-slate-500 dark:text-white/45">分值</span>
            <input
              type="number"
              min={1}
              step={1}
              value={question.score}
              disabled={isEditLocked}
              onChange={(event) =>
                updateQuestion(question.id, (current) => ({
                  ...current,
                  score: Number(event.target.value || 1),
                }))
              }
              className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
            />
          </label>
        </div>

        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label className="space-y-2 text-sm">
            <span className="text-slate-500 dark:text-white/45">补充说明</span>
            <input
              value={question.promptText}
              disabled={isEditLocked}
              onChange={(event) => updateQuestion(question.id, (current) => ({ ...current, promptText: event.target.value }))}
              className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
              placeholder="例如：可多选；大小写不敏感；按语境选择"
            />
          </label>

          <label className="space-y-2 text-sm">
            <span className="text-slate-500 dark:text-white/45">解析</span>
            <input
              value={question.explanationText}
              disabled={isEditLocked}
              onChange={(event) => updateQuestion(question.id, (current) => ({ ...current, explanationText: event.target.value }))}
              className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
              placeholder="按发布时选择的结果公布方式向学生展示"
            />
          </label>
        </div>

        {!isFillBlank && (
          <div className="mt-5 space-y-3">
            <div className="flex items-center justify-between gap-4">
              <div className="text-sm font-semibold text-slate-700 dark:text-white/70">选项与答案</div>
              <button
                type="button"
                disabled={isEditLocked}
                onClick={() =>
                  updateQuestion(question.id, (current) => ({
                    ...current,
                    options: [...current.options, createOption(current.options.length)],
                  }))
                }
                className="rounded-full border border-slate-200 px-3 py-2 text-xs font-bold disabled:opacity-40 dark:border-white/10"
              >
                添加选项
              </button>
            </div>

            {question.options.map((option) => {
              const selected = question.correctAnswers.includes(option.key);
              return (
                <div key={option.id} className="grid gap-3 md:grid-cols-[80px_1fr_140px]">
                  <div className="rounded-2xl border border-slate-200/70 bg-white/70 px-4 py-3 text-center font-black text-slate-700 dark:border-white/10 dark:bg-white/5 dark:text-white/70">
                    {option.key}
                  </div>
                  <input
                    value={option.label}
                    disabled={isEditLocked}
                    onChange={(event) =>
                      updateQuestion(question.id, (current) => ({
                        ...current,
                        options: current.options.map((item) =>
                          item.id === option.id ? { ...item, label: event.target.value } : item
                        ),
                      }))
                    }
                    className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
                    placeholder={`选项 ${option.key}`}
                  />
                  <div className="flex items-center justify-between gap-3 rounded-2xl border border-slate-200/70 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">
                    <label className="inline-flex items-center gap-2 text-sm text-slate-600 dark:text-white/60">
                      <input
                        type={isSingleChoice ? 'radio' : 'checkbox'}
                        name={`correct-${question.id}`}
                        checked={selected}
                        disabled={isEditLocked}
                        onChange={() =>
                          updateQuestion(question.id, (current) => ({
                            ...current,
                            correctAnswers: isSingleChoice
                              ? [option.key]
                              : selected
                                ? current.correctAnswers.filter((item) => item !== option.key)
                                : [...current.correctAnswers, option.key],
                          }))
                        }
                      />
                      正确
                    </label>
                    <button
                      type="button"
                      disabled={isEditLocked || question.options.length <= 2}
                      onClick={() =>
                        updateQuestion(question.id, (current) => ({
                          ...current,
                          options: current.options.filter((item) => item.id !== option.id),
                          correctAnswers: current.correctAnswers.filter((item) => item !== option.key),
                        }))
                      }
                      className="text-rose-500 disabled:opacity-40"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {isFillBlank && (
          <label className="mt-5 block space-y-2 text-sm">
            <span className="text-slate-500 dark:text-white/45">可接受答案</span>
            <textarea
              value={question.correctAnswers.join('\n')}
              disabled={isEditLocked}
              onChange={(event) =>
                updateQuestion(question.id, (current) => ({
                  ...current,
                  correctAnswers: event.target.value.split('\n').map((item) => item.trim()).filter(Boolean),
                }))
              }
              rows={4}
              className="w-full rounded-[1.6rem] border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
              placeholder="每行一个可接受答案"
            />
          </label>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow="通用测评"
        title={isCreateMode ? '新建通用测评' : '编辑通用测评'}
        subtitle={
          isCreateMode
            ? '本页负责整卷编辑。v1 只开放单选、多选、填空三种题型，并使用整卷统一倒计时。'
            : `试卷 #${paperId} · 最近发布 ${formatDateTime(detailQuery.data?.latestPublishAt)}`
        }
        actions={
          <div className="flex flex-wrap gap-3">
            <Link to="/teacher/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
              返回列表
            </Link>
            <button
              type="button"
              disabled={saveMutation.isPending || (detailQuery.isLoading && !isCreateMode) || isEditLocked}
              onClick={() => saveMutation.mutate(serializeDraft(draft))}
              className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
            >
              保存试卷
            </button>
          </div>
        }
      />

      {isEditLocked && (
        <div className="rounded-[1.8rem] border border-amber-500/20 bg-amber-500/10 px-5 py-4 text-sm text-amber-700 dark:text-amber-300">
          当前试卷已经发布。为避免发布后题目漂移，编辑区已锁定；你仍可继续发布到其他班级。
        </div>
      )}

      {(feedback || errorMessage) && (
        <div className={`rounded-[1.8rem] px-5 py-4 text-sm ${feedback ? 'border border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400' : 'border border-rose-500/20 bg-rose-500/5 text-rose-500'}`}>
          {feedback || errorMessage}
        </div>
      )}

      {detailQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {detailQuery.error.message}
        </div>
      )}

      <div className="grid gap-8 xl:grid-cols-[1.2fr_0.8fr]">
        <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-6">
          <div className="grid gap-4 md:grid-cols-[1fr_180px]">
            <label className="space-y-2 text-sm">
              <span className="text-slate-500 dark:text-white/45">试卷标题</span>
              <input
                value={draft.title}
                disabled={isEditLocked}
                onChange={(event) => setDraft((current) => ({ ...current, title: event.target.value }))}
                className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
                placeholder="例如：Unit 3 阅读与词汇测评"
              />
            </label>

            <label className="space-y-2 text-sm">
              <span className="text-slate-500 dark:text-white/45">时长（分钟）</span>
              <input
                type="number"
                min={1}
                step={1}
                value={draft.durationMinutes}
                disabled={isEditLocked}
                onChange={(event) =>
                  setDraft((current) => ({
                    ...current,
                    durationMinutes: Number(event.target.value || 1),
                  }))
                }
                className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
              />
            </label>
          </div>

          <label className="block space-y-2 text-sm">
            <span className="text-slate-500 dark:text-white/45">试卷描述</span>
            <textarea
              value={draft.description}
              disabled={isEditLocked}
              onChange={(event) => setDraft((current) => ({ ...current, description: event.target.value }))}
              rows={3}
              className="w-full rounded-[1.8rem] border border-slate-200 bg-white/70 px-4 py-3 disabled:opacity-70 dark:border-white/10 dark:bg-white/5"
            />
          </label>

          <div className="flex items-center justify-between gap-4">
            <div>
              <div className="text-lg font-black text-slate-900 dark:text-white">题目列表</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">支持单选、多选、填空；学生端按整卷作答，并有统一交卷与结果回看。</div>
            </div>
            <button
              type="button"
              disabled={isEditLocked}
              onClick={() => setDraft((current) => ({ ...current, questions: [...current.questions, createQuestion()] }))}
              className="rounded-full border border-slate-200 px-4 py-3 text-sm font-bold disabled:opacity-40 dark:border-white/10"
            >
              <Plus size={14} className="inline-block mr-2" />
              添加题目
            </button>
          </div>

          <div className="space-y-5">
            {draft.questions.map((question, index) => renderQuestionEditor(question, index))}
          </div>
        </section>

        <section className="space-y-8">
          <div className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
            <div>
              <SectionEyebrow>概览</SectionEyebrow>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">试卷概览</div>
            </div>

            <div className="grid gap-3 text-sm text-slate-600 dark:text-white/60">
              <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">题目数：{draft.questions.length}</div>
              <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">
                总分：{draft.questions.reduce((sum, item) => sum + Number(item.score || 0), 0)}
              </div>
              <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">时长：{draft.durationMinutes} 分钟</div>
              {!isCreateMode && (
                <div className="flex items-center justify-between rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">
                  <span>状态</span>
                  <StatusBadge
                    label={assessmentPaperStatusLabel(detailQuery.data?.status)}
                    tone={assessmentPaperStatusTone(detailQuery.data?.status)}
                  />
                </div>
              )}
            </div>
          </div>

          <div className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
            <div>
              <SectionEyebrow>发布</SectionEyebrow>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">发布到班级</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">先保存试卷，再选择班级和时间窗。整卷计时会在学生开始作答后立即生效。</div>
            </div>

            {isCreateMode ? (
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-4 py-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                先保存试卷，保存后才可发布。
              </div>
            ) : (
              <>
                <label className="block space-y-2 text-sm">
                  <span className="text-slate-500 dark:text-white/45">班级</span>
                  <select
                    value={publishDraft.teachingClassId}
                    onChange={(event) => setPublishDraft((current) => ({ ...current, teachingClassId: event.target.value }))}
                    className="native-select w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  >
                    <option value="">选择班级</option>
                    {(classesQuery.data || []).map((item) => (
                      <option key={item.classId} value={item.classId}>
                        {item.className}
                      </option>
                    ))}
                  </select>
                </label>

                <div className="grid gap-4 md:grid-cols-2">
                  <label className="space-y-2 text-sm">
                    <span className="text-slate-500 dark:text-white/45">开始时间</span>
                    <input
                      type="datetime-local"
                      value={publishDraft.startsAt}
                      onChange={(event) => setPublishDraft((current) => ({ ...current, startsAt: event.target.value }))}
                      className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                  <label className="space-y-2 text-sm">
                    <span className="text-slate-500 dark:text-white/45">截止时间</span>
                    <input
                      type="datetime-local"
                      value={publishDraft.dueAt}
                      onChange={(event) => setPublishDraft((current) => ({ ...current, dueAt: event.target.value }))}
                      className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                </div>

                <label className="block space-y-2 text-sm">
                  <span className="text-slate-500 dark:text-white/45">结果公布方式</span>
                  <select
                    value={publishDraft.resultReleasePolicy}
                    onChange={(event) => setPublishDraft((current) => ({
                      ...current,
                      resultReleasePolicy: event.target.value as AssessmentResultReleasePolicy,
                    }))}
                    className="native-select w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  >
                    <option value="AFTER_DUE">截止后公布（正式测评）</option>
                    <option value="IMMEDIATE">交卷后立即公布（练习）</option>
                  </select>
                  <span className="block text-xs text-slate-400 dark:text-white/35">
                    截止后公布时，学生提前交卷不会看到分数、正确答案或解析。
                  </span>
                </label>

                <label className="block space-y-2 text-sm">
                  <span className="text-slate-500 dark:text-white/45">作答说明</span>
                  <textarea
                    value={publishDraft.instructionsText}
                    onChange={(event) => setPublishDraft((current) => ({ ...current, instructionsText: event.target.value }))}
                    rows={4}
                    className="w-full rounded-[1.8rem] border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    placeholder="例如：请独立完成；交卷后不可再次进入；填空题大小写不作区分"
                  />
                </label>

                <button
                  type="button"
                  disabled={
                    publishMutation.isPending ||
                    !publishDraft.teachingClassId ||
                    (publishDraft.resultReleasePolicy === 'AFTER_DUE' && !publishDraft.dueAt)
                  }
                  onClick={() => publishMutation.mutate()}
                  className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:opacity-60"
                >
                  <Send size={14} />
                  发布到班级
                </button>
              </>
            )}
          </div>

          {!isCreateMode && (
            <div className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
              <div>
                <SectionEyebrow>记录</SectionEyebrow>
                <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">发布记录</div>
              </div>

              {!detailQuery.data?.publishes.length && (
                <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-4 py-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                  当前还没有发布记录。
                </div>
              )}

              <div className="space-y-4">
                {(detailQuery.data?.publishes || []).map((publish) => (
                  <Link
                    key={publish.publishId}
                    to={`/teacher/assessments/publishes/${publish.publishId}`}
                    className="block rounded-[1.6rem] border border-slate-200/70 bg-white/70 p-4 text-sm transition-all hover:border-primary/40 dark:border-white/10 dark:bg-white/5"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-4">
                      <div>
                        <div className="font-black text-slate-900 dark:text-white">{publish.className}</div>
                        <div className="mt-2 text-slate-500 dark:text-white/45">
                          开始 {formatDateTime(publish.startsAt)} · 截止 {formatDateTime(publish.dueAt)}
                        </div>
                      </div>
                      <StatusBadge label={String(publish.status || '--')} />
                    </div>
                    <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">已分配 {publish.assignedCount}</span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">尝试 {publish.attemptCount}</span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">已交卷 {publish.submittedCount}</span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">待完成 {publish.pendingCount}</span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">发布于 {formatDateTime(publish.publishedAt)}</span>
                    </div>
                    {publish.instructionsText && (
                      <div className="mt-3 rounded-[1.2rem] border border-dashed border-slate-200/70 px-4 py-3 text-slate-500 dark:border-white/10 dark:text-white/50">
                        {publish.instructionsText}
                      </div>
                    )}
                    <div className="mt-3 text-xs font-bold text-primary">查看发布详情与学生名册</div>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default TeacherAssessmentEditorPage;
