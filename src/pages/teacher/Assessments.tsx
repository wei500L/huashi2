import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { FilePenLine, Plus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

const TeacherAssessmentsPage: React.FC = () => {
  const navigate = useNavigate();
  const papersQuery = useQuery({
    queryKey: ['teacher-assessment-papers'],
    queryFn: ({ signal }) => assessmentService.listTeacherPapers({ signal }),
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="通用测评"
        subtitle="这里是并行于词汇诊断/训练之外的通用作业测评模块。教师可创建整卷、发布到班级，并查看每次发布的完成情况。"
        actions={
          <button type="button" onClick={() => navigate('/teacher/assessments/new')} className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white">
            <Plus size={16} />
            新建试卷
          </button>
        }
      />

      {papersQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {papersQuery.error.message}
        </div>
      )}

      {papersQuery.isLoading && (
        <div className="rounded-[2.2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          正在加载试卷...
        </div>
      )}

      {!papersQuery.isLoading && !papersQuery.data?.length && (
        <div className="rounded-[2.2rem] border border-dashed border-slate-300 bg-white/55 p-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
          当前还没有通用测评试卷。先创建一份整卷，再发布到班级。
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-2">
        {(papersQuery.data || []).map((paper) => (
          <button
            key={paper.paperId}
            type="button"
            onClick={() => navigate(`/teacher/assessments/${paper.paperId}`)}
            className="text-left rounded-[2.4rem] liquid-glass-panel p-7 edge-light transition-all hover:border-primary/40"
          >
            <div className="inline-flex rounded-2xl bg-primary/10 p-3 text-primary">
              <FilePenLine size={18} />
            </div>
            <div className="mt-5 flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="text-[10px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{paper.paperCode}</div>
                <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{paper.title}</div>
                <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{paper.description || '无描述'}</div>
              </div>
              <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                {paper.status}
              </div>
            </div>

            <div className="mt-5 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
              <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{paper.questionCount} 题</span>
              <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{paper.totalScore} 分</span>
              <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{paper.durationMinutes} 分钟</span>
            </div>

            <div className="mt-5 text-xs text-slate-400 dark:text-white/30">
              最近更新 {formatDateTime(paper.updatedAt)} · 最近发布 {formatDateTime(paper.latestPublishAt)}
            </div>
          </button>
        ))}
      </div>
    </div>
  );
};

export default TeacherAssessmentsPage;
