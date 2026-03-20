import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@/components/common';
import { adminService } from '@/lib/services';

const AdminUsersPage: React.FC = () => {
  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => adminService.listUsers(),
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader title="用户管理" subtitle="管理员只读视图，直接反映 /api/admin/users 合同。" />

      {usersQuery.error && (
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">{usersQuery.error.message}</div>
      )}

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
