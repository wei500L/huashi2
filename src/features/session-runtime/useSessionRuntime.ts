import React from 'react';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import { useLeaveProtection } from './useLeaveProtection';

type SessionRefetchResult<TNextItem> = {
  data?: TNextItem;
};

type SessionRuntimeMessages = {
  saved: string;
  savedAndExit: string;
  keepaliveFailed: string;
  leaveConfirm: string;
};

type SessionRuntimeOptions<TNextItem, TSnapshot> = {
  active: boolean;
  sessionId: number | null;
  nextItem?: TNextItem | null;
  refetchCurrent: () => Promise<SessionRefetchResult<TNextItem>>;
  buildSnapshot: (sessionId: number, nextItem?: TNextItem | null) => TSnapshot;
  saveProgress: (sessionId: number, snapshot: TSnapshot) => Promise<unknown>;
  saveProgressKeepalive: (sessionId: number, snapshot: TSnapshot) => Promise<unknown>;
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

export function useSessionRuntime<TNextItem, TSnapshot>({
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
}: SessionRuntimeOptions<TNextItem, TSnapshot>) {
  const resolvedMessages = React.useMemo(
    () => ({ ...defaultMessages, ...messages }),
    [messages]
  );
  const [saveMessage, setSaveMessage] = React.useState<string | null>(null);
  const [saveErrorMessage, setSaveErrorMessage] = React.useState<string | null>(null);
  const [isSaving, setIsSaving] = React.useState(false);
  const allowNavigationRef = React.useRef<(callback: () => void) => void>((callback) => {
    callback();
  });
  const saveProgressManuallyRef = React.useRef<
    (options?: { onSuccess?: () => void; exitAfterSave?: boolean }) => Promise<{ status: 'saved' | 'completed' | 'failed' }>
  >(async () => ({ status: 'failed' }));

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
        return { status: 'failed' as const };
      }

      setIsSaving(true);
      setSaveMessage(null);
      setSaveErrorMessage(null);

      try {
        await saveProgress(sessionId, buildSnapshot(sessionId, nextItem));
        setSaveMessage(options?.exitAfterSave ? resolvedMessages.savedAndExit : resolvedMessages.saved);
        if (options?.onSuccess) {
          if (options.exitAfterSave) {
            allowNavigationRef.current(() => options.onSuccess?.());
          } else {
            options.onSuccess();
          }
        }
        return { status: 'saved' as const };
      } catch (error) {
        const normalizedError = normalizeApiError(error);
        if (normalizedError.status === 409) {
          const completed = await resolveConflict();
          if (completed) {
            if (options?.onSuccess) {
              if (options.exitAfterSave) {
                allowNavigationRef.current(() => options.onSuccess?.());
              } else {
                options.onSuccess();
              }
            }
            return { status: 'completed' as const };
          }
        }
        setSaveErrorMessage(getApiErrorMessage(error));
        return { status: 'failed' as const };
      } finally {
        setIsSaving(false);
      }
    },
    [active, buildSnapshot, nextItem, resolveConflict, resolvedMessages.saved, resolvedMessages.savedAndExit, saveProgress, sessionId]
  );

  saveProgressManuallyRef.current = saveProgressManually;

  const { allowNavigation } = useLeaveProtection({
    active: active && !!sessionId,
    leaveConfirm: resolvedMessages.leaveConfirm,
    onRouteLeave: async () => {
      const result = await saveProgressManuallyRef.current({ exitAfterSave: true });
      return result.status === 'saved' || result.status === 'completed';
    },
    onBackgroundPersist: saveSnapshotKeepalive,
  });
  allowNavigationRef.current = allowNavigation;

  return {
    isSaving,
    saveMessage,
    saveErrorMessage,
    resetFeedback,
    saveProgressManually,
  };
}
