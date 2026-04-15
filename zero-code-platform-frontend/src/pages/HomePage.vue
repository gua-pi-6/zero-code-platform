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
  top: 8px;
  width: 120vw;
  height: 900px;
  background:
    radial-gradient(circle at 22% 62%, rgba(171, 255, 237, 0.55), transparent 0 26%),
    radial-gradient(circle at 76% 32%, rgba(178, 255, 252, 0.46), transparent 0 20%),
    radial-gradient(circle at 84% 78%, rgba(76, 117, 255, 0.44), transparent 0 28%),
    radial-gradient(circle at 50% 84%, rgba(125, 213, 255, 0.22), transparent 0 22%);
  filter: blur(10px);
}

.home-page::after {
  top: 0;
  width: 100vw;
  height: 100%;
  background: linear-gradient(
    180deg,
    rgba(245, 244, 237, 0.02) 0%,
    rgba(245, 244, 237, 0.26) 34%,
    rgba(245, 244, 237, 0.92) 74%,
    rgba(245, 244, 237, 0.98) 100%
  );
}

.home-hero,
.showcase-section {
  position: relative;
  z-index: 1;
}

.home-hero {
  padding: 68px 0 56px;
}

.hero-shell {
  display: grid;
  gap: 24px;
  justify-items: center;
}

.hero-copy {
  display: grid;
  gap: 10px;
  text-align: center;
}

.hero-title {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: clamp(2.8rem, 6vw, 4.8rem);
  line-height: 1.04;
  letter-spacing: -0.04em;
}

.hero-title__mark {
  width: clamp(50px, 5vw, 62px);
  height: clamp(50px, 5vw, 62px);
  border-radius: 20px;
  box-shadow: 0 12px 28px rgba(29, 53, 86, 0.12);
}

.hero-subtitle {
  margin: 0;
  color: var(--text-muted);
  font-size: 1.06rem;
  line-height: 1.8;
}

.hero-composer {
  width: min(760px, 100%);
  padding: 18px 18px 14px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.42) inset,
    0 28px 72px rgba(43, 60, 93, 0.12);
  backdrop-filter: blur(16px);
}

:deep(.prompt-input textarea) {
  min-height: 160px !important;
  padding: 18px 20px !important;
  border: none !important;
  border-radius: 24px !important;
  background: transparent !important;
  box-shadow: none !important;
  color: var(--text-strong) !important;
  font-size: 1rem;
  line-height: 1.8;
  resize: none !important;
}

:deep(.prompt-input textarea::placeholder) {
  color: #98a0b8;
}

.composer-footer {
  display: flex;
  justify-content: end;
  margin-top: 10px;
}

.composer-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 999px;
  color: #ffffff;
  background: #b5b8c1;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    background-color 0.2s ease;
}

.composer-submit:hover:not(:disabled) {
  background: var(--accent-warm);
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
  border: 1px solid rgba(232, 230, 220, 0.96);
  border-radius: 14px;
  color: var(--text-default);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    transform 0.2s ease;
}

.hero-chip:hover {
  border-color: rgba(201, 100, 66, 0.24);
  transform: translateY(-1px);
}

.showcase-section {
  margin-top: 6px;
}

.showcase-panel {
  padding: 26px 26px 30px;
  border: 1px solid rgba(240, 238, 230, 0.96);
  border-radius: 34px;
  background: rgba(250, 249, 245, 0.9);
  box-shadow: 0 24px 72px rgba(34, 47, 82, 0.08);
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
  font-size: clamp(1.8rem, 3vw, 2.4rem);
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
  border: 1px solid rgba(220, 220, 220, 0.7);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.4);
}

.placeholder-card__line,
.placeholder-card__avatar,
.placeholder-card__block {
  background: linear-gradient(90deg, rgba(230, 230, 230, 0.85), rgba(243, 243, 243, 0.92));
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
    padding-top: 42px;
  }

  .hero-title {
    flex-direction: column;
    gap: 8px;
  }

  .hero-composer {
    padding: 16px 16px 14px;
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
