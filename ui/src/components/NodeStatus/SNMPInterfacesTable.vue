<template>
  <TableCard>
    <div class="header">
      <div class="title-container">
        <span class="title"> SNMP Interfaces </span>
      </div>
    </div>
    <div class="container">
      <table
        class="data-table tc3"
        aria-label="SNMP Interfaces Table"
      >
        <thead>
          <tr>
            <th scope="col">Alias</th>
            <th scope="col">IP Addr</th>
            <th scope="col">Graphs</th>
            <th scope="col">Physical Addr</th>
            <th scope="col">Index</th>
            <th scope="col">Desc</th>
            <th scope="col">Type</th>
            <th scope="col">Name</th>
            <th scope="col">Speed</th>
            <th scope="col">Admin Status</th>
            <th scope="col">Operator Status</th>
          </tr>
        </thead>
        <TransitionGroup
          name="data-table"
          tag="tbody"
        >
          <tr
            v-for="snmpInterface in nodeStatusStore.node.snmpInterfaces"
            :key="snmpInterface.id"
          >
            <td>{{ snmpInterface.ifAlias }}</td>
            <td>{{ snmpInterface.ipAddress }}</td>
            <td>
              <FeatherTooltip
                title="Traffic"
                v-slot="{ attrs, on }"
              >
                <FeatherButton
                  v-if="snmpInterface.ifName"
                  v-bind="attrs"
                  v-on="on"
                  icon="Traffic"
                  text
                  @click="metricsModal.setIfNameAndOpenModal(snmpInterface.ifName)"
                  ><FeatherIcon :icon="icons.Traffic" />
                </FeatherButton>
              </FeatherTooltip>

              <FeatherTooltip
                title="Flows"
                v-slot="{ attrs, on }"
              >
                <FeatherButton
                  v-if="snmpInterface.exporter.ipInterface"
                  v-bind="attrs"
                  v-on="on"
                  icon="Flows"
                  text
                  @click="routeToFlows(snmpInterface.exporter)"
                  ><FeatherIcon :icon="icons.Flows"
                /></FeatherButton>
              </FeatherTooltip>
            </td>
            <td>{{ snmpInterface.physicalAddr }}</td>
            <td>{{ snmpInterface.ifIndex }}</td>
            <td>{{ snmpInterface.ifDescr }}</td>
            <td>{{ snmpInterface.ifType }}</td>
            <td>{{ snmpInterface.ifName }}</td>
            <td>{{ snmpInterface.ifSpeed }}</td>
            <td>{{ snmpInterface.ifAdminStatus }}</td>
            <td>{{ snmpInterface.ifOperatorStatus }}</td>
          </tr>
        </TransitionGroup>
      </table>
    </div>
  </TableCard>
  <NodeStatusMetricsModal ref="metricsModal" />
</template>

<script lang="ts" setup>
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { useFlowsStore } from '@/store/Views/flowsStore'
import { Exporter } from '@/types/graphql'
import { DeepPartial } from '@/types'
import Traffic from '@featherds/icon/action/Workflow'
import Flows from '@featherds/icon/action/SendWorkflow'

const router = useRouter()
const flowsStore = useFlowsStore()
const nodeStatusStore = useNodeStatusStore()

const metricsModal = ref()

const icons = markRaw({
  Traffic,
  Flows
})

const routeToFlows = (exporter: DeepPartial<Exporter>) => {
  const { id: nodeId, nodeLabel } = nodeStatusStore.node

  flowsStore.filters.selectedExporters = [
    {
      _text: `${nodeLabel?.toUpperCase()} : ${exporter.snmpInterface?.ifName || exporter.ipInterface?.ipAddress}}`,
      value: {
        nodeId,
        ipInterfaceId: exporter.ipInterface?.id
      }
    }
  ]
  router.push('/flows').catch(() => 'Route to /flows unsuccessful.')
}
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
