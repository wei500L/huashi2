import React from 'react';
import { PageHeader } from '@/components/common';
import { roleLabel, workspaceLabels } from '@/lib/format';
import { useAuthStore, useUIStore } from '@/store';

const SettingsPage: React.FC = () => {
  const { user } = useAuthStore();
  const { isDarkMode, toggleDarkMode } = useUIStore();

  return (
    <div className="space-y-8">
      <PageHeader title="设置" subtitle="当前页只提供本地主题和会话信息，避免超出既有后端合同。" />

      <section className="rounded-[2.5rem] liquid-glass-panel p-8">
        <div className="grid md:grid-cols-2 gap-6">
          <div className="rounded-[1.8rem] border border-slate-200/70 dark:border-white/10 p-5 bg-white/60 dark:bg-white/5">
            <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">当前账户</div>
            <div className="mt-3 text-2xl font-black text-slate-900 dark:text-white">{user?.displayName || '--'}</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
              {(user?.roles || []).map((role) => roleLabel(role)).join(' / ') || '--'} · {user?.email || '--'}
            </div>
            <div className="mt-2 text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
              Workspace: {workspaceLabels(user?.capabilities).join(' / ') || '--'}
            </div>
          </div>
          <div className="rounded-[1.8rem] border border-slate-200/70 dark:border-white/10 p-5 bg-white/60 dark:bg-white/5">
            <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">主题模式</div>
            <div className="mt-4">
              <button type="button" onClick={toggleDarkMode} className="btn-liquid px-5 py-3 text-white">
                切换到{isDarkMode ? '浅色' : '深色'}模式
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default SettingsPage;
