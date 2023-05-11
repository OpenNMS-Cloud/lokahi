<!-- 
  Component props structure:
    item: {
      style: 'CRITICAL',
      label: '99%' // optional: to display 99% as label inside the pill if present, instead of `CRITICAL`
    }

  The class names are based on the `severity` types of the BE graphql schema.
    enum Severity {
      CLEARED
      CRITICAL
      INDETERMINATE
      MAJOR
      MINOR
      NORMAL
      SEVERITY_UNDEFINED
      UNRECOGNIZED
      WARNING
    }
 -->
<template>
  <div class="pill-color-wrapper">
    <span
      :class="['pill-style', `${item.style?.toLowerCase()}-color`]"
      data-test="pill-style"
    >
      {{ item.label || item.style }}
    </span>
  </div>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'

type Pill = {
  style: string
  label?: string
}

defineProps({
  item: {
    type: Object as PropType<Pill>,
    required: true
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars.scss';
@use '@/styles/pillColor.scss';

.pill-style {
  @include typography.title();
  display: inline-block;
  font-size: 0.8rem;
  line-height: normal;
  padding: 2px 8px;
  border-radius: vars.$border-radius-sm;
}
</style>
