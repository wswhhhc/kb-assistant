import { ElMessage } from 'element-plus'

/**
 * 流式请求封装（SSE），自动处理 token 注入和 401 重定向
 * @param {string} url - 请求地址（完整路径或 /api/xxx）
 * @param {object} data - POST body
 * @param {object} handlers - 事件处理器
 * @param {function} handlers.onAnswer - 收到 answer 事件回调 (text)
 * @param {function} handlers.onCitations - 收到 citations 事件回调 (citations)
 * @param {function} handlers.onDone - 完成事件回调 (payload)
 * @param {function} [handlers.onError] - 错误回调 (error)，默认弹出错误提示
 */
export async function streamRequest(url, data, handlers) {
  const token = localStorage.getItem('token') || ''

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify(data)
  })

  if (response.status === 401) {
    localStorage.removeItem('token')
    window.location.href = '/login'
    return
  }

  if (!response.ok) {
    const err = new Error(`请求失败: ${response.status}`)
    if (handlers.onError) {
      handlers.onError(err)
    } else {
      ElMessage.error(err.message)
    }
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let currentEvent = ''
  let dataBuilder = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')

      for (const line of lines) {
        if (line === '') {
          // 事件结束，处理累积的数据
          if (currentEvent && dataBuilder) {
            processEvent(currentEvent, dataBuilder, handlers)
          }
          currentEvent = ''
          dataBuilder = ''
          continue
        }

        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
          continue
        }

        if (line.startsWith('data:')) {
          if (dataBuilder) dataBuilder += '\n'
          dataBuilder += line.slice(5).trim()
        }
      }
    }
  } catch (err) {
    console.warn('流式读取中断:', err)
  }
}

function processEvent(eventName, data, handlers) {
  try {
    if (eventName === 'citations') {
      handlers.onCitations?.(JSON.parse(data))
      return
    }

    const payload = JSON.parse(data)

    if (eventName === 'answer') {
      handlers.onAnswer?.(payload.text || '')
      return
    }

    if (eventName === 'done') {
      handlers.onDone?.(payload)
    }
  } catch (err) {
    console.warn('SSE 事件解析失败:', eventName, err)
  }
}
