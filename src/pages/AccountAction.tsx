import React from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { authService } from '@/lib/services';

const AccountActionPage: React.FC = () => {
  const navigate = useNavigate();
  const { token } = useParams();
  const [password, setPassword] = React.useState('');
  const [confirmPassword, setConfirmPassword] = React.useState('');
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);

  const previewQuery = useQuery({
    queryKey: ['account-action-preview', token],
    queryFn: ({ signal }) => authService.previewAccountAction(token as string, { signal }),
    enabled: !!token,
  });

  const completeMutation = useMutation({
    mutationFn: () => authService.completeAccountAction(token as string, { password }),
    onSuccess: () => {
      setFeedback('操作已完成，请使用新密码登录。');
      setErrorMessage(null);
      window.setTimeout(() => navigate('/login', { replace: true }), 1200);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '账户操作失败'));
    },
  });

  const actionLabel = previewQuery.data?.purpose === 'PASSWORD_RESET' ? '重设密码' : '激活账户';

  return (
    <div className="page-stack min-h-screen px-4 py-8 sm:px-6 sm:py-12">
      <PageHeader title={actionLabel || '账户操作'} subtitle="通过一次性链接完成账户激活或密码重置。" />

      <section className="mx-auto w-full max-w-3xl min-w-0 rounded-2xl liquid-glass-panel p-4 sm:rounded-3xl sm:p-6 md:p-8">
        {previewQuery.isLoading && (
          <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-6 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
            正在校验链接...
          </div>
        )}

        {previewQuery.data && (
          <div className="space-y-6">
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="text-2xl font-black text-slate-900 dark:text-white">{previewQuery.data.displayName}</div>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                {previewQuery.data.username} · {previewQuery.data.email}
              </div>
              <div className="mt-3 text-xs text-slate-400 dark:text-white/30">
                链接有效期至 {formatDateTime(previewQuery.data.expiresAt)}
              </div>
            </div>

            <div className="grid gap-4">
              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">新密码</div>
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  placeholder="至少 8 位"
                />
              </label>
              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">确认密码</div>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  placeholder="再次输入密码"
                />
              </label>
            </div>

            {feedback && (
              <div className="rounded-[1.8rem] border border-emerald-500/20 bg-emerald-500/5 px-5 py-4 text-sm text-emerald-600 dark:text-emerald-400">
                {feedback}
              </div>
            )}
            {errorMessage && (
              <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
                {errorMessage}
              </div>
            )}

            <div className="page-actions">
              <button
                type="button"
                onClick={() => {
                  if (password !== confirmPassword) {
                    setFeedback(null);
                    setErrorMessage('两次输入的密码不一致。');
                    return;
                  }
                  void completeMutation.mutateAsync();
                }}
                disabled={completeMutation.isPending}
                className="btn-liquid inline-flex items-center justify-center px-6 py-3 text-white disabled:opacity-60"
              >
                {completeMutation.isPending ? '提交中...' : actionLabel}
              </button>
              <Link to="/login" className="inline-flex items-center justify-center rounded-2xl border border-slate-200 px-6 py-3 text-sm dark:border-white/10">
                返回登录
              </Link>
            </div>
          </div>
        )}

        {previewQuery.error && (
          <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-6 text-sm text-rose-500">
            {getApiErrorMessage(previewQuery.error, '链接无效或已过期')}
          </div>
        )}
      </section>
    </div>
  );
};

export default AccountActionPage;
