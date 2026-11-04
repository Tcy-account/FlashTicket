<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { forgotPassword, resetPassword } from '../api';

const router = useRouter();
const message = useMessage();

const step = ref<'email' | 'reset'>('email');
const form = reactive({ email: '', verifyCode: '', newPassword: '', confirmPassword: '' });
const sending = ref(false);
const countdown = ref(0);
const loading = ref(false);
let timer: ReturnType<typeof setInterval> | null = null;

function startCountdown() {
  countdown.value = 60;
  timer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0 && timer) {
      clearInterval(timer);
      timer = null;
    }
  }, 1000);
}

async function onSendCode() {
  if (!/^\S+@\S+\.\S+$/.test(form.email)) {
    message.warning('请输入正确的邮箱');
    return;
  }
  sending.value = true;
  try {
    await forgotPassword({ email: form.email });
    message.success('验证码已发送（若邮箱已注册）');
    step.value = 'reset';
    startCountdown();
  } catch (e: any) {
    message.error(e?.message ?? '发送失败');
  } finally {
    sending.value = false;
  }
}

async function onSubmit() {
  if (!/^\d{6}$/.test(form.verifyCode)) {
    message.warning('验证码为 6 位数字');
    return;
  }
  if (form.newPassword.length < 12) {
    message.warning('新密码至少 12 位');
    return;
  }
  if (form.newPassword !== form.confirmPassword) {
    message.warning('两次输入的新密码不一致');
    return;
  }
  loading.value = true;
  try {
    await resetPassword({ email: form.email, verifyCode: form.verifyCode, newPassword: form.newPassword });
    message.success('密码已重置，请登录');
    router.push('/login');
  } catch (e: any) {
    message.error(e?.message ?? '重置失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-[var(--bg-page)]">
    <div class="w-full max-w-md rounded-xl border border-[var(--border)] bg-white p-8">
      <h1 class="text-2xl font-bold">找回密码</h1>
      <p class="mt-1 text-sm text-[var(--text-secondary)]">通过邮箱验证码重置密码</p>

      <n-form class="mt-6" label-placement="top" @keyup.enter="onSubmit">
        <n-form-item label="邮箱">
          <n-input v-model:value="form.email" size="large" placeholder="注册邮箱" />
        </n-form-item>

        <template v-if="step === 'reset'">
          <n-form-item label="验证码">
            <div class="flex w-full gap-2">
              <n-input v-model:value="form.verifyCode" size="large" placeholder="6 位数字" />
              <n-button size="large" :disabled="countdown > 0" :loading="sending" @click="onSendCode">
                {{ countdown > 0 ? `${countdown}s` : '重发' }}
              </n-button>
            </div>
          </n-form-item>
          <n-form-item label="新密码">
            <n-input v-model:value="form.newPassword" type="password" show-password-on="click" size="large" placeholder="至少 12 位" />
          </n-form-item>
          <n-form-item label="确认新密码">
            <n-input v-model:value="form.confirmPassword" type="password" show-password-on="click" size="large" />
          </n-form-item>
          <n-button type="primary" size="large" block :loading="loading" @click="onSubmit">重置密码</n-button>
        </template>

        <n-button v-else type="primary" size="large" block :loading="sending" @click="onSendCode">
          发送验证码
        </n-button>
      </n-form>

      <div class="mt-4 text-center">
        <router-link to="/login" class="text-sm text-[var(--brand)]">返回登录</router-link>
      </div>
    </div>
  </div>
</template>
