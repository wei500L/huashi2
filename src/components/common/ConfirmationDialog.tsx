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
  const titleId = React.useId();
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
      <div className="fixed inset-0 z-[70] flex items-center justify-center overflow-y-auto px-4 py-8">
        <div
          ref={dialogRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby={titleId}
          tabIndex={-1}
          className="w-full max-w-3xl rounded-xl border border-border-subtle bg-surface p-5 shadow-[0_30px_80px_rgba(15,23,42,0.28)]"
        >
          <span id={titleId} className="sr-only">{pending ? (pendingTitle ?? title) : title}</span>
          <FeedbackState
            kind={pending ? 'saving' : 'destructive'}
            compact
            className="border-0 bg-transparent p-0 shadow-none"
            title={pending ? (pendingTitle ?? title) : title}
            description={pending ? (pendingDescription ?? description) : description}
            impact={pending ? undefined : safety}
            nextStep={pending ? undefined : nextStep}
          />

          {!pending && details ? <div className="mt-5">{details}</div> : null}

          <div className="mt-6 flex flex-wrap justify-end gap-3">
            <button
              ref={cancelButtonRef}
              type="button"
              onClick={handleClose}
              disabled={pending}
              className="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-bold text-slate-600 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:text-white/60"
            >
              {cancelLabel}
            </button>
            <button
              type="button"
              onClick={onConfirm}
              disabled={pending}
              className="rounded-2xl border border-rose-600 bg-rose-600 px-5 py-3 text-sm font-bold text-white shadow-sm hover:bg-rose-700 disabled:cursor-not-allowed disabled:opacity-55"
            >
              {pending ? (pendingTitle ?? confirmLabel) : confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </>
  );
};
