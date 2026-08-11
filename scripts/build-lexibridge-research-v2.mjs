import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';

const sourcePath = new URL('../app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V1.json', import.meta.url);
const targetPath = new URL('../app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V2.json', import.meta.url);
const seed = JSON.parse(readFileSync(sourcePath, 'utf8'));

seed.packageCode = 'LEXIBRIDGE_RESEARCH_V2';
seed.questionnaire.questionnaireCode = 'LEXIBRIDGE_RESEARCH_V2';
seed.questionnaire.title = 'Lexi-bridge 英法词汇认知迁移研究问卷 V2';
seed.questionnaire.versionNo = 2;
seed.questionnaire.status = 'REVIEW_REQUIRED';
seed.source.questionnaireDocx = 'Lexi-bridge 大创测试题目编写2.docx';
seed.source.analysisDocx = 'Lexi-bridge 大创题目答案分析2.docx';

const sectionInstructions = new Map([
  ['P1A', '选择法语单词对应的正确中文含义。'],
  ['P1B_SYNONYM', '选择与短语中加粗下划线词意思相同的选项。'],
  ['P1B_ANTONYM', '选择与短语中加粗下划线词意思相反的选项。'],
  ['P2', '根据句子选择加粗下划线词的同义解释。'],
  ['P3', '阅读完整短文并选择对应空格的最佳答案。'],
  ['P4T1', '阅读完整短文并选择最佳答案。'],
  ['P4T2', '阅读完整短文并选择最佳答案。'],
  ['P4T3', '阅读完整短文并判断正误；选择错误时必须填写原因。'],
]);

const originalP1B = seed.sections.find((section) => section.sectionCode === 'P1B');
const replacementSections = [
  {
    ...originalP1B,
    sectionCode: 'P1B_SYNONYM',
    title: 'Partie 1 / Section B · 同义项',
    scoredItemCount: 5,
    description: sectionInstructions.get('P1B_SYNONYM'),
  },
  {
    ...originalP1B,
    sectionCode: 'P1B_ANTONYM',
    title: 'Partie 1 / Section B · 反义项',
    sortOrder: originalP1B.sortOrder + 1,
    scoredItemCount: 5,
    description: sectionInstructions.get('P1B_ANTONYM'),
  },
];

seed.sections = seed.sections.flatMap((section) => {
  if (section.sectionCode === 'P1B') return replacementSections;
  const shifted = section.sortOrder > originalP1B.sortOrder
    ? { ...section, sortOrder: section.sortOrder + 1 }
    : { ...section };
  if (sectionInstructions.has(shifted.sectionCode)) {
    shifted.description = sectionInstructions.get(shifted.sectionCode);
  }
  return [shifted];
});

const instruction = seed.items.find((item) => item.itemCode === 'BASIC-INSTRUCTION');
instruction.stemText = '亲爱的同学：\n您好！欢迎参与本次法语词汇与阅读理解能力研究。姓名和联系方式仅用于研究参与确认与必要联络；资料会加密保存，并与正式答题、评分、自动分析和普通结果页面隔离，仅限问卷所有者或管理员在授权场景访问。正式测试共 60 题，约 40 分钟，资料填写时间不计入测试。请勿查阅词典或与他人交流，独立完成作答。';

const gaokaoIndex = seed.items.findIndex((item) => item.itemCode === 'BASIC-GAOKAO-ENGLISH');
const englishMajor = {
  itemCode: 'BASIC-ENGLISH-MAJOR',
  sectionCode: 'BASIC_INFO',
  sortOrder: 6,
  questionType: 'SINGLE_CHOICE',
  stemText: '您的英语专业背景：',
  promptText: '请选择英语专业或非英语专业。',
  options: [
    { key: 'ENGLISH_MAJOR', label: '英语专业' },
    { key: 'NON_ENGLISH_MAJOR', label: '非英语专业' },
  ],
  correctAnswers: [],
  explanationText: null,
  optionExplanations: {},
  requiredAnswer: true,
  scored: false,
  score: 0,
  weight: 1,
  transferCategory: null,
  contextLevel: null,
  constructCode: null,
  targetWord: null,
  displayCondition: null,
  presentation: null,
  sourceReference: 'v2:participant-profile:english-major',
};
seed.items.splice(gaokaoIndex + 1, 0, englishMajor);

for (const item of seed.items) {
  if (item.sectionCode === 'BASIC_INFO' && item.itemCode !== 'BASIC-ENGLISH-MAJOR' && item.sortOrder > 5) {
    item.sortOrder += 1;
  }
  if (item.itemCode === 'BASIC-TEM4' || item.itemCode === 'BASIC-TEM8') {
    item.displayCondition = { fieldCode: 'BASIC-ENGLISH-MAJOR', operator: 'EQ', value: 'ENGLISH_MAJOR' };
  }
  if (item.itemCode.startsWith('P1B-')) {
    item.sectionCode = Number(item.itemCode.slice(-2)) <= 5 ? 'P1B_SYNONYM' : 'P1B_ANTONYM';
  }
}

const emphasisTargets = new Map([
  ['P1B-01', 'entreprise'], ['P1B-02', 'courses'], ['P1B-03', 'boisson'], ['P1B-04', 'blesser'],
  ['P1B-05', 'accomplir'], ['P1B-06', 'important'], ['P1B-07', 'ennuyeux'], ['P1B-08', 'sensible'],
  ['P1B-09', 'propre'], ['P1B-10', 'exacte'], ['P2-01', 'location'], ['P2-02', 'commerce'],
  ['P2-03', 'peinture'], ['P2-04', 'reporte'], ['P2-05', 'annoncent'], ['P2-06', 'capturer'],
  ['P2-07', 'se dépêcher'], ['P2-08', 'physicienne'], ['P2-09', 'sale'], ['P2-10', 'constituer'],
]);
for (const item of seed.items) {
  const target = emphasisTargets.get(item.itemCode);
  item.presentation = target
    ? { emphasis: [{ text: target, bold: true, underline: true, occurrence: 1 }] }
    : null;
  item.contentHash = createHash('sha256')
    .update(JSON.stringify({ packageCode: seed.packageCode, item }))
    .digest('hex');
}

seed.source.questionnaireSha256 = createHash('sha256')
  .update(JSON.stringify({ packageCode: seed.packageCode, sections: seed.sections, items: seed.items }))
  .digest('hex');

writeFileSync(targetPath, `${JSON.stringify(seed, null, 2)}\n`);
