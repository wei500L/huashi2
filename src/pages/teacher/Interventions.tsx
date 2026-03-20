import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2 } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { teacherAnalyticsService, teacherInterventionService } from '@/lib/services';
import { formatDateTime } from '@/lib/format';

const TeacherInterventionsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [status, setStatus] = React.useState('');
  const [classId, setClassId] = React.useState('');

  const classesQuery = useQuery({
    queryKey: ['teacher-classes'],
    queryFn: () => teacherAnalyticsService.listClasses(),
  });

  const interventionsQuery = useQuery({
    queryKey: ['teacher-interventions', classId, status],
    queryFn: () =>
      teacherInterventionService.list({
        classId: classId ? Number(classId) : undefined,
        status: status || undefined,
        pageNo: 1,
        pageSize: 50,
      }),
  });

  const completeMutation = useMutation({
    mutationFn: (interventionId: number) => teacherInterventionService.complete(interventionId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['teacher-interventions'] });
    },
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader title="干预工作台" subtitle="真实显示 AI 建议入库后的 intervention_record，并支持标记完成。" />

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <div className="grid md:grid-cols-2 gap-4">
          <input
            value={status}
            onChange={(event) => setStatus(event.target.value.toUpperCase())}
            className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
            placeholder="状态筛选，例如 PENDING / COMPLETED"
          />
          <select
            value={classId}
            onChange={(event) => setClassId(event.target.value)}
            className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
          >
            <option value="">全部班级</option>
            {(classesQuery.data || []).map((item) => (
              <option key={item.classId} value={item.classId}>
                {item.className}
              </option>
            ))}
          </select>
        </div>
      </section>

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <div className="space-y-4">
          {(interventionsQuery.data?.records || []).map((item) => (
            <div key={item.id} className="rounded-[1.8rem] border border-slate-200/70 dark:border-white/10 p-5 bg-white/60 dark:bg-white/5">
              <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-6">
                <div className="space-y-2">
                  <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                    {item.className} · {item.priority} · {item.status}
                  </div>
                  <div className="text-xl font-black text-slate-900 dark:text-white">{item.studentName}</div>
                  <div className="text-sm font-bold text-slate-800 dark:text-white/85">{item.patternDetected}</div>
                  <div className="text-sm text-slate-500 dark:text-white/45 leading-6">{item.suggestedAction}</div>
                  <div className="text-sm text-slate-500 dark:text-white/45">
                    计划时间 {formatDateTime(item.plannedAt)} · 完成时间 {formatDateTime(item.completedAt)}
                  </div>
                </div>
                {item.status !== 'COMPLETED' && (
                  <button
                    type="button"
                    onClick={() => completeMutation.mutate(item.id)}
                    className="btn-liquid px-5 py-3 text-white flex items-center gap-2"
                  >
                    <CheckCircle2 size={14} /> 标记完成
                  </button>
                )}
              </div>
            </div>
          ))}
          {!interventionsQuery.isLoading && !interventionsQuery.data?.records.length && (
            <div className="text-sm text-slate-500 dark:text-white/45">当前没有干预记录。</div>
          )}
        </div>
      </section>
    </div>
  );
};

export default TeacherInterventionsPage;
