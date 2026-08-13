import React, { useEffect, useEffectEvent, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell, CheckCheck } from 'lucide-react';
import { motion, useReducedMotion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FeedbackState } from '@/components/common/FeedbackState';
import type { NotificationItemVO, NotificationSocketMessage, PageResult } from '@/lib/contracts';
import { getProductizedErrorState } from '@/lib/async-state';
import { buildNotificationAuthMessage, buildNotificationWebSocketUrl } from '@/lib/notifications';
import { notificationService } from '@/lib/services';
import { cn } from '@/lib/utils';
import { useAuthStore, useUIStore } from '@/store';
import { useDialogAccessibility } from '@/lib/a11y';

const RECENT_PAGE_SIZE = 8;

function formatNotificationTime(value: string, locale: 'zh-CN' | 'en-US') {
  return new Intl.DateTimeFormat(locale, {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function levelAccent(level: string) {
  switch (level) {
    case 'ERROR':
      return 'bg-rose-500';
    case 'WARNING':
      return 'bg-amber-500';
    case 'SUCCESS':
      return 'bg-emerald-500';
    default:
      return 'bg-sky-500';
  }
}

function levelLabel(level: string, t: (key: string) => string) {
  switch (level) {
    case 'ERROR':
      return t('shell.notifications.levels.error');
    case 'WARNING':
      return t('shell.notifications.levels.warning');
    case 'SUCCESS':
      return t('shell.notifications.levels.success');
    default:
      return t('shell.notifications.levels.info');
  }
}

function levelTone(level: string) {
  switch (level) {
    case 'ERROR':
      return 'border-rose-500/20 bg-rose-500/10 text-rose-700 dark:text-rose-300';
    case 'WARNING':
      return 'border-amber-500/20 bg-amber-500/10 text-amber-700 dark:text-amber-300';
    case 'SUCCESS':
      return 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300';
    default:
      return 'border-sky-500/20 bg-sky-500/10 text-sky-700 dark:text-sky-300';
  }
}

export const NotificationBell: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const buttonRef = useRef<HTMLButtonElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const panelTitleId = React.useId();
  const session = useAuthStore((state) => state.session);
  const authStatus = useAuthStore((state) => state.status);
  const locale = useUIStore((state) => state.locale);
  const [isOpen, setIsOpen] = useState(false);
  const [panelStyle, setPanelStyle] = useState<{ top: number; left: number; width: number } | null>(null);
  const prefersReducedMotion = useReducedMotion();
  const isAuthenticated = authStatus === 'authenticated' && Boolean(session?.accessToken);
  const closeNotifications = React.useCallback(() => {
    setIsOpen(false);
    buttonRef.current?.focus();
  }, []);

  useDialogAccessibility({
    open: isOpen && Boolean(panelStyle),
    containerRef: panelRef,
    onClose: closeNotifications,
  });

  const unreadCountQuery = useQuery({
    queryKey: ['notifications', 'count'],
    queryFn: ({ signal }) => notificationService.getUnreadCount({ signal }),
    enabled: isAuthenticated,
    refetchInterval: 60_000,
    staleTime: 15_000,
  });

  const recentNotificationsQuery = useQuery({
    queryKey: ['notifications', 'recent'],
    queryFn: ({ signal }) => notificationService.list({ pageNo: 1, pageSize: RECENT_PAGE_SIZE }, { signal }),
    enabled: isAuthenticated,
    refetchInterval: 120_000,
    staleTime: 15_000,
  });

  const markReadMutation = useMutation({
    mutationFn: (notificationId: number) => notificationService.markRead(notificationId),
    onSuccess: (notification) => {
      queryClient.setQueryData<PageResult<NotificationItemVO> | undefined>(['notifications', 'recent'], (current) => {
        if (!current) {
          return current;
        }
        return {
          ...current,
          records: current.records.map((item) => (item.id === notification.id ? notification : item)),
        };
      });
      queryClient.setQueryData<{ unreadCount: number } | undefined>(['notifications', 'count'], (current) => ({
        unreadCount: Math.max(0, (current?.unreadCount ?? 0) - 1),
      }));
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: () => notificationService.markAllRead(),
    onSuccess: (payload) => {
      queryClient.setQueryData<PageResult<NotificationItemVO> | undefined>(['notifications', 'recent'], (current) => {
        if (!current) {
          return current;
        }
        return {
          ...current,
          records: current.records.map((item) => ({
            ...item,
            status: 'READ',
            readAt: item.readAt ?? new Date().toISOString(),
          })),
        };
      });
      queryClient.setQueryData(['notifications', 'count'], payload);
    },
  });

  const unreadCount = unreadCountQuery.data?.unreadCount ?? 0;
  const notifications = recentNotificationsQuery.data?.records ?? [];
  const notificationErrorState = recentNotificationsQuery.error
    ? getProductizedErrorState(recentNotificationsQuery.error, {
        resourceLabel: t('shell.notifications.resourceLabel'),
        taskLabel: t('shell.notifications.taskLabel'),
        retryActionLabel: t('shell.notifications.retry'),
      })
    : null;
  const buttonLabel = useMemo(
    () => (unreadCount > 0 ? `${t('shell.notifications.open')} (${unreadCount})` : t('shell.notifications.open')),
    [t, unreadCount]
  );

  const handleSocketMessage = useEffectEvent((event: MessageEvent<string>) => {
    let payload: NotificationSocketMessage | null = null;
    try {
      payload = JSON.parse(event.data) as NotificationSocketMessage;
    } catch {
      return;
    }
    if (payload?.type !== 'NOTIFICATION_CREATED' || !payload.notification) {
      return;
    }
    const notification = payload.notification;

    queryClient.setQueryData(['notifications', 'count'], { unreadCount: payload.unreadCount });
    queryClient.setQueryData<PageResult<NotificationItemVO> | undefined>(['notifications', 'recent'], (current) => ({
      total: Math.max(current?.total ?? 0, (current?.records?.length ?? 0) + 1),
      pageNo: 1,
      pageSize: RECENT_PAGE_SIZE,
      records: [notification, ...(current?.records ?? []).filter((item) => item.id !== notification.id)]
        .filter((item): item is NotificationItemVO => Boolean(item))
        .slice(0, RECENT_PAGE_SIZE),
    }));
  });

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const updatePanelPosition = () => {
      const button = buttonRef.current;
      if (!button) {
        return;
      }
      const rect = button.getBoundingClientRect();
      const width = Math.min(window.innerWidth - 16, 384);
      const left = Math.min(
        Math.max(8, rect.right - width),
        Math.max(8, window.innerWidth - width - 8)
      );
      setPanelStyle({
        top: rect.bottom + 14,
        left,
        width,
      });
    };

    updatePanelPosition();

    const handlePointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (panelRef.current?.contains(target) || buttonRef.current?.contains(target)) {
        return;
      }
      setIsOpen(false);
    };
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
        buttonRef.current?.focus();
      }
    };
    window.addEventListener('resize', updatePanelPosition);
    window.addEventListener('scroll', updatePanelPosition, true);
    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleEscape);
    return () => {
      window.removeEventListener('resize', updatePanelPosition);
      window.removeEventListener('scroll', updatePanelPosition, true);
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    const accessToken = session?.accessToken;
    const wsUrl = accessToken ? buildNotificationWebSocketUrl() : null;
    if (!wsUrl) {
      return;
    }

    let closedByEffect = false;
    let reconnectTimer: number | null = null;
    let socket: WebSocket | null = null;

    const connect = () => {
      if (closedByEffect) {
        return;
      }
      socket = new WebSocket(wsUrl);
      socket.onopen = () => {
        if (closedByEffect || !accessToken) {
          return;
        }
        socket?.send(buildNotificationAuthMessage(accessToken));
      };
      socket.onmessage = handleSocketMessage;
      socket.onclose = () => {
        if (closedByEffect) {
          return;
        }
        reconnectTimer = window.setTimeout(connect, 3_000);
      };
    };

    reconnectTimer = window.setTimeout(connect, 0);

    return () => {
      closedByEffect = true;
      if (reconnectTimer !== null) {
        window.clearTimeout(reconnectTimer);
      }
      if (socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) {
        socket.close();
      }
    };
  }, [handleSocketMessage, isAuthenticated, session?.accessToken]);

  const handleNotificationClick = (notification: NotificationItemVO) => {
    if (notification.actionUrl) {
      navigate(notification.actionUrl);
    }
    setIsOpen(false);
    if (notification.status === 'UNREAD') {
      void markReadMutation.mutateAsync(notification.id).catch(() => {
        // Keep navigation responsive even when mark-read fails.
      });
    }
  };

  const panel = isOpen && panelStyle
    ? createPortal(
      <div
        ref={panelRef}
        id="notifications-panel"
        role="dialog"
        aria-labelledby={panelTitleId}
        tabIndex={-1}
        style={{
          position: 'fixed',
          top: panelStyle.top,
          left: panelStyle.left,
          width: panelStyle.width,
          maxHeight: `calc(100dvh - ${panelStyle.top}px - max(0.5rem, env(safe-area-inset-bottom)))`,
          overflowY: 'auto',
        }}
        className="z-[140] rounded-xl border border-border-subtle bg-surface p-3 shadow-lg sm:p-4"
      >
        <div className="flex items-center justify-between gap-3">
          <div>
            <div id={panelTitleId} className="text-sm font-black text-slate-900 dark:text-white">{t('shell.notifications.title')}</div>
            <div aria-live="polite" className="mt-1 text-xs text-slate-400 dark:text-white/40">
              {t('shell.notifications.unreadCount', { count: unreadCount })}
            </div>
          </div>
          <button
            type="button"
            onClick={() => markAllReadMutation.mutate()}
            disabled={unreadCount === 0 || markAllReadMutation.isPending}
            className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-3 py-1.5 text-[11px] font-black uppercase tracking-[0.18em] text-slate-500 disabled:cursor-not-allowed disabled:opacity-40 dark:border-white/10 dark:text-white/55"
          >
            <CheckCheck size={14} />
            {t('shell.notifications.markAllRead')}
          </button>
        </div>

        {markAllReadMutation.isPending && (
          <FeedbackState
            kind="saving"
            compact
            className="mt-4 px-4 py-4"
            title={t('shell.notifications.markingAllReadTitle')}
            description={t('shell.notifications.markingAllReadDescription')}
          />
        )}
        {markAllReadMutation.isSuccess && !markAllReadMutation.isPending && (
          <FeedbackState
            kind="success"
            compact
            className="mt-4 px-4 py-4"
            title={t('shell.notifications.markedAllReadTitle')}
            description={t('shell.notifications.markedAllReadDescription')}
          />
        )}
        {markAllReadMutation.isError && (
          <FeedbackState
            kind="retry"
            compact
            className="mt-4 px-4 py-4"
            title={t('shell.notifications.markAllReadErrorTitle')}
            description={t('shell.notifications.markAllReadErrorDescription')}
            primaryAction={{
              label: t('shell.notifications.retry'),
              onClick: () => markAllReadMutation.mutate(),
            }}
          />
        )}

        <ul aria-live="polite" className="mt-4 max-h-[calc(100vh-10rem)] space-y-1 overflow-y-auto pr-1" aria-label={t('shell.notifications.title')}>
          {recentNotificationsQuery.isLoading ? (
            <li><FeedbackState
              kind="loading" compact className="px-4 py-5"
              title={t('shell.notifications.loadingTitle')} description={t('shell.notifications.loadingDescription')}
            /></li>
          ) : notificationErrorState ? (
            <li><FeedbackState
              kind={notificationErrorState.kind}
              compact
              className="px-4 py-5"
              title={notificationErrorState.title}
              description={notificationErrorState.description}
              impact={notificationErrorState.impact}
              nextStep={notificationErrorState.nextStep}
              primaryAction={{
                label: t('shell.notifications.retry'),
                onClick: () => void recentNotificationsQuery.refetch(),
              }}
            /></li>
          ) : notifications.length === 0 ? (
            <li><FeedbackState
              kind="empty"
              compact
              className="px-4 py-5"
              title={t('shell.notifications.emptyTitle')}
              description={t('shell.notifications.emptyDescription')}
            /></li>
          ) : (
            notifications.map((notification, index) => (
              <motion.li
                key={notification.id}
                initial={prefersReducedMotion ? false : { opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                transition={prefersReducedMotion ? { duration: 0 } : { duration: 0.16, delay: Math.min(index * 0.03, 0.12) }}
              >
                <button
                  type="button"
                  onClick={() => handleNotificationClick(notification)}
                  className={cn(
                  'w-full rounded-lg border px-3 py-2.5 text-left transition-colors',
                  notification.status === 'UNREAD'
                    ? 'border-slate-200/80 bg-slate-50/80 dark:border-white/10 dark:bg-white/5'
                    : 'border-transparent bg-transparent hover:border-slate-200/70 hover:bg-slate-50/70 dark:hover:border-white/10 dark:hover:bg-white/5'
                )}
                >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className={cn('w-2.5 h-2.5 rounded-full shrink-0', levelAccent(notification.level))} aria-hidden="true" />
                      <span className="truncate text-sm font-black text-slate-900 dark:text-white">{notification.title}</span>
                      <span className={cn('shrink-0 rounded-full border px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-[0.12em]', levelTone(notification.level))}>{levelLabel(notification.level, t)}</span>
                    </div>
                    <p className="mt-2 max-h-12 overflow-hidden text-sm leading-6 text-slate-500 dark:text-white/58">
                      {notification.content}
                    </p>
                    <div className="mt-2 text-[11px] uppercase tracking-[0.18em] text-slate-400 dark:text-white/30">
                      {formatNotificationTime(notification.createdAt, locale)}
                    </div>
                  </div>
                  {notification.status === 'UNREAD' && <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary shrink-0" />}
                </div>
                </button>
              </motion.li>
            ))
          )}
        </ul>
      </div>,
      document.body
    )
    : null;

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        aria-label={buttonLabel}
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        aria-controls="notifications-panel"
        onClick={() => setIsOpen((value) => !value)}
        title={buttonLabel}
        className="relative flex min-h-11 min-w-11 items-center justify-center rounded-lg border border-border-subtle p-2.5 hover:border-border-strong hover:bg-surface-sunken"
      >
        <Bell size={20} className="text-slate-500 dark:text-white/70" />
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 min-w-5 h-5 px-1 rounded-full bg-rose-500 text-white text-[10px] font-black flex items-center justify-center">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>
      {panel}
    </>
  );
};
