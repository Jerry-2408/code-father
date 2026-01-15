<template>
  <div class="user-message-content">
    <template v-if="hasElementInfo">
      <span class="message-before">{{ beforeElementInfo }}</span>
      <span 
        class="element-info-header" 
        @click="toggleCollapse"
        :class="{ collapsed: isCollapsed }"
      >
        <span class="collapse-icon">{{ isCollapsed ? '▼' : '▲' }}</span>
        选中元素信息：
      </span>
      <span 
        class="element-info-content" 
        :style="{ display: isCollapsed ? 'none' : 'block' }"
      >
        {{ afterElementInfo }}
      </span>
    </template>
    <template v-else>
      {{ content }}
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface Props {
  content: string
  index: number
}

const props = defineProps<Props>()

const elementModifyMarker = '#请对以下选中元素进行修改，尽量只修改当前选中的元素#'

const cleanedContent = computed(() => {
  return props.content
      .replace(elementModifyMarker + '\n', '')
      .replace(elementModifyMarker, '')
})

// 默认折叠状态（隐藏）
const isCollapsed = ref(true)

// 检查是否包含"选中元素信息："
const hasElementInfo = computed(() => {
  return cleanedContent.value.includes('选中元素信息：')
})

// 分割内容
const beforeElementInfo = computed(() => {
  if (!hasElementInfo.value) return ''
  const index = cleanedContent.value.indexOf('选中元素信息：')
  return cleanedContent.value.substring(0, index)
})

const afterElementInfo = computed(() => {
  if (!hasElementInfo.value) return ''
  const index = cleanedContent.value.indexOf('选中元素信息：')
  return cleanedContent.value.substring(index + '选中元素信息：'.length)
})

// 切换折叠状态
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped>
.user-message-content {
  white-space: pre-wrap;
  word-wrap: break-word;
}

.element-info-header {
  cursor: pointer;
  user-select: none;
  display: inline-block;
  font-weight: 500;
  color: #1890ff;
  transition: color 0.2s;
}

.element-info-header:hover {
  color: #40a9ff;
}

.collapse-icon {
  display: inline-block;
  margin-right: 4px;
  font-size: 12px;
  transition: transform 0.2s;
}

.element-info-content {
  display: block;
  margin-top: 4px;
  padding-left: 16px;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>

