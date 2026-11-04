<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { adminLogin } from '../api';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const form = reactive({ account: '', password: '' });
const loading = ref(false);

async function onSubmit() {
  if (!form.account.trim() || !form.password) {
    ElMessage.warning('请输入账号和密码');
    return;
  }
  loading.value = true;
  try {
    const resp = await adminLogin({ account: form.account.trim(), password: form.password });
    auth.setSession(resp.data.accessToken, resp.data.user);
    ElMessage.success('登录成功');
    router.push('/');
  } catch (e: any) {
    ElMessage.error(e?.message ?? '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login">
    <div class="login-panel">
      <div class="brand">
        <div class="logo">FlashTicket</div>
        <div class="slogan">运营管理端</div>
      </div>
      <div class="form">
        <h2 class="title">后台登录</h2>
        <p class="subtitle">仅限具备后台角色（运营/财务/管理员/超管）的账号登录</p>
        <el-form @keyup.enter="onSubmit">
          <el-form-item>
            <el-input v-model="form.account" size="large" placeholder="用户名" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" show-password size="large" placeholder="密码" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="onSubmit">
            登录
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
}
.login-panel {
  display: flex;
  width: 760px;
  max-width: 92vw;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}
.brand {
  width: 38%;
  background: #19182f;
  color: #fff;
  padding: 48px 32px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  .logo {
    font-size: 20px;
    font-weight: 600;
  }
  .slogan {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.6);
  }
}
.form {
  flex: 1;
  padding: 48px 40px;
  .title {
    margin: 0 0 8px;
    font-size: 22px;
    font-weight: 600;
  }
  .subtitle {
    margin: 0 0 24px;
    font-size: 13px;
    color: #9ca3af;
  }
}
.submit {
  width: 100%;
}
</style>
