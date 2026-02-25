<template>
  <div class="interview-report">
    <div class="mb-4 flex items-center justify-between">
      <h3 class="text-lg font-semibold text-gray-800">多维评估报告</h3>
      <el-tag
        v-if="report"
        :type="recommendationTagType"
        size="large"
        class="recommendation-tag"
      >
        {{ recommendationLabel }}
      </el-tag>
    </div>

    <div v-if="report" class="space-y-4">
      <!-- Score bars -->
      <div class="score-item">
        <div class="flex justify-between text-sm text-gray-600 mb-1">
          <span>规则熟悉度</span>
          <span class="font-mono">{{ report.ruleFamiliarity }}/10</span>
        </div>
        <el-progress
          :percentage="report.ruleFamiliarity * 10"
          :color="scoreColor(report.ruleFamiliarity)"
          :stroke-width="12"
          :show-text="false"
        />
      </div>

      <div class="score-item">
        <div class="flex justify-between text-sm text-gray-600 mb-1">
          <span>沟通能力</span>
          <span class="font-mono">{{ report.communicationScore }}/10</span>
        </div>
        <el-progress
          :percentage="report.communicationScore * 10"
          :color="scoreColor(report.communicationScore)"
          :stroke-width="12"
          :show-text="false"
        />
      </div>

      <div class="score-item">
        <div class="flex justify-between text-sm text-gray-600 mb-1">
          <span>抗压能力</span>
          <span class="font-mono">{{ report.pressureScore }}/10</span>
        </div>
        <el-progress
          :percentage="report.pressureScore * 10"
          :color="scoreColor(report.pressureScore)"
          :stroke-width="12"
          :show-text="false"
        />
      </div>

      <div class="score-item total-score pt-2 border-t border-gray-200">
        <div class="flex justify-between text-sm font-semibold text-gray-800 mb-1">
          <span>总分</span>
          <span class="font-mono text-base">{{ report.totalScore }}/10</span>
        </div>
        <el-progress
          :percentage="report.totalScore * 10"
          :color="scoreColor(report.totalScore)"
          :stroke-width="16"
          :show-text="false"
        />
      </div>

      <!-- AI Comment -->
      <div v-if="report.aiComment" class="ai-comment mt-4 p-3 bg-gray-50 rounded-lg">
        <div class="text-sm font-medium text-gray-700 mb-1">AI 评语</div>
        <div class="text-sm text-gray-600 leading-relaxed">{{ report.aiComment }}</div>
      </div>
    </div>

    <el-empty v-else description="暂无评估报告" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { InterviewReportDTO } from '@/api/interview'

const props = defineProps<{ report: InterviewReportDTO | null }>()

const recommendationLabel = computed(() => {
  if (!props.report) return ''
  const score = props.report.totalScore
  if (score >= 8) return '建议通过'
  if (score >= 6) return '重点审查对话内容'
  return '建议拒绝'
})

const recommendationTagType = computed(() => {
  if (!props.report) return 'info'
  const score = props.report.totalScore
  if (score >= 8) return 'success'
  if (score >= 6) return 'warning'
  return 'danger'
})

function scoreColor(score: number): string {
  if (score >= 8) return '#10b981'
  if (score >= 6) return '#f59e0b'
  return '#ef4444'
}
</script>
