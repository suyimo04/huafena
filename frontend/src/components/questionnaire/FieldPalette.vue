<template>
  <div class="field-palette">
    <h3 class="palette-title">字段类型</h3>
    <p class="palette-hint">拖拽字段到画布中</p>
    <draggable
      :list="paletteItems"
      :group="{ name: 'fields', pull: 'clone', put: false }"
      :clone="cloneField"
      :sort="false"
      item-key="type"
      class="palette-list"
    >
      <template #item="{ element }">
        <div class="palette-item" :data-testid="`palette-${element.type}`">
          <span class="palette-icon">{{ element.icon }}</span>
          <span class="palette-label">{{ element.label }}</span>
        </div>
      </template>
    </draggable>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import draggable from 'vuedraggable'
import { FIELD_TYPE_LIST, createDefaultField } from '@/types/questionnaire'
import type { PaletteItem, QuestionnaireField } from '@/types/questionnaire'

const emit = defineEmits<{
  (e: 'clone', field: QuestionnaireField): void
}>()

let fieldCounter = 0

const paletteItems = ref<PaletteItem[]>([...FIELD_TYPE_LIST])

function cloneField(item: PaletteItem): QuestionnaireField {
  fieldCounter++
  const field = createDefaultField(item.type, fieldCounter)
  emit('clone', field)
  return field
}
</script>

<style scoped>
.field-palette {
  padding: 16px;
}

.palette-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.palette-hint {
  font-size: 12px;
  color: #9ca3af;
  margin: 0 0 12px 0;
}

.palette-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: grab;
  transition: all 0.2s ease;
  user-select: none;
}

.palette-item:hover {
  border-color: #10b981;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.15);
  transform: translateY(-1px);
}

.palette-item:active {
  cursor: grabbing;
}

.palette-icon {
  font-size: 18px;
}

.palette-label {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
}
</style>
