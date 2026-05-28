<template>
  <div class="chat-sidebar">
    <div class="sidebar-header">
      <el-button type="primary" class="new-session-btn" @click="$emit('create')">新建会话</el-button>
    </div>
    <div class="sidebar-kb-select">
      <el-select :model-value="currentKbId" @update:model-value="$emit('update:currentKbId', $event); $emit('kb-change')" placeholder="选择知识库">
        <el-option v-for="kb in kbList" :key="kb.id" :label="kb.name" :value="kb.id" />
      </el-select>
    </div>
    <el-scrollbar class="sidebar-scroll">
      <div
        v-for="s in sessions"
        :key="s.id"
        class="session-item"
        :class="{ active: s.id === currentSessionId }"
        @click="$emit('switch-session', s.id)"
      >
        <div class="session-title">{{ s.title }}</div>
        <div class="session-time">{{ formatTime(s.updatedAt) }}</div>
        <el-button text size="small" type="danger" class="session-del"
          @click.stop="$emit('delete-session', s.id)">×</el-button>
      </div>
      <div v-if="sessions.length === 0" class="sidebar-empty">
        暂无会话
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { formatTime } from '@/utils/markdown'

defineProps({
  kbList: { type: Array, default: () => [] },
  sessions: { type: Array, default: () => [] },
  currentKbId: { type: Number, default: null },
  currentSessionId: { type: Number, default: null },
})

defineEmits(['create', 'kb-change', 'switch-session', 'delete-session'])
</script>

<style scoped>
.chat-sidebar {
  width: 260px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.sidebar-header {
  padding: 12px;
}
.new-session-btn {
  width: 100%;
}
.sidebar-kb-select {
  padding: 0 12px 12px;
}
.sidebar-scroll {
  flex: 1;
}
.session-item {
  position: relative;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; }
.session-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 20px;
}
.session-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
}
.session-del {
  position: absolute;
  right: 4px;
  top: 4px;
  display: none;
}
.session-item:hover .session-del { display: block; }
.sidebar-empty {
  text-align: center;
  color: #909399;
  padding: 20px;
  font-size: 13px;
}
</style>
