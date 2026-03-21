import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { lexicalListService } from '@/lib/services';

const TeacherLexicalListsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [newListName, setNewListName] = React.useState('');
  const [newListDescription, setNewListDescription] = React.useState('');
  const [pairIdsCsv, setPairIdsCsv] = React.useState('');

  const listsQuery = useQuery({
    queryKey: ['lexical-lists'],
    queryFn: ({ signal }) => lexicalListService.pageQuery({ pageNo: 1, pageSize: 50 }, { signal }),
  });

  const detailQuery = useQuery({
    queryKey: ['lexical-list-detail', selectedId],
    queryFn: ({ signal }) => lexicalListService.getDetail(selectedId as number, { signal }),
    enabled: !!selectedId,
  });

  const createMutation = useMutation({
    mutationFn: () => lexicalListService.create({ listName: newListName, description: newListDescription, active: true }),
    onSuccess: async (id) => {
      setSelectedId(id);
      setNewListName('');
      setNewListDescription('');
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', id] });
    },
  });

  const addItemsMutation = useMutation({
    mutationFn: () =>
      lexicalListService.addItems(selectedId as number, {
        lexicalPairIds: pairIdsCsv
          .split(',')
          .map((item) => Number(item.trim()))
          .filter((item) => Number.isFinite(item) && item > 0),
      }),
    onSuccess: async () => {
      setPairIdsCsv('');
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', selectedId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
    },
  });

  const deleteItemMutation = useMutation({
    mutationFn: (itemId: number) => lexicalListService.deleteItem(selectedId as number, itemId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lexical-list-detail', selectedId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-lists'] });
    },
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader title="词表管理" subtitle="真实接入 lexical list 创建、详情、添加词对和删除词表项。" />

      <div className="grid xl:grid-cols-[0.9fr_1.1fr] gap-8">
        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30 mb-6">lists</div>
          <div className="space-y-4">
            {(listsQuery.data?.records || []).map((item) => (
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

              <div className="rounded-[1.8rem] border border-slate-200/70 dark:border-white/10 p-5 bg-white/60 dark:bg-white/5">
                <div className="text-sm font-bold text-slate-900 dark:text-white">添加词对</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">输入 lexicalPairId，逗号分隔。</div>
                <div className="mt-4 flex gap-3">
                  <input value={pairIdsCsv} onChange={(event) => setPairIdsCsv(event.target.value)} className="flex-1 rounded-2xl border border-slate-200 dark:border-white/10 bg-white/70 dark:bg-white/5 px-4 py-3" placeholder="1, 2, 3" />
                  <button type="button" onClick={() => addItemsMutation.mutate()} className="btn-liquid px-5 py-3 text-white">
                    添加
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
