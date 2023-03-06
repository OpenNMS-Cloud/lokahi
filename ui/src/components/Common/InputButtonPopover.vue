<template>
  <FeatherPopover
    :pointer-alignment="alignment"
    :placement="placement"
  >
    <template #default>
      <div class="input-add-popover">
        <FeatherInput
          label="Type..."
          v-model="inputValue"
        />
        <FeatherButton
          @click="handler"
          :disabled="!inputValue"
          primary
          >{{ label }}</FeatherButton
        >
      </div>
    </template>
    <template #trigger="{ attrs, on }">
      <FeatherButton
        v-bind="attrs"
        v-on="on"
        class="add-btn"
      >
        <Icon :icon="icon" />
      </FeatherButton>
    </template>
  </FeatherPopover>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { PointerAlignment, PopoverPlacement } from '@featherds/popover'
import { IIcon } from '@/types'

const props = defineProps({
  placement: {
    type: Object as PropType<PopoverPlacement>,
    default: PopoverPlacement.top
  },
  alignment: {
    type: Object as PropType<PointerAlignment>,
    default: PointerAlignment.center
  },
  icon: {
    type: Object as PropType<IIcon>,
    required: true
  },
  label: {
    type: String,
    default: 'Add'
  },
  handler: {
    type: Function,
    required: true
  }
})

const inputValue = ref()

const handler = () => {
  // reset value in search box, to have the whole list filtering on input text
  props.handler(inputValue.value)
  inputValue.value = ''
}

const buttonSize = `${props.icon.size || 1}rem` // FeatherIcon default width/height: 1rem
const iconPosTop = `${1 - Number(props.icon.size)}rem`
const iconPosLeft = `${1 - Number(props.icon.size)}rem`
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.add-btn {
  color: white;
  border-radius: 50%;
  width: v-bind(buttonSize);
  height: v-bind(buttonSize);
  min-width: inherit;
  padding: var(variables.$spacing-m);
  line-height: inherit;
  background-color: var(variables.$shade-2);
  // TODO: how to remove the weird blue border when button clicked
  :deep {
    > .btn-content {
      display: block;
      > svg {
        top: v-bind(iconPosTop);
        left: v-bind(iconPosLeft);
      }
    }
  }
}
</style>
