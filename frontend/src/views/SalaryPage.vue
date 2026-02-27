<template>
  <div class="salary-page p-6">
    <h2 class="text-xl font-semibold text-gray-800 mb-5">薪资管理</h2>

    <!-- Stats cards -->
    <div class="grid grid-cols-1 md:grid-cols-5 gap-5 mb-6">
      <div class="stat-card">
        <div class="stat-card-inner">
          <div>
            <div class="stat-label">组长</div>
            <div class="stat-value">{{ roleCount('LEADER') }}</div>
          </div>
          <div class="stat-icon pink-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-inner">
          <div>
            <div class="stat-label">副组长</div>
            <div class="stat-value">{{ roleCount('VICE_LEADER') }}</div>
          </div>
          <div class="stat-icon purple-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-inner">
          <div>
            <div class="stat-label">实习组员</div>
            <div class="stat-value">{{ roleCount('INTERN') }}</div>
          </div>
          <div class="stat-icon orange-bg">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-inner">
          <div>
            <div class="stat-label">未保存修改</div>
            <div class="stat-value" :class="editedCount > 0 ? '' : 'muted'">{{ editedCount }}</div>
          </div>
          <div class="stat-icon" :class="editedCount > 0 ? 'pink-bg' : 'muted-bg'">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </div>
        </div>
      </div>
      <!-- 薪酬池概览 -->
      <!-- 薪酬池概览 -->
      <div class="stat-card pool-card">
        <div class="pool-header">
          <span class="stat-label">薪酬池{{ currentPeriod ? ' · ' + currentPeriod : '' }}</span>
          <span class="pool-total">{{ poolTotal }} 迷你币</span>
        </div>
        <div class="pool-progress-bar">
          <div class="pool-progress-fill" :style="{ width: poolPercent + '%' }" :class="{ 'pool-over': poolRemaining < 0 }"></div>
        </div>
        <div class="pool-details">
          <div class="pool-detail-item">
            <span class="pool-dot pool-dot--used"></span>
            <span>已分配 <b>{{ poolAllocated }}</b></span>
          </div>
          <div class="pool-detail-item">
            <span class="pool-dot pool-dot--remain"></span>
            <span>剩余 <b :class="{ 'pool-negative': poolRemaining < 0 }">{{ poolRemaining }}</b></span>
          </div>
        </div>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar-card mb-5">
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div class="flex items-center gap-2">
          <el-select v-model="currentPeriod" @change="onPeriodChange" placeholder="选择周期" class="period-select">
            <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.period">
              <span>{{ p.period }}</span>
              <el-tag v-if="p.archived" size="small" type="info" class="ml-2">已归档</el-tag>
            </el-option>
          </el-select>
          <el-button class="btn-outline" @click="openCreatePeriodDialog">新建周期</el-button>
          <el-button class="btn-outline" @click="fetchMembers" :loading="loading">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-1"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
            刷新
          </el-button>
          <el-button class="btn-outline" @click="handleExportExcel" :disabled="members.length === 0">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-1"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            导出Excel
          </el-button>
        </div>
        <div class="flex items-center gap-2">
          <el-button v-if="canEdit && !isCurrentPeriodArchived && !noPeriodSelected" class="btn-pink" :loading="saving" :disabled="editedCount === 0" @click="handleBatchSave">
            保存修改
          </el-button>
          <el-button v-if="canEdit && !isCurrentPeriodArchived && !noPeriodSelected" class="btn-outline" @click="handleCalculate" :loading="calculating">计算薪资</el-button>
          <el-button v-if="canEdit && !isCurrentPeriodArchived && !noPeriodSelected" class="btn-outline" @click="handleArchive" :loading="archiving">归档</el-button>
          <el-tag v-if="isCurrentPeriodArchived" type="info" size="large" effect="dark" class="archived-tag">🔒 已归档</el-tag>
          <el-button v-if="canEdit" class="btn-outline" @click="openConfigDrawer">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mr-1"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
            配置管理
          </el-button>
        </div>
      </div>
    </div>

    <!-- No period selected notice -->
    <div v-if="noPeriodSelected" class="no-period-banner mb-4">
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      <span>请先选择或新建一个薪酬周期</span>
    </div>

    <!-- Archived period read-only notice -->
    <div v-if="isCurrentPeriodArchived" class="archived-banner mb-4">
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
      <span>该周期已归档，数据已锁定为只读状态</span>
    </div>

    <!-- Validation error banner -->
    <div v-if="globalError" class="error-banner mb-4">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
      {{ globalError }}
    </div>

    <!-- Salary table -->
    <div class="table-card" :class="{ 'table-card--archived': isCurrentPeriodArchived }">
      <el-table :data="members" v-loading="loading" class="salary-table" :row-class-name="rowClassName">
        <el-table-column label="序号" width="60" align="center">
          <template #default="{ $index }">
            <span class="row-index">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="成员" width="130" fixed sortable>
          <template #default="{ row }">
            <div class="member-cell">
              <div class="avatar-circle" :class="'avatar-' + row.role.toLowerCase()">
                {{ row.username.charAt(0) }}
              </div>
              <span class="member-name">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100" align="center" sortable :sort-method="sortByRole">
          <template #default="{ row }">
            <span class="role-badge" :class="'role-' + row.role.toLowerCase()">{{ roleLabel(row.role) }}</span>
          </template>
        </el-table-column>

        <!-- 汇总列 -->
        <el-table-column v-if="!allInterns" label="迷你币" width="100" align="center" class-name="computed-col" sortable :sort-method="sortByMiniCoins">
          <template #default="{ row }">
            <span v-if="!isIntern(row)" class="computed-points">{{ computeMiniCoins(row) }}</span>
            <span v-else class="muted-text">-</span>
          </template>
        </el-table-column>

        <el-table-column label="总积分" width="100" align="center" class-name="computed-col computed-col--total" sortable :sort-method="sortByTotal">
          <template #default="{ row }">
            <span class="total-points">{{ computeTotalPoints(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="基础积分" width="100" align="center" class-name="computed-col" sortable :sort-method="sortByBase">
          <template #default="{ row }">
            <span class="computed-points">{{ computeBasePoints(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="奖励积分" width="100" align="center" class-name="computed-col" sortable :sort-method="sortByBonus">
          <template #default="{ row }">
            <span class="computed-points">{{ computeBonusPoints(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="签到积分" width="100" align="center" class-name="computed-col" sortable :sort-method="sortByCheckin">
          <template #default="{ row }">
            <span :class="computeCheckinPoints(row) < 0 ? 'negative-points' : 'positive-points'">
              {{ computeCheckinPoints(row) }}
            </span>
          </template>
        </el-table-column>

        <!-- 基础职责积分维度 -->
        <el-table-column label="社群活跃度" width="130" sortable :sort-method="sortByCommunity">
          <template #default="{ row }">
            <div class="editable-cell" @click="startEdit(row, 'communityActivityPoints')">
              <el-tooltip v-if="hasValidationError(row.userId, 'communityActivityPoints')" :content="getValidationError(row.userId, 'communityActivityPoints')" placement="top">
                <el-input-number
                  v-if="isEditing(row.userId, 'communityActivityPoints')"
                  v-model="getEditRow(row.userId).communityActivityPoints"
                  :min="0" :max="configForm.community_activity_max" size="small" controls-position="right"
                  class="validation-error"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else class="validation-error-text">{{ getDisplayValue(row, 'communityActivityPoints') }}</span>
              </el-tooltip>
              <template v-else>
                <el-input-number
                  v-if="isEditing(row.userId, 'communityActivityPoints')"
                  v-model="getEditRow(row.userId).communityActivityPoints"
                  :min="0" :max="configForm.community_activity_max" size="small" controls-position="right"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else>{{ getDisplayValue(row, 'communityActivityPoints') }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="签到次数" width="130" sortable :sort-method="sortByCheckinCount">
          <template #default="{ row }">
            <div class="editable-cell" @click="startEdit(row, 'checkinCount')">
              <el-tooltip v-if="hasValidationError(row.userId, 'checkinCount')" :content="getValidationError(row.userId, 'checkinCount')" placement="top">
                <el-input-number
                  v-if="isEditing(row.userId, 'checkinCount')"
                  v-model="getEditRow(row.userId).checkinCount"
                  :min="0" size="small" controls-position="right"
                  class="validation-error"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else class="validation-error-text">{{ getDisplayValue(row, 'checkinCount') }}</span>
              </el-tooltip>
              <template v-else>
                <el-input-number
                  v-if="isEditing(row.userId, 'checkinCount')"
                  v-model="getEditRow(row.userId).checkinCount"
                  :min="0" size="small" controls-position="right"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else>{{ getDisplayValue(row, 'checkinCount') }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="违规处理次数" width="140" sortable :sort-method="sortByViolation">
          <template #default="{ row }">
            <div class="editable-cell" @click="startEdit(row, 'violationHandlingCount')">
              <el-tooltip v-if="hasValidationError(row.userId, 'violationHandlingCount')" :content="getValidationError(row.userId, 'violationHandlingCount')" placement="top">
                <el-input-number
                  v-if="isEditing(row.userId, 'violationHandlingCount')"
                  v-model="getEditRow(row.userId).violationHandlingCount"
                  :min="0" size="small" controls-position="right"
                  class="validation-error"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else class="validation-error-text">{{ getDisplayValue(row, 'violationHandlingCount') }}</span>
              </el-tooltip>
              <template v-else>
                <el-input-number
                  v-if="isEditing(row.userId, 'violationHandlingCount')"
                  v-model="getEditRow(row.userId).violationHandlingCount"
                  :min="0" size="small" controls-position="right"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else>{{ getDisplayValue(row, 'violationHandlingCount') }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="任务完成积分" width="140" sortable :sort-method="sortByTask">
          <template #default="{ row }">
            <div class="editable-cell" @click="startEdit(row, 'taskCompletionPoints')">
              <el-tooltip v-if="hasValidationError(row.userId, 'taskCompletionPoints')" :content="getValidationError(row.userId, 'taskCompletionPoints')" placement="top">
                <el-input-number
                  v-if="isEditing(row.userId, 'taskCompletionPoints')"
                  v-model="getEditRow(row.userId).taskCompletionPoints"
                  :min="0" :max="configForm.task_completion_max" size="small" controls-position="right"
                  class="validation-error"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else class="validation-error-text">{{ getDisplayValue(row, 'taskCompletionPoints') }}</span>
              </el-tooltip>
              <template v-else>
                <el-input-number
                  v-if="isEditing(row.userId, 'taskCompletionPoints')"
                  v-model="getEditRow(row.userId).taskCompletionPoints"
                  :min="0" :max="configForm.task_completion_max" size="small" controls-position="right"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else>{{ getDisplayValue(row, 'taskCompletionPoints') }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="公告次数" width="120" sortable :sort-method="sortByAnnouncement">
          <template #default="{ row }">
            <div class="editable-cell" @click="startEdit(row, 'announcementCount')">
              <el-tooltip v-if="hasValidationError(row.userId, 'announcementCount')" :content="getValidationError(row.userId, 'announcementCount')" placement="top">
                <el-input-number
                  v-if="isEditing(row.userId, 'announcementCount')"
                  v-model="getEditRow(row.userId).announcementCount"
                  :min="0" size="small" controls-position="right"
                  class="validation-error"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else class="validation-error-text">{{ getDisplayValue(row, 'announcementCount') }}</span>
              </el-tooltip>
              <template v-else>
                <el-input-number
                  v-if="isEditing(row.userId, 'announcementCount')"
                  v-model="getEditRow(row.userId).announcementCount"
                  :min="0" size="small" controls-position="right"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else>{{ getDisplayValue(row, 'announcementCount') }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <!-- 卓越贡献积分维度 -->
        <el-table-column label="活动举办积分" width="140" sortable :sort-method="sortByEvent">
          <template #default="{ row }">
            <div class="editable-cell" @click="startEdit(row, 'eventHostingPoints')">
              <el-tooltip v-if="hasValidationError(row.userId, 'eventHostingPoints')" :content="getValidationError(row.userId, 'eventHostingPoints')" placement="top">
                <el-input-number
                  v-if="isEditing(row.userId, 'eventHostingPoints')"
                  v-model="getEditRow(row.userId).eventHostingPoints"
                  :min="0" :max="configForm.event_hosting_max" size="small" controls-position="right"
                  class="validation-error"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else class="validation-error-text">{{ getDisplayValue(row, 'eventHostingPoints') }}</span>
              </el-tooltip>
              <template v-else>
                <el-input-number
                  v-if="isEditing(row.userId, 'eventHostingPoints')"
                  v-model="getEditRow(row.userId).eventHostingPoints"
                  :min="0" :max="configForm.event_hosting_max" size="small" controls-position="right"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else>{{ getDisplayValue(row, 'eventHostingPoints') }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="生日福利积分" width="140" sortable :sort-method="sortByBirthday">
          <template #default="{ row }">
            <div class="editable-cell" @click="startEdit(row, 'birthdayBonusPoints')">
              <el-tooltip v-if="hasValidationError(row.userId, 'birthdayBonusPoints')" :content="getValidationError(row.userId, 'birthdayBonusPoints')" placement="top">
                <el-input-number
                  v-if="isEditing(row.userId, 'birthdayBonusPoints')"
                  v-model="getEditRow(row.userId).birthdayBonusPoints"
                  :min="0" :max="configForm.birthday_bonus_max" size="small" controls-position="right"
                  class="validation-error"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else class="validation-error-text">{{ getDisplayValue(row, 'birthdayBonusPoints') }}</span>
              </el-tooltip>
              <template v-else>
                <el-input-number
                  v-if="isEditing(row.userId, 'birthdayBonusPoints')"
                  v-model="getEditRow(row.userId).birthdayBonusPoints"
                  :min="0" :max="configForm.birthday_bonus_max" size="small" controls-position="right"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else>{{ getDisplayValue(row, 'birthdayBonusPoints') }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="月度评议积分" width="140" sortable :sort-method="sortByMonthly">
          <template #default="{ row }">
            <div class="editable-cell" @click="startEdit(row, 'monthlyExcellentPoints')">
              <el-tooltip v-if="hasValidationError(row.userId, 'monthlyExcellentPoints')" :content="getValidationError(row.userId, 'monthlyExcellentPoints')" placement="top">
                <el-input-number
                  v-if="isEditing(row.userId, 'monthlyExcellentPoints')"
                  v-model="getEditRow(row.userId).monthlyExcellentPoints"
                  :min="0" :max="configForm.monthly_excellent_max" size="small" controls-position="right"
                  class="validation-error"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else class="validation-error-text">{{ getDisplayValue(row, 'monthlyExcellentPoints') }}</span>
              </el-tooltip>
              <template v-else>
                <el-input-number
                  v-if="isEditing(row.userId, 'monthlyExcellentPoints')"
                  v-model="getEditRow(row.userId).monthlyExcellentPoints"
                  :min="0" :max="configForm.monthly_excellent_max" size="small" controls-position="right"
                  @blur="stopEdit()" @keyup.enter="stopEdit()"
                />
                <span v-else>{{ getDisplayValue(row, 'monthlyExcellentPoints') }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <div @click="startEdit(row, 'remark')" class="editable-cell">
              <el-input
                v-if="isEditing(row.userId, 'remark')"
                v-model="getEditRow(row.userId).remark"
                size="small"
                @blur="stopEdit()" @keyup.enter="stopEdit()"
              />
              <span v-else class="remark-text">{{ getDisplayValue(row, 'remark') ?? '-' }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Config Management Drawer -->
    <el-drawer v-if="canEdit" v-model="configDrawerVisible" title="配置管理" size="520px" direction="rtl">
      <div class="config-drawer-body">
        <!-- 保存按钮 -->
        <div class="config-actions mb-4">
          <el-button class="btn-pink" :loading="configSaving" @click="handleSaveConfig">保存配置</el-button>
        </div>

        <!-- 配置校验错误 -->
        <div v-if="configError" class="error-banner mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          {{ configError }}
        </div>

        <!-- Section 1: 薪酬池参数 -->
        <div class="config-section">
          <h4 class="config-section-title">薪酬池参数</h4>
          <el-form label-position="left" label-width="160px" size="small">
            <el-form-item label="薪酬池总额（迷你币）">
              <el-input-number v-model="configForm.salary_pool_total" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="正式成员数量（人）">
              <el-input-number v-model="configForm.formal_member_count" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="基准分配额（迷你币）">
              <el-input-number v-model="configForm.base_allocation" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="个人最低迷你币（枚）">
              <el-input-number v-model="configForm.mini_coins_min" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="个人最高迷你币（枚）">
              <el-input-number v-model="configForm.mini_coins_max" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="积分转迷你币比例（倍）">
              <el-input-number v-model="configForm.points_to_coins_ratio" :min="1" controls-position="right" />
            </el-form-item>
          </el-form>
        </div>

        <!-- Section 2: 积分维度配置 -->
        <div class="config-section">
          <h4 class="config-section-title">积分维度配置</h4>
          <el-form label-position="left" label-width="180px" size="small">
            <el-form-item label="社群活跃度上限（分）">
              <el-input-number v-model="configForm.community_activity_max" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="违规处理积分系数（分/次）">
              <el-input-number v-model="configForm.violation_handling_multiplier" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="公告积分系数（分/次）">
              <el-input-number v-model="configForm.announcement_multiplier" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="任务完成积分上限（分）">
              <el-input-number v-model="configForm.task_completion_max" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="活动举办积分上限（分）">
              <el-input-number v-model="configForm.event_hosting_max" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="生日福利积分上限（分）">
              <el-input-number v-model="configForm.birthday_bonus_max" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="月度评议积分上限（分）">
              <el-input-number v-model="configForm.monthly_excellent_max" :min="1" controls-position="right" />
            </el-form-item>
          </el-form>
        </div>

        <!-- Section 3: 签到奖惩表 -->
        <div class="config-section">
          <h4 class="config-section-title">签到奖惩表</h4>
          <el-table :data="configCheckinTiers" size="small" border class="config-tier-table">
            <el-table-column label="最低次数（次）" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.minCount" :min="0" size="small" controls-position="right" class="tier-input" />
              </template>
            </el-table-column>
            <el-table-column label="最高次数（次）" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.maxCount" :min="0" size="small" controls-position="right" class="tier-input" />
              </template>
            </el-table-column>
            <el-table-column label="积分（分）" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.points" size="small" controls-position="right" class="tier-input" />
              </template>
            </el-table-column>
            <el-table-column label="等级标记" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.label" size="small" />
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- Section 4: 流转阈值 -->
        <div class="config-section">
          <h4 class="config-section-title">流转阈值</h4>
          <el-form label-position="left" label-width="180px" size="small">
            <el-form-item label="转正积分阈值（分）">
              <el-input-number v-model="configForm.promotion_points_threshold" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="降级薪酬阈值（分）">
              <el-input-number v-model="configForm.demotion_salary_threshold" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="降级连续月数（月）">
              <el-input-number v-model="configForm.demotion_consecutive_months" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="开除积分阈值（分）">
              <el-input-number v-model="configForm.dismissal_points_threshold" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="开除连续月数（月）">
              <el-input-number v-model="configForm.dismissal_consecutive_months" :min="1" controls-position="right" />
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-drawer>

    <!-- Create Period Dialog -->
    <el-dialog v-model="showCreatePeriodDialog" title="新建周期" width="360px" :close-on-click-modal="false">
      <el-date-picker
        v-model="newPeriodDate"
        type="month"
        placeholder="选择年月"
        format="YYYY-MM"
        value-format="YYYY-MM"
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="showCreatePeriodDialog = false">取消</el-button>
        <el-button class="btn-pink" :disabled="!newPeriodDate" @click="handleCreatePeriod">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  getSalaryMembers,
  batchSaveSalary,
  calculateAndDistribute,
  archiveSalary,
  getSalaryPeriods,
  createSalaryPeriod,
  getPoolSummary,
} from '@/api/salary'
import type { SalaryMemberDTO, SalaryPeriodDTO } from '@/api/salary'
import { getCheckinTiers, getSalaryConfig, updateSalaryConfig, updateCheckinTiers } from '@/api/salaryConfig'
import type { CheckinTier, SalaryConfigMap } from '@/api/salaryConfig'
import * as XLSX from 'xlsx'

const authStore = useAuthStore()
const canEdit = computed(() => ['ADMIN', 'LEADER', 'VICE_LEADER'].includes(authStore.role))

const currentPeriod = ref('')
const periods = ref<SalaryPeriodDTO[]>([])
const members = ref<SalaryMemberDTO[]>([])
const loading = ref(false)
const saving = ref(false)
const calculating = ref(false)
const archiving = ref(false)
const globalError = ref('')
const checkinTiers = ref<CheckinTier[]>([
  { minCount: 0, maxCount: 19, points: -20, label: '不合格' },
  { minCount: 20, maxCount: 29, points: -10, label: '需改进' },
  { minCount: 30, maxCount: 39, points: 0, label: '合格' },
  { minCount: 40, maxCount: 49, points: 30, label: '良好' },
  { minCount: 50, maxCount: 999, points: 50, label: '优秀' },
])

// --- Inline editing state ---
const editedRows = reactive<Map<number, SalaryMemberDTO>>(new Map())
const editingCell = ref<{ userId: number; field: string } | null>(null)
const violatingIds = ref<Set<number>>(new Set())
const validationErrors = reactive<Map<string, string>>(new Map())

const editedCount = computed(() => editedRows.size)

const isCurrentPeriodArchived = computed(() => {
  const p = periods.value.find(p => p.period === currentPeriod.value)
  return p?.archived ?? false
})

const noPeriodSelected = computed(() => !currentPeriod.value)

const allInterns = computed(() =>
  members.value.length > 0 && members.value.every(m => isIntern(m))
)

// --- Salary pool overview (from backend API) ---
const poolTotal = ref(0)
const poolAllocated = ref(0)
const poolRemaining = ref(0)
const poolPercent = computed(() => {
  if (poolTotal.value <= 0) return 0
  return Math.min(Math.round((poolAllocated.value / poolTotal.value) * 100), 100)
})

async function fetchPoolSummary() {
  if (!currentPeriod.value) {
    // 没选周期时显示配置总额，已分配为0
    poolTotal.value = configForm.salary_pool_total
    poolAllocated.value = 0
    poolRemaining.value = configForm.salary_pool_total
    return
  }
  try {
    const res = await getPoolSummary(currentPeriod.value)
    if (res.data) {
      poolTotal.value = res.data.total
      poolAllocated.value = res.data.allocated
      poolRemaining.value = res.data.remaining
    }
  } catch { /* keep previous values */ }
}

// --- Helper: parse config value with fallback (handles "0" correctly) ---
function parseConfigInt(val: string | undefined | null, fallback: number): number {
  if (val == null || val === '') return fallback
  const n = Number(val)
  return Number.isNaN(n) ? fallback : n
}

// --- Input validation ranges (dynamic based on config) ---
function getFieldRange(field: string): { min: number; max: number; label: string } | undefined {
  const ranges: Record<string, { min: number; max: number; label: string }> = {
    communityActivityPoints: { min: 0, max: configForm.community_activity_max, label: '社群活跃度' },
    checkinCount: { min: 0, max: Infinity, label: '签到次数' },
    violationHandlingCount: { min: 0, max: Infinity, label: '违规处理次数' },
    taskCompletionPoints: { min: 0, max: configForm.task_completion_max, label: '任务完成积分' },
    announcementCount: { min: 0, max: Infinity, label: '公告次数' },
    eventHostingPoints: { min: 0, max: configForm.event_hosting_max, label: '活动举办积分' },
    birthdayBonusPoints: { min: 0, max: configForm.birthday_bonus_max, label: '生日福利积分' },
    monthlyExcellentPoints: { min: 0, max: configForm.monthly_excellent_max, label: '月度评议积分' },
  }
  return ranges[field]
}

function validateField(userId: number, field: string, value: number | null | undefined): void {
  const key = `${userId}:${field}`
  const range = getFieldRange(field)
  if (!range) {
    validationErrors.delete(key)
    return
  }
  const v = value ?? 0
  if (v < range.min || v > range.max) {
    const maxLabel = range.max === Infinity ? '∞' : String(range.max)
    validationErrors.set(key, `${range.label}范围: ${range.min}-${maxLabel}`)
  } else {
    validationErrors.delete(key)
  }
}

function hasValidationError(userId: number, field: string): boolean {
  const key = `${userId}:${field}`
  // Check current value
  const edited = editedRows.get(userId) ?? editingBuffer.get(userId)
  if (edited) {
    validateField(userId, field, (edited as any)[field])
  }
  return validationErrors.has(key)
}

function getValidationError(userId: number, field: string): string {
  return validationErrors.get(`${userId}:${field}`) ?? ''
}

function isIntern(row: SalaryMemberDTO): boolean {
  return row.role === 'INTERN' || row.role.includes('实习') || row.role.toUpperCase().includes('INTERN')
}

function roleCount(role: string) {
  return members.value.filter(m => m.role === role).length
}

function roleLabel(role: string): string {
  const m: Record<string, string> = {
    ADMIN: '管理员', LEADER: '组长', VICE_LEADER: '副组长', MEMBER: '正式成员', INTERN: '实习组员',
  }
  return m[role] ?? role
}

// Temporary editing buffer - holds a copy while editing, but doesn't count as "modified" yet
const editingBuffer = reactive<Map<number, SalaryMemberDTO>>(new Map())

function getEditRow(userId: number): SalaryMemberDTO {
  // Return from editedRows if already marked as modified
  if (editedRows.has(userId)) {
    return editedRows.get(userId)!
  }
  // Otherwise use/create a temporary buffer for editing
  if (!editingBuffer.has(userId)) {
    const original = members.value.find(r => r.userId === userId)
    if (original) editingBuffer.set(userId, { ...original })
  }
  return editingBuffer.get(userId)!
}

function getDisplayValue(row: SalaryMemberDTO, field: string): any {
  const edited = editedRows.get(row.userId) ?? editingBuffer.get(row.userId)
  if (edited) return (edited as any)[field]
  return (row as any)[field]
}

// --- Calculation functions ---
function lookupCheckinTier(count: number): number {
  const c = count < 0 ? 0 : count
  for (const tier of checkinTiers.value) {
    if (c >= tier.minCount && c <= tier.maxCount) {
      return tier.points
    }
  }
  return 0
}

function computeCheckinPoints(row: SalaryMemberDTO): number {
  const edited = editedRows.get(row.userId) ?? editingBuffer.get(row.userId)
  const r = edited ?? row
  return lookupCheckinTier(r.checkinCount ?? 0)
}

function computeBasePoints(row: SalaryMemberDTO): number {
  const edited = editedRows.get(row.userId) ?? editingBuffer.get(row.userId)
  const r = edited ?? row
  const checkinPts = lookupCheckinTier(r.checkinCount ?? 0)
  const violationPts = (r.violationHandlingCount ?? 0) * configForm.violation_handling_multiplier
  const announcementPts = (r.announcementCount ?? 0) * configForm.announcement_multiplier
  return (r.communityActivityPoints ?? 0)
    + checkinPts
    + violationPts
    + (r.taskCompletionPoints ?? 0)
    + announcementPts
}

function computeBonusPoints(row: SalaryMemberDTO): number {
  const edited = editedRows.get(row.userId) ?? editingBuffer.get(row.userId)
  const r = edited ?? row
  return (r.eventHostingPoints ?? 0)
    + (r.birthdayBonusPoints ?? 0)
    + (r.monthlyExcellentPoints ?? 0)
}

function computeTotalPoints(row: SalaryMemberDTO): number {
  return computeBasePoints(row) + computeBonusPoints(row)
}

function computeMiniCoins(row: SalaryMemberDTO): number {
  const raw = computeTotalPoints(row) * configForm.points_to_coins_ratio
  return Math.min(Math.max(raw, configForm.mini_coins_min), configForm.mini_coins_max)
}

// --- Sort helpers for computed columns ---
function sortByRole(a: SalaryMemberDTO, b: SalaryMemberDTO) { return a.role.localeCompare(b.role) }
function sortByMiniCoins(a: SalaryMemberDTO, b: SalaryMemberDTO) { return computeMiniCoins(a) - computeMiniCoins(b) }
function sortByTotal(a: SalaryMemberDTO, b: SalaryMemberDTO) { return computeTotalPoints(a) - computeTotalPoints(b) }
function sortByBase(a: SalaryMemberDTO, b: SalaryMemberDTO) { return computeBasePoints(a) - computeBasePoints(b) }
function sortByBonus(a: SalaryMemberDTO, b: SalaryMemberDTO) { return computeBonusPoints(a) - computeBonusPoints(b) }
function sortByCheckin(a: SalaryMemberDTO, b: SalaryMemberDTO) { return computeCheckinPoints(a) - computeCheckinPoints(b) }
function sortByField(field: string) {
  return (a: SalaryMemberDTO, b: SalaryMemberDTO) => (getDisplayValue(a, field) ?? 0) - (getDisplayValue(b, field) ?? 0)
}
const sortByCommunity = sortByField('communityActivityPoints')
const sortByCheckinCount = sortByField('checkinCount')
const sortByViolation = sortByField('violationHandlingCount')
const sortByTask = sortByField('taskCompletionPoints')
const sortByAnnouncement = sortByField('announcementCount')
const sortByEvent = sortByField('eventHostingPoints')
const sortByBirthday = sortByField('birthdayBonusPoints')
const sortByMonthly = sortByField('monthlyExcellentPoints')

function isEditing(userId: number, field: string): boolean {
  if (!canEdit.value) return false
  return editingCell.value?.userId === userId && editingCell.value?.field === field
}

function startEdit(row: SalaryMemberDTO, field: string) {
  if (!canEdit.value) return
  if (isCurrentPeriodArchived.value) return
  if (noPeriodSelected.value) return
  getEditRow(row.userId)
  editingCell.value = { userId: row.userId, field }
}

function stopEdit() {
  if (editingCell.value) {
    const { userId } = editingCell.value
    const buffer = editingBuffer.get(userId)
    const original = members.value.find(r => r.userId === userId)
    if (buffer && original) {
      // Check if any editable field actually changed
      const editableFields = [
        'communityActivityPoints', 'checkinCount', 'violationHandlingCount',
        'taskCompletionPoints', 'announcementCount', 'eventHostingPoints',
        'birthdayBonusPoints', 'monthlyExcellentPoints', 'remark',
      ]
      const changed = editableFields.some(f => (buffer as any)[f] !== (original as any)[f])
      if (changed) {
        // Move to editedRows so it counts as modified
        editedRows.set(userId, buffer)
      } else if (!editedRows.has(userId)) {
        // No change and not previously modified - remove from buffer
        editingBuffer.delete(userId)
      }
    }
  }
  editingCell.value = null
}

function rowClassName({ row }: { row: SalaryMemberDTO }) {
  if (violatingIds.value.has(row.userId)) return 'violating-row'
  return ''
}

// --- API calls ---
async function fetchMembers() {
  if (!currentPeriod.value) {
    members.value = []
    editedRows.clear()
    editingBuffer.clear()
    return
  }
  loading.value = true
  globalError.value = ''
  try {
    const res = await getSalaryMembers(currentPeriod.value)
    members.value = res.data ?? []
    editedRows.clear()
    editingBuffer.clear()
    violatingIds.value.clear()
    validationErrors.clear()
  } catch {
    members.value = []
  } finally {
    loading.value = false
  }
  await fetchPoolSummary()
}

async function fetchCheckinTiers() {
  try {
    const res = await getCheckinTiers()
    if (res.data && res.data.length > 0) {
      checkinTiers.value = res.data
    }
  } catch {
    // Use default tiers on failure
  }
}

async function fetchPeriods() {
  try {
    const res = await getSalaryPeriods()
    periods.value = res.data ?? []
    // Default select the latest non-archived period
    const activePeriod = periods.value.find(p => !p.archived)
    if (activePeriod) {
      currentPeriod.value = activePeriod.period
    } else if (periods.value.length > 0) {
      // All archived, select the latest one
      currentPeriod.value = periods.value[0].period
    } else {
      currentPeriod.value = ''
    }
  } catch {
    periods.value = []
    currentPeriod.value = ''
  }
}

async function onPeriodChange() {
  await fetchMembers()
}

// --- Create Period Dialog ---
const showCreatePeriodDialog = ref(false)
const newPeriodDate = ref('')

function openCreatePeriodDialog() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  newPeriodDate.value = `${year}-${month}`
  showCreatePeriodDialog.value = true
}

async function handleCreatePeriod() {
  const period = newPeriodDate.value
  if (!period) return
  try {
    await createSalaryPeriod(period)
    ElMessage.success(`周期 ${period} 创建成功`)
    showCreatePeriodDialog.value = false
    newPeriodDate.value = ''
    await fetchPeriods()
    currentPeriod.value = period
    await fetchMembers()
  } catch {
    // handled by interceptor
  }
}

async function handleBatchSave() {
  globalError.value = ''
  violatingIds.value.clear()

  const editedList = Array.from(editedRows.values())
  if (editedList.length === 0) return

  const records = editedList.map(m => ({
    id: m.id ?? 0,
    userId: m.userId,
    basePoints: computeBasePoints(m),
    bonusPoints: computeBonusPoints(m),
    deductions: m.deductions ?? 0,
    totalPoints: computeTotalPoints(m),
    miniCoins: isIntern(m) ? 0 : computeMiniCoins(m),
    salaryAmount: m.salaryAmount,
    remark: m.remark,
    version: m.version ?? 0,
    period: currentPeriod.value,
    archived: false,
    communityActivityPoints: m.communityActivityPoints ?? 0,
    checkinCount: m.checkinCount ?? 0,
    checkinPoints: computeCheckinPoints(m),
    violationHandlingCount: m.violationHandlingCount ?? 0,
    violationHandlingPoints: (m.violationHandlingCount ?? 0) * configForm.violation_handling_multiplier,
    taskCompletionPoints: m.taskCompletionPoints ?? 0,
    announcementCount: m.announcementCount ?? 0,
    announcementPoints: (m.announcementCount ?? 0) * configForm.announcement_multiplier,
    eventHostingPoints: m.eventHostingPoints ?? 0,
    birthdayBonusPoints: m.birthdayBonusPoints ?? 0,
    monthlyExcellentPoints: m.monthlyExcellentPoints ?? 0,
  }))

  saving.value = true
  try {
    const res = await batchSaveSalary({
      records,
      operatorId: authStore.user?.id ?? 0,
    }, currentPeriod.value || undefined)
    const data = res.data
    if (data?.success) {
      ElMessage.success('保存成功')
      await fetchMembers()
    } else {
      globalError.value = data?.globalError ?? '保存失败'
      if (data?.violatingUserIds?.length) {
        data.violatingUserIds.forEach(uid => violatingIds.value.add(uid))
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
    await calculateAndDistribute(currentPeriod.value || undefined)
    ElMessage.success('薪资计算完成')
    await fetchMembers()
  } catch {
    // handled by interceptor
  } finally {
    calculating.value = false
  }
}

async function handleArchive() {
  try {
    await ElMessageBox.confirm('确定要归档当前薪资数据吗？', '确认归档', { type: 'warning' })
  } catch { return }
  archiving.value = true
  try {
    await archiveSalary(authStore.user?.id ?? 0, currentPeriod.value || undefined)
    ElMessage.success('归档成功')
    const archivedPeriod = currentPeriod.value
    await fetchPeriods()
    currentPeriod.value = archivedPeriod
    await fetchMembers()
  } catch {
    // handled by interceptor
  } finally {
    archiving.value = false
  }
}

// --- Export Excel ---
function handleExportExcel() {
  const rows = members.value.map((m, i) => {
    const r = editedRows.get(m.userId) ?? editingBuffer.get(m.userId) ?? m
    const row: Record<string, any> = {
      '序号': i + 1,
      '成员': m.username,
      '角色': roleLabel(m.role),
    }
    if (!allInterns.value) {
      row['迷你币'] = isIntern(m) ? '-' : computeMiniCoins(m)
    }
    row['总积分'] = computeTotalPoints(m)
    row['基础积分'] = computeBasePoints(m)
    row['奖励积分'] = computeBonusPoints(m)
    row['签到积分'] = computeCheckinPoints(m)
    row['社群活跃度'] = r.communityActivityPoints ?? 0
    row['签到次数'] = r.checkinCount ?? 0
    row['违规处理次数'] = r.violationHandlingCount ?? 0
    row['任务完成积分'] = r.taskCompletionPoints ?? 0
    row['公告次数'] = r.announcementCount ?? 0
    row['活动举办积分'] = r.eventHostingPoints ?? 0
    row['生日福利积分'] = r.birthdayBonusPoints ?? 0
    row['月度评议积分'] = r.monthlyExcellentPoints ?? 0
    row['备注'] = r.remark ?? ''
    return row
  })
  const ws = XLSX.utils.json_to_sheet(rows)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '薪资数据')
  XLSX.writeFile(wb, `薪资数据_${currentPeriod.value || 'unknown'}.xlsx`)
}

// --- Config Drawer state ---
const configDrawerVisible = ref(false)
const configSaving = ref(false)
const configError = ref('')
const configForm = reactive({
  salary_pool_total: 2000,
  formal_member_count: 5,
  base_allocation: 400,
  mini_coins_min: 200,
  mini_coins_max: 400,
  points_to_coins_ratio: 2,
  promotion_points_threshold: 100,
  demotion_salary_threshold: 150,
  demotion_consecutive_months: 2,
  dismissal_points_threshold: 100,
  dismissal_consecutive_months: 2,
  // 积分维度配置
  community_activity_max: 100,
  violation_handling_multiplier: 3,
  announcement_multiplier: 5,
  monthly_excellent_max: 30,
  task_completion_max: 100,
  event_hosting_max: 100,
  birthday_bonus_max: 50,
})
const configCheckinTiers = ref<CheckinTier[]>([])

async function openConfigDrawer() {
  configError.value = ''
  configDrawerVisible.value = true
  try {
    const [configRes, tiersRes] = await Promise.all([
      getSalaryConfig(),
      getCheckinTiers(),
    ])
    if (configRes.data) {
      const c = configRes.data
      configForm.salary_pool_total = parseConfigInt(c.salary_pool_total, 2000)
      configForm.formal_member_count = parseConfigInt(c.formal_member_count, 5)
      configForm.base_allocation = parseConfigInt(c.base_allocation, 400)
      configForm.mini_coins_min = parseConfigInt(c.mini_coins_min, 200)
      configForm.mini_coins_max = parseConfigInt(c.mini_coins_max, 400)
      configForm.points_to_coins_ratio = parseConfigInt(c.points_to_coins_ratio, 2)
      configForm.promotion_points_threshold = parseConfigInt(c.promotion_points_threshold, 100)
      configForm.demotion_salary_threshold = parseConfigInt(c.demotion_salary_threshold, 150)
      configForm.demotion_consecutive_months = parseConfigInt(c.demotion_consecutive_months, 2)
      configForm.dismissal_points_threshold = parseConfigInt(c.dismissal_points_threshold, 100)
      configForm.dismissal_consecutive_months = parseConfigInt(c.dismissal_consecutive_months, 2)
      configForm.community_activity_max = parseConfigInt(c.community_activity_max, 100)
      configForm.violation_handling_multiplier = parseConfigInt(c.violation_handling_multiplier, 3)
      configForm.announcement_multiplier = parseConfigInt(c.announcement_multiplier, 5)
      configForm.monthly_excellent_max = parseConfigInt(c.monthly_excellent_max, 30)
      configForm.task_completion_max = parseConfigInt(c.task_completion_max, 100)
      configForm.event_hosting_max = parseConfigInt(c.event_hosting_max, 100)
      configForm.birthday_bonus_max = parseConfigInt(c.birthday_bonus_max, 50)
    }
    if (tiersRes.data && tiersRes.data.length > 0) {
      configCheckinTiers.value = tiersRes.data.map(t => ({ ...t }))
    } else {
      configCheckinTiers.value = checkinTiers.value.map(t => ({ ...t }))
    }
  } catch {
    configCheckinTiers.value = checkinTiers.value.map(t => ({ ...t }))
  }
}

async function handleSaveConfig() {
  configError.value = ''
  configSaving.value = true
  try {
    const configMap: SalaryConfigMap = {
      salary_pool_total: String(configForm.salary_pool_total),
      formal_member_count: String(configForm.formal_member_count),
      base_allocation: String(configForm.base_allocation),
      mini_coins_min: String(configForm.mini_coins_min),
      mini_coins_max: String(configForm.mini_coins_max),
      points_to_coins_ratio: String(configForm.points_to_coins_ratio),
      promotion_points_threshold: String(configForm.promotion_points_threshold),
      demotion_salary_threshold: String(configForm.demotion_salary_threshold),
      demotion_consecutive_months: String(configForm.demotion_consecutive_months),
      dismissal_points_threshold: String(configForm.dismissal_points_threshold),
      dismissal_consecutive_months: String(configForm.dismissal_consecutive_months),
      community_activity_max: String(configForm.community_activity_max),
      violation_handling_multiplier: String(configForm.violation_handling_multiplier),
      announcement_multiplier: String(configForm.announcement_multiplier),
      monthly_excellent_max: String(configForm.monthly_excellent_max),
      task_completion_max: String(configForm.task_completion_max),
      event_hosting_max: String(configForm.event_hosting_max),
      birthday_bonus_max: String(configForm.birthday_bonus_max),
    }
    await updateSalaryConfig(configMap)
    await updateCheckinTiers(configCheckinTiers.value)
    // Refresh local checkin tiers used for calculation
    checkinTiers.value = configCheckinTiers.value.map(t => ({ ...t }))
    ElMessage.success('配置保存成功')
    configDrawerVisible.value = false
    // Refresh table data to reflect new config
    await fetchMembers()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.response?.data || e?.message || '保存失败'
    configError.value = typeof msg === 'string' ? msg : '保存失败'
  } finally {
    configSaving.value = false
  }
}

onMounted(async () => {
  await fetchCheckinTiers()
  // Load salary config for miniCoins clamp calculation
  try {
    const configRes = await getSalaryConfig()
    if (configRes.data) {
      const c = configRes.data
      configForm.salary_pool_total = parseConfigInt(c.salary_pool_total, 2000)
      configForm.formal_member_count = parseConfigInt(c.formal_member_count, 5)
      configForm.base_allocation = parseConfigInt(c.base_allocation, 400)
      configForm.mini_coins_min = parseConfigInt(c.mini_coins_min, 200)
      configForm.mini_coins_max = parseConfigInt(c.mini_coins_max, 400)
      configForm.points_to_coins_ratio = parseConfigInt(c.points_to_coins_ratio, 2)
      configForm.community_activity_max = parseConfigInt(c.community_activity_max, 100)
      configForm.violation_handling_multiplier = parseConfigInt(c.violation_handling_multiplier, 3)
      configForm.announcement_multiplier = parseConfigInt(c.announcement_multiplier, 5)
      configForm.monthly_excellent_max = parseConfigInt(c.monthly_excellent_max, 30)
      configForm.task_completion_max = parseConfigInt(c.task_completion_max, 100)
      configForm.event_hosting_max = parseConfigInt(c.event_hosting_max, 100)
      configForm.birthday_bonus_max = parseConfigInt(c.birthday_bonus_max, 50)
    }
  } catch { /* use defaults */ }
  await fetchPeriods()
  await fetchMembers()
})
</script>

<style scoped>
/* ===== Art Design Pro - Pink Theme ===== */

.salary-page {
  background: #fafafa;
  min-height: 100%;
}

/* --- Stat Cards --- */
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 22px;
  border: 1px solid #f5e6ea;
  box-shadow: 0 2px 12px rgba(233, 30, 99, 0.04);
  transition: box-shadow 0.25s, transform 0.25s;
}
.stat-card:hover {
  box-shadow: 0 6px 20px rgba(233, 30, 99, 0.1);
  transform: translateY(-2px);
}
.stat-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-label {
  font-size: 13px;
  color: #888;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: #333;
}
.stat-value.muted { color: #ccc; }
.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon.pink-bg { background: #fce4ec; color: #e91e63; }
.stat-icon.purple-bg { background: #f3e5f5; color: #9c27b0; }
.stat-icon.orange-bg { background: #fff3e0; color: #ff9800; }
.stat-icon.muted-bg { background: #f5f5f5; color: #bbb; }

/* --- Pool Card --- */
.pool-card {
  padding: 16px 20px !important;
}
.pool-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.pool-total {
  font-size: 16px;
  font-weight: 700;
  color: #e91e63;
}
.pool-progress-bar {
  height: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 10px;
}
.pool-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ec407a, #e91e63);
  border-radius: 4px;
  transition: width 0.4s ease;
}
.pool-progress-fill.pool-over {
  background: linear-gradient(90deg, #ef5350, #c62828);
}
.pool-details {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
}
.pool-detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.pool-detail-item b {
  color: #333;
  font-weight: 600;
}
.pool-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.pool-dot--used { background: #e91e63; }
.pool-dot--remain { background: #e0e0e0; }
.pool-negative { color: #c62828 !important; }

/* --- Toolbar Card --- */
.toolbar-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 20px;
  border: 1px solid #f5e6ea;
  box-shadow: 0 1px 6px rgba(233, 30, 99, 0.03);
}

/* --- Period Selector --- */
.period-select {
  width: 160px;
}

/* --- Buttons --- */
.btn-pink {
  background: linear-gradient(135deg, #ec407a, #e91e63) !important;
  color: #fff !important;
  border: none !important;
  border-radius: 8px !important;
  font-weight: 500 !important;
  padding: 8px 20px !important;
  transition: all 0.2s !important;
}
.btn-pink:hover:not(:disabled) {
  background: linear-gradient(135deg, #f06292, #ec407a) !important;
  box-shadow: 0 4px 14px rgba(233, 30, 99, 0.3) !important;
}
.btn-pink:disabled {
  opacity: 0.5 !important;
}
.btn-outline {
  background: #fff !important;
  color: #e91e63 !important;
  border: 1px solid #f8bbd0 !important;
  border-radius: 8px !important;
  font-weight: 500 !important;
  padding: 8px 16px !important;
  transition: all 0.2s !important;
}
.btn-outline:hover {
  background: #fce4ec !important;
  border-color: #e91e63 !important;
}

/* --- Archived Banner --- */
.archived-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #f5f5f5, #eeeeee);
  border: 2px solid #bdbdbd;
  border-radius: 10px;
  color: #616161;
  font-size: 14px;
  font-weight: 600;
}
.archived-tag {
  font-size: 14px !important;
  padding: 8px 16px !important;
  border-radius: 8px !important;
}

/* --- No Period Banner --- */
.no-period-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #fff8e1, #fff3c4);
  border: 2px solid #ffe082;
  border-radius: 10px;
  color: #f57f17;
  font-size: 14px;
  font-weight: 600;
}

/* --- Error Banner --- */
.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fff0f3;
  border: 1px solid #f8bbd0;
  border-radius: 10px;
  color: #c62828;
  font-size: 13px;
}

/* --- Table Card --- */
.table-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #f5e6ea;
  box-shadow: 0 2px 12px rgba(233, 30, 99, 0.04);
  overflow: hidden;
}
.table-card--archived {
  position: relative;
  border-color: #bdbdbd;
}
.table-card--archived::after {
  content: '已归档';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 28px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.08);
  letter-spacing: 8px;
  pointer-events: none;
  z-index: 10;
}
.table-card--archived :deep(.editable-cell) {
  pointer-events: none;
  cursor: default;
  opacity: 0.7;
}
.table-card--archived :deep(.editable-cell:hover) {
  background: transparent;
}

/* --- Table Overrides --- */
:deep(.salary-table .el-table__body td) {
  padding: 10px 0;
}

/* --- Row Index --- */
.row-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #fce4ec;
  color: #e91e63;
  font-size: 12px;
  font-weight: 600;
}

/* --- Member Cell --- */
.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar-circle {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}
.avatar-leader { background: #fce4ec; color: #e91e63; }
.avatar-vice_leader { background: #f3e5f5; color: #9c27b0; }
.avatar-intern { background: #fff3e0; color: #ff9800; }
.member-name {
  font-weight: 500;
  color: #333;
}

/* --- Role Badge --- */
.role-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}
.role-leader { background: #fce4ec; color: #e91e63; }
.role-vice_leader { background: #f3e5f5; color: #9c27b0; }
.role-intern { background: #fff3e0; color: #e65100; }

/* --- Editable Cell --- */
.editable-cell {
  cursor: pointer;
  min-height: 34px;
  display: flex;
  align-items: center;
  padding: 2px 6px;
  border-radius: 6px;
  transition: background-color 0.15s;
  color: #444;
}
.editable-cell:hover {
  background: #fce4ec;
}

/* --- Data Display --- */
.total-points {
  font-weight: 600;
  color: #e91e63;
}
.computed-points {
  font-weight: 500;
  color: #555;
}
.positive-points {
  font-weight: 500;
  color: #2e7d32;
}
.negative-points {
  font-weight: 500;
  color: #c62828;
}
.remark-text {
  color: #888;
  font-size: 13px;
}
.muted-text {
  color: #ccc;
}

/* --- Validation Error --- */
.validation-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #f44336 !important;
}
.validation-error-text {
  color: #f44336;
  border-bottom: 2px solid #f44336;
  padding-bottom: 1px;
}

/* --- Computed Column Highlight --- */
:deep(.computed-col) {
  background-color: #fdf2f5 !important;
}
:deep(.computed-col--total) {
  background-color: #fce4ec !important;
}
:deep(.el-table__header .computed-col) {
  background-color: #fce4ec !important;
  font-weight: 600;
}
:deep(.el-table__header .computed-col--total) {
  background-color: #f8bbd0 !important;
}

/* --- Violating Row --- */
:deep(.violating-row) {
  background-color: #fff0f0 !important;
}
:deep(.violating-row td) {
  background-color: #fff0f0 !important;
}

/* --- Input overrides for pink theme --- */
:deep(.el-input-number--small) {
  width: 100px;
}
:deep(.el-input__wrapper) {
  border-radius: 6px !important;
}
:deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 1px #e91e63 !important;
}

/* --- Config Drawer --- */
.config-drawer-body {
  padding: 0 8px;
}
.config-actions {
  display: flex;
  justify-content: flex-end;
}
.config-section {
  margin-bottom: 24px;
}
.config-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 2px solid #fce4ec;
}
.config-tier-table {
  width: 100%;
}
.tier-input {
  width: 80px !important;
}
:deep(.config-tier-table .el-input-number--small) {
  width: 80px;
}
</style>
