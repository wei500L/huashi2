import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BookOpen, Check, Plus, Search } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import type { AddLexicalListItemsResultVO } from '@/lib/contracts';
import { lexicalListService, lexicalPairService } from '@/lib/services';

function parsePairIdsCsv(value: string): number[] {
  return Array.from(
    new Set(
      value
        .split(',')
        .map((item) => Number(item.trim()))
        .filter((item) => Number.isFinite(item) && item > 0)
    )
  );
}

export function formatAddLexicalListItemsFeedback(result: AddLexicalListItemsResultVO): string {
  const skippedCount = result.skippedPairIds.length;
  return skippedCount
    ? `已添加 ${result.addedCount} 个词对，跳过 ${skippedCount} 个已存在词对。`
    : `已添加 ${result.addedCount} 个词对。`;
}

const TeacherLexicalListsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [newListName, setNewListName] = React.useState('');
  const [newListDescription, setNewListDescription] = React.useState('');
  const [pairIdsCsv, setPairIdsCsv] = React.useState('');
  const [pairSearchKeyword, setPairSearchKeyword] = React.useState('');
  const [selectedPairIds, setSelectedPairIds] = React.useState<number[]>([]);
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);

  const listsQuery = useQuery({
    queryKey: ['lexical-lists'],
    queryFn: ({ signal }) => lexicalListService.pageQuery({ pageNo: 1, pageSize: 50 }, { signal }),
  });

  const detailQuery = useQuery({
    queryKey: ['lexical-list-detail', selectedId],
    queryFn: ({ signal }) => lexicalListService.getDetail(selectedId as number, { signal }),
    enabled: !!selectedId,
  });

  const pairSearchQuery = useQuery({
    queryKey: ['lexical-list-pair-search', selectedId, pairSearchKeyword],
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
    enabled: !!selectedId,
  });

  const manualPairIds = React.useMemo(() => parsePairIdsCsv(pairIdsCsv), [pairIdsCsv]);
  const existingPairIds = React.useMemo(
    () => detailQuery.data?.items.map((item) => item.lexicalPairId) || [],
    [detailQuery.data?.items]
  );
  const existingPairIdSet = React.useMemo(() => new Set(existingPairIds), [existingPairIds]);
  const pendingAddPairIds = React.useMemo(
    () =>
      Array.from(new Set([...manualPairIds, ...selectedPairIds])).filter((pairId) => !existingPairIdSet.has(pairId)),
    [existingPairIdSet, manualPairIds, selectedPairIds]
  );
  const skippedPairIds = React.useMemo(
    () =>
      Array.from(new Set([...manualPairIds, ...selectedPairIds])).filter((pairId) => existingPairIdSet.has(pairId)),
    [existingPairIdSet, manualPairIds, selectedPairIds]
  );

  React.useEffect(() => {
    setSelectedPairIds((current) => current.filter((pairId) => !existingPairIdSet.has(pairId)));
  }, [existingPairIdSet]);

  React.useEffect(() => {
    setPairIdsCsv('');
    setPairSearchKeyword('');
    setSelectedPairIds([]);
  }, [selectedId]);

  const createMutation = useMutation({
    mutationFn: () => lexicalListService.create({ listName: newListName, description: newListDescription, active: true }),
    onSuccess: async (id) => {
      setSelectedId(id);
      setNewListName('');
      setNewListDescription('');
      setFeedback('词表已创建。现在可以直接搜索词对并加入当前词表。');
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', id] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '词表创建失败');
    },
  });

  const addItemsMutation = useMutation({
    mutationFn: () =>
      lexicalListService.addItems(selectedId as number, {
        lexicalPairIds: pendingAddPairIds,
      }),
    onSuccess: async (result) => {
      setPairIdsCsv('');
      setSelectedPairIds([]);
      setFeedback(formatAddLexicalListItemsFeedback(result));
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', selectedId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '词对添加失败');
    },
  });

  const deleteItemMutation = useMutation({
    mutationFn: (itemId: number) => lexicalListService.deleteItem(selectedId as number, itemId),
    onSuccess: async () => {
      setFeedback('词对已从当前词表移除。');
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', selectedId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '词对删除失败');
    },
  });

  const togglePairSelection = React.useCallback(
    (pairId: number) => {
      if (existingPairIdSet.has(pairId)) {
        return;
      }
      setSelectedPairIds((current) =>
        current.includes(pairId) ? current.filter((item) => item !== pairId) : [...current, pairId]
      );
      setFeedback(null);
      setErrorMessage(null);
    },
    [existingPairIdSet]
  );

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="词表管理"
        subtitle="当前词表仍通过 lexicalPairId 建立关联，但现在可以直接搜索词对并批量加入。手动输入 Pair #id 仍作为兜底入口保留。"
        actions={
          <Link
            to="/teacher/lexical-pairs"
            className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
          >
            <BookOpen size={14} />
            去词对管理
          </Link>
        }
      />

      <div className="grid xl:grid-cols-[0.9fr_1.1fr] gap-8">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">lists</div>
          <div className="space-y-4">
            {(listsQuery.data?.records || []).map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  setFeedback(null);
                  setErrorMessage(null);
                  setSelectedId(item.id);
                }}
                className={`w-full text-left rounded-[1.6rem] border p-4 transition-all ${
                  selectedId === item.id
                    ? 'border-primary/40 bg-primary/5'
                    : 'border-slate-200/70 dark:border-white/10 bg-white/60 dark:bg-white/5'
                }`}
              >
                <div className="font-black text-slate-900 dark:text-white">{item.listName}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{item.itemCount} 个词对 · {item.ownerDisplayName || item.ownerUserId}</div>
              </button>
            ))}
          </div>

          <div className="mt-8 space-y-4">
            <input value={newListName} onChange={(event) => setNewListName(event.target.value)} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="新词表名称" />
            <textarea value={newListDescription} onChange={(event) => setNewListDescription(event.target.value)} rows={3} className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="词表描述" />
            <button type="button" onClick={() => createMutation.mutate()} className="btn-liquid px-5 py-3 text-white flex items-center gap-2">
              <Plus size={14} /> 创建词表
            </button>
          </div>
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">detail</div>
          {detailQuery.data ? (
            <div className="space-y-6">
              <div>
                <div className="text-3xl font-black text-slate-900 dark:text-white">{detailQuery.data.listName}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{detailQuery.data.description || '无描述'}</div>
              </div>

              {feedback && (
                <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-400">
                  {feedback}
                </div>
              )}
              {errorMessage && (
                <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
                  {errorMessage}
                </div>
              )}

              <div className="rounded-[1.8rem] border border-slate-200/70 dark:border-white/10 p-5 bg-white/60 dark:bg-white/5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-bold text-slate-900 dark:text-white">添加词对</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      先在这里检索词对，再批量加入当前词表。搜索不到时，仍可直接输入 lexicalPairId。
                    </div>
                  </div>
                  <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                    待添加 {pendingAddPairIds.length} 个
                  </div>
                </div>

                <div className="mt-4 rounded-[1.6rem] border border-slate-200/70 bg-white/70 p-4 dark:border-white/10 dark:bg-white/5">
                  <div className="relative">
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
                      <div className="text-sm text-slate-500 dark:text-white/45">正在加载可选词对...</div>
                    )}
                    {!pairSearchQuery.isLoading &&
                      (pairSearchQuery.data?.records || []).map((pair) => {
                        const isSelected = selectedPairIds.includes(pair.id);
                        const isExisting = existingPairIdSet.has(pair.id);
                        return (
                          <button
                            key={pair.id}
                            type="button"
                            onClick={() => togglePairSelection(pair.id)}
                            disabled={isExisting}
                            className={`w-full rounded-[1.4rem] border p-4 text-left transition-all ${
                              isExisting
                                ? 'cursor-not-allowed border-slate-200/70 bg-slate-100/70 opacity-60 dark:border-white/10 dark:bg-white/5'
                                : isSelected
                                  ? 'border-primary/40 bg-primary/5'
                                  : 'border-slate-200/70 bg-white/70 dark:border-white/10 dark:bg-white/5'
                            }`}
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
                              <span
                                className={`inline-flex items-center gap-2 rounded-full px-3 py-2 text-xs font-bold ${
                                  isExisting
                                    ? 'border border-slate-200/70 text-slate-400 dark:border-white/10 dark:text-white/30'
                                    : isSelected
                                      ? 'bg-primary text-white'
                                      : 'border border-slate-200/70 text-slate-600 dark:border-white/10 dark:text-white/70'
                                }`}
                              >
                                {isSelected && <Check size={12} />}
                                {isExisting ? '已在词表' : isSelected ? '已选择' : '选择'}
                              </span>
                            </div>
                          </button>
                        );
                      })}
                    {!pairSearchQuery.isLoading && !pairSearchQuery.data?.records.length && (
                      <div className="text-sm text-slate-500 dark:text-white/45">
                        没有匹配结果。可以换关键词，或在下方直接输入 Pair #id。
                      </div>
                    )}
                  </div>
                </div>

                <div className="mt-4">
                  <div className="text-xs uppercase tracking-[0.2em] text-slate-400 dark:text-white/30">manual ids</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    兼容旧流程：直接输入 lexicalPairId，逗号分隔，例如 `12, 18, 21`。
                  </div>
                  <input
                    value={pairIdsCsv}
                    onChange={(event) => setPairIdsCsv(event.target.value)}
                    className="mt-3 w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3"
                    placeholder="1, 2, 3"
                  />
                </div>

                {!!pendingAddPairIds.length && (
                  <div className="mt-4 rounded-2xl border border-slate-200/70 bg-white/70 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/65">
                    即将添加 {pendingAddPairIds.map((pairId) => `Pair #${pairId}`).join('、')}
                  </div>
                )}

                {!!skippedPairIds.length && (
                  <div className="mt-3 rounded-2xl border border-amber-500/20 bg-amber-500/5 px-4 py-3 text-sm text-amber-700 dark:text-amber-400">
                    这些词对已在当前词表中，提交时会自动跳过：{skippedPairIds.map((pairId) => `Pair #${pairId}`).join('、')}
                  </div>
                )}

                <div className="mt-4 flex gap-3">
                  <button
                    type="button"
                    onClick={() => addItemsMutation.mutate()}
                    disabled={!pendingAddPairIds.length || addItemsMutation.isPending}
                    className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
                  >
                    {addItemsMutation.isPending ? '添加中...' : `添加词对${pendingAddPairIds.length ? ` (${pendingAddPairIds.length})` : ''}`}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setPairIdsCsv('');
                      setPairSearchKeyword('');
                      setSelectedPairIds([]);
                      setFeedback(null);
                      setErrorMessage(null);
                    }}
                    className="rounded-2xl border border-slate-200/70 px-5 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/70"
                  >
                    清空待选
                  </button>
                </div>
              </div>

              <div className="space-y-4">
                {detailQuery.data.items.map((item) => (
                  <div key={item.itemId} className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 p-4 bg-white/60 dark:bg-white/5">
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <div className="font-black text-slate-900 dark:text-white">{item.englishWord} / {item.frenchWord}</div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          Pair #{item.lexicalPairId} · 排序 {item.sortOrder}
                        </div>
                      </div>
                      <button type="button" onClick={() => deleteItemMutation.mutate(item.itemId)} className="rounded-full border border-rose-500/20 px-4 py-2 text-sm text-rose-500">
                        删除
                      </button>
                    </div>
                  </div>
                ))}
                {!detailQuery.data.items.length && (
                  <div className="text-sm text-slate-500 dark:text-white/45">当前词表还没有词对。</div>
                )}
              </div>
            </div>
          ) : (
            <div className="text-sm text-slate-500 dark:text-white/45">选择左侧词表查看详情。</div>
          )}
        </section>
      </div>
    </div>
  );
};

export default TeacherLexicalListsPage;
