import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';

const BASE = process.env.BASE_URL || 'https://huashi.qsfw.eu.cc';
const USER = process.env.ADMIN_USER || 'admin';
const PASS = process.env.ADMIN_PASS || 'Admin@123456';

async function main() {
  await mkdir('qa-output/config-center-probe', { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const errors = [];
  const failed = [];

  page.on('pageerror', (e) => errors.push({ type: 'pageerror', message: e.message, stack: e.stack }));
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push({ type: 'console', message: msg.text() });
  });
  page.on('response', async (res) => {
    if (res.status() >= 400 && res.url().includes('/api/')) {
      let body = '';
      try {
        body = (await res.text()).slice(0, 500);
      } catch {
        body = '';
      }
      failed.push({ url: res.url(), status: res.status(), body });
    }
  });

  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle', timeout: 60_000 });
  await page.locator('input[type="password"]').fill(PASS);
  const textInput = page.locator('input[type="text"], input[name="usernameOrEmail"], input:not([type="password"])').first();
  await textInput.fill(USER);
  await Promise.all([
    page.waitForURL((u) => !u.pathname.includes('/login'), { timeout: 45_000 }).catch(() => null),
    page.click('button[type="submit"]'),
  ]);
  await page.waitForTimeout(1200);

  await page.goto(`${BASE}/admin/config-center`, { waitUntil: 'networkidle', timeout: 60_000 });
  await page.waitForTimeout(3500);

  const bodyText = await page.locator('body').innerText();
  const htmlSnippet = await page.locator('main, [role="alert"], body').first().innerHTML().catch(() => '');
  await page.screenshot({ path: 'qa-output/config-center-probe/page.png', fullPage: true });

  console.log(JSON.stringify({
    url: page.url(),
    title: await page.title(),
    bodySnippet: bodyText.slice(0, 2000),
    errors,
    failedApis: failed,
    hasErrorBoundary: /页面加载遇到问题|could not be rendered|ErrorBoundary|渲染失败/i.test(bodyText),
    htmlSnippet: htmlSnippet.slice(0, 1500),
  }, null, 2));

  await browser.close();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
