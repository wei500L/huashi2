import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { DataTable, PageHeader, SectionEyebrow, StatusBadge, WorkflowStepper } from '@/components/common';
import type { WorkflowStage } from '@/components/common';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import { assessmentAttemptStatusLabel, assessmentAttemptStatusTone, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

const TeacherAssessmentPublishDetailPage: React.FC = () => {
  const { t } = useTranslation();
  const params = useParams<{ publishId: string }>();
  const publishId = Number(params.publishId);
  const publishQuery = useQuery({
    queryKey: ['teacher-assessment-publish', publishId],
    queryFn: ({ signal }) => assessmentService.getTeacherPublish(publishId, { signal }),
    enabled: Number.isFinite(publishId),
  });

  const publish = publishQuery.data;
  const accessDenied = publishQuery.error ? [401, 403].includes(normalizeApiError(publishQuery.error).status) : false;
  const allAssignedStudentsSubmitted = Boolean(
    publish && publish.assignedCount > 0 && publish.submittedCount >= publish.assignedCount
  );
  const publishedSaveState = t('ui.pages.publishDetail.workflow.publishedSave');
  const workflowStages: WorkflowStage[] = publish ? [
    {
      key: 'input', label: t('ui.pages.publishDetail.workflow.inputLabel'), status: 'complete', statusLabel: t('ui.pages.publishDetail.workflow.completeStatus'),
      reason: t('ui.pages.publishDetail.workflow.noReason'), fallback: t('ui.pages.publishDetail.workflow.inputFallback'),
      saveState: publishedSaveState, nextAction: t('ui.pages.publishDetail.workflow.inputNext'),
    },
    {
      key: 'validation', label: t('ui.pages.publishDetail.workflow.validationLabel'), status: 'complete', statusLabel: t('ui.pages.publishDetail.workflow.completeStatus'),
      reason: t('ui.pages.publishDetail.workflow.validationReason'), fallback: t('ui.pages.publishDetail.workflow.validationFallback'),
      saveState: publishedSaveState, nextAction: t('ui.pages.publishDetail.workflow.validationNext'),
    },
    {
      key: 'preview', label: t('ui.pages.publishDetail.workflow.previewLabel'), status: 'complete', statusLabel: t('ui.pages.publishDetail.workflow.completeStatus'),
      reason: t('ui.pages.publishDetail.workflow.noReason'), fallback: t('ui.pages.publishDetail.workflow.previewFallback'),
      saveState: publishedSaveState, nextAction: t('ui.pages.publishDetail.workflow.previewNext'),
    },
    {
      key: 'repair', label: t('ui.pages.publishDetail.workflow.repairLabel'),
      status: publish.notStartedCount > 0 || publish.inProgressCount > 0 ? 'warning' : 'complete',
      statusLabel: publish.notStartedCount > 0 || publish.inProgressCount > 0 ? t('ui.pages.publishDetail.workflow.monitoringStatus') : t('ui.pages.publishDetail.workflow.completeStatus'),
      reason: publish.notStartedCount > 0 || publish.inProgressCount > 0
        ? t('ui.pages.publishDetail.workflow.repairReason', { notStarted: publish.notStartedCount, inProgress: publish.inProgressCount })
        : t('ui.pages.publishDetail.workflow.noReason'),
      fallback: t('ui.pages.publishDetail.workflow.repairFallback'), saveState: publishedSaveState,
      nextAction: t('ui.pages.publishDetail.workflow.repairNext'),
    },
    {
      key: 'publish', label: t('ui.pages.publishDetail.workflow.publishLabel'), status: 'complete', statusLabel: t('ui.pages.publishDetail.workflow.completeStatus'),
      reason: t('ui.pages.publishDetail.workflow.publishReason', { time: formatDateTime(publish.publishedAt) }),
      fallback: t('ui.pages.publishDetail.workflow.publishFallback'), saveState: publishedSaveState,
      nextAction: t('ui.pages.publishDetail.workflow.publishNext'),
    },
    {
      key: 'complete', label: t('ui.pages.publishDetail.workflow.completeLabel'),
      status: allAssignedStudentsSubmitted ? 'complete' : 'current',
      statusLabel: allAssignedStudentsSubmitted ? t('ui.pages.publishDetail.workflow.completeStatus') : t('ui.pages.publishDetail.workflow.monitoringStatus'),
      reason: allAssignedStudentsSubmitted
        ? t('ui.pages.publishDetail.workflow.noReason')
        : t('ui.pages.publishDetail.workflow.completeReason', { submitted: publish.submittedCount, assigned: publish.assignedCount }),
      fallback: t('ui.pages.publishDetail.workflow.completeFallback'), saveState: t('ui.pages.publishDetail.workflow.readOnlySave'),
      nextAction: allAssignedStudentsSubmitted ? t('ui.pages.publishDetail.workflow.completeNextDone') : t('ui.pages.publishDetail.workflow.completeNext'),
    },
  ] : [];

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow={t('ui.sections.assessments')}
        title={publish?.paperTitle || t('ui.pages.publishDetail.fallbackTitle')}
        subtitle={
          publish
            ? `${publish.className} · ${t('ui.meta.publishedAt', { time: formatDateTime(publish.publishedAt) })} · ${t('ui.meta.dueAt', { time: formatDateTime(publish.dueAt) })}`
            : t('ui.pages.publishDetail.loadingSubtitle')
        }
        actions={
          <div className="flex flex-wrap gap-3">
            <Link to="/teacher/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
              {t('ui.actions.backToAssessments')}
            </Link>
            {publish && (
              <Link
                to={`/teacher/assessments/${publish.paperId}`}
                className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
              >
                {t('ui.actions.backToPaper')}
              </Link>
            )}
          </div>
        }
      />

      {publishQuery.error && (
        <div role="alert" className={`rounded-[2rem] border p-6 ${accessDenied ? 'border-amber-500/25 bg-amber-500/[0.08] text-amber-800 dark:text-amber-200' : 'border-rose-500/20 bg-rose-500/5 text-rose-500'}`}>
          <div className="mb-1 font-black">{accessDenied ? t('ui.pages.publishDetail.workflow.permissionTitle') : t('ui.pages.publishDetail.workflow.loadErrorTitle')}</div>
          {getApiErrorMessage(publishQuery.error)}
          <div className="mt-2 text-xs opacity-75">{t('ui.pages.publishDetail.workflow.loadErrorSafety')}</div>
        </div>
      )}

      {publishQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          {t('ui.labels.loadingPublishDetail')}
        </div>
      )}

      {publish && (
        <>
          <WorkflowStepper
            title={t('ui.pages.publishDetail.workflow.title')}
            description={t('ui.pages.publishDetail.workflow.description')}
            stages={workflowStages}
          />

          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-5">
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.assignedStudents')}</SectionEyebrow>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{publish.assignedCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.assignedStudents')}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.notStarted')}</SectionEyebrow>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{publish.notStartedCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.notStarted')}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.inProgress')}</SectionEyebrow>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{publish.inProgressCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.inProgress')}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.submitted')}</SectionEyebrow>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{publish.submittedCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.submitted')}</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <SectionEyebrow>{t('ui.meta.averageScore')}</SectionEyebrow>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">
                {publish.averageScore === null || publish.averageScore === undefined ? '--' : publish.averageScore.toFixed(1)}
              </div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.meta.averageScore')}</div>
            </div>
          </div>

          <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <SectionEyebrow>{t('ui.sections.publishWindow')}</SectionEyebrow>
                <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{publish.className}</div>
                <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                  <StatusBadge label={t('ui.meta.questionCount', { count: publish.questionCount })} />
                  <StatusBadge label={t('ui.meta.totalScore', { count: publish.totalScore })} />
                  <StatusBadge label={t('ui.meta.durationMinutes', { count: publish.durationMinutes })} />
                  <StatusBadge label={String(publish.status)} />
                </div>
              </div>
              <div className="grid gap-2 text-right text-sm text-slate-500 dark:text-white/45">
                <div>{t('ui.meta.startsAt', { time: formatDateTime(publish.startsAt) })}</div>
                <div>{t('ui.meta.dueAt', { time: formatDateTime(publish.dueAt) })}</div>
                <div>{t('ui.meta.publishAt', { time: formatDateTime(publish.publishedAt) })}</div>
              </div>
            </div>

            {publish.paperDescription && (
              <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/70 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                {publish.paperDescription}
              </div>
            )}

            {publish.instructionsText && (
              <div className="rounded-[1.6rem] border border-dashed border-slate-200/80 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:text-white/60">
                {publish.instructionsText}
              </div>
            )}
          </section>

          <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
            <div>
              <SectionEyebrow>{t('ui.sections.roster')}</SectionEyebrow>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{t('ui.sections.roster')}</div>
            </div>

            {!publish.roster.length ? (
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-4 py-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                {t('ui.labels.noRoster')}
              </div>
            ) : (
              <DataTable
                rows={publish.roster}
                getRowId={(item) => item.studentUserId}
                caption={t('ui.sections.roster')}
                density="compact"
                columns={[
                  {
                    id: 'student',
                    header: t('ui.labels.student'),
                    accessor: 'studentName',
                    sortable: true,
                    className: 'min-w-40 font-semibold text-slate-900 dark:text-white',
                  },
                  {
                    id: 'status',
                    header: t('ui.labels.status'),
                    sortable: true,
                    sortValue: (item) => item.attemptStatus,
                    render: (item) => <StatusBadge label={assessmentAttemptStatusLabel(item.attemptStatus)} tone={assessmentAttemptStatusTone(item.attemptStatus)} />,
                  },
                  {
                    id: 'progress',
                    header: t('ui.labels.progress'),
                    sortable: true,
                    sortValue: (item) => item.answeredCount || 0,
                    render: (item) => t('ui.meta.progress', { current: item.answeredCount || 0, total: item.questionCount || publish.questionCount }),
                  },
                  {
                    id: 'score',
                    header: t('ui.meta.averageScore'),
                    sortable: true,
                    sortValue: (item) => item.totalScore,
                    render: (item) => item.totalScore === null || item.totalScore === undefined ? '—' : item.totalScore,
                  },
                  {
                    id: 'updated',
                    header: t('ui.labels.lastSaved'),
                    sortValue: (item) => item.lastSavedAt,
                    render: (item) => formatDateTime(item.lastSavedAt),
                  },
                  {
                    id: 'action',
                    header: t('ui.labels.action'),
                    render: (item) => item.attemptStatus === 'SUBMITTED' && item.attemptId ? (
                      <Link to={`/teacher/assessments/attempts/${item.attemptId}/result`} className="btn-secondary px-3 py-2 text-xs">{t('ui.actions.viewResult')}</Link>
                    ) : item.attemptId ? <StatusBadge label={t('ui.labels.stillAnswering')} tone="warning" /> : <StatusBadge label={t('ui.labels.notEnteredAssessment')} />,
                  },
                ]}
              />
            )}
          </section>
        </>
      )}
    </div>
  );
};

export default TeacherAssessmentPublishDetailPage;
