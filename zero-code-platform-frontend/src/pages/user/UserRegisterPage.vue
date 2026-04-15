<template>
  <div id="userRegisterPage" class="page-shell auth-page">
    <div class="surface-panel auth-layout">
      <section class="auth-aside">
        <span class="page-eyebrow">Create Account</span>
        <h1 class="auth-title">注册一个新的创作工作台</h1>
        <p class="auth-description">
          从今天开始沉淀你的应用项目。注册后，你可以把每一次生成、修改和部署都保存在同一条工作流里。
        </p>

        <ul class="editorial-list">
          <li>
            <span>History</span>
            <strong>保留你的项目脉络</strong>
            <p>每个应用都会保留创建来源和后续对话，便于持续迭代。</p>
          </li>
          <li>
            <span>Preview</span>
            <strong>边聊边看结果</strong>
            <p>注册后即可在聊天页实时预览页面效果，并继续补充需求。</p>
          </li>
        </ul>
      </section>

      <section class="auth-form-panel">
        <div class="auth-form-header">
          <h2>创建账号</h2>
          <p>填写以下信息，马上开始生成你的第一个应用。</p>
        </div>

        <a-form :model="formState" name="register" autocomplete="off" @finish="handleSubmit">
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
          <a-form-item
            name="checkPassword"
            :rules="[
              { required: true, message: '请再次输入密码' },
              { min: 8, message: '密码长度不能少于 8 位' },
              { validator: validateCheckPassword },
            ]"
          >
            <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" />
          </a-form-item>

          <div class="auth-tips">
            已经有账号？
            <RouterLink to="/user/login">去登录</RouterLink>
          </div>

          <a-form-item>
            <a-button type="primary" html-type="submit" block>注册并开始</a-button>
          </a-form-item>
        </a-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userRegister } from '@/api/userController'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (
  rule: unknown,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error(`注册失败：${res.data.message}`)
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
