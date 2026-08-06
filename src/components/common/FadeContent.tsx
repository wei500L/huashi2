import React from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';

type FadeContentProps = {
  contentKey: React.Key;
  children: React.ReactNode;
  className?: string;
};

// Framer Motion's HTML event overloads conflict with React 19's stricter
// DOM event types under a fresh npm install. Keep the animation props local
// while preserving the normal div event surface for this wrapper.
type FadeMotionProps = React.HTMLAttributes<HTMLDivElement> & {
  initial?: unknown;
  animate?: unknown;
  exit?: unknown;
  transition?: unknown;
};

const FadeMotion = motion.div as unknown as React.ComponentType<FadeMotionProps>;

export const FadeContent: React.FC<FadeContentProps> = ({
  contentKey,
  children,
  className,
}) => {
  const reduceMotion = useReducedMotion();

  return (
    <AnimatePresence mode="wait" initial={false}>
      <FadeMotion
        key={contentKey}
        initial={reduceMotion ? false : { opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={reduceMotion ? undefined : { opacity: 0 }}
        transition={{ duration: reduceMotion ? 0 : 0.18, ease: 'easeOut' }}
        className={className}
      >
        {children}
      </FadeMotion>
    </AnimatePresence>
  );
};
