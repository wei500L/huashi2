import React from 'react';
import { ArrowUpRight, LockKeyhole, Microscope } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const normalizeReleaseCode = (value: string) => value.replace(/\s+/g, '').toUpperCase();

const ResearchLandingPage: React.FC = () => {
  const navigate = useNavigate();
  const [releaseCode, setReleaseCode] = React.useState('');
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    const normalized = normalizeReleaseCode(releaseCode);
    setReleaseCode(normalized);
    if (!normalized.startsWith('RES-') || normalized.length <= 4) {
      setErrorMessage('请输入以 RES- 开头的有效发布编号。');
      return;
    }
    setErrorMessage(null);
    navigate(`/research/${encodeURIComponent(normalized)}`);
  };

  return (
    <main className="research-entry min-w-0">
      <header className="research-nav min-w-0">
        <div className="research-wordmark min-w-0">
          <span className="research-wordmark-mark">EF</span>
          <span className="min-w-0 truncate">TRANSFER / RESEARCH</span>
        </div>
        <div className="research-nav-meta">
          <span>PUBLIC STUDY</span>
          <span className="research-nav-dot" />
          <span>VOLUNTARY PARTICIPATION</span>
        </div>
      </header>

      <section className="research-hero min-w-0" aria-labelledby="research-entry-title">
        <div className="research-hero-copy min-w-0">
          <p className="research-kicker"><span className="research-kicker-line" />社会研究测试 / RESEARCH SURVEY</p>
          <h1 id="research-entry-title" className="research-heading"><span>参与研究，</span><span>留下你的语言轨迹。</span></h1>
          <p className="research-lede">
            这里是 EF Transfer 的公开研究问卷入口。问卷属于自愿参与的社会研究，不是课堂必测任务；系统不会在此公开列出任何研究项目。
          </p>
          <div className="research-hero-notes">
            <span>01 / 输入发布编号</span><span>02 / 验证参与码</span><span>03 / 保存并提交答卷</span>
          </div>
        </div>

        <div className="research-access-card min-w-0">
          <div className="research-card-topline"><span>RELEASE ACCESS</span><LockKeyhole size={15} className="shrink-0" /></div>
          <div className="mt-6 inline-flex rounded-full border border-border-subtle p-3 text-primary sm:mt-8"><Microscope size={24} /></div>
          <h2>输入研究人员提供的发布编号。</h2>
          <p>发布编号以 RES- 开头。进入具体问卷后，再使用独立参与码验证身份并恢复作答进度。</p>
          <form className="research-code-form min-w-0" onSubmit={submit} noValidate>
            <label htmlFor="release-code">发布编号 / RELEASE CODE</label>
            <input
              id="release-code"
              value={releaseCode}
              onChange={(event) => {
                setReleaseCode(event.target.value.toUpperCase());
                if (errorMessage) setErrorMessage(null);
              }}
              placeholder="RES-XXXXXXXXXXXX"
              autoComplete="off"
              spellCheck={false}
              className="min-w-0"
            />
            {errorMessage ? <p className="research-form-error" role="alert">{errorMessage}</p> : null}
            <button type="submit" className="research-primary-button w-full sm:w-auto">
              进入研究问卷 <ArrowUpRight size={18} className="shrink-0" />
            </button>
          </form>
          <div className="research-access-foot">具体编号有效性由研究服务确认。</div>
        </div>
      </section>

      <footer className="research-footer min-w-0">
        <span>EF TRANSFER PLATFORM</span>
        <span>research participation portal</span>
      </footer>
    </main>
  );
};

export default ResearchLandingPage;
