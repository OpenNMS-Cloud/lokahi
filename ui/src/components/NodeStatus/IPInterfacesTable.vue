<template>
  <TableCard>
    <div class="header">
      <div class="title-container">
        <span class="title">
          {{ nodeStatusStore.isAzure ? 'Network Interfaces' : 'IP Interfaces' }}
        </span>
      </div>
    </div>
    <div class="container">
      <table
        class="data-table"
        aria-label="IP Interfaces Table"
      >
        <thead>
          <tr>
            <th scope="col">IP Address</th>
            <th scope="col">Graphs</th>
            <th scope="col">IP Hostname</th>
            <th scope="col">Netmask</th>
            <th scope="col">Primary</th>
          </tr>
        </thead>
        <TransitionGroup
          name="data-table"
          tag="tbody"
        >
          <tr
            v-for="ipInterface in nodeData.node.ipInterfaces"
            :key="ipInterface.id"
          >
            <td>{{ ipInterface.ipAddress }}</td>
            <td>
              <FeatherIcon
                :icon="MultilineChart"
                class="icon-metrics"
                @click="metricsModal.openMetricsModal(ipInterface)"
              />
            </td>
            <td>{{ ipInterface.hostname }}</td>
            <td>{{ ipInterface.netmask }}</td>
            <td>{{ ipInterface.snmpPrimary }}</td>
          </tr>
        </TransitionGroup>
      </table>
    </div>
  </TableCard>
  <NodeStatusMetricsModal ref="metricsModal" />
</template>

<script lang="ts" setup>
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import MultilineChart from '@material-design-icons/svg/outlined/multiline_chart.svg'
const metricsModal = ref()

const nodeStatusStore = useNodeStatusStore()

const nodeData = computed(() => {
  return {
    node: nodeStatusStore.fetchedData?.node
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/table/scss/table';
@use '@/styles/_transitionDataTable';

.header {
  display: flex;
  justify-content: space-between;
  .title-container {
    display: flex;
    .title {
      @include typography.headline3;
      margin-left: 15px;
      margin-top: 2px;
    }
  }
}

.container {
  display: block;
  overflow-x: auto;
  table {
    width: 100%;
    @include table.table;
    thead {
      background: var(variables.$background);
      text-transform: uppercase;
    }
    td {
      white-space: nowrap;
      div {
        border-radius: 5px;
        padding: 0px 5px 0px 5px;
      }
    }
    .icon-metrics {
      cursor: pointer;
      color: var(variables.$primary);
    }
  }
}
</style>
