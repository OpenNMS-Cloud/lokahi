<template>
  <component :is="tag" :title="propsData.label" class="container" data-test="propsData-chip">
    <label v-if="propsData.label" :for="propsData.label">{{ propsData.label }}</label>
    <FeatherChip :id="propsData.label" :class="`bg-status ${propsData.bgColor}`" data-test="chip">
      {{ propsData.value }}
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
    value: props.metric.status || '--',
    bgColor: props.metric.status || 'unknown'
  }

  if ('value' in props.metric) {
    let bgColor = 'unknown'

    if (props.metric.value !== undefined) {
      bgColor = props.metric.value > 0 ? 'up' : 'down'
    }

    chip = {
      ...chip,
      value: getHumanReadableDuration(props.metric.value as number),
      bgColor
    }
  }

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
