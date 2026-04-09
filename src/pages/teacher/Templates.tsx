import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BookOpen, FilePenLine, Plus, Trash2 } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
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

const TeacherTemplatesPage: React.FC = () => {
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

  const createDraftMutation = useMutation({
    mutationFn: () => diagnosisTemplateService.createDraft(),
    onSuccess: (draft) => {
      setFeedback('已创建新草稿。');
      setErrorMessage(null);
      navigate(`/teacher/diagnosis-template-drafts/${draft.draftId}${buildDraftEditorSearch(location.search)}`);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '创建草稿失败'));
    },
  });

  const createFromTemplateMutation = useMutation({
    mutationFn: (templateId: number) => diagnosisTemplateService.createDraftFromTemplate(templateId),
    onSuccess: (draft) => {
      setFeedback('已生成可编辑草稿。');
      setErrorMessage(null);
      navigate(`/teacher/diagnosis-template-drafts/${draft.draftId}${buildDraftEditorSearch(location.search)}`);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '生成草稿失败'));
    },
  });

  const deleteDraftMutation = useMutation({
    mutationFn: (draftId: number) => diagnosisTemplateService.deleteDraft(draftId),
    onSuccess: async () => {
      setFeedback('草稿已删除。');
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-template-drafts'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '删除草稿失败'));
    },
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="诊断模板"
        subtitle="在这里沉淀可复用的诊断模板，快速整理草稿、完善题项内容，并发布给后续教学场景直接使用。"
        actions={
          <div className="flex flex-wrap gap-3">
            <Link
              to="/teacher/lexical-pairs"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <BookOpen size={14} />
              去词对管理
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
              新建草稿
            </button>
          </div>
        }
      />

      {source && (
        <div className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/70">
          已从教师工作台带入相关信息。你可以直接创建新草稿，或继续完善已有模板，减少重复操作。
        </div>
      )}

      {wantsCreateDraft && (
        <div className="rounded-[1.8rem] border border-amber-500/20 bg-amber-500/10 px-5 py-4 text-sm text-amber-700 dark:text-amber-300">
          建议先新建一份模板草稿，先搭好诊断结构，再逐步补充词对、题项和说明内容。
        </div>
      )}

      {pairId && (
        <div className="rounded-[1.8rem] border border-sky-500/20 bg-sky-500/5 px-5 py-4 text-sm text-sky-700 dark:text-sky-300">
          已带入词对信息。创建或打开草稿后，你可以直接围绕这组词对继续完善模板内容。当前 Pair #{pairId}。
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
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">drafts</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">草稿列表</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">在这里继续完善未完成的模板，随时补充内容并准备发布。</div>
          </div>

          <div className="space-y-4">
            {draftsQuery.isLoading && (
              <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-5 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                正在加载草稿...
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
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{draft.description || '无描述'}</div>
                  </div>
                  <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                    {draft.syncState}
                  </div>
                </div>

                <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">Draft #{draft.draftId}</span>
                  {draft.publishedTemplateId && (
                    <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">Template #{draft.publishedTemplateId}</span>
                  )}
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">v{draft.version}</span>
                </div>

                <div className="mt-3 text-xs text-slate-400 dark:text-white/30">最近更新 {formatDateTime(draft.updatedAt)}</div>

                <div className="mt-4 flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => navigate(`/teacher/diagnosis-template-drafts/${draft.draftId}${buildDraftEditorSearch(location.search)}`)}
                    className="btn-liquid inline-flex items-center gap-2 px-4 py-3 text-white"
                  >
                    <FilePenLine size={14} />
                    继续编辑
                  </button>
                  <button
                    type="button"
                    onClick={() => deleteDraftMutation.mutate(draft.draftId)}
                    className="inline-flex items-center gap-2 rounded-2xl border border-rose-500/20 px-4 py-3 text-sm text-rose-500"
                  >
                    <Trash2 size={14} />
                    删除草稿
                  </button>
                </div>
              </div>
            ))}

            {!draftsQuery.isLoading && !(draftsQuery.data?.records || []).length && (
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-5 py-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                当前还没有草稿。先创建一个空白草稿，把模板名称、词对和题项内容逐步补齐即可。
              </div>
            )}
          </div>
        </section>

        <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
          <div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">published</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">已发布模板</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">查看已经可用的模板，并基于现有内容快速生成新版草稿继续优化。</div>
          </div>

          <div className="space-y-4">
            {templatesQuery.isLoading && (
              <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-5 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                正在加载模板...
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
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{template.description || '无描述'}</div>
                  </div>
                  <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                    {template.status}
                  </div>
                </div>

                <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{template.itemCount} 个题项</span>
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{template.estimatedDurationMinutes} 分钟</span>
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{template.scoringVersion}</span>
                </div>

                <div className="mt-3 text-xs text-slate-400 dark:text-white/30">最近更新 {formatDateTime(template.updatedAt)}</div>

                <div className="mt-4">
                  <button
                    type="button"
                    onClick={() => createFromTemplateMutation.mutate(template.id)}
                    className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
                  >
                    <FilePenLine size={14} />
                    基于此模板创建草稿
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
};

export default TeacherTemplatesPage;
