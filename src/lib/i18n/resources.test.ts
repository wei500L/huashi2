import { describe, expect, it } from 'vitest';
import { resources } from './resources';

describe('i18n resources', () => {
  it('merges all feature slices into the single translation namespace', () => {
    const zh = resources['zh-CN'].translation as {
      common: unknown;
      shell: unknown;
      teacherWorkspace: unknown;
      diagnosis: unknown;
      training: unknown;
      dashboard: unknown;
      analytics: unknown;
      ui: unknown;
    };

    expect(zh.common).toBeDefined();
    expect(zh.shell).toBeDefined();
    expect(zh.teacherWorkspace).toBeDefined();
    expect(zh.diagnosis).toBeDefined();
    expect(zh.training).toBeDefined();
    expect(zh.dashboard).toBeDefined();
    expect(zh.analytics).toBeDefined();
    expect(zh.ui).toBeDefined();
  });

  it('keeps existing translation key paths stable across locales', () => {
    const zh = resources['zh-CN'].translation as {
      teacherWorkspace: {
        pageTitle: string;
        generated: {
          focus: {
            interventions: {
              title: string;
            };
          };
        };
      };
    };
    const en = resources['en-US'].translation as {
      shell: {
        nav: {
          teacherWorkspace: string;
        };
      };
      training: {
        summaryTitle: string;
      };
      ui: {
        actions: {
          createPaper: string;
        };
      };
    };

    expect(zh.teacherWorkspace.pageTitle).toBe('教师工作台');
    expect(en.shell.nav.teacherWorkspace).toBe('Teacher Workspace');
    expect(en.training.summaryTitle).toBe('Training Summary');
    expect(zh.teacherWorkspace.generated.focus.interventions.title).toBe('处理待跟进干预');
    expect(en.ui.actions.createPaper).toBe('New paper');
  });
});
