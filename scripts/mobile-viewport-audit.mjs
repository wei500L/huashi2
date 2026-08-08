#!/usr/bin/env node
/**
 * Mobile viewport smoke audit for EF.Transfer.
 *
 * Usage:
 *   node scripts/mobile-viewport-audit.mjs
 *   BASE_URL=https://huashi.qsfw.eu.cc node scripts/mobile-viewport-audit.mjs
 *
 * Optional env:
 *   BASE_URL          default https://huashi.qsfw.eu.cc
 *   OUT_DIR           default qa-output/mobile-responsive-<date>
 *   STUDENT_USER      default student.li
 *   STUDENT_PASS      default Student@123456
 *   TEACHER_USER      default teacher.zhang
 *   TEACHER_PASS      default Teacher@123456
 *   ADMIN_USER        default admin
 *   ADMIN_PASS        default Admin@123456
 *   VIEWPORTS         default 375x812,390x844,768x1024
 *   HEADLESS          default 1
 */

import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { chromium } from 'playwright';

const BASE_URL = (process.env.BASE_URL || 'https://huashi.qsfw.eu.cc').replace(/\/$/, '');
const dateStamp = new Date().toISOString().slice(0, 10);
const OUT_DIR = process.env.OUT_DIR || path.join('qa-output', `mobile-responsive-${dateStamp}`);
const HEADLESS = process.env.HEADLESS !== '0';

const ACCOUNTS = {
  student: {
    username: process.env.STUDENT_USER || 'student.li',
    password: process.env.STUDENT_PASS || 'Student@123456',
  },
  teacher: {
    username: process.env.TEACHER_USER || 'teacher.zhang',
    password: process.env.TEACHER_PASS || 'Teacher@123456',
  },
  admin: {
    username: process.env.ADMIN_USER || 'admin',
    password: process.env.ADMIN_PASS || 'Admin@123456',
  },
};

const VIEWPORTS = (process.env.VIEWPORTS || '375x812,390x844,768x1024')
  .split(',')
  .map((token) => token.trim())
  .filter(Boolean)
  .map((token) => {
    const [width, height] = token.split('x').map(Number);
    return { name: token, width, height };
  });

const ROUTES = {
  student: [
    '/dashboard',
    '/diagnosis',
    '/training',
    '/analytics',
    '/assessments',
    '/errors',
    '/history',
    '/student/research',
    '/settings',
  ],
  teacher: [
    '/teacher/workspace',
    '/teacher/classes',
    '/teacher/assessments',
    '/teacher/research',
    '/teacher/diagnosis-templates',
    '/teacher/lexical-pairs',
    '/teacher/lexical-lists',
    '/teacher/interventions',
  ],
  admin: [
    '/admin/dashboard',
    '/admin/users',
    '/admin/audit-logs',
    '/admin/config',
    '/admin/lexical-pairs',
  ],
  public: ['/login', '/register', '/research'],
};

function slug(value) {
  return String(value).replace(/[^a-zA-Z0-9._-]+/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '');
}

async function login(page, account) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle', timeout: 60_000 });
  await page.fill('input[name="usernameOrEmail"], input#usernameOrEmail, input[type="text"]', account.username);
  await page.fill('input[name="password"], input#password, input[type="password"]', account.password);
  await Promise.all([
    page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 45_000 }).catch(() => null),
    page.click('button[type="submit"]'),
  ]);
  await page.waitForTimeout(800);
}

async function measurePage(page) {
  return page.evaluate(() => {
    const doc = document.documentElement;
    const body = document.body;
    const scrollWidth = Math.max(doc.scrollWidth, body?.scrollWidth || 0);
    const clientWidth = doc.clientWidth;
    const overflowX = scrollWidth > clientWidth + 1;
    const wideNodes = Array.from(document.querySelectorAll('body *'))
      .filter((el) => {
        if (!(el instanceof HTMLElement)) return false;
        const style = window.getComputedStyle(el);
        if (style.display === 'none' || style.visibility === 'hidden') return false;
        const rect = el.getBoundingClientRect();
        return rect.width > clientWidth + 2;
      })
      .slice(0, 8)
      .map((el) => {
        const rect = el.getBoundingClientRect();
        return {
          tag: el.tagName.toLowerCase(),
          className: typeof el.className === 'string' ? el.className.slice(0, 160) : '',
          width: Math.round(rect.width),
        };
      });
    return {
      path: window.location.pathname + window.location.search,
      title: document.title,
      scrollWidth,
      clientWidth,
      overflowX,
      wideNodes,
    };
  });
}

async function captureRoute(page, role, route, viewport) {
  const url = `${BASE_URL}${route}`;
  const shotName = `${role}__${slug(route)}__${viewport.name}.png`;
  const shotPath = path.join(OUT_DIR, 'screenshots', shotName);
  try {
    await page.goto(url, { waitUntil: 'networkidle', timeout: 60_000 });
    await page.waitForTimeout(500);
    // Dismiss onboarding overlays if present so overflow is measured on content.
    const skip = page.getByRole('button', { name: /跳过|Skip|关闭|Close/i }).first();
    if (await skip.isVisible().catch(() => false)) {
      await skip.click().catch(() => null);
      await page.waitForTimeout(250);
    }
    const metrics = await measurePage(page);
    await page.screenshot({ path: shotPath, fullPage: true });
    return {
      role,
      route,
      viewport: viewport.name,
      ok: !metrics.overflowX,
      screenshot: shotPath,
      ...metrics,
    };
  } catch (error) {
    return {
      role,
      route,
      viewport: viewport.name,
      ok: false,
      error: error instanceof Error ? error.message : String(error),
      screenshot: shotPath,
    };
  }
}

async function auditRole(browser, role, routes) {
  const results = [];
  for (const viewport of VIEWPORTS) {
    const context = await browser.newContext({
      viewport: { width: viewport.width, height: viewport.height },
      deviceScaleFactor: 1,
      isMobile: viewport.width < 768,
      hasTouch: viewport.width < 768,
    });
    const page = await context.newPage();
    try {
      if (role !== 'public') {
        await login(page, ACCOUNTS[role]);
      }
      for (const route of routes) {
        // eslint-disable-next-line no-await-in-loop
        const result = await captureRoute(page, role, route, viewport);
        results.push(result);
        const status = result.ok ? 'PASS' : 'FAIL';
        console.log(`[${status}] ${role} ${viewport.name} ${route}${result.error ? ` :: ${result.error}` : result.overflowX ? ' :: overflow-x' : ''}`);
      }
    } finally {
      await context.close();
    }
  }
  return results;
}

async function main() {
  await mkdir(path.join(OUT_DIR, 'screenshots'), { recursive: true });
  const browser = await chromium.launch({ headless: HEADLESS });
  const all = [];
  try {
    all.push(...(await auditRole(browser, 'public', ROUTES.public)));
    all.push(...(await auditRole(browser, 'student', ROUTES.student)));
    all.push(...(await auditRole(browser, 'teacher', ROUTES.teacher)));
    all.push(...(await auditRole(browser, 'admin', ROUTES.admin)));
  } finally {
    await browser.close();
  }

  const failures = all.filter((item) => !item.ok);
  const summary = {
    baseUrl: BASE_URL,
    generatedAt: new Date().toISOString(),
    viewports: VIEWPORTS,
    total: all.length,
    passed: all.length - failures.length,
    failed: failures.length,
    failures: failures.map((item) => ({
      role: item.role,
      route: item.route,
      viewport: item.viewport,
      overflowX: item.overflowX,
      scrollWidth: item.scrollWidth,
      clientWidth: item.clientWidth,
      error: item.error,
      wideNodes: item.wideNodes,
    })),
    results: all,
  };

  await writeFile(path.join(OUT_DIR, 'overflow.json'), JSON.stringify(summary, null, 2), 'utf8');
  const report = [
    `# Mobile viewport audit`,
    '',
    `- Base URL: \`${BASE_URL}\``,
    `- Generated: ${summary.generatedAt}`,
    `- Total: ${summary.total}`,
    `- Passed: ${summary.passed}`,
    `- Failed: ${summary.failed}`,
    '',
    '## Failures',
    '',
  ];
  if (!failures.length) {
    report.push('_None_');
  } else {
    for (const item of failures) {
      report.push(`- **${item.role}** \`${item.route}\` @ ${item.viewport}: ${item.error || `overflow ${item.scrollWidth}>${item.clientWidth}`}`);
    }
  }
  report.push('', '## Notes', '', '- Table-internal horizontal scroll is expected only inside `.scroll-region`.', '- This script measures document-level `scrollWidth` after optional onboarding dismiss.', '');
  await writeFile(path.join(OUT_DIR, 'report.md'), report.join('\n'), 'utf8');
  console.log(`\nWrote ${path.join(OUT_DIR, 'report.md')} (${summary.passed}/${summary.total} passed)`);
  process.exitCode = failures.length ? 1 : 0;
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
