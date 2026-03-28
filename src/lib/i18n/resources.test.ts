import { describe, expect, it } from 'vitest';
import { resources } from './resources';

describe('i18n resources', () => {
  it('merges all feature slices into the single translation namespace', () => {
    expect(resources['zh-CN'].translation.common).toBeDefined();
    expect(resources['zh-CN'].translation.shell).toBeDefined();
    expect(resources['zh-CN'].translation.teacherWorkspace).toBeDefined();
    expect(resources['zh-CN'].translation.diagnosis).toBeDefined();
    expect(resources['zh-CN'].translation.training).toBeDefined();
    expect(resources['zh-CN'].translation.dashboard).toBeDefined();
    expect(resources['zh-CN'].translation.analytics).toBeDefined();
  });

  it('keeps existing translation key paths stable across locales', () => {
    expect(resources['zh-CN'].translation.teacherWorkspace.pageTitle).toBe('教师工作台');
    expect(resources['en-US'].translation.shell.nav.teacherWorkspace).toBe('Teacher Workspace');
    expect(resources['en-US'].translation.training.summaryTitle).toBe('Training Summary');
  });
});
