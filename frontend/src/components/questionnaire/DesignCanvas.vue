<template>
  <div class="design-canvas">
    <div class="canvas-header">
      <h3 class="canvas-title">设计画布</h3>
      <span class="field-count">{{ fields.length }} 个字段</span>
    </div>

    <draggable
      :list="fields"
      group="fields"
      item-key="key"
      class="canvas-drop-zone"
      :class="{ 'canvas-empty': fields.length === 0 }"
      ghost-class="ghost-field"
      @change="onDragChange"
    >
      <template #item="{ element, index }">
        <div
          class="canvas-field"
          :class="{ 'canvas-field-active': selectedKey === element.key }"
          :data-testid="`canvas-field-${element.key}`"
          @click="selectField(element.key)"
        >
          <div class="field-header">
            <span class="field-type-badge">{{ getTypeLabel(element.type) }}</span>
            <span class="field-label">{{ element.label }}</span>
            <span v-if="element.required" class="field-required">*</span>
          </div>
          <div class="field-preview">
            <span class="field-key">{{ element.key }}</span>
          </div>
          <div class="field-actions">
            <el-button type="danger" text size="small" @click.stop="removeField(index)">
              删除
            </el-button>
          </div>
        </div>
      </template>
    </draggable>

    <div v-if="fields.length === 0" class="empty-hint">
      <p>将左侧字段拖拽到此处</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import draggable from 'vuedraggable'
import { FIELD_TYPE_LIST } from '@/types/questionnaire'
import type { QuestionnaireField } from '@/types/questionnaire'

const props = defineProps<{
  fields: QuestionnaireField[]
  selectedKey: string | null
}>()

const emit = defineEmits<{
  (e: 'update:fields', fields: QuestionnaireField[]): void
  (e: 'select', key: string | null): void
  (e: 'remove', index: number): void
}>()

function getTypeLabel(type: string): string {
  return FIELD_TYPE_LIST.find((f) => f.type === type)?.label ?? type
}

function selectField(key: string) {
  emit('select', key)
}

function removeField(index: number) {
  emit('remove', index)
}

function onDragChange() {
  emit('update:fields', [...props.fields])
}
</script>

<style scoped>
.design-canvas {
  padding: 16px;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.canvas-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.canvas-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.field-count {
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 10px;
}

.canvas-drop-zone {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 200px;
  padding: 8px;
  border: 2px dashed transparent;
  border-radius: 12px;
  transition: border-color 0.2s;
}

.canvas-drop-zone.canvas-empty {
  border-color: #d1d5db;
}

.canvas-field {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.canvas-field:hover {
  border-color: #10b981;
  box-shadow: 0 1px 6px rgba(16, 185, 129, 0.12);
}

.canvas-field-active {
  border-color: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.field-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.field-type-badge {
  font-size: 11px;
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.field-label {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.field-required {
  color: #ef4444;
  font-weight: 600;
}

.field-preview {
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-key {
  font-size: 11px;
  color: #9ca3af;
  font-family: monospace;
}

.field-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.ghost-field {
  opacity: 0.4;
  background: rgba(16, 185, 129, 0.05);
  border: 2px dashed #10b981;
}

.empty-hint {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 40px 0;
}

.empty-hint p {
  margin: 0;
}
</style>
