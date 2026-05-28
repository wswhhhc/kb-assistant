/**
 * 简易 Markdown 渲染，支持粗体、行内代码、换行和引用标记
 * @param {string} text - 原始文本
 * @param {Array} [citations] - 引用列表
 * @param {number} [msgIdx] - 消息索引（用于生成引用 onclick）
 * @returns {string} 渲染后的 HTML
 */
export function renderMarkdown(text, citations = null, msgIdx = -1) {
  if (!text) return ''

  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code style="background:#f4f4f5;padding:1px 4px;border-radius:3px;font-size:0.9em;">$1</code>')

  if (citations && citations.length > 0 && msgIdx >= 0) {
    return escaped
      .replace(/\n/g, '<br>')
      .replace(/\[(\d+)\]/g, (match, num) => {
        const ci = parseInt(num) - 1
        if (ci >= 0 && ci < citations.length) {
          return `<a class="citation-marker" href="javascript:void(0)" onclick="window.__goToCitation(${msgIdx}, ${ci})" title="${citations[ci].fileName}">[${num}]</a>`
        }
        return match
      })
  }

  return escaped.replace(/\n/g, '<br>')
}

export function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 16).replace('T', ' ')
}

export function buildSessionTitle(text) {
  const normalized = (text || '').replace(/\s+/g, ' ').trim()
  if (!normalized) return '新会话'
  return normalized.length > 20 ? `${normalized.slice(0, 20)}...` : normalized
}
