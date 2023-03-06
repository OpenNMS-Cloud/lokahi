<template>
  <div class="overlay">
    <feather-checkbox
      id="tagged"
      v-model="isChecked"
      @update:model-value="editNodeTags(node.id)"
      class="tag-node-checkbox"
    />
    <section class="overlay-header">
      <Icon
        :icon="storage"
        data-test="icon-storage"
      />
      <h4 data-test="heading">{{ node?.label }}</h4>
    </section>
    <section class="overlay-content">
      <div class="title">
        <label for="iconCheckbox">Tagged</label
        ><Icon
          :icon="checkbox"
          data-test="icon-checkbox"
        />
      </div>
      <FeatherChipList
        condensed
        label="Tag list"
      >
        <FeatherChip
          v-for="tag in node.anchor.tagValue"
          :key="tag.id"
          >{{ tag.name }}</FeatherChip
        >
      </FeatherChipList>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import Storage from '@material-design-icons/svg/outlined/storage.svg'
import Checkbox from '@material-design-icons/svg/outlined/check_box.svg'
import { NodeContent } from '@/types/inventory'
import { IIcon } from '@/types'

const props = defineProps({
  node: {
    type: Object as PropType<NodeContent>,
    required: true
  }
})

const isChecked = ref(false)

watchEffect(() => {
  isChecked.value = props.node.isTaggingChecked as boolean
})

const emit = defineEmits(['editTagsNode'])

const editNodeTags = (id: number) => {
  emit('editTagsNode', { id, toAdd: isChecked.value })
}

const storage: IIcon = {
  image: Storage,
  title: 'Node',
  size: 1.5
}
const checkbox: IIcon = {
  image: Checkbox,
  title: '',
  size: 3
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
  background-color: rgba(10, 12, 27, 0.75); // darker than shade-1
  padding: var(variables.$spacing-l) var(variables.$spacing-l);
  border-radius: 0 vars.$border-radius-m vars.$border-radius-m 0;
  .tag-node-checkbox {
    position: absolute;
    top: 0;
    right: 0;
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
  > .overlay-header {
    margin-bottom: var(variables.$spacing-xs);
    margin-right: var(variables.$spacing-s);
    display: flex;
    flex-direction: row;
    gap: 0.5rem;
    align-items: center;
    color: $color-header-title;
    > h4 {
      color: $color-header-title;
    }
  }
  > .overlay-content {
    > .title {
      display: flex;
      flex-direction: row;
      align-items: center;
      color: $color-header-title;
    }
    label {
      font-size: 1.5rem;
      font-weight: bold;
    }
    :deep(svg) {
      margin-left: var(variables.$spacing-xs);
    }
    .chip-list {
      margin-top: var(variables.$spacing-s);
      :deep {
        .chip {
          background-color: var(variables.$primary);
          .label {
            color: var(variables.$primary-text-on-color);
          }
        }
      }
    }
  }
}
</style>
