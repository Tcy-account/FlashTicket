<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { login } from '../api';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const message = useMessage();
const auth = useAuthStore();

const form = reactive({ account: '', password: '' });
const loading = ref(false);

async function onSubmit() {
  if (!form.account.trim() || !form.password) {
    message.warning('请输入账号和密码');
    return;
  }
  loading.value = true;
  try {
    const resp = await login({ account: form.account.trim(), password: form.password });
    auth.setSession(resp.data.accessToken, resp.data.user);
    message.success('登录成功');
    router.push('/');
  } catch (e: any) {
    // 后端 M02 就绪前会返回 401/404，如实展示，不伪装成功
    message.error(e?.message ?? '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="flex min-h-screen">
    <!-- 左侧品牌展示区（约 42%） -->
    <aside
      class="relative hidden w-5/12 flex-col justify-between overflow-hidden p-12 md:flex"
      style="background: linear-gradient(160deg, #19182f 0%, #221a45 55%, #35247a 100%)"
    >
      <div
        class="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full opacity-30"
        style="background: radial-gradient(circle, #635bff 0%, transparent 65%)"
      />
      <div
        class="pointer-events-none absolute -bottom-32 -left-16 h-80 w-80 rounded-full opacity-20"
        style="background: radial-gradient(circle, #8b5cf6 0%, transparent 65%)"
      />

      <div class="relative">
        <div class="text-2xl font-semibold text-white">FlashTicket</div>
        <div class="mt-4 text-sm text-white/60">每一场热爱，都值得奔赴</div>
      </div>
      <div class="relative">
        <h1 class="text-3xl font-bold leading-snug text-white">精彩现场，一触即达。</h1>
        <p class="mt-3 text-sm text-white/60">发现喜爱的演出，开启每一场难忘的现场体验。</p>
      </div>
    </aside>

    <!-- 右侧登录表单 -->
    <main class="flex w-full items-center justify-center bg-white md:w-7/12">
      <div class="w-full max-w-md px-8 py-10">
        <h2 class="text-2xl font-semibold">欢迎回来</h2>
        <p class="mt-2 text-sm text-[var(--text-secondary)]">登录 FlashTicket 账号</p>

        <n-form class="mt-8" @keyup.enter="onSubmit">
          <n-form-item label="用户名或邮箱">
            <n-input v-model:value="form.account" size="large" placeholder="用户名或已验证邮箱" />
          </n-form-item>
          <n-form-item label="密码">
            <n-input
              v-model:value="form.password"
              type="password"
              show-password-on="click"
              size="large"
              placeholder="请输入密码"
            />
          </n-form-item>
          <n-button
            type="primary"
            size="large"
            block
            :loading="loading"
            class="!h-12 !rounded-lg"
            @click="onSubmit"
          >
            登录
          </n-button>
        </n-form>

        <div class="mt-6 text-center text-sm text-[var(--text-secondary)]">
          还没有账号？
          <router-link to="/register" class="font-medium text-[var(--brand)]">立即注册</router-link>
          <span class="mx-2 text-[var(--border)]">|</span>
          <router-link to="/forgot" class="font-medium text-[var(--brand)]">忘记密码</router-link>
        </div>
      </div>
    </main>
  </div>
</template>
