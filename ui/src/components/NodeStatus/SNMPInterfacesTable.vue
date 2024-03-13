<template>
  <TableCard>
    <div class="header">
      <div class="title-container">
        <span class="title">
          SNMP Interfaces
        </span>
      </div>
      <div class="action-container">
        <div class="search-container">
          <FeatherInput
            :label="searchLabel"
            v-model.trim="searchVal"
            type="search"
            data-test="search-input"
            @update:model-value="onSearchChange"
          >
            <template #pre>
              <FeatherIcon :icon="icons.Search" />
            </template>
          </FeatherInput>
        </div>
        <div class="download-csv">
          <FeatherButton
              primary
              icon="Download"
            >
              <FeatherIcon :icon="icons.DownloadFile"> </FeatherIcon>
            </FeatherButton>
        </div>
        <div class="refresh">
          <FeatherButton
              primary
              icon="Refresh"
            >
              <FeatherIcon :icon="icons.Refresh"> </FeatherIcon>
            </FeatherButton>
        </div>
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
            <FeatherSortHeader
                v-for="col of columns"
                :key="col.label"
                scope="col"
                :property="col.id"
                :sort="(sort as any)[col.id]"
                v-on:sort-changed="sortChanged"
                >
                {{ col.label }}
              </FeatherSortHeader>
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
import { useFlowsStore } from '@/store/Views/flowsStore'
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { DeepPartial } from '@/types'
import { Exporter, SnmpInterface } from '@/types/graphql'
import DownloadFile from '@featherds/icon/action/DownloadFile'
import Search from '@featherds/icon/action/Search'
import Flows from '@featherds/icon/action/SendWorkflow'
import Traffic from '@featherds/icon/action/Workflow'
import Refresh from '@featherds/icon/navigation/Refresh'
import { SORT } from '@featherds/table'
import { sortBy } from 'lodash'

const router = useRouter()
const flowsStore = useFlowsStore()
const nodeStatusStore = useNodeStatusStore()

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pageObjects = ref([] as any[])
const clonedInterfaces = ref([] as any[])
const searchLabel = ref('Search SNMP Interfaces')
const searchVal = ref('')
const searchableAttributes = ['ifName', 'ifDescr', 'ifAlias', 'physicalAddr']
const metricsModal = ref()
const emptyListContent = {
  msg: 'No results found.'
}
const hasSNMPInterfaces = computed(() => {
  return nodeStatusStore.node.snmpInterfaces && nodeStatusStore.node.snmpInterfaces.length > 0
})
const snmpInterfaces = computed(() => {
  if (hasSNMPInterfaces.value) {
    return nodeStatusStore.node.snmpInterfaces
  }
  return []
})
const icons = markRaw({
  Traffic,
  Flows,
  DownloadFile,
  Refresh,
  Search
})
const columns = [
  { id: 'ifAlias', label: 'Alias' },
  { id: 'ipAddress', label: 'IP Addr' },
  { id: 'graphs', label: 'Graphs' },
  { id: 'physicalAddr', label: 'Physical Addr' },
  { id: 'ifIndex', label: 'Index' },
  { id: 'ifDescr', label: 'Desc' },
  { id: 'ifType', label: 'Type' },
  { id: 'ifName', label: 'Name' },
  { id: 'ifSpeed', label: 'Speed' },
  { id: 'ifAdminStatus', label: 'Admin Status' },
  { id: 'ifOperatorStatus', label: 'Operator Status' }
]
const sort = reactive({
  ifAlias: SORT.NONE,
  ipAddress: SORT.NONE,
  graphs: SORT.NONE,
  physicalAddr: SORT.NONE,
  ifIndex: SORT.NONE,
  ifDescr: SORT.NONE,
  ifType: SORT.NONE,
  ifName: SORT.NONE,
  ifSpeed: SORT.NONE,
  ifAdminStatus: SORT.NONE,
  ifOperatorStatus: SORT.NONE
}) as any
const updateSNMPInterfaces = () => {
  if (hasSNMPInterfaces.value) {
    total.value = snmpInterfaces.value.length
    clonedInterfaces.value = snmpInterfaces.value
    pageObjects.value = getPageObjects(snmpInterfaces.value, page.value, pageSize.value)
  }
}
onMounted(() => {
  updateSNMPInterfaces()
})
watch(() => snmpInterfaces.value, () => {
  updateSNMPInterfaces()
})
// Function to retrieve objects for a given page
const getPageObjects = (array: Array<any>, pageNumber: number, pageSize: number) => {
  const startIndex = (pageNumber - 1) * pageSize
  const endIndex = startIndex + pageSize
  return array.slice(startIndex, endIndex)
}
const sortChanged = (sortObj: Record<string, string>) => {
  if (sortObj.value === 'asc') {
    clonedInterfaces.value = sortBy(snmpInterfaces.value, sortObj.property)
  }
  if (sortObj.value === 'desc') {
    clonedInterfaces.value = sortBy(snmpInterfaces.value, sortObj.property).reverse()
  }
  if (sortObj.value === 'none') {
    clonedInterfaces.value = sortBy(snmpInterfaces.value, 'id')
  }

  page.value = 1
  pageObjects.value = getPageObjects(clonedInterfaces.value, page.value, pageSize.value)
  for (const prop in sort) {
    sort[prop] = SORT.NONE
  }
  sort[sortObj.property] = sortObj.value
}
const updatePage = (v: number) => {
  if (hasSNMPInterfaces.value) {
    page.value = v
    pageObjects.value = getPageObjects(clonedInterfaces.value, v, pageSize.value)
  }
}
const updatePageSize = (v: number) => {
  if (hasSNMPInterfaces.value) {
    pageSize.value = v
    pageObjects.value = getPageObjects(clonedInterfaces.value, page.value, v)
  }
}
const searchPageObjects = (searchTerm: any) => {
  return snmpInterfaces.value.filter((item: SnmpInterface) => {
    return searchableAttributes.some((attr) => {
      const value = item[attr as unknown as keyof SnmpInterface]
      return value.toLowerCase().includes(searchTerm.toLowerCase())
    })
  })
}
const onSearchChange = (searchTerm: any) => {
  if (searchTerm.trim().length > 0) {
    const searchObjects = searchPageObjects(searchTerm)

    page.value = 1
    total.value = searchObjects.length
    clonedInterfaces.value = searchObjects
    pageObjects.value = getPageObjects(searchObjects, page.value, pageSize.value)
  } else {
    page.value = 1
    total.value = snmpInterfaces.value.length
    clonedInterfaces.value = snmpInterfaces.value
    pageObjects.value = getPageObjects(snmpInterfaces.value, page.value, pageSize.value)
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
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/table/scss/table';
@use '@/styles/_transitionDataTable';

.header {
  display: flex;
  justify-content: space-between;
  padding: 0px 10px;
  .title-container {
    display: flex;
    .title {
      @include typography.headline3;
      margin-left: 15px;
      margin-top: 2px;
    }
  }
  .action-container{
    display: flex;
    justify-content: flex-end;
    gap: 5px;
    width: 30%;
    >.search-container{
      width: 80%;
      margin-right: 5px;
      :deep(.label-border) {
        width: 130px !important;
      }
    }
  }
}

.container {
  display: block;
  overflow-x: auto;
  padding: 0px 15px;

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

  :deep(.ip-interfaces-pagination) {
    border: none;
    padding: 20px 0px;
  }
}
</style>
