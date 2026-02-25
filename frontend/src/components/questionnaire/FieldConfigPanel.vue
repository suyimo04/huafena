<template>
  <div class="field-config-panel">
    <template v-if="field">
      <h3 class="config-title">字段属性</h3>

      <el-form label-position="top" size="default" class="config-form">
        <el-form-item label="字段标识">
          <el-input v-model="localField.key" @change="emitUpdate" />
        </el-form-item>

        <el-form-item label="字段标签">
          <el-input v-model="localField.label" @change="emitUpdate" />
        </el-form-item>

        <el-form-item label="字段类型">
          <el-select v-model="localField.type" @change="emitUpdate" class="w-full">
            <el-option
              v-for="ft in fieldTypes"
              :key="ft.type"
              :label="ft.label"
              :value="ft.type"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="是否必填">
          <el-switch v-model="localField.required" @change="emitUpdate" />
        </el-form-item>

        <!-- Options for choice-type fields -->
        <template v-if="hasOptions">
          <el-form-item label="选项列表">
            <div class="options-list">
              <div
                v-for="(opt, idx) in localField.options"
                :key="idx"
                class="option-row"
              >
                <el-input
                  v-model="opt.label"
                  placeholder="选项标签"
                  size="small"
                  @change="emitUpdate"
                />
                <el-input
                  v-model="opt.value"
                  placeholder="选项值"
                  size="small"
                  @change="emitUpdate"
                />
                <el-button
                  type="danger"
                  text
                  size="small"
                  @click="removeOption(idx)"
                >
                  删除
                </el-button>
              </div>
              <el-button type="primary" text size="small" @click="addOption">
                + 添加选项
              </el-button>
            </div>
          </el-form-item>
        </template>

        <!-- Validation Rules Editor -->
        <el-divider />
        <ValidationRuleEditor
          :model-value="localField.validationRules"
          :field-type="localField.type"
          @update:model-value="onValidationUpdate"
        />

        <!-- Conditional Logic Editor -->
        <el-divider />
        <ConditionalLogicEditor
          :model-value="localField.conditionalLogic"
          :available-fields="availableFields"
          @update:model-value="onConditionalLogicUpdate"
        />
      </el-form>
    </template>

    <div v-else class="no-selection">
      <p>请在画布中选择一个字段</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { FIELD_TYPE_LIST } from '@/types/questionnaire'
import type { QuestionnaireField, ValidationRules, ConditionalLogic } from '@/types/questionnaire'
import ValidationRuleEditor from './ValidationRuleEditor.vue'
import ConditionalLogicEditor from './ConditionalLogicEditor.vue'

const props = defineProps<{
  field: QuestionnaireField | null
  allFields?: QuestionnaireField[]
}>()

const emit = defineEmits<{
  (e: 'update', field: QuestionnaireField): void
}>()

const fieldTypes = FIELD_TYPE_LIST

const localField = ref<QuestionnaireField>(createEmpty())

function createEmpty(): QuestionnaireField {
  return { key: '', type: 'TEXT', label: '', required: false, validationRules: null, conditionalLogic: null, options: null }
}

watch(
  () => props.field,
  (f) => {
    if (f) {
      localField.value = JSON.parse(JSON.stringify(f))
    }
  },
  { immediate: true, deep: true },
)

const hasOptions = computed(() =>
  ['SINGLE_CHOICE', 'MULTI_CHOICE', 'DROPDOWN'].includes(localField.value.type),
)

// Available fields for conditional logic (exclude current field)
const availableFields = computed(() =>
  (props.allFields ?? []).filter((f) => f.key !== localField.value.key),
)

function emitUpdate() {
  emit('update', JSON.parse(JSON.stringify(localField.value)))
}

function onValidationUpdate(rules: ValidationRules | null) {
  localField.value.validationRules = rules
  emitUpdate()
}

function onConditionalLogicUpdate(logic: ConditionalLogic | null) {
  localField.value.conditionalLogic = logic
  emitUpdate()
}

function addOption() {
  if (!localField.value.options) localField.value.options = []
  const idx = localField.value.options.length + 1
  localField.value.options.push({ value: `option${idx}`, label: `选项 ${idx}` })
  emitUpdate()
}

function removeOption(index: number) {
  localField.value.options?.splice(index, 1)
  emitUpdate()
}
</script>

<style scoped>
.field-config-panel {
  padding: 16px;
}

.config-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.config-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: #6b7280;
  padding-bottom: 4px;
}

.w-full {
  width: 100%;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.option-row {
  display: flex;
  gap: 6px;
  align-items: center;
}

.option-row .el-input {
  flex: 1;
}

.no-selection {
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  padding: 40px 0;
}

.no-selection p {
  margin: 0;
}
</style>
