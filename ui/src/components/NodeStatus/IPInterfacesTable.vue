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
        class="data-table tc2"
        aria-label="IP Interfaces Table"
      >
        <thead>
          <tr>
            <th scope="col">IP Address</th>
            <th scope="col" v-if="nodeStatusStore.isAzure">Graphs</th>
            <th scope="col" v-if="!nodeStatusStore.isAzure">IP Hostname</th>
            <th scope="col" v-if="!nodeStatusStore.isAzure">Netmask</th>
            <th scope="col" v-if="!nodeStatusStore.isAzure">Primary</th>
          </tr>
        </thead>
        <TransitionGroup
          name="data-table"
          tag="tbody"
        >
          <tr
            v-for="ipInterface in nodeStatusStore.node.ipInterfaces"
            :key="ipInterface.id"
          >
            <td>{{ ipInterface.ipAddress }}</td>
            <td v-if="nodeStatusStore.isAzure">
              <FeatherButton
                text
                @click="metricsModal.openAzureMetrics(ipInterface.ipAddress)"
                >Traffic
              </FeatherButton>
            </td>
            <td v-if="!nodeStatusStore.isAzure">{{ ipInterface.hostname }}</td>
            <td v-if="!nodeStatusStore.isAzure">{{ ipInterface.netmask }}</td>
            <td v-if="!nodeStatusStore.isAzure">{{ ipInterface.snmpPrimary }}</td>
          </tr>
        </TransitionGroup>
      </table>
    </div>
  </TableCard>
  <NodeStatusMetricsModal ref="metricsModal" />
</template>

<script lang="ts" setup>
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
const nodeStatusStore = useNodeStatusStore()
const metricsModal = ref()
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
  }
}
</style>
