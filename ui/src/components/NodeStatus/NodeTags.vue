<template>
  <div>
    <section class="node-component-header">
      <h3 data-test="heading" class="node-label">Tags</h3>
    </section>
    <section class="node-component-content">
      <FeatherChipList
        label=""
        data-test="node-tags-chip-list"
      >
        <FeatherChip
          v-for="(item, index) in chips"
          :key="index"
        >
          <span>{{ item }}</span>
          <template v-slot:icon
            ><FeatherIcon
              @click="unselectItem(item as string)"
              :icon="CancelIcon"
          /></template>
        </FeatherChip>
      </FeatherChipList>
    </section>
  </div>
</template>

<script lang="ts" setup>
import CancelIcon from '@featherds/icon/navigation/Cancel'

// TODO: Actual implementation
const chips = ref<string[]>(['Tag Name 1', 'Other Name', 'Name 3'])

const unselectItem = (name: string) => {
  // TODO
  console.log(`Unselected tag: ${name}`)
}

</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';
@use '@featherds/styles/mixins/typography';

.node-component-header {
  margin-bottom: var(variables.$spacing-s);
  display: flex;
  flex-direction: row;
  gap: 0.5rem;
  align-items: center;
  justify-content: space-between;
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

</style>
