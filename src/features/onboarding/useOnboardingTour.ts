import React from 'react';
import {
  buildOnboardingStorageKey,
  clearOnboardingTourSeen,
  hasSeenOnboardingTour,
  markOnboardingTourSeen,
} from './storage';

type UseOnboardingTourOptions = {
  tourId: string;
  userId?: number | string | null;
  enabled?: boolean;
  autoStartDelayMs?: number;
};

type UseOnboardingTourResult = {
  isOpen: boolean;
  start: () => void;
  complete: () => void;
  reset: () => void;
  storageKey: string | null;
};

export function useOnboardingTour({
  tourId,
  userId,
  enabled = true,
  autoStartDelayMs = 240,
}: UseOnboardingTourOptions): UseOnboardingTourResult {
  const storageKey = React.useMemo(
    () => (userId === null || userId === undefined ? null : buildOnboardingStorageKey(tourId, userId)),
    [tourId, userId]
  );
  const [isOpen, setIsOpen] = React.useState(false);

  React.useEffect(() => {
    if (!enabled || !storageKey || hasSeenOnboardingTour(storageKey)) {
      setIsOpen(false);
      return;
    }

    const timer = window.setTimeout(() => setIsOpen(true), autoStartDelayMs);
    return () => window.clearTimeout(timer);
  }, [autoStartDelayMs, enabled, storageKey]);

  const start = React.useCallback(() => {
    if (!enabled || !storageKey) {
      return;
    }
    setIsOpen(true);
  }, [enabled, storageKey]);

  const complete = React.useCallback(() => {
    markOnboardingTourSeen(storageKey);
    setIsOpen(false);
  }, [storageKey]);

  const reset = React.useCallback(() => {
    clearOnboardingTourSeen(storageKey);
    if (enabled && storageKey) {
      setIsOpen(true);
    }
  }, [enabled, storageKey]);

  return {
    isOpen,
    start,
    complete,
    reset,
    storageKey,
  };
}
