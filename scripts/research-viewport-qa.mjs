#!/usr/bin/env node
/**
 * Research questionnaire viewport QA (desktop/tablet/mobile).
 *
 * Boots the local Vite dev server, mocks the public-assessment API, and verifies:
 *   - entry page shows 60 formal questions and 40 minutes
 *   - answering page status line "正式题 60 道 · 限时 40 分钟"
 *   - countdown starts at 40:00 and stays in the top-right region on scroll
 *   - non-English-major profile shows 资料 xx/06; English-major shows /08
 *   - switching back from English major clears hidden TEM answers
 *   - text inputs/buttons keep 44px-plus touch targets
 *   - screenshots at desktop, tablet portrait/landscape, and mobile widths
 *   - result summary/analysis remain readable without horizontal overflow
 *
 * Usage:
 *   npm run build
 *   node scripts/research-viewport-qa.mjs
 *   BASE_URL=http://localhost:3000 VIEWPORTS=1440x900 node scripts/research-viewport-qa.mjs
 */

import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { spawn } from 'node:child_process';
import { chromium } from 'playwright';

const RELEASE_CODE = 'RES-QA-000000';
const BASE_URL = (process.env.BASE_URL || 'http://localhost:3000').replace(/\/$/, '');
const OUT_DIR = process.env.OUT_DIR || path.join('qa-output', `research-viewport-${new Date().toISOString().slice(0, 10)}`);
const HEADLESS = process.env.HEADLESS !== '0';
const BOOT = process.env.BOOT !== '0';

const VIEWPORTS = (process.env.VIEWPORTS || '1440x900,1024x768,768x1024,390x844,320x568')
  .split(',')
  .map((token) => token.trim())
  .filter(Boolean)
  .map((token) => {
    const [width, height] = token.split('x').map(Number);
    return { name: token, width, height };
  });

const nowIso = () => new Date().toISOString();
const questionTypeOf = (order) => {
  if (order === 1) return 'INSTRUCTION';
  if (order === 5) return 'SINGLE_CHOICE';
  if (order === 8 || order === 9) return 'NUMBER';
  if (order === 2 || order === 3) return 'SHORT_TEXT';
  if (order <= 9) return 'NUMBER';
  return 'SINGLE_CHOICE';
};

const buildQuestions = () => {
  const basicStems = {
    1: '【亲爱的同学：\n您好！欢迎参与本次法语词汇与阅读理解能力测试！本测试结果仅用于学术研究，所有数据严格保密。答题过程中请勿查阅词典、相互交流，独立完成作答。整套测试答题时长约 40 分钟，请合理安排时间。\n感谢您的配合与支持！】',
    2: '您的姓名：',
    3: '您的联系方式是（电话/QQ/……）：',
    4: '您的英语学习水平为：高考英语分数______',
    5: '您是否为英语专业学生：',
    6: '□ 四级分数_____',
    7: '□ 六级分数_____',
    8: '□ 专四分数_____',
    9: '□ 专八分数_____',
  };
  const basicCodes = { 1: 'BASIC-INSTRUCTION', 2: 'BASIC-NAME', 3: 'BASIC-CONTACT', 4: 'BASIC-GAOKAO-ENGLISH', 5: 'BASIC-ENGLISH-MAJOR', 6: 'BASIC-CET4', 7: 'BASIC-CET6', 8: 'BASIC-TEM4', 9: 'BASIC-TEM8' };
  const questions = [];
  for (let order = 1; order <= 69; order += 1) {
    const formal = order >= 10;
    let options = [];
    if (order === 5) {
      options = [
        { key: 'ENGLISH_MAJOR', label: '英语专业' },
        { key: 'NON_ENGLISH_MAJOR', label: '非英语专业' },
      ];
    } else if (formal) {
      options = ['选项甲', '选项乙', '选项丙', '选项丁'].map((label, index) => ({ key: String.fromCharCode(65 + index), label }));
    }
    questions.push({
      questionId: 1000 + order,
      questionOrder: order,
      questionType: questionTypeOf(order),
      itemCode: formal ? `P1A-${String(order - 9).padStart(2, '0')}` : basicCodes[order],
      sectionCode: formal ? 'P1A' : 'BASIC_INFO',
      sectionTitle: formal ? 'Partie 1 – Compréhension lexicale 词汇理解 / Section A' : '基本信息',
      sharedMaterial: null,
      formalSection: formal,
      stemText: formal ? `法语单词释义第 ${order - 9} 题` : basicStems[order],
      promptText: formal ? '请选出下列法语单词对应的正确中文含义' : null,
      options,
      required: formal || order === 2 || order === 5,
      justificationRequired: false,
      responses: [],
      justificationText: null,
      displayCondition: order === 8 || order === 9
        ? JSON.stringify({ fieldCode: 'BASIC-ENGLISH-MAJOR', operator: 'EQ', value: 'ENGLISH_MAJOR' })
        : null,
    });
  }
  return questions;
};

const questions = buildQuestions();

const state = {
  answers: {}, // questionOrder -> string[]
  version: 1,
  submitted: false,
};

const attemptPayload = () => ({
  attemptId: 42001,
  releaseCode: RELEASE_CODE,
  paperTitle: 'Lexi-bridge 英法词汇认知迁移研究问卷',
  paperDescription: '跨语言词汇认知迁移的研究与干预',
  instructionsText: null,
  status: state.submitted ? 'SUBMITTED' : 'IN_PROGRESS',
  durationMinutes: 40,
  questionCount: 69,
  answeredCount: Object.values(state.answers).filter((values) => values.some((value) => value && value.trim())).length,
  startedAt: nowIso(),
  expiresAt: new Date(Date.now() + 40 * 60 * 1000).toISOString(),
  lastSavedAt: nowIso(),
  version: state.version,
  serverTime: nowIso(),
  questions: questions.map((question) => ({
    ...question,
    responses: state.answers[question.questionOrder] || [],
  })),
});

const jsonReply = (route, payload) => route.fulfill({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify({ success: true, code: 'OK', message: 'ok', data: payload, traceId: null }),
});

const handleApi = (route, url) => {
  const segments = url.pathname.split('/').filter(Boolean);
  // /api/public/assessments/{releaseCode}[/action]
  const release = segments[3];
  const action = segments[4];
  if (release !== RELEASE_CODE) {
    return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ code: 'NOT_FOUND', message: 'not found', data: null }) });
  }
  if (!action && route.request().method() === 'GET') {
    return jsonReply(route, {
      releaseCode: RELEASE_CODE,
      title: 'Lexi-bridge 英法词汇认知迁移研究问卷',
      description: '一项关于英语与法语之间词义迁移的公开研究。',
      instructionsText: null,
      durationMinutes: 40,
      questionCount: 69,
      formalQuestionCount: 60,
      profileFieldCount: 8,
      status: 'APPROVED',
      startsAt: nowIso(),
      dueAt: null,
      qrEntryEnabled: false,
    });
  }
  if (action === 'verify' && route.request().method() === 'POST') {
    return jsonReply(route, { verified: true, resumed: false, releaseCode: RELEASE_CODE, attempt: attemptPayload() });
  }
  if (action === 'attempt' && route.request().method() === 'GET') {
    return jsonReply(route, attemptPayload());
  }
  if (action === 'responses' && route.request().method() === 'POST') {
    let body = {};
    try {
      body = route.request().postDataJSON() || {};
    } catch {
      body = {};
    }
    state.version += 1;
    state.answers = {};
    (body.responses || []).forEach((entry) => {
      state.answers[entry.questionOrder] = entry.responses || [];
    });
    return jsonReply(route, {
      attemptId: 42001,
      status: 'IN_PROGRESS',
      answeredCount: Object.values(state.answers).filter((values) => values.some((value) => value && value.trim())).length,
      lastSavedAt: nowIso(),
      version: state.version,
    });
  }
  if (action === 'timing' && route.request().method() === 'POST') {
    return jsonReply(route, {});
  }
  if (action === 'submit' && route.request().method() === 'POST') {
    state.submitted = true;
    return jsonReply(route, { attemptId: 42001, status: 'SUBMITTED', submittedAt: nowIso(), version: state.version });
  }
  if (action === 'result' && route.request().method() === 'GET') {
    return jsonReply(route, {
      attemptId: 42001,
      releaseCode: RELEASE_CODE,
      paperTitle: 'Lexi-bridge 英法词汇认知迁移研究问卷',
      status: 'SUBMITTED',
      questionCount: 60,
      answeredCount: 60,
      objectiveScore: 0,
      totalScore: 60,
      submittedAt: nowIso(),
      scoreVisible: true,
      aiAnalysisStatus: 'COMPLETED',
      qualityFlags: ['FAST_ITEM', 'TIMING_GAP'],
      aiAnalysis: {
        performanceOverview: '本次作答呈现出稳定的词形识别能力；部分语境判断仍会受到熟悉词形的干扰。',
        strengths: ['能够识别多数基础词形线索。', '在短语境中保持了较稳定的判断节奏。'],
        risks: ['少数相似词形可能触发英语迁移。', '过快作答题目需要结合复测谨慎解释。'],
        contextInterpretation: '语境信息能够帮助修正第一印象，但长句中的限定成分仍容易被忽略。',
        reactionTimeInterpretation: '整体节奏连续，部分题目反应时间偏短。',
        recommendations: ['先核对词形相似但含义不同的词。', '用完整句子复述词义。', '间隔一周后复测易错词。'],
        confidence: 0.78,
        qualityNotice: '这份结果用于研究反馈，不替代正式语言能力诊断。',
      },
      questions: [],
    });
  }
  return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ code: 'NOT_FOUND', message: 'not found', data: null }) });
};

const bootServer = async () => {
  const child = spawn('npx', ['vite', '--port', '3000', '--strictPort'], {
    cwd: process.cwd(),
    env: { ...process.env, VITE_PROXY_TARGET: 'http://localhost:9' },
    stdio: 'ignore',
  });
  const deadline = Date.now() + 60_000;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`${BASE_URL}/`, { signal: AbortSignal.timeout(2000) });
      if (response.ok || response.status < 500) return child;
    } catch {
      // not ready yet
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  child.kill('SIGTERM');
  throw new Error(`Vite dev server did not become ready at ${BASE_URL}`);
};

const checkPageWidth = async (page, viewport, stage, failures) => {
  const metrics = await page.evaluate(() => {
    const root = document.documentElement;
    const body = document.body;
    return {
      clientWidth: root.clientWidth,
      scrollWidth: Math.max(root.scrollWidth, body?.scrollWidth || 0),
    };
  });
  if (metrics.scrollWidth > metrics.clientWidth + 1) {
    failures.push(`[${viewport.name}] ${stage} overflows horizontally: ${JSON.stringify(metrics)}`);
  }
};

const checkTouchTargets = async (page, selector, viewport, stage, failures) => {
  const targets = await page.locator(selector).evaluateAll((elements) => elements
    .filter((element) => {
      const style = window.getComputedStyle(element);
      return style.display !== 'none' && style.visibility !== 'hidden';
    })
    .map((element) => {
      const style = window.getComputedStyle(element);
      return {
        height: element.getBoundingClientRect().height,
        label: element.getAttribute('aria-label') || element.textContent?.replace(/\s+/g, ' ').trim() || element.tagName,
        minHeight: style.minHeight,
        transform: style.transform,
      };
    }));
  const undersized = targets.filter(({ height }) => height > 0 && height < 44);
  if (undersized.length > 0) {
    failures.push(`[${viewport.name}] ${stage} has touch targets below 44px: ${undersized.map(({ height, label, minHeight, transform }) => `${label}=${height.toFixed(1)} (min ${minHeight}, transform ${transform})`).join(', ')}`);
  }
};

const main = async () => {
  let server = null;
  if (BOOT) server = await bootServer();
  await mkdir(OUT_DIR, { recursive: true });
  let browser = null;
  const failures = [];
  const results = [];

  try {
    browser = await chromium.launch({ headless: HEADLESS });
    for (const viewport of VIEWPORTS) {
      state.answers = {};
      state.version = 1;
      state.submitted = false;
      const context = await browser.newContext({ viewport: { width: viewport.width, height: viewport.height } });
      const page = await context.newPage();
      const apiRequests = [];
      await page.route('**/api/public/assessments/**', (route) => {
        const url = new URL(route.request().url());
        apiRequests.push(`${route.request().method()} ${url.pathname}`);
        return handleApi(route, url);
      });
      await page.goto(`${BASE_URL}/research/${RELEASE_CODE}`, { waitUntil: 'domcontentloaded' });
      await page.locator('.research-facts').waitFor({ state: 'visible', timeout: 15_000 });

      const entryFacts = await page.locator('.research-facts strong').allTextContents();
      if (entryFacts.slice(0, 2).join(',') !== '60,40') {
        failures.push(`[${viewport.name}] entry facts are ${entryFacts.join(',')}, expected 60,40`);
      }
      await checkPageWidth(page, viewport, 'entry', failures);
      await checkTouchTargets(page, '.research-code-form input, .research-code-form button', viewport, 'entry', failures);
      await page.screenshot({ path: path.join(OUT_DIR, `${viewport.name}-entry.png`), fullPage: false });

      await page.locator('#participation-code').fill('AAAA-BBBB-CCCC');
      await page.locator('.research-code-form button[type="submit"]').click();
      await page.locator('[role="timer"]').waitFor({ state: 'visible', timeout: 10_000 });

      const timerText = (await page.locator('[role="timer"]').textContent()) || '';
      if (!timerText.trim().startsWith('40:00')) {
        failures.push(`[${viewport.name}] countdown did not start at 40:00, got "${timerText}"`);
      }

      const statusLine = await page.locator('.research-progress-meta').textContent();
      if (!statusLine || !statusLine.includes('正式题 60 道') || !statusLine.includes('限时 40 分钟')) {
        failures.push(`[${viewport.name}] status line missing 60 道/40 分钟: "${statusLine}"`);
      }
      await checkPageWidth(page, viewport, 'assessment', failures);
      await checkTouchTargets(page, '.research-question-footer button', viewport, 'assessment navigation', failures);
      if (viewport.width <= 1024) {
        await checkTouchTargets(page, '.research-map-dot', viewport, 'question map', failures);
      }

      const timerBox = async () => {
        const box = await page.locator('[role="timer"]').boundingBox();
        return box;
      };
      const rightRegion = (box) => box && box.x + box.width / 2 > viewport.width / 2 && box.y < 200;
      let box = await timerBox();
      if (!rightRegion(box)) {
        failures.push(`[${viewport.name}] countdown not in top-right region at top: ${JSON.stringify(box)}`);
      }
      await page.mouse.wheel(0, 500);
      await page.waitForTimeout(400);
      box = await timerBox();
      if (!rightRegion(box)) {
        failures.push(`[${viewport.name}] countdown not in top-right region after scroll: ${JSON.stringify(box)}`);
      }

      await page.locator('.research-question-footer button').last().click();
      await page.waitForTimeout(300);
      await page.locator('#text-answer-2').fill('张三');
      await page.waitForTimeout(1200);
      let progressText = (await page.locator('.research-progress-meta').textContent()) || '';
      if (!progressText.includes('资料 01/06')) {
        failures.push(`[${viewport.name}] non-English-major profile count not 01/06: "${progressText}"`);
      }

      await page.locator('[role="timer"]').scrollIntoViewIfNeeded();
      await page.screenshot({ path: path.join(OUT_DIR, `${viewport.name}-profile.png`), fullPage: false });

      const stageNumber = async () => {
        const raw = (await page.locator('.research-question-number').textContent()) || '';
        const match = raw.match(/资料\s*(\d+)\//);
        return match ? Number(match[1]) : 0;
      };
      const goToStage = async (target) => {
        let guard = 0;
        while (guard < 12 && (await stageNumber()) !== target) {
          const current = await stageNumber();
          const buttons = page.locator('.research-question-footer button');
          const button = current > target ? buttons.first() : buttons.last();
          await button.click();
          await page.waitForTimeout(250);
          guard += 1;
        }
        if ((await stageNumber()) !== target) {
          failures.push(`[${viewport.name}] could not navigate to profile stage ${target}`);
        }
      };

      await goToStage(4);
      await page.locator('.research-option').first().click();
      await page.waitForTimeout(300);
      progressText = (await page.locator('.research-progress-meta').textContent()) || '';
      if (!progressText.includes('资料 02/08')) {
        failures.push(`[${viewport.name}] English-major profile count not 02/08: "${progressText}"`);
      }
      await page.waitForTimeout(1200);

      await goToStage(7);
      const temInput = page.locator('#number-answer-8');
      if (!(await temInput.isVisible())) {
        failures.push(`[${viewport.name}] TEM4 field should be visible for English majors`);
      }
      await temInput.fill('80');
      await page.waitForTimeout(1200);

      await goToStage(4);
      await page.locator('.research-option').nth(1).click();
      await page.waitForTimeout(300);
      progressText = (await page.locator('.research-progress-meta').textContent()) || '';
      if (!progressText.includes('资料 02/06')) {
        failures.push(`[${viewport.name}] non-English-major profile count not 02/06 after switch: "${progressText}"`);
      }
      await page.waitForTimeout(400);

      await goToStage(4);
      await page.locator('.research-option').first().click();
      await page.waitForTimeout(300);
      await goToStage(7);
      const temValue = (await page.locator('#number-answer-8').inputValue()).trim();
      if (temValue !== '') {
        failures.push(`[${viewport.name}] TEM4 answer not cleared after switching back to English major, got "${temValue}"`);
      }

      await page.screenshot({ path: path.join(OUT_DIR, `${viewport.name}-assessment.png`), fullPage: false });

      await page.getByRole('button', { name: /^正式题1，/ }).click();
      await page.locator('.research-option').first().waitFor({ state: 'visible', timeout: 10_000 });
      await checkPageWidth(page, viewport, 'formal question', failures);
      await checkTouchTargets(page, '.research-option, .research-question-footer button', viewport, 'formal question', failures);
      await page.screenshot({ path: path.join(OUT_DIR, `${viewport.name}-formal.png`), fullPage: false });

      state.submitted = true;
      await page.evaluate((releaseCode) => {
        window.localStorage.setItem(`ef-transfer-public-assessment-session:${releaseCode}`, String(Date.now()));
      }, RELEASE_CODE);
      await page.goto(`${BASE_URL}/research/${RELEASE_CODE}?qa=result`, { waitUntil: 'domcontentloaded' });
      try {
        await page.locator('.research-result h1').waitFor({ state: 'visible', timeout: 10_000 });
      } catch (error) {
        const bodyText = ((await page.locator('body').textContent()) || '').replace(/\s+/g, ' ').trim();
        throw new Error(`Result page did not render for ${viewport.name}; body ends with "${bodyText.slice(-480)}"; requests: ${apiRequests.slice(-8).join(' | ')}`, { cause: error });
      }
      await page.locator('.research-result-summary').waitFor({ state: 'visible', timeout: 10_000 });
      await page.locator('.research-analysis-content').waitFor({ state: 'visible', timeout: 10_000 });
      await checkPageWidth(page, viewport, 'result', failures);
      const resultHeading = await page.locator('.research-result h1').boundingBox();
      if (!resultHeading || resultHeading.x < -1 || resultHeading.x + resultHeading.width > viewport.width + 1) {
        failures.push(`[${viewport.name}] result heading escapes viewport: ${JSON.stringify(resultHeading)}`);
      }
      await page.screenshot({ path: path.join(OUT_DIR, `${viewport.name}-result.png`), fullPage: true });

      results.push(`[${viewport.name}] ok`);
      await context.close();
    }
  } finally {
    if (browser) await browser.close();
    if (server) server.kill('SIGTERM');
  }

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    viewports: VIEWPORTS.map((viewport) => viewport.name),
    results,
    failures,
  };
  await writeFile(path.join(OUT_DIR, 'report.json'), JSON.stringify(report, null, 2) + '\n', 'utf-8');
  results.forEach((line) => console.log(line));
  if (failures.length > 0) {
    failures.forEach((line) => console.error(`FAIL ${line}`));
    console.error(`Screenshots and report: ${OUT_DIR}`);
    process.exitCode = 1;
  } else {
    console.log(`All viewport checks passed. Screenshots and report: ${OUT_DIR}`);
  }
};

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
