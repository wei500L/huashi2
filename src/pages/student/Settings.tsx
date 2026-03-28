import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { PageHeader } from '@/components/common';
import { formatDateTime, roleLabel, workspaceLabels } from '@/lib/format';
import { authService } from '@/lib/services';
import { useAuthStore, useUIStore } from '@/store';

const SettingsPage: React.FC = () => {
  const { user, logout } = useAuthStore();
  const { isDarkMode, toggleDarkMode } = useUIStore();

  const sessionQuery = useQuery({
    queryKey: ['auth-session-overview'],
    queryFn: ({ signal }) => authService.getSessionOverview({ signal }),
  });

  return (
    <div className="space-y-8 pb-20">
      <PageHeader title="设置" subtitle="当前页补充账号信息、组织关系和会话概览；资料编辑仍保持只读。" />

      <div className="grid gap-8 xl:grid-cols-3">
        <section className="rounded-[2.4rem] liquid-glass-panel p-8 space-y-4">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">account</div>
          <div className="text-2xl font-black text-slate-900 dark:text-white">{user?.displayName || '--'}</div>
          <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
            <div>用户名：{user?.username || '--'}</div>
            <div>邮箱：{user?.email || '--'}</div>
            <div>角色：{(user?.roles || []).map((role) => roleLabel(role)).join(' / ') || '--'}</div>
            <div>Workspace：{workspaceLabels(user?.capabilities).join(' / ') || '--'}</div>
            <div>最近登录：{formatDateTime(user?.lastLoginAt)}</div>
          </div>
        </section>

        <section className="rounded-[2.4rem] liquid-glass-panel p-8 space-y-4">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">organization</div>
          {user?.studentProfile ? (
            <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
              <div>学号：{user.studentProfile.studentNo}</div>
              <div>年级：{user.studentProfile.gradeName}</div>
              <div>英语水平：{user.studentProfile.englishLevel}</div>
              <div>法语水平：{user.studentProfile.frenchLevel}</div>
              <div>课程阶段：{user.studentProfile.courseStage}</div>
            </div>
          ) : user?.teacherProfile ? (
            <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
              <div>工号：{user.teacherProfile.employeeNo}</div>
              <div>部门：{user.teacherProfile.department}</div>
              <div>职称：{user.teacherProfile.title}</div>
            </div>
          ) : (
            <div className="text-sm text-slate-500 dark:text-white/45">当前账号未关联学生或教师档案。</div>
          )}
        </section>

        <section className="rounded-[2.4rem] liquid-glass-panel p-8 space-y-4">
          <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">session</div>
          {sessionQuery.data ? (
            <div className="space-y-2 text-sm text-slate-500 dark:text-white/45">
              <div>活动会话：{sessionQuery.data.hasActiveSession ? '有' : '无'}</div>
              <div>Access Token 到期：{formatDateTime(sessionQuery.data.accessTokenExpiresAt)}</div>
              <div>Refresh 签发：{formatDateTime(sessionQuery.data.refreshSessionIssuedAt)}</div>
              <div>Refresh 到期：{formatDateTime(sessionQuery.data.refreshSessionExpiresAt)}</div>
              <div>UA 指纹：{sessionQuery.data.userAgentFingerprint || '--'}</div>
              <div>来源 IP：{sessionQuery.data.issuedIpAddress || '--'}</div>
            </div>
          ) : sessionQuery.isLoading ? (
            <div className="text-sm text-slate-500 dark:text-white/45">正在加载会话信息...</div>
          ) : (
            <div className="text-sm text-rose-500">{sessionQuery.error instanceof Error ? sessionQuery.error.message : '会话信息加载失败'}</div>
          )}

          <div className="pt-4 flex flex-wrap gap-3">
            <button type="button" onClick={toggleDarkMode} className="rounded-2xl border border-slate-200 px-5 py-3 text-sm dark:border-white/10">
              切换到{isDarkMode ? '浅色' : '深色'}模式
            </button>
            <button type="button" onClick={() => void logout()} className="btn-liquid px-5 py-3 text-white">
              退出所有会话
            </button>
          </div>
        </section>
      </div>
    </div>
  );
};

export default SettingsPage;
