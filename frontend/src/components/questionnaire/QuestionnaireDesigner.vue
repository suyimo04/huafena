<template>
  <div class="questionnaire-designer">
    <!-- Left: Field Palette -->
    <aside class="designer-panel panel-left theme-card">
      <FieldPalette />
    </aside>

    <!-- Center: Design Canvas -->
    <main class="designer-panel panel-center theme-card">
      <DesignCanvas
        :fields="fields"
        :selected-key="selectedFieldKey"
        @update:fields="onFieldsUpdate"
        @select="onSelectField"
        @remove="onRemoveField"
      />
      <!-- Group Manager below canvas -->
      <div class="group-section theme-card" style="margin-top: 12px;">
        <GroupManager
          :model-value="groups"
          :available-fields="fields"
          @update:model-value="onGroupsUpdate"
        />
      </div>
    </main>

    <!-- Right: Field Config + Version History -->
    <aside class="designer-panel panel-right theme-card">
      <FieldConfigPanel
        :field="selectedField"
        :all-fields="fields"
        @update="onFieldConfigUpdate"
      />
      <el-divider style="margin: 12px 0" />
      <VersionHistory
        :versions="versions"
        :active-version-id="activeVersionId"
        :loading="versionLoading"
        :saving="saving"
        :publishing="publishing"
        @save="onSaveVersion"
        @publish="onPublishVersion"
        @select-version="onSelectVersion"
      />
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import FieldPalette from './FieldPalette.vue'
import DesignCanvas from './DesignCanvas.vue'
import FieldConfigPanel from './FieldConfigPanel.vue'
import GroupManager from './GroupManager.vue'
import VersionHistory from './VersionHistory.vue'
import type { QuestionnaireField, FieldGroup } from '@/types/questionnaire'
import type { VersionDTO } from '@/api/questionnaire'

const fields = ref<QuestionnaireField[]>([])
const groups = ref<FieldGroup[]>([])
const selectedFieldKey = ref<string | null>(null)

/* ---- Version management state ---- */
const versions = ref<VersionDTO[]>([])
const activeVersionId = ref<number | null>(null)
const versionLoading = ref(false)
const saving = ref(false)
const publishing = ref(false)

const selectedField = computed<QuestionnaireField | null>(() => {
  if (!selectedFieldKey.value) return null
  return fields.value.find((f) => f.key === selectedFieldKey.value) ?? null
})

function onFieldsUpdate(updated: QuestionnaireField[]) {
  fields.value = updated
}

function onSelectField(key: string | null) {
  selectedFieldKey.value = key
}

function onRemoveField(index: number) {
  const removed = fields.value[index]
  fields.value.splice(index, 1)
  if (selectedFieldKey.value === removed?.key) {
    selectedFieldKey.value = null
  }
}

function onFieldConfigUpdate(updated: QuestionnaireField) {
  const idx = fields.value.findIndex((f) => f.key === selectedFieldKey.value)
  if (idx !== -1) {
    fields.value.splice(idx, 1, updated)
    selectedFieldKey.value = updated.key
  }
}

function onGroupsUpdate(updated: FieldGroup[]) {
  groups.value = updated
}

const emit = defineEmits<{
  save: []
  publish: []
  selectVersion: [version: VersionDTO]
}>()

function onSaveVersion() {
  emit('save')
}

function onPublishVersion() {
  emit('publish')
}

function onSelectVersion(ver: VersionDTO) {
  emit('selectVersion', ver)
}

defineExpose({
  fields,
  groups,
  selectedFieldKey,
  versions,
  activeVersionId,
  versionLoading,
  saving,
  publishing,
})
</script>

<style scoped>
.questionnaire-designer {
  display: flex;
  gap: 16px;
  height: calc(100vh - 120px);
  min-height: 500px;
}

.designer-panel {
  overflow-y: auto;
}

.panel-left {
  width: 220px;
  flex-shrink: 0;
}

.panel-center {
  flex: 1;
  min-width: 0;
}

.panel-right {
  width: 280px;
  flex-shrink: 0;
}
</style>
