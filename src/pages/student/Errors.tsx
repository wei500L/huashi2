import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@/components/common';
import { trainingService } from '@/lib/services';
import { formatDateTime, lexicalPairTypeLabel } from '@/lib/format';

const ErrorsPage: React.FC = () => {
  const wrongBookQuery = useQuery({
    queryKey: ['wrong-book'],
    queryFn: () => trainingService.getWrongBook(),
  });
  const reviewScheduleQuery = useQuery({
    queryKey: ['review-schedule', true],
    queryFn: () => trainingService.getReviewSchedule(true),
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader title="错题与复习" subtitle="真实 wrong-book 与 review-schedule 数据。" />

      <div className="grid xl:grid-cols-2 gap-8">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">wrong book</div>
          <div className="space-y-4">
            {(wrongBookQuery.data || []).map((item) => (
              <div key={item.wrongBookId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                  {lexicalPairTypeLabel(item.lexicalPairType)} · 最近错误 {item.lastErrorType} · 累计 {item.wrongCount} 次
                </div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                  下次复习：{formatDateTime(item.nextReviewAt)}
                </div>
              </div>
            ))}
            {!wrongBookQuery.isLoading && !wrongBookQuery.data?.length && (
              <div className="text-sm text-slate-500 dark:text-white/45">暂无错题记录。</div>
            )}
          </div>
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">review schedule</div>
          <div className="space-y-4">
            {(reviewScheduleQuery.data || []).map((item) => (
              <div key={item.reviewScheduleId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                  {item.reviewMode} · 第 {item.scheduleStage} 阶段 · 间隔 {item.intervalDays} 天
                </div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                  到期：{formatDateTime(item.dueAt)} · {item.triggerReason}
                </div>
              </div>
            ))}
            {!reviewScheduleQuery.isLoading && !reviewScheduleQuery.data?.length && (
              <div className="text-sm text-slate-500 dark:text-white/45">暂无待复习项目。</div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
};

export default ErrorsPage;
