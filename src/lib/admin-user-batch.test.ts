import { describe, expect, it } from 'vitest';
import {
  buildAdminUserBatchTemplate,
  buildBatchCreateRequestFromCsv,
  buildBulkAccessUpdateRequest,
  parseAdminUserCsv,
} from './admin-user-batch';

describe('admin-user-batch', () => {
  it('parses batch user CSV rows into create items', () => {
    const rows = parseAdminUserCsv(`
username,email,displayName,roles,enabled,credentialMode,initialPassword
student001,student001@example.com,学生 001,STUDENT,true,INVITE_LINK,
teacher001,teacher001@example.com,教师 001,"TEACHER,STUDENT",false,MANUAL_PASSWORD,Teacher@123456
`.trim());

    expect(rows).toHaveLength(2);
    expect(rows[0]).toMatchObject({
      rowNumber: 2,
      username: 'student001',
      roles: ['STUDENT'],
      enabled: true,
      credentialMode: 'INVITE_LINK',
    });
    expect(rows[1]).toMatchObject({
      rowNumber: 3,
      username: 'teacher001',
      roles: ['TEACHER', 'STUDENT'],
      enabled: false,
      credentialMode: 'MANUAL_PASSWORD',
      initialPassword: 'Teacher@123456',
    });
  });

  it('builds batch requests for CSV imports and bulk access updates', () => {
    expect(buildBatchCreateRequestFromCsv(`
username,email,displayName,roles
student002,student002@example.com,学生 002,STUDENT
`.trim())).toMatchObject({
      operation: 'IMPORT_CREATE',
      createItems: [
        {
          username: 'student002',
          roles: ['STUDENT'],
        },
      ],
    });

    expect(buildBulkAccessUpdateRequest([1, 2], ['TEACHER'], false)).toEqual({
      operation: 'BULK_ACCESS_UPDATE',
      userIds: [1, 2],
      roles: ['TEACHER'],
      enabled: false,
    });
  });

  it('rejects invalid CSV rows and exposes a reusable template', () => {
    expect(buildAdminUserBatchTemplate()).toContain('username,email,displayName,roles,enabled,credentialMode,initialPassword');

    expect(() =>
      parseAdminUserCsv(`
username,email,displayName,roles,credentialMode
teacher003,teacher003@example.com,教师 003,TEACHER,MANUAL_PASSWORD
`.trim())
    ).toThrow('第 2 行缺少初始密码。');
  });
});
