export interface SessionUser {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  role: "ROLE_CUSTOMER" | "ROLE_SELLER" | "ROLE_ADMIN";
  status: "ACTIVE" | "INACTIVE" | "BLOCKED";
  avatarUrl?: string;
}

export interface AuthSession {
  user: SessionUser;
  accessToken: string;
  refreshToken: string;
  rememberMe: boolean;
}

const SESSION_KEY = "helishop-session-v3";
const listeners = new Set<(session: AuthSession | null) => void>();

function parseSession(value: string | null): AuthSession | null {
  try {
    const session = value ? JSON.parse(value) : null;
    return session?.user?.id && session.accessToken && session.refreshToken ? session : null;
  } catch {
    return null;
  }
}

function removeLegacySessions() {
  for (const storage of [localStorage, sessionStorage]) {
    for (const key of ["accessToken", "refreshToken", "helishop-auth-storage"]) storage.removeItem(key);
  }
}

export function readAuthSession(): AuthSession | null {
  const current = parseSession(sessionStorage.getItem(SESSION_KEY)) || parseSession(localStorage.getItem(SESSION_KEY));
  if (current) return current;

  // Metadata alone must never resurrect a logged-out session.
  const storage = sessionStorage.getItem("refreshToken") ? sessionStorage : localStorage;
  const accessToken = storage.getItem("accessToken");
  const refreshToken = storage.getItem("refreshToken");
  let legacy;
  try { legacy = JSON.parse(localStorage.getItem("helishop-auth-storage") || "null")?.state; } catch { legacy = null; }
  const migrated = legacy?.user?.id && accessToken && refreshToken && legacy.refreshToken === refreshToken
    ? { user: legacy.user, accessToken, refreshToken, rememberMe: storage === localStorage } : null;
  removeLegacySessions();
  if (migrated) storage.setItem(SESSION_KEY, JSON.stringify(migrated));
  return migrated;
}

export function writeAuthSession(session: AuthSession | null) {
  localStorage.removeItem(SESSION_KEY);
  sessionStorage.removeItem(SESSION_KEY);
  removeLegacySessions();
  if (session) {
    const storage = session.rememberMe ? localStorage : sessionStorage;
    storage.setItem(SESSION_KEY, JSON.stringify(session));
  }
  listeners.forEach(listener => listener(session));
}

export function updateSessionAccessToken(token: string) {
  const session = readAuthSession();
  if (session) writeAuthSession({ ...session, accessToken: token });
}

export function subscribeAuthSession(listener: (session: AuthSession | null) => void) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}
