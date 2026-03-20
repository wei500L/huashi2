import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { diagnosisTemplateService } from '@/lib/services';
import type { DiagnosisTemplateDetailVO, DiagnosisTemplateItemVO, DiagnosisTemplateUpsertRequest } from '@/lib/contracts';

type TemplateEditorState = {
  id?: number;
  templateName: string;
  description: string;
  status: string;
  estimatedDurationMinutes: number;
  scoringVersion: string;
  itemsJson: string;
};

const emptyTemplateEditor: TemplateEditorState = {
  templateName: '新建诊断模板',
  description: '',
  status: 'DRAFT',
  estimatedDurationMinutes: 10,
  scoringVersion: 'RULE_V1',
  itemsJson: JSON.stringify(
    [
      {
        lexicalPairId: 1,
        taskType: 'REACTION_TIME',
        blockCode: 'B1',
        sortOrder: 1,
        contextSupportLevel: 'LOW',
        expectedSemanticMatch: true,
        stimulus: {
          instruction: '判断词义是否一致',
          promptText: '请快速作答',
          contextSentence: '',
        },
        options: [
          { key: 'semantic_match', label: '语义一致', semanticMatch: true, ignoreContextTrap: false },
          { key: 'semantic_mismatch', label: '语义不一致', semanticMatch: false, ignoreContextTrap: false },
        ],
        correctAnswerKey: 'semantic_match',
        scoringProfile: {
          reactionTimeWeight: 1,
          hesitationWeight: 1,
          accuracyWeight: 1,
        },
      },
    ],
    null,
    2
  ),
};

function toEditor(detail?: DiagnosisTemplateDetailVO | null): TemplateEditorState {
  if (!detail) {
    return emptyTemplateEditor;
  }
  return {
    id: detail.id,
    templateName: detail.templateName,
    description: detail.description || '',
    status: detail.status,
    estimatedDurationMinutes: detail.estimatedDurationMinutes,
    scoringVersion: detail.scoringVersion,
    itemsJson: JSON.stringify(
      detail.items.map((item) => ({
        lexicalPairId: item.lexicalPairId,
        taskType: item.taskType,
        blockCode: item.blockCode,
        sortOrder: item.sortOrder,
        contextSupportLevel: item.contextSupportLevel,
        expectedSemanticMatch: item.expectedSemanticMatch,
        stimulus: item.stimulus,
        options: item.options,
        correctAnswerKey: item.correctAnswerKey,
        scoringProfile: item.scoringProfile,
      })),
      null,
      2
    ),
  };
}

function sanitizeItems(items: DiagnosisTemplateItemVO[]): DiagnosisTemplateUpsertRequest['items'] {
  return items.map((item) => ({
    lexicalPairId: Number(item.lexicalPairId),
    taskType: item.taskType,
    blockCode: item.blockCode,
    sortOrder: Number(item.sortOrder),
    contextSupportLevel: item.contextSupportLevel,
    expectedSemanticMatch: Boolean(item.expectedSemanticMatch),
    stimulus: {
      instruction: item.stimulus?.instruction || '',
      contextSentence: item.stimulus?.contextSentence || '',
      promptText: item.stimulus?.promptText || '',
    },
    options: (item.options || []).map((option) => ({
      key: option.key,
      label: option.label,
      semanticMatch: option.semanticMatch ?? null,
      ignoreContextTrap: option.ignoreContextTrap ?? false,
    })),
    correctAnswerKey: item.correctAnswerKey,
    scoringProfile: item.scoringProfile || null,
  }));
}

const TeacherTemplatesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [editor, setEditor] = React.useState<TemplateEditorState>(emptyTemplateEditor);
  const [parseError, setParseError] = React.useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ['teacher-diagnosis-templates'],
    queryFn: () => diagnosisTemplateService.listTeacherTemplates({ pageNo: 1, pageSize: 50 }),
  });

  const detailQuery = useQuery({
    queryKey: ['teacher-diagnosis-template', selectedId],
    queryFn: () => diagnosisTemplateService.getTeacherTemplate(selectedId as number),
    enabled: !!selectedId,
  });

  React.useEffect(() => {
    if (detailQuery.data) {
      setEditor(toEditor(detailQuery.data));
      setParseError(null);
    }
  }, [detailQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const items = JSON.parse(editor.itemsJson) as DiagnosisTemplateItemVO[];
      const payload: DiagnosisTemplateUpsertRequest = {
        templateName: editor.templateName,
        description: editor.description,
        status: editor.status,
        estimatedDurationMinutes: Number(editor.estimatedDurationMinutes),
        scoringVersion: editor.scoringVersion,
        items: sanitizeItems(items),
      };
      if (editor.id) {
        return diagnosisTemplateService.updateTeacherTemplate(editor.id, payload);
      }
      return diagnosisTemplateService.createTeacherTemplate(payload);
    },
    onSuccess: async (id) => {
      setSelectedId(id);
      setParseError(null);
      await queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-templates'] });
      await queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-template', id] });
    },
    onError: (error) => {
      setParseError(error instanceof Error ? error.message : '模板保存失败');
    },
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="诊断模板"
        subtitle="列表、详情和编辑器全部绑定真实教师模板接口。当前不提供删除动作。"
        actions={
          <button
            type="button"
            onClick={() => {
              setSelectedId(null);
              setEditor(emptyTemplateEditor);
              setParseError(null);
            }}
            className="btn-liquid px-5 py-3 text-white flex items-center gap-2"
          >
            <Plus size={14} /> 新建模板
          </button>
        }
      />

      <div className="grid xl:grid-cols-[0.9fr_1.1fr] gap-8">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">templates</div>
          <div className="space-y-4">
            {(listQuery.data?.records || []).map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => setSelectedId(item.id)}
                className={`w-full text-left rounded-[1.6rem] border p-4 transition-all ${
                  selectedId === item.id
                    ? 'border-primary/40 bg-primary/5'
                    : 'border-slate-200/70 dark:border-white/10 bg-white/60 dark:bg-white/5'
                }`}
              >
                <div className="font-black text-slate-900 dark:text-white">{item.templateName}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{item.status} · {item.itemCount} 题 · {item.estimatedDurationMinutes} 分钟</div>
              </button>
            ))}
            {!listQuery.isLoading && !listQuery.data?.records.length && (
              <div className="text-sm text-slate-500 dark:text-white/45">当前没有模板。</div>
            )}
          </div>
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">editor</div>
          <div className="space-y-5">
            <input
              value={editor.templateName}
              onChange={(event) => setEditor((state) => ({ ...state, templateName: event.target.value }))}
              className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
              placeholder="模板名称"
            />
            <textarea
              value={editor.description}
              onChange={(event) => setEditor((state) => ({ ...state, description: event.target.value }))}
              rows={3}
              className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
              placeholder="模板描述"
            />
            <div className="grid md:grid-cols-3 gap-4">
              <input
                value={editor.status}
                onChange={(event) => setEditor((state) => ({ ...state, status: event.target.value.toUpperCase() }))}
                className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
                placeholder="DRAFT / PUBLISHED / ARCHIVED"
              />
              <input
                type="number"
                value={editor.estimatedDurationMinutes}
                onChange={(event) => setEditor((state) => ({ ...state, estimatedDurationMinutes: Number(event.target.value) }))}
                className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
                placeholder="时长"
              />
              <input
                value={editor.scoringVersion}
                onChange={(event) => setEditor((state) => ({ ...state, scoringVersion: event.target.value }))}
                className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
                placeholder="RULE_V1"
              />
            </div>
            <textarea
              value={editor.itemsJson}
              onChange={(event) => setEditor((state) => ({ ...state, itemsJson: event.target.value }))}
              rows={20}
              className="w-full rounded-3xl border border-slate-200 dark:border-white/10 bg-slate-950 text-slate-100 px-4 py-4 font-mono text-sm"
            />
            {parseError && <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{parseError}</div>}
            <button
              type="button"
              onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending}
              className="btn-liquid px-6 py-3 text-white disabled:opacity-60"
            >
              {saveMutation.isPending ? '保存中...' : editor.id ? '更新模板' : '创建模板'}
            </button>
          </div>
        </section>
      </div>
    </div>
  );
};

export default TeacherTemplatesPage;
