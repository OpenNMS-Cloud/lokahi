<template>
  <div class="node-tag-section">
    <section class="node-component-header">
      <h3 data-test="heading" class="node-label">Tags</h3>
      <FeatherButton text class="manage-btn">Manage</FeatherButton>
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
          <span class="node-tag-content">{{ item }}</span>
          <FeatherIcon
              @click="unselectItem(item as string)"
              :icon="CancelIcon"
          /> 
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

.node-tag-section{
  margin: 0px 1.5rem 1.5rem 1.5rem;
  .node-component-header {
    margin-bottom: var(variables.$spacing-s);
    display: flex;
    flex-direction: row;
    gap: 0.5rem;
    align-items: center;
    justify-content: space-between;
    .btn-text {
    &:hover, &:focus {
      border-color: transparent;
    }
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
    .node-tag-content {
      padding-right: var(variables.$spacing-s);
    }
    :deep(.chip-icon) {
    background-color: transparent;
    padding: 6px 8px 6px 12px;
  }
  }
  .manage-btn {
    color: var(variables.$primary);
    font-weight: bold;
  } 
}
</style>
