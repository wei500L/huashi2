import React from 'react';
import { createPortal } from 'react-dom';
import { Check, ChevronDown, LoaderCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

export type CustomSelectOption = {
  value: string;
  label: string;
};

export type CustomSelectProps = {
  value: string | number | boolean;
  options: CustomSelectOption[];
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  loading?: boolean;
  validationState?: 'default' | 'invalid' | 'success';
  id?: string;
  ariaLabel?: string;
  ariaDescribedBy?: string;
  className?: string;
  triggerClassName?: string;
  menuClassName?: string;
};

export const CustomSelect: React.FC<CustomSelectProps> = ({
  value,
  options,
  onChange,
  placeholder = '--',
  disabled = false,
  loading = false,
  validationState = 'default',
  id,
  ariaLabel,
  ariaDescribedBy,
  className,
  triggerClassName,
  menuClassName,
}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [isMounted, setIsMounted] = React.useState(false);
  const [activeIndex, setActiveIndex] = React.useState(-1);
  const [menuStyle, setMenuStyle] = React.useState<React.CSSProperties | null>(null);
  const containerRef = React.useRef<HTMLDivElement | null>(null);
  const menuRef = React.useRef<HTMLDivElement | null>(null);
  const triggerRef = React.useRef<HTMLButtonElement | null>(null);
  const optionRefs = React.useRef<Array<HTMLButtonElement | null>>([]);
  const listboxId = React.useId();
  const selectedOption = options.find((option) => option.value === String(value));
  const selectedIndex = options.findIndex((option) => option.value === String(value));
  const isUnavailable = disabled || loading;

  const updateMenuPosition = React.useCallback(() => {
    const triggerElement = containerRef.current;
    if (!triggerElement) {
      return;
    }

    const rect = triggerElement.getBoundingClientRect();
    const viewportPadding = 8;
    const preferredMaxHeight = Math.min(288, Math.max(120, window.innerHeight - rect.bottom - viewportPadding));
    const shouldOpenAbove = rect.bottom + preferredMaxHeight > window.innerHeight - viewportPadding && rect.top > preferredMaxHeight;
    const maxHeight = Math.min(288, Math.max(120, shouldOpenAbove ? rect.top - viewportPadding * 2 : window.innerHeight - rect.bottom - viewportPadding));
    setMenuStyle({
      left: Math.min(Math.max(viewportPadding, rect.left), Math.max(viewportPadding, window.innerWidth - rect.width - viewportPadding)),
      top: shouldOpenAbove ? Math.max(viewportPadding, rect.top - maxHeight - 8) : rect.bottom + 8,
      width: rect.width,
      maxHeight,
    });
  }, []);

  const closeMenu = React.useCallback((restoreFocus = false) => {
    setIsOpen(false);
    if (restoreFocus) {
      window.requestAnimationFrame(() => triggerRef.current?.focus());
    }
  }, []);

  const openMenu = React.useCallback((target: 'selected' | 'first' | 'last' = 'selected') => {
    if (isUnavailable || options.length === 0) {
      return;
    }

    const nextIndex = target === 'first'
      ? 0
      : target === 'last'
        ? options.length - 1
        : Math.max(0, selectedIndex);
    setActiveIndex(nextIndex);
    setIsOpen(true);
  }, [isUnavailable, options.length, selectedIndex]);

  const selectOption = React.useCallback((index: number) => {
    const option = options[index];
    if (!option) {
      return;
    }
    onChange(option.value);
    closeMenu(true);
  }, [closeMenu, onChange, options]);

  React.useLayoutEffect(() => {
    if (!isOpen) {
      return;
    }

    setIsMounted(true);
    updateMenuPosition();
  }, [isOpen, updateMenuPosition]);

  React.useEffect(() => {
    if (isOpen) {
      return;
    }

    const timeoutId = window.setTimeout(() => setIsMounted(false), 160);
    return () => window.clearTimeout(timeoutId);
  }, [isOpen]);

  React.useEffect(() => {
    if (!isOpen) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!containerRef.current?.contains(target) && !menuRef.current?.contains(target)) {
        closeMenu();
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        closeMenu(true);
      }
    };

    document.addEventListener('pointerdown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    window.addEventListener('resize', updateMenuPosition);
    window.addEventListener('scroll', updateMenuPosition, true);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('resize', updateMenuPosition);
      window.removeEventListener('scroll', updateMenuPosition, true);
    };
  }, [closeMenu, isOpen, updateMenuPosition]);

  React.useEffect(() => {
    if (!isOpen || !isMounted || activeIndex < 0) {
      return;
    }
    window.requestAnimationFrame(() => optionRefs.current[activeIndex]?.focus());
  }, [activeIndex, isMounted, isOpen]);

  React.useEffect(() => {
    if (isUnavailable && isOpen) {
      closeMenu();
    }
  }, [closeMenu, isOpen, isUnavailable]);

  const moveOptionFocus = (nextIndex: number) => {
    const boundedIndex = (nextIndex + options.length) % options.length;
    setActiveIndex(boundedIndex);
    optionRefs.current[boundedIndex]?.focus();
  };

  const handleTriggerKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      openMenu('selected');
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      openMenu('last');
    } else if (event.key === 'Home') {
      event.preventDefault();
      openMenu('first');
    } else if (event.key === 'End') {
      event.preventDefault();
      openMenu('last');
    }
  };

  const handleOptionKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      moveOptionFocus(index + 1);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      moveOptionFocus(index - 1);
    } else if (event.key === 'Home') {
      event.preventDefault();
      moveOptionFocus(0);
    } else if (event.key === 'End') {
      event.preventDefault();
      moveOptionFocus(options.length - 1);
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      selectOption(index);
    } else if (event.key === 'Tab') {
      closeMenu();
    }
  };

  return (
    <div ref={containerRef} className={cn('relative', className)}>
      <button
        ref={triggerRef}
        id={id}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-controls={listboxId}
        aria-label={ariaLabel}
        aria-describedby={ariaDescribedBy}
        aria-invalid={validationState === 'invalid' || undefined}
        aria-busy={loading || undefined}
        data-state={validationState}
        disabled={isUnavailable}
        onKeyDown={handleTriggerKeyDown}
        onClick={() => (isOpen ? closeMenu() : openMenu('selected'))}
        className={cn(
          'surface-control flex min-h-12 w-full items-center justify-between px-4 py-3 text-left text-sm outline-none active:bg-surface-sunken',
          triggerClassName,
        )}
      >
        <span className={cn('truncate', selectedOption ? 'text-slate-900 dark:text-white/90' : 'text-slate-400 dark:text-white/55')}>
          {selectedOption?.label ?? placeholder}
        </span>
        {loading ? (
          <LoaderCircle size={18} className="ml-3 shrink-0 animate-pulse text-muted" aria-hidden="true" />
        ) : (
          <ChevronDown
            size={18}
            className={cn('ml-3 shrink-0 text-muted transition-transform duration-200 ease-out', isOpen && 'rotate-180')}
          />
        )}
      </button>

      {isMounted && menuStyle && createPortal(
        <div
          ref={menuRef}
          id={listboxId}
          role="listbox"
          aria-label={ariaLabel}
          style={menuStyle}
          className={cn(
            'fixed z-[100] max-h-72 overflow-y-auto rounded-[1.35rem] border border-slate-200/80 bg-white/95 p-1 shadow-[0_24px_80px_rgba(15,23,42,0.18)] backdrop-blur-xl transition-[opacity,transform] duration-150 ease-out dark:border-white/10 dark:bg-slate-950/95',
            isOpen ? 'translate-y-0 scale-100 opacity-100' : '-translate-y-1 scale-[0.98] opacity-0',
            menuClassName,
          )}
        >
          {options.map((option, index) => {
            const isSelected = option.value === String(value);
            return (
              <button
                key={option.value}
                ref={(element) => { optionRefs.current[index] = element; }}
                type="button"
                role="option"
                aria-selected={isSelected}
                tabIndex={index === activeIndex ? 0 : -1}
                onFocus={() => setActiveIndex(index)}
                onKeyDown={(event) => handleOptionKeyDown(event, index)}
                onClick={() => selectOption(index)}
                className={cn(
                  'flex w-full items-center justify-between gap-3 rounded-[1rem] px-4 py-2.5 text-left text-sm font-semibold transition-colors duration-150 ease-out',
                  isSelected
                    ? 'bg-surface-sunken text-foreground'
                    : 'text-muted hover:bg-surface-raised hover:text-foreground',
                )}
              >
                <span>{option.label}</span>
                {isSelected ? <Check size={16} className="shrink-0 text-foreground" aria-hidden="true" /> : null}
              </button>
            );
          })}
        </div>,
        document.body,
      )}
    </div>
  );
};
