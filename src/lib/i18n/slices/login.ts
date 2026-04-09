import type { TranslationSlice } from '../resources';

export const loginSlice: TranslationSlice = {
  'zh-CN': {
    login: {
      badge: 'EF.Transfer',
      title: '英法词汇迁移教学工作台',
      subtitle: '登录后即可继续完成学生风险诊断、词汇内容整理和教学跟进，让备课与干预安排保持在同一条工作线上。',
      accountLogin: '账号登录',
      accountLoginTitle: '登录后继续你的教学工作',
      usernameLabel: '用户名或邮箱',
      usernamePlaceholder: '输入用户名或邮箱',
      passwordLabel: '密码',
      passwordPlaceholder: '输入密码',
      submit: '进入工作台',
      submitting: '正在登录...',
      sessionExpired: '登录态已失效，请重新登录。',
      validation: {
        usernameRequired: '请输入用户名或邮箱',
        passwordRequired: '请输入密码',
      },
      valuePillars: {
        diagnosis: {
          label: '诊断学生英法迁移风险',
          hint: '把诊断结果直接转成教学动作，不再停留在数据展示。',
        },
        content: {
          label: '组织可复用词汇内容',
          hint: '用模板、词对和词表沉淀教师资产，降低后续备课成本。',
        },
        interventions: {
          label: '跟进教学干预闭环',
          hint: '围绕待办、排期和完成备注推进真正可执行的教学跟进。',
        },
      },
    },
  },
  'en-US': {
    login: {
      badge: 'EF.Transfer',
      title: 'English-French lexical transfer teaching workspace',
      subtitle: 'Sign in to continue student risk diagnosis, lexical content planning, and teaching follow-through in one connected workflow.',
      accountLogin: 'Account Login',
      accountLoginTitle: 'Continue your teaching workflow',
      usernameLabel: 'Username or email',
      usernamePlaceholder: 'Enter username or email',
      passwordLabel: 'Password',
      passwordPlaceholder: 'Enter password',
      submit: 'Enter workspace',
      submitting: 'Signing in...',
      sessionExpired: 'Your session expired. Please sign in again.',
      validation: {
        usernameRequired: 'Enter a username or email',
        passwordRequired: 'Enter a password',
      },
      valuePillars: {
        diagnosis: {
          label: 'Diagnose English-French transfer risk',
          hint: 'Turn diagnosis output into concrete teaching moves instead of leaving it as a report.',
        },
        content: {
          label: 'Organize reusable lexical assets',
          hint: 'Use templates, lexical pairs, and lexical lists to reduce future prep costs.',
        },
        interventions: {
          label: 'Close the intervention loop',
          hint: 'Drive real follow-through with pending work, scheduling, and completion notes.',
        },
      },
    },
  },
};
