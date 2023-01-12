<template>
  <ul>
    <li v-for="node in tabContent" :key="node?.id">
      <section class="header">
        <Icon :icon="icon" data-test="icon" />
        <h4 data-test="heading">{{ node?.label }}</h4>
      </section>
      <section class="node-content">
        <div>
          <InventoryMetricChipList :metrics="node?.metrics" data-test="metric-chip-list" />
          <InventoryTextAnchorList :anchor="node.anchor" data-test="text-anchor-list" />
        </div>
        <InventoryIconActionList :node="node" class="icon-action" data-test="icon-action-list" />
      </section>
    </li>
  </ul>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import Storage from '@material-design-icons/svg/outlined/storage.svg'
import { NodeContent } from '@/types/inventory'
import { IIcon } from '@/types'
  
defineProps({
  tabContent: {
    type: Object as PropType<NodeContent[]>,
    required: true
  }
})

const icon: IIcon = {
  image: Storage,
  title: 'Node'
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

ul {
  display: flex;
  flex-flow: row wrap;
  gap: 1rem;
  > li {
    padding: var(variables.$spacing-l) var(variables.$spacing-l);
    border: 1px solid var(variables.$secondary-text-on-surface); 
    border-radius: 10px;
    border-left: 10px solid var(variables.$secondary-text-on-surface); // TODO set color dynamically to the node's status

    > .header {
      margin-bottom: var(variables.$spacing-s);
      display: flex;
      flex-direction: row;
      gap: 0.5rem;
      align-items: center;
      > h4 {
        color: var(variables.$primary);
      }
    }
  }
  .node-content {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    gap: 2rem;
  }
}
</style>