import React from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronRight, PencilLine, Plus, Users } from 'lucide-react';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { teacherClassService } from '@/lib/services';

const TeacherClassesPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const source = searchParams.get('source');

  const classesQuery = useQuery({
    queryKey: ['teacher-classes-management'],
    queryFn: ({ signal }) => teacherClassService.listClasses({ signal }),
  });

  const buildDetailPath = React.useCallback(
    (classId: number) => (source ? `/teacher/classes/${classId}?source=${encodeURIComponent(source)}` : `/teacher/classes/${classId}`),
    [source]
  );

  const buildEditPath = React.useCallback(
    (classId: number) =>
      source ? `/teacher/classes/${classId}/edit?source=${encodeURIComponent(source)}` : `/teacher/classes/${classId}/edit`,
    [source]
  );

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="班级与学生"
        subtitle="先把教学对象建起来，再去发布诊断、测评和学生分析。班级创建、编辑和学生分配都从这里进入。"
        actions={
          <button
            type="button"
            onClick={() => navigate(source ? `/teacher/classes/new?source=${encodeURIComponent(source)}` : '/teacher/classes/new')}
            className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white"
          >
            <Plus size={16} />
            新建班级
          </button>
        }
      />

      {source && (
        <div className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/70">
          当前从教师工作台进入。班级创建和详情页都会保留入口上下文，方便回到最近的教学任务。
        </div>
      )}

      {classesQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
          {classesQuery.error.message}
        </div>
      )}

      {classesQuery.isLoading && (
        <div className="rounded-[2rem] liquid-glass-panel p-8 text-sm text-slate-500 dark:text-white/45">
          正在加载班级列表…
        </div>
      )}

      {!classesQuery.isLoading && !classesQuery.data?.length && (
        <div className="rounded-[2rem] border border-dashed border-slate-300 bg-white/55 p-8 text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
          当前账号还没有班级。先创建一个班级，再去分配学生、发布诊断和课堂测评。
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-2">
        {(classesQuery.data || []).map((item) => (
          <div key={item.classId} className="rounded-[2.5rem] liquid-glass-panel p-8 edge-light">
            <div className="flex items-start justify-between gap-4">
              <div className="inline-flex rounded-2xl bg-primary/10 p-3 text-primary">
                <Users size={18} />
              </div>
              <StatusBadge label={`学生 ${item.studentCount}`} />
            </div>

            <SectionEyebrow className="mt-6">{item.classCode}</SectionEyebrow>
            <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{item.className}</div>
            <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{item.gradeName}</div>

            <div className="mt-8 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => navigate(buildDetailPath(item.classId))}
                className="inline-flex items-center gap-2 rounded-full bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-700 dark:bg-white dark:text-slate-900 dark:hover:bg-slate-200"
              >
                进入详情
                <ChevronRight size={14} />
              </button>
              <button
                type="button"
                onClick={() => navigate(buildEditPath(item.classId))}
                className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-primary/40 hover:text-primary dark:border-white/10 dark:text-white/80"
              >
                <PencilLine size={14} />
                编辑班级
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default TeacherClassesPage;
