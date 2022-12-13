<template>
  <component :is="tag" :title="propsData.label" class="container" data-test="propsData-chip">
    <label v-if="propsData.label" :for="propsData.label">{{ propsData.label }}</label>
    <FeatherChip :id="propsData.label" :class="`bg-status ${propsData.status}`" data-test="chip">
      {{ propsData.text }}
    </FeatherChip>
  </component>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { FeatherChip } from '@featherds/chips'
import { Chip } from '@/types/metric'
import { getHumanReadableDuration } from '@/components/utils'

const props = defineProps({
  metric: {
    type: Object as PropType<Chip>,
    required: true
  },
  tag: {
    type: String,
    default: 'div'
  }
})

const propsData = computed(() => {
  let chip = {
    label: props.metric.label,
    text: props.metric.status || '--',
    status: props.metric.status?.toLocaleLowerCase() || 'unknown'
  }
  
  if('timestamp' in props.metric) {
    chip = {
      ...chip,
      text: getHumanReadableDuration(props.metric?.timestamp as number)
    }
  }
  // console.log('chip',chip)
  return chip
})
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";
@use "@/styles/_statusBackground";

.container {
  text-align: center !important;
  box-shadow: none !important;
}

label {
  display: block;
  text-transform: capitalize;
  color: var(variables.$primary-text-on-surface);
}

.chip {
  margin: 0;
}
</style>