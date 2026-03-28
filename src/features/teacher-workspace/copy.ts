import type { TeacherWorkspaceSummaryVO } from '@/lib/contracts';

export type WorkspaceFocusItem = {
  id: string;
  title: string;
  description: string;
  to: string;
  actionLabel: string;
  tone: 'action' | 'attention' | 'stable';
};

export function buildTeacherWorkspaceFocusItems(
  summary?: TeacherWorkspaceSummaryVO | null
): WorkspaceFocusItem[] {
  if (!summary) {
    return [];
  }

  const items: WorkspaceFocusItem[] = [];

  if (summary.pendingInterventionCount > 0) {
    items.push({
      id: 'interventions',
      title: '处理待跟进干预',
      description: `还有 ${summary.pendingInterventionCount} 条干预未闭环，优先把排期和完成备注补齐。`,
      to: '/teacher/interventions',
      actionLabel: '进入干预工作台',
      tone: 'attention',
    });
  }

  if (summary.draftTemplateCount <= 0) {
    items.push({
      id: 'drafts-empty',
      title: '创建第一份模板草稿',
      description: '模板仍然是教师工作流的起点，先把诊断草稿建起来，后续才能串联词对和班级执行。',
      to: '/teacher/diagnosis-templates',
      actionLabel: '新建模板草稿',
      tone: 'action',
    });
  } else {
    items.push({
      id: 'drafts-existing',
      title: '整理待发布草稿',
      description: `当前有 ${summary.draftTemplateCount} 份草稿，优先把最接近发布的一份推进到预览与发布。`,
      to: '/teacher/diagnosis-templates',
      actionLabel: '查看模板草稿',
      tone: 'action',
    });
  }

  if (summary.pendingImportBatchCount > 0) {
    items.push({
      id: 'imports',
      title: '收敛导入批次',
      description: `还有 ${summary.pendingImportBatchCount} 个导入批次未处理完成，建议先清理草稿或重试导入。`,
      to: '/teacher/lexical-pairs/imports',
      actionLabel: '进入导入中心',
      tone: 'attention',
    });
  }

  if (summary.lexicalListCount <= 0) {
    items.push({
      id: 'lists-empty',
      title: '补出第一份词表',
      description: '词表是把词对沉淀成可复用教学资产的关键节点，至少需要一份基础词表承接模板和训练。',
      to: '/teacher/lexical-lists',
      actionLabel: '创建词表',
      tone: 'action',
    });
  } else {
    items.push({
      id: 'lists-existing',
      title: '维护词表资产',
      description: `当前已沉淀 ${summary.lexicalListCount} 份词表，继续清理排序和条目命名可以降低后续教学配置成本。`,
      to: '/teacher/lexical-lists',
      actionLabel: '查看词表',
      tone: 'stable',
    });
  }

  if (summary.classCount <= 0) {
    items.push({
      id: 'classes-empty',
      title: '补齐班级与学生',
      description: '还没有可访问班级，教师工作台会缺少真实教学上下文，建议先确认班级和学生关联。',
      to: '/teacher/classes',
      actionLabel: '查看班级与学生',
      tone: 'attention',
    });
  }

  return items.slice(0, 4);
}
