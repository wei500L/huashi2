import React from 'react';
import { ArrowRight, KeyRound, Microscope, ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader, SectionEyebrow } from '@/components/common';
import { getApiErrorMessage } from '@/lib/api';
import { publicAssessmentService } from '@/lib/services';

const normalizeReleaseCode = (value: string) => value.replace(/\s+/g, '').toUpperCase();

const StudentResearchPage: React.FC = () => {
  const navigate = useNavigate();
  const [releaseCode, setReleaseCode] = React.useState('');
  const [checking, setChecking] = React.useState(false);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (checking) return;

    const normalized = normalizeReleaseCode(releaseCode);
    setReleaseCode(normalized);
    if (!normalized.startsWith('RES-') || normalized.length <= 4) {
      setErrorMessage('请输入以 RES- 开头的有效发布编号。');
      return;
    }

    setChecking(true);
    setErrorMessage(null);
    try {
      await publicAssessmentService.getMetadata(normalized);
      navigate(`/research/${encodeURIComponent(normalized)}`);
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, '无法确认该研究问卷，请检查发布编号后重试。'));
    } finally {
      setChecking(false);
    }
  };

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow="RESEARCH PARTICIPATION"
        title="研究问卷"
        subtitle="从学生工作区进入自愿参与的社会研究问卷；课堂测评与研究参与保持独立。"
      />

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(22rem,0.9fr)]">
        <div className="rounded-[2.4rem] liquid-glass-panel p-7 sm:p-9">
          <div className="inline-flex rounded-2xl bg-primary/10 p-3 text-primary"><Microscope size={22} /></div>
          <SectionEyebrow className="mt-6">VOLUNTARY STUDY</SectionEyebrow>
          <h2 className="mt-3 text-2xl font-black text-slate-900 dark:text-white">研究参与不是课堂必测任务</h2>
          <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-600 dark:text-white/55">
            只有在研究人员向你提供发布编号与独立参与码后才需要进入。平台不会在这里公开列出研究项目，也不会把研究问卷混入“课堂测评”。
          </p>

          <div className="mt-8 grid gap-4 sm:grid-cols-3">
            {[
              ['01', '确认发布编号', '输入 RES- 开头的编号，由服务端确认问卷状态。'],
              ['02', '验证参与码', '参与码独立于学生账号，用于保护研究身份和进度。'],
              ['03', '保存并提交', '沿用公开问卷的自动保存、恢复与结果查询能力。'],
            ].map(([step, title, description]) => (
              <div key={step} className="rounded-[1.5rem] border border-slate-200/80 bg-white/55 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                <div className="text-xs font-black tracking-[0.18em] text-primary">{step}</div>
                <div className="mt-3 text-sm font-black text-slate-900 dark:text-white">{title}</div>
                <p className="mt-2 text-xs leading-6 text-slate-500 dark:text-white/45">{description}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-[2.4rem] border border-primary/15 bg-primary/[0.06] p-7 sm:p-9 dark:bg-primary/[0.08]">
          <div className="flex items-center justify-between text-xs font-black tracking-[0.16em] text-primary">
            <span>RELEASE ACCESS</span>
            <ShieldCheck size={18} />
          </div>
          <h2 className="mt-7 text-xl font-black text-slate-900 dark:text-white">输入研究问卷发布编号</h2>
          <p className="mt-3 text-sm leading-6 text-slate-600 dark:text-white/55">
            发布编号中的空格会被自动移除并转换为大写。确认有效后将进入独立的参与码验证页面。
          </p>

          <form onSubmit={submit} className="mt-8 space-y-4" noValidate>
            <label htmlFor="student-research-release-code" className="block text-xs font-black tracking-[0.12em] text-slate-600 dark:text-white/60">
              发布编号 / RELEASE CODE
            </label>
            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 focus-within:border-primary/50 dark:border-white/10 dark:bg-white/5">
              <KeyRound size={18} className="shrink-0 text-primary" />
              <input
                id="student-research-release-code"
                value={releaseCode}
                onChange={(event) => {
                  setReleaseCode(event.target.value.toUpperCase());
                  if (errorMessage) setErrorMessage(null);
                }}
                placeholder="RES-XXXXXXXXXXXX"
                autoComplete="off"
                spellCheck={false}
                className="min-w-0 flex-1 bg-transparent text-sm font-bold uppercase tracking-wider text-slate-900 outline-none placeholder:text-slate-400 dark:text-white"
              />
            </div>
            {errorMessage ? <p role="alert" className="text-sm leading-6 text-rose-500">{errorMessage}</p> : null}
            <button type="submit" disabled={checking} className="btn-liquid inline-flex w-full items-center justify-center gap-2 px-5 py-3 text-sm font-bold text-white disabled:opacity-60">
              {checking ? '正在确认问卷…' : '确认并进入研究问卷'} <ArrowRight size={17} />
            </button>
          </form>
        </div>
      </section>
    </div>
  );
};

export default StudentResearchPage;
