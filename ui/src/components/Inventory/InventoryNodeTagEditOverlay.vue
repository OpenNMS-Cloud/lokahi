<template>
  <div class="overlay">
    <FeatherCheckbox id="tagged" :modelValue="isChecked" @update:model-value="inventoryStore.addRemoveNodesSelected(node)"
      class="tag-node-checkbox" data-test="tab-node-checkbox" />
    <section class="overlay-header">
      <Icon :icon="storage" data-test="icon-storage" />
      <h4 data-test="heading">{{ node?.label }}</h4>
    </section>
    <section class="overlay-content">
      <div class="title">
        <label for="iconCheckbox" data-test="title-label">Tags</label>
      </div>
      <div class="inline">
        <FeatherChipList condensed label="Tag list" data-test="tag-list">
          <FeatherChip v-for="tag in node.anchor.tagValue" :key="tag.id">{{ tag.name }}</FeatherChip>
        </FeatherChipList>
        <FeatherChipList condensed label="Tag list" data-test="tag-list" v-if="isChecked">
          <FeatherChip class="new-chip"
            v-for="tag in tagStore.tagsSelected.filter((d) => !node.anchor.tagValue.find((e) => e.name === d.name))"
            :key="tag.id">{{ tag.name }}</FeatherChip>
        </FeatherChipList>
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import Storage from '@material-design-icons/svg/outlined/storage.svg'
import { InventoryNode } from '@/types/inventory'
import { IIcon } from '@/types'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { useTagStore } from '@/store/Components/tagStore'

const inventoryStore = useInventoryStore()
const tagStore = useTagStore();
const props = defineProps({
  node: {
    type: Object as PropType<InventoryNode>,
    required: true
  }
})

const isChecked = computed(() => !!inventoryStore.nodesSelected.find((d) => d.id === props.node.id) || false);



const storage: IIcon = {
  image: Storage,
  title: 'Node',
  size: 1.5
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';

.overlay {
  $color-header-title: white;

  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(10, 12, 27, 1);
  padding: var(variables.$spacing-s) var(variables.$spacing-l);
  border-radius: 0 vars.$border-radius-m vars.$border-radius-m 0;
  overflow-y: auto;

  .tag-node-checkbox {
    position: absolute;
    top: var(variables.$spacing-s);
    right: var(variables.$spacing-s);

    :deep {
      label {
        display: none;
      }

      .feather-checkbox {
        .box {
          border-color: $color-header-title;
        }

        &[aria-checked='true'] {
          .box {
            border-color: var(variables.$primary);
          }
        }
      }
    }
  }

  >.overlay-header {
    margin-bottom: var(variables.$spacing-s);
    margin-right: var(variables.$spacing-s);
    display: flex;
    flex-direction: row;
    gap: 0.5rem;
    align-items: center;
    color: $color-header-title;

    >h4 {
      color: $color-header-title;
    }
  }

  >.overlay-content {
    >.title {
      display: flex;
      flex-direction: row;
      align-items: center;
      color: $color-header-title;
      margin-bottom: 8px;
    }

    label {
      font-size: 1.2rem;
      font-weight: bold;
    }

    .chip-list {
      margin-top: var(variables.$spacing-s);
      display: inline;

      :deep {
        .chip {
          background-color: var(variables.$primary);

          .label {
            color: var(variables.$primary-text-on-color);
          }
        }

        .chip.new-chip {
          background-color: var(variables.$high-visibility-text-on-surface);

          .label {
            color: var(variables.$high-visibility-on-surface);
          }
        }
      }
    }
  }
}

.inline {
  display: inline;
}
</style>
