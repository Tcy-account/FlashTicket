import { defineStore } from 'pinia';
import { setAccessToken } from '@flashticket/api-client';
import type { UserProfile } from '../api/types';

interface AuthState {
  accessToken: string | null;
  user: UserProfile | null;
}

/**
 * 登录态（规范：Access Token 存内存，Refresh Token 走 HttpOnly Cookie）。
 * M02 登录接口就绪后，由 LoginView 调用 setSession 写入。
 */
export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: null,
    user: null,
  }),
  getters: {
    isLoggedIn: (state): boolean => !!state.accessToken,
  },
  actions: {
    setSession(token: string | null, user: UserProfile | null) {
      this.accessToken = token;
      this.user = user;
      setAccessToken(token);
    },
    logout() {
      this.setSession(null, null);
    },
  },
});
