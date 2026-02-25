import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import InterviewReport from '@/components/interview/InterviewReport.vue'
import type { InterviewReportDTO } from '@/api/interview'

function makeReport(overrides: Partial<InterviewReportDTO> = {}): InterviewReportDTO {
  return {
    id: 1,
    interviewId: 10,
    ruleFamiliarity: 7,
    communicationScore: 8,
    pressureScore: 6,
    totalScore: 7,
    aiComment: '候选人表现良好',
    reviewerComment: null,
    reviewResult: null,
    suggestedMentor: null,
    recommendationLabel: null,
    manualApproved: null,
    reviewedAt: null,
    createdAt: '2024-01-15T10:00:00',
    ...overrides,
  }
}

function createWrapper(report: InterviewReportDTO | null = null) {
  return mount(InterviewReport, {
    props: { report },
    global: { plugins: [createPinia(), ElementPlus] },
  })
}

describe('InterviewReport', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should show empty state when report is null', () => {
    const wrapper = createWrapper(null)
    expect(wrapper.text()).toContain('暂无评估报告')
  })

  it('should display all four score dimensions', () => {
    const wrapper = createWrapper(makeReport())
    expect(wrapper.text()).toContain('规则熟悉度')
    expect(wrapper.text()).toContain('7/10')
    expect(wrapper.text()).toContain('沟通能力')
    expect(wrapper.text()).toContain('8/10')
    expect(wrapper.text()).toContain('抗压能力')
    expect(wrapper.text()).toContain('6/10')
    expect(wrapper.text()).toContain('总分')
  })

  it('should display AI comment', () => {
    const wrapper = createWrapper(makeReport({ aiComment: '表现优秀' }))
    expect(wrapper.text()).toContain('表现优秀')
    expect(wrapper.text()).toContain('AI 评语')
  })

  it('should show "建议通过" tag when totalScore >= 8', () => {
    const wrapper = createWrapper(makeReport({ totalScore: 9 }))
    expect(wrapper.find('.recommendation-tag').text()).toBe('建议通过')
  })

  it('should show "重点审查对话内容" tag when totalScore is 6-7', () => {
    const wrapper = createWrapper(makeReport({ totalScore: 6 }))
    expect(wrapper.find('.recommendation-tag').text()).toBe('重点审查对话内容')
  })

  it('should show "建议拒绝" tag when totalScore <= 5', () => {
    const wrapper = createWrapper(makeReport({ totalScore: 4 }))
    expect(wrapper.find('.recommendation-tag').text()).toBe('建议拒绝')
  })

  it('should show green/success tag for score >= 8', () => {
    const wrapper = createWrapper(makeReport({ totalScore: 8 }))
    const tag = wrapper.find('.recommendation-tag')
    expect(tag.html()).toContain('success')
  })

  it('should show orange/warning tag for score 6-7', () => {
    const wrapper = createWrapper(makeReport({ totalScore: 7 }))
    const tag = wrapper.find('.recommendation-tag')
    expect(tag.html()).toContain('warning')
  })

  it('should show red/danger tag for score <= 5', () => {
    const wrapper = createWrapper(makeReport({ totalScore: 3 }))
    const tag = wrapper.find('.recommendation-tag')
    expect(tag.html()).toContain('danger')
  })
})
