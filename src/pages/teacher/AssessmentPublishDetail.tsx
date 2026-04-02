import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

function rosterStatusStyle(status: string) {
  if (status === 'SUBMITTED') {
    return 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300';
  }
  if (status === 'IN_PROGRESS') {
    return 'border-amber-500/20 bg-amber-500/10 text-amber-700 dark:text-amber-300';
  }
  return 'border-slate-200/70 bg-white/70 text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45';
}

const TeacherAssessmentPublishDetailPage: React.FC = () => {
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
        title={publish?.paperTitle || '发布详情'}
        subtitle={
          publish
            ? `${publish.className} · 发布时间 ${formatDateTime(publish.publishedAt)} · 截止 ${formatDateTime(publish.dueAt)}`
            : '正在加载本次发布的学生名册与完成情况'
        }
        actions={
          <div className="flex flex-wrap gap-3">
            <Link to="/teacher/assessments" className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
              返回测评列表
            </Link>
            {publish && (
              <Link
                to={`/teacher/assessments/${publish.paperId}`}
                className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
              >
                返回试卷
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
          正在加载发布详情...
        </div>
      )}

      {publish && (
        <>
          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-5">
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">assigned</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{publish.assignedCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">已分配学生</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">not started</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{publish.notStartedCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">尚未开始</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">in progress</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{publish.inProgressCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">正在作答</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">submitted</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">{publish.submittedCount}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">已交卷</div>
            </div>
            <div className="rounded-[2rem] liquid-glass-panel p-6">
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">average</div>
              <div className="mt-3 text-4xl font-black text-slate-900 dark:text-white">
                {publish.averageScore === null || publish.averageScore === undefined ? '--' : publish.averageScore.toFixed(1)}
              </div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">平均分</div>
            </div>
          </div>

          <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">publish window</div>
                <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{publish.className}</div>
                <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{publish.questionCount} 题</span>
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{publish.totalScore} 分</span>
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{publish.durationMinutes} 分钟</span>
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{publish.status}</span>
                </div>
              </div>
              <div className="grid gap-2 text-right text-sm text-slate-500 dark:text-white/45">
                <div>开始时间：{formatDateTime(publish.startsAt)}</div>
                <div>截止时间：{formatDateTime(publish.dueAt)}</div>
                <div>发布时间：{formatDateTime(publish.publishedAt)}</div>
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
              <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">roster</div>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">学生完成名册</div>
            </div>

            {!publish.roster.length ? (
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-4 py-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                本次发布还没有学生名册快照。
              </div>
            ) : (
              <div className="space-y-4">
                {publish.roster.map((item) => (
                  <div key={item.studentUserId} className="rounded-[1.8rem] border border-slate-200/70 bg-white/70 p-5 dark:border-white/10 dark:bg-white/5">
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <div className="text-xl font-black text-slate-900 dark:text-white">{item.studentName}</div>
                        <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                          <span className={`rounded-full border px-3 py-1 ${rosterStatusStyle(item.attemptStatus)}`}>{item.attemptStatus}</span>
                          <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                            进度 {item.answeredCount || 0} / {item.questionCount || publish.questionCount}
                          </span>
                          {item.totalScore !== null && item.totalScore !== undefined && (
                            <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">得分 {item.totalScore}</span>
                          )}
                        </div>
                        <div className="mt-3 grid gap-2 text-sm text-slate-500 dark:text-white/45 md:grid-cols-2">
                          <div>开始时间：{formatDateTime(item.startedAt)}</div>
                          <div>最后保存：{formatDateTime(item.lastSavedAt)}</div>
                          <div>作答时限：{formatDateTime(item.expiresAt)}</div>
                          <div>交卷时间：{formatDateTime(item.submittedAt)}</div>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-3">
                        {item.attemptStatus === 'SUBMITTED' && item.attemptId ? (
                          <Link
                            to={`/teacher/assessments/attempts/${item.attemptId}/result`}
                            className="btn-liquid px-5 py-3 text-sm text-white"
                          >
                            查看答卷
                          </Link>
                        ) : item.attemptId ? (
                          <div className="rounded-full border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-700 dark:text-amber-300">
                            学生仍在作答
                          </div>
                        ) : (
                          <div className="rounded-full border border-slate-200 px-4 py-3 text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
                            尚未进入测评
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
