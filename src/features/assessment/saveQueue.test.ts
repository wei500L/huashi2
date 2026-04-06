import { describe, expect, it } from 'vitest';
import { enqueueSerializedTask } from './saveQueue';

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (error?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('enqueueSerializedTask', () => {
  it('runs tasks one at a time in call order', async () => {
    const queueRef = { current: Promise.resolve() };
    const firstTask = deferred<string>();
    const secondTask = deferred<string>();
    const events: string[] = [];

    const first = enqueueSerializedTask(queueRef, async () => {
      events.push('first:start');
      const value = await firstTask.promise;
      events.push(`first:${value}`);
      return value;
    });
    const second = enqueueSerializedTask(queueRef, async () => {
      events.push('second:start');
      const value = await secondTask.promise;
      events.push(`second:${value}`);
      return value;
    });

    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(events).toEqual(['first:start']);

    firstTask.resolve('done');
    await expect(first).resolves.toBe('done');
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(events).toEqual(['first:start', 'first:done', 'second:start']);

    secondTask.resolve('latest');
    await expect(second).resolves.toBe('latest');
    expect(events).toEqual(['first:start', 'first:done', 'second:start', 'second:latest']);
  });
});
