import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BookOpen, FilePenLine, Globe2, Plus, Share2, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import {
  diagnosisTemplateSyncStateLabel,
  diagnosisTemplateSyncStateTone,
  formatDateTime,
} from '@/lib/format';
import { diagnosisTemplateService } from '@/lib/services';

function buildDraftEditorSearch(rawSearch: string): string {
  const params = new URLSearchParams(rawSearch);
  params.delete('intent');
  if (params.get('pairId')) {
    params.set('step', '3');
  }
  const next = params.toString();
  return next ? `?${next}` : '';
}

function templateShareScopeTone(scope?: string | null): 'success' | 'neutral' {
  return scope === 'PUBLIC' ? 'success' : 'neutral';
}

const TeacherTemplatesPage: React.FC = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const locationSearchParams = React.useMemo(() => new URLSearchParams(location.search), [location.search]);
  const pairId = locationSearchParams.get('pairId');
  const source = locationSearchParams.get('source');
  const wantsCreateDraft = locationSearchParams.get('intent') === 'create-draft';

  const draftsQuery = useQuery({
    queryKey: ['teacher-diagnosis-template-drafts'],
    queryFn: ({ signal }) => diagnosisTemplateService.listDrafts({ pageNo: 1, pageSize: 50 }, { signal }),
  });

  const templatesQuery = useQuery({
    queryKey: ['teacher-diagnosis-templates'],
    queryFn: ({ signal }) => diagnosisTemplateService.listTeacherTemplates({ pageNo: 1, pageSize: 50 }, { signal }),
  });

  const marketTemplatesQuery = useQuery({
    queryKey: ['teacher-diagnosis-template-market'],
    queryFn: ({ signal }) => diagnosisTemplateService.listMarketTemplates({ pageNo: 1, pageSize: 50 }, { signal }),
  });

  const createDraftMutation = useMutation({
    mutationFn: () => diagnosisTemplateService.createDraft(),
    onSuccess: (draft) => {
      setFeedback(t('ui.messages.draftCreated'));
      setErrorMessage(null);
      navigate(`/teacher/diagnosis-template-drafts/${draft.draftId}${buildDraftEditorSearch(location.search)}`);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, t('ui.errors.createDraftFailed')));
    },
  });

  const createFromTemplateMutation = useMutation({
    mutationFn: (templateId: number) => diagnosisTemplateService.createDraftFromTemplate(templateId),
    onSuccess: (draft) => {
      setFeedback(t('ui.messages.editableDraftGenerated'));
      setErrorMessage(null);
      navigate(`/teacher/diagnosis-template-drafts/${draft.draftId}${buildDraftEditorSearch(location.search)}`);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, t('ui.errors.createDraftFromTemplateFailed')));
    },
  });

  const deleteDraftMutation = useMutation({
    mutationFn: (draftId: number) => diagnosisTemplateService.deleteDraft(draftId),
    onSuccess: async () => {
      setFeedback(t('ui.messages.draftDeleted'));
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-template-drafts'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, t('ui.errors.deleteDraftFailed')));
    },
  });

  const updateSharingMutation = useMutation({
    mutationFn: ({ templateId, shareScope }: { templateId: number; shareScope: 'PRIVATE' | 'PUBLIC' }) =>
      diagnosisTemplateService.updateTeacherTemplateSharing(templateId, { shareScope }),
    onSuccess: async (template) => {
      setFeedback(template.shareScope === 'PUBLIC' ? t('ui.messages.templateShared') : t('ui.messages.templateUnshared'));
      setErrorMessage(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-templates'] }),
        queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-template-market'] }),
      ]);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, t('ui.errors.updateTemplateSharingFailed')));
    },
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow={t('ui.sections.publishedTemplates')}
        title={t('ui.pages.templates.title')}
        subtitle={t('ui.pages.templates.subtitle')}
        actions={
          <div className="flex flex-wrap gap-3">
            <Link
              to="/teacher/lexical-pairs"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <BookOpen size={14} />
              {t('ui.actions.goLexicalPairs')}
            </Link>
            <button
              type="button"
              onClick={() => createDraftMutation.mutate()}
              disabled={createDraftMutation.isPending}
              className={`btn-liquid px-5 py-3 text-white inline-flex items-center gap-2 disabled:opacity-60 ${
                wantsCreateDraft ? 'ring-2 ring-amber-400/70 ring-offset-2 ring-offset-transparent' : ''
              }`}
            >
              <Plus size={14} />
              {t('ui.actions.createDraft')}
            </button>
          </div>
        }
      />

      {source && (
        <div className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/70">
          {t('ui.labels.sourceNotice')}
        </div>
      )}

      {wantsCreateDraft && (
        <div className="rounded-[1.8rem] border border-amber-500/20 bg-amber-500/10 px-5 py-4 text-sm text-amber-700 dark:text-amber-300">
          {t('ui.labels.createDraftNotice')}
        </div>
      )}

      {pairId && (
        <div className="rounded-[1.8rem] border border-sky-500/20 bg-sky-500/5 px-5 py-4 text-sm text-sky-700 dark:text-sky-300">
          {t('ui.labels.pairNotice', { id: pairId })}
        </div>
      )}

      {feedback && (
        <div className="rounded-[1.8rem] border border-emerald-500/20 bg-emerald-500/5 px-5 py-4 text-sm text-emerald-600 dark:text-emerald-400">
          {feedback}
        </div>
      )}
      {errorMessage && (
        <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
          {errorMessage}
        </div>
      )}

      <div className="grid gap-8 xl:grid-cols-[0.9fr_1.1fr]">
        <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
          <div>
            <SectionEyebrow>{t('ui.sections.drafts')}</SectionEyebrow>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{t('taskPages.teacherTemplates.draftsTitle')}</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('taskPages.teacherTemplates.draftsSubtitle')}</div>
          </div>

          <div className="space-y-4">
            {draftsQuery.isLoading && (
              <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-5 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                {t('ui.labels.loadingDrafts')}
              </div>
            )}

            {(draftsQuery.data?.records || []).map((draft) => (
              <div
                key={draft.draftId}
                className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]"
              >
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <div className="text-lg font-black text-slate-900 dark:text-white">{draft.templateName}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{draft.description || t('ui.labels.noDescription')}</div>
                  </div>
                  <StatusBadge label={diagnosisTemplateSyncStateLabel(draft.syncState)} tone={diagnosisTemplateSyncStateTone(draft.syncState)} />
                </div>

                <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                  <StatusBadge label={t('ui.meta.draftId', { id: draft.draftId })} />
                  {draft.publishedTemplateId && (
                    <StatusBadge label={t('ui.meta.templateId', { id: draft.publishedTemplateId })} />
                  )}
                  <StatusBadge label={t('ui.meta.version', { value: draft.version })} />
                </div>

                <div className="mt-3 text-xs text-slate-400 dark:text-white/30">{t('ui.meta.lastUpdated', { time: formatDateTime(draft.updatedAt) })}</div>

                <div className="mt-4 flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => navigate(`/teacher/diagnosis-template-drafts/${draft.draftId}${buildDraftEditorSearch(location.search)}`)}
                    className="btn-liquid inline-flex items-center gap-2 px-4 py-3 text-white"
                  >
                    <FilePenLine size={14} />
                    {t('ui.actions.continueEditing')}
                  </button>
                  <button
                    type="button"
                    onClick={() => deleteDraftMutation.mutate(draft.draftId)}
                    className="inline-flex items-center gap-2 rounded-2xl border border-rose-500/20 px-4 py-3 text-sm text-rose-500"
                  >
                    <Trash2 size={14} />
                    {t('ui.actions.deleteDraft')}
                  </button>
                </div>
              </div>
            ))}

            {!draftsQuery.isLoading && !(draftsQuery.data?.records || []).length && (
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-5 py-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                {t('ui.labels.noDrafts')}
              </div>
            )}
          </div>
        </section>

        <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
          <div>
            <SectionEyebrow>{t('ui.sections.publishedTemplates')}</SectionEyebrow>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{t('taskPages.teacherTemplates.publishedTitle')}</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('taskPages.teacherTemplates.publishedSubtitle')}</div>
          </div>

          <div className="space-y-4">
            {templatesQuery.isLoading && (
              <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-5 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                {t('ui.labels.loadingTemplates')}
              </div>
            )}

            {(templatesQuery.data?.records || []).map((template) => (
              <div
                key={template.id}
                className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]"
              >
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <div className="text-lg font-black text-slate-900 dark:text-white">{template.templateName}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{template.description || t('ui.labels.noDescription')}</div>
                  </div>
                  <StatusBadge label={String(template.status)} tone="success" />
                </div>

                <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                  <StatusBadge label={t('ui.meta.questionCount', { count: template.itemCount })} />
                  <StatusBadge label={t('ui.meta.durationMinutes', { count: template.estimatedDurationMinutes })} />
                  <StatusBadge label={template.scoringVersion} />
                  <StatusBadge label={template.targetClassName ? `班级：${template.targetClassName}` : '所有学生可见'} />
                  <StatusBadge
                    label={template.shareScope === 'PUBLIC' ? t('ui.meta.templateShared') : t('ui.meta.templatePrivate')}
                    tone={templateShareScopeTone(template.shareScope)}
                  />
                </div>

                <div className="mt-3 text-xs text-slate-400 dark:text-white/30">{t('ui.meta.lastUpdated', { time: formatDateTime(template.updatedAt) })}</div>

                <div className="mt-4 flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => createFromTemplateMutation.mutate(template.id)}
                    className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
                  >
                    <FilePenLine size={14} />
                    {t('ui.actions.createDraftFromTemplate')}
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      updateSharingMutation.mutate({
                        templateId: template.id,
                        shareScope: template.shareScope === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC',
                      })
                    }
                    disabled={updateSharingMutation.isPending}
                    className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-3 text-sm disabled:opacity-60 dark:border-white/10"
                  >
                    <Share2 size={14} />
                    {template.shareScope === 'PUBLIC' ? t('ui.actions.stopSharingTemplate') : t('ui.actions.shareTemplate')}
                  </button>
                </div>
              </div>
            ))}

            {!templatesQuery.isLoading && !(templatesQuery.data?.records || []).length && (
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-5 py-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                {t('ui.labels.noTemplates')}
              </div>
            )}
          </div>
        </section>
      </div>

      <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <SectionEyebrow>{t('ui.sections.templateMarket')}</SectionEyebrow>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{t('ui.pages.templates.marketTitle')}</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{t('ui.pages.templates.marketSubtitle')}</div>
          </div>
          <div className="inline-flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/5 px-4 py-2 text-sm text-emerald-700 dark:text-emerald-300">
            <Globe2 size={14} />
            {t('ui.labels.marketOnlyPublic')}
          </div>
        </div>

        <div className="space-y-4">
          {marketTemplatesQuery.isLoading && (
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-5 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
              {t('ui.labels.loadingTemplateMarket')}
            </div>
          )}

          {(marketTemplatesQuery.data?.records || []).map((template) => (
            <div
              key={template.id}
              className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]"
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="text-lg font-black text-slate-900 dark:text-white">{template.templateName}</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{template.description || t('ui.labels.noDescription')}</div>
                </div>
                <StatusBadge label={t('ui.meta.templateShared')} tone="success" />
              </div>

              <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                <StatusBadge label={t('ui.meta.questionCount', { count: template.itemCount })} />
                <StatusBadge label={t('ui.meta.durationMinutes', { count: template.estimatedDurationMinutes })} />
                <StatusBadge label={template.scoringVersion} />
                <StatusBadge label={template.targetClassName ? `班级：${template.targetClassName}` : '所有学生可见'} />
                <StatusBadge label={t('ui.meta.ownerName', { name: template.ownerDisplayName || `教师 #${template.ownerUserId}` })} />
              </div>

              <div className="mt-3 text-xs text-slate-400 dark:text-white/30">{t('ui.meta.lastUpdated', { time: formatDateTime(template.updatedAt) })}</div>

              <div className="mt-4">
                <button
                  type="button"
                  onClick={() => createFromTemplateMutation.mutate(template.id)}
                  className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
                >
                  <FilePenLine size={14} />
                  {t('ui.actions.createDraftFromTemplate')}
                </button>
              </div>
            </div>
          ))}

          {!marketTemplatesQuery.isLoading && !(marketTemplatesQuery.data?.records || []).length && (
            <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-5 py-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
              {t('ui.labels.noTemplateMarket')}
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

export default TeacherTemplatesPage;
