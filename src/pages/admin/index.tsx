import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckSquare2, Copy, Database, FileDown, KeyRound, LayoutDashboard, MailPlus, Plus, Shield, Upload, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import {
  buildAdminUserBatchTemplate,
  buildBatchCreateRequestFromCsv,
  buildBulkAccessUpdateRequest,
} from '@/lib/admin-user-batch';
import {
  formatDateTime,
  invitationStatusLabel,
  profileLinkStatusLabel,
  roleLabel,
  sessionActivityLabel,
} from '@/lib/format';
import { adminService, lexicalPairService } from '@/lib/services';
import type {
  AccountActionLinkVO,
  AdminUserBatchCreateItemRequest,
  AdminUserBatchResultVO,
  AdminUserAccessUpdateRequest,
  AdminUserCreateRequest,
  Role,
  UserSummaryVO,
} from '@/lib/contracts';
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
  credentialMode: 'INVITE_LINK',
  enabled: true,
  roles: ['STUDENT'],
};

const emptyAccessForm: AdminUserAccessUpdateRequest = {
  enabled: true,
  roles: ['STUDENT'],
};

function toggleRole(roles: Role[], role: Role): Role[] {
  return roles.includes(role) ? roles.filter((item) => item !== role) : [...roles, role];
}

function downloadTextFile(filename: string, content: string) {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function copyActionLink(link: AccountActionLinkVO | null) {
  if (!link) {
    return Promise.reject(new Error('当前没有可复制的链接'));
  }
  return navigator.clipboard.writeText(`${window.location.origin}${link.linkUrl}`);
}

const AdminUsersPage: React.FC = () => {
  const queryClient = useQueryClient();
  const currentUserId = useAuthStore((state) => state.user?.id ?? null);
  const [filters, setFilters] = React.useState({
    keyword: '',
    role: '',
    enabled: 'ALL',
    invitationStatus: '',
    profileLinkStatus: '',
    pageNo: 1,
    pageSize: 10,
  });
  const [showCreateForm, setShowCreateForm] = React.useState(false);
  const [createForm, setCreateForm] = React.useState<AdminUserCreateRequest>(emptyCreateForm);
  const [editingUser, setEditingUser] = React.useState<UserSummaryVO | null>(null);
  const [accessForm, setAccessForm] = React.useState<AdminUserAccessUpdateRequest>(emptyAccessForm);
  const [bulkAccessForm, setBulkAccessForm] = React.useState<AdminUserAccessUpdateRequest>(emptyAccessForm);
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [latestActionLink, setLatestActionLink] = React.useState<AccountActionLinkVO | null>(null);
  const [selectedUserIds, setSelectedUserIds] = React.useState<number[]>([]);
  const [csvFilename, setCsvFilename] = React.useState<string | null>(null);
  const [csvPreview, setCsvPreview] = React.useState<AdminUserBatchCreateItemRequest[]>([]);
  const [batchResult, setBatchResult] = React.useState<AdminUserBatchResultVO | null>(null);

  const usersQuery = useQuery({
    queryKey: ['admin-users', filters],
    queryFn: ({ signal }) =>
      adminService.listUsers(
        {
          pageNo: filters.pageNo,
          pageSize: filters.pageSize,
          keyword: filters.keyword.trim() || undefined,
          role: filters.role || undefined,
          enabled: filters.enabled === 'ALL' ? undefined : filters.enabled === 'ENABLED',
          invitationStatus: filters.invitationStatus || undefined,
          profileLinkStatus: filters.profileLinkStatus || undefined,
        },
        { signal }
      ),
  });

  const overviewQuery = useQuery({
    queryKey: ['lexical-pair-overview'],
    queryFn: ({ signal }) => lexicalPairService.getOverview({ signal }),
  });

  const createMutation = useMutation({
    mutationFn: (payload: AdminUserCreateRequest) => adminService.createUser(payload),
    onSuccess: async (result) => {
      setFeedback(`已创建用户 ${result.user.username}。`);
      setErrorMessage(null);
      setLatestActionLink(result.accountAction || null);
      setBatchResult(null);
      setCreateForm(emptyCreateForm);
      setShowCreateForm(false);
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '创建用户失败');
    },
  });

  const updateAccessMutation = useMutation({
    mutationFn: ({ userId, payload }: { userId: number; payload: AdminUserAccessUpdateRequest }) =>
      adminService.updateUserAccess(userId, payload),
    onSuccess: async (user) => {
      setEditingUser(user);
      setAccessForm({ enabled: user.enabled, roles: user.roles });
      setFeedback(`已更新 ${user.username} 的访问权限。`);
      setErrorMessage(null);
      setBatchResult(null);
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '更新访问权限失败');
    },
  });

  const batchMutation = useMutation({
    mutationFn: adminService.batchUsers,
    onSuccess: async (result) => {
      setBatchResult(result);
      setLatestActionLink(result.createdUsers[0]?.accountAction || null);
      setErrorMessage(null);
      if (result.operation === 'IMPORT_CREATE') {
        setFeedback(`已批量创建 ${result.successCount} 个账号。`);
        setCsvFilename(null);
        setCsvPreview([]);
      } else {
        setFeedback(`已批量更新 ${result.successCount} 个账号的角色与状态。`);
        setSelectedUserIds([]);
      }
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '批量操作失败');
    },
  });

  const inviteMutation = useMutation({
    mutationFn: (userId: number) => adminService.createInviteLink(userId),
    onSuccess: (link) => {
      setLatestActionLink(link);
      setFeedback('已生成新的邀请链接。');
      setErrorMessage(null);
      setBatchResult(null);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '生成邀请链接失败');
    },
  });

  const resetMutation = useMutation({
    mutationFn: (userId: number) => adminService.createPasswordResetLink(userId),
    onSuccess: (link) => {
      setLatestActionLink(link);
      setFeedback('已生成密码重置链接。');
      setErrorMessage(null);
      setBatchResult(null);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '生成密码重置链接失败');
    },
  });

  const startEditing = (user: UserSummaryVO) => {
    setEditingUser(user);
    setAccessForm({ enabled: user.enabled, roles: user.roles });
    setFeedback(null);
    setErrorMessage(null);
  };

  const currentPageUsers = React.useMemo(() => usersQuery.data?.records || [], [usersQuery.data?.records]);
  const currentPageUserIds = React.useMemo(() => currentPageUsers.map((user) => user.id), [currentPageUsers]);
  const currentPageUserIdSet = React.useMemo(() => new Set(currentPageUserIds), [currentPageUserIds]);
  const allUsersOnPageSelected = currentPageUserIds.length > 0 && currentPageUserIds.every((userId) => selectedUserIds.includes(userId));

  React.useEffect(() => {
    setSelectedUserIds((previous) => previous.filter((userId) => currentPageUserIdSet.has(userId)));
  }, [currentPageUserIdSet]);

  const totalUsers = usersQuery.data?.total || 0;
  const totalPages = Math.max(1, Math.ceil(totalUsers / filters.pageSize));

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="用户管理"
        subtitle="支持搜索、分页、邀请链接、密码重置、CSV 批量创建和批量角色更新。默认创建流程走一次性邀请链接，不依赖邮件系统。"
        actions={
          <div className="flex flex-wrap gap-3">
            <Link
              to="/admin/dashboard"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <LayoutDashboard size={14} />
              仪表盘
            </Link>
            <button
              type="button"
              onClick={() => setShowCreateForm((value) => !value)}
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

      {(feedback || errorMessage) && (
        <div className={`rounded-[1.8rem] border px-5 py-4 text-sm ${
          errorMessage
            ? 'border-rose-500/20 bg-rose-500/5 text-rose-500'
            : 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
        }`}>
          {errorMessage || feedback}
        </div>
      )}

      {latestActionLink && (
        <div className="rounded-[1.8rem] border border-sky-500/20 bg-sky-500/5 p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="text-sm font-black text-sky-800 dark:text-sky-200">最新生成链接</div>
              <div className="mt-2 break-all text-sm text-sky-700 dark:text-sky-300">{window.location.origin}{latestActionLink.linkUrl}</div>
              <div className="mt-2 text-xs text-sky-600 dark:text-sky-400">有效期至 {formatDateTime(latestActionLink.expiresAt)}</div>
            </div>
            <button
              type="button"
              onClick={() => {
                void copyActionLink(latestActionLink)
                  .then(() => {
                    setFeedback('链接已复制到剪贴板。');
                    setErrorMessage(null);
                  })
                  .catch((error) => {
                    setFeedback(null);
                    setErrorMessage(error instanceof Error ? error.message : '复制失败');
                  });
              }}
              className="rounded-2xl border border-sky-500/20 px-4 py-3 text-sm text-sky-700 dark:text-sky-300"
            >
              <Copy size={14} className="mr-2 inline-block" />
              复制链接
            </button>
          </div>
        </div>
      )}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 text-left dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">系统用户数</div>
          <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">{usersQuery.data?.total ?? '--'}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">当前筛选后的总数</div>
        </div>
        <div className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 text-left dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">活动会话用户</div>
          <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
            {(usersQuery.data?.records || []).filter((user) => user.hasActiveSession).length}
          </div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">当前页中存在活动登录会话的账号</div>
        </div>
        <Link
          to="/admin/lexical-pairs"
          className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 text-left dark:border-white/10 dark:bg-white/[0.03]"
        >
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">语料总量</div>
          <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">{overviewQuery.data?.totalCount ?? '--'}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">待嵌入 {overviewQuery.data?.pendingEmbeddingCount ?? '--'} 条</div>
        </Link>
        <div className="rounded-[2rem] border border-amber-500/20 bg-amber-500/5 px-5 py-5 text-left">
          <div className="text-[11px] uppercase tracking-[0.28em] text-amber-600/70 dark:text-amber-400/70">待激活邀请</div>
          <div className="mt-2 text-3xl font-black text-amber-600 dark:text-amber-400">
            {(usersQuery.data?.records || []).filter((user) => user.invitationStatus === 'PENDING').length}
          </div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">当前页中待激活账号数</div>
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.3fr_1fr]">
        <div className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">CSV 批量导入</div>
              <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">上传 CSV 批量创建账号</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                使用 `username,email,displayName,roles,enabled,credentialMode,initialPassword` 表头。`roles` 推荐用 `|` 分隔多个角色。
              </div>
            </div>
            <button
              type="button"
              onClick={() => downloadTextFile('admin-user-batch-template.csv', buildAdminUserBatchTemplate())}
              className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <FileDown size={14} className="mr-2 inline-block" />
              下载模板
            </button>
          </div>

          <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/55 p-5 dark:border-white/15 dark:bg-white/[0.02]">
            <label className="flex cursor-pointer flex-wrap items-center justify-between gap-4">
              <div>
                <div className="text-sm font-black text-slate-900 dark:text-white">{csvFilename || '选择 CSV 文件'}</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">前端先校验并预览，再调用单个批量 API。</div>
              </div>
              <span className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
                <Upload size={14} className="mr-2 inline-block" />
                上传 CSV
              </span>
              <input
                type="file"
                accept=".csv,text/csv"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  const input = event.currentTarget;
                  input.value = '';
                  if (!file) {
                    return;
                  }
                  void file.text()
                    .then((text) => {
                      const request = buildBatchCreateRequestFromCsv(text);
                      setCsvFilename(file.name);
                      setCsvPreview(request.createItems || []);
                      setBatchResult(null);
                      setFeedback(`已解析 ${request.createItems?.length || 0} 行待导入账号。`);
                      setErrorMessage(null);
                    })
                    .catch((error) => {
                      setCsvFilename(null);
                      setCsvPreview([]);
                      setFeedback(null);
                      setErrorMessage(error instanceof Error ? error.message : 'CSV 解析失败');
                    });
                }}
              />
            </label>
          </div>

          {csvPreview.length > 0 && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-4 dark:border-white/10 dark:bg-white/[0.03]">
                <div className="text-sm text-slate-500 dark:text-white/45">
                  已解析 <span className="font-black text-slate-900 dark:text-white">{csvPreview.length}</span> 行，预览前 3 行如下。
                </div>
                <button
                  type="button"
                  onClick={() => batchMutation.mutate({ operation: 'IMPORT_CREATE', createItems: csvPreview })}
                  disabled={batchMutation.isPending}
                  className="btn-liquid px-6 py-3 text-white disabled:opacity-60"
                >
                  {batchMutation.isPending ? '导入中...' : '执行批量创建'}
                </button>
              </div>

              <div className="grid gap-3">
                {csvPreview.slice(0, 3).map((item) => (
                  <div key={`${item.rowNumber}-${item.username}`} className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/[0.03]">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <div className="text-sm font-black text-slate-900 dark:text-white">{item.displayName}</div>
                        <div className="mt-1 text-sm text-slate-500 dark:text-white/45">{item.username} · {item.email}</div>
                      </div>
                      <div className="flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                        <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">第 {item.rowNumber} 行</span>
                        <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{item.roles.map((role) => roleLabel(role)).join(' / ')}</span>
                        <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{item.enabled ? '启用' : '禁用'}</span>
                        <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{item.credentialMode === 'MANUAL_PASSWORD' ? '手动密码' : '邀请链接'}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-5">
          <div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">批量角色更新</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">对当前勾选账号统一改权</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">会覆盖所选账号的角色集合，并同步更新启用状态。</div>
          </div>

          <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-4 dark:border-white/10 dark:bg-white/[0.03]">
            <div className="flex items-center gap-3">
              <Users size={16} className="text-slate-400 dark:text-white/30" />
              <div className="text-sm text-slate-500 dark:text-white/45">
                已选择 <span className="font-black text-slate-900 dark:text-white">{selectedUserIds.length}</span> 个账号
              </div>
            </div>
          </div>

          <div className="flex flex-wrap gap-3">
            {roleOptions.map((role) => (
              <label key={role.value} className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10">
                <input
                  type="checkbox"
                  checked={bulkAccessForm.roles.includes(role.value)}
                  onChange={() => setBulkAccessForm((state) => ({ ...state, roles: toggleRole(state.roles, role.value) }))}
                />
                {role.label}
              </label>
            ))}
            <label className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10">
              <input
                type="checkbox"
                checked={bulkAccessForm.enabled}
                onChange={(event) => setBulkAccessForm((state) => ({ ...state, enabled: event.target.checked }))}
              />
              账号启用
            </label>
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => batchMutation.mutate(buildBulkAccessUpdateRequest(selectedUserIds, bulkAccessForm.roles, bulkAccessForm.enabled))}
              disabled={batchMutation.isPending || selectedUserIds.length === 0}
              className="btn-liquid px-6 py-3 text-white disabled:opacity-60"
            >
              {batchMutation.isPending ? '更新中...' : '应用到所选账号'}
            </button>
            <button
              type="button"
              onClick={() => setSelectedUserIds([])}
              disabled={selectedUserIds.length === 0}
              className="rounded-2xl border border-slate-200 px-6 py-3 text-sm disabled:opacity-40 dark:border-white/10"
            >
              清空选择
            </button>
          </div>
        </div>
      </section>

      {showCreateForm && (
        <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-5">
          <div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">创建用户</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">创建基础账号</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">默认使用邀请链接交付账号；若选择手动密码模式，再录入初始密码。</div>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <input value={createForm.username} onChange={(event) => setCreateForm((state) => ({ ...state, username: event.target.value }))} placeholder="用户名" className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5" />
            <input value={createForm.email} onChange={(event) => setCreateForm((state) => ({ ...state, email: event.target.value }))} placeholder="邮箱" className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5" />
            <input value={createForm.displayName} onChange={(event) => setCreateForm((state) => ({ ...state, displayName: event.target.value }))} placeholder="显示名称" className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5" />
            <select
              value={createForm.credentialMode}
              onChange={(event) => setCreateForm((state) => ({ ...state, credentialMode: event.target.value as AdminUserCreateRequest['credentialMode'] }))}
              className="native-select rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
            >
              <option value="INVITE_LINK">邀请链接</option>
              <option value="MANUAL_PASSWORD">手动密码</option>
            </select>
            {createForm.credentialMode === 'MANUAL_PASSWORD' && (
              <input
                type="password"
                value={createForm.initialPassword || ''}
                onChange={(event) => setCreateForm((state) => ({ ...state, initialPassword: event.target.value }))}
                placeholder="初始密码"
                className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
              />
            )}
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
                  onChange={() => setCreateForm((state) => ({ ...state, roles: toggleRole(state.roles, role.value) }))}
                />
                {role.label}
              </label>
            ))}
            <label className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10">
              <input type="checkbox" checked={createForm.enabled} onChange={(event) => setCreateForm((state) => ({ ...state, enabled: event.target.checked }))} />
              创建后立即启用
            </label>
          </div>

          <div className="flex flex-wrap gap-3">
            <button type="button" onClick={() => createMutation.mutate(createForm)} disabled={createMutation.isPending} className="btn-liquid px-6 py-3 text-white disabled:opacity-60">
              {createMutation.isPending ? '创建中...' : '创建用户'}
            </button>
            <button type="button" onClick={() => setShowCreateForm(false)} className="rounded-2xl border border-slate-200 px-6 py-3 text-sm dark:border-white/10">
              取消
            </button>
          </div>
        </section>
      )}

      <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-5">
        <div className="grid gap-4 md:grid-cols-5">
          <input
            value={filters.keyword}
            onChange={(event) => setFilters((state) => ({ ...state, keyword: event.target.value, pageNo: 1 }))}
            placeholder="搜索用户名 / 邮箱 / 显示名"
            className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 md:col-span-2 dark:border-white/10 dark:bg-white/5"
          />
          <select value={filters.role} onChange={(event) => setFilters((state) => ({ ...state, role: event.target.value, pageNo: 1 }))} className="native-select rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">
            <option value="">全部角色</option>
            {roleOptions.map((role) => <option key={role.value} value={role.value}>{role.label}</option>)}
          </select>
          <select value={filters.enabled} onChange={(event) => setFilters((state) => ({ ...state, enabled: event.target.value, pageNo: 1 }))} className="native-select rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">
            <option value="ALL">全部状态</option>
            <option value="ENABLED">仅启用</option>
            <option value="DISABLED">仅禁用</option>
          </select>
          <select value={filters.invitationStatus} onChange={(event) => setFilters((state) => ({ ...state, invitationStatus: event.target.value, pageNo: 1 }))} className="native-select rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">
            <option value="">全部邀请状态</option>
            <option value="PENDING">{invitationStatusLabel('PENDING')}</option>
            <option value="CONSUMED">{invitationStatusLabel('CONSUMED')}</option>
            <option value="EXPIRED">{invitationStatusLabel('EXPIRED')}</option>
            <option value="NONE">{invitationStatusLabel('NONE')}</option>
          </select>
        </div>

        <div className="grid gap-4 md:grid-cols-[1fr_auto_auto]">
          <div className="flex flex-wrap gap-3">
            <select value={filters.profileLinkStatus} onChange={(event) => setFilters((state) => ({ ...state, profileLinkStatus: event.target.value, pageNo: 1 }))} className="native-select min-w-[220px] rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5">
              <option value="">全部资料关联状态</option>
              <option value="UNLINKED">{profileLinkStatusLabel('UNLINKED')}</option>
              <option value="STUDENT_ONLY">{profileLinkStatusLabel('STUDENT_ONLY')}</option>
              <option value="TEACHER_ONLY">{profileLinkStatusLabel('TEACHER_ONLY')}</option>
              <option value="BOTH">{profileLinkStatusLabel('BOTH')}</option>
            </select>
            <button
              type="button"
              onClick={() => setSelectedUserIds(allUsersOnPageSelected ? [] : currentPageUserIds)}
              disabled={currentPageUserIds.length === 0}
              className="rounded-2xl border border-slate-200 px-4 py-3 text-sm disabled:opacity-40 dark:border-white/10"
            >
              <CheckSquare2 size={14} className="mr-2 inline-block" />
              {allUsersOnPageSelected ? '取消全选本页' : '全选本页'}
            </button>
          </div>
          <button type="button" onClick={() => setFilters((state) => ({ ...state, pageNo: Math.max(1, state.pageNo - 1) }))} disabled={filters.pageNo <= 1} className="rounded-2xl border border-slate-200 px-4 py-3 text-sm disabled:opacity-40 dark:border-white/10">
            上一页
          </button>
          <button type="button" onClick={() => setFilters((state) => ({ ...state, pageNo: Math.min(totalPages, state.pageNo + 1) }))} disabled={filters.pageNo >= totalPages} className="rounded-2xl border border-slate-200 px-4 py-3 text-sm disabled:opacity-40 dark:border-white/10">
            下一页
          </button>
        </div>

        <div className="space-y-4">
          {(usersQuery.data?.records || []).map((user) => (
            <div key={user.id} className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="flex items-start gap-4">
                  <label className="mt-1 inline-flex items-center gap-2 rounded-full border border-slate-200 px-3 py-2 text-xs dark:border-white/10">
                    <input
                      type="checkbox"
                      checked={selectedUserIds.includes(user.id)}
                      onChange={(event) => {
                        setSelectedUserIds((state) =>
                          event.target.checked ? Array.from(new Set([...state, user.id])) : state.filter((item) => item !== user.id)
                        );
                      }}
                    />
                    勾选
                  </label>
                  <div>
                    <div className="flex flex-wrap items-center gap-3">
                      <div className="text-lg font-black text-slate-900 dark:text-white">{user.displayName}</div>
                      {user.id === currentUserId && (
                        <span className="rounded-full border border-amber-500/20 bg-amber-500/10 px-3 py-1 text-xs text-amber-600 dark:text-amber-400">
                          当前账号
                        </span>
                      )}
                      <span className={`rounded-full border px-3 py-1 text-xs ${user.enabled ? 'border-emerald-500/20 text-emerald-600 dark:text-emerald-400' : 'border-rose-500/20 text-rose-500'}`}>
                        {user.enabled ? '启用中' : '已禁用'}
                      </span>
                    </div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{user.username} · {user.email}</div>
                    <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{user.roles.map((role) => roleLabel(role)).join(' / ')}</span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">资料：{profileLinkStatusLabel(user.profileLinkStatus)}</span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">邀请：{invitationStatusLabel(user.invitationStatus)}</span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">会话：{sessionActivityLabel(user.hasActiveSession)}</span>
                    </div>
                    <div className="mt-3 text-xs text-slate-400 dark:text-white/30">最近登录 {formatDateTime(user.lastLoginAt)}</div>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2">
                  <button type="button" onClick={() => startEditing(user)} className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
                    编辑权限
                  </button>
                  <button type="button" onClick={() => inviteMutation.mutate(user.id)} className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
                    <MailPlus size={14} className="mr-2 inline-block" />
                    邀请链接
                  </button>
                  <button type="button" onClick={() => resetMutation.mutate(user.id)} className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
                    <KeyRound size={14} className="mr-2 inline-block" />
                    重置密码
                  </button>
                </div>
              </div>
            </div>
          ))}

          {!usersQuery.isLoading && !(usersQuery.data?.records || []).length && (
            <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/55 px-5 py-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
              当前筛选条件下没有账号。
            </div>
          )}
        </div>
      </section>

      {batchResult && (
        <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-5">
          <div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">批量结果</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">
              {batchResult.operation === 'IMPORT_CREATE' ? '批量创建完成' : '批量角色更新完成'}
            </div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
              共处理 {batchResult.totalCount} 个账号，成功 {batchResult.successCount} 个。
            </div>
          </div>

          {batchResult.operation === 'IMPORT_CREATE' ? (
            <div className="grid gap-3">
              {batchResult.createdUsers.map((item) => (
                <div key={item.user.id} className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/[0.03]">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <div className="text-sm font-black text-slate-900 dark:text-white">{item.user.displayName}</div>
                      <div className="mt-1 text-sm text-slate-500 dark:text-white/45">{item.user.username} · {item.user.email}</div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 text-xs dark:border-white/10">{item.user.roles.map((role) => roleLabel(role)).join(' / ')}</span>
                      {item.accountAction && (
                        <button
                          type="button"
                          onClick={() => {
                            void copyActionLink(item.accountAction || null)
                              .then(() => {
                                setFeedback(`已复制 ${item.user.username} 的邀请链接。`);
                                setErrorMessage(null);
                              })
                              .catch((error) => {
                                setFeedback(null);
                                setErrorMessage(error instanceof Error ? error.message : '复制失败');
                              });
                          }}
                          className="rounded-2xl border border-slate-200 px-4 py-2 text-sm dark:border-white/10"
                        >
                          <Copy size={14} className="mr-2 inline-block" />
                          复制邀请链接
                        </button>
                      )}
                    </div>
                  </div>
                  {item.accountAction && (
                    <div className="mt-3 break-all text-xs text-slate-500 dark:text-white/45">
                      {window.location.origin}{item.accountAction.linkUrl}
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="grid gap-3">
              {batchResult.updatedUsers.map((user) => (
                <div key={user.id} className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/[0.03]">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <div className="text-sm font-black text-slate-900 dark:text-white">{user.displayName}</div>
                      <div className="mt-1 text-sm text-slate-500 dark:text-white/45">{user.username} · {user.email}</div>
                    </div>
                    <div className="flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{user.roles.map((role) => roleLabel(role)).join(' / ')}</span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">{user.enabled ? '启用中' : '已禁用'}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {editingUser && (
        <section className="rounded-[2.5rem] liquid-glass-panel p-8 space-y-5">
          <div>
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">访问权限</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{editingUser.displayName}</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{editingUser.username} · {editingUser.email}</div>
          </div>

          <div className="flex flex-wrap gap-3">
            {roleOptions.map((role) => (
              <label key={role.value} className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10">
                <input
                  type="checkbox"
                  checked={accessForm.roles.includes(role.value)}
                  onChange={() => setAccessForm((state) => ({ ...state, roles: toggleRole(state.roles, role.value) }))}
                />
                {role.label}
              </label>
            ))}
            <label className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10">
              <input type="checkbox" checked={accessForm.enabled} onChange={(event) => setAccessForm((state) => ({ ...state, enabled: event.target.checked }))} />
              账号启用
            </label>
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => updateAccessMutation.mutate({ userId: editingUser.id, payload: accessForm })}
              disabled={updateAccessMutation.isPending}
              className="btn-liquid px-6 py-3 text-white disabled:opacity-60"
            >
              {updateAccessMutation.isPending ? '保存中...' : '保存权限'}
            </button>
            <button type="button" onClick={() => setEditingUser(null)} className="rounded-2xl border border-slate-200 px-6 py-3 text-sm dark:border-white/10">
              关闭
            </button>
          </div>
        </section>
      )}
    </div>
  );
};

export default AdminUsersPage;
