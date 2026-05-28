import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'

export function useCitationNavigation() {
  const router = useRouter()
  const citationDialogVisible = ref(false)
  const currentCitation = ref(null)

  function goToCitation(msgIdx, ci) {
    // 通过事件触发消息中的引用定位 — 由 ChatMessageList 监听
    window.dispatchEvent(new CustomEvent('citation-navigate', {
      detail: { msgIdx, ci }
    }))
  }

  function openCitation(c) {
    currentCitation.value = c
    citationDialogVisible.value = true
  }

  function showCitationContent(c) {
    openCitation(c)
  }

  function goToDocumentSource(citation) {
    citationDialogVisible.value = false
    if (!citation?.documentId) return

    // 从 URL 参数中获取 currentKbId
    const route = router.currentRoute.value
    const kbId = route.query.kbId
    router.push({
      name: 'DocumentDetail',
      params: { kbId, documentId: citation.documentId },
      query: { chunkIndex: citation.chunkIndex }
    })
  }

  // 挂载全局函数供 v-html 内的 onclick 使用
  onMounted(() => {
    window.__goToCitation = goToCitation
  })

  onUnmounted(() => {
    delete window.__goToCitation
  })

  return {
    citationDialogVisible,
    currentCitation,
    openCitation,
    showCitationContent,
    goToCitation,
    goToDocumentSource,
  }
}
