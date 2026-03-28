import React from 'react';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';

type SessionRefetchResult<TNextItem> = {
  data?: TNextItem;
};

type SessionRuntimeMessages = {
  saved: string;
  savedAndExit: string;
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
    } catch (error) {
      const normalizedError = normalizeApiError(error);
      if (normalizedError.status === 409) {
        await resolveConflict();
      }
    }
  }, [active, buildSnapshot, nextItem, resolveConflict, saveProgressKeepalive, sessionId]);

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
        setIsSaving(false);
      }
    },
    [active, buildSnapshot, nextItem, refetchCurrent, resolveConflict, resolvedMessages.saved, resolvedMessages.savedAndExit, saveProgress, sessionId]
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
    const onBeforeUnload = () => {
      void saveSnapshotKeepalive();
    };
    document.addEventListener('visibilitychange', onVisibilityChange);
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('beforeunload', onBeforeUnload);
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
