<template>
  <div class="group-manager">
    <div class="gm-header">
      <h4 class="section-title">分组管理</h4>
      <el-button type="primary" text size="small" @click="showAddDialog = true">+ 新建分组</el-button>
    </div>

    <div v-if="localGroups.length === 0" class="no-groups">
      <p>暂无分组</p>
    </div>

    <div v-else class="group-list">
      <div v-for="(group, idx) in localGroups" :key="group.name" class="group-item">
        <div class="group-header">
          <span class="group-name">{{ group.name }}</span>
          <span class="group-field-count">{{ group.fields.length }} 个字段</span>
          <el-button type="danger" text size="small" @click="removeGroup(idx)">删除</el-button>
        </div>
        <div class="group-fields">
          <el-select
            :model-value="group.fields"
            multiple
            placeholder="选择字段"
            size="small"
            class="w-full"
            @update:model-value="(val: string[]) => updateGroupFields(idx, val)"
          >
            <el-option v-for="f in availableFields" :key="f.key" :label="f.label" :value="f.key" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- Add group dialog -->
    <el-dialog v-model="showAddDialog" title="新建分组" width="360px" :append-to-body="true">
      <el-form @submit.prevent="addGroup">
        <el-form-item label="分组名称">
          <el-input v-model="newGroupName" placeholder="输入分组名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!newGroupName.trim()" @click="addGroup">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FieldGroup, QuestionnaireField } from '@/types/questionnaire'

const props = defineProps<{
  modelValue: FieldGroup[]
  availableFields: QuestionnaireField[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: FieldGroup[]): void
}>()

const localGroups = ref<FieldGroup[]>([])
const showAddDialog = ref(false)
const newGroupName = ref('')

watch(
  () => props.modelValue,
  (v) => {
    localGroups.value = v ? JSON.parse(JSON.stringify(v)) : []
  },
  { immediate: true, deep: true },
)

function emitUpdate() {
  emit('update:modelValue', JSON.parse(JSON.stringify(localGroups.value)))
}

function addGroup() {
  const name = newGroupName.value.trim()
  if (!name) return
  localGroups.value.push({
    name,
    sortOrder: localGroups.value.length + 1,
    fields: [],
  })
  newGroupName.value = ''
  showAddDialog.value = false
  emitUpdate()
}

function removeGroup(index: number) {
  localGroups.value.splice(index, 1)
  // Re-number sortOrder
  localGroups.value.forEach((g, i) => (g.sortOrder = i + 1))
  emitUpdate()
}

function updateGroupFields(index: number, fields: string[]) {
  localGroups.value[index].fields = fields
  emitUpdate()
}
</script>

<style scoped>
.group-manager {
  margin-top: 8px;
}

.gm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin: 0;
  padding-bottom: 0;
}

.no-groups {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 12px 0;
}

.no-groups p {
  margin: 0;
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.group-item {
  background: rgba(16, 185, 129, 0.04);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.group-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
}

.group-field-count {
  font-size: 11px;
  color: #9ca3af;
  flex: 1;
}

.group-fields {
  width: 100%;
}

.w-full {
  width: 100%;
}
</style>
