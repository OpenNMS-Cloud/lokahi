<!--
  Props:
    - items: actionable item list
      - default: empty list
      - structure:
        {
          label: 'some label',
          handler: some method to be called on click
        }
 -->
<template>
  <FeatherDropdown>
    <template v-slot:trigger="{ attrs, on }">
      <FeatherButton
        link
        href="#"
        v-bind="attrs"
        v-on="on"
        icon="More Options"
        data-test="more-options-btn"
      >
        <FeatherIcon :icon="icon.MoreVert" />
      </FeatherButton>
    </template>
    <FeatherDropdownItem
      v-for="item in items"
      :key="item.label"
      data-test="dropdownItem"
      @click="item.handler"
    >
      {{ item.label }}
    </FeatherDropdownItem>
  </FeatherDropdown>
</template>

<script setup lang="ts">
import MoreVert from '@featherds/icon/navigation/MoreVert'
import { PropType } from 'vue'
import { ContextMenuItem } from '@/types'

defineProps({
  items: {
    type: Array as PropType<ContextMenuItem[]>,
    default: () => []
  }
})

const icon = markRaw({
  MoreVert
})
</script>
