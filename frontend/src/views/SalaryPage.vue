<template>
  <div class="p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-4">薪资管理</h2>

    <!-- Toolbar -->
    <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
      <div class="flex items-center gap-2">
        <span v-if="editedCount > 0" class="text-sm text-orange-500 font-medium">
          {{ editedCount }} 条未保存修改
        </span>
      </div>
      <div class="flex items-center gap-2">
        <el-button v-if="canEdit" type="primary" :loading="saving" :disabled="editedCount === 0" @click="handleBatchSave">
          批量保存
        </el-button>
        <el-button v-if="canEdit" @click="handleCalculate" :loading="calculating">计算薪资</el-button>
        <el-button v-if="canEdit" @click="handleArchive" :loading="archiving">归档</el-button>
      </div>
    </div>

    <!-- Validation error banner -->
    <div v-if="globalError" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
      {{ globalError }}
    </div>

    <!-- Salary table -->
    <el-table :data="displayRecords" v-loading="loading" stripe class="w-full" :row-class-name="rowClassName">
      <el-table-column prop="userId" label="成员ID" width="90" />
      <el-table-column label="基础积分" width="120">
        <template #default="{ row }">
          <div @click="startEdit(row, 'basePoints')" class="cursor-pointer min-h-[32px] flex items-center">
            <el-input-number
              v-if="isEditing(row.id, 'basePoints')"
              v-model="getEditRow(row.id).basePoints"
              :min="0"
              size="small"
              @blur="stopEdit()"
              @keyup.enter="stopEdit()"
            />
            <span v-else>{{ getDisplayValue(row, 'basePoints') }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="奖励积分" width="120">
        <template #default="{ row }">
          <div @click="startEdit(row, 'bonusPoints')" class="cursor-pointer min-h-[32px] flex items-center">
            <el-input-number
              v-if="isEditing(row.id, 'bonusPoints')"
              v-model="getEditRow(row.id).bonusPoints"
              :min="0"
              size="small"
              @blur="stopEdit()"
              @keyup.enter="stopEdit()"
            />
            <span v-else>{{ getDisplayValue(row, 'bonusPoints') }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="扣减" width="120">
        <template #default="{ row }">
          <div @click="startEdit(row, 'deductions')" class="cursor-pointer min-h-[32px] flex items-center">
            <el-input-number
              v-if="isEditing(row.id, 'deductions')"
              v-model="getEditRow(row.id).deductions"
              :min="0"
              size="small"
              @blur="stopEdit()"
              @keyup.enter="stopEdit()"
            />
            <span v-else>{{ getDisplayValue(row, 'deductions') }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="总积分" width="100">
        <template #default="{ row }">
          {{ getDisplayValue(row, 'totalPoints') }}
        </template>
      </el-table-column>
      <el-table-column label="迷你币" width="100">
        <template #default="{ row }">
          <div @click="startEdit(row, 'miniCoins')" class="cursor-pointer min-h-[32px] flex items-center">
            <el-input-number
              v-if="isEditing(row.id, 'miniCoins')"
              v-model="getEditRow(row.id).miniCoins"
              :min="0"
              size="small"
              @blur="stopEdit()"
              @keyup.enter="stopEdit()"
            />
            <span v-else>{{ getDisplayValue(row, 'miniCoins') }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="薪资金额" width="120">
        <template #default="{ row }">
          {{ getDisplayValue(row, 'salaryAmount') }}
        </template>
      </el-table-column>
      <el-table-column label="备注">
        <template #default="{ row }">
          <div @click="startEdit(row, 'remark')" class="cursor-pointer min-h-[32px] flex items-center">
            <el-input
              v-if="isEditing(row.id, 'remark')"
              v-model="getEditRow(row.id).remark"
              size="small"
              @blur="stopEdit()"
              @keyup.enter="stopEdit()"
            />
            <span v-else>{{ getDisplayValue(row, 'remark') ?? '-' }}</span>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  getSalaryList,
  batchSaveSalary,
  calculateSalaries,
  archiveSalary,
} from '@/api/salary'
import type { SalaryRecord } from '@/api/salary'

const authStore = useAuthStore()
const canEdit = computed(() => ['ADMIN', 'LEADER'].includes(authStore.role))

const records = ref<SalaryRecord[]>([])
const loading = ref(false)
const saving = ref(false)
const calculating = ref(false)
const archiving = ref(false)
const globalError = ref('')

// --- Inline editing state ---
const editedRows = reactive<Map<number, SalaryRecord>>(new Map())
const editingCell = ref<{ id: number; field: string } | null>(null)
const violatingIds = ref<Set<number>>(new Set())

const editedCount = computed(() => editedRows.size)

const displayRecords = computed(() => records.value)

function getEditRow(id: number): SalaryRecord {
  if (!editedRows.has(id)) {
    const original = records.value.find((r) => r.id === id)
    if (original) editedRows.set(id, { ...original })
  }
  return editedRows.get(id)!
}

function getDisplayValue(row: SalaryRecord, field: string): any {
  const edited = editedRows.get(row.id)
  if (edited) return (edited as any)[field]
  return (row as any)[field]
}

function isEditing(id: number, field: string): boolean {
  if (!canEdit.value) return false
  return editingCell.value?.id === id && editingCell.value?.field === field
}

function startEdit(row: SalaryRecord, field: string) {
  if (!canEdit.value) return
  // Ensure edit row exists
  getEditRow(row.id)
  editingCell.value = { id: row.id, field }
}

function stopEdit() {
  editingCell.value = null
}

function rowClassName({ row }: { row: SalaryRecord }) {
  if (violatingIds.value.has(row.id)) return 'violating-row'
  return ''
}

// --- API calls ---
async function fetchSalaryList() {
  loading.value = true
  try {
    const res = await getSalaryList()
    records.value = res.data ?? []
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

async function handleBatchSave() {
  globalError.value = ''
  violatingIds.value.clear()

  const editedList = Array.from(editedRows.values())
  if (editedList.length === 0) return

  // Client-side validation
  const errors: string[] = []
  const badIds = new Set<number>()
  let totalMiniCoins = 0

  for (const r of records.value) {
    const edited = editedRows.get(r.id)
    const mc = edited ? edited.miniCoins : r.miniCoins
    totalMiniCoins += mc
    if (mc < 200 || mc > 400) {
      errors.push(`成员 ${r.userId} 迷你币 ${mc} 不在 [200, 400] 范围内`)
      badIds.add(r.id)
    }
  }

  if (totalMiniCoins > 2000) {
    errors.push(`迷你币总额 ${totalMiniCoins} 超过 2000 上限`)
  }

  if (records.value.length !== 5) {
    errors.push(`正式成员数量为 ${records.value.length}，应为 5 人`)
  }

  if (errors.length > 0) {
    globalError.value = errors.join('；')
    violatingIds.value = badIds
    return
  }

  saving.value = true
  try {
    const res = await batchSaveSalary({
      records: editedList,
      operatorId: authStore.user?.id ?? 0,
    })
    const data = res.data
    if (data?.success) {
      ElMessage.success('批量保存成功')
      editedRows.clear()
      violatingIds.value.clear()
      await fetchSalaryList()
    } else {
      globalError.value = data?.globalError ?? '保存失败'
      if (data?.violatingUserIds?.length) {
        const idSet = new Set(data.violatingUserIds)
        records.value.forEach((r) => {
          if (idSet.has(r.userId)) violatingIds.value.add(r.id)
        })
      }
    }
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

async function handleCalculate() {
  calculating.value = true
  try {
    await calculateSalaries()
    ElMessage.success('薪资计算完成')
    await fetchSalaryList()
  } catch {
    // handled by interceptor
  } finally {
    calculating.value = false
  }
}

async function handleArchive() {
  try {
    await ElMessageBox.confirm('确定要归档当前薪资数据吗？', '确认归档', { type: 'warning' })
  } catch {
    return
  }
  archiving.value = true
  try {
    await archiveSalary(authStore.user?.id ?? 0)
    ElMessage.success('归档成功')
    await fetchSalaryList()
  } catch {
    // handled by interceptor
  } finally {
    archiving.value = false
  }
}

onMounted(fetchSalaryList)
</script>

<style scoped>
:deep(.violating-row) {
  background-color: #fef2f2 !important;
}
:deep(.violating-row td) {
  background-color: #fef2f2 !important;
}
</style>
