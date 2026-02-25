import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import InterviewChat from '@/components/interview/InterviewChat.vue'

vi.mock('@/api/interview', () => ({
  sendMessage: vi.fn(),
  endInterview: vi.fn(),
  getMessages: vi.fn(),
  getInterview: vi.fn(),
  startInterview: vi.fn(),
  getReport: vi.fn(),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  }
})

const mockMessages = [
  {
    id: 1,
    interviewId: 10,
    role: 'AI',
    content: '你好，请描述一下如何处理群内冲突。',
    timestamp: '2024-01-15T10:00:00',
    timeLimitSeconds: 60,
  },
]

function createWrapper(interviewId = 10) {
  return mount(InterviewChat, {
    props: { interviewId },
    global: {
      plugins: [createPinia(), ElementPlus],
    },
  })
}

describe('InterviewChat', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should load messages on mount', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })

    const wrapper = createWrapper()
    await flushPromises()

    expect(api.getMessages).toHaveBeenCalledWith(10)
    expect(wrapper.text()).toContain('你好，请描述一下如何处理群内冲突。')
  })

  it('should display AI messages on the left and user messages on the right', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: [
        ...mockMessages,
        {
          id: 2,
          interviewId: 10,
          role: 'USER',
          content: '我会先了解情况。',
          timestamp: '2024-01-15T10:01:00',
          timeLimitSeconds: 60,
        },
      ],
    })

    const wrapper = createWrapper()
    await flushPromises()

    const bubbles = wrapper.findAll('.message-bubble')
    expect(bubbles.length).toBe(2)
    // AI message should have justify-start
    expect(bubbles[0].classes()).toContain('justify-start')
    // User message should have justify-end
    expect(bubbles[1].classes()).toContain('justify-end')
  })

  it('should send message when clicking send button', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })
    ;(api.sendMessage as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: {
        id: 3,
        interviewId: 10,
        role: 'AI',
        content: '很好，请继续。',
        timestamp: '2024-01-15T10:02:00',
        timeLimitSeconds: 60,
      },
    })

    const wrapper = createWrapper()
    await flushPromises()

    const input = wrapper.find('.el-input input')
    await input.setValue('我会先了解双方的诉求。')
    await wrapper.find('.el-button--primary').trigger('click')
    await flushPromises()

    expect(api.sendMessage).toHaveBeenCalledWith(10, '我会先了解双方的诉求。')
    expect(wrapper.text()).toContain('我会先了解双方的诉求。')
    expect(wrapper.text()).toContain('很好，请继续。')
  })

  it('should start 60s countdown after receiving AI message', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })

    const wrapper = createWrapper()
    await flushPromises()

    // Countdown should start at 60
    expect(wrapper.text()).toContain('60s')

    // Advance 10 seconds
    vi.advanceTimersByTime(10000)
    await flushPromises()
    expect(wrapper.text()).toContain('50s')
  })

  it('should show red countdown when <= 10 seconds', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })

    const wrapper = createWrapper()
    await flushPromises()

    // Advance to 51 seconds (9 remaining)
    vi.advanceTimersByTime(51000)
    await flushPromises()

    const timerEl = wrapper.find('.countdown-timer')
    expect(timerEl.classes()).toContain('bg-red-100')
  })

  it('should call endInterview when clicking end button', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })
    ;(api.endInterview as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { id: 1, interviewId: 10, totalScore: 8 },
    })

    const wrapper = createWrapper()
    await flushPromises()

    const endBtn = wrapper.findAll('.el-button').find((b) => b.text().includes('结束面试'))
    expect(endBtn).toBeDefined()
    await endBtn!.trigger('click')
    await flushPromises()

    expect(api.endInterview).toHaveBeenCalledWith(10)
  })

  it('should disable input after interview ends', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })
    ;(api.endInterview as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { id: 1, interviewId: 10, totalScore: 8 },
    })

    const wrapper = createWrapper()
    await flushPromises()

    // End the interview
    const endBtn = wrapper.findAll('.el-button').find((b) => b.text().includes('结束面试'))
    await endBtn!.trigger('click')
    await flushPromises()

    const input = wrapper.find('.el-input input')
    expect((input.element as HTMLInputElement).disabled).toBe(true)
  })

  it('should not send empty messages', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })

    const wrapper = createWrapper()
    await flushPromises()

    // Try to send with empty input
    const sendBtn = wrapper.find('.el-button--primary')
    expect(sendBtn.attributes('disabled')).toBeDefined()
  })

  it('should show loading indicator while sending', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })

    let resolveMsg: (v: any) => void
    ;(api.sendMessage as ReturnType<typeof vi.fn>).mockReturnValueOnce(
      new Promise((r) => { resolveMsg = r })
    )

    const wrapper = createWrapper()
    await flushPromises()

    const input = wrapper.find('.el-input input')
    await input.setValue('测试消息')
    await wrapper.find('.el-button--primary').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('AI 正在思考...')

    resolveMsg!({
      data: {
        id: 4,
        interviewId: 10,
        role: 'AI',
        content: '收到',
        timestamp: '2024-01-15T10:03:00',
        timeLimitSeconds: 60,
      },
    })
    await flushPromises()

    expect(wrapper.text()).not.toContain('AI 正在思考...')
  })

  it('should emit ended event when interview ends', async () => {
    const api = await import('@/api/interview')
    ;(api.getMessages as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: mockMessages,
    })
    ;(api.endInterview as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      data: { id: 1, interviewId: 10, totalScore: 8 },
    })

    const wrapper = createWrapper()
    await flushPromises()

    const endBtn = wrapper.findAll('.el-button').find((b) => b.text().includes('结束面试'))
    await endBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('ended')).toBeTruthy()
  })
})
