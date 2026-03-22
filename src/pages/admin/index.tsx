import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Database, Plus, Shield } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { adminService, lexicalPairService } from '@/lib/services';
import type { AdminUserAccessUpdateRequest, AdminUserCreateRequest, Role, UserSummaryVO } from '@/lib/contracts';
import { useAuthStore } from '@/store';

const roleOptions: Array<{ value: Role; label: string }> = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'TEACHER', label: '教师' },
  { value: 'STUDENT', label: '学生' },
];

const emptyCreateForm: AdminUserCreateRequest = {
  username: '',
  email: '',
  displayName: '',
  initialPassword: '',
  enabled: true,
  roles: ['STUDENT'],
};

function toggleRole(roles: Role[], role: Role): Role[] {
  return roles.includes(role) ? roles.filter((item) => item !== role) : [...roles, role];
}

function roleSummary(roles: Role[]): string {
  return roles.join(', ');
}

const AdminUsersPage: React.FC = () => {
  const queryClient = useQueryClient();
  const currentUserId = useAuthStore((state) => state.user?.id ?? null);
  const [showCreateForm, setShowCreateForm] = React.useState(false);
  const [createForm, setCreateForm] = React.useState<AdminUserCreateRequest>(emptyCreateForm);
  const [createError, setCreateError] = React.useState<string | null>(null);
  const [createFeedback, setCreateFeedback] = React.useState<string | null>(null);
  const [editingUser, setEditingUser] = React.useState<UserSummaryVO | null>(null);
  const [accessForm, setAccessForm] = React.useState<AdminUserAccessUpdateRequest>({ enabled: true, roles: ['STUDENT'] });
  const [accessError, setAccessError] = React.useState<string | null>(null);
  const [accessFeedback, setAccessFeedback] = React.useState<string | null>(null);

  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: ({ signal }) => adminService.listUsers({ signal }),
  });

  const overviewQuery = useQuery({
    queryKey: ['lexical-pair-overview'],
    queryFn: ({ signal }) => lexicalPairService.getOverview({ signal }),
  });

  const createMutation = useMutation({
    mutationFn: (payload: AdminUserCreateRequest) => adminService.createUser(payload),
    onSuccess: async (user) => {
      setCreateError(null);
      setCreateFeedback(`已创建用户 ${user.username}。`);
      setCreateForm(emptyCreateForm);
      setShowCreateForm(false);
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (error) => {
      setCreateFeedback(null);
      setCreateError(error instanceof Error ? error.message : '创建用户失败');
    },
  });

  const updateAccessMutation = useMutation({
    mutationFn: ({ userId, payload }: { userId: number; payload: AdminUserAccessUpdateRequest }) =>
      adminService.updateUserAccess(userId, payload),
    onSuccess: async (user) => {
      setAccessError(null);
      setAccessFeedback(`已更新 ${user.username} 的访问权限。`);
      setEditingUser(user);
      setAccessForm({ enabled: user.enabled, roles: user.roles });
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (error) => {
      setAccessFeedback(null);
      setAccessError(error instanceof Error ? error.message : '更新访问权限失败');
    },
  });

  const startEditing = (user: UserSummaryVO) => {
    setEditingUser(user);
    setAccessForm({
      enabled: user.enabled,
      roles: user.roles,
    });
    setAccessError(null);
    setAccessFeedback(null);
  };

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="用户管理"
        subtitle="支持新建账号、启用/禁用以及角色分配。档案字段暂不在本页维护。"
        actions={
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => {
                setShowCreateForm((value) => !value);
                setCreateError(null);
                setCreateFeedback(null);
              }}
              className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white"
            >
              <Plus size={14} />
              新建用户
            </button>
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

      {createFeedback && !showCreateForm && (
        <div className="rounded-[2rem] border border-emerald-500/20 bg-emerald-500/5 p-6 text-emerald-600 dark:text-emerald-400">
          {createFeedback}
        </div>
      )}

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
        <div className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 text-left dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">系统用户数</div>
          <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">{usersQuery.data?.length ?? '--'}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">管理员可在下方直接变更启用状态和角色</div>
        </div>
        <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 px-5 py-5 text-left">
          <div className="text-[11px] uppercase tracking-[0.28em] text-rose-500/70">已禁用账号</div>
          <div className="mt-2 text-3xl font-black text-rose-500">
            {usersQuery.data?.filter((user) => !user.enabled).length ?? '--'}
          </div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">变更后立即生效，下一次鉴权会按新权限校验</div>
        </div>
      </section>

      {showCreateForm && (
        <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-5">
          <div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">create user</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">创建基础账号</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">本次只维护账号、角色和启用状态，不创建学生/教师档案。</div>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <input
              value={createForm.username}
              onChange={(event) => setCreateForm((state) => ({ ...state, username: event.target.value }))}
              placeholder="用户名"
              className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
            />
            <input
              value={createForm.email}
              onChange={(event) => setCreateForm((state) => ({ ...state, email: event.target.value }))}
              placeholder="邮箱"
              className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
            />
            <input
              value={createForm.displayName}
              onChange={(event) => setCreateForm((state) => ({ ...state, displayName: event.target.value }))}
              placeholder="显示名称"
              className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
            />
            <input
              type="password"
              value={createForm.initialPassword}
              onChange={(event) => setCreateForm((state) => ({ ...state, initialPassword: event.target.value }))}
              placeholder="初始密码"
              className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
            />
          </div>

          <div className="flex flex-wrap gap-3">
            {roleOptions.map((role) => (
              <label
                key={role.value}
                className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10"
              >
                <input
                  type="checkbox"
                  checked={createForm.roles.includes(role.value)}
                  onChange={() =>
                    setCreateForm((state) => ({
                      ...state,
                      roles: toggleRole(state.roles, role.value),
                    }))
                  }
                />
                {role.label}
              </label>
            ))}
            <label className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10">
              <input
                type="checkbox"
                checked={createForm.enabled}
                onChange={(event) => setCreateForm((state) => ({ ...state, enabled: event.target.checked }))}
              />
              创建后立即启用
            </label>
          </div>

          {createFeedback && <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-400">{createFeedback}</div>}
          {createError && <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{createError}</div>}

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => createMutation.mutate(createForm)}
              disabled={createMutation.isPending}
              className="btn-liquid px-6 py-3 text-white disabled:opacity-60"
            >
              {createMutation.isPending ? '创建中...' : '创建用户'}
            </button>
            <button
              type="button"
              onClick={() => {
                setShowCreateForm(false);
                setCreateForm(emptyCreateForm);
                setCreateError(null);
              }}
              className="rounded-2xl border border-slate-200 px-6 py-3 text-sm dark:border-white/10"
            >
              取消
            </button>
          </div>
        </section>
      )}

      {editingUser && (
        <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-5">
          <div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">access control</div>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <div className="text-2xl font-black text-slate-900 dark:text-white">{editingUser.displayName}</div>
              <div className="rounded-full border border-slate-200 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                {editingUser.username}
              </div>
              {editingUser.id === currentUserId && (
                <div className="rounded-full border border-amber-500/20 bg-amber-500/10 px-3 py-1 text-xs text-amber-600 dark:text-amber-400">
                  当前登录账号
                </div>
              )}
            </div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">调整启用状态和角色集合。保存时会整体替换角色。</div>
          </div>

          <div className="flex flex-wrap gap-3">
            {roleOptions.map((role) => (
              <label
                key={role.value}
                className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10"
              >
                <input
                  type="checkbox"
                  checked={accessForm.roles.includes(role.value)}
                  onChange={() =>
                    setAccessForm((state) => ({
                      ...state,
                      roles: toggleRole(state.roles, role.value),
                    }))
                  }
                />
                {role.label}
              </label>
            ))}
            <label className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10">
              <input
                type="checkbox"
                checked={accessForm.enabled}
                onChange={(event) => setAccessForm((state) => ({ ...state, enabled: event.target.checked }))}
              />
              账号启用
            </label>
          </div>

          {accessFeedback && <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-400">{accessFeedback}</div>}
          {accessError && <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{accessError}</div>}

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() =>
                updateAccessMutation.mutate({
                  userId: editingUser.id,
                  payload: accessForm,
                })
              }
              disabled={updateAccessMutation.isPending}
              className="btn-liquid px-6 py-3 text-white disabled:opacity-60"
            >
              {updateAccessMutation.isPending ? '保存中...' : '保存访问权限'}
            </button>
            <button
              type="button"
              onClick={() => {
                setEditingUser(null);
                setAccessError(null);
                setAccessFeedback(null);
              }}
              className="rounded-2xl border border-slate-200 px-6 py-3 text-sm dark:border-white/10"
            >
              关闭
            </button>
          </div>
        </section>
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
              <th className="py-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {(usersQuery.data || []).map((user) => (
              <tr key={user.id} className="border-t border-slate-200/70 dark:border-white/10">
                <td className="py-4 font-black text-slate-900 dark:text-white">{user.displayName}</td>
                <td className="py-4 text-slate-500 dark:text-white/45">{user.username}</td>
                <td className="py-4 text-slate-500 dark:text-white/45">{user.email}</td>
                <td className="py-4 text-slate-500 dark:text-white/45">{roleSummary(user.roles)}</td>
                <td className="py-4 text-slate-500 dark:text-white/45">{user.enabled ? 'Enabled' : 'Disabled'}</td>
                <td className="py-4">
                  <button
                    type="button"
                    onClick={() => startEditing(user)}
                    className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold dark:border-white/10"
                  >
                    编辑访问
                  </button>
                </td>
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
