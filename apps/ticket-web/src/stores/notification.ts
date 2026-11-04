import { defineStore } from 'pinia';
import { getStreamToken, getUnreadCount } from '../api';

interface NotificationState {
  unreadCount: number;
}

let eventSource: EventSource | null = null;
let reconnectTimer: number | null = null;

/**
 * 通知中心全局状态（M14）：未读数 + SSE 实时连接。
 *
 * 登录后建立 SSE 连接，实时接收 notification.created / unread-count 事件；
 * 断线自动重连，离线期间未读数由页面进入时 REST 补查。
 */
export const useNotificationStore = defineStore('notification', {
  state: (): NotificationState => ({
    unreadCount: 0,
  }),
  actions: {
    async refreshUnread() {
      try {
        const r = await getUnreadCount();
        this.unreadCount = r.data;
      } catch {
        // 未读查询失败静默忽略，下次再补
      }
    },
    async connect() {
      this.disconnect();
      try {
        const tokenResp = await getStreamToken();
        const token = tokenResp.data.token;
        const es = new EventSource(`/api/v1/notifications/stream?token=${encodeURIComponent(token)}`);
        eventSource = es;
        es.addEventListener('notification.created', () => {
          void this.refreshUnread();
        });
        es.addEventListener('notification.unread-count', (e) => {
          const n = Number((e as MessageEvent).data);
          if (!Number.isNaN(n)) this.unreadCount = n;
        });
        es.onerror = () => {
          this.disconnect();
          reconnectTimer = window.setTimeout(() => this.connect(), 5000);
        };
      } catch {
        reconnectTimer = window.setTimeout(() => this.connect(), 10000);
      }
    },
    disconnect() {
      if (eventSource) {
        eventSource.close();
        eventSource = null;
      }
      if (reconnectTimer !== null) {
        window.clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
    },
  },
});
