import { normalizeApiError } from './api';
import i18n from './i18n';
import { DEFAULT_LOCALE, type SupportedLocale } from './locale';

export type AsyncStateKind =
  | 'loading'
  | 'empty'
  | 'permission'
  | 'error'
  | 'retry'
  | 'saving'
  | 'saved'
  | 'success'
  | 'destructive';

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

function getActiveLocale(): SupportedLocale {
  const locale = i18n.resolvedLanguage ?? i18n.language;
  return locale === 'en-US' ? 'en-US' : DEFAULT_LOCALE;
}

const COPY = {
  'zh-CN': {
    defaultResourceLabel: '当前内容',
    defaultTaskLabel: '当前操作',
    defaultRetryActionLabel: '重试',
    expiredTitle: '登录状态已失效',
    expiredDescription: (resourceLabel: string) => `系统暂时无法验证你的身份，因此${resourceLabel}没有成功打开。`,
    expiredImpact: (taskLabel: string) => `这会影响${taskLabel}，但不会改动你已经保存的数据。`,
    expiredNextStep: '请重新登录后再试。',
    forbiddenTitle: '当前账号暂无访问权限',
    forbiddenDescription: (resourceLabel: string) => `系统已拦截这次请求，因此${resourceLabel}不会在当前页面展示。`,
    forbiddenImpact: (taskLabel: string) => `这会影响${taskLabel}，但不会影响其他已经授权的页面。`,
    forbiddenNextStep: '请切换到有权限的账号，或联系老师 / 管理员开通权限后再试。',
    retryTitle: '服务暂时没有响应',
    retryDescription: (resourceLabel: string) => `系统没有完成这次请求，因此${resourceLabel}暂时无法更新。`,
    retryImpact: (taskLabel: string) => `这会影响${taskLabel}，但不会覆盖你已经看到的数据。`,
    retryNextStep: (retryActionLabel: string) => `请点击“${retryActionLabel}”；如果仍然失败，稍后再回来查看。`,
    errorTitle: (resourceLabel: string) => `${resourceLabel}暂时没有加载成功`,
    errorDescription: '系统已收到请求，但返回结果暂时无法正常展示。',
    errorImpact: (taskLabel: string) => `这会影响${taskLabel}，但不会影响当前页面其他已加载的内容。`,
    errorNextStep: '请稍后再试；如果问题持续出现，可返回上一层后重新进入。',
  },
  'en-US': {
    defaultResourceLabel: 'this content',
    defaultTaskLabel: 'this action',
    defaultRetryActionLabel: 'Retry',
    expiredTitle: 'Your sign-in session expired',
    expiredDescription: (resourceLabel: string) => `The system could not verify your identity, so ${resourceLabel} could not be opened.`,
    expiredImpact: (taskLabel: string) => `This affects ${taskLabel}, but it does not change any data you already saved.`,
    expiredNextStep: 'Please sign in again and retry.',
    forbiddenTitle: 'This account cannot access the requested content',
    forbiddenDescription: (resourceLabel: string) => `${resourceLabel} is blocked from this page because the request was denied.`,
    forbiddenImpact: (taskLabel: string) => `This affects ${taskLabel}, but other authorized pages remain available.`,
    forbiddenNextStep: 'Switch to an account with access, or ask a teacher or administrator to enable the permission.',
    retryTitle: 'The service is temporarily unavailable',
    retryDescription: (resourceLabel: string) => `The request did not complete, so ${resourceLabel} cannot be refreshed yet.`,
    retryImpact: (taskLabel: string) => `This affects ${taskLabel}, but it does not overwrite data already shown on the page.`,
    retryNextStep: (retryActionLabel: string) => `Select "${retryActionLabel}". If it still fails, come back and try again later.`,
    errorTitle: (resourceLabel: string) => `${resourceLabel} could not be loaded`,
    errorDescription: 'The request reached the system, but the response cannot be displayed right now.',
    errorImpact: (taskLabel: string) => `This affects ${taskLabel}, but it does not affect other content already loaded on the page.`,
    errorNextStep: 'Try again later. If the issue persists, go back one level and reopen the page.',
  },
} as const;

export function getProductizedErrorState(
  error: unknown,
  options: ProductizedErrorOptions = {}
): ProductizedErrorState {
  const locale = getActiveLocale();
  const copy = COPY[locale];
  const normalizedError = normalizeApiError(error);
  const resourceLabel = options.resourceLabel ?? copy.defaultResourceLabel;
  const taskLabel = options.taskLabel ?? copy.defaultTaskLabel;
  const retryActionLabel = options.retryActionLabel ?? copy.defaultRetryActionLabel;
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
      title: copy.expiredTitle,
      description: copy.expiredDescription(resourceLabel),
      impact: copy.expiredImpact(taskLabel),
      nextStep: copy.expiredNextStep,
    };
  }

  if (normalizedError.status === 403 || PERMISSION_CODES.has(normalizedCode ?? '')) {
    return {
      kind: 'permission',
      title: copy.forbiddenTitle,
      description: copy.forbiddenDescription(resourceLabel),
      impact: copy.forbiddenImpact(taskLabel),
      nextStep: options.permissionNextStep ?? copy.forbiddenNextStep,
    };
  }

  if (isRetryable) {
    return {
      kind: 'retry',
      title: copy.retryTitle,
      description: copy.retryDescription(resourceLabel),
      impact: copy.retryImpact(taskLabel),
      nextStep: copy.retryNextStep(retryActionLabel),
    };
  }

  return {
    kind: 'error',
    title: copy.errorTitle(resourceLabel),
    description: copy.errorDescription,
    impact: copy.errorImpact(taskLabel),
    nextStep: copy.errorNextStep,
  };
}
