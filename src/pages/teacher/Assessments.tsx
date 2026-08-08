import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { FilePenLine, Microscope, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { assessmentPaperStatusLabel, assessmentPaperStatusTone, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

const TeacherAssessmentsPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const papersQuery = useQuery({
    queryKey: ['teacher-assessment-papers'],
    queryFn: ({ signal }) => assessmentService.listTeacherPapers({ purpose: 'CLASS_ASSESSMENT' }, { signal }),
  });

  return (
    <div className="page-stack pb-16">
      <PageHeader
        eyebrow={t('ui.sections.assessments')}
        title={t('ui.pages.teacherAssessments.title')}
        subtitle={t('ui.pages.teacherAssessments.subtitle')}
        actions={
          <div className="page-actions">
            <button type="button" onClick={() => navigate('/teacher/research')} className="inline-flex items-center gap-2 rounded-2xl border border-primary/20 bg-primary/10 px-5 py-3 font-bold text-primary">
              <Microscope size={16} />
              研究问卷
            </button>
            <button type="button" onClick={() => navigate('/teacher/assessments/new')} className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white">
              <Plus size={16} />
              {t('ui.actions.createPaper')}
            </button>
          </div>
        }
      />

      {papersQuery.error && (
        <div className="min-w-0 rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4 text-rose-500 sm:p-6">
          {papersQuery.error.message}
        </div>
      )}

      {papersQuery.isLoading && (
        <div className="min-w-0 rounded-2xl liquid-glass-panel p-4 text-sm text-slate-500 sm:p-6 md:p-8 dark:text-white/45">
          {t('ui.labels.loadingPapers')}
        </div>
      )}

      {!papersQuery.isLoading && !papersQuery.data?.length && (
        <div className="min-w-0 rounded-2xl border border-dashed border-slate-300 bg-white/55 p-4 text-sm text-slate-500 sm:p-6 md:p-8 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
          {t('ui.labels.noPapers')}
        </div>
      )}

      <div className="content-grid-2">
        {(papersQuery.data || []).map((paper) => (
          <button
            key={paper.paperId}
            type="button"
            onClick={() => navigate(`/teacher/assessments/${paper.paperId}`)}
            className="min-w-0 rounded-2xl liquid-glass-panel p-4 text-left edge-light transition-all hover:border-primary/40 sm:rounded-[2.4rem] sm:p-6 md:p-7"
          >
            <div className="inline-flex rounded-2xl bg-primary/10 p-3 text-primary">
              <FilePenLine size={18} />
            </div>
            <div className="mt-5 flex min-w-0 flex-wrap items-start justify-between gap-4">
              <div className="min-w-0">
                <SectionEyebrow>{paper.paperCode}</SectionEyebrow>
                <div className="mt-3 break-words text-2xl font-black text-slate-900 dark:text-white">{paper.title}</div>
                <div className="mt-3 break-words text-sm text-slate-500 dark:text-white/45">{paper.description || t('ui.labels.noDescription')}</div>
              </div>
              <StatusBadge label={assessmentPaperStatusLabel(paper.status)} tone={assessmentPaperStatusTone(paper.status)} />
            </div>

            <div className="mt-5 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
              <StatusBadge label={t('ui.meta.questionCount', { count: paper.questionCount })} />
              <StatusBadge label={t('ui.meta.totalScore', { count: paper.totalScore })} />
              <StatusBadge label={t('ui.meta.durationMinutes', { count: paper.durationMinutes })} />
            </div>

            <div className="mt-5 text-xs text-slate-400 dark:text-white/30">
              {t('ui.meta.lastUpdated', { time: formatDateTime(paper.updatedAt) })} · {t('ui.meta.lastPublished', { time: formatDateTime(paper.latestPublishAt) })}
            </div>
          </button>
        ))}
      </div>
    </div>
  );
};

export default TeacherAssessmentsPage;
