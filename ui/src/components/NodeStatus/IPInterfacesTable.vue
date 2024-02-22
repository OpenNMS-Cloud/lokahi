<template>
  <TableCard class="ip-interfaces-container">
    <div class="header">
      <div class="title-container">
        <span class="title">
          {{ nodeStatusStore.isAzure ? 'Network Interfaces' : 'IP Interfaces' }}
        </span>
      </div>
      <div class="action-container">
        <div class="search-container">
          <FeatherInput
            :label="searchLabel"
            v-model.trim="searchVal"
            type="search"
            data-test="search-input"
          >
            <template #pre>
              <FeatherIcon :icon="fsIcons.Search" />
            </template>
          </FeatherInput>
          {{ searchVal }}
        </div>
        <div class="download-csv">
          <FeatherButton
              primary
              icon="Download"
            >
              <FeatherIcon :icon="fsIcons.DownloadFile"> </FeatherIcon>
            </FeatherButton>
        </div>
        <div class="refresh">
          <FeatherButton
              primary
              icon="Refresh"
            >
              <FeatherIcon :icon="fsIcons.Refresh"> </FeatherIcon>
            </FeatherButton>
        </div>
      </div>
    </div>
    <div class="container">
      <table
        class="data-table tc2"
        aria-label="IP Interfaces Table"
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
            v-for="ipInterface in pageObjects"
            :key="ipInterface.id"
          >
            <td>{{ ipInterface.ipAddress }}</td>
            <td v-if="nodeStatusStore.isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.privateIpId }}</td>
            <td v-if="nodeStatusStore.isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.interfaceName }}</td>
            <td v-if="nodeStatusStore.isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.publicIpAddress }}</td>
            <td v-if="nodeStatusStore.isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.publicIpId }}</td>
            <td v-if="nodeStatusStore.isAzure">
              <FeatherTooltip
                title="Traffic"
              >
                <FeatherButton v-if="nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.publicIpAddress != ''"
                  icon="Traffic"
                  text
                  @click="metricsModal.openAzureMetrics(nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId))"
                  ><FeatherIcon :icon="icons.Traffic" />
                </FeatherButton>
              </FeatherTooltip>
            </td>
            <td v-if="nodeStatusStore.isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.location }}</td>
            <td v-if="!nodeStatusStore.isAzure">{{ ipInterface.hostname }}</td>
            <td v-if="!nodeStatusStore.isAzure">{{ ipInterface.netmask }}</td>
            <td v-if="!nodeStatusStore.isAzure">{{ ipInterface.snmpPrimary }}</td>
          </tr>
        </TransitionGroup>
      </table>
      <FeatherPagination
      v-model="page"
      :pageSize="pageSize"
      :total="total"
      @update:modelValue="updatePage"
      @update:pageSize="updatePageSize"
      class="ip-interfaces-pagination py-2"
      v-if="nodeStatusStore?.node?.ipInterfaces?.length && nodeStatusStore?.node?.ipInterfaces?.length > 0"
    ></FeatherPagination>
    </div>
  </TableCard>
  <NodeStatusMetricsModal ref="metricsModal" />
</template>

<script lang="ts" setup>
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import Traffic from '@featherds/icon/action/Workflow'
import { FeatherPagination } from '@featherds/pagination'
import DownloadFile from '@featherds/icon/action/DownloadFile'
import Refresh from '@featherds/icon/navigation/Refresh'
import Search from '@featherds/icon/action/Search'
import { SORT } from '@featherds/table'
const nodeStatusStore = useNodeStatusStore()
const metricsModal = ref()

const fsIcons = markRaw({
  DownloadFile,
  Refresh,
  Search
})

const sort = reactive({
  iPAddress: SORT.NONE,
  privateIPID: SORT.NONE,
  interfaceName: SORT.NONE,
  publicIP: SORT.NONE,
  graphs: SORT.NONE,
  location: SORT.NONE,
  iPHostname: SORT.NONE,
  netmask: SORT.NONE,
  primary: SORT.NONE
}) as any

// Function to retrieve objects for a given page
function getPageObjects(array: Array<any>, pageNumber: number, pageSize: number) {
  const startIndex = (pageNumber - 1) * pageSize
  const endIndex = startIndex + pageSize
  return array.slice(startIndex, endIndex)
}

const page = ref(0)
const pageSize = ref(0)
const total = ref(0)
const pageObjects = ref([] as any[])
const searchLabel = ref('Search IP Interfaces')
const searchVal = ref('')
let columns = [] as any[]

watchEffect(() => {
  if (nodeStatusStore?.node?.ipInterfaces?.length && nodeStatusStore?.node?.ipInterfaces?.length > 0) {
    page.value = 1
    pageSize.value = 10
    total.value = nodeStatusStore?.node?.ipInterfaces?.length
    pageObjects.value = getPageObjects(nodeStatusStore?.node?.ipInterfaces, page.value, pageSize.value)
  }
})

watchEffect(() => {
  if (nodeStatusStore.isAzure) {
    columns = [
      { id: 'iPAddress', label: 'IP Address' },
      { id: 'privateIPID', label: 'Private IP ID' },
      { id: 'interfaceName', label: 'Interface Name' },
      { id: 'publicIP', label: 'Public IP ID' },
      { id: 'graphs', label: 'Graphs' },
      { id: 'location', label: 'Location' }
    ]
  } else if (!nodeStatusStore.isAzure) {
    columns = [
      { id: 'IP Address', label: 'IP Address' },
      { id: 'iPHostname', label: 'IP Hostname' },
      { id: 'netmask', label: 'Netmask' },
      { id: 'Primary', label: 'Primary' }
    ]
  }
})

const sortChanged = (sortObj: Record<string, string>) => {
  // store.setTopNNodesTableSort(sortObj)

  for (const prop in sort) {
    sort[prop] = SORT.NONE
  }

  sort[sortObj.property] = sortObj.value
}

const updatePage = (v: number) => {
  if (nodeStatusStore?.node?.ipInterfaces?.length && nodeStatusStore?.node?.ipInterfaces?.length > 0) {
    pageObjects.value = getPageObjects(nodeStatusStore?.node?.ipInterfaces, v, pageSize.value)
  }
}

const updatePageSize = (v: number) => {
  if (nodeStatusStore?.node?.ipInterfaces?.length && nodeStatusStore?.node?.ipInterfaces?.length > 0) {
    pageObjects.value = getPageObjects(nodeStatusStore?.node?.ipInterfaces, page.value, v)
  }
}

const icons = markRaw({
  Traffic
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
        width: 110px !important;
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
