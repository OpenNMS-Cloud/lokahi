<template>
  <component :is="asLi ? 'li' : 'div'" :title="metric.label" class="container pointer" data-test="metric-chip">
    <label :for="metric.type">{{ metric.type }}</label>
    <FeatherChip :id="metric.type" :class="`bg-status ${itemBgColor}`" data-test="chip">
      {{ itemText }}
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
  asLi: {
    type: Boolean,
    default: false
  }
})

const itemBgColor = props.metric.status?.toLowerCase() || 'unknown'

const itemText = props.metric.type === 'status' ? props.metric.status : getHumanReadableDuration(props.metric?.timestamp as number, props.metric?.timeUnit)
</script>

<style lang="scss" scoped>
@use "@/styles/_statusBackground";

.container {
  text-align: center;
}

label {
  display: block;
  text-transform: capitalize;
}
</style>