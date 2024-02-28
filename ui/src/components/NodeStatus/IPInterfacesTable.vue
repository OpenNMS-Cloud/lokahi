<template>
  <TableCard class="ip-interfaces-container">
    <div class="header">
      <div class="title-container">
        <span class="title">
          {{ isAzure ? 'Network Interfaces' : 'IP Interfaces' }}
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
            <td v-if="isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.privateIpId }}</td>
            <td v-if="isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.interfaceName }}</td>
            <td v-if="isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.publicIpAddress }}</td>
            <td v-if="isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.publicIpId }}</td>
            <td v-if="isAzure">
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
            <td v-if="isAzure">{{ nodeStatusStore.node.azureInterfaces.get(ipInterface.azureInterfaceId)?.location }}</td>
            <td v-if="!isAzure">{{ ipInterface.hostname }}</td>
            <td v-if="!isAzure">{{ ipInterface.netmask }}</td>
            <td v-if="!isAzure">{{ ipInterface.snmpPrimary }}</td>
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
      v-if="hasIPInterfaces"
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
import { sortBy } from 'lodash'
const nodeStatusStore = useNodeStatusStore()
const metricsModal = ref()

const fsIcons = markRaw({
  DownloadFile,
  Refresh,
  Search
})

const sort = reactive({
  ipAddress: SORT.NONE,
  privateIpId: SORT.NONE,
  interfaceName: SORT.NONE,
  publicIpId: SORT.NONE,
  publicIpAddress: SORT.NONE,
  location: SORT.NONE,
  hostname: SORT.NONE,
  netmask: SORT.NONE,
  snmpPrimary: SORT.NONE
}) as any

const page = ref(0)
const pageSize = ref(0)
const total = ref(0)
const pageObjects = ref([] as any[])
const clonedInterfaces = ref([] as any[])
const searchLabel = ref('Search IP Interfaces')
const searchVal = ref('')
const isMounted = ref(false)
onMounted(() => {
  isMounted.value = true
})
const isAzure = computed(() => {
  return nodeStatusStore.isAzure
})
const hasIPInterfaces = computed(() => {
  return nodeStatusStore?.node?.ipInterfaces?.length && nodeStatusStore?.node?.ipInterfaces?.length > 0 && isMounted.value
})
const ipInterfaces = computed(() => {
  if (hasIPInterfaces.value) {
    let interfaces = [] as any[]
    if (isAzure.value) {
      nodeStatusStore?.node?.ipInterfaces?.forEach(element => {
        interfaces.push(nodeStatusStore.node.azureInterfaces.get(element.azureInterfaceId))
      })
    } else {
      interfaces = nodeStatusStore.node.ipInterfaces ?? []
    }
    return interfaces
  }
  return []
})
const columns = computed(() => {
  if (isAzure.value) {
    return [
      { id: 'ipAddress', label: 'IP Address' },
      { id: 'privateIpId', label: 'Private IP ID' },
      { id: 'interfaceName', label: 'Interface Name' },
      { id: 'publicIpId', label: 'Public IP ID' },
      { id: 'publicIpAddress', label: 'Graphs' },
      { id: 'location', label: 'Location' }
    ]
  } else {
    return [
      { id: 'ipAddress', label: 'IP Address' },
      { id: 'hostname', label: 'IP Hostname' },
      { id: 'netmask', label: 'Netmask' },
      { id: 'snmpPrimary', label: 'Primary' }
    ]
  }
})
watch([() => ipInterfaces.value, () => isMounted.value], () => {
  if (ipInterfaces.value?.length && isMounted.value) {
    page.value = 1
    pageSize.value = 10
    total.value = ipInterfaces.value.length
    clonedInterfaces.value = ipInterfaces.value
    pageObjects.value = getPageObjects(ipInterfaces.value, page.value, pageSize.value)
  }
})
// Function to retrieve objects for a given page
const getPageObjects = (array: Array<any>, pageNumber: number, pageSize: number) => {
  const startIndex = (pageNumber - 1) * pageSize
  const endIndex = startIndex + pageSize
  return array.slice(startIndex, endIndex)
}
const sortChanged = (sortObj: Record<string, string>) => {
  let sorted = [] as any
  if (sortObj.value === 'asc') {
    sorted = sortBy(ipInterfaces.value, sortObj.property)
  } else if (sortObj.value === 'desc') {
    sorted = sortBy(ipInterfaces.value, sortObj.property).reverse()
  } else {
    sorted = ipInterfaces.value
  }
  clonedInterfaces.value = sorted

  page.value = 1
  pageSize.value = 10
  total.value = ipInterfaces.value.length

  pageObjects.value = getPageObjects(sorted, page.value, pageSize.value)
  for (const prop in sort) {
    sort[prop] = SORT.NONE
  }
  sort[sortObj.property] = sortObj.value
}
const updatePage = (v: number) => {
  if (hasIPInterfaces.value) {
    total.value = ipInterfaces.value.length
    pageObjects.value = getPageObjects(clonedInterfaces.value, v, pageSize.value)
  }
}
const updatePageSize = (v: number) => {
  if (hasIPInterfaces.value) {
    pageSize.value = v
    pageObjects.value = getPageObjects(clonedInterfaces.value, page.value, v)
  }
}
onBeforeUnmount(() => {
  isMounted.value = false
})
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
