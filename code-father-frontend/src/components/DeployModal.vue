<template>
  <a-modal v-model:open="visible" :title="modalTitle" :footer="null" width="640px">
    <div class="deploy-modal">
      <div class="status-panel">
        <div class="status-icon" :class="statusClass">
          <CheckCircleOutlined v-if="isSuccess" />
          <CloseCircleOutlined v-else-if="isFailed" />
          <CloudUploadOutlined v-else />
        </div>
        <h3>{{ statusHeading }}</h3>
        <p>{{ statusDescription }}</p>
        <a-tag v-if="task?.status" :color="statusColor">{{ task.status }}</a-tag>
      </div>

      <a-alert
        v-if="task?.errorMessage"
        type="error"
        show-icon
        :message="task.errorMessage"
        class="deploy-alert"
      />

      <div class="deploy-info">
        <div class="info-row">
          <span class="label">部署地址</span>
          <a-input :value="task?.deployUrl || deployUrlPlaceholder" readonly>
            <template #suffix>
              <a-button type="text" :disabled="!task?.deployUrl" @click="handleCopyUrl">
                <CopyOutlined />
              </a-button>
            </template>
          </a-input>
        </div>
      </div>

      <div class="deploy-actions">
        <a-button
          type="primary"
          :loading="startingDeploy"
          :disabled="startButtonDisabled"
          @click="handleStartDeploy"
        >
          {{ startButtonText }}
        </a-button>
        <a-button :disabled="!isSuccess || !task?.deployUrl" @click="handleOpenSite">访问网站</a-button>
        <a-button @click="handleClose">关闭</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  CloudUploadOutlined,
  CopyOutlined,
} from '@ant-design/icons-vue'

interface Props {
  open: boolean
  task?: API.AppDeployTaskVO
  startingDeploy: boolean
  startButtonDisabled: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'start-deploy'): void
  (e: 'open-site'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const taskStatus = computed(() => props.task?.status)

const isSuccess = computed(() => taskStatus.value === 'SUCCESS')
const isFailed = computed(() => taskStatus.value === 'FAILED')
const isPending = computed(() => taskStatus.value === 'PENDING')
const isDeploying = computed(() => taskStatus.value === 'DEPLOYING')
const isScreenshotting = computed(() => taskStatus.value === 'SCREENSHOTTING')
const isRunning = computed(() => isPending.value || isDeploying.value || isScreenshotting.value)

const modalTitle = computed(() => {
  if (isSuccess.value) return '部署成功'
  if (isFailed.value) return '部署失败'
  return '应用部署'
})

const statusHeading = computed(() => {
  if (isSuccess.value) return '网站部署成功'
  if (isFailed.value) return '网站部署失败'
  if (isScreenshotting.value) return '正在生成封面截图'
  if (isDeploying.value) return '正在构建并发布应用'
  if (isPending.value) return '部署任务已提交'
  return '准备开始部署'
})

const statusDescription = computed(() => {
  if (isSuccess.value) return '部署任务已完成，现在可以访问网站。'
  if (isFailed.value) return '部署任务已结束，但未成功完成，请查看失败原因。'
  if (isScreenshotting.value) return '应用已经发布完成，正在补充封面截图。'
  if (isDeploying.value) return '后端正在构建项目并同步部署目录，请稍候。'
  if (isPending.value) return '部署任务正在排队，后端即将开始处理。'
  return '点击开始部署后，将创建部署任务并持续查询进度。'
})

const statusColor = computed(() => {
  if (isSuccess.value) return 'success'
  if (isFailed.value) return 'error'
  if (isScreenshotting.value) return 'processing'
  if (isDeploying.value) return 'processing'
  if (isPending.value) return 'default'
  return 'default'
})

const statusClass = computed(() => {
  if (isSuccess.value) return 'success'
  if (isFailed.value) return 'failed'
  return 'running'
})

const startButtonText = computed(() => {
  if (props.startingDeploy) return '开始部署中'
  if (isRunning.value) return '部署进行中'
  return '开始部署'
})

const deployUrlPlaceholder = computed(() => {
  return isRunning.value ? '部署完成后可访问' : '开始部署后生成访问地址'
})

const handleCopyUrl = async () => {
  if (!props.task?.deployUrl) {
    return
  }
  try {
    await navigator.clipboard.writeText(props.task.deployUrl)
    message.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败：', error)
    message.error('复制失败')
  }
}

const handleStartDeploy = () => {
  emit('start-deploy')
}

const handleOpenSite = () => {
  emit('open-site')
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.deploy-modal {
  padding: 8px 8px 16px;
}

.status-panel {
  text-align: center;
  padding: 16px 12px 12px;
}

.status-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.status-icon.running {
  color: #1677ff;
  background: #e6f4ff;
}

.status-icon.success {
  color: #52c41a;
  background: #f6ffed;
}

.status-icon.failed {
  color: #ff4d4f;
  background: #fff2f0;
}

.status-panel h3 {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 600;
}

.status-panel p {
  margin: 0 0 12px;
  color: #666;
}

.deploy-alert {
  margin-bottom: 16px;
}

.deploy-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 24px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label {
  font-size: 13px;
  color: #666;
}

.value {
  color: #1f1f1f;
  font-weight: 500;
}

.deploy-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
