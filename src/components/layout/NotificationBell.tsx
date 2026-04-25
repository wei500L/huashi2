import React, { useEffect, useEffectEvent, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell, CheckCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { NotificationItemVO, NotificationSocketMessage, PageResult } from '@/lib/contracts';
import { buildNotificationWebSocketUrl } from '@/lib/notifications';
import { notificationService } from '@/lib/services';
import { cn } from '@/lib/utils';
import { useAuthStore, useUIStore } from '@/store';

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

export const NotificationBell: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const buttonRef = useRef<HTMLButtonElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const session = useAuthStore((state) => state.session);
  const authStatus = useAuthStore((state) => state.status);
  const locale = useUIStore((state) => state.locale);
  const [isOpen, setIsOpen] = useState(false);
  const [panelStyle, setPanelStyle] = useState<{ top: number; left: number; width: number } | null>(null);
  const isAuthenticated = authStatus === 'authenticated' && Boolean(session?.accessToken);

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
    const wsUrl = accessToken ? buildNotificationWebSocketUrl(accessToken) : null;
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
        style={{
          position: 'fixed',
          top: panelStyle.top,
          left: panelStyle.left,
          width: panelStyle.width,
        }}
        className="z-[140] rounded-[1.8rem] border border-slate-200/80 bg-white/92 p-4 shadow-[0_24px_80px_rgba(15,23,42,0.18)] backdrop-blur-2xl dark:border-white/10 dark:bg-slate-950/92"
      >
        <div className="flex items-center justify-between gap-3">
          <div>
            <div className="text-sm font-black text-slate-900 dark:text-white">{t('shell.notifications.title')}</div>
            <div className="mt-1 text-xs text-slate-400 dark:text-white/40">
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

        <div className="mt-4 space-y-2">
          {recentNotificationsQuery.isLoading ? (
            <div className="rounded-2xl border border-slate-200/70 px-4 py-6 text-sm text-slate-400 dark:border-white/10 dark:text-white/40">
              {t('shell.notifications.loading')}
            </div>
          ) : notifications.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-200/80 px-4 py-8 text-center text-sm text-slate-400 dark:border-white/10 dark:text-white/35">
              {t('shell.notifications.empty')}
            </div>
          ) : (
            notifications.map((notification) => (
              <button
                key={notification.id}
                type="button"
                onClick={() => handleNotificationClick(notification)}
                className={cn(
                  'w-full rounded-[1.35rem] border px-4 py-3 text-left transition-all',
                  notification.status === 'UNREAD'
                    ? 'border-slate-200/80 bg-slate-50/80 dark:border-white/10 dark:bg-white/5'
                    : 'border-transparent bg-transparent hover:border-slate-200/70 hover:bg-slate-50/70 dark:hover:border-white/10 dark:hover:bg-white/5'
                )}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className={cn('w-2.5 h-2.5 rounded-full shrink-0', levelAccent(notification.level))} />
                      <span className="truncate text-sm font-black text-slate-900 dark:text-white">{notification.title}</span>
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
            ))
          )}
        </div>
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
        onClick={() => setIsOpen((value) => !value)}
        className="relative p-2.5 hover:bg-black/5 dark:hover:bg-white/5 rounded-full transition-all border border-transparent hover:border-slate-200 dark:hover:border-white/10"
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
