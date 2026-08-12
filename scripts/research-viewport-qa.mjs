#!/usr/bin/env node
/* global console, fetch, AbortSignal, setTimeout, URL */
/**
 * Research questionnaire viewport QA (desktop/tablet/mobile).
 *
 * Boots the local Vite dev server, mocks the public-assessment API, and verifies:
 *   - entry page shows 60 formal questions and 60 minutes
 *   - answering page status line "正式题 60 道 · 限时 60 分钟"
 *   - countdown starts at 60:00 and stays in the top-right region on scroll
 *   - non-English-major profile shows 资料 xx/06; English-major shows /08
 *   - switching back from English major clears hidden TEM answers
 *   - text inputs/buttons keep 44px-plus touch targets
 *   - screenshots at 1440x900, 1024x768, 390x844 and 320px width
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

const VIEWPORTS = (process.env.VIEWPORTS || '1440x900,1024x768,390x844,320x568')
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
};

const attemptPayload = () => ({
  attemptId: 42001,
  releaseCode: RELEASE_CODE,
  paperTitle: 'Lexi-bridge 英法词汇认知迁移研究问卷',
  paperDescription: '跨语言词汇认知迁移的研究与干预',
  instructionsText: null,
  status: 'IN_PROGRESS',
  durationMinutes: 60,
  questionCount: 69,
  answeredCount: Object.values(state.answers).filter((values) => values.some((value) => value && value.trim())).length,
  startedAt: nowIso(),
  expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
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
      durationMinutes: 60,
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
    return jsonReply(route, { verified: true, resumed: true, releaseCode: RELEASE_CODE, attempt: attemptPayload() });
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
    return jsonReply(route, { attemptId: 42001, status: 'SUBMITTED', submittedAt: nowIso(), version: state.version });
  }
  if (action === 'result' && route.request().method() === 'GET') {
    return jsonReply(route, {
      attemptId: 42001,
      paperTitle: 'Lexi-bridge 英法词汇认知迁移研究问卷',
      objectiveScore: 0,
      totalScore: 60,
      scoreVisible: true,
      aiAnalysisStatus: 'SKIPPED',
      qualityFlags: [],
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

const main = async () => {
  let server = null;
  if (BOOT) server = await bootServer();
  await mkdir(OUT_DIR, { recursive: true });
  const browser = await chromium.launch({ headless: HEADLESS });
  const failures = [];
  const results = [];

  try {
    for (const viewport of VIEWPORTS) {
      state.answers = {};
      state.version = 1;
      const context = await browser.newContext({ viewport: { width: viewport.width, height: viewport.height } });
      const page = await context.newPage();
      await page.route('**/api/public/assessments/**', (route) => handleApi(route, new URL(route.request().url())));
      await page.goto(`${BASE_URL}/research/${RELEASE_CODE}`, { waitUntil: 'domcontentloaded' });
      await page.locator('.research-facts').waitFor({ state: 'visible', timeout: 15_000 });

      const entryFacts = await page.locator('.research-facts strong').allTextContents();
      if (entryFacts.slice(0, 2).join(',') !== '60,60') {
        failures.push(`[${viewport.name}] entry facts are ${entryFacts.join(',')}, expected 60,60`);
      }
      await page.screenshot({ path: path.join(OUT_DIR, `${viewport.name}-entry.png`), fullPage: false });

      await page.locator('#participation-code').fill('AAAA-BBBB-CCCC');
      await page.locator('.research-code-form button[type="submit"]').click();
      await page.locator('[role="timer"]').waitFor({ state: 'visible', timeout: 10_000 });

      const timerText = (await page.locator('[role="timer"]').textContent()) || '';
      if (!timerText.trim().startsWith('60:00')) {
        failures.push(`[${viewport.name}] countdown did not start at 60:00, got "${timerText}"`);
      }

      const statusLine = await page.locator('.research-progress-meta').textContent();
      if (!statusLine || !statusLine.includes('正式题 60 道') || !statusLine.includes('限时 60 分钟')) {
        failures.push(`[${viewport.name}] status line missing 60 道/60 分钟: "${statusLine}"`);
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

      const touchTarget = await page.locator('.research-text-input').first().boundingBox();
      if (touchTarget && touchTarget.height < 44) {
        failures.push(`[${viewport.name}] text input touch target ${touchTarget.height}px < 44px`);
      }

      await page.screenshot({ path: path.join(OUT_DIR, `${viewport.name}-assessment.png`), fullPage: false });
      results.push(`[${viewport.name}] ok`);
      await context.close();
    }
  } finally {
    await browser.close();
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
