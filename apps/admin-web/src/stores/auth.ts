import { defineStore } from 'pinia';
import { setAccessToken } from '@flashticket/api-client';
import type { AdminProfile } from '../api/types';

interface AuthState {
  accessToken: string | null;
  user: AdminProfile | null;
}

/**
 * 后台登录态（Access Token 存内存，Refresh Token 走 HttpOnly Cookie）。
 */
export const useAuthStore = defineStore('admin-auth', {
  state: (): AuthState => ({
    accessToken: null,
    user: null,
  }),
  getters: {
    isLoggedIn: (state): boolean => !!state.accessToken,
    permissions: (state): string[] => state.user?.permissions ?? [],
  },
  actions: {
    setSession(token: string | null, user: AdminProfile | null) {
      this.accessToken = token;
      this.user = user;
      setAccessToken(token);
    },
    hasPermission(code: string): boolean {
      return this.permissions.includes(code);
    },
    logout() {
      this.setSession(null, null);
    },
  },
});
