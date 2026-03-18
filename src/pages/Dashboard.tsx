import React from 'react';
import ReactECharts from 'echarts-for-react';
import { useAuthStore } from '../store/useAuthStore';
import { 
  TrendingUp, 
  AlertCircle, 
  Zap, 
  Target 
} from 'lucide-react';

const Dashboard: React.FC = () => {
  const { user } = useAuthStore();

  if (!user) return null;

  // Radar Chart Data
  const radarOption = {
    title: { text: '认知迁移多维度分析', left: 'center', top: 10, textStyle: { fontSize: 14 } },
    radar: {
      indicator: [
        { name: '正迁移能力', max: 1 },
        { name: '语境敏感度', max: 1 },
        { name: '语义辨析力', max: 1 },
        { name: '反应准确性', max: 1 },
        { name: '抗干扰能力', max: 1 },
      ]
    },
    series: [{
      name: '用户指标',
      type: 'radar',
      data: [{
        value: [
          user.transferStats.positiveTransferScore,
          user.transferStats.contextSensitivity,
          user.transferStats.semanticDiscrimination,
          0.85, // Mock Accuracy
          1 - user.transferStats.negativeTransferRisk
        ],
        name: '当前状态',
        areaStyle: { color: 'rgba(59, 130, 246, 0.3)' },
        lineStyle: { color: '#3b82f6' },
        itemStyle: { color: '#3b82f6' }
      }]
    }]
  };

  // Trend Chart Data (Mock)
  const trendOption = {
    xAxis: { type: 'category', data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] },
    yAxis: { type: 'value' },
    series: [{
      data: [65, 72, 68, 85, 92, 88, 95],
      type: 'line',
      smooth: true,
      lineStyle: { color: '#6366f1', width: 3 },
      areaStyle: { color: 'rgba(99, 102, 241, 0.1)' }
    }],
    grid: { left: '10%', right: '5%', bottom: '15%', top: '10%' }
  };

  const statCards = [
    { label: '正迁移得分', value: `${(user.transferStats.positiveTransferScore * 100).toFixed(0)}%`, icon: TrendingUp, color: 'text-emerald-600', bg: 'bg-emerald-50' },
    { label: '负迁移风险', value: `${(user.transferStats.negativeTransferRisk * 100).toFixed(0)}%`, icon: AlertCircle, color: 'text-rose-600', bg: 'bg-rose-50' },
    { label: '平均反应时', value: '420ms', icon: Zap, color: 'text-amber-600', bg: 'bg-amber-50' },
    { label: '综合词汇能力', value: 'B2+', icon: Target, color: 'text-blue-600', bg: 'bg-blue-100' },
  ];

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col gap-2">
        <h2 className="text-2xl font-bold">欢迎回来, {user.username}</h2>
        <p className="text-slate-500">这是您基于英法双语认知迁移模型的学情分析报告</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat, i) => (
          <div key={i} className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
            <div className={`p-3 rounded-lg ${stat.bg} ${stat.color}`}>
              <stat.icon size={24} />
            </div>
            <div>
              <p className="text-sm text-slate-500">{stat.label}</p>
              <p className="text-2xl font-bold">{stat.value}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Radar Chart */}
        <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm h-[400px]">
          <ReactECharts option={radarOption} style={{ height: '100%' }} />
        </div>

        {/* Trend Chart */}
        <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm h-[400px]">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-bold text-slate-800 dark:text-slate-100">迁移能力进步趋势</h3>
            <select className="text-xs border rounded px-2 py-1 bg-transparent">
              <option>最近7天</option>
              <option>最近30天</option>
            </select>
          </div>
          <ReactECharts option={trendOption} style={{ height: '300px' }} />
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
