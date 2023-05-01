<template>
  <TableCard>
    <div class="header">
      <div class="title-container">
        <span class="title">
          SNMP Interfaces
        </span>
      </div>
    </div>
    <div class="container">
      <table class="data-table" aria-label="SNMP Interfaces Table">
        <thead>
          <tr>
            <th scope="col">Alias</th>
            <th scope="col">IP Addr</th>
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
        <TransitionGroup name="data-table" tag="tbody">
          <tr v-for="snmpInterface in nodeData.node.snmpInterfaces" :key="snmpInterface.id">
            <td>{{ snmpInterface.ifAlias }}</td>
            <td>{{ snmpInterface.ipAddress }}</td>
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
