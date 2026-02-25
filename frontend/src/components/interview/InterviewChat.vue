<template>
  <div class="interview-chat flex flex-col h-full">
    <!-- Header with timer -->
    <div class="chat-header flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-white/80">
      <span class="font-semibold text-gray-700">AI 面试对话</span>
      <div class="flex items-center gap-4">
        <span
          v-if="countdown > 0"
          class="countdown-timer text-sm font-mono px-3 py-1 rounded-full"
          :class="countdown <= 10 ? 'bg-red-100 text-red-600' : 'bg-emerald-100 text-emerald-700'"
        >
          ⏱ {{ countdown }}s
        </span>
        <el-button type="danger" size="small" @click="handleEnd" :loading="ending">
          结束面试
        </el-button>
      </div>
    </div>

    <!-- Messages area -->
    <div ref="messagesRef" class="messages-area flex-1 overflow-y-auto p-4 space-y-3">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-bubble flex"
        :class="msg.role === 'USER' ? 'justify-end' : 'justify-start'"
      >
        <div
          class="max-w-[75%] px-4 py-2 rounded-xl text-sm leading-relaxed"
          :class="msg.role === 'USER'
            ? 'bg-emerald-500 text-white rounded-br-sm'
            : 'bg-gray-100 text-gray-800 rounded-bl-sm'"
        >
          {{ msg.content }}
        </div>
      </div>
      <div v-if="sending" class="flex justify-start">
        <div class="bg-gray-100 text-gray-500 px-4 py-2 rounded-xl rounded-bl-sm text-sm">
          AI 正在思考...
        </div>
      </div>
    </div>

    <!-- Input area -->
    <div class="chat-input flex items-center gap-2 px-4 py-3 border-t border-gray-200 bg-white/80">
      <el-input
        v-model="inputText"
        placeholder="输入你的回答..."
        :disabled="sending || ended"
        @keyup.enter="handleSend"
        class="flex-1"
      />
      <el-button
        type="primary"
        :disabled="!inputText.trim() || sending || ended"
        :loading="sending"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { InterviewMessageDTO } from '@/api/interview'
import { sendMessage, endInterview, getMessages } from '@/api/interview'

const props = defineProps<{ interviewId: number }>()
const emit = defineEmits<{ (e: 'ended'): void }>()

const messages = ref<InterviewMessageDTO[]>([])
const inputText = ref('')
const sending = ref(false)
const ending = ref(false)
const ended = ref(false)
const countdown = ref(0)
const messagesRef = ref<HTMLElement | null>(null)

let timer: ReturnType<typeof setInterval> | null = null

function startCountdown() {
  stopCountdown()
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}

function stopCountdown() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  countdown.value = 0
}

async function scrollToBottom() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

async function loadMessages() {
  try {
    const res = await getMessages(props.interviewId)
    messages.value = res.data
    await scrollToBottom()
    // Start countdown if last message is from AI
    if (messages.value.length > 0 && messages.value[messages.value.length - 1].role !== 'USER') {
      startCountdown()
    }
  } catch {
    // silently fail on initial load
  }
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value || ended.value) return

  stopCountdown()
  inputText.value = ''
  sending.value = true

  try {
    const res = await sendMessage(props.interviewId, text)
    // Add user message locally
    messages.value.push({
      id: Date.now(),
      interviewId: props.interviewId,
      role: 'USER',
      content: text,
      timestamp: new Date().toISOString(),
      timeLimitSeconds: 60,
    })
    // Add AI response
    messages.value.push(res.data)
    await scrollToBottom()
    startCountdown()
  } catch {
    ElMessage.error('发送失败，请重试')
  } finally {
    sending.value = false
  }
}

async function handleEnd() {
  ending.value = true
  stopCountdown()
  try {
    await endInterview(props.interviewId)
    ended.value = true
    ElMessage.success('面试已结束')
    emit('ended')
  } catch {
    ElMessage.error('结束面试失败')
  } finally {
    ending.value = false
  }
}

watch(() => props.interviewId, () => {
  ended.value = false
  messages.value = []
  loadMessages()
}, { immediate: true })

onBeforeUnmount(() => {
  stopCountdown()
})
</script>
