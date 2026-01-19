<template>
  <div id="userEditPage">
    <div class="page-header">
      <h1>编辑个人信息</h1>
    </div>

    <div class="edit-container">
      <a-card title="基本信息" :loading="loading">
        <a-form :model="formData" :rules="rules" layout="vertical" @finish="handleSubmit" ref="formRef">
          <a-form-item label="账号" name="userAccount">
            <a-input v-model:value="formData.userAccount" disabled />
          </a-form-item>

          <a-form-item label="用户名" name="userName">
            <a-input v-model:value="formData.userName" placeholder="请输入用户名" :maxlength="50" show-count />
          </a-form-item>

          <a-form-item label="用户头像" name="userAvatar" extra="先选择本地图片，上传成功后自动填充头像地址">
            <div class="avatar-upload">
              <a-avatar :src="formData.userAvatar" :size="64">{{ formData.userName?.charAt(0) || 'U' }}</a-avatar>
              <div class="avatar-upload-actions">
                <input type="file" accept="image/*" @change="handleAvatarChange" :disabled="uploadingAvatar" />
                <a-input v-model:value="formData.userAvatar" placeholder="头像图片地址" style="margin-top: 8px" readonly />
              </div>
            </div>
          </a-form-item>

          <a-form-item label="用户简介" name="userProfile">
            <a-textarea v-model:value="formData.userProfile" placeholder="请输入用户简介" :rows="4" :maxlength="200" show-count />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting">保存修改</a-button>
              <a-button @click="resetForm">重置</a-button>
              <a-button @click="openResetPasswordModal">忘记密码</a-button>
              <a-button type="link" @click="goBack">返回主页</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card title="我的信息" style="margin-top: 24px">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="用户ID">{{ loginUser?.id }}</a-descriptions-item>
          <a-descriptions-item label="账号">{{ loginUser?.userAccount }}</a-descriptions-item>
          <a-descriptions-item label="用户名">{{ loginUser?.userName }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatTime(loginUser?.createTime) }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-modal
        v-model:open="resetPasswordVisible"
        title="重置密码"
        :confirm-loading="resetPasswordSubmitting"
        @ok="handleResetPasswordSubmit"
        @cancel="handleResetPasswordCancel"
        destroy-on-close
      >
        <a-form :model="resetPasswordForm" :rules="resetPasswordRules" ref="resetPasswordFormRef">
          <a-form-item label="新密码" name="newPassword">
            <a-input-password v-model:value="resetPasswordForm.newPassword" placeholder="请输入新密码" />
          </a-form-item>
          <a-form-item label="确认密码" name="confirmPassword">
            <a-input-password v-model:value="resetPasswordForm.confirmPassword" placeholder="请再次输入新密码" />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { getLoginUser, updateUser } from '@/api/userController'
import { formatTime } from '@/utils/time'
import request from '@/request'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const loginUser = ref<API.LoginUserVO>()
const loading = ref(false)
const submitting = ref(false)
const uploadingAvatar = ref(false)
const formRef = ref<FormInstance>()

const resetPasswordVisible = ref(false)
const resetPasswordSubmitting = ref(false)
const resetPasswordFormRef = ref<FormInstance>()

const resetPasswordForm = reactive({
  newPassword: '',
  confirmPassword: '',
})

const formData = reactive({
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const rules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 50, message: '用户名长度在1-50个字符', trigger: 'blur' },
  ],
  userAvatar: [{ type: 'url', message: '请输入有效的URL', trigger: 'blur' }],
}

const resetPasswordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) => {
        if (!value) {
          return Promise.resolve()
        }
        if (value !== resetPasswordForm.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur',
    },
  ],
}

const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
      formData.userAccount = loginUser.value.userAccount || ''
      formData.userName = loginUser.value.userName || ''
      formData.userAvatar = loginUser.value.userAvatar || ''
      formData.userProfile = loginUser.value.userProfile || ''
    } else {
      message.error('获取用户信息失败')
      router.push('/')
    }
  } catch (error) {
    message.error('获取用户信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!loginUser.value?.id) {
    message.error('用户ID不存在')
    return
  }
  submitting.value = true
  try {
    const res = await updateUser({
      id: (loginUser.value?.id != null ? String(loginUser.value?.id) : undefined) as any,
      userName: formData.userName,
      userAvatar: formData.userAvatar,
      userProfile: formData.userProfile,
    })

    if (res.data.code === 0) {
      message.success('修改成功')
      await loginUserStore.fetchLoginUser()
      await fetchUserInfo()
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch (error) {
    message.error('修改失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  if (loginUser.value) {
    formData.userAccount = loginUser.value.userAccount || ''
    formData.userName = loginUser.value.userName || ''
    formData.userAvatar = loginUser.value.userAvatar || ''
    formData.userProfile = loginUser.value.userProfile || ''
  }
  formRef.value?.clearValidate()
}

const goBack = () => {
  router.push('/')
}

const openResetPasswordModal = () => {
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordFormRef.value?.clearValidate()
  resetPasswordVisible.value = true
}

const handleResetPasswordCancel = () => {
  resetPasswordVisible.value = false
}

const handleResetPasswordSubmit = async () => {
  try {
    await resetPasswordFormRef.value?.validate()
  } catch {
    return
  }

  resetPasswordSubmitting.value = true
  try {
    const res = await updateUser({
      id: (loginUser.value?.id != null ? String(loginUser.value?.id) : undefined) as any,
      userPassword: resetPasswordForm.newPassword,
    })

    if (res.data.code === 0) {
      message.success('密码重置成功')
      resetPasswordVisible.value = false
      await loginUserStore.fetchLoginUser()
      await fetchUserInfo()
    } else {
      message.error('密码重置失败：' + res.data.message)
    }
  } catch (error) {
    message.error('密码重置失败')
  } finally {
    resetPasswordSubmitting.value = false
  }
}

const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const form = new FormData()
  form.append('file', file)

  uploadingAvatar.value = true
  try {
    const res = await request.post<API.BaseResponseString>('/common/upload', form, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })

    if (res.data.code === 0 && res.data.data) {
      formData.userAvatar = res.data.data
      message.success('头像上传成功')
    } else {
      message.error('头像上传失败：' + res.data.message)
    }
  } catch (error) {
    message.error('头像上传失败')
  } finally {
    uploadingAvatar.value = false
    target.value = ''
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
#userEditPage {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.edit-container {
  border-radius: 8px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-upload-actions {
  display: flex;
  flex-direction: column;
}

:deep(.ant-card-head) {
  background: #fafafa;
}

:deep(.ant-descriptions-item-label) {
  background: #fafafa;
  font-weight: 500;
}
</style>
