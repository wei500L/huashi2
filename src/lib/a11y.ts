import React from 'react';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

let scrollLockCount = 0;
let previousBodyOverflow = '';

function getFocusableElements(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
    (element) => !element.hasAttribute('disabled') && element.getAttribute('aria-hidden') !== 'true'
  );
}

export function useBodyScrollLock(locked: boolean): void {
  React.useEffect(() => {
    if (!locked) {
      return;
    }

    if (scrollLockCount === 0) {
      previousBodyOverflow = window.document.body.style.overflow;
      window.document.body.style.overflow = 'hidden';
    }
    scrollLockCount += 1;

    return () => {
      scrollLockCount = Math.max(0, scrollLockCount - 1);
      if (scrollLockCount === 0) {
        window.document.body.style.overflow = previousBodyOverflow;
      }
    };
  }, [locked]);
}

export function useDialogAccessibility({
  open,
  containerRef,
  initialFocusRef,
  onClose,
}: {
  open: boolean;
  containerRef: React.RefObject<HTMLElement | null>;
  initialFocusRef?: React.RefObject<HTMLElement | null>;
  onClose: () => void;
}): void {
  React.useEffect(() => {
    if (!open) {
      return;
    }

    const previousActiveElement = window.document.activeElement instanceof HTMLElement
      ? window.document.activeElement
      : null;

    const focusFrame = window.requestAnimationFrame(() => {
      const container = containerRef.current;
      if (!container) {
        return;
      }
      const fallbackTarget = getFocusableElements(container)[0] ?? container;
      (initialFocusRef?.current ?? fallbackTarget).focus();
    });

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }

      if (event.key !== 'Tab') {
        return;
      }

      const container = containerRef.current;
      if (!container) {
        return;
      }

      const focusable = getFocusableElements(container);
      if (focusable.length === 0) {
        event.preventDefault();
        container.focus();
        return;
      }

      const firstElement = focusable[0];
      const lastElement = focusable[focusable.length - 1];
      const activeElement = window.document.activeElement;

      if (event.shiftKey) {
        if (activeElement === firstElement || activeElement === container) {
          event.preventDefault();
          lastElement.focus();
        }
        return;
      }

      if (activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      }
    };

    window.document.addEventListener('keydown', handleKeyDown);

    return () => {
      window.cancelAnimationFrame(focusFrame);
      window.document.removeEventListener('keydown', handleKeyDown);
      if (previousActiveElement?.isConnected) {
        previousActiveElement.focus();
      }
    };
  }, [containerRef, initialFocusRef, onClose, open]);
}
