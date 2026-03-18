import React from 'react';
import { 
  TrendingUp, 
  AlertTriangle, 
  Zap, 
  Calendar, 
  ChevronRight, 
  Sparkles,
  RefreshCw,
  Flame,
  ArrowUpRight
} from 'lucide-react';
import { useDashboard } from '@/hooks/useDashboard';
import { StatCard, PageHeader } from '@/components/common';
import { ChartCard } from '@/components/common/ChartCard';
import { TrainingTask, ErrorWordPair } from '@/types/learning';

const Dashboard: React.FC = () => {
  const { data, loading, error } = useDashboard();

  if (error) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-rose-500">
      <AlertTriangle size={48} />
      <p className="mt-4 font-bold">{error}</p>
      <button className="mt-4 px-6 py-2 bg-primary text-white rounded-lg" onClick={() => window.location.reload()}>重试</button>
    </div>
  );

  // ECharts Configurations
  const trendOption = data ? {
    tooltip: { trigger: 'axis' },
    legend: { data: ['正确率 (%)', '反应时 (ms)'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '5%', containLabel: true },
    xAxis: { type: 'category', data: data.trends.dates, axisLine: { show: false } },
    yAxis: [
      { type: 'value', min: 0, max: 100, position: 'left', name: '正确率' },
      { type: 'value', position: 'right', name: '反应时' }
    ],
    series: [
      { 
        name: '正确率 (%)', 
        type: 'line', 
        smooth: true, 
        data: data.trends.scores,
        itemStyle: { color: '#3b82f6' },
        areaStyle: { color: 'rgba(59, 130, 246, 0.1)' }
      },
      { 
        name: '反应时 (ms)', 
        type: 'line', 
        yAxisIndex: 1, 
        smooth: true, 
        data: data.trends.rt,
        itemStyle: { color: '#f59e0b' }
      }
    ]
  } : {};

  const distributionOption = data ? {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: data.errorDistribution
    }]
  } : {};

  return (
    <div className="space-y-8 pb-12">
      {/* 顶部欢迎区 */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Sparkles className="text-amber-500" size={18} />
            <span className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Cognitive Profile</span>
          </div>
          <h1 className="text-3xl font-black flex items-center gap-3">
            你好, {data?.userProfile.name}
            <span className="flex items-center gap-1 text-xs bg-amber-100 text-amber-700 px-2 py-1 rounded-full border border-amber-200">
              <Flame size={12} /> {data?.userProfile.streak} 天连胜
            </span>
          </h1>
          <p className="text-muted-foreground mt-2 max-w-lg">
            你的当前语言水平为 <span className="font-bold text-foreground">{data?.userProfile.level}</span>。基于最近的实验数据，你的正迁移表现非常稳定，建议专注于克服同形异义词干扰。
          </p>
        </div>
        <div className="flex gap-4">
          <button className="flex items-center gap-2 px-6 py-3 bg-primary text-white font-bold rounded-2xl shadow-xl shadow-primary/20 hover:scale-105 transition-all">
            开始新诊断 <ChevronRight size={18} />
          </button>
        </div>
      </div>

      {/* 关键指标卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard 
          title="正迁移得分" 
          value={`${(data?.metrics.positiveTransferScore || 0) * 100}%`} 
          icon={TrendingUp} 
          trend={{ value: 12, isUp: true }}
          color="text-blue-500"
        />
        <StatCard 
          title="负迁移风险" 
          value={`${(data?.metrics.negativeTransferRisk || 0) * 100}%`} 
          icon={AlertTriangle} 
          trend={{ value: 5, isUp: false }}
          color="text-rose-500"
        />
        <StatCard 
          title="语境敏感度" 
          value={`${(data?.metrics.contextSensitivity || 0) * 100}%`} 
          icon={RefreshCw} 
          color="text-emerald-500"
        />
        <StatCard 
          title="平均反应时" 
          value={`${data?.metrics.avgResponseTime}ms`} 
          icon={Zap} 
          color="text-amber-500"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* 左侧图表区 */}
        <div className="lg:col-span-2 space-y-8">
          <ChartCard 
            title="学习表现趋势 (最近 7 天)" 
            option={trendOption} 
            loading={loading}
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <ChartCard 
              title="错误分布统计" 
              option={distributionOption} 
              loading={loading}
              height={280}
            />
            <div className="bg-card border border-border rounded-xl p-6 flex flex-col justify-between">
              <div className="flex justify-between items-start">
                <h3 className="text-sm font-semibold">认知负荷仪表盘</h3>
                <span className="text-[10px] bg-muted px-2 py-1 rounded-full uppercase tracking-tighter">Negative Risk</span>
              </div>
              <div className="flex-1 flex flex-col items-center justify-center py-4">
                <div className="text-4xl font-black text-rose-500 mb-1">{data?.metrics.negativeTransferRisk}</div>
                <div className="text-xs text-muted-foreground">High Risk Threshold: 0.5</div>
                <div className="w-full h-2 bg-muted rounded-full mt-6 overflow-hidden">
                  <div className="h-full bg-rose-500 transition-all" style={{ width: `${(data?.metrics.negativeTransferRisk || 0) * 100}%` }} />
                </div>
              </div>
              <p className="text-[11px] text-muted-foreground leading-relaxed">
                当前风险处于安全范围内，但由于你在 False Friends 类词汇上的犹豫时长较长，风险指数略有波动。
              </p>
            </div>
          </div>
        </div>

        {/* 右侧侧边栏：任务与错题 */}
        <div className="space-y-8">
          {/* 推荐任务 */}
          <section>
            <h3 className="text-sm font-bold mb-4 flex items-center gap-2 uppercase tracking-widest text-muted-foreground">
              Today's Missions
            </h3>
            <div className="space-y-4">
              {data?.recommendedTasks.map(task => (
                <div key={task.id} className="group bg-card border border-border p-4 rounded-2xl hover:border-primary/50 hover:shadow-lg transition-all cursor-pointer">
                  <div className="flex justify-between items-start mb-2">
                    <span className={cn(
                      "text-[10px] font-bold px-2 py-0.5 rounded-full",
                      task.priority === 'HIGH' ? "bg-rose-100 text-rose-600" : "bg-blue-100 text-blue-600"
                    )}>
                      {task.priority}
                    </span>
                    <span className="text-[10px] text-muted-foreground">{task.estimatedTime} min</span>
                  </div>
                  <h4 className="font-bold text-sm group-hover:text-primary transition-colors">{task.title}</h4>
                  <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{task.description}</p>
                </div>
              ))}
            </div>
          </section>

          {/* 最近错题 */}
          <section>
            <h3 className="text-sm font-bold mb-4 flex items-center gap-2 uppercase tracking-widest text-muted-foreground">
              Critical Word Pairs
            </h3>
            <div className="bg-card border border-border rounded-2xl divide-y divide-border">
              {data?.recentErrors.map(error => (
                <div key={error.id} className="p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center text-xs font-bold font-mono">
                      {error.en[0].toUpperCase()}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold">{error.en}</span>
                        <span className="text-[10px] text-muted-foreground">vs</span>
                        <span className="text-xs font-bold">{error.fr}</span>
                      </div>
                      <p className="text-[10px] text-rose-500 font-medium">错误频次: {error.errorCount} 次</p>
                    </div>
                  </div>
                  <button className="p-2 hover:bg-muted rounded-full transition-colors text-muted-foreground">
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

// Utility function (already in components but for local safety in this snippet)
function cn(...inputs: any[]) {
  return inputs.filter(Boolean).join(' ');
}

export default Dashboard;
