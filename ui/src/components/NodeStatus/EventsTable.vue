<template>
  <TableCard>
    <div class="header">
      <div class="title-container">
        <span class="title">
          Events
        </span>
      </div>
    </div>
    <div class="container">
      <table class="data-table" aria-label="Events Table" data-test="data-table">
        <thead>
          <tr>
            <th scope="col">ID</th>
            <th scope="col">Time</th>
            <th scope="col">UEI</th>
            <th scope="col">Node Id</th>
            <th scope="col">IP Address</th>
          </tr>
        </thead>
        <TransitionGroup name="data-table" tag="tbody">
          <tr v-for="event in nodeData.events" :key="event.id as number" data-test="data-item">
            <td>{{ event.id }}</td>
            <td>{{ event.producedTime }}</td>
            <td>{{ event.uei }}</td>
            <td>{{ event.nodeId }}</td>
            <td>{{ event.ipAddress }}</td>
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
</template>

<script lang="ts" setup>
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'

const nodeStatusStore = useNodeStatusStore()

// Pagination
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const updatePageSize = (v: number) => { pageSize.value = v }
  
const nodeData = computed(() => {
  const events = nodeStatusStore.fetchedData?.events

  total.value = events?.length || 0

  return {
    events
  }
})
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";
@use "@featherds/table/scss/table";
@use "@/styles/_transitionDataTable";

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
  .search {
    width: 300px;
  }
  .btns {
    display: flex;
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

.feather-pagination {
  margin-top: var(variables.$spacing-xl);
  justify-content: center;
}
</style>
