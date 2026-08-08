import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { History, RefreshCw, Shield, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageHeader, Pagination } from '@/components/common';
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

type AuditLogSort = 'createdAt' | 'actionType' | 'responseCode';

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
  const [sort, setSort] = React.useState<{ key: AuditLogSort; direction: 'asc' | 'desc' }>({ key: 'createdAt', direction: 'desc' });

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

  const records = React.useMemo(() => {
    const source = auditLogsQuery.data?.records || [];
    return [...source].sort((left, right) => {
      const leftValue = left[sort.key] ?? '';
      const rightValue = right[sort.key] ?? '';
      const result = String(leftValue).localeCompare(String(rightValue), undefined, { numeric: true, sensitivity: 'base' });
      return sort.direction === 'asc' ? result : -result;
    });
  }, [auditLogsQuery.data?.records, sort]);
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
    <div className="page-stack pb-16 sm:pb-20">
      <PageHeader
        title="审计日志"
        subtitle="管理员可以按时间、操作类型和用户筛选审计记录，追踪后台行为、接口路径和请求负载。"
        actions={
          <div className="page-actions">
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
        <div role="alert" className="rounded-lg border border-error/30 bg-error/5 px-4 py-3 text-sm text-rose-600 sm:px-5 sm:py-4 dark:text-rose-400">
          {getApiErrorMessage(auditLogsQuery.error, '审计日志加载失败')}
        </div>
      )}

      <section className="stat-grid">
        <div className="min-w-0 rounded-xl border border-border-subtle bg-surface px-4 py-4 shadow-sm sm:px-5 sm:py-5">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">命中记录</div>
          <div className="mt-2 text-2xl font-black text-slate-900 sm:text-3xl dark:text-white">{total || '--'}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">当前筛选条件下的总数</div>
        </div>
        <div className="min-w-0 rounded-xl border border-border-subtle bg-surface px-4 py-4 shadow-sm sm:px-5 sm:py-5">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">当前页记录</div>
          <div className="mt-2 text-2xl font-black text-slate-900 sm:text-3xl dark:text-white">{records.length}</div>
          <div className="mt-3 text-sm text-slate-500 dark:text-white/45">第 {filters.pageNo} / {totalPages} 页</div>
        </div>
        <div className="min-w-0 rounded-xl border border-border-subtle bg-surface px-4 py-4 shadow-sm sm:px-5 sm:py-5">
          <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">当前页操作类型</div>
          <div className="mt-2 text-2xl font-black text-slate-900 sm:text-3xl dark:text-white">{actionTypeOptions.length}</div>
          <div className="mt-3 break-words text-sm text-slate-500 dark:text-white/45">{actionTypeOptions.slice(0, 2).join(' / ') || '暂无数据'}</div>
        </div>
        <div className="min-w-0 rounded-xl border border-info/30 bg-info/5 px-4 py-4 sm:px-5 sm:py-5">
          <div className="text-[11px] uppercase tracking-[0.28em] text-sky-700/70 dark:text-sky-300/70">时间窗口</div>
          <div className="mt-2 inline-flex items-center gap-2 text-base font-black text-sky-800 sm:text-lg dark:text-sky-200">
            <History size={18} className="shrink-0" />
            审计回放
          </div>
          <div className="mt-3 break-words text-sm text-sky-700 dark:text-sky-300">{filteredWindowLabel}</div>
        </div>
      </section>

      <section className="page-panel space-y-5">
        <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4 xl:grid-cols-4">
          <input
            value={filters.userKeyword}
            onChange={(event) => setFilters((state) => ({ ...state, userKeyword: event.target.value, pageNo: 1 }))}
            placeholder="用户 ID / 用户名 / 显示名 / 邮箱"
            className="filter-field min-w-0 rounded-lg border border-border-subtle bg-surface px-4 py-3"
          />
          <div className="min-w-0">
            <input
              list="audit-action-type-options"
              value={filters.actionType}
              onChange={(event) => setFilters((state) => ({ ...state, actionType: event.target.value, pageNo: 1 }))}
              placeholder="操作类型，例如 template_create"
              className="filter-field w-full min-w-0 rounded-lg border border-border-subtle bg-surface px-4 py-3"
            />
            <datalist id="audit-action-type-options">
              {actionTypeOptions.map((actionType) => <option key={actionType} value={actionType} />)}
            </datalist>
          </div>
          <input
            type="datetime-local"
            value={filters.startAt}
            onChange={(event) => setFilters((state) => ({ ...state, startAt: event.target.value, pageNo: 1 }))}
            className="filter-field min-w-0 rounded-lg border border-border-subtle bg-surface px-3 py-3 sm:px-4"
          />
          <input
            type="datetime-local"
            value={filters.endAt}
            onChange={(event) => setFilters((state) => ({ ...state, endAt: event.target.value, pageNo: 1 }))}
            className="filter-field min-w-0 rounded-lg border border-border-subtle bg-surface px-3 py-3 sm:px-4"
          />
        </div>

        <div className="page-toolbar border-t border-border-subtle pt-4">
          <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
            <label className="text-sm text-slate-500 dark:text-white/55" htmlFor="audit-sort">当前页排序</label>
            <select
              id="audit-sort"
              value={`${sort.key}:${sort.direction}`}
              onChange={(event) => {
                const [key, direction] = event.target.value.split(':') as [AuditLogSort, 'asc' | 'desc'];
                setSort({ key, direction });
              }}
              className="native-select filter-field rounded-lg border border-border-subtle bg-surface px-3 py-2 text-sm"
            >
              <option value="createdAt:desc">时间：新到旧</option>
              <option value="createdAt:asc">时间：旧到新</option>
              <option value="actionType:asc">操作类型：A-Z</option>
              <option value="responseCode:asc">响应：成功优先</option>
            </select>
            <label className="text-sm text-slate-500 dark:text-white/55" htmlFor="audit-page-size">每页</label>
            <select
              id="audit-page-size"
              value={filters.pageSize}
              onChange={(event) => setFilters((state) => ({ ...state, pageSize: Number(event.target.value), pageNo: 1 }))}
              className="native-select filter-field rounded-lg border border-border-subtle bg-surface px-3 py-2 text-sm"
            >
              {[20, 50, 100].map((size) => <option key={size} value={size}>{size} 条</option>)}
            </select>
          </div>
          <span className="text-xs text-slate-400 dark:text-white/40">排序仅作用于当前页，服务端筛选与分页仍保持不变。</span>
        </div>

        <div className="page-toolbar">
          <button
            type="button"
            onClick={() => setFilters(defaultFilters)}
            className="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm sm:w-auto dark:border-white/10"
          >
            重置筛选
          </button>
          <Pagination
            page={filters.pageNo}
            pageCount={totalPages}
            total={total}
            pageSize={filters.pageSize}
            itemLabel="条记录"
            onPageChange={(pageNo) => setFilters((state) => ({ ...state, pageNo }))}
            label="审计日志分页"
            previousLabel="上一页"
            nextLabel="下一页"
            disabled={auditLogsQuery.isFetching}
            className="w-full min-w-0 sm:max-w-xs sm:w-auto"
          />
        </div>

        <div className="min-w-0 space-y-4">
          {auditLogsQuery.isLoading && (
            <div className="rounded-lg border border-border-subtle bg-surface px-4 py-8 text-sm text-slate-500 sm:px-5 dark:text-white/45">
              正在加载审计日志...
            </div>
          )}

          {!auditLogsQuery.isLoading && !records.length && (
            <div className="rounded-lg border border-dashed border-border-subtle bg-surface px-4 py-8 text-sm text-slate-500 sm:px-5 dark:text-white/45">
              当前筛选条件下没有审计记录。
            </div>
          )}

          {records.map((log) => (
            <article key={log.id} className="min-w-0 rounded-lg border border-border-subtle bg-surface p-4 shadow-sm sm:p-5">
              <div className="min-w-0">
                <div className="flex min-w-0 flex-wrap items-center gap-2 sm:gap-3">
                  <div className="min-w-0 break-all text-base font-black text-slate-900 sm:text-lg dark:text-white">{log.actionType}</div>
                  <span className={`rounded-full border px-3 py-1 text-xs font-bold ${responseTone(log.responseCode)}`}>
                    {log.responseCode}
                  </span>
                  <span className="rounded-full border border-slate-200/70 px-3 py-1 text-xs dark:border-white/10">
                    {log.requestMethod}
                  </span>
                </div>

                <div className="mt-2 break-words text-sm text-slate-500 dark:text-white/45">
                  {resolveActorLabel(log)} · {formatDateTime(log.createdAt)}
                </div>

                <div className="mt-2 text-xs tabular-nums text-slate-400 dark:text-white/35">日志 ID：{log.id}</div>

                <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                  <span className="max-w-full break-all rounded-full border border-border-subtle px-3 py-1">
                    目标：{log.targetType || '--'}{log.targetId != null ? ` #${log.targetId}` : ''}
                  </span>
                  <span className="max-w-full break-all rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                    Trace：{log.traceId || '--'}
                  </span>
                  {log.actorUserId != null && (
                    <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                      Actor ID：{log.actorUserId}
                    </span>
                  )}
                </div>

                <div className="mt-4 min-w-0 rounded-lg border border-border-subtle bg-surface-raised px-3 py-3 text-sm text-slate-600 sm:px-4 sm:py-4 dark:text-white/60">
                  <div className="font-bold text-slate-800 dark:text-white">请求路径</div>
                  <div className="mt-2 break-all">{log.requestPath}</div>
                </div>

                {log.requestPayload && (
                  <details className="mt-4 min-w-0 rounded-lg border border-border-subtle bg-surface-raised px-3 py-3 sm:px-4 sm:py-4">
                    <summary className="cursor-pointer text-sm font-bold text-slate-800 dark:text-white">查看请求负载</summary>
                    <pre className="mt-3 max-w-full overflow-x-auto whitespace-pre-wrap break-all text-xs text-slate-600 dark:text-white/60">
                      {log.requestPayload}
                    </pre>
                  </details>
                )}
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
};

export default AdminAuditLogsPage;
