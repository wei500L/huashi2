import React from 'react';
import { ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FeedbackState } from '@/components/common/FeedbackState';
import { getProductizedErrorState } from '@/lib/async-state';
import { buildProgressPercent } from './helpers';

type SessionSaveActionsProps = {
  isSaving?: boolean;
  disabled?: boolean;
  onSave: () => void;
  onSaveAndExit: () => void;
};

export const SessionSaveActions: React.FC<SessionSaveActionsProps> = ({
  isSaving,
  disabled,
  onSave,
  onSaveAndExit,
}) => {
  const { t } = useTranslation();

  return (
  <div className="flex flex-wrap items-center gap-3">
    <button
      type="button"
      onClick={onSave}
      disabled={disabled || isSaving}
      className="min-h-11 rounded-full border border-slate-200 px-5 py-3 text-sm font-bold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 disabled:opacity-60 dark:border-white/10"
    >
      {isSaving ? t('ui.sessionState.savingAction') : t('ui.sessionState.saveAction')}
    </button>
    <button
      type="button"
      onClick={onSaveAndExit}
      disabled={disabled || isSaving}
      className="btn-liquid min-h-11 px-5 py-3 text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 disabled:opacity-60"
    >
      {isSaving ? t('ui.sessionState.savingAction') : t('ui.sessionState.saveAndExitAction')}
    </button>
    {isSaving && (
      <span role="status" aria-live="polite" className="text-sm font-bold text-sky-700 dark:text-sky-300">
        {t('ui.sessionState.savingInline')}
      </span>
    )}
  </div>
  );
};

type SessionFeedbackBannersProps = {
  isSaving?: boolean;
  saveMessage?: string | null;
  saveErrorMessage?: string | null;
  saveConflictMessage?: string | null;
  submitErrorMessage?: string | null;
  submitInfoMessage?: string | null;
  loadInfoMessage?: string | null;
  loadError?: unknown;
  onRetrySave?: () => void;
  onRetrySubmit?: () => void;
  onRetryLoad?: () => void;
};

export const SessionFeedbackBanners: React.FC<SessionFeedbackBannersProps> = ({
  isSaving,
  saveMessage,
  saveErrorMessage,
  saveConflictMessage,
  submitErrorMessage,
  submitInfoMessage,
  loadInfoMessage,
  loadError,
  onRetrySave,
  onRetrySubmit,
  onRetryLoad,
}) => {
  const { t } = useTranslation();
  const loadErrorState = loadError
    ? getProductizedErrorState(loadError, {
        resourceLabel: t('ui.sessionState.currentItem'),
        taskLabel: t('ui.sessionState.continueSession'),
        retryActionLabel: t('ui.sessionState.retryLoad'),
      })
    : null;

  return (
  <>
    {isSaving && (
      <FeedbackState
        kind="saving"
        compact
        title={t('ui.sessionState.savingTitle')}
        description={t('ui.sessionState.savingDescription')}
      />
    )}
    {saveMessage && (
      <FeedbackState
        kind="saved"
        compact
        title={t('ui.sessionState.savedTitle')}
        description={saveMessage}
      />
    )}
    {saveErrorMessage && !saveConflictMessage && (
      <FeedbackState
        kind="retry"
        compact
        title={t('ui.sessionState.saveFailedTitle')}
        description={saveErrorMessage}
        impact={t('ui.sessionState.saveFailedSafety')}
        nextStep={t('ui.sessionState.saveFailedNextStep')}
        primaryAction={onRetrySave ? { label: t('ui.sessionState.saveAction'), onClick: onRetrySave } : undefined}
      />
    )}
    {saveConflictMessage && (
      <FeedbackState
        kind="retry"
        compact
        title={t('ui.sessionState.conflictTitle', '检测到会话冲突')}
        description={saveConflictMessage}
        impact={t('ui.sessionState.conflictSafety', '服务器上的最新进度仍然安全，当前页面不会自动覆盖它。')}
        nextStep={t('ui.sessionState.conflictNextStep', '检查已同步的题目后，再次保存或继续作答。')}
        primaryAction={onRetrySave ? { label: t('ui.sessionState.saveAction'), onClick: onRetrySave } : undefined}
      />
    )}
    {submitInfoMessage && (
      <FeedbackState
        kind="saving"
        compact
        title={t('ui.sessionState.submittingTitle')}
        description={submitInfoMessage}
      />
    )}
    {loadInfoMessage && (
      <FeedbackState
        kind="loading"
        compact
        title={t('ui.sessionState.loadingTitle')}
        description={loadInfoMessage}
      />
    )}
    {submitErrorMessage && (
      <FeedbackState
        kind="retry"
        compact
        title={t('ui.sessionState.submitFailedTitle')}
        description={submitErrorMessage}
        impact={t('ui.sessionState.submitFailedSafety')}
        nextStep={t('ui.sessionState.submitFailedNextStep')}
        primaryAction={onRetrySubmit ? { label: t('ui.sessionState.retrySubmit', '重试提交'), onClick: onRetrySubmit } : undefined}
      />
    )}
    {loadErrorState && (
      <FeedbackState
        kind={loadErrorState.kind}
        compact
        title={loadErrorState.title}
        description={loadErrorState.description}
        impact={loadErrorState.impact}
        nextStep={loadErrorState.nextStep}
        primaryAction={onRetryLoad ? { label: t('ui.sessionState.retryLoad'), onClick: onRetryLoad } : undefined}
      />
    )}
  </>
  );
};

type SessionProgressHeaderProps = {
  icon?: React.ReactNode;
  label: string;
  currentItem?: number | null;
  answeredItems?: number | null;
  totalItems?: number | null;
  savedState?: 'idle' | 'saving' | 'saved' | 'error' | 'conflict';
  savedAtLabel?: string | null;
  remainingLabel?: string | null;
  remainingMs?: number | null;
  onExit?: () => void;
  exitLabel?: string;
  exitDisabled?: boolean;
  gradientClassName: string;
};

export const SessionProgressHeader: React.FC<SessionProgressHeaderProps> = ({
  icon,
  label,
  currentItem,
  answeredItems,
  totalItems,
  savedState = 'idle',
  savedAtLabel,
  remainingLabel,
  remainingMs,
  onExit,
  exitLabel,
  exitDisabled,
  gradientClassName,
}) => {
  const { t } = useTranslation();
  const safeTotal = Math.max(1, totalItems || 0);
  const progressValue = currentItem ?? answeredItems ?? 0;
  const progressPercent = buildProgressPercent(progressValue, totalItems);
  const isUrgent = typeof remainingMs === 'number' && remainingMs > 0 && remainingMs <= 5 * 60 * 1000;
  const stateLabels = {
    idle: t('ui.sessionState.progressReady', '进行中'),
    saving: t('ui.sessionState.savingAction'),
    saved: t('ui.sessionState.savedTitle'),
    error: t('ui.sessionState.saveFailedTitle'),
    conflict: t('ui.sessionState.conflictTitle', '检测到其他设备的更新'),
  } as const;
  const stateTone = savedState === 'error' || savedState === 'conflict'
    ? 'text-amber-700 dark:text-amber-300'
    : savedState === 'saved'
      ? 'text-emerald-700 dark:text-emerald-300'
      : 'text-slate-600 dark:text-white/60';

  return (
    <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/70 p-4 dark:border-white/10 dark:bg-white/5">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:gap-6">
        <div className="min-w-[8rem]">
          <div className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.18em] text-slate-400 dark:text-white/35">
            {icon}
            <span>{label}</span>
          </div>
          <div className="mt-1 text-lg font-black text-slate-900 dark:text-white" aria-live="polite">
            {currentItem != null ? `${currentItem} / ${totalItems || 0}` : `${answeredItems || 0} / ${totalItems || 0}`}
          </div>
        </div>

        <div className="min-w-0 flex-1">
          <div
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={safeTotal}
            aria-valuenow={Math.min(safeTotal, Math.max(0, progressValue))}
            aria-label={label}
            className="h-2.5 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-white/10"
          >
            <div
              className={`h-full transition-[width] duration-300 motion-reduce:transition-none ${gradientClassName}`}
              style={{ width: `${progressPercent}%` }}
            />
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 dark:text-white/45">
            <span>{t('ui.sessionState.answeredLabel', '已完成')} {answeredItems || 0} / {totalItems || 0}</span>
            <span role="status" aria-live="polite" className={`inline-flex items-center gap-1.5 ${stateTone}`}>
              <span aria-hidden="true" className={`size-1.5 rounded-full ${savedState === 'saving' ? 'bg-sky-500' : savedState === 'saved' ? 'bg-emerald-500' : savedState === 'error' || savedState === 'conflict' ? 'bg-amber-500' : 'bg-slate-400'}`} />
              {stateLabels[savedState]}
              {savedAtLabel ? <span className="text-slate-400 dark:text-white/35">· {savedAtLabel}</span> : null}
            </span>
          </div>
        </div>

        {remainingLabel ? (
          <div className={`shrink-0 rounded-2xl border px-3 py-2 text-sm font-black tabular-nums ${isUrgent ? 'border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300' : 'border-slate-200/70 text-slate-700 dark:border-white/10 dark:text-white/75'}`}>
            <span className="mr-2 text-xs font-bold text-slate-400 dark:text-white/35">{t('ui.sessionState.remainingLabel', '剩余时间')}</span>
            <span role="timer" aria-live={isUrgent ? 'polite' : undefined}>{remainingLabel}</span>
          </div>
        ) : null}

        {onExit ? (
          <button
            type="button"
            onClick={onExit}
            disabled={exitDisabled}
      className="min-h-11 shrink-0 rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 transition-colors motion-reduce:transition-none hover:border-primary/40 hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 disabled:cursor-not-allowed disabled:opacity-55 dark:border-white/10 dark:text-white/70"
          >
            {exitLabel || t('ui.sessionState.exitAction', '退出')}
          </button>
        ) : null}
      </div>
    </div>
  );
};

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
      className="min-h-11 w-full rounded-[1.8rem] border border-slate-200 bg-white/70 px-5 py-4 text-left transition-all motion-reduce:transition-none hover:border-primary/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 disabled:opacity-60 dark:border-white/10 dark:bg-white/5"
  >
    <div className="flex items-center justify-between gap-4">
      <span className="font-bold text-slate-900 dark:text-white">{label}</span>
      {icon || <ChevronRight className="text-primary" size={16} />}
    </div>
  </button>
);
