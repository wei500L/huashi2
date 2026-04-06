export function enqueueSerializedTask<T>(
  queueRef: { current: Promise<void> },
  task: () => Promise<T>
): Promise<T> {
  const queuedTask = queueRef.current.catch(() => undefined).then(task);
  queueRef.current = queuedTask.then(
    () => undefined,
    () => undefined
  );
  return queuedTask;
}
