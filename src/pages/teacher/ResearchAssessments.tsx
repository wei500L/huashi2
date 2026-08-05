import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  BarChart3,
  BookOpen,
  Download,
  FileSearch,
  Files,
  Plus,
  Search,
  Send,
  Upload,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader, SectionEyebrow, StatusBadge } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { saveBlob } from '@/lib/api';
import type { QuestionBankImportPreflightVO } from '@/lib/contracts';
import { assessmentPaperStatusLabel, assessmentPaperStatusTone, formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

type ResearchTab = 'bank' | 'questionnaires' | 'releases' | 'data';

const tabs: Array<{ id: ResearchTab; label: string; description: string; icon: typeof BookOpen }> = [
  { id: 'bank', label: '项目题库', description: '共享题目、版本与内容审核', icon: BookOpen },
  { id: 'questionnaires', label: '问卷', description: '问卷版本与编辑入口', icon: Files },
  { id: 'releases', label: '发布', description: '班级和参与码发布', icon: Send },
  { id: 'data', label: '数据', description: '完成率与规则分析', icon: BarChart3 },
];

const reviewTone = (status: string) => {
  if (status === 'APPROVED') return 'success' as const;
  if (status === 'REJECTED') return 'danger' as const;
  return 'warning' as const;
};

const reviewLabel = (status: string) => {
  if (status === 'APPROVED') return '审核通过';
  if (status === 'REJECTED') return '已驳回';
  return '待人工审核';
};

const EmptyPanel: React.FC<{ title: string; description: string; action?: React.ReactNode }> = ({ title, description, action }) => (
  <div className="rounded-[2.2rem] border border-dashed border-slate-300 bg-white/55 px-7 py-12 text-center dark:border-white/15 dark:bg-white/[0.02]">
    <FileSearch className="mx-auto h-9 w-9 text-slate-300 dark:text-white/20" />
    <h2 className="mt-4 text-lg font-black text-slate-800 dark:text-white">{title}</h2>
    <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-slate-500 dark:text-white/45">{description}</p>
    {action ? <div className="mt-6">{action}</div> : null}
  </div>
);

const ResearchAssessmentsPage: React.FC = () => {
  const navigate = useNavigate();
  const fileInputRef = React.useRef<HTMLInputElement | null>(null);
  const [activeTab, setActiveTab] = React.useState<ResearchTab>('bank');
  const [keyword, setKeyword] = React.useState('');
  const [tag, setTag] = React.useState('');
  const [reviewStatus, setReviewStatus] = React.useState('');
  const [preflight, setPreflight] = React.useState<QuestionBankImportPreflightVO | null>(null);
  const [actionError, setActionError] = React.useState<string | null>(null);
  const [uploading, setUploading] = React.useState(false);
  const [downloading, setDownloading] = React.useState(false);
  const [committing, setCommitting] = React.useState(false);

  const bankQuery = useQuery({
    queryKey: ['teacher-question-bank', keyword.trim(), tag, reviewStatus],
    queryFn: ({ signal }) => assessmentService.listQuestionBankItems({
      pageNo: 1,
      pageSize: 50,
      keyword: keyword.trim() || undefined,
      tag: tag || undefined,
      reviewStatus: reviewStatus || undefined,
    }, { signal }),
    enabled: activeTab === 'bank',
    retry: false,
  });

  const papersQuery = useQuery({
    queryKey: ['teacher-assessment-papers'],
    queryFn: ({ signal }) => assessmentService.listTeacherPapers({ signal }),
    enabled: activeTab !== 'bank',
  });

  const downloadTemplate = async () => {
    setDownloading(true);
    setActionError(null);
    try {
      saveBlob(await assessmentService.downloadQuestionBankImportTemplate(), 'lexibridge-question-bank-template.xlsx');
    } catch (error) {
      setActionError(getApiErrorMessage(error, '模板下载失败。'));
    } finally {
      setDownloading(false);
    }
  };

  const preflightFile = async (file?: File) => {
    if (!file) return;
    setUploading(true);
    setActionError(null);
    setPreflight(null);
    try {
      setPreflight(await assessmentService.preflightQuestionBankImport(file));
    } catch (error) {
      setActionError(getApiErrorMessage(error, '上传预检失败。'));
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const commitPreflight = async () => {
    if (!preflight || preflight.errorCount > 0) return;
    setCommitting(true);
    setActionError(null);
    try {
      await assessmentService.commitQuestionBankImport(preflight.importId, { confirmed: true });
      setPreflight({ ...preflight, status: 'COMMITTED' });
      await bankQuery.refetch();
    } catch (error) {
      setActionError(getApiErrorMessage(error, '导入提交失败。'));
    } finally {
      setCommitting(false);
    }
  };

  const papers = papersQuery.data || [];
  const publishedPapers = papers.filter((paper) => paper.latestPublishAt);

  return (
    <div className="space-y-7 pb-20">
      <PageHeader
        eyebrow="LEXI-BRIDGE RESEARCH"
        title="研究问卷"
        subtitle="从共享题库组织问卷版本，发布到班级或公开参与码，并查看规则分析与后续 AI 解读。"
        actions={
          <button type="button" onClick={() => navigate('/teacher/assessments/new')} className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white">
            <Plus size={16} />新建问卷
          </button>
        }
      />

      <nav aria-label="研究问卷板块" className="grid gap-3 rounded-[2rem] liquid-glass-panel p-3 md:grid-cols-4">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const selected = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              aria-current={selected ? 'page' : undefined}
              onClick={() => setActiveTab(tab.id)}
              className={`rounded-[1.45rem] px-4 py-4 text-left transition ${selected ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'hover:bg-white/70 dark:hover:bg-white/5'}`}
            >
              <div className="flex items-center gap-2 font-black"><Icon size={17} />{tab.label}</div>
              <div className={`mt-1 text-xs ${selected ? 'text-white/70' : 'text-slate-400 dark:text-white/35'}`}>{tab.description}</div>
            </button>
          );
        })}
      </nav>

      {activeTab === 'bank' ? (
        <div className="space-y-5">
          <section className="rounded-[2rem] liquid-glass-panel p-5">
            <div className="flex flex-col gap-3 xl:flex-row xl:items-center">
              <label className="relative min-w-0 flex-1">
                <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索题号、题干或目标词" className="w-full rounded-2xl border border-slate-200 bg-white py-3 pl-11 pr-4 outline-none focus:border-primary dark:border-white/10 dark:bg-white/5" />
              </label>
              <select value={tag} onChange={(event) => setTag(event.target.value)} className="rounded-2xl border border-slate-200 bg-white px-4 py-3 dark:border-white/10 dark:bg-slate-900">
                <option value="">全部研究标签</option>
                <option value="COGNATE">同源词</option>
                <option value="FALSE_FRIEND">假朋友</option>
                <option value="FRENCH_CONTROL">纯法语对照</option>
              </select>
              <select value={reviewStatus} onChange={(event) => setReviewStatus(event.target.value)} className="rounded-2xl border border-slate-200 bg-white px-4 py-3 dark:border-white/10 dark:bg-slate-900">
                <option value="">全部审核状态</option>
                <option value="APPROVED">审核通过</option>
                <option value="REVIEW_REQUIRED">待人工审核</option>
                <option value="REJECTED">已驳回</option>
              </select>
              <button type="button" disabled={downloading} onClick={() => void downloadTemplate()} className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 px-4 py-3 text-sm font-bold dark:border-white/10">
                <Download size={16} />{downloading ? '下载中…' : '下载导入模板'}
              </button>
              <button type="button" disabled={uploading} onClick={() => fileInputRef.current?.click()} className="btn-liquid inline-flex items-center justify-center gap-2 px-4 py-3 text-sm text-white">
                <Upload size={16} />{uploading ? '预检中…' : '上传并预检'}
              </button>
              <input ref={fileInputRef} type="file" accept=".xlsx,.json" className="hidden" onChange={(event) => void preflightFile(event.target.files?.[0])} />
            </div>
          </section>

          {actionError ? <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-600">{actionError}</div> : null}
          {preflight ? (
            <section className="rounded-[2rem] border border-amber-500/20 bg-amber-500/5 p-6">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div><SectionEyebrow>IMPORT PREFLIGHT</SectionEyebrow><h2 className="mt-2 text-lg font-black">{preflight.sourceFileName}</h2></div>
                <StatusBadge label={preflight.status} tone={preflight.errorCount ? 'danger' : preflight.reviewRequiredCount ? 'warning' : 'success'} />
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <StatusBadge label={`${preflight.rowCount} 行`} />
                <StatusBadge label={`${preflight.errorCount} 个错误`} tone={preflight.errorCount ? 'danger' : 'neutral'} />
                <StatusBadge label={`${preflight.warningCount} 个提醒`} tone="warning" />
                <StatusBadge label={`${preflight.reviewRequiredCount} 项待审核`} tone="warning" />
              </div>
              {preflight.issues.length ? <p className="mt-4 text-sm text-slate-500">首项：{preflight.issues[0].message}</p> : null}
              <div className="mt-5 flex flex-wrap items-center gap-3">
                <button type="button" disabled={committing || preflight.errorCount > 0 || preflight.status === 'COMMITTED'} onClick={() => void commitPreflight()} className="btn-liquid px-4 py-2 text-sm text-white">
                  {committing ? '提交中…' : preflight.status === 'COMMITTED' ? '已提交到题库' : '确认并提交到题库'}
                </button>
                {preflight.reviewRequiredCount > 0 && preflight.status !== 'COMMITTED' ? <span className="text-xs text-amber-700 dark:text-amber-300">提交后仍需完成内容审核，审核通过前不可发布。</span> : null}
              </div>
            </section>
          ) : null}

          {bankQuery.isLoading ? <div className="rounded-[2rem] liquid-glass-panel p-8 text-sm text-slate-500">正在加载项目题库…</div> : null}
          {!bankQuery.isLoading && !bankQuery.data?.records.length ? (
            <EmptyPanel title="题库中暂无匹配题目" description={bankQuery.error ? '题库服务尚未返回数据。你仍可先下载模板，或上传 XLSX / JSON 执行预检。' : '调整关键词或标签筛选，或使用标准模板导入第一批研究题目。'} />
          ) : null}
          <div className="grid gap-4 xl:grid-cols-2">
            {(bankQuery.data?.records || []).map((item) => (
              <article key={`${item.itemId}-${item.version}`} className="rounded-[2rem] liquid-glass-panel p-6">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div><SectionEyebrow>{item.questionCode}</SectionEyebrow><h2 className="mt-2 line-clamp-3 font-bold leading-7 text-slate-900 dark:text-white">{item.stemText}</h2></div>
                  <StatusBadge label={reviewLabel(item.reviewStatus)} tone={reviewTone(item.reviewStatus)} />
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  <StatusBadge label={`v${item.version}`} tone="info" />
                  <StatusBadge label={item.questionType} />
                  {item.transferCategory ? <StatusBadge label={item.transferCategory} /> : null}
                  {item.contextLevel ? <StatusBadge label={item.contextLevel} /> : null}
                  {item.tags.map((itemTag) => <StatusBadge key={itemTag} label={itemTag} />)}
                </div>
                <p className="mt-4 text-xs text-slate-400">更新于 {formatDateTime(item.updatedAt)}</p>
              </article>
            ))}
          </div>
        </div>
      ) : null}

      {activeTab === 'questionnaires' ? (
        <div className="space-y-5">
          {papersQuery.isLoading ? <div className="rounded-[2rem] liquid-glass-panel p-8 text-sm text-slate-500">正在加载问卷版本…</div> : null}
          {!papersQuery.isLoading && !papers.length ? <EmptyPanel title="还没有问卷版本" description="从项目题库选择题目创建第一份问卷；现有课堂测评编辑器会继续保持兼容。" action={<button className="btn-liquid px-5 py-3 text-sm text-white" onClick={() => navigate('/teacher/assessments/new')}>新建问卷</button>} /> : null}
          <div className="grid gap-5 xl:grid-cols-2">
            {papers.map((paper) => (
              <button key={paper.paperId} type="button" onClick={() => navigate(`/teacher/assessments/${paper.paperId}`)} className="rounded-[2rem] liquid-glass-panel p-6 text-left transition hover:border-primary/40">
                <div className="flex items-start justify-between gap-3"><SectionEyebrow>{paper.paperCode}</SectionEyebrow><StatusBadge label={assessmentPaperStatusLabel(paper.status)} tone={assessmentPaperStatusTone(paper.status)} /></div>
                <h2 className="mt-3 text-xl font-black text-slate-900 dark:text-white">{paper.title}</h2>
                <p className="mt-2 text-sm text-slate-500">{paper.questionCount} 题 · {paper.durationMinutes} 分钟 · 更新于 {formatDateTime(paper.updatedAt)}</p>
              </button>
            ))}
          </div>
        </div>
      ) : null}

      {activeTab === 'releases' ? (
        publishedPapers.length ? (
          <div className="grid gap-5 xl:grid-cols-2">{publishedPapers.map((paper) => <article key={paper.paperId} className="rounded-[2rem] liquid-glass-panel p-6"><SectionEyebrow>{paper.paperCode}</SectionEyebrow><h2 className="mt-3 text-xl font-black">{paper.title}</h2><p className="mt-3 text-sm text-slate-500">最近发布：{formatDateTime(paper.latestPublishAt)}</p><button onClick={() => navigate(`/teacher/assessments/${paper.paperId}`)} className="mt-5 text-sm font-bold text-primary">查看问卷与发布记录 →</button></article>)}</div>
        ) : <EmptyPanel title="暂无研究问卷发布" description="问卷通过内容审核后，可在问卷详情中选择班级发布或公开参与码发布。" />
      ) : null}

      {activeTab === 'data' ? (
        <div className="space-y-5">
          <div className="grid gap-4 md:grid-cols-3"><div className="rounded-[2rem] liquid-glass-panel p-6"><div className="text-sm text-slate-500">问卷版本</div><div className="mt-2 text-3xl font-black">{papers.length}</div></div><div className="rounded-[2rem] liquid-glass-panel p-6"><div className="text-sm text-slate-500">已有发布</div><div className="mt-2 text-3xl font-black">{publishedPapers.length}</div></div><div className="rounded-[2rem] liquid-glass-panel p-6"><div className="text-sm text-slate-500">完成率</div><div className="mt-2 text-3xl font-black">—</div></div></div>
          <EmptyPanel title="尚无可汇总的研究数据" description="产生答卷后，这里将展示完成率、题目难度、干扰项分布、维度正确率和反应时统计；敏感信息默认不进入导出。" />
        </div>
      ) : null}
    </div>
  );
};

export default ResearchAssessmentsPage;
