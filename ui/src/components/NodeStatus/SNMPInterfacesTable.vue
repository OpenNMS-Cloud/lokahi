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
        data-test="SNMPInterfacesTable"
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
            v-for="snmpInterface in pageObjects"
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
      <div v-if="!hasSNMPInterfaces">
        <EmptyList
          :content="emptyListContent"
          data-test="empty-list"
        />
      </div>
      <FeatherPagination
      v-model="page"
      :pageSize="pageSize"
      :total="total"
      @update:modelValue="updatePage"
      @update:pageSize="updatePageSize"
      class="ip-interfaces-pagination py-2"
      v-if="hasSNMPInterfaces"
    ></FeatherPagination>
    </div>
  </TableCard>
  <NodeStatusMetricsModal ref="metricsModal" />
</template>

<script lang="ts" setup>
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { useFlowsStore } from '@/store/Views/flowsStore'
import { Exporter, SnmpInterface } from '@/types/graphql'
import { DeepPartial } from '@/types'
import Traffic from '@featherds/icon/action/Workflow'
import Flows from '@featherds/icon/action/SendWorkflow'

const router = useRouter()
const flowsStore = useFlowsStore()
const nodeStatusStore = useNodeStatusStore()

const page = ref(0)
const pageSize = ref(0)
const total = ref(0)
const pageObjects = ref([] as any[])
const searchLabel = ref('Search IP Interfaces')
const searchVal = ref('')
const isMounted = ref(false)
const metricsModal = ref()
const emptyListContent = {
  msg: 'No results found.'
}
const snmpInterfaces = computed(() => {
  if (nodeStatusStore.node.snmpInterfaces && nodeStatusStore.node.snmpInterfaces.length > 0 && isMounted.value) {
    return nodeStatusStore.node.snmpInterfaces
  }
  return []
})
const hasSNMPInterfaces = computed(() => {
  return snmpInterfaces.value && snmpInterfaces.value.length > 0 && isMounted.value
})
const icons = markRaw({
  Traffic,
  Flows
})

onMounted(() => {
  isMounted.value = true
})

watch(() => [snmpInterfaces.value], () => {
  if (snmpInterfaces.value?.length && isMounted.value) {
    page.value = 1
    pageSize.value = 10
    total.value = snmpInterfaces.value.length
    pageObjects.value = getPageObjects(snmpInterfaces.value, page.value, pageSize.value)
  }
})
// Function to retrieve objects for a given page
const getPageObjects = (array: Array<any>, pageNumber: number, pageSize: number) => {
  const startIndex = (pageNumber - 1) * pageSize
  const endIndex = startIndex + pageSize
  return array.slice(startIndex, endIndex)
}
const updatePage = (v: number) => {
  if (hasSNMPInterfaces.value) {
    total.value = snmpInterfaces.value.length
    pageObjects.value = getPageObjects(snmpInterfaces.value, v, pageSize.value)
  }
}
const updatePageSize = (v: number) => {
  if (hasSNMPInterfaces.value) {
    pageSize.value = v
    pageObjects.value = getPageObjects(snmpInterfaces.value, page.value, v)
  }
}
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
onBeforeUnmount(() => {
  isMounted.value = false
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
  }
}
</style>
