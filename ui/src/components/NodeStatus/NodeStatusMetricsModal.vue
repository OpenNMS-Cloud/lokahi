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
            :graph="bitsInOut"
            @has-data="displayEmptyMsgIfNoData"
          />
          <LineGraph
            v-if="!isAzure"
            :graph="bandwidthInOut"
            @has-data="displayEmptyMsgIfNoData"
          />
          <LineGraph
            v-if="!isAzure"
            :graph="nodeLatency"
            @has-data="displayEmptyMsgIfNoData"
          />
          <LineGraph
            v-if="!isAzure"
            :graph="errorsInOut"
            @has-data="displayEmptyMsgIfNoData"
          />
        </div>
        <div
          v-if="!hasMetricData"
          class="empty"
        >
          Currently no data available.
        </div>
      </div>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import { TimeRangeUnit } from '@/types/graphql'
import useModal from '@/composables/useModal'
import Close from '@featherds/icon/navigation/Cancel'
import { GraphProps } from '@/types/graphs'
import { useRoute } from 'vue-router'

const route = useRoute()
const { openModal, closeModal, isVisible } = useModal()

const interfaceName = ref()
const instance = ref()
const ifName = ref()
const hasMetricData = ref(false)
const isAzure = ref(false)

const bandwidthInOut = computed<GraphProps>(() => {
  return {
    label: 'Bandwidth Utility Inbound / Outbound (%)',
    metrics: ['bw_util_network_in', 'bw_util_network_out'],
    monitor: 'SNMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute,
    ifName: ifName.value
  }
})

const bitsInOut = computed<GraphProps>(() => {
  return {
    label: 'Bits Inbound / Outbound',
    metrics: ['network_in_bits', 'network_out_bits'],
    monitor: 'SNMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute,
    ifName: ifName.value
  }
})

const nodeLatency = computed<GraphProps>(() => {
  return {
    label: 'ICMP Response Time',
    metrics: ['response_time_msec'],
    monitor: 'ICMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute,
    ifName: ifName.value
  }
})

const errorsInOut = computed<GraphProps>(() => {
  return {
    label: 'Errors Inbound / Outbound',
    metrics: ['network_errors_in', 'network_errors_out'],
    monitor: 'SNMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute,
    ifName: ifName.value
  }
})

const openAzureMetrics = (inst: string) => {
  isAzure.value = true // azure nodes can only display bytes in/out
  interfaceName.value = inst
  instance.value = inst
  openModal()
}

const setIfNameAndOpenModal = (ifNameStr: string) => {
  isAzure.value = false
  interfaceName.value = ifNameStr
  ifName.value = ifNameStr
  openModal()
}

const displayEmptyMsgIfNoData = (hasData: boolean) => {
  if (hasMetricData.value) return
  hasMetricData.value = hasData
}

defineExpose({ openAzureMetrics, setIfNameAndOpenModal })
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins.scss';

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
  flex-direction: column;
  flex-wrap: wrap;
  width: 100%;
  @include mediaQueriesMixins.screen-md {
    flex-direction: row;
  }
  > div {
    margin-top: var(variables.$spacing-l);
    margin-right: 0;
    display: flex;
    @include mediaQueriesMixins.screen-md {
      margin-right: var(variables.$spacing-l);
    }
  }
  > div:nth-child(2n) {
    margin-right: 0;
  }
}
.empty {
  margin-top: var(variables.$spacing-xl);
}
</style>
