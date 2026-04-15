const ONBOARDING_STORAGE_PREFIX = 'ef-onboarding-seen:v1';

export function buildOnboardingStorageKey(tourId: string, userId: number | string): string {
  return `${ONBOARDING_STORAGE_PREFIX}:${tourId}:${userId}`;
}

export function hasSeenOnboardingTour(storageKey: string | null): boolean {
  if (!storageKey || typeof window === 'undefined') {
    return false;
  }
  return window.localStorage.getItem(storageKey) === '1';
}

export function markOnboardingTourSeen(storageKey: string | null): void {
  if (!storageKey || typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(storageKey, '1');
}

export function clearOnboardingTourSeen(storageKey: string | null): void {
  if (!storageKey || typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(storageKey);
}
