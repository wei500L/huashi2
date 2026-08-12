import { beforeEach, describe, expect, it } from 'vitest';
import {
  formatDate,
  formatDateTime,
  parseBackendDateTime,
  assessmentAttemptStatusLabel,
  assessmentPaperStatusLabel,
  diagnosisSessionStatusLabel,
  diagnosisTaskTypeLabel,
  diagnosisTemplateSyncStateLabel,
  embeddingStatusLabel,
  errorTypeLabel,
  invitationStatusLabel,
  lexicalPairTypeLabel,
  lexicalImportBatchStatusLabel,
  lexicalImportRowStatusLabel,
  profileLinkStatusLabel,
  riskLevelLabel,
  roleLabel,
  sessionActivityLabel,
  trainingSessionStatusLabel,
  trainingModeLabel,
  workspaceLabels,
} from './format';
import i18n from './i18n';

beforeEach(async () => {
  await i18n.changeLanguage('zh-CN');
});

describe('format display mappings', () => {
  it('maps student-facing enum values to Chinese labels', () => {
    expect(riskLevelLabel('HIGH')).toBe('高风险');
    expect(trainingModeLabel('FALSE_FRIEND_DISCRIM')).toBe('纠偏：同形异义词辨析');
    expect(diagnosisTaskTypeLabel('REACTION_TIME')).toBe('反应时判断');
    expect(diagnosisSessionStatusLabel('COMPLETED')).toBe('已完成');
    expect(diagnosisSessionStatusLabel('ABANDONED')).toBe('已废弃');
    expect(trainingSessionStatusLabel('IN_PROGRESS')).toBe('进行中');
    expect(trainingSessionStatusLabel('ABANDONED')).toBe('已废弃');
    expect(assessmentAttemptStatusLabel('IN_PROGRESS')).toBe('进行中');
    expect(lexicalImportBatchStatusLabel('IMPORTING')).toBe('导入中');
    expect(lexicalImportRowStatusLabel('INVALID')).toBe('需修正');
    expect(embeddingStatusLabel('PENDING')).toBe('待嵌入');
    expect(errorTypeLabel('FALSE_FRIEND_CONFUSION')).toBe('假朋友混淆');
    expect(lexicalPairTypeLabel('FALSE_FRIEND')).toBe('同形异义词');
  });

  it('maps teacher and admin enum values to Chinese labels', () => {
    expect(diagnosisTemplateSyncStateLabel('DIRTY')).toBe('待同步');
    expect(invitationStatusLabel('CONSUMED')).toBe('已使用');
    expect(profileLinkStatusLabel('STUDENT_ONLY')).toBe('仅关联学生档案');
    expect(roleLabel('ADMIN')).toBe('管理员');
    expect(workspaceLabels(['ADMIN_CONSOLE', 'STUDENT_WORKSPACE'])).toEqual(['管理后台', '学生工作台']);
    expect(sessionActivityLabel(true)).toBe('活跃中');
    expect(sessionActivityLabel(false)).toBe('无活动会话');
  });

  it('does not leak raw enum codes for unknown values', () => {
    expect(trainingModeLabel('UNKNOWN_MODE')).toBe('未定义训练模式');
    expect(invitationStatusLabel('UNKNOWN')).toBe('未知邀请状态');
    expect(profileLinkStatusLabel('MYSTERY')).toBe('未知关联状态');
    expect(lexicalPairTypeLabel('ALIEN')).toBe('未定义词对类型');
  });

  it('formats naive backend timestamps in Beijing time', () => {
    expect(parseBackendDateTime('2026-08-12T05:40:08').toISOString()).toBe('2026-08-12T05:40:08.000Z');
    expect(formatDateTime('2026-08-12T05:40:08')).toContain('13:40');
    expect(formatDateTime('2026-08-12T05:40:08')).toMatch(/2026/);
    expect(formatDateTime('2026-08-12T05:40:08')).toMatch(/8月12日|12/);
    expect(formatDate('2026-08-12T16:20:00')).toContain('2026');
  });

  it('switches labels with the active locale', async () => {
    await i18n.changeLanguage('en-US');

    expect(riskLevelLabel('HIGH')).toBe('High risk');
    expect(trainingModeLabel('FALSE_FRIEND_DISCRIM')).toBe('Correct: false-friend discrimination');
    expect(diagnosisTaskTypeLabel('REACTION_TIME')).toBe('Reaction-time check');
    expect(diagnosisSessionStatusLabel('COMPLETED')).toBe('Completed');
    expect(diagnosisSessionStatusLabel('ABANDONED')).toBe('Abandoned');
    expect(trainingSessionStatusLabel('IN_PROGRESS')).toBe('In progress');
    expect(trainingSessionStatusLabel('ABANDONED')).toBe('Abandoned');
    expect(assessmentAttemptStatusLabel('IN_PROGRESS')).toBe('In progress');
    expect(assessmentPaperStatusLabel('DRAFT')).toBe('Draft');
    expect(lexicalImportBatchStatusLabel('IMPORTING')).toBe('Importing');
    expect(lexicalImportRowStatusLabel('INVALID')).toBe('Needs correction');
    expect(embeddingStatusLabel('FAILED')).toBe('Embedding failed');
    expect(roleLabel('ADMIN')).toBe('Administrator');
    expect(workspaceLabels(['ADMIN_CONSOLE', 'STUDENT_WORKSPACE'])).toEqual(['Admin console', 'Student workspace']);
    expect(sessionActivityLabel(true)).toBe('Active');
    expect(sessionActivityLabel(false)).toBe('No active session');
  });
});
