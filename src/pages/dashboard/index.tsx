import React, { useState } from 'react';
import { 
  TrendingUp, 
  AlertTriangle, 
  Zap, 
  Calendar, 
  ChevronRight, 
  Sparkles,
  RefreshCw,
  Flame,
  ArrowUpRight,
  ChevronDown
} from 'lucide-react';
import { useDashboard } from '@/hooks/useDashboard';
import { StatCard, PageHeader, AnimatedNumber, Magnetic } from '@/components/common';
import { ChartCard } from '@/components/common/ChartCard';
import { TrainingTask, ErrorWordPair } from '@/types/learning';
import { motion, AnimatePresence } from 'framer-motion';

function cn(...inputs: any[]) {
  return inputs.filter(Boolean).join(' ');
}

const Dashboard: React.FC = () => {
  const { data, loading, error } = useDashboard();
  const [timeRange, setTimeRange] = useState('7D');
  const [isTimeRangeOpen, setIsTimeRangeOpen] = useState(false);

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: { staggerChildren: 0.1 }
    }
  };

  const itemVariants = {
    hidden: { y: 30, opacity: 0, filter: "blur(10px)" },
    visible: { 
      y: 0, 
      opacity: 1, 
      filter: "blur(0px)",
      transition: { duration: 0.8, ease: [0.16, 1, 0.3, 1] } 
    }
  };

  if (error) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-rose-500 liquid-glass p-12 rounded-[3rem] max-w-lg mx-auto mt-20 shadow-[0_0_100px_rgba(244,63,94,0.15)] edge-light">
      <AlertTriangle size={80} className="drop-shadow-[0_0_25px_rgba(244,63,94,0.8)] animate-pulse mb-8" />
      <h2 className="font-black text-2xl tracking-tighter uppercase mb-2 text-slate-900 dark:text-rose-500">Neural Link Severed</h2>
      <p className="text-center text-slate-500 dark:text-rose-400/60 font-medium mb-10 max-w-xs">{error}</p>
      <button className="btn-liquid text-white px-10 py-4" onClick={() => window.location.reload()}>RE-ESTABLISH CONNECTION</button>
    </div>
  );

  const trendOption = data ? {
    backgroundColor: 'transparent',
    tooltip: { 
      trigger: 'axis',
      backgroundColor: 'rgba(10, 5, 30, 0.9)',
      borderColor: 'rgba(139, 92, 246, 0.4)',
      textStyle: { color: '#fff', fontFamily: 'Inter', fontWeight: 600 },
      padding: [15, 20],
      borderRadius: 20,
      extraCssText: 'backdrop-filter: blur(20px); border-width: 2px;'
    },
    legend: { 
      data: ['正确率 (%)', '反应时 (ms)'], 
      bottom: 0,
      textStyle: { color: '#64748b', fontFamily: 'Inter', fontSize: 11, fontWeight: 700 },
      icon: 'circle',
      itemGap: 30
    },
    grid: { left: '2%', right: '2%', bottom: '15%', top: '5%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: data.trends.dates, 
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontFamily: 'Inter', margin: 20, fontWeight: 600 }
    },
    yAxis: [
      { 
        type: 'value', min: 0, max: 100, position: 'left',
        splitLine: { lineStyle: { color: 'rgba(0,0,0,0.03)' } },
        axisLabel: { color: '#94a3b8', fontFamily: 'Inter', fontWeight: 600 }
      },
      { 
        type: 'value', position: 'right',
        splitLine: { show: false },
        axisLabel: { color: '#94a3b8', fontFamily: 'Inter', fontWeight: 600 }
      }
    ],
    series: [
      { 
        name: '正确率 (%)', 
        type: 'line', 
        smooth: 0.5, 
        symbol: 'none',
        data: data.trends.scores,
        itemStyle: { color: '#8b5cf6' },
        lineStyle: { width: 5, shadowColor: 'rgba(139,92,246,0.5)', shadowBlur: 20 },
        areaStyle: { 
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: 'rgba(139,92,246,0.3)' }, { offset: 1, color: 'rgba(139,92,246,0)' }]
          } 
        }
      },
      { 
        name: '反应时 (ms)', 
        type: 'line', 
        yAxisIndex: 1, 
        smooth: 0.5, 
        symbol: 'none',
        data: data.trends.rt,
        itemStyle: { color: '#f59e0b' },
        lineStyle: { width: 4, shadowColor: 'rgba(245,158,11,0.4)', shadowBlur: 20 },
        areaStyle: { 
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: 'rgba(245,158,11,0.1)' }, { offset: 1, color: 'rgba(245,158,11,0)' }]
          } 
        }
      }
    ]
  } : {};

  const distributionOption = data ? {
    backgroundColor: 'transparent',
    color: ['#8b5cf6', '#d946ef', '#3b82f6', '#10b981'],
    series: [{
      type: 'pie',
      radius: ['60%', '85%'],
      avoidLabelOverlap: false,
      itemStyle: { 
        borderRadius: 15, 
        borderColor: 'inherit', 
        borderWidth: 5,
        shadowBlur: 30,
        shadowColor: 'rgba(0,0,0,0.1)'
      },
      label: { show: false },
      data: data.errorDistribution
    }]
  } : {};

  return (
    <motion.div 
      variants={containerVariants}
      initial="hidden"
      animate="visible"
      className="space-y-12 pb-20"
    >
      {/* 1. Welcoming Hero Section */}
      <motion.div variants={itemVariants} className="flex flex-col md:flex-row md:items-center justify-between gap-10 p-10 md:p-16 liquid-glass-panel rounded-[3.5rem] edge-light fluid-texture text-foreground">
        <div className="flex-1">
          <div className="flex items-center gap-3 mb-8">
            <div className="p-2 bg-primary/10 rounded-xl border border-primary/20">
              <Sparkles className="text-primary animate-pulse" size={20} />
            </div>
            <span className="text-[10px] font-black uppercase tracking-[0.4em] text-slate-400 dark:text-primary/60">Neural Engine Synchronized</span>
          </div>
          <h1 className="text-5xl md:text-7xl font-black text-slate-900 dark:text-white tracking-tighter mb-8 flex flex-wrap items-center gap-x-6 leading-[1.1]">
            Hello, <span className="text-gradient-animated drop-shadow-xl">{data?.userProfile?.name}</span>
            <span className="text-xs font-black bg-white dark:bg-white/5 border border-slate-200 dark:border-white/10 px-5 py-2.5 rounded-full flex items-center gap-2 text-amber-600 dark:text-amber-400 shadow-sm dark:shadow-[0_0_20px_rgba(245,158,11,0.2)] ml-4 tracking-widest uppercase">
              <Flame size={16} fill="currentColor" className="animate-pulse" /> {data?.userProfile?.streak} DAY STREAK
            </span>
          </h1>
          <p className="text-lg md:text-xl text-slate-500 dark:text-white/40 max-w-3xl font-medium leading-relaxed">
            Your current cognitive mastery is <span className="text-slate-900 dark:text-white font-black">{data?.userProfile?.level}</span>.
            Real-time analysis suggests stable <span className="text-emerald-600 dark:text-emerald-400/80 font-bold">Positive Transfer</span> patterns. We recommend focusing on homograph discrimination to optimize neural processing.
          </p>
        </div>
        <div className="shrink-0">
          <Magnetic strength={0.15}>
            <button className="btn-liquid text-white flex items-center gap-4 px-12 py-7 text-xl group shadow-2xl">
              START NEW DIAGNOSIS 
              <ChevronRight size={28} className="group-hover:translate-x-1.5 transition-transform duration-500" />
            </button>
          </Magnetic>
        </div>
      </motion.div>

      {/* 2. Key Metrics Grid */}
      <motion.div variants={itemVariants} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 md:gap-10">
        <StatCard title="Positive Score" value={`${((data?.metrics?.positiveTransferScore || 0) * 100).toFixed(0)}%`} icon={TrendingUp} trend={{ value: 12, isUp: true }} color="text-blue-600 dark:text-blue-500" />
        <StatCard title="Negative Risk" value={`${((data?.metrics?.negativeTransferRisk || 0) * 100).toFixed(0)}%`} icon={AlertTriangle} trend={{ value: 5, isUp: false }} color="text-rose-600 dark:text-rose-500" />
        <StatCard title="Context Sensitivity" value={`${((data?.metrics?.contextSensitivity || 0) * 100).toFixed(0)}%`} icon={RefreshCw} color="text-emerald-600 dark:text-emerald-500" />
        <StatCard title="Latency (avg)" value={`${data?.metrics?.avgResponseTime || 0}ms`} icon={Zap} color="text-amber-600 dark:text-amber-500" />
      </motion.div>

      {/* 3. Charts & Detailed Analysis */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10 md:gap-12">
        <motion.div variants={itemVariants} className="lg:col-span-2 space-y-10 md:space-y-12">
          <ChartCard 
            title="Performance Trajectory" 
            option={trendOption} 
            loading={loading}
            extra={
              <div className="relative">
                <button 
                  onClick={() => setIsTimeRangeOpen(!isTimeRangeOpen)}
                  onBlur={() => setTimeout(() => setIsTimeRangeOpen(false), 200)}
                  className="flex items-center gap-3 px-5 py-2.5 rounded-2xl bg-white/50 dark:bg-white/5 border border-slate-200 dark:border-white/10 text-[10px] font-black uppercase tracking-widest text-slate-500 dark:text-white/50 hover:text-primary dark:hover:text-white transition-all backdrop-blur-xl"
                >
                  <Calendar size={14} /> {timeRange === '7D' ? 'Last 7 Days' : 'Last Month'} <ChevronDown size={14} className={cn("transition-transform duration-300", isTimeRangeOpen && "rotate-180")} />
                </button>
                
                <AnimatePresence>
                  {isTimeRangeOpen && (
                    <motion.div 
                      initial={{ opacity: 0, y: 10, scale: 0.95 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: 10, scale: 0.95 }}
                      transition={{ duration: 0.2, ease: "easeOut" }}
                      className="absolute top-full right-0 mt-3 w-40 liquid-glass p-2 z-50 overflow-hidden shadow-2xl"
                    >
                      <div className="px-4 py-3 text-[10px] font-bold text-slate-400 dark:text-white/40 hover:text-primary hover:bg-primary/10 rounded-xl cursor-pointer transition-all" onClick={() => { setTimeRange('7D'); setIsTimeRangeOpen(false); }}>LAST 7 DAYS</div>
                      <div className="px-4 py-3 text-[10px] font-bold text-slate-400 dark:text-white/40 hover:text-primary hover:bg-primary/10 rounded-xl cursor-pointer transition-all" onClick={() => { setTimeRange('30D'); setIsTimeRangeOpen(false); }}>LAST 30 DAYS</div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            }
          />
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-10 md:gap-12">
            <ChartCard title="Error Distribution" option={distributionOption} loading={loading} height={320} />
            <div className="liquid-glass-panel edge-light fluid-texture p-10 flex flex-col justify-between rounded-[2.5rem]">
              <div className="flex justify-between items-start">
                <h3 className="text-[10px] font-black tracking-[0.3em] text-slate-400 dark:text-white/30 uppercase">Cognitive Load Index</h3>
                <span className="text-[9px] font-black bg-rose-500/10 dark:bg-rose-500/20 text-rose-600 dark:text-rose-400 border border-rose-500/20 dark:border-rose-500/30 px-3 py-1.5 rounded-full uppercase tracking-widest">High Risk Alert</span>
              </div>
              <div className="flex-1 flex flex-col items-center justify-center py-8">
                <div className="relative group">
                  <div className="text-7xl md:text-8xl font-black text-glow-rose tracking-tighter mb-4 transition-transform group-hover:scale-110 duration-500 tabular-nums">
                    <AnimatedNumber value={data?.metrics?.negativeTransferRisk || 0} format={(v) => v.toFixed(2)} />
                  </div>
                  <div className="absolute -inset-8 bg-rose-500/5 dark:bg-rose-500/10 rounded-full blur-[40px] -z-10 animate-pulse-slow" />
                </div>
                <p className="text-[10px] font-black text-slate-400 dark:text-white/20 tracking-widest uppercase mt-4">Threshold: 0.5 Critical</p>
                <div className="w-full h-4 bg-slate-100 dark:bg-black/40 rounded-full mt-10 overflow-hidden border border-slate-200 dark:border-white/5 shadow-inner p-1">
                  <div className="h-full bg-gradient-to-r from-rose-600 via-rose-400 to-rose-500 rounded-full relative" style={{ width: `${(data?.metrics?.negativeTransferRisk || 0) * 100}%` }}>
                    <div className="absolute inset-0 bg-[linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent)] animate-fluid-flow" style={{ backgroundSize: '200% 100%' }} />
                  </div>
                </div>
              </div>
              <p className="text-xs text-slate-500 dark:text-white/40 font-medium leading-relaxed bg-white/50 dark:bg-white/5 p-6 rounded-2xl border border-slate-100 dark:border-white/5">
                Current metrics indicate a stable baseline, though <span className="text-rose-600 dark:text-rose-400 font-bold">False Friends</span> interference remains a localized peak.
              </p>
            </div>
          </div>
        </motion.div>

        {/* 4. Side Lists */}
        <motion.div variants={itemVariants} className="space-y-10 md:space-y-12">
          <section className="liquid-glass p-8 rounded-[2.5rem] edge-light">
            <h3 className="text-[10px] font-black mb-8 flex items-center gap-3 uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
              <Zap size={16} className="text-primary" /> Current Objectives
            </h3>
            <div className="space-y-5">
              {data?.recommendedTasks?.map(task => (
                <motion.div 
                  key={task.id} 
                  whileHover={{ x: 5 }}
                  className="group bg-slate-50/50 dark:bg-white/[0.03] border border-slate-100 dark:border-white/[0.05] p-6 rounded-3xl hover:bg-white dark:hover:bg-white/[0.08] hover:border-primary/40 transition-all cursor-pointer relative shadow-sm hover:shadow-md"
                >
                  <div className="flex justify-between items-start mb-4">
                    <span className={cn(
                      "text-[9px] font-black px-3 py-1 rounded-lg border uppercase tracking-widest",
                      task.priority === 'HIGH' ? "bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20" : "bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20"
                    )}>{task.priority}</span>
                    <span className="text-[9px] font-bold text-slate-400 dark:text-white/20 uppercase tracking-tighter">{task.estimatedTime} MIN</span>
                  </div>
                  <h4 className="font-black text-base text-slate-800 dark:text-white group-hover:text-primary transition-colors mb-2 uppercase tracking-tight">{task.title}</h4>
                  <p className="text-xs text-slate-500 dark:text-white/30 font-medium leading-relaxed line-clamp-2">{task.description}</p>
                </motion.div>
              ))}
            </div>
          </section>

          <section className="liquid-glass p-8 rounded-[2.5rem] edge-light">
            <h3 className="text-[10px] font-black mb-8 flex items-center gap-3 uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">
              <AlertTriangle size={16} className="text-rose-500" /> High-Risk Tokens
            </h3>
            <div className="bg-slate-50/50 dark:bg-black/30 border border-slate-100 dark:border-white/5 rounded-3xl divide-y divide-slate-100 dark:divide-white/5 overflow-hidden shadow-sm">
              {data?.recentErrors?.map(error => (
                <motion.div 
                  key={error.id} 
                  whileHover={{ x: 5 }}
                  className="group p-5 flex items-center justify-between group cursor-pointer bg-slate-50/50 dark:bg-transparent hover:bg-white dark:hover:bg-white/[0.03] transition-all"
                >
                  <div className="flex items-center gap-5">
                    <div className="w-12 h-12 rounded-2xl bg-white dark:bg-white/5 border border-slate-200 dark:border-white/10 flex items-center justify-center text-lg font-black text-slate-700 dark:text-white/80 group-hover:border-primary/50 group-hover:text-primary transition-all shadow-sm">
                      {error.en[0].toUpperCase()}
                    </div>
                    <div>
                      <div className="flex items-center gap-3 mb-1">
                        <span className="text-sm font-black text-slate-800 dark:text-white/90">{error.en}</span>
                        <div className="w-1.5 h-1.5 rounded-full bg-slate-300 dark:bg-white/10" />
                        <span className="text-sm font-black text-slate-800 dark:text-white/90">{error.fr}</span>
                      </div>
                      <p className="text-[10px] text-rose-600 dark:text-rose-400 font-black uppercase tracking-widest flex items-center gap-2">
                        <span className="w-1.5 h-1.5 rounded-full bg-rose-500 shadow-[0_0_10px_rgba(244,63,94,1)] animate-pulse" />
                        Error Rate: {error.errorCount}X
                      </p>
                    </div>
                  </div>
                  <ArrowUpRight size={18} className="text-slate-300 dark:text-white/20 group-hover:text-primary transition-all" />
                </motion.div>
              ))}
            </div>
          </section>
        </motion.div>
      </div>
    </motion.div>
  );
};

export default Dashboard;
