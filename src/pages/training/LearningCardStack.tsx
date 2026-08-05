import React from 'react';
import { BookOpenText, Languages, Lightbulb } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FadeContent } from '@/components/common/FadeContent';
import type { TrainingQuestionItemVO } from '@/lib/contracts';

type LearningLayer = 'pair' | 'context' | 'explanation';

type LearningCardStackProps = {
  item: TrainingQuestionItemVO;
  explanation?: string | null;
  explanationAvailable: boolean;
};

const layers: LearningLayer[] = ['pair', 'context', 'explanation'];

export const LearningCardStack: React.FC<LearningCardStackProps> = ({
  item,
  explanation,
  explanationAvailable,
}) => {
  const { t } = useTranslation();
  const [activeLayer, setActiveLayer] = React.useState<LearningLayer>('pair');

  React.useEffect(() => {
    setActiveLayer('pair');
  }, [item.itemResultId]);

  const availableLayers = React.useMemo(
    () => (explanationAvailable ? layers : layers.slice(0, 2)),
    [explanationAvailable]
  );
  const activeIndex = availableLayers.indexOf(activeLayer);

  React.useEffect(() => {
    if (!explanationAvailable && activeLayer === 'explanation') {
      setActiveLayer('context');
    }
  }, [activeLayer, explanationAvailable]);

  const moveLayer = React.useCallback((direction: -1 | 1) => {
    const index = availableLayers.indexOf(activeLayer);
    const nextIndex = Math.min(availableLayers.length - 1, Math.max(0, index + direction));
    setActiveLayer(availableLayers[nextIndex]);
  }, [activeLayer, availableLayers]);

  const handleLayerKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.target instanceof Element && event.target.closest('button')) {
      return;
    }
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      event.preventDefault();
      moveLayer(event.key === 'ArrowRight' ? 1 : -1);
    }
    if (event.key.toLowerCase() === 'e' && explanationAvailable) {
      event.preventDefault();
      setActiveLayer('explanation');
    }
  };

  const layerButtonClass = (layer: LearningLayer) =>
    `min-h-11 rounded-full border px-4 py-2 text-sm font-bold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 ${
      activeLayer === layer
        ? 'border-primary/30 bg-primary/10 text-primary'
        : 'border-slate-200 bg-white/70 text-slate-600 dark:border-white/10 dark:bg-white/5 dark:text-white/60'
    } disabled:cursor-not-allowed disabled:opacity-45`;

  return (
    <div
      tabIndex={0}
      role="region"
      aria-label={t('training.cardLayersLabel')}
      onKeyDown={handleLayerKeyDown}
      className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
    >
      <div className="mb-5 flex flex-wrap items-center gap-3" aria-label={t('training.cardLayersLabel')}>
        <button
          type="button"
          aria-pressed={activeLayer === 'pair'}
          onClick={() => setActiveLayer('pair')}
          className={layerButtonClass('pair')}
        >
          <Languages size={16} className="mr-2 inline" aria-hidden="true" />
          {t('training.pairLayer')}
        </button>
        <button
          type="button"
          aria-pressed={activeLayer === 'context'}
          onClick={() => setActiveLayer('context')}
          className={layerButtonClass('context')}
        >
          <BookOpenText size={16} className="mr-2 inline" aria-hidden="true" />
          {t('training.contextLayer')}
        </button>
        <button
          type="button"
          aria-pressed={activeLayer === 'explanation'}
          aria-keyshortcuts="E"
          disabled={!explanationAvailable}
          onClick={() => setActiveLayer('explanation')}
          className={layerButtonClass('explanation')}
        >
          <Lightbulb size={16} className="mr-2 inline" aria-hidden="true" />
          {explanationAvailable ? t('training.viewExplanation') : t('training.explanationAfterAnswer')}
        </button>
        <span className="text-xs text-slate-400 dark:text-white/35">
          {t('training.stackKeyboardHint')}
        </span>
      </div>

      <div className="min-h-[21rem] rounded-[1rem] border border-slate-200/80 bg-white/95 p-7 shadow-[var(--shadow-sm)] focus-within:ring-2 focus-within:ring-primary/20 dark:border-white/10 dark:bg-slate-950/95 sm:p-9">
          <FadeContent contentKey={`${item.itemResultId}-${activeLayer}`} className="min-h-[16rem]">
            {activeLayer === 'pair' ? (
              <div className="grid min-h-[16rem] content-center gap-7 md:grid-cols-2">
                <div>
                  <div className="text-xs font-black uppercase tracking-[0.24em] text-sky-500">
                    {t('diagnosis.english')}
                  </div>
                  <div className="mt-3 break-words text-4xl font-black text-slate-900 dark:text-white">
                    {item.englishWord}
                  </div>
                </div>
                <div className="border-t border-slate-200 pt-7 dark:border-white/10 md:border-l md:border-t-0 md:pl-8 md:pt-0">
                  <div className="text-xs font-black uppercase tracking-[0.24em] text-rose-500">
                    {t('diagnosis.french')}
                  </div>
                  <div className="mt-3 break-words text-4xl font-black text-slate-900 dark:text-white">
                    {item.frenchWord}
                  </div>
                  {item.chineseGloss ? (
                    <div className="mt-4 text-sm leading-6 text-slate-500 dark:text-white/45">{item.chineseGloss}</div>
                  ) : null}
                </div>
              </div>
            ) : activeLayer === 'context' ? (
              <div className="flex min-h-[16rem] flex-col justify-center">
                <div className="text-xs font-black uppercase tracking-[0.24em] text-emerald-600 dark:text-emerald-300">
                  {t('training.contextLayer')}
                </div>
                <div className="mt-5 text-xl font-black leading-8 text-slate-900 dark:text-white">
                  {item.content.question}
                </div>
                {item.content.sentence ? (
                  <blockquote className="mt-5 border-l-4 border-emerald-500/35 pl-5 text-lg italic leading-8 text-slate-600 dark:text-white/60">
                    {item.content.sentence}
                  </blockquote>
                ) : (
                  <div className="mt-5 text-sm text-slate-500 dark:text-white/45">{t('training.noContextSentence')}</div>
                )}
              </div>
            ) : (
              <div className="flex min-h-[16rem] flex-col justify-center">
                <div className="text-xs font-black uppercase tracking-[0.24em] text-amber-600 dark:text-amber-300">
                  {t('training.semanticExplanation')}
                </div>
                <div className="mt-5 text-lg leading-8 text-slate-700 dark:text-white/70">
                  {explanation || t('training.noExplanation')}
                </div>
              </div>
            )}
          </FadeContent>
      </div>
      </div>

      <div className="mt-2 flex items-center justify-between gap-3">
        <button
          type="button"
          disabled={activeIndex <= 0}
          onClick={() => moveLayer(-1)}
          className="min-h-11 rounded-full border border-slate-200 px-4 py-2 text-sm font-bold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 disabled:opacity-40 dark:border-white/10"
        >
          {t('training.previousLayer')}
        </button>
        <span className="text-xs font-bold text-slate-400 dark:text-white/35" aria-live="polite">
          {activeIndex + 1} / {availableLayers.length}
        </span>
        <button
          type="button"
          disabled={activeIndex >= availableLayers.length - 1}
          onClick={() => moveLayer(1)}
          className="min-h-11 rounded-full border border-slate-200 px-4 py-2 text-sm font-bold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 disabled:opacity-40 dark:border-white/10"
        >
          {t('training.nextLayer')}
        </button>
      </div>
    </div>
  );
};
