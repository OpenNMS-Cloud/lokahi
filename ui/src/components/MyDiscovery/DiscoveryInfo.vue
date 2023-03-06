<template>
  <div class="detection-method">
    <div class="header">
      <div class="title">{{ info.label }}</div>
      <div class="bg-status">{{ info.metrics.find((m) => m.type == 'status')?.status }}</div>
      <MetricChip
        :metric="{ status: info.metrics.find((m) => m.type == 'status')?.status }"
        class="bg-status"
      />
    </div>

    <div class="ip-info">
      <FeatherIcon
        :icon="Icons.Location"
        class="icon"
      />
      <div class="text">{{ info.anchor?.managementIpValue }}</div>
    </div>
    <div>{{ info }}</div>
  </div>
</template>

<script lang="ts" setup>
import Location from '@featherds/icon/action/Location'
import { NodeContent } from '@/types/inventory'
import { markRaw } from 'vue'
const Icons = markRaw({
  Location
})

defineProps<{
  info: NodeContent
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/_statusBackground';
.header {
  display: flex;
  border-bottom: 1px solid var(variables.$shade-4);
  margin-bottom: var(variables.$spacing-l);
  padding-bottom: var(variables.$spacing-l);
  justify-content: space-between;
  > .title {
    @include typography.headline4;
  }
}

.ip-info {
  border: 1px solid var(variables.$shade-4);
  padding: var(variables.$spacing-xs) var(variables.$spacing-l);
  width: fit-content;
  min-width: 150px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: var(variables.$spacing-s) 0;
  .icon {
    font-size: 32px;
    margin-bottom: var(variables.$spacing-xs);
    color: var(variables.$shade-2);
  }
  .text {
    @include typography.body-small;
  }
}
</style>
