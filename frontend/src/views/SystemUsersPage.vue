<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-xl font-semibold text-gray-800">用户管理</h2>
      <el-button type="primary" @click="showCreateDialog">新增用户</el-button>
    </div>

    <el-table :data="users" v-loading="loading" stripe class="w-full">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="160" />
      <el-table-column prop="role" label="角色" width="140">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" min-width="200">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" :type="row.enabled ? 'warning' : 'success'"
            @click="toggleEnabled(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password
            :placeholder="isEdit ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="选择角色" class="w-full">
            <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAllUsers, createUser, updateUser, deleteUser } from '@/api/admin'
import type { UserDTO } from '@/api/admin'

const users = ref<UserDTO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  role: 'MEMBER',
  enabled: true,
})

const roleOptions = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'LEADER', label: '组长' },
  { value: 'VICE_LEADER', label: '副组长' },
  { value: 'MEMBER', label: '正式成员' },
  { value: 'INTERN', label: '实习成员' },
  { value: 'APPLICANT', label: '申请者' },
]

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员', LEADER: '组长', VICE_LEADER: '副组长',
  MEMBER: '正式成员', INTERN: '实习成员', APPLICANT: '申请者',
}

function roleLabel(role: string) { return ROLE_LABELS[role] ?? role }

function roleTagType(role: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    ADMIN: 'danger', LEADER: 'warning', VICE_LEADER: '',
    MEMBER: 'success', INTERN: 'info', APPLICANT: 'info',
  }
  return map[role] ?? 'info'
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await listAllUsers()
    users.value = res.data ?? []
  } catch { /* interceptor */ } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  isEdit.value = false
  editingId.value = null
  form.username = ''
  form.password = ''
  form.role = 'MEMBER'
  form.enabled = true
  dialogVisible.value = true
}

function showEditDialog(row: UserDTO) {
  isEdit.value = true
  editingId.value = row.id
  form.username = row.username
  form.password = ''
  form.role = row.role
  form.enabled = row.enabled
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateUser(editingId.value, {
        role: form.role,
        enabled: String(form.enabled),
        password: form.password || undefined,
      })
      ElMessage.success('用户更新成功')
    } else {
      if (!form.username || !form.password) {
        ElMessage.warning('用户名和密码不能为空')
        return
      }
      await createUser({
        username: form.username,
        password: form.password,
        role: form.role,
        enabled: String(form.enabled),
      })
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    await fetchUsers()
  } catch { /* interceptor */ } finally {
    submitting.value = false
  }
}

async function toggleEnabled(row: UserDTO) {
  try {
    await updateUser(row.id, { enabled: String(!row.enabled) })
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    await fetchUsers()
  } catch { /* interceptor */ }
}

async function handleDelete(row: UserDTO) {
  try {
    await ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '确认删除', { type: 'warning' })
  } catch { return }
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await fetchUsers()
  } catch { /* interceptor */ }
}

onMounted(fetchUsers)
</script>
