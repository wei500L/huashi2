/* eslint-disable react-refresh/only-export-components */
import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDown, ArrowUp, Check, Link as LinkIcon, Plus, Search, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { LexicalPairSuggestionInput } from '@/components/common/LexicalPairSuggestionInput';
import { contextLevelLabel, formatDateTime, lexicalPairTypeLabel, riskLevelLabel } from '@/lib/format';
import type {
  AddLexicalListItemsResultVO,
  LexicalListDetailVO,
  LexicalListItemVO,
  LexicalPairDetailVO,
  LexicalPairSummaryVO,
  UpdateLexicalListRequest,
} from '@/lib/contracts';
import { lexicalListService, lexicalPairService } from '@/lib/services';

type ListEditorState = UpdateLexicalListRequest;

const emptyCreateForm = {
  listName: '',
  description: '',
  active: true,
};

const emptyEditorForm: ListEditorState = {
  listName: '',
  description: '',
  active: true,
};

export function formatAddLexicalListItemsFeedback(result: AddLexicalListItemsResultVO): string {
  const skippedCount = result.skippedPairIds.length;
  return skippedCount
    ? `已添加 ${result.addedCount} 个词对，跳过 ${skippedCount} 个已存在词对。`
    : `已添加 ${result.addedCount} 个词对。`;
}

function formatPairLabel(englishWord: string, frenchWord: string): string {
  return `${englishWord} / ${frenchWord}`;
}

function moveOrderedItemIds(detail: LexicalListDetailVO, itemId: number, delta: -1 | 1): number[] | null {
  const currentIndex = detail.items.findIndex((item) => item.itemId === itemId);
  if (currentIndex < 0) {
    return null;
  }
  const nextIndex = currentIndex + delta;
  if (nextIndex < 0 || nextIndex >= detail.items.length) {
    return null;
  }
  const orderedIds = detail.items.map((item) => item.itemId);
  const [targetId] = orderedIds.splice(currentIndex, 1);
  orderedIds.splice(nextIndex, 0, targetId);
  return orderedIds;
}

const TeacherLexicalListsPage: React.FC = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const pairIdFromQuery = Number(searchParams.get('pairId') || '0');
  const listIdFromQuery = Number(searchParams.get('listId') || '0');
  const hasPairContext = Number.isFinite(pairIdFromQuery) && pairIdFromQuery > 0;
  const hasListContext = Number.isFinite(listIdFromQuery) && listIdFromQuery > 0;
  const wantsCreateList = searchParams.get('intent') === 'create-list';
  const source = searchParams.get('source');

  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [listKeyword, setListKeyword] = React.useState('');
  const [itemKeyword, setItemKeyword] = React.useState('');
  const [itemPageNo, setItemPageNo] = React.useState(1);
  const [pairSearchKeyword, setPairSearchKeyword] = React.useState('');
  const [selectedPairIds, setSelectedPairIds] = React.useState<number[]>([]);
  const [createForm, setCreateForm] = React.useState(emptyCreateForm);
  const [editorForm, setEditorForm] = React.useState<ListEditorState>(emptyEditorForm);
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [deleteListConfirmOpen, setDeleteListConfirmOpen] = React.useState(false);

  const listsQuery = useQuery({
    queryKey: ['lexical-lists', listKeyword],
    queryFn: ({ signal }) =>
      lexicalListService.pageQuery(
        {
          pageNo: 1,
          pageSize: 100,
          keyword: listKeyword.trim() || undefined,
        },
        { signal }
      ),
  });

  const detailQuery = useQuery({
    queryKey: ['lexical-list-detail', selectedId],
    queryFn: ({ signal }) => lexicalListService.getDetail(selectedId as number, { signal }),
    enabled: selectedId !== null,
  });

  const itemsQuery = useQuery({
    queryKey: ['lexical-list-items', selectedId, itemPageNo, itemKeyword],
    queryFn: ({ signal }) =>
      lexicalListService.pageItems(
        selectedId as number,
        {
          pageNo: itemPageNo,
          pageSize: 8,
          keyword: itemKeyword.trim() || undefined,
        },
        { signal }
      ),
    enabled: selectedId !== null,
  });

  const pairFocusQuery = useQuery({
    queryKey: ['lexical-list-focus-pair', pairIdFromQuery],
    queryFn: ({ signal }) => lexicalPairService.getDetail(pairIdFromQuery, { signal }),
    enabled: hasPairContext,
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
    enabled: selectedId !== null,
  });

  const existingPairIds = React.useMemo(
    () => detailQuery.data?.items.map((item) => item.lexicalPairId) || [],
    [detailQuery.data?.items]
  );
  const existingPairIdSet = React.useMemo(() => new Set(existingPairIds), [existingPairIds]);

  const candidatePairs = React.useMemo(() => {
    const byId = new Map<number, LexicalPairDetailVO | LexicalPairSummaryVO>();
    if (pairFocusQuery.data) {
      byId.set(pairFocusQuery.data.id, pairFocusQuery.data);
    }
    for (const pair of pairSearchQuery.data?.records || []) {
      byId.set(pair.id, pair);
    }
    return Array.from(byId.values());
  }, [pairFocusQuery.data, pairSearchQuery.data?.records]);

  const pendingPairIds = React.useMemo(
    () => selectedPairIds.filter((pairId) => !existingPairIdSet.has(pairId)),
    [existingPairIdSet, selectedPairIds]
  );

  const totalItemPages = React.useMemo(() => {
    if (!itemsQuery.data) {
      return 1;
    }
    return Math.max(1, Math.ceil(itemsQuery.data.total / itemsQuery.data.pageSize));
  }, [itemsQuery.data]);

  React.useEffect(() => {
    const availableIds = new Set((listsQuery.data?.records || []).map((item) => item.id));
    if (selectedId !== null && availableIds.has(selectedId)) {
      return;
    }
    if (hasListContext && availableIds.has(listIdFromQuery)) {
      setSelectedId(listIdFromQuery);
      return;
    }
    const firstId = listsQuery.data?.records?.[0]?.id ?? null;
    setSelectedId(firstId);
  }, [hasListContext, listIdFromQuery, listsQuery.data?.records, selectedId]);

  React.useEffect(() => {
    const nextSearchParams = new URLSearchParams(searchParams);
    if (selectedId) {
      nextSearchParams.set('listId', String(selectedId));
      if (nextSearchParams.get('intent') === 'create-list') {
        nextSearchParams.delete('intent');
      }
    } else {
      nextSearchParams.delete('listId');
    }
    if (nextSearchParams.toString() !== searchParams.toString()) {
      setSearchParams(nextSearchParams, { replace: true });
    }
  }, [searchParams, selectedId, setSearchParams]);

  React.useEffect(() => {
    if (!detailQuery.data) {
      return;
    }
    setEditorForm({
      listName: detailQuery.data.listName,
      description: detailQuery.data.description || '',
      active: detailQuery.data.active,
    });
  }, [detailQuery.data]);

  React.useEffect(() => {
    setItemPageNo(1);
    setItemKeyword('');
    setPairSearchKeyword('');
    setSelectedPairIds([]);
    setFeedback(null);
    setErrorMessage(null);
  }, [selectedId]);

  React.useEffect(() => {
    if (!hasPairContext || selectedId === null || !pairFocusQuery.data) {
      return;
    }
    if (existingPairIdSet.has(pairFocusQuery.data.id)) {
      return;
    }
    setSelectedPairIds((current) =>
      current.includes(pairFocusQuery.data.id) ? current : [...current, pairFocusQuery.data.id]
    );
  }, [existingPairIdSet, hasPairContext, pairFocusQuery.data, selectedId]);

  const createMutation = useMutation({
    mutationFn: () =>
      lexicalListService.create({
        listName: createForm.listName.trim(),
        description: createForm.description.trim() || undefined,
        active: createForm.active,
      }),
    onSuccess: async (id) => {
      setCreateForm(emptyCreateForm);
      setSelectedId(id);
      setFeedback('词表已创建。现在可以直接搜索词对并加入当前词表。');
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '词表创建失败');
    },
  });

  const updateMutation = useMutation({
    mutationFn: () => lexicalListService.update(selectedId as number, editorForm),
    onSuccess: async () => {
      setFeedback('词表信息已更新。');
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', selectedId] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '词表更新失败');
    },
  });

  const deleteListMutation = useMutation({
    mutationFn: () => lexicalListService.delete(selectedId as number),
    onSuccess: async () => {
      const deletedId = selectedId;
      setDeleteListConfirmOpen(false);
      setFeedback('词表已删除。');
      setErrorMessage(null);
      setSelectedId(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
      if (deletedId !== null) {
        await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', deletedId] });
        await queryClient.invalidateQueries({ queryKey: ['lexical-list-items', deletedId] });
      }
    },
    onError: (error) => {
      setDeleteListConfirmOpen(false);
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '词表删除失败');
    },
  });

  const addItemsMutation = useMutation({
    mutationFn: () =>
      lexicalListService.addItems(selectedId as number, {
        lexicalPairIds: pendingPairIds,
      }),
    onSuccess: async (result) => {
      setSelectedPairIds([]);
      setPairSearchKeyword('');
      setFeedback(formatAddLexicalListItemsFeedback(result));
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', selectedId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-items', selectedId] });
      if (hasPairContext) {
        const nextSearchParams = new URLSearchParams(searchParams);
        nextSearchParams.delete('pairId');
        setSearchParams(nextSearchParams, { replace: true });
      }
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
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', selectedId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-items', selectedId] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '词对删除失败');
    },
  });

  const reorderMutation = useMutation({
    mutationFn: (orderedItemIds: number[]) =>
      lexicalListService.reorderItems(selectedId as number, {
        orderedItemIds,
      }),
    onSuccess: async () => {
      setFeedback('词表排序已更新。');
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', selectedId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-items', selectedId] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '词表排序保存失败');
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

  const moveItem = React.useCallback(
    (item: LexicalListItemVO, delta: -1 | 1) => {
      if (!detailQuery.data) {
        return;
      }
      const orderedItemIds = moveOrderedItemIds(detailQuery.data, item.itemId, delta);
      if (!orderedItemIds) {
        return;
      }
      reorderMutation.mutate(orderedItemIds);
    },
    [detailQuery.data, reorderMutation]
  );

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow="词表"
        title={t('taskPages.teacherLexicalLists.pageTitle')}
        subtitle={t('taskPages.teacherLexicalLists.pageSubtitle')}
        actions={
          <Link
            to="/teacher/lexical-pairs"
            className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
          >
            <LinkIcon size={14} />
            {t('ui.actions.goLexicalPairs')}
          </Link>
        }
      />

      {source && (
        <div className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/70">
          当前从教师工作台进入。所选词表和上下文参数会同步到 URL，刷新后仍可恢复当前工作位置。
        </div>
      )}

      {wantsCreateList && (
        <div className="rounded-[1.8rem] border border-amber-500/20 bg-amber-500/10 px-5 py-4 text-sm text-amber-700 dark:text-amber-300">
          教师工作台建议你先创建一份词表。系统不会自动创建资源，但左侧“新建词表”区域已作为当前主动作高亮显示。
        </div>
      )}

      {(feedback || errorMessage) && (
        <div
          className={`rounded-[1.8rem] border px-5 py-4 text-sm ${
            errorMessage
              ? 'border-rose-500/20 bg-rose-500/5 text-rose-500'
              : 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
          }`}
        >
          {errorMessage || feedback}
        </div>
      )}

      <div className="grid gap-8 xl:grid-cols-[0.82fr_1.18fr]">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <SectionEyebrow>列表</SectionEyebrow>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">词表列表</div>
            </div>
            <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
              共 {listsQuery.data?.total ?? 0} 个
            </div>
          </div>

          <div className="relative">
            <Search
              size={16}
              className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-white/30"
            />
            <input
              value={listKeyword}
              onChange={(event) => setListKeyword(event.target.value)}
              className="w-full rounded-2xl border border-slate-200 bg-white/70 py-3 pl-11 pr-4 dark:border-white/10 dark:bg-white/5"
              placeholder="搜索词表名称"
            />
          </div>

          <div className="space-y-4">
            {(listsQuery.data?.records || []).map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => setSelectedId(item.id)}
                className={`w-full rounded-[1.7rem] border p-4 text-left transition-all ${
                  selectedId === item.id
                    ? 'border-primary/40 bg-primary/5'
                    : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/5'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="font-black text-slate-900 dark:text-white">{item.listName}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      {item.itemCount} 个词对 · {item.active ? '启用中' : '已停用'}
                    </div>
                  </div>
                  <div className="text-xs text-slate-400 dark:text-white/30">{formatDateTime(item.updatedAt || item.createdAt)}</div>
                </div>
              </button>
            ))}

            {!listsQuery.isLoading && !(listsQuery.data?.records || []).length && (
              <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/55 px-5 py-6 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                当前还没有词表。先创建一个词表，再把词对接到模板或训练链路里。
              </div>
            )}
          </div>

          <div
            className={`rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5 space-y-4 ${
              wantsCreateList ? 'ring-2 ring-amber-400/70 ring-offset-2 ring-offset-transparent' : ''
            }`}
          >
            <div>
              <div className="text-sm font-bold text-slate-900 dark:text-white">新建词表</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                词表创建完成后，可直接从右侧搜索词对加入，也可以接收词对工作台带来的条目。
              </div>
            </div>
            <input
              value={createForm.listName}
              onChange={(event) => setCreateForm((current) => ({ ...current, listName: event.target.value }))}
              className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
              placeholder="词表名称"
            />
            <textarea
              value={createForm.description}
              onChange={(event) => setCreateForm((current) => ({ ...current, description: event.target.value }))}
              rows={3}
              className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
              placeholder="词表描述"
            />
            <label className="inline-flex items-center gap-2 text-sm text-slate-600 dark:text-white/60">
              <input
                type="checkbox"
                checked={createForm.active}
                onChange={(event) => setCreateForm((current) => ({ ...current, active: event.target.checked }))}
              />
              创建后立即启用
            </label>
            <button
              type="button"
              onClick={() => createMutation.mutate()}
              disabled={!createForm.listName.trim() || createMutation.isPending}
              className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:opacity-60"
            >
              <Plus size={14} />
              {createMutation.isPending ? '创建中...' : '创建词表'}
            </button>
          </div>
        </section>

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          {detailQuery.data ? (
            <div className="space-y-8">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <SectionEyebrow>详情</SectionEyebrow>
                  <div className="mt-3 text-3xl font-black text-slate-900 dark:text-white">{detailQuery.data.listName}</div>
                  <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
                    创建于 {formatDateTime(detailQuery.data.createdAt)} · 最近更新 {formatDateTime(detailQuery.data.updatedAt || detailQuery.data.createdAt)}
                  </div>
                </div>
                <StatusBadge label={`${detailQuery.data.itemCount} 个词对`} className="px-4 py-2 text-sm" />
              </div>

              <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5 space-y-4">
                <div className="text-sm font-bold text-slate-900 dark:text-white">词表信息</div>
                <div className="grid gap-4 md:grid-cols-2">
                  <input
                    value={editorForm.listName}
                    onChange={(event) => setEditorForm((current) => ({ ...current, listName: event.target.value }))}
                    className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    placeholder="词表名称"
                  />
                  <label className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60">
                    <input
                      type="checkbox"
                      checked={editorForm.active}
                      onChange={(event) => setEditorForm((current) => ({ ...current, active: event.target.checked }))}
                    />
                    词表启用
                  </label>
                </div>
                <textarea
                  value={editorForm.description || ''}
                  onChange={(event) => setEditorForm((current) => ({ ...current, description: event.target.value }))}
                  rows={3}
                  className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  placeholder="词表描述"
                />
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => updateMutation.mutate()}
                    disabled={!editorForm.listName.trim() || updateMutation.isPending}
                    className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
                  >
                    {updateMutation.isPending ? '保存中...' : '保存词表'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setDeleteListConfirmOpen(true);
                    }}
                    disabled={deleteListMutation.isPending}
                    className="rounded-2xl border border-rose-500/20 px-5 py-3 text-sm text-rose-500 disabled:opacity-60"
                  >
                    <Trash2 size={14} className="mr-2 inline-block" />
                    {deleteListMutation.isPending ? '删除中...' : '删除词表'}
                  </button>
                </div>
              </div>

              {pairFocusQuery.data && (
                <div className="rounded-[1.8rem] border border-sky-500/20 bg-sky-500/5 p-5">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <div className="text-sm font-black text-sky-800 dark:text-sky-200">来自词对工作台的待加入词对</div>
                      <div className="mt-2 text-sm text-sky-700 dark:text-sky-300">
                        {formatPairLabel(pairFocusQuery.data.englishWord, pairFocusQuery.data.frenchWord)} · {pairFocusQuery.data.chineseGloss}
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => togglePairSelection(pairFocusQuery.data.id)}
                      disabled={existingPairIdSet.has(pairFocusQuery.data.id)}
                      className="rounded-2xl border border-sky-500/20 px-4 py-3 text-sm text-sky-700 disabled:opacity-60 dark:text-sky-300"
                    >
                      {existingPairIdSet.has(pairFocusQuery.data.id) ? '已在词表中' : '加入待选'}
                    </button>
                  </div>
                </div>
              )}

              <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/5 space-y-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-bold text-slate-900 dark:text-white">加入词对</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      直接搜索词对并加入当前词表，不再要求手工记录 Pair ID。
                    </div>
                  </div>
                  <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                    待加入 {pendingPairIds.length} 个
                  </div>
                </div>

                <LexicalPairSuggestionInput
                  value={pairSearchKeyword}
                  onChange={setPairSearchKeyword}
                  onSuggestionSelect={(suggestion) => {
                    setPairSearchKeyword(suggestion.englishWord);
                    togglePairSelection(suggestion.id);
                  }}
                  active
                  placeholder="搜索英文、法文、中文、拼音或首字母"
                />

                <div className="space-y-3">
                  {pairSearchQuery.isLoading && (
                    <div className="text-sm text-slate-500 dark:text-white/45">正在加载可选词对...</div>
                  )}
                  {!pairSearchQuery.isLoading &&
                    candidatePairs.map((pair) => {
                      const isSelected = selectedPairIds.includes(pair.id);
                      const isExisting = existingPairIdSet.has(pair.id);
                      return (
                        <button
                          key={pair.id}
                          type="button"
                          onClick={() => togglePairSelection(pair.id)}
                          disabled={isExisting}
                          className={`w-full rounded-[1.5rem] border p-4 text-left transition-all ${
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
                                {formatPairLabel(pair.englishWord, pair.frenchWord)}
                              </div>
                              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{pair.chineseGloss}</div>
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
                  {!pairSearchQuery.isLoading && !candidatePairs.length && (
                    <div className="rounded-[1.5rem] border border-dashed border-slate-300 bg-white/55 px-4 py-5 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                      当前没有匹配词对，换个关键词后再试。
                    </div>
                  )}
                </div>

                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => addItemsMutation.mutate()}
                    disabled={!pendingPairIds.length || addItemsMutation.isPending}
                    className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
                  >
                    {addItemsMutation.isPending ? '添加中...' : `加入词表${pendingPairIds.length ? ` (${pendingPairIds.length})` : ''}`}
                  </button>
                  <button
                    type="button"
                    onClick={() => setSelectedPairIds([])}
                    className="rounded-2xl border border-slate-200/70 px-5 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/70"
                  >
                    清空待选
                  </button>
                </div>
              </div>

              <div className="space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-bold text-slate-900 dark:text-white">词表条目</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      支持按关键词筛选、移除条目，并对当前词表顺序做显式调整。
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <input
                      value={itemKeyword}
                      onChange={(event) => {
                        setItemKeyword(event.target.value);
                        setItemPageNo(1);
                      }}
                      className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 text-sm dark:border-white/10 dark:bg-white/5"
                      placeholder="筛选当前词表条目"
                    />
                  </div>
                </div>

                {(itemsQuery.data?.records || []).map((item) => {
                  const allItems = detailQuery.data.items;
                  const itemIndex = allItems.findIndex((current) => current.itemId === item.itemId);
                  const canMoveUp = itemIndex > 0;
                  const canMoveDown = itemIndex >= 0 && itemIndex < allItems.length - 1;
                  return (
                    <div
                      key={item.itemId}
                      className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/5"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-4">
                        <div>
                          <div className="font-black text-slate-900 dark:text-white">
                            {formatPairLabel(item.englishWord, item.frenchWord)}
                          </div>
                          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                            {item.chineseGloss} · 排序 {item.sortOrder}
                          </div>
                          <div className="mt-2 flex flex-wrap gap-2">
                            <StatusBadge label={lexicalPairTypeLabel(item.lexicalPairType)} />
                            <StatusBadge label={riskLevelLabel(item.riskLevel)} tone={item.riskLevel === 'HIGH' || item.riskLevel === 'CRITICAL' ? 'danger' : item.riskLevel === 'MEDIUM' ? 'warning' : 'success'} />
                            <StatusBadge label={contextLevelLabel(item.defaultContextSupport)} tone="info" />
                          </div>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Link
                            to={`/teacher/lexical-pairs/${item.lexicalPairId}/edit`}
                            className="rounded-2xl border border-slate-200/70 px-4 py-2 text-sm text-slate-600 dark:border-white/10 dark:text-white/70"
                          >
                            查看词对
                          </Link>
                          <button
                            type="button"
                            onClick={() => moveItem(item, -1)}
                            disabled={!canMoveUp || reorderMutation.isPending}
                            className="rounded-2xl border border-slate-200/70 px-3 py-2 text-sm text-slate-600 disabled:opacity-40 dark:border-white/10 dark:text-white/70"
                          >
                            <ArrowUp size={14} />
                          </button>
                          <button
                            type="button"
                            onClick={() => moveItem(item, 1)}
                            disabled={!canMoveDown || reorderMutation.isPending}
                            className="rounded-2xl border border-slate-200/70 px-3 py-2 text-sm text-slate-600 disabled:opacity-40 dark:border-white/10 dark:text-white/70"
                          >
                            <ArrowDown size={14} />
                          </button>
                          <button
                            type="button"
                            onClick={() => deleteItemMutation.mutate(item.itemId)}
                            disabled={deleteItemMutation.isPending}
                            className="rounded-2xl border border-rose-500/20 px-4 py-2 text-sm text-rose-500 disabled:opacity-60"
                          >
                            删除
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}

                {!itemsQuery.isLoading && !(itemsQuery.data?.records || []).length && (
                  <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/55 px-5 py-6 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                    当前词表还没有符合筛选条件的条目。
                  </div>
                )}

                <div className="flex flex-wrap items-center justify-between gap-3 rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 text-sm text-slate-500 dark:border-white/10 dark:bg-white/5 dark:text-white/45">
                  <div>
                    第 {itemsQuery.data?.pageNo ?? 1} / {totalItemPages} 页 · 共 {itemsQuery.data?.total ?? 0} 条
                  </div>
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={() => setItemPageNo((current) => Math.max(1, current - 1))}
                      disabled={itemPageNo <= 1}
                      className="rounded-2xl border border-slate-200/70 px-4 py-2 disabled:opacity-40 dark:border-white/10"
                    >
                      上一页
                    </button>
                    <button
                      type="button"
                      onClick={() => setItemPageNo((current) => Math.min(totalItemPages, current + 1))}
                      disabled={itemPageNo >= totalItemPages}
                      className="rounded-2xl border border-slate-200/70 px-4 py-2 disabled:opacity-40 dark:border-white/10"
                    >
                      下一页
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/55 px-6 py-10 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
              先从左侧选择词表，或者新建一个词表后再开始维护条目。
            </div>
          )}
        </section>
        <ConfirmationDialog
          open={deleteListConfirmOpen && Boolean(detailQuery.data)}
          title="确认删除当前词表？"
          description={`词表“${detailQuery.data?.listName ?? '--'}”及其维护关系将被删除。`}
          safety="此操作不可撤销；词表中的关联关系会被移除，但词对本身不会被删除。"
          nextStep="先确认词表名称和影响范围；如需保留请取消，仅在确认无误后删除。"
          confirmLabel="确认删除词表"
          cancelLabel="取消，保留词表"
          pending={deleteListMutation.isPending}
          pendingTitle="正在删除词表"
          pendingDescription="删除请求已经提交，请等待服务器确认。"
          onCancel={() => setDeleteListConfirmOpen(false)}
          onConfirm={() => deleteListMutation.mutate()}
        />
      </div>
    </div>
  );
};

export default TeacherLexicalListsPage;
