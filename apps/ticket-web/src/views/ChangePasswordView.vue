<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { changePassword } from '../api';

const router = useRouter();
const message = useMessage();

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
const loading = ref(false);

async function onSubmit() {
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
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword });
    message.success('密码已修改');
    router.push('/profile');
  } catch (e: any) {
    message.error(e?.message ?? '修改失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-[var(--bg-page)]">
    <div class="w-full max-w-md rounded-xl border border-[var(--border)] bg-white p-8">
      <h1 class="text-2xl font-bold">修改密码</h1>
      <p class="mt-1 text-sm text-[var(--text-secondary)]">修改后其他设备将被下线</p>

      <n-form class="mt-6" label-placement="top" @keyup.enter="onSubmit">
        <n-form-item label="当前密码">
          <n-input v-model:value="form.oldPassword" type="password" show-password-on="click" size="large" />
        </n-form-item>
        <n-form-item label="新密码">
          <n-input v-model:value="form.newPassword" type="password" show-password-on="click" size="large" placeholder="至少 12 位" />
        </n-form-item>
        <n-form-item label="确认新密码">
          <n-input v-model:value="form.confirmPassword" type="password" show-password-on="click" size="large" />
        </n-form-item>
        <n-button type="primary" size="large" block :loading="loading" @click="onSubmit">确认修改</n-button>
      </n-form>

      <div class="mt-4 text-center">
        <router-link to="/profile" class="text-sm text-[var(--brand)]">返回个人中心</router-link>
      </div>
    </div>
  </div>
</template>
