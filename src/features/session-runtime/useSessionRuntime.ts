import React from 'react';
import { useBeforeUnload, useBlocker } from 'react-router';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';

type SessionRefetchResult<TNextItem> = {
  data?: TNextItem;
};

type SessionRuntimeMessages = {
  saved: string;
  savedAndExit: string;
  keepaliveFailed: string;
  leaveConfirm: string;
};

type SessionRuntimeOptions<TNextItem> = {
  active: boolean;
  sessionId: number | null;
  nextItem?: TNextItem | null;
  refetchCurrent: () => Promise<SessionRefetchResult<TNextItem>>;
  buildSnapshot: (sessionId: number, nextItem?: TNextItem | null) => Record<string, unknown>;
  saveProgress: (sessionId: number, snapshot: Record<string, unknown>) => Promise<unknown>;
  saveProgressKeepalive: (sessionId: number, snapshot: Record<string, unknown>) => Promise<unknown>;
  isCompleted: (nextItem?: TNextItem) => boolean;
  onCompleted: (nextItem: TNextItem) => void;
  messages?: Partial<SessionRuntimeMessages>;
};

const defaultMessages: SessionRuntimeMessages = {
  saved: '进度已保存。',
  savedAndExit: '进度已保存，稍后可在历史页继续。',
  keepaliveFailed: '自动保存失败，请先手动保存后再离开当前页。',
  leaveConfirm: '当前会话仍在进行中，确认离开此页面吗？未保存的进度可能丢失。',
};

export function useSessionRuntime<TNextItem>({
  active,
  sessionId,
  nextItem,
  refetchCurrent,
  buildSnapshot,
  saveProgress,
  saveProgressKeepalive,
  isCompleted,
  onCompleted,
  messages,
}: SessionRuntimeOptions<TNextItem>) {
  const resolvedMessages = React.useMemo(
    () => ({ ...defaultMessages, ...messages }),
    [messages]
  );
  const [saveMessage, setSaveMessage] = React.useState<string | null>(null);
  const [saveErrorMessage, setSaveErrorMessage] = React.useState<string | null>(null);
  const [isSaving, setIsSaving] = React.useState(false);
  const allowNavigationRef = React.useRef(false);

  const resetFeedback = React.useCallback(() => {
    setSaveMessage(null);
    setSaveErrorMessage(null);
  }, []);

  const resolveConflict = React.useCallback(async () => {
    const refreshed = await refetchCurrent();
    if (refreshed.data && isCompleted(refreshed.data)) {
      onCompleted(refreshed.data);
      return true;
    }
    return false;
  }, [isCompleted, onCompleted, refetchCurrent]);

  const saveSnapshotKeepalive = React.useCallback(async () => {
    if (!active || !sessionId) {
      return;
    }
    try {
      await saveProgressKeepalive(sessionId, buildSnapshot(sessionId, nextItem));
      setSaveErrorMessage((current) => (current === resolvedMessages.keepaliveFailed ? null : current));
    } catch (error) {
      const normalizedError = normalizeApiError(error);
      if (normalizedError.status === 409) {
        const completed = await resolveConflict();
        if (completed) {
          return;
        }
      }
      setSaveErrorMessage(resolvedMessages.keepaliveFailed);
    }
  }, [active, buildSnapshot, nextItem, resolveConflict, resolvedMessages.keepaliveFailed, saveProgressKeepalive, sessionId]);

  const saveProgressManually = React.useCallback(
    async (options?: { onSuccess?: () => void; exitAfterSave?: boolean }) => {
      if (!active || !sessionId) {
        return false;
      }

      setIsSaving(true);
      setSaveMessage(null);
      setSaveErrorMessage(null);

      try {
        await saveProgress(sessionId, buildSnapshot(sessionId, nextItem));
        setSaveMessage(options?.exitAfterSave ? resolvedMessages.savedAndExit : resolvedMessages.saved);
        if (options?.exitAfterSave) {
          allowNavigationRef.current = true;
        }
        options?.onSuccess?.();
        return true;
      } catch (error) {
        const normalizedError = normalizeApiError(error);
        if (normalizedError.status === 409) {
          const completed = await resolveConflict();
          if (completed) {
            return false;
          }
        }
        setSaveErrorMessage(getApiErrorMessage(error));
        return false;
      } finally {
        if (options?.exitAfterSave) {
          window.setTimeout(() => {
            allowNavigationRef.current = false;
          }, 0);
        }
        setIsSaving(false);
      }
    },
    [active, buildSnapshot, nextItem, refetchCurrent, resolveConflict, resolvedMessages.saved, resolvedMessages.savedAndExit, saveProgress, sessionId]
  );

  const blocker = useBlocker(() => active && !!sessionId && !allowNavigationRef.current);

  React.useEffect(() => {
    if (blocker.state !== 'blocked') {
      return;
    }
    const shouldLeave = window.confirm(resolvedMessages.leaveConfirm);
    if (shouldLeave) {
      allowNavigationRef.current = true;
      void saveSnapshotKeepalive().finally(() => {
        blocker.proceed();
        window.setTimeout(() => {
          allowNavigationRef.current = false;
        }, 0);
      });
      return;
    }
    blocker.reset();
  }, [blocker, resolvedMessages.leaveConfirm, saveSnapshotKeepalive]);

  useBeforeUnload(
    React.useCallback(
      (event) => {
        if (!active || !sessionId || allowNavigationRef.current) {
          return;
        }
        event.preventDefault();
        event.returnValue = resolvedMessages.leaveConfirm;
        void saveSnapshotKeepalive();
      },
      [active, resolvedMessages.leaveConfirm, saveSnapshotKeepalive, sessionId]
    )
  );

  React.useEffect(() => {
    if (!active || !sessionId) {
      return;
    }
    const onVisibilityChange = () => {
      if (document.hidden) {
        void saveSnapshotKeepalive();
      }
    };
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [active, saveSnapshotKeepalive, sessionId]);

  return {
    isSaving,
    saveMessage,
    saveErrorMessage,
    resetFeedback,
    saveProgressManually,
  };
}
