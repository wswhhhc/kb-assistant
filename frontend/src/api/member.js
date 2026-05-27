import request from '@/utils/request'

export function listMembers(knowledgeBaseId) {
  return request.get(`/knowledge-bases/${knowledgeBaseId}/members`)
}

export function addMember(knowledgeBaseId, data) {
  return request.post(`/knowledge-bases/${knowledgeBaseId}/members`, data)
}

export function removeMember(knowledgeBaseId, userId) {
  return request.delete(`/knowledge-bases/${knowledgeBaseId}/members/${userId}`)
}
