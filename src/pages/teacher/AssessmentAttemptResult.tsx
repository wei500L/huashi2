import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, FileText, XCircle } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

function questionTypeLabel(questionType: string) {
  switch (questionType) {
    case 'SINGLE_CHOICE':
      return '单选题';
    case 'MULTIPLE_CHOICE':
      return '多选题';
    case 'FILL_BLANK':
      return '填空题';
    default:
      return questionType;
  }
}

const TeacherAssessmentAttemptResultPage: React.FC = () => {
  const params = useParams<{ attemptId: string }>();
  const attemptId = Number(params.attemptId);
  const resultQuery = useQuery({
    queryKey: ['teacher-assessment-attempt-result', attemptId],
    queryFn: ({ signal }) => assessmentService.getTeacherAttemptResult(attemptId, { signal }),
    enabled: Number.isFinite(attemptId),
    retry: false,
  });

  const result = resultQuery.data;

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title={result?.paperTitle || '学生答卷'}
        subtitle={
          result
            ? `${result.studentName} · ${result.className} · 交卷时间 ${formatDateTime(result.submittedAt)}`
            : '正在加载学生答卷'
        }
        actions={
          <div className="flex flex-wrap gap-3">
            <Link to="/teacher/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
              返回测评列表
            </Link>
            {result && (
              <Link
                to={`/teacher/assessments/publishes/${result.publishId}`}
                className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
              >
                返回发布详情
              </Link>
            )}
          </div>
        }
      />

      {resultQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {getApiErrorMessage(resultQuery.error)}
        </div>
      )}

      {resultQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          正在加载学生答卷...
        </div>
      )}

      {result && (
        <>
          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-5">
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">student</div>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{result.studentName}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">作答学生</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">score</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{result.totalScore}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">总分</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">accuracy</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">
                {result.questionCount ? Math.round((result.correctCount / result.questionCount) * 100) : 0}%
              </div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">答对率</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">answered</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{result.answeredCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">已作答题数</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">submitted</div>
              <div className="mt-3 text-lg font-black text-slate-900 dark:text-white">{formatDateTime(result.submittedAt)}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">交卷时间</div>
            </div>
          </div>

          {result.instructionsText && (
            <div className="rounded-[2rem] border border-dashed border-slate-200/80 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
              {result.instructionsText}
            </div>
          )}

          <div className="space-y-5">
            {result.questions.map((question) => (
              <div key={question.answerId} className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">
                      Question {question.questionOrder} · {questionTypeLabel(question.questionType)}
                    </div>
                    <div className="mt-3 text-xl font-black text-slate-900 dark:text-white">{question.stemText}</div>
                    {question.promptText && <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{question.promptText}</div>}
                  </div>
                  <div
                    className={`rounded-full px-4 py-2 text-sm font-bold ${
                      question.correct ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300' : 'bg-rose-500/10 text-rose-600 dark:text-rose-300'
                    }`}
                  >
                    {question.correct ? '答对' : '答错'}
                  </div>
                </div>

                {!!question.options.length && (
                  <div className="grid gap-3">
                    {question.options.map((option) => {
                      const selected = question.responses.includes(option.key);
                      const correct = question.correctAnswers.includes(option.key);
                      return (
                        <div
                          key={option.key}
                          className={`rounded-[1.4rem] border px-4 py-4 text-sm ${
                            correct
                              ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                              : selected
                                ? 'border-rose-500/20 bg-rose-500/5 text-rose-600 dark:text-rose-300'
                                : 'border-slate-200/70 bg-white/70 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
                          }`}
                        >
                          <div className="flex items-start gap-3">
                            {correct ? <CheckCircle2 size={16} /> : selected ? <XCircle size={16} /> : <FileText size={16} />}
                            <div>
                              <div className="font-semibold">{option.key}</div>
                              <div className="mt-1">{option.label}</div>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {!question.options.length && (
                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                      <div className="font-semibold text-slate-900 dark:text-white">学生答案</div>
                      <div className="mt-2">{question.responses.join(' / ') || '未作答'}</div>
                    </div>
                    <div className="rounded-[1.4rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-4 text-sm text-emerald-700 dark:text-emerald-300">
                      <div className="font-semibold">正确答案</div>
                      <div className="mt-2">{question.correctAnswers.join(' / ') || '--'}</div>
                    </div>
                  </div>
                )}

                <div className="grid gap-3 text-sm md:grid-cols-3">
                  <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    得分：{question.scoreAwarded ?? 0} / {question.score}
                  </div>
                  <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    学生答案：{question.responses.join(' / ') || '未作答'}
                  </div>
                  <div className="rounded-[1.4rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-emerald-700 dark:text-emerald-300">
                    正确答案：{question.correctAnswers.join(' / ') || '--'}
                  </div>
                </div>

                {question.explanationText && (
                  <div className="rounded-[1.4rem] border border-dashed border-slate-200/80 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                    {question.explanationText}
                  </div>
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default TeacherAssessmentAttemptResultPage;
