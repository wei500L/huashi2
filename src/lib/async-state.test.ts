import { describe, expect, it } from 'vitest';
import { ApiError } from './api';
import { getProductizedErrorState } from './async-state';

describe('getProductizedErrorState', () => {
  it('maps expired sessions to a permission-style login recovery state', () => {
    const state = getProductizedErrorState(new ApiError('jwt expired', 401, 'TOKEN_EXPIRED'), {
      resourceLabel: '学习历史',
      taskLabel: '查看学习历史',
    });

    expect(state.kind).toBe('permission');
    expect(state.title).toBe('登录状态已失效');
    expect(state.nextStep).toContain('重新登录');
    expect(JSON.stringify(state)).not.toContain('jwt expired');
  });

  it('maps 403 errors to a permission state without leaking raw backend copy', () => {
    const state = getProductizedErrorState(new ApiError('permission denied by upstream', 403, 'ACCESS_DENIED'), {
      resourceLabel: '诊断结果',
      taskLabel: '查看诊断详情',
    });

    expect(state.kind).toBe('permission');
    expect(state.title).toBe('当前账号暂无访问权限');
    expect(state.description).toContain('不会在当前页面展示');
    expect(JSON.stringify(state)).not.toContain('permission denied by upstream');
  });

  it('maps 5xx errors to a retryable state', () => {
    const state = getProductizedErrorState(new ApiError('upstream timeout', 503), {
      resourceLabel: '训练总结',
      taskLabel: '查看训练详情',
      retryActionLabel: '重新获取',
    });

    expect(state.kind).toBe('retry');
    expect(state.title).toBe('服务暂时没有响应');
    expect(state.nextStep).toContain('重新获取');
    expect(JSON.stringify(state)).not.toContain('upstream timeout');
  });

  it('maps non-retryable 4xx errors to a generic request failure state', () => {
    const state = getProductizedErrorState(new ApiError('bad request from api', 400), {
      resourceLabel: '测评结果',
      taskLabel: '查看测评详情',
    });

    expect(state.kind).toBe('error');
    expect(state.title).toBe('测评结果暂时没有加载成功');
    expect(state.nextStep).toContain('稍后再试');
    expect(JSON.stringify(state)).not.toContain('bad request from api');
  });
});
