<template>
  <div ref="markdownRef" class="markdown-content" v-html="renderedMarkdown"></div>
</template>

<script setup lang="ts">
import { computed, nextTick, watch, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

// 引入代码高亮样式
import 'highlight.js/styles/github.css'

interface Props {
  content: string
}

const props = defineProps<Props>()
const markdownRef = ref<HTMLElement>()
const previousContentLength = ref(0)
const hasToolCallLoader = ref(false)

// 配置 markdown-it 实例
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
        // 忽略错误，使用默认处理
      }
    }

    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  },
})

// 处理调用结果的HTML
const processCallResultHtml = (html: string): string => {
  // 查找"[调用结果]"和"[#调用结果]"
  const startMarker = '[调用结果]'
  const endMarker = '[#调用结果]'
  
  // 使用正则表达式匹配，考虑HTML标签
  const regex = new RegExp(
    `(${startMarker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})([\\s\\S]*?)(${endMarker.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`,
    'g'
  )
  
  return html.replace(regex, (match, start, content, end) => {
    // 文档图标
    const documentIcon = '<svg class="call-result-icon" viewBox="64 64 896 896" width="14" height="14" fill="currentColor"><path d="M854.6 288.6L639.4 73.4c-6-6-14.1-9.4-22.6-9.4H192c-17.7 0-32 14.3-32 32v832c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V311.3c0-8.5-3.4-16.6-9.4-22.7zM602 137.8L790.2 326H602V137.8zM792 888H232V136h302v240c0 17.7 14.3 32 32 32h226v480z"/></svg>'
    
    // 包装整段内容，添加图标，隐藏结束标记
    return `<div class="call-result-container"><span class="call-result-icon-wrapper">${documentIcon}</span>${start}${content}<span class="call-result-end-marker">${end}</span></div>`
  })
}

// 计算渲染后的 Markdown
const renderedMarkdown = computed(() => {
  const html = md.render(props.content)
  return processCallResultHtml(html)
})

// 处理包含工具调用关键词的行
const processToolCallLines = () => {
  nextTick(() => {
    if (!markdownRef.value) return
    
    // 查找所有包含工具调用关键词的文本节点
    const walker = document.createTreeWalker(
      markdownRef.value,
      NodeFilter.SHOW_TEXT,
      null
    )
    
    const textNodes: Node[] = []
    let node: Node | null
    while (node = walker.nextNode()) {
      const text = node.textContent || ''
      if (text.includes('[工具调用]') || text.includes('[选择工具]')) {
        textNodes.push(node)
      }
    }
    
    // 处理每个文本节点
    textNodes.forEach(textNode => {
      const parent = textNode.parentElement
      if (!parent) return
      
      // 跳过已经处理过的元素
      if (parent.classList.contains('tool-call-container')) return
      if (parent.closest('.tool-call-container')) return
      
      // 查找包含文本的块级父元素（p, li, div等）
      let container: HTMLElement | null = parent
      while (container && container !== markdownRef.value) {
        const tagName = container.tagName
        if (tagName && tagName.match(/^(P|LI|DIV|H[1-6])$/i)) {
          break
        }
        container = container.parentElement
      }
      
      if (!container || container === markdownRef.value) {
        container = parent
      }
      
      // 检查是否包含关键词
      if (container.textContent && 
          (container.textContent.includes('[工具调用]') || 
           container.textContent.includes('[选择工具]'))) {
        // 给容器添加类名（用于hover效果）
        container.classList.add('tool-call-container')
        
        // 在关键词前添加图标（如果还没有添加）
        const html = container.innerHTML
        if (!html.includes('tool-call-icon')) {
          const pencilIcon = '<svg class="tool-call-icon" viewBox="64 64 896 896" width="14" height="14" fill="currentColor"><path d="M880 836H144c-17.7 0-32 14.3-32 32v36c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-36c0-17.7-14.3-32-32-32zm-622.3-84c2 0 4-.2 6-.5L431.9 722c2-.4 3.9-1.3 5.3-2.8l423.9-423.9a9.96 9.96 0 0 0 0-14.1L694.9 114.9c-1.9-1.9-4.4-2.9-7.1-2.9s-5.2 1-7.1 2.9L256.8 538.8c-1.5 1.5-2.4 3.3-2.8 5.3l-29.5 168.2a33.5 33.5 0 0 0 9.4 29.8c6.6 6.4 14.9 9.9 23.8 9.9z"/></svg>'
          container.innerHTML = html.replace(
            /(\[工具调用\])/g,
            `<span class="tool-call-icon-wrapper">${pencilIcon}</span>$1`
          )
        }
        
        // 检查是否需要添加加载图标
        updateToolCallLoader(container)
      }
    })
  })
}

// 更新工具调用行的加载图标
const updateToolCallLoader = (container: Element) => {
  const existingLoader = container.querySelector('.tool-call-loader')
  const hasToolCall = container.textContent?.includes('[工具调用]')
  
  // 检查该容器是否已经移除过加载图标（避免重复添加）
  const loaderRemoved = container.classList.contains('tool-call-loader-removed')
  
  // 检查"[工具调用]"后面是否还有其他内容
  // 如果该容器后面还有兄弟元素，或者该容器后面还有文本内容，说明下一个消息流已经到来
  const hasNextContent = checkHasNextContent(container)
  
  if (hasToolCall && !existingLoader && !loaderRemoved && !hasNextContent) {
    // 如果包含"[工具调用]"且没有加载图标，且没有移除过，且后面没有新内容，添加加载图标
    const loader = document.createElement('span')
    loader.className = 'tool-call-loader'
    loader.innerHTML = '<span class="tool-call-spinner"></span>'
    container.appendChild(loader)
    hasToolCallLoader.value = true
  }
}

// 检查容器后面是否还有其他内容
const checkHasNextContent = (container: Element): boolean => {
  if (!markdownRef.value) return false
  
  // 检查该容器后面是否还有兄弟元素
  let nextSibling = container.nextElementSibling
  while (nextSibling) {
    const siblingText = nextSibling.textContent || ''
    if (siblingText.trim().length > 0) {
      return true
    }
    nextSibling = nextSibling.nextElementSibling
  }
  
  return false
}

// 处理包含调用结果的内容（DOM处理，用于动态更新）
const processCallResult = () => {
  // 已经在HTML字符串层面处理了，这里不需要额外处理
  // 保留函数以保持接口一致性
}

// 监听内容变化
watch(() => props.content, (newContent, oldContent) => {
  const contentLength = newContent.length
  const oldLength = oldContent?.length || 0
  
  processToolCallLines()
  processCallResult()
  
  // 如果内容长度增加，说明有新消息流到来，移除所有加载图标并标记
  if (contentLength > oldLength && hasToolCallLoader.value) {
    nextTick(() => {
      if (markdownRef.value) {
        const containers = markdownRef.value.querySelectorAll('.tool-call-container')
        containers.forEach(container => {
          const loader = container.querySelector('.tool-call-loader')
          if (loader) {
            loader.remove()
            // 标记该容器已经移除过加载图标，避免再次添加
            container.classList.add('tool-call-loader-removed')
          }
        })
        hasToolCallLoader.value = false
      }
    })
  }
  
  previousContentLength.value = contentLength
}, { immediate: true })
</script>

<style scoped>
.markdown-content {
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;
}

/* 全局样式，影响 v-html 内容 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 1.5em 0 0.5em 0;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-content :deep(h1) {
  font-size: 1.5em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h2) {
  font-size: 1.3em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h3) {
  font-size: 1.1em;
}

.markdown-content :deep(p) {
  margin: 0.8em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin: 0.3em 0;
}

.markdown-content :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #ddd;
  background-color: #f9f9f9;
  color: #666;
}

.markdown-content :deep(code) {
  background-color: #f1f1f1;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
}

.markdown-content :deep(pre) {
  background-color: #f8f8f8;
  border: 1px solid #e1e1e1;
  border-radius: 6px;
  padding: 1em;
  overflow-x: auto;
  margin: 1em 0;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 0.9em;
  line-height: 1.4;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  border: 1px solid #ddd;
  padding: 0.5em 0.8em;
  text-align: left;
}

.markdown-content :deep(table th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

.markdown-content :deep(table tr:nth-child(even)) {
  background-color: #f9f9f9;
}

.markdown-content :deep(a) {
  color: #1890ff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 0.5em 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #eee;
  margin: 1.5em 0;
}

/* 代码高亮样式优化 */
.markdown-content :deep(.hljs) {
  background-color: #f8f8f8 !important;
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
  line-height: 1.4;
}

/* 特定语言的代码块样式 */
.markdown-content :deep(.hljs-keyword) {
  color: #d73a49;
  font-weight: 600;
}

.markdown-content :deep(.hljs-string) {
  color: #032f62;
}

.markdown-content :deep(.hljs-comment) {
  color: #6a737d;
  font-style: italic;
}

.markdown-content :deep(.hljs-number) {
  color: #005cc5;
}

.markdown-content :deep(.hljs-function) {
  color: #6f42c1;
}

.markdown-content :deep(.hljs-tag) {
  color: #22863a;
}

.markdown-content :deep(.hljs-attr) {
  color: #6f42c1;
}

.markdown-content :deep(.hljs-title) {
  color: #6f42c1;
  font-weight: 600;
}

/* 工具调用行样式 */
.markdown-content :deep(.tool-call-container) {
  padding: 4px 8px;
  margin: 4px 0;
  border-radius: 4px;
  transition: box-shadow 0.2s ease;
  cursor: pointer;
}

.markdown-content :deep(.tool-call-container:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.markdown-content :deep(.tool-call-icon-wrapper) {
  display: inline-flex;
  align-items: center;
  margin-right: 4px;
  vertical-align: middle;
  margin-top: -2px;
}

.markdown-content :deep(.tool-call-icon) {
  display: inline-block;
  color: #8c8c8c;
  vertical-align: middle;
}

/* 工具调用加载图标样式 */
.markdown-content :deep(.tool-call-loader) {
  display: inline-block;
  margin-left: 8px;
  vertical-align: middle;
}

.markdown-content :deep(.tool-call-spinner) {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid #d9d9d9;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: tool-call-spin 0.8s linear infinite;
}

@keyframes tool-call-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 调用结果样式 */
.markdown-content :deep(.call-result-container) {
  padding: 4px 8px;
  margin: 4px 0;
  border-radius: 4px;
  transition: box-shadow 0.2s ease;
  cursor: pointer;
}

.markdown-content :deep(.call-result-container:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.markdown-content :deep(.call-result-icon-wrapper) {
  display: inline-flex;
  align-items: center;
  margin-right: 4px;
  vertical-align: middle;
  margin-top: -2px;
}

.markdown-content :deep(.call-result-icon) {
  display: inline-block;
  color: #8c8c8c;
  vertical-align: middle;
}

.markdown-content :deep(.call-result-end-marker) {
  display: none;
}
</style>
