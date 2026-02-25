<template>
  <div class="public-link-manager">
    <div class="flex justify-between items-center mb-4">
      <h3 class="text-lg font-semibold">公开问卷链接</h3>
      <el-button type="primary" @click="showGenerateDialog = true">生成公开链接</el-button>
    </div>

    <el-table v-loading="loading" :data="links" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="linkToken" label="链接标识" min-width="200">
        <template #default="{ row }">
          <span class="font-mono text-sm">{{ row.linkToken }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="templateId" label="模板ID" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" size="small">
            {{ row.active ? '有效' : '已失效' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="过期时间" width="180">
        <template #default="{ row }">{{ row.expiresAt ? formatTime(row.expiresAt) : '永不过期' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="copyLink(row.linkToken)">复制链接</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Generate Dialog -->
    <el-dialog v-model="showGenerateDialog" title="生成公开链接" width="450px">
      <el-form label-width="100px">
        <el-form-item label="问卷模板">
          <el-select v-model="selectedTemplateId" placeholder="请选择问卷模板" style="width: 100%">
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="t.title"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" :loading="generating" :disabled="!selectedTemplateId" @click="handleGenerate">
          生成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPublicLinks, generatePublicLink, type PublicLinkDTO } from '@/api/application'
import { getTemplates, type TemplateDTO } from '@/api/questionnaire'

const loading = ref(false)
const links = ref<PublicLinkDTO[]>([])
const templates = ref<TemplateDTO[]>([])
const showGenerateDialog = ref(false)
const selectedTemplateId = ref<number | null>(null)
const generating = ref(false)

function formatTime(dt: string) {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 19)
}

function buildFullUrl(linkToken: string) {
  return `${window.location.origin}/public/questionnaire/${linkToken}`
}

async function copyLink(linkToken: string) {
  try {
    await navigator.clipboard.writeText(buildFullUrl(linkToken))
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

async function fetchLinks() {
  loading.value = true
  try {
    const res = await getPublicLinks()
    links.value = res.data
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function fetchTemplates() {
  try {
    const res = await getTemplates()
    templates.value = res.data
  } catch {
    // handled by interceptor
  }
}

async function handleGenerate() {
  if (!selectedTemplateId.value) return
  generating.value = true
  try {
    await generatePublicLink({ templateId: selectedTemplateId.value })
    ElMessage.success('公开链接已生成')
    showGenerateDialog.value = false
    selectedTemplateId.value = null
    await fetchLinks()
  } catch {
    // handled by interceptor
  } finally {
    generating.value = false
  }
}

onMounted(() => {
  fetchLinks()
  fetchTemplates()
})
</script>
