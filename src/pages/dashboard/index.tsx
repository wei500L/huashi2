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
import { StatCard, PageHeader } from '@/components/common';
import { ChartCard } from '@/components/common/ChartCard';
import { TrainingTask, ErrorWordPair } from '@/types/learning';

function cn(...inputs: any[]) {
  return inputs.filter(Boolean).join(' ');
}

const Dashboard: React.FC = () => {
  const { data, loading, error } = useDashboard();
  const [timeRange, setTimeRange] = useState('7D');

  if (error) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-rose-500 liquid-glass p-8 rounded-3xl max-w-md mx-auto mt-20 shadow-[0_0_50px_rgba(244,63,94,0.2)]">
      <AlertTriangle size={64} className="drop-shadow-[0_0_15px_rgba(244,63,94,0.8)] animate-pulse" />
      <p className="mt-6 font-bold text-xl tracking-wider uppercase">Connection Interrupted</p>
      <p className="mt-2 text-sm text-rose-400/80">{error}</p>
      <button className="mt-8 px-8 py-3 bg-rose-500/20 text-rose-400 font-bold rounded-2xl border border-rose-500/30 hover:bg-rose-500/40 hover:shadow-[0_0_20px_rgba(244,63,94,0.4)] transition-all" onClick={() => window.location.reload()}>REINITIALIZE</button>
    </div>
  );

  const trendOption = data ? {
    backgroundColor: 'transparent',
    tooltip: { 
      trigger: 'axis',
      backgroundColor: 'rgba(10, 5, 30, 0.85)',
      borderColor: 'rgba(139, 92, 246, 0.3)',
      textStyle: { color: '#fff', fontFamily: 'Inter' },
      padding: 12,
      borderRadius: 12,
      extraCssText: 'backdrop-filter: blur(10px); box-shadow: 0 8px 32px rgba(0,0,0,0.5);'
    },
    legend: { 
      data: ['正确率 (%)', '反应时 (ms)'], 
      bottom: 0,
      textStyle: { color: 'rgba(255,255,255,0.6)', fontFamily: 'Inter' },
      icon: 'circle',
      itemWidth: 10
    },
    grid: { left: '2%', right: '2%', bottom: '15%', top: '5%', containLabel: true, show: false },
    xAxis: { 
      type: 'category', 
      data: data.trends.dates, 
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: 'rgba(255,255,255,0.4)', fontFamily: 'Inter', margin: 16 }
    },
    yAxis: [
      { 
        type: 'value', min: 0, max: 100, position: 'left', name: '正确率',
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.03)' } },
        axisLabel: { color: 'rgba(255,255,255,0.4)', fontFamily: 'Inter' },
        nameTextStyle: { color: 'rgba(255,255,255,0.4)', fontFamily: 'Inter', padding: [0, 0, 0, 20] }
      },
      { 
        type: 'value', position: 'right', name: '反应时',
        splitLine: { show: false },
        axisLabel: { color: 'rgba(255,255,255,0.4)', fontFamily: 'Inter' },
        nameTextStyle: { color: 'rgba(255,255,255,0.4)', fontFamily: 'Inter', padding: [0, 20, 0, 0] }
      }
    ],
    series: [
      { 
        name: '正确率 (%)', 
        type: 'line', 
        smooth: 0.6, 
        symbol: 'circle',
        symbolSize: 8,
        showSymbol: false,
        data: data.trends.scores,
        itemStyle: { color: '#8b5cf6', borderColor: '#fff', borderWidth: 2, shadowColor: '#8b5cf6', shadowBlur: 10 },
        lineStyle: { width: 4, shadowColor: 'rgba(139,92,246,0.6)', shadowBlur: 15 },
        areaStyle: { 
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: 'rgba(139,92,246,0.4)' }, { offset: 1, color: 'rgba(139,92,246,0.01)' }]
          } 
        }
      },
      { 
        name: '反应时 (ms)', 
        type: 'line', 
        yAxisIndex: 1, 
        smooth: 0.6, 
        symbol: 'circle',
        symbolSize: 8,
        showSymbol: false,
        data: data.trends.rt,
        itemStyle: { color: '#f59e0b', borderColor: '#fff', borderWidth: 2, shadowColor: '#f59e0b', shadowBlur: 10 },
        lineStyle: { width: 3, shadowColor: 'rgba(245,158,11,0.6)', shadowBlur: 15 },
        areaStyle: { 
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: 'rgba(245,158,11,0.15)' }, { offset: 1, color: 'rgba(245,158,11,0.01)' }]
          } 
        }
      }
    ]
  } : {};

  const distributionOption = data ? {
    backgroundColor: 'transparent',
    tooltip: { 
      trigger: 'item',
      backgroundColor: 'rgba(10, 5, 30, 0.85)',
      borderColor: 'rgba(139, 92, 246, 0.3)',
      textStyle: { color: '#fff', fontFamily: 'Inter' },
      padding: 12,
      borderRadius: 12,
      extraCssText: 'backdrop-filter: blur(10px); box-shadow: 0 8px 32px rgba(0,0,0,0.5);'
    },
    color: ['#8b5cf6', '#ec4899', '#3b82f6', '#10b981', '#f59e0b'],
    series: [{
      type: 'pie',
      radius: ['55%', '80%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { 
        borderRadius: 20, 
        borderColor: '#060514', 
        borderWidth: 6,
        shadowBlur: 20,
        shadowColor: 'rgba(0,0,0,0.8)'
      },
      label: { show: false },
      data: data.errorDistribution
    }]
  } : {};

  return (
    <div className="space-y-10 pb-12 relative z-10">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-8 p-8 liquid-glass-panel rounded-3xl edge-light fluid-texture">
        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 bg-amber-500/10 rounded-lg border border-amber-500/20 shadow-[0_0_10px_rgba(245,158,11,0.2)]">
              <Sparkles className="text-amber-400 drop-shadow-[0_0_8px_rgba(245,158,11,0.8)]" size={18} />
            </div>
            <span className="text-xs font-bold uppercase tracking-widest text-amber-400/80">Cognitive Profile Active</span>
          </div>
          <h1 className="text-4xl font-black flex items-center gap-4 text-white drop-shadow-[0_0_15px_rgba(255,255,255,0.2)]">
            你好, {data?.userProfile?.name}
            <span className="flex items-center gap-1.5 text-xs bg-gradient-to-r from-amber-500/20 to-orange-500/20 text-amber-300 px-3 py-1.5 rounded-full border border-amber-500/30 shadow-[0_0_15px_rgba(245,158,11,0.3)] backdrop-blur-md">
              <Flame size={14} className="animate-pulse" /> {data?.userProfile?.streak} 天连胜
            </span>
          </h1>
          <p className="text-white/60 mt-4 max-w-2xl leading-relaxed text-sm font-medium">
            你的当前语言水平为 <span className="font-black text-primary drop-shadow-[0_0_8px_rgba(139,92,246,0.8)] text-glow-primary">{data?.userProfile?.level}</span>。基于最近的实验数据，你的正迁移表现非常稳定，建议专注于克服同形异义词干扰。
          </p>
        </div>
        <div className="flex gap-4 relative z-10">
          <button className="group flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-primary to-purple-600 text-white font-black rounded-2xl shadow-[0_0_30px_rgba(139,92,246,0.5)] hover:shadow-[0_0_40px_rgba(139,92,246,0.7)] hover:scale-105 border border-white/20 transition-all duration-300">
            开始新诊断 <ChevronRight size={20} className="group-hover:translate-x-1 transition-transform" />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard 
          title="正迁移得分" 
          value={`${((data?.metrics?.positiveTransferScore || 0) * 100).toFixed(0)}%`} 
          icon={TrendingUp} 
          trend={{ value: 12, isUp: true }}
          color="text-blue-500"
        />
        <StatCard 
          title="负迁移风险" 
          value={`${((data?.metrics?.negativeTransferRisk || 0) * 100).toFixed(0)}%`} 
          icon={AlertTriangle} 
          trend={{ value: 5, isUp: false }}
          color="text-rose-500"
        />
        <StatCard 
          title="语境敏感度" 
          value={`${((data?.metrics?.contextSensitivity || 0) * 100).toFixed(0)}%`} 
          icon={RefreshCw} 
          color="text-emerald-500"
        />
        <StatCard 
          title="平均反应时" 
          value={`${data?.metrics?.avgResponseTime || 0}ms`} 
          icon={Zap} 
          color="text-amber-500"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <ChartCard 
            title="学习表现趋势" 
            option={trendOption} 
            loading={loading}
            extra={
              <div className="relative group cursor-pointer">
                <button className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-xs font-medium text-white/70 hover:text-white hover:bg-white/10 transition-colors backdrop-blur-md">
                  <Calendar size={12} />
                  {timeRange === '7D' ? '最近 7 天' : '最近 30 天'}
                  <ChevronDown size={12} />
                </button>
                <div className="absolute top-full right-0 mt-2 w-32 bg-black/60 backdrop-blur-xl border border-white/10 rounded-xl shadow-[0_10px_30px_rgba(0,0,0,0.8)] opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 z-50 overflow-hidden">
                  <div className="px-4 py-2 text-xs text-white/80 hover:bg-primary/20 hover:text-primary cursor-pointer transition-colors" onClick={() => setTimeRange('7D')}>最近 7 天</div>
                  <div className="px-4 py-2 text-xs text-white/80 hover:bg-primary/20 hover:text-primary cursor-pointer transition-colors" onClick={() => setTimeRange('30D')}>最近 30 天</div>
                </div>
              </div>
            }
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <ChartCard 
              title="错误分布统计" 
              option={distributionOption} 
              loading={loading}
              height={300}
            />
            <div className="liquid-glass-panel edge-light fluid-texture p-8 flex flex-col justify-between rounded-3xl group">
              <div className="flex justify-between items-start relative z-10">
                <h3 className="text-sm font-bold tracking-widest uppercase text-white/90 drop-shadow-[0_0_8px_rgba(255,255,255,0.3)]">认知负荷仪表盘</h3>
                <span className="text-[10px] bg-rose-500/20 text-rose-400 border border-rose-500/30 px-3 py-1 rounded-full uppercase tracking-tighter font-black shadow-[0_0_10px_rgba(244,63,94,0.3)]">Negative Risk</span>
              </div>
              <div className="flex-1 flex flex-col items-center justify-center py-6 relative z-10">
                <div className="relative">
                  <div className="text-6xl font-black text-glow-rose mb-2 tracking-tighter">{data?.metrics?.negativeTransferRisk || 0}</div>
                  <div className="absolute -inset-4 bg-rose-500/10 rounded-full blur-2xl -z-10 animate-pulse" />
                </div>
                <div className="text-xs font-bold text-white/40 tracking-widest uppercase mt-2">High Risk Threshold: 0.5</div>
                <div className="w-full h-3 bg-black/40 rounded-full mt-8 overflow-hidden border border-white/5 shadow-[inset_0_2px_4px_rgba(0,0,0,0.5)]">
                  <div className="h-full bg-gradient-to-r from-rose-600 to-rose-400 relative" style={{ width: `${(data?.metrics?.negativeTransferRisk || 0) * 100}%` }}>
                    <div className="absolute inset-0 bg-[linear-gradient(90deg,transparent,rgba(255,255,255,0.5),transparent)] animate-[fluid-flow_2s_linear_infinite]" style={{ backgroundSize: '200% 100%' }} />
                  </div>
                </div>
              </div>
              <p className="text-[11px] text-white/50 leading-relaxed relative z-10 font-medium bg-black/20 p-4 rounded-xl border border-white/5 backdrop-blur-md">
                当前风险处于安全范围内，但由于你在 <span className="text-rose-400 font-bold">False Friends</span> 类词汇上的犹豫时长较长，风险指数略有波动。
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-8">
          <section className="liquid-glass p-6 rounded-3xl edge-light fluid-texture">
            <h3 className="text-sm font-bold mb-6 flex items-center gap-2 uppercase tracking-widest text-white/70 relative z-10">
              <Zap size={16} className="text-primary" /> Today's Missions
            </h3>
            <div className="space-y-4 relative z-10">
              {data?.recommendedTasks?.map(task => (
                <div key={task.id} className="group bg-white/5 border border-white/10 p-5 rounded-2xl hover:bg-white/10 hover:border-primary/50 hover:shadow-[0_10px_30px_rgba(139,92,246,0.2)] transition-all cursor-pointer backdrop-blur-md relative overflow-hidden">
                  <div className="absolute top-0 left-0 w-1 h-full bg-transparent group-hover:bg-primary transition-colors shadow-[0_0_10px_rgba(139,92,246,0.8)]" />
                  <div className="flex justify-between items-start mb-3">
                    <span className={cn(
                      "text-[10px] font-black px-2.5 py-1 rounded-full border shadow-[0_0_10px_currentColor]",
                      task.priority === 'HIGH' ? "bg-rose-500/10 text-rose-400 border-rose-500/30" : "bg-blue-500/10 text-blue-400 border-blue-500/30"
                    )}>
                      {task.priority}
                    </span>
                    <span className="text-[10px] font-bold text-white/50 flex items-center gap-1">
                      <Calendar size={10} /> {task.estimatedTime} min
                    </span>
                  </div>
                  <h4 className="font-bold text-sm text-white/90 group-hover:text-primary transition-all">{task.title}</h4>
                  <p className="text-xs text-white/50 mt-2 line-clamp-2 leading-relaxed">{task.description}</p>
                </div>
              ))}
            </div>
          </section>

          <section className="liquid-glass p-6 rounded-3xl edge-light fluid-texture">
            <h3 className="text-sm font-bold mb-6 flex items-center gap-2 uppercase tracking-widest text-white/70 relative z-10">
              <AlertTriangle size={16} className="text-rose-500" /> Critical Word Pairs
            </h3>
            <div className="bg-black/20 border border-white/5 rounded-2xl divide-y divide-white/5 backdrop-blur-md relative z-10">
              {data?.recentErrors?.map(error => (
                <div key={error.id} className="p-4 flex items-center justify-between hover:bg-white/5 transition-colors group cursor-pointer first:rounded-t-2xl last:rounded-b-2xl">
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-white/10 to-transparent border border-white/10 flex items-center justify-center text-sm font-black font-mono text-white/80 group-hover:border-primary/50 group-hover:text-primary transition-all">
                      {error.en[0].toUpperCase()}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-white/90">{error.en}</span>
                        <span className="text-[10px] text-white/40 italic">vs</span>
                        <span className="text-sm font-bold text-white/90">{error.fr}</span>
                      </div>
                      <p className="text-[11px] text-rose-400 font-bold mt-1 flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-rose-500 shadow-[0_0_5px_rgba(244,63,94,0.8)] animate-pulse" />
                        错误频次: {error.errorCount} 次
                      </p>
                    </div>
                  </div>
                  <button className="p-2 bg-white/5 hover:bg-primary hover:text-white rounded-full transition-all text-white/50 border border-white/10 shadow-sm group-hover:shadow-[0_0_10px_rgba(139,92,246,0.5)]">
                    <ArrowUpRight size={14} />
                  </button>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
