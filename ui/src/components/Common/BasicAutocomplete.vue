<!-- 
  Autocomplete search upon text entering in the input box. Search on input focus is disabled to prevent having a too long list.
  
  - Props
    - items-selected: list of items to be saved
    - get-items: method to call to get the list; endpoint query.
    - items: results from the endpoint query
    - render-type: single (default) | multi.
      - multi: 
        - does not allow to add new value if not exists in the list
    - preselectedItems: the list of items that was previously selected
  
  - Options
    - allow-new: to add new value, if not exists in the list (applicable only to 'single' render-type)
  
  TODO:
    - buggy with allow-new option and mouse clicking
      - when mouse clicks on existing item on the list, the new event is also triggered (@new="addValue")
      - probable cause: the `NEW` item is on focus 
      - ok when using arrow up/down and enter key to select item
 -->
<template>
  <div>
    <FeatherAutocomplete
      :model-value="modelValue"
      @update:model-value="updateModelValue"
      @search="search"
      @new="addValue"
      :loading="loading"
      :results="results"
      :label="(props.label as string)"
      :type="props.renderType"
      :allow-new="allowNew"
      data-test="fds-autocomplete"
      :disabled="disabled"
    />
    <FeatherChipList
      v-if="props.showList"
      label="Item chip list with icon"
      data-test="fds-chip-list"
    >
      <FeatherChip
        v-for="(item, index) in selectedItems"
        :key="index"
      >
        <span>{{ item.name }}</span>
        <template v-slot:icon
          ><FeatherIcon
            v-if="!disabled"
            @click="unselectItem(item.name as string)"
            :icon="CancelIcon"
        /></template>
      </FeatherChip>
    </FeatherChipList>
  </div>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { IAutocompleteItemType } from '@featherds/autocomplete'
import CancelIcon from '@featherds/icon/navigation/Cancel'

type IAutocomplete = IAutocompleteItemType & { _text?: string }
type TypeSingle = 'single'
type TypeMulti = 'multi'

const emits = defineEmits(['items-selected'])

const props = defineProps({
  label: {
    type: String,
    requied: true
  },
  getItems: {
    type: Function,
    required: true
  },
  items: {
    type: [Object],
    reqired: true
  },
  renderType: {
    type: String as PropType<TypeSingle | TypeMulti>,
    default: 'single'
  },
  // applicable only to 'single' renderType
  allowNew: {
    type: Boolean,
    default: true
  },
  showList: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean
  },
  preselectedItems: {
    type: [Object],
    reqired: false
  }
})

const loading = ref(false)
const selectedItems = ref<IAutocomplete[]>([])
const results = computed(() => {
  loading.value = false
  let items = props.items?.map((item: Record<string, any>) => ({
    ...item,
    _text: item.name
  }))

  // filter out already selected options
  if (props.renderType === 'multi') {
    items = items.filter((item: Record<string, any>) => {
      return !selectedItems.value.some(({ name }) => name === item.name)
    })
  }

  return items
})

onMounted(() => {
  props.getItems()
})

watchEffect(() => {
  if (props.preselectedItems) {
    selectedItems.value = props.preselectedItems as IAutocomplete[]
  }
})

const modelValue = ref<IAutocomplete[] | undefined>()

const updateModelValue = (selected: any) => {
  if (props.renderType === 'single') {
    const exists = selectedItems.value.some(({ name }) => name === selected.name)
    if (!exists) {
      selectedItems.value.push(selected)

      modelValue.value = undefined

      emits('items-selected', selectedItems.value)
    }
  } else if (props.renderType === 'multi') {
    selectedItems.value = [...selectedItems.value, ...selected]
    emits('items-selected', selectedItems.value)
  }
}

const search = (q: string) => {
  if (!q) return // prevent making request on focus
  loading.value = true
  props.getItems(q)
}

const addValue = (q: string) => {
  const exists = selectedItems.value.some(({ name }) => name === q)

  if (!exists) {
    selectedItems.value?.push({ name: q, _text: q })
    emits('items-selected', selectedItems.value)
  }
}

const unselectItem = (q: string) => {
  selectedItems.value = selectedItems.value?.filter((v) => {
    if (v.name !== q) return v
  })

  emits('items-selected', selectedItems.value)
}

const reset = () => {
  modelValue.value = undefined
  selectedItems.value = []
}

defineExpose({
  reset,
  addValue
})
</script>

<style lang="scss" scoped>
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

:deep(.feather-input-sub-text) {
  display: none;
}
</style>
