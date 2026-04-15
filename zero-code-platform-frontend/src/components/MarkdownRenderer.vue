<template>
  <div class="markdown-content" v-html="renderedMarkdown"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

interface Props {
  content: string
}

const props = defineProps<Props>()

const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
        )
      } catch {
        // Ignore highlighting errors and fall back to escaped content.
      }
    }

    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  },
})

const renderedMarkdown = computed(() => md.render(props.content))
</script>

<style scoped>
.markdown-content {
  color: var(--text-default);
  line-height: 1.8;
  word-break: break-word;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 1.4em 0 0.6em;
  color: var(--text-strong);
  font-family: var(--font-serif);
  line-height: 1.2;
}

.markdown-content :deep(h1) {
  font-size: 1.8rem;
}

.markdown-content :deep(h2) {
  font-size: 1.55rem;
}

.markdown-content :deep(h3) {
  font-size: 1.3rem;
}

.markdown-content :deep(p),
.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.9em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 1.35em;
}

.markdown-content :deep(blockquote) {
  margin: 1.2em 0;
  padding: 14px 18px;
  border-left: 3px solid rgba(201, 100, 66, 0.5);
  border-radius: 0 18px 18px 0;
  background: rgba(201, 100, 66, 0.08);
  color: var(--text-muted);
}

.markdown-content :deep(code) {
  padding: 0.2em 0.45em;
  border-radius: 8px;
  background: rgba(48, 48, 46, 0.08);
  color: var(--text-strong);
  font-family: var(--font-mono);
  font-size: 0.92em;
}

.markdown-content :deep(pre) {
  margin: 1.2em 0;
  padding: 16px;
  overflow-x: auto;
  border: 1px solid var(--border-strong);
  border-radius: 18px;
  background: #f7f4ec;
}

.markdown-content :deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-content :deep(table) {
  width: 100%;
  margin: 1.2em 0;
  border-collapse: collapse;
  overflow: hidden;
  border-radius: 16px;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  text-align: left;
}

.markdown-content :deep(th) {
  color: var(--text-strong);
  background: rgba(232, 230, 220, 0.7);
}

.markdown-content :deep(a) {
  color: var(--accent);
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  color: var(--accent-strong);
}

.markdown-content :deep(img) {
  max-width: 100%;
  border-radius: 18px;
}

.markdown-content :deep(hr) {
  margin: 1.6em 0;
  border: 0;
  border-top: 1px solid var(--border-strong);
}

.markdown-content :deep(.hljs) {
  background: transparent !important;
}
</style>
