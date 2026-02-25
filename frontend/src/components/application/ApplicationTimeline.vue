<template>
  <div class="application-timeline">
    <el-timeline v-if="entries.length > 0">
      <el-timeline-item
        v-for="entry in entries"
        :key="entry.id"
        :timestamp="formatTime(entry.createdAt)"
        :type="timelineItemType(entry.status)"
        :hollow="false"
        placement="top"
      >
        <div class="timeline-card">
          <div class="timeline-status">
            <el-tag :type="statusTagType(entry.status)" size="small">
              {{ statusLabel(entry.status) }}
            </el-tag>
          </div>
          <div class="timeline-desc">{{ entry.description }}</div>
          <div v-if="entry.operator" class="timeline-operator">
            操作人: {{ entry.operator }}
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无流程记录" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getApplicationTimeline, type TimelineEntry } from '@/api/application'

const props = defineProps<{
  applicationId: number | null
}>()

const entries = ref<TimelineEntry[]>([])
const loading = ref(false)

async function fetchTimeline(id: number) {
  loading.value = true
  try {
    const res = await getApplicationTimeline(id)
    entries.value = res.data ?? []
  } catch {
    entries.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.applicationId,
  (id) => {
    if (id) fetchTimeline(id)
    else entries.value = []
  },
  { immediate: true },
)

function formatTime(dt: string) {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 19)
}

function timelineItemType(status: string): '' | 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    PENDING_INITIAL_REVIEW: 'warning',
    INITIAL_REVIEW_PASSED: 'success',
    REJECTED: 'danger',
    AUTO_REJECTED: 'danger',
    AI_INTERVIEW_IN_PROGRESS: 'primary',
    PENDING_REVIEW: 'warning',
    INTERN_OFFERED: 'success',
    INTERNSHIP_IN_PROGRESS: 'primary',
    MEMBER_CONVERTED: 'success',
  }
  return map[status] || 'info'
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    PENDING_INITIAL_REVIEW: 'warning',
    INITIAL_REVIEW_PASSED: 'success',
    REJECTED: 'danger',
    AUTO_REJECTED: 'danger',
    AI_INTERVIEW_IN_PROGRESS: '',
    PENDING_REVIEW: 'warning',
    INTERN_OFFERED: 'success',
    INTERNSHIP_IN_PROGRESS: '',
    MEMBER_CONVERTED: 'success',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_INITIAL_REVIEW: '待初审',
    INITIAL_REVIEW_PASSED: '初审通过',
    REJECTED: '已拒绝',
    AUTO_REJECTED: '自动筛选拒绝',
    AI_INTERVIEW_IN_PROGRESS: 'AI面试中',
    PENDING_REVIEW: '待复审',
    INTERN_OFFERED: '已发实习邀请',
    INTERNSHIP_IN_PROGRESS: '实习中',
    CONVERSION_REVIEW: '转正评审中',
    MEMBER_CONVERTED: '已转正',
  }
  return map[status] || status
}
</script>

<style scoped>
.application-timeline {
  padding: 12px 0;
}
.timeline-card {
  padding: 4px 0;
}
.timeline-status {
  margin-bottom: 4px;
}
.timeline-desc {
  font-size: 14px;
  color: #606266;
  margin-bottom: 2px;
}
.timeline-operator {
  font-size: 12px;
  color: #909399;
}
</style>
