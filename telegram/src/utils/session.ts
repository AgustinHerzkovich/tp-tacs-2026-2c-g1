interface SessionState {
  userId?: number;
  activeMenu?: string;
}

const sessions = new Map<number, SessionState>();

export function getSession(chatId: number): SessionState {
  const session = sessions.get(chatId);
  if (session) return session;
  const created: SessionState = {};
  sessions.set(chatId, created);
  return created;
}

export function getUserId(chatId: number): number | undefined {
  return sessions.get(chatId)?.userId;
}

export function setUserId(chatId: number, userId: number | undefined): void {
  const session = getSession(chatId);
  session.userId = userId;
}

export function getActiveMenu(chatId: number): string | undefined {
  return sessions.get(chatId)?.activeMenu;
}

export function setActiveMenu(chatId: number, menu?: string): void {
  const session = getSession(chatId);
  session.activeMenu = menu;
}