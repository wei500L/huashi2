import React from 'react';
import { ChevronRight } from 'lucide-react';
import { getApiErrorMessage } from '@/lib/api';
import { buildProgressPercent } from './helpers';

type SessionSaveActionsProps = {
  isBusy?: boolean;
  onSave: () => void;
  onSaveAndExit: () => void;
};

export const SessionSaveActions: React.FC<SessionSaveActionsProps> = ({
  isBusy,
  onSave,
  onSaveAndExit,
}) => (
  <div className="flex flex-wrap items-center gap-3">
    <button
      type="button"
      onClick={onSave}
      disabled={isBusy}
      className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold disabled:opacity-60 dark:border-white/10"
    >
      保存进度
    </button>
    <button
      type="button"
      onClick={onSaveAndExit}
      disabled={isBusy}
      className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
    >
      保存并退出
    </button>
  </div>
);

type SessionFeedbackBannersProps = {
  saveMessage?: string | null;
  saveErrorMessage?: string | null;
  submitErrorMessage?: string | null;
  submitInfoMessage?: string | null;
  loadInfoMessage?: string | null;
  loadError?: unknown;
  onRetryLoad?: () => void;
};

export const SessionFeedbackBanners: React.FC<SessionFeedbackBannersProps> = ({
  saveMessage,
  saveErrorMessage,
  submitErrorMessage,
  submitInfoMessage,
  loadInfoMessage,
  loadError,
  onRetryLoad,
}) => (
  <>
    {saveMessage && (
      <div className="rounded-[1.6rem] border border-emerald-500/20 bg-emerald-500/5 px-5 py-4 text-sm text-emerald-600 dark:text-emerald-400">
        {saveMessage}
      </div>
    )}
    {saveErrorMessage && (
      <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
        {saveErrorMessage}
      </div>
    )}
    {submitInfoMessage && (
      <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/5 px-5 py-4 text-sm text-amber-600 dark:text-amber-400">
        {submitInfoMessage}
      </div>
    )}
    {loadInfoMessage && (
      <div className="rounded-[1.6rem] border border-sky-500/20 bg-sky-500/5 px-5 py-4 text-sm text-sky-700 dark:text-sky-300">
        <div>{loadInfoMessage}</div>
        <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-sky-500/10">
          <div className="h-full w-1/3 animate-pulse rounded-full bg-sky-500/70" />
        </div>
      </div>
    )}
    {submitErrorMessage && (
      <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
        {submitErrorMessage}
      </div>
    )}
    {loadError && (
      <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
        <div>{getApiErrorMessage(loadError)}</div>
        {onRetryLoad && (
          <button
            type="button"
            onClick={onRetryLoad}
            className="mt-4 rounded-full border border-rose-500/20 px-4 py-2 text-sm font-bold"
          >
            重试加载当前题
          </button>
        )}
      </div>
    )}
  </>
);

type SessionProgressHeaderProps = {
  icon?: React.ReactNode;
  label: string;
  answeredItems?: number | null;
  totalItems?: number | null;
  gradientClassName: string;
};

export const SessionProgressHeader: React.FC<SessionProgressHeaderProps> = ({
  icon,
  label,
  answeredItems,
  totalItems,
  gradientClassName,
}) => (
  <div className="flex items-center justify-between">
    <div className="flex items-center gap-3 text-sm text-slate-500 dark:text-white/45">
      {icon}
      <span>{label}</span>
    </div>
    <div className="h-2 w-56 overflow-hidden rounded-full bg-slate-200 dark:bg-white/10">
      <div
        className={`h-full ${gradientClassName}`}
        style={{ width: `${buildProgressPercent(answeredItems, totalItems)}%` }}
      />
    </div>
  </div>
);

type SessionOptionButtonProps = {
  label: string;
  icon?: React.ReactNode;
  disabled?: boolean;
  onClick: () => void;
};

export const SessionOptionButton: React.FC<SessionOptionButtonProps> = ({
  label,
  icon,
  disabled,
  onClick,
}) => (
  <button
    type="button"
    disabled={disabled}
    onClick={onClick}
    className="w-full rounded-[1.8rem] border border-slate-200 bg-white/70 px-5 py-4 text-left transition-all hover:border-primary/50 disabled:opacity-60 dark:border-white/10 dark:bg-white/5"
  >
    <div className="flex items-center justify-between gap-4">
      <span className="font-bold text-slate-900 dark:text-white">{label}</span>
      {icon || <ChevronRight className="text-primary" size={16} />}
    </div>
  </button>
);
