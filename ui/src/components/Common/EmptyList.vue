<!-- 
  To display a message if the list is empty, and an action (optional)

  Component props structure:
    {
      msg: 'This is an empty list.',
      btn: { // optional
        label
        action
    }
}
 -->
<template>
  <div class="empty-list">
    <div data-test="msg">{{ content.msg }}</div>
    <FeatherButton
      v-if="content.btn"
      secondary
      @click="content.btn?.action"
      data-test="btn"
      >{{ content.btn?.label }}</FeatherButton
    >
  </div>
</template>

<script setup lang="ts">
import { fncVoid } from '@/types'

type Content = {
  msg: string
  btn?: {
    label: string
    action: fncVoid
  }
}

defineProps<{
  content: Content
}>()
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';

.empty-list {
  display: flex;
  flex-direction: column;
  width: 100%;
  justify-content: center;
  align-items: center;
  height: 200px;
  > button {
    margin-top: var(variables.$spacing-l);
  }
}
</style>
