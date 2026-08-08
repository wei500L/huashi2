import React from 'react';
import { FeedbackState } from './FeedbackState';
import { useBodyScrollLock, useDialogAccessibility } from '@/lib/a11y';

type ConfirmationDialogProps = {
  open: boolean;
  title: string;
  description: string;
  safety: string;
  nextStep: string;
  confirmLabel: string;
  cancelLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
  pending?: boolean;
  pendingTitle?: string;
  pendingDescription?: string;
  details?: React.ReactNode;
};

export const ConfirmationDialog: React.FC<ConfirmationDialogProps> = ({
  open,
  title,
  description,
  safety,
  nextStep,
  confirmLabel,
  cancelLabel,
  onConfirm,
  onCancel,
  pending = false,
  pendingTitle,
  pendingDescription,
  details,
}) => {
  const dialogRef = React.useRef<HTMLDivElement | null>(null);
  const cancelButtonRef = React.useRef<HTMLButtonElement | null>(null);
  const dialogTitle = pending ? (pendingTitle ?? title) : title;
  const handleClose = React.useCallback(() => {
    if (!pending) {
      onCancel();
    }
  }, [onCancel, pending]);

  useBodyScrollLock(open);
  useDialogAccessibility({
    open,
    containerRef: dialogRef,
    initialFocusRef: cancelButtonRef,
    onClose: handleClose,
  });

  if (!open) {
    return null;
  }

  return (
    <>
      <div aria-hidden="true" onClick={handleClose} className="fixed inset-0 z-[60] bg-slate-950/45 backdrop-blur-sm" />
      <div className="fixed inset-0 z-[70] flex items-end justify-center overflow-y-auto px-3 py-4 sm:items-center sm:px-4 sm:py-8">
        <div
          ref={dialogRef}
          role="dialog"
          aria-modal="true"
          aria-label={dialogTitle}
          tabIndex={-1}
          className="safe-area-dialog max-h-[calc(100dvh-2rem)] w-full max-w-3xl overflow-y-auto rounded-t-2xl border border-border-subtle bg-surface p-4 shadow-[0_30px_80px_rgba(15,23,42,0.28)] sm:rounded-xl sm:p-5"
        >
          <FeedbackState
            kind={pending ? 'saving' : 'destructive'}
            compact
            className="border-0 bg-transparent p-0 shadow-none"
            title={dialogTitle}
            description={pending ? (pendingDescription ?? description) : description}
            impact={pending ? undefined : safety}
            nextStep={pending ? undefined : nextStep}
          />

          {!pending && details ? <div className="mt-5">{details}</div> : null}

          <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:flex-wrap sm:justify-end">
            <button
              ref={cancelButtonRef}
              type="button"
              onClick={handleClose}
              disabled={pending}
              className="min-h-11 w-full rounded-2xl border border-slate-200 px-5 py-3 text-sm font-bold text-slate-600 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto dark:border-white/10 dark:text-white/60"
            >
              {cancelLabel}
            </button>
            <button
              type="button"
              onClick={onConfirm}
              disabled={pending}
              className="min-h-11 w-full rounded-2xl border border-rose-600 bg-rose-600 px-5 py-3 text-sm font-bold text-white shadow-sm hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-55 sm:w-auto"
            >
              {pending ? (pendingTitle ?? confirmLabel) : confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </>
  );
};
