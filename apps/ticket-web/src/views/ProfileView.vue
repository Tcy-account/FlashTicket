<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { getMe, updateProfile } from '../api';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const message = useMessage();
const auth = useAuthStore();

const loading = ref(true);
const saving = ref(false);
const form = reactive({ nickname: '', avatarUrl: '' });

async function load() {
  loading.value = true;
  try {
    const resp = await getMe();
    form.nickname = resp.data.nickname;
    form.avatarUrl = resp.data.avatarUrl ?? '';
    auth.user = resp.data;
  } catch (e: any) {
    message.error(e?.message ?? '加载失败');
  } finally {
    loading.value = false;
  }
}

async function onSave() {
  if (form.nickname.trim().length < 2 || form.nickname.trim().length > 32) {
    message.warning('昵称长度需为 2-32 字符');
    return;
  }
  saving.value = true;
  try {
    const resp = await updateProfile({ nickname: form.nickname.trim(), avatarUrl: form.avatarUrl });
    auth.user = resp.data;
    message.success('资料已更新');
  } catch (e: any) {
    message.error(e?.message ?? '更新失败');
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="min-h-screen bg-[var(--bg-page)]">
    <header class="sticky top-0 z-10 border-b border-[var(--border)] bg-white">
      <div class="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <div class="text-xl font-semibold text-[var(--brand)]">FlashTicket</div>
        <nav class="flex items-center gap-6 text-sm">
          <router-link to="/" class="text-[var(--text-secondary)] hover:text-[var(--brand)]">首页</router-link>
          <router-link to="/profile" class="text-[var(--brand)]">个人中心</router-link>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-2xl px-6 py-12">
      <h1 class="text-2xl font-bold">个人中心</h1>
      <p class="mt-1 text-sm text-[var(--text-secondary)]">管理你的账号资料</p>

      <div class="mt-8 rounded-xl border border-[var(--border)] bg-white p-8">
        <div class="mb-6 space-y-1 text-sm">
          <div class="flex justify-between">
            <span class="text-[var(--text-secondary)]">用户名</span>
            <span class="text-[var(--text-primary)]">{{ auth.user?.username ?? '—' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-[var(--text-secondary)]">邮箱</span>
            <span class="text-[var(--text-primary)]">{{ auth.user?.email ?? '—' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-[var(--text-secondary)]">角色</span>
            <span class="text-[var(--text-primary)]">{{ (auth.user?.roles ?? []).join(' / ') || '—' }}</span>
          </div>
        </div>

        <n-form label-placement="top">
          <n-form-item label="昵称">
            <n-input v-model:value="form.nickname" size="large" placeholder="2-32 字符" />
          </n-form-item>
          <n-form-item label="头像地址（可选）">
            <n-input v-model:value="form.avatarUrl" size="large" placeholder="https://..." />
          </n-form-item>
          <div class="flex gap-3">
            <n-button type="primary" size="large" :loading="saving" @click="onSave">保存</n-button>
            <n-button size="large" @click="router.push('/password')">修改密码</n-button>
          </div>
        </n-form>
      </div>
    </main>
  </div>
</template>
