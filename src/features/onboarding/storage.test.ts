import { describe, expect, it } from 'vitest';
import {
  buildOnboardingStorageKey,
  clearOnboardingTourSeen,
  hasSeenOnboardingTour,
  markOnboardingTourSeen,
} from './storage';

describe('onboarding storage helpers', () => {
  it('builds a stable storage key per tour and user', () => {
    expect(buildOnboardingStorageKey('student-dashboard', 12)).toBe('ef-onboarding-seen:v1:student-dashboard:12');
  });

  it('marks and clears seen state in localStorage', () => {
    const key = buildOnboardingStorageKey('admin-dashboard', 5);

    expect(hasSeenOnboardingTour(key)).toBe(false);

    markOnboardingTourSeen(key);
    expect(hasSeenOnboardingTour(key)).toBe(true);

    clearOnboardingTourSeen(key);
    expect(hasSeenOnboardingTour(key)).toBe(false);
  });
});
