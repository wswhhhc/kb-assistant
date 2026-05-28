<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)" title="反馈原因" width="420px">
    <el-form label-width="0">
      <el-form-item>
        <el-radio-group :model-value="reasonType" @update:model-value="$emit('update:reasonType', $event)">
          <el-radio-button label="LOW_QUALITY">答非所问</el-radio-button>
          <el-radio-button label="NO_HIT">没检索到重点</el-radio-button>
          <el-radio-button label="INSUFFICIENT_CITATION">引用不充分</el-radio-button>
          <el-radio-button label="MODEL_ERROR">内容有误</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-input
          :model-value="reason" @update:model-value="$emit('update:reason', $event)"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入这条回答哪里需要改进（选填）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="danger" :loading="submitting" @click="$emit('confirm')">提交反馈</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  reasonType: { type: String, default: 'LOW_QUALITY' },
  reason: { type: String, default: '' },
  submitting: { type: Boolean, default: false },
})
defineEmits(['update:visible', 'update:reasonType', 'update:reason', 'confirm'])
</script>
