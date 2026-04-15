import { describe, expect, it } from 'vitest';
import type { TFunction } from 'i18next';
import { buildDocumentTitle, resolveRouteTitle } from './page-title';
import { resources } from './i18n/resources';

function translate(locale: 'zh-CN' | 'en-US'): TFunction {
  const dictionary = resources[locale].translation as Record<string, unknown>;

  return ((key: string) => {
    return key.split('.').reduce<unknown>((current, segment) => {
      if (!current || typeof current !== 'object') {
        return undefined;
      }
      return (current as Record<string, unknown>)[segment];
    }, dictionary) as string;
  }) as unknown as TFunction;
}

describe('page title helpers', () => {
  it('resolves task-oriented route titles from shell translations', () => {
    const t = translate('zh-CN');

    expect(resolveRouteTitle('/diagnosis', t)).toBe('开始风险诊断');
    expect(resolveRouteTitle('/analytics', t)).toBe('查看高风险词对');
    expect(resolveRouteTitle('/teacher/diagnosis-templates', t)).toBe('发布诊断任务');
    expect(resolveRouteTitle('/teacher/interventions', t)).toBe('跟进高风险学生');
    expect(resolveRouteTitle('/register', t)).toBe('学生注册');
  });

  it('builds the document title from the resolved task title and app name', () => {
    const t = translate('en-US');

    expect(buildDocumentTitle('/analytics', t)).toBe('Review High-Risk Pairs | EF.Transfer');
  });
});
