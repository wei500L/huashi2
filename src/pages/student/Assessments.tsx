import React from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { CheckCircle2, Clock3, FileText, Microscope, PlayCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { assessmentAttemptStatusLabel, assessmentAttemptStatusTone, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';
import type { StudentAssessmentSummaryVO } from '@/lib/contracts';
import type { TFunction } from 'i18next';

function resolveAction(item: StudentAssessmentSummaryVO, now: number, t: TFunction) {
  const startsAt = item.startsAt ? new Date(item.startsAt).getTime() : null;
  const dueAt = item.dueAt ? new Date(item.dueAt).getTime() : null;
  if (item.attemptStatus === 'SUBMITTED' && item.attemptId) {
    const resultAvailableAt = item.resultAvailableAt ? new Date(item.resultAvailableAt).getTime() : null;
    if (item.releaseStatus === 'PENDING' && (!resultAvailableAt || resultAvailableAt > now)) {
      return {
        label: `已交卷，结果将于 ${formatDateTime(item.resultAvailableAt)} 公布`,
        disabled: true,
        icon: Clock3,
      };
    }
    return { label: t('ui.actions.viewResult'), disabled: false, icon: CheckCircle2 };
  }
  if (startsAt && startsAt > now) {
    return { label: t('ui.meta.notStarted'), disabled: true, icon: Clock3 };
  }
  if (!item.attemptId && dueAt && dueAt <= now) {
    return { label: t('ui.meta.dueAt', { time: formatDateTime(item.dueAt) }), disabled: true, icon: Clock3 };
  }
  if (item.attemptId) {
    return { label: t('ui.actions.continueAnswering'), disabled: false, icon: PlayCircle };
  }
  return { label: t('ui.actions.start'), disabled: false, icon: PlayCircle };
}

const StudentAssessmentsPage: React.FC = () => {
  const { t } = useTranslation();
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [now, setNow] = React.useState(Date.now());
  const assessmentsQuery = useQuery({
    queryKey: ['student-assessments'],
    queryFn: ({ signal }) => assessmentService.listStudentAssessments({ signal }),
  });

  React.useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const startMutation = useMutation({
    mutationFn: (publishId: number) => assessmentService.startStudentAttempt(publishId),
    onSuccess: (attempt) => {
      setErrorMessage(null);
      const targetPath = attempt.status === 'SUBMITTED'
        ? `/assessments/attempts/${attempt.attemptId}/result`
        : `/assessments/attempts/${attempt.attemptId}`;
      window.location.assign(targetPath);
    },
    onError: (error) => {
      setErrorMessage(getApiErrorMessage(error, t('ui.actions.enterAssessmentCenter')));
    },
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow={t('shell.nav.assessments')}
        title={t('shell.titles.assessments')}
        subtitle={t('taskPages.studentAssessments.pageSubtitle')}
      />

      <section className="flex flex-col gap-5 rounded-[2rem] border border-primary/15 bg-primary/[0.06] p-6 sm:flex-row sm:items-center sm:justify-between dark:bg-primary/[0.08]">
        <div className="flex items-start gap-4">
          <div className="rounded-2xl bg-primary/10 p-3 text-primary"><Microscope size={20} /></div>
          <div>
            <SectionEyebrow>VOLUNTARY RESEARCH</SectionEyebrow>
            <h2 className="mt-2 text-lg font-black text-slate-900 dark:text-white">收到研究问卷发布编号？</h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 dark:text-white/55">
              研究问卷属于自愿参与的社会研究，不计入这里的课堂必测任务；请前往独立入口验证发布编号和参与码。
            </p>
          </div>
        </div>
        <Link to="/student/research" className="btn-liquid inline-flex shrink-0 items-center justify-center gap-2 px-5 py-3 text-sm font-bold text-white">
          进入研究问卷 <Microscope size={16} />
        </Link>
      </section>

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
          {t('ui.labels.loadingAssessments')}
        </div>
      )}

      {!assessmentsQuery.isLoading && !assessmentsQuery.data?.length && (
        <div className="rounded-[2.2rem] border border-dashed border-slate-300 bg-white/55 p-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
          {t('ui.labels.noAssessments')}
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-2">
        {(assessmentsQuery.data || []).map((item) => {
          const action = resolveAction(item, now, t);
          const ActionIcon = action.icon;
          return (
            <div key={item.publishId} className="rounded-[2.4rem] liquid-glass-panel p-7 space-y-5">
              <div className="inline-flex rounded-2xl bg-primary/10 p-3 text-primary">
                <FileText size={18} />
              </div>

              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <SectionEyebrow>{item.className}</SectionEyebrow>
                  <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{item.title}</div>
                  <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{item.description || t('ui.labels.noDescription')}</div>
                </div>
                <StatusBadge
                  label={item.attemptStatus ? assessmentAttemptStatusLabel(item.attemptStatus) : t('ui.meta.notStarted')}
                  tone={assessmentAttemptStatusTone(item.attemptStatus)}
                />
              </div>

              <div className="flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                <StatusBadge label={t('ui.meta.questionCount', { count: item.questionCount })} />
                <StatusBadge label={t('ui.meta.totalScore', { count: item.totalScore })} />
                <StatusBadge label={t('ui.meta.durationMinutes', { count: item.durationMinutes })} />
              </div>

              <div className="grid gap-2 text-sm text-slate-500 dark:text-white/45">
                <div>{t('ui.meta.startsAt', { time: formatDateTime(item.startsAt) })}</div>
                <div>{t('ui.meta.dueAt', { time: formatDateTime(item.dueAt) })}</div>
                <div>{t('ui.meta.publishAt', { time: formatDateTime(item.publishedAt) })}</div>
                {item.attemptId && <div>{t('ui.meta.progress', { current: item.answeredCount || 0, total: item.questionCount })}</div>}
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
                    window.location.assign(`/assessments/attempts/${item.attemptId}/result`);
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
