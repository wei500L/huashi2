import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { History, RefreshCw, Shield, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { adminService } from '@/lib/services';
import type { AdminAuditLogItemVO } from '@/lib/contracts';

type AuditLogFilters = {
  pageNo: number;
  pageSize: number;
  startAt: string;
  endAt: string;
  actionType: string;
  userKeyword: string;
};

const defaultFilters: AuditLogFilters = {
  pageNo: 1,
  pageSize: 20,
  startAt: '',
  endAt: '',
  actionType: '',
  userKeyword: '',
};

function resolveActorLabel(log: AdminAuditLogItemVO): string {
  if (log.actorDisplayName && log.actorUsername && log.actorDisplayName !== log.actorUsername) {
    return `${log.actorDisplayName} (${log.actorUsername})`;
  }
  if (log.actorDisplayName) {
    return log.actorDisplayName;
  }
  if (log.actorUsername) {
    return log.actorUsername;
  }
  if (log.actorUserId != null) {
    return `用户 #${log.actorUserId}`;
  }
  return '系统';
}

function responseTone(responseCode: string): string {
  return responseCode === 'SUCCESS'
    ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
    : 'border-rose-500/20 bg-rose-500/10 text-rose-700 dark:text-rose-300';
}

const AdminAuditLogsPage: React.FC = () => {
  const [filters, setFilters] = React.useState<AuditLogFilters>(defaultFilters);

  const auditLogsQuery = useQuery({
    queryKey: ['admin-audit-logs', filters],
    queryFn: ({ signal }) =>
      adminService.listAuditLogs(
        {
          pageNo: filters.pageNo,
          pageSize: filters.pageSize,
          startAt: filters.startAt || undefined,
          endAt: filters.endAt || undefined,
          actionType: filters.actionType.trim() || undefined,
          userKeyword: filters.userKeyword.trim() || undefined,
        },
        { signal }
      ),
  });

  const records = auditLogsQuery.data?.records || [];
  const total = auditLogsQuery.data?.total || 0;
  const totalPages = Math.max(1, Math.ceil(total / filters.pageSize));
  const actionTypeOptions = React.useMemo(
    () => Array.from(new Set(records.map((item) => item.actionType))).sort((left, right) => left.localeCompare(right)),
    [records]
  );
  const filteredWindowLabel = filters.startAt || filters.endAt
    ? `${filters.startAt || '最早'} 至 ${filters.endAt || '现在'}`
    : '未限定时间';

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="审计日志"
        subtitle="管理员可以按时间、操作类型和用户筛选审计记录，追踪后台行为、接口路径和请求负载。"
        actions={
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => void auditLogsQuery.refetch()}
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <RefreshCw size={14} />
              刷新
            </button>
            <Link
              to="/admin/users"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <Users size={14} />
              用户管理
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

      {auditLogsQuery.isError && (
        <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-600 dark:text-rose-400">
          {getApiErrorMessage(auditLogsQuery.error, '审计日志加载失败')}
        </div>
      )}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">命中记录</div>
          <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">{total || '--'}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">当前筛选条件下的总数</div>
        </div>
        <div className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">当前页记录</div>
          <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">{records.length}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">第 {filters.pageNo} / {totalPages} 页</div>
        </div>
        <div className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">当前页操作类型</div>
          <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">{actionTypeOptions.length}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{actionTypeOptions.slice(0, 2).join(' / ') || '暂无数据'}</div>
        </div>
        <div className="rounded-[2rem] border border-sky-500/20 bg-sky-500/5 px-5 py-5">
          <div className="text-[11px] uppercase tracking-[0.28em] text-sky-700/70 dark:text-sky-300/70">时间窗口</div>
          <div className="mt-2 inline-flex items-center gap-2 text-lg font-black text-sky-800 dark:text-sky-200">
            <History size={18} />
            审计回放
          </div>
          <div className="mt-3 text-sm text-sky-700 dark:text-sky-300">{filteredWindowLabel}</div>
        </div>
      </section>

      <section className="rounded-[2.5rem] liquid-glass-panel space-y-5 p-8">
        <div className="grid gap-4 xl:grid-cols-4">
          <input
            value={filters.userKeyword}
            onChange={(event) => setFilters((state) => ({ ...state, userKeyword: event.target.value, pageNo: 1 }))}
            placeholder="用户 ID / 用户名 / 显示名 / 邮箱"
            className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
          />
          <div>
            <input
              list="audit-action-type-options"
              value={filters.actionType}
              onChange={(event) => setFilters((state) => ({ ...state, actionType: event.target.value, pageNo: 1 }))}
              placeholder="操作类型，例如 template_create"
              className="w-full rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
            />
            <datalist id="audit-action-type-options">
              {actionTypeOptions.map((actionType) => <option key={actionType} value={actionType} />)}
            </datalist>
          </div>
          <input
            type="datetime-local"
            value={filters.startAt}
            onChange={(event) => setFilters((state) => ({ ...state, startAt: event.target.value, pageNo: 1 }))}
            className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
          />
          <input
            type="datetime-local"
            value={filters.endAt}
            onChange={(event) => setFilters((state) => ({ ...state, endAt: event.target.value, pageNo: 1 }))}
            className="rounded-2xl border border-slate-200 bg-white/70 px-4 py-3 dark:border-white/10 dark:bg-white/5"
          />
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <button
            type="button"
            onClick={() => setFilters(defaultFilters)}
            className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
          >
            重置筛选
          </button>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => setFilters((state) => ({ ...state, pageNo: Math.max(1, state.pageNo - 1) }))}
              disabled={filters.pageNo <= 1}
              className="rounded-2xl border border-slate-200 px-4 py-3 text-sm disabled:opacity-40 dark:border-white/10"
            >
              上一页
            </button>
            <button
              type="button"
              onClick={() => setFilters((state) => ({ ...state, pageNo: Math.min(totalPages, state.pageNo + 1) }))}
              disabled={filters.pageNo >= totalPages}
              className="rounded-2xl border border-slate-200 px-4 py-3 text-sm disabled:opacity-40 dark:border-white/10"
            >
              下一页
            </button>
          </div>
        </div>

        <div className="space-y-4">
          {auditLogsQuery.isLoading && (
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
              正在加载审计日志...
            </div>
          )}

          {!auditLogsQuery.isLoading && !records.length && (
            <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/55 px-5 py-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
              当前筛选条件下没有审计记录。
            </div>
          )}

          {records.map((log) => (
            <article key={log.id} className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-3">
                    <div className="text-lg font-black text-slate-900 dark:text-white">{log.actionType}</div>
                    <span className={`rounded-full border px-3 py-1 text-xs font-bold ${responseTone(log.responseCode)}`}>
                      {log.responseCode}
                    </span>
                    <span className="rounded-full border border-slate-200/70 px-3 py-1 text-xs dark:border-white/10">
                      {log.requestMethod}
                    </span>
                  </div>

                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {resolveActorLabel(log)} · {formatDateTime(log.createdAt)}
                  </div>

                  <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                    <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                      目标：{log.targetType}{log.targetId ? ` #${log.targetId}` : ''}
                    </span>
                    <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                      Trace：{log.traceId}
                    </span>
                    {log.actorUserId != null && (
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                        Actor ID：{log.actorUserId}
                      </span>
                    )}
                  </div>

                  <div className="mt-4 rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                    <div className="font-bold text-slate-800 dark:text-white">请求路径</div>
                    <div className="mt-2 break-all">{log.requestPath}</div>
                  </div>

                  {log.requestPayload && (
                    <details className="mt-4 rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-4 dark:border-white/10 dark:bg-white/[0.03]">
                      <summary className="cursor-pointer text-sm font-bold text-slate-800 dark:text-white">查看请求负载</summary>
                      <pre className="mt-3 overflow-x-auto whitespace-pre-wrap break-all text-xs text-slate-600 dark:text-white/60">
                        {log.requestPayload}
                      </pre>
                    </details>
                  )}
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
};

export default AdminAuditLogsPage;
