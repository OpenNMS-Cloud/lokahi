<template>
  <div>
    <FeatherSelect
      :style="{ width: size + 'px' }"
      label=""
      :options="list"
      text-prop="name"
      v-model="selectedItem"
      hideLabel
      :disabled="isDisabled"
      @update:modelValue="setSelectedItem"
    >
    </FeatherSelect>
    <FeatherChipList
      v-if="selectedItem && selectedItem.id && props.showChip"
      label="Item chip list with icon"
      data-test="fds-select-chip"
      class="basic-select-chip-list">
      <FeatherChip>
          <span>{{ selectedItem.name }}</span>
          <template v-slot:icon>
            <FeatherIcon
              @click="unselectItem()"
              :icon="CancelIcon"
        /></template>
    </FeatherChip>
  </FeatherChipList>
</div>
</template>

<script lang="ts" setup>
import { ISelectItemType } from '@featherds/select/src/components/types'
import CancelIcon from '@featherds/icon/navigation/Cancel'
import { PropType } from 'vue'


const emit = defineEmits(['item-selected'])

const props = defineProps({
  list: {
    type: Object as PropType<ISelectItemType[]>
  }, // accept the structure [{id, name}]
  size: {
    type: Number,
    required: false
  },
  isDisabled: {
    type: Boolean,
    required: false
  },
  showChip: {
    type: Boolean,
    default: false
  }
})

const selectedItem = ref(props.list?.[0] || undefined)
const setSelectedItem = (selected: ISelectItemType | undefined) => {
  emit('item-selected', selected?.id)
}
const unselectItem = () => {
  selectedItem.value = {}
  emit('item-selected', selectedItem.value)
}
</script>

<style scoped lang="scss">
@use '@/styles/mediaQueriesMixins.scss';

:deep(.chip-label-button) {
  display: flex;
  .chip-icon {
    order: 2;
    &:hover {
      cursor: pointer;
    }
    > svg {
      &:hover {
        cursor: pointer;
      }
    }
  }
  > .label {
    order: 1;
  }
}
:deep(.label-border) {
  width: 0 !important;
}
.basic-select-chip-list {
  margin-top: 0;
}
</style>
