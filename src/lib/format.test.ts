import { describe, expect, it } from 'vitest';
import {
  assessmentAttemptStatusLabel,
  diagnosisTemplateSyncStateLabel,
  errorTypeLabel,
  invitationStatusLabel,
  lexicalPairTypeLabel,
  profileLinkStatusLabel,
  riskLevelLabel,
  roleLabel,
  sessionActivityLabel,
  trainingModeLabel,
  workspaceLabels,
} from './format';

describe('format display mappings', () => {
  it('maps student-facing enum values to Chinese labels', () => {
    expect(riskLevelLabel('HIGH')).toBe('高风险');
    expect(trainingModeLabel('FALSE_FRIEND_DISCRIM')).toBe('纠偏：同形异义词辨析');
    expect(assessmentAttemptStatusLabel('IN_PROGRESS')).toBe('进行中');
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
});
