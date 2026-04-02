import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime, lexicalPairTypeLabel } from '@/lib/format';
import { trainingService } from '@/lib/services';
import { buildTrainingHref } from '@/lib/training-launch';

const ErrorsPage: React.FC = () => {
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
        title="错题与复习"
        subtitle="集中处理易错词对和到期复习任务，优先把高风险内容转回训练链路。"
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
                立即开始复习
              </button>
            )}
            <button
              type="button"
              onClick={() => navigate('/training')}
              className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold dark:border-white/10"
            >
              返回训练计划
            </button>
          </div>
        }
      />

      <div className="grid gap-8 xl:grid-cols-2">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="mb-6 text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">错题本</div>
          {wrongBookQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">正在加载错题...</div>
          ) : wrongBookQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(wrongBookQuery.error)}
            </div>
          ) : !wrongBookQuery.data?.length ? (
            <div className="text-sm text-slate-500 dark:text-white/45">暂无错题记录。</div>
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
                    {lexicalPairTypeLabel(item.lexicalPairType)} · 推荐 {item.recommendedMode} · 最近错误 {item.lastErrorType} · 累计 {item.wrongCount} 次
                  </div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    下次复习：{formatDateTime(item.nextReviewAt)}
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
                    开始纠错
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="mb-6 flex items-center justify-between gap-3">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">待复习计划</div>
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
                按计划开始
              </button>
            )}
          </div>
          {reviewScheduleQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">正在加载复习计划...</div>
          ) : reviewScheduleQuery.error ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              {getApiErrorMessage(reviewScheduleQuery.error)}
            </div>
          ) : !reviewScheduleQuery.data?.length ? (
            <div className="text-sm text-slate-500 dark:text-white/45">暂无待复习项目。</div>
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
                        {item.reviewMode} · 第 {item.scheduleStage} 阶段 · 间隔 {item.intervalDays} 天
                      </div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        到期：{formatDateTime(item.dueAt)} · {item.triggerReason}
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
                      开始
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
