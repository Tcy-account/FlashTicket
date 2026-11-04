<script setup lang="ts">
import { watch } from 'vue';
import { NConfigProvider, NMessageProvider, NDialogProvider } from 'naive-ui';
import { useAuthStore } from './stores/auth';
import { useNotificationStore } from './stores/notification';

const themeOverrides = {
  common: {
    primaryColor: '#635BFF',
    primaryColorHover: '#5148E5',
    primaryColorPressed: '#5148E5',
    primaryColorSuppl: '#8B5CF6',
  },
};

// 登录后建立通知 SSE 连接，退出后断开
const auth = useAuthStore();
const notif = useNotificationStore();
watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      void notif.refreshUnread();
      void notif.connect();
    } else {
      notif.disconnect();
    }
  },
  { immediate: true },
);

// 暂强制浅色主题：设计令牌（tokens.css）与页面背景均按浅色设计，
// 跟随系统切深色会导致 Naive 组件白字画在白底上（标签/占位符隐形）。
// 深色模式待设计规范补充深色令牌后再启用：:theme="isDark ? darkTheme : null"
</script>

<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>
