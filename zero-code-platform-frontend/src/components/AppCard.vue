<template>
  <article
    class="app-card"
    :class="[
      `app-card--${variant}`,
      {
        'app-card--featured': featured,
      },
    ]"
    @click="handleViewChat"
  >
    <div class="app-visual">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <template v-if="isWorkspaceCard">
          <div class="workspace-skeleton">
            <div class="workspace-skeleton__line workspace-skeleton__line--top"></div>
            <div class="workspace-skeleton__layout">
              <div class="workspace-skeleton__avatar">
                <img src="@/assets/logo.png" alt="NoCode" />
              </div>
              <div class="workspace-skeleton__content">
                <div class="workspace-skeleton__line"></div>
                <div class="workspace-skeleton__line"></div>
                <div class="workspace-skeleton__line workspace-skeleton__line--short"></div>
                <div class="workspace-skeleton__block"></div>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="gallery-skeleton">
            <div class="gallery-skeleton__line gallery-skeleton__line--top"></div>
            <div class="gallery-skeleton__layout">
              <div class="gallery-skeleton__sidebar">
                <div class="gallery-skeleton__avatar">
                  <img src="@/assets/logo.png" alt="NoCode" />
                </div>
                <div class="gallery-skeleton__sidebar-line gallery-skeleton__sidebar-line--short"></div>
                <div class="gallery-skeleton__sidebar-line"></div>
              </div>
              <div class="gallery-skeleton__content">
                <div class="gallery-skeleton__line"></div>
                <div class="gallery-skeleton__line"></div>
                <div class="gallery-skeleton__line gallery-skeleton__line--short"></div>
                <div class="gallery-skeleton__block"></div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <div v-if="isWorkspaceCard" class="workspace-overlay">
        <button type="button" class="workspace-overlay__button" @click.stop="handleViewChat">
          查看对话
        </button>
      </div>

      <template v-else>
        <div class="gallery-overlay">
          <button type="button" class="gallery-overlay__button" @click.stop="handleViewWork">
            <ReadOutlined />
            <span>预览</span>
          </button>
        </div>

        <div class="gallery-badges">
          <span v-if="featured" class="gallery-badge gallery-badge--accent">精选</span>
          <span v-if="app.deployKey" class="gallery-badge">已部署</span>
        </div>
      </template>
    </div>

    <div class="app-body" :class="{ 'app-body--gallery': !isWorkspaceCard }">
      <template v-if="isWorkspaceCard">
        <h3 class="workspace-title">{{ app.appName || '新会话' }}</h3>
        <p class="workspace-time">创建于 {{ workspaceTimeLabel }}</p>
      </template>

      <template v-else>
        <div class="gallery-info">
          <a-avatar class="gallery-avatar" :src="app.user?.userAvatar" :size="36">
            {{ app.user?.userName?.charAt(0) || 'U' }}
          </a-avatar>
          <div class="gallery-copy">
            <h3 class="gallery-title">{{ app.appName || '未命名应用' }}</h3>
            <p class="gallery-meta">{{ authorLabel }} {{ galleryDateLabel }}</p>
          </div>
        </div>
      </template>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ReadOutlined } from '@ant-design/icons-vue'
import { toIdString } from '@/utils/id'
import { formatDate, formatRelativeTime } from '@/utils/time'

interface Props {
  app: API.AppVO
  featured?: boolean
  variant?: 'gallery' | 'workspace'
}

interface Emits {
  (e: 'view-chat', appId: string | undefined): void
  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
  variant: 'gallery',
})

const emit = defineEmits<Emits>()

const isWorkspaceCard = computed(() => props.variant === 'workspace')
const workspaceTimeLabel = computed(() => formatRelativeTime(props.app.createTime) || '刚刚')
const galleryDateLabel = computed(() => formatDate(props.app.createTime) || '')
const authorLabel = computed(() => props.app.user?.userName || '创作者')

const handleViewChat = () => {
  emit('view-chat', toIdString(props.app.id) || undefined)
}

const handleViewWork = () => {
  if (props.app.deployKey) {
    emit('view-work', props.app)
    return
  }

  handleViewChat()
}
</script>

<style scoped>
.app-card {
  display: grid;
  gap: 10px;
  cursor: pointer;
}

.app-visual {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(20, 20, 19, 0.08);
  background: #ffffff;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.app-card:hover .app-visual {
  transform: translateY(-4px);
  border-color: rgba(20, 20, 19, 0.14);
  box-shadow: 0 22px 42px rgba(20, 20, 19, 0.08);
}

.app-card--workspace .app-visual {
  aspect-ratio: 1.52 / 1;
  border-radius: 14px;
}

.app-card--gallery .app-visual {
  aspect-ratio: 1.55 / 1;
  border-radius: 12px;
}

.app-visual img,
.app-placeholder {
  width: 100%;
  height: 100%;
}

.app-visual img {
  display: block;
  object-fit: cover;
}

.app-placeholder {
  position: relative;
}

.app-card--gallery .app-placeholder {
  display: block;
  padding: 18px;
  background: #ffffff;
}

.workspace-skeleton {
  height: 100%;
  padding: 14px;
  background: rgba(246, 246, 244, 0.92);
}

.workspace-skeleton__line,
.workspace-skeleton__avatar,
.workspace-skeleton__block {
  background: linear-gradient(90deg, rgba(226, 226, 226, 0.88), rgba(240, 240, 240, 0.94));
}

.workspace-skeleton__line {
  height: 10px;
  border-radius: 999px;
}

.workspace-skeleton__line--top {
  width: 72%;
}

.workspace-skeleton__line--short {
  width: 48%;
}

.workspace-skeleton__layout {
  display: grid;
  grid-template-columns: 54px 1fr;
  gap: 12px;
  margin-top: 14px;
}

.workspace-skeleton__avatar {
  display: grid;
  place-items: center;
  border-radius: 14px;
}

.workspace-skeleton__avatar img {
  width: 32px;
  height: 32px;
  border-radius: 12px;
  opacity: 0.72;
}

.workspace-skeleton__content {
  display: grid;
  gap: 10px;
}

.workspace-skeleton__block {
  height: 42px;
  border-radius: 10px;
}

.gallery-skeleton {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
  background: rgba(246, 246, 244, 0.92);
}

.gallery-skeleton__line,
.gallery-skeleton__sidebar,
.gallery-skeleton__avatar,
.gallery-skeleton__sidebar-line,
.gallery-skeleton__block {
  background: linear-gradient(90deg, rgba(226, 226, 226, 0.88), rgba(240, 240, 240, 0.94));
}

.gallery-skeleton__line {
  height: 10px;
  border-radius: 999px;
}

.gallery-skeleton__line--top {
  width: 76%;
  margin: 16px 16px 0;
}

.gallery-skeleton__line--short {
  width: 42%;
}

.gallery-skeleton__layout {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 12px;
  margin: 14px 12px 12px;
  flex: 1;
  min-height: 0;
  align-items: stretch;
}

.gallery-skeleton__sidebar {
  display: grid;
  grid-template-rows: 1fr auto auto;
  gap: 12px;
  min-height: 0;
  padding: 12px 10px;
  border-radius: 12px;
}

.gallery-skeleton__avatar {
  display: grid;
  place-items: center;
  align-self: start;
  width: 44px;
  height: 44px;
  margin: 0 auto;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
}

.gallery-skeleton__avatar img {
  width: 26px;
  height: 26px;
  border-radius: 10px;
  opacity: 0.88;
}

.gallery-skeleton__sidebar-line {
  height: 10px;
  border-radius: 999px;
}

.gallery-skeleton__sidebar-line--short {
  width: 74%;
}

.gallery-skeleton__content {
  display: grid;
  grid-template-rows: auto auto auto minmax(58px, 1fr);
  gap: 10px;
  min-height: 0;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.45);
}

.gallery-skeleton__block {
  height: auto;
  min-height: 42px;
  border-radius: 10px;
}

.workspace-overlay,
.gallery-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: end;
  justify-content: center;
  padding: 14px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.workspace-overlay {
  background: rgba(20, 20, 19, 0.22);
}

.gallery-overlay {
  background: linear-gradient(180deg, rgba(220, 220, 220, 0.2) 0%, rgba(191, 191, 191, 0.48) 100%);
}

.app-card:hover .workspace-overlay,
.app-card:hover .gallery-overlay {
  opacity: 1;
}

.workspace-overlay__button,
.gallery-overlay__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  min-height: 42px;
  border: none;
  border-radius: 12px;
  color: var(--text-strong);
  background: rgba(255, 255, 255, 0.94);
  font-size: 0.96rem;
  line-height: 1;
  cursor: pointer;
}

.gallery-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
  transition: opacity 0.2s ease;
}

.app-card:hover .gallery-badges {
  opacity: 0;
}

.gallery-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  color: var(--text-default);
  background: rgba(255, 255, 255, 0.84);
  font-size: 12px;
  font-weight: 600;
}

.gallery-badge--accent {
  color: var(--text-default);
  background: rgba(20, 20, 19, 0.06);
}

.app-body {
  display: grid;
  gap: 6px;
}

.app-body--gallery {
  gap: 0;
}

.workspace-title {
  margin: 0;
  color: var(--text-strong);
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.4;
}

.workspace-time {
  margin: 0;
  color: #9d9890;
  font-size: 0.9rem;
  line-height: 1.5;
}

.gallery-info {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.gallery-avatar {
  flex: none;
}

.gallery-copy {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.gallery-title {
  margin: 0;
  overflow: hidden;
  color: #1b1b1a;
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gallery-meta {
  margin: 0;
  color: #8d887f;
  font-size: 0.9rem;
  line-height: 1.4;
}

</style>
