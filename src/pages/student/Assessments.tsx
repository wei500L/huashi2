import React from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { CheckCircle2, Clock3, FileText, PlayCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';
import type { StudentAssessmentSummaryVO } from '@/lib/contracts';

function resolveAction(item: StudentAssessmentSummaryVO, now: number) {
  const startsAt = item.startsAt ? new Date(item.startsAt).getTime() : null;
  const dueAt = item.dueAt ? new Date(item.dueAt).getTime() : null;
  if (item.attemptStatus === 'SUBMITTED' && item.attemptId) {
    return { label: '查看结果', disabled: false, icon: CheckCircle2 };
  }
  if (startsAt && startsAt > now) {
    return { label: '未开始', disabled: true, icon: Clock3 };
  }
  if (!item.attemptId && dueAt && dueAt <= now) {
    return { label: '已截止', disabled: true, icon: Clock3 };
  }
  if (item.attemptId) {
    return { label: '继续作答', disabled: false, icon: PlayCircle };
  }
  return { label: '开始测评', disabled: false, icon: PlayCircle };
}

const StudentAssessmentsPage: React.FC = () => {
  const navigate = useNavigate();
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const now = Date.now();
  const assessmentsQuery = useQuery({
    queryKey: ['student-assessments'],
    queryFn: ({ signal }) => assessmentService.listStudentAssessments({ signal }),
  });

  const startMutation = useMutation({
    mutationFn: (publishId: number) => assessmentService.startStudentAttempt(publishId),
    onSuccess: (attempt) => {
      setErrorMessage(null);
      if (attempt.status === 'SUBMITTED') {
        navigate(`/assessments/attempts/${attempt.attemptId}/result`);
        return;
      }
      navigate(`/assessments/attempts/${attempt.attemptId}`);
    },
    onError: (error) => {
      setErrorMessage(getApiErrorMessage(error, '进入测评失败'));
    },
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="通用测评"
        subtitle="这里承接老师发布到班级的整卷测评。支持统一开始、整卷倒计时、题号导航和交卷后题目级回看。"
      />

      {errorMessage && (
        <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
          {errorMessage}
        </div>
      )}

      {assessmentsQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {assessmentsQuery.error.message}
        </div>
      )}

      {assessmentsQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          正在加载测评任务...
        </div>
      )}

      {!assessmentsQuery.isLoading && !assessmentsQuery.data?.length && (
        <div className="rounded-[2.2rem] border border-dashed border-slate-300 bg-white/55 p-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
          当前没有可作答的通用测评。
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-2">
        {(assessmentsQuery.data || []).map((item) => {
          const action = resolveAction(item, now);
          const ActionIcon = action.icon;
          return (
            <div key={item.publishId} className="rounded-[2.4rem] liquid-glass-panel p-7 space-y-5">
              <div className="inline-flex rounded-2xl bg-primary/10 p-3 text-primary">
                <FileText size={18} />
              </div>

              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{item.className}</div>
                  <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{item.title}</div>
                  <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{item.description || '无描述'}</div>
                </div>
                <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                  {item.attemptStatus || '待开始'}
                </div>
              </div>

              <div className="flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{item.questionCount} 题</span>
                <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{item.totalScore} 分</span>
                <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{item.durationMinutes} 分钟</span>
              </div>

              <div className="grid gap-2 text-sm text-slate-500 dark:text-white/45">
                <div>开始时间：{formatDateTime(item.startsAt)}</div>
                <div>截止时间：{formatDateTime(item.dueAt)}</div>
                <div>发布时间：{formatDateTime(item.publishedAt)}</div>
                {item.attemptId && <div>当前进度：{item.answeredCount || 0} / {item.questionCount}</div>}
              </div>

              {item.instructionsText && (
                <div className="rounded-[1.4rem] border border-dashed border-slate-200/80 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                  {item.instructionsText}
                </div>
              )}

              <button
                type="button"
                disabled={startMutation.isPending || action.disabled}
                onClick={() => {
                  if (item.attemptStatus === 'SUBMITTED' && item.attemptId) {
                    navigate(`/assessments/attempts/${item.attemptId}/result`);
                    return;
                  }
                  void startMutation.mutate(item.publishId);
                }}
                className={`inline-flex items-center gap-2 rounded-full px-5 py-3 text-sm font-bold ${
                  action.disabled
                    ? 'border border-slate-200 bg-white/70 text-slate-400 dark:border-white/10 dark:bg-white/5 dark:text-white/30'
                    : 'btn-liquid text-white'
                }`}
              >
                <ActionIcon size={16} />
                {action.label}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default StudentAssessmentsPage;
