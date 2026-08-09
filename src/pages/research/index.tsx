import React from 'react';
import { useGSAP } from '@gsap/react';
import gsap from 'gsap';
import { AlertCircle, ArrowUpRight, Check, CheckCircle2, ChevronLeft, ChevronRight, Clock3, Compass, Gauge, Lightbulb, LockKeyhole, Save, Send, ShieldCheck } from 'lucide-react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { useParams, useSearchParams } from 'react-router-dom';
import { getApiErrorMessage, normalizeApiError } from '@/lib/api';
import type {
  PublicAssessmentAttemptVO,
  PublicAssessmentMetadataVO,
  PublicAssessmentQuestionVO,
  PublicAssessmentResultVO,
} from '@/lib/contracts';
import { publicAssessmentService } from '@/lib/services';
import {
  forgetPublicSession,
  getAnswerProgress,
  hasPublicSessionMarker,
  rememberPublicSession,
} from './research-session';

gsap.registerPlugin(useGSAP);

type ResponsesByOrder = Record<number, string[]>;
type JustificationsByOrder = Record<number, string>;

const QUESTION_TYPE_LABELS: Record<string, string> = {
  INFORMED_CONSENT: '参与确认',
  INSTRUCTION: '阅读说明',
  SINGLE_CHOICE: '单项选择',
  MULTIPLE_CHOICE: '多项选择',
  TRUE_FALSE_WITH_JUSTIFICATION: '判断与说明',
  NUMBER: '数字填写',
  TEXT: '文字填写',
};

function hasQuestionResponse(question: PublicAssessmentQuestionVO, responses: string[], justification: string): boolean {
  if (question.questionType === 'INSTRUCTION') return true;
  const hasValue = responses.some((value) => value.trim().length > 0);
  if (!hasValue) return false;
  return !question.justificationRequired || justification.trim().length > 0;
}

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
  qrEntering: boolean;
  qrRequested: boolean;
  errorMessage: string | null;
  onCodeChange: (value: string) => void;
  onVerify: (event: React.FormEvent) => void;
}> = ({ metadata, participationCode, verifying, qrEntering, qrRequested, errorMessage, onCodeChange, onVerify }) => {
  const title = metadata?.title || 'Language in transit';
  const description = metadata?.description || metadata?.instructionsText || '一项关于英语与法语之间词义迁移的公开研究。';
  return (
    <main className="research-entry min-w-0">
      <header className="research-nav min-w-0">
        <div className="research-wordmark min-w-0"><span className="research-wordmark-mark">EF</span><span className="min-w-0 truncate">TRANSFER / RESEARCH</span></div>
        <div className="research-nav-meta"><span>PUBLIC STUDY</span><span className="research-nav-dot" /><span>2026 · V1</span></div>
      </header>

      <section className="research-hero min-w-0" aria-labelledby="research-title">
        <div className="research-hero-copy min-w-0">
          <p className="research-kicker"><span className="research-kicker-line" />语言迁移实验室 / LEXI-BRIDGE</p>
          <div className="research-language-loop min-w-0 flex-wrap" aria-label="English to French to Chinese">
            <span>meaning</span><span className="research-loop-arrow">→</span><span>sens</span><span className="research-loop-arrow">→</span><span>意义</span>
          </div>
          <h1 id="research-title" className="research-heading"><span>Language</span><span>in transit.</span></h1>
          <p className="research-lede">我们追踪一个词如何穿越语言、语境与记忆。你的每一次选择，都会成为这张迁移地图上的一个坐标。</p>
          <div className="research-hero-notes">
            <span>01 / 研究材料</span><span>02 / 语义判断</span><span>03 / 迁移分析</span>
          </div>
        </div>
        <div className="research-hero-visual min-w-0" aria-hidden="true">
          <ThreadMap />
          <div className="research-visual-caption min-w-0 gap-2"><span className="min-w-0 truncate">word / monde / 世界</span><span className="shrink-0">semantic drift study</span></div>
          <div className="research-halftone" />
        </div>
      </section>

      <section className="research-entry-grid min-w-0" aria-label="参与研究">
        <div className="research-material-panel min-w-0">
          <div className="research-section-index">A / THE MATERIAL</div>
          <h2>先看见材料，<br /><em>再决定它的方向。</em></h2>
          <p className="break-words">{description}</p>
          <div className="research-facts">
            <div><strong>{metadata?.questionCount ?? '—'}</strong><span>items / 题</span></div>
            <div><strong>{metadata?.durationMinutes ?? '—'}</strong><span>min / 分钟</span></div>
            <div><strong>EN ↔ FR</strong><span>language pair</span></div>
          </div>
          <div className="research-material-source min-w-0 break-words">CURRENT PAPER / {title}</div>
        </div>
        <div className="research-access-card min-w-0">
          <div className="research-card-topline"><span>PARTICIPANT ACCESS</span><LockKeyhole size={15} strokeWidth={1.8} className="shrink-0" /></div>
          <h2>{qrEntering ? '正在识别本设备并进入问卷。' : '带着研究员提供的参与码进入。'}</h2>
          <p>{qrRequested ? '二维码会尝试自动进入；如识别失败，仍可输入参与码继续。' : '没有公开注册。你的回答会以匿名方式保存，仅用于本次研究。'}</p>
          <form onSubmit={onVerify} className="research-code-form min-w-0">
            <label htmlFor="participation-code">参与码 / ACCESS CODE</label>
            <input id="participation-code" value={participationCode} onChange={(event) => onCodeChange(event.target.value.toUpperCase())} placeholder="XXXX-XXXX-XXXX" autoComplete="one-time-code" className="min-w-0" />
            {errorMessage ? <p className="research-form-error" role="alert">{errorMessage}</p> : null}
            <button type="submit" disabled={verifying || qrEntering || !participationCode.trim()} className="research-primary-button w-full sm:w-auto">{verifying ? '正在验证…' : qrEntering ? '二维码进入中…' : '验证并开始'}<ArrowUpRight size={18} className="shrink-0" /></button>
          </form>
          <div className="research-access-foot"><Check size={14} className="shrink-0" /> 可中途离开，进度会自动保存</div>
        </div>
      </section>

      <section className="research-method min-w-0" aria-labelledby="method-title">
        <div className="min-w-0"><div className="research-section-index">B / HOW WE READ</div><h2 id="method-title">这不是语言考试。<br /><em>这是迁移的现场记录。</em></h2></div>
        <div className="research-method-list min-w-0">
          <article><span>01</span><div className="min-w-0"><h3>看见上下文</h3><p>短句、词组与文化线索组成研究材料，先读完整，再做判断。</p></div></article>
          <article><span>02</span><div className="min-w-0"><h3>标记语义距离</h3><p>我们关心熟悉感从哪里来，也关心“看似相同”何时开始偏移。</p></div></article>
          <article><span>03</span><div className="min-w-0"><h3>返回你的路径</h3><p>提交后可查看规则评分与研究分析，解释在完整作答后呈现。</p></div></article>
        </div>
      </section>

      <footer className="research-footer min-w-0"><span>EF TRANSFER PLATFORM</span><span>an editorial study of language movement</span></footer>
    </main>
  );
};

const instructionText = (question: PublicAssessmentQuestionVO) => {
  const normalize = (part?: string | null) => part
    ?.trim()
    .replace(/^【\s*/, '')
    .replace(/\s*】$/, '')
    .replace(/\r\n/g, '\n');
  const uniqueParts = [question.stemText, question.promptText]
    .map(normalize)
    .filter((part, index, parts): part is string => Boolean(part) && parts.indexOf(part) === index);
  return uniqueParts.join('\n\n');
};

const ResearchOption: React.FC<{
  inputType: 'radio' | 'checkbox';
  inputName?: string;
  value: string;
  label: string;
  index: number;
  selected: boolean;
  disabled: boolean;
  reducedMotion: boolean;
  onChange: () => void;
}> = ({ inputType, inputName, value, label, index, selected, disabled, reducedMotion, onChange }) => {
  const optionRef = React.useRef<HTMLLabelElement>(null);
  const { contextSafe } = useGSAP(() => {
    const option = optionRef.current;
    if (!option || reducedMotion) return;
    if (selected) {
      gsap.fromTo(option, { scale: 0.985, y: 2 }, {
        scale: 1,
        y: 0,
        duration: 0.38,
        ease: 'back.out(1.7)',
        overwrite: 'auto',
        clearProps: 'transform',
      });
      gsap.fromTo('.research-option-selection-wave', { autoAlpha: 0.26, scale: 0.62 }, {
        autoAlpha: 0,
        scale: 1.45,
        duration: 0.58,
        ease: 'power2.out',
        overwrite: 'auto',
      });
      gsap.fromTo('.research-option-key, .research-option-control', { scale: 0.76 }, {
        scale: 1,
        duration: 0.34,
        stagger: 0.035,
        ease: 'back.out(2)',
        overwrite: 'auto',
        clearProps: 'transform',
      });
    }
  }, { dependencies: [selected, reducedMotion], scope: optionRef, revertOnUpdate: true });

  const press = contextSafe(() => {
    if (!optionRef.current || reducedMotion || disabled) return;
    gsap.to(optionRef.current, { scale: 0.985, y: 1, duration: 0.1, ease: 'power2.out', overwrite: 'auto' });
  });
  const release = contextSafe(() => {
    if (!optionRef.current || reducedMotion || disabled) return;
    gsap.to(optionRef.current, { scale: 1, y: 0, duration: 0.28, ease: 'back.out(1.8)', overwrite: 'auto', clearProps: 'transform' });
  });

  return (
    <label
      ref={optionRef}
      className={`research-option ${selected ? 'is-selected' : ''}`}
      onPointerDown={press}
      onPointerUp={release}
      onPointerCancel={release}
      onPointerLeave={release}
    >
      <span className="research-option-selection-wave" aria-hidden="true" />
      <input className="research-option-input" type={inputType} name={inputName} value={value} checked={selected} disabled={disabled} onChange={onChange} />
      <span className="research-option-key" aria-hidden="true">{String.fromCharCode(65 + index)}</span>
      <span className="research-option-label">{label}</span>
      <span className={`research-option-control ${inputType === 'radio' ? 'is-radio' : ''}`} aria-hidden="true">
        {inputType === 'radio' ? <span /> : selected ? <Check size={13} strokeWidth={2.6} /> : null}
      </span>
      <span className="research-option-confirmation" aria-hidden={!selected}>
        {selected ? <><Check size={13} />已选择</> : '选择此项'}
      </span>
    </label>
  );
};

const PublicQuestion: React.FC<{
  question: PublicAssessmentQuestionVO;
  responses: string[];
  justification: string;
  disabled: boolean;
  reducedMotion: boolean;
  onResponsesChange: (responses: string[]) => void;
  onJustificationChange: (value: string) => void;
}> = ({ question, responses, justification, disabled, reducedMotion, onResponsesChange, onJustificationChange }) => {
  const type = question.questionType;
  if (type === 'INSTRUCTION') {
    return <section className="research-instruction" aria-labelledby={`instruction-title-${question.questionOrder}`}>
      <div className="research-instruction-label"><ShieldCheck size={17} /><span>RESEARCH NOTE / 作答说明</span></div>
      <h1 id={`instruction-title-${question.questionOrder}`}>作答前请阅读</h1>
      <p>{instructionText(question)}</p>
    </section>;
  }
  if (type === 'SINGLE_CHOICE' || type === 'INFORMED_CONSENT' || type === 'TRUE_FALSE_WITH_JUSTIFICATION') {
    return <div className="research-options">{question.options.map((option, index) => <ResearchOption
      key={option.key}
      inputType="radio"
      inputName={`question-${question.questionOrder}`}
      value={option.key}
      label={option.label}
      index={index}
      selected={responses[0] === option.key}
      disabled={disabled}
      reducedMotion={reducedMotion}
      onChange={() => onResponsesChange([option.key])}
    />)}
      {type === 'TRUE_FALSE_WITH_JUSTIFICATION' && responses[0] === 'F' ? <label className="research-justification">请说明判断为错误的原因<textarea value={justification} disabled={disabled} onChange={(event) => onJustificationChange(event.target.value)} rows={4} /></label> : null}
    </div>;
  }
  if (type === 'MULTIPLE_CHOICE') return <div className="research-options">{question.options.map((option, index) => {
    const selected = responses.includes(option.key);
    return <ResearchOption
      key={option.key}
      inputType="checkbox"
      value={option.key}
      label={option.label}
      index={index}
      selected={selected}
      disabled={disabled}
      reducedMotion={reducedMotion}
      onChange={() => onResponsesChange(selected ? responses.filter((value) => value !== option.key) : [...responses, option.key])}
    />;
  })}</div>;
  if (type === 'NUMBER') return <div className="research-number-field">
    <input type="number" inputMode="decimal" step="any" value={responses[0] || ''} disabled={disabled} onChange={(event) => onResponsesChange(event.target.value ? [event.target.value] : [])} className="research-text-input" placeholder="请输入数字" aria-describedby={`number-hint-${question.questionOrder}`} />
    <p id={`number-hint-${question.questionOrder}`}>仅接受数字；无效字符不会被记录。</p>
  </div>;
  return <input type="text" value={responses[0] || ''} disabled={disabled} onChange={(event) => onResponsesChange(event.target.value ? [event.target.value] : [])} className="research-text-input" placeholder="请输入答案" />;
};

const PublicResult: React.FC<{ result: PublicAssessmentResultVO }> = ({ result }) => {
  const fallback = result.aiAnalysisStatus === 'FALLBACK';
  const waiting = result.aiAnalysisStatus === 'PENDING' || result.aiAnalysisStatus === 'PROCESSING';
  const confidence = result.aiAnalysis ? Math.round(result.aiAnalysis.confidence * 100) : null;
  return (
  <main className="research-result min-w-0">
    <div className="research-result-inner min-w-0">
      <div className="research-result-mark"><CheckCircle2 size={24} /></div>
      <p className="research-kicker">LEXI-BRIDGE / SUBMISSION COMPLETE</p>
      <h1>你的迁移路径，<br /><em>已经被记录。</em></h1>
      <p className="research-result-lede break-words">感谢参与「{result.paperTitle}」。重复打开此链接，会返回同一份研究结果。</p>
      {result.scoreVisible ? (
        <div className="research-score min-w-0 gap-3">
          <span>规则评分</span>
          <strong className="min-w-0 break-words">{result.objectiveScore ?? '—'}<small>{result.totalScore != null ? ` / ${result.totalScore}` : ''}</small></strong>
        </div>
      ) : null}
      {result.qualityFlags?.length ? <div className="research-quality break-words">数据质量提醒：{result.qualityFlags.join('、')}</div> : null}
      <div className="research-analysis min-w-0">
        <div className="research-analysis-heading">
          <div>
            <div className="research-section-index">C / RESEARCH READING</div>
            <h2>你的词汇迁移画像</h2>
            <p>把规则评分、语境表现与作答节奏放在一起阅读；它提供解释线索，不替代正式能力诊断。</p>
          </div>
          <div className={`research-analysis-source ${fallback ? 'is-fallback' : waiting ? 'is-waiting' : 'is-ai'}`}>
            <span aria-hidden="true" />
            {fallback ? '规则降级 · 非模型结论' : waiting ? '模型生成中 · 自动重试' : result.aiAnalysis ? '真实模型分析' : `分析状态 · ${result.aiAnalysisStatus || 'PENDING'}`}
          </div>
        </div>
        {result.aiAnalysis ? <div className="research-analysis-content min-w-0">
          <section className="research-analysis-overview-card">
            <div className="research-analysis-card-label"><Compass size={16} />核心解读</div>
            <p className="research-analysis-overview break-words">{result.aiAnalysis.performanceOverview}</p>
            <div className="research-analysis-confidence" aria-label={`分析置信度 ${confidence}%`}>
              <span>证据置信度</span>
              <div><i style={{ width: `${confidence}%` }} /></div>
              <strong>{confidence}%</strong>
            </div>
          </section>
          <div className="research-analysis-grid min-w-0">
            <section className="research-analysis-card is-strength"><div className="research-analysis-card-label"><CheckCircle2 size={16} />已观察到的优势</div><ul>{result.aiAnalysis.strengths.map((item) => <li key={item} className="break-words">{item}</li>)}</ul></section>
            <section className="research-analysis-card is-risk"><div className="research-analysis-card-label"><AlertCircle size={16} />风险与证据边界</div><ul>{result.aiAnalysis.risks.map((item) => <li key={item} className="break-words">{item}</li>)}</ul></section>
          </div>
          <div className="research-analysis-insights">
            <section><div className="research-analysis-card-label"><Gauge size={16} />语境表现</div><p className="break-words">{result.aiAnalysis.contextInterpretation}</p></section>
            <section><div className="research-analysis-card-label"><Clock3 size={16} />作答节奏</div><p className="break-words">{result.aiAnalysis.reactionTimeInterpretation}</p></section>
          </div>
          <section className="research-analysis-actions">
            <div className="research-analysis-card-label"><Lightbulb size={16} />下一步怎么做</div>
            <ol>{result.aiAnalysis.recommendations.map((item, index) => <li key={item}><span>{String(index + 1).padStart(2, '0')}</span><p className="break-words">{item}</p></li>)}</ol>
          </section>
          {result.aiAnalysis.qualityNotice ? <div className="research-analysis-notice break-words"><ShieldCheck size={17} /><div><strong>如何理解这份分析</strong><p>{result.aiAnalysis.qualityNotice}</p></div></div> : null}
        </div> : <div className={`research-analysis-empty ${waiting ? 'is-waiting' : ''}`}><span aria-hidden="true" />{waiting ? '正在生成与本次答题结果相关的分析；页面会自动更新，失败时系统将自动重试。' : '分析暂不可用，请稍后重新进入结果页。'}</div>}
      </div>
    </div>
  </main>
  );
};

const ResearchParticipantPage: React.FC = () => {
  const { releaseCode = '' } = useParams<{ releaseCode: string }>();
  const [searchParams] = useSearchParams();
  const reducedMotion = Boolean(useReducedMotion());
  const normalizedReleaseCode = releaseCode.trim();
  const qrRequested = searchParams.get('entry') === 'qr';
  const [metadata, setMetadata] = React.useState<PublicAssessmentMetadataVO | null>(null);
  const [attempt, setAttempt] = React.useState<PublicAssessmentAttemptVO | null>(null);
  const [result, setResult] = React.useState<PublicAssessmentResultVO | null>(null);
  const [participationCode, setParticipationCode] = React.useState('');
  const [responsesByOrder, setResponsesByOrder] = React.useState<ResponsesByOrder>({});
  const [justificationsByOrder, setJustificationsByOrder] = React.useState<JustificationsByOrder>({});
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const [navigationDirection, setNavigationDirection] = React.useState(1);
  const [loading, setLoading] = React.useState(true);
  const [verifying, setVerifying] = React.useState(false);
  const [qrEntering, setQrEntering] = React.useState(false);
  const [saving, setSaving] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [saveMessage, setSaveMessage] = React.useState<string | null>(null);
  const [dirtyRevision, setDirtyRevision] = React.useState(0);
  const [saveCycle, setSaveCycle] = React.useState(0);
  const hydratedRef = React.useRef(false);
  const qrEntryAttemptedRef = React.useRef(false);
  const currentVersionRef = React.useRef(1);
  const savedRevisionRef = React.useRef(0);
  const failedRevisionRef = React.useRef(0);
  const saveInFlightRef = React.useRef(false);
  const releaseGenerationRef = React.useRef(0);

  React.useEffect(() => {
    if (!result || !['PENDING', 'PROCESSING'].includes(result.aiAnalysisStatus || '')) return;
    let active = true;
    let attempts = 0;
    const refresh = async () => {
      if (!active || attempts >= 24) return;
      attempts += 1;
      try {
        const nextResult = await publicAssessmentService.getResult(normalizedReleaseCode);
        if (!active) return;
        setResult(nextResult);
        if (['PENDING', 'PROCESSING'].includes(nextResult.aiAnalysisStatus || '')) window.setTimeout(refresh, 2500);
      } catch {
        if (active) window.setTimeout(refresh, 4000);
      }
    };
    const timer = window.setTimeout(refresh, 1500);
    return () => { active = false; window.clearTimeout(timer); };
  }, [normalizedReleaseCode, result]);

  const applyAttempt = React.useCallback((nextAttempt: PublicAssessmentAttemptVO) => {
    const hydrated = hydrateResponses(nextAttempt);
    currentVersionRef.current = nextAttempt.version;
    savedRevisionRef.current = 0;
    failedRevisionRef.current = 0;
    setAttempt(nextAttempt);
    setResponsesByOrder(hydrated.responses);
    setJustificationsByOrder(hydrated.justifications);
    setDirtyRevision(0);
    hydratedRef.current = true;
  }, []);

  React.useEffect(() => {
    if (!normalizedReleaseCode) { setErrorMessage('公开问卷链接无效。'); setLoading(false); return; }
    releaseGenerationRef.current += 1;
    qrEntryAttemptedRef.current = false;
    hydratedRef.current = false;
    saveInFlightRef.current = false;
    savedRevisionRef.current = 0;
    failedRevisionRef.current = 0;
    currentVersionRef.current = 1;
    setMetadata(null);
    setAttempt(null);
    setResult(null);
    setParticipationCode('');
    setResponsesByOrder({});
    setJustificationsByOrder({});
    setSelectedIndex(0);
    setNavigationDirection(1);
    setDirtyRevision(0);
    setSaving(false);
    setSubmitting(false);
    setSaveMessage(null);
    const controller = new AbortController();
    const load = async () => {
      setLoading(true);
      setErrorMessage(null);
      try {
        const nextMetadata = await publicAssessmentService.getMetadata(normalizedReleaseCode, { signal: controller.signal });
        setMetadata(nextMetadata);
        if (hasPublicSessionMarker(normalizedReleaseCode)) {
          try {
            const restoredAttempt = await publicAssessmentService.getAttempt(normalizedReleaseCode, { signal: controller.signal });
            applyAttempt(restoredAttempt);
            if (restoredAttempt.status === 'SUBMITTED') {
              setResult(await publicAssessmentService.getResult(normalizedReleaseCode, { signal: controller.signal }));
            }
          } catch (error) {
            const status = normalizeApiError(error).status;
            if (status === 401 || status === 403) {
              forgetPublicSession(normalizedReleaseCode);
            } else {
              throw error;
            }
          }
        }
      } catch (error) {
        if (!controller.signal.aborted) setErrorMessage(getApiErrorMessage(error, '无法加载公开问卷。'));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };
    void load(); return () => controller.abort();
  }, [applyAttempt, normalizedReleaseCode]);

  React.useEffect(() => {
    if (!qrRequested || loading || attempt || result || qrEntryAttemptedRef.current) return;
    if (!metadata?.qrEntryEnabled) {
      setErrorMessage('此问卷暂未开启二维码免码参与，请输入参与码继续。');
      qrEntryAttemptedRef.current = true;
      return;
    }
    qrEntryAttemptedRef.current = true;
    const generation = releaseGenerationRef.current;
    let active = true;
    const enter = async () => {
      setQrEntering(true);
      setErrorMessage(null);
      try {
        const { default: FingerprintJS } = await import('@fingerprintjs/fingerprintjs');
        const agent = await FingerprintJS.load();
        const fingerprint = await agent.get();
        const session = await publicAssessmentService.enterByQr(normalizedReleaseCode, {
          browserFingerprint: fingerprint.visitorId,
        });
        const submittedResult = session.attempt.status === 'SUBMITTED'
          ? await publicAssessmentService.getResult(normalizedReleaseCode)
          : null;
        if (!active || releaseGenerationRef.current !== generation) return;
        rememberPublicSession(normalizedReleaseCode);
        applyAttempt(session.attempt);
        if (submittedResult) setResult(submittedResult);
      } catch (error) {
        if (!active || releaseGenerationRef.current !== generation) return;
        const normalizedError = normalizeApiError(error);
        setErrorMessage(normalizedError.code === 'RATE_LIMITED'
          ? getApiErrorMessage(normalizedError)
          : '二维码自动进入失败，请输入参与码继续。');
      } finally {
        if (active && releaseGenerationRef.current === generation) setQrEntering(false);
      }
    };
    void enter();
    return () => { active = false; };
  }, [applyAttempt, attempt, loading, metadata?.qrEntryEnabled, normalizedReleaseCode, qrRequested, result]);

  const buildResponses = React.useCallback(() => (attempt?.questions || []).map((question) => ({ questionOrder: question.questionOrder, responses: responsesByOrder[question.questionOrder] || [], justificationText: justificationsByOrder[question.questionOrder] || null })), [attempt?.questions, justificationsByOrder, responsesByOrder]);
  const attemptId = attempt?.attemptId; const attemptStatus = attempt?.status;
  React.useEffect(() => {
    if (!attemptId || attemptStatus !== 'IN_PROGRESS' || !hydratedRef.current || submitting
      || dirtyRevision === 0 || dirtyRevision <= savedRevisionRef.current
      || dirtyRevision <= failedRevisionRef.current || saveInFlightRef.current) return;
    const revisionToSave = dirtyRevision;
    const generation = releaseGenerationRef.current;
    const timer = window.setTimeout(async () => {
      saveInFlightRef.current = true;
      setSaving(true);
      setSaveMessage(null);
      try {
        const progress = await publicAssessmentService.saveResponses(normalizedReleaseCode, {
          responses: buildResponses(),
          baseVersion: currentVersionRef.current,
        });
        if (releaseGenerationRef.current === generation) {
          currentVersionRef.current = progress.version;
          savedRevisionRef.current = Math.max(savedRevisionRef.current, revisionToSave);
          setAttempt((current) => current ? { ...current, version: progress.version, answeredCount: progress.answeredCount, lastSavedAt: progress.lastSavedAt } : current);
          setSaveMessage('已自动保存');
        }
      } catch (error) {
        if (releaseGenerationRef.current === generation) {
          failedRevisionRef.current = Math.max(failedRevisionRef.current, revisionToSave);
          setSaveMessage(getApiErrorMessage(error, '自动保存失败，请修改后重试。'));
        }
      } finally {
        if (releaseGenerationRef.current === generation) {
          saveInFlightRef.current = false;
          setSaving(false);
          setSaveCycle((value) => value + 1);
        }
      }
    }, 900);
    return () => window.clearTimeout(timer);
  }, [attemptId, attemptStatus, buildResponses, dirtyRevision, normalizedReleaseCode, saveCycle, submitting]);
  React.useEffect(() => { if (!attempt || attempt.status !== 'IN_PROGRESS') return; const report = () => { if (document.hidden || !document.hasFocus()) return; const question = attempt.questions[selectedIndex]; if (!question) return; void publicAssessmentService.recordTiming(normalizedReleaseCode, { questionOrder: question.questionOrder, activeDurationMs: 15_000, eventId: crypto.randomUUID() }).catch(() => undefined); }; const timer = window.setInterval(report, 15_000); return () => window.clearInterval(timer); }, [attempt, normalizedReleaseCode, selectedIndex]);
  const verify = async (event: React.FormEvent) => { event.preventDefault(); setVerifying(true); setErrorMessage(null); try { const session = await publicAssessmentService.verifyCode(normalizedReleaseCode, { participationCode: participationCode.trim().toUpperCase() }); rememberPublicSession(normalizedReleaseCode); applyAttempt(session.attempt); if (session.attempt.status === 'SUBMITTED') setResult(await publicAssessmentService.getResult(normalizedReleaseCode)); } catch (error) { setErrorMessage(getApiErrorMessage(error, '参与码验证失败。')); } finally { setVerifying(false); } };
  const submit = async () => { if (!attempt || submitting || saving || saveInFlightRef.current) return; setSubmitting(true); setErrorMessage(null); try { await publicAssessmentService.submit(normalizedReleaseCode, { responses: buildResponses(), baseVersion: currentVersionRef.current, reason: 'MANUAL' }); setResult(await publicAssessmentService.getResult(normalizedReleaseCode)); } catch (error) { setErrorMessage(getApiErrorMessage(error, '提交失败，请检查必答题后重试。')); } finally { setSubmitting(false); } };

  if (loading) return <div className="research-loading">正在加载研究入口…</div>;
  if (result) return <PublicResult result={result} />;
  if (!attempt) return <ResearchEntry metadata={metadata} participationCode={participationCode} verifying={verifying} qrEntering={qrEntering} qrRequested={qrRequested} errorMessage={errorMessage} onCodeChange={setParticipationCode} onVerify={verify} />;

  const currentQuestion = attempt.questions[selectedIndex];
  const { answeredCount, questionCount } = getAnswerProgress(attempt.questions, responsesByOrder);
  if (!currentQuestion) return <div className="research-loading">问卷暂无可作答题目。</div>;
  const currentResponses = responsesByOrder[currentQuestion.questionOrder] || [];
  const currentJustification = justificationsByOrder[currentQuestion.questionOrder] || '';
  const currentAnswered = hasQuestionResponse(currentQuestion, currentResponses, currentJustification);
  const progressPercent = Math.round((answeredCount / Math.max(1, questionCount)) * 100);
  const hasPendingSave = dirtyRevision > savedRevisionRef.current;
  const hasSaveError = hasPendingSave && failedRevisionRef.current >= dirtyRevision && Boolean(saveMessage);
  const saveState = hasSaveError ? 'error' : saving ? 'saving' : hasPendingSave ? 'pending' : saveMessage ? 'saved' : 'ready';
  const saveStatusText = hasSaveError
    ? saveMessage
    : saving
      ? '正在保存本题更改…'
      : hasPendingSave
        ? '已记录，等待自动保存'
        : saveMessage || (attempt.lastSavedAt ? '答卷已恢复，当前内容安全' : '自动保存已开启');
  const navigateToQuestion = (nextIndex: number) => {
    const boundedIndex = Math.min(attempt.questions.length - 1, Math.max(0, nextIndex));
    if (boundedIndex === selectedIndex) return;
    setNavigationDirection(boundedIndex > selectedIndex ? 1 : -1);
    setSelectedIndex(boundedIndex);
  };
  return (
    <main className="research-assessment min-w-0">
      <div className="research-assessment-inner min-w-0">
        <header className="research-assessment-header min-w-0">
          <div className="min-w-0">
            <div className="research-wordmark min-w-0">
              <span className="research-wordmark-mark">EF</span>
              <span className="min-w-0 break-words">{attempt.paperTitle}</span>
            </div>
            <div className="research-progress-copy">LEXI-BRIDGE / 学生答卷</div>
          </div>
          <div className={`research-save-status is-${saveState}`} role="status" aria-live="polite">
            {hasSaveError ? <AlertCircle size={15} /> : saving || hasPendingSave ? <Clock3 size={15} /> : saveMessage ? <CheckCircle2 size={15} /> : <Save size={15} />}
            <span>{saveStatusText}</span>
          </div>
        </header>
        <div className="research-progress-overview">
          <div className="research-progress-heading">
            <span>作答路线 / RESPONSE ROUTE</span>
            <strong>{progressPercent}%</strong>
          </div>
          <div className="research-progress" role="progressbar" aria-label="问卷作答进度" aria-valuemin={0} aria-valuemax={questionCount} aria-valuenow={answeredCount}>
            <motion.span initial={false} animate={{ scaleX: progressPercent / 100 }} transition={{ duration: reducedMotion ? 0 : 0.38, ease: [0.2, 0, 0, 1] }} />
          </div>
          <div className="research-progress-meta"><span>已回答 {answeredCount} 题</span><span>共 {questionCount} 道正式题</span></div>
        </div>
        <div className="research-assessment-layout min-w-0">
          <aside className="research-route-panel" aria-label="当前作答位置">
            <div className="research-section-index">CURRENT POSITION</div>
            <div className="research-route-position"><strong>{String(selectedIndex + 1).padStart(2, '0')}</strong><span>/ {String(attempt.questions.length).padStart(2, '0')}</span></div>
            <p>{currentQuestion.questionType === 'INSTRUCTION' ? '阅读说明' : currentAnswered ? '本题已作答' : '等待你的判断'}</p>
            <div className="research-route-line" aria-hidden="true">
              <span className="is-complete" />
              <span className="is-current" />
              <span />
            </div>
            <dl className="research-route-stats">
              <div><dt>已回答</dt><dd>{answeredCount}</dd></div>
              <div><dt>待完成</dt><dd>{Math.max(0, questionCount - answeredCount)}</dd></div>
            </dl>
            <p className="research-route-note">选择后立即记录；约 1 秒后自动保存。你可以随时返回上一题修改。</p>
          </aside>
          <motion.section className="research-question-card min-w-0" layout={!reducedMotion} transition={{ layout: { duration: 0.24, ease: [0.2, 0, 0, 1] } }}>
            <AnimatePresence mode="wait" initial={false} custom={navigationDirection}>
              <motion.div
                key={currentQuestion.questionId}
                custom={navigationDirection}
                variants={{
                  enter: (direction: number) => ({ opacity: 0, x: reducedMotion ? 0 : direction * 22 }),
                  center: { opacity: 1, x: 0 },
                  exit: (direction: number) => ({ opacity: 0, x: reducedMotion ? 0 : direction * -14 }),
                }}
                initial="enter"
                animate="center"
                exit="exit"
                transition={{ duration: reducedMotion ? 0 : 0.22, ease: [0.2, 0, 0, 1] }}
              >
                <div className="research-question-meta">
                  <div className="research-section-index">{currentQuestion.sectionTitle || 'LEXI-BRIDGE RESEARCH'}</div>
                  <div className="research-question-tags">
                    {currentQuestion.required ? <span>必答</span> : <span>可跳过</span>}
                    <span>{QUESTION_TYPE_LABELS[currentQuestion.questionType] || currentQuestion.questionType}</span>
                  </div>
                </div>
                {currentQuestion.sharedMaterial ? <div className="research-shared-material break-words">{currentQuestion.sharedMaterial}</div> : null}
                <div className="research-question-number">ITEM {String(currentQuestion.questionOrder).padStart(2, '0')} · {String(selectedIndex + 1).padStart(2, '0')} / {String(attempt.questions.length).padStart(2, '0')}</div>
                {currentQuestion.questionType !== 'INSTRUCTION' ? <h1 className="break-words">{currentQuestion.stemText}</h1> : null}
                {currentQuestion.promptText && currentQuestion.questionType !== 'INSTRUCTION' ? (
                  <p className="research-question-prompt break-words">{currentQuestion.promptText}</p>
                ) : null}
                <PublicQuestion
                  question={currentQuestion}
                  responses={currentResponses}
                  justification={currentJustification}
                  disabled={submitting}
                  reducedMotion={reducedMotion}
                  onResponsesChange={(responses) => {
                    setResponsesByOrder((current) => ({ ...current, [currentQuestion.questionOrder]: responses }));
                    setDirtyRevision((value) => value + 1);
                  }}
                  onJustificationChange={(value) => {
                    setJustificationsByOrder((current) => ({ ...current, [currentQuestion.questionOrder]: value }));
                    setDirtyRevision((revision) => revision + 1);
                  }}
                />
              </motion.div>
            </AnimatePresence>
            {errorMessage ? <p className="research-form-error research-question-error" role="alert"><AlertCircle size={15} />{errorMessage}</p> : null}
            <footer className="research-question-footer">
              <button type="button" disabled={selectedIndex === 0} onClick={() => navigateToQuestion(selectedIndex - 1)} className="research-quiet-button">
                <ChevronLeft size={17} />上一题
              </button>
              <div className={`research-answer-feedback ${currentAnswered ? 'is-complete' : ''}`} aria-live="polite">
                {currentQuestion.questionType === 'INSTRUCTION' ? '阅读完成后继续' : currentAnswered ? <><CheckCircle2 size={15} />本题回答已记录</> : currentQuestion.required ? '请选择或填写答案' : '本题可以暂时跳过'}
              </div>
              {selectedIndex < attempt.questions.length - 1 ? (
                <button type="button" onClick={() => navigateToQuestion(selectedIndex + 1)} className="research-primary-button w-full sm:w-auto">
                  {currentQuestion.questionType === 'INSTRUCTION' ? '开始作答' : currentAnswered ? '已记录，继续' : '暂不回答，下一题'}<ChevronRight size={17} className="shrink-0" />
                </button>
              ) : (
                <button type="button" disabled={submitting || saving} onClick={() => void submit()} className="research-primary-button w-full sm:w-auto">
                  {submitting ? '正在提交…' : saving ? '正在保存…' : `提交问卷 · ${answeredCount}/${questionCount}`}
                  <Send size={16} className="shrink-0" />
                </button>
              )}
            </footer>
          </motion.section>
        </div>
      </div>
    </main>
  );
};

export default ResearchParticipantPage;
