const CUSTOM_CURSOR_ATTRIBUTE = 'data-custom-cursor';
const CUSTOM_CURSOR_ENABLED_VALUE = 'enabled';

function getRootElement(): HTMLElement | null {
  if (typeof document === 'undefined') {
    return null;
  }

  return document.documentElement;
}

export function canUseCustomCursor(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false;
  }

  const hasFinePointer = window.matchMedia('(hover: hover) and (pointer: fine)').matches;
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  return hasFinePointer && !prefersReducedMotion;
}

export function setCustomCursorEnabled(enabled: boolean): void {
  const root = getRootElement();
  if (!root) {
    return;
  }

  if (enabled && canUseCustomCursor()) {
    root.setAttribute(CUSTOM_CURSOR_ATTRIBUTE, CUSTOM_CURSOR_ENABLED_VALUE);
    return;
  }

  root.removeAttribute(CUSTOM_CURSOR_ATTRIBUTE);
}
