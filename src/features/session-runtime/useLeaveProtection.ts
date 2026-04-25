import React from 'react';
import { useBeforeUnload, useBlocker } from 'react-router';

type Blocker = {
  state: string;
  proceed: () => void;
  reset: () => void;
};

function useOptionalBlocker(shouldBlock: () => boolean): Blocker | null {
  try {
    return useBlocker(shouldBlock) as Blocker;
  } catch {
    return null;
  }
}

type LeaveProtectionOptions = {
  active: boolean;
  leaveConfirm: string;
  onRouteLeave: () => Promise<boolean>;
  onBackgroundPersist?: () => Promise<void>;
};

export function useLeaveProtection({
  active,
  leaveConfirm,
  onRouteLeave,
  onBackgroundPersist,
}: LeaveProtectionOptions) {
  const allowNavigationRef = React.useRef(false);

  const allowNavigation = React.useCallback(<T,>(callback: () => T): T => {
    allowNavigationRef.current = true;
    try {
      return callback();
    } finally {
      window.setTimeout(() => {
        allowNavigationRef.current = false;
      }, 0);
    }
  }, []);

  const blocker = useOptionalBlocker(() => active && !allowNavigationRef.current);

  React.useEffect(() => {
    if (!blocker || blocker.state !== 'blocked') {
      return;
    }
    if (!window.confirm(leaveConfirm)) {
      blocker.reset();
      return;
    }
    void (async () => {
      try {
        if (await onRouteLeave()) {
          allowNavigation(() => blocker.proceed());
          return;
        }
      } catch {
        // Save failures are surfaced by the owning page.
      }
      blocker.reset();
    })();
  }, [allowNavigation, blocker, leaveConfirm, onRouteLeave]);

  React.useEffect(() => {
    if (!active || blocker) {
      return;
    }

    const onClick = (event: MouseEvent) => {
      if (allowNavigationRef.current || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
      }
      const target = event.target instanceof Element ? event.target.closest('a[href]') : null;
      if (!(target instanceof HTMLAnchorElement) || target.target || target.download) {
        return;
      }
      const nextUrl = new URL(target.href, window.location.href);
      if (nextUrl.origin !== window.location.origin || nextUrl.pathname === window.location.pathname) {
        return;
      }
      event.preventDefault();
      if (!window.confirm(leaveConfirm)) {
        return;
      }
      void (async () => {
        try {
          if (await onRouteLeave()) {
            allowNavigation(() => window.location.assign(nextUrl.pathname + nextUrl.search + nextUrl.hash));
          }
        } catch {
          // Save failures are surfaced by the owning page.
        }
      })();
    };

    document.addEventListener('click', onClick, true);
    return () => document.removeEventListener('click', onClick, true);
  }, [active, allowNavigation, blocker, leaveConfirm, onRouteLeave]);

  useBeforeUnload(
    React.useCallback(
      (event) => {
        if (!active || allowNavigationRef.current) {
          return;
        }
        event.preventDefault();
        event.returnValue = leaveConfirm;
        void onBackgroundPersist?.().catch(() => undefined);
      },
      [active, leaveConfirm, onBackgroundPersist]
    )
  );

  React.useEffect(() => {
    if (!active || !onBackgroundPersist) {
      return;
    }
    const onVisibilityChange = () => {
      if (document.hidden) {
        void onBackgroundPersist().catch(() => undefined);
      }
    };
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [active, onBackgroundPersist]);

  return {
    allowNavigation,
  };
}
