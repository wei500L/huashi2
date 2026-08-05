import React from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { useDialogAccessibility } from '@/lib/a11y';
import { cn } from '@/lib/utils';
import { useReducedMotion } from 'framer-motion';

export type OnboardingPlacement = 'top' | 'right' | 'bottom' | 'left' | 'center';

export type OnboardingStep = {
  id: string;
  selector: string;
  title: string;
  description: string;
  placement?: OnboardingPlacement;
  spotlightPadding?: number;
};

type SpotlightRect = {
  top: number;
  left: number;
  width: number;
  height: number;
};

type OnboardingTourProps = {
  open: boolean;
  steps: OnboardingStep[];
  onComplete: () => void;
  className?: string;
};

const PANEL_GAP = 18;
const VIEWPORT_MARGIN = 16;

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function resolveSpotlightRect(step: OnboardingStep | undefined): SpotlightRect | null {
  if (!step) {
    return null;
  }

  const target = window.document.querySelector<HTMLElement>(step.selector);
  if (!target) {
    return null;
  }

  const rect = target.getBoundingClientRect();
  const padding = step.spotlightPadding ?? 12;
  return {
    top: rect.top - padding,
    left: rect.left - padding,
    width: rect.width + padding * 2,
    height: rect.height + padding * 2,
  };
}

function resolvePopoverStyle(
  rect: SpotlightRect | null,
  placement: OnboardingPlacement,
  panelWidth: number,
  panelHeight: number
): React.CSSProperties {
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;

  if (!rect || placement === 'center') {
    return {
      top: Math.max(VIEWPORT_MARGIN, (viewportHeight - panelHeight) / 2),
      left: Math.max(VIEWPORT_MARGIN, (viewportWidth - panelWidth) / 2),
    };
  }

  const placements: OnboardingPlacement[] = placement === 'bottom'
    ? ['bottom', 'top', 'right', 'left']
    : placement === 'top'
      ? ['top', 'bottom', 'right', 'left']
      : placement === 'right'
        ? ['right', 'left', 'bottom', 'top']
        : ['left', 'right', 'bottom', 'top'];

  for (const candidate of placements) {
    const style = (() => {
      switch (candidate) {
        case 'top':
          return {
            top: rect.top - panelHeight - PANEL_GAP,
            left: rect.left + rect.width / 2 - panelWidth / 2,
          };
        case 'right':
          return {
            top: rect.top + rect.height / 2 - panelHeight / 2,
            left: rect.left + rect.width + PANEL_GAP,
          };
        case 'left':
          return {
            top: rect.top + rect.height / 2 - panelHeight / 2,
            left: rect.left - panelWidth - PANEL_GAP,
          };
        case 'bottom':
        default:
          return {
            top: rect.top + rect.height + PANEL_GAP,
            left: rect.left + rect.width / 2 - panelWidth / 2,
          };
      }
    })();

    const fitsVertically = style.top >= VIEWPORT_MARGIN && style.top + panelHeight <= viewportHeight - VIEWPORT_MARGIN;
    const fitsHorizontally = style.left >= VIEWPORT_MARGIN && style.left + panelWidth <= viewportWidth - VIEWPORT_MARGIN;
    if (fitsVertically && fitsHorizontally) {
      return style;
    }
  }

  return {
    top: clamp(rect.top + rect.height + PANEL_GAP, VIEWPORT_MARGIN, viewportHeight - panelHeight - VIEWPORT_MARGIN),
    left: clamp(rect.left + rect.width / 2 - panelWidth / 2, VIEWPORT_MARGIN, viewportWidth - panelWidth - VIEWPORT_MARGIN),
  };
}

export const OnboardingTour: React.FC<OnboardingTourProps> = ({ open, steps, onComplete, className }) => {
  const { t } = useTranslation();
  const reducedMotion = useReducedMotion();
  const [stepIndex, setStepIndex] = React.useState(0);
  const [spotlightRect, setSpotlightRect] = React.useState<SpotlightRect | null>(null);
  const [panelStyle, setPanelStyle] = React.useState<React.CSSProperties>({});
  const panelRef = React.useRef<HTMLDivElement | null>(null);
  const initialFocusRef = React.useRef<HTMLButtonElement | null>(null);
  const activeStep = steps[stepIndex];

  React.useEffect(() => {
    if (!open) {
      setStepIndex(0);
    } else if (stepIndex >= steps.length) {
      setStepIndex(0);
    }
  }, [open, stepIndex, steps.length]);

  const refreshLayout = React.useCallback(() => {
    if (!open) {
      return;
    }

    const nextRect = resolveSpotlightRect(activeStep);
    setSpotlightRect(nextRect);

    const panel = panelRef.current;
    if (!panel) {
      return;
    }

    const panelRect = panel.getBoundingClientRect();
    setPanelStyle(
      resolvePopoverStyle(nextRect, activeStep?.placement ?? 'bottom', panelRect.width, panelRect.height)
    );
  }, [activeStep, open]);

  React.useEffect(() => {
    if (!open || !activeStep) {
      return;
    }

    const target = window.document.querySelector<HTMLElement>(activeStep.selector);
    if (!target) {
      return;
    }

    const frame = window.requestAnimationFrame(() => {
      target.scrollIntoView({ block: 'center', inline: 'nearest', behavior: reducedMotion ? 'auto' : 'smooth' });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [activeStep, open, reducedMotion]);

  React.useLayoutEffect(() => {
    refreshLayout();
  }, [refreshLayout]);

  React.useEffect(() => {
    if (!open) {
      return;
    }

    const target = activeStep ? window.document.querySelector<HTMLElement>(activeStep.selector) : null;
    const resizeObserver = typeof ResizeObserver !== 'undefined'
      ? new ResizeObserver(() => refreshLayout())
      : null;

    if (target && resizeObserver) {
      resizeObserver.observe(target);
    }
    if (panelRef.current && resizeObserver) {
      resizeObserver.observe(panelRef.current);
    }

    const handleWindowChange = () => refreshLayout();
    window.addEventListener('resize', handleWindowChange);
    window.addEventListener('scroll', handleWindowChange, true);

    return () => {
      resizeObserver?.disconnect();
      window.removeEventListener('resize', handleWindowChange);
      window.removeEventListener('scroll', handleWindowChange, true);
    };
  }, [activeStep, open, refreshLayout]);

  useDialogAccessibility({
    open,
    containerRef: panelRef,
    initialFocusRef,
    onClose: onComplete,
  });

  if (!open || !activeStep || typeof document === 'undefined') {
    return null;
  }

  const isLastStep = stepIndex === steps.length - 1;

  return createPortal(
    <div className="fixed inset-0 z-[120]" aria-hidden={false}>
      <div className="absolute inset-0 bg-slate-950/58" />

      {spotlightRect ? (
        <>
          <div
            className="pointer-events-none absolute rounded-[2rem] border border-sky-300/90 shadow-[0_0_0_9999px_rgba(2,6,23,0.58)] transition-all motion-layout duration-200"
            style={spotlightRect}
          />
          <div
            className="pointer-events-none absolute rounded-[2rem] border border-white/70 transition-all motion-layout duration-200"
            style={{
              ...spotlightRect,
              boxShadow: '0 0 0 1px rgba(255,255,255,0.08), 0 0 28px rgba(125,211,252,0.45)',
            }}
          />
        </>
      ) : null}

      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={`onboarding-tour-title-${activeStep.id}`}
        className={cn(
          'safe-area-dialog fixed max-h-[calc(100dvh-2rem)] w-[min(22rem,calc(100vw-2rem))] overflow-y-auto rounded-[2rem] border border-white/15 bg-slate-950/96 p-4 text-white shadow-[0_30px_80px_rgba(2,6,23,0.5)] backdrop-blur sm:p-6',
          className
        )}
        style={panelStyle}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="text-[11px] font-black uppercase tracking-[0.28em] text-sky-200/75">
              {t('ui.onboarding.stepCounter', { current: stepIndex + 1, total: steps.length })}
            </div>
            <h2 id={`onboarding-tour-title-${activeStep.id}`} className="mt-3 text-xl font-black tracking-tight">
              {activeStep.title}
            </h2>
          </div>
          <button
            ref={initialFocusRef}
            type="button"
            onClick={onComplete}
            className="rounded-full border border-white/10 px-3 py-1.5 text-xs font-bold text-white/70 transition hover:border-white/30 hover:text-white"
          >
            {t('ui.onboarding.skip')}
          </button>
        </div>

        <p className="mt-4 text-sm leading-6 text-white/72">
          {activeStep.description}
        </p>

        {!spotlightRect && (
          <div className="mt-4 rounded-[1.2rem] border border-amber-300/20 bg-amber-300/10 px-4 py-3 text-xs leading-5 text-amber-100/90">
            {t('ui.onboarding.targetPending')}
          </div>
        )}

        <div className="mt-6 flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={() => setStepIndex((current) => Math.max(0, current - 1))}
            disabled={stepIndex === 0}
            className="rounded-full border border-white/10 px-4 py-2.5 text-sm font-bold text-white/75 transition hover:border-white/30 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
          >
            {t('ui.onboarding.previous')}
          </button>

          <button
            type="button"
            onClick={() => {
              if (isLastStep) {
                onComplete();
                return;
              }
              setStepIndex((current) => Math.min(steps.length - 1, current + 1));
            }}
            className="rounded-full bg-sky-400 px-5 py-2.5 text-sm font-black text-slate-950 transition hover:bg-sky-300"
          >
            {isLastStep ? t('ui.onboarding.finish') : t('ui.onboarding.next')}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
};
