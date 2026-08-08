import React from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronRight, LayoutGrid, List, PencilLine, Plus, Search, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { PageHeader } from '@/components/common';
import type { TeachingClassSummaryVO } from '@/lib/contracts';
import { teacherClassService } from '@/lib/services';
import { cn } from '@/lib/utils';

type ClassView = 'cards' | 'list';
type ClassSort = 'name' | 'students-desc' | 'students-asc';

const TeacherClassesPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const source = searchParams.get('source');
  const [keyword, setKeyword] = React.useState('');
  const [gradeFilter, setGradeFilter] = React.useState('all');
  const [sort, setSort] = React.useState<ClassSort>('name');
  const [view, setView] = React.useState<ClassView>('list');

  const classesQuery = useQuery({
    queryKey: ['teacher-classes-management'],
    queryFn: ({ signal }) => teacherClassService.listClasses({ signal }),
  });

  const buildDetailPath = React.useCallback(
    (classId: number) => (source ? `/teacher/classes/${classId}?source=${encodeURIComponent(source)}` : `/teacher/classes/${classId}`),
    [source]
  );
  const buildEditPath = React.useCallback(
    (classId: number) => source ? `/teacher/classes/${classId}/edit?source=${encodeURIComponent(source)}` : `/teacher/classes/${classId}/edit`,
    [source]
  );

  const gradeOptions = React.useMemo(
    () => Array.from(new Set((classesQuery.data || []).map((item) => item.gradeName).filter(Boolean))).sort((a, b) => a.localeCompare(b)),
    [classesQuery.data]
  );

  const visibleClasses = React.useMemo(() => {
    const normalizedKeyword = keyword.trim().toLocaleLowerCase();
    const rows = (classesQuery.data || []).filter((item) => {
      const matchesGrade = gradeFilter === 'all' || item.gradeName === gradeFilter;
      const matchesKeyword = !normalizedKeyword || [item.className, item.classCode, item.gradeName]
        .some((value) => value?.toLocaleLowerCase().includes(normalizedKeyword));
      return matchesGrade && matchesKeyword;
    });
    return [...rows].sort((left, right) => {
      if (sort === 'students-desc') return right.studentCount - left.studentCount;
      if (sort === 'students-asc') return left.studentCount - right.studentCount;
      return left.className.localeCompare(right.className, undefined, { numeric: true, sensitivity: 'base' });
    });
  }, [classesQuery.data, gradeFilter, keyword, sort]);

  const hasFilters = Boolean(keyword.trim()) || gradeFilter !== 'all';
  const newClassPath = source ? `/teacher/classes/new?source=${encodeURIComponent(source)}` : '/teacher/classes/new';

  const renderRowActions = (item: TeachingClassSummaryVO) => (
    <div className="flex items-center justify-end gap-1.5">
      <button
        type="button"
        onClick={() => navigate(buildEditPath(item.classId))}
        className="rounded-lg p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-primary dark:text-white/45 dark:hover:bg-white/[0.06]"
        aria-label={t('teacherWorkspace.classesPage.editClass', { name: item.className })}
        title={t('teacherWorkspace.classesPage.edit')}
      >
        <PencilLine size={15} />
      </button>
      <button
        type="button"
        onClick={() => navigate(buildDetailPath(item.classId))}
        className="inline-flex items-center gap-1 rounded-lg bg-slate-900 px-3 py-2 text-xs font-bold text-white transition hover:bg-slate-700 dark:bg-white dark:text-slate-900"
      >
        {t('teacherWorkspace.classesPage.open')}
        <ChevronRight size={13} />
      </button>
    </div>
  );

  return (
    <div className="page-stack pb-16">
      <PageHeader
        compact
        title={t('teacherWorkspace.classesPage.title')}
        subtitle={t('teacherWorkspace.classesPage.subtitle')}
        actions={
          <button type="button" onClick={() => navigate(newClassPath)} className="btn-liquid inline-flex items-center gap-2 px-4 py-2.5 text-sm text-white">
            <Plus size={16} />
            {t('teacherWorkspace.classesPage.create')}
          </button>
        }
      />

      {source ? (
        <div className="min-w-0 rounded-xl border border-slate-200/80 bg-white/70 px-4 py-3 text-xs leading-5 text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/65">
          {t('teacherWorkspace.classesPage.workspaceContext')}
        </div>
      ) : null}

      {classesQuery.error ? (
        <div className="min-w-0 rounded-xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{classesQuery.error.message}</div>
      ) : null}

      <section className="page-panel !p-0 overflow-hidden">
        <div className="page-toolbar border-b border-slate-200/70 p-4 dark:border-white/10">
          <label className="flex min-w-0 flex-1 items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2.5 dark:border-white/10 dark:bg-white/[0.04]">
            <Search size={15} className="shrink-0 text-slate-400" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder={t('teacherWorkspace.classesPage.searchPlaceholder')}
              className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-slate-400"
            />
          </label>
          <div className="grid min-w-0 w-full grid-cols-1 gap-2 sm:grid-cols-2 lg:w-auto">
            <select value={gradeFilter} onChange={(event) => setGradeFilter(event.target.value)} className="filter-field min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-xs font-semibold outline-none dark:border-white/10 dark:bg-slate-900">
              <option value="all">{t('teacherWorkspace.classesPage.allGrades')}</option>
              {gradeOptions.map((grade) => <option key={grade} value={grade}>{grade}</option>)}
            </select>
            <select value={sort} onChange={(event) => setSort(event.target.value as ClassSort)} className="filter-field min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-xs font-semibold outline-none dark:border-white/10 dark:bg-slate-900">
              <option value="name">{t('teacherWorkspace.classesPage.sortName')}</option>
              <option value="students-desc">{t('teacherWorkspace.classesPage.sortStudentsDesc')}</option>
              <option value="students-asc">{t('teacherWorkspace.classesPage.sortStudentsAsc')}</option>
            </select>
          </div>
          <div className="inline-flex max-w-full self-start overflow-x-auto rounded-xl bg-slate-100 p-1 dark:bg-white/[0.06]" role="group" aria-label={t('teacherWorkspace.classesPage.viewLabel')}>
            {([
              { id: 'list' as const, label: t('teacherWorkspace.classesPage.listView'), icon: List },
              { id: 'cards' as const, label: t('teacherWorkspace.classesPage.cardView'), icon: LayoutGrid },
            ]).map((item) => {
              const Icon = item.icon;
              return (
                <button key={item.id} type="button" onClick={() => setView(item.id)} aria-pressed={view === item.id} className={cn('inline-flex shrink-0 items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-bold transition-colors', view === item.id ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-800 dark:text-white' : 'text-slate-500 dark:text-white/45')}>
                  <Icon size={14} />{item.label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="flex min-w-0 flex-wrap items-center justify-between gap-3 px-4 py-3 text-xs text-slate-500 dark:text-white/45">
          <span className="min-w-0">{t('teacherWorkspace.classesPage.resultCount', { visible: visibleClasses.length, total: classesQuery.data?.length || 0 })}</span>
          {hasFilters ? <button type="button" onClick={() => { setKeyword(''); setGradeFilter('all'); }} className="shrink-0 font-bold text-primary">{t('teacherWorkspace.classesPage.clearFilters')}</button> : null}
        </div>

        {classesQuery.isLoading ? <div className="border-t border-slate-200/70 px-4 py-8 text-sm text-slate-500 dark:border-white/10 dark:text-white/45">{t('teacherWorkspace.classesPage.loading')}</div> : null}

        {!classesQuery.isLoading && !classesQuery.data?.length ? (
          <div className="border-t border-slate-200/70 px-4 py-10 text-center dark:border-white/10">
            <Users size={22} className="mx-auto text-slate-400" />
            <div className="mt-3 text-sm font-black text-slate-900 dark:text-white">{t('teacherWorkspace.classesPage.emptyTitle')}</div>
            <div className="mx-auto mt-1 max-w-lg text-xs leading-5 text-slate-500 dark:text-white/45">{t('teacherWorkspace.classesPage.emptyDescription')}</div>
            <button type="button" onClick={() => navigate(newClassPath)} className="mt-4 rounded-xl bg-primary px-4 py-2.5 text-xs font-black text-white">{t('teacherWorkspace.classesPage.create')}</button>
          </div>
        ) : null}

        {!classesQuery.isLoading && Boolean(classesQuery.data?.length) && !visibleClasses.length ? (
          <div className="border-t border-slate-200/70 px-4 py-10 text-center text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
            {t('teacherWorkspace.classesPage.noMatches')}
          </div>
        ) : null}

        {!classesQuery.isLoading && visibleClasses.length && view === 'list' ? (
          <div className="scroll-region overflow-x-auto border-t border-slate-200/70 dark:border-white/10" tabIndex={0} role="region" aria-label={t('teacherWorkspace.classesPage.className')} onKeyDown={(event) => { if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') { event.preventDefault(); event.currentTarget.scrollBy({ left: event.key === 'ArrowRight' ? 160 : -160, behavior: 'auto' }); } }}>
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="bg-slate-50/80 text-[11px] font-bold text-slate-500 dark:bg-white/[0.025] dark:text-white/40">
                <tr>
                  <th className="px-4 py-2.5">{t('teacherWorkspace.classesPage.className')}</th>
                  <th className="px-4 py-2.5">{t('teacherWorkspace.classesPage.code')}</th>
                  <th className="px-4 py-2.5">{t('teacherWorkspace.classesPage.grade')}</th>
                  <th className="px-4 py-2.5 text-right">{t('teacherWorkspace.classesPage.students')}</th>
                  <th className="px-4 py-2.5 text-right">{t('teacherWorkspace.classesPage.actions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200/70 dark:divide-white/10">
                {visibleClasses.map((item) => (
                  <tr key={item.classId} className="transition-colors hover:bg-slate-50/70 dark:hover:bg-white/[0.025]">
                    <td className="max-w-[360px] px-4 py-3"><button type="button" onClick={() => navigate(buildDetailPath(item.classId))} className="block max-w-full truncate text-left font-black text-slate-900 hover:text-primary dark:text-white" title={item.className}>{item.className}</button></td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-500 dark:text-white/45">{item.classCode || '--'}</td>
                    <td className="px-4 py-3 text-slate-600 dark:text-white/60">{item.gradeName || '--'}</td>
                    <td className="px-4 py-3 text-right font-black tabular-nums text-slate-900 dark:text-white">{item.studentCount}</td>
                    <td className="px-4 py-3">{renderRowActions(item)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

        {!classesQuery.isLoading && visibleClasses.length && view === 'cards' ? (
          <div className="grid min-w-0 grid-cols-1 gap-3 border-t border-slate-200/70 p-4 md:grid-cols-2 xl:grid-cols-3 dark:border-white/10">
            {visibleClasses.map((item) => (
              <article key={item.classId} className="min-w-0 rounded-xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.035]">
                <div className="flex min-w-0 items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="truncate text-base font-black text-slate-900 dark:text-white" title={item.className}>{item.className}</div>
                    <div className="mt-1 truncate text-xs text-slate-500 dark:text-white/45">{item.classCode || '--'} · {item.gradeName || '--'}</div>
                  </div>
                  <span className="shrink-0 rounded-lg bg-slate-100 px-2 py-1 text-xs font-black tabular-nums text-slate-700 dark:bg-white/[0.06] dark:text-white/70">{item.studentCount}</span>
                </div>
                <div className="mt-4 min-w-0">{renderRowActions(item)}</div>
              </article>
            ))}
          </div>
        ) : null}
      </section>
    </div>
  );
};

export default TeacherClassesPage;
