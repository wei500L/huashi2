import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
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
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {getApiErrorMessage(publishQuery.error)}
        </div>
      )}

      {publishQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          {t('ui.labels.loadingPublishDetail')}
        </div>
      )}

      {publish && (
        <>
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
              <div className="space-y-4">
                {publish.roster.map((item) => (
                  <div key={item.studentUserId} className="rounded-[1.8rem] border border-slate-200/70 bg-white/70 p-5 dark:border-white/10 dark:bg-white/5">
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <div className="text-xl font-black text-slate-900 dark:text-white">{item.studentName}</div>
                        <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                          <StatusBadge
                            label={assessmentAttemptStatusLabel(item.attemptStatus)}
                            tone={assessmentAttemptStatusTone(item.attemptStatus)}
                          />
                          <StatusBadge label={t('ui.meta.progress', { current: item.answeredCount || 0, total: item.questionCount || publish.questionCount })} />
                          {item.totalScore !== null && item.totalScore !== undefined && (
                            <StatusBadge label={`${t('ui.meta.averageScore')} ${item.totalScore}`} />
                          )}
                        </div>
                        <div className="mt-3 grid gap-2 text-sm text-slate-500 dark:text-white/45 md:grid-cols-2">
                          <div>{t('ui.meta.startsAt', { time: formatDateTime(item.startedAt) })}</div>
                          <div>{t('ui.meta.lastSavedAt', { time: formatDateTime(item.lastSavedAt) })}</div>
                          <div>{t('ui.meta.expiresAt', { time: formatDateTime(item.expiresAt) })}</div>
                          <div>{t('ui.meta.submittedAt', { time: formatDateTime(item.submittedAt) })}</div>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-3">
                        {item.attemptStatus === 'SUBMITTED' && item.attemptId ? (
                          <Link
                            to={`/teacher/assessments/attempts/${item.attemptId}/result`}
                            className="btn-liquid px-5 py-3 text-sm text-white"
                          >
                            {t('ui.actions.viewResult')}
                          </Link>
                        ) : item.attemptId ? (
                          <div className="rounded-full border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-700 dark:text-amber-300">
                            {t('ui.labels.stillAnswering')}
                          </div>
                        ) : (
                          <div className="rounded-full border border-slate-200 px-4 py-3 text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
                            {t('ui.labels.notEnteredAssessment')}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
};

export default TeacherAssessmentPublishDetailPage;
