import React from 'react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';

export type TabItem<T extends string = string> = {
  id: T;
  label: React.ReactNode;
  description?: React.ReactNode;
  badge?: React.ReactNode;
  href?: string;
  disabled?: boolean;
};

type TabsProps<T extends string> = {
  items: TabItem<T>[];
  value?: T;
  defaultValue?: T;
  onChange?: (value: T) => void;
  variant?: 'page' | 'local';
  ariaLabel?: string;
  className?: string;
};

/**
 * Page tabs use a nav/list and can be links; local tabs use a controlled tablist.
 * Both keep the active state visible to assistive technology.
 */
export function Tabs<T extends string>({
  items,
  value,
  defaultValue,
  onChange,
  variant = 'local',
  ariaLabel,
  className,
}: TabsProps<T>) {
  const [uncontrolledValue, setUncontrolledValue] = React.useState<T | undefined>(defaultValue ?? items[0]?.id);
  const activeValue = value ?? uncontrolledValue;
  const isPage = variant === 'page';

  const select = (next: T) => {
    if (value === undefined) setUncontrolledValue(next);
    onChange?.(next);
  };
  const handleKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (isPage || !items.length) return;
    const available = items.map((item, itemIndex) => ({ item, itemIndex })).filter(({ item }) => !item.disabled);
    const currentIndex = available.findIndex(({ item }) => item.id === items[index]?.id);
    if (currentIndex < 0) return;
    const nextIndex = event.key === 'ArrowRight' || event.key === 'ArrowDown'
      ? (currentIndex + 1) % available.length
      : event.key === 'ArrowLeft' || event.key === 'ArrowUp'
        ? (currentIndex - 1 + available.length) % available.length
        : event.key === 'Home'
          ? 0
          : event.key === 'End'
            ? available.length - 1
            : -1;
    if (nextIndex < 0) return;
    event.preventDefault();
    const next = available[nextIndex];
    select(next.item.id);
    requestAnimationFrame(() => {
      document.getElementById(`tab-${next.item.id}`)?.focus();
    });
  };

  return (
    <nav aria-label={ariaLabel} className={cn(isPage ? 'page-tabs' : 'local-tabs', className)}>
      {isPage ? (
        <ul className="flex min-w-max items-center gap-1" role="list">
          {items.map((item) => (
            <li key={item.id}>
              {item.href ? (
                <Link
                  to={item.href}
                  aria-current={activeValue === item.id ? 'page' : undefined}
                  className={cn('tab-trigger', activeValue === item.id && 'tab-trigger-active')}
                >
                  {item.label}
                  {item.badge}
                </Link>
              ) : (
                <button
                  type="button"
                  disabled={item.disabled}
                  aria-current={activeValue === item.id ? 'page' : undefined}
                  onClick={() => select(item.id)}
                  className={cn('tab-trigger', activeValue === item.id && 'tab-trigger-active')}
                >
                  {item.label}
                  {item.badge}
                </button>
              )}
            </li>
          ))}
        </ul>
      ) : (
        <div role="tablist" aria-label={ariaLabel} className="flex min-w-max items-center gap-1">
          {items.map((item) => {
            const selected = activeValue === item.id;
            return (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={selected}
                aria-controls={`tabpanel-${item.id}`}
                id={`tab-${item.id}`}
                tabIndex={selected ? 0 : -1}
                disabled={item.disabled}
                onClick={() => select(item.id)}
                onKeyDown={(event) => handleKeyDown(event, items.indexOf(item))}
                className={cn('tab-trigger', selected && 'tab-trigger-active')}
              >
                <span className="flex min-w-0 flex-col items-start">
                  <span>{item.label}</span>
                  {item.description ? <span className="tab-description">{item.description}</span> : null}
                </span>
                {item.badge}
              </button>
            );
          })}
        </div>
      )}
    </nav>
  );
}

export type { TabsProps };
