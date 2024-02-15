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
  <div :class="['empty-list', bg ? 'bg' : '']">
    <div data-test="msg">{{ msg }}</div>
    <FeatherButton v-if="content.btn" secondary @click="content.btn?.action" data-test="btn">{{ content.btn?.label }}
    </FeatherButton>
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

const props = defineProps<{
  content: Content,
  bg?: boolean
}>()

const msg = computed(() => props.content.msg || '')
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

  >button {
    margin-top: var(variables.$spacing-l);
  }
}

.bg {
  background-color: var(variables.$surface);
  border-radius: vars.$border-radius-surface;
  border: 1px solid var(variables.$border-on-surface);
}
</style>
