<template>
  <div class="interview-review flex flex-col h-full">
    <!-- Header -->
    <div class="flex items-center gap-2 px-4 py-3 border-b border-gray-200">
      <el-button text @click="$emit('back')">← 返回列表</el-button>
      <span class="text-gray-500">面试 #{{ interviewId }} 复审</span>
    </div>

    <div v-loading="loading" class="flex-1 overflow-y-auto p-4 space-y-6">
      <!-- Report section -->
      <div class="bg-white rounded-xl border border-gray-200 p-4 shadow-sm">
        <InterviewReport :report="report" />
      </div>

      <!-- Chat history section -->
      <div class="bg-white rounded-xl border border-gray-200 shadow-sm">
        <div class="px-4 py-3 border-b border-gray-100 font-semibold text-gray-700">
          对话记录
        </div>
        <div class="messages-area max-h-80 overflow-y-auto p-4 space-y-3">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-bubble flex"
            :class="msg.role === 'USER' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[75%] px-4 py-2 rounded-xl text-sm leading-relaxed"
              :class="msg.role === 'USER'
                ? 'bg-emerald-500 text-white rounded-br-sm'
                : 'bg-gray-100 text-gray-800 rounded-bl-sm'"
            >
              {{ msg.content }}
            </div>
          </div>
          <el-empty v-if="messages.length === 0" description="暂无对话记录" />
        </div>
      </div>

      <!-- Review form -->
      <div
        v-if="!report?.reviewedAt"
        class="review-form bg-white rounded-xl border border-gray-200 p-4 shadow-sm"
      >
        <h3 class="text-lg font-semibold text-gray-800 mb-4">复审表单</h3>
        <el-form :model="reviewForm" label-position="top">
          <el-form-item label="复审结果">
            <el-radio-group v-model="reviewForm.approved">
              <el-radio :value="true">通过</el-radio>
              <el-radio :value="false">拒绝</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="复审评语">
            <el-input
              v-model="reviewForm.comment"
              type="textarea"
              :rows="3"
              placeholder="请输入复审评语..."
            />
          </el-form-item>
          <el-form-item label="建议导师">
            <el-input
              v-model="reviewForm.suggestedMentor"
              placeholder="请输入建议的实习导师..."
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="reviewForm.approved === null"
              @click="handleSubmit"
            >
              提交复审
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- Already reviewed info -->
      <div
        v-else
        class="bg-white rounded-xl border border-gray-200 p-4 shadow-sm"
      >
        <h3 class="text-lg font-semibold text-gray-800 mb-2">复审结果</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="结果">
            <el-tag :type="report.manualApproved ? 'success' : 'danger'">
              {{ report.manualApproved ? '通过' : '拒绝' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评语">
            {{ report.reviewerComment || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="建议导师">
            {{ report.suggestedMentor || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="复审时间">
            {{ formatTime(report.reviewedAt!) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import InterviewReport from './InterviewReport.vue'
import type { InterviewReportDTO, InterviewMessageDTO } from '@/api/interview'
import { getReport, getMessages, submitReview } from '@/api/interview'

const props = defineProps<{ interviewId: number }>()
const emit = defineEmits<{ (e: 'back'): void; (e: 'reviewed'): void }>()

const report = ref<InterviewReportDTO | null>(null)
const messages = ref<InterviewMessageDTO[]>([])
const loading = ref(false)
const submitting = ref(false)

const reviewForm = ref<{
  approved: boolean | null
  comment: string
  suggestedMentor: string
}>({
  approved: null,
  comment: '',
  suggestedMentor: '',
})

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const [reportRes, messagesRes] = await Promise.all([
      getReport(props.interviewId),
      getMessages(props.interviewId),
    ])
    report.value = reportRes.data
    messages.value = messagesRes.data
  } catch {
    ElMessage.error('加载面试数据失败')
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (reviewForm.value.approved === null) return
  submitting.value = true
  try {
    await submitReview(props.interviewId, {
      approved: reviewForm.value.approved,
      reviewComment: reviewForm.value.comment || undefined,
      suggestedMentor: reviewForm.value.suggestedMentor || undefined,
    })
    ElMessage.success('复审提交成功')
    await loadData()
    emit('reviewed')
  } catch {
    ElMessage.error('复审提交失败')
  } finally {
    submitting.value = false
  }
}

watch(() => props.interviewId, () => {
  reviewForm.value = { approved: null, comment: '', suggestedMentor: '' }
  loadData()
}, { immediate: true })
</script>
