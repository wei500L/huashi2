import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Plus, Upload } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { lexicalPairService } from '@/lib/services';
import { lexicalPairTypeLabel } from '@/lib/format';
import type { LexicalPairDetailVO, LexicalPairSenseVO, LexicalPairUpsertRequest } from '@/lib/contracts';

type PairEditorState = {
  id?: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  semanticOverlapScore: number;
  falseFriendRisk: number;
  defaultContextSupport: string;
  difficultyLevel: number;
  notes: string;
  source: string;
  active: boolean;
  tagsCsv: string;
  sensesJson: string;
};

const emptyPairEditor: PairEditorState = {
  englishWord: '',
  frenchWord: '',
  chineseGloss: '',
  lexicalPairType: 'COGNATE',
  semanticOverlapScore: 0.5,
  falseFriendRisk: 0.1,
  defaultContextSupport: 'LOW',
  difficultyLevel: 3,
  notes: '',
  source: 'teacher_manual',
  active: true,
  tagsCsv: '',
  sensesJson: JSON.stringify(
    [
      {
        sortOrder: 1,
        englishDefinition: '',
        frenchDefinition: '',
        chineseDefinition: '',
        examples: [],
      },
    ],
    null,
    2
  ),
};

function toEditor(detail?: LexicalPairDetailVO | null): PairEditorState {
  if (!detail) {
    return emptyPairEditor;
  }
  return {
    id: detail.id,
    englishWord: detail.englishWord,
    frenchWord: detail.frenchWord,
    chineseGloss: detail.chineseGloss,
    lexicalPairType: detail.lexicalPairType,
    semanticOverlapScore: detail.semanticOverlapScore,
    falseFriendRisk: detail.falseFriendRisk,
    defaultContextSupport: detail.defaultContextSupport,
    difficultyLevel: detail.difficultyLevel,
    notes: detail.notes || '',
    source: detail.source || '',
    active: detail.active,
    tagsCsv: detail.tags.join(', '),
    sensesJson: JSON.stringify(
      detail.senses.map((sense) => ({
        sortOrder: sense.sortOrder,
        englishDefinition: sense.englishDefinition,
        frenchDefinition: sense.frenchDefinition,
        chineseDefinition: sense.chineseDefinition,
        examples: sense.examples.map((example) => ({
          sortOrder: example.sortOrder,
          englishExample: example.englishExample,
          frenchExample: example.frenchExample,
          chineseTranslation: example.chineseTranslation,
          contextSupportLevel: example.contextSupportLevel,
          source: example.source || '',
        })),
      })),
      null,
      2
    ),
  };
}

function sanitizePairPayload(editor: PairEditorState): LexicalPairUpsertRequest {
  const senses = JSON.parse(editor.sensesJson) as LexicalPairSenseVO[];
  return {
    englishWord: editor.englishWord,
    frenchWord: editor.frenchWord,
    chineseGloss: editor.chineseGloss,
    lexicalPairType: editor.lexicalPairType,
    semanticOverlapScore: Number(editor.semanticOverlapScore),
    falseFriendRisk: Number(editor.falseFriendRisk),
    defaultContextSupport: editor.defaultContextSupport,
    difficultyLevel: Number(editor.difficultyLevel),
    notes: editor.notes,
    source: editor.source,
    active: editor.active,
    tags: editor.tagsCsv
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean),
    senses: senses.map((sense) => ({
      sortOrder: Number(sense.sortOrder),
      englishDefinition: sense.englishDefinition,
      frenchDefinition: sense.frenchDefinition,
      chineseDefinition: sense.chineseDefinition,
      examples: sense.examples.map((example) => ({
        sortOrder: Number(example.sortOrder),
        englishExample: example.englishExample,
        frenchExample: example.frenchExample,
        chineseTranslation: example.chineseTranslation,
        contextSupportLevel: example.contextSupportLevel,
        source: example.source || '',
      })),
    })),
  };
}

const TeacherLexicalPairsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [editor, setEditor] = React.useState<PairEditorState>(emptyPairEditor);
  const [error, setError] = React.useState<string | null>(null);
  const [importFile, setImportFile] = React.useState<File | null>(null);
  const [importResult, setImportResult] = React.useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ['lexical-pairs'],
    queryFn: () => lexicalPairService.pageQuery({ pageNo: 1, pageSize: 50 }),
  });

  const detailQuery = useQuery({
    queryKey: ['lexical-pair-detail', selectedId],
    queryFn: () => lexicalPairService.getDetail(selectedId as number),
    enabled: !!selectedId,
  });

  React.useEffect(() => {
    if (detailQuery.data) {
      setEditor(toEditor(detailQuery.data));
      setError(null);
    }
  }, [detailQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = sanitizePairPayload(editor);
      if (editor.id) {
        return lexicalPairService.update(editor.id, payload);
      }
      return lexicalPairService.create(payload);
    },
    onSuccess: async (id) => {
      setSelectedId(id);
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-pair-detail', id] });
    },
    onError: (mutationError) => {
      setError(mutationError instanceof Error ? mutationError.message : '词对保存失败');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => lexicalPairService.delete(id),
    onSuccess: async () => {
      setSelectedId(null);
      setEditor(emptyPairEditor);
      await queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
    },
  });

  const importTemplateMutation = useMutation({
    mutationFn: () => lexicalPairService.getImportTemplate(),
    onSuccess: (template) => {
      const blob = new Blob([`${template.headerLine}\n${template.exampleLine}\n`], { type: 'text/csv;charset=utf-8' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = 'lexical-pairs-template.csv';
      link.click();
      URL.revokeObjectURL(link.href);
    },
  });

  const importMutation = useMutation({
    mutationFn: async () => {
      if (!importFile) {
        throw new Error('请先选择 CSV 文件');
      }
      const formData = new FormData();
      formData.append('file', importFile);
      return lexicalPairService.importCsv(formData);
    },
    onSuccess: (result) => {
      setImportResult(`导入完成：成功 ${result.successCount} 条，失败 ${result.failedCount} 条`);
      void queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
    },
    onError: (mutationError) => {
      setImportResult(mutationError instanceof Error ? mutationError.message : '导入失败');
    },
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="词对管理"
        subtitle="真实接入 lexical pairs CRUD、CSV 模板与导入。"
        actions={
          <div className="flex gap-3">
            <button type="button" onClick={() => importTemplateMutation.mutate()} className="rounded-full border border-slate-200 dark:border-white/10 px-4 py-3 text-sm flex items-center gap-2">
              <Download size={14} /> 下载模板
            </button>
            <button
              type="button"
              onClick={() => {
                setSelectedId(null);
                setEditor(emptyPairEditor);
                setError(null);
              }}
              className="btn-liquid px-5 py-3 text-white flex items-center gap-2"
            >
              <Plus size={14} /> 新建词对
            </button>
          </div>
        }
      />

      <div className="grid xl:grid-cols-[0.9fr_1.1fr] gap-8">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">lexical pairs</div>
          <div className="space-y-4 max-h-[720px] overflow-y-auto no-scrollbar">
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
                <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                  {lexicalPairTypeLabel(item.lexicalPairType)} · 风险 {item.falseFriendRisk} · 难度 {item.difficultyLevel}
                </div>
              </button>
            ))}
          </div>

          <div className="mt-8 space-y-3">
            <input type="file" accept=".csv" onChange={(event) => setImportFile(event.target.files?.[0] || null)} className="block w-full text-sm" />
            <button type="button" onClick={() => importMutation.mutate()} className="rounded-full border border-slate-200 dark:border-white/10 px-4 py-3 text-sm flex items-center gap-2">
              <Upload size={14} /> 导入 CSV
            </button>
            {importResult && <div className="text-sm text-slate-500 dark:text-white/45">{importResult}</div>}
          </div>
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="space-y-5">
            <div className="grid md:grid-cols-2 gap-4">
              <input value={editor.englishWord} onChange={(event) => setEditor((state) => ({ ...state, englishWord: event.target.value }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="English word" />
              <input value={editor.frenchWord} onChange={(event) => setEditor((state) => ({ ...state, frenchWord: event.target.value }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="French word" />
            </div>
            <input value={editor.chineseGloss} onChange={(event) => setEditor((state) => ({ ...state, chineseGloss: event.target.value }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="中文释义" />
            <div className="grid md:grid-cols-3 gap-4">
              <input value={editor.lexicalPairType} onChange={(event) => setEditor((state) => ({ ...state, lexicalPairType: event.target.value.toUpperCase() }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="词对类型" />
              <input type="number" step="0.01" value={editor.semanticOverlapScore} onChange={(event) => setEditor((state) => ({ ...state, semanticOverlapScore: Number(event.target.value) }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="语义重叠分" />
              <input type="number" step="0.01" value={editor.falseFriendRisk} onChange={(event) => setEditor((state) => ({ ...state, falseFriendRisk: Number(event.target.value) }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="负迁移风险" />
            </div>
            <div className="grid md:grid-cols-3 gap-4">
              <input value={editor.defaultContextSupport} onChange={(event) => setEditor((state) => ({ ...state, defaultContextSupport: event.target.value.toUpperCase() }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="默认语境支持" />
              <input type="number" value={editor.difficultyLevel} onChange={(event) => setEditor((state) => ({ ...state, difficultyLevel: Number(event.target.value) }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="难度 1-5" />
              <input value={editor.tagsCsv} onChange={(event) => setEditor((state) => ({ ...state, tagsCsv: event.target.value }))} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="标签，逗号分隔" />
            </div>
            <textarea value={editor.notes} onChange={(event) => setEditor((state) => ({ ...state, notes: event.target.value }))} rows={3} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="备注" />
            <textarea value={editor.sensesJson} onChange={(event) => setEditor((state) => ({ ...state, sensesJson: event.target.value }))} rows={18} className="w-full rounded-3xl border border-slate-200 dark:border-white/10 bg-slate-950 text-slate-100 px-4 py-4 font-mono text-sm" />
            {error && <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{error}</div>}
            <div className="flex gap-3">
              <button type="button" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending} className="btn-liquid px-6 py-3 text-white disabled:opacity-60">
                {editor.id ? '更新词对' : '创建词对'}
              </button>
              {editor.id && (
                <button type="button" onClick={() => deleteMutation.mutate(editor.id!)} className="rounded-full border border-rose-500/20 px-5 py-3 text-sm text-rose-500">
                  删除
                </button>
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default TeacherLexicalPairsPage;
