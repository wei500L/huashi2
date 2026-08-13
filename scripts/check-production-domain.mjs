#!/usr/bin/env node

import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const ROOT = process.cwd();
const EXPECTED_DOMAIN = 'huashi.mnari.cn';
const LEGACY_DOMAIN = ['huashi', 'qsfw', 'eu', 'cc'].join('.');
const SKIPPED_DIRECTORIES = new Set([
  '.git',
  '.idea',
  '.vscode',
  'dist',
  'node_modules',
  'qa-output',
  'target',
]);
const TEXT_EXTENSIONS = new Set([
  '',
  '.conf',
  '.env',
  '.example',
  '.java',
  '.json',
  '.md',
  '.mjs',
  '.ps1',
  '.sql',
  '.ts',
  '.tsx',
  '.yaml',
  '.yml',
]);

async function collectTextFiles(directory) {
  const files = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    if (entry.isDirectory() && SKIPPED_DIRECTORIES.has(entry.name)) continue;
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...await collectTextFiles(absolutePath));
    } else if (entry.isFile() && TEXT_EXTENSIONS.has(path.extname(entry.name))) {
      files.push(absolutePath);
    }
  }
  return files;
}

const files = await collectTextFiles(ROOT);
const staleReferences = [];
for (const file of files) {
  const content = await readFile(file, 'utf8');
  if (content.includes(LEGACY_DOMAIN)) {
    staleReferences.push(path.relative(ROOT, file));
  }
}

const nginxConfigPath = path.join(ROOT, 'deploy/nginx', `${EXPECTED_DOMAIN}.conf`);
const nginxConfig = await readFile(nginxConfigPath, 'utf8');
const envExample = await readFile(path.join(ROOT, 'deploy/.env.example'), 'utf8');
const errors = [];
if (staleReferences.length > 0) {
  errors.push(`Legacy production domain remains in: ${staleReferences.join(', ')}`);
}
if (!nginxConfig.includes(`server_name ${EXPECTED_DOMAIN};`)) {
  errors.push(`Nginx config does not declare ${EXPECTED_DOMAIN}`);
}
if (!envExample.includes(`PUBLIC_DOMAIN=${EXPECTED_DOMAIN}`)) {
  errors.push(`deploy/.env.example does not default PUBLIC_DOMAIN to ${EXPECTED_DOMAIN}`);
}

if (errors.length > 0) {
  errors.forEach((error) => globalThis.console.error(error));
  process.exitCode = 1;
} else {
  globalThis.console.log(`Production domain configuration is consistent: ${EXPECTED_DOMAIN}`);
}
