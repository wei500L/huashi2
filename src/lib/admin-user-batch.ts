import type { AdminUserBatchCreateItemRequest, AdminUserBatchRequest, Role } from './contracts';

const requiredHeaders = ['username', 'email', 'displayName', 'roles'] as const;
const supportedRoles: Role[] = ['ADMIN', 'TEACHER', 'STUDENT'];
const supportedCredentialModes = ['INVITE_LINK', 'MANUAL_PASSWORD'] as const;

export const adminUserBatchTemplateHeaders = [
  'username',
  'email',
  'displayName',
  'roles',
  'enabled',
  'credentialMode',
  'initialPassword',
] as const;

export function buildAdminUserBatchTemplate(): string {
  return [
    adminUserBatchTemplateHeaders.join(','),
    'student001,student001@example.com,学生 001,STUDENT,true,INVITE_LINK,',
    'teacher001,teacher001@example.com,教师 001,TEACHER|STUDENT,true,MANUAL_PASSWORD,Teacher@123456',
  ].join('\n');
}

export function buildBatchCreateRequestFromCsv(csvText: string): AdminUserBatchRequest {
  return {
    operation: 'IMPORT_CREATE',
    createItems: parseAdminUserCsv(csvText),
  };
}

export function buildBulkAccessUpdateRequest(userIds: number[], roles: Role[], enabled: boolean): AdminUserBatchRequest {
  return {
    operation: 'BULK_ACCESS_UPDATE',
    userIds,
    roles,
    enabled,
  };
}

export function parseAdminUserCsv(csvText: string): AdminUserBatchCreateItemRequest[] {
  const rows = parseCsvRows(csvText);
  if (rows.length < 2) {
    throw new Error('CSV 至少需要表头和一行数据。');
  }

  const headers = rows[0].map((cell) => normalizeHeader(cell));
  const headerIndex = new Map(headers.map((header, index) => [header, index]));
  const missingHeaders = requiredHeaders.filter((header) => !headerIndex.has(header));
  if (missingHeaders.length > 0) {
    throw new Error(`CSV 缺少必填列：${missingHeaders.join('、')}`);
  }

  const parsedItems: AdminUserBatchCreateItemRequest[] = [];
  rows.slice(1).forEach((row, index) => {
    const rowNumber = index + 2;
    if (row.every((cell) => !cell.trim())) {
      return;
    }

    const username = requireCell(row, rowNumber, headerIndex, 'username');
    const email = requireCell(row, rowNumber, headerIndex, 'email');
    const displayName = requireCell(row, rowNumber, headerIndex, 'displayName');
    const roles = parseRoleList(readCell(row, headerIndex, 'roles'), rowNumber);
    const enabled = parseEnabledValue(readCell(row, headerIndex, 'enabled'), rowNumber);
    const credentialMode = parseCredentialModeValue(readCell(row, headerIndex, 'credentialMode'), rowNumber);
    const initialPassword = readCell(row, headerIndex, 'initialPassword').trim();

    if (credentialMode === 'MANUAL_PASSWORD' && !initialPassword) {
      throw new Error(`第 ${rowNumber} 行缺少初始密码。`);
    }

    parsedItems.push({
      rowNumber,
      username,
      email,
      displayName,
      roles,
      enabled,
      credentialMode,
      initialPassword: initialPassword || undefined,
    });
  });

  if (!parsedItems.length) {
    throw new Error('CSV 中没有可导入的数据行。');
  }

  return parsedItems;
}

function parseCsvRows(csvText: string): string[][] {
  const normalizedText = csvText.replace(/^\uFEFF/, '');
  const rows: string[][] = [];
  let currentRow: string[] = [];
  let currentCell = '';
  let inQuotes = false;

  for (let index = 0; index < normalizedText.length; index += 1) {
    const char = normalizedText[index];
    const nextChar = normalizedText[index + 1];

    if (char === '"') {
      if (inQuotes && nextChar === '"') {
        currentCell += '"';
        index += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }

    if (!inQuotes && char === ',') {
      currentRow.push(currentCell);
      currentCell = '';
      continue;
    }

    if (!inQuotes && (char === '\n' || char === '\r')) {
      if (char === '\r' && nextChar === '\n') {
        index += 1;
      }
      currentRow.push(currentCell);
      rows.push(currentRow);
      currentRow = [];
      currentCell = '';
      continue;
    }

    currentCell += char;
  }

  if (currentCell || currentRow.length > 0) {
    currentRow.push(currentCell);
    rows.push(currentRow);
  }

  return rows.filter((row) => row.length > 1 || row.some((cell) => cell.trim().length > 0));
}

function normalizeHeader(value: string): string {
  return value.trim().replace(/^\uFEFF/, '');
}

function readCell(row: string[], headerIndex: Map<string, number>, header: string): string {
  const index = headerIndex.get(header);
  return index == null ? '' : row[index] || '';
}

function requireCell(row: string[], rowNumber: number, headerIndex: Map<string, number>, header: string): string {
  const value = readCell(row, headerIndex, header).trim();
  if (!value) {
    throw new Error(`第 ${rowNumber} 行的 ${header} 不能为空。`);
  }
  return value;
}

function parseRoleList(rawValue: string, rowNumber: number): Role[] {
  const roles = rawValue
    .split(/[|,;/，、]+/)
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean) as Role[];
  if (!roles.length) {
    throw new Error(`第 ${rowNumber} 行至少需要一个角色。`);
  }
  roles.forEach((role) => {
    if (!supportedRoles.includes(role)) {
      throw new Error(`第 ${rowNumber} 行包含不支持的角色：${role}`);
    }
  });
  return Array.from(new Set(roles));
}

function parseEnabledValue(rawValue: string, rowNumber: number): boolean {
  const normalized = rawValue.trim().toLowerCase();
  if (!normalized) {
    return true;
  }
  if (['true', '1', 'yes', 'enabled', '启用', '开启'].includes(normalized)) {
    return true;
  }
  if (['false', '0', 'no', 'disabled', '禁用', '关闭'].includes(normalized)) {
    return false;
  }
  throw new Error(`第 ${rowNumber} 行的 enabled 只能填写 true 或 false。`);
}

function parseCredentialModeValue(rawValue: string, rowNumber: number): 'INVITE_LINK' | 'MANUAL_PASSWORD' {
  const normalized = rawValue.trim().toUpperCase();
  if (!normalized) {
    return 'INVITE_LINK';
  }
  if (supportedCredentialModes.includes(normalized as 'INVITE_LINK' | 'MANUAL_PASSWORD')) {
    return normalized as 'INVITE_LINK' | 'MANUAL_PASSWORD';
  }
  throw new Error(`第 ${rowNumber} 行的 credentialMode 仅支持 INVITE_LINK 或 MANUAL_PASSWORD。`);
}
