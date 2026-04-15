<template>
  <div id="appChatPage" class="page-shell--wide app-chat-page">
    <section class="surface-panel workspace-shell">
      <header class="workspace-header">
        <div class="workspace-title">
          <span class="page-eyebrow">App Workspace</span>
          <div class="workspace-title__row">
            <h1>{{ appInfo?.appName || '应用工作区' }}</h1>
            <a-tag v-if="appInfo?.codeGenType" color="processing">
              {{ formatCodeGenType(appInfo.codeGenType) }}
            </a-tag>
          </div>
          <p>
            {{ isOwner ? '继续补充需求、查看预览或发布站点。' : '当前为只读模式，你可以查看结果但不能继续生成。' }}
          </p>
        </div>

        <div class="workspace-actions">
          <a-button @click="showAppDetail">
            <template #icon>
              <InfoCircleOutlined />
            </template>
            应用概览
          </a-button>
          <a-button
            v-if="isOwner"
            @click="downloadCode"
            :loading="downloading"
            :disabled="!appInfo?.id"
          >
            <template #icon>
              <DownloadOutlined />
            </template>
            下载代码
          </a-button>
          <a-button type="primary" @click="deployApp" :loading="deploying" :disabled="!isOwner">
            <template #icon>
              <CloudUploadOutlined />
            </template>
            部署站点
          </a-button>
        </div>
      </header>

      <div class="workspace-grid">
        <section class="conversation-panel">
          <div class="panel-header">
            <div>
              <h2>对话生成区</h2>
              <p>每一次输入都会沿用当前应用上下文，并通过 SSE 实时返回生成结果。</p>
            </div>
            <span class="meta-pill">{{ messages.length }} 条消息</span>
          </div>

          <div ref="messagesContainer" class="messages-container">
            <div v-if="hasMoreHistory" class="load-more-container">
              <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory">
                加载更早的历史消息
              </a-button>
            </div>

            <div v-for="(messageItem, index) in messages" :key="index" class="message-item">
              <div v-if="messageItem.type === 'user'" class="message-row message-row--user">
                <div class="message-bubble message-bubble--user">
                  {{ messageItem.content }}
                </div>
                <a-avatar class="message-avatar" :src="loginUserStore.loginUser.userAvatar">
                  {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
                </a-avatar>
              </div>

              <div v-else class="message-row message-row--ai">
                <a-avatar class="message-avatar" :src="aiAvatar" />
                <div class="message-bubble message-bubble--ai">
                  <MarkdownRenderer v-if="messageItem.content" :content="messageItem.content" />
                  <div v-if="messageItem.loading" class="loading-indicator">
                    <a-spin size="small" />
                    <span>AI 正在生成内容，请稍候...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedElementInfo" class="selected-element-panel">
            <div class="selected-element-panel__header">
              <div>
                <strong>当前选中元素</strong>
                <p>接下来的修改指令会自动带上该元素的上下文信息。</p>
              </div>
              <a-button size="small" @click="clearSelectedElement">清除选择</a-button>
            </div>
            <div class="selected-element-tags">
              <span class="status-tag status-tag--accent">
                {{ selectedElementInfo.tagName.toLowerCase() }}
              </span>
              <span v-if="selectedElementInfo.id" class="meta-pill">#{{ selectedElementInfo.id }}</span>
              <span v-if="selectedElementInfo.className" class="meta-pill">
                .{{ selectedElementInfo.className.split(' ').join('.') }}
              </span>
            </div>
            <div class="selected-element-meta">
              <p v-if="selectedElementInfo.textContent">
                文本：{{ selectedElementInfo.textContent.slice(0, 80) }}
                {{ selectedElementInfo.textContent.length > 80 ? '...' : '' }}
              </p>
              <p v-if="selectedElementInfo.pagePath">页面：{{ selectedElementInfo.pagePath }}</p>
              <code>{{ selectedElementInfo.selector }}</code>
            </div>
          </div>

          <div class="composer-panel">
            <div class="composer-panel__header">
              <div>
                <h3>继续生成</h3>
                <p>{{ isOwner ? '输入新的页面需求或局部修改指令。' : '当前应用不属于你，只支持查看。' }}</p>
              </div>
            </div>

            <a-textarea
              v-model:value="userInput"
              class="composer-input"
              :placeholder="getInputPlaceholder()"
              :rows="4"
              :maxlength="1000"
              :disabled="isGenerating || !isOwner"
              @keydown.enter.prevent="sendMessage"
            />

            <div class="composer-panel__footer">
              <span class="composer-hint">
                {{ isOwner ? 'Shift + Enter 可换行，Enter 发送。' : '只读模式下无法继续发送指令。' }}
              </span>
              <a-button type="primary" :loading="isGenerating" :disabled="!isOwner" @click="sendMessage">
                <template #icon>
                  <SendOutlined />
                </template>
                发送指令
              </a-button>
            </div>
          </div>
        </section>

        <section class="preview-panel">
          <div class="panel-header">
            <div>
              <h2>实时预览区</h2>
              <p>生成完成后自动刷新静态预览，同时支持可视化选区辅助局部修改。</p>
            </div>
            <div class="preview-actions">
              <a-button
                v-if="isOwner && previewUrl"
                :class="{ 'preview-action--active': isEditMode }"
                @click="toggleEditMode"
              >
                <template #icon>
                  <EditOutlined />
                </template>
                {{ isEditMode ? '退出选区模式' : '进入选区模式' }}
              </a-button>
              <a-button v-if="previewUrl" @click="openInNewTab">
                <template #icon>
                  <ExportOutlined />
                </template>
                新标签打开
              </a-button>
            </div>
          </div>

          <div class="preview-stage">
            <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
              <strong>预览将在这里出现</strong>
              <p>先完成一次生成，随后就可以在这里直接查看页面效果并继续细化。</p>
            </div>
            <div v-else-if="isGenerating" class="preview-loading">
              <a-spin size="large" />
              <p>正在整理最新的页面预览...</p>
            </div>
            <iframe
              v-else
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
              @load="onIframeLoad"
            ></iframe>
          </div>
        </section>
      </div>
    </section>

    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CloudUploadOutlined,
  DownloadOutlined,
  EditOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  SendOutlined,
} from '@ant-design/icons-vue'
import {
  deleteApp as deleteAppApi,
  deployApp as deployAppApi,
  getAppVoById,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import aiAvatar from '@/assets/aiAvatar.png'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import request from '@/request'
import { useLoginUserStore } from '@/stores/loginUser'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import { hasId, sameId, toIdString } from '@/utils/id'
import { type ElementInfo, VisualEditor } from '@/utils/visualEditor'

interface MessageItem {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appInfo = ref<API.AppVO>()
const appId = ref<string>('')

const messages = ref<MessageItem[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()

const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

const previewUrl = ref('')
const previewReady = ref(false)

const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

const downloading = ref(false)
const activeEventSource = ref<EventSource | null>(null)

const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

const isOwner = computed(() => sameId(appInfo.value?.userId, loginUserStore.loginUser.id))
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

const appDetailVisible = ref(false)

const handleWindowMessage = (event: MessageEvent) => {
  visualEditor.handleIframeMessage(event)
}

const clearActiveStream = () => {
  if (activeEventSource.value) {
    activeEventSource.value.close()
    activeEventSource.value = null
  }
}

const showAppDetail = () => {
  appDetailVisible.value = true
}

const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return

  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }

    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }

    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        const historyMessages: MessageItem[] = chatHistories
          .map((chat) => ({
            type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
            content: chat.message || '',
            createTime: chat.createTime,
          }))
          .reverse()

        if (isLoadMore) {
          messages.value.unshift(...historyMessages)
        } else {
          messages.value = historyMessages
        }

        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载聊天记录失败', error)
    message.error('加载聊天记录失败')
  } finally {
    loadingHistory.value = false
  }
}

const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用 ID 不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      await loadChatHistory()

      if (messages.value.length >= 2 || appInfo.value.deployKey) {
        updatePreview()
      }

      if (
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('加载应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('加载应用信息失败', error)
    message.error('加载应用信息失败')
    router.push('/')
  }
}

const sendInitialMessage = async (prompt: string) => {
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value || !isOwner.value) {
    return
  }

  let content = userInput.value.trim()
  if (selectedElementInfo.value) {
    let elementContext = '\n\n已选中页面元素，请优先围绕这个元素进行修改：'
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径：${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签：${selectedElementInfo.value.tagName.toLowerCase()}`
    elementContext += `\n- 选择器：${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前文本：${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    content += elementContext
  }

  userInput.value = ''
  messages.value.push({
    type: 'user',
    content,
  })

  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  isGenerating.value = true
  await generateCode(content, aiMessageIndex)
}

const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const params = new URLSearchParams({
      appId: toIdString(appId.value),
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`
    eventSource = new EventSource(url, {
      withCredentials: true,
    })
    activeEventSource.value = eventSource

    let fullContent = ''

    eventSource.onmessage = (event) => {
      if (streamCompleted) return

      try {
        const parsed = JSON.parse(event.data)
        const content = parsed.d
        if (content !== undefined && content !== null && messages.value[aiMessageIndex]) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息流失败', error)
        handleError(error, aiMessageIndex)
      }
    }

    eventSource.addEventListener('done', async () => {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      clearActiveStream()

      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    eventSource.addEventListener('business-error', (event: MessageEvent) => {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        const errorMessage = errorData.message || '生成服务返回了业务错误'
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].content = `错误：${errorMessage}`
          messages.value[aiMessageIndex].loading = false
        }
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        clearActiveStream()
      } catch (parseError) {
        console.error('解析业务错误失败', parseError, event.data)
        handleError(new Error('无法识别的错误返回'), aiMessageIndex)
      }
    })

    eventSource.onerror = () => {
      if (streamCompleted || !isGenerating.value) return

      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        clearActiveStream()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE 连接中断'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 SSE 连接失败', error)
    handleError(error, aiMessageIndex)
  }
}

const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败', error)
  if (messages.value[aiMessageIndex]) {
    messages.value[aiMessageIndex].content =
      '生成过程出现异常，请稍后重试。如果问题持续存在，可以重新进入页面后再次发起生成。'
    messages.value[aiMessageIndex].loading = false
  }
  message.error('生成失败，请稍后重试')
  isGenerating.value = false
  clearActiveStream()
}

const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    previewUrl.value = getStaticPreviewUrl(codeGenType, String(appId.value))
    previewReady.value = false
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const downloadCode = async () => {
  if (!hasId(appId.value)) {
    message.error('应用 ID 不存在')
    return
  }

  downloading.value = true
  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const response = await fetch(`${baseURL}/app/download/${toIdString(appId.value)}`, {
      method: 'GET',
      credentials: 'include',
    })

    if (!response.ok) {
      throw new Error(`下载失败：${response.status}`)
    }

    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName =
      contentDisposition?.match(/filename="?([^";]+)"?/)?.[1] ||
      `app-${toIdString(appId.value)}.zip`
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    URL.revokeObjectURL(downloadUrl)

    message.success('代码包下载成功')
  } catch (error) {
    console.error('下载代码失败', error)
    message.error('下载失败，请稍后重试')
  } finally {
    downloading.value = false
  }
}

const deployApp = async () => {
  if (!hasId(appId.value)) {
    message.error('应用 ID 不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: toIdString(appId.value),
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error(`部署失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('部署失败', error)
    message.error('部署失败，请稍后重试')
  } finally {
    deploying.value = false
  }
}

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement | null
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

const editApp = () => {
  if (hasId(appInfo.value?.id)) {
    router.push(`/app/edit/${toIdString(appInfo.value?.id)}`)
  }
}

const deleteApp = async () => {
  if (!hasId(appInfo.value?.id)) return

  try {
    const res = await deleteAppApi({ id: toIdString(appInfo.value?.id) })
    if (res.data.code === 0) {
      message.success('应用已删除')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error(`删除失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('删除应用失败', error)
    message.error('删除失败')
  }
}

const toggleEditMode = () => {
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement | null
  if (!iframe) {
    message.warning('请先生成并加载预览内容')
    return
  }

  if (!previewReady.value) {
    message.warning('预览尚未准备完成，请稍后再试')
    return
  }

  isEditMode.value = visualEditor.toggleEditMode()
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `描述你希望如何修改 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，例如调整文案、间距、颜色或布局。`
  }
  return '输入新的页面需求、交互说明或局部改版指令，例如：把首屏改成两栏结构，并加强主标题层级。'
}

onMounted(() => {
  fetchAppInfo()
  window.addEventListener('message', handleWindowMessage)
})

onUnmounted(() => {
  clearActiveStream()
  window.removeEventListener('message', handleWindowMessage)
  if (isEditMode.value) {
    visualEditor.disableEditMode()
  }
})
</script>

<style scoped>
.app-chat-page {
  padding-bottom: 24px;
}

.workspace-shell {
  display: grid;
  gap: 24px;
  padding: 24px;
}

.workspace-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.workspace-title__row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.workspace-title h1 {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: clamp(2rem, 3vw, 3rem);
  line-height: 1.08;
}

.workspace-title p {
  margin: 12px 0 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.workspace-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.92fr) minmax(420px, 1.08fr);
  gap: 20px;
  min-height: calc(100vh - 240px);
}

.conversation-panel,
.preview-panel {
  display: grid;
  grid-template-rows: auto 1fr auto;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
}

.preview-panel {
  grid-template-rows: auto 1fr;
}

.panel-header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 22px 18px;
  border-bottom: 1px solid var(--border-light);
}

.panel-header h2,
.composer-panel__header h3 {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: 1.5rem;
}

.panel-header p,
.composer-panel__header p {
  margin: 8px 0 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.messages-container {
  padding: 22px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.message-item + .message-item {
  margin-top: 18px;
}

.message-row {
  display: flex;
  align-items: start;
  gap: 10px;
}

.message-row--user {
  justify-content: end;
}

.message-row--ai {
  justify-content: start;
}

.message-bubble {
  max-width: min(88%, 760px);
  padding: 16px 18px;
  border-radius: 20px;
  line-height: 1.75;
}

.message-bubble--user {
  color: #fff;
  background: #0071e3;
}

.message-bubble--ai {
  color: var(--text-default);
  background: #fbfaf6;
  border: 1px solid var(--border-light);
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-subtle);
}

.selected-element-panel {
  padding: 18px 22px;
  border-top: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
  background: rgba(56, 152, 236, 0.06);
}

.selected-element-panel__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.selected-element-panel__header strong {
  color: var(--text-strong);
}

.selected-element-panel__header p,
.selected-element-meta p {
  margin: 6px 0 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.selected-element-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.selected-element-meta {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.selected-element-meta code {
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  padding: 6px 10px;
  overflow-x: auto;
  border-radius: 12px;
  background: rgba(20, 20, 19, 0.06);
  color: var(--text-strong);
  font-family: var(--font-mono);
}

.composer-panel {
  display: grid;
  gap: 16px;
  padding: 20px 22px 22px;
  background: #f8f7f3;
}

.composer-input {
  min-height: 138px;
}

.composer-panel__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.composer-hint {
  color: var(--text-subtle);
  font-size: 13px;
}

.preview-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.preview-action--active {
  color: #fff !important;
  border-color: var(--success) !important;
  background: var(--success) !important;
}

.preview-stage {
  position: relative;
  overflow: hidden;
  min-height: 420px;
  background: #f7f6f2;
}

.preview-placeholder,
.preview-loading {
  display: grid;
  place-items: center;
  align-content: center;
  height: 100%;
  padding: 40px;
  text-align: center;
}

.preview-placeholder strong {
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: 2rem;
}

.preview-placeholder p,
.preview-loading p {
  margin: 12px 0 0;
  color: var(--text-muted);
  line-height: 1.8;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}

@media (max-width: 1180px) {
  .workspace-header,
  .workspace-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .workspace-grid {
    min-height: auto;
  }

  .conversation-panel,
  .preview-panel {
    min-height: 520px;
  }
}

@media (max-width: 768px) {
  .workspace-shell {
    padding: 18px;
  }

  .panel-header,
  .messages-container,
  .composer-panel,
  .selected-element-panel {
    padding-inline: 18px;
  }

  .composer-panel__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .message-bubble {
    max-width: 100%;
  }
}
</style>
