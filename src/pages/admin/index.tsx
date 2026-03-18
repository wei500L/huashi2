import React, { useState } from 'react';
import { 
  Users, 
  BarChart, 
  BookOpen, 
  Settings, 
  ShieldAlert, 
  Search, 
  Filter, 
  ChevronRight, 
  MoreVertical,
  ArrowUpRight,
  Plus,
  Edit2,
  Trash2,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { useAdmin } from '@/hooks/useAdmin';
import { StatCard, PageHeader } from '@/components/common';
import { ChartCard } from '@/components/common/ChartCard';
import { StudentSummary, AdminWordPair } from '@/types/admin';

const AdminPanel: React.FC = () => {
  const { classStats, students, vocab, interventions, loading } = useAdmin();
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'STUDENTS' | 'VOCAB' | 'INTERVENTION'>('OVERVIEW');

  // 1. 班级风险分布柱状图
  const riskOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['0-0.2', '0.2-0.4', '0.4-0.6', '0.6-0.8', '0.8-1.0'] },
    yAxis: { type: 'value', name: '学生人数' },
    series: [{ 
      data: [12, 10, 5, 3, 2], 
      type: 'bar', 
      itemStyle: { color: (params: any) => params.dataIndex >= 3 ? '#ef4444' : '#3b82f6' } 
    }]
  };

  // 2. 错误类型饼图
  const errorOption = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: 45, name: '同形异义词混淆' },
        { value: 25, name: '语音负迁移' },
        { value: 30, name: '语境辨析失误' }
      ]
    }]
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-20">
      <PageHeader 
        title="试点课程指挥中心" 
        subtitle={`${classStats?.className || ''} | 管理试点班级的认知迁移数据与干预逻辑`}
        actions={
          <div className="flex gap-2 bg-muted p-1 rounded-xl">
            {(['OVERVIEW', 'STUDENTS', 'VOCAB', 'INTERVENTION'] as const).map(tab => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={cn(
                  "px-4 py-1.5 rounded-lg text-xs font-bold transition-all",
                  activeTab === tab ? "bg-card text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
                )}
              >
                {tab === 'OVERVIEW' ? '班级总览' : tab === 'STUDENTS' ? '学生列表' : tab === 'VOCAB' ? '词对管理' : '干预策略'}
              </button>
            ))}
          </div>
        }
      />

      {activeTab === 'OVERVIEW' && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <StatCard title="全班平均正迁移" value={`${(classStats?.avgPositiveScore || 0) * 100}%`} icon={Users} color="text-blue-500" />
            <StatCard title="高风险学生占比" value="15.6%" icon={ShieldAlert} color="text-rose-500" />
            <StatCard title="训练平均进度" value={`${(classStats?.completionRate || 0) * 100}%`} icon={CheckCircle2} color="text-emerald-500" />
            <StatCard title="平均负迁移风险" value={classStats?.avgNegativeRisk || 0} icon={AlertCircle} color="text-amber-500" />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <ChartCard title="全班负迁移风险分布 (阶梯)" option={riskOption} loading={loading} />
            <ChartCard title="主流错误类型画像" option={errorOption} loading={loading} />
          </div>
        </>
      )}

      {activeTab === 'STUDENTS' && (
        <div className="bg-card border border-border rounded-3xl overflow-hidden shadow-sm">
          <div className="p-6 border-b border-border flex items-center justify-between bg-muted/20">
            <div className="relative max-w-sm w-full">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={16} />
              <input type="text" placeholder="搜索学生姓名、ID..." className="w-full bg-background border-border rounded-xl py-2 pl-10 pr-4 text-sm" />
            </div>
            <div className="flex gap-2">
              <button className="p-2 border border-border rounded-lg hover:bg-muted"><Filter size={16} /></button>
            </div>
          </div>
          <table className="w-full text-sm">
            <thead className="bg-muted/30 text-muted-foreground uppercase text-[10px] font-black tracking-widest">
              <tr>
                <th className="px-6 py-4 text-left">学生姓名</th>
                <th className="px-6 py-4 text-left">语言水平</th>
                <th className="px-6 py-4 text-left">正迁移得分</th>
                <th className="px-6 py-4 text-left">负迁移风险</th>
                <th className="px-6 py-4 text-left">最后活动</th>
                <th className="px-6 py-4 text-right">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {students.map(s => (
                <tr key={s.id} className="hover:bg-muted/20 transition-all cursor-pointer group">
                  <td className="px-6 py-4 font-bold flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs">
                      {s.name[0]}
                    </div>
                    {s.name}
                    {s.status === 'WARNING' && <div className="w-2 h-2 rounded-full bg-rose-500 animate-pulse" />}
                  </td>
                  <td className="px-6 py-4 text-muted-foreground text-xs">{s.enLevel} / {s.frLevel}</td>
                  <td className="px-6 py-4 font-mono font-bold">{s.positiveTransferScore}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <div className="flex-1 h-1.5 w-20 bg-muted rounded-full overflow-hidden">
                        <div className={cn("h-full", s.negativeTransferRisk > 0.5 ? "bg-rose-500" : "bg-primary")} style={{ width: `${s.negativeTransferRisk * 100}%` }} />
                      </div>
                      <span className="text-[10px] font-bold">{s.negativeTransferRisk}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-muted-foreground text-xs">{s.lastActive}</td>
                  <td className="px-6 py-4 text-right">
                    <button className="p-2 hover:bg-muted rounded-lg text-primary opacity-0 group-hover:opacity-100 transition-all">
                      <ArrowUpRight size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'VOCAB' && (
        <div className="space-y-6">
          <div className="flex justify-between items-center">
            <h3 className="font-bold flex items-center gap-2 uppercase tracking-widest text-muted-foreground text-xs">
              <BookOpen size={14} /> 实验词库 (Current Pool)
            </h3>
            <button className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-xl text-xs font-bold shadow-lg shadow-primary/20">
              <Plus size={14} /> 添加新词对
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {vocab.map(v => (
              <div key={v.id} className="bg-card border border-border p-6 rounded-3xl hover:border-primary transition-all shadow-sm">
                <div className="flex justify-between items-start mb-4">
                  <span className={cn(
                    "px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-tighter",
                    v.type === 'FALSE_FRIEND' ? "bg-rose-50 text-rose-600" : "bg-blue-50 text-blue-600"
                  )}>
                    {v.type}
                  </span>
                  <div className="flex gap-2">
                    <button className="p-1 hover:bg-muted rounded text-muted-foreground"><Edit2 size={12} /></button>
                    <button className="p-1 hover:bg-muted rounded text-rose-500"><Trash2 size={12} /></button>
                  </div>
                </div>
                <div className="flex items-center gap-4 mb-4">
                  <div className="flex-1">
                    <div className="text-xl font-black">{v.en}</div>
                    <div className="text-[10px] text-muted-foreground uppercase font-bold tracking-widest">English</div>
                  </div>
                  <div className="w-px h-8 bg-border" />
                  <div className="flex-1">
                    <div className="text-xl font-black">{v.fr}</div>
                    <div className="text-[10px] text-muted-foreground uppercase font-bold tracking-widest">French</div>
                  </div>
                </div>
                <div className="p-3 bg-muted/50 rounded-xl text-xs font-medium text-slate-600 mb-4 italic text-center">
                  "{v.zh}"
                </div>
                <div className="flex justify-between items-center pt-4 border-t border-border">
                  <div className="flex items-center gap-1">
                    <span className="text-[10px] text-muted-foreground">难度:</span>
                    <div className="flex gap-0.5">
                      {[1,2,3,4,5].map(star => (
                        <div key={star} className={cn("w-1.5 h-1.5 rounded-full", star <= v.difficulty ? "bg-amber-400" : "bg-muted")} />
                      ))}
                    </div>
                  </div>
                  <div className="text-[10px] text-muted-foreground">语义相似度: {(v.semanticSimilarity * 100).toFixed(0)}%</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'INTERVENTION' && (
        <div className="max-w-4xl mx-auto space-y-6">
          <div className="bg-primary/5 border border-primary/20 p-8 rounded-[2.5rem]">
            <h2 className="text-2xl font-black mb-2 flex items-center gap-3">
              <Brain className="text-primary" size={28} /> AI 干预辅助
            </h2>
            <p className="text-sm text-slate-600 max-w-xl">
              系统已根据班级近期的负迁移表现，自动计算了以下干预方案。你可以审核并一键下发给受影响的学生。
            </p>
          </div>
          
          <div className="space-y-4">
            {interventions.map(i => (
              <div key={i.id} className="bg-card border border-border p-6 rounded-3xl flex items-center justify-between hover:border-primary transition-all">
                <div className="flex gap-6 items-center">
                  <div className={cn(
                    "w-12 h-12 rounded-2xl flex items-center justify-center",
                    i.priority === 'URGENT' ? "bg-rose-100 text-rose-600" : "bg-amber-100 text-amber-600"
                  )}>
                    {i.priority === 'URGENT' ? <ShieldAlert size={24} /> : <Target size={24} />}
                  </div>
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-black text-sm">{students.find(s => s.id === i.studentId)?.name}</span>
                      <span className="text-[10px] text-muted-foreground uppercase font-bold tracking-widest">Warning Identified</span>
                    </div>
                    <p className="text-xs font-bold text-foreground mb-1">{i.patternDetected}</p>
                    <p className="text-xs text-muted-foreground">建议: {i.suggestedAction}</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  {i.applied ? (
                    <span className="flex items-center gap-1.5 text-xs font-black text-emerald-600 bg-emerald-50 px-3 py-1.5 rounded-xl">
                      <CheckCircle2 size={14} /> 已执行
                    </span>
                  ) : (
                    <button className="bg-foreground text-background px-6 py-2.5 rounded-xl text-xs font-black hover:scale-105 transition-all">
                      执行干预
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

// Utility function
function cn(...inputs: any[]) {
  return inputs.filter(Boolean).join(' ');
}

export default AdminPanel;
