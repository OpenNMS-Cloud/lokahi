<template>
  <div class="main-section">
    <section class="feather-row">
      <h3 data-test="heading" class="feather-col-6">Recent Alerts</h3>
      <div class="btns feather-col-6">
        <FeatherButton
          primary
          icon="Download"
          >
        <FeatherIcon :icon="icons.DownloadFile" @click.prevent="nodeStatusStore.downloadAlertsByNodesToCsv"/>
          </FeatherButton>
          <FeatherButton
            primary
            icon="Refresh"
            @click="fetchAlertsByNodeList"
          >
          <FeatherIcon :icon="icons.Refresh"/>
          </FeatherButton>
      </div>
    </section>
    <section class="node-component-content">
      <TableCard>
        <div class="container">
          <table class="data-table" aria-label="Recent Alerts Table" data-test="data-table">
            <thead>
              <tr>
                <FeatherSortHeader
                v-for="col of columns"
                :key="col.label"
                scope="col"
                :property="col.id"
                :sort="(sort as any)[col.id]"
                v-on:sort-changed="sortChanged(col.id, $event)"
              >
                {{ col.label }}
              </FeatherSortHeader>
              </tr>
            </thead>
            <TransitionGroup name="data-table" tag="tbody"  v-if="isAlertsLength">
              <tr v-for="alert in alertsData" :key="alert.label as string" data-test="data-item">
                <td class="alert-type">
                  <router-link to="#">
                  {{ alert?.type || 'Unknown' }}
                  </router-link>
                </td>
                <td>
                  <PillColor
                    :item="showSeverity(alert?.severity)"
                      data-test="severity-label"
                  />
                </td>
                <td  class="date-time">
                  <div
                    class="date headline"
                    data-test="date"
                  >
                   <span>{{ fnsFormat(alert.lastUpdateTimeMs, 'M/dd/yyyy') }}</span>
                  </div>
                  <div
                    class="time"
                    data-test="time"
                  >
                    <span>{{ fnsFormat(alert.lastUpdateTimeMs, 'HH:mm:ssxxx') }}</span>
                  </div>
                </td>
              </tr>
            </TransitionGroup>
          </table>
          <div v-if="!isAlertsLength || total === 0">
            <EmptyList
              :content="emptyListContent"
              data-test="empty-list"
            />
          </div>
        </div>
        <div>
          <div
            class="alert-list-bottom"
            v-if="isAlertsLength"
          >
            <FeatherPagination
                v-model="page"
                :pageSize="pageSize"
                :total="total"
                :pageSizes="[10, 20, 50]"
                @update:model-value="onPageChanged"
                @update:pageSize="onPageSizeChanged"
                data-test="pagination"
            />
          </div>
        </div>
      </TableCard>
    </section>
  </div>
</template>

<script lang="ts" setup>
import DownloadFile from '@featherds/icon/action/DownloadFile'
import Refresh from '@featherds/icon/navigation/Refresh'
import { SORT } from '@featherds/table'
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { IAlert } from '@/types/alerts'
import { format as fnsFormat } from 'date-fns'
import useSpinner from '@/composables/useSpinner'

const icons = markRaw({
  DownloadFile,
  Refresh
})

const alertsData = ref<IAlert[]>([])
const { startSpinner, stopSpinner } = useSpinner()
const nodeStatusStore = useNodeStatusStore()

const columns = [
  { id: 'type', label: 'Alert Type' },
  { id: 'severity', label: 'Severity' },
  { id: 'lastEventTime', label: 'Time' }
]

const emptyListContent = {
  msg: 'No results found.'
}
const sort = reactive({
  alertType: SORT.NONE,
  severity: SORT.NONE,
  data: SORT.NONE,
  time: SORT.ASCENDING
}) as any

const sortChanged = (columnId: string, sortObj: Record<string, string>) => {

  startSpinner()

  if (sortObj.value === 'asc' || sortObj.value === 'desc') {
    const sortAscending  = sortObj.value === 'asc' ? true : false
    const sortByAlerts = { sortAscending, sortBy: columnId }
    nodeStatusStore.alertsByNodeSortChanged(sortByAlerts)

  } else {
    const sortAscending  = true
    const sortByAlerts = { sortAscending, sortBy: 'id' }
    nodeStatusStore.alertsByNodeSortChanged(sortByAlerts)

  }

  for (const prop in sort) {
    sort[prop] = SORT.NONE
  }
  sort[sortObj.property] = sortObj.value

}

const showSeverity = (value: any) => {
  return { style: value as string }
}

const fetchAlertsByNodeList = async () => {
  startSpinner()
  await nodeStatusStore.getAlertsByNode()
  stopSpinner()
}

onMounted(() => {
  fetchAlertsByNodeList()
})

const page = computed(() => nodeStatusStore.alertsPagination.page || 1)
const pageSize = computed(() => nodeStatusStore.alertsPagination.pageSize)
const total = computed(() => nodeStatusStore.alertsPagination.total)
const data = computed(() => nodeStatusStore.fetchAlertsByNodeData || [])

const onPageChanged = (p: number) => {
  startSpinner()
  nodeStatusStore.setAlertsByNodePage(p)
}

const onPageSizeChanged = (p: number) => {
  startSpinner()
  nodeStatusStore.setAlertsByNodePageSize(p)
}

const isAlertsLength = computed(() => {
  const alerts = data.value?.alerts || []
  return alerts.length > 0
})

watch(() => nodeStatusStore.fetchAlertsByNodeData, () => {
  if (isAlertsLength.value) {
    const alerts = data.value?.alerts || []
    alertsData.value = [...alerts]
  }
  stopSpinner()
})

</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';
@use '@featherds/styles/mixins/typography';
@use '@featherds/table/scss/table';
@use '@/styles/_transitionDataTable';

.main-section {
  padding: 0 var(variables.$spacing-s) 0 var(variables.$spacing-s);
}
.feather-row {
  margin-bottom: var(variables.$spacing-s);
  display: flex;
  flex-direction: row;
  gap: 0.5rem;
  align-items: center;
  justify-content: space-between;
  padding-left: 0.9rem;
}

.node-component-label {
  margin: 0;
  line-height: 20px;
  letter-spacing: 0.28px;
}

.node-component-content {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 2rem;
}

.container {
  display: block;
  table {
    width: 100%;
    @include table.table;
    thead {
      background: var(variables.$background);
      text-transform: uppercase;
    }
   tr {
    .date-time {
      width: 15%;
     }
    td {
      white-space: nowrap;
      a {
        text-decoration: none;
      }
      a:visited{
        color: var(variables.$primary);
      }
    }
    td:first-child{
        color: var(variables.$primary);
      }
    }
  }
}
.alert-list-bottom {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  padding: var(variables.$spacing-m) 0 var(variables.$spacing-s) var(variables.$spacing-s);

  :deep(> .feather-pagination) {
    border: 0;
    min-height: auto;
  }
}
</style>
