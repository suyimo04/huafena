<template>
  <div class="field-renderer">
    <label class="field-label">
      {{ field.label }}
      <span v-if="field.required" class="field-required-star">*</span>
    </label>

    <!-- TEXT -->
    <el-input
      v-if="field.type === 'TEXT'"
      :model-value="modelValue as string"
      placeholder="请输入"
      @update:model-value="$emit('update:modelValue', $event)"
    />

    <!-- NUMBER -->
    <el-input-number
      v-else-if="field.type === 'NUMBER'"
      :model-value="modelValue as number"
      :controls="true"
      placeholder="请输入数字"
      @update:model-value="$emit('update:modelValue', $event)"
    />

    <!-- SINGLE_CHOICE -->
    <el-radio-group
      v-else-if="field.type === 'SINGLE_CHOICE'"
      :model-value="modelValue as string"
      @update:model-value="$emit('update:modelValue', $event)"
    >
      <el-radio
        v-for="opt in field.options ?? []"
        :key="opt.value"
        :value="opt.value"
      >
        {{ opt.label }}
      </el-radio>
    </el-radio-group>

    <!-- MULTI_CHOICE -->
    <el-checkbox-group
      v-else-if="field.type === 'MULTI_CHOICE'"
      :model-value="(modelValue as string[]) ?? []"
      @update:model-value="$emit('update:modelValue', $event)"
    >
      <el-checkbox
        v-for="opt in field.options ?? []"
        :key="opt.value"
        :label="opt.value"
        :value="opt.value"
      >
        {{ opt.label }}
      </el-checkbox>
    </el-checkbox-group>

    <!-- DROPDOWN -->
    <el-select
      v-else-if="field.type === 'DROPDOWN'"
      :model-value="modelValue as string"
      placeholder="请选择"
      @update:model-value="$emit('update:modelValue', $event)"
    >
      <el-option
        v-for="opt in field.options ?? []"
        :key="opt.value"
        :label="opt.label"
        :value="opt.value"
      />
    </el-select>

    <!-- DATE -->
    <el-date-picker
      v-else-if="field.type === 'DATE'"
      :model-value="modelValue as string"
      type="date"
      placeholder="请选择日期"
      value-format="YYYY-MM-DD"
      @update:model-value="$emit('update:modelValue', $event)"
    />

    <div v-if="error" class="field-error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import type { QuestionnaireField } from '@/types/questionnaire'

defineProps<{
  field: QuestionnaireField
  modelValue: unknown
  error: string | null
}>()

defineEmits<{
  'update:modelValue': [value: unknown]
}>()
</script>

<style scoped>
.field-renderer {
  margin-bottom: 18px;
}
.field-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}
.field-required-star {
  color: #ef4444;
  margin-left: 2px;
}
.field-error {
  color: #ef4444;
  font-size: 12px;
  margin-top: 4px;
}
</style>
