import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Users } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { teacherAnalyticsService } from '@/lib/services';

const TeacherClassesPage: React.FC = () => {
  const navigate = useNavigate();
  const classesQuery = useQuery({
    queryKey: ['teacher-classes'],
    queryFn: ({ signal }) => teacherAnalyticsService.listClasses({ signal }),
  });

  return (
    <div className="space-y-8">
      <PageHeader title="班级与学生" subtitle="这里承接教师工作台里的班级动态，继续下钻到班级详情和学生分析，而不是只停留在列表查看。" />

      {classesQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{classesQuery.error.message}</div>
      )}

      {!classesQuery.isLoading && !classesQuery.data?.length && (
        <div className="rounded-[2rem] border border-slate-200 dark:border-white/10 p-8 text-slate-500 dark:text-white/45">
          当前账号还没有可访问班级。
        </div>
      )}

      <div className="grid lg:grid-cols-2 gap-6">
        {(classesQuery.data || []).map((item) => (
          <button
            key={item.classId}
            type="button"
            onClick={() => navigate(`/teacher/classes/${item.classId}`)}
            className="text-left rounded-[2.5rem] liquid-glass-panel p-8 edge-light hover:border-primary/40 transition-all"
          >
            <div className="inline-flex p-3 rounded-2xl bg-primary/10 text-primary mb-6">
              <Users size={18} />
            </div>
            <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{item.classCode}</div>
            <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{item.className}</div>
            <div className="mt-3 text-slate-500 dark:text-white/45">{item.gradeName}</div>
            <div className="mt-6 text-sm text-slate-500 dark:text-white/45">学生人数 {item.studentCount}</div>
          </button>
        ))}
      </div>
    </div>
  );
};

export default TeacherClassesPage;
