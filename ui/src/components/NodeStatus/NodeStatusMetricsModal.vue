<template>
  <PrimaryModal
    :visible="isVisible"
    hideTitle
    :minWidth="385"
  >
    <template #content
      ><div class="modal-content">
        <div class="header">
          <div class="title">{{ interfaceName }}</div>
          <FeatherIcon
            :icon="Close"
            class="pointer"
            @click="closeModal"
          />
        </div>
        <div class="metrics">
          <LineGraph
            :graph="nodeLatency"
            @has-data="(hasData) => (hasMetricInfo = hasData)"
          />
          <LineGraph
            :graph="bytesInOut"
            @has-data="(hasData) => (hasMetricInfo = hasData)"
          />
        </div>
        <div
          v-if="!hasMetricInfo"
          class="empty"
        >
          No metric data at the moment for this interface.
        </div>
      </div>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import { TimeRangeUnit } from '@/types/graphql'
import useModal from '@/composables/useModal'
import Close from '@featherds/icon/navigation/Cancel'
import { useGraphsQueries } from '@/store/Queries/graphsQueries'
import { GraphProps } from '@/types/graphs'
import { useRoute } from 'vue-router'
//v-if="hasMetricInfo"
const route = useRoute()

const store = useGraphsQueries()

const { openModal, closeModal, isVisible } = useModal()

const interfaceName = ref()
const instance = ref()
const hasMetricInfo = ref(false)
const nodeLatency = computed<GraphProps>(() => {
  return {
    label: 'ICMP Response Time',
    metrics: ['response_time_msec'],
    monitor: 'ICMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute
  }
})

const bytesInOut = computed<GraphProps>(() => {
  return {
    label: 'Bytes Inbound / Outbound',
    metrics: ['network_in_total_bytes', 'network_out_total_bytes'],
    monitor: 'SNMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute
  }
})

onMounted(async () => {
  await store.fetchNode()
})

const openMetricsModal = (interfaceInfo: any) => {
  interfaceName.value = interfaceInfo.ipAddress
  instance.value = interfaceInfo.ipAddress
  openModal()
}

defineExpose({ openMetricsModal })
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
.modal-content {
  max-width: 785px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  @include typography.headline3;
}
.metrics {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  width: 100%;
  > div {
    margin-right: var(variables.$spacing-l);
    margin-top: var(variables.$spacing-l);
    display: flex;
  }
  > div:nth-child(2n) {
    margin-right: 0;
  }
}
.empty {
  margin-top: var(variables.$spacing-xl);
}
</style>
