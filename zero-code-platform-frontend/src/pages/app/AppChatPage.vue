<template>
  <div id="appChatPage" class="app-chat-page">
    <section class="workspace-shell">
      <header class="workspace-header">
        <div class="workspace-leading">
          <button type="button" class="workspace-brand" @click="goHome">
            <img class="workspace-brand__logo" :src="logoUrl" alt="返回首页" />
          </button>
          <div class="workspace-title-row">
            <h1>{{ appInfo?.appName || '应用工作台' }}</h1>
          </div>
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

      <div ref="workspaceMain" class="workspace-main" :style="workspaceMainStyle">
        <section ref="conversationPanel" class="conversation-panel">
          <div ref="messagesContainer" class="messages-container">
            <div v-if="hasMoreHistory" class="load-more-container">
              <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory">
                加载更早消息
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
                    <span>AI 正在生成中，请稍候...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedElementInfo" class="selected-element-panel">
            <div class="selected-element-panel__header">
              <div>
                <strong>当前选中元素</strong>
                <p>接下来的修改会优先围绕这个选中元素展开。</p>
              </div>
              <a-button size="small" @click="clearSelectedElement">清除选中</a-button>
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
            <div class="composer-input-shell">
              <a-textarea
                ref="composerTextarea"
                v-model:value="userInput"
                class="composer-input"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                :disabled="isGenerating || !isOwner"
                @keydown="handleComposerKeydown"
              />

              <a-button v-if="false"
                class="composer-edit-button"
                :class="{ 'composer-edit-button--active': isEditMode }"
                :disabled="!isOwner || !previewUrl"
                @click="toggleEditMode"
              >
                <template #icon>
                  <AimOutlined />
                </template>
                编辑
              </a-button>

              <div>
                <div class="composer-action-group">
                  <div class="composer-action-item">
                    <div class="composer-action-bubble">选择指定元素编辑</div>
                  <a-button
                    class="composer-edit-button"
                    :class="{ 'composer-edit-button--active': isEditMode }"
                    :disabled="!isOwner || !previewUrl || isGenerating || isSwitchingChatMode"
                    @mousedown.prevent
                    @click="toggleEditMode"
                  >
                    <template #icon>
                      <AimOutlined />
                    </template>
                    编辑
                  </a-button>
                  </div>

                  <div class="composer-action-item">
                    <div class="composer-action-bubble">仅对话，不对项目做修改</div>
                  <a-button
                    class="composer-mode-button"
                    :class="{ 'composer-mode-button--active': isConversationMode }"
                    :disabled="!isOwner || isGenerating || isSwitchingChatMode"
                    @mousedown.prevent
                    @click="toggleConversationMode"
                  >
                    <template #icon>
                      <CommentOutlined />
                    </template>
                    对话
                  </a-button>
                  </div>
                </div>

                <span class="composer-send-tooltip-anchor">
                  <button
                    type="button"
                    class="composer-send-button"
                    :class="{ 'composer-send-button--enabled': canTriggerComposerAction }"
                    :disabled="!canTriggerComposerAction"
                    @click="sendMessage"
                  >
                    <span
                      v-if="isGenerating"
                      class="composer-send-button__stop-icon"
                      aria-hidden="true"
                    ></span>
                    <ArrowUpOutlined v-else />
                  </button>
                </span>
              </div>
            </div>
          </div>
        </section>

        <button
          type="button"
          class="workspace-divider"
          aria-label="调整工作台宽度"
          @pointerdown="startResize"
        ></button>

        <section
          class="preview-panel"
          :class="{ 'preview-panel--with-files': generatedFiles.length > 0 }"
        >
          <div class="preview-stage">
            <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
              <strong>预览将在这里出现</strong>
              <p>先完成一次生成，随后就可以在这里直接查看页面效果并继续细化。</p>
            </div>
            <div v-else-if="isGenerating" class="preview-loading">
              <a-spin size="large" />
              <p>正在根据最新需求更新页面预览...</p>
            </div>
            <iframe
              v-else
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
              @load="onIframeLoad"
            ></iframe>
          </div>

          <aside v-if="generatedFiles.length" class="preview-file-rail">
            <details class="file-drawer" open>
              <summary>生成文件</summary>
              <div class="file-drawer__list">
                <article v-for="filePath in generatedFiles" :key="filePath" class="file-chip">
                  <strong>{{ getFileLabel(filePath) }}</strong>
                  <span>{{ filePath }}</span>
                </article>
              </div>
            </details>
          </aside>
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
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  AimOutlined,
  ArrowUpOutlined,
  CloudUploadOutlined,
  CommentOutlined,
  DownloadOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'
import aiAvatar from '@/assets/aiAvatar.png'
import logoUrl from '@/assets/logo.png'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import {
  deleteApp as deleteAppApi,
  deployApp as deployAppApi,
  getAppVoById,
  switchChatMode as switchChatModeApi,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import request from '@/request'
import { useLoginUserStore } from '@/stores/loginUser'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
import { hasId, sameId, toIdString } from '@/utils/id'
import { type ElementInfo, VisualEditor } from '@/utils/visualEditor'

interface MessageItem {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

type ChatMode = 'chat' | 'edit'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appInfo = ref<API.AppVO>()
const appId = ref<string>('')

const messages = ref<MessageItem[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const workspaceMain = ref<HTMLElement>()
const conversationPanel = ref<HTMLElement>()
const composerTextarea = ref<any>()

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
const activeStreamAbortController = ref<AbortController | null>(null)
const activeAiMessageIndex = ref<number | null>(null)
const isSwitchingChatMode = ref(false)

const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

const appDetailVisible = ref(false)

const DEFAULT_LEFT_PANEL_WIDTH = 440
const MIN_LEFT_PANEL_WIDTH = 360
const DIVIDER_WIDTH = 18
const MAX_LEFT_PANEL_RATIO = 0.45
const COMPOSER_MIN_HEIGHT = 138
const COMPOSER_MAX_HEIGHT_RATIO = 0.6
const CHAT_MODE_CONVERSATION: ChatMode = 'chat'
const CHAT_MODE_EDIT: ChatMode = 'edit'
const leftPanelWidth = ref(DEFAULT_LEFT_PANEL_WIDTH)
const isStackedLayout = ref(false)
const activePointerId = ref<number | null>(null)
const activeDivider = ref<HTMLElement | null>(null)
const chatMode = ref<ChatMode>(CHAT_MODE_EDIT)

const CHAT_ONLY_PATTERNS: RegExp[] = []

const EDIT_INTENT_PATTERNS: RegExp[] = []

const isOwner = computed(() => sameId(appInfo.value?.userId, loginUserStore.loginUser.id))
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const isConversationMode = computed(() => chatMode.value === CHAT_MODE_CONVERSATION)
const isChatOnlyMode = computed({
  get: () => isConversationMode.value,
  set: (value: boolean) => {
    chatMode.value = value ? CHAT_MODE_CONVERSATION : CHAT_MODE_EDIT
  },
})
const canTriggerComposerAction = computed(
  () => !isSwitchingChatMode.value && isOwner.value && (isGenerating.value || Boolean(userInput.value.trim()))
)
const canSendMessage = computed(
  () => Boolean(userInput.value.trim()) && isOwner.value && !isGenerating.value && !isSwitchingChatMode.value
)
const workspaceMainStyle = computed(() =>
  isStackedLayout.value
    ? undefined
    : {
        gridTemplateColumns: `${leftPanelWidth.value}px ${DIVIDER_WIDTH}px minmax(0, 1fr)`,
      }
)

const clampLeftPanelWidth = (nextWidth: number) => {
  const containerWidth = workspaceMain.value?.clientWidth || 0
  if (!containerWidth) {
    return Math.max(nextWidth, MIN_LEFT_PANEL_WIDTH)
  }

  const maxLeftWidth = Math.max(MIN_LEFT_PANEL_WIDTH, Math.floor(containerWidth * MAX_LEFT_PANEL_RATIO))
  return Math.min(Math.max(nextWidth, MIN_LEFT_PANEL_WIDTH), maxLeftWidth)
}

const syncLeftPanelWidth = () => {
  isStackedLayout.value = window.innerWidth <= 1100
  if (!workspaceMain.value || isStackedLayout.value) return
  leftPanelWidth.value = clampLeftPanelWidth(leftPanelWidth.value || DEFAULT_LEFT_PANEL_WIDTH)
}

const stopResize = () => {
  window.removeEventListener('pointermove', handleResize)
  window.removeEventListener('pointerup', stopResize)
  window.removeEventListener('pointercancel', stopResize)

  if (
    activeDivider.value &&
    activePointerId.value !== null &&
    activeDivider.value.hasPointerCapture(activePointerId.value)
  ) {
    activeDivider.value.releasePointerCapture(activePointerId.value)
  }

  activeDivider.value = null
  activePointerId.value = null
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

const handleResize = (event: PointerEvent) => {
  if (activePointerId.value !== null && event.pointerId !== activePointerId.value) return
  if (!workspaceMain.value) return
  const bounds = workspaceMain.value.getBoundingClientRect()
  const nextWidth = event.clientX - bounds.left - DIVIDER_WIDTH / 2
  leftPanelWidth.value = clampLeftPanelWidth(nextWidth)
}

const startResize = (event: PointerEvent) => {
  if (isStackedLayout.value) return

  event.preventDefault()
  activePointerId.value = event.pointerId
  activeDivider.value = event.currentTarget as HTMLElement
  activeDivider.value?.setPointerCapture(event.pointerId)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  handleResize(event)
  window.addEventListener('pointermove', handleResize)
  window.addEventListener('pointerup', stopResize)
  window.addEventListener('pointercancel', stopResize)
}

const extractGeneratedFiles = (content: string) => {
  const filePattern =
    /(^|[\s(>])((?:[\w.-]+\/)*[\w.-]+\.(?:vue|jsx|tsx|js|ts|css|scss|less|html|json|md))(?![\w/.-])/gm
  const files: string[] = []
  let match: RegExpExecArray | null

  while ((match = filePattern.exec(content)) !== null) {
    files.push(match[2])
  }

  return files
}

const generatedFiles = computed(() => {
  const seen = new Set<string>()
  const files: string[] = []

  for (const messageItem of messages.value) {
    if (messageItem.type !== 'ai' || !messageItem.content) continue

    for (const filePath of extractGeneratedFiles(messageItem.content)) {
      if (!seen.has(filePath)) {
        seen.add(filePath)
        files.push(filePath)
      }
    }
  }

  return files.slice(0, 24)
})

const getFileLabel = (filePath: string) => {
  const segments = filePath.split('/')
  return segments[segments.length - 1] || filePath
}

const getComposerNativeTextarea = () =>
  composerTextarea.value?.resizableTextArea?.textArea as HTMLTextAreaElement | undefined

const syncComposerTextareaHeight = async () => {
  await nextTick()

  const textarea = getComposerNativeTextarea()
  if (!textarea) return

  const panelHeight = conversationPanel.value?.clientHeight || window.innerHeight
  const maxHeight = Math.max(COMPOSER_MIN_HEIGHT, Math.floor(panelHeight * COMPOSER_MAX_HEIGHT_RATIO))

  textarea.style.height = 'auto'
  textarea.style.maxHeight = `${maxHeight}px`

  const nextHeight = Math.max(COMPOSER_MIN_HEIGHT, Math.min(textarea.scrollHeight, maxHeight))
  textarea.style.height = `${nextHeight}px`
  textarea.style.overflowY = textarea.scrollHeight > maxHeight ? 'auto' : 'hidden'
}

const handleComposerKeydown = (event: KeyboardEvent) => {
  if (event.key !== 'Enter' || event.isComposing) return
  if (event.shiftKey) return

  event.preventDefault()
  sendMessage()
}

const goHome = () => {
  router.push('/')
}

const getRouteAppId = () => {
  const rawId = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id
  return toIdString(rawId)
}

const stripNoChangePhrases = (input: string) => input







const isChatOnlyIntent = (_input: string) => false

const isEditIntent = (_input: string) => false

const clearEditingContext = () => {
  if (selectedElementInfo.value) {
    clearSelectedElement()
  }

  if (isEditMode.value) {
    toggleEditMode()
  }
}

const persistChatMode = async (targetMode: ChatMode) => {
  const currentAppId = toIdString(appId.value)
  if (!currentAppId) {
    throw new Error('应用 ID 不存在')
  }

  const res = await switchChatModeApi({
    appId: currentAppId,
    targetMode,
  })

  if (res.data.code !== 0 || !res.data.data) {
    throw new Error(res.data.message || '切换聊天模式失败')
  }
}

const toggleConversationMode = async () => {
  if (!isOwner.value || isGenerating.value || isSwitchingChatMode.value) {
    return
  }

  const nextMode = isConversationMode.value ? CHAT_MODE_EDIT : CHAT_MODE_CONVERSATION
  isSwitchingChatMode.value = true
  try {
    await persistChatMode(nextMode)
    chatMode.value = nextMode

    if (nextMode === CHAT_MODE_CONVERSATION) {
      clearEditingContext()
    }
  } catch (error) {
    console.error('切换聊天模式失败', error)
    message.error(error instanceof Error ? error.message : '切换聊天模式失败')
  } finally {
    isSwitchingChatMode.value = false
  }
}

const appendLocalAiMessage = async (content: string) => {
  messages.value.push({
    type: 'ai',
    content,
  })
  await scrollToBottom()
}

const buildGenerationMessage = (input: string) => {
  const trimmedInput = input.trim()
  if (!trimmedInput) {
    return ''
  }

  return trimmedInput
}

const buildPreviewUrl = () => {
  const currentAppId = toIdString(appId.value)
  if (!currentAppId) {
    return ''
  }

  const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
  const basePreviewUrl = getStaticPreviewUrl(codeGenType, currentAppId)
  const separator = basePreviewUrl.includes('?') ? '&' : '?'
  return `${basePreviewUrl}${separator}previewAt=${Date.now()}`
}

const handleWindowMessage = (event: MessageEvent) => {
  visualEditor.handleIframeMessage(event)
}

const clearActiveStream = () => {
  if (activeStreamAbortController.value) {
    activeStreamAbortController.value.abort()
    activeStreamAbortController.value = null
  }

  if (activeEventSource.value) {
    activeEventSource.value.close()
    activeEventSource.value = null
  }
}

const finishGeneratingState = () => {
  isGenerating.value = false
  activeAiMessageIndex.value = null
}

const stopGeneration = () => {
  if (!isGenerating.value) {
    return
  }

  const currentAiMessageIndex = activeAiMessageIndex.value
  if (currentAiMessageIndex !== null && messages.value[currentAiMessageIndex]) {
    const currentMessage = messages.value[currentAiMessageIndex]
    currentMessage.loading = false
    if (!currentMessage.content.trim()) {
      currentMessage.content = '已手动停止生成。'
    }
  }

  clearActiveStream()
  finishGeneratingState()
  message.info('已停止当前生成')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
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
    console.error('加载聊天历史失败', error)
    message.error('加载聊天历史失败')
  } finally {
    loadingHistory.value = false
  }
}

const loadMoreHistory = async () => {
  const container = messagesContainer.value
  const previousHeight = container?.scrollHeight || 0
  const previousScrollTop = container?.scrollTop || 0

  await loadChatHistory(true)
  await nextTick()

  if (container) {
    container.scrollTop = container.scrollHeight - previousHeight + previousScrollTop
  }
}

const fetchAppInfo = async (reloadHistory = true) => {
  const id = getRouteAppId()
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

      if (reloadHistory) {
        await loadChatHistory()
        await scrollToBottom()
      }

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
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败', error)
    message.error('获取应用信息失败')
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

  await scrollToBottom()

  activeAiMessageIndex.value = aiMessageIndex
  isGenerating.value = true
  await streamChatMessage(buildGenerationMessage(prompt), aiMessageIndex, CHAT_MODE_EDIT)
}

const legacySendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value || !isOwner.value) {
    return
  }

  const rawInput = userInput.value.trim()
  const chatOnlyIntent = isChatOnlyIntent(rawInput)
  const editIntent = isEditIntent(rawInput)

  if (chatOnlyIntent) {
    isChatOnlyMode.value = true
    userInput.value = ''
    messages.value.push({
      type: 'user',
      content: rawInput,
    })
    clearEditingContext()
    await scrollToBottom()
    await appendLocalAiMessage(
      '当前已开启仅对话模式，这条消息不会修改应用。'
    )
    return
  }

  if (isChatOnlyMode.value && !editIntent) {
    userInput.value = ''
    messages.value.push({
      type: 'user',
      content: rawInput,
    })
    await scrollToBottom()
    await appendLocalAiMessage(
      '当前仍处于仅对话模式，发送修改指令后可切回编辑模式。'
    )
    return
  }

  if (isChatOnlyMode.value && editIntent) {
    isChatOnlyMode.value = false
  }

  let content = rawInput
  if (selectedElementInfo.value) {
    let elementContext = '\n\n请优先围绕当前选中的页面元素进行修改：'
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面：${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签：${selectedElementInfo.value.tagName.toLowerCase()}`
    elementContext += `\n- 选择器：${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 文本：${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    content += elementContext
  }

  userInput.value = ''
  messages.value.push({
    type: 'user',
    content,
  })

  clearEditingContext()

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await scrollToBottom()

  isGenerating.value = true
  await streamChatMessage(buildGenerationMessage(content), aiMessageIndex, CHAT_MODE_EDIT)
}

const sendMessage = async () => {
  if (isGenerating.value) {
    stopGeneration()
    return
  }

  if (!userInput.value.trim() || !isOwner.value || isSwitchingChatMode.value) {
    return
  }

  const chatType = chatMode.value
  let content = userInput.value.trim()

  if (chatType === CHAT_MODE_EDIT && selectedElementInfo.value) {
    let elementContext = '\n\n请优先围绕当前选中的页面元素进行修改：'
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面：${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签：${selectedElementInfo.value.tagName.toLowerCase()}`
    elementContext += `\n- 选择器：${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 文本：${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    content += elementContext
  }

  userInput.value = ''
  messages.value.push({
    type: 'user',
    content,
  })

  clearEditingContext()

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await scrollToBottom()

  activeAiMessageIndex.value = aiMessageIndex
  isGenerating.value = true
  const userMessage = chatType === CHAT_MODE_CONVERSATION ? content : buildGenerationMessage(content)
  await streamChatMessage(userMessage, aiMessageIndex, chatType)
}

const generateCode = async (userMessage: string, aiMessageIndex: number, chatType: ChatMode) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const params = new URLSearchParams({
      appId: toIdString(appId.value),
      message: userMessage,
      chatMode: chatType,
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
        }
      } catch (error) {
        console.error('解析响应消息失败', error)
        handleError(error, aiMessageIndex)
      }
    }

    const refreshPreviewIfNeeded = () => {
      if (chatType !== CHAT_MODE_EDIT) {
        return
      }

      setTimeout(async () => {
        await fetchAppInfo(false)
        updatePreview()
      }, 1000)
    }

    eventSource.addEventListener('done', () => {
      if (streamCompleted) return

      streamCompleted = true
      clearActiveStream()
      finishGeneratingState()
      refreshPreviewIfNeeded()
    })

    eventSource.addEventListener('business-error', (event: MessageEvent) => {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        const errorMessage = errorData.message || '生成失败'
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].content = `错误：${errorMessage}`
          messages.value[aiMessageIndex].loading = false
        }
        message.error(errorMessage)

        streamCompleted = true
        clearActiveStream()
        finishGeneratingState()
      } catch (parseError) {
        console.error('解析服务端错误信息失败', parseError, event.data)
        handleError(new Error('解析服务端错误信息失败'), aiMessageIndex)
      }
    })

    eventSource.onerror = () => {
      if (streamCompleted || !isGenerating.value) return

      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        clearActiveStream()
        finishGeneratingState()
        refreshPreviewIfNeeded()
      } else {
        handleError(new Error('SSE 连接失败'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 SSE 连接失败', error)
    handleError(error, aiMessageIndex)
  }
}

const setAiMessageError = (aiMessageIndex: number, errorText: string) => {
  if (!messages.value[aiMessageIndex]) {
    return
  }

  messages.value[aiMessageIndex].content = `错误：${errorText}`
  messages.value[aiMessageIndex].loading = false
}

const parseSseEvent = (eventBlock: string) => {
  const lines = eventBlock.split('\n')
  let eventName = 'message'
  const dataLines: string[] = []

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim() || 'message'
      continue
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }

  return {
    event: eventName,
    data: dataLines.join('\n'),
  }
}

const FILE_PATH_PATTERN =
  /(^|[\s(>])((?:[\w.-]+\/)*[\w.-]+\.(?:vue|jsx|tsx|js|ts|css|scss|less|html|json|md))(?![\w/.-])/im

const CODE_SIGNAL_PATTERN =
  /```(?:html|css|scss|less|js|javascript|ts|tsx|jsx|vue|json)?|<!doctype|<html|<body|<head|<template|<script|<style|\[工具调用\]/i

const shouldRefreshPreviewAfterEdit = (aiContent: string) => {
  if (!aiContent.trim()) {
    return false
  }

  return CODE_SIGNAL_PATTERN.test(aiContent) || FILE_PATH_PATTERN.test(aiContent)
}

const readUnexpectedResponseMessage = async (response: Response) => {
  const contentType = response.headers.get('content-type') || ''

  try {
    if (contentType.includes('application/json')) {
      const data = await response.json()
      if (data?.message) {
        return String(data.message)
      }
    }

    const text = await response.text()
    return text || `请求失败（${response.status}）`
  } catch {
    return `请求失败（${response.status}）`
  }
}

const refreshPreviewAfterEdit = (chatType: ChatMode, aiContent: string) => {
  if (chatType !== CHAT_MODE_EDIT) {
    return
  }

  if (!shouldRefreshPreviewAfterEdit(aiContent)) {
    return
  }

  setTimeout(async () => {
    await fetchAppInfo(false)
    updatePreview()
  }, 1000)
}

const streamChatMessage = async (userMessage: string, aiMessageIndex: number, chatType: ChatMode) => {
  let streamCompleted = false
  const abortController = new AbortController()
  activeStreamAbortController.value = abortController

  try {
    await persistChatMode(chatType)

    const currentAppId = toIdString(appId.value)
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const endpoint = chatType === CHAT_MODE_CONVERSATION ? '/app/chat/discuss' : '/app/chat/edit'
    const response = await fetch(`${baseURL}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      credentials: 'include',
      body: JSON.stringify({
        appId: currentAppId,
        message: userMessage,
      }),
      signal: abortController.signal,
    })

    const contentType = response.headers.get('content-type') || ''
    if (!contentType.includes('text/event-stream')) {
      throw new Error(await readUnexpectedResponseMessage(response))
    }

    if (!response.body) {
      throw new Error('未收到流式响应内容')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let fullContent = ''
    let buffer = ''
    let shouldStopReading = false

    while (!shouldStopReading) {
      const { value, done } = await reader.read()
      if (done) {
        break
      }

      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')

      let separatorIndex = buffer.indexOf('\n\n')
      while (separatorIndex !== -1) {
        const eventBlock = buffer.slice(0, separatorIndex).trim()
        buffer = buffer.slice(separatorIndex + 2)

        if (eventBlock) {
          const sseEvent = parseSseEvent(eventBlock)

          if (sseEvent.event === 'message') {
            try {
              const parsed = JSON.parse(sseEvent.data)
              const content = parsed.d
              if (content !== undefined && content !== null && messages.value[aiMessageIndex]) {
                fullContent += content
                messages.value[aiMessageIndex].content = fullContent
                messages.value[aiMessageIndex].loading = false
              }
            } catch (error) {
              console.error('解析响应消息失败', error)
              handleError(error, aiMessageIndex)
              shouldStopReading = true
            }
          }

          if (sseEvent.event === 'business-error') {
            try {
              const errorData = JSON.parse(sseEvent.data)
              const errorMessage = errorData.message || '生成失败'
              setAiMessageError(aiMessageIndex, errorMessage)
              message.error(errorMessage)
              finishGeneratingState()
              streamCompleted = true
              shouldStopReading = true
            } catch (error) {
              console.error('解析服务端错误信息失败', error, sseEvent.data)
              handleError(new Error('解析服务端错误信息失败'), aiMessageIndex)
              shouldStopReading = true
            }
          }

          if (sseEvent.event === 'done') {
            finishGeneratingState()
            refreshPreviewAfterEdit(chatType, fullContent)
            streamCompleted = true
            shouldStopReading = true
          }
        }

        if (shouldStopReading) {
          await reader.cancel()
          break
        }

        separatorIndex = buffer.indexOf('\n\n')
      }
    }

    if (!streamCompleted && isGenerating.value) {
      finishGeneratingState()
      refreshPreviewAfterEdit(chatType, fullContent)
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      return
    }

    console.error('创建流式连接失败', error)
    const errorMessage = error instanceof Error ? error.message : '创建流式连接失败'
    setAiMessageError(aiMessageIndex, errorMessage)
    message.error(errorMessage)
    clearActiveStream()
    finishGeneratingState()
  } finally {
    if (activeStreamAbortController.value === abortController) {
      activeStreamAbortController.value = null
    }
  }
}

const legacyGenerateCode = async (userMessage: string, aiMessageIndex: number) => {
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
        }
      } catch (error) {
        console.error('解析服务端错误信息失败', error)
        handleError(error, aiMessageIndex)
      }
    }

    eventSource.addEventListener('done', async () => {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      clearActiveStream()

      setTimeout(async () => {
        await fetchAppInfo(false)
        updatePreview()
      }, 1000)
    })

    eventSource.addEventListener('business-error', (event: MessageEvent) => {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        const errorMessage = errorData.message || '生成失败'
        if (messages.value[aiMessageIndex]) {
          messages.value[aiMessageIndex].content = `错误：${errorMessage}`
          messages.value[aiMessageIndex].loading = false
        }
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        clearActiveStream()
      } catch (parseError) {
        console.error('解析服务端错误信息失败', parseError, event.data)
        handleError(new Error('解析服务端错误信息失败'), aiMessageIndex)
      }
    })

    eventSource.onerror = () => {
      if (streamCompleted || !isGenerating.value) return

      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        clearActiveStream()

        setTimeout(async () => {
          await fetchAppInfo(false)
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE 连接失败'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 SSE 连接失败', error)
    handleError(error, aiMessageIndex)
  }
}

const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成失败', error)
  if (messages.value[aiMessageIndex]) {
    messages.value[aiMessageIndex].content =
      '生成过程中出现了一点问题，请稍后重试。'
    messages.value[aiMessageIndex].loading = false
  }
  message.error('生成失败，请稍后重试')
  clearActiveStream()
  finishGeneratingState()
}

const updatePreview = () => {
  const nextPreviewUrl = buildPreviewUrl()
  if (!nextPreviewUrl) return

  previewUrl.value = nextPreviewUrl
  previewReady.value = false
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
    const downloadLink = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadLink
    link.download = fileName
    link.click()
    URL.revokeObjectURL(downloadLink)

    message.success('代码下载成功')
  } catch (error) {
    console.error('下载代码失败', error)
    message.error('下载代码失败')
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
    message.error('部署失败')
  } finally {
    deploying.value = false
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
      message.success('应用删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error(`删除失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('删除应用失败', error)
    message.error('删除应用失败')
  }
}

const toggleEditMode = () => {
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement | null
  if (!iframe) {
    message.warning('请先生成并加载预览内容')
    return
  }

  if (!previewReady.value) {
    message.warning('预览仍在加载中，请稍后再试')
    return
  }

  isEditMode.value = visualEditor.toggleEditMode()
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (isChatOnlyMode.value) {
    return '当前处于仅对话模式，发送消息不会修改应用。'
  }

  if (selectedElementInfo.value) {
    return `请描述你希望如何修改当前选中的 ${selectedElementInfo.value.tagName.toLowerCase()} 元素`
  }

  return isOwner.value
    ? '请描述下一步你希望对页面进行的修改或细化需求'
    : '当前应用对你是只读状态，无法继续发送指令'
}

onMounted(() => {
  fetchAppInfo()
  nextTick(() => {
    syncLeftPanelWidth()
    syncComposerTextareaHeight()
  })
  window.addEventListener('message', handleWindowMessage)
  window.addEventListener('resize', syncLeftPanelWidth)
  window.addEventListener('resize', syncComposerTextareaHeight)
})

watch(userInput, () => {
  syncComposerTextareaHeight()
})

onUnmounted(() => {
  clearActiveStream()
  stopResize()
  window.removeEventListener('message', handleWindowMessage)
  window.removeEventListener('resize', syncLeftPanelWidth)
  window.removeEventListener('resize', syncComposerTextareaHeight)
  if (isEditMode.value) {
    visualEditor.disableEditMode()
  }
})
</script>

<style scoped>
.app-chat-page {
  width: 100%;
  height: 100svh;
  background: #ffffff;
}

.workspace-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  width: 100%;
  height: 100%;
}

.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 14px 22px;
  border-bottom: 1px solid rgba(32, 35, 41, 0.08);
  background: #ffffff;
  backdrop-filter: blur(18px);
}

.workspace-leading {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.workspace-brand {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.workspace-brand__logo {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 14px;
  box-shadow: none;
}

.workspace-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex-wrap: wrap;
}

.workspace-title-row h1 {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: 1.55rem;
  line-height: 1.1;
}

.workspace-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.workspace-main {
  display: grid;
  grid-template-columns: minmax(360px, 440px) 12px minmax(0, 1fr);
  min-height: 0;
}

.conversation-panel {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto auto;
  min-height: 0;
  background: #ffffff;
}

.workspace-divider {
  position: relative;
  padding: 0;
  border: none;
  background: transparent;
  cursor: col-resize;
  touch-action: none;
  outline: none;
  z-index: 1;
}

.workspace-divider::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 1px;
  background: rgba(32, 35, 41, 0.12);
  transform: translateX(-50%);
}

.workspace-divider::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 6px;
  height: 84px;
  border-radius: 999px;
  background: rgba(32, 35, 41, 0.12);
  transform: translate(-50%, -50%);
  transition: background-color 0.2s ease;
}

.workspace-divider:hover::after,
.workspace-divider:focus-visible::after {
  background: rgba(20, 20, 19, 0.18);
}

.messages-container {
  min-height: 0;
  padding: 20px 20px 18px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
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
  align-items: flex-start;
  gap: 12px;
}

.message-row--user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(92%, 680px);
  padding: 16px 18px;
  border-radius: 18px;
  line-height: 1.75;
}

.message-bubble--user {
  color: var(--text-strong);
  background: #d9d9d9;
}

.message-bubble--ai {
  color: var(--text-default);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(32, 35, 41, 0.08);
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
  padding: 16px 20px;
  border-top: 1px solid rgba(32, 35, 41, 0.08);
  background: rgba(20, 20, 19, 0.03);
}

.selected-element-panel__header {
  display: flex;
  align-items: flex-start;
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
  border-radius: 10px;
  background: rgba(20, 20, 19, 0.06);
  color: var(--text-strong);
  font-family: var(--font-mono);
}

.composer-panel {
  display: grid;
  gap: 12px;
  padding: 16px 20px 18px;
  border-top: 1px solid rgba(32, 35, 41, 0.08);
  background: #ffffff;
}

.composer-input-shell {
  position: relative;
}

.composer-action-group {
  position: absolute;
  bottom: 12px;
  left: 12px;
  z-index: 1;
  display: inline-flex;
  align-items: flex-end;
  gap: 12px;
}

.composer-action-item {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

.composer-action-bubble {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  padding: 7px 12px;
  border-radius: 8px;
  background: #2d2f33;
  color: #fff;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
  box-shadow: 0 8px 18px rgba(20, 20, 19, 0.18);
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transform: translate(-50%, 6px);
  transition:
    opacity 0.18s ease,
    transform 0.18s ease,
    visibility 0.18s ease;
}

.composer-action-bubble::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  width: 0;
  height: 0;
  border-top: 7px solid #2d2f33;
  border-right: 7px solid transparent;
  border-left: 7px solid transparent;
  transform: translateX(-50%);
}

.composer-action-item:hover .composer-action-bubble,
.composer-action-item:focus-within .composer-action-bubble {
  opacity: 1;
  visibility: visible;
  transform: translate(-50%, 0);
}

.composer-input-shell::after {
  content: '';
  position: absolute;
  top: 12px;
  right: 12px;
  width: 12px;
  height: 12px;
  pointer-events: none;
  background:
    linear-gradient(135deg, transparent 0 48%, rgba(32, 35, 41, 0.34) 48% 54%, transparent 54%),
    linear-gradient(135deg, transparent 0 66%, rgba(32, 35, 41, 0.22) 66% 72%, transparent 72%),
    linear-gradient(135deg, transparent 0 84%, rgba(32, 35, 41, 0.14) 84% 90%, transparent 90%);
  transform: scaleX(-1);
}

.composer-input {
  min-height: 138px;
}

.composer-input-shell :deep(.ant-input) {
  resize: none;
  padding-right: 58px;
  padding-bottom: 56px;
  min-height: 138px;
  overflow-y: hidden;
  scrollbar-gutter: stable;
  transition: height 0.12s ease;
}

.composer-edit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 30px;
  padding-inline: 11px;
  border-color: rgba(32, 35, 41, 0.12);
  border-radius: 999px;
  color: var(--text-strong);
  font-size: 13px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 12px rgba(20, 20, 19, 0.06);
  transition:
    border-color 0.18s ease,
    color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
  -webkit-tap-highlight-color: transparent;
}

.composer-edit-button :deep(.anticon) {
  font-size: 12px;
}

.composer-mode-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 30px;
  padding-inline: 11px;
  border-color: rgba(32, 35, 41, 0.12);
  border-radius: 999px;
  color: var(--text-strong);
  font-size: 13px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 12px rgba(20, 20, 19, 0.06);
  transition:
    border-color 0.18s ease,
    color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
  -webkit-tap-highlight-color: transparent;
}

.composer-mode-button :deep(.anticon) {
  font-size: 12px;
}

.composer-edit-button:not(:disabled):hover,
.composer-mode-button:not(:disabled):hover,
.composer-edit-button:not(:disabled):focus,
.composer-edit-button:not(:disabled):focus-visible,
.composer-mode-button:not(:disabled):focus,
.composer-mode-button:not(:disabled):focus-visible,
.composer-edit-button:not(:disabled):active,
.composer-mode-button:not(:disabled):active,
:deep(.composer-edit-button.ant-btn-default:not(:disabled):hover),
:deep(.composer-edit-button.ant-btn-default:not(:disabled):focus),
:deep(.composer-edit-button.ant-btn-default:not(:disabled):focus-visible),
:deep(.composer-edit-button.ant-btn-default:not(:disabled):active),
:deep(.composer-mode-button.ant-btn-default:not(:disabled):hover),
:deep(.composer-mode-button.ant-btn-default:not(:disabled):focus),
:deep(.composer-mode-button.ant-btn-default:not(:disabled):focus-visible),
:deep(.composer-mode-button.ant-btn-default:not(:disabled):active) {
  border-color: rgba(32, 35, 41, 0.12);
  color: var(--text-strong);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 12px rgba(20, 20, 19, 0.06);
  outline: none;
}

.composer-edit-button--active,
.composer-mode-button--active,
.composer-edit-button--active:not(:disabled):hover,
.composer-edit-button--active:not(:disabled):focus,
.composer-edit-button--active:not(:disabled):focus-visible,
.composer-edit-button--active:not(:disabled):active,
.composer-mode-button--active:not(:disabled):hover,
.composer-mode-button--active:not(:disabled):focus,
.composer-mode-button--active:not(:disabled):focus-visible,
.composer-mode-button--active:not(:disabled):active,
:deep(.composer-edit-button--active.ant-btn-default:not(:disabled):hover),
:deep(.composer-edit-button--active.ant-btn-default:not(:disabled):focus),
:deep(.composer-edit-button--active.ant-btn-default:not(:disabled):focus-visible),
:deep(.composer-edit-button--active.ant-btn-default:not(:disabled):active),
:deep(.composer-mode-button--active.ant-btn-default:not(:disabled):hover),
:deep(.composer-mode-button--active.ant-btn-default:not(:disabled):focus),
:deep(.composer-mode-button--active.ant-btn-default:not(:disabled):focus-visible),
:deep(.composer-mode-button--active.ant-btn-default:not(:disabled):active) {
  border-color: #5ed9d3;
  color: #18b8b1;
  background: #effcfb;
  box-shadow: 0 4px 12px rgba(94, 217, 211, 0.18);
  outline: none;
}

.composer-send-tooltip-anchor {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 1;
  display: inline-flex;
}

.composer-send-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.96);
  background: #c6cad1;
  box-shadow: 0 8px 18px rgba(20, 20, 19, 0.08);
  cursor: not-allowed;
  transition:
    transform 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.composer-send-button--enabled {
  background: #171f2f;
  cursor: pointer;
}

.composer-send-button--enabled:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgba(23, 31, 47, 0.22);
}

.composer-send-button:disabled {
  pointer-events: none;
}

.composer-send-button__stop-icon {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.96);
}

.composer-send-button :deep(.anticon),
.composer-send-button :deep(.ant-spin) {
  font-size: 16px;
}

.preview-panel {
  display: grid;
  min-height: 0;
  background: rgba(255, 255, 255, 0.94);
}

.preview-panel--with-files {
  grid-template-columns: minmax(0, 1fr) 180px;
}

.preview-stage {
  position: relative;
  min-height: 0;
  overflow: hidden;
  background: #fff;
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

.preview-file-rail {
  overflow-y: auto;
  padding: 16px 14px;
  border-left: 1px solid rgba(32, 35, 41, 0.08);
  background: #ffffff;
}

.file-drawer {
  display: grid;
  gap: 12px;
}

.file-drawer summary {
  cursor: pointer;
  color: var(--text-strong);
  font-size: 0.95rem;
  font-weight: 700;
  list-style: none;
}

.file-drawer summary::-webkit-details-marker {
  display: none;
}

.file-drawer__list {
  display: grid;
  gap: 10px;
}

.file-chip {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid rgba(32, 35, 41, 0.08);
  border-radius: 14px;
  background: #fff;
}

.file-chip strong {
  color: var(--text-strong);
  font-size: 0.92rem;
  line-height: 1.35;
}

.file-chip span {
  color: var(--text-subtle);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

@media (max-width: 1280px) {
  .workspace-main {
    grid-template-columns: minmax(320px, 400px) 12px minmax(0, 1fr);
  }
}

@media (max-width: 1100px) {
  .app-chat-page {
    height: auto;
    min-height: 100svh;
  }

  .workspace-main {
    grid-template-columns: 1fr;
  }

  .conversation-panel {
    min-height: 620px;
    border-bottom: 1px solid rgba(32, 35, 41, 0.08);
  }

  .workspace-divider {
    display: none;
  }

  .preview-panel {
    min-height: 680px;
  }
}

@media (max-width: 768px) {
  .workspace-header {
    flex-direction: column;
    align-items: stretch;
  }

  .workspace-actions {
    justify-content: flex-start;
  }

  .messages-container,
  .selected-element-panel,
  .composer-panel {
    padding-inline: 16px;
  }

  .message-bubble {
    max-width: 100%;
  }

  .preview-panel--with-files {
    grid-template-columns: 1fr;
  }

  .preview-file-rail {
    border-top: 1px solid rgba(32, 35, 41, 0.08);
    border-left: none;
  }
}
</style>
