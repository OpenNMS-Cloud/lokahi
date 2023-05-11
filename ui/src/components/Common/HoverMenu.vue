<!-- 
  It display an icon, once hover, presenting a list of actionable items
  
  Props:
    - items: actionable item list
      - default: empty list
      - structure:
        {
          label: 'some label',
          handler: some method to be called on click
        }
    - icon: three vertical dots (MoreVert)
      - different icon size could be set
    - listPosition: positioning the list related to the icon
      - 'right' as default
      - configurable: could be expanded later if needed
 -->
<template>
  <div
    class="hover-menu-wrapper"
    ref="contextMenuIconRef"
  >
    <FeatherIcon
      :icon="icon.MoreVert"
      class="hover-menu-icon"
      data-test="hover-menu-icon"
    />
    <ul
      class="hover-menu-list"
      :style="{ top: `${contextMenuListPos.top}px`, left: `${contextMenuListPos.left}px` }"
      data-test="hover-menu-list"
    >
      <li
        v-for="item in items"
        :key="item.label"
        @click="item.handler"
        :data-test="item.label"
      >
        {{ item.label }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import MoreVert from '@featherds/icon/navigation/MoreVert'
import { PropType } from 'vue'
import { ContextMenuItem } from '@/types'

const props = defineProps({
  items: {
    type: Array as PropType<ContextMenuItem[]>,
    default: () => []
  },
  iconSize: {
    type: Number,
    default: 1.5
  },
  listPosition: {
    type: String,
    default: 'right'
  }
})

const propsIconSize = ref(` ${props.iconSize}rem`)
const contextMenuIconRef = ref(null)
const contextMenuIconSize = reactive(useElementSize(contextMenuIconRef))
const contextMenuListPos = reactive({ top: 0, left: 0 })
watchEffect(() => {
  switch (props.listPosition) {
    // to make the positionning configurable when needed
    case 'left':
    case 'bottom':
    case 'top':
      break
    default: // position: right
      contextMenuListPos.left = contextMenuIconSize.width
  }
})

const icon = markRaw({
  MoreVert
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/elevation';

.hover-menu-wrapper {
  position: relative;
  margin-top: 4px;
}

.hover-menu-icon {
  width: v-bind(propsIconSize);
  height: v-bind(propsIconSize);
  &:hover {
    cursor: pointer;
    & + .hover-menu-list {
      display: block;
    }
  }
}

.hover-menu-list {
  position: absolute;
  display: none;
  padding: var(variables.$spacing-s) var(variables.$spacing-m);
  box-shadow: var(variables.$shadow-1);
  @include elevation.elevation(2);
  text-align: left;
  z-index: var(variables.$zindex-tooltip);
  &:hover {
    display: block;
  }
  > * {
    margin-bottom: var(variables.$spacing-xs);
    text-transform: capitalize;
    &:last-child {
      margin-bottom: 0;
    }
    &:hover {
      cursor: pointer;
    }
  }
}
</style>
