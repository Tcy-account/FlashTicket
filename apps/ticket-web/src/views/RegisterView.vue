<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { sendVerifyCode, register } from '../api';

const router = useRouter();
const message = useMessage();

const form = reactive({
  username: '',
  email: '',
  verifyCode: '',
  password: '',
  confirmPassword: '',
});

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
  if (!form.email) {
    message.warning('请先填写邮箱');
    return;
  }
  sending.value = true;
  try {
    await sendVerifyCode({ email: form.email, purpose: 'REGISTER' });
    message.success('验证码已发送');
    startCountdown();
  } catch (e: any) {
    message.error(e?.message ?? '验证码发送失败');
  } finally {
    sending.value = false;
  }
}

function validate(): string | null {
  if (!/^[a-zA-Z0-9_]{4,32}$/.test(form.username)) return '用户名需为 4-32 位字母、数字或下划线';
  if (!/^\S+@\S+\.\S+$/.test(form.email)) return '邮箱格式不正确';
  if (!/^\d{6}$/.test(form.verifyCode)) return '验证码为 6 位数字';
  if (form.password.length < 12 || form.password.length > 64) return '密码长度需为 12-64 位';
  if (form.password !== form.confirmPassword) return '两次输入的密码不一致';
  return null;
}

async function onSubmit() {
  const err = validate();
  if (err) {
    message.warning(err);
    return;
  }
  loading.value = true;
  try {
    await register({
      username: form.username,
      email: form.email,
      verifyCode: form.verifyCode,
      password: form.password,
    });
    message.success('注册成功，请登录');
    router.push('/login');
  } catch (e: any) {
    message.error(e?.message ?? '注册失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="flex min-h-screen">
    <!-- 左侧品牌展示区（约 42%，收窄避免与表单五五开） -->
    <aside
      class="relative hidden w-5/12 flex-col justify-between overflow-hidden p-12 md:flex"
      style="background: linear-gradient(160deg, #19182f 0%, #221a45 55%, #35247a 100%)"
    >
      <!-- 品牌光晕装饰 -->
      <div
        class="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full opacity-30"
        style="background: radial-gradient(circle, #635bff 0%, transparent 65%)"
      />
      <div
        class="pointer-events-none absolute -bottom-32 -left-16 h-80 w-80 rounded-full opacity-20"
        style="background: radial-gradient(circle, #8b5cf6 0%, transparent 65%)"
      />

      <div class="relative text-2xl font-semibold text-white">FlashTicket</div>
      <div class="relative">
        <h1 class="text-3xl font-bold leading-snug text-white">精彩现场，一触即达。</h1>
        <p class="mt-3 text-sm text-white/60">发现喜爱的演出，开启每一场难忘的现场体验。</p>
      </div>
    </aside>

    <!-- 右侧注册表单 -->
    <main class="flex w-full items-center justify-center bg-white md:w-7/12">
      <div class="w-full max-w-md px-8 py-10">
        <h2 class="text-2xl font-semibold">创建账号</h2>
        <p class="mt-2 text-sm text-[var(--text-secondary)]">注册后即可开始抢票</p>

        <n-form class="mt-6" @keyup.enter="onSubmit">
          <n-form-item label="用户名">
            <n-input v-model:value="form.username" size="large" placeholder="4-32 位字母、数字或下划线" />
          </n-form-item>
          <n-form-item label="邮箱">
            <n-input v-model:value="form.email" size="large" placeholder="用于验证与通知" />
          </n-form-item>
          <n-form-item label="邮箱验证码">
            <div class="flex w-full gap-2">
              <n-input v-model:value="form.verifyCode" size="large" placeholder="6 位数字" />
              <n-button
                size="large"
                :disabled="countdown > 0"
                :loading="sending"
                @click="onSendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
              </n-button>
            </div>
          </n-form-item>
          <n-form-item label="密码">
            <n-input
              v-model:value="form.password"
              type="password"
              show-password-on="click"
              size="large"
              placeholder="至少 12 位"
            />
          </n-form-item>
          <n-form-item label="确认密码">
            <n-input
              v-model:value="form.confirmPassword"
              type="password"
              show-password-on="click"
              size="large"
              placeholder="再次输入密码"
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
            注册
          </n-button>
        </n-form>

        <div class="mt-6 text-center text-sm text-[var(--text-secondary)]">
          已有账号？
          <router-link to="/login" class="font-medium text-[var(--brand)]">立即登录</router-link>
        </div>
      </div>
    </main>
  </div>
</template>
