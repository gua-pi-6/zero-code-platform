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
          <span>{{ featured ? '精选案例' : '作品预览' }}</span>
          <strong>{{ previewLetter }}</strong>
        </template>
      </div>

      <div v-if="isWorkspaceCard" class="workspace-overlay">
        <button type="button" class="workspace-overlay__button" @click.stop="handleViewChat">
          查看对话
        </button>
      </div>

      <div v-else class="gallery-badges">
        <span v-if="featured" class="gallery-badge gallery-badge--accent">精选</span>
        <span v-if="app.deployKey" class="gallery-badge">已部署</span>
      </div>
    </div>

    <div class="app-body">
      <template v-if="isWorkspaceCard">
        <h3 class="workspace-title">{{ app.appName || '新会话' }}</h3>
        <p class="workspace-time">创建于 {{ timeLabel }}</p>
      </template>

      <template v-else>
        <div class="gallery-info">
          <a-avatar class="gallery-avatar" :src="app.user?.userAvatar" :size="34">
            {{ app.user?.userName?.charAt(0) || 'U' }}
          </a-avatar>
          <div class="gallery-copy">
            <h3 class="gallery-title">{{ app.appName || '未命名应用' }}</h3>
            <p class="gallery-meta">
              <span>{{ authorLabel }}</span>
              <span>{{ dateLabel }}</span>
            </p>
          </div>
        </div>
      </template>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
const previewLetter = computed(() => props.app.appName?.charAt(0)?.toUpperCase() || 'N')
const timeLabel = computed(
  () => formatRelativeTime(props.app.updateTime || props.app.createTime) || '刚刚',
)
const dateLabel = computed(() => formatDate(props.app.updateTime || props.app.createTime) || '刚刚')
const authorLabel = computed(() => props.app.user?.userName || '创作者')

const handleViewChat = () => {
  emit('view-chat', toIdString(props.app.id) || undefined)
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
  display: grid;
  align-content: space-between;
  padding: 18px;
  background: #ffffff;
}

.app-card--gallery .app-placeholder span {
  color: rgba(20, 20, 19, 0.58);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.app-card--gallery .app-placeholder strong {
  align-self: end;
  color: rgba(20, 20, 19, 0.16);
  font-family: var(--font-serif);
  font-size: clamp(3rem, 6vw, 4.6rem);
  line-height: 0.92;
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

.workspace-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: end;
  justify-content: center;
  padding: 14px;
  background: rgba(20, 20, 19, 0.22);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.app-card:hover .workspace-overlay {
  opacity: 1;
}

.workspace-overlay__button {
  width: 100%;
  min-height: 42px;
  border: none;
  border-radius: 12px;
  color: var(--text-strong);
  background: rgba(255, 255, 255, 0.94);
  cursor: pointer;
}

.gallery-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 8px;
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

.workspace-title {
  margin: 0;
  color: var(--text-strong);
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.45;
}

.workspace-time {
  margin: 0;
  color: var(--text-subtle);
  font-size: 0.9rem;
  line-height: 1.6;
}

.gallery-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.gallery-avatar {
  flex: none;
  box-shadow: 0 8px 18px rgba(20, 20, 19, 0.08);
}

.gallery-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.gallery-title {
  margin: 0;
  overflow: hidden;
  color: var(--text-strong);
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gallery-meta {
  display: flex;
  gap: 8px;
  margin: 0;
  color: var(--text-subtle);
  font-size: 0.88rem;
  line-height: 1.5;
  white-space: nowrap;
}
</style>
