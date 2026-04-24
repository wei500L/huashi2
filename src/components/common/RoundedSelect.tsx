import React from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

export type RoundedSelectOption = {
  value: string;
  label: string;
};

type RoundedSelectProps = {
  value: string;
  options: RoundedSelectOption[];
  placeholder?: string;
  className?: string;
  onChange: (value: string) => void;
};

export const RoundedSelect: React.FC<RoundedSelectProps> = ({
  value,
  options,
  placeholder = '--',
  className,
  onChange,
}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [menuStyle, setMenuStyle] = React.useState<React.CSSProperties | null>(null);
  const containerRef = React.useRef<HTMLDivElement | null>(null);
  const menuRef = React.useRef<HTMLDivElement | null>(null);
  const listboxId = React.useId();
  const selectedOption = options.find((option) => option.value === value);

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

  React.useLayoutEffect(() => {
    if (isOpen) {
      updateMenuPosition();
    }
  }, [isOpen, updateMenuPosition]);

  React.useEffect(() => {
    if (!isOpen) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!containerRef.current?.contains(target) && !menuRef.current?.contains(target)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
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
  }, [isOpen, updateMenuPosition]);

  return (
    <div ref={containerRef} className={cn('relative', className)}>
      <button
        type="button"
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-controls={listboxId}
        onClick={() => setIsOpen((current) => !current)}
        className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-white/75 px-4 py-3 text-left outline-none transition focus:border-primary/50 dark:border-white/10 dark:bg-slate-950/40 dark:focus:border-primary/50 dark:focus:shadow-[0_0_0_1px_rgba(139,92,246,0.22)]"
      >
        <span className={selectedOption ? 'text-slate-900 dark:text-white' : 'text-slate-400 dark:text-white/55'}>
          {selectedOption?.label ?? placeholder}
        </span>
        <ChevronDown size={18} className={cn('text-slate-500 transition dark:text-white/80', isOpen && 'rotate-180')} />
      </button>

      {isOpen && menuStyle && createPortal(
        <div
          ref={menuRef}
          id={listboxId}
          role="listbox"
          style={menuStyle}
          className="fixed z-[100] overflow-hidden rounded-2xl border border-slate-200/80 bg-white/95 p-1 shadow-[0_24px_80px_rgba(15,23,42,0.18)] backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/95"
        >
          {options.map((option) => {
            const isSelected = option.value === value;
            return (
              <button
                key={option.value}
                type="button"
                role="option"
                aria-selected={isSelected}
                onClick={() => {
                  onChange(option.value);
                  setIsOpen(false);
                }}
                className={cn(
                  'block w-full rounded-xl px-4 py-2.5 text-left text-sm font-semibold transition',
                  isSelected
                    ? 'bg-primary/15 text-primary dark:bg-primary/20 dark:text-white'
                    : 'text-slate-700 hover:bg-slate-100 dark:text-white/85 dark:hover:bg-white/8',
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
