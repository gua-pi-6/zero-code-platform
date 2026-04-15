<template>
  <div id="appEditPage" class="page-shell page-shell--wide edit-page">
    <section class="edit-header">
      <div>
        <span class="page-eyebrow">App Settings</span>
        <h1 class="page-title page-title--section">{{ appInfo?.appName || '编辑应用设置' }}</h1>
        <p class="page-subtitle">
          调整应用名称、封面和后台元信息。这里不改动原有生成逻辑，只负责管理展示层和元数据。
        </p>
      </div>

      <div class="edit-header__actions">
        <a-button @click="goToChat">返回对话</a-button>
        <a-button v-if="appInfo?.deployKey" type="primary" ghost @click="openPreview">
          打开预览
        </a-button>
      </div>
    </section>

    <section class="edit-layout">
      <div class="surface-panel edit-form-panel">
        <div class="panel-heading">
          <h2>基础信息</h2>
          <p>可编辑字段会根据当前用户权限自动收敛，避免破坏原有权限边界。</p>
        </div>

        <a-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item label="应用名称" name="appName">
            <a-input
              v-model:value="formData.appName"
              placeholder="请输入应用名称"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="应用封面"
            name="cover"
            extra="建议使用 400 x 300 左右的图片地址，以获得更稳定的首页展示效果。"
          >
            <a-input v-model:value="formData.cover" placeholder="请输入封面图片 URL" />
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            label="优先级"
            name="priority"
            extra="设置为 99 时通常用于首页精选推荐。"
          >
            <a-input-number v-model:value="formData.priority" :min="0" :max="99" />
          </a-form-item>

          <a-form-item label="初始提示词" name="initPrompt">
            <a-textarea
              v-model:value="formData.initPrompt"
              :rows="4"
              :maxlength="1000"
              disabled
              show-count
            />
            <div class="field-note">初始提示词由创建阶段生成，这里保持只读。</div>
          </a-form-item>

          <a-form-item label="生成方式" name="codeGenType">
            <a-input :value="formatCodeGenType(formData.codeGenType)" disabled />
            <div class="field-note">生成方式会影响预览和下载结构，因此不在这里改动。</div>
          </a-form-item>

          <a-form-item v-if="formData.deployKey" label="部署标识" name="deployKey">
            <a-input v-model:value="formData.deployKey" disabled />
          </a-form-item>

          <div class="form-actions">
            <a-button type="primary" html-type="submit" :loading="submitting">保存更改</a-button>
            <a-button @click="resetForm">重置</a-button>
          </div>
        </a-form>
      </div>

      <div class="edit-side">
        <div class="surface-panel edit-cover-panel">
          <div class="panel-heading">
            <h2>封面预览</h2>
            <p>首页和卡片视图会优先使用这里的封面作为视觉入口。</p>
          </div>

          <div v-if="coverPreviewUrl" class="cover-preview">
            <img :src="coverPreviewUrl" alt="应用封面预览" />
          </div>
          <div v-else class="cover-placeholder">
            <strong>{{ appInfo?.appName?.charAt(0) || 'A' }}</strong>
            <p>暂未设置封面</p>
          </div>
        </div>

        <div class="surface-panel edit-meta-panel">
          <div class="panel-heading">
            <h2>只读元信息</h2>
            <p>以下数据来自现有后端接口，只作为辅助参考，不参与编辑提交。</p>
          </div>

          <dl class="meta-list">
            <div class="meta-item">
              <dt>应用 ID</dt>
              <dd>{{ appInfo?.id || '-' }}</dd>
            </div>
            <div class="meta-item">
              <dt>创建者</dt>
              <dd><UserInfo :user="appInfo?.user" size="small" /></dd>
            </div>
            <div class="meta-item">
              <dt>创建时间</dt>
              <dd>{{ formatTime(appInfo?.createTime) || '-' }}</dd>
            </div>
            <div class="meta-item">
              <dt>更新时间</dt>
              <dd>{{ formatTime(appInfo?.updateTime) || '-' }}</dd>
            </div>
            <div class="meta-item">
              <dt>部署时间</dt>
              <dd>{{ appInfo?.deployedTime ? formatTime(appInfo.deployedTime) : '未部署' }}</dd>
            </div>
            <div class="meta-item">
              <dt>部署站点</dt>
              <dd>
                <a-button v-if="appInfo?.deployKey" type="link" @click="openPreview" size="small">
                  打开站点
                </a-button>
                <span v-else>暂无</span>
              </dd>
            </div>
          </dl>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { getStaticPreviewUrl } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'
import { formatCodeGenType } from '@/utils/codeGenTypes'
import { hasId, sameId, toIdString } from '@/utils/id'
import { formatTime } from '@/utils/time'
import UserInfo from '@/components/UserInfo.vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appInfo = ref<API.AppVO>()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
})

const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const coverPreviewUrl = computed(() => formData.cover || appInfo.value?.cover || '')

const rules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 1, max: 50, message: '应用名称长度需在 1 到 50 个字符之间', trigger: 'blur' },
  ],
  cover: [{ type: 'url', message: '请输入合法的图片地址', trigger: 'blur' }],
  priority: [{ type: 'number', min: 0, max: 99, message: '优先级范围为 0 到 99', trigger: 'blur' }],
}

const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用 ID 不存在')
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      if (!isAdmin.value && !sameId(appInfo.value.userId, loginUserStore.loginUser.id)) {
        message.error('你没有权限编辑这个应用')
        router.push('/')
        return
      }

      formData.appName = appInfo.value.appName || ''
      formData.cover = appInfo.value.cover || ''
      formData.priority = appInfo.value.priority || 0
      formData.initPrompt = appInfo.value.initPrompt || ''
      formData.codeGenType = appInfo.value.codeGenType || ''
      formData.deployKey = appInfo.value.deployKey || ''
    } else {
      message.error('加载应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('加载应用信息失败', error)
    message.error('加载应用信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const currentAppId = toIdString(appInfo.value?.id)
  if (!currentAppId) return

  submitting.value = true
  try {
    const res = isAdmin.value
      ? await updateAppByAdmin({
          id: currentAppId,
          appName: formData.appName,
          cover: formData.cover,
          priority: formData.priority,
        })
      : await updateApp({
          id: currentAppId,
          appName: formData.appName,
        })

    if (res.data.code === 0) {
      message.success('保存成功')
      await fetchAppInfo()
    } else {
      message.error(`保存失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('保存失败', error)
    message.error('保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  if (appInfo.value) {
    formData.appName = appInfo.value.appName || ''
    formData.cover = appInfo.value.cover || ''
    formData.priority = appInfo.value.priority || 0
  }
  formRef.value?.clearValidate()
}

const goToChat = () => {
  if (hasId(appInfo.value?.id)) {
    router.push(`/app/chat/${toIdString(appInfo.value?.id)}`)
  }
}

const openPreview = () => {
  if (appInfo.value?.codeGenType && appInfo.value?.id) {
    const url = getStaticPreviewUrl(appInfo.value.codeGenType, String(appInfo.value.id))
    window.open(url, '_blank')
  }
}

onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
.edit-page {
  display: grid;
  gap: 28px;
  padding-bottom: 40px;
}

.edit-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.edit-header__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.edit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.75fr);
  gap: 24px;
}

.edit-form-panel,
.edit-cover-panel,
.edit-meta-panel {
  padding: 28px;
}

.edit-side {
  display: grid;
  gap: 24px;
}

.panel-heading h2 {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: 1.8rem;
}

.panel-heading p {
  margin: 10px 0 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.field-note {
  margin-top: 8px;
  color: var(--text-subtle);
  font-size: 13px;
}

.form-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-preview {
  overflow: hidden;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.7);
}

.cover-preview img {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 10;
  object-fit: cover;
}

.cover-placeholder {
  display: grid;
  place-items: center;
  gap: 10px;
  aspect-ratio: 16 / 10;
  border: 1px solid var(--border-light);
  border-radius: 26px;
  background:
    radial-gradient(circle at top left, rgba(201, 100, 66, 0.18), transparent 24%),
    linear-gradient(135deg, #efe9dc, #e6ddce);
}

.cover-placeholder strong {
  color: rgba(20, 20, 19, 0.14);
  font-family: var(--font-serif);
  font-size: clamp(4rem, 8vw, 5.2rem);
  line-height: 1;
}

.cover-placeholder p {
  margin: 0;
  color: var(--text-subtle);
}

.meta-list {
  display: grid;
  gap: 14px;
  margin: 0;
}

.meta-item {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-light);
}

.meta-item:last-child {
  border-bottom: none;
}

.meta-item dt {
  color: var(--text-subtle);
  font-size: 13px;
  font-weight: 600;
}

.meta-item dd {
  margin: 0;
  color: var(--text-strong);
}

@media (max-width: 1040px) {
  .edit-header,
  .edit-layout {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .edit-form-panel,
  .edit-cover-panel,
  .edit-meta-panel {
    padding: 22px;
  }

  .meta-item {
    grid-template-columns: 1fr;
  }
}
</style>
