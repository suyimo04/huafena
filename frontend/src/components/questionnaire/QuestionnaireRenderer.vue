<template>
  <div class="questionnaire-renderer">
    <!-- Grouped fields -->
    <GroupRenderer
      v-for="group in sortedGroups"
      :key="group.name"
      :group="group"
      :fields="getGroupFields(group)"
      :answers="answers"
      :errors="errors"
      @update:answer="handleAnswer"
    />

    <!-- Ungrouped fields -->
    <div v-if="ungroupedFields.length > 0" class="ungrouped-section">
      <FieldRenderer
        v-for="field in visibleUngroupedFields"
        :key="field.key"
        :field="field"
        :model-value="answers[field.key]"
        :error="errors[field.key] ?? null"
        @update:model-value="handleAnswer(field.key, $event)"
      />
    </div>

    <div class="renderer-actions">
      <el-button type="primary" @click="handleSubmit">提交</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { QuestionnaireSchema, FieldGroup, QuestionnaireField } from '@/types/questionnaire'
import { evaluateConditionalLogic, validateField } from '@/utils/questionnaireLogic'
import GroupRenderer from './GroupRenderer.vue'
import FieldRenderer from './FieldRenderer.vue'

const props = defineProps<{
  schema: QuestionnaireSchema
}>()

const emit = defineEmits<{
  submit: [answers: Record<string, unknown>]
}>()

const answers = ref<Record<string, unknown>>({})
const errors = ref<Record<string, string>>({})

const fieldMap = computed(() => {
  const map = new Map<string, QuestionnaireField>()
  for (const f of props.schema.fields) {
    map.set(f.key, f)
  }
  return map
})

const sortedGroups = computed(() =>
  [...props.schema.groups].sort((a, b) => a.sortOrder - b.sortOrder),
)

const groupedFieldKeys = computed(() => {
  const keys = new Set<string>()
  for (const g of props.schema.groups) {
    for (const k of g.fields) keys.add(k)
  }
  return keys
})

const ungroupedFields = computed(() =>
  props.schema.fields.filter((f) => !groupedFieldKeys.value.has(f.key)),
)

const visibleUngroupedFields = computed(() =>
  ungroupedFields.value.filter((f) =>
    evaluateConditionalLogic(f.conditionalLogic, answers.value),
  ),
)

function getGroupFields(group: FieldGroup): QuestionnaireField[] {
  return group.fields
    .map((key) => fieldMap.value.get(key))
    .filter((f): f is QuestionnaireField => !!f)
}

function handleAnswer(key: string, value: unknown) {
  answers.value[key] = value
  // Clear error on change
  if (errors.value[key]) {
    delete errors.value[key]
  }
}

function validate(): Record<string, string> {
  const result: Record<string, string> = {}
  for (const field of props.schema.fields) {
    const visible = evaluateConditionalLogic(field.conditionalLogic, answers.value)
    if (!visible) continue
    const err = validateField(field, answers.value[field.key])
    if (err) result[field.key] = err
  }
  errors.value = result
  return result
}

function getAnswers(): Record<string, unknown> {
  return { ...answers.value }
}

function handleSubmit() {
  const validationErrors = validate()
  if (Object.keys(validationErrors).length === 0) {
    emit('submit', getAnswers())
  }
}

defineExpose({ validate, getAnswers })
</script>

<style scoped>
.questionnaire-renderer {
  max-width: 640px;
}
.ungrouped-section {
  margin-bottom: 24px;
}
.renderer-actions {
  margin-top: 24px;
}
</style>
