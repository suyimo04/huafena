<template>
  <div class="conditional-logic-editor">
    <h4 class="section-title">条件逻辑</h4>

    <div v-if="logic" class="logic-config">
      <!-- Action: SHOW / HIDE -->
      <el-form-item label="动作">
        <el-radio-group v-model="logic.action" @change="emitUpdate">
          <el-radio value="SHOW">显示</el-radio>
          <el-radio value="HIDE">隐藏</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- Logic operator: AND / OR -->
      <el-form-item label="逻辑运算">
        <el-radio-group v-model="logic.logicOperator" @change="emitUpdate">
          <el-radio value="AND">全部满足 (AND)</el-radio>
          <el-radio value="OR">任一满足 (OR)</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- Conditions list -->
      <div class="conditions-list">
        <div v-for="(cond, idx) in logic.conditions" :key="idx" class="condition-row">
          <el-select v-model="cond.fieldKey" placeholder="选择字段" size="small" class="cond-field" @change="emitUpdate">
            <el-option v-for="f in availableFields" :key="f.key" :label="f.label" :value="f.key" />
          </el-select>
          <el-select v-model="cond.operator" placeholder="运算符" size="small" class="cond-op" @change="emitUpdate">
            <el-option v-for="op in operators" :key="op.value" :label="op.label" :value="op.value" />
          </el-select>
          <el-input v-model="cond.value" placeholder="值" size="small" class="cond-val" @change="emitUpdate" />
          <el-button type="danger" text size="small" @click="removeCondition(idx)">删除</el-button>
        </div>
      </div>

      <div class="logic-actions">
        <el-button type="primary" text size="small" @click="addCondition">+ 添加条件</el-button>
        <el-button type="danger" text size="small" @click="clearLogic">清除逻辑</el-button>
      </div>
    </div>

    <div v-else class="no-logic">
      <el-button type="primary" text size="small" @click="enableLogic">+ 添加条件逻辑</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ConditionalLogic, ConditionalLogicCondition, QuestionnaireField } from '@/types/questionnaire'

const props = defineProps<{
  modelValue: ConditionalLogic | null
  availableFields: QuestionnaireField[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ConditionalLogic | null): void
}>()

const operators = [
  { value: 'EQUALS', label: '等于' },
  { value: 'NOT_EQUALS', label: '不等于' },
  { value: 'CONTAINS', label: '包含' },
  { value: 'GREATER_THAN', label: '大于' },
  { value: 'LESS_THAN', label: '小于' },
  { value: 'IN', label: '在列表中' },
  { value: 'NOT_IN', label: '不在列表中' },
]

const logic = ref<ConditionalLogic | null>(null)

watch(
  () => props.modelValue,
  (v) => {
    logic.value = v ? JSON.parse(JSON.stringify(v)) : null
  },
  { immediate: true },
)

function emitUpdate() {
  emit('update:modelValue', logic.value ? JSON.parse(JSON.stringify(logic.value)) : null)
}

function enableLogic() {
  logic.value = {
    action: 'SHOW',
    logicOperator: 'AND',
    conditions: [{ fieldKey: '', operator: 'EQUALS', value: '' }],
  }
  emitUpdate()
}

function addCondition() {
  if (!logic.value) return
  logic.value.conditions.push({ fieldKey: '', operator: 'EQUALS', value: '' })
  emitUpdate()
}

function removeCondition(index: number) {
  if (!logic.value) return
  logic.value.conditions.splice(index, 1)
  if (logic.value.conditions.length === 0) {
    logic.value = null
  }
  emitUpdate()
}

function clearLogic() {
  logic.value = null
  emitUpdate()
}
</script>

<style scoped>
.conditional-logic-editor {
  margin-top: 8px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 8px 0;
  padding-bottom: 6px;
  border-bottom: 1px solid #f3f4f6;
}

.conditions-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.condition-row {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
}

.cond-field {
  flex: 2;
  min-width: 80px;
}

.cond-op {
  flex: 2;
  min-width: 70px;
}

.cond-val {
  flex: 2;
  min-width: 60px;
}

.logic-actions {
  display: flex;
  gap: 8px;
}

.no-logic {
  text-align: center;
  padding: 8px 0;
}
</style>
