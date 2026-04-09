import { normalizeApiError } from './api';

export type AsyncStateKind = 'loading' | 'empty' | 'permission' | 'error' | 'retry';

export type ProductizedErrorState = {
  kind: Extract<AsyncStateKind, 'permission' | 'error' | 'retry'>;
  title: string;
  description: string;
  impact: string;
  nextStep: string;
};

type ProductizedErrorOptions = {
  resourceLabel?: string;
  taskLabel?: string;
  retryActionLabel?: string;
  permissionNextStep?: string;
};

const RETRYABLE_CODES = new Set(['RATE_LIMITED', 'AI_PROVIDER_UNAVAILABLE']);
const PERMISSION_CODES = new Set(['FORBIDDEN', 'ACCESS_DENIED', 'NO_PERMISSION', 'NO_ACCESS']);

export function getProductizedErrorState(
  error: unknown,
  options: ProductizedErrorOptions = {}
): ProductizedErrorState {
  const normalizedError = normalizeApiError(error);
  const resourceLabel = options.resourceLabel ?? '当前内容';
  const taskLabel = options.taskLabel ?? '当前操作';
  const retryActionLabel = options.retryActionLabel ?? '重试';
  const normalizedCode = normalizedError.code?.toUpperCase();
  const message = normalizedError.message.toLowerCase();
  const isRetryable =
    !normalizedError.status ||
    normalizedError.status >= 500 ||
    RETRYABLE_CODES.has(normalizedCode ?? '') ||
    message.includes('timeout') ||
    message.includes('network');

  if (normalizedError.code === 'TOKEN_EXPIRED' || normalizedError.status === 401) {
    return {
      kind: 'permission',
      title: '登录状态已失效',
      description: `系统暂时无法验证你的身份，因此${resourceLabel}没有成功打开。`,
      impact: `这会影响${taskLabel}，但不会改动你已经保存的数据。`,
      nextStep: '请重新登录后再试。',
    };
  }

  if (normalizedError.status === 403 || PERMISSION_CODES.has(normalizedCode ?? '')) {
    return {
      kind: 'permission',
      title: '当前账号暂无访问权限',
      description: `系统已拦截这次请求，因此${resourceLabel}不会在当前页面展示。`,
      impact: `这会影响${taskLabel}，但不会影响其他已经授权的页面。`,
      nextStep: options.permissionNextStep ?? '请切换到有权限的账号，或联系老师 / 管理员开通权限后再试。',
    };
  }

  if (isRetryable) {
    return {
      kind: 'retry',
      title: '服务暂时没有响应',
      description: `系统没有完成这次请求，因此${resourceLabel}暂时无法更新。`,
      impact: `这会影响${taskLabel}，但不会覆盖你已经看到的数据。`,
      nextStep: `请点击“${retryActionLabel}”；如果仍然失败，稍后再回来查看。`,
    };
  }

  return {
    kind: 'error',
    title: `${resourceLabel}暂时没有加载成功`,
    description: `系统已收到请求，但返回结果暂时无法正常展示。`,
    impact: `这会影响${taskLabel}，但不会影响当前页面其他已加载的内容。`,
    nextStep: '请稍后再试；如果问题持续出现，可返回上一层后重新进入。',
  };
}
