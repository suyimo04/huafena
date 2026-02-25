<template>
  <div class="validation-rule-editor">
    <h4 class="section-title">验证规则</h4>

    <!-- TEXT fields: minLength, maxLength, pattern -->
    <template v-if="fieldType === 'TEXT'">
      <el-form-item label="最小长度">
        <el-input-number v-model="rules.minLength" :min="0" controls-position="right" class="w-full" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="最大长度">
        <el-input-number v-model="rules.maxLength" :min="0" controls-position="right" class="w-full" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="正则表达式">
        <el-input v-model="rules.pattern" placeholder="例如: ^[a-zA-Z]+$" @change="emitUpdate" />
      </el-form-item>
    </template>

    <!-- NUMBER fields: min, max -->
    <template v-if="fieldType === 'NUMBER'">
      <el-form-item label="最小值">
        <el-input-number v-model="rules.min" controls-position="right" class="w-full" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="最大值">
        <el-input-number v-model="rules.max" controls-position="right" class="w-full" @change="emitUpdate" />
      </el-form-item>
    </template>

    <!-- DATE fields: minDate, maxDate -->
    <template v-if="fieldType === 'DATE'">
      <el-form-item label="最早日期">
        <el-date-picker v-model="rules.minDate" type="date" value-format="YYYY-MM-DD" class="w-full" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="最晚日期">
        <el-date-picker v-model="rules.maxDate" type="date" value-format="YYYY-MM-DD" class="w-full" @change="emitUpdate" />
      </el-form-item>
    </template>

    <!-- MULTI_CHOICE fields: minSelect, maxSelect -->
    <template v-if="fieldType === 'MULTI_CHOICE'">
      <el-form-item label="最少选择">
        <el-input-number v-model="rules.minSelect" :min="0" controls-position="right" class="w-full" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="最多选择">
        <el-input-number v-model="rules.maxSelect" :min="0" controls-position="right" class="w-full" @change="emitUpdate" />
      </el-form-item>
    </template>

    <!-- Custom error message (all types) -->
    <el-form-item label="自定义错误提示">
      <el-input v-model="rules.customMessage" placeholder="验证失败时显示的提示" @change="emitUpdate" />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FieldType, ValidationRules } from '@/types/questionnaire'

const props = defineProps<{
  modelValue: ValidationRules | null
  fieldType: FieldType
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ValidationRules | null): void
}>()

const rules = ref<ValidationRules>({})

watch(
  () => props.modelValue,
  (v) => {
    rules.value = v ? { ...v } : {}
  },
  { immediate: true },
)

function emitUpdate() {
  const hasValues = Object.values(rules.value).some(
    (v) => v !== undefined && v !== null && v !== '',
  )
  emit('update:modelValue', hasValues ? { ...rules.value } : null)
}
</script>

<style scoped>
.validation-rule-editor {
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

.w-full {
  width: 100%;
}
</style>
