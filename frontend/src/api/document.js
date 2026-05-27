import request from '@/utils/request'

export function listDocuments(knowledgeBaseId, params) {
  return request.get(`/knowledge-bases/${knowledgeBaseId}/documents`, { params })
}

export function uploadDocument(knowledgeBaseId, formData) {
  return request.post(`/knowledge-bases/${knowledgeBaseId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteDocument(id) {
  return request.delete(`/documents/${id}`)
}
