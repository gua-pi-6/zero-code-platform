<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowUpOutlined } from '@ant-design/icons-vue'
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import AppCard from '@/components/AppCard.vue'
import { getDeployUrl } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'
import { hasId, toIdString } from '@/utils/id'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const userPrompt = ref('')
const creating = ref(false)

const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 12,
  total: 0,
})

const heroSuggestions = ['波普风电商页面', '企业网站', '电商运营后台', '暗黑话题社区']
const placeholderWorks = Array.from({ length: 8 }, (_, index) => index)

const setPrompt = (prompt: string) => {
  userPrompt.value = `使用 NoCode 创建一个${prompt}，整体视觉精致，保留完整业务逻辑，并支持继续迭代。`
}

const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请先输入你的应用需求')
    return
  }

  if (!hasId(loginUserStore.loginUser.id)) {
    message.warning('请先登录后再创建应用')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      await router.push(`/app/chat/${toIdString(res.data.data)}`)
    } else {
      message.error(`创建失败：${res.data.message}`)
    }
  } catch (error) {
    console.error('创建应用失败', error)
    message.error('创建失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

const handlePromptKeydown = (event: KeyboardEvent) => {
  if (event.key !== 'Enter' || event.isComposing) {
    return
  }

  if (event.shiftKey) {
    return
  }

  event.preventDefault()
  createApp()
}

const loadMyApps = async () => {
  if (!hasId(loginUserStore.loginUser.id)) {
    myApps.value = []
    myAppsPage.total = 0
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = (res.data.data.records || []).slice(0, 10)
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的作品失败', error)
  }
}

const loadFeaturedApps = async (append = false) => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      featuredApps.value = append ? [...featuredApps.value, ...records] : records
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载案例广场失败', error)
  }
}

const loadMoreFeatured = async () => {
  if (featuredApps.value.length >= featuredAppsPage.total) {
    return
  }

  featuredAppsPage.current += 1
  await loadFeaturedApps(true)
}

const viewChat = (appId: string | undefined) => {
  if (appId) {
    router.push(`/app/chat/${toIdString(appId)}?view=1`)
  }
}

const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    window.open(getDeployUrl(app.deployKey), '_blank')
  }
}

watch(
  () => loginUserStore.loginUser.id,
  () => {
    myAppsPage.current = 1
    loadMyApps()
  },
)

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<template>
  <div class="home-page">
    <section class="home-hero">
      <div class="page-shell page-shell--wide hero-shell">
        <div class="hero-copy">
          <h1 class="hero-title">
            一句话
            <img class="hero-title__mark" src="@/assets/logo.png" alt="NoCode" />
            呈所想
          </h1>
          <p class="hero-subtitle">与 AI 对话轻松创建应用和网站</p>
        </div>

        <div class="hero-composer">
          <a-textarea
            v-model:value="userPrompt"
            class="prompt-input"
            placeholder="使用 NoCode 创建一个高效的小型业务应用、精美网站或运营后台。"
            :auto-size="{ minRows: 5, maxRows: 7 }"
            :maxlength="1000"
            @keydown="handlePromptKeydown"
          />

          <div class="composer-footer">
            <button type="button" class="composer-submit" :disabled="creating" @click="createApp">
              <ArrowUpOutlined />
            </button>
          </div>
        </div>

        <div class="hero-suggestions">
          <button
            v-for="suggestion in heroSuggestions"
            :key="suggestion"
            type="button"
            class="hero-chip"
            @click="setPrompt(suggestion)"
          >
            {{ suggestion }}
          </button>
        </div>
      </div>
    </section>

    <section class="page-shell page-shell--wide showcase-section">
      <div class="showcase-panel">
        <div class="showcase-block">
          <div class="section-header">
            <h2>我的作品</h2>
          </div>

          <div v-if="myApps.length" class="works-grid">
            <AppCard
              v-for="app in myApps"
              :key="app.id"
              :app="app"
              variant="workspace"
              @view-chat="viewChat"
              @view-work="viewWork"
            />
          </div>

          <div v-else class="works-grid works-grid--placeholder">
            <div v-for="item in placeholderWorks" :key="item" class="placeholder-card">
              <div class="placeholder-card__visual">
                <div class="placeholder-card__line placeholder-card__line--short"></div>
                <div class="placeholder-card__layout">
                  <div class="placeholder-card__avatar"></div>
                  <div class="placeholder-card__content">
                    <div class="placeholder-card__line"></div>
                    <div class="placeholder-card__line"></div>
                    <div class="placeholder-card__line placeholder-card__line--mini"></div>
                    <div class="placeholder-card__block"></div>
                  </div>
                </div>
              </div>
              <strong>新会话</strong>
              <span>创建于 刚刚</span>
            </div>
          </div>
        </div>

        <div class="showcase-block showcase-block--gallery">
          <div class="section-header">
            <h2>案例广场</h2>
          </div>

          <div v-if="featuredApps.length" class="gallery-grid">
            <AppCard
              v-for="app in featuredApps"
              :key="app.id"
              :app="app"
              featured
              variant="gallery"
              @view-chat="viewChat"
              @view-work="viewWork"
            />
          </div>

          <div v-else class="surface-panel empty-state gallery-empty">
            <strong>案例正在整理中</strong>
            <p>稍后回来，这里会展示更多精选作品。</p>
          </div>

          <div v-if="featuredApps.length < featuredAppsPage.total" class="gallery-more">
            <a-button @click="loadMoreFeatured">查看更多</a-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  position: relative;
  padding-bottom: 64px;
  overflow: hidden;
  background: #ffffff;
}

.home-page::before,
.home-page::after {
  position: absolute;
  left: 50%;
  pointer-events: none;
  content: '';
  transform: translateX(-50%);
}

.home-page::before {
  content: none;
}

.home-page::after {
  content: none;
}

.home-hero,
.showcase-section {
  position: relative;
  z-index: 1;
}

.home-hero {
  padding: 104px 0 118px;
}

.hero-shell {
  display: grid;
  gap: 44px;
  justify-items: center;
}

.hero-copy {
  display: grid;
  gap: 10px;
  padding-top: clamp(1.32rem, 2.88vw, 2.4rem);
  text-align: center;
}

.hero-title {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: clamp(1.32rem, 2.88vw, 2.4rem);
  font-weight: 700;
  line-height: 1.04;
  letter-spacing: -0.04em;
}

.hero-title__mark {
  display: block;
  width: clamp(40px, 4vw, 48px);
  height: clamp(40px, 4vw, 48px);
  border-radius: 16px;
  background: transparent;
  box-shadow: none;
}

.hero-subtitle {
  margin: 0;
  color: var(--text-muted);
  font-size: 1rem;
  line-height: 1.72;
}

.hero-composer {
  position: relative;
  width: min(760px, 100%);
  padding: 16px 16px 18px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 28px;
  background: #ffffff;
  box-shadow: 0 16px 42px rgba(20, 20, 19, 0.05);
}

.hero-composer:focus-within {
  border-color: rgba(20, 20, 19, 0.08);
  box-shadow: 0 16px 42px rgba(20, 20, 19, 0.05);
}

:deep(.prompt-input) {
  display: block;
  padding: 0 !important;
  background: transparent !important;
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
}

:deep(.prompt-input:hover),
:deep(.prompt-input:focus),
:deep(.prompt-input:focus-within) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

:deep(.prompt-input .ant-input),
:deep(.prompt-input textarea) {
  min-height: 144px !important;
  padding: 8px 68px 44px 14px !important;
  border: none !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  color: var(--text-strong) !important;
  font-size: 1rem;
  line-height: 1.75;
  resize: none !important;
}

:deep(.prompt-input .ant-input:focus),
:deep(.prompt-input textarea:focus) {
  border: none !important;
  box-shadow: none !important;
  outline: none !important;
}

:deep(.prompt-input .ant-input::placeholder),
:deep(.prompt-input textarea::placeholder) {
  color: #b3afa8;
}

.composer-footer {
  position: absolute;
  right: 16px;
  bottom: 14px;
}

.composer-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 999px;
  color: #ffffff;
  background: var(--text-strong);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    background-color 0.2s ease;
}

.composer-submit:hover:not(:disabled) {
  background: #262624;
  transform: translateY(-1px);
}

.composer-submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.hero-suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.hero-chip {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 14px;
  color: var(--text-default);
  background: #ffffff;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    transform 0.2s ease;
}

.hero-chip:hover {
  border-color: rgba(20, 20, 19, 0.16);
  transform: translateY(-1px);
}

.showcase-section {
  margin-top: 6px;
}

.showcase-panel {
  padding: 26px 26px 30px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 34px;
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(20, 20, 19, 0.06);
}

.showcase-block + .showcase-block {
  margin-top: 36px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-header h2 {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: clamp(1.08rem, 1.8vw, 1.44rem);
  font-weight: 700;
  line-height: 1.12;
}

.works-grid,
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.works-grid--placeholder {
  gap: 22px 18px;
}

.placeholder-card {
  display: grid;
  gap: 10px;
}

.placeholder-card strong,
.placeholder-card span {
  display: block;
}

.placeholder-card strong {
  color: var(--text-strong);
  font-size: 1rem;
}

.placeholder-card span {
  color: var(--text-subtle);
  font-size: 0.92rem;
}

.placeholder-card__visual {
  padding: 14px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 14px;
  background: #ffffff;
}

.placeholder-card__line,
.placeholder-card__avatar,
.placeholder-card__block {
  background: linear-gradient(90deg, rgba(226, 226, 226, 0.84), rgba(240, 240, 240, 0.92));
}

.placeholder-card__line {
  height: 10px;
  border-radius: 999px;
}

.placeholder-card__line--short {
  width: 72%;
}

.placeholder-card__line--mini {
  width: 48%;
}

.placeholder-card__layout {
  display: grid;
  grid-template-columns: 54px 1fr;
  gap: 12px;
  margin-top: 14px;
}

.placeholder-card__avatar {
  width: 54px;
  height: 92px;
  border-radius: 14px;
}

.placeholder-card__content {
  display: grid;
  gap: 10px;
}

.placeholder-card__block {
  height: 42px;
  border-radius: 10px;
}

.gallery-empty {
  padding: 56px 28px;
}

.gallery-more {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 1200px) {
  .works-grid,
  .gallery-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .works-grid,
  .gallery-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-hero {
    padding: 64px 0 76px;
  }

  .hero-title {
    flex-direction: column;
    gap: 8px;
  }

  .hero-composer {
    padding: 14px 14px 16px;
    border-radius: 26px;
  }

  .showcase-panel {
    padding: 22px 18px 24px;
    border-radius: 28px;
  }

  .works-grid,
  .gallery-grid {
    grid-template-columns: 1fr;
  }
}
</style>
