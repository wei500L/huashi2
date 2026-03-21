import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';

const storage = new Map<string, string>();

const storageMock = {
  get length() {
    return storage.size;
  },
  clear() {
    storage.clear();
  },
  getItem(key: string) {
    return storage.has(key) ? storage.get(key)! : null;
  },
  key(index: number) {
    return Array.from(storage.keys())[index] ?? null;
  },
  removeItem(key: string) {
    storage.delete(key);
  },
  setItem(key: string, value: string) {
    storage.set(key, String(value));
  },
} satisfies Storage;

vi.stubGlobal('localStorage', storageMock);
Object.defineProperty(window, 'localStorage', {
  configurable: true,
  value: storageMock,
});
