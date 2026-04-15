<template>
  <div id="userLoginPage" class="page-shell auth-page">
    <div class="surface-panel auth-layout">
      <section class="auth-aside">
        <span class="page-eyebrow">Welcome Back</span>
        <h1 class="auth-title">登录到你的应用工作台</h1>
        <p class="auth-description">
          继续对话、继续预览、继续部署。你之前创建的每一个应用都在这里等你回来迭代。
        </p>

        <ul class="editorial-list">
          <li>
            <span>Workspace</span>
            <strong>继续已有项目</strong>
            <p>重新进入聊天工作区，查看历史生成记录和当前预览结果。</p>
          </li>
          <li>
            <span>Deploy</span>
            <strong>随时下载与发布</strong>
            <p>登录后你可以直接下载代码包，或把应用发布到部署地址。</p>
          </li>
        </ul>
      </section>

      <section class="auth-form-panel">
        <div class="auth-form-header">
          <h2>账号登录</h2>
          <p>请输入你的账号和密码，继续管理应用。</p>
        </div>

        <a-form :model="formState" name="login" autocomplete="off" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
          </a-form-item>
          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能少于 8 位' },
            ]"
          >
            <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
          </a-form-item>

          <div class="auth-tips">
            还没有账号？
            <RouterLink to="/user/register">立即注册</RouterLink>
          </div>

          <a-form-item>
            <a-button type="primary" html-type="submit" block>登录</a-button>
          </a-form-item>
        </a-form>
      </section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLogin } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await userLogin(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error(`登录失败：${res.data.message}`)
  }
}
</script>

<style scoped>
.auth-page {
  padding-bottom: 48px;
}

.auth-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 460px);
  gap: 28px;
  padding: 28px;
}

.auth-aside,
.auth-form-panel {
  display: grid;
  gap: 24px;
}

.auth-aside {
  padding: 18px;
}

.auth-title {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: clamp(2.2rem, 4vw, 3.6rem);
  line-height: 1.08;
}

.auth-description {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.8;
}

.auth-form-panel {
  padding: 22px;
  border: 1px solid var(--border-light);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.5);
}

.auth-form-header h2 {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: 2rem;
}

.auth-form-header p {
  margin: 10px 0 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.auth-tips {
  margin-bottom: 18px;
  color: var(--text-muted);
  text-align: right;
}

.auth-tips a {
  color: var(--accent);
}

@media (max-width: 900px) {
  .auth-layout {
    grid-template-columns: 1fr;
    padding: 22px;
  }
}
</style>
