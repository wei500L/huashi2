import type { TeacherWorkspaceOverviewVO, TeacherWorkspaceSummaryVO } from '@/lib/contracts';

export type WorkspaceFocusItem = {
  id: string;
  title: string;
  description: string;
  to: string;
  actionLabel: string;
  tone: 'action' | 'attention' | 'stable';
};

export type WorkspaceEmptyStateConfig = {
  title: string;
  description: string;
  actionLabel: string;
  to: string;
};

type WorkspaceSection = 'classes' | 'drafts' | 'interventions' | 'lexicalLists';

export function buildWorkspaceLink(
  pathname: string,
  params: Record<string, string | number | null | undefined>
): string {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') {
      return;
    }
    searchParams.set(key, String(value));
  });
  const search = searchParams.toString();
  return search ? `${pathname}?${search}` : pathname;
}

function getSummary(overview?: TeacherWorkspaceOverviewVO | null): TeacherWorkspaceSummaryVO | null {
  return overview?.summary || null;
}

export function buildTeacherWorkspaceFocusItems(
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceFocusItem[] {
  const summary = getSummary(overview);
  if (!summary) {
    return [];
  }

  const items: WorkspaceFocusItem[] = [];
  const firstDraft = overview?.draftTemplates[0];
  const firstIntervention = overview?.pendingInterventions[0];
  const firstLexicalList = overview?.recentLexicalLists[0];

  if (summary.pendingInterventionCount > 0) {
    items.push({
      id: 'interventions',
      title: '处理待跟进干预',
      description: `还有 ${summary.pendingInterventionCount} 条干预未闭环，优先把排期和完成备注补齐。`,
      to: buildWorkspaceLink('/teacher/interventions', {
        view: 'pending',
        focusId: firstIntervention?.id,
        source: 'workspace',
      }),
      actionLabel: '进入干预工作台',
      tone: 'attention',
    });
  }

  if (summary.draftTemplateCount <= 0) {
    items.push({
      id: 'drafts-empty',
      title: '创建第一份模板草稿',
      description: '模板仍然是教师工作流的起点，先把诊断草稿建起来，后续才能串联词对和班级执行。',
      to: buildWorkspaceLink('/teacher/diagnosis-templates', {
        intent: 'create-draft',
        source: 'workspace-onboarding',
      }),
      actionLabel: '新建模板草稿',
      tone: 'action',
    });
  } else {
    items.push({
      id: 'drafts-existing',
      title: '整理待发布草稿',
      description: `当前有 ${summary.draftTemplateCount} 份草稿，优先把最接近发布的一份推进到预览与发布。`,
      to: buildWorkspaceLink(
        firstDraft ? `/teacher/diagnosis-template-drafts/${firstDraft.draftId}` : '/teacher/diagnosis-templates',
        {
          step: firstDraft ? 4 : undefined,
          source: 'workspace',
        }
      ),
      actionLabel: '查看模板草稿',
      tone: 'action',
    });
  }

  if (summary.pendingImportBatchCount > 0) {
    items.push({
      id: 'imports',
      title: '收敛导入批次',
      description: `还有 ${summary.pendingImportBatchCount} 个导入批次未处理完成，建议先清理草稿或重试导入。`,
      to: buildWorkspaceLink('/teacher/lexical-pairs/imports', {
        view: 'pending',
        source: 'workspace',
      }),
      actionLabel: '进入导入中心',
      tone: 'attention',
    });
  }

  if (summary.lexicalListCount <= 0) {
    items.push({
      id: 'lists-empty',
      title: '补出第一份词表',
      description: '词表是把词对沉淀成可复用教学资产的关键节点，至少需要一份基础词表承接模板和训练。',
      to: buildWorkspaceLink('/teacher/lexical-lists', {
        intent: 'create-list',
        source: 'workspace-onboarding',
      }),
      actionLabel: '创建词表',
      tone: 'action',
    });
  } else {
    items.push({
      id: 'lists-existing',
      title: '维护词表资产',
      description: `当前已沉淀 ${summary.lexicalListCount} 份词表，继续清理排序和条目命名可以降低后续教学配置成本。`,
      to: buildWorkspaceLink('/teacher/lexical-lists', {
        listId: firstLexicalList?.id,
        source: 'workspace',
      }),
      actionLabel: '查看词表',
      tone: 'stable',
    });
  }

  if (summary.classCount <= 0) {
    items.push({
      id: 'classes-empty',
      title: '补齐班级与学生',
      description: '还没有可访问班级，教师工作台会缺少真实教学上下文，建议先确认班级和学生关联。',
      to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      actionLabel: '查看班级与学生',
      tone: 'attention',
    });
  }

  return items.slice(0, 4);
}

export function buildTeacherWorkspaceOnboardingItems(
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceFocusItem[] {
  const summary = getSummary(overview);
  if (!summary) {
    return [];
  }

  if (summary.classCount <= 0) {
    return [
      {
        id: 'setup-classes',
        title: '先补齐班级与学生',
        description: '当前没有班级上下文，后续草稿、干预和学生跟进都很难形成真实教学闭环。优先确认班级与学生关联，再回来推进内容资产。',
        actionLabel: '进入班级与学生',
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
        tone: 'attention',
      },
    ];
  }

  const items: WorkspaceFocusItem[] = [];
  const firstClass = overview?.recentClasses[0];
  const firstDraft = overview?.draftTemplates[0];

  if (summary.draftTemplateCount <= 0) {
    items.push({
      id: 'setup-draft',
      title: '创建第一份模板草稿',
      description: '先用四步编辑器搭起一份可发布草稿，后续词对和词表才能被接进诊断与训练链路。',
      actionLabel: '新建模板草稿',
      to: buildWorkspaceLink('/teacher/diagnosis-templates', {
        intent: 'create-draft',
        source: 'workspace-onboarding',
      }),
      tone: 'action',
    });
  } else if (summary.lexicalListCount <= 0) {
    items.push({
      id: 'setup-list',
      title: '创建第一份词表',
      description: '已经有草稿后，下一步建议补一份基础词表，把词对沉淀成可复用资产，降低后续配置成本。',
      actionLabel: '创建词表',
      to: buildWorkspaceLink('/teacher/lexical-lists', {
        intent: 'create-list',
        source: 'workspace-onboarding',
      }),
      tone: 'action',
    });
  }

  if (summary.pendingImportBatchCount > 0) {
    items.push({
      id: 'triage-imports',
      title: '处理待确认导入批次',
      description: `还有 ${summary.pendingImportBatchCount} 个导入批次停留在解析或草稿阶段，先把可导入内容收口，避免词对资产长期堆积。`,
      actionLabel: '进入导入中心',
      to: buildWorkspaceLink('/teacher/lexical-pairs/imports', {
        view: 'pending',
        source: 'workspace-onboarding',
      }),
      tone: 'attention',
    });
  }

  if (summary.pendingInterventionCount > 0) {
    items.push({
      id: 'triage-interventions',
      title: '处理待跟进干预',
      description: `还有 ${summary.pendingInterventionCount} 条干预待推进，建议优先补齐排期、课堂执行方式和完成备注。`,
      actionLabel: '进入干预工作台',
      to: buildWorkspaceLink('/teacher/interventions', {
        view: 'pending',
        source: 'workspace-onboarding',
      }),
      tone: 'attention',
    });
  }

  if (!items.length) {
    const steadyStatePath = firstDraft
      ? `/teacher/diagnosis-template-drafts/${firstDraft.draftId}`
      : firstClass
        ? `/teacher/classes/${firstClass.classId}`
        : '/teacher/classes';
    items.push({
      id: 'steady-state',
      title: '从最近工作继续推进',
      description: firstDraft
        ? '基础资产已经齐备，建议优先完成最近更新的草稿预览与发布，或者回看最近活跃班级继续跟进学生。'
        : '基础资产已经齐备，建议从最近活跃班级继续回看高风险学生和教学动作。',
      actionLabel: firstDraft ? '打开最近草稿' : '查看最近班级',
      to: buildWorkspaceLink(steadyStatePath, {
        step: firstDraft ? 4 : undefined,
        source: 'workspace',
      }),
      tone: 'stable',
    });
  }

  return items.slice(0, 3);
}

export function buildTeacherWorkspaceEmptyState(
  section: WorkspaceSection,
  overview?: TeacherWorkspaceOverviewVO | null
): WorkspaceEmptyStateConfig {
  const summary = getSummary(overview);
  const firstClass = overview?.recentClasses[0];

  if (!summary) {
    return {
      title: '暂无工作台数据',
      description: '教师工作台正在等待数据返回。稍后重试，或先回到班级与学生页确认当前账号权限。',
      actionLabel: '查看班级与学生',
      to: buildWorkspaceLink('/teacher/classes', { source: 'workspace' }),
    };
  }

  if (section === 'classes') {
    if (summary.classCount <= 0) {
      return {
        title: '先补齐班级与学生',
        description: '没有班级时，教师工作台缺少真实教学上下文。先确认班级与学生关系，再回来查看班级动态。',
        actionLabel: '进入班级与学生',
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      };
    }
    return {
      title: '还没有近期班级动态',
      description: '当前没有最近活跃班级卡片，建议直接查看班级与学生列表，继续下钻到班级详情和学生分析。',
      actionLabel: '查看班级与学生',
      to: buildWorkspaceLink('/teacher/classes', { source: 'workspace' }),
    };
  }

  if (section === 'drafts') {
    if (summary.classCount <= 0) {
      return {
        title: '先补教学上下文，再建草稿',
        description: '建议先补齐班级与学生，再创建第一份模板草稿。这样草稿、干预和班级执行之间的关系会更清晰。',
        actionLabel: '进入班级与学生',
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      };
    }
    return {
      title: '创建第一份模板草稿',
      description: '当前还没有草稿。先用四步编辑器创建一份草稿，后续词对、词表和发布流程才能真正接入。',
      actionLabel: '新建模板草稿',
      to: buildWorkspaceLink('/teacher/diagnosis-templates', {
        intent: 'create-draft',
        source: 'workspace-onboarding',
      }),
    };
  }

  if (section === 'interventions') {
    if (summary.classCount <= 0) {
      return {
        title: '先补班级上下文，再形成干预待办',
        description: '没有班级和学生时，系统很难生成可执行的教学干预。先补齐教学对象，再回来看干预工作流。',
        actionLabel: '进入班级与学生',
        to: buildWorkspaceLink('/teacher/classes', { source: 'workspace-onboarding' }),
      };
    }
    return {
      title: '当前没有待处理干预',
      description: '当前待办已经清空。建议回到最近活跃班级或学生详情，继续检查高风险学生是否需要新的跟进。',
      actionLabel: firstClass ? '打开最近班级' : '查看干预工作台',
      to: buildWorkspaceLink(
        firstClass ? `/teacher/classes/${firstClass.classId}` : '/teacher/interventions',
        { source: 'workspace' }
      ),
    };
  }

  if (summary.draftTemplateCount <= 0) {
    return {
      title: '先整理模板和词对，再沉淀词表',
      description: '词表更适合承接已经明确的模板和词对结构。先把草稿或词对准备好，再创建首个词表。',
      actionLabel: '进入模板与草稿',
      to: buildWorkspaceLink('/teacher/diagnosis-templates', {
        intent: 'create-draft',
        source: 'workspace-onboarding',
      }),
    };
  }

  return {
    title: '创建第一份词表',
    description: '已经具备模板草稿后，建议尽快沉淀至少一份词表，把词对资产变成可复用的教学配置。',
    actionLabel: '创建词表',
    to: buildWorkspaceLink('/teacher/lexical-lists', {
      intent: 'create-list',
      source: 'workspace-onboarding',
    }),
  };
}
