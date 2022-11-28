<template>
  <ul class="tab-content">
    <li v-for="node in tab.nodes" :key="node.id">
      <section class="header">
        <Icon :item="item" data-test="icon" />
        <h4 data-test="heading">{{ node.name }}</h4>
      </section>
      <section class="node-content">
        <div>
          <MetricChipList :items="node.metrics" data-test="metric-chip-list" />
          <TextAnchorList :anchor="node.anchor" data-test="text-anchor-list" />
        </div>
        <IconActionList class="icon-action" data-test="icon-action-list" />
      </section>
    </li>
  </ul>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import Storage from '@material-design-icons/svg/outlined/storage.svg'
import { TabNode } from '@/types/inventory'
  
defineProps({
  tab: {
    type: Object as PropType<TabNode>,
    required: true
  }
})

const item = {
  icon: Storage,
  title: 'Node'
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

.tab-content {
  display: flex;
  flex-flow: row wrap;
  gap: 1rem;
  > li {
    padding: var(variables.$spacing-l);
    border: 1px solid var(variables.$secondary-text-on-surface); 
    border-radius: 10px;
    // TODO: set color dynamically
    border-left: 10px solid var(variables.$secondary-text-on-surface);
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
}

.node-content {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 2rem;
}
</style>