import React from 'react';
import { useBeforeUnload, useBlocker } from 'react-router';

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

  const blocker = useBlocker(() => active && !allowNavigationRef.current);

  React.useEffect(() => {
    if (blocker.state !== 'blocked') {
      return;
    }
    const shouldLeave = window.confirm(leaveConfirm);
    if (!shouldLeave) {
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
        // Leave protection callbacks surface their own UI state.
      }
      blocker.reset();
    })();
  }, [allowNavigation, blocker, leaveConfirm, onRouteLeave]);

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
