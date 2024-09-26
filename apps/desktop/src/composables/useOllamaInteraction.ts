import {onMounted, onUnmounted} from 'vue'
import {invoke} from '@tauri-apps/api/core'
import {listen, type UnlistenFn} from '@tauri-apps/api/event'
import {info, error} from '@tauri-apps/plugin-log'
import {useChatStore} from "~/stores"

export function useOllamaInteraction() {
  const chatStore = useChatStore()
  let unlisten: UnlistenFn | null = null

  onMounted(async () => {
    unlisten = await listen<string>('ollama-response', (event) => {
      chatStore.updateStreamingMessage((prev) => prev + event.payload)
      chatStore.setIsStreaming(true)
    })

    await listen('ollama-response-end', () => {
      chatStore.setIsStreaming(false)
      chatStore.addMessage({
        sender: 'ai',
        content: chatStore.currentStreamingMessage
      })
      chatStore.clearStreamingMessage()
    })
  })

  onUnmounted(() => {
    if (unlisten) unlisten()
  })

  async function sendPrompt(message: string, userId: string, editorContent?: string, language?: string) {
    if (!message.trim()) return
    chatStore.setIsSending(true)
    chatStore.setIsStreaming(true)
    chatStore.setPromptError(null)
    chatStore.clearStreamingMessage()
    try {
      let fullPrompt: string
      if (editorContent && editorContent.trim()) {
        fullPrompt = `User's code (${language || 'unspecified language'}):\n\`\`\`${language || ''}\n${editorContent}\n\`\`\`\n\nUser's question: ${message}`
      } else {
        fullPrompt = message
      }

      await invoke('send_prompt_to_ollama', {
        model: "llama3:8b",
        prompt: fullPrompt,
        userId: userId
      })
      await info('Prompt sent successfully')
    } catch (err) {
      if (err instanceof Error) {
        await error(`Failed to send prompt to Ollama: ${err.message}`)
        chatStore.setPromptError(err.message)
      } else {
        await error('An unknown error occurred while sending prompt to Ollama')
        chatStore.setPromptError('An unknown error occurred')
      }
    } finally {
      chatStore.setIsSending(false)
      // We do not set isStreaming to false here, as the response may still come in.
    }
  }

  return {
    sendPrompt
  }
}