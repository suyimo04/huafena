<template>
  <div class="p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">AI 面试</h2>

    <!-- Interview list when no active view -->
    <div v-if="!activeInterviewId && !reviewInterviewId">
      <el-table :data="interviews" v-loading="loading" stripe class="w-full">
        <el-table-column prop="id" label="面试ID" width="80" />
        <el-table-column prop="applicationId" label="申请ID" width="80" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              type="primary"
              size="small"
              @click="openChat(row.id)"
            >
              继续面试
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_REVIEW' || row.status === 'REVIEWED' || row.status === 'COMPLETED'"
              size="small"
              type="warning"
              @click="openReview(row.id)"
            >
              {{ row.status === 'PENDING_REVIEW' ? '复审' : '查看报告' }}
            </el-button>
            <el-button
              v-if="row.status !== 'IN_PROGRESS'"
              size="small"
              @click="openChat(row.id)"
            >
              查看记录
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Active chat view -->
    <div v-if="activeInterviewId" class="chat-container bg-white rounded-xl border border-gray-200 shadow-sm" style="height: 70vh;">
      <div class="flex items-center gap-2 px-4 py-2 border-b border-gray-100">
        <el-button text @click="closeChat">← 返回列表</el-button>
      </div>
      <InterviewChat
        :interview-id="activeInterviewId"
        @ended="onInterviewEnded"
        style="height: calc(100% - 45px);"
      />
    </div>

    <!-- Review view -->
    <div v-if="reviewInterviewId" class="bg-white rounded-xl border border-gray-200 shadow-sm" style="min-height: 70vh;">
      <InterviewReview
        :interview-id="reviewInterviewId"
        @back="closeReview"
        @reviewed="onReviewed"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import InterviewChat from '@/components/interview/InterviewChat.vue'
import InterviewReview from '@/components/interview/InterviewReview.vue'
import type { InterviewDTO } from '@/api/interview'
import { getInterview } from '@/api/interview'
import { getApplications } from '@/api/application'

const interviews = ref<InterviewDTO[]>([])
const loading = ref(false)
const activeInterviewId = ref<number | null>(null)
const reviewInterviewId = ref<number | null>(null)

const statusMap: Record<string, { label: string; type: string }> = {
  NOT_STARTED: { label: '未开始', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  PENDING_REVIEW: { label: '待复审', type: '' },
  REVIEWED: { label: '已复审', type: 'success' },
}

function statusLabel(status: string) {
  return statusMap[status]?.label ?? status
}

function statusTagType(status: string) {
  return statusMap[status]?.type ?? 'info'
}

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

async function fetchInterviews() {
  loading.value = true
  try {
    const appRes = await getApplications()
    const apps = appRes.data || []
    const interviewApps = apps.filter((a) =>
      ['AI_INTERVIEW_IN_PROGRESS', 'PENDING_REVIEW', 'INTERN_OFFERED'].includes(a.status)
    )
    const results: InterviewDTO[] = []
    for (const app of interviewApps) {
      try {
        const res = await getInterview(app.id)
        if (res.data) results.push(res.data)
      } catch {
        // interview may not exist yet
      }
    }
    interviews.value = results
  } catch {
    interviews.value = []
  } finally {
    loading.value = false
  }
}

function openChat(id: number) {
  activeInterviewId.value = id
  reviewInterviewId.value = null
}

function closeChat() {
  activeInterviewId.value = null
  fetchInterviews()
}

function openReview(id: number) {
  reviewInterviewId.value = id
  activeInterviewId.value = null
}

function closeReview() {
  reviewInterviewId.value = null
  fetchInterviews()
}

function onInterviewEnded() {
  // Stay on chat, user can click back
}

function onReviewed() {
  // Stay on review, data reloads automatically
}

onMounted(fetchInterviews)
</script>
