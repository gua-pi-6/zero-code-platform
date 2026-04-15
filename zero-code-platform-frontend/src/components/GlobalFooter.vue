<template>
  <a-layout-footer :class="['footer', { 'footer--home': isHomePage }]">
    <div class="footer-shell page-shell--wide">
      <template v-if="isHomePage">
        <div class="footer-meta">
          <p class="footer-title">©2026 零代码应用平台</p>
          <p class="footer-copy">用自然语言生成页面、流程与站点，并继续把应用沉淀为可维护的数字作品。</p>
        </div>

        <div class="footer-links">
          <a href="https://www.codefather.cn" target="_blank" rel="noopener noreferrer">
            使用文档
          </a>
          <a href="https://www.codefather.cn" target="_blank" rel="noopener noreferrer">
            交流社区
          </a>
          <span>作品内容由 AI 生成</span>
        </div>
      </template>

      <template v-else>
        <div>
          <p class="footer-title">零代码应用平台</p>
          <p class="footer-copy">用提示词、对话、预览和部署，把一个想法收敛成可运行的应用。</p>
        </div>
        <p class="footer-link">
          <a href="https://www.codefather.cn" target="_blank" rel="noopener noreferrer">
            CodeFather 出品
          </a>
        </p>
      </template>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isHomePage = computed(() => route.path === '/')
</script>

<style scoped>
.footer {
  position: relative;
  z-index: 1;
  padding: 20px 0 36px;
}

.footer-shell {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  width: var(--content-width-wide);
  margin: 0 auto;
  padding: 16px 6px 0;
  border-top: 1px solid rgba(94, 93, 89, 0.12);
}

.footer--home .footer-shell {
  align-items: center;
  padding-top: 24px;
}

.footer-meta {
  display: grid;
  gap: 8px;
}

.footer-title {
  margin: 0;
  color: var(--text-strong);
  font-family: var(--font-serif);
  font-size: 1.15rem;
}

.footer-copy,
.footer-link {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: end;
  gap: 16px;
  color: var(--text-muted);
}

.footer-link a,
.footer-links a {
  transition: color 0.2s ease;
}

.footer-link a:hover,
.footer-links a:hover {
  color: var(--accent-warm);
}

@media (max-width: 768px) {
  .footer-shell,
  .footer--home .footer-shell {
    flex-direction: column;
    align-items: start;
    padding-inline: 0;
  }

  .footer-links {
    justify-content: start;
  }
}
</style>
