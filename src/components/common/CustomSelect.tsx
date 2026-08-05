import React from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

export type CustomSelectOption = {
  value: string;
  label: string;
};

type CustomSelectProps = {
  value: string | number | boolean;
  options: CustomSelectOption[];
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
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
  className,
  triggerClassName,
  menuClassName,
}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [isMounted, setIsMounted] = React.useState(false);
  const [menuStyle, setMenuStyle] = React.useState<React.CSSProperties | null>(null);
  const containerRef = React.useRef<HTMLDivElement | null>(null);
  const menuRef = React.useRef<HTMLDivElement | null>(null);
  const listboxId = React.useId();
  const selectedOption = options.find((option) => option.value === String(value));

  const updateMenuPosition = React.useCallback(() => {
    const triggerElement = containerRef.current;
    if (!triggerElement) {
      return;
    }

    const rect = triggerElement.getBoundingClientRect();
    setMenuStyle({
      left: rect.left,
      top: rect.bottom + 8,
      width: rect.width,
    });
  }, []);

  const closeMenu = React.useCallback(() => {
    setIsOpen(false);
  }, []);

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
        closeMenu();
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

  return (
    <div ref={containerRef} className={cn('relative', className)}>
      <button
        type="button"
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-controls={listboxId}
        disabled={disabled}
        onClick={() => setIsOpen((current) => !current)}
        className={cn(
          'flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-left text-sm outline-none transition-[border-color,box-shadow,background-color] duration-200 ease-out focus:border-primary/40 disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:bg-slate-950/45 dark:focus:border-primary/50 dark:focus:shadow-[0_0_0_1px_hsl(var(--focus)/0.24)]',
          triggerClassName,
        )}
      >
        <span className={cn('truncate', selectedOption ? 'text-slate-900 dark:text-white/90' : 'text-slate-400 dark:text-white/55')}>
          {selectedOption?.label ?? placeholder}
        </span>
        <ChevronDown
          size={18}
          className={cn('ml-3 shrink-0 text-slate-500 transition-transform duration-200 ease-out dark:text-white/80', isOpen && 'rotate-180')}
        />
      </button>

      {isMounted && menuStyle && createPortal(
        <div
          ref={menuRef}
          id={listboxId}
          role="listbox"
          style={menuStyle}
          className={cn(
            'fixed z-[100] max-h-72 overflow-y-auto rounded-[1.35rem] border border-slate-200/80 bg-white/95 p-1 shadow-[0_24px_80px_rgba(15,23,42,0.18)] backdrop-blur-xl transition-[opacity,transform] duration-150 ease-out dark:border-white/10 dark:bg-slate-950/95',
            isOpen ? 'translate-y-0 scale-100 opacity-100' : '-translate-y-1 scale-[0.98] opacity-0',
            menuClassName,
          )}
        >
          {options.map((option) => {
            const isSelected = option.value === String(value);
            return (
              <button
                key={option.value}
                type="button"
                role="option"
                aria-selected={isSelected}
                onClick={() => {
                  onChange(option.value);
                  closeMenu();
                }}
                className={cn(
                  'block w-full rounded-[1rem] px-4 py-2.5 text-left text-sm font-semibold transition-colors duration-150 ease-out',
                  isSelected
                    ? 'bg-primary/15 text-primary dark:bg-primary/20 dark:text-white'
                    : 'text-slate-700 hover:bg-slate-100 dark:text-white/85 dark:hover:bg-white/10',
                )}
              >
                {option.label}
              </button>
            );
          })}
        </div>,
        document.body,
      )}
    </div>
  );
};
