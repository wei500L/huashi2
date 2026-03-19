import React, { useState, useEffect, useRef } from 'react';
import { useTransferStore } from '../store/useTransferStore';
import { WordPairType } from '../types';
import { Timer, Brain, CheckCircle2, AlertCircle, ChevronRight, RefreshCw, Zap, Target } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Magnetic } from '@/components/common';

const Diagnostic: React.FC = () => {
  const { activeWordPair, startDiagnostic, recordResponse, currentSession } = useTransferStore();
  const [step, setStep] = useState<'IDLE' | 'TEST' | 'RESULT'>('IDLE');
  const [startTime, setStartTime] = useState<number>(0);

  const handleStart = () => {
    startDiagnostic();
    setStep('TEST');
    setStartTime(Date.now());
  };

  const handleResponse = (isSame: boolean) => {
    if (!activeWordPair) return;

    const endTime = Date.now();
    const rt = endTime - startTime;
    
    const correct = activeWordPair.type === WordPairType.Cognate ? isSame : !isSame;

    recordResponse(activeWordPair.id, {
      accuracy: correct ? 1 : 0,
      responseTime: rt,
      hesitationDuration: rt > 1000 ? rt - 1000 : 0,
      errorType: correct ? 'none' : 'semantic'
    });

    if (activeWordPair.id === 'w2') {
      setStep('RESULT');
    } else {
      setStartTime(Date.now());
    }
  };

  if (step === 'IDLE') {
    return (
      <motion.div 
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="flex flex-col items-center justify-center min-h-[70vh] max-w-3xl mx-auto text-center"
      >
        <div className="liquid-glass-panel border-beam p-16 rounded-[3.5rem] edge-light fluid-texture w-full">
          <div className="inline-flex p-5 bg-primary/10 rounded-3xl mb-8 border border-primary/20 shadow-lg">
            <Brain size={48} className="text-primary animate-pulse" />
          </div>
          <h2 className="text-5xl font-black text-slate-900 dark:text-white tracking-tighter mb-6">英法词汇认知迁移诊断</h2>
          <p className="text-lg text-slate-500 dark:text-white/40 leading-relaxed mb-12 max-w-xl mx-auto">
            在本任务中，您将看到一组英法词对。请根据您的直觉，快速判断它们的<span className="text-primary font-bold">核心语义是否相同</span>。我们将分析您的跨语言迁移路径。
          </p>
          <Magnetic strength={0.2}>
            <button 
              onClick={handleStart}
              className="btn-liquid flex items-center gap-3 px-12 py-5 text-xl group mx-auto"
            >
              开始任务 (Start Engine) <ChevronRight className="group-hover:translate-x-1 transition-transform" />
            </button>
          </Magnetic>
        </div>
      </motion.div>
    );
  }

  if (step === 'TEST' && activeWordPair) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[70vh] space-y-16">
        <div className="flex gap-4 items-center px-6 py-2 bg-white/50 dark:bg-white/5 border border-slate-200 dark:border-white/10 rounded-full backdrop-blur-xl">
          <Timer size={18} className="text-primary" />
          <span className="text-xs font-black tracking-[0.3em] text-slate-400 dark:text-white/30 uppercase animate-pulse">Neural Stimulus Presenting</span>
        </div>

        <div className="flex flex-col md:flex-row gap-12 items-center w-full max-w-5xl justify-center">
          <motion.div 
            key={`en-${activeWordPair.en}`}
            initial={{ x: -50, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            className="text-center space-y-4 flex-1"
          >
            <span className="text-[10px] font-black text-blue-500 uppercase tracking-[0.4em]">Stimulus A / English</span>
            <div className="text-6xl font-black p-16 liquid-glass rounded-[2.5rem] min-w-[300px] border-beam text-slate-900 dark:text-white">
              {activeWordPair.en}
            </div>
          </motion.div>
          
          <div className="text-4xl font-black text-slate-200 dark:text-white/10 italic">VS</div>

          <motion.div 
            key={`fr-${activeWordPair.fr}`}
            initial={{ x: 50, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            className="text-center space-y-4 flex-1"
          >
            <span className="text-[10px] font-black text-rose-500 uppercase tracking-[0.4em]">Stimulus B / French</span>
            <div className="text-6xl font-black p-16 liquid-glass rounded-[2.5rem] min-w-[300px] border-beam text-slate-900 dark:text-white">
              {activeWordPair.fr}
            </div>
          </motion.div>
        </div>

        <div className="flex gap-8">
          <Magnetic strength={0.1}>
            <button 
              onClick={() => handleResponse(true)}
              className="px-12 py-6 bg-emerald-500/10 dark:bg-emerald-500/5 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 rounded-3xl font-black text-lg hover:bg-emerald-500 hover:text-white transition-all shadow-xl dark:shadow-emerald-500/10 uppercase tracking-widest"
            >
              相同 (Same)
            </button>
          </Magnetic>
          <Magnetic strength={0.1}>
            <button 
              onClick={() => handleResponse(false)}
              className="px-12 py-6 bg-rose-500/10 dark:bg-rose-500/5 text-rose-600 dark:text-rose-400 border border-rose-500/20 rounded-3xl font-black text-lg hover:bg-rose-500 hover:text-white transition-all shadow-xl dark:shadow-rose-500/10 uppercase tracking-widest"
            >
              不同 (Diff)
            </button>
          </Magnetic>
        </div>

        <div className="w-full max-w-lg bg-slate-100 dark:bg-white/5 h-1.5 rounded-full overflow-hidden border border-slate-200 dark:border-white/5">
          <motion.div 
            initial={{ width: 0 }}
            animate={{ width: '50%' }}
            className="bg-primary h-full shadow-[0_0_10px_rgba(139,92,246,0.8)]"
          />
        </div>
      </div>
    );
  }

  if (step === 'RESULT') {
    return (
      <motion.div 
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-5xl mx-auto space-y-10 py-12"
      >
        <div className="text-center">
          <div className="inline-flex p-4 bg-emerald-500/10 rounded-full mb-6 border border-emerald-500/20">
            <CheckCircle2 size={48} className="text-emerald-500" />
          </div>
          <h2 className="text-5xl font-black text-slate-900 dark:text-white tracking-tight mb-2">程序诊断已完成</h2>
          <p className="text-slate-500 dark:text-white/40 font-medium">Cognitive Data Successfully Encoded</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
          <div className="liquid-glass-panel p-8 rounded-[2.5rem]">
            <h3 className="text-[10px] font-black mb-8 flex items-center gap-3 uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
              <Zap size={16} className="text-primary" /> Stimulus Trajectory
            </h3>
            <div className="space-y-4">
              {currentSession.map((rec, i) => (
                <div key={i} className="flex justify-between items-center p-5 bg-white/50 dark:bg-white/5 border border-slate-100 dark:border-white/5 rounded-3xl">
                  <div className="flex items-center gap-4">
                    <span className="text-xs font-black text-slate-400">#{i+1}</span>
                    <span className="text-sm font-bold text-slate-800 dark:text-white/90">TRIAL SESSION</span>
                  </div>
                  <div className="flex items-center gap-6">
                    <span className={rec.score === 100 ? "text-emerald-500" : "text-rose-500"}>{rec.score === 100 ? 'SUCCESS' : 'ERROR'}</span>
                    <span className="font-mono text-xs font-black bg-black/5 dark:bg-black/40 px-3 py-1 rounded-lg text-primary">{rec.metrics?.responseTime || 0}ms</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-primary p-10 rounded-[2.5rem] shadow-2xl shadow-primary/20 text-white relative overflow-hidden group">
            <div className="absolute top-0 right-0 p-8 opacity-10 group-hover:scale-110 transition-transform">
              <Target size={120} />
            </div>
            <h3 className="text-[10px] font-black mb-8 uppercase tracking-[0.3em] opacity-60">Neural Insights</h3>
            <ul className="space-y-6 text-sm font-medium">
              <li className="flex gap-4 items-start">
                <div className="w-1.5 h-1.5 rounded-full bg-white mt-1.5" />
                <span className="opacity-90">您的**正迁移效率**高于常模，能快速识别英法同源词。</span>
              </li>
              <li className="flex gap-4 items-start">
                <div className="w-1.5 h-1.5 rounded-full bg-white mt-1.5" />
                <span className="opacity-90">在同形异义词上存在约 **300ms** 的认知负荷（Hesitation）。</span>
              </li>
              <li className="flex gap-4 items-start">
                <div className="w-1.5 h-1.5 rounded-full bg-white mt-1.5" />
                <span className="opacity-90">建议加强针对法律与医学领域的法语高频负迁移词汇纠偏。</span>
              </li>
            </ul>
            <button 
              onClick={() => setStep('IDLE')}
              className="mt-12 w-full py-4 bg-white text-primary font-black rounded-2xl hover:scale-[1.02] transition-transform shadow-lg uppercase text-xs tracking-widest"
            >
              重启新诊断 (Re-Initialize)
            </button>
          </div>
        </div>
      </motion.div>
    );
  }

  return null;
};

export default Diagnostic;
