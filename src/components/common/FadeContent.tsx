import React from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';

type FadeContentProps = React.HTMLAttributes<HTMLDivElement> & {
  contentKey: React.Key;
  children: React.ReactNode;
};

export const FadeContent: React.FC<FadeContentProps> = ({
  contentKey,
  children,
  className,
  ...props
}) => {
  const reduceMotion = useReducedMotion();

  return (
    <AnimatePresence mode="wait" initial={false}>
      <motion.div
        key={contentKey}
        initial={reduceMotion ? false : { opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={reduceMotion ? undefined : { opacity: 0 }}
        transition={{ duration: reduceMotion ? 0 : 0.18, ease: 'easeOut' }}
        className={className}
        {...props}
      >
        {children}
      </motion.div>
    </AnimatePresence>
  );
};
