<template>
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
</template>

<script lang="ts" setup>
import { ISelectItemType } from '@featherds/select/src/components/types'

const emit = defineEmits(['item-selected'])

const props = defineProps<{
  list: ISelectItemType[] // accept the structure [{id, name}]
  size?: number
  isDisabled?: boolean
}>()

const selectedItem = ref(props.list[0])
const setSelectedItem = (selected: ISelectItemType | undefined) => {
  emit('item-selected', selected?.id)
}
</script>

<style scoped lang="scss">
:deep(.label-border) {
  width: 0 !important;
}
</style>
