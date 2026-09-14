type ChangeListener = () => void;

let pendingRequests = 0;
const listeners = new Set<ChangeListener>();

export function beginRequest(): void {
  pendingRequests += 1;
  emit();
}

export function endRequest(): void {
  pendingRequests = Math.max(0, pendingRequests - 1);
  emit();
}

export function getPendingRequests(): number {
  return pendingRequests;
}

export function subscribeLoading(listener: ChangeListener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function emit(): void {
  for (const listener of listeners) listener();
}