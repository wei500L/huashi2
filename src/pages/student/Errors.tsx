import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { PageHeader, SectionEyebrow } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime, lexicalPairTypeLabel, trainingModeLabel } from '@/lib/format';
import { trainingService } from '@/lib/services';
import { buildTrainingHref } from '@/lib/training-launch';

const ErrorsPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const wrongBookQuery = useQuery({
    queryKey: ['wrong-book'],
    queryFn: ({ signal }) => trainingService.getWrongBook({ signal }),
  });
  const reviewScheduleQuery = useQuery({
    queryKey: ['review-schedule', true],
    queryFn: ({ signal }) => trainingService.getReviewSchedule(true, { signal }),
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow={t('shell.nav.errors')}
        title={t('ui.pages.errors.title')}
        subtitle={t('ui.pages.errors.subtitle')}
        actions={
          <div className="flex flex-wrap gap-3">
            {!!reviewScheduleQuery.data?.length && (
              <button
                type="button"
                onClick={() =>
                  navigate(
                    buildTrainingHref({
                      mode: reviewScheduleQuery.data[0].reviewMode,
                      source: 'errors-review-top',
                      lexicalPairId: reviewScheduleQuery.data[0].lexicalPairId,
                      wrongBookId: reviewScheduleQuery.data[0].wrongBookId,
                      reviewScheduleId: reviewScheduleQuery.data[0].reviewScheduleId,
                    })
                  )
                }
                className="btn-liquid px-5 py-3 text-white"
              >
                {t('ui.actions.startReviewNow')}
              </button>
            )}
            <button
              type="button"
              onClick={() => navigate('/training')}
              className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
            >
              {t('common.actions.backToTrainingHome')}
            </button>
          </div>
        }
      />

      <div className="grid gap-8 xl:grid-cols-2">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <SectionEyebrow className="mb-6">{t('ui.sections.wrongBook')}</SectionEyebrow>
          {wrongBookQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.loadingWrongBook')}</div>
          ) : wrongBookQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(wrongBookQuery.error)}
            </div>
          ) : !wrongBookQuery.data?.length ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noWrongBook')}</div>
          ) : (
            <div className="space-y-4">
              {wrongBookQuery.data.map((item) => (
                <div
                  key={item.wrongBookId}
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                >
                  <div className="font-black text-slate-900 dark:text-white">
                    {item.englishWord} / {item.frenchWord}
                  </div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {lexicalPairTypeLabel(item.lexicalPairType)} · {trainingModeLabel(item.recommendedMode)} · {t('ui.meta.recentWrongCount', {
                      type: item.lastErrorType,
                      count: item.wrongCount,
                    })}
                  </div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {t('ui.meta.nextReviewAt', { time: formatDateTime(item.nextReviewAt) })}
                  </div>
                  <button
                    type="button"
                    onClick={() =>
                      navigate(
                        buildTrainingHref({
                          mode: item.recommendedMode,
                          source: 'errors-wrong-book-item',
                          lexicalPairId: item.lexicalPairId,
                          wrongBookId: item.wrongBookId,
                        })
                      )
                    }
                    className="mt-4 rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-primary dark:border-white/10"
                  >
                    {t('ui.actions.remediateNow')}
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="mb-6 flex items-center justify-between gap-3">
            <SectionEyebrow>{t('ui.sections.reviewSchedule')}</SectionEyebrow>
            {!!reviewScheduleQuery.data?.length && (
              <button
                type="button"
                onClick={() =>
                  navigate(
                    buildTrainingHref({
                      mode: reviewScheduleQuery.data[0].reviewMode,
                      source: 'errors-review-schedule',
                      lexicalPairId: reviewScheduleQuery.data[0].lexicalPairId,
                      wrongBookId: reviewScheduleQuery.data[0].wrongBookId,
                      reviewScheduleId: reviewScheduleQuery.data[0].reviewScheduleId,
                    })
                  )
                }
                className="text-sm font-bold text-primary"
              >
                {t('ui.actions.startReviewNow')}
              </button>
            )}
          </div>
          {reviewScheduleQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.loadingReviewSchedule')}</div>
          ) : reviewScheduleQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(reviewScheduleQuery.error)}
            </div>
          ) : !reviewScheduleQuery.data?.length ? (
            <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.labels.noReviewItems')}</div>
          ) : (
            <div className="space-y-4">
              {reviewScheduleQuery.data.map((item) => (
                <div
                  key={item.reviewScheduleId}
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="font-black text-slate-900 dark:text-white">
                        {item.englishWord} / {item.frenchWord}
                      </div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {trainingModeLabel(item.reviewMode)} · {t('ui.meta.reviewStage', {
                          stage: item.scheduleStage,
                          days: item.intervalDays,
                        })}
                      </div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {t('ui.meta.dueWithReason', { time: formatDateTime(item.dueAt), reason: item.triggerReason })}
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() =>
                        navigate(
                          buildTrainingHref({
                            mode: item.reviewMode,
                            source: 'errors-review-item',
                            lexicalPairId: item.lexicalPairId,
                            wrongBookId: item.wrongBookId,
                            reviewScheduleId: item.reviewScheduleId,
                          })
                        )
                      }
                      className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-primary dark:border-white/10"
                    >
                      {t('ui.actions.start')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default ErrorsPage;
