<template>
  <div class="top-node-wrapper">
    <TableCard class="border">
      <div class="header">
        <div class="title-container">
          <div class="title-time">
            <span class="title">Top Nodes</span>
            <span class="time-frame">24 h</span>
          </div>
          <FeatherButton
            icon="Download to CSV"
            @click="downloadTopNodesToCsv"
            class="btn"
          >
            <FeatherIcon :icon="icons.Download" />
          </FeatherButton>
        </div>
      </div>
      <div class="container">
        <table
          class="data-table"
          aria-label="Top Nodes Table"
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
              v-for="topNode in store.topNodes"
              :key="topNode.id"
            >
              <td>{{ topNode.nodeLabel }}</td>
              <td>{{ topNode.location?.location }}</td>
              <td>{{ topNode.responseTime }} ms</td>
              <td>{{ topNode.reachability }}%</td>
            </tr>
          </TransitionGroup>
        </table>
        <FeatherPagination
          v-model="page"
          :pageSize="pageSize"
          :total="total"
        ></FeatherPagination>
      </div>
    </TableCard>
  </div>
</template>

<script setup lang="ts">
import { useDashboardStore } from '@/store/Views/dashboardStore'
import { buildCsvExport, generateBlob, generateDownload } from '../utils'
import Download from '@featherds/icon/action/DownloadFile'
import { SORT } from '@featherds/table'
import { clone } from 'lodash'
const store = useDashboardStore()

const icons = markRaw({
  Download
})

const page = 1
const pageSize = 4
const total = 4

const columns = [
  { id: 'nodeLabel', label: 'Node' },
  { id: 'location', label: 'Location' },
  { id: 'responseTime', label: 'Response Time' },
  { id: 'reachability', label: 'Reachability' }
]

const sort = reactive({
  nodeLabel: SORT.NONE,
  location: SORT.NONE,
  responseTime: SORT.NONE,
  reachability: SORT.NONE
})

const sortChanged = (sortObj: any) => {
  console.log(sortObj)
}

const downloadTopNodesToCsv = async () => {
  const exportableNodes = []
  const exportableNode: any = {}

  for (const node of store.topNodes) {
    for (const col of columns) {
      let val: string | null = null

      if (col.id === 'location') {
        val = (node as any)[col.id][col.id]
      } else {
        val = (node as any)[col.id]
      }

      if (val !== null) {
        exportableNode[col.id] = val
      }
    }
    const copy = clone(exportableNode)
    exportableNodes.push(copy)
  }

  const csvRows = buildCsvExport(columns, exportableNodes)
  const data = csvRows.join('\n')

  const blob = generateBlob(data, 'text/csv')
  generateDownload(blob, `TopNodes.csv`)
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/table/scss/table';
@use '@/styles/_transitionDataTable';
@use '@/styles/vars.scss';

.top-node-wrapper {
  margin-bottom: 20px;
  width: 100%;
  .header {
    display: flex;
    justify-content: space-between;
    .title-container {
      display: flex;
      justify-content: space-between;
      width: 100%;
      margin-bottom: 20px;

      .title-time {
        display: flex;
        flex-direction: column;
        .title {
          @include typography.headline3;
          margin-left: 20px;
          margin-top: 2px;
        }
        .time-frame {
          @include typography.caption;
          margin-left: 20px;
        }
      }
      .btn {
        margin-right: 15px;
      }
    }
  }

  .container {
    display: block;
    overflow-x: auto;
    margin: 0px 20px;
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
}
</style>

<style land="scss">
/* hide page size selection for top nodes */
.per-page-text,
.page-size-select {
  display: none !important;
}
</style>
