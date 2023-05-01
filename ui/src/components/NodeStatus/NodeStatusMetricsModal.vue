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
            :graph="bytesInOut"
            @has-data="(hasData) => (hasMetricInfo = hasData)"
          />
        </div>
        <div
          v-if="!hasMetricInfo"
          class="empty"
        >
          Currently no data available.
        </div>
      </div>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import { TimeRangeUnit, IpInterface } from '@/types/graphql'
import useModal from '@/composables/useModal'
import Close from '@featherds/icon/navigation/Cancel'
import { GraphProps } from '@/types/graphs'
import { useRoute } from 'vue-router'

const route = useRoute()
const { openModal, closeModal, isVisible } = useModal()

const interfaceName = ref()
const instance = ref()
const hasMetricInfo = ref(false)

const bytesInOut = computed<GraphProps>(() => {
  return {
    label: 'Bits Inbound / Outbound',
    metrics: ['network_in_bits', 'network_out_bits'],
    monitor: 'SNMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute
  }
})

const openMetricsModal = (interfaceInfo: IpInterface) => {
  interfaceName.value = interfaceInfo.ipAddress
  instance.value = interfaceInfo.ipAddress
  openModal()
}

defineExpose({ openMetricsModal })
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
