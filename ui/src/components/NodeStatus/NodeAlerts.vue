<template>
  <div class="main-section">
    <section class="feather-row">
      <h3 data-test="heading" class="feather-col-6">Recent Alerts</h3>
      <div class="btns feather-col-6">
        <FeatherButton
          primary
          icon="Download"
          >
        <FeatherIcon :icon="icons.DownloadFile"> </FeatherIcon>
          </FeatherButton>
          <FeatherButton
            primary
            icon="Refresh"
          >
          <FeatherIcon :icon="icons.Refresh"> </FeatherIcon>
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
                v-on:sort-changed="sortChanged"
              >
                {{ col.label }}
              </FeatherSortHeader>
              </tr>
            </thead>
            <TransitionGroup name="data-table" tag="tbody">
              <tr v-for="alert in alertsData" :key="alert.label as string" data-test="data-item">
                <td class="alert-type">
                  <router-link to="#">
                  {{ alert?.label || 'Unknown' }}
                  </router-link>
                </td>
                <td>
                  <PillColor
                    :item="showSeverity(alert?.severity)"
                      data-test="severity-label"
                  />
                </td>
                <td  class="date headline"
                  data-test="date"> {{ fnsFormat(alert?.lastUpdateTimeMs, 'M/dd/yyyy') }}
                </td>
                <td  class="time"
                  data-test="time">{{ fnsFormat(alert?.lastUpdateTimeMs, 'HH:mm:ssxxx') }}</td>
              </tr>
            </TransitionGroup>
          </table>
        </div>
        <div>
          <!-- <FeatherPagination
            v-model="page"
            :pageSize="pageSize"
            :total="total"
            @update:pageSize="updatePageSize"
          /> -->
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

const icons = markRaw({
  DownloadFile,
  Refresh
})

const alertsData = ref<IAlert[]>([])
const nodeStatusStore = useNodeStatusStore()

const columns = [
  { id: 'alertType', label: 'Alert Type' },
  { id: 'severity', label: 'Severity' },
  { id: 'date', label: 'Date' },
  { id: 'time', label: 'Time' }
]

const sort = reactive({
  alertType: SORT.NONE,
  severity: SORT.NONE,
  data: SORT.NONE,
  time: SORT.ASCENDING
}) as any

const sortChanged = (sortObj: Record<string, string>) => {

  for (const prop in sort) {
    sort[prop] = SORT.NONE
  }
  sort[sortObj.property] = sortObj.value
}

const showSeverity = (value: any) => {
  return { style: value as string }
}

const fetchAlertsByNodeList = async () => {
  await nodeStatusStore.getNodeByAlerts()
}

onMounted(() => {
  fetchAlertsByNodeList()
})

const data = computed(() => nodeStatusStore.fetchAlertsByNodeData || [])

const isAlertsLength = computed(() => {
  const alerts = data.value?.alerts || []
  return alerts.length > 0
})

watch(() => nodeStatusStore.fetchAlertsByNodeData, () => {
  if (isAlertsLength.value) {
    const alerts = data.value?.alerts || []
    alertsData.value = [...alerts]
  }
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
  padding: 0 var(variables.$spacing-s) var(variables.$spacing-l) var(variables.$spacing-s);
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
    td {
      white-space: nowrap;
    }
    td:first-child{
        color: var(variables.$primary);
      }
    }
  }
}
</style>
