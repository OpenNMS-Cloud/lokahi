<template>
  <div class="top-node-wrapper">
    <TableCard class="card border">
      <div class="header">
        <div class="title-container">
          <div class="title-time">
            <span class="title">Top Nodes</span>
            <span class="time-frame">24 h</span>
          </div>
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
              v-for="topNode in topNodes"
              :key="topNode.nodeLabel"
            >
              <td>{{ topNode.nodeLabel }}</td>
              <td>{{ topNode.location }}</td>
              <td>{{ topNode.avgResponseTime ? `${+topNode.avgResponseTime.toFixed(2)}ms` : `--` }}</td>
              <td>{{ +topNode.reachability.toFixed(2) }}%</td>
            </tr>
          </TransitionGroup>
        </table>
      </div>
    </TableCard>
  </div>
</template>

<script setup lang="ts">
import { useDashboardStore } from '@/store/Views/dashboardStore'
import { SORT } from '@featherds/table'
import { orderBy } from 'lodash'
import { TopNNode } from '@/types/graphql'
const store = useDashboardStore()
const topNodes = ref([] as TopNNode[])

const columns = [
  { id: 'nodeLabel', label: 'Node' },
  { id: 'location', label: 'Location' },
  { id: 'avgResponseTime', label: 'Response Time' },
  { id: 'reachability', label: 'Reachability' }
]

const sort = reactive({
  nodeLabel: SORT.NONE,
  location: SORT.NONE,
  avgResponseTime: SORT.NONE,
  reachability: SORT.NONE
})

const sortChanged = (sortObj: any) => {
  topNodes.value = orderBy(topNodes.value, sortObj.property, sortObj.value)
  ;(sort as any)[sortObj.property] = sortObj.value
}

onMounted(async () => {
  await store.getTopNNodes()
  topNodes.value = orderBy(store.topNodes, ['reachability', 'avgResponseTime'], ['asc', 'asc'])
})
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

  .card {
    height: 442px;

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
        .btns {
          margin-right: 15px;
        }
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
