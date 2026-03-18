import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Rocket, 
  Target, 
  Brain, 
  Zap, 
  ChevronRight, 
  CheckCircle, 
  XCircle,
  Lightbulb,
  ArrowRight,
  Flame,
  Award,
  RefreshCcw,
  BookOpen
} from 'lucide-react';
import { useTrainingStore } from '@/store/training.store';
import { useDiagnosisStore } from '@/store/diagnosis.store';
import { Exercise, TrainingMode } from '@/types/training';

const TrainingPage: React.FC = () => {
  const { plan, generatePlan, startSession, currentSession, submitAnswer, endSession } = useTrainingStore();
  const { result: diagResult } = useDiagnosisStore();
  
  const [phase, setPhase] = useState<'HOME' | 'SESSION' | 'SUMMARY'>('HOME');
  const [showFeedback, setShowFeedback] = useState<boolean>(false);
  const [lastCorrect, setLastCorrect] = useState<boolean>(false);
  const startTimeRef = useRef<number>(0);

  // 初始化推荐计划
  useEffect(() => {
    if (diagResult) {
      generatePlan(diagResult);
    }
  }, [diagResult]);

  const handleStart = (mode: TrainingMode) => {
    startSession(mode);
    setPhase('SESSION');
    startTimeRef.current = Date.now();
  };

  const handleExerciseSubmit = (optionIdx: number) => {
    if (!currentSession) return;
    const exercise = currentSession.exercises[currentSession.currentIdx];
    const isCorrect = optionIdx === exercise.content.correctAnswer;
    const rt = Date.now() - startTimeRef.current;

    setLastCorrect(isCorrect);
    setShowFeedback(true);

    // 延迟提交，给用户看反馈
    setTimeout(() => {
      submitAnswer({ exerciseId: exercise.id, isCorrect, responseTime: rt });
      setShowFeedback(false);
      
      if (currentSession.currentIdx + 1 >= currentSession.exercises.length) {
        setPhase('SUMMARY');
        endSession();
      } else {
        startTimeRef.current = Date.now();
      }
    }, 1500);
  };

  // 1. 训练主页
  if (phase === 'HOME') {
    return (
      <div className="max-w-4xl mx-auto space-y-8 animate-in fade-in duration-500">
        <div className="bg-gradient-to-br from-primary to-accent p-10 rounded-[2.5rem] text-white shadow-2xl shadow-primary/20 relative overflow-hidden">
          <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
            <div className="flex-1 text-center md:text-left">
              <div className="inline-flex items-center gap-2 bg-white/20 backdrop-blur-md px-4 py-1.5 rounded-full text-xs font-bold mb-4">
                <Rocket size={14} /> AI 智能自适应计划
              </div>
              <h1 className="text-4xl font-black mb-4 leading-tight">为你准备的训练建议</h1>
              <p className="text-blue-50/80 leading-relaxed max-w-md">
                {plan?.recommendationReason || '请先完成一次智能诊断，以便我们为你生成精准的训练路径。'}
              </p>
            </div>
            {plan && (
              <div className="grid grid-cols-2 gap-4 w-full md:w-auto">
                {plan.targetMetrics.map((m, i) => (
                  <div key={i} className="bg-white/10 border border-white/20 p-4 rounded-2xl flex items-center gap-3">
                    <Target size={18} />
                    <span className="text-xs font-bold">{m}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
          {/* Decorative shapes */}
          <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/2" />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {plan?.suggestedSessions.map((s, i) => (
            <div 
              key={i} 
              className="bg-card border border-border p-6 rounded-3xl hover:border-primary transition-all group cursor-pointer shadow-sm hover:shadow-xl"
              onClick={() => handleStart(s.mode)}
            >
              <div className="w-12 h-12 bg-muted rounded-2xl flex items-center justify-center mb-4 text-muted-foreground group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                {s.mode === 'FALSE_FRIEND_DISCRIM' ? <Zap size={24} /> : s.mode === 'CONTEXT_FIX' ? <Brain size={24} /> : <Rocket size={24} />}
              </div>
              <h3 className="font-bold mb-2">{s.label}</h3>
              <p className="text-xs text-muted-foreground mb-4">包含 {s.count} 组针对性词对练习</p>
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-black uppercase tracking-widest text-primary">Start Training</span>
                <ChevronRight size={16} className="text-primary opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  // 2. 训练作答页
  if (phase === 'SESSION' && currentSession) {
    const exercise = currentSession.exercises[currentSession.currentIdx];
    const progress = ((currentSession.currentIdx) / currentSession.exercises.length) * 100;

    return (
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-8">
          <button onClick={() => setPhase('HOME')} className="p-2 hover:bg-muted rounded-full"><ArrowRight className="rotate-180" size={20} /></button>
          <div className="flex-1 h-3 bg-muted rounded-full overflow-hidden">
            <motion.div className="h-full bg-primary" initial={{ width: 0 }} animate={{ width: `${progress}%` }} />
          </div>
          <span className="text-xs font-black">{currentSession.currentIdx + 1} / {currentSession.exercises.length}</span>
        </div>

        <AnimatePresence mode="wait">
          {!showFeedback ? (
            <motion.div 
              key={exercise.id}
              initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
              className="bg-card border border-border p-10 rounded-[2.5rem] shadow-2xl shadow-primary/5 min-h-[450px] flex flex-col"
            >
              <div className="flex items-center gap-2 mb-8">
                <div className={cn(
                  "px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest border",
                  exercise.cognitiveTag === 'TRAP' ? "bg-rose-50 text-rose-600 border-rose-100" : "bg-blue-50 text-blue-600 border-blue-100"
                )}>
                  {exercise.cognitiveTag === 'TRAP' ? '⚠️ Negative Transfer Trap' : '✨ Positive Transfer Opportunity'}
                </div>
              </div>

              <div className="flex-1 flex flex-col items-center justify-center text-center">
                <div className="flex items-baseline gap-6 mb-4">
                  <span className="text-5xl font-black">{exercise.wordPair.en}</span>
                  <span className="text-lg text-muted-foreground">vs</span>
                  <span className="text-5xl font-black">{exercise.wordPair.fr}</span>
                </div>
                <p className="text-lg font-medium text-muted-foreground italic mb-12">"{exercise.content.question}"</p>

                <div className="grid grid-cols-1 gap-3 w-full max-w-md">
                  {exercise.content.options?.map((opt, i) => (
                    <button 
                      key={i} 
                      onClick={() => handleExerciseSubmit(i)}
                      className="p-4 bg-background border border-border rounded-2xl font-bold hover:border-primary hover:bg-primary/5 transition-all text-sm"
                    >
                      {opt}
                    </button>
                  ))}
                </div>
              </div>
            </motion.div>
          ) : (
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
              className={cn(
                "p-12 rounded-[2.5rem] flex flex-col items-center justify-center text-center min-h-[450px]",
                lastCorrect ? "bg-emerald-50 border-2 border-emerald-100" : "bg-rose-50 border-2 border-rose-100"
              )}
            >
              <div className={cn("w-20 h-20 rounded-full flex items-center justify-center mb-6", lastCorrect ? "bg-emerald-100 text-emerald-600" : "bg-rose-100 text-rose-600")}>
                {lastCorrect ? <CheckCircle size={40} /> : <XCircle size={40} />}
              </div>
              <h2 className={cn("text-3xl font-black mb-4", lastCorrect ? "text-emerald-700" : "text-rose-700")}>
                {lastCorrect ? '认知辨析正确！' : '触发迁移陷阱！'}
              </h2>
              <p className="text-muted-foreground text-sm max-w-md leading-relaxed">
                {exercise.content.explanation}
              </p>
              <div className="mt-8 flex items-center gap-2 text-muted-foreground font-mono text-xs">
                <RefreshCw size={14} className="animate-spin-slow" /> Loading next stimulus...
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    );
  }

  // 3. 总结页
  if (phase === 'SUMMARY') {
    return (
      <div className="max-w-4xl mx-auto space-y-8 py-12 text-center animate-in slide-in-from-bottom duration-700">
        <div className="inline-flex items-center justify-center w-24 h-24 bg-amber-100 text-amber-600 rounded-full mb-6">
          <Award size={48} />
        </div>
        <h1 className="text-4xl font-black mb-2">训练已达标</h1>
        <p className="text-muted-foreground">你在同形异义词辨析上的认知反应已趋于稳定</p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 my-12">
          <div className="bg-card border border-border p-8 rounded-3xl">
            <div className="text-3xl font-black mb-1">92%</div>
            <div className="text-xs text-muted-foreground uppercase font-bold tracking-widest">本次正确率</div>
          </div>
          <div className="bg-card border border-border p-8 rounded-3xl">
            <div className="text-3xl font-black mb-1">450ms</div>
            <div className="text-xs text-muted-foreground uppercase font-bold tracking-widest">平均反应时</div>
          </div>
          <div className="bg-card border border-border p-8 rounded-3xl">
            <div className="text-3xl font-black mb-1 text-emerald-500">-120ms</div>
            <div className="text-xs text-muted-foreground uppercase font-bold tracking-widest">认知延迟缩减</div>
          </div>
        </div>

        <div className="flex justify-center gap-4 pt-8">
          <button 
            onClick={() => setPhase('HOME')}
            className="px-8 py-4 bg-primary text-white font-black rounded-2xl shadow-xl shadow-primary/20 hover:scale-105 transition-transform"
          >
            继续下一轮推荐训练
          </button>
          <button 
            onClick={() => window.location.href = '/dashboard'}
            className="px-8 py-4 bg-muted text-foreground font-black rounded-2xl hover:bg-muted/80 transition-all"
          >
            返回总览
          </button>
        </div>
      </div>
    );
  }

  return null;
};

// Utils
function cn(...inputs: any[]) {
  return inputs.filter(Boolean).join(' ');
}

export default TrainingPage;
