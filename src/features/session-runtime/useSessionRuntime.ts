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
  conflict: string;
  leaveConfirm: string;
};

type SessionRuntimeOptions<TNextItem, TSnapshot, THeartbeat> = {
  active: boolean;
  sessionId: number | null;
  nextItem?: TNextItem | null;
  refetchCurrent: () => Promise<SessionRefetchResult<TNextItem>>;
  buildSnapshot: (sessionId: number, nextItem?: TNextItem | null) => TSnapshot;
  heartbeat?: (sessionId: number) => Promise<THeartbeat>;
  heartbeatIntervalMs?: number;
  shouldHeartbeat?: (nextItem?: TNextItem | null) => boolean;
  isHeartbeatInProgress?: (heartbeat: THeartbeat) => boolean;
  saveProgress: (sessionId: number, snapshot: TSnapshot) => Promise<unknown>;
  saveProgressKeepalive: (sessionId: number, snapshot: TSnapshot) => Promise<unknown>;
  isCompleted: (nextItem?: TNextItem) => boolean;
  onCompleted: (nextItem: TNextItem) => void;
  messages?: Partial<SessionRuntimeMessages>;
};

type SessionConnectionState = 'online' | 'offline' | 'recovering' | 'recovered' | 'recovery-error';

const defaultMessages: SessionRuntimeMessages = {
  saved: '进度已保存。',
  savedAndExit: '进度已保存，稍后可在历史页继续。',
  keepaliveFailed: '自动保存失败，请先手动保存后再离开当前页。',
  conflict: '检测到其他设备或页面的更新，已重新同步当前会话；请检查当前题目后再保存。',
  leaveConfirm: '当前会话仍在进行中，确认离开此页面吗？未保存的进度可能丢失。',
};

export function useSessionRuntime<TNextItem, TSnapshot, THeartbeat = never>({
  active,
  sessionId,
  nextItem,
  refetchCurrent,
  buildSnapshot,
  heartbeat,
  heartbeatIntervalMs = 60000,
  shouldHeartbeat,
  isHeartbeatInProgress,
  saveProgress,
  saveProgressKeepalive,
  isCompleted,
  onCompleted,
  messages,
}: SessionRuntimeOptions<TNextItem, TSnapshot, THeartbeat>) {
  const resolvedMessages = React.useMemo(
    () => ({ ...defaultMessages, ...messages }),
    [messages]
  );
  const [saveMessage, setSaveMessage] = React.useState<string | null>(null);
  const [saveErrorMessage, setSaveErrorMessage] = React.useState<string | null>(null);
  const [saveConflictMessage, setSaveConflictMessage] = React.useState<string | null>(null);
  const [isSaving, setIsSaving] = React.useState(false);
  const [connectionState, setConnectionState] = React.useState<SessionConnectionState>(() =>
    typeof navigator !== 'undefined' && navigator.onLine === false ? 'offline' : 'online'
  );
  const heartbeatInFlightRef = React.useRef(false);
  const allowNavigationRef = React.useRef<(callback: () => void) => void>((callback) => {
    callback();
  });
  const saveProgressManuallyRef = React.useRef<
    (options?: { onSuccess?: () => void; exitAfterSave?: boolean }) => Promise<{ status: 'saved' | 'completed' | 'failed' }>
  >(async () => ({ status: 'failed' }));

  const resetFeedback = React.useCallback(() => {
    setSaveMessage(null);
    setSaveErrorMessage(null);
    setSaveConflictMessage(null);
  }, []);

  const syncStateFromServer = React.useCallback(async () => {
    const refreshed = await refetchCurrent();
    if (refreshed.data && isCompleted(refreshed.data)) {
      onCompleted(refreshed.data);
    }
    return refreshed.data;
  }, [isCompleted, onCompleted, refetchCurrent]);

  const resolveConflict = React.useCallback(async () => {
    const refreshed = await syncStateFromServer();
    return !!(refreshed && isCompleted(refreshed));
  }, [isCompleted, syncStateFromServer]);

  const recoverConnection = React.useCallback(async () => {
    if (!active || !sessionId) {
      setConnectionState('online');
      return;
    }
    setConnectionState('recovering');
    try {
      await syncStateFromServer();
      setConnectionState('recovered');
    } catch {
      setConnectionState('recovery-error');
    }
  }, [active, sessionId, syncStateFromServer]);

  React.useEffect(() => {
    const handleOffline = () => setConnectionState('offline');
    const handleOnline = () => {
      void recoverConnection();
    };
    window.addEventListener('offline', handleOffline);
    window.addEventListener('online', handleOnline);
    return () => {
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('online', handleOnline);
    };
  }, [recoverConnection]);

  React.useEffect(() => {
    if (connectionState !== 'recovered') {
      return;
    }
    const timer = window.setTimeout(() => setConnectionState('online'), 4000);
    return () => window.clearTimeout(timer);
  }, [connectionState]);

  const sendHeartbeat = React.useCallback(async () => {
    if (!active || !sessionId || !heartbeat || heartbeatInFlightRef.current) {
      return;
    }
    if (shouldHeartbeat && !shouldHeartbeat(nextItem)) {
      return;
    }
    heartbeatInFlightRef.current = true;
    try {
      const response = await heartbeat(sessionId);
      if (isHeartbeatInProgress && !isHeartbeatInProgress(response)) {
        await syncStateFromServer();
      }
    } catch (error) {
      const normalizedError = normalizeApiError(error);
      if (normalizedError.status === 409) {
        await resolveConflict();
      }
    } finally {
      heartbeatInFlightRef.current = false;
    }
  }, [
    active,
    heartbeat,
    isHeartbeatInProgress,
    nextItem,
    resolveConflict,
    sessionId,
    shouldHeartbeat,
    syncStateFromServer,
  ]);

  const saveSnapshotKeepalive = React.useCallback(async () => {
    if (!active || !sessionId) {
      return;
    }
    try {
      await saveProgressKeepalive(sessionId, buildSnapshot(sessionId, nextItem));
      setSaveErrorMessage((current) => (current === resolvedMessages.keepaliveFailed ? null : current));
      setSaveConflictMessage(null);
    } catch (error) {
      const normalizedError = normalizeApiError(error);
      if (normalizedError.status === 409) {
        const completed = await resolveConflict();
        if (completed) {
          return;
        }
        setSaveConflictMessage(resolvedMessages.conflict);
      }
      setSaveErrorMessage(resolvedMessages.keepaliveFailed);
    }
  }, [active, buildSnapshot, nextItem, resolveConflict, resolvedMessages.conflict, resolvedMessages.keepaliveFailed, saveProgressKeepalive, sessionId]);

  React.useEffect(() => {
    if (!active || !sessionId || !heartbeat) {
      return;
    }
    if (shouldHeartbeat && !shouldHeartbeat(nextItem)) {
      return;
    }
    const timer = window.setInterval(() => {
      void sendHeartbeat();
    }, heartbeatIntervalMs);
    return () => {
      window.clearInterval(timer);
    };
  }, [active, heartbeat, heartbeatIntervalMs, nextItem, sendHeartbeat, sessionId, shouldHeartbeat]);

  const saveProgressManually = React.useCallback(
    async (options?: { onSuccess?: () => void; exitAfterSave?: boolean }) => {
      if (!active || !sessionId) {
        return { status: 'failed' as const };
      }

      setIsSaving(true);
      setSaveMessage(null);
      setSaveErrorMessage(null);
      setSaveConflictMessage(null);

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
          setSaveConflictMessage(resolvedMessages.conflict);
        }
        setSaveErrorMessage(getApiErrorMessage(error));
        return { status: 'failed' as const };
      } finally {
        setIsSaving(false);
      }
    },
    [active, buildSnapshot, nextItem, resolveConflict, resolvedMessages.conflict, resolvedMessages.saved, resolvedMessages.savedAndExit, saveProgress, sessionId]
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
    saveConflictMessage,
    connectionState,
    recoverConnection,
    resetFeedback,
    saveProgressManually,
  };
}
