import React from 'react';
import { ArrowUpRight, Check, CheckCircle2, ChevronLeft, ChevronRight, Clock3, LockKeyhole, Save, Send } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import type {
  PublicAssessmentAttemptVO,
  PublicAssessmentMetadataVO,
  PublicAssessmentQuestionVO,
  PublicAssessmentResultVO,
} from '@/lib/contracts';
import { publicAssessmentService } from '@/lib/services';

type ResponsesByOrder = Record<number, string[]>;
type JustificationsByOrder = Record<number, string>;

const hasResponse = (values?: string[]) => Boolean(values?.some((value) => value.trim().length > 0));

function hydrateResponses(attempt: PublicAssessmentAttemptVO) {
  const responses: ResponsesByOrder = {};
  const justifications: JustificationsByOrder = {};
  attempt.questions.forEach((question) => {
    responses[question.questionOrder] = question.responses || [];
    justifications[question.questionOrder] = question.justificationText || '';
  });
  return { responses, justifications };
}

const ThreadMap: React.FC = () => (
  <svg className="research-thread-map" viewBox="0 0 720 410" aria-hidden="true" focusable="false">
    <path d="M20 280 C 170 55, 260 350, 390 155 S 560 65, 705 220" />
    <path d="M10 95 C 155 240, 230 12, 360 250 S 560 365, 710 112" />
    <path d="M84 360 C 190 225, 285 205, 375 290 S 560 360, 650 35" />
    <circle cx="390" cy="155" r="5" />
    <circle cx="360" cy="250" r="5" />
    <circle cx="375" cy="290" r="5" />
  </svg>
);

const ResearchEntry: React.FC<{
  metadata: PublicAssessmentMetadataVO | null;
  participationCode: string;
  verifying: boolean;
  errorMessage: string | null;
  onCodeChange: (value: string) => void;
  onVerify: (event: React.FormEvent) => void;
}> = ({ metadata, participationCode, verifying, errorMessage, onCodeChange, onVerify }) => {
  const title = metadata?.title || 'Language in transit';
  const description = metadata?.description || metadata?.instructionsText || '一项关于英语与法语之间词义迁移的公开研究。';
  return (
    <main className="research-entry">
      <header className="research-nav">
        <div className="research-wordmark"><span className="research-wordmark-mark">EF</span><span>TRANSFER / RESEARCH</span></div>
        <div className="research-nav-meta"><span>PUBLIC STUDY</span><span className="research-nav-dot" /><span>2026 · V1</span></div>
      </header>

      <section className="research-hero" aria-labelledby="research-title">
        <div className="research-hero-copy">
          <p className="research-kicker"><span className="research-kicker-line" />语言迁移实验室 / LEXI-BRIDGE</p>
          <div className="research-language-loop" aria-label="English to French to Chinese">
            <span>meaning</span><span className="research-loop-arrow">→</span><span>sens</span><span className="research-loop-arrow">→</span><span>意义</span>
          </div>
          <h1 id="research-title" className="research-heading"><span>Language</span><span>in transit.</span></h1>
          <p className="research-lede">我们追踪一个词如何穿越语言、语境与记忆。你的每一次选择，都会成为这张迁移地图上的一个坐标。</p>
          <div className="research-hero-notes">
            <span>01 / 研究材料</span><span>02 / 语义判断</span><span>03 / 迁移分析</span>
          </div>
        </div>
        <div className="research-hero-visual" aria-hidden="true">
          <ThreadMap />
          <div className="research-visual-caption"><span>word / monde / 世界</span><span>semantic drift study</span></div>
          <div className="research-halftone" />
        </div>
      </section>

      <section className="research-entry-grid" aria-label="参与研究">
        <div className="research-material-panel">
          <div className="research-section-index">A / THE MATERIAL</div>
          <h2>先看见材料，<br /><em>再决定它的方向。</em></h2>
          <p>{description}</p>
          <div className="research-facts">
            <div><strong>{metadata?.questionCount ?? '—'}</strong><span>items / 题</span></div>
            <div><strong>{metadata?.durationMinutes ?? '—'}</strong><span>min / 分钟</span></div>
            <div><strong>EN ↔ FR</strong><span>language pair</span></div>
          </div>
          <div className="research-material-source">CURRENT PAPER / {title}</div>
        </div>
        <div className="research-access-card">
          <div className="research-card-topline"><span>PARTICIPANT ACCESS</span><LockKeyhole size={15} strokeWidth={1.8} /></div>
          <h2>带着研究员提供的参与码进入。</h2>
          <p>没有公开注册。你的回答会以匿名方式保存，仅用于本次研究。</p>
          <form onSubmit={onVerify} className="research-code-form">
            <label htmlFor="participation-code">参与码 / ACCESS CODE</label>
            <input id="participation-code" value={participationCode} onChange={(event) => onCodeChange(event.target.value.toUpperCase())} placeholder="XXXX-XXXX-XXXX" autoComplete="one-time-code" />
            {errorMessage ? <p className="research-form-error" role="alert">{errorMessage}</p> : null}
            <button type="submit" disabled={verifying || !participationCode.trim()} className="research-primary-button">{verifying ? '正在验证…' : '验证并开始'}<ArrowUpRight size={18} /></button>
          </form>
          <div className="research-access-foot"><Check size={14} /> 可中途离开，进度会自动保存</div>
        </div>
      </section>

      <section className="research-method" aria-labelledby="method-title">
        <div><div className="research-section-index">B / HOW WE READ</div><h2 id="method-title">这不是语言考试。<br /><em>这是迁移的现场记录。</em></h2></div>
        <div className="research-method-list">
          <article><span>01</span><div><h3>看见上下文</h3><p>短句、词组与文化线索组成研究材料，先读完整，再做判断。</p></div></article>
          <article><span>02</span><div><h3>标记语义距离</h3><p>我们关心熟悉感从哪里来，也关心“看似相同”何时开始偏移。</p></div></article>
          <article><span>03</span><div><h3>返回你的路径</h3><p>提交后可查看规则评分与研究分析，解释在完整作答后呈现。</p></div></article>
        </div>
      </section>

      <footer className="research-footer"><span>EF TRANSFER PLATFORM</span><span>an editorial study of language movement</span></footer>
    </main>
  );
};

const PublicQuestion: React.FC<{
  question: PublicAssessmentQuestionVO;
  responses: string[];
  justification: string;
  disabled: boolean;
  onResponsesChange: (responses: string[]) => void;
  onJustificationChange: (value: string) => void;
}> = ({ question, responses, justification, disabled, onResponsesChange, onJustificationChange }) => {
  const type = question.questionType;
  if (type === 'INSTRUCTION') return <p className="research-instruction">{question.promptText || question.stemText}</p>;
  if (type === 'SINGLE_CHOICE' || type === 'INFORMED_CONSENT' || type === 'TRUE_FALSE_WITH_JUSTIFICATION') {
    return <div className="research-options">{question.options.map((option) => <label key={option.key} className={`research-option ${responses[0] === option.key ? 'is-selected' : ''}`}><input type="radio" name={`question-${question.questionOrder}`} value={option.key} checked={responses[0] === option.key} disabled={disabled} onChange={() => onResponsesChange([option.key])} /><span>{option.label}</span></label>)}
      {type === 'TRUE_FALSE_WITH_JUSTIFICATION' && responses[0] === 'F' ? <label className="research-justification">请说明判断为错误的原因<textarea value={justification} disabled={disabled} onChange={(event) => onJustificationChange(event.target.value)} rows={4} /></label> : null}
    </div>;
  }
  if (type === 'MULTIPLE_CHOICE') return <div className="research-options">{question.options.map((option) => { const selected = responses.includes(option.key); return <label key={option.key} className={`research-option ${selected ? 'is-selected' : ''}`}><input type="checkbox" value={option.key} checked={selected} disabled={disabled} onChange={() => onResponsesChange(selected ? responses.filter((value) => value !== option.key) : [...responses, option.key])} /><span>{option.label}</span></label>; })}</div>;
  return <input type={type === 'NUMBER' ? 'number' : 'text'} value={responses[0] || ''} disabled={disabled} onChange={(event) => onResponsesChange(event.target.value ? [event.target.value] : [])} className="research-text-input" placeholder="请输入答案" />;
};

const PublicResult: React.FC<{ result: PublicAssessmentResultVO }> = ({ result }) => (
  <main className="research-result"><div className="research-result-inner"><div className="research-result-mark"><CheckCircle2 size={24} /></div><p className="research-kicker">LEXI-BRIDGE / SUBMISSION COMPLETE</p><h1>你的迁移路径，<br /><em>已经被记录。</em></h1><p className="research-result-lede">感谢参与「{result.paperTitle}」。重复打开此链接，会返回同一份研究结果。</p>{result.scoreVisible ? <div className="research-score"><span>规则评分</span><strong>{result.objectiveScore ?? '—'}<small>{result.totalScore != null ? ` / ${result.totalScore}` : ''}</small></strong></div> : null}{result.qualityFlags?.length ? <div className="research-quality">数据质量提醒：{result.qualityFlags.join('、')}</div> : null}<div className="research-analysis"><div className="research-section-index">C / RESEARCH READING</div><h2>Interpretation</h2>{result.aiAnalysis ? <p>{result.aiAnalysis.performanceOverview}</p> : <p>分析状态：{result.aiAnalysisStatus || 'PENDING'}。稍后重新打开页面即可查看更新。</p>}</div></div></main>
);

const ResearchParticipantPage: React.FC = () => {
  const { releaseCode = '' } = useParams<{ releaseCode: string }>();
  const normalizedReleaseCode = releaseCode.trim();
  const [metadata, setMetadata] = React.useState<PublicAssessmentMetadataVO | null>(null);
  const [attempt, setAttempt] = React.useState<PublicAssessmentAttemptVO | null>(null);
  const [result, setResult] = React.useState<PublicAssessmentResultVO | null>(null);
  const [participationCode, setParticipationCode] = React.useState('');
  const [responsesByOrder, setResponsesByOrder] = React.useState<ResponsesByOrder>({});
  const [justificationsByOrder, setJustificationsByOrder] = React.useState<JustificationsByOrder>({});
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [verifying, setVerifying] = React.useState(false);
  const [saving, setSaving] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [saveMessage, setSaveMessage] = React.useState<string | null>(null);
  const [dirtyRevision, setDirtyRevision] = React.useState(0);
  const hydratedRef = React.useRef(false);
  const currentVersionRef = React.useRef(1);

  const applyAttempt = React.useCallback((nextAttempt: PublicAssessmentAttemptVO) => { const hydrated = hydrateResponses(nextAttempt); currentVersionRef.current = nextAttempt.version; setAttempt(nextAttempt); setResponsesByOrder(hydrated.responses); setJustificationsByOrder(hydrated.justifications); setDirtyRevision(0); hydratedRef.current = true; }, []);

  React.useEffect(() => {
    if (!normalizedReleaseCode) { setErrorMessage('公开问卷链接无效。'); setLoading(false); return; }
    const controller = new AbortController();
    const load = async () => { setLoading(true); setErrorMessage(null); try { const nextMetadata = await publicAssessmentService.getMetadata(normalizedReleaseCode, { signal: controller.signal }); setMetadata(nextMetadata); try { const restoredAttempt = await publicAssessmentService.getAttempt(normalizedReleaseCode, { signal: controller.signal }); applyAttempt(restoredAttempt); if (restoredAttempt.status === 'SUBMITTED') setResult(await publicAssessmentService.getResult(normalizedReleaseCode, { signal: controller.signal })); } catch (error) { const status = normalizeApiError(error).status; if (status !== 401 && status !== 403) throw error; } } catch (error) { if (!controller.signal.aborted) setErrorMessage(getApiErrorMessage(error, '无法加载公开问卷。')); } finally { if (!controller.signal.aborted) setLoading(false); } };
    void load(); return () => controller.abort();
  }, [applyAttempt, normalizedReleaseCode]);

  const buildResponses = React.useCallback(() => (attempt?.questions || []).map((question) => ({ questionOrder: question.questionOrder, responses: responsesByOrder[question.questionOrder] || [], justificationText: justificationsByOrder[question.questionOrder] || null })), [attempt?.questions, justificationsByOrder, responsesByOrder]);
  const attemptId = attempt?.attemptId; const attemptStatus = attempt?.status;
  React.useEffect(() => { if (!attemptId || attemptStatus !== 'IN_PROGRESS' || !hydratedRef.current || dirtyRevision === 0) return; const timer = window.setTimeout(async () => { setSaving(true); setSaveMessage(null); try { const progress = await publicAssessmentService.saveResponses(normalizedReleaseCode, { responses: buildResponses(), baseVersion: currentVersionRef.current }); currentVersionRef.current = progress.version; setAttempt((current) => current ? { ...current, version: progress.version, answeredCount: progress.answeredCount, lastSavedAt: progress.lastSavedAt } : current); setSaveMessage('已自动保存'); } catch (error) { setSaveMessage(getApiErrorMessage(error, '自动保存失败，请重试。')); } finally { setSaving(false); } }, 900); return () => window.clearTimeout(timer); }, [attemptId, attemptStatus, buildResponses, dirtyRevision, normalizedReleaseCode]);
  React.useEffect(() => { if (!attempt || attempt.status !== 'IN_PROGRESS') return; const report = () => { if (document.hidden || !document.hasFocus()) return; const question = attempt.questions[selectedIndex]; if (!question) return; void publicAssessmentService.recordTiming(normalizedReleaseCode, { questionOrder: question.questionOrder, activeDurationMs: 15_000, eventId: crypto.randomUUID() }).catch(() => undefined); }; const timer = window.setInterval(report, 15_000); return () => window.clearInterval(timer); }, [attempt, normalizedReleaseCode, selectedIndex]);
  const verify = async (event: React.FormEvent) => { event.preventDefault(); setVerifying(true); setErrorMessage(null); try { const session = await publicAssessmentService.verifyCode(normalizedReleaseCode, { participationCode: participationCode.trim().toUpperCase() }); applyAttempt(session.attempt); if (session.attempt.status === 'SUBMITTED') setResult(await publicAssessmentService.getResult(normalizedReleaseCode)); } catch (error) { setErrorMessage(getApiErrorMessage(error, '参与码验证失败。')); } finally { setVerifying(false); } };
  const submit = async () => { if (!attempt || submitting) return; setSubmitting(true); setErrorMessage(null); try { await publicAssessmentService.submit(normalizedReleaseCode, { responses: buildResponses(), baseVersion: currentVersionRef.current, reason: 'MANUAL' }); setResult(await publicAssessmentService.getResult(normalizedReleaseCode)); } catch (error) { setErrorMessage(getApiErrorMessage(error, '提交失败，请检查必答题后重试。')); } finally { setSubmitting(false); } };

  if (loading) return <div className="research-loading">正在加载研究入口…</div>;
  if (result) return <PublicResult result={result} />;
  if (!attempt) return <ResearchEntry metadata={metadata} participationCode={participationCode} verifying={verifying} errorMessage={errorMessage} onCodeChange={setParticipationCode} onVerify={verify} />;

  const currentQuestion = attempt.questions[selectedIndex];
  const answeredCount = attempt.questions.filter((question) => hasResponse(responsesByOrder[question.questionOrder])).length;
  if (!currentQuestion) return <div className="research-loading">问卷暂无可作答题目。</div>;
  return <main className="research-assessment"><div className="research-assessment-inner"><header className="research-assessment-header"><div><div className="research-wordmark"><span className="research-wordmark-mark">EF</span><span>{attempt.paperTitle}</span></div><div className="research-progress-copy">已答 {answeredCount} / {attempt.questionCount}</div></div><div className="research-save-status">{saving ? <Clock3 size={15} /> : <Save size={15} />}{saveMessage || (attempt.lastSavedAt ? '答卷已恢复' : '自动保存已开启')}</div></header><div className="research-progress"><span style={{ width: `${Math.round((answeredCount / Math.max(1, attempt.questionCount)) * 100)}%` }} /></div><section className="research-question-card">{currentQuestion.sectionTitle ? <div className="research-section-index">{currentQuestion.sectionTitle}</div> : null}{currentQuestion.sharedMaterial ? <div className="research-shared-material">{currentQuestion.sharedMaterial}</div> : null}<div className="research-question-number">QUESTION {String(currentQuestion.questionOrder).padStart(2, '0')}</div><h1>{currentQuestion.stemText}</h1>{currentQuestion.promptText && currentQuestion.questionType !== 'INSTRUCTION' ? <p className="research-question-prompt">{currentQuestion.promptText}</p> : null}<PublicQuestion question={currentQuestion} responses={responsesByOrder[currentQuestion.questionOrder] || []} justification={justificationsByOrder[currentQuestion.questionOrder] || ''} disabled={submitting} onResponsesChange={(responses) => { setResponsesByOrder((current) => ({ ...current, [currentQuestion.questionOrder]: responses })); setDirtyRevision((value) => value + 1); }} onJustificationChange={(value) => { setJustificationsByOrder((current) => ({ ...current, [currentQuestion.questionOrder]: value })); setDirtyRevision((revision) => revision + 1); }} />{errorMessage ? <p className="research-form-error" role="alert">{errorMessage}</p> : null}<footer className="research-question-footer"><button type="button" disabled={selectedIndex === 0} onClick={() => setSelectedIndex((value) => Math.max(0, value - 1))} className="research-quiet-button"><ChevronLeft size={17} />上一题</button>{selectedIndex < attempt.questions.length - 1 ? <button type="button" onClick={() => setSelectedIndex((value) => Math.min(attempt.questions.length - 1, value + 1))} className="research-primary-button">下一题<ChevronRight size={17} /></button> : <button type="button" disabled={submitting} onClick={() => void submit()} className="research-primary-button">{submitting ? '正在提交…' : '提交问卷'}<Send size={16} /></button>}</footer></section></div></main>;
};

export default ResearchParticipantPage;
