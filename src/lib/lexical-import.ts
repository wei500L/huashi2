export const templateFieldLabels: Record<string, string> = {
  english_word: '英语词',
  french_word: '法语词',
  chinese_gloss: '中文释义',
  lexical_pair_type: '词对类型',
  semantic_overlap_score: '语义重合度',
  false_friend_risk: '负迁移风险',
  default_context_support: '默认语境支持',
  difficulty_level: '难度等级',
  notes: '备注',
  source: '来源',
  active: '启用状态',
  tags: '标签',
  knowledge_status: '知识状态',
  embedding_status: '向量状态',
  sense_english_definition: '英语义项',
  sense_french_definition: '法语义项',
  sense_chinese_definition: '中文义项',
  example_english: '英语例句',
  example_french: '法语例句',
  example_chinese: '中文译文',
  example_context_support: '例句语境支持',
};

export function fieldLabel(fieldName: string): string {
  return templateFieldLabels[fieldName] || fieldName;
}

export function translateImportMessage(message: string): string {
  const trimmed = message.trim();
  if (!trimmed) {
    return '导入失败，请检查文件内容。';
  }
  if (trimmed === 'CSV file must not be empty' || trimmed === 'Import file must not be empty') {
    return '上传文件不能为空。';
  }
  if (trimmed === 'Failed to read CSV file' || trimmed === 'Failed to read import file') {
    return '文件读取失败，请确认文件内容未损坏。';
  }
  if (trimmed === 'Unsupported import file type') {
    return '仅支持上传 CSV 或 XLSX 文件。';
  }
  if (trimmed === 'Import file must not exceed 50MB') {
    return '单个导入文件不能超过 50MB。';
  }
  if (trimmed === 'Duplicate lexical pair in CSV file') {
    return '同一份导入文件中出现了重复词对，请先去重。';
  }
  if (trimmed === 'Duplicate lexical pair in import batch') {
    return '当前导入批次中存在重复词对，请先修正或跳过其中一行。';
  }
  if (trimmed === 'Import batch is not editable right now') {
    return '该导入批次当前不可编辑，请等待后台处理完成。';
  }
  if (trimmed === 'Import batch is already processing') {
    return '该导入批次正在处理中，请稍后刷新状态。';
  }
  if (trimmed === 'Imported rows can no longer be edited') {
    return '已导入的行不能再编辑。';
  }
  if (trimmed === 'No ready import rows to commit') {
    return '当前没有可提交导入的有效行。';
  }
  if (trimmed === 'Import file must contain a header row') {
    return '文件缺少表头，无法创建导入草稿。';
  }
  if (trimmed === 'Lexical pair already exists') {
    return '系统中已存在相同英语词 / 法语词的词对。';
  }
  if (trimmed === 'Each sense must have at least one definition') {
    return '每个义项至少需要填写一种释义。';
  }
  if (trimmed === 'Each example must contain at least one sentence or translation') {
    return '每条例句至少需要填写英文、法文或中文中的一项。';
  }
  if (trimmed === 'Sense definition is required when example columns are provided') {
    return '填写例句时，必须同时填写对应义项释义。';
  }
  if (trimmed === 'active must be true or false') {
    return '`active` 列只能填写 true 或 false。';
  }
  if (trimmed.startsWith('Missing required CSV headers: ')) {
    const headers = trimmed
      .replace('Missing required CSV headers: ', '')
      .split(',')
      .map((item) => fieldLabel(item.trim()))
      .filter(Boolean);
    return `文件缺少必填列：${headers.join('、')}。`;
  }
  if (trimmed.endsWith(' is required')) {
    return `${fieldLabel(trimmed.replace(/ is required$/, ''))}不能为空。`;
  }
  if (trimmed.endsWith(' must be between 0 and 1')) {
    return `${fieldLabel(trimmed.replace(/ must be between 0 and 1$/, ''))}必须填写 0 到 1 之间的小数。`;
  }
  if (trimmed.endsWith(' must be between 1 and 5')) {
    return `${fieldLabel(trimmed.replace(/ must be between 1 and 5$/, ''))}必须填写 1 到 5 之间的整数。`;
  }
  if (trimmed.endsWith(' must be a decimal number')) {
    return `${fieldLabel(trimmed.replace(/ must be a decimal number$/, ''))}必须是小数。`;
  }
  if (trimmed.endsWith(' must be an integer')) {
    return `${fieldLabel(trimmed.replace(/ must be an integer$/, ''))}必须是整数。`;
  }
  if (trimmed.startsWith('Unsupported import batch status:') || trimmed.startsWith('Unsupported import row status:')) {
    return '筛选状态不受支持，请刷新页面后重试。';
  }
  return trimmed;
}

export function formatFileSize(fileSizeBytes?: number | null): string {
  if (!fileSizeBytes || fileSizeBytes <= 0) {
    return '--';
  }
  const mb = fileSizeBytes / (1024 * 1024);
  if (mb >= 1) {
    return `${mb.toFixed(1)} MB`;
  }
  return `${(fileSizeBytes / 1024).toFixed(1)} KB`;
}
