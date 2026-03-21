import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Database, Shield } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { formatDateTime } from '@/lib/format';
import { adminService, lexicalPairService } from '@/lib/services';

const AdminUsersPage: React.FC = () => {
  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: ({ signal }) => adminService.listUsers({ signal }),
  });

  const overviewQuery = useQuery({
    queryKey: ['lexical-pair-overview'],
    queryFn: ({ signal }) => lexicalPairService.getOverview({ signal }),
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="用户管理"
        subtitle="管理员只读视图，直接反映 /api/admin/users 合同。常用运维入口放在右侧，避免反复切换工作区。"
        actions={
          <div className="flex flex-wrap gap-3">
            <Link
              to="/admin/lexical-pairs"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <Database size={14} />
              语料库管理
            </Link>
            <Link
              to="/admin/config-center"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <Shield size={14} />
              配置中心
            </Link>
          </div>
        }
      />

      {usersQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{usersQuery.error.message}</div>
      )}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <Link
          to="/admin/lexical-pairs"
          className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 text-left dark:border-white/10 dark:bg-white/[0.03]"
        >
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">语料总量</div>
          <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">{overviewQuery.data?.totalCount ?? '--'}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">启用中 {overviewQuery.data?.activeCount ?? '--'} 条</div>
        </Link>
        <Link
          to="/admin/lexical-pairs"
          className="rounded-[2rem] border border-amber-500/20 bg-amber-500/5 px-5 py-5 text-left"
        >
          <div className="text-[11px] uppercase tracking-[0.28em] text-amber-600/70 dark:text-amber-400/70">待嵌入</div>
          <div className="mt-2 text-3xl font-black text-amber-600 dark:text-amber-400">
            {overviewQuery.data?.pendingEmbeddingCount ?? '--'}
          </div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">适合导入后优先关注</div>
        </Link>
        <Link
          to="/admin/lexical-pairs"
          className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 px-5 py-5 text-left"
        >
          <div className="text-[11px] uppercase tracking-[0.28em] text-rose-500/70">嵌入失败</div>
          <div className="mt-2 text-3xl font-black text-rose-500">{overviewQuery.data?.failedEmbeddingCount ?? '--'}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">点击进入语料页继续排查</div>
        </Link>
        <Link
          to="/admin/lexical-pairs"
          className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 text-left dark:border-white/10 dark:bg-white/[0.03]"
        >
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">最近语料更新</div>
          <div className="mt-3 text-base font-black text-slate-900 dark:text-white">
            {formatDateTime(overviewQuery.data?.latestUpdatedAt)}
          </div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
            最近新增 {formatDateTime(overviewQuery.data?.latestCreatedAt)}
          </div>
        </Link>
      </section>

      <section className="rounded-[2.5rem] liquid-glass-panel p-8 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="text-left text-slate-400 dark:text-white/30 uppercase tracking-[0.24em] text-[10px]">
            <tr>
              <th className="py-4">Display Name</th>
              <th className="py-4">Username</th>
              <th className="py-4">Email</th>
              <th className="py-4">Roles</th>
              <th className="py-4">Enabled</th>
            </tr>
          </thead>
          <tbody>
            {(usersQuery.data || []).map((user) => (
              <tr key={user.id} className="border-t border-slate-200/70 dark:border-white/10">
                <td className="py-4 font-black text-slate-900 dark:text-white">{user.displayName}</td>
                <td className="py-4 text-slate-500 dark:text-white/45">{user.username}</td>
                <td className="py-4 text-slate-500 dark:text-white/45">{user.email}</td>
                <td className="py-4 text-slate-500 dark:text-white/45">{user.roles.join(', ')}</td>
                <td className="py-4 text-slate-500 dark:text-white/45">{user.enabled ? 'Enabled' : 'Disabled'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!usersQuery.isLoading && !usersQuery.data?.length && (
          <div className="pt-6 text-sm text-slate-500 dark:text-white/45">当前没有用户数据。</div>
        )}
      </section>
    </div>
  );
};

export default AdminUsersPage;
