<template>
  <div class="version-history">
    <div class="version-header">
      <h3 class="version-title">版本历史</h3>
      <div class="version-actions">
        <el-button type="primary" size="small" :loading="saving" @click="onSave">
          保存
        </el-button>
        <el-button
          size="small"
          :loading="publishing"
          :disabled="!canPublish"
          @click="onPublish"
        >
          发布
        </el-button>
      </div>
    </div>

    <el-divider style="margin: 8px 0" />

    <div v-if="loading" class="version-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="versions.length === 0" class="version-empty">
      暂无版本记录
    </div>

    <ul v-else class="version-list">
      <li
        v-for="ver in versions"
        :key="ver.id"
        class="version-item"
        :class="{
          'version-active': ver.id === activeVersionId,
          'version-selected': ver.id === selectedVersionId,
        }"
        @click="onSelectVersion(ver)"
      >
        <div class="version-info">
          <span class="version-number">v{{ ver.versionNumber }}</span>
          <el-tag
            :type="ver.status === 'PUBLISHED' ? 'success' : 'info'"
            size="small"
            class="version-status"
          >
            {{ ver.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
          <el-tag
            v-if="ver.id === activeVersionId"
            type="warning"
            size="small"
            effect="plain"
            class="version-active-tag"
          >
            当前
          </el-tag>
        </div>
        <div class="version-date">{{ formatDate(ver.createdAt) }}</div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import type { VersionDTO } from '@/api/questionnaire'

export interface Props {
  versions: VersionDTO[]
  activeVersionId: number | null
  loading?: boolean
  saving?: boolean
  publishing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  saving: false,
  publishing: false,
})

const emit = defineEmits<{
  save: []
  publish: []
  selectVersion: [version: VersionDTO]
}>()

const selectedVersionId = ref<number | null>(null)

const canPublish = computed(() => {
  if (props.versions.length === 0) return false
  const latest = props.versions[0]
  return latest && latest.status === 'DRAFT'
})

function onSave() {
  emit('save')
}

function onPublish() {
  emit('publish')
}

function onSelectVersion(ver: VersionDTO) {
  selectedVersionId.value = ver.id
  emit('selectVersion', ver)
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

defineExpose({ selectedVersionId })
</script>

<style scoped>
.version-history {
  padding: 12px;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.version-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.version-actions {
  display: flex;
  gap: 6px;
}

.version-loading,
.version-empty {
  text-align: center;
  color: #999;
  padding: 24px 0;
  font-size: 13px;
}

.version-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.version-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.version-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 4px;
}

.version-item:hover {
  background: #f0fdf4;
}

.version-item.version-selected {
  background: #ecfdf5;
  border: 1px solid #10b981;
}

.version-item.version-active {
  border-left: 3px solid #10b981;
}

.version-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.version-number {
  font-weight: 600;
  font-size: 13px;
}

.version-date {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
</style>
