import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, FileText, XCircle } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/common';
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

const StudentAssessmentResultPage: React.FC = () => {
  const params = useParams<{ attemptId: string }>();
  const attemptId = Number(params.attemptId);
  const resultQuery = useQuery({
    queryKey: ['student-assessment-result', attemptId],
    queryFn: ({ signal }) => assessmentService.getStudentAttemptResult(attemptId, { signal }),
    enabled: Number.isFinite(attemptId),
    retry: false,
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title={resultQuery.data?.paperTitle || '测评结果'}
        subtitle={resultQuery.data ? `${resultQuery.data.className} · 交卷时间 ${formatDateTime(resultQuery.data.submittedAt)}` : '正在加载测评结果'}
        actions={
          <Link to="/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
            返回测评列表
          </Link>
        }
      />

      {resultQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {resultQuery.error.message}
        </div>
      )}

      {resultQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          正在加载测评结果...
        </div>
      )}

      {resultQuery.data && (
        <>
          <div className="grid gap-6 md:grid-cols-4">
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">score</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{resultQuery.data.totalScore}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">客观得分</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">accuracy</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">
                {resultQuery.data.questionCount ? Math.round((resultQuery.data.correctCount / resultQuery.data.questionCount) * 100) : 0}%
              </div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">答对率</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">answered</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{resultQuery.data.answeredCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">已作答题数</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">time</div>
              <div className="mt-3 text-xl font-black text-slate-900 dark:text-white">{formatDateTime(resultQuery.data.startedAt)}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">开始作答</div>
            </div>
          </div>

          {resultQuery.data.instructionsText && (
            <div className="rounded-[2rem] border border-dashed border-slate-200/80 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
              {resultQuery.data.instructionsText}
            </div>
          )}

          <div className="space-y-5">
            {resultQuery.data.questions.map((question) => (
              <div key={question.answerId} className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">
                      Question {question.questionOrder} · {questionTypeLabel(question.questionType)}
                    </div>
                    <div className="mt-3 text-xl font-black text-slate-900 dark:text-white">{question.stemText}</div>
                    {question.promptText && <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{question.promptText}</div>}
                  </div>
                  <div className={`rounded-full px-4 py-2 text-sm font-bold ${question.correct ? 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300' : 'bg-rose-500/10 text-rose-600 dark:text-rose-300'}`}>
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
                      <div className="font-semibold text-slate-900 dark:text-white">你的答案</div>
                      <div className="mt-2">{question.responses.join(' / ') || '未作答'}</div>
                    </div>
                    <div className="rounded-[1.4rem] border border-emerald-500/20 bg-emerald-500/10 px-4 py-4 text-sm text-emerald-700 dark:text-emerald-300">
                      <div className="font-semibold">正确答案</div>
                      <div className="mt-2">{question.correctAnswers.join(' / ') || '--'}</div>
                    </div>
                  </div>
                )}

                <div className="grid gap-3 md:grid-cols-3 text-sm">
                  <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    得分：{question.scoreAwarded ?? 0} / {question.score}
                  </div>
                  <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    你的答案：{question.responses.join(' / ') || '未作答'}
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

export default StudentAssessmentResultPage;
