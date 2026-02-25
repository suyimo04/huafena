<template>
  <div class="group-renderer">
    <h3 class="group-title">{{ group.name }}</h3>
    <FieldRenderer
      v-for="field in visibleFields"
      :key="field.key"
      :field="field"
      :model-value="answers[field.key]"
      :error="errors[field.key] ?? null"
      @update:model-value="$emit('update:answer', field.key, $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FieldGroup, QuestionnaireField } from '@/types/questionnaire'
import { evaluateConditionalLogic } from '@/utils/questionnaireLogic'
import FieldRenderer from './FieldRenderer.vue'

const props = defineProps<{
  group: FieldGroup
  fields: QuestionnaireField[]
  answers: Record<string, unknown>
  errors: Record<string, string>
}>()

defineEmits<{
  'update:answer': [key: string, value: unknown]
}>()

const visibleFields = computed(() =>
  props.fields.filter((f) =>
    evaluateConditionalLogic(f.conditionalLogic, props.answers),
  ),
)
</script>

<style scoped>
.group-renderer {
  margin-bottom: 24px;
}
.group-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
}
</style>
