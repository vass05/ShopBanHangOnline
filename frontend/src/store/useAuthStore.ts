import { create } from "zustand";
import { api } from "@/lib/api";
import { readAuthSession, writeAuthSession, updateSessionAccessToken, subscribeAuthSession } from "@/lib/authSession";
import type { SessionUser } from "@/lib/authSession";

export type User = SessionUser;

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  setAuth: (payload: { user: User; accessToken: string; refreshToken: string; rememberMe?: boolean }) => void;
  updateUser: (partialUser: Partial<User>) => void;
  updateAccessToken: (token: string) => void;
  corruptAccessToken: () => void;
  logout: () => Promise<void>;
  clearAuth: () => void;
}

const sessionState = (session: ReturnType<typeof readAuthSession>) => ({
  user: session?.user || null,
  accessToken: session?.accessToken || null,
  refreshToken: session?.refreshToken || null,
  isAuthenticated: !!session,
});

export const useAuthStore = create<AuthState>()(() => ({
  ...sessionState(readAuthSession()),
  setAuth: ({ rememberMe = true, ...session }) => writeAuthSession({ ...session, rememberMe }),
  updateUser: (partialUser) => {
    const session = readAuthSession();
    if (session) writeAuthSession({ ...session, user: { ...session.user, ...partialUser } });
  },
  updateAccessToken: updateSessionAccessToken,
  corruptAccessToken: () => updateSessionAccessToken("corrupted_token_test_401_simulation"),
  logout: async () => {
    const refreshToken = readAuthSession()?.refreshToken;
    writeAuthSession(null);
    try {
      if (refreshToken) await api.post("/auth/logout", { refreshToken });
    } catch {
      // Local logout is complete even if the server is unavailable.
    }
  },
  clearAuth: () => writeAuthSession(null),
}));

subscribeAuthSession(session => useAuthStore.setState(sessionState(session)));
window.addEventListener("storage", () => useAuthStore.setState(sessionState(readAuthSession())));
