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
      className="rounded-full border border-slate-200 px-5 py-3 text-sm font-bold disabled:opacity-60 dark:border-white/10"
    >
      {isSaving ? t('ui.sessionState.savingAction') : t('ui.sessionState.saveAction')}
    </button>
    <button
      type="button"
      onClick={onSaveAndExit}
      disabled={disabled || isSaving}
      className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
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
  submitErrorMessage?: string | null;
  submitInfoMessage?: string | null;
  loadInfoMessage?: string | null;
  loadError?: unknown;
  onRetryLoad?: () => void;
};

export const SessionFeedbackBanners: React.FC<SessionFeedbackBannersProps> = ({
  isSaving,
  saveMessage,
  saveErrorMessage,
  submitErrorMessage,
  submitInfoMessage,
  loadInfoMessage,
  loadError,
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
    {saveErrorMessage && (
      <FeedbackState
        kind="retry"
        compact
        title={t('ui.sessionState.saveFailedTitle')}
        description={saveErrorMessage}
        impact={t('ui.sessionState.saveFailedSafety')}
        nextStep={t('ui.sessionState.saveFailedNextStep')}
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
  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
    <div className="flex items-center gap-3 text-sm text-slate-500 dark:text-white/45">
      {icon}
      <span>{label}</span>
    </div>
    <div
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={Math.max(1, totalItems || 0)}
      aria-valuenow={answeredItems || 0}
      aria-label={label}
      className="h-2 w-full overflow-hidden rounded-full bg-slate-200 sm:w-56 dark:bg-white/10"
    >
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
