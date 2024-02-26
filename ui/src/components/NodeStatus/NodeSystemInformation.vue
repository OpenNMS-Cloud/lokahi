<template>
  <div class="main-section">
    <section class="node-component-header">
      <div class="feather-row">
        <h3 data-test="heading" class="node-label">System Information</h3>
      </div>
    </section>
    <section class="node-component-content">
      <div class="feather-row">
        <div class="feather-col-6 ">Monitoring Location</div>
        <div class="feather-col-6 column-value">{{ nodeData.node.location?.location || '' }}</div>
      </div>
      <div class="feather-row">
        <div class="feather-col-6 ">System Name</div>
        <div class="feather-col-6 column-value">{{ nodeData.node.systemName  || '' }}</div>
      </div>
      <div class="feather-row">
        <div class="feather-col-6 ">System Location</div>
        <div class="feather-col-6 column-value">{{ nodeData.node.systemLocation || '' }}</div>
      </div>
      <div class="feather-row">
        <div class="feather-col-6 ">Contact</div>
        <div class="feather-col-6 column-value">{{ nodeData.node.systemContact || '' }}</div>
      </div>
        <div class="feather-row">
        <div class="feather-col-6 ">System Description</div>
      </div>
      <div class="feather-row">
        <div class="feather-col-12 column-value">{{ nodeData.node.systemDescr || '' }}</div>
      </div>
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
 }
 .node-component-content {
    .feather-row {
      min-height: 2.5rem;
      line-height: 1.25rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      .column-value {
        font-weight: var(variables.$font-semibold);
        color: var(variables.$primary-text-on-surface);
      }

      .feather-col-6.column-value {
        text-align: right;
      }
      .feather-col-12.column-value {
        text-align: left;
      }
    }
  }
}
</style>
