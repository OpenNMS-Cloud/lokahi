<template>
  <div class="main-section">
    <section class="node-component-header">
      <h3 data-test="heading" class="node-label">System Information</h3>
    </section>
    <section class="node-component-content">
      <table class="simple-table">
        <tr>
          <td>Monitoring Location</td>
          <td class="value">{{ nodeData.node.location?.location || 'Default' }}</td>
        </tr>
        <tr>
          <td>System Name</td>
          <td class="value">{{ nodeData.node.systemName || 'server-123' }}</td>
        </tr>
        <tr>
          <td>System Location</td>
          <td class="value">{{ nodeData.node.systemLocation || 'Somewhere' }}</td>
        </tr>
        <tr>
          <td>Contact</td>
          <td class="value">{{ nodeData.node.systemContact || 'someone@opennms.com' }}</td>
        </tr>
        <tr>
          <td>System Description</td>
        </tr>
        <tr>
          <td>{{ nodeData.node.systemDescr || 'Linux Ubuntu' }}</td>
        </tr>
      </table>
    </section>
  </div> 
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
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';
@use '@featherds/styles/mixins/typography';

.main-section { 
  padding: 0 var(variables.$spacing-l) var(variables.$spacing-l) var(variables.$spacing-l);
  .node-component-header {
  margin-bottom: var(variables.$spacing-s);
  display: flex;
  flex-direction: row;
  gap: 0.5rem;
  align-items: center;
  justify-content: space-between;
  .node-label{
    color: var(variables.$secondary);
  }
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

.simple-table {
  width: 100%;
  td.value {
    text-align: right;
  }
  td.value, tr:last-child td{
    font-weight: 700;
    color: var(variables.$secondary);
    padding-top: var(variables.$spacing-m);
  }
 }
}

</style>
