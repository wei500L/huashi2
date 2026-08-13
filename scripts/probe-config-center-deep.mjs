import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';

const BASE = process.env.BASE_URL || 'https://huashi.mnari.cn';

async function main() {
  await mkdir('qa-output/config-center-probe', { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const errors = [];
  page.on('pageerror', (e) => errors.push(`PAGE: ${e.message}\n${e.stack || ''}`));
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(`CONSOLE: ${msg.text()}`);
  });

  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle', timeout: 60_000 });
  await page.locator('input[type="password"]').fill('Admin@123456');
  await page.locator('input[type="text"], input[name="usernameOrEmail"], input:not([type="password"])').first().fill('admin');
  await Promise.all([
    page.waitForURL((u) => !u.pathname.includes('/login'), { timeout: 45_000 }).catch(() => null),
    page.click('button[type="submit"]'),
  ]);
  await page.waitForTimeout(1000);
  await page.goto(`${BASE}/admin/config-center`, { waitUntil: 'domcontentloaded', timeout: 60_000 });
  await page.waitForTimeout(2500);

  const steps = [];
  const clickIf = async (name, locator) => {
    const count = await locator.count();
    if (!count) {
      steps.push(`${name}: missing`);
      return false;
    }
    try {
      await locator.first().click({ timeout: 5000 });
      await page.waitForTimeout(800);
      steps.push(`${name}: ok`);
      return true;
    } catch (error) {
      steps.push(`${name}: fail ${error instanceof Error ? error.message : String(error)}`);
      return false;
    }
  };

  await clickIf('tab-stability', page.getByRole('button', { name: '稳定性' }));
  await clickIf('tab-rag', page.getByRole('button', { name: 'RAG 参数' }));
  await clickIf('tab-ops', page.getByRole('button', { name: '运维操作' }));
  await clickIf('tab-provider', page.getByRole('button', { name: '模型接入' }));
  await clickIf('edit', page.getByRole('button', { name: /进入编辑/ }));
  await clickIf('expand-provider', page.getByText('展开查看 Chat / Embedding / Rerank 详细配置'));
  await clickIf('validate', page.getByRole('button', { name: /校验/ }));
  await clickIf('cancel', page.getByRole('button', { name: /取消|退出编辑/ }));

  const bodyText = await page.locator('body').innerText();
  await page.screenshot({ path: 'qa-output/config-center-probe/mobile-interact.png', fullPage: true });
  console.log(JSON.stringify({
    url: page.url(),
    steps,
    errors,
    hasErrorBoundary: /页面加载遇到问题|渲染失败|could not be rendered/i.test(bodyText),
    bodySnippet: bodyText.slice(0, 1200),
  }, null, 2));
  await browser.close();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
