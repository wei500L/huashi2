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
let previousBodyOverscrollBehavior = '';
let previousBodyPaddingRight = '';

function getFocusableElements(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
    (element) => !element.hasAttribute('disabled') && element.getAttribute('aria-hidden') !== 'true'
  );
}

function getScrollbarCompensation(): string {
  const scrollbarWidth = Math.max(0, window.innerWidth - window.document.documentElement.clientWidth);
  return scrollbarWidth > 0 ? `${scrollbarWidth}px` : '';
}

export function useBodyScrollLock(locked: boolean): void {
  React.useEffect(() => {
    if (!locked) {
      return;
    }

    if (scrollLockCount === 0) {
      previousBodyOverflow = window.document.body.style.overflow;
      previousBodyOverscrollBehavior = window.document.body.style.overscrollBehavior;
      previousBodyPaddingRight = window.document.body.style.paddingRight;
      const compensation = getScrollbarCompensation();
      window.document.body.style.overflow = 'hidden';
      window.document.body.style.overscrollBehavior = 'none';
      // Keep layout width stable when the vertical scrollbar disappears.
      if (compensation) {
        window.document.body.style.paddingRight = compensation;
      }
    }
    scrollLockCount += 1;

    return () => {
      scrollLockCount = Math.max(0, scrollLockCount - 1);
      if (scrollLockCount === 0) {
        window.document.body.style.overflow = previousBodyOverflow;
        window.document.body.style.overscrollBehavior = previousBodyOverscrollBehavior;
        window.document.body.style.paddingRight = previousBodyPaddingRight;
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
