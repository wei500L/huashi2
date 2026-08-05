import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { PageHeader, SectionEyebrow } from '@/components/common';
import { getProductizedErrorState } from '@/lib/async-state';
import { FeedbackState } from '@/components/common/FeedbackState';
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
  const dueReviewItems = React.useMemo(
    () => (reviewScheduleQuery.data || []).filter((item) => Date.parse(item.dueAt) <= Date.now()),
    [reviewScheduleQuery.data]
  );
  const futureReviewItems = React.useMemo(
    () => (reviewScheduleQuery.data || []).filter((item) => Date.parse(item.dueAt) > Date.now()),
    [reviewScheduleQuery.data]
  );

  const renderQueryError = (error: unknown, resourceLabel: string, taskLabel: string, onRetry: () => void) => {
    const state = getProductizedErrorState(error, {
      resourceLabel,
      taskLabel,
      retryActionLabel: t('ui.sessionState.retryFetch'),
    });
    return (
      <FeedbackState
        kind={state.kind}
        compact
        title={state.title}
        description={state.description}
        impact={state.impact}
        nextStep={state.nextStep}
        primaryAction={{ label: t('ui.sessionState.retryFetch'), onClick: onRetry }}
      />
    );
  };

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow={t('shell.nav.errors')}
        title={t('ui.pages.errors.title')}
        subtitle={t('ui.pages.errors.subtitle')}
        actions={
          <div className="flex flex-wrap gap-3">
            {!!dueReviewItems.length && (
              <button
                type="button"
                onClick={() =>
                  navigate(
                    buildTrainingHref({
                      mode: dueReviewItems[0].reviewMode,
                      source: 'errors-review-top',
                      lexicalPairId: dueReviewItems[0].lexicalPairId,
                      wrongBookId: dueReviewItems[0].wrongBookId,
                      reviewScheduleId: dueReviewItems[0].reviewScheduleId,
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
            renderQueryError(wrongBookQuery.error, '错题本', '查看错题本', () => void wrongBookQuery.refetch())
          ) : !wrongBookQuery.data?.length ? (
            <FeedbackState
              kind="empty"
              compact
              title={t('ui.labels.noWrongBook')}
              description={t('ui.sessionState.emptyWrongBookDescription')}
              impact={t('ui.sessionState.emptyWrongBookImpact')}
              nextStep={t('ui.sessionState.emptyWrongBookNextStep')}
              primaryAction={{ label: t('common.actions.backToTrainingHome'), onClick: () => navigate('/training') }}
            />
          ) : (
            <div className="space-y-4">
              {wrongBookQuery.data.map((item) => (
                <div
                  key={item.wrongBookId}
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                >
                  <div className="min-w-0 font-black break-words text-slate-900 dark:text-white">
                    {item.englishWord} / {item.frenchWord}
                  </div>
                  <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">
                    {lexicalPairTypeLabel(item.lexicalPairType)} · {trainingModeLabel(item.recommendedMode)} · {t('ui.meta.recentWrongCount', {
                      type: item.lastErrorType,
                      count: item.wrongCount,
                    })}
                  </div>
                  <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">
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
            renderQueryError(reviewScheduleQuery.error, '复习计划', '查看复习计划', () => void reviewScheduleQuery.refetch())
          ) : !reviewScheduleQuery.data?.length ? (
            <FeedbackState
              kind="empty"
              compact
              title={t('ui.labels.noReviewItems')}
              description={t('ui.sessionState.emptyReviewDescription')}
              impact={t('ui.sessionState.emptyReviewImpact')}
              nextStep={t('ui.sessionState.emptyReviewNextStep')}
              primaryAction={{ label: t('common.actions.backToTrainingHome'), onClick: () => navigate('/training') }}
            />
          ) : (
            <div className="space-y-4">
              {dueReviewItems.map((item) => (
                <div
                  key={item.reviewScheduleId}
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="min-w-0 font-black break-words text-slate-900 dark:text-white">
                        {item.englishWord} / {item.frenchWord}
                      </div>
                      <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">
                        {trainingModeLabel(item.reviewMode)} · {t('ui.meta.reviewStage', {
                          stage: item.scheduleStage,
                          days: item.intervalDays,
                        })}
                      </div>
                      <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">
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
              {futureReviewItems.length ? (
                <div className="rounded-[1.6rem] border border-sky-500/15 bg-sky-500/5 p-4 text-sm text-sky-800 dark:text-sky-200">
                  <div className="font-bold">{t('ui.sessionState.futureReviewTitle')}</div>
                  <div className="mt-2 leading-6">{t('ui.sessionState.futureReviewDescription', { count: futureReviewItems.length })}</div>
                </div>
              ) : null}
              {!dueReviewItems.length && futureReviewItems.length ? (
                <div className="text-sm text-slate-500 dark:text-white/45">{t('ui.sessionState.noDueReview')}</div>
              ) : null}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default ErrorsPage;
