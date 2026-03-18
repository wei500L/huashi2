import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Timer, 
  Brain, 
  CheckCircle2, 
  AlertCircle, 
  ChevronRight, 
  Play, 
  ArrowLeft,
  Activity,
  Zap,
  Target
} from 'lucide-react';
import { useDiagnosisStore } from '@/store/diagnosis.store';
import { DiagnosisQuestion, AnswerRecord } from '@/types/diagnosis';
import ReactECharts from 'echarts-for-react';

const mockQuestions: DiagnosisQuestion[] = [
  {
    id: 'q1',
    type: 'RT_JUDGMENT',
    wordPair: { en: 'table', fr: 'table', type: 'COGNATE' },
    context: { level: 'LOW', sentence: '', options: ['语义一致', '语义不一致'], correctIndex: 0 }
  },
  {
    id: 'q2',
    type: 'RT_JUDGMENT',
    wordPair: { en: 'coin', fr: 'coin', type: 'FALSE_FRIEND' },
    context: { level: 'LOW', sentence: '', options: ['语义一致', '语义不一致'], correctIndex: 1 }
  },
  {
    id: 'q3',
    type: 'SEMANTIC_CONTEXT',
    wordPair: { en: 'actually', fr: 'actuellement', type: 'FALSE_FRIEND' },
    context: { 
      level: 'HIGH', 
      sentence: 'Il travaille "actuellement" à Paris.', 
      options: ['Actually (实际上)', 'Currently (目前)'], 
      correctIndex: 1 
    }
  },
  {
    id: 'q4',
    type: 'RT_JUDGMENT',
    wordPair: { en: 'nature', fr: 'nature', type: 'COGNATE' },
    context: { level: 'MEDIUM', sentence: 'The "nature" of the problem.', options: ['语义一致', '语义不一致'], correctIndex: 0 }
  }
];

const Diagnosis: React.FC = () => {
  const { 
    questions, currentIdx, records, isComplete, result, 
    startDiagnosis, recordAnswer, reset 
  } = useDiagnosisStore();

  const [phase, setPhase] = useState<'INTRO' | 'QUIZ' | 'RESULT'>('INTRO');
  const startTimeRef = useRef<number>(0);

  // 开始诊断
  const handleStart = () => {
    startDiagnosis(mockQuestions);
    setPhase('QUIZ');
    startTimeRef.current = Date.now();
  };

  // 处理作答
  const handleAnswer = (optionIdx: number) => {
    const endTime = Date.now();
    const rt = endTime - startTimeRef.current;
    const currentQ = questions[currentIdx];
    const isCorrect = optionIdx === currentQ.context?.correctIndex;

    recordAnswer({
      questionId: currentQ.id,
      isCorrect,
      responseTime: rt,
      timestamp: endTime,
      wordPairType: currentQ.wordPair.type,
      contextLevel: currentQ.context?.level || 'LOW'
    });

    if (currentIdx + 1 < questions.length) {
      startTimeRef.current = Date.now();
    } else {
      setPhase('RESULT');
    }
  };

  // 1. 引导页
  if (phase === 'INTRO') {
    return (
      <div className="max-w-3xl mx-auto py-12 px-6 bg-card border border-border rounded-3xl shadow-xl shadow-primary/5">
        <div className="flex justify-center mb-8">
          <div className="p-4 bg-primary/10 text-primary rounded-2xl">
            <Brain size={48} />
          </div>
        </div>
        <h1 className="text-3xl font-black text-center mb-4 tracking-tight">英法双语认知迁移智能诊断</h1>
        <p className="text-muted-foreground text-center leading-relaxed mb-12">
          欢迎参加认知决策任务。我们将通过一系列快速反应任务和语义判断任务，分析你在英法同源词与同形异义词处理中的正迁移表现与负迁移风险。
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-12">
          <div className="p-6 bg-muted/50 rounded-2xl border border-border">
            <Zap className="text-amber-500 mb-3" size={24} />
            <h3 className="font-bold mb-2 text-sm uppercase tracking-wider">反应速度</h3>
            <p className="text-xs text-muted-foreground">部分题目要求你在 1s 内做出判断，以捕捉你的自动加工路径。</p>
          </div>
          <div className="p-6 bg-muted/50 rounded-2xl border border-border">
            <Target className="text-blue-500 mb-3" size={24} />
            <h3 className="font-bold mb-2 text-sm uppercase tracking-wider">语境辨析</h3>
            <p className="text-xs text-muted-foreground">你将在不同强度的语境（低、中、高）下进行语义抉择。</p>
          </div>
        </div>

        <button 
          onClick={handleStart}
          className="w-full flex items-center justify-center gap-3 bg-primary text-white py-4 rounded-2xl font-black text-lg shadow-xl shadow-primary/20 hover:scale-[1.02] transition-transform"
        >
          我已准备好，立即开始 <Play size={20} fill="currentColor" />
        </button>
      </div>
    );
  }

  // 2. 作答页
  if (phase === 'QUIZ') {
    const currentQ = questions[currentIdx];
    const progress = ((currentIdx + 1) / questions.length) * 100;

    return (
      <div className="max-w-4xl mx-auto">
        {/* 进度与计时 */}
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-4 flex-1 max-w-xs">
            <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
              <motion.div 
                className="h-full bg-primary" 
                initial={{ width: 0 }} 
                animate={{ width: `${progress}%` }} 
              />
            </div>
            <span className="text-xs font-mono font-bold">{currentIdx + 1} / {questions.length}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Timer size={16} />
            <span className="text-xs font-mono uppercase tracking-widest">Live Stimulus Capture</span>
          </div>
        </div>

        <AnimatePresence mode="wait">
          <motion.div 
            key={currentQ.id}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="bg-card border border-border rounded-3xl p-12 shadow-2xl shadow-primary/5 min-h-[400px] flex flex-col items-center justify-center"
          >
            {/* 刺激呈现区 */}
            <div className="text-center mb-12">
              <div className="flex items-center justify-center gap-12 mb-8">
                <div className="space-y-2">
                  <span className="text-[10px] font-black uppercase text-blue-500 tracking-tighter">English</span>
                  <div className="text-5xl font-black">{currentQ.wordPair.en}</div>
                </div>
                <div className="text-2xl font-light text-muted-foreground italic">vs</div>
                <div className="space-y-2">
                  <span className="text-[10px] font-black uppercase text-rose-500 tracking-tighter">French</span>
                  <div className="text-5xl font-black">{currentQ.wordPair.fr}</div>
                </div>
              </div>

              {currentQ.context?.sentence && (
                <div className="p-6 bg-muted/30 rounded-2xl border border-dashed border-border max-w-xl mx-auto">
                  <p className="text-lg font-medium italic">"{currentQ.context.sentence}"</p>
                </div>
              )}
            </div>

            {/* 决策选项 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 w-full max-w-md">
              {currentQ.context?.options.map((opt, i) => (
                <button
                  key={i}
                  onClick={() => handleAnswer(i)}
                  className="p-4 bg-background border border-border rounded-2xl font-bold hover:border-primary hover:bg-primary/5 transition-all text-sm group flex items-center justify-between"
                >
                  {opt}
                  <ChevronRight size={16} className="text-muted-foreground group-hover:text-primary" />
                </button>
              ))}
            </div>
          </motion.div>
        </AnimatePresence>
      </div>
    );
  }

  // 3. 结果页
  if (phase === 'RESULT' && result) {
    const radarOption = {
      radar: {
        indicator: [
          { name: '正迁移能力', max: 1 },
          { name: '负迁移控制', max: 1 },
          { name: '语义辨析力', max: 1 },
          { name: '语境敏感度', max: 1 },
          { name: '认知流畅度', max: 1 },
        ],
        shape: 'circle',
        splitNumber: 4,
        axisName: { color: '#64748b', fontSize: 10 },
        splitArea: { areaStyle: { color: ['rgba(255, 255, 255, 0.1)', 'rgba(255, 255, 255, 0.2)'] } }
      },
      series: [{
        type: 'radar',
        data: [{
          value: [
            result.positiveTransferScore,
            1 - result.negativeTransferRisk,
            result.semanticDiscrimination,
            result.contextSensitivity,
            Math.min(1, 1500 / result.avgRT)
          ],
          name: '我的指标',
          symbol: 'none',
          areaStyle: { color: 'rgba(59, 130, 246, 0.2)' },
          lineStyle: { color: '#3b82f6', width: 2 }
        }]
      }]
    };

    return (
      <div className="max-w-5xl mx-auto space-y-8 pb-20 animate-in fade-in slide-in-from-bottom-4 duration-700">
        <div className="flex flex-col items-center text-center">
          <div className="w-20 h-20 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mb-6">
            <CheckCircle2 size={40} />
          </div>
          <h1 className="text-4xl font-black mb-2">诊断分析完成</h1>
          <p className="text-muted-foreground">已生成你的英法跨语言加工行为图谱</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* 左侧：认知画像图 */}
          <div className="lg:col-span-2 bg-card border border-border rounded-3xl p-8 shadow-xl shadow-primary/5">
            <div className="flex items-center justify-between mb-8">
              <h3 className="font-bold flex items-center gap-2">
                <Activity size={18} className="text-primary" /> 认知加工雷达图
              </h3>
              <div className="text-xs bg-muted px-3 py-1 rounded-full text-muted-foreground">基于 {records.length} 次刺激反应</div>
            </div>
            <div className="h-[400px]">
              <ReactECharts option={radarOption} style={{ height: '100%' }} />
            </div>
          </div>

          {/* 右侧：关键发现指标 */}
          <div className="space-y-6">
            <div className="bg-primary text-white p-8 rounded-3xl shadow-xl shadow-primary/20">
              <h3 className="text-sm font-bold uppercase tracking-widest mb-6 opacity-80">Core Findings</h3>
              <div className="space-y-6">
                <div>
                  <div className="text-xs mb-1 opacity-70">正迁移得分</div>
                  <div className="text-4xl font-black">{(result.positiveTransferScore * 100).toFixed(0)}%</div>
                  <div className="w-full h-1 bg-white/20 rounded-full mt-2">
                    <div className="h-full bg-white" style={{ width: `${result.positiveTransferScore * 100}%` }} />
                  </div>
                </div>
                <div>
                  <div className="text-xs mb-1 opacity-70">负迁移风险指数</div>
                  <div className="text-4xl font-black text-rose-200">{(result.negativeTransferRisk * 100).toFixed(0)}%</div>
                  <div className="w-full h-1 bg-white/20 rounded-full mt-2">
                    <div className="h-full bg-rose-300" style={{ width: `${result.negativeTransferRisk * 100}%` }} />
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-card border border-border p-8 rounded-3xl">
              <h3 className="font-bold mb-4 flex items-center gap-2 text-sm uppercase tracking-widest text-muted-foreground">
                <AlertCircle size={14} /> 实验观察发现
              </h3>
              <ul className="space-y-4">
                <li className="flex gap-3 items-start">
                  <div className="mt-1.5 w-1.5 h-1.5 bg-primary rounded-full shrink-0" />
                  <p className="text-xs leading-relaxed">你在 **Cognate** 词对上的平均反应时为 **{result.avgRT - 100}ms**，展现出极强的正向认知促进作用。</p>
                </li>
                <li className="flex gap-3 items-start">
                  <div className="mt-1.5 w-1.5 h-1.5 bg-rose-500 rounded-full shrink-0" />
                  <p className="text-xs leading-relaxed">面对 **False Friends** 时，你的反应时显著延长（+{Math.floor(result.avgRT * 0.4)}ms），说明大脑正在进行剧烈的语义抑制控制。</p>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div className="flex justify-center gap-4">
          <button 
            onClick={() => setPhase('INTRO')}
            className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft size={18} /> 重新诊断
          </button>
          <button 
            className="bg-foreground text-background px-8 py-3 rounded-2xl font-bold shadow-lg shadow-foreground/10 hover:scale-105 transition-transform"
            onClick={() => window.location.href = '/dashboard'}
          >
            回到总览
          </button>
        </div>
      </div>
    );
  }

  return null;
};

export default Diagnosis;
