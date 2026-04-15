import React from 'react';
import { useDeferredValue } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { LexicalPairSuggestionVO } from '@/lib/contracts';
import { lexicalPairService } from '@/lib/services';
import { lexicalPairTypeLabel } from '@/lib/format';
import { cn } from '@/lib/utils';

function matchedByLabel(matchedBy: string, t: ReturnType<typeof useTranslation>['t']) {
  switch (matchedBy) {
    case 'ENGLISH_WORD':
    case 'ENGLISH_PREFIX':
      return t('ui.lexicalSearch.matchedBy.english');
    case 'FRENCH_WORD':
    case 'FRENCH_PREFIX':
      return t('ui.lexicalSearch.matchedBy.french');
    case 'CHINESE_GLOSS':
      return t('ui.lexicalSearch.matchedBy.chinese');
    case 'PINYIN':
      return t('ui.lexicalSearch.matchedBy.pinyin');
    case 'INITIALS':
      return t('ui.lexicalSearch.matchedBy.initials');
    default:
      return t('ui.lexicalSearch.matchedBy.keyword');
  }
}

export interface LexicalPairSuggestionInputProps {
  value: string;
  onChange: (value: string) => void;
  onSuggestionSelect: (suggestion: LexicalPairSuggestionVO) => void;
  placeholder: string;
  active?: boolean;
  disabled?: boolean;
  className?: string;
  inputClassName?: string;
}

export const LexicalPairSuggestionInput: React.FC<LexicalPairSuggestionInputProps> = ({
  value,
  onChange,
  onSuggestionSelect,
  placeholder,
  active,
  disabled,
  className,
  inputClassName,
}) => {
  const { t } = useTranslation();
  const deferredKeyword = useDeferredValue(value.trim());
  const [open, setOpen] = React.useState(false);
  const [activeIndex, setActiveIndex] = React.useState(0);

  const suggestionsQuery = useQuery({
    queryKey: ['lexical-pair-suggestions', deferredKeyword, active],
    queryFn: ({ signal }) =>
      lexicalPairService.suggest(
        {
          keyword: deferredKeyword,
          limit: 6,
          active,
        },
        { signal }
      ),
    enabled: open && deferredKeyword.length >= 2,
    staleTime: 60_000,
  });

  const suggestions = suggestionsQuery.data || [];

  React.useEffect(() => {
    setActiveIndex(0);
  }, [deferredKeyword, suggestions.length]);

  const commitSelection = React.useCallback(
    (suggestion: LexicalPairSuggestionVO) => {
      onSuggestionSelect(suggestion);
      setOpen(false);
    },
    [onSuggestionSelect]
  );

  return (
    <div className={cn('relative', className)}>
      <Search size={16} className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-white/30" />
      <input
        value={value}
        disabled={disabled}
        onFocus={() => setOpen(true)}
        onBlur={() => window.setTimeout(() => setOpen(false), 120)}
        onChange={(event) => {
          onChange(event.target.value);
          if (!open) {
            setOpen(true);
          }
        }}
        onKeyDown={(event) => {
          if (!open || suggestions.length === 0) {
            return;
          }
          if (event.key === 'ArrowDown') {
            event.preventDefault();
            setActiveIndex((current) => (current + 1) % suggestions.length);
            return;
          }
          if (event.key === 'ArrowUp') {
            event.preventDefault();
            setActiveIndex((current) => (current - 1 + suggestions.length) % suggestions.length);
            return;
          }
          if (event.key === 'Enter') {
            event.preventDefault();
            const suggestion = suggestions[activeIndex];
            if (suggestion) {
              commitSelection(suggestion);
            }
            return;
          }
          if (event.key === 'Escape') {
            setOpen(false);
          }
        }}
        className={cn(
          'w-full rounded-2xl border border-slate-200 bg-white/70 py-3 pl-11 pr-4 dark:border-white/10 dark:bg-white/5',
          inputClassName
        )}
        placeholder={placeholder}
      />

      {open && deferredKeyword.length >= 2 && (
        <div className="absolute left-0 right-0 top-[calc(100%+0.55rem)] z-20 rounded-[1.5rem] border border-slate-200/80 bg-white/95 p-2 shadow-[0_24px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/95">
          {suggestionsQuery.isLoading ? (
            <div className="px-3 py-3 text-sm text-slate-500 dark:text-white/45">{t('ui.lexicalSearch.loadingSuggestions')}</div>
          ) : suggestions.length === 0 ? (
            <div className="px-3 py-3 text-sm text-slate-500 dark:text-white/45">{t('ui.lexicalSearch.noSuggestions')}</div>
          ) : (
            <div className="space-y-1">
              {suggestions.map((suggestion, index) => (
                <button
                  key={suggestion.id}
                  type="button"
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => commitSelection(suggestion)}
                  className={cn(
                    'w-full rounded-[1.1rem] px-3 py-3 text-left transition-all',
                    index === activeIndex
                      ? 'bg-primary/10 text-slate-900 dark:text-white'
                      : 'hover:bg-black/5 dark:hover:bg-white/5'
                  )}
                >
                  <div className="font-bold text-slate-900 dark:text-white">
                    {suggestion.englishWord} / {suggestion.frenchWord}
                  </div>
                  <div className="mt-1 text-sm text-slate-500 dark:text-white/50">
                    {suggestion.chineseGloss} · {lexicalPairTypeLabel(suggestion.lexicalPairType)}
                  </div>
                  <div className="mt-2 text-[11px] uppercase tracking-[0.18em] text-slate-400 dark:text-white/30">
                    {matchedByLabel(suggestion.matchedBy, t)}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
