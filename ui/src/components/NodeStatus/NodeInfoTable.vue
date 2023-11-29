<template>
  <TableCard>
    <div class="container">
      <table class="data-table" aria-label="Node Info Table">
        <thead>
          <tr>
            <th scope="col">Location</th>
            <th scope="col">System Name</th>
            <th scope="col">System Description</th>
            <th scope="col">System Location</th>
            <th scope="col">System Contact</th>
            <th scope="col">ObjectID</th>
          </tr>
        </thead>
        <TransitionGroup name="data-table" tag="tbody">
          <tr v-for="node in nodeData" :key="node.id">
            <td>{{ node.location?.location }}</td>
            <td>{{ node.systemName }}</td>
            <td>{{ node.systemDescr }}</td>
            <td>{{ node.systemLocation }}</td>
            <td>{{ node.systemContact }}</td>
            <td>{{ node.objectId }}</td>
          </tr>
        </TransitionGroup>
      </table>
    </div>
  </TableCard>
</template>

<script lang="ts" setup>
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
const nodeStatusStore = useNodeStatusStore()
  
const nodeData = computed(() => {
  return {
    node: nodeStatusStore.fetchedData?.node
  }
})
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";
@use "@featherds/table/scss/table";
@use "@/styles/_transitionDataTable";

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
