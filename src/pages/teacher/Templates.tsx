import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BookOpen, Copy, Plus, Search } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { diagnosisTemplateService, lexicalPairService } from '@/lib/services';
import type {
  DiagnosisTemplateDetailVO,
  DiagnosisTemplateItemVO,
  DiagnosisTemplateUpsertRequest,
  LexicalPairSummaryVO,
} from '@/lib/contracts';

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

function parseTemplateItemsJson(itemsJson: string): DiagnosisTemplateItemVO[] {
  const parsed = JSON.parse(itemsJson) as unknown;
  if (!Array.isArray(parsed)) {
    throw new Error('items JSON 必须是数组。');
  }
  return parsed as DiagnosisTemplateItemVO[];
}

function buildTemplateItemFromPair(pair: LexicalPairSummaryVO, sortOrder: number): DiagnosisTemplateItemVO {
  const expectedSemanticMatch = pair.semanticOverlapScore >= 0.5;
  return {
    lexicalPairId: pair.id,
    englishWord: pair.englishWord,
    frenchWord: pair.frenchWord,
    chineseGloss: pair.chineseGloss,
    lexicalPairType: pair.lexicalPairType,
    taskType: 'REACTION_TIME',
    blockCode: `B${Math.max(1, Math.ceil(sortOrder / 5))}`,
    sortOrder,
    contextSupportLevel: pair.defaultContextSupport?.toUpperCase() || 'LOW',
    expectedSemanticMatch,
    stimulus: {
      instruction: '结合语境判断英法词义是否一致',
      promptText: `${pair.englishWord} / ${pair.frenchWord}`,
      contextSentence: '',
    },
    options: [
      { key: 'semantic_match', label: '语义一致', semanticMatch: true, ignoreContextTrap: false },
      { key: 'semantic_mismatch', label: '语义不一致', semanticMatch: false, ignoreContextTrap: false },
    ],
    correctAnswerKey: expectedSemanticMatch ? 'semantic_match' : 'semantic_mismatch',
    scoringProfile: {
      reactionTimeWeight: 1,
      hesitationWeight: 1,
      accuracyWeight: 1,
    },
  };
}

const TeacherTemplatesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [editor, setEditor] = React.useState<TemplateEditorState>(emptyTemplateEditor);
  const [parseError, setParseError] = React.useState<string | null>(null);
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [pairSearchKeyword, setPairSearchKeyword] = React.useState('');

  const listQuery = useQuery({
    queryKey: ['teacher-diagnosis-templates'],
    queryFn: ({ signal }) => diagnosisTemplateService.listTeacherTemplates({ pageNo: 1, pageSize: 50 }, { signal }),
  });

  const detailQuery = useQuery({
    queryKey: ['teacher-diagnosis-template', selectedId],
    queryFn: ({ signal }) => diagnosisTemplateService.getTeacherTemplate(selectedId as number, { signal }),
    enabled: !!selectedId,
  });

  const pairSearchQuery = useQuery({
    queryKey: ['teacher-template-pair-search', pairSearchKeyword],
    queryFn: ({ signal }) =>
      lexicalPairService.pageQuery(
        {
          pageNo: 1,
          pageSize: 8,
          keyword: pairSearchKeyword.trim() || undefined,
          active: true,
        },
        { signal }
      ),
  });

  const parsedItemCount = React.useMemo(() => {
    try {
      return parseTemplateItemsJson(editor.itemsJson).length;
    } catch {
      return null;
    }
  }, [editor.itemsJson]);

  React.useEffect(() => {
    if (detailQuery.data) {
      setEditor(toEditor(detailQuery.data));
      setParseError(null);
    }
  }, [detailQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const items = parseTemplateItemsJson(editor.itemsJson);
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
      setFeedback(editor.id ? '模板已更新。' : '模板已创建。');
      await queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-templates'] });
      await queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-template', id] });
    },
    onError: (error) => {
      setParseError(error instanceof Error ? error.message : '模板保存失败');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (templateId: number) => diagnosisTemplateService.deleteTeacherTemplate(templateId),
    onSuccess: async (result) => {
      setParseError(null);
      if (result.outcome === 'DELETED') {
        setFeedback('模板已删除。');
        setSelectedId(null);
        setEditor(emptyTemplateEditor);
      } else {
        setFeedback('模板已有学生使用，已自动归档。');
        setEditor((state) => ({ ...state, status: result.status || 'ARCHIVED' }));
      }
      await queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-templates'] });
      if (result.outcome === 'ARCHIVED') {
        await queryClient.invalidateQueries({ queryKey: ['teacher-diagnosis-template', result.templateId] });
      }
    },
    onError: (error) => {
      setFeedback(null);
      setParseError(error instanceof Error ? error.message : '模板删除失败');
    },
  });

  const insertPairIntoTemplate = React.useCallback(
    (pair: LexicalPairSummaryVO) => {
      try {
        const currentItems = parseTemplateItemsJson(editor.itemsJson);
        const useEmptySeed = !editor.id && editor.itemsJson === emptyTemplateEditor.itemsJson;
        const baseItems = useEmptySeed ? [] : currentItems;
        if (baseItems.some((item) => Number(item.lexicalPairId) === pair.id)) {
          setFeedback(`Pair #${pair.id} 已存在于当前模板中。`);
          setParseError(null);
          return;
        }

        const nextItems = [...baseItems, buildTemplateItemFromPair(pair, baseItems.length + 1)];
        setEditor((state) => ({ ...state, itemsJson: JSON.stringify(nextItems, null, 2) }));
        setFeedback(`已插入 Pair #${pair.id} 的题目骨架。你可以继续调整 taskType、contextSupportLevel 或选项。`);
        setParseError(null);
      } catch (error) {
        setFeedback(null);
        setParseError(error instanceof Error ? error.message : '当前 JSON 无法解析，暂时不能插入词对');
      }
    },
    [editor.id, editor.itemsJson]
  );

  const copyPairId = React.useCallback(async (pairId: number) => {
    try {
      if (!navigator.clipboard) {
        throw new Error('当前浏览器不支持复制到剪贴板');
      }
      await navigator.clipboard.writeText(String(pairId));
      setFeedback(`Pair #${pairId} 已复制到剪贴板。`);
      setParseError(null);
    } catch (error) {
      setFeedback(null);
      setParseError(error instanceof Error ? error.message : '复制 Pair #id 失败');
    }
  }, []);

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="诊断模板"
        subtitle="模板 item 仍需要显式引用 lexicalPairId，但现在可以直接检索词对并插入题目骨架，再继续手改 JSON。"
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
              onClick={() => {
                setSelectedId(null);
                setEditor(emptyTemplateEditor);
                setParseError(null);
                setFeedback(null);
                setPairSearchKeyword('');
              }}
              className="btn-liquid px-5 py-3 text-white flex items-center gap-2"
            >
              <Plus size={14} /> 新建模板
            </button>
          </div>
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
                onClick={() => {
                  setParseError(null);
                  setFeedback(null);
                  setSelectedId(item.id);
                }}
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

            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-white">检索词对并插入题目骨架</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    插入时会自动带上 lexicalPairId、默认语境支持、排序和一组基础选项，后面仍可手工修改。
                  </div>
                </div>
                <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                  {parsedItemCount === null ? 'JSON 待修正' : `当前 ${parsedItemCount} 题`}
                </div>
              </div>

              <div className="relative mt-4">
                <Search
                  size={16}
                  className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-white/30"
                />
                <input
                  value={pairSearchKeyword}
                  onChange={(event) => setPairSearchKeyword(event.target.value)}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 pl-11 pr-4 py-3"
                  placeholder="搜索英文、法文或中文释义"
                />
              </div>

              <div className="mt-4 space-y-3">
                {pairSearchQuery.isLoading && (
                  <div className="text-sm text-slate-500 dark:text-white/45">正在加载可引用的词对...</div>
                )}
                {!pairSearchQuery.isLoading &&
                  (pairSearchQuery.data?.records || []).map((pair) => (
                    <div
                      key={pair.id}
                      className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 p-4 dark:border-white/10 dark:bg-white/5"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div>
                          <div className="font-black text-slate-900 dark:text-white">
                            {pair.englishWord} / {pair.frenchWord}
                          </div>
                          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                            Pair #{pair.id} · {pair.chineseGloss}
                          </div>
                          <div className="mt-2 text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-white/30">
                            {pair.lexicalPairType} · {pair.riskLevel} · {pair.defaultContextSupport}
                          </div>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <button
                            type="button"
                            onClick={() => {
                              void copyPairId(pair.id);
                            }}
                            className="inline-flex items-center gap-2 rounded-full border border-slate-200/70 px-3 py-2 text-xs text-slate-600 dark:border-white/10 dark:text-white/70"
                          >
                            <Copy size={12} />
                            复制 Pair #id
                          </button>
                          <button
                            type="button"
                            onClick={() => insertPairIntoTemplate(pair)}
                            className="btn-liquid px-4 py-2 text-xs text-white"
                          >
                            插入题目骨架
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                {!pairSearchQuery.isLoading && !pairSearchQuery.data?.records.length && (
                  <div className="text-sm text-slate-500 dark:text-white/45">
                    没有匹配词对。你可以换关键词，或先去词对管理确认导入是否完成。
                  </div>
                )}
              </div>
            </div>

            <div>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="text-sm font-bold text-slate-900 dark:text-white">题目 JSON</div>
                <div className="text-xs text-slate-500 dark:text-white/45">
                  {parsedItemCount === null ? '当前 JSON 无法解析' : `当前共 ${parsedItemCount} 条 item`}
                </div>
              </div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                保存时会直接提交这个数组。插入的骨架只是起点，仍建议补充上下文句和更精细的评分参数。
              </div>
              <textarea
                value={editor.itemsJson}
                onChange={(event) => setEditor((state) => ({ ...state, itemsJson: event.target.value }))}
                rows={20}
                className="mt-4 w-full rounded-3xl border border-slate-200 dark:border-white/10 bg-slate-950 text-slate-100 px-4 py-4 font-mono text-sm"
              />
            </div>
            {feedback && <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-400">{feedback}</div>}
            {parseError && <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{parseError}</div>}
            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => saveMutation.mutate()}
                disabled={saveMutation.isPending}
                className="btn-liquid px-6 py-3 text-white disabled:opacity-60"
              >
                {saveMutation.isPending ? '保存中...' : editor.id ? '更新模板' : '创建模板'}
              </button>
              {editor.id && (
                <button
                  type="button"
                  onClick={() => {
                    const templateId = editor.id;
                    if (!templateId) {
                      return;
                    }
                    if (!window.confirm('确认删除该模板？如果已有学生使用，系统会自动改为归档。')) {
                      return;
                    }
                    deleteMutation.mutate(templateId);
                  }}
                  disabled={deleteMutation.isPending}
                  className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-6 py-3 text-rose-500 disabled:opacity-60"
                >
                  {deleteMutation.isPending ? '处理中...' : '删除模板'}
                </button>
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default TeacherTemplatesPage;
